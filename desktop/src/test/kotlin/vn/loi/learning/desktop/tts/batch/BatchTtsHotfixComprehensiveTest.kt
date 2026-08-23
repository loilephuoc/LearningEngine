package vn.loi.learning.desktop.tts.batch

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.runtime.DesktopLogLevel
import vn.loi.learning.desktop.runtime.DesktopRuntimeLogger
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.EdgeTtsEngine
import vn.loi.learning.desktop.tts.TtsEngine
import vn.loi.learning.desktop.tts.TtsError
import vn.loi.learning.desktop.tts.TtsException
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsPreviewStore
import vn.loi.learning.desktop.tts.TtsSynthesisRequest
import vn.loi.learning.desktop.tts.TtsSynthesisResult
import vn.loi.learning.desktop.tts.TtsVoice

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class BatchTtsHotfixComprehensiveTest {

    private val voicePrimaryEn = TtsVoice("en-US-Primary", "Primary", "en-US", "en")
    private val voiceFallback1En = TtsVoice("en-US-Fallback1", "Fallback 1", "en-US", "en")
    private val voiceFallback2En = TtsVoice("en-US-Fallback2", "Fallback 2", "en-US", "en")
    private val voiceVi = TtsVoice("vi-VN-Primary", "Vietnamese", "vi-VN", "vi")

    // ======================================================================
    // 1. PROVIDER HANG / UNINTERRUPTIBLE ATTEMPTS & TIMEOUT RECOVERY
    // ======================================================================

    @Test
    fun `uninterruptible hanging provider attempt is abandoned on timeout and batch advances`() = runTest {
        val root = Files.createTempDirectory("tts-hang-adv")
        val workerPool = Executors.newCachedThreadPool()
        try {
            val hangingLatch = CountDownLatch(1)
            val hangCallCount = AtomicInteger(0)

            val uncooperativeEngine = object : TtsEngine {
                override suspend fun listVoices() = listOf(voicePrimaryEn, voiceFallback1En)
                override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult =
                    suspendCancellableCoroutine { continuation ->
                        if (request.voice.id == voicePrimaryEn.id) {
                            hangCallCount.incrementAndGet()
                            val future = workerPool.submit {
                                while (!hangingLatch.await(50, TimeUnit.MILLISECONDS)) {
                                    // uninterruptibly hang
                                }
                            }
                            continuation.invokeOnCancellation {
                                future.cancel(true)
                            }
                        } else {
                            Files.createDirectories(outputFile.parent)
                            Files.write(outputFile, byteArrayOf(1, 2, 3))
                            continuation.resume(TtsSynthesisResult(outputFile, 3))
                        }
                    }
            }

            val service = DesktopTtsAudioService(
                ttsEngine = uncooperativeEngine,
                mediaStorage = TestStorage(root),
                previewStore = TtsPreviewStore(root),
                ioDispatcher = coroutineContext[kotlinx.coroutines.CoroutineDispatcher] ?: Dispatchers.Default
            )

            // Policy: 50ms per attempt, max 1 attempt per voice
            val policy = BatchTtsExecutionPolicy(
                attemptTimeoutMillis = 50,
                maxAttemptsPerVoice = 1,
                initialRetryDelayMillis = 0,
                maxRetryDelayMillis = 0,
                maxCandidateVoices = 2
            )

            val runner = BatchTtsRunner(service, this, policy)

            val target1 = BatchTtsJob(
                contentId = "c1",
                field = TtsField.QUESTION,
                text = "apple",
                language = TtsLanguage.ENGLISH,
                voice = voicePrimaryEn,
                candidateVoices = listOf(voicePrimaryEn, voiceFallback1En),
                id = "j1"
            )
            val target2 = BatchTtsJob(
                contentId = "c2",
                field = TtsField.QUESTION,
                text = "banana",
                language = TtsLanguage.ENGLISH,
                voice = voiceFallback1En,
                candidateVoices = listOf(voiceFallback1En),
                id = "j2"
            )

            var summary: BatchTtsSummary? = null
            runner.runBatch(listOf(target1, target2), "test_pkg") { summary = it }.join()

            hangingLatch.countDown() // unblock background thread

            assertNotNull(summary)
            val nonNullSummary = summary!!
            assertEquals(2, nonNullSummary.totalJobs)
            assertEquals(2, nonNullSummary.completedJobs)
            assertEquals(2, nonNullSummary.successCount)
            assertEquals(0, nonNullSummary.failedCount)
            assertTrue(nonNullSummary.isFinished)

            // Target 1 succeeded via fallback 1
            val result1 = nonNullSummary.jobResults[0]
            assertEquals(BatchTtsJobStatus.SUCCESS, result1.status)
            assertTrue(result1.recoveredViaFallback)
            assertEquals(voiceFallback1En.id, result1.actualVoiceUsed?.id)
        } finally {
            workerPool.shutdownNow()
            root.toFile().deleteRecursively()
        }
    }

    // ======================================================================
    // 2. EDGE TTS ENGINE ATTEMPT ABANDONMENT & LATE CALLBACK IMMUNITY
    // ======================================================================

    @Test
    fun `EdgeTtsEngine abandons cancelled synthesis without corrupting output file`() = runTest {
        val root = Files.createTempDirectory("edge-tts-abandon")
        val outFile = root.resolve("out.mp3")
        val executor = Executors.newSingleThreadExecutor()

        try {
            val engine = EdgeTtsEngine(executor = executor)
            val ex = assertFailsWith<TtsException> {
                engine.synthesize(
                    TtsSynthesisRequest(text = "", voice = voicePrimaryEn),
                    outFile
                )
            }
            assertTrue(ex.error is TtsError.InvalidText)
            assertFalse(Files.exists(outFile))
        } finally {
            executor.shutdownNow()
            root.toFile().deleteRecursively()
        }
    }

    // ======================================================================
    // 3. PER-TARGET WATCHDOG SAFETY
    // ======================================================================

    @Test
    fun `runner watchdog terminates stalled target when all candidates hang`() = runTest {
        val root = Files.createTempDirectory("tts-watchdog")
        val pool = Executors.newCachedThreadPool()
        try {
            val hangingLatch = CountDownLatch(1)
            val hangEngine = object : TtsEngine {
                override suspend fun listVoices() = listOf(voicePrimaryEn)
                override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult =
                    suspendCancellableCoroutine { continuation ->
                        val future = pool.submit {
                            while (!hangingLatch.await(50, TimeUnit.MILLISECONDS)) {}
                        }
                        continuation.invokeOnCancellation {
                            future.cancel(true)
                        }
                    }
            }

            val service = DesktopTtsAudioService(
                ttsEngine = hangEngine,
                mediaStorage = TestStorage(root),
                previewStore = TtsPreviewStore(root),
                ioDispatcher = coroutineContext[kotlinx.coroutines.CoroutineDispatcher] ?: Dispatchers.Default
            )

            val policy = BatchTtsExecutionPolicy(
                attemptTimeoutMillis = 50,
                maxAttemptsPerVoice = 1,
                initialRetryDelayMillis = 0,
                maxRetryDelayMillis = 0,
                maxCandidateVoices = 1
            )

            val runner = BatchTtsRunner(service, this, policy)
            val target = BatchTtsJob("c1", TtsField.QUESTION, "text", TtsLanguage.ENGLISH, voicePrimaryEn, candidateVoices = listOf(voicePrimaryEn))

            var finalSummary: BatchTtsSummary? = null
            runner.runBatch(listOf(target), "pkg") { finalSummary = it }.join()
            hangingLatch.countDown()

            assertNotNull(finalSummary)
            val summary = finalSummary!!
            assertEquals(1, summary.completedJobs)
            assertEquals(1, summary.failedCount)
            assertEquals(0, summary.successCount)
            assertEquals(BatchTtsJobStatus.FAILED, summary.jobResults.first().status)
        } finally {
            pool.shutdownNow()
            root.toFile().deleteRecursively()
        }
    }

    // ======================================================================
    // 4. LARGE BATCH FORWARD PROGRESS (327 TARGETS SIMULATION)
    // ======================================================================

    @Test
    fun `large 327-target batch with failures at 24 100 250 completes 100 percent terminally`() = runTest {
        val root = Files.createTempDirectory("tts-large-batch")
        try {
            val failureIndices = setOf(24, 100, 250)
            val engine = object : TtsEngine {
                override suspend fun listVoices() = listOf(voicePrimaryEn)
                override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                    val index = request.text.removePrefix("item-").toIntOrNull() ?: 0
                    if (index in failureIndices) {
                        throw TtsException(TtsError.GenerationFailed("Simulated provider failure at target $index"))
                    }
                    Files.createDirectories(outputFile.parent)
                    Files.write(outputFile, byteArrayOf(1, 2))
                    return TtsSynthesisResult(outputFile, 2)
                }
            }

            val service = DesktopTtsAudioService(
                ttsEngine = engine,
                mediaStorage = TestStorage(root),
                previewStore = TtsPreviewStore(root),
                ioDispatcher = coroutineContext[kotlinx.coroutines.CoroutineDispatcher] ?: Dispatchers.Default
            )

            val runner = BatchTtsRunner(
                service,
                this,
                BatchTtsExecutionPolicy(attemptTimeoutMillis = 500, maxAttemptsPerVoice = 1, initialRetryDelayMillis = 0, maxRetryDelayMillis = 0)
            )

            val totalCount = 327
            val jobs = (1..totalCount).map { i ->
                BatchTtsJob("c$i", TtsField.QUESTION, "item-$i", TtsLanguage.ENGLISH, voicePrimaryEn, id = "job-$i")
            }

            var finalSummary: BatchTtsSummary? = null
            runner.runBatch(jobs, "pkg_large") { finalSummary = it }.join()

            assertNotNull(finalSummary)
            val summary = finalSummary!!
            assertEquals(327, summary.totalJobs)
            assertEquals(327, summary.completedJobs)
            assertEquals(3, summary.failedCount)
            assertEquals(324, summary.successCount)
            assertEquals(0, summary.cancelledCount)
            assertTrue(summary.isFinished)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    // ======================================================================
    // 5. DEAD LEASE RECOVERY AND CONCURRENCY REJECTION
    // ======================================================================

    @Test
    fun `dead lease is reclaimed immediately while live owner blocks concurrent acquisition`() {
        val root = Files.createTempDirectory("tts-lease-test")
        try {
            val repo = BatchTtsCheckpointRepository(root)
            val job = BatchTtsJob("c1", TtsField.QUESTION, "text", TtsLanguage.ENGLISH, voicePrimaryEn)
            val plan = BatchTtsPlanIdentity.create("pkg", listOf(job), false)

            // 1. Acquire lease
            val ownership1 = repo.acquire(plan)

            // 2. Concurrent acquisition while alive must fail
            assertFailsWith<IllegalStateException> {
                repo.acquire(plan)
            }

            // 3. Simulate dead process: close ownership channel but leave lock file pointing to non-existent PID (999999999)
            ownership1.close()
            val lockFile = repo.checkpointPath(plan).resolveSibling("${plan.id}.lock")
            val deadPid = 999_999_999L
            val deadLease = BatchTtsLeaseInfo(
                planId = plan.id,
                ownerPid = deadPid,
                ownerStartTimestamp = System.currentTimeMillis() - 10_000L,
                leaseCreationTimestamp = System.currentTimeMillis() - 10_000L
            )
            Files.createDirectories(lockFile.parent)
            Files.writeString(lockFile, kotlinx.serialization.json.Json.encodeToString(BatchTtsLeaseInfo.serializer(), deadLease))

            // 4. Dead owner lease must be reclaimed immediately without waiting 24 hours
            val reclaimedOwnership = repo.acquire(plan)
            assertNotNull(reclaimedOwnership)
            reclaimedOwnership.close()
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    // ======================================================================
    // 6. CRASH RESUME: 23 COMPLETED TARGETS NEVER REGENERATED
    // ======================================================================

    @Test
    fun `crash resume preserves 23 completed targets and starts at target 24`() = runTest {
        val root = Files.createTempDirectory("tts-resume-test")
        try {
            val storage = TestStorage(root.resolve("media"))
            val totalTargets = 50
            val jobs = (1..totalTargets).map { i ->
                BatchTtsJob("c$i", TtsField.QUESTION, "text-$i", TtsLanguage.ENGLISH, voicePrimaryEn, id = "job-$i")
            }
            val plan = BatchTtsPlanIdentity.create("resume_pkg", jobs, false)
            val repo = BatchTtsCheckpointRepository(root.resolve("checkpoints"))
            val store = repo.storeFor(plan)

            // Pre-populate 23 completed targets with stored media assets
            val completedRecords = (1..23).map { i ->
                val assetPath = "resume_pkg/c${i}_question.mp3"
                storage.store("resume_pkg", "c${i}_question.mp3", byteArrayOf(1, 2, 3))
                BatchTtsCheckpointRecord(
                    jobId = "job-$i",
                    textFingerprint = jobs[i - 1].textFingerprint(),
                    status = "SUCCESS",
                    assetRelativePath = assetPath
                )
            }
            store.save(BatchTtsCheckpoint(plan.id, "resume_pkg", false, completedRecords))

            val synthesisCallCount = AtomicInteger(0)
            val engine = object : TtsEngine {
                override suspend fun listVoices() = listOf(voicePrimaryEn)
                override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                    synthesisCallCount.incrementAndGet()
                    Files.createDirectories(outputFile.parent)
                    Files.write(outputFile, byteArrayOf(9))
                    return TtsSynthesisResult(outputFile, 1)
                }
            }

            val service = DesktopTtsAudioService(
                ttsEngine = engine,
                mediaStorage = storage,
                previewStore = TtsPreviewStore(root),
                ioDispatcher = coroutineContext[kotlinx.coroutines.CoroutineDispatcher] ?: Dispatchers.Default
            )

            val runner = BatchTtsRunner(
                service,
                this,
                checkpointStore = store,
                assetExists = { storage.exists(it) },
                checkpointOwnership = repo.acquire(plan)
            )

            var finalSummary: BatchTtsSummary? = null
            runner.runBatch(jobs, "resume_pkg", plan.id, false) { finalSummary = it }.join()

            assertNotNull(finalSummary)
            val summary = finalSummary!!
            assertEquals(50, summary.totalJobs)
            assertEquals(50, summary.completedJobs)
            assertEquals(50, summary.successCount)
            assertEquals(0, summary.failedCount)

            // Exact proof: only targets 24..50 were synthesized! (50 - 23 = 27 calls)
            assertEquals(27, synthesisCallCount.get())
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    // ======================================================================
    // 7. STRUCTURED RUNTIME LOGGING (ZERO CONTENT TEXT)
    // ======================================================================

    @Test
    fun `structured logger emits required event codes and contains zero vocabulary content`() = runTest {
        val loggedLines = mutableListOf<String>()
        val testLogger = object : DesktopRuntimeLogger {
            override val filePath: Path = Path.of("test.log")
            override fun log(level: DesktopLogLevel, eventCode: String, message: String) {
                loggedLines.add("$eventCode: $message")
            }
            override fun close() {}
        }

        val eventLogger = RuntimeBatchTtsEventLogger(testLogger)
        val root = Files.createTempDirectory("tts-log-test")

        try {
            val secretText = "THIS_IS_SECRET_VOCABULARY_CONTENT_12345"
            val job = BatchTtsJob("c1", TtsField.QUESTION, secretText, TtsLanguage.ENGLISH, voicePrimaryEn, id = "j1")
            val plan = BatchTtsPlanIdentity.create("log_pkg", listOf(job), false)
            val repo = BatchTtsCheckpointRepository(root, eventLogger = eventLogger)

            eventLogger.logPlanCreated(plan.id, "log_pkg", 1, false, "QUESTION", "en")
            val ownership = repo.acquire(plan)

            val engine = object : TtsEngine {
                override suspend fun listVoices() = listOf(voicePrimaryEn)
                override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                    Files.createDirectories(outputFile.parent)
                    Files.write(outputFile, byteArrayOf(1))
                    return TtsSynthesisResult(outputFile, 1)
                }
            }

            val service = DesktopTtsAudioService(
                ttsEngine = engine,
                mediaStorage = TestStorage(root),
                previewStore = TtsPreviewStore(root),
                ioDispatcher = coroutineContext[kotlinx.coroutines.CoroutineDispatcher] ?: Dispatchers.Default
            )

            val runner = BatchTtsRunner(
                service,
                this,
                checkpointStore = repo.storeFor(plan),
                checkpointOwnership = ownership,
                eventLogger = eventLogger
            )

            runner.runBatch(listOf(job), "log_pkg", plan.id, false) {}.join()

            // Verify expected events are present
            val eventsJoined = loggedLines.joinToString("\n")
            assertTrue(eventsJoined.contains("BATCH_TTS_PLAN_CREATED"))
            assertTrue(eventsJoined.contains("BATCH_TTS_LEASE_ACQUIRE_ATTEMPT"))
            assertTrue(eventsJoined.contains("BATCH_TTS_TARGET_START"))
            assertTrue(eventsJoined.contains("BATCH_TTS_TARGET_SUCCESS"))
            assertTrue(eventsJoined.contains("BATCH_TTS_BATCH_COMPLETED"))

            // Verify ZERO vocabulary text is present in any log event
            assertFalse(eventsJoined.contains(secretText))
            assertFalse(eventsJoined.contains("SECRET_VOCABULARY"))
        } finally {
            root.toFile().deleteRecursively()
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
}
