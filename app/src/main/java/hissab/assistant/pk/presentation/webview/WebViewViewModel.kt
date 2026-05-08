package hissab.assistant.pk.presentation.webview

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import hissab.assistant.pk.domain.usecase.GetDefaultWebPageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for the WebView screen.
 *
 * Responsibilities:
 *  - Resolve which URL to load via the [GetDefaultWebPageUseCase].
 *  - Hold the screen's [WebViewUiState] (loading/progress/error).
 *  - Expose intents the Composable can call when WebView callbacks fire.
 */
@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val getDefaultWebPageUseCase: GetDefaultWebPageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebViewUiState())
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()

    init {
        loadDefaultPage()
    }

    /** Resolves the default page through the domain use case. */
    private fun loadDefaultPage() {
        val page = getDefaultWebPageUseCase()
        _uiState.update {
            it.copy(
                url = page.url,
                isLoading = true,
                progress = 0,
                errorMessage = null
            )
        }
    }

    fun onPageStarted() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    }

    fun onProgressChanged(newProgress: Int) {
        _uiState.update { it.copy(progress = newProgress) }
    }

    fun onPageFinished() {
        _uiState.update { it.copy(isLoading = false, progress = 100) }
    }

    fun onError(message: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = message
            )
        }
    }

    /** Called from the UI to clear the error and trigger another load attempt. */
    fun onRetry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, progress = 0) }
    }
}
