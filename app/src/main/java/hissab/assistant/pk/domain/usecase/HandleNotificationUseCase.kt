package hissab.assistant.pk.domain.usecase

import hissab.assistant.pk.domain.model.NotificationPayload
import hissab.assistant.pk.domain.repository.NotificationDisplayer
import javax.inject.Inject

/**
 * Processes an incoming FCM message and delegates display to [NotificationDisplayer].
 *
 * Validates the payload before forwarding: a message with no title and no body
 * is silently dropped (FCM sometimes sends empty keep-alive pings).
 */
class HandleNotificationUseCase @Inject constructor(
    private val displayer: NotificationDisplayer,
) {
    operator fun invoke(payload: NotificationPayload): Result<Unit> {
        if (payload.title.isBlank() && payload.body.isBlank()) {
            return Result.failure(IllegalArgumentException("Empty notification payload — ignored."))
        }
        return runCatching { displayer.show(payload) }
    }
}
