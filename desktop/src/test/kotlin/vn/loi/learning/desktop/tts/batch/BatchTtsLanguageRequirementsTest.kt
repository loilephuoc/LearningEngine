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
        val requirements = requirements(setOf(TtsField.TRANSLATION))
        assertFalse(requirements.requiresEnglish)
        assertTrue(requirements.requiresVietnamese)
        assertTrue(requirements.configurationsValid(null, vi))
    }

    @Test
    fun `English fields render and require English only`() {
        val requirements = requirements(setOf(TtsField.QUESTION, TtsField.EXAMPLE))
        assertTrue(requirements.requiresEnglish)
        assertFalse(requirements.requiresVietnamese)
        assertTrue(requirements.configurationsValid(en, null))
    }

    @Test
    fun `mixed targets render and require both languages`() {
        val requirements = requirements(setOf(TtsField.QUESTION, TtsField.ANSWER))
        assertTrue(requirements.requiresEnglish)
        assertTrue(requirements.requiresVietnamese)
        assertFalse(requirements.configurationsValid(en, null))
        assertFalse(requirements.configurationsValid(null, vi))
        assertTrue(requirements.configurationsValid(en, vi))
    }

    @Test
    fun `changing mixed to translation only drops stale English validation`() {
        assertFalse(requirements(setOf(TtsField.QUESTION, TtsField.TRANSLATION)).configurationsValid(null, vi))
        assertTrue(requirements(setOf(TtsField.TRANSLATION)).configurationsValid(null, vi))
    }

    @Test
    fun `changing translation only to mixed makes English required`() {
        assertTrue(requirements(setOf(TtsField.TRANSLATION)).configurationsValid(null, vi))
        assertFalse(requirements(setOf(TtsField.QUESTION, TtsField.TRANSLATION)).configurationsValid(null, vi))
    }

    @Test
    fun `zero pending target count preserves selected language requirement`() {
        val scanWithZeroViTargets = BatchTtsScopeScan(
            totalSelectedItems = 10,
            selectedFields = setOf(TtsField.QUESTION, TtsField.ANSWER),
            validTargets = emptyList(),
            missingCountByField = emptyMap(),
            existingAudioSkippedCount = 10,
            emptyTextSkippedCount = 0,
            englishTargetsCount = 10,
            vietnameseTargetsCount = 0,
            representativeEnglishText = "Hello",
            representativeVietnameseText = "Xin chào"
        )
        val requirements = BatchTtsLanguageRequirements.from(scanWithZeroViTargets)
        assertTrue(requirements.requiresEnglish, "English must be required when Question is selected")
        assertTrue(requirements.requiresVietnamese, "Vietnamese must be required when Answer is selected even if target count is 0")
    }

    private fun requirements(fields: Set<TtsField>) = BatchTtsLanguageRequirements.from(fields)
}
