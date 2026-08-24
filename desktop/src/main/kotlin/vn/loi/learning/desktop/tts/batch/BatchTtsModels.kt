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
    INVALID_TEXT("Invalid text"),
    GENERATION_FAILED("Generation failed"),
    OUTPUT_WRITE_FAILED("Output write failed"),
    CANCELLED("Cancelled"),
    UNKNOWN("Unknown error")
}

/**
 * Filter mode for target discovery and batch execution.
 */
enum class BatchTtsScope {
    MISSING_ONLY,
    ALL
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
 * Representative sample text with precise source attribution for preview in batch dialog.
 */
data class BatchTtsSample(
    val field: TtsField,
    val text: String,
    val contentId: String,
    val itemIndex: Int,
    val itemLabel: String
) {
    val displaySource: String get() = "Item #$itemIndex ($itemLabel)"
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
    val representativeVietnameseText: String?,
    val samplesByField: Map<TtsField, BatchTtsSample> = emptyMap(),
    val allSamplesByField: Map<TtsField, List<BatchTtsSample>> = emptyMap()
) {
    val totalValidTargets: Int get() = validTargets.size
    val hasTargets: Boolean get() = validTargets.isNotEmpty()

    fun sampleFor(field: TtsField): BatchTtsSample? =
        samplesByField[field] ?: allSamplesByField[field]?.firstOrNull()

    fun samplesFor(field: TtsField): List<BatchTtsSample> =
        allSamplesByField[field] ?: (samplesByField[field]?.let { listOf(it) } ?: emptyList())
}

data class BatchTtsLanguageRequirements(
    val requiresEnglish: Boolean,
    val requiresVietnamese: Boolean
) {
    fun configurationsValid(englishVoice: TtsVoice?, vietnameseVoice: TtsVoice?): Boolean =
        (!requiresEnglish || englishVoice != null) && (!requiresVietnamese || vietnameseVoice != null)

    companion object {
        fun from(selectedFields: Set<TtsField>): BatchTtsLanguageRequirements {
            val hasEnglish = selectedFields.any {
                BatchTtsLanguageResolver.resolveTargetLanguage(null, null, it) == TtsLanguage.ENGLISH
            }
            val hasVietnamese = selectedFields.any {
                BatchTtsLanguageResolver.resolveTargetLanguage(null, null, it) == TtsLanguage.VIETNAMESE
            }
            return BatchTtsLanguageRequirements(
                requiresEnglish = hasEnglish,
                requiresVietnamese = hasVietnamese
            )
        }

        fun from(scan: BatchTtsScopeScan): BatchTtsLanguageRequirements = from(scan.selectedFields)
    }
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
    val overwriteExisting: Boolean = false,
    val id: String = "${contentId}_${field.name.lowercase()}"
) {
    val requestedVoice: TtsVoice get() = voice

    fun textFingerprint(): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest("${field.name}\u0000${language.code}\u0000$text".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

data class BatchTtsExecutionPolicy(
    val attemptTimeoutMillis: Long = 12_000,
    val maxAttemptsPerVoice: Int = 2,
    val initialRetryDelayMillis: Long = 500,
    val maxRetryDelayMillis: Long = 2_000,
    val maxCandidateVoices: Int = 4
) {
    init {
        require(attemptTimeoutMillis > 0)
        require(maxAttemptsPerVoice > 0)
        require(initialRetryDelayMillis >= 0)
        require(maxRetryDelayMillis >= initialRetryDelayMillis)
        require(maxCandidateVoices > 0)
    }
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
    val voiceAttempts: List<VoiceAttempt> get() = attempts
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
    val isCancelled: Boolean = false,
    val currentOperation: String? = null,
    val currentVoiceName: String? = null,
    val currentCandidateIndex: Int = 0,
    val totalCandidatesForCurrentJob: Int = 0,
    val currentExecutingVoice: TtsVoice? = null,
    val elapsedMillis: Long = 0L,
    val estimatedRemainingMillis: Long? = null
) {
    val isCurrentCandidateFallback: Boolean
        get() = currentCandidateIndex > 1

    val hasFailures: Boolean
        get() = failedCount > 0

    val successfulResults: List<BatchTtsJobResult>
        get() = jobResults.filter { it.status == BatchTtsJobStatus.SUCCESS && it.assetRelativePath != null }

    val failedResults: List<BatchTtsJobResult>
        get() = jobResults.filter { it.status == BatchTtsJobStatus.FAILED }

    val fallbackRecoveredCount: Int
        get() = jobResults.count { it.status == BatchTtsJobStatus.SUCCESS && it.recoveredViaFallback }

    val progressPercent: Float
        get() = if (totalJobs > 0) (completedJobs.toFloat() / totalJobs.toFloat()).coerceIn(0f, 1f) else 0f

    val targetsPerSecond: Double
        get() = if (elapsedMillis > 500 && completedJobs > 0) (completedJobs.toDouble() * 1000.0) / elapsedMillis.toDouble() else 0.0

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
