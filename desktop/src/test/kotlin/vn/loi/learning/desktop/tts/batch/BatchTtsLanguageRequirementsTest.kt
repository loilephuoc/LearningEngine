package vn.loi.learning.desktop.tts.batch

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsVoice

class BatchTtsLanguageRequirementsTest {
    private val en = TtsVoice("en", "English", "en-US", "en")
    private val vi = TtsVoice("vi", "Vietnamese", "vi-VN", "vi")

    @Test
    fun `translation only renders and requires Vietnamese only`() {
        val requirements = requirements(0, 1)
        assertFalse(requirements.requiresEnglish)
        assertTrue(requirements.requiresVietnamese)
        assertTrue(requirements.configurationsValid(null, vi))
    }

    @Test
    fun `English fields render and require English only`() {
        val requirements = requirements(3, 0)
        assertTrue(requirements.requiresEnglish)
        assertFalse(requirements.requiresVietnamese)
        assertTrue(requirements.configurationsValid(en, null))
    }

    @Test
    fun `mixed targets render and require both languages`() {
        val requirements = requirements(1, 1)
        assertFalse(requirements.configurationsValid(en, null))
        assertFalse(requirements.configurationsValid(null, vi))
        assertTrue(requirements.configurationsValid(en, vi))
    }

    @Test
    fun `changing mixed to translation only drops stale English validation`() {
        assertFalse(requirements(1, 1).configurationsValid(null, vi))
        assertTrue(requirements(0, 1).configurationsValid(null, vi))
    }

    @Test
    fun `changing translation only to mixed makes English required`() {
        assertTrue(requirements(0, 1).configurationsValid(null, vi))
        assertFalse(requirements(1, 1).configurationsValid(null, vi))
    }

    private fun requirements(enCount: Int, viCount: Int) = BatchTtsLanguageRequirements.from(
        BatchTtsScopeScan(1, setOf(TtsField.QUESTION), emptyList(), emptyMap(), 0, 0, enCount, viCount, null, null)
    )
}
