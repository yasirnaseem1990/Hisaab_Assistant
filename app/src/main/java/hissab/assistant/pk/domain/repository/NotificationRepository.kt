package hissab.assistant.pk.domain.repository

/**
 * Contract for persisting and retrieving the FCM registration token.
 *
 * The token must be sent to the backend so targeted pushes can be delivered.
 * On each fresh install Firebase issues a new token via [onNewToken]; this
 * repository keeps the last-known value so it can be re-uploaded if the
 * backend loses it.
 */
interface NotificationRepository {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    suspend fun deleteToken()
}
