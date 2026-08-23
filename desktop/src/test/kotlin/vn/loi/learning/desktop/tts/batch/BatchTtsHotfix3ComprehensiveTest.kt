package vn.loi.learning.desktop.tts.batch

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsEngine
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsSynthesisRequest
import vn.loi.learning.desktop.tts.TtsSynthesisResult
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.preset.TtsLanguagePresetConfig
import vn.loi.learning.desktop.tts.preset.TtsPreset
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyConfig
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyMode
import vn.loi.learning.desktop.tts.ui.OrderedFallbackVoices

@OptIn(ExperimentalCoroutinesApi::class)
class BatchTtsHotfix3ComprehensiveTest {

    private val voicePrimaryVi = TtsVoice("vi-VN-HoaiMyNeural", "HoaiMy", "vi-VN", "vi", "Female")
    private val voiceFallback1Vi = TtsVoice("vi-VN-NamMinhNeural", "NamMinh", "vi-VN", "vi", "Male")
    private val voiceFallback2Vi = TtsVoice("vi-VN-ThirdVoiceNeural", "ThirdVoice", "vi-VN", "vi", "Female")
    private val voiceFallback3Vi = TtsVoice("vi-VN-FourthVoiceNeural", "FourthVoice", "vi-VN", "vi", "Male")

    private val voicePrimaryEn = TtsVoice("en-US-AvaMultilingualNeural", "Ava", "en-US", "en", "Female")
    private val voiceFallback1En = TtsVoice("en-US-AndrewMultilingualNeural", "Andrew", "en-US", "en", "Male")
    private val voiceFallback2En = TtsVoice("en-US-EmmaNeural", "Emma", "en-US", "en", "Female")

    private class RecordingLogger : BatchTtsEventLogger {
        val events = mutableListOf<String>()
        val voiceHealthEvents = mutableListOf<String>()
        val applyEvents = mutableListOf<String>()

        override fun logPlanCreated(planId: String, packageId: String, targetCount: Int, overwriteMode: Boolean, selectedFields: String, languageRequirements: String) {
            events += "PLAN_CREATED:$planId:count=$targetCount"
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
        override fun logAttemptAbandoned(targetIndex: Int, voiceId: String, attemptNumber: Int, reason: String) {}
        override fun logWorkerPoolRotated(abandonedWorkerCount: Int, newPoolId: String) {}
        override fun logFallback(targetIndex: Int, fromVoiceId: String, toVoiceId: String, candidateIndex: Int) {
            events += "FALLBACK:$targetIndex:$fromVoiceId->$toVoiceId"
        }
        override fun logVoiceHealthChanged(voiceId: String, oldState: String, newState: String, reason: String) {
            voiceHealthEvents += "HEALTH_CHANGED:$voiceId:$oldState->$newState"
        }
        override fun logVoiceCircuitOpen(voiceId: String, consecutiveFailures: Int, cooldownMillis: Long) {
            voiceHealthEvents += "CIRCUIT_OPEN:$voiceId:failures=$consecutiveFailures:cooldown=$cooldownMillis"
        }
        override fun logVoiceCircuitProbe(voiceId: String, targetIndex: Int) {
            voiceHealthEvents += "CIRCUIT_PROBE:$voiceId:$targetIndex"
        }
        override fun logVoiceCircuitBypassed(voiceId: String, targetIndex: Int) {
            voiceHealthEvents += "CIRCUIT_BYPASSED:$voiceId:$targetIndex"
        }
        override fun logVoiceCircuitRecovered(voiceId: String) {
            voiceHealthEvents += "CIRCUIT_RECOVERED:$voiceId"
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
        override fun logApplyStarted(packageName: String, totalTargets: Int) {
            applyEvents += "APPLY_STARTED:$packageName:$totalTargets"
        }
        override fun logApplyProgress(packageName: String, appliedCount: Int, totalTargets: Int, failedCount: Int) {
            applyEvents += "APPLY_PROGRESS:$appliedCount/$totalTargets:failed=$failedCount"
        }
        override fun logApplyTargetSuccess(contentId: String, field: String, assetPath: String) {
            applyEvents += "APPLY_SUCCESS:$contentId:$field:$assetPath"
        }
        override fun logApplyTargetFailure(contentId: String, field: String, reason: String) {
            applyEvents += "APPLY_FAILURE:$contentId:$field:$reason"
        }
        override fun logApplyCompleted(packageName: String, appliedCount: Int, failedCount: Int, durationMillis: Long) {
            applyEvents += "APPLY_COMPLETED:$packageName:applied=$appliedCount:failed=$failedCount"
        }
        override fun logApplyCancelled(packageName: String, appliedCount: Int, totalTargets: Int) {
            applyEvents += "APPLY_CANCELLED:$packageName"
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

    // 1. Capability A: Ordered Multi-Voice Resilient Pool per Language (1 primary + 3 fallbacks = 4 voices)
    @Test
    fun testOrderedMultiVoicePoolPreservesOrderAndFiltersLanguageAndDuplicates() {
        val catalog = listOf(
            voicePrimaryVi,
            voiceFallback1Vi,
            voiceFallback2Vi,
            voiceFallback3Vi,
            voicePrimaryEn,
            voiceFallback1En
        )

        // Hydrate Vietnamese pool with 1 primary + 3 fallbacks
        val viFallbacks = OrderedFallbackVoices.hydrate(
            ids = listOf(voiceFallback1Vi.id, voiceFallback2Vi.id, voiceFallback3Vi.id, voicePrimaryVi.id, "invalid-voice-id", voicePrimaryEn.id),
            catalog = catalog,
            language = TtsLanguage.VIETNAMESE,
            primary = voicePrimaryVi
        )

        // Primary voice is excluded from fallbacks, cross-language English voice excluded, invalid ID excluded
        assertEquals(3, viFallbacks.voices.size)
        assertEquals(listOf(voiceFallback1Vi, voiceFallback2Vi, voiceFallback3Vi), viFallbacks.voices)

        // Strategy candidate chain must have exactly primary + fallbacks in order
        val strategy = VoiceStrategyConfig(
            mode = VoiceStrategyMode.FALLBACK_CHAIN,
            primaryVoice = voicePrimaryVi,
            fallbackVoices = viFallbacks.voices,
            candidateVoices = listOf(voicePrimaryVi) + viFallbacks.voices
        )
        val candidateChain = strategy.candidateChainForTarget(0)
        assertEquals(4, candidateChain.size)
        assertEquals(voicePrimaryVi, candidateChain[0])
        assertEquals(voiceFallback1Vi, candidateChain[1])
        assertEquals(voiceFallback2Vi, candidateChain[2])
        assertEquals(voiceFallback3Vi, candidateChain[3])

        // Build batch jobs using Scanner
        val target = BatchTtsTarget("item_1", TtsField.TRANSLATION, "Xin chào", TtsLanguage.VIETNAMESE, isMissing = true, hasAudio = false)
        val jobs = BatchTtsScanner.buildJobsWithStrategy(
            targets = listOf(target),
            englishStrategy = null,
            vietnameseStrategy = strategy
        )

        assertEquals(1, jobs.size)
        assertEquals(4, jobs[0].candidateVoices.size)
        assertEquals(voicePrimaryVi, jobs[0].candidateVoices[0])
        assertEquals(voiceFallback1Vi, jobs[0].candidateVoices[1])
        assertEquals(voiceFallback2Vi, jobs[0].candidateVoices[2])
        assertEquals(voiceFallback3Vi, jobs[0].candidateVoices[3])
    }

    // 2. Capability B: Dynamic Circuit Breaker with Exponential Backoff and 0ms Bypass
    @Test
    fun testCircuitBreakerExponentialCooldownAndBypass() = runBlocking {
        val logger = RecordingLogger()
        val tracker = BatchTtsVoiceHealthTracker(
            circuitOpenCooldownMillis = 1000L,
            maxCooldownMillis = 8000L,
            maxConsecutiveHardTimeoutsBeforeOpen = 1,
            maxConsecutiveFastFailuresBeforeOpen = 2,
            eventLogger = logger
        )

        val candidates = listOf(voicePrimaryVi, voiceFallback1Vi)

        // Initial state: HEALTHY
        assertEquals(VoiceHealthState.HEALTHY, tracker.getStatus(voicePrimaryVi.id).state)
        assertTrue(tracker.isCandidateEligible(voicePrimaryVi.id, hasAlternativeCandidates = true))

        // Target 1 hard timeout -> opens circuit (cooldown 1000ms)
        tracker.recordHardTimeout(voicePrimaryVi.id)
        val status1 = tracker.getStatus(voicePrimaryVi.id)
        assertEquals(VoiceHealthState.CIRCUIT_OPEN, status1.state)
        assertEquals(1, status1.circuitOpenCount)
        assertFalse(tracker.isCandidateEligible(voicePrimaryVi.id, hasAlternativeCandidates = true))

        // When prioritized, healthy fallback comes FIRST
        val prioritized1 = tracker.prioritizeCandidates(candidates)
        assertEquals(voiceFallback1Vi, prioritized1[0])
        assertEquals(voicePrimaryVi, prioritized1[1])

        // Wait for 1000ms cooldown to expire -> transitions to HALF_OPEN_PROBE
        delay(1050L)
        val statusProbe1 = tracker.getStatus(voicePrimaryVi.id)
        assertEquals(VoiceHealthState.HALF_OPEN_PROBE, statusProbe1.state)
        assertTrue(tracker.isCandidateEligible(voicePrimaryVi.id, hasAlternativeCandidates = true))

        // Probe fails -> opens circuit again with DOUBLED cooldown (2000ms)
        tracker.recordHardTimeout(voicePrimaryVi.id)
        val status2 = tracker.getStatus(voicePrimaryVi.id)
        assertEquals(VoiceHealthState.CIRCUIT_OPEN, status2.state)
        assertEquals(2, status2.circuitOpenCount)

        // Cooldown is now 2000ms: at 1050ms it should STILL be CIRCUIT_OPEN
        delay(1050L)
        assertEquals(VoiceHealthState.CIRCUIT_OPEN, tracker.getStatus(voicePrimaryVi.id).state)

        // After remaining 1000ms, it opens for probe 2
        delay(1050L)
        assertEquals(VoiceHealthState.HALF_OPEN_PROBE, tracker.getStatus(voicePrimaryVi.id).state)

        // Probe succeeds -> recovers to HEALTHY and resets open count
        tracker.recordSuccess(voicePrimaryVi.id)
        val statusRecovered = tracker.getStatus(voicePrimaryVi.id)
        assertEquals(VoiceHealthState.HEALTHY, statusRecovered.state)
        assertEquals(0, statusRecovered.circuitOpenCount)
        assertEquals(0, statusRecovered.consecutiveHardTimeouts)
        assertTrue(logger.voiceHealthEvents.any { it.startsWith("CIRCUIT_RECOVERED:${voicePrimaryVi.id}") })
    }

    // 3. Capability D: Exhausted Voice Pool -> Mark Target FAILED -> Continue Batch
    @Test
    fun testExhaustedPoolMarksTargetFailedAndContinuesBatchToCompletion() = runBlocking {
        val tempDir = Files.createTempDirectory("batch-tts-exhausted-test")
        val logger = RecordingLogger()
        val storage = TestStorage(tempDir)

        val engine = object : TtsEngine {
            override suspend fun listVoices(): List<TtsVoice> = listOf(voicePrimaryVi, voiceFallback1Vi)
            override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                if (request.text.contains("fail")) {
                    // All voices fail on this target
                    error("Simulated provider outage for target")
                }
                // Other targets succeed
                val fakeMp3 = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x44.toByte())
                Files.write(outputFile, fakeMp3)
                return TtsSynthesisResult(outputFile, 4L)
            }
        }
        val ttsService = DesktopTtsAudioService(engine, storage)
        val checkpointStore = BatchTtsCheckpointStore(tempDir.resolve("ckpt.json"))

        val jobs = listOf(
            BatchTtsJob("c1", TtsField.TRANSLATION, "fail target 1", TtsLanguage.VIETNAMESE, voicePrimaryVi, candidateVoices = listOf(voicePrimaryVi, voiceFallback1Vi)),
            BatchTtsJob("c2", TtsField.TRANSLATION, "good target 2", TtsLanguage.VIETNAMESE, voicePrimaryVi, candidateVoices = listOf(voicePrimaryVi, voiceFallback1Vi)),
            BatchTtsJob("c3", TtsField.TRANSLATION, "fail target 3", TtsLanguage.VIETNAMESE, voicePrimaryVi, candidateVoices = listOf(voicePrimaryVi, voiceFallback1Vi)),
            BatchTtsJob("c4", TtsField.TRANSLATION, "good target 4", TtsLanguage.VIETNAMESE, voicePrimaryVi, candidateVoices = listOf(voicePrimaryVi, voiceFallback1Vi))
        )

        var finalSummary: BatchTtsSummary? = null
        val runner = BatchTtsRunner(
            ttsService = ttsService,
            scope = this,
            policy = BatchTtsExecutionPolicy(attemptTimeoutMillis = 1000L, maxAttemptsPerVoice = 1),
            checkpointStore = checkpointStore,
            eventLogger = logger
        )

        val job = runner.runBatch(
            jobs = jobs,
            packageName = "pkg_fail_continue",
            onProgress = { finalSummary = it }
        )
        job.join()

        assertNotNull(finalSummary)
        assertTrue(finalSummary!!.isFinished)
        assertFalse(finalSummary!!.isCancelled)
        assertEquals(4, finalSummary!!.totalJobs)
        assertEquals(4, finalSummary!!.completedJobs)
        assertEquals(2, finalSummary!!.successCount)
        assertEquals(2, finalSummary!!.failedCount)
        assertTrue(finalSummary!!.hasFailures)

        val failedResults = finalSummary!!.failedResults
        assertEquals(2, failedResults.size)
        assertEquals("c1", failedResults[0].job.contentId)
        assertEquals("c3", failedResults[1].job.contentId)

        // Checkpoint contains all 4 records including FAILED
        val checkpoint = checkpointStore.load()
        assertNotNull(checkpoint)
        assertEquals(4, checkpoint.records.size)
        assertEquals("FAILED", checkpoint.records[0].status)
        assertEquals("SUCCESS", checkpoint.records[1].status)
        assertEquals("FAILED", checkpoint.records[2].status)
        assertEquals("SUCCESS", checkpoint.records[3].status)

        // Verification of structured logs
        assertTrue(logger.events.contains("TARGET_FAILED:1:GENERATION_FAILED"))
        assertTrue(logger.events.contains("TARGET_FAILED:3:GENERATION_FAILED"))
        assertTrue(logger.events.any { it.startsWith("BATCH_COMPLETED:success=2,failed=2") })

        tempDir.toFile().deleteRecursively()
    }

    // 4. Capability E: Failed-Only Target Isolation and Retry Workflow
    @Test
    fun testRetryFailedOnlyIsolatesFailedJobsAndMergesResults() = runBlocking {
        val tempDir = Files.createTempDirectory("batch-tts-retry-test")
        val logger = RecordingLogger()
        val storage = TestStorage(tempDir)

        var serviceRepaired = false

        val engine = object : TtsEngine {
            override suspend fun listVoices(): List<TtsVoice> = listOf(voicePrimaryVi, voiceFallback1Vi)
            override suspend fun synthesize(request: TtsSynthesisRequest, outputFile: Path): TtsSynthesisResult {
                if (request.text.contains("c2") && !serviceRepaired) {
                    error("Temporary provider outage on c2")
                }
                val fakeMp3 = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x44.toByte())
                Files.write(outputFile, fakeMp3)
                return TtsSynthesisResult(outputFile, 4L)
            }
        }
        val ttsService = DesktopTtsAudioService(engine, storage)

        val jobs = listOf(
            BatchTtsJob("c1", TtsField.TRANSLATION, "good c1", TtsLanguage.VIETNAMESE, voicePrimaryVi, candidateVoices = listOf(voicePrimaryVi)),
            BatchTtsJob("c2", TtsField.TRANSLATION, "bad c2", TtsLanguage.VIETNAMESE, voicePrimaryVi, candidateVoices = listOf(voicePrimaryVi)),
            BatchTtsJob("c3", TtsField.TRANSLATION, "good c3", TtsLanguage.VIETNAMESE, voicePrimaryVi, candidateVoices = listOf(voicePrimaryVi))
        )

        // Run initial batch
        val runner1 = BatchTtsRunner(ttsService = ttsService, scope = this, eventLogger = logger)
        var summary1: BatchTtsSummary? = null
        runner1.runBatch(jobs = jobs, packageName = "pkg_retry", onProgress = { summary1 = it }).join()

        assertNotNull(summary1)
        assertEquals(2, summary1!!.successCount)
        assertEquals(1, summary1!!.failedCount)
        assertEquals("c2", summary1!!.failedResults.first().job.contentId)

        // Step 2: Retry Failed Targets Only
        serviceRepaired = true
        val failedJobs = summary1!!.failedResults.map { it.job }
        assertEquals(1, failedJobs.size)
        assertEquals("c2", failedJobs.first().contentId)

        val previousSuccesses = summary1!!.successfulResults
        assertEquals(2, previousSuccesses.size)

        var summaryRetry: BatchTtsSummary? = null
        val runner2 = BatchTtsRunner(ttsService = ttsService, scope = this, eventLogger = logger)
        runner2.runBatch(
            jobs = failedJobs,
            packageName = "pkg_retry",
            onProgress = { updatedSummary ->
                summaryRetry = updatedSummary.copy(
                    totalJobs = previousSuccesses.size + updatedSummary.totalJobs,
                    completedJobs = previousSuccesses.size + updatedSummary.completedJobs,
                    successCount = previousSuccesses.size + updatedSummary.successCount,
                    failedCount = updatedSummary.failedCount,
                    jobResults = previousSuccesses + updatedSummary.jobResults
                )
            }
        ).join()

        assertNotNull(summaryRetry)
        assertTrue(summaryRetry!!.isFinished)
        assertEquals(3, summaryRetry!!.totalJobs)
        assertEquals(3, summaryRetry!!.completedJobs)
        assertEquals(3, summaryRetry!!.successCount)
        assertEquals(0, summaryRetry!!.failedCount)
        assertFalse(summaryRetry!!.hasFailures)
        assertEquals(3, summaryRetry!!.successfulResults.size)

        tempDir.toFile().deleteRecursively()
    }

    // 5. Capability G: Visible & Measurable Apply Phase Lifecycle and Progress
    @Test
    fun testApplyPhaseReportsLiveProgressAndCompletesDurably() {
        val logger = RecordingLogger()

        val results = listOf(
            BatchTtsJobResult(
                job = BatchTtsJob("c1", TtsField.QUESTION, "apple", TtsLanguage.ENGLISH, voicePrimaryEn),
                status = BatchTtsJobStatus.SUCCESS,
                assetRelativePath = "pkg/c1_q.mp3"
            ),
            BatchTtsJobResult(
                job = BatchTtsJob("c2", TtsField.TRANSLATION, "quả táo", TtsLanguage.VIETNAMESE, voicePrimaryVi),
                status = BatchTtsJobStatus.SUCCESS,
                assetRelativePath = "pkg/c2_tr.mp3"
            ),
            BatchTtsJobResult(
                job = BatchTtsJob("c3", TtsField.ANSWER, "fruit", TtsLanguage.ENGLISH, voicePrimaryEn),
                status = BatchTtsJobStatus.SUCCESS,
                assetRelativePath = "pkg/c3_a.mp3"
            )
        )

        val progressUpdates = mutableListOf<Triple<Int, Int, Int>>()
        var completedApplied = 0
        var completedFailed = 0

        logger.logApplyStarted("test_pkg", results.size)

        // Simulate apply with incremental progress
        for ((idx, res) in results.withIndex()) {
            val applied = idx + 1
            val failed = 0
            progressUpdates.add(Triple(applied, results.size, failed))
            logger.logApplyProgress("test_pkg", applied, results.size, failed)
            logger.logApplyTargetSuccess(res.job.contentId, res.job.field.name, res.assetRelativePath!!)
        }

        completedApplied = results.size
        completedFailed = 0
        logger.logApplyCompleted("test_pkg", completedApplied, completedFailed, 250L)

        assertEquals(3, progressUpdates.size)
        assertEquals(Triple(1, 3, 0), progressUpdates[0])
        assertEquals(Triple(2, 3, 0), progressUpdates[1])
        assertEquals(Triple(3, 3, 0), progressUpdates[2])

        assertTrue(logger.applyEvents.contains("APPLY_STARTED:test_pkg:3"))
        assertTrue(logger.applyEvents.contains("APPLY_PROGRESS:1/3:failed=0"))
        assertTrue(logger.applyEvents.contains("APPLY_PROGRESS:2/3:failed=0"))
        assertTrue(logger.applyEvents.contains("APPLY_PROGRESS:3/3:failed=0"))
        assertTrue(logger.applyEvents.contains("APPLY_COMPLETED:test_pkg:applied=3:failed=0"))
    }
}
