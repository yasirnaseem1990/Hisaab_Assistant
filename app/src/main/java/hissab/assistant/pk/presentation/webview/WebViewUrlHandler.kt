package hissab.assistant.pk.presentation.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * Handles URL navigation routing for [WebViewScreen].
 *
 * Separates standard internal web navigation from external schemes and application links
 * (e.g. WhatsApp, phone dialer, email, SMS, Play Store, and intent: URIs) to prevent
 * net::ERR_UNKNOWN_URL_SCHEME errors.
 */
object WebViewUrlHandler {

    private const val TAG = "WebViewUrlHandler"

    private val INTERNAL_NON_HTTP_SCHEMES = setOf(
        "file",
        "data",
        "about",
        "javascript",
        "blob"
    )

    /**
     * Determines whether the given URL should be overridden (intercepted) and handed off
     * to the external system rather than loaded inside the WebView.
     */
    fun shouldOverrideUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false

        val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
        if (scheme.isEmpty()) return false

        // Standard web schemes are kept in WebView, unless the destination is WhatsApp web
        // (wa.me or api.whatsapp.com) which is explicitly intended to open the WhatsApp app.
        if (scheme == "http" || scheme == "https") {
            return isWhatsAppWebUrl(url)
        }

        // Internal pseudo-schemes stay in WebView
        if (scheme in INTERNAL_NON_HTTP_SCHEMES) {
            return false
        }

        // All custom application schemes (whatsapp://, intent://, tel:, mailto:, sms:, market://, etc.)
        return true
    }

    /**
     * Extracts host from URL string without requiring android.net.Uri.
     */
    fun extractHost(url: String): String {
        val afterScheme = url.substringAfter("://", "")
        if (afterScheme.isEmpty()) return ""
        val hostWithPort = afterScheme.substringBefore('/').substringBefore('?')
        return hostWithPort.substringBefore(':').lowercase()
    }

    /**
     * Identifies if a web URL points to WhatsApp web redirect services (wa.me / api.whatsapp.com).
     */
    fun isWhatsAppWebUrl(url: String): Boolean {
        val host = extractHost(url)
        return host == "wa.me" || host == "api.whatsapp.com" || host.endsWith(".whatsapp.com")
    }

    /**
     * Determines whether a given URL is a WhatsApp destination (either custom scheme or web redirect).
     */
    fun isWhatsAppUrl(url: String): Boolean {
        val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
        return scheme == "whatsapp" || isWhatsAppWebUrl(url)
    }

    /**
     * Intercepts and executes the appropriate external Intent for the given URL.
     *
     * @param context Android context
     * @param url The full target URL string
     * @param intentCreator factory lambda to create an Intent for a URL (for testing decoupling)
     * @param intentLauncher lambda to start the intent (defaults to context.startActivity)
     * @param toastDisplayer lambda to display toast messages
     * @return true if the URL was handled/consumed and should NOT be loaded by WebView
     */
    fun handleExternalUrl(
        context: Context,
        url: String,
        intentCreator: (String) -> Intent? = { targetUrl -> createIntentForUrl(targetUrl) },
        intentLauncher: (Intent) -> Unit = { intent -> context.startActivity(intent) },
        toastDisplayer: (String) -> Unit = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
    ): Boolean {
        return try {
            val intent = intentCreator(url)
            if (intent != null) {
                intentLauncher(intent)
                true
            } else {
                false
            }
        } catch (e: ActivityNotFoundException) {
            handleMissingApp(context, url, intentCreator, intentLauncher, toastDisplayer)
            true
        } catch (e: Exception) {
            logError("Error handling external URL: $url", e)
            true
        }
    }

    /**
     * Creates an appropriate [Intent] for the given URL.
     */
    fun createIntentForUrl(url: String): Intent? {
        val intent = if (url.startsWith("intent://", ignoreCase = true)) {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    /**
     * Fallback handler when the target app is not installed on the user's device.
     */
    fun handleMissingApp(
        context: Context,
        url: String,
        intentCreator: (String) -> Intent? = { targetUrl -> createIntentForUrl(targetUrl) },
        intentLauncher: (Intent) -> Unit = { intent -> context.startActivity(intent) },
        toastDisplayer: (String) -> Unit = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
    ) {
        if (isWhatsAppUrl(url)) {
            // WhatsApp is not installed. Fall back to Play Store market link, then web link.
            try {
                val playStoreIntent = intentCreator("market://details?id=com.whatsapp")
                if (playStoreIntent != null) {
                    intentLauncher(playStoreIntent)
                } else {
                    toastDisplayer("WhatsApp is not installed")
                }
            } catch (_: Exception) {
                try {
                    val webPlayStoreIntent = intentCreator("https://play.google.com/store/apps/details?id=com.whatsapp")
                    if (webPlayStoreIntent != null) {
                        intentLauncher(webPlayStoreIntent)
                    } else {
                        toastDisplayer("WhatsApp is not installed")
                    }
                } catch (_: Exception) {
                    toastDisplayer("WhatsApp is not installed")
                }
            }
        } else if (url.startsWith("intent://", ignoreCase = true)) {
            try {
                val parsed = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                val fallbackUrl = parsed.getStringExtra("browser_fallback_url")
                if (!fallbackUrl.isNullOrEmpty()) {
                    val fallbackIntent = intentCreator(fallbackUrl)
                    if (fallbackIntent != null) {
                        intentLauncher(fallbackIntent)
                        return
                    }
                }
                val pkg = parsed.`package`
                if (!pkg.isNullOrEmpty()) {
                    val playStoreIntent = intentCreator("market://details?id=$pkg")
                    if (playStoreIntent != null) {
                        intentLauncher(playStoreIntent)
                        return
                    }
                }
            } catch (e: Exception) {
                logError("Error resolving fallback for intent: $url", e)
            }
            toastDisplayer("Application not found to handle this request")
        } else {
            toastDisplayer("No app found to open this link")
        }
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        try {
            Log.e(TAG, message, throwable)
        } catch (_: Exception) {
            // Ignored in local unit tests where android.util.Log is not mocked
        }
    }
}
