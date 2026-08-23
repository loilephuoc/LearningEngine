package vn.loi.learning.desktop.tts.batch

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsEngine
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsSynthesisRequest
import vn.loi.learning.desktop.tts.TtsSynthesisResult
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyConfig
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyMode
import vn.loi.learning.domain.content.model.ContentId
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BatchTtsHotfix4ComprehensiveTest {

    @TempDir
    lateinit var tempDir: Path

    private val enVoiceAva = TtsVoice("en-US-AvaMultilingualNeural", "Microsoft Ava", "en-US", "en", "Female")
    private val enVoiceAndrew = TtsVoice("en-US-AndrewMultilingualNeural", "Microsoft Andrew", "en-US", "en", "Male")
    private val enVoiceSonia = TtsVoice("en-GB-SoniaNeural", "Microsoft Sonia", "en-GB", "en", "Female")
    private val viVoiceHoaiMy = TtsVoice("vi-VN-HoaiMyNeural", "Microsoft HoaiMy", "vi-VN", "vi", "Female")
    private val viVoiceNamMinh = TtsVoice("vi-VN-NamMinhNeural", "Microsoft NamMinh", "vi-VN", "vi", "Male")

    private class TestStorage(private val root: Path) : ContentMediaStorage {
        override fun store(packageName: String, fileName: String, content: ByteArray): ContentMediaAsset {
            val relPath = "$packageName/$fileName"
            val dest = root.resolve(relPath)
            Files.createDirectories(dest.parent)
            Files.write(dest, content)
            return ContentMediaAsset(packageName, fileName, relPath)
        }

        override fun resolve(relativePath: String): Path? {
            val path = root.resolve(relativePath)
            return if (Files.exists(path)) path else null
        }

        override fun exists(relativePath: String): Boolean {
            return Files.exists(root.resolve(relativePath))
        }
    }

    private fun createVocabItem(
        id: String = "vocab-1",
        q: String = "ache",
        a: String = "Mặc dù, tuy nhiên.",
        ex: String = "My tooth aches.",
        tr: String = "Răng tôi bị đau.",
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
            pronunciation = "/eɪk/",
            partOfSpeech = "VERB",
            group = "Vocabulary",
            section = "Health",
            lesson = "Unit 1",
            packageName = "Vocabulary_In_Use_Upper_Intermediate",
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
            learningItemCount = 1,
            learningItemIds = emptyList(),
            learningModes = emptyList(),
            tags = emptySet(),
            searchableText = "$q $a"
        )

    @Test
    fun `field language resolution correctly classifies Vocabulary_In_Use_Upper_Intermediate fields`() {
        val item = createVocabItem()
        val scan = BatchTtsScanner.scanBatchScope(listOf(item), TtsField.entries.toSet())

        assertEquals(4, scan.totalValidTargets)
        assertEquals(2, scan.englishTargetsCount) // Question + Example
        assertEquals(2, scan.vietnameseTargetsCount) // Answer + Translation

        val qTarget = scan.validTargets.first { it.field == TtsField.QUESTION }
        val aTarget = scan.validTargets.first { it.field == TtsField.ANSWER }
        val exTarget = scan.validTargets.first { it.field == TtsField.EXAMPLE }
        val trTarget = scan.validTargets.first { it.field == TtsField.TRANSLATION }

        assertEquals(TtsLanguage.ENGLISH, qTarget.language)
        assertEquals(TtsLanguage.VIETNAMESE, aTarget.language)
        assertEquals(TtsLanguage.ENGLISH, exTarget.language)
        assertEquals(TtsLanguage.VIETNAMESE, trTarget.language)

        assertEquals("ache", scan.representativeEnglishText)
        assertEquals("Mặc dù, tuy nhiên.", scan.representativeVietnameseText)
    }

    @Test
    fun `sample extraction prefers missing target over existing audio when overwrite is off`() {
        val item1 = createVocabItem(
            id = "v-1",
            q = "ache",
            qAudio = "existing-ache.mp3", // Already has Question audio
            ex = "My tooth aches."
        )
        val item2 = createVocabItem(
            id = "v-2",
            q = "fever",
            qAudio = null, // Missing Question audio
            ex = "She has a high fever."
        )

        // When scanning only QUESTION (overwrite = false)
        val scan = BatchTtsScanner.scanBatchScope(listOf(item1, item2), setOf(TtsField.QUESTION), overwriteExisting = false)

        assertEquals(1, scan.totalValidTargets)
        assertEquals("v-2", scan.validTargets.single().contentId)

        // The question sample MUST come from item2 (the missing target), not item1
        val qSample = scan.sampleFor(TtsField.QUESTION)
        assertEquals("fever", qSample?.text)
        assertEquals("v-2", qSample?.contentId)
    }

    @Test
    fun `planner rejects incompatible candidate voices for target language`() {
        val item = createVocabItem()
        val scan = BatchTtsScanner.scanBatchScope(listOf(item), setOf(TtsField.ANSWER))
        val targets = scan.validTargets // Answer is VIETNAMESE

        // Attempting to build jobs with an English voice for Vietnamese target throws exception
        assertFailsWith<IllegalArgumentException> {
            BatchTtsScanner.buildJobsWithStrategy(
                targets = targets,
                englishStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, enVoiceAva),
                vietnameseStrategy = VoiceStrategyConfig(VoiceStrategyMode.SINGLE_VOICE, enVoiceAva) // Mismatched!
            )
        }
    }

    @Test
    fun `runner validates voice language defense-in-depth and succeeds with correct multi-voice pool`() = runBlocking {
        val item = createVocabItem()
        val scan = BatchTtsScanner.scanBatchScope(listOf(item), setOf(TtsField.QUESTION, TtsField.ANSWER))

        val enStrategy = VoiceStrategyConfig(VoiceStrategyMode.FALLBACK_CHAIN, enVoiceAva, listOf(enVoiceAndrew, enVoiceSonia))
        val viStrategy = VoiceStrategyConfig(VoiceStrategyMode.FALLBACK_CHAIN, viVoiceHoaiMy, listOf(viVoiceNamMinh))

        val jobs = BatchTtsScanner.buildJobsWithStrategy(
            targets = scan.validTargets,
            englishStrategy = enStrategy,
            vietnameseStrategy = viStrategy
        )

        assertEquals(2, jobs.size)
        val qJob = jobs.first { it.field == TtsField.QUESTION }
        val aJob = jobs.first { it.field == TtsField.ANSWER }

        assertEquals(TtsLanguage.ENGLISH, qJob.language)
        assertEquals("en-US-AvaMultilingualNeural", qJob.voice.id)
        assertEquals(3, qJob.candidateVoices.size)

        assertEquals(TtsLanguage.VIETNAMESE, aJob.language)
        assertEquals("vi-VN-HoaiMyNeural", aJob.voice.id)
        assertEquals(2, aJob.candidateVoices.size)

        val storage = TestStorage(tempDir)
        val engine = object : TtsEngine {
            override suspend fun listVoices(): List<TtsVoice> = listOf(enVoiceAva, enVoiceAndrew, enVoiceSonia, viVoiceHoaiMy, viVoiceNamMinh)
            override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                val fakeMp3 = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x44.toByte())
                Files.write(outputFile, fakeMp3)
                return TtsSynthesisResult(outputFile, 4L)
            }
        }
        val ttsService = DesktopTtsAudioService(engine, storage)
        val checkpointStore = BatchTtsCheckpointStore(tempDir.resolve("ckpt.json"))

        var finalSummary: BatchTtsSummary? = null
        val runner = BatchTtsRunner(
            ttsService = ttsService,
            scope = this,
            policy = BatchTtsExecutionPolicy(attemptTimeoutMillis = 1000L, maxAttemptsPerVoice = 1),
            checkpointStore = checkpointStore
        )

        val job = runner.runBatch(
            jobs = jobs,
            packageName = "Vocabulary_In_Use_Upper_Intermediate",
            onProgress = { finalSummary = it }
        )
        job.join()

        assertNotNull(finalSummary)
        assertTrue(finalSummary!!.isFinished)
        assertFalse(finalSummary!!.isCancelled)
        assertEquals(2, finalSummary!!.totalJobs)
        assertEquals(2, finalSummary!!.completedJobs)
        assertEquals(2, finalSummary!!.successCount)
        assertEquals(0, finalSummary!!.failedCount)
    }
}
