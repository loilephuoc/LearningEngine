package vn.loi.learning.desktop.tts.batch

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsEngine
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsPreviewStore
import vn.loi.learning.desktop.tts.TtsSynthesisRequest
import vn.loi.learning.desktop.tts.TtsSynthesisResult
import vn.loi.learning.desktop.tts.TtsVoice

@OptIn(ExperimentalCoroutinesApi::class)
class BatchTtsHotfix2ComprehensiveTest {

    private val voicePrimary = TtsVoice("vi-VN-HoaiMyNeural", "HoaiMy", "vi-VN", "vi", "Female")
    private val voiceFallback = TtsVoice("vi-VN-NamMinhNeural", "NamMinh", "vi-VN", "vi", "Male")

    private class RecordingEventLogger : BatchTtsEventLogger {
        val events = mutableListOf<String>()
        val voiceHealthEvents = mutableListOf<String>()

        override fun logPlanCreated(planId: String, packageId: String, targetCount: Int, overwriteMode: Boolean, selectedFields: String, languageRequirements: String) {
            events += "PLAN_CREATED:$planId"
        }
        override fun logLeaseAcquireAttempt(planId: String, ownerPid: Long, ownerToken: String, existingOwnerPid: Long?, existingOwnerAlive: Boolean?, decision: String) {}
        override fun logLeaseReclaimed(planId: String, deadOwnerPid: Long, leaseAgeMillis: Long) {}
        override fun logResume(planId: String, successCount: Int, failedCount: Int, skippedCount: Int, pendingCount: Int) {
            events += "RESUME:success=$successCount,pending=$pendingCount"
        }
        override fun logTargetStart(planId: String, targetIndex: Int, jobId: String, contentId: String, field: String, language: String) {
            events += "TARGET_START:$targetIndex"
        }
        override fun logAttemptStart(targetIndex: Int, voiceId: String, attemptNumber: Int, candidateIndex: Int) {
            events += "ATTEMPT_START:$targetIndex:$voiceId:$attemptNumber"
        }
        override fun logAttemptTimeout(targetIndex: Int, voiceId: String, attemptNumber: Int, candidateIndex: Int, timeoutMillis: Long) {
            events += "ATTEMPT_TIMEOUT:$targetIndex:$voiceId"
        }
        override fun logAttemptFailure(targetIndex: Int, voiceId: String, attemptNumber: Int, candidateIndex: Int, errorCategory: String, reason: String) {
            events += "ATTEMPT_FAILURE:$targetIndex:$voiceId:$errorCategory"
        }
        override fun logAttemptClosed(targetIndex: Int, voiceId: String, attemptNumber: Int, candidateIndex: Int, details: String) {}
        override fun logAttemptAbandoned(targetIndex: Int, voiceId: String, attemptNumber: Int, reason: String) {
            events += "ATTEMPT_ABANDONED:$voiceId"
        }
        override fun logWorkerPoolRotated(abandonedWorkerCount: Int, newPoolId: String) {}
        override fun logFallback(targetIndex: Int, fromVoiceId: String, toVoiceId: String, candidateIndex: Int) {
            events += "FALLBACK:$targetIndex:$fromVoiceId->$toVoiceId"
        }
        override fun logVoiceHealthChanged(voiceId: String, oldState: String, newState: String, reason: String) {
            voiceHealthEvents += "HEALTH_CHANGED:$voiceId:$oldState->$newState"
        }
        override fun logVoiceCircuitOpen(voiceId: String, consecutiveFailures: Int, cooldownMillis: Long) {
            voiceHealthEvents += "CIRCUIT_OPEN:$voiceId:failures=$consecutiveFailures"
        }
        override fun logVoiceCircuitProbe(voiceId: String, targetIndex: Int) {
            voiceHealthEvents += "CIRCUIT_PROBE:$voiceId:$targetIndex"
        }
        override fun logTargetSuccess(planId: String, targetIndex: Int, jobId: String, voiceId: String, assetPath: String, recoveredViaFallback: Boolean) {
            events += "TARGET_SUCCESS:$targetIndex:$voiceId:fallback=$recoveredViaFallback"
        }
        override fun logTargetFailed(planId: String, targetIndex: Int, jobId: String, errorCategory: String, attemptsCount: Int) {
            events += "TARGET_FAILED:$targetIndex:$errorCategory"
        }
        override fun logTargetSkipped(planId: String, targetIndex: Int, jobId: String, reason: String) {}
        override fun logTargetCancelled(planId: String, targetIndex: Int, jobId: String) {
            events += "TARGET_CANCELLED:$targetIndex"
        }
        override fun logCancelRequested(planId: String, currentTargetIndex: Int) {
            events += "CANCEL_REQUESTED:$currentTargetIndex"
        }
        override fun logCancelAcknowledged(planId: String, currentTargetIndex: Int) {
            events += "CANCEL_ACKNOWLEDGED:$currentTargetIndex"
        }
        override fun logCheckpointWritten(planId: String, targetIndex: Int, recordsCount: Int) {}
        override fun logBatchCompleted(planId: String, totalCount: Int, successCount: Int, failedCount: Int, skippedCount: Int, cancelledCount: Int) {
            events += "BATCH_COMPLETED:success=$successCount,failed=$failedCount,cancelled=$cancelledCount"
        }
        override fun logBatchCancelled(planId: String, completedCount: Int, cancelledCount: Int) {
            events += "BATCH_CANCELLED:completed=$completedCount,cancelled=$cancelledCount"
        }
        override fun logBatchFatal(planId: String, reason: String) {
            events += "BATCH_FATAL:$reason"
        }
    }

    private class TestStorage(private val root: Path) : ContentMediaStorage {
        override fun store(packageName: String, fileName: String, content: ByteArray): ContentMediaAsset {
            val relPath = "$packageName/$fileName"
            val dest = root.resolve(relPath)
            Files.createDirectories(dest.parent)
            Files.write(dest, content)
            return ContentMediaAsset(packageName, fileName, relPath)
        }
        override fun storeStream(packageName: String, fileName: String, source: Path) = store(packageName, fileName, Files.readAllBytes(source))
        override fun resolve(relativePath: String): Path? = root.resolve(relativePath).takeIf(Files::exists)
        override fun exists(relativePath: String) = Files.exists(root.resolve(relativePath))
    }

    // 1. Test fast cancellation during hanging synthesis
    @Test
    fun testCancelWhileSynthesisHangsTerminatesPromptlyWithoutFatal() = runBlocking {
        val tempDir = Files.createTempDirectory("batch-tts-cancel-test")
        val logger = RecordingEventLogger()
        val storage = TestStorage(tempDir)

        val uncooperativeEngine = object : TtsEngine {
            override suspend fun listVoices(): List<TtsVoice> = listOf(voicePrimary, voiceFallback)
            override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                // Hang indefinitely
                delay(120_000L)
                error("Should not return")
            }
        }
        val ttsService = DesktopTtsAudioService(uncooperativeEngine, storage)
        val checkpointStore = BatchTtsCheckpointStore(tempDir.resolve("ckpt.json"))

        val jobs = (1..5).map { idx ->
            BatchTtsJob(
                contentId = "card_$idx",
                field = TtsField.TRANSLATION,
                text = "Câu $idx",
                language = TtsLanguage.VIETNAMESE,
                voice = voicePrimary,
                candidateVoices = listOf(voicePrimary, voiceFallback)
            )
        }

        var latestSummary: BatchTtsSummary? = null
        val runner = BatchTtsRunner(
            ttsService = ttsService,
            scope = this,
            policy = BatchTtsExecutionPolicy(attemptTimeoutMillis = 5_000L, maxAttemptsPerVoice = 1),
            checkpointStore = checkpointStore,
            eventLogger = logger
        )

        val startTime = System.currentTimeMillis()
        val job = runner.runBatch(
            jobs = jobs,
            packageName = "pkg_test",
            onProgress = { latestSummary = it }
        )

        // Give it 150ms to start target 1
        delay(150L)
        // Click cancel
        runner.cancel()

        job.join()
        val cancelDuration = System.currentTimeMillis() - startTime

        // Cancel must complete promptly without waiting for the 5s/120s timeout
        assertTrue(cancelDuration < 3_000L, "Cancellation took ${cancelDuration}ms, must be < 3000ms")
        assertTrue(runner.isCancelled)
        assertNotNull(latestSummary)
        assertTrue(latestSummary!!.isCancelled)
        assertTrue(latestSummary!!.isFinished)
        assertEquals(5, latestSummary!!.cancelledCount)

        // Event logs verification
        assertTrue(logger.events.any { it.startsWith("CANCEL_REQUESTED") })
        assertTrue(logger.events.any { it.startsWith("CANCEL_ACKNOWLEDGED") })
        assertTrue(logger.events.any { it.startsWith("BATCH_CANCELLED") })
        // Crucial: BATCH_FATAL must NEVER be logged for cancellation
        assertFalse(logger.events.any { it.startsWith("BATCH_FATAL") }, "Cancellation must not log BATCH_FATAL")

        tempDir.toFile().deleteRecursively()
    }

    // 2. Test Voice Health Circuit Breaker routes around broken voice
    @Test
    fun testVoiceHealthCircuitBreakerRoutesToHealthyFallbackImmediately() = runBlocking {
        val tempDir = Files.createTempDirectory("batch-tts-circuit-test")
        val logger = RecordingEventLogger()
        val storage = TestStorage(tempDir)

        val primaryAttemptCounts = AtomicInteger(0)
        val fallbackAttemptCounts = AtomicInteger(0)

        val engine = object : TtsEngine {
            override suspend fun listVoices(): List<TtsVoice> = listOf(voicePrimary, voiceFallback)
            override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                if (request.voice.id == voicePrimary.id) {
                    primaryAttemptCounts.incrementAndGet()
                    // Hang so withTimeout throws TimeoutCancellationException
                    delay(30_000L)
                    error("Hang")
                } else {
                    fallbackAttemptCounts.incrementAndGet()
                    Files.createDirectories(outputFile.parent)
                    Files.writeString(outputFile, "audio_content")
                    return TtsSynthesisResult(outputFile, 13L)
                }
            }
        }

        val ttsService = DesktopTtsAudioService(engine, storage)
        val healthTracker = BatchTtsVoiceHealthTracker(
            circuitOpenCooldownMillis = 60_000L,
            maxConsecutiveHardTimeoutsBeforeOpen = 1,
            eventLogger = logger
        )

        val jobs = (1..4).map { idx ->
            BatchTtsJob(
                contentId = "card_$idx",
                field = TtsField.TRANSLATION,
                text = "Câu $idx",
                language = TtsLanguage.VIETNAMESE,
                voice = voicePrimary,
                candidateVoices = listOf(voicePrimary, voiceFallback)
            )
        }

        var latestSummary: BatchTtsSummary? = null
        val runner = BatchTtsRunner(
            ttsService = ttsService,
            scope = this,
            policy = BatchTtsExecutionPolicy(attemptTimeoutMillis = 200L, maxAttemptsPerVoice = 1),
            healthTracker = healthTracker,
            eventLogger = logger
        )

        val start = System.currentTimeMillis()
        val job = runner.runBatch(
            jobs = jobs,
            packageName = "pkg_circuit",
            onProgress = { latestSummary = it }
        )
        job.join()
        val elapsed = System.currentTimeMillis() - start

        assertNotNull(latestSummary)
        assertEquals(4, latestSummary!!.successCount)
        assertEquals(0, latestSummary!!.failedCount)
        assertEquals(4, latestSummary!!.fallbackRecoveredCount)

        // Primary voice timed out on target 1 (1 attempt), which opened circuit!
        // Target 2, 3, 4 should have bypassed primary voice completely!
        assertEquals(1, primaryAttemptCounts.get(), "Primary voice should only be attempted ONCE before circuit breaker opens")
        assertEquals(4, fallbackAttemptCounts.get(), "Fallback voice should successfully synthesize all 4 targets")

        // Whole batch elapsed time should be small (target 1: 200ms timeout + fallback; target 2-4: immediate fallback)
        assertTrue(elapsed < 2_500L, "Total elapsed ${elapsed}ms should be fast with circuit breaker")

        // Health event assertions
        assertTrue(logger.voiceHealthEvents.any { it.startsWith("CIRCUIT_OPEN:${voicePrimary.id}") })
        tempDir.toFile().deleteRecursively()
    }

    // 3. Test Checkpoint Resume after user cancellation
    @Test
    fun testResumeAfterCancellationSkipsCompletedTargetsAndFinishes() = runBlocking {
        val tempDir = Files.createTempDirectory("batch-tts-resume-test")
        val logger = RecordingEventLogger()
        val storage = TestStorage(tempDir)

        val engine = object : TtsEngine {
            override suspend fun listVoices(): List<TtsVoice> = listOf(voicePrimary)
            override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                Files.createDirectories(outputFile.parent)
                Files.writeString(outputFile, "audio")
                return TtsSynthesisResult(outputFile, 5L)
            }
        }
        val ttsService = DesktopTtsAudioService(engine, storage)
        val ckptFile = tempDir.resolve("checkpoint.json")
        val checkpointStore = BatchTtsCheckpointStore(ckptFile)

        val jobs = (1..6).map { idx ->
            BatchTtsJob(
                contentId = "word_$idx",
                field = TtsField.QUESTION,
                text = "Word $idx",
                language = TtsLanguage.ENGLISH,
                voice = voicePrimary
            )
        }

        // Run batch 1: cancel after 3 items complete
        var completedSoFar = 0
        var batch1Summary: BatchTtsSummary? = null
        var runner1: BatchTtsRunner? = null

        runner1 = BatchTtsRunner(
            ttsService = ttsService,
            scope = this,
            policy = BatchTtsExecutionPolicy(attemptTimeoutMillis = 1_000L),
            checkpointStore = checkpointStore,
            assetExists = { storage.exists(it) },
            eventLogger = logger
        )

        val job1 = runner1.runBatch(
            jobs = jobs,
            packageName = "pkg_resume",
            onProgress = {
                batch1Summary = it
                completedSoFar = it.completedJobs
                if (completedSoFar == 3) {
                    runner1.cancel()
                }
            }
        )
        job1.join()

        val savedCheckpoint = checkpointStore.load()
        assertNotNull(savedCheckpoint)
        val successRecords = savedCheckpoint.records.filter { it.status == BatchTtsJobStatus.SUCCESS.name }
        assertEquals(3, successRecords.size)

        // Run batch 2: resume and complete
        val logger2 = RecordingEventLogger()
        var batch2Summary: BatchTtsSummary? = null
        val runner2 = BatchTtsRunner(
            ttsService = ttsService,
            scope = this,
            policy = BatchTtsExecutionPolicy(attemptTimeoutMillis = 1_000L),
            checkpointStore = checkpointStore,
            assetExists = { storage.exists(it) },
            eventLogger = logger2
        )

        val job2 = runner2.runBatch(
            jobs = jobs,
            packageName = "pkg_resume",
            onProgress = { batch2Summary = it }
        )
        job2.join()

        assertNotNull(batch2Summary)
        assertTrue(batch2Summary!!.isFinished)
        assertEquals(6, batch2Summary!!.successCount)
        assertEquals(0, batch2Summary!!.failedCount)
        assertEquals(0, batch2Summary!!.cancelledCount)

        // Resumed log should indicate 3 were resumed
        assertTrue(logger2.events.any { it.contains("RESUME:success=3") })

        tempDir.toFile().deleteRecursively()
    }

    // 4. Test repeated cancellation is idempotent
    @Test
    fun testRepeatedCancellationIsIdempotent() = runBlocking {
        val tempDir = Files.createTempDirectory("batch-tts-idempotent-cancel")
        val logger = RecordingEventLogger()
        val storage = TestStorage(tempDir)
        val engine = object : TtsEngine {
            override suspend fun listVoices(): List<TtsVoice> = listOf(voicePrimary)
            override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                delay(10_000L)
                error("Hang")
            }
        }
        val ttsService = DesktopTtsAudioService(engine, storage)
        val runner = BatchTtsRunner(
            ttsService = ttsService,
            scope = this,
            policy = BatchTtsExecutionPolicy(attemptTimeoutMillis = 5_000L),
            eventLogger = logger
        )

        val jobs = listOf(
            BatchTtsJob("c1", TtsField.QUESTION, "Hello", TtsLanguage.ENGLISH, voicePrimary)
        )
        val job = runner.runBatch(jobs, "pkg_idem", onProgress = {})
        delay(50L)

        // Call cancel multiple times
        runner.cancel()
        runner.cancel()
        runner.cancel()

        job.join()

        // Verify only 1 CANCEL_REQUESTED is logged
        val cancelRequests = logger.events.count { it.startsWith("CANCEL_REQUESTED") }
        assertEquals(1, cancelRequests, "Repeated cancel clicks should only log CANCEL_REQUESTED once")
        tempDir.toFile().deleteRecursively()
    }
}
