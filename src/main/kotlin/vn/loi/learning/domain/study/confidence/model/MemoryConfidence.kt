package vn.loi.learning.domain.study.confidence.model

import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating

@JvmInline
value class MemoryConfidenceScore private constructor(val value: Int) :
    Comparable<MemoryConfidenceScore> {
    override fun compareTo(other: MemoryConfidenceScore): Int = value.compareTo(other.value)

    companion object {
        fun of(value: Int): MemoryConfidenceScore {
            require(value in 0..100) { "Memory confidence score must be between 0 and 100." }
            return MemoryConfidenceScore(value)
        }

        fun bounded(value: Int): MemoryConfidenceScore = MemoryConfidenceScore(value.coerceIn(0, 100))
    }
}

enum class MemoryConfidenceTier {
    VERY_LOW,
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH;

    companion object {
        fun from(score: MemoryConfidenceScore): MemoryConfidenceTier =
            when (score.value) {
                in 0..19 -> VERY_LOW
                in 20..39 -> LOW
                in 40..59 -> MEDIUM
                in 60..79 -> HIGH
                else -> VERY_HIGH
            }
    }
}

enum class MemoryConfidenceSpacingBand {
    FIRST,
    IMMEDIATE,
    SHORT,
    MEDIUM,
    LONG,
    VERY_LONG
}

enum class MemoryConfidenceReason {
    NO_DURABLE_EVIDENCE,
    FIRST_REVIEW_SUCCESS,
    IMMEDIATE_SUCCESS,
    SPACED_SUCCESS,
    LONG_SPACED_SUCCESS,
    RECALL_FAILURE,
    EFFORTFUL_RECALL,
    CONSISTENT_SUCCESS,
    UNRELIABLE_HISTORY
}

data class MemoryConfidenceEvidence(
    val rating: ReviewRating,
    val occurredAt: Moment,
    val stageBefore: LearningStage? = null,
    val stageAfter: LearningStage? = null,
    val reliable: Boolean = true
)

data class MemoryConfidence(
    val score: MemoryConfidenceScore,
    val tier: MemoryConfidenceTier,
    val evaluatedReviewCount: Int,
    val reliable: Boolean,
    val primaryReason: MemoryConfidenceReason,
    val supportingReasons: Set<MemoryConfidenceReason> = emptySet()
)

data class MemoryConfidenceProjection(
    val previousConfidence: MemoryConfidence,
    val projectedConfidence: MemoryConfidence,
    val delta: Int,
    val appliedSpacingBand: MemoryConfidenceSpacingBand?,
    val pendingEvidenceApplied: Boolean
)
