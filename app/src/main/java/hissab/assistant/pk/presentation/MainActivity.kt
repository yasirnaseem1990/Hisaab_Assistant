package hissab.assistant.pk.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import hissab.assistant.pk.presentation.notification.NotificationPermissionManager
import hissab.assistant.pk.presentation.theme.HisaabAssistantTheme
import hissab.assistant.pk.presentation.webview.WebViewScreen

/**
 * Single Activity that hosts the Compose UI tree.
 *
 * Also handles the POST_NOTIFICATIONS runtime permission on Android 13+.
 * The permission is requested once at startup; the user's decision is
 * respected — we don't re-ask on every launch.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    private val notificationPermissionManager by lazy {
        NotificationPermissionManager(this, notificationPermissionLauncher)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleSharedImage(intent)
        requestNotificationPermissionIfNeeded()
        setContent {
            HisaabAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    HisaabAssistantApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedImage(intent)
    }

    private fun handleSharedImage(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND) return
        if (!intent.type.orEmpty().startsWith("image/")) return

        val imageUri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
        Log.d(TAG, "Shared image URI: $imageUri")
        // TODO: pass imageUri to ViewModel / WebView bridge
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (!notificationPermissionManager.isGranted()) {
            notificationPermissionManager.requestPermission()
        }
    }
}

@Composable
private fun HisaabAssistantApp() {
    WebViewScreen(modifier = Modifier.fillMaxSize())
}
