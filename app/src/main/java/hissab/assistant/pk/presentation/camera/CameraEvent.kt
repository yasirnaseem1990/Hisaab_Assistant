package hissab.assistant.pk.presentation.camera

import java.io.File

/**
 * One-off events emitted by [CameraViewModel] via a `SharedFlow` (replay = 0).
 *
 * Modelled separately from [CameraUiState] because they must fire exactly once
 * and must NOT be re-delivered on configuration change.
 */
sealed interface CameraEvent {

    /** A photo was captured and saved to [file] (a JPEG in the app's external cache). */
    data class ImageReady(val file: File) : CameraEvent

    /** Capture failed; [message] is safe to surface to the user. */
    data class Error(val message: String) : CameraEvent
}
