package vn.loi.learning.android.autoplay

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AutoPlayPreferencesTest {

    class FakePreferenceStore(var currentConfig: AutoPlayConfig = AutoPlayConfig()) : AutoPlayPreferenceStore {
        override fun load(): AutoPlayConfig = currentConfig
        override fun save(config: AutoPlayConfig) {
            currentConfig = config
        }
    }

    @Test
    fun `preferences controller saves and updates direction and decimal timing values`() {
        val store = FakePreferenceStore()
        val controller = AutoPlayPreferencesController(store)

        controller.updateDirection(AutoPlayDirection.ENGLISH_TO_VIETNAMESE)
        controller.updatePlayFrontAudio(false)
        controller.updateFrontDelayMs(1500L) // 1.5s
        controller.updatePlayAnswerAudio(true)
        controller.updatePostAnswerDelayMs(750L) // 0.75s
        controller.updatePlayExampleEnglishAudio(false)
        controller.updatePostExampleEnglishDelayMs(1250L) // 1.25s
        controller.updatePlayExampleVietnameseAudio(true)
        controller.updatePostExampleVietnameseDelayMs(2750L) // 2.75s

        val updated = controller.current()
        assertEquals(AutoPlayDirection.ENGLISH_TO_VIETNAMESE, updated.direction)
        assertFalse(updated.playFrontAudio)
        assertEquals(1500L, updated.frontDelayMs)
        assertEquals(1.5, updated.frontDelaySeconds)
        assertTrue(updated.playAnswerAudio)
        assertEquals(750L, updated.postAnswerDelayMs)
        assertEquals(0.75, updated.postAnswerDelaySeconds)
        assertFalse(updated.playExampleEnglishAudio)
        assertEquals(1250L, updated.postExampleEnglishDelayMs)
        assertEquals(1.25, updated.postExampleEnglishDelaySeconds)
        assertTrue(updated.playExampleVietnameseAudio)
        assertEquals(2750L, updated.postExampleVietnameseDelayMs)
        assertEquals(2.75, updated.postExampleVietnameseDelaySeconds)

        // Stored config matches
        assertEquals(updated, store.load())
    }

    @Test
    fun `decimal normalization accepts dot and comma formats`() {
        assertEquals(1.5, AutoPlayConfig.normalizeDecimalSeconds("1.5"))
        assertEquals(1.5, AutoPlayConfig.normalizeDecimalSeconds("1,5"))
        assertEquals(0.75, AutoPlayConfig.normalizeDecimalSeconds(" 0,75 "))
        assertEquals(10.0, AutoPlayConfig.normalizeDecimalSeconds("10"))
        assertEquals(null, AutoPlayConfig.normalizeDecimalSeconds("abc"))
        assertEquals(null, AutoPlayConfig.normalizeDecimalSeconds(""))
    }

    @Test
    fun `decimal format helper formats integers and fractional numbers cleanly`() {
        assertEquals("2", AutoPlayConfig.formatSeconds(2.0))
        assertEquals("1.5", AutoPlayConfig.formatSeconds(1.5))
        assertEquals("0.75", AutoPlayConfig.formatSeconds(0.75))
        assertEquals("2.25", AutoPlayConfig.formatSeconds(2.25))
    }
}
