package hissab.assistant.pk.domain.usecase

import hissab.assistant.pk.domain.repository.NotificationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetFcmTokenUseCaseTest {

    private lateinit var repository: NotificationRepository
    private lateinit var useCase: GetFcmTokenUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetFcmTokenUseCase(repository)
    }

    @Test
    fun `invoke returns stored token`() = runTest {
        coEvery { repository.getToken() } returns "stored-token"

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals("stored-token", result.getOrNull())
        coVerify(exactly = 1) { repository.getToken() }
    }

    @Test
    fun `invoke returns null when no token stored`() = runTest {
        coEvery { repository.getToken() } returns null

        val result = useCase()

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `invoke wraps repository exception in Result failure`() = runTest {
        coEvery { repository.getToken() } throws RuntimeException("read error")

        val result = useCase()

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }
}
