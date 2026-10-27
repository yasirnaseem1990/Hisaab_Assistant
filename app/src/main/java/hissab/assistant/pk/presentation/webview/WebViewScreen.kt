package hissab.assistant.pk.presentation.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hissab.assistant.pk.R
import hissab.assistant.pk.presentation.camera.CameraOverlay
import hissab.assistant.pk.presentation.notification.NotificationPermissionManager
import hissab.assistant.pk.presentation.notification.NotificationSettingsPrompt
import hissab.assistant.pk.presentation.notification.WhatsAppNotificationGate
import hissab.assistant.pk.presentation.notification.notificationSettingsIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

/**
 * Production-ready WebViewScreen using MVVM architecture.
 * Handles Edge-to-Edge, Hardware Permissions, Lifecycle events, 
 * and Camera/Gallery image picking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    modifier: Modifier = Modifier,
    viewModel: WebViewViewModel = hiltViewModel(),
) {
    val uiState: WebViewUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fcmToken: String? by viewModel.fcmToken.collectAsStateWithLifecycle()
    val context: Context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val scope: CoroutineScope = rememberCoroutineScope()

    // Reference to WebView for back-nav and lifecycle management
    var webViewRef: WebView? by remember { mutableStateOf(null) }
    var canGoBack: Boolean by remember { mutableStateOf(false) }

    // Controls visibility of the native in-app camera overlay, opened via the
    // JS bridge (window.AndroidBridge.openCamera()).
    var showCameraOverlay: Boolean by remember { mutableStateOf(false) }

    // --- State for Image Picking ---
    var filePathCallback: ValueCallback<Array<Uri>>? by remember { mutableStateOf(null) }
    var showImageSourceSheet: Boolean by remember { mutableStateOf(false) }
    val sheetState: SheetState = rememberModalBottomSheetState()

    // Gallery Launcher (system picker — matches iOS "Photo Library").
    val galleryLauncher: ManagedActivityResultLauncher<String, List<@JvmSuppressWildcards Uri>> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            filePathCallback?.onReceiveValue(if (uris.isNotEmpty()) uris.toTypedArray() else null)
            filePathCallback = null
        }
    )

    // Permission Request for the WebView's own getUserMedia (camera/mic) requests.
    // The in-app camera overlay handles its OWN camera permission internally.
    var pendingPermissionRequest: PermissionRequest? by remember { mutableStateOf<PermissionRequest?>(null) }

    val permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { permissions ->
                val allGranted: Boolean = permissions.values.all { it }
                if (allGranted) {
                    pendingPermissionRequest?.grant(pendingPermissionRequest?.resources)
                } else {
                    pendingPermissionRequest?.deny()
                }
                pendingPermissionRequest = null
            }
        )

    // --- Notification check before WhatsApp (mirrors iOS) ---
    // Triggered by the web app's MyHisaabAndroid.checkNotificationPermission call
    // (first WhatsApp tap) and by any navigation to a WhatsApp URL. Asks for
    // notifications, or points to Settings once they are permanently off, at most
    // once per launch; then replies to the web app and/or opens WhatsApp, whatever
    // the user chose.
    val activity: Activity? = LocalActivity.current
    // Checks only: this screen launches the request itself so it can record the
    // rationale state just before asking (see WhatsAppNotificationGate).
    val notificationPermission: NotificationPermissionManager? = remember(activity) {
        activity?.let { NotificationPermissionManager(it, launcher = null) }
    }
    var pendingWhatsAppUrl: String? by rememberSaveable { mutableStateOf(null) }
    var isNotificationCheckRunning: Boolean by rememberSaveable { mutableStateOf(false) }
    var rationaleBeforeRequest: Boolean by rememberSaveable { mutableStateOf(false) }
    var showNotificationPrompt: Boolean by rememberSaveable { mutableStateOf(false) }
    // Replies only matter to the page that asked, so these needn't survive process death.
    val pendingCheckRequests: MutableList<NotificationCheckRequest> = remember { mutableListOf() }

    fun finishNotificationCheck() {
        isNotificationCheckRunning = false
        val status = if (notificationPermission?.areNotificationsEnabled() != false) {
            NotificationPermissionScript.STATUS_GRANTED
        } else {
            NotificationPermissionScript.STATUS_DENIED
        }
        pendingCheckRequests.forEach { request ->
            webViewRef?.evaluateJavascript(NotificationPermissionScript.reply(request, status), null)
        }
        pendingCheckRequests.clear()
        pendingWhatsAppUrl?.let { url ->
            pendingWhatsAppUrl = null
            WebViewUrlHandler.handleExternalUrl(context, url)
        }
    }

    fun finishOrShowPrompt(action: WhatsAppNotificationGate.Action) {
        if (action == WhatsAppNotificationGate.Action.SHOW_SETTINGS_PROMPT) {
            showNotificationPrompt = true
        } else {
            finishNotificationCheck()
        }
    }

    val notificationPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                finishOrShowPrompt(
                    WhatsAppNotificationGate.onPermissionResult(
                        granted = granted,
                        rationaleBeforeRequest = rationaleBeforeRequest,
                        rationaleAfterRequest = notificationPermission?.shouldShowRationale() == true,
                    )
                )
            }
        )

    // Fires when the user comes back from Settings, whatever they changed there.
    val notificationSettingsLauncher: ManagedActivityResultLauncher<Intent, ActivityResult> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = { finishNotificationCheck() }
        )

    fun startNotificationCheck() {
        // A check already in progress also finishes this trigger (see finishNotificationCheck).
        if (isNotificationCheckRunning) return
        val action = WhatsAppNotificationGate.onCheckRequested(
            notificationsEnabled = notificationPermission?.areNotificationsEnabled() ?: true,
            canRequestPermission = notificationPermission?.canRequestPermission() == true,
            checkedThisLaunch = viewModel.isNotificationCheckDone,
        )
        if (action == WhatsAppNotificationGate.Action.CONTINUE) {
            finishNotificationCheck()
            return
        }
        isNotificationCheckRunning = true
        viewModel.onNotificationCheckDone()
        if (action == WhatsAppNotificationGate.Action.REQUEST_PERMISSION) {
            rationaleBeforeRequest = notificationPermission?.shouldShowRationale() == true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showNotificationPrompt = true
        }
    }

    val onWhatsAppUrl: (String) -> Unit = { url ->
        pendingWhatsAppUrl = url
        startNotificationCheck()
    }

    val onNotificationCheckRequested: (NotificationCheckRequest) -> Unit = { request ->
        pendingCheckRequests += request
        startNotificationCheck()
    }

    // Handle System Back Button
    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    // --- FCM token injection (mirrors iOS `injectFCMToken`) ---
    // Re-runs when: (a) the token first arrives or rotates mid-session, and
    // (b) every completed page load — navigations wipe window-scoped globals,
    // so injecting once (the iOS approach) silently loses the token on reload.
    // evaluateJavascript must run on the main thread; LaunchedEffect does.
    LaunchedEffect(fcmToken, uiState.pageLoadCount, webViewRef) {
        val token: String? = fcmToken
        val webView: WebView? = webViewRef
        if (token.isNullOrBlank() || uiState.pageLoadCount == 0 || webView == null) {
            return@LaunchedEffect
        }
        webView.evaluateJavascript(FcmTokenScript.build(token)) { result ->
            // evaluateJavascript reports "null" on success for statements;
            // log only for diagnosability, never surface to the user.
            Log.d("WebViewScreen", "FCM token injected into web (result=$result)")
        }
    }

    // Lifecycle Management: Pause WebView when app is in background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webViewRef?.onPause()
                Lifecycle.Event.ON_RESUME -> webViewRef?.onResume()
                Lifecycle.Event.ON_DESTROY -> {
                    webViewRef?.destroy()
                    webViewRef = null
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Inset content layer (WebView, loading, error). The camera overlay below
        // is intentionally OUTSIDE this padding so it can render edge-to-edge.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues())
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Loading State UI
            if (uiState.isLoading && uiState.progress < 100) {
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        configureSettings()

                        webViewClient = createWebViewClient(
                            onPageStarted = { viewModel.onPageStarted() },
                            onPageFinished = { view ->
                                canGoBack = view?.canGoBack() == true
                                viewModel.onPageFinished()
                            },
                            onError = { msg -> viewModel.onError(msg) },
                            onWhatsAppUrl = onWhatsAppUrl,
                        )

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                viewModel.onProgressChanged(newProgress)
                            }

                            override fun onPermissionRequest(request: PermissionRequest) {
                                val resources = request.resources
                                val permsNeeded = mutableListOf<String>()

                                if (resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                                    permsNeeded.add(Manifest.permission.CAMERA)
                                }
                                if (resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                                    permsNeeded.add(Manifest.permission.RECORD_AUDIO)
                                }

                                if (permsNeeded.isEmpty()) {
                                    request.grant(resources)
                                    return
                                }

                                // Senior Strategy: Check if already granted to avoid UI flicker
                                val allGranted = permsNeeded.all {
                                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                                }

                                if (allGranted) {
                                    request.grant(resources)
                                } else {
                                    pendingPermissionRequest = request
                                    permissionLauncher.launch(permsNeeded.toTypedArray())
                                }
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                callback: ValueCallback<Array<Uri>>,
                                params: FileChooserParams,
                            ): Boolean {
                                filePathCallback?.onReceiveValue(null) // Cancel any previous callback
                                filePathCallback = callback
                                showImageSourceSheet = true
                                return true
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                            handleDownload(
                                context = context,
                                webView = this,
                                scope = scope,
                                url = url,
                                contentDisposition = contentDisposition,
                                mimeType = mimeType
                            )
                        }

                        // JS bridge: lets the web app open the native camera
                        // overlay or send downloaded files/blobs to native code.
                        addJavascriptInterface(
                            WebAppBridge(
                                onOpenCamera = { showCameraOverlay = true },
                                onDownloadFile = { dataUrl, fileName ->
                                    scope.launch(Dispatchers.IO) {
                                        val file = saveAndPrepareDataUrl(
                                            context = context,
                                            dataUrl = dataUrl,
                                            suggestedFileName = fileName
                                        )
                                        if (file != null) {
                                            val mime = determineMimeType(file, null)
                                            withContext(Dispatchers.Main) {
                                                shareFile(context, file, mime)
                                            }
                                        }
                                    }
                                }
                            ),
                            WebAppBridge.NAME,
                        )

                        // JS bridge: the web app asks native to check notification
                        // permission on the first WhatsApp tap.
                        addJavascriptInterface(
                            NotificationPermissionBridge(onCheckRequested = onNotificationCheckRequested),
                            NotificationPermissionBridge.NAME,
                        )

                        webViewRef = this
                        loadUrl(uiState.url)
                    }
                },
                update = { _ ->
                    // Handle dynamic URL updates if necessary
                }
            )
        }

        // --- Image Source Selection Sheet ---
        if (showImageSourceSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showImageSourceSheet = false
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = null
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Choose Image Source",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    SourceItem(
                        text = "Camera",
                        icon = Icons.Default.PhotoCamera,
                        onClick = {
                            // Open the in-app camera overlay (over the WebView)
                            // instead of the system camera app. The pending
                            // filePathCallback stays alive until the overlay
                            // returns a photo (or is dismissed).
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showImageSourceSheet = false
                                showCameraOverlay = true
                            }
                        }
                    )

                    SourceItem(
                        text = "Gallery",
                        icon = Icons.Default.PhotoLibrary,
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showImageSourceSheet = false
                                galleryLauncher.launch("image/*")
                            }
                        }
                    )
                }
            }
        }

        // Error Handling Layer
        uiState.errorMessage?.let { message ->
            ErrorOverlay(
                message = message,
                onRetry = {
                    viewModel.onRetry()
                    webViewRef?.reload()
                }
            )
        }
        } // end inset content layer

        // --- Native in-app Camera overlay (over the WebView) ---
        // Opened from the file-chooser "Camera" option, or the JS bridge.
        if (showCameraOverlay) {
            CameraOverlay(
                onImageCaptured = { file ->
                    val callback = filePathCallback
                    if (callback != null) {
                        // HTML <input type=file> path: hand the photo back as a Uri.
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )
                        callback.onReceiveValue(arrayOf(uri))
                        filePathCallback = null
                    } else {
                        // JS-bridge path: deliver as a data URL via the shared callback.
                        webViewRef?.let { injectFileAsDataUrl(it, file, scope) }
                    }
                    showCameraOverlay = false
                },
                onClose = {
                    // Dismissed without capturing — release the file input.
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = null
                    showCameraOverlay = false
                },
            )
        }

        // --- "Turn On Notifications" prompt before WhatsApp ---
        if (showNotificationPrompt) {
            NotificationSettingsPrompt(
                onOpenSettings = {
                    showNotificationPrompt = false
                    try {
                        notificationSettingsLauncher.launch(notificationSettingsIntent(context))
                    } catch (e: ActivityNotFoundException) {
                        Log.w("WebViewScreen", "No notification settings screen; continuing", e)
                        finishNotificationCheck()
                    }
                },
                onNotNow = {
                    showNotificationPrompt = false
                    finishNotificationCheck()
                },
            )
        }
    }
}

/**
 * JS-bridge delivery (mirrors the iOS `sendImageToWeb`): encodes the captured file
 * to a Base64 data URL off the main thread and hands it to the page via the shared
 * `window.receiveCameraImage(...)` callback. Feature-guarded, so it is a no-op if
 * the web app hasn't defined the handler. Base64 is `A–Z a–z 0–9 + / =`, safe inside
 * the single-quoted JS string with no escaping.
 */
private fun injectFileAsDataUrl(webView: WebView, file: File, scope: CoroutineScope) {
    scope.launch {
        val dataUrl = withContext(Dispatchers.IO) {
            "data:image/jpeg;base64," + Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        }
        val js = "if (typeof window.receiveCameraImage === 'function') " +
                "{ window.receiveCameraImage('$dataUrl'); }"
        webView.evaluateJavascript(js, null)
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureSettings() {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    // Disable native long click behaviors (e.g. text selection, magnifier, context menu)
    // which would otherwise send touchcancel to JavaScript and interrupt press-and-hold gestures.
    isLongClickable = true
    setOnLongClickListener { true }

    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true

        // Fix for Web-based Mic/Camera: Set a standard mobile User Agent
        userAgentString = userAgentString.replace("wv", "") // Remove 'wv' to look like standard Chrome

        // Responsive & Viewport
        useWideViewPort = true
        loadWithOverviewMode = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false

        // Performance
        cacheMode = WebSettings.LOAD_DEFAULT
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        mediaPlaybackRequiresUserGesture = false

        // Security & Access
        allowFileAccess = true
        allowContentAccess = true
    }
}

private fun createWebViewClient(
    onPageStarted: () -> Unit,
    onPageFinished: (WebView?) -> Unit,
    onError: (String) -> Unit,
    onWhatsAppUrl: (String) -> Unit,
): WebViewClient = object : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url?.toString() ?: return false
        return handleUrl(view, url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (view == null || url == null) return false
        return handleUrl(view, url)
    }

    private fun handleUrl(view: WebView, url: String): Boolean {
        // WhatsApp goes through the notification gate first; the screen opens it
        // once the permission flow resolves.
        if (WebViewUrlHandler.isWhatsAppUrl(url)) {
            onWhatsAppUrl(url)
            return true
        }
        if (WebViewUrlHandler.shouldOverrideUrl(url)) {
            return WebViewUrlHandler.handleExternalUrl(view.context, url)
        }
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished(view)
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        // Only trigger error for the main frame to avoid 3rd party resource failures breaking the UI
        if (request?.isForMainFrame == true) {
            val failingUrl = request.url?.toString()
            if (failingUrl != null && WebViewUrlHandler.shouldOverrideUrl(failingUrl)) {
                return
            }
            onError(error?.description?.toString() ?: "Connection Error")
        }
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        // Edge Case: WebView renderer crashed (low memory or engine error)
        // API 26+ handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (detail?.didCrash() == false) {
                view?.reload()
                return true
            }
        }
        view?.reload()
        return true
    }
}

@Composable
private fun SourceItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorOverlay(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(text = stringResource(id = R.string.retry))
            }
        }
    }
}

private fun handleDownload(
    context: Context,
    webView: WebView,
    scope: CoroutineScope,
    url: String,
    contentDisposition: String?,
    mimeType: String?,
) {
    if (url.startsWith("blob:")) {
        val suggestedName = guessFileName(url, contentDisposition, mimeType)
        val js = """
            (function() {
                var xhr = new XMLHttpRequest();
                xhr.open('GET', '$url', true);
                xhr.responseType = 'blob';
                xhr.onload = function() {
                    var reader = new FileReader();
                    reader.onloadend = function() {
                        if (window.AndroidBridge && typeof window.AndroidBridge.processDataUrl === 'function') {
                            window.AndroidBridge.processDataUrl(reader.result, '$suggestedName');
                        }
                    };
                    reader.readAsDataURL(xhr.response);
                };
                xhr.onerror = function() {
                    console.error('Failed to fetch blob URL');
                };
                xhr.send();
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    } else if (url.startsWith("data:")) {
        scope.launch(Dispatchers.IO) {
            val file = saveAndPrepareDataUrl(context, url, contentDisposition, mimeType)
            if (file != null) {
                val effMime = determineMimeType(file, mimeType)
                withContext(Dispatchers.Main) {
                    shareFile(context, file, effMime)
                }
            }
        }
    } else if (url.startsWith("http://") || url.startsWith("https://")) {
        scope.launch(Dispatchers.IO) {
            val file = downloadHttpFile(context, url, contentDisposition, mimeType)
            if (file != null) {
                val effMime = determineMimeType(file, mimeType)
                withContext(Dispatchers.Main) {
                    shareFile(context, file, effMime)
                }
            }
        }
    }
}

private fun saveAndPrepareDataUrl(
    context: Context,
    dataUrl: String,
    contentDisposition: String? = null,
    givenMimeType: String? = null,
    suggestedFileName: String? = null,
): File? {
    try {
        val commaIndex = dataUrl.indexOf(',')
        if (commaIndex == -1) return null

        val header = dataUrl.substring(0, commaIndex)
        val dataPart = dataUrl.substring(commaIndex + 1)
        val isBase64 = header.contains(";base64", ignoreCase = true)

        val parsedMime = header.removePrefix("data:")
            .split(";")
            .firstOrNull()
            ?.trim()
            ?.ifEmpty { null }
            ?: givenMimeType
            ?: "application/octet-stream"

        val bytes = if (isBase64) {
            Base64.decode(dataPart, Base64.DEFAULT)
        } else {
            URLDecoder.decode(dataPart, "UTF-8").toByteArray(Charsets.UTF_8)
        }

        val ext = when {
            parsedMime.contains("pdf", ignoreCase = true) -> ".pdf"
            parsedMime.contains("csv", ignoreCase = true) -> ".csv"
            parsedMime.contains("json", ignoreCase = true) -> ".json"
            parsedMime.contains("excel", ignoreCase = true) || parsedMime.contains("spreadsheet", ignoreCase = true) -> ".xlsx"
            else -> ".bin"
        }

        val filename = suggestedFileName?.ifBlank { null }
            ?: guessFileName(dataUrl, contentDisposition, parsedMime, ext)

        val downloadsDir = File(context.cacheDir, "downloads").apply { mkdirs() }
        val targetFile = File(downloadsDir, filename)
        targetFile.writeBytes(bytes)
        return targetFile
    } catch (e: Exception) {
        Log.e("WebViewScreen", "Error saving Data URL file", e)
        return null
    }
}

private fun downloadHttpFile(
    context: Context,
    urlString: String,
    contentDisposition: String?,
    mimeType: String?,
): File? {
    try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val cookie = CookieManager.getInstance().getCookie(urlString)
        if (!cookie.isNullOrEmpty()) {
            connection.setRequestProperty("Cookie", cookie)
        }
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            Log.e("WebViewScreen", "HTTP download failed with code ${connection.responseCode}")
            return null
        }

        val effMimeType = connection.contentType ?: mimeType ?: "application/octet-stream"
        val cd = connection.getHeaderField("Content-Disposition") ?: contentDisposition
        val filename = guessFileName(urlString, cd, effMimeType)

        val downloadsDir = File(context.cacheDir, "downloads").apply { mkdirs() }
        val targetFile = File(downloadsDir, filename)

        connection.inputStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return targetFile
    } catch (e: Exception) {
        Log.e("WebViewScreen", "Error downloading HTTP file", e)
        return null
    }
}

private fun guessFileName(
    url: String,
    contentDisposition: String?,
    mimeType: String?,
    defaultExt: String = "",
): String {
    var name = URLUtil.guessFileName(url, contentDisposition, mimeType)
    if (name.equals("downloadfile.bin", ignoreCase = true) || name.equals("downloadfile", ignoreCase = true) || name.endsWith(".bin")) {
        val ext = when {
            mimeType?.contains("pdf", ignoreCase = true) == true -> ".pdf"
            mimeType?.contains("csv", ignoreCase = true) == true -> ".csv"
            url.contains(".pdf", ignoreCase = true) -> ".pdf"
            url.contains(".csv", ignoreCase = true) -> ".csv"
            else -> defaultExt.ifEmpty { ".pdf" }
        }
        val prefix = "statement_${System.currentTimeMillis()}"
        name = "$prefix$ext"
    }
    return name
}

private fun determineMimeType(file: File, fallbackMime: String?): String {
    val name = file.name.lowercase()
    return when {
        name.endsWith(".pdf") -> "application/pdf"
        name.endsWith(".csv") -> "text/csv"
        name.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        name.endsWith(".json") -> "application/json"
        !fallbackMime.isNullOrBlank() && fallbackMime != "application/octet-stream" -> fallbackMime
        else -> "application/octet-stream"
    }
}

private fun shareFile(context: Context, file: File, mimeType: String) {
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(shareIntent, "Share Document").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        Log.e("WebViewScreen", "Error opening share sheet for file: ${file.absolutePath}", e)
    }
}
