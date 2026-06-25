package hissab.assistant.pk.domain.usecase

import hissab.assistant.pk.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Persists a new FCM registration token.
 *
 * Called whenever Firebase rotates the device token. The caller (FCM service)
 * should also upload the token to the app backend via this use case's result.
 */
class SaveFcmTokenUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(token: String): Result<Unit> {
        if (token.isBlank()) {
            return Result.failure(IllegalArgumentException("FCM token must not be blank."))
        }
        return runCatching { repository.saveToken(token) }
    }
}
