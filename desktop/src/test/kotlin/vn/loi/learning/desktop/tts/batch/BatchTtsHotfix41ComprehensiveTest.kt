package vn.loi.learning.desktop.tts.batch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.strategy.VoiceAttempt
import vn.loi.learning.desktop.tts.ui.OrderedFallbackVoices
import vn.loi.learning.domain.content.model.ContentId

class BatchTtsHotfix41ComprehensiveTest {

    private val voiceEnPrimary = TtsVoice("en-US-JennyNeural", "Jenny", "en-US", "en", "Female")
    private val voiceEnFallback1 = TtsVoice("en-US-GuyNeural", "Guy", "en-US", "en", "Male")
    private val voiceEnFallback2 = TtsVoice("en-US-AriaNeural", "Aria", "en-US", "en", "Female")
    private val voiceEnFallback3 = TtsVoice("en-GB-SoniaNeural", "Sonia", "en-GB", "en", "Female")
    private val voiceEnFallback4 = TtsVoice("en-US-ChristopherNeural", "Christopher", "en-US", "en", "Male")

    private val voiceViPrimary = TtsVoice("vi-VN-HoaiMyNeural", "Hoài My", "vi-VN", "vi", "Female")
    private val voiceViFallback1 = TtsVoice("vi-VN-NamMinhNeural", "Nam Minh", "vi-VN", "vi", "Male")

    private val catalog = listOf(
        voiceEnPrimary,
        voiceEnFallback1,
        voiceEnFallback2,
        voiceEnFallback3,
        voiceEnFallback4,
        voiceViPrimary,
        voiceViFallback1
    )

    private fun createBrowserItem(
        id: String = "vocab-1",
        q: String = "apple",
        a: String = "Quả táo",
        ex: String = "I eat an apple every day.",
        tr: String = "Tôi ăn một quả táo mỗi ngày."
    ): PackageContentBrowserItem =
        PackageContentBrowserItem(
            index = 1,
            contentId = ContentId(id),
            questionText = q,
            answerText = a,
            pronunciation = "/ˈæp.əl/",
            partOfSpeech = "NOUN",
            group = "Vocabulary",
            section = "Food",
            lesson = "Unit 1",
            packageName = "Vocabulary_In_Use_Upper_Intermediate",
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
            searchableText = "$q $a $ex $tr"
        )

    // ======================================================================
    // 1. Fallback Voice UX & OrderedFallbackVoices Domain Model
    // ======================================================================

    @Test
    fun `fallback voices add enforces maximum of 3 fallbacks`() {
        var fallbacks = OrderedFallbackVoices.EMPTY
        assertEquals(0, fallbacks.voices.size)

        fallbacks = fallbacks.add(voiceEnFallback1, voiceEnPrimary)
        assertEquals(1, fallbacks.voices.size)
        assertEquals(voiceEnFallback1.id, fallbacks.voices[0].id)

        fallbacks = fallbacks.add(voiceEnFallback2, voiceEnPrimary)
        assertEquals(2, fallbacks.voices.size)

        fallbacks = fallbacks.add(voiceEnFallback3, voiceEnPrimary)
        assertEquals(3, fallbacks.voices.size)

        // 4th addition must throw IllegalArgumentException due to MAX_FALLBACKS = 3
        assertThrows<IllegalArgumentException> {
            fallbacks.add(voiceEnFallback4, voiceEnPrimary)
        }
        assertEquals(listOf(voiceEnFallback1.id, voiceEnFallback2.id, voiceEnFallback3.id), fallbacks.ids)
    }

    @Test
    fun `fallback voices add rejects primary voice and duplicates`() {
        val emptyFallbacks = OrderedFallbackVoices.EMPTY

        // Rejects primary voice
        assertThrows<IllegalArgumentException> {
            emptyFallbacks.add(voiceEnPrimary, voiceEnPrimary)
        }

        // Adds unique voice
        val singleFallback = emptyFallbacks.add(voiceEnFallback1, voiceEnPrimary)
        assertEquals(1, singleFallback.voices.size)

        // Rejects duplicate addition
        assertThrows<IllegalArgumentException> {
            singleFallback.add(voiceEnFallback1, voiceEnPrimary)
        }
    }

    @Test
    fun `fallback voices reordering and removal maintain execution sequence`() {
        var fallbacks = OrderedFallbackVoices.EMPTY
            .add(voiceEnFallback1, voiceEnPrimary)
            .add(voiceEnFallback2, voiceEnPrimary)
            .add(voiceEnFallback3, voiceEnPrimary)

        // Move item 1 (voiceEnFallback2) UP to position 0
        fallbacks = fallbacks.move(voiceEnFallback2.id, -1)
        assertEquals(listOf(voiceEnFallback2.id, voiceEnFallback1.id, voiceEnFallback3.id), fallbacks.ids)

        // Move item 0 (voiceEnFallback2) DOWN to position 1
        fallbacks = fallbacks.move(voiceEnFallback2.id, 1)
        assertEquals(listOf(voiceEnFallback1.id, voiceEnFallback2.id, voiceEnFallback3.id), fallbacks.ids)

        // Remove item 1 (voiceEnFallback2)
        fallbacks = fallbacks.remove(voiceEnFallback2.id)
        assertEquals(listOf(voiceEnFallback1.id, voiceEnFallback3.id), fallbacks.ids)
        assertEquals(2, fallbacks.voices.size)
    }

    @Test
    fun `fallback voices hydration filters catalog for matching language and excludes primary`() {
        val rawIds = listOf(voiceEnFallback1.id, voiceViPrimary.id, voiceEnFallback2.id, "non-existent-id")

        val hydratedEn = OrderedFallbackVoices.hydrate(rawIds, catalog, TtsLanguage.ENGLISH, voiceEnPrimary)
        assertEquals(listOf(voiceEnFallback1.id, voiceEnFallback2.id), hydratedEn.ids)

        // If primary voice is set to voiceEnFallback1, hydration must exclude it
        val hydratedEnWithNewPrimary = OrderedFallbackVoices.hydrate(rawIds, catalog, TtsLanguage.ENGLISH, voiceEnFallback1)
        assertEquals(listOf(voiceEnFallback2.id), hydratedEnWithNewPrimary.ids)
    }

    // ======================================================================
    // 2. Multi-Target Preview Navigation in BatchTtsScanner
    // ======================================================================

    @Test
    fun `scanBatchScope collects all samples per field for multi-target browsing`() {
        val items = listOf(
            createBrowserItem(
                id = "item-1",
                q = "Apple",
                a = "Quả táo",
                ex = "I eat an apple every day.",
                tr = "Tôi ăn một quả táo mỗi ngày."
            ),
            createBrowserItem(
                id = "item-2",
                q = "Banana",
                a = "Quả chuối",
                ex = "Monkeys love bananas.",
                tr = "Những con khỉ thích chuối."
            ),
            createBrowserItem(
                id = "item-3",
                q = "Orange",
                a = "Quả cam",
                ex = "Orange juice is fresh.",
                tr = "Nước cam rất tươi."
            )
        )

        val selectedFields = setOf(TtsField.QUESTION, TtsField.ANSWER, TtsField.EXAMPLE, TtsField.TRANSLATION)
        val scan = BatchTtsScanner.scanBatchScope(
            items = items,
            selectedFields = selectedFields,
            overwriteExisting = false
        )

        assertEquals(3, scan.totalSelectedItems)
        assertEquals(12, scan.totalValidTargets) // 3 items * 4 fields

        // Verify samplesFor returns multiple samples per field
        val questionSamples = scan.samplesFor(TtsField.QUESTION)
        assertEquals(3, questionSamples.size)
        assertEquals("Apple", questionSamples[0].text)
        assertEquals("Item #1 (Apple)", questionSamples[0].displaySource)
        assertEquals("Banana", questionSamples[1].text)
        assertEquals("Orange", questionSamples[2].text)

        val answerSamples = scan.samplesFor(TtsField.ANSWER)
        assertEquals(3, answerSamples.size)
        assertEquals("Quả táo", answerSamples[0].text)
        assertEquals("Item #1 (Apple)", answerSamples[0].displaySource)

        val exampleSamples = scan.samplesFor(TtsField.EXAMPLE)
        assertEquals(3, exampleSamples.size)
        assertEquals("I eat an apple every day.", exampleSamples[0].text)

        val translationSamples = scan.samplesFor(TtsField.TRANSLATION)
        assertEquals(3, translationSamples.size)
        assertEquals("Tôi ăn một quả táo mỗi ngày.", translationSamples[0].text)
    }

    // ======================================================================
    // 3. Runtime Voice Transparency in BatchTtsSummary
    // ======================================================================

    @Test
    fun `summary accurately reports candidate index and fallback badge transparency`() {
        val job = BatchTtsJob(
            contentId = "item-1",
            field = TtsField.QUESTION,
            text = "Hello world",
            language = TtsLanguage.ENGLISH,
            voice = voiceEnPrimary,
            candidateVoices = listOf(voiceEnPrimary, voiceEnFallback1, voiceEnFallback2)
        )

        // Primary execution: candidate 1 of 3
        val primarySummary = BatchTtsSummary(
            totalJobs = 10,
            completedJobs = 2,
            currentJob = job,
            currentVoiceName = voiceEnPrimary.displayName,
            currentCandidateIndex = 1,
            totalCandidatesForCurrentJob = 3,
            currentExecutingVoice = voiceEnPrimary
        )

        assertFalse(primarySummary.isCurrentCandidateFallback)
        assertEquals(voiceEnPrimary.displayName, primarySummary.currentExecutingVoice?.displayName)

        // Fallback candidate 2 execution: candidate 2 of 3
        val fallbackSummary = BatchTtsSummary(
            totalJobs = 10,
            completedJobs = 2,
            currentJob = job,
            currentVoiceName = voiceEnFallback1.displayName,
            currentCandidateIndex = 2,
            totalCandidatesForCurrentJob = 3,
            currentExecutingVoice = voiceEnFallback1
        )

        assertTrue(fallbackSummary.isCurrentCandidateFallback)
        assertEquals("Guy", fallbackSummary.currentExecutingVoice?.displayName)
    }

    // ======================================================================
    // 4. Truthful Wording & Cancellation Apply Retention
    // ======================================================================

    @Test
    fun `cancelled batch summary retains successful results for atomic Apply`() {
        val job1 = BatchTtsJob("item-1", TtsField.QUESTION, "Hello", TtsLanguage.ENGLISH, voiceEnPrimary)
        val job2 = BatchTtsJob("item-2", TtsField.QUESTION, "World", TtsLanguage.ENGLISH, voiceEnPrimary)
        val job3 = BatchTtsJob("item-3", TtsField.QUESTION, "Peace", TtsLanguage.ENGLISH, voiceEnPrimary)

        val result1 = BatchTtsJobResult(
            job = job1,
            status = BatchTtsJobStatus.SUCCESS,
            assetRelativePath = "audio/item-1_question.mp3",
            actualVoiceUsed = voiceEnPrimary
        )
        val result2 = BatchTtsJobResult(
            job = job2,
            status = BatchTtsJobStatus.SUCCESS,
            assetRelativePath = "audio/item-2_question.mp3",
            actualVoiceUsed = voiceEnFallback1,
            recoveredViaFallback = true,
            attempts = listOf(
                VoiceAttempt(voiceEnPrimary, false, TtsErrorCategory.VOICE_UNAVAILABLE, "Circuit open"),
                VoiceAttempt(voiceEnFallback1, true, null, null)
            )
        )
        val result3 = BatchTtsJobResult(
            job = job3,
            status = BatchTtsJobStatus.CANCELLED,
            errorCategory = TtsErrorCategory.CANCELLED,
            errorMessage = "Cancelled by user"
        )

        val cancelledSummary = BatchTtsSummary(
            totalJobs = 3,
            completedJobs = 3,
            successCount = 2,
            cancelledCount = 1,
            failedCount = 0,
            jobResults = listOf(result1, result2, result3),
            isFinished = true,
            isCancelled = true
        )

        assertTrue(cancelledSummary.isCancelled)
        assertEquals(2, cancelledSummary.successCount)
        assertEquals(1, cancelledSummary.cancelledCount)
        assertEquals(1, cancelledSummary.fallbackRecoveredCount)
        assertFalse(cancelledSummary.hasFailures)

        // Crucial safety check: successful subset is preserved and available for atomic Apply
        assertEquals(2, cancelledSummary.successfulResults.size)
        assertEquals("audio/item-1_question.mp3", cancelledSummary.successfulResults[0].assetRelativePath)
        assertEquals("audio/item-2_question.mp3", cancelledSummary.successfulResults[1].assetRelativePath)
    }
}
