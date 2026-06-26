package hissab.assistant.pk.data.notification

import android.content.SharedPreferences
import androidx.core.content.edit
import hissab.assistant.pk.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed store for the FCM registration token.
 *
 * SharedPreferences is chosen over DataStore here because the write is tiny
 * and happens on a background thread managed by the FCM service; adding a
 * Preferences DataStore dependency just to store one string is over-engineering.
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val prefs: SharedPreferences,
) : NotificationRepository {

    override suspend fun saveToken(token: String) {
        prefs.edit { putString(KEY_FCM_TOKEN, token) }
    }

    override suspend fun getToken(): String? =
        prefs.getString(KEY_FCM_TOKEN, null)

    override suspend fun deleteToken() {
        prefs.edit { remove(KEY_FCM_TOKEN) }
    }

    companion object {
        const val KEY_FCM_TOKEN = "fcm_token"
        const val PREFS_NAME = "hisaab_notification_prefs"
    }
}
