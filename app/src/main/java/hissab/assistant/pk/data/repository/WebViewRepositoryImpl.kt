package hissab.assistant.pk.data.repository

import hissab.assistant.pk.domain.model.WebPage
import hissab.assistant.pk.domain.repository.WebViewRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete [WebViewRepository] backed by a hard-coded URL.
 *
 * If, in the future, the URL should come from BuildConfig, a remote config
 * service, or local preferences, this is the only class that needs to change —
 * the rest of the app simply depends on the [WebViewRepository] abstraction.
 */
@Singleton
class WebViewRepositoryImpl @Inject constructor() : WebViewRepository {

    override fun getDefaultPage(): WebPage = WebPage(
        url = DEFAULT_URL,
        title = "Hisaab Assistant"
    )

    private companion object {
        const val DEFAULT_URL = "https://main.d2j64p9g0am2pq.amplifyapp.com"
    }
}
