package hissab.assistant.pk.domain.repository

import hissab.assistant.pk.domain.model.WebPage

/**
 * Abstraction over the source that provides the URL/page configuration to load
 * inside the WebView.
 *
 * The presentation/use-case layers depend on this interface — never on concrete
 * implementations — so the data source (remote config, local constant, BuildConfig,
 * etc.) can change without rippling through the rest of the app.
 */
interface WebViewRepository {
    /**
     * Returns the [WebPage] that should be displayed when the app launches.
     */
    fun getDefaultPage(): WebPage
}
