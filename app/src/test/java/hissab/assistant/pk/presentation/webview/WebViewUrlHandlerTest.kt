package hissab.assistant.pk.presentation.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebViewUrlHandlerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
    }

    // ── shouldOverrideUrl: Custom App Schemes (WhatsApp, tel, mailto, etc.) ────

    @Test
    fun `shouldOverrideUrl returns true for whatsapp scheme`() {
        val url = "whatsapp://send/?phone=923444472282&text=Hello%20I%20need%20help&t"
        assertTrue(WebViewUrlHandler.shouldOverrideUrl(url))
    }

    @Test
    fun `shouldOverrideUrl returns true for simple whatsapp scheme`() {
        assertTrue(WebViewUrlHandler.shouldOverrideUrl("whatsapp://send?text=Test"))
    }

    @Test
    fun `shouldOverrideUrl returns true for wa me web links`() {
        assertTrue(WebViewUrlHandler.shouldOverrideUrl("https://wa.me/923444472282?text=Hello"))
        assertTrue(WebViewUrlHandler.shouldOverrideUrl("http://wa.me/923444472282"))
    }

    @Test
    fun `shouldOverrideUrl returns true for api whatsapp com web links`() {
        assertTrue(WebViewUrlHandler.shouldOverrideUrl("https://api.whatsapp.com/send?phone=923444472282"))
    }

    @Test
    fun `shouldOverrideUrl returns true for tel scheme`() {
        assertTrue(WebViewUrlHandler.shouldOverrideUrl("tel:+923444472282"))
        assertTrue(WebViewUrlHandler.shouldOverrideUrl("tel:03444472282"))
    }

    @Test
    fun `shouldOverrideUrl returns true for mailto scheme`() {
        assertTrue(WebViewUrlHandler.shouldOverrideUrl("mailto:support@myhisaab.pk?subject=Help"))
    }

    @Test
    fun `shouldOverrideUrl returns true for sms scheme`() {
        assertTrue(WebViewUrlHandler.shouldOverrideUrl("sms:+923444472282?body=help"))
    }

    @Test
    fun `shouldOverrideUrl returns true for market scheme`() {
        assertTrue(WebViewUrlHandler.shouldOverrideUrl("market://details?id=com.whatsapp"))
    }

    @Test
    fun `shouldOverrideUrl returns true for intent scheme`() {
        val url = "intent://send?phone=123#Intent;scheme=whatsapp;package=com.whatsapp;end"
        assertTrue(WebViewUrlHandler.shouldOverrideUrl(url))
    }

    @Test
    fun `shouldOverrideUrl returns true for geo scheme`() {
        assertTrue(WebViewUrlHandler.shouldOverrideUrl("geo:31.5204,74.3587"))
    }

    // ── shouldOverrideUrl: In-App / Standard Web Navigation ─────────────────────

    @Test
    fun `shouldOverrideUrl returns false for standard webapp http and https urls`() {
        assertFalse(WebViewUrlHandler.shouldOverrideUrl("https://myhisaab.pk/dashboard"))
        assertFalse(WebViewUrlHandler.shouldOverrideUrl("https://app.myhisaab.pk/records"))
        assertFalse(WebViewUrlHandler.shouldOverrideUrl("http://localhost:3000/main"))
    }

    @Test
    fun `shouldOverrideUrl returns false for pseudo and internal schemes`() {
        assertFalse(WebViewUrlHandler.shouldOverrideUrl("about:blank"))
        assertFalse(WebViewUrlHandler.shouldOverrideUrl("javascript:console.log('test')"))
        assertFalse(WebViewUrlHandler.shouldOverrideUrl("data:text/html;base64,PGh0bWw+PC9odG1sPg=="))
        assertFalse(WebViewUrlHandler.shouldOverrideUrl("file:///android_asset/index.html"))
        assertFalse(WebViewUrlHandler.shouldOverrideUrl("blob:https://myhisaab.pk/1234-5678"))
    }

    // ── shouldOverrideUrl: Edge Cases ─────────────────────────────────────────

    @Test
    fun `shouldOverrideUrl returns false for null, empty, or blank string`() {
        assertFalse(WebViewUrlHandler.shouldOverrideUrl(null))
        assertFalse(WebViewUrlHandler.shouldOverrideUrl(""))
        assertFalse(WebViewUrlHandler.shouldOverrideUrl("   "))
    }

    @Test
    fun `shouldOverrideUrl returns false for strings without scheme`() {
        assertFalse(WebViewUrlHandler.shouldOverrideUrl("just_a_string_without_scheme"))
    }

    // ── Host and WhatsApp identification helpers ──────────────────────────────

    @Test
    fun `extractHost correctly parses domain from http and https urls`() {
        assertEquals("wa.me", WebViewUrlHandler.extractHost("https://wa.me/923444472282"))
        assertEquals("api.whatsapp.com", WebViewUrlHandler.extractHost("https://api.whatsapp.com/send?phone=123"))
        assertEquals("myhisaab.pk", WebViewUrlHandler.extractHost("https://myhisaab.pk/dashboard"))
        assertEquals("myhisaab.pk", WebViewUrlHandler.extractHost("https://myhisaab.pk:8080/dashboard?ref=1"))
    }

    @Test
    fun `isWhatsAppUrl accurately detects both custom scheme and web redirect links`() {
        assertTrue(WebViewUrlHandler.isWhatsAppUrl("whatsapp://send/?phone=923444472282"))
        assertTrue(WebViewUrlHandler.isWhatsAppUrl("https://wa.me/923444472282"))
        assertTrue(WebViewUrlHandler.isWhatsAppUrl("https://api.whatsapp.com/send?phone=923444472282"))
        assertFalse(WebViewUrlHandler.isWhatsAppUrl("https://google.com"))
        assertFalse(WebViewUrlHandler.isWhatsAppUrl("tel:+923444472282"))
    }

    // ── handleExternalUrl: Launching and Fallback Behavior ─────────────────────

    @Test
    fun `handleExternalUrl passes target URL to intentCreator and launches intent`() {
        val mockIntent: Intent = mockk(relaxed = true)
        val createdUrls = mutableListOf<String>()
        var launchedIntent: Intent? = null

        val handled = WebViewUrlHandler.handleExternalUrl(
            context = context,
            url = "whatsapp://send/?phone=923444472282",
            intentCreator = { url ->
                createdUrls.add(url)
                mockIntent
            },
            intentLauncher = { intent -> launchedIntent = intent }
        )

        assertTrue(handled)
        assertEquals(listOf("whatsapp://send/?phone=923444472282"), createdUrls)
        assertEquals(mockIntent, launchedIntent)
    }

    @Test
    fun `handleExternalUrl falls back to Play Store when WhatsApp is not installed`() {
        val createdUrls = mutableListOf<String>()
        var callCount = 0

        val handled = WebViewUrlHandler.handleExternalUrl(
            context = context,
            url = "whatsapp://send/?phone=923444472282",
            intentCreator = { url ->
                createdUrls.add(url)
                mockk(relaxed = true)
            },
            intentLauncher = {
                callCount++
                if (callCount == 1) {
                    // First attempt to open WhatsApp app fails (not installed)
                    throw ActivityNotFoundException("WhatsApp not found")
                }
            }
        )

        assertTrue(handled)
        assertEquals(2, createdUrls.size)
        assertEquals("whatsapp://send/?phone=923444472282", createdUrls[0])
        assertEquals("market://details?id=com.whatsapp", createdUrls[1])
    }

    @Test
    fun `handleExternalUrl falls back to Web Play Store when market link also fails`() {
        val createdUrls = mutableListOf<String>()
        var callCount = 0

        val handled = WebViewUrlHandler.handleExternalUrl(
            context = context,
            url = "whatsapp://send/?phone=923444472282",
            intentCreator = { url ->
                createdUrls.add(url)
                mockk(relaxed = true)
            },
            intentLauncher = {
                callCount++
                when (callCount) {
                    1 -> throw ActivityNotFoundException("WhatsApp not found")
                    2 -> throw ActivityNotFoundException("Play Store not found")
                }
            }
        )

        assertTrue(handled)
        assertEquals(3, createdUrls.size)
        assertEquals("whatsapp://send/?phone=923444472282", createdUrls[0])
        assertEquals("market://details?id=com.whatsapp", createdUrls[1])
        assertEquals("https://play.google.com/store/apps/details?id=com.whatsapp", createdUrls[2])
    }

    @Test
    fun `handleExternalUrl shows toast when non-whatsapp app is missing`() {
        var displayedToast: String? = null

        val handled = WebViewUrlHandler.handleExternalUrl(
            context = context,
            url = "tel:+923444472282",
            intentCreator = { mockk(relaxed = true) },
            intentLauncher = { throw ActivityNotFoundException("No dialer") },
            toastDisplayer = { msg -> displayedToast = msg }
        )

        assertTrue(handled)
        assertEquals("No app found to open this link", displayedToast)
    }

    @Test
    fun `handleExternalUrl returns false when intentCreator returns null`() {
        val handled = WebViewUrlHandler.handleExternalUrl(
            context = context,
            url = "unknown://path",
            intentCreator = { null }
        )

        assertFalse(handled)
    }
}
