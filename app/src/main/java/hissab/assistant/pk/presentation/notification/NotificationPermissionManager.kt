package hissab.assistant.pk.presentation.notification

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Handles the POST_NOTIFICATIONS runtime permission introduced in Android 13 (API 33).
 *
 * On API < 33 there is no runtime permission — notifications are always allowed
 * unless the user disables them in system settings.
 *
 * [sdkVersionProvider] and [permissionChecker] are injectable so the class can be
 * unit-tested without the Android runtime or Robolectric.
 */
class NotificationPermissionManager(
    private val activity: Activity,
    private val launcher: ActivityResultLauncher<String>?,
    private val sdkVersionProvider: () -> Int = { Build.VERSION.SDK_INT },
    private val permissionChecker: (String) -> Int = { permission ->
        ContextCompat.checkSelfPermission(activity, permission)
    },
) {
    fun isGranted(): Boolean {
        if (sdkVersionProvider() < Build.VERSION_CODES.TIRAMISU) return true
        return permissionChecker(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun shouldShowRationale(): Boolean {
        if (sdkVersionProvider() < Build.VERSION_CODES.TIRAMISU) return false
        return activity.shouldShowRequestPermissionRationale(
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    fun requestPermission() {
        if (sdkVersionProvider() < Build.VERSION_CODES.TIRAMISU) return
        launcher?.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
