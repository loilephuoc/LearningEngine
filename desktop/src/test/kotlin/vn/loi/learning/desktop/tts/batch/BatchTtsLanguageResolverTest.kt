package vn.loi.learning.desktop.tts.batch

import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.domain.content.model.ContentId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatchTtsLanguageResolverTest {

    private fun createItem(
        packageName: String = "Vocabulary_In_Use_Upper_Intermediate",
        q: String = "ache",
        a: String = "Mặc dù, tuy nhiên.",
        ex: String = "My head aches terribly.",
        tr: String = "Đầu tôi đau nhức khủng khiếp."
    ): PackageContentBrowserItem =
        PackageContentBrowserItem(
            index = 1,
            contentId = ContentId("item-vocab-1"),
            questionText = q,
            answerText = a,
            pronunciation = "/eɪk/",
            partOfSpeech = "VERB",
            group = "Health",
            section = "A",
            lesson = "Unit 1",
            packageName = packageName,
            hasImage = false,
            hasAudio = false,
            imageRef = null,
            audioRef = null,
            questionAudioRef = null,
            answerAudioRef = null,
            exampleAudioRef = null,
            translationAudioRef = null,
            exampleText = ex,
            exampleTranslation = tr,
            learningItemCount = 1,
            learningItemIds = emptyList(),
            learningModes = emptyList(),
            tags = emptySet(),
            searchableText = "$q $a"
        )

    @Test
    fun `resolves canonical languages for Vocabulary_In_Use_Upper_Intermediate package fixture`() {
        val item = createItem()

        // Question -> English (Prompt)
        assertEquals(
            TtsLanguage.ENGLISH,
            BatchTtsLanguageResolver.resolveRequiredLanguage(item.packageName, item, TtsField.QUESTION)
        )

        // Answer -> Vietnamese (Translated Definition)
        assertEquals(
            TtsLanguage.VIETNAMESE,
            BatchTtsLanguageResolver.resolveRequiredLanguage(item.packageName, item, TtsField.ANSWER)
        )

        // Example -> English (Sample Sentence)
        assertEquals(
            TtsLanguage.ENGLISH,
            BatchTtsLanguageResolver.resolveRequiredLanguage(item.packageName, item, TtsField.EXAMPLE)
        )

        // Translation -> Vietnamese (Example Translation)
        assertEquals(
            TtsLanguage.VIETNAMESE,
            BatchTtsLanguageResolver.resolveRequiredLanguage(item.packageName, item, TtsField.TRANSLATION)
        )
    }

    @Test
    fun `voice compatibility checker accurately validates locales and codes`() {
        // en-US voice is compatible with English
        assertTrue(BatchTtsLanguageResolver.isVoiceCompatibleWithLanguage("en", "en-US", TtsLanguage.ENGLISH))
        assertTrue(BatchTtsLanguageResolver.isVoiceCompatibleWithLanguage("en", "en-GB", TtsLanguage.ENGLISH))
        assertTrue(BatchTtsLanguageResolver.isVoiceCompatibleWithLanguage("en", "en-AU", TtsLanguage.ENGLISH))

        // en-US voice is NOT compatible with Vietnamese
        assertFalse(BatchTtsLanguageResolver.isVoiceCompatibleWithLanguage("en", "en-US", TtsLanguage.VIETNAMESE))

        // vi-VN voice is compatible with Vietnamese
        assertTrue(BatchTtsLanguageResolver.isVoiceCompatibleWithLanguage("vi", "vi-VN", TtsLanguage.VIETNAMESE))

        // vi-VN voice is NOT compatible with English
        assertFalse(BatchTtsLanguageResolver.isVoiceCompatibleWithLanguage("vi", "vi-VN", TtsLanguage.ENGLISH))
    }

    @Test
    fun `BatchTtsLanguageRequirements recomputes immediately on mixed-field selections`() {
        val item = createItem()

        // Question + Answer -> requires English AND Vietnamese
        val qaScan = BatchTtsScanner.scanBatchScope(listOf(item), setOf(TtsField.QUESTION, TtsField.ANSWER))
        val qaReq = BatchTtsLanguageRequirements.from(qaScan)
        assertTrue(qaReq.requiresEnglish)
        assertTrue(qaReq.requiresVietnamese)

        // Question + Example -> requires ONLY English
        val qeScan = BatchTtsScanner.scanBatchScope(listOf(item), setOf(TtsField.QUESTION, TtsField.EXAMPLE))
        val qeReq = BatchTtsLanguageRequirements.from(qeScan)
        assertTrue(qeReq.requiresEnglish)
        assertFalse(qeReq.requiresVietnamese)

        // Answer + Translation -> requires ONLY Vietnamese
        val atScan = BatchTtsScanner.scanBatchScope(listOf(item), setOf(TtsField.ANSWER, TtsField.TRANSLATION))
        val atReq = BatchTtsLanguageRequirements.from(atScan)
        assertFalse(atReq.requiresEnglish)
        assertTrue(atReq.requiresVietnamese)
    }
}
