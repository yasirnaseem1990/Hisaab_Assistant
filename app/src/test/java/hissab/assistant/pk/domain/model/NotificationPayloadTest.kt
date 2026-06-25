package hissab.assistant.pk.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationPayloadTest {

    @Test
    fun `default channelId is CHANNEL_DEFAULT`() {
        val payload = NotificationPayload(id = "1", title = "T", body = "B")
        assertEquals(NotificationPayload.CHANNEL_DEFAULT, payload.channelId)
    }

    @Test
    fun `default data map is empty`() {
        val payload = NotificationPayload(id = "1", title = "T", body = "B")
        assertEquals(emptyMap<String, String>(), payload.data)
    }

    @Test
    fun `default imageUrl is null`() {
        val payload = NotificationPayload(id = "1", title = "T", body = "B")
        assertNull(payload.imageUrl)
    }

    @Test
    fun `default deepLink is null`() {
        val payload = NotificationPayload(id = "1", title = "T", body = "B")
        assertNull(payload.deepLink)
    }

    @Test
    fun `CHANNEL_DEFAULT constant value is correct`() {
        assertEquals("hisaab_default", NotificationPayload.CHANNEL_DEFAULT)
    }

    @Test
    fun `CHANNEL_PRIORITY constant value is correct`() {
        assertEquals("hisaab_priority", NotificationPayload.CHANNEL_PRIORITY)
    }

    @Test
    fun `KEY_DEEP_LINK constant value is correct`() {
        assertEquals("deep_link", NotificationPayload.KEY_DEEP_LINK)
    }

    @Test
    fun `KEY_CHANNEL constant value is correct`() {
        assertEquals("channel_id", NotificationPayload.KEY_CHANNEL)
    }

    @Test
    fun `two payloads with same fields are equal`() {
        val a = NotificationPayload(id = "1", title = "T", body = "B")
        val b = NotificationPayload(id = "1", title = "T", body = "B")
        assertEquals(a, b)
    }

    @Test
    fun `copy preserves all fields`() {
        val original = NotificationPayload(
            id = "x",
            title = "Original",
            body = "Body",
            imageUrl = "https://img.png",
            deepLink = "hisaab://home",
            data = mapOf("k" to "v"),
            channelId = NotificationPayload.CHANNEL_PRIORITY,
        )
        val copy = original.copy(title = "Changed")

        assertEquals("Changed", copy.title)
        assertEquals("Body", copy.body)
        assertEquals("hisaab://home", copy.deepLink)
        assertEquals(NotificationPayload.CHANNEL_PRIORITY, copy.channelId)
    }
}
