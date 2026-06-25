package hissab.assistant.pk.data.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import hissab.assistant.pk.domain.model.NotificationPayload
import hissab.assistant.pk.domain.usecase.HandleNotificationUseCase
import hissab.assistant.pk.domain.usecase.SaveFcmTokenUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Entry point for all Firebase Cloud Messaging events.
 *
 * Lifecycle notes:
 * - The service is started by the FCM SDK; it runs even when the app is killed.
 * - [onMessageReceived] is only called when the message has NO "notification"
 *   key **or** when the app is in the foreground. For guaranteed delivery in
 *   all states, the backend should send data-only messages (omit the FCM
 *   "notification" block and put everything inside "data").
 * - A [SupervisorJob]-backed scope is used so that a failure in one child
 *   coroutine does not cancel the scope itself; the scope is cancelled in
 *   [onDestroy] to prevent leaks.
 */
@AndroidEntryPoint
class HisaabFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var handleNotification: HandleNotificationUseCase
    @Inject lateinit var saveFcmToken: SaveFcmTokenUseCase

    private val serviceScope = CoroutineScope(SupervisorJob())

    override fun onNewToken(token: String) {
        serviceScope.launch {
            saveFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = message.toPayload()
        handleNotification(payload)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun RemoteMessage.toPayload(): NotificationPayload {
        val title = notification?.title ?: data["title"].orEmpty()
        val body = notification?.body ?: data["body"].orEmpty()
        val imageUrl = notification?.imageUrl?.toString() ?: data["image_url"]
        val deepLink = data[NotificationPayload.KEY_DEEP_LINK]
        val channelId = data[NotificationPayload.KEY_CHANNEL]
            ?.takeIf { it.isNotBlank() }
            ?: NotificationPayload.CHANNEL_DEFAULT

        return NotificationPayload(
            id = messageId ?: UUID.randomUUID().toString(),
            title = title,
            body = body,
            imageUrl = imageUrl,
            deepLink = deepLink,
            data = data,
            channelId = channelId,
        )
    }
}
