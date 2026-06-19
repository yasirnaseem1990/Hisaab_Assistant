package hissab.assistant.pk.domain.repository

import java.io.File

/**
 * Abstraction over turning a captured photo on disk into a Base64 *data URL*
 * that can be handed back to the web layer (mirrors the iOS contract:
 * `data:image/jpeg;base64,...`).
 *
 * Declared in the domain layer in terms of [File] (pure JVM, no Android types)
 * so the layer stays framework-agnostic and the contract is trivially mockable
 * in unit tests. The Android-specific decode/rotate/compress work lives in the
 * data-layer implementation.
 */
interface ImageProcessor {

    /**
     * Reads [imageFile], corrects its orientation, optionally downscales it and
     * re-encodes it as a JPEG data URL.
     *
     * The function is **main-safe** — the implementation moves heavy work onto a
     * background dispatcher — so callers may invoke it from the main thread.
     *
     * @param imageFile the JPEG written by the camera.
     * @param quality JPEG compression quality, 0..100. Defaults to 60 to match
     *   the iOS implementation (`jpegData(compressionQuality: 0.6)`).
     * @param maxDimensionPx longest-edge cap applied before encoding. Large
     *   sensors otherwise produce multi-megabyte Base64 strings that are slow
     *   and risky to push across the JS bridge. `0` disables downscaling.
     * @return [Result.success] with a `data:image/jpeg;base64,...` string, or
     *   [Result.failure] describing why encoding failed. Never throws for
     *   expected failures (missing/corrupt file, OOM).
     */
    suspend fun toBase64DataUrl(
        imageFile: File,
        quality: Int = DEFAULT_QUALITY,
        maxDimensionPx: Int = DEFAULT_MAX_DIMENSION_PX,
    ): Result<String>

    companion object {
        const val DEFAULT_QUALITY: Int = 60
        const val DEFAULT_MAX_DIMENSION_PX: Int = 1920
    }
}
