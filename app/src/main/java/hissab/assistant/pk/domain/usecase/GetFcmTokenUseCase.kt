package hissab.assistant.pk.domain.usecase

import hissab.assistant.pk.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Returns the stored FCM registration token, or null if none has been saved yet.
 */
class GetFcmTokenUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(): Result<String?> =
        runCatching { repository.getToken() }
}
