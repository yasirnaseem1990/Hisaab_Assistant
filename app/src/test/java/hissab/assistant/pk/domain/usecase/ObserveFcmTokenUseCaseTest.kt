package hissab.assistant.pk.domain.usecase

import hissab.assistant.pk.domain.repository.NotificationRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveFcmTokenUseCaseTest {

    @Test
    fun `invoke relays repository emissions including rotation`() = runTest {
        val repository: NotificationRepository = mockk()
        // null (nothing cached yet) -> first token -> rotated token
        every { repository.observeToken() } returns flowOf(null, "token-1", "token-2")

        val emissions = ObserveFcmTokenUseCase(repository)().toList()

        assertEquals(listOf(null, "token-1", "token-2"), emissions)
    }
}
