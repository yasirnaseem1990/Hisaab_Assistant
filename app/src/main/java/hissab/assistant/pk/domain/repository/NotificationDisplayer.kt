package hissab.assistant.pk.domain.repository

import hissab.assistant.pk.domain.model.NotificationPayload

/**
 * Domain-layer contract for showing a local notification to the user.
 *
 * Keeping this in the domain means use cases can depend on it without
 * importing any Android framework types.
 */
interface NotificationDisplayer {
    fun show(payload: NotificationPayload)
}
