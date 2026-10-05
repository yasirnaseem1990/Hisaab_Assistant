package hissab.assistant.pk.presentation.webview

/**
 * Builds the JavaScript snippet injected into the WebView to convey the active
 * FCM registration token to the web application.
 */
object FcmTokenScript {
    fun build(token: String): String {
        val sanitizedToken = token.replace("\\", "\\\\").replace("'", "\\'")
        return """
            (function() {
                window.MYHISAAB_FCM_TOKEN = '$sanitizedToken';
                if (typeof window.receiveFcmToken === 'function') {
                    window.receiveFcmToken('$sanitizedToken');
                }
            })();
        """.trimIndent()
    }
}
