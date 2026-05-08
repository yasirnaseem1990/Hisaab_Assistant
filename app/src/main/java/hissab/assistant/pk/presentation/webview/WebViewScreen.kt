package hissab.assistant.pk.presentation.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hissab.assistant.pk.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

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
    val context: Context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val scope: CoroutineScope = rememberCoroutineScope()

    // Reference to WebView for back-nav and lifecycle management
    var webViewRef: WebView? by remember { mutableStateOf<WebView?>(null) }
    var canGoBack: Boolean by remember { mutableStateOf(false) }

    // --- State for Image Picking ---
    var filePathCallback: ValueCallback<Array<Uri>>? by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var showImageSourceSheet: Boolean by remember { mutableStateOf(false) }
    val sheetState: SheetState = rememberModalBottomSheetState()

    // To store the temporary URI for camera capture
    var tempCameraUri: Uri? by remember { mutableStateOf<Uri?>(null) }

    // 1. Gallery Launcher
    val galleryLauncher: ManagedActivityResultLauncher<String, List<@JvmSuppressWildcards Uri>> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            filePathCallback?.onReceiveValue(if (uris.isNotEmpty()) uris.toTypedArray() else null)
            filePathCallback = null
        }
    )

    // 2. Camera Launcher
    val cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempCameraUri != null) {
                filePathCallback?.onReceiveValue(arrayOf(tempCameraUri!!))
            } else {
                filePathCallback?.onReceiveValue(null)
            }
            filePathCallback = null
            tempCameraUri = null
        }
    )

    // 2. Permission Request (Camera/Mic)
    var pendingPermissionRequest: PermissionRequest? by remember { mutableStateOf<PermissionRequest?>(null) }
    var isManualCameraRequest: Boolean by remember { mutableStateOf(false) }

    val permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { permissions ->
                val allGranted: Boolean = permissions.values.all { it }

                if (isManualCameraRequest) {
                    if (allGranted) {
                        val tempFile = createTempImageFile(context)
                        tempCameraUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )
                        cameraLauncher.launch(tempCameraUri!!)
                    } else {
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = null
                    }
                    isManualCameraRequest = false
                } else {
                    if (allGranted) {
                        pendingPermissionRequest?.grant(pendingPermissionRequest?.resources)
                    } else {
                        pendingPermissionRequest?.deny()
                    }
                    pendingPermissionRequest = null
                }
            }
        )

    // Handle System Back Button
    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
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

    Box(
        modifier = modifier
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
                            onError = { msg -> viewModel.onError(msg) }
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

                        webViewRef = this
                        loadUrl(uiState.url)
                    }
                },
                update = { view ->
                    // Handle dynamic URL updates if necessary
                    if (view.url != uiState.url && uiState.url.isNotBlank()) {
                        // view.loadUrl(uiState.url) // Only if you want to drive URL from ViewModel
                    }
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
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showImageSourceSheet = false

                                val hasCameraPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasCameraPermission) {
                                    val tempFile = createTempImageFile(context)
                                    tempCameraUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        tempFile
                                    )
                                    cameraLauncher.launch(tempCameraUri!!)
                                } else {
                                    isManualCameraRequest = true
                                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                }
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
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureSettings() {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true

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
): WebViewClient = object : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        // Keep all navigation within this WebView
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

private fun createTempImageFile(context: Context): File {
    val directory = File(context.externalCacheDir, "Images")
    if (!directory.exists()) directory.mkdirs()
    return File.createTempFile("CAPTURED_", ".jpg", directory)
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
