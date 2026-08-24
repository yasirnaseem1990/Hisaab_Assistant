package hissab.assistant.pk.domain.usecase

import hissab.assistant.pk.domain.repository.NotificationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncFcmTokenUseCaseTest {

    private lateinit var repository: NotificationRepository
    private lateinit var useCase: SyncFcmTokenUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = SyncFcmTokenUseCase(repository)
    }

    @Test
    fun `invoke returns fetched token on success`() = runTest {
        coEvery { repository.fetchToken() } returns "fresh-token"

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals("fresh-token", result.getOrNull())
        coVerify(exactly = 1) { repository.fetchToken() }
    }

    @Test
    fun `invoke wraps provider failure in Result failure`() = runTest {
        // e.g. missing Google Play services or no network on first launch.
        coEvery { repository.fetchToken() } throws IllegalStateException("SERVICE_NOT_AVAILABLE")

        val result = useCase()

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
