package hissab.assistant.pk.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import hissab.assistant.pk.presentation.theme.HisaabAssistantTheme
import hissab.assistant.pk.presentation.webview.WebViewScreen

/**
 * Single Activity that hosts the Compose UI tree. It displays the [WebViewScreen]
 * filling the entire screen.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HisaabAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HisaabAssistantApp()
                }
            }
        }
    }
}

@Composable
private fun HisaabAssistantApp() {
    WebViewScreen(
        modifier = Modifier.fillMaxSize()
    )
}
