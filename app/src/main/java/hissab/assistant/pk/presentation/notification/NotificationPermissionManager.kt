package hissab.assistant.pk.presentation.notification

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Handles the POST_NOTIFICATIONS runtime permission introduced in Android 13 (API 33).
 *
 * On API < 33 there is no runtime permission — notifications are always allowed
 * unless the user disables them in system settings.
 *
 * [sdkVersionProvider], [permissionChecker] and [notificationsEnabledChecker] are
 * injectable so the class can be unit-tested without the Android runtime or Robolectric.
 */
class NotificationPermissionManager(
    private val activity: Activity,
    private val launcher: ActivityResultLauncher<String>?,
    private val sdkVersionProvider: () -> Int = { Build.VERSION.SDK_INT },
    private val permissionChecker: (String) -> Int = { permission ->
        ContextCompat.checkSelfPermission(activity, permission)
    },
    private val notificationsEnabledChecker: () -> Boolean = {
        NotificationManagerCompat.from(activity).areNotificationsEnabled()
    },
) {
    fun isGranted(): Boolean {
        if (sdkVersionProvider() < Build.VERSION_CODES.TIRAMISU) return true
        return permissionChecker(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Whether notifications will actually be shown: covers the runtime permission
     * on API 33+ and the app-level toggle in system settings on every API level.
     */
    fun areNotificationsEnabled(): Boolean = notificationsEnabledChecker()

    /**
     * True when there is a runtime permission left to ask for (API 33+, not granted).
     * If the user has permanently denied it, the system answers "denied" without
     * showing its dialog.
     */
    fun canRequestPermission(): Boolean =
        sdkVersionProvider() >= Build.VERSION_CODES.TIRAMISU && !isGranted()

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
