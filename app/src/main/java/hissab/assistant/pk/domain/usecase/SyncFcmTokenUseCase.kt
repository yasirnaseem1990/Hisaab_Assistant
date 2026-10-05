package hissab.assistant.pk.domain.usecase

import hissab.assistant.pk.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Fetches a fresh FCM token from the provider and updates storage.
 */
class SyncFcmTokenUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(): Result<String> =
        runCatching { repository.fetchToken() }
}
