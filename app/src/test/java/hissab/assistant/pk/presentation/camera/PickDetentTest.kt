package hissab.assistant.pk.presentation.camera

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the pure detent-selection logic behind the resizable camera
 * sheet. Screen height 2000px: hidden=0, half=1000, full=1880.
 */
class PickDetentTest {

    private val hidden = 0f
    private val half = 1000f
    private val full = 1880f

    private fun pick(current: Float, velocity: Float) =
        pickDetent(current, velocity, hidden, half, full)

    @Test
    fun `slow release snaps to nearest detent`() {
        assertEquals(half, pick(current = 1000f, velocity = 0f))
        assertEquals(half, pick(current = 1400f, velocity = 0f))   // nearer half
        assertEquals(full, pick(current = 1600f, velocity = 0f))   // nearer full
        assertEquals(hidden, pick(current = 300f, velocity = 0f))  // near closed
    }

    @Test
    fun `fast flick up grows to the next detent`() {
        assertEquals(full, pick(current = 1000f, velocity = -1000f))
        assertEquals(half, pick(current = 0f, velocity = -1000f))
    }

    @Test
    fun `fast flick down shrinks to the next detent`() {
        assertEquals(half, pick(current = 1880f, velocity = 1000f))
    }

    @Test
    fun `flick down from half dismisses`() {
        assertEquals(hidden, pick(current = 1000f, velocity = 1000f))
    }

    @Test
    fun `flick up at full stays full`() {
        assertEquals(full, pick(current = 1880f, velocity = -1000f))
    }

    @Test
    fun `small velocity below threshold is treated as a slow release`() {
        assertEquals(half, pick(current = 1300f, velocity = -200f))
    }
}
