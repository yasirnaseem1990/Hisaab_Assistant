package hissab.assistant.pk.domain.usecase

import hissab.assistant.pk.domain.model.NotificationPayload
import hissab.assistant.pk.domain.repository.NotificationDisplayer
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HandleNotificationUseCaseTest {

    private lateinit var displayer: NotificationDisplayer
    private lateinit var useCase: HandleNotificationUseCase

    @Before
    fun setUp() {
        displayer = mockk()
        useCase = HandleNotificationUseCase(displayer)
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `invoke shows notification when title and body are present`() {
        justRun { displayer.show(any()) }
        val payload = validPayload()

        val result = useCase(payload)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { displayer.show(payload) }
    }

    @Test
    fun `invoke succeeds when only title is present`() {
        justRun { displayer.show(any()) }
        val payload = validPayload(title = "Hello", body = "")

        val result = useCase(payload)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { displayer.show(payload) }
    }

    @Test
    fun `invoke succeeds when only body is present`() {
        justRun { displayer.show(any()) }
        val payload = validPayload(title = "", body = "Message body")

        val result = useCase(payload)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke passes payload with data map to displayer`() {
        justRun { displayer.show(any()) }
        val payload = validPayload(data = mapOf("key" to "value"))

        useCase(payload)

        verify { displayer.show(match { it.data["key"] == "value" }) }
    }

    @Test
    fun `invoke passes payload with deepLink to displayer`() {
        justRun { displayer.show(any()) }
        val payload = validPayload(deepLink = "hisaab://home")

        useCase(payload)

        verify { displayer.show(match { it.deepLink == "hisaab://home" }) }
    }

    // ── Empty payload guard ───────────────────────────────────────────────────

    @Test
    fun `invoke returns failure when title and body are both blank`() {
        val payload = validPayload(title = "", body = "")

        val result = useCase(payload)

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        verify(exactly = 0) { displayer.show(any()) }
    }

    @Test
    fun `invoke returns failure when title and body are whitespace only`() {
        val payload = validPayload(title = "   ", body = "\t")

        val result = useCase(payload)

        assertFalse(result.isSuccess)
    }

    // ── Displayer throws ──────────────────────────────────────────────────────

    @Test
    fun `invoke wraps displayer exception in Result failure`() {
        every { displayer.show(any()) } throws RuntimeException("system error")
        val payload = validPayload()

        val result = useCase(payload)

        assertFalse(result.isSuccess)
        assertEquals("system error", result.exceptionOrNull()?.message)
    }

    // ── Channel routing ───────────────────────────────────────────────────────

    @Test
    fun `invoke forwards priority channel id`() {
        justRun { displayer.show(any()) }
        val payload = validPayload(channelId = NotificationPayload.CHANNEL_PRIORITY)

        useCase(payload)

        verify { displayer.show(match { it.channelId == NotificationPayload.CHANNEL_PRIORITY }) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun validPayload(
        title: String = "Test Title",
        body: String = "Test body",
        channelId: String = NotificationPayload.CHANNEL_DEFAULT,
        data: Map<String, String> = emptyMap(),
        deepLink: String? = null,
    ) = NotificationPayload(
        id = "test-id",
        title = title,
        body = body,
        channelId = channelId,
        data = data,
        deepLink = deepLink,
    )
}
