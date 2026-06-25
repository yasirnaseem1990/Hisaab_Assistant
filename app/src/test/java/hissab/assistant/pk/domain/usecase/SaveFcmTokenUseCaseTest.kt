package hissab.assistant.pk.domain.usecase

import hissab.assistant.pk.domain.repository.NotificationRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveFcmTokenUseCaseTest {

    private lateinit var repository: NotificationRepository
    private lateinit var useCase: SaveFcmTokenUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = SaveFcmTokenUseCase(repository)
    }

    @Test
    fun `invoke saves valid token`() = runTest {
        coJustRun { repository.saveToken(any()) }

        val result = useCase("valid-fcm-token-abc123")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.saveToken("valid-fcm-token-abc123") }
    }

    @Test
    fun `invoke returns failure for blank token`() = runTest {
        val result = useCase("")

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.saveToken(any()) }
    }

    @Test
    fun `invoke returns failure for whitespace-only token`() = runTest {
        val result = useCase("   ")

        assertFalse(result.isSuccess)
        coVerify(exactly = 0) { repository.saveToken(any()) }
    }

    @Test
    fun `invoke wraps repository exception in Result failure`() = runTest {
        io.mockk.coEvery { repository.saveToken(any()) } throws RuntimeException("DB error")

        val result = useCase("some-token")

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `invoke saves long token successfully`() = runTest {
        coJustRun { repository.saveToken(any()) }
        val longToken = "a".repeat(500)

        val result = useCase(longToken)

        assertTrue(result.isSuccess)
        coVerify { repository.saveToken(longToken) }
    }
}
