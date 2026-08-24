package hissab.assistant.pk.domain.model

/**
 * Immutable value object representing a push notification received from FCM.
 *
 * Both "notification" and "data" FCM message types are normalised here so the
 * rest of the domain never has to care which wire format was used.
 */
data class NotificationPayload(
    val id: String,
    val title: String,
    val body: String,
    val imageUrl: String? = null,
    val deepLink: String? = null,
    val data: Map<String, String> = emptyMap(),
    val channelId: String = CHANNEL_DEFAULT,
) {
    companion object {
        const val CHANNEL_DEFAULT = "hisaab_default"
        const val CHANNEL_PRIORITY = "hisaab_priority"

        /** Key used in the FCM data payload to carry a deep-link destination. */
        const val KEY_DEEP_LINK = "deep_link"

        /** Key used to override the notification channel. */
        const val KEY_CHANNEL = "channel_id"
    }
}
