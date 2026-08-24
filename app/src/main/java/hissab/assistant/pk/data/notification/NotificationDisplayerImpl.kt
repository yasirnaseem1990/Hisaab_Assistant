package hissab.assistant.pk.data.notification

import android.app.PendingIntent
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import hissab.assistant.pk.R
import hissab.assistant.pk.domain.model.NotificationPayload
import hissab.assistant.pk.domain.repository.NotificationDisplayer
import hissab.assistant.pk.presentation.MainActivity
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and posts a local notification from a [NotificationPayload].
 *
 * - Uses [NotificationCompat] so the same code works from API 24 to current.
 * - Notification IDs auto-increment so rapid messages don't overwrite each other.
 * - Tap opens [MainActivity] with FLAG_ACTIVITY_SINGLE_TOP to avoid back-stack duplication.
 * - PRIORITY_HIGH + DEFAULT_ALL ensures heads-up + sound on pre-O devices;
 *   on O+ the channel importance governs the same behaviour.
 */
@Singleton
class NotificationDisplayerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationDisplayer {

    private val idCounter = AtomicInteger(0)

    override fun show(payload: NotificationPayload) {
        val largeBitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        val notification = NotificationCompat.Builder(context, payload.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.brand_orange))
            .setLargeIcon(largeBitmap)
            .setContentTitle(payload.title.ifBlank { null })
            .setContentText(payload.body.ifBlank { null })
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setContentIntent(buildTapIntent(payload))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context)
                .notify(idCounter.incrementAndGet(), notification)
        }
    }

    private fun buildTapIntent(payload: NotificationPayload): PendingIntent {
        val intent: Intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            payload.deepLink?.let { putExtra(EXTRA_DEEP_LINK, it) }
            payload.data.forEach { (k, v) -> putExtra(k, v) }
        }
        val flags: Int =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return getActivity(
            context,
            idCounter.get(),
            intent,
            flags
        )
    }

    companion object {
        const val EXTRA_DEEP_LINK = "extra_deep_link"
    }
}
