package vn.loi.learning.desktop.tts.batch

import java.net.SocketTimeoutException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsAudioFileNamer
import vn.loi.learning.desktop.tts.TtsEngine
import vn.loi.learning.desktop.tts.TtsError
import vn.loi.learning.desktop.tts.TtsException
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsPreviewStore
import vn.loi.learning.desktop.tts.TtsSynthesisRequest
import vn.loi.learning.desktop.tts.TtsSynthesisResult
import vn.loi.learning.desktop.tts.TtsVoice
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BatchTtsRunnerTest {

    private val voiceEn = TtsVoice("en-US-Ava", "Ava", "en-US", "en", "Female")
    private val voiceVi = TtsVoice("vi-VN-HoaiMy", "HoaiMy", "vi-VN", "vi", "Female")

    private lateinit var tempMediaDir: Path
    private lateinit var tempPreviewDir: Path
    private lateinit var fakeStorage: FakeStorage
    private lateinit var fakeEngine: ConfigurableFakeEngine
    private lateinit var ttsService: DesktopTtsAudioService

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeTest
    fun setup() {
        tempMediaDir = Files.createTempDirectory("batch-runner-media-")
        tempPreviewDir = Files.createTempDirectory("batch-runner-preview-")
        fakeStorage = FakeStorage(tempMediaDir)
        fakeEngine = ConfigurableFakeEngine()
        ttsService = DesktopTtsAudioService(
            ttsEngine = fakeEngine,
            mediaStorage = fakeStorage,
            fileNamer = TtsAudioFileNamer(Clock.fixed(Instant.parse("2026-08-22T05:00:00Z"), ZoneOffset.UTC)),
            previewStore = TtsPreviewStore(tempPreviewDir)
        )
    }

    @AfterTest
    fun tearDown() {
        ttsService.close()
        tempMediaDir.toFile().deleteRecursively()
        tempPreviewDir.toFile().deleteRecursively()
    }

    @Test
    fun `runBatch executes all jobs sequentially and reports accurate progress`() = testScope.runTest {
        val runner = BatchTtsRunner(ttsService, this)
        val appliedRecords = mutableListOf<Triple<String, TtsField, String>>()

        val jobs = listOf(
            BatchTtsJob(contentId = "c1", field = TtsField.QUESTION, text = "apple", language = TtsLanguage.ENGLISH, voice = voiceEn, id = "j1"),
            BatchTtsJob(contentId = "c1", field = TtsField.ANSWER, text = "fruit", language = TtsLanguage.ENGLISH, voice = voiceEn, id = "j2"),
            BatchTtsJob(contentId = "c1", field = TtsField.TRANSLATION, text = "quả táo", language = TtsLanguage.VIETNAMESE, voice = voiceVi, id = "j3")
        )

        var finalSummary: BatchTtsSummary? = null

        val job = runner.runBatch(
            jobs = jobs,
            packageName = "pkg_test",
            onApply = { cid, field, ref -> appliedRecords.add(Triple(cid, field, ref)) },
            onProgress = { summary -> finalSummary = summary }
        )

        job.join()

        assertNotNull(finalSummary)
        val summary = finalSummary!!
        assertEquals(3, summary.totalJobs)
        assertEquals(3, summary.completedJobs)
        assertEquals(3, summary.successCount)
        assertEquals(0, summary.failedCount)
        assertEquals(0, summary.cancelledCount)
        assertTrue(summary.isFinished)
        assertFalse(summary.isCancelled)

        // Applied exactly 3 items
        assertEquals(3, appliedRecords.size)
        assertEquals("c1", appliedRecords[0].first)
        assertEquals(TtsField.QUESTION, appliedRecords[0].second)
        assertEquals("c1", appliedRecords[1].first)
        assertEquals(TtsField.ANSWER, appliedRecords[1].second)
        assertEquals("c1", appliedRecords[2].first)
        assertEquals(TtsField.TRANSLATION, appliedRecords[2].second)
    }

    @Test
    fun `error isolation ensures single failure does not abort remaining batch jobs`() = testScope.runTest {
        val runner = BatchTtsRunner(ttsService, this)
        val appliedRecords = mutableListOf<Triple<String, TtsField, String>>()

        // Configure engine to fail on the 2nd job
        fakeEngine.failOnTexts.add("corrupted_text")

        val jobs = listOf(
            BatchTtsJob(contentId = "c1", field = TtsField.QUESTION, text = "good_text_1", language = TtsLanguage.ENGLISH, voice = voiceEn, id = "j1"),
            BatchTtsJob(contentId = "c2", field = TtsField.QUESTION, text = "corrupted_text", language = TtsLanguage.ENGLISH, voice = voiceEn, id = "j2"),
            BatchTtsJob(contentId = "c3", field = TtsField.QUESTION, text = "good_text_2", language = TtsLanguage.ENGLISH, voice = voiceEn, id = "j3")
        )

        var finalSummary: BatchTtsSummary? = null

        val job = runner.runBatch(
            jobs = jobs,
            packageName = "pkg_test",
            onApply = { cid, field, ref -> appliedRecords.add(Triple(cid, field, ref)) },
            onProgress = { summary -> finalSummary = summary }
        )

        job.join()

        assertNotNull(finalSummary)
        val summary = finalSummary!!
        assertEquals(3, summary.totalJobs)
        assertEquals(3, summary.completedJobs)
        assertEquals(2, summary.successCount)
        assertEquals(1, summary.failedCount)
        assertEquals(0, summary.cancelledCount)

        // Only the 2 successful items are applied
        assertEquals(2, appliedRecords.size)
        assertEquals("c1", appliedRecords[0].first)
        assertEquals("c3", appliedRecords[1].first)

        // Failed item is captured in jobResults
        assertEquals(1, summary.failedResults.size)
        val failedResult = summary.failedResults.first()
        assertEquals("c2", failedResult.job.contentId)
        assertEquals(BatchTtsJobStatus.FAILED, failedResult.status)
        assertEquals(TtsErrorCategory.GENERATION_FAILED, failedResult.errorCategory)
    }

    @Test
    fun `classifyError maps various exceptions to friendly categories`() {
        val timeoutEx = SocketTimeoutException("Read timed out")
        val (cat1, _) = BatchTtsRunner.classifyError(timeoutEx)
        assertEquals(TtsErrorCategory.TIMEOUT, cat1)

        val netEx = TtsException(TtsError.NoNetwork)
        val (cat2, msg2) = BatchTtsRunner.classifyError(netEx)
        assertEquals(TtsErrorCategory.NETWORK_UNAVAILABLE, cat2)
        assertEquals("Network connection unavailable", msg2)

        val voiceEx = TtsException(TtsError.VoiceUnavailable("invalid-voice"))
        val (cat3, _) = BatchTtsRunner.classifyError(voiceEx)
        assertEquals(TtsErrorCategory.VOICE_UNAVAILABLE, cat3)
    }

    @Test
    fun `retry failed only re-runs failed jobs and preserves previous successful results`() = testScope.runTest {
        val runner = BatchTtsRunner(ttsService, this)
        val appliedRecords = mutableListOf<Triple<String, TtsField, String>>()

        fakeEngine.failOnTexts.add("transient_failure")

        val initialJobs = listOf(
            BatchTtsJob(contentId = "c1", field = TtsField.QUESTION, text = "text1", language = TtsLanguage.ENGLISH, voice = voiceEn, id = "j1"),
            BatchTtsJob(contentId = "c2", field = TtsField.QUESTION, text = "transient_failure", language = TtsLanguage.ENGLISH, voice = voiceEn, id = "j2")
        )

        var summary1: BatchTtsSummary? = null
        val job1 = runner.runBatch(
            jobs = initialJobs,
            packageName = "pkg",
            onApply = { cid, field, ref -> appliedRecords.add(Triple(cid, field, ref)) },
            onProgress = { summary1 = it }
        )
        job1.join()

        assertEquals(1, summary1?.successCount)
        assertEquals(1, summary1?.failedCount)
        assertEquals(1, appliedRecords.size)

        // Clear transient failure to simulate recovery
        fakeEngine.failOnTexts.clear()

        val failedJobsToRetry = summary1!!.failedResults.map { it.job }
        assertEquals(1, failedJobsToRetry.size)

        var summary2: BatchTtsSummary? = null
        val job2 = runner.runBatch(
            jobs = failedJobsToRetry,
            packageName = "pkg",
            onApply = { cid, field, ref -> appliedRecords.add(Triple(cid, field, ref)) },
            onProgress = { summary2 = it }
        )
        job2.join()

        assertEquals(1, summary2?.successCount)
        assertEquals(0, summary2?.failedCount)
        // Now total 2 applied items!
        assertEquals(2, appliedRecords.size)
        assertEquals("c2", appliedRecords[1].first)
    }

    private class ConfigurableFakeEngine : TtsEngine {
        val failOnTexts = mutableSetOf<String>()

        override suspend fun listVoices(): List<TtsVoice> = emptyList()

        override suspend fun synthesize(request: TtsSynthesisRequest, targetFile: Path): TtsSynthesisResult {
            if (request.text in failOnTexts) {
                throw TtsException(TtsError.GenerationFailed("Simulated failure for '${request.text}'"))
            }
            Files.createDirectories(targetFile.parent)
            Files.write(targetFile, byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00))
            return TtsSynthesisResult(targetFile, 4)
        }
    }

    private class FakeStorage(private val root: Path) : ContentMediaStorage {
        override fun store(packageName: String, fileName: String, content: ByteArray): ContentMediaAsset {
            val relPath = "$packageName/$fileName"
            val dest = root.resolve(relPath)
            Files.createDirectories(dest.parent)
            Files.write(dest, content)
            return ContentMediaAsset(packageName, fileName, relPath)
        }

        override fun storeStream(packageName: String, fileName: String, source: Path): ContentMediaAsset =
            store(packageName, fileName, Files.readAllBytes(source))

        override fun resolve(relativePath: String): Path? {
            val path = root.resolve(relativePath)
            return if (Files.exists(path)) path else null
        }

        override fun exists(relativePath: String): Boolean =
            Files.exists(root.resolve(relativePath))
    }
}
