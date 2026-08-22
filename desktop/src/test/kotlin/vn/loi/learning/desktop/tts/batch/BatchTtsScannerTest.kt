package vn.loi.learning.desktop.tts.batch

import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.domain.content.model.ContentId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatchTtsScannerTest {

    private val enVoice = TtsVoice(
        id = "en-US-AvaMultilingualNeural",
        displayName = "Microsoft Ava",
        locale = "en-US",
        language = "en",
        gender = "Female"
    )

    private val viVoice = TtsVoice(
        id = "vi-VN-HoaiMyNeural",
        displayName = "Microsoft HoaiMy",
        locale = "vi-VN",
        language = "vi",
        gender = "Female"
    )

    private fun createBrowserItem(
        id: String = "cnt-1",
        q: String = "genuine",
        a: String = "authentic",
        ex: String = "Genuine leather",
        tr: String = "Da thật",
        qAudio: String? = null,
        aAudio: String? = null,
        exAudio: String? = null,
        trAudio: String? = null
    ): PackageContentBrowserItem =
        PackageContentBrowserItem(
            index = 1,
            contentId = ContentId(id),
            questionText = q,
            answerText = a,
            pronunciation = "/ˈdʒen.ju.ɪn/",
            partOfSpeech = "ADJ",
            group = "Vocabulary",
            section = "A",
            lesson = "Lesson 1",
            packageName = "oxford_3000",
            hasImage = false,
            hasAudio = qAudio != null || aAudio != null || exAudio != null || trAudio != null,
            imageRef = null,
            audioRef = qAudio ?: aAudio ?: exAudio ?: trAudio,
            questionAudioRef = qAudio,
            answerAudioRef = aAudio,
            exampleAudioRef = exAudio,
            translationAudioRef = trAudio,
            exampleText = ex,
            exampleTranslation = tr,
            learningItemCount = 2,
            learningItemIds = emptyList(),
            learningModes = emptyList(),
            tags = emptySet(),
            searchableText = "$q $a"
        )

    @Test
    fun `defaultLanguageFor maps fields to expected languages`() {
        assertEquals(TtsLanguage.ENGLISH, BatchTtsScanner.defaultLanguageFor(TtsField.QUESTION))
        assertEquals(TtsLanguage.ENGLISH, BatchTtsScanner.defaultLanguageFor(TtsField.ANSWER))
        assertEquals(TtsLanguage.ENGLISH, BatchTtsScanner.defaultLanguageFor(TtsField.EXAMPLE))
        assertEquals(TtsLanguage.VIETNAMESE, BatchTtsScanner.defaultLanguageFor(TtsField.TRANSLATION))
    }

    @Test
    fun `1 item with 4 missing fields produces 4 independent audio targets`() {
        val item = createBrowserItem(
            id = "c100",
            q = "apple",
            a = "fruit",
            ex = "Fresh apple",
            tr = "Quả táo"
        )

        val scan = BatchTtsScanner.scanBatchScope(listOf(item), TtsField.entries.toSet())

        assertEquals(1, scan.totalSelectedItems)
        assertEquals(4, scan.totalValidTargets)
        assertEquals(3, scan.englishTargetsCount)
        assertEquals(1, scan.vietnameseTargetsCount)
        assertEquals(0, scan.existingAudioSkippedCount)
        assertEquals(0, scan.emptyTextSkippedCount)
        assertEquals(1, scan.missingCountByField[TtsField.QUESTION])
        assertEquals(1, scan.missingCountByField[TtsField.ANSWER])
        assertEquals(1, scan.missingCountByField[TtsField.EXAMPLE])
        assertEquals(1, scan.missingCountByField[TtsField.TRANSLATION])
        assertEquals("apple", scan.representativeEnglishText)
        assertEquals("Quả táo", scan.representativeVietnameseText)
    }

    @Test
    fun `selected fields filter restricts generated targets while reporting per-field missing counts`() {
        val item = createBrowserItem(
            q = "banana",
            a = "yellow fruit",
            ex = "Sweet banana",
            tr = "Quả chuối"
        )

        // Only select QUESTION and TRANSLATION
        val scan = BatchTtsScanner.scanBatchScope(listOf(item), setOf(TtsField.QUESTION, TtsField.TRANSLATION))

        assertEquals(2, scan.totalValidTargets)
        assertEquals(1, scan.englishTargetsCount)
        assertEquals(1, scan.vietnameseTargetsCount)
        assertTrue(scan.validTargets.any { it.field == TtsField.QUESTION })
        assertTrue(scan.validTargets.any { it.field == TtsField.TRANSLATION })
        assertFalse(scan.validTargets.any { it.field == TtsField.ANSWER })
        assertFalse(scan.validTargets.any { it.field == TtsField.EXAMPLE })

        // But missing counts for all fields in selection are still computed
        assertEquals(1, scan.missingCountByField[TtsField.ANSWER])
        assertEquals(1, scan.missingCountByField[TtsField.EXAMPLE])
    }

    @Test
    fun `scanBatchScope protects existing audio and skips empty text`() {
        val item1 = createBrowserItem(
            id = "item1",
            q = "cat",
            a = "feline",
            ex = "A black cat",
            tr = "Con mèo",
            qAudio = "existing_cat_q.mp3",
            exAudio = "existing_cat_ex.mp3"
        )
        val item2 = createBrowserItem(
            id = "item2",
            q = "dog",
            a = "canine",
            ex = "", // Empty example
            tr = "   " // Whitespace translation
        )

        val scan = BatchTtsScanner.scanBatchScope(listOf(item1, item2), TtsField.entries.toSet())

        assertEquals(2, scan.totalSelectedItems)
        // Item1: ANSWER (missing), TRANSLATION (missing). QUESTION (existing -> skip), EXAMPLE (existing -> skip)
        // Item2: QUESTION (missing), ANSWER (missing). EXAMPLE (empty -> skip), TRANSLATION (empty -> skip)
        // Total valid targets = 2 + 2 = 4
        assertEquals(4, scan.totalValidTargets)
        assertEquals(2, scan.existingAudioSkippedCount)
        assertEquals(2, scan.emptyTextSkippedCount)
        assertEquals(3, scan.englishTargetsCount) // item1 ANSWER, item2 QUESTION, item2 ANSWER
        assertEquals(1, scan.vietnameseTargetsCount) // item1 TRANSLATION
    }

    @Test
    fun `scanTargets skips blank text fields even when audio is missing`() {
        val item = createBrowserItem(
            q = "word",
            a = "", // Blank answer
            ex = "   ", // Blank example
            tr = "từ"
        )

        val targets = BatchTtsScanner.scanTargets(listOf(item), missingOnly = true)

        assertEquals(2, targets.size)
        assertTrue(targets.any { it.field == TtsField.QUESTION })
        assertTrue(targets.any { it.field == TtsField.TRANSLATION })
        assertFalse(targets.any { it.field == TtsField.ANSWER })
        assertFalse(targets.any { it.field == TtsField.EXAMPLE })
    }

    @Test
    fun `buildJobs maps targets to executable jobs with assigned voices and rates`() {
        val item = createBrowserItem(
            id = "c1",
            q = "apple",
            a = "fruit",
            ex = "An apple a day",
            tr = "quả táo"
        )

        val targets = BatchTtsScanner.scanTargets(listOf(item), missingOnly = true)
        val jobs = BatchTtsScanner.buildJobs(
            targets = targets,
            englishVoice = enVoice,
            vietnameseVoice = viVoice,
            englishRate = 5,
            vietnameseRate = -5
        )

        assertEquals(4, jobs.size)

        val qJob = jobs.first { it.field == TtsField.QUESTION }
        assertEquals("c1", qJob.contentId)
        assertEquals("apple", qJob.text)
        assertEquals(TtsLanguage.ENGLISH, qJob.language)
        assertEquals(enVoice.id, qJob.voice.id)
        assertEquals(5, qJob.rate)

        val trJob = jobs.first { it.field == TtsField.TRANSLATION }
        assertEquals("c1", trJob.contentId)
        assertEquals("quả táo", trJob.text)
        assertEquals(TtsLanguage.VIETNAMESE, trJob.language)
        assertEquals(viVoice.id, trJob.voice.id)
        assertEquals(-5, trJob.rate)
    }

    @Test
    fun `strategy jobs preserve batch pitch and volume for each language`() {
        val targets = BatchTtsScanner.scanTargets(listOf(createBrowserItem(
            id = "c-config", q = "word", a = "answer", ex = "example", tr = "nghĩa"
        )), missingOnly = true)
        val jobs = BatchTtsScanner.buildJobsWithStrategy(
            targets,
            vn.loi.learning.desktop.tts.strategy.VoiceStrategyConfig(
                vn.loi.learning.desktop.tts.strategy.VoiceStrategyMode.SINGLE_VOICE, enVoice
            ),
            vn.loi.learning.desktop.tts.strategy.VoiceStrategyConfig(
                vn.loi.learning.desktop.tts.strategy.VoiceStrategyMode.SINGLE_VOICE, viVoice
            ),
            englishRate = 25,
            vietnameseRate = -10,
            englishPitch = "+5Hz",
            vietnamesePitch = "-3Hz",
            englishVolume = "+8%",
            vietnameseVolume = "-4%"
        )
        assertTrue(jobs.filter { it.language == TtsLanguage.ENGLISH }.all {
            it.rate == 25 && it.pitch == "+5Hz" && it.volume == "+8%"
        })
        assertTrue(jobs.filter { it.language == TtsLanguage.VIETNAMESE }.all {
            it.rate == -10 && it.pitch == "-3Hz" && it.volume == "-4%"
        })
    }
}
