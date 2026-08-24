package hissab.assistant.pk.presentation.notification

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationPermissionManagerTest {

    private lateinit var activity: Activity
    private lateinit var launcher: ActivityResultLauncher<String>

    @Before
    fun setUp() {
        activity = mockk(relaxed = true)
        launcher = mockk(relaxed = true)
    }

    // ── isGranted – below API 33 ──────────────────────────────────────────────

    @Test
    fun `isGranted returns true when SDK is below TIRAMISU`() {
        val manager = buildManager(sdk = Build.VERSION_CODES.S, permissionResult = PackageManager.PERMISSION_DENIED)

        assertTrue(manager.isGranted())
    }

    // ── isGranted – API 33+ ───────────────────────────────────────────────────

    @Test
    fun `isGranted returns true when permission is granted on API 33`() {
        val manager = buildManager(sdk = Build.VERSION_CODES.TIRAMISU, permissionResult = PackageManager.PERMISSION_GRANTED)

        assertTrue(manager.isGranted())
    }

    @Test
    fun `isGranted returns false when permission is denied on API 33`() {
        val manager = buildManager(sdk = Build.VERSION_CODES.TIRAMISU, permissionResult = PackageManager.PERMISSION_DENIED)

        assertFalse(manager.isGranted())
    }

    @Test
    fun `isGranted returns false when permission is denied on API 34`() {
        val manager = buildManager(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, permissionResult = PackageManager.PERMISSION_DENIED)

        assertFalse(manager.isGranted())
    }

    // ── shouldShowRationale ───────────────────────────────────────────────────

    @Test
    fun `shouldShowRationale returns false when SDK below 33`() {
        val manager = buildManager(sdk = Build.VERSION_CODES.S)

        assertFalse(manager.shouldShowRationale())
    }

    @Test
    fun `shouldShowRationale returns activity value when SDK is 33`() {
        every { activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) } returns true
        val manager = buildManager(sdk = Build.VERSION_CODES.TIRAMISU)

        assertTrue(manager.shouldShowRationale())
    }

    @Test
    fun `shouldShowRationale returns false when activity returns false on API 33`() {
        every { activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) } returns false
        val manager = buildManager(sdk = Build.VERSION_CODES.TIRAMISU)

        assertFalse(manager.shouldShowRationale())
    }

    // ── requestPermission ─────────────────────────────────────────────────────

    @Test
    fun `requestPermission does nothing on API below 33`() {
        val manager = buildManager(sdk = Build.VERSION_CODES.S)

        manager.requestPermission()

        verify(exactly = 0) { launcher.launch(any()) }
    }

    @Test
    fun `requestPermission launches POST_NOTIFICATIONS on API 33`() {
        val manager = buildManager(sdk = Build.VERSION_CODES.TIRAMISU)

        manager.requestPermission()

        verify(exactly = 1) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    @Test
    fun `requestPermission with null launcher does not crash on API 33`() {
        val manager = NotificationPermissionManager(
            activity = activity,
            launcher = null,
            sdkVersionProvider = { Build.VERSION_CODES.TIRAMISU },
        )

        manager.requestPermission() // must not throw
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildManager(
        sdk: Int,
        permissionResult: Int = PackageManager.PERMISSION_DENIED,
    ) = NotificationPermissionManager(
        activity = activity,
        launcher = launcher,
        sdkVersionProvider = { sdk },
        permissionChecker = { permissionResult },
    )
}
