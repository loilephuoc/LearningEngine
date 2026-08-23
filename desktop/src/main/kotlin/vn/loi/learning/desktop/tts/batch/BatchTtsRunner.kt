package vn.loi.learning.desktop.tts.batch

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsError
import vn.loi.learning.desktop.tts.TtsException
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.strategy.VoiceAttempt

/**
 * Sequential runner for batch TTS jobs with real-time progress, multi-attempt fallback chains,
 * error isolation, cancellation support, and Generate != Apply boundary.
 */
class BatchTtsRunner(
    private val ttsService: DesktopTtsAudioService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val policy: BatchTtsExecutionPolicy = BatchTtsExecutionPolicy(),
    private val checkpointStore: BatchTtsCheckpointStore? = null,
    private val assetExists: (String) -> Boolean = { true },
    private val checkpointOwnership: AutoCloseable? = null
) {
    private val cancelFlag = AtomicBoolean(false)
    private var activeJob: Job? = null

    /**
     * Triggers cooperative cancellation of the running batch.
     */
    fun cancel() {
        cancelFlag.set(true)
        activeJob?.cancel()
    }

    val isCancelled: Boolean
        get() = cancelFlag.get()

    /**
     * Executes a list of batch jobs sequentially with fallback support.
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

        activeJob = scope.launch {
          try {
            // Initial broadcast
            onProgress(
                BatchTtsSummary(
                    totalJobs = total,
                    completedJobs = 0,
                    successCount = 0,
                    skippedCount = 0,
                    failedCount = 0,
                    cancelledCount = 0,
                    currentJob = jobs.firstOrNull()
                )
            )

            for (i in jobs.indices) {
                if (cancelFlag.get()) {
                    // Mark remaining jobs as cancelled
                    for (remIndex in i until jobs.size) {
                        val remJob = jobs[remIndex]
                        results.add(
                            BatchTtsJobResult(
                                job = remJob,
                                status = BatchTtsJobStatus.CANCELLED,
                                errorCategory = TtsErrorCategory.CANCELLED,
                                errorMessage = "Batch cancelled by user"
                            )
                        )
                        cancelledCount++
                        completedCount++
                    }
                    break
                }

                val currentJob = jobs[i]

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
                    successCount++
                    completedCount++
                    onProgress(summary(total, completedCount, successCount, failedCount, cancelledCount, results, jobs.getOrNull(i + 1)))
                    continue
                }

                // Notify running
                onProgress(
                    BatchTtsSummary(
                        totalJobs = total,
                        completedJobs = completedCount,
                        successCount = successCount,
                        skippedCount = 0,
                        failedCount = failedCount,
                        cancelledCount = cancelledCount,
                        currentJob = currentJob,
                        jobResults = results.toList()
                    )
                )

                val attempts = mutableListOf<VoiceAttempt>()
                var jobSuccess = false
                var successVoice: TtsVoice? = null
                var successAssetPath: String? = null
                var lastCategory: TtsErrorCategory? = null
                var lastErrorMessage: String? = null
                var stopFallback = false

                val candidateChain = (if (currentJob.candidateVoices.isNotEmpty()) {
                    currentJob.candidateVoices
                } else {
                    listOf(currentJob.voice)
                }).filter { it.language.equals(currentJob.language.code, true) || it.locale.startsWith(currentJob.language.code, true) }
                    .distinctBy { it.id }
                    .take(policy.maxCandidateVoices)

                for (voiceCandidate in candidateChain) {
                    if (cancelFlag.get()) break

                    for (attemptNumber in 1..policy.maxAttemptsPerVoice) {
                      if (cancelFlag.get()) break
                      try {
                        val asset = withContext(Dispatchers.IO) { withTimeout(policy.attemptTimeoutMillis) { ttsService.generatePermanentAudio(
                            contentId = currentJob.contentId,
                            packageName = packageName,
                            field = currentJob.field,
                            text = currentJob.text,
                            voice = voiceCandidate,
                            rate = currentJob.rate,
                            pitch = currentJob.pitch,
                            volume = currentJob.volume
                        ) } }

                        // Synthesis succeeded with this voice
                        attempts.add(VoiceAttempt(voice = voiceCandidate, isSuccess = true))
                        jobSuccess = true
                        successVoice = voiceCandidate
                        successAssetPath = asset.relativePath

                        onApply?.invoke(currentJob.contentId, currentJob.field, asset.relativePath)
                        break
                      } catch (te: TimeoutCancellationException) {
                        lastCategory = TtsErrorCategory.TIMEOUT
                        lastErrorMessage = "Attempt timed out after ${policy.attemptTimeoutMillis} ms"
                        attempts.add(VoiceAttempt(voiceCandidate, false, lastCategory, lastErrorMessage))
                      } catch (ce: CancellationException) {
                        attempts.add(
                            VoiceAttempt(
                                voice = voiceCandidate,
                                isSuccess = false,
                                errorCategory = TtsErrorCategory.CANCELLED,
                                errorMessage = "Operation cancelled"
                            )
                        )
                        cancelFlag.set(true)
                        break
                    } catch (ex: Exception) {
                        val (category, message) = classifyError(ex)
                        lastCategory = category
                        lastErrorMessage = message
                        attempts.add(
                            VoiceAttempt(
                                voice = voiceCandidate,
                                isSuccess = false,
                                errorCategory = category,
                                errorMessage = message
                            )
                        )
                        // Non-retryable errors (invalid text, disk output failure, cancellation) should not trigger further voice fallbacks
                        if (category == TtsErrorCategory.CANCELLED ||
                            category == TtsErrorCategory.OUTPUT_WRITE_FAILED ||
                            (ex is TtsException && ex.error is TtsError.InvalidText)
                        ) {
                            stopFallback = true
                            break
                        }
                        if (!isRetryable(category)) break
                      }
                      if (jobSuccess || cancelFlag.get()) break
                      if (attemptNumber < policy.maxAttemptsPerVoice) {
                        val retryDelay = (policy.initialRetryDelayMillis * (1L shl (attemptNumber - 1)))
                            .coerceAtMost(policy.maxRetryDelayMillis)
                        if (retryDelay > 0) delay(retryDelay)
                      }
                    }
                    if (jobSuccess || cancelFlag.get() || stopFallback) break
                }

                if (jobSuccess && successVoice != null && successAssetPath != null) {
                    val recoveredViaFallback = successVoice.id != currentJob.requestedVoice.id
                    results.add(
                        BatchTtsJobResult(
                            job = currentJob,
                            status = BatchTtsJobStatus.SUCCESS,
                            assetRelativePath = successAssetPath,
                            actualVoiceUsed = successVoice,
                            attempts = attempts.toList(),
                            recoveredViaFallback = recoveredViaFallback
                        )
                    )
                    successCount++
                } else if (cancelFlag.get()) {
                    results.add(
                        BatchTtsJobResult(
                            job = currentJob,
                            status = BatchTtsJobStatus.CANCELLED,
                            actualVoiceUsed = null,
                            attempts = attempts.toList(),
                            errorCategory = TtsErrorCategory.CANCELLED,
                            errorMessage = "Job cancelled"
                        )
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

                // Broadcast progress after item completion
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
                        isCancelled = cancelFlag.get()
                    )
                )
            }

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
                    isCancelled = cancelFlag.get()
                )
            )
            if (!cancelFlag.get() && completedCount == total && failedCount == 0) checkpointStore?.clear()
          } finally {
            checkpointOwnership?.close()
          }
        }

        return activeJob!!
    }

    companion object {
        private fun isRetryable(category: TtsErrorCategory): Boolean = category in setOf(
            TtsErrorCategory.NETWORK_UNAVAILABLE,
            TtsErrorCategory.TIMEOUT,
            TtsErrorCategory.VOICE_UNAVAILABLE,
            TtsErrorCategory.GENERATION_FAILED,
            TtsErrorCategory.UNKNOWN
        )

        private fun summary(
            total: Int, completed: Int, successes: Int, failures: Int, cancelled: Int,
            results: List<BatchTtsJobResult>, current: BatchTtsJob?
        ) = BatchTtsSummary(total, completed, successes, 0, failures, cancelled, current, results.toList(), completed == total, false)
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
