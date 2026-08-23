package vn.loi.learning.desktop.tts.batch

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsError
import vn.loi.learning.desktop.tts.TtsException
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.strategy.VoiceAttempt

/**
 * Sequential runner for batch TTS jobs with real-time progress, voice health routing / circuit breaker,
 * bounded latency, fast cancellation lifecycle, multi-attempt fallback chains, per-target watchdog isolation,
 * guaranteed forward progress, and Generate != Apply boundary.
 */
@OptIn(ExperimentalStdlibApi::class)
class BatchTtsRunner(
    private val ttsService: DesktopTtsAudioService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val policy: BatchTtsExecutionPolicy = BatchTtsExecutionPolicy(),
    private val checkpointStore: BatchTtsCheckpointStore? = null,
    private val assetExists: (String) -> Boolean = { true },
    private val checkpointOwnership: AutoCloseable? = null,
    private val eventLogger: BatchTtsEventLogger = BatchTtsEventLogger.NoOp,
    private val healthTracker: BatchTtsVoiceHealthTracker = BatchTtsVoiceHealthTracker(eventLogger = eventLogger),
    private val ioDispatcher: CoroutineDispatcher = scope.coroutineContext[CoroutineDispatcher] ?: Dispatchers.IO
) {
    private val cancelFlag = AtomicBoolean(false)
    private var activeJob: Job? = null
    private var activeBatchId: String = ""
    private val currentTargetIndex = AtomicInteger(0)

    /**
     * Triggers cooperative, prompt cancellation of the running batch.
     */
    fun cancel() {
        if (cancelFlag.compareAndSet(false, true)) {
            eventLogger.logCancelRequested(activeBatchId, currentTargetIndex.get())
            activeJob?.cancel(CancellationException("Batch execution cancelled by user"))
        }
    }

    val isCancelled: Boolean
        get() = cancelFlag.get()

    /**
     * Executes a list of batch jobs sequentially with voice health routing and fallback support.
     *
     * @param jobs The batch jobs to execute.
     * @param packageName Target package name.
     * @param onApply Optional callback to apply the audio reference immediately; if null, only files are generated.
     * @param onProgress Callback invoked on each state change / step.
     */
    fun runBatch(
        jobs: List<BatchTtsJob>,
        packageName: String,
        batchId: String = packageName,
        overwriteExisting: Boolean = jobs.any { it.overwriteExisting },
        onApply: ((contentId: String, field: TtsField, audioRef: String) -> Unit)? = null,
        onProgress: (BatchTtsSummary) -> Unit
    ): Job {
        cancelFlag.set(false)
        activeBatchId = batchId
        currentTargetIndex.set(0)

        val total = jobs.size
        val results = mutableListOf<BatchTtsJobResult>()
        val checkpoint = checkpointStore?.load()?.takeIf {
            it.batchId == batchId && it.packageName == packageName && it.overwriteExisting == overwriteExisting
        }
        val resumable = checkpoint?.records.orEmpty().associateBy { it.jobId }

        var successCount = 0
        var failedCount = 0
        var cancelledCount = 0
        var completedCount = 0
        val startTimeMillis = System.currentTimeMillis()

        activeJob = scope.launch {
            try {
                if (resumable.isNotEmpty()) {
                    val matchingResumed = jobs.count { job ->
                        val rec = resumable[job.id]
                        rec != null &&
                            rec.textFingerprint == job.textFingerprint() &&
                            rec.status == BatchTtsJobStatus.SUCCESS.name &&
                            rec.assetRelativePath?.let(assetExists) == true
                    }
                    eventLogger.logResume(
                        planId = batchId,
                        successCount = matchingResumed,
                        failedCount = 0,
                        skippedCount = 0,
                        pendingCount = total - matchingResumed
                    )
                }

                // Initial broadcast
                onProgress(
                    BatchTtsSummary(
                        totalJobs = total,
                        completedJobs = 0,
                        successCount = 0,
                        skippedCount = 0,
                        failedCount = 0,
                        cancelledCount = 0,
                        currentJob = jobs.firstOrNull(),
                        currentOperation = "Đang khởi tạo phiên tạo audio...",
                        elapsedMillis = 0L
                    )
                )

                for (i in jobs.indices) {
                    currentTargetIndex.set(i + 1)
                    if (cancelFlag.get() || !isActive) {
                        break
                    }

                    val currentJob = jobs[i]

                    // Check for crash resume matching record
                    val resumed = resumable[currentJob.id]?.takeIf {
                        it.textFingerprint == currentJob.textFingerprint() &&
                            it.status == BatchTtsJobStatus.SUCCESS.name &&
                            it.assetRelativePath?.let(assetExists) == true
                    }
                    if (resumed != null) {
                        results += BatchTtsJobResult(
                            job = currentJob,
                            status = BatchTtsJobStatus.SUCCESS,
                            assetRelativePath = resumed.assetRelativePath,
                            actualVoiceUsed = currentJob.voice
                        )
                        eventLogger.logTargetSuccess(
                            planId = batchId,
                            targetIndex = i + 1,
                            jobId = currentJob.id,
                            voiceId = currentJob.voice.id,
                            assetPath = resumed.assetRelativePath.orEmpty(),
                            recoveredViaFallback = false
                        )
                        successCount++
                        completedCount++
                        continue
                    }

                    eventLogger.logTargetStart(
                        planId = batchId,
                        targetIndex = i + 1,
                        jobId = currentJob.id,
                        contentId = currentJob.contentId,
                        field = currentJob.field.name,
                        language = currentJob.voice.language
                    )

                    val attempts = mutableListOf<VoiceAttempt>()
                    var jobSuccess = false
                    var successVoice: TtsVoice? = null
                    var successAssetPath: String? = null
                    var lastCategory: TtsErrorCategory? = null
                    var lastErrorMessage: String? = null
                    var stopFallback = false

                    // Build candidate chain with language filtering and health prioritization
                    val rawCandidateChain = (if (currentJob.candidateVoices.isNotEmpty()) {
                        currentJob.candidateVoices
                    } else {
                        listOf(currentJob.voice)
                    }).filter { BatchTtsLanguageResolver.isVoiceCompatibleWithLanguage(it.language, it.locale, currentJob.language) }
                        .distinctBy { it.id }
                        .take(policy.maxCandidateVoices)
                    val candidateChain = healthTracker.prioritizeCandidates(rawCandidateChain)

                    // Target watchdog budget
                    val targetWatchdogBudgetMillis = ((policy.attemptTimeoutMillis + 2000L) * candidateChain.size.coerceAtLeast(1) + 5000L)
                        .coerceAtLeast(15_000L)

                    try {
                        withContext(Dispatchers.IO) {
                            withTimeout(targetWatchdogBudgetMillis) {
                                for ((candIndex, voice) in candidateChain.withIndex()) {
                                    if (jobSuccess || cancelFlag.get() || !isActive || stopFallback) break

                                    val hasAlternativeCandidates = candIndex < candidateChain.size - 1
                                    val isEligible = healthTracker.isCandidateEligible(voice.id, hasAlternativeCandidates)

                                    if (!isEligible) {
                                        // Unhealthy voice; bypass without spending timeout budget
                                        eventLogger.logVoiceCircuitBypassed(voice.id, i + 1)
                                        attempts.add(
                                            VoiceAttempt(
                                                voice = voice,
                                                isSuccess = false,
                                                errorCategory = TtsErrorCategory.VOICE_UNAVAILABLE,
                                                errorMessage = "Voice '${voice.id}' is currently marked unhealthy by circuit breaker; bypassed for fallback"
                                            )
                                        )
                                        continue
                                    }

                                    if (candIndex > 0) {
                                        val previousVoice = candidateChain[candIndex - 1]
                                        eventLogger.logFallback(
                                            targetIndex = i + 1,
                                            fromVoiceId = previousVoice.id,
                                            toVoiceId = voice.id,
                                            candidateIndex = candIndex + 1
                                        )
                                    }

                                    var voiceAttempt = 0
                                    var currentBackoff = policy.initialRetryDelayMillis

                                    while (voiceAttempt < policy.maxAttemptsPerVoice && !jobSuccess && !cancelFlag.get() && isActive && !stopFallback) {
                                        voiceAttempt++

                                        val opLabel = if (candIndex == 0) {
                                            if (voiceAttempt == 1) "Đang tạo audio..." else "Đang thử lại (lần $voiceAttempt)..."
                                        } else {
                                            "Đang thử giọng dự phòng: ${voice.displayName.ifBlank { voice.id }} (lần $voiceAttempt)..."
                                        }

                                        val elapsedNow = System.currentTimeMillis() - startTimeMillis
                                        val eta = calculateEta(elapsedNow, completedCount, total)

                                        onProgress(
                                            BatchTtsSummary(
                                                totalJobs = total,
                                                completedJobs = completedCount,
                                                successCount = successCount,
                                                skippedCount = 0,
                                                failedCount = failedCount,
                                                cancelledCount = cancelledCount,
                                                currentJob = currentJob,
                                                jobResults = results.toList(),
                                                isFinished = false,
                                                isCancelled = false,
                                                currentOperation = opLabel,
                                                currentVoiceName = voice.displayName.ifBlank { voice.id },
                                                elapsedMillis = elapsedNow,
                                                estimatedRemainingMillis = eta
                                            )
                                        )

                                        eventLogger.logAttemptStart(
                                            targetIndex = i + 1,
                                            voiceId = voice.id,
                                            attemptNumber = voiceAttempt,
                                            candidateIndex = candIndex + 1
                                        )

                                        try {
                                            val generatedAsset = withTimeout(policy.attemptTimeoutMillis) {
                                                ttsService.generatePermanentAudio(
                                                    contentId = currentJob.contentId,
                                                    packageName = packageName,
                                                    field = currentJob.field,
                                                    text = currentJob.text,
                                                    voice = voice,
                                                    rate = currentJob.rate,
                                                    pitch = currentJob.pitch,
                                                    volume = currentJob.volume,
                                                    language = currentJob.language.code
                                                )
                                            }

                                            val generatedPath = generatedAsset.relativePath
                                            jobSuccess = true
                                            successVoice = voice
                                            successAssetPath = generatedPath
                                            healthTracker.recordSuccess(voice.id)
                                            attempts.add(VoiceAttempt(voice, true, null, null))
                                            onApply?.invoke(currentJob.contentId, currentJob.field, generatedPath)

                                            eventLogger.logAttemptClosed(
                                                targetIndex = i + 1,
                                                voiceId = voice.id,
                                                attemptNumber = voiceAttempt,
                                                candidateIndex = candIndex + 1,
                                                details = "SUCCESS"
                                            )
                                            break
                                        } catch (t: TimeoutCancellationException) {
                                            lastCategory = TtsErrorCategory.TIMEOUT
                                            lastErrorMessage = "Synthesis attempt timed out (${policy.attemptTimeoutMillis}ms)"
                                            healthTracker.recordHardTimeout(voice.id)
                                            attempts.add(VoiceAttempt(voice, false, lastCategory, lastErrorMessage))
                                            eventLogger.logAttemptTimeout(
                                                targetIndex = i + 1,
                                                voiceId = voice.id,
                                                attemptNumber = voiceAttempt,
                                                candidateIndex = candIndex + 1,
                                                timeoutMillis = policy.attemptTimeoutMillis
                                            )
                                            if (hasAlternativeCandidates) {
                                                // On hard timeout with alternative fallback available: do not burn more time on the same voice; break to fallback immediately!
                                                break
                                            }
                                        } catch (ce: CancellationException) {
                                            lastCategory = TtsErrorCategory.CANCELLED
                                            lastErrorMessage = "Operation cancelled"
                                            attempts.add(VoiceAttempt(voice, false, lastCategory, lastErrorMessage))
                                            break
                                        } catch (e: Exception) {
                                            val (cat, msg) = classifyError(e)
                                            lastCategory = cat
                                            lastErrorMessage = msg
                                            healthTracker.recordFastFailure(voice.id)
                                            attempts.add(VoiceAttempt(voice, false, cat, msg))
                                            eventLogger.logAttemptFailure(
                                                targetIndex = i + 1,
                                                voiceId = voice.id,
                                                attemptNumber = voiceAttempt,
                                                candidateIndex = candIndex + 1,
                                                errorCategory = cat.name,
                                                reason = msg
                                            )

                                            if (!isRetryable(cat)) {
                                                if (cat == TtsErrorCategory.INVALID_TEXT) {
                                                    stopFallback = true
                                                }
                                                break
                                            }

                                            if (voiceAttempt < policy.maxAttemptsPerVoice && !cancelFlag.get() && isActive) {
                                                delay(currentBackoff)
                                                currentBackoff = (currentBackoff * 2).coerceAtMost(policy.maxRetryDelayMillis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (tce: TimeoutCancellationException) {
                        lastCategory = TtsErrorCategory.TIMEOUT
                        lastErrorMessage = "Target watchdog budget exceeded (${targetWatchdogBudgetMillis}ms)"
                        attempts.add(VoiceAttempt(candidateChain.firstOrNull() ?: currentJob.voice, false, lastCategory, lastErrorMessage))
                        eventLogger.logAttemptTimeout(
                            targetIndex = i + 1,
                            voiceId = candidateChain.firstOrNull()?.id ?: currentJob.voice.id,
                            attemptNumber = 1,
                            candidateIndex = 1,
                            timeoutMillis = targetWatchdogBudgetMillis
                        )
                    } catch (ce: CancellationException) {
                        lastCategory = TtsErrorCategory.CANCELLED
                        lastErrorMessage = "Operation cancelled"
                    }

                    val resolvedSuccessVoice = successVoice
                    val resolvedSuccessAssetPath = successAssetPath

                    if (jobSuccess && resolvedSuccessVoice != null && resolvedSuccessAssetPath != null) {
                        val recoveredViaFallback = resolvedSuccessVoice.id != currentJob.requestedVoice.id
                        results.add(
                            BatchTtsJobResult(
                                job = currentJob,
                                status = BatchTtsJobStatus.SUCCESS,
                                assetRelativePath = resolvedSuccessAssetPath,
                                actualVoiceUsed = resolvedSuccessVoice,
                                attempts = attempts.toList(),
                                recoveredViaFallback = recoveredViaFallback
                            )
                        )
                        eventLogger.logTargetSuccess(
                            planId = batchId,
                            targetIndex = i + 1,
                            jobId = currentJob.id,
                            voiceId = resolvedSuccessVoice.id,
                            assetPath = resolvedSuccessAssetPath,
                            recoveredViaFallback = recoveredViaFallback
                        )
                        successCount++
                    } else if (cancelFlag.get() || !isActive) {
                        results.add(
                            BatchTtsJobResult(
                                job = currentJob,
                                status = BatchTtsJobStatus.CANCELLED,
                                actualVoiceUsed = null,
                                attempts = attempts.toList(),
                                errorCategory = TtsErrorCategory.CANCELLED,
                                errorMessage = "Batch cancelled by user"
                            )
                        )
                        eventLogger.logTargetCancelled(
                            planId = batchId,
                            targetIndex = i + 1,
                            jobId = currentJob.id
                        )
                        cancelledCount++
                    } else {
                        results.add(
                            BatchTtsJobResult(
                                job = currentJob,
                                status = BatchTtsJobStatus.FAILED,
                                actualVoiceUsed = null,
                                attempts = attempts.toList(),
                                recoveredViaFallback = false,
                                errorCategory = lastCategory ?: TtsErrorCategory.UNKNOWN,
                                errorMessage = lastErrorMessage ?: "Generation failed after ${attempts.size} attempts"
                            )
                        )
                        eventLogger.logTargetFailed(
                            planId = batchId,
                            targetIndex = i + 1,
                            jobId = currentJob.id,
                            errorCategory = (lastCategory ?: TtsErrorCategory.UNKNOWN).name,
                            attemptsCount = attempts.size
                        )
                        failedCount++
                    }

                    completedCount++
                    checkpointStore?.save(
                        BatchTtsCheckpoint(
                            batchId = batchId,
                            packageName = packageName,
                            overwriteExisting = overwriteExisting,
                            records = results.map { result ->
                                BatchTtsCheckpointRecord(result.job.id, result.job.textFingerprint(), result.status.name, result.assetRelativePath)
                            }
                        )
                    )
                    eventLogger.logCheckpointWritten(
                        planId = batchId,
                        targetIndex = i + 1,
                        recordsCount = results.size
                    )

                    val elapsedNow = System.currentTimeMillis() - startTimeMillis
                    val eta = calculateEta(elapsedNow, completedCount, total)

                    onProgress(
                        BatchTtsSummary(
                            totalJobs = total,
                            completedJobs = completedCount,
                            successCount = successCount,
                            skippedCount = 0,
                            failedCount = failedCount,
                            cancelledCount = cancelledCount,
                            currentJob = if (i + 1 < jobs.size && !cancelFlag.get()) jobs[i + 1] else null,
                            jobResults = results.toList(),
                            isFinished = completedCount == total || cancelFlag.get(),
                            isCancelled = cancelFlag.get(),
                            elapsedMillis = elapsedNow,
                            estimatedRemainingMillis = eta
                        )
                    )
                }

                // If cancellation was requested, mark all remaining pending jobs as CANCELLED
                if (cancelFlag.get() || completedCount < total) {
                    val completedJobIds = results.map { it.job.id }.toSet()
                    for ((remIndex, remJob) in jobs.withIndex()) {
                        if (remJob.id !in completedJobIds) {
                            results.add(
                                BatchTtsJobResult(
                                    job = remJob,
                                    status = BatchTtsJobStatus.CANCELLED,
                                    errorCategory = TtsErrorCategory.CANCELLED,
                                    errorMessage = "Batch cancelled by user"
                                )
                            )
                            eventLogger.logTargetCancelled(
                                planId = batchId,
                                targetIndex = remIndex + 1,
                                jobId = remJob.id
                            )
                            cancelledCount++
                            completedCount++
                        }
                    }
                    checkpointStore?.save(
                        BatchTtsCheckpoint(
                            batchId = batchId,
                            packageName = packageName,
                            overwriteExisting = overwriteExisting,
                            records = results.map { result ->
                                BatchTtsCheckpointRecord(result.job.id, result.job.textFingerprint(), result.status.name, result.assetRelativePath)
                            }
                        )
                    )
                }

                val finalElapsed = System.currentTimeMillis() - startTimeMillis

                // Final broadcast
                onProgress(
                    BatchTtsSummary(
                        totalJobs = total,
                        completedJobs = completedCount,
                        successCount = successCount,
                        skippedCount = 0,
                        failedCount = failedCount,
                        cancelledCount = cancelledCount,
                        currentJob = null,
                        jobResults = results.toList(),
                        isFinished = true,
                        isCancelled = cancelFlag.get(),
                        elapsedMillis = finalElapsed,
                        estimatedRemainingMillis = 0L
                    )
                )

                if (cancelFlag.get()) {
                    eventLogger.logCancelAcknowledged(batchId, currentTargetIndex.get())
                    eventLogger.logBatchCancelled(
                        planId = batchId,
                        completedCount = completedCount,
                        cancelledCount = cancelledCount
                    )
                } else {
                    eventLogger.logBatchCompleted(
                        planId = batchId,
                        totalCount = total,
                        successCount = successCount,
                        failedCount = failedCount,
                        skippedCount = 0,
                        cancelledCount = cancelledCount
                    )
                }
                if (!cancelFlag.get() && completedCount == total && failedCount == 0) {
                    checkpointStore?.clear()
                }
            } catch (ce: CancellationException) {
                // Cancellation is a normal lifecycle transition, NOT a fatal error
                cancelFlag.set(true)
                eventLogger.logCancelAcknowledged(batchId, currentTargetIndex.get())

                val completedJobIds = results.map { it.job.id }.toSet()
                for ((remIndex, remJob) in jobs.withIndex()) {
                    if (remJob.id !in completedJobIds) {
                        results.add(
                            BatchTtsJobResult(
                                job = remJob,
                                status = BatchTtsJobStatus.CANCELLED,
                                errorCategory = TtsErrorCategory.CANCELLED,
                                errorMessage = "Batch cancelled by user"
                            )
                        )
                        eventLogger.logTargetCancelled(
                            planId = batchId,
                            targetIndex = remIndex + 1,
                            jobId = remJob.id
                        )
                        cancelledCount++
                        completedCount++
                    }
                }

                checkpointStore?.save(
                    BatchTtsCheckpoint(
                        batchId = batchId,
                        packageName = packageName,
                        overwriteExisting = overwriteExisting,
                        records = results.map { result ->
                            BatchTtsCheckpointRecord(result.job.id, result.job.textFingerprint(), result.status.name, result.assetRelativePath)
                        }
                    )
                )

                val finalElapsed = System.currentTimeMillis() - startTimeMillis
                onProgress(
                    BatchTtsSummary(
                        totalJobs = total,
                        completedJobs = completedCount,
                        successCount = successCount,
                        skippedCount = 0,
                        failedCount = failedCount,
                        cancelledCount = cancelledCount,
                        currentJob = null,
                        jobResults = results.toList(),
                        isFinished = true,
                        isCancelled = true,
                        currentOperation = "Đã hủy phiên tạo audio",
                        elapsedMillis = finalElapsed,
                        estimatedRemainingMillis = 0L
                    )
                )
                eventLogger.logBatchCancelled(
                    planId = batchId,
                    completedCount = completedCount,
                    cancelledCount = cancelledCount
                )
            } catch (fatal: Throwable) {
                eventLogger.logBatchFatal(
                    planId = batchId,
                    reason = fatal.message ?: fatal::class.simpleName ?: "Fatal batch error"
                )
                throw fatal
            } finally {
                checkpointOwnership?.close()
            }
        }

        return activeJob!!
    }

    companion object {
        private fun calculateEta(elapsedMillis: Long, completed: Int, total: Int): Long? {
            if (completed <= 0 || elapsedMillis < 500L || completed >= total) return null
            val avgPerTarget = elapsedMillis.toDouble() / completed.toDouble()
            val remaining = (total - completed).toDouble() * avgPerTarget
            return remaining.toLong().coerceAtLeast(0L)
        }

        private fun isRetryable(category: TtsErrorCategory): Boolean = category in setOf(
            TtsErrorCategory.NETWORK_UNAVAILABLE,
            TtsErrorCategory.TIMEOUT,
            TtsErrorCategory.VOICE_UNAVAILABLE,
            TtsErrorCategory.GENERATION_FAILED,
            TtsErrorCategory.UNKNOWN
        )

        /**
         * Classifies an exception into friendly category and UI message.
         */
        fun classifyError(throwable: Throwable): Pair<TtsErrorCategory, String> {
            return when (throwable) {
                is TtsException -> when (val err = throwable.error) {
                    is TtsError.NoNetwork -> TtsErrorCategory.NETWORK_UNAVAILABLE to "Network connection unavailable"
                    is TtsError.Timeout -> TtsErrorCategory.TIMEOUT to "Request timed out"
                    is TtsError.VoiceUnavailable -> TtsErrorCategory.VOICE_UNAVAILABLE to "Voice '${err.voiceId}' is unavailable"
                    is TtsError.ProviderUnavailable -> TtsErrorCategory.GENERATION_FAILED to "TTS provider unavailable: ${err.details}"
                    is TtsError.GenerationFailed -> TtsErrorCategory.GENERATION_FAILED to err.details
                    is TtsError.OutputWriteFailed -> TtsErrorCategory.OUTPUT_WRITE_FAILED to "Failed to write output audio: ${err.details}"
                    is TtsError.InvalidText -> TtsErrorCategory.INVALID_TEXT to "Invalid text: ${err.reason}"
                    is TtsError.Cancelled -> TtsErrorCategory.CANCELLED to "Operation cancelled"
                }
                is SocketTimeoutException -> TtsErrorCategory.TIMEOUT to "Connection timed out"
                is IOException -> TtsErrorCategory.NETWORK_UNAVAILABLE to "I/O or network communication error: ${throwable.message ?: "Unknown I/O error"}"
                is CancellationException -> TtsErrorCategory.CANCELLED to "Operation cancelled"
                else -> TtsErrorCategory.UNKNOWN to (throwable.message ?: "Unexpected error: ${throwable::class.simpleName}")
            }
        }
    }
}
