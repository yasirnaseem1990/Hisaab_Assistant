package hissab.assistant.pk.domain.repository

import kotlinx.coroutines.flow.Flow

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

    /**
     * Emits the last-known token immediately (or null if none) and then every
     * subsequent change — including mid-session rotations delivered through
     * the FCM service's `onNewToken`.
     */
    fun observeToken(): Flow<String?>

    /**
     * Fetches a fresh registration token from the push provider, persists it,
     * and returns it. Throws if the provider is unavailable (e.g. missing
     * Google Play services); callers wrap this in a [Result].
     */
    suspend fun fetchToken(): String
}
