package hissab.assistant.pk.presentation.webview

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FcmTokenScriptTest {

    @Test
    fun `script contains global assignment and event dispatch`() {
        val script = FcmTokenScript.build("abc123")

        assertTrue(script.contains("window.MYHISAAB_FCM_TOKEN"))
        assertTrue(script.contains("window.dispatchEvent"))
        assertTrue(script.contains(FcmTokenScript.EVENT_NAME))
    }

    @Test
    fun `payload carries token and android platform`() {
        val script = FcmTokenScript.build("my-token")
        val payload = extractPayload(script)

        assertEquals("my-token", payload.getString("token"))
        assertEquals("android", payload.getString("platform"))
    }

    @Test
    fun `token with quotes and backslashes is safely escaped`() {
        // Realistic FCM tokens are [A-Za-z0-9:_-], but the builder must not
        // rely on that: a hostile or malformed value must not break out of
        // the script (the naive string-interpolation approach would).
        val hostile = """evil'); alert("xss"); \ end"""
        val script = FcmTokenScript.build(hostile)
        val payload = extractPayload(script)

        assertEquals(hostile, payload.getString("token"))
    }

    @Test
    fun `script is wrapped in an IIFE to avoid polluting page scope`() {
        val script = FcmTokenScript.build("t")
        assertTrue(script.trimStart().startsWith("(function()"))
        assertTrue(script.trimEnd().endsWith("})();"))
    }

    /**
     * Pulls the JSON object literal assigned to `var payload = ...;`.
     * Line-based on purpose: the token itself may contain `;`, but JSONObject
     * escapes control characters, so a raw newline can never appear inside
     * the serialized payload — the line boundary is a safe terminator.
     */
    private fun extractPayload(script: String): JSONObject {
        val marker = "var payload = "
        val line = script.lines().first { it.contains(marker) }
        return JSONObject(line.substringAfter(marker).trim().removeSuffix(";"))
    }
}
