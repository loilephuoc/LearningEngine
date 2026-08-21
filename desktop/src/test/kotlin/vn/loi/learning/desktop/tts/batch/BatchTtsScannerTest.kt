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
    fun `scanTargets identifies missing fields and protects existing audio`() {
        val item = createBrowserItem(
            q = "genuine",
            a = "authentic",
            ex = "Genuine leather",
            tr = "Da thật",
            qAudio = "pkg/q.mp3", // Already has audio!
            aAudio = null,
            exAudio = null,
            trAudio = null
        )

        val targets = BatchTtsScanner.scanTargets(listOf(item), missingOnly = true)

        // Question must be skipped because it already has audio!
        assertEquals(3, targets.size)
        assertFalse(targets.any { it.field == TtsField.QUESTION })
        assertTrue(targets.any { it.field == TtsField.ANSWER })
        assertTrue(targets.any { it.field == TtsField.EXAMPLE })
        assertTrue(targets.any { it.field == TtsField.TRANSLATION })
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
}
