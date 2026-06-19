package hissab.assistant.pk.presentation.camera

/**
 * Immutable UI state for the in-app camera overlay.
 *
 * @param isProcessing true while a captured frame is being encoded to Base64.
 *   The capture button is disabled and a spinner is shown to prevent
 *   double-captures.
 * @param useBackCamera which lens is currently bound. Defaults to the back
 *   camera to match the iOS implementation.
 */
data class CameraUiState(
    val isProcessing: Boolean = false,
    val useBackCamera: Boolean = true,
)
