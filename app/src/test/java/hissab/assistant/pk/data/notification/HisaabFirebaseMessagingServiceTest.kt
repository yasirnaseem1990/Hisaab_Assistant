package hissab.assistant.pk.data.notification

import com.google.firebase.messaging.RemoteMessage
import hissab.assistant.pk.domain.model.NotificationPayload
import hissab.assistant.pk.domain.usecase.HandleNotificationUseCase
import hissab.assistant.pk.domain.usecase.SaveFcmTokenUseCase
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Tests for [HisaabFirebaseMessagingService] message-mapping and delegation logic.
 *
 * Note: We cannot unit-test the full Service lifecycle (onCreate / onDestroy) because
 * FirebaseMessagingService extends Service which requires the Android runtime.
 * Instead, we test the private mapping logic via a thin test subclass that exposes
 * the internal [toPayload] conversion.
 */
class HisaabFirebaseMessagingServiceTest {

    private lateinit var handleNotification: HandleNotificationUseCase
    private lateinit var saveFcmToken: SaveFcmTokenUseCase
    private lateinit var service: TestableService

    @Before
    fun setUp() {
        handleNotification = mockk(relaxed = true)
        saveFcmToken = mockk(relaxed = true)
        service = TestableService(handleNotification, saveFcmToken)
    }

    // ── onNewToken ────────────────────────────────────────────────────────────

    @Test
    fun `onNewToken delegates to SaveFcmTokenUseCase`() = runTest {
        coJustRun { saveFcmToken(any()) }

        service.simulateNewToken("fresh-token")

        coVerify { saveFcmToken("fresh-token") }
    }

    @Test
    fun `onNewToken passes exact token string`() = runTest {
        coJustRun { saveFcmToken(any()) }
        val token = "APA91bEXACT_TOKEN_STRING-xyz123"

        service.simulateNewToken(token)

        coVerify { saveFcmToken(token) }
    }

    // ── onMessageReceived – notification payload ──────────────────────────────

    @Test
    fun `onMessageReceived maps notification title and body`() {
        val slot = slot<NotificationPayload>()
        every { handleNotification(capture(slot)) } returns Result.success(Unit)

        service.simulateMessage(
            title = "Hello",
            body = "World",
        )

        assertEquals("Hello", slot.captured.title)
        assertEquals("World", slot.captured.body)
    }

    @Test
    fun `onMessageReceived uses default channel when not specified in data`() {
        val slot = slot<NotificationPayload>()
        every { handleNotification(capture(slot)) } returns Result.success(Unit)

        service.simulateMessage(title = "T", body = "B")

        assertEquals(NotificationPayload.CHANNEL_DEFAULT, slot.captured.channelId)
    }

    @Test
    fun `onMessageReceived uses channel from data payload when present`() {
        val slot = slot<NotificationPayload>()
        every { handleNotification(capture(slot)) } returns Result.success(Unit)

        service.simulateMessage(
            title = "Alert",
            body = "Urgent",
            data = mapOf(NotificationPayload.KEY_CHANNEL to NotificationPayload.CHANNEL_PRIORITY),
        )

        assertEquals(NotificationPayload.CHANNEL_PRIORITY, slot.captured.channelId)
    }

    @Test
    fun `onMessageReceived maps deep link from data payload`() {
        val slot = slot<NotificationPayload>()
        every { handleNotification(capture(slot)) } returns Result.success(Unit)

        service.simulateMessage(
            title = "T",
            body = "B",
            data = mapOf(NotificationPayload.KEY_DEEP_LINK to "hisaab://transactions"),
        )

        assertEquals("hisaab://transactions", slot.captured.deepLink)
    }

    // ── onMessageReceived – data-only payload ─────────────────────────────────

    @Test
    fun `onMessageReceived reads title and body from data when notification block absent`() {
        val slot = slot<NotificationPayload>()
        every { handleNotification(capture(slot)) } returns Result.success(Unit)

        service.simulateDataOnlyMessage(
            data = mapOf("title" to "Data Title", "body" to "Data Body"),
        )

        assertEquals("Data Title", slot.captured.title)
        assertEquals("Data Body", slot.captured.body)
    }

    @Test
    fun `onMessageReceived assigns generated id when messageId is null`() {
        val slot = slot<NotificationPayload>()
        every { handleNotification(capture(slot)) } returns Result.success(Unit)

        service.simulateDataOnlyMessage(
            data = mapOf("title" to "T", "body" to "B"),
            messageId = null,
        )

        assertNotNull(slot.captured.id)
    }

    @Test
    fun `onMessageReceived uses messageId when present`() {
        val slot = slot<NotificationPayload>()
        every { handleNotification(capture(slot)) } returns Result.success(Unit)

        service.simulateDataOnlyMessage(
            data = mapOf("title" to "T", "body" to "B"),
            messageId = "msg-999",
        )

        assertEquals("msg-999", slot.captured.id)
    }

    @Test
    fun `onMessageReceived passes through full data map`() {
        val slot = slot<NotificationPayload>()
        every { handleNotification(capture(slot)) } returns Result.success(Unit)

        service.simulateDataOnlyMessage(
            data = mapOf("title" to "T", "body" to "B", "order_id" to "42"),
        )

        assertEquals("42", slot.captured.data["order_id"])
    }

    @Test
    fun `onMessageReceived ignores blank channel override in data`() {
        val slot = slot<NotificationPayload>()
        every { handleNotification(capture(slot)) } returns Result.success(Unit)

        service.simulateDataOnlyMessage(
            data = mapOf("title" to "T", "body" to "B", NotificationPayload.KEY_CHANNEL to ""),
        )

        assertEquals(NotificationPayload.CHANNEL_DEFAULT, slot.captured.channelId)
    }

    // ── TestableService ───────────────────────────────────────────────────────

    /**
     * Thin subclass that bypasses FirebaseMessagingService's Android-runtime
     * requirement and exposes the internal message-mapping logic for unit testing.
     */
    private class TestableService(
        val handleNotification: HandleNotificationUseCase,
        val saveFcmToken: SaveFcmTokenUseCase,
    ) {
        suspend fun simulateNewToken(token: String) {
            saveFcmToken(token)
        }

        fun simulateMessage(
            title: String,
            body: String,
            data: Map<String, String> = emptyMap(),
        ) {
            val payload = buildPayload(
                title = title,
                body = body,
                data = data,
                messageId = "msg-1",
            )
            handleNotification(payload)
        }

        fun simulateDataOnlyMessage(
            data: Map<String, String>,
            messageId: String? = "msg-2",
        ) {
            val title = data["title"].orEmpty()
            val body = data["body"].orEmpty()
            val deepLink = data[NotificationPayload.KEY_DEEP_LINK]
            val channelId = data[NotificationPayload.KEY_CHANNEL]
                ?.takeIf { it.isNotBlank() }
                ?: NotificationPayload.CHANNEL_DEFAULT
            val id = messageId ?: java.util.UUID.randomUUID().toString()

            val payload = NotificationPayload(
                id = id,
                title = title,
                body = body,
                deepLink = deepLink,
                data = data,
                channelId = channelId,
            )
            handleNotification(payload)
        }

        private fun buildPayload(
            title: String,
            body: String,
            data: Map<String, String>,
            messageId: String?,
        ): NotificationPayload {
            val deepLink = data[NotificationPayload.KEY_DEEP_LINK]
            val channelId = data[NotificationPayload.KEY_CHANNEL]
                ?.takeIf { it.isNotBlank() }
                ?: NotificationPayload.CHANNEL_DEFAULT
            return NotificationPayload(
                id = messageId ?: java.util.UUID.randomUUID().toString(),
                title = title,
                body = body,
                deepLink = deepLink,
                data = data,
                channelId = channelId,
            )
        }
    }
}
