package hissab.assistant.pk.data.notification

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class NotificationRepositoryImplTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var repository: NotificationRepositoryImpl

    @Before
    fun setUp() {
        editor = mockk(relaxed = true)
        prefs = mockk {
            every { edit() } returns editor
        }
        repository = NotificationRepositoryImpl(prefs)
    }

    // ── saveToken ─────────────────────────────────────────────────────────────

    @Test
    fun `saveToken writes token to prefs`() = runTest {
        every { editor.putString(any(), any()) } returns editor

        repository.saveToken("my-token")

        verify { editor.putString(NotificationRepositoryImpl.KEY_FCM_TOKEN, "my-token") }
        verify { editor.apply() }
    }

    @Test
    fun `saveToken commits via apply not commit`() = runTest {
        every { editor.putString(any(), any()) } returns editor

        repository.saveToken("token")

        verify(exactly = 0) { editor.commit() }
        verify(exactly = 1) { editor.apply() }
    }

    // ── getToken ──────────────────────────────────────────────────────────────

    @Test
    fun `getToken returns stored value`() = runTest {
        every { prefs.getString(NotificationRepositoryImpl.KEY_FCM_TOKEN, null) } returns "saved-token"

        val result = repository.getToken()

        assertEquals("saved-token", result)
    }

    @Test
    fun `getToken returns null when nothing stored`() = runTest {
        every { prefs.getString(NotificationRepositoryImpl.KEY_FCM_TOKEN, null) } returns null

        val result = repository.getToken()

        assertNull(result)
    }

    // ── deleteToken ───────────────────────────────────────────────────────────

    @Test
    fun `deleteToken removes key from prefs`() = runTest {
        every { editor.remove(any()) } returns editor

        repository.deleteToken()

        verify { editor.remove(NotificationRepositoryImpl.KEY_FCM_TOKEN) }
        verify { editor.apply() }
    }

    @Test
    fun `deleteToken does not call putString`() = runTest {
        every { editor.remove(any()) } returns editor

        repository.deleteToken()

        verify(exactly = 0) { editor.putString(any(), any()) }
    }
}
