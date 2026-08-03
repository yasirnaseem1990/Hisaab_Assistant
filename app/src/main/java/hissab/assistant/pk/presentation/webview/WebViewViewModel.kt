package hissab.assistant.pk.presentation.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hissab.assistant.pk.domain.usecase.GetDefaultWebPageUseCase
import hissab.assistant.pk.domain.usecase.ObserveFcmTokenUseCase
import hissab.assistant.pk.domain.usecase.SyncFcmTokenUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the WebView screen.
 *
 * Responsibilities:
 *  - Resolve which URL to load via the [GetDefaultWebPageUseCase].
 *  - Hold the screen's [WebViewUiState] (loading/progress/error).
 *  - Expose intents the Composable can call when WebView callbacks fire.
 *  - Expose the FCM token as a [StateFlow] so the screen can inject it into
 *    the page (and re-inject on reloads and mid-session token rotations).
 */
@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val getDefaultWebPageUseCase: GetDefaultWebPageUseCase,
    observeFcmToken: ObserveFcmTokenUseCase,
    private val syncFcmToken: SyncFcmTokenUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebViewUiState())
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()

    /**
     * Last-known FCM token; null until one is available. Emits again on
     * rotation, which retriggers injection in the screen.
     */
    val fcmToken: StateFlow<String?> = observeFcmToken()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(FCM_TOKEN_STOP_TIMEOUT_MS),
            initialValue = null,
        )

    init {
        loadDefaultPage()
        syncFcmTokenFromProvider()
    }

    /**
     * Requests a fresh token from FCM. Failure is non-fatal: the cached token
     * (if any) still flows through [fcmToken]; if there is none, the web app
     * simply never sees `MYHISAAB_FCM_TOKEN` — same behaviour as a browser
     * without push support, which it must handle anyway.
     */
    private fun syncFcmTokenFromProvider() {
        viewModelScope.launch {
            syncFcmToken()
                .onFailure { error ->
                    android.util.Log.w(TAG, "FCM token sync failed; using cached token if present.", error)
                }
        }
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
        _uiState.update {
            it.copy(
                isLoading = false,
                progress = 100,
                pageLoadCount = it.pageLoadCount + 1,
            )
        }
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

    private companion object {
        const val TAG = "WebViewViewModel"

        /** Keep the token flow warm across config changes (standard 5s grace). */
        const val FCM_TOKEN_STOP_TIMEOUT_MS = 5_000L
    }
}
