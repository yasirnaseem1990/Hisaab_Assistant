package hissab.assistant.pk.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import hissab.assistant.pk.domain.model.NotificationPayload
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates and maintains FCM notification channels.
 *
 * The method is idempotent — re-creating an already-registered channel is a no-op on Android O+.
 *
 * Two channels are registered:
 *  - [NotificationPayload.CHANNEL_DEFAULT]  — standard alerts (IMPORTANCE_DEFAULT)
 *  - [NotificationPayload.CHANNEL_PRIORITY] — critical alerts (IMPORTANCE_HIGH)
 *    with Do Not Disturb bypass requested if the user has already granted
 *    ACCESS_NOTIFICATION_POLICY (Settings > Apps > Special app access > Do Not Disturb).
 */
@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannels(listOf(defaultChannel(), priorityChannel()))
    }

    /** Returns true when the user has granted Do Not Disturb policy access to this app. */
    fun canBypassDnd(): Boolean =
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).isNotificationPolicyAccessGranted

    private fun defaultChannel() = NotificationChannel(
        NotificationPayload.CHANNEL_DEFAULT,
        "MyHisaab Notifications",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "General notifications from MyHisaab"
    }

    private fun priorityChannel() = NotificationChannel(
        NotificationPayload.CHANNEL_PRIORITY,
        "MyHisaab Priority Alerts",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Urgent alerts that may override Do Not Disturb"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && canBypassDnd()) {
            setBypassDnd(true)
        }
    }
}
