package vn.loi.learning.desktop.tts.batch

import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.TtsLanguage
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.tts.strategy.VoiceAttempt

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
    val hasAudio: Boolean,
    val previousAudioRef: String? = null
) {
    val canGenerate: Boolean
        get() = isMissing && text.isNotBlank()
}

/**
 * Comprehensive analysis of a selected item scope across chosen audio fields.
 */
data class BatchTtsScopeScan(
    val totalSelectedItems: Int,
    val selectedFields: Set<TtsField>,
    val validTargets: List<BatchTtsTarget>,
    val missingCountByField: Map<TtsField, Int>,
    val existingAudioSkippedCount: Int,
    val emptyTextSkippedCount: Int,
    val englishTargetsCount: Int,
    val vietnameseTargetsCount: Int,
    val representativeEnglishText: String?,
    val representativeVietnameseText: String?
) {
    val totalValidTargets: Int get() = validTargets.size
    val hasTargets: Boolean get() = validTargets.isNotEmpty()
}

/**
 * Executable batch job representation with candidate voice chain for fallback / rotation.
 */
data class BatchTtsJob(
    val contentId: String,
    val field: TtsField,
    val text: String,
    val language: TtsLanguage,
    val voice: TtsVoice,
    val rate: Int = 0,
    val pitch: String? = null,
    val volume: String? = null,
    val previousAudioRef: String? = null,
    val candidateVoices: List<TtsVoice> = listOf(voice),
    val id: String = "${contentId}_${field.name.lowercase()}"
) {
    val requestedVoice: TtsVoice get() = voice
}

/**
 * Execution result for an individual batch job including attempt history and fallback recovery details.
 */
data class BatchTtsJobResult(
    val job: BatchTtsJob,
    val status: BatchTtsJobStatus,
    val assetRelativePath: String? = null,
    val actualVoiceUsed: TtsVoice? = null,
    val attempts: List<VoiceAttempt> = emptyList(),
    val recoveredViaFallback: Boolean = false,
    val errorCategory: TtsErrorCategory? = null,
    val errorMessage: String? = null
) {
    val requestedVoice: TtsVoice get() = job.requestedVoice
}

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

    val successfulResults: List<BatchTtsJobResult>
        get() = jobResults.filter { it.status == BatchTtsJobStatus.SUCCESS && it.assetRelativePath != null }

    val failedResults: List<BatchTtsJobResult>
        get() = jobResults.filter { it.status == BatchTtsJobStatus.FAILED }

    val fallbackRecoveredCount: Int
        get() = jobResults.count { it.status == BatchTtsJobStatus.SUCCESS && it.recoveredViaFallback }

    companion object {
        fun initial(total: Int, skippedCount: Int = 0): BatchTtsSummary =
            BatchTtsSummary(
                totalJobs = total,
                completedJobs = skippedCount,
                skippedCount = skippedCount
            )
    }
}

/**
 * Record of a single field change within an atomic TTS batch apply for undo.
 */
data class BatchTtsUndoEntry(
    val contentId: String,
    val field: TtsField,
    val previousAudioRef: String?,
    val appliedAudioRef: String
)

/**
 * Snapshot of an applied batch TTS operation enabling full atomic undo and safe file cleanup.
 */
data class BatchTtsUndoSnapshot(
    val packageName: String,
    val entries: List<BatchTtsUndoEntry>,
    val newlyCreatedAssetPaths: Set<String>,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalApplied: Int get() = entries.size
    val displayLabel: String get() = "Batch TTS ($totalApplied audio targets)"
}
