package hissab.assistant.pk.data.notification

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import hissab.assistant.pk.di.IoDispatcher
import hissab.assistant.pk.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * SharedPreferences-backed store for the FCM registration token.
 *
 * SharedPreferences is chosen over DataStore here because the write is tiny
 * and happens on a background thread managed by the FCM service; adding a
 * Preferences DataStore dependency just to store one string is over-engineering.
 *
 * An in-memory [MutableStateFlow] mirrors the persisted value so that
 * consumers (e.g. the WebView token injector) react to mid-session token
 * rotations without polling disk. The flow is lazily seeded from disk on
 * first collection to keep construction main-safe.
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val prefs: SharedPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NotificationRepository {

    private val tokenFlow = MutableStateFlow<String?>(null)
    private val seededFromDisk = AtomicBoolean(false)

    override suspend fun saveToken(token: String) {
        withContext(ioDispatcher) {
            prefs.edit { putString(KEY_FCM_TOKEN, token) }
        }
        seededFromDisk.set(true) // In-memory value is now authoritative.
        tokenFlow.value = token
    }

    override suspend fun getToken(): String? = withContext(ioDispatcher) {
        prefs.getString(KEY_FCM_TOKEN, null)
    }

    override suspend fun deleteToken() {
        withContext(ioDispatcher) {
            prefs.edit { remove(KEY_FCM_TOKEN) }
        }
        seededFromDisk.set(true)
        tokenFlow.value = null
    }

    override fun observeToken(): Flow<String?> = tokenFlow.onStart {
        if (seededFromDisk.compareAndSet(false, true)) {
            tokenFlow.value = getToken()
        }
    }

    /**
     * Asks the FCM SDK for the current registration token. This is the source
     * of truth: on a fresh install the cached value is null until FCM issues
     * one, and `onNewToken` alone is not guaranteed to have fired before the
     * UI needs the token. Persisting via [saveToken] also propagates the value
     * to [observeToken] collectors.
     */
    override suspend fun fetchToken(): String {
        val token = suspendCancellableCoroutine<String> { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("FCM token fetch failed.")
                    )
                }
            }
        }
        saveToken(token)
        return token
    }

    companion object {
        const val KEY_FCM_TOKEN = "fcm_token"
        const val PREFS_NAME = "hisaab_notification_prefs"
    }
}
