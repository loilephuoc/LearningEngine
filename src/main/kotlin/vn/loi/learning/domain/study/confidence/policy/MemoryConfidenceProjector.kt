package vn.loi.learning.domain.study.confidence.policy

import vn.loi.learning.domain.study.confidence.model.MemoryConfidence
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceEvidence
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceProjection
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceReason
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceScore
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceSpacingBand
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceTier
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewRating

object MemoryConfidenceProjector {
    private const val TWELVE_HOURS = 12L * 60L * 60L * 1_000L
    private const val THREE_DAYS = 3L * 24L * 60L * 60L * 1_000L
    private const val FOURTEEN_DAYS = 14L * 24L * 60L * 60L * 1_000L
    private const val SIXTY_DAYS = 60L * 24L * 60L * 60L * 1_000L

    fun project(
        history: List<ReviewEvent>,
        pendingEvidence: MemoryConfidenceEvidence? = null
    ): MemoryConfidenceProjection {
        val canonical = canonicalize(history)
        val reliable = validateContinuity(canonical)
        val durable = evaluate(canonical.map(::toEvidence), reliable)
        if (pendingEvidence == null) {
            return MemoryConfidenceProjection(durable, durable, 0, null, false)
        }
        val combined = canonical.map(::toEvidence) + pendingEvidence
        val projected = evaluate(combined, reliable && pendingEvidence.reliable)
        return MemoryConfidenceProjection(
            previousConfidence = durable,
            projectedConfidence = projected,
            delta = projected.score.value - durable.score.value,
            appliedSpacingBand =
                spacingBand(combined.getOrNull(combined.lastIndex - 1), pendingEvidence),
            pendingEvidenceApplied = true
        )
    }

    private fun canonicalize(history: List<ReviewEvent>): List<ReviewEvent> {
        if (history.isEmpty()) return emptyList()
        require(history.map { it.learnerId }.distinct().size == 1) {
            "Memory confidence history must belong to one learner."
        }
        require(history.map { it.learningItemId }.distinct().size == 1) {
            "Memory confidence history must belong to one learning item."
        }
        val duplicateIds = history.groupBy { it.id }.filterValues { it.size > 1 }
        require(duplicateIds.isEmpty()) { "Memory confidence history contains duplicate event IDs." }
        return history.sortedWith(compareBy<ReviewEvent>({ it.reviewedAt }, { it.id.value }))
    }

    private fun validateContinuity(history: List<ReviewEvent>): Boolean =
        history.zipWithNext().all { (previous, next) ->
            previous.stateAfter == next.stateBefore &&
                next.stateAfter.reviewCount == previous.stateAfter.reviewCount + 1
        }

    private fun evaluate(
        evidence: List<MemoryConfidenceEvidence>,
        reliable: Boolean
    ): MemoryConfidence {
        if (evidence.isEmpty()) {
            return confidence(0, 0, true, MemoryConfidenceReason.NO_DURABLE_EVIDENCE)
        }
        var score = 0
        var lastReason = MemoryConfidenceReason.NO_DURABLE_EVIDENCE
        var spacedSuccessStreak = 0
        val supporting = linkedSetOf<MemoryConfidenceReason>()
        evidence.forEachIndexed { index, current ->
            val previous = evidence.getOrNull(index - 1)
            val band = spacingBand(previous, current)
            val success = current.rating in setOf(ReviewRating.GOOD, ReviewRating.EASY)
            val spacingBonus = if (success) spacingBonus(band) else 0
            spacedSuccessStreak =
                if (success && band !in setOf(MemoryConfidenceSpacingBand.FIRST, MemoryConfidenceSpacingBand.IMMEDIATE)) {
                    spacedSuccessStreak + 1
                } else if (success) {
                    0
                } else {
                    0
                }
            val consistencyBonus = if (spacedSuccessStreak >= 2) 2 else 0
            val delta =
                when (current.rating) {
                    ReviewRating.AGAIN -> -14
                    ReviewRating.HARD -> -5
                    ReviewRating.GOOD -> 8 + spacingBonus + consistencyBonus
                    ReviewRating.EASY -> 12 + spacingBonus + consistencyBonus
                }
            score = (score + delta).coerceIn(0, 100)
            lastReason =
                when {
                    current.rating == ReviewRating.AGAIN -> MemoryConfidenceReason.RECALL_FAILURE
                    current.rating == ReviewRating.HARD -> MemoryConfidenceReason.EFFORTFUL_RECALL
                    band == MemoryConfidenceSpacingBand.FIRST ->
                        MemoryConfidenceReason.FIRST_REVIEW_SUCCESS
                    band == MemoryConfidenceSpacingBand.IMMEDIATE ->
                        MemoryConfidenceReason.IMMEDIATE_SUCCESS
                    band in setOf(
                        MemoryConfidenceSpacingBand.LONG,
                        MemoryConfidenceSpacingBand.VERY_LONG
                    ) -> MemoryConfidenceReason.LONG_SPACED_SUCCESS
                    else -> MemoryConfidenceReason.SPACED_SUCCESS
                }
            if (consistencyBonus > 0) supporting += MemoryConfidenceReason.CONSISTENT_SUCCESS
        }
        if (!reliable) supporting += MemoryConfidenceReason.UNRELIABLE_HISTORY
        return confidence(
            score,
            evidence.size,
            reliable,
            if (reliable) lastReason else MemoryConfidenceReason.UNRELIABLE_HISTORY,
            supporting
        )
    }

    private fun spacingBand(
        previous: MemoryConfidenceEvidence?,
        current: MemoryConfidenceEvidence
    ): MemoryConfidenceSpacingBand {
        if (previous == null) return MemoryConfidenceSpacingBand.FIRST
        val elapsed = (current.occurredAt.epochMillis - previous.occurredAt.epochMillis).coerceAtLeast(0L)
        return when {
            elapsed < TWELVE_HOURS -> MemoryConfidenceSpacingBand.IMMEDIATE
            elapsed < THREE_DAYS -> MemoryConfidenceSpacingBand.SHORT
            elapsed < FOURTEEN_DAYS -> MemoryConfidenceSpacingBand.MEDIUM
            elapsed < SIXTY_DAYS -> MemoryConfidenceSpacingBand.LONG
            else -> MemoryConfidenceSpacingBand.VERY_LONG
        }
    }

    private fun spacingBonus(band: MemoryConfidenceSpacingBand): Int =
        when (band) {
            MemoryConfidenceSpacingBand.FIRST,
            MemoryConfidenceSpacingBand.IMMEDIATE -> 0
            MemoryConfidenceSpacingBand.SHORT -> 2
            MemoryConfidenceSpacingBand.MEDIUM -> 7
            MemoryConfidenceSpacingBand.LONG -> 12
            MemoryConfidenceSpacingBand.VERY_LONG -> 18
        }

    private fun toEvidence(event: ReviewEvent): MemoryConfidenceEvidence =
        MemoryConfidenceEvidence(
            rating = event.rating,
            occurredAt = event.reviewedAt,
            stageBefore = event.stateBefore.stage,
            stageAfter = event.stateAfter.stage
        )

    private fun confidence(
        score: Int,
        count: Int,
        reliable: Boolean,
        reason: MemoryConfidenceReason,
        supporting: Set<MemoryConfidenceReason> = emptySet()
    ): MemoryConfidence {
        val value = MemoryConfidenceScore.bounded(score)
        return MemoryConfidence(
            score = value,
            tier = MemoryConfidenceTier.from(value),
            evaluatedReviewCount = count,
            reliable = reliable,
            primaryReason = reason,
            supportingReasons = supporting
        )
    }
}
