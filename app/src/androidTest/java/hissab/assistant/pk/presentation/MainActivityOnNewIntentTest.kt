package hissab.assistant.pk.presentation

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityOnNewIntentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    // ── onNewIntent: stored intent is always updated ──────────────────────────

    @Test
    fun onNewIntent_updatesStoredIntent() {
        launchActivity().use { scenario ->
            val incoming = shareIntent(uri = sampleUri())
            scenario.onActivity { activity ->
                activity.onNewIntent(incoming)
                assertEquals(incoming, activity.intent)
            }
        }
    }

    @Test
    fun onNewIntent_withNonShareIntent_updatesStoredIntent() {
        launchActivity().use { scenario ->
            val incoming = Intent(Intent.ACTION_VIEW)
            scenario.onActivity { activity ->
                activity.onNewIntent(incoming)
                assertEquals(incoming, activity.intent)
            }
        }
    }

    // ── onNewIntent: valid share intent does not crash ────────────────────────

    @Test
    fun onNewIntent_withImageJpeg_doesNotCrash() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.onNewIntent(shareIntent(mimeType = "image/jpeg", uri = sampleUri()))
            }
        }
    }

    @Test
    fun onNewIntent_withImagePng_doesNotCrash() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.onNewIntent(shareIntent(mimeType = "image/png", uri = sampleUri()))
            }
        }
    }

    // ── onNewIntent: edge cases do not crash ──────────────────────────────────

    @Test
    fun onNewIntent_withNullExtraStream_doesNotCrash() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.onNewIntent(shareIntent(uri = null))
            }
        }
    }

    @Test
    fun onNewIntent_withWrongMimeType_doesNotCrash() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.onNewIntent(shareIntent(mimeType = "application/pdf", uri = sampleUri()))
            }
        }
    }

    @Test
    fun onNewIntent_withNonSendAction_doesNotCrash() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.onNewIntent(Intent(Intent.ACTION_VIEW))
            }
        }
    }

    // ── consecutive calls ─────────────────────────────────────────────────────

    @Test
    fun onNewIntent_calledTwice_storedIntentReflectsLastCall() {
        launchActivity().use { scenario ->
            val first = shareIntent(uri = sampleUri("1"))
            val second = shareIntent(uri = sampleUri("2"))
            scenario.onActivity { activity ->
                activity.onNewIntent(first)
                activity.onNewIntent(second)
                assertEquals(second, activity.intent)
            }
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun launchActivity(): ActivityScenario<MainActivity> =
        ActivityScenario.launch(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        )

    private fun shareIntent(
        action: String = Intent.ACTION_SEND,
        mimeType: String = "image/jpeg",
        uri: Uri? = sampleUri(),
    ) = Intent(action).apply {
        type = mimeType
        uri?.let { putExtra(Intent.EXTRA_STREAM, it) }
    }

    private fun sampleUri(id: String = "42") =
        Uri.parse("content://media/external/images/media/$id")
}
