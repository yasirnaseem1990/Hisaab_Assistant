package hissab.assistant.pk.domain.usecase

import hissab.assistant.pk.domain.repository.ImageProcessor
import java.io.File
import javax.inject.Inject

/**
 * Encodes a freshly-captured photo into a Base64 JPEG data URL ready to be
 * delivered to the web app.
 *
 * Thin by design: it exists so the presentation layer depends on a stable
 * domain operation ("encode this capture") rather than on [ImageProcessor]
 * directly, keeping the ViewModel decoupled from the data layer and leaving an
 * obvious seam for future rules (watermarking, size policies, analytics).
 */
class EncodeCapturedImageUseCase @Inject constructor(
    private val imageProcessor: ImageProcessor,
) {
    suspend operator fun invoke(
        imageFile: File,
        quality: Int = ImageProcessor.DEFAULT_QUALITY,
    ): Result<String> = imageProcessor.toBase64DataUrl(imageFile = imageFile, quality = quality)
}
