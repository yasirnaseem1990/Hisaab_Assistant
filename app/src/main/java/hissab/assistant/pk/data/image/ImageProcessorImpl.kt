package hissab.assistant.pk.data.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import hissab.assistant.pk.di.IoDispatcher
import hissab.assistant.pk.domain.repository.ImageProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Android implementation of [ImageProcessor].
 *
 * Pipeline: decode (sub-sampled) -> correct EXIF orientation -> downscale to a
 * sane max dimension -> JPEG compress -> Base64 (NO_WRAP) -> data URL.
 *
 * All work runs on an injected IO dispatcher, so the function is main-safe.
 * Intermediate [Bitmap]s are recycled eagerly to keep the native heap small —
 * full-resolution captures can be tens of megabytes uncompressed.
 */
@Singleton
class ImageProcessorImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ImageProcessor {

    override suspend fun toBase64DataUrl(
        imageFile: File,
        quality: Int,
        maxDimensionPx: Int,
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            coroutineContext.ensureActive()

            require(imageFile.exists() && imageFile.length() > 0L) {
                "Captured image file is missing or empty: ${imageFile.path}"
            }

            val decoded = decodeSampledBitmap(imageFile, maxDimensionPx)
                ?: error("Unable to decode captured image")

            coroutineContext.ensureActive()
            val oriented = applyExifOrientation(imageFile, decoded)
            val scaled = downscaleIfNeeded(oriented, maxDimensionPx)

            coroutineContext.ensureActive()
            val base64 = ByteArrayOutputStream().use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), stream)
                Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            }
            scaled.recycle()

            DATA_URL_PREFIX + base64
        }.onFailure { throwable ->
            // runCatching also traps CancellationException — never swallow it,
            // or structured concurrency / lifecycle cancellation silently breaks.
            if (throwable is CancellationException) throw throwable
        }
    }

    /** Decodes with an [BitmapFactory.Options.inSampleSize] so we never load far more pixels than we keep. */
    private fun decodeSampledBitmap(file: File, maxDimensionPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx)
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimensionPx: Int): Int {
        if (maxDimensionPx <= 0 || width <= 0 || height <= 0) return 1
        var sampleSize = 1
        var longest = max(width, height)
        while (longest / 2 >= maxDimensionPx) {
            longest /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * Cameras typically store the sensor orientation as EXIF metadata rather than
     * baking it into the pixels. We must apply it manually, otherwise the encoded
     * JPEG looks rotated/mirrored on the web side.
     */
    private fun applyExifOrientation(file: File, bitmap: Bitmap): Bitmap {
        val orientation = ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f); matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f); matrix.postScale(-1f, 1f)
            }

            else -> return bitmap // ORIENTATION_NORMAL / UNDEFINED -> nothing to do
        }

        val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (transformed !== bitmap) bitmap.recycle()
        return transformed
    }

    private fun downscaleIfNeeded(bitmap: Bitmap, maxDimensionPx: Int): Bitmap {
        if (maxDimensionPx <= 0) return bitmap
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimensionPx) return bitmap

        val scale = maxDimensionPx.toFloat() / longest
        val targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)

        val scaled = bitmap.scale(targetWidth, targetHeight)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private companion object {
        const val DATA_URL_PREFIX = "data:image/jpeg;base64,"
    }
}
