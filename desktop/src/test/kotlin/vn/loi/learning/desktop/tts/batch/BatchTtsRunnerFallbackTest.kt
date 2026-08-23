package vn.loi.learning.desktop.tts.batch

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsEngine
import vn.loi.learning.desktop.tts.TtsError
import vn.loi.learning.desktop.tts.TtsException
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsPreviewStore
import vn.loi.learning.desktop.tts.TtsSynthesisRequest
import vn.loi.learning.desktop.tts.TtsSynthesisResult
import vn.loi.learning.desktop.tts.TtsVoice

@OptIn(ExperimentalCoroutinesApi::class)
class BatchTtsRunnerFallbackTest {

    private val ava = TtsVoice("en-US-AvaMultilingualNeural", "Ava", "en-US", "en", "Female")
    private val jenny = TtsVoice("en-US-JennyNeural", "Jenny", "en-US", "en", "Female")
    private val guy = TtsVoice("en-US-GuyNeural", "Guy", "en-US", "en", "Male")

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

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

    private class MockTtsEngine(
        val failVoices: Set<String> = emptySet()
    ) : TtsEngine {
        override suspend fun listVoices(): List<TtsVoice> = emptyList()

        override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
            if (request.voice.id in failVoices) {
                throw TtsException(TtsError.Timeout)
            }
            Files.createDirectories(outputFile.parent)
            Files.write(outputFile, byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00))
            return TtsSynthesisResult(outputFile, 4)
        }
    }

    @Test
    fun `Fallback chain recovers when primary voice fails`() = testScope.runTest {
        val tempDir = Files.createTempDirectory("tts_runner_fb_test")

        try {
            // Ava fails, Jenny succeeds
            val engine = MockTtsEngine(failVoices = setOf(ava.id))
            val service = DesktopTtsAudioService(
                ttsEngine = engine,
                mediaStorage = FakeStorage(tempDir),
                previewStore = TtsPreviewStore(tempDir)
            )
            val runner = BatchTtsRunner(service, this)

            val job = BatchTtsJob(
                contentId = "c1",
                field = TtsField.QUESTION,
                text = "apple",
                language = TtsLanguage.ENGLISH,
                voice = ava,
                candidateVoices = listOf(ava, jenny, guy)
            )

            var finalSummary: BatchTtsSummary? = null
            val runningJob = runner.runBatch(
                jobs = listOf(job),
                packageName = "test_pkg",
                onApply = null,
                onProgress = { finalSummary = it }
            )

            runningJob.join()

            val summary = finalSummary
            assertNotNull(summary)
            assertEquals(1, summary.totalJobs)
            assertEquals(1, summary.successCount)
            assertEquals(0, summary.failedCount)
            assertEquals(1, summary.fallbackRecoveredCount)

            val res = summary.jobResults.first()
            assertEquals(BatchTtsJobStatus.SUCCESS, res.status)
            assertTrue(res.recoveredViaFallback)
            assertEquals(ava.id, res.requestedVoice.id)
            assertEquals(jenny.id, res.actualVoiceUsed?.id)
            assertEquals(3, res.attempts.size)
            assertFalse(res.attempts[0].isSuccess)
            assertEquals(ava.id, res.attempts[0].voice.id)
            assertTrue(res.attempts[2].isSuccess)
            assertEquals(jenny.id, res.attempts[2].voice.id)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `When all fallback attempts fail target is marked as FAILED with attempt history`() = testScope.runTest {
        val tempDir = Files.createTempDirectory("tts_runner_all_fail_test")

        try {
            // Both Ava and Jenny fail
            val engine = MockTtsEngine(failVoices = setOf(ava.id, jenny.id))
            val service = DesktopTtsAudioService(
                ttsEngine = engine,
                mediaStorage = FakeStorage(tempDir),
                previewStore = TtsPreviewStore(tempDir)
            )
            val runner = BatchTtsRunner(service, this)

            val job = BatchTtsJob(
                contentId = "c2",
                field = TtsField.ANSWER,
                text = "banana",
                language = TtsLanguage.ENGLISH,
                voice = ava,
                candidateVoices = listOf(ava, jenny)
            )

            var finalSummary: BatchTtsSummary? = null
            val runningJob = runner.runBatch(
                jobs = listOf(job),
                packageName = "test_pkg",
                onProgress = { finalSummary = it }
            )

            runningJob.join()

            val summary = finalSummary
            assertNotNull(summary)
            assertEquals(1, summary.totalJobs)
            assertEquals(0, summary.successCount)
            assertEquals(1, summary.failedCount)
            assertEquals(0, summary.fallbackRecoveredCount)

            val res = summary.jobResults.first()
            assertEquals(BatchTtsJobStatus.FAILED, res.status)
            assertNull(res.actualVoiceUsed)
            assertEquals(4, res.attempts.size)
            assertFalse(res.recoveredViaFallback)
            assertEquals(TtsErrorCategory.TIMEOUT, res.errorCategory)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Cancellation stops runner and marks remaining jobs as CANCELLED`() = testScope.runTest {
        val tempDir = Files.createTempDirectory("tts_runner_cancel_test")

        try {
            val engine = MockTtsEngine()
            val service = DesktopTtsAudioService(
                ttsEngine = engine,
                mediaStorage = FakeStorage(tempDir),
                previewStore = TtsPreviewStore(tempDir)
            )
            val runner = BatchTtsRunner(service, this)

            val jobs = listOf(
                BatchTtsJob("c1", TtsField.QUESTION, "one", TtsLanguage.ENGLISH, ava),
                BatchTtsJob("c2", TtsField.QUESTION, "two", TtsLanguage.ENGLISH, ava),
                BatchTtsJob("c3", TtsField.QUESTION, "three", TtsLanguage.ENGLISH, ava)
            )

            var finalSummary: BatchTtsSummary? = null
            val runningJob = runner.runBatch(
                jobs = jobs,
                packageName = "test_pkg",
                onProgress = {
                    finalSummary = it
                    if (it.completedJobs == 1) {
                        runner.cancel()
                    }
                }
            )

            runningJob.join()

            val summary = finalSummary
            assertNotNull(summary)
            assertTrue(summary.isCancelled)
            assertEquals(1, summary.successCount)
            assertEquals(2, summary.cancelledCount)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Non-retryable invalid text error does not attempt subsequent fallback voices`() = testScope.runTest {
        val tempDir = Files.createTempDirectory("tts_runner_invalid_text_test")

        try {
            // Engine fails with non-retryable InvalidText
            val engine = object : TtsEngine {
                var callCount = 0
                override suspend fun listVoices(): List<TtsVoice> = emptyList()
                override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                    callCount++
                    throw TtsException(TtsError.InvalidText("Invalid characters"))
                }
            }
            val service = DesktopTtsAudioService(
                ttsEngine = engine,
                mediaStorage = FakeStorage(tempDir),
                previewStore = TtsPreviewStore(tempDir)
            )
            val runner = BatchTtsRunner(service, this)

            val job = BatchTtsJob(
                contentId = "c1",
                field = TtsField.QUESTION,
                text = "bad_text",
                language = TtsLanguage.ENGLISH,
                voice = ava,
                candidateVoices = listOf(ava, jenny, guy) // 3 candidates
            )

            var finalSummary: BatchTtsSummary? = null
            val runningJob = runner.runBatch(
                jobs = listOf(job),
                packageName = "test_pkg",
                onProgress = { finalSummary = it }
            )
            runningJob.join()

            val summary = finalSummary
            assertNotNull(summary)
            assertEquals(1, summary.failedCount)
            // Call count should be exactly 1, because InvalidText is non-retryable and should NOT try jenny and guy!
            assertEquals(1, engine.callCount)
            val result = summary.failedResults.first()
            assertEquals(1, result.voiceAttempts.size)
            assertEquals(TtsErrorCategory.INVALID_TEXT, result.errorCategory)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
