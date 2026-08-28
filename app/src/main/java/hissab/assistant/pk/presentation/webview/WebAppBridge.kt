package hissab.assistant.pk.presentation.webview

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * JavaScript <-> native bridge for the WebView, mirroring the iOS
 * `WKScriptMessageHandler` contract.
 *
 * The web app opens the native camera overlay by calling:
 * ```js
 * window.AndroidBridge.openCamera();
 * ```
 * Native then returns the captured photo to the page via the SAME callback the
 * iOS app uses, so a single web integration works on both platforms:
 * ```js
 * window.receiveCameraImage('data:image/jpeg;base64,....');
 * ```
 *
 * ### Threading
 * Methods annotated with [JavascriptInterface] are invoked on a WebView binder
 * thread, NOT the main thread. Any UI-affecting work (showing the overlay) MUST
 * be marshalled to the main thread — done here via [mainHandler] so callers
 * receive [onOpenCamera] safely on the UI thread.
 *
 * ### Security
 * Only the explicit `@JavascriptInterface` methods below are reachable from JS;
 * nothing else on this object is exposed. The bridge carries no secrets and
 * performs no privileged action on its own — it only requests UI the user must
 * still interact with.
 */
class WebAppBridge(
    private val onOpenCamera: () -> Unit,
    private val onDownloadFile: (dataUrl: String, fileName: String?) -> Unit = { _, _ -> },
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Entry point the web app calls to launch the in-app camera overlay. */
    @JavascriptInterface
    fun openCamera() {
        mainHandler.post { onOpenCamera() }
    }

    /** Lets the web app feature-detect native camera support before calling [openCamera]. */
    @JavascriptInterface
    fun isCameraAvailable(): Boolean = true

    /**
     * Receives a converted blob or data URL from JS and delegates to native download/share.
     */
    @JavascriptInterface
    fun processDataUrl(dataUrl: String, fileName: String?) {
        mainHandler.post { onDownloadFile(dataUrl, fileName) }
    }

    /**
     * Entry point for JS to share a file directly via Data URL.
     */
    @JavascriptInterface
    fun shareFile(dataUrl: String, fileName: String?) {
        mainHandler.post { onDownloadFile(dataUrl, fileName) }
    }

    companion object {
        /** The `window.<NAME>` object the web app talks to. */
        const val NAME = "AndroidBridge"
    }
}
