package hissab.assistant.pk.presentation.webview

/**
 * Immutable UI state for the WebView screen, exposed by [WebViewViewModel].
 *
 * Modelling state as a single sealed/data structure makes the screen's behaviour
 * easy to reason about and easy to render declaratively from Compose.
 */
data class WebViewUiState(
    val url: String = "",
    val isLoading: Boolean = true,
    val progress: Int = 0,
    val errorMessage: String? = null,
    /**
     * Monotonic counter incremented on every completed page load. Navigations
     * and reloads wipe `window`-scoped injections, so the FCM token injector
     * keys off this to re-run after each load (0 = nothing loaded yet).
     */
    val pageLoadCount: Int = 0,
)
