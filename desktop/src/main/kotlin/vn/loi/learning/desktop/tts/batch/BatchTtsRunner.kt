package vn.loi.learning.desktop.tts.batch

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import vn.loi.learning.desktop.tts.DesktopTtsAudioService
import vn.loi.learning.desktop.tts.TtsError
import vn.loi.learning.desktop.tts.TtsException
import vn.loi.learning.desktop.tts.TtsField

/**
 * Sequential runner for batch TTS jobs with real-time progress, error isolation,
 * cancellation support, and friendly error classification.
 */
class BatchTtsRunner(
    private val ttsService: DesktopTtsAudioService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
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
     * Executes a list of batch jobs sequentially.
     *
     * @param jobs The batch jobs to execute.
     * @param packageName Target package name.
     * @param onApply Callback invoked upon each successful generation to persist the audio reference.
     * @param onProgress Callback invoked on each state change / step.
     */
    fun runBatch(
        jobs: List<BatchTtsJob>,
        packageName: String,
        onApply: (contentId: String, field: TtsField, audioRef: String) -> Unit,
        onProgress: (BatchTtsSummary) -> Unit
    ): Job {
        cancelFlag.set(false)
        val total = jobs.size
        val results = mutableListOf<BatchTtsJobResult>()

        var successCount = 0
        var failedCount = 0
        var cancelledCount = 0
        var completedCount = 0

        activeJob = scope.launch {
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

                try {
                    val asset = ttsService.generatePermanentAudio(
                        contentId = currentJob.contentId,
                        packageName = packageName,
                        field = currentJob.field,
                        text = currentJob.text,
                        voice = currentJob.voice,
                        rate = currentJob.rate
                    )

                    // Apply audio reference
                    onApply(currentJob.contentId, currentJob.field, asset.relativePath)

                    results.add(
                        BatchTtsJobResult(
                            job = currentJob,
                            status = BatchTtsJobStatus.SUCCESS,
                            assetRelativePath = asset.relativePath
                        )
                    )
                    successCount++
                } catch (ce: CancellationException) {
                    results.add(
                        BatchTtsJobResult(
                            job = currentJob,
                            status = BatchTtsJobStatus.CANCELLED,
                            errorCategory = TtsErrorCategory.CANCELLED,
                            errorMessage = "Job cancelled"
                        )
                    )
                    cancelledCount++
                    cancelFlag.set(true)
                } catch (ex: Exception) {
                    val (category, message) = classifyError(ex)
                    results.add(
                        BatchTtsJobResult(
                            job = currentJob,
                            status = BatchTtsJobStatus.FAILED,
                            errorCategory = category,
                            errorMessage = message
                        )
                    )
                    failedCount++
                }

                completedCount++

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
        }

        return activeJob!!
    }

    companion object {
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
                    is TtsError.InvalidText -> TtsErrorCategory.GENERATION_FAILED to "Invalid text: ${err.reason}"
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
