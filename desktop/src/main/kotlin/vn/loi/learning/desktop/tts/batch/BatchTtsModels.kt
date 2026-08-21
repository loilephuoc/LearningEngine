package vn.loi.learning.desktop.tts.batch

import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice

/**
 * Lifecycle states of an individual TTS batch job.
 */
enum class BatchTtsJobStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    SKIPPED,
    FAILED,
    CANCELLED
}

/**
 * User-friendly classification of TTS errors for UI reporting and retry management.
 */
enum class TtsErrorCategory(val displayLabel: String) {
    NETWORK_UNAVAILABLE("Network unavailable"),
    TIMEOUT("Timeout"),
    VOICE_UNAVAILABLE("Voice unavailable"),
    GENERATION_FAILED("Generation failed"),
    OUTPUT_WRITE_FAILED("Output write failed"),
    CANCELLED("Cancelled"),
    UNKNOWN("Unknown error")
}

/**
 * A target field identified during scanning of content items.
 */
data class BatchTtsTarget(
    val contentId: String,
    val field: TtsField,
    val text: String,
    val language: TtsLanguage,
    val isMissing: Boolean,
    val hasAudio: Boolean
) {
    val canGenerate: Boolean
        get() = isMissing && text.isNotBlank()
}

/**
 * Executable batch job representation.
 */
data class BatchTtsJob(
    val contentId: String,
    val field: TtsField,
    val text: String,
    val language: TtsLanguage,
    val voice: TtsVoice,
    val rate: Int = 0,
    val id: String = "${contentId}_${field.name.lowercase()}"
)

/**
 * Execution result for an individual batch job.
 */
data class BatchTtsJobResult(
    val job: BatchTtsJob,
    val status: BatchTtsJobStatus,
    val assetRelativePath: String? = null,
    val errorCategory: TtsErrorCategory? = null,
    val errorMessage: String? = null
)

/**
 * Aggregated summary of batch execution progress and final results.
 */
data class BatchTtsSummary(
    val totalJobs: Int,
    val completedJobs: Int = 0,
    val successCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val cancelledCount: Int = 0,
    val currentJob: BatchTtsJob? = null,
    val jobResults: List<BatchTtsJobResult> = emptyList(),
    val isFinished: Boolean = false,
    val isCancelled: Boolean = false
) {
    val hasFailures: Boolean
        get() = failedCount > 0

    val failedResults: List<BatchTtsJobResult>
        get() = jobResults.filter { it.status == BatchTtsJobStatus.FAILED }

    companion object {
        fun initial(total: Int, skippedCount: Int = 0): BatchTtsSummary =
            BatchTtsSummary(
                totalJobs = total,
                completedJobs = skippedCount,
                skippedCount = skippedCount
            )
    }
}
