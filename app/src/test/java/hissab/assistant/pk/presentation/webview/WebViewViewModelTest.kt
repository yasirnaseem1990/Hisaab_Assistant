package hissab.assistant.pk.presentation.webview

import android.util.Log
import hissab.assistant.pk.domain.model.WebPage
import hissab.assistant.pk.domain.usecase.GetDefaultWebPageUseCase
import hissab.assistant.pk.domain.usecase.ObserveFcmTokenUseCase
import hissab.assistant.pk.domain.usecase.SyncFcmTokenUseCase
import hissab.assistant.pk.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WebViewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getDefaultWebPage: GetDefaultWebPageUseCase
    private lateinit var observeFcmToken: ObserveFcmTokenUseCase
    private lateinit var syncFcmToken: SyncFcmTokenUseCase
    private val tokenSource = MutableStateFlow<String?>(null)

    @Before
    fun setUp() {
        // ViewModel logs sync failures; android.util.Log is not available in
        // local unit tests (would throw "not mocked"), so stub it statically.
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0

        getDefaultWebPage = mockk {
            every { this@mockk() } returns WebPage(url = "https://myhisaab.example")
        }
        observeFcmToken = mockk {
            every { this@mockk() } returns tokenSource
        }
        syncFcmToken = mockk {
            coEvery { this@mockk() } returns Result.success("synced-token")
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun createViewModel() =
        WebViewViewModel(getDefaultWebPage, observeFcmToken, syncFcmToken)

    // ── page state ────────────────────────────────────────────────────────────

    @Test
    fun `init resolves default url`() = runTest {
        val viewModel = createViewModel()

        assertEquals("https://myhisaab.example", viewModel.uiState.value.url)
    }

    @Test
    fun `onPageFinished increments pageLoadCount per completed load`() = runTest {
        val viewModel = createViewModel()
        assertEquals(0, viewModel.uiState.value.pageLoadCount)

        viewModel.onPageFinished()
        viewModel.onPageFinished() // reload / navigation

        assertEquals(2, viewModel.uiState.value.pageLoadCount)
    }

    // ── fcm token ─────────────────────────────────────────────────────────────

    @Test
    fun `init triggers token sync from provider`() = runTest {
        createViewModel()

        coVerify(exactly = 1) { syncFcmToken() }
    }

    @Test
    fun `fcmToken is null before any token exists`() = runTest {
        val viewModel = createViewModel()

        assertNull(viewModel.fcmToken.value)
    }

    @Test
    fun `fcmToken emits token and subsequent rotation`() = runTest {
        val viewModel = createViewModel()

        tokenSource.value = "token-1"
        assertEquals("token-1", viewModel.fcmToken.first { it != null })

        tokenSource.value = "token-2" // FCM rotated mid-session
        assertEquals("token-2", viewModel.fcmToken.first { it == "token-2" })
    }

    @Test
    fun `sync failure is non-fatal and cached token still flows`() = runTest {
        coEvery { syncFcmToken() } returns Result.failure(IllegalStateException("no play services"))
        tokenSource.value = "cached-token"

        val viewModel = createViewModel()

        assertEquals("cached-token", viewModel.fcmToken.first { it != null })
        assertEquals("https://myhisaab.example", viewModel.uiState.value.url)
        assertNull(viewModel.uiState.value.errorMessage) // never surfaced to UI
    }
}
