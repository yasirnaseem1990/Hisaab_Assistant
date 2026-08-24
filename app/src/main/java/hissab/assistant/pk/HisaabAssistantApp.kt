package hissab.assistant.pk

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import hissab.assistant.pk.data.notification.NotificationChannelManager
import javax.inject.Inject

/**
 * Application class — Hilt entry point.
 *
 * Notification channels are registered here (in [onCreate]) rather than lazily,
 * because a channel must exist before the first notification can be posted.
 * Re-creating an already-registered channel is a no-op on Android O+.
 */
@HiltAndroidApp
class HisaabAssistantApp : Application() {

    @Inject lateinit var notificationChannelManager: NotificationChannelManager

    override fun onCreate() {
        super.onCreate()
        notificationChannelManager.createChannels()
    }
}
