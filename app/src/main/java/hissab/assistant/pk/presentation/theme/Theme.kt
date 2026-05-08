package hissab.assistant.pk.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Brand-driven color scheme. `primary` is the same orange used by the
 * "Next" / "Register Now" buttons in the web app, so the native top app bar
 * and status bar match the WebView content out of the box.
 */
private val LightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = OnBrandOrange,
    primaryContainer = BrandOrange,
    onPrimaryContainer = OnBrandOrange,
    secondary = BrandOrangeDark,
    onSecondary = OnBrandOrange
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandOrange,
    onPrimary = OnBrandOrange,
    primaryContainer = BrandOrangeDark,
    onPrimaryContainer = OnBrandOrange,
    secondary = BrandOrangeDark,
    onSecondary = OnBrandOrange
)

@Composable
fun HisaabAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Default to the brand palette. Callers can opt back into Android 12+
    // wallpaper-based dynamic color by passing `dynamicColor = true`.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Match the status bar to the top app bar (= brand orange).
            window.statusBarColor = colorScheme.primary.toArgb()
            // Status bar text/icons should be light on the orange background.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
