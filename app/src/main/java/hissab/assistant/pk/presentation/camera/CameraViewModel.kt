package hissab.assistant.pk.presentation.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Owns the camera overlay's state and the capture flow.
 *
 * The ViewModel knows nothing about CameraX: the Composable drives the hardware
 * (preview/capture) and hands the resulting [File] here. The ViewModel is the
 * single place that de-dupes captures and forwards the result as a one-off event.
 *
 * The captured file is deliberately NOT deleted here — the WebView's file input
 * (or the JS bridge) reads it after capture; the OS reclaims the external cache.
 */
@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    // replay = 0 so a delivered photo is never re-emitted after rotation.
    private val _events = MutableSharedFlow<CameraEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<CameraEvent> = _events.asSharedFlow()

    /**
     * Called by the UI once CameraX has written a still to [imageFile].
     * Guards against re-entrancy so a rapid double-tap can't deliver twice.
     */
    fun onImageCaptured(imageFile: File) {
        if (_uiState.value.isProcessing) return
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            _events.emit(CameraEvent.ImageReady(imageFile))
        }
    }

    /** Called when CameraX itself fails to capture (hardware/IO error). */
    fun onCaptureFailed(message: String) {
        _uiState.update { it.copy(isProcessing = false) }
        viewModelScope.launch { _events.emit(CameraEvent.Error(message)) }
    }

    /** Flip between front and back lenses. Ignored mid-capture to avoid rebinding races. */
    fun toggleLens() {
        if (_uiState.value.isProcessing) return
        _uiState.update { it.copy(useBackCamera = !it.useBackCamera) }
    }
}
