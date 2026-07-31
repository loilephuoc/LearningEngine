package vn.loi.learning.application.confidence

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.confidence.model.MemoryConfidence
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceProjection
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceReason
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceScore
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceTier
import vn.loi.learning.domain.study.memory.model.ReviewRating

class MemoryConfidenceRatingGateTest {
    @Test
    fun `non Easy candidates pass through without confidence`() {
        listOf(ReviewRating.AGAIN, ReviewRating.HARD, ReviewRating.GOOD).forEach {
            assertEquals(it, MemoryConfidenceRatingGate.apply(it, null))
        }
    }

    @Test
    fun `Easy requires reliable projected High or Very High confidence`() {
        assertEquals(ReviewRating.EASY, gate(MemoryConfidenceTier.HIGH))
        assertEquals(ReviewRating.EASY, gate(MemoryConfidenceTier.VERY_HIGH))
        assertEquals(ReviewRating.GOOD, gate(MemoryConfidenceTier.MEDIUM))
        assertEquals(ReviewRating.GOOD, gate(MemoryConfidenceTier.LOW))
        assertEquals(ReviewRating.GOOD, gate(MemoryConfidenceTier.VERY_LOW))
        assertEquals(ReviewRating.GOOD, MemoryConfidenceRatingGate.apply(ReviewRating.EASY, null))
        assertEquals(ReviewRating.GOOD, gate(MemoryConfidenceTier.HIGH, reliable = false))
    }

    @Test
    fun `gate uses projected rather than previous confidence`() {
        assertEquals(
            ReviewRating.EASY,
            MemoryConfidenceRatingGate.apply(
                ReviewRating.EASY,
                projection(MemoryConfidenceTier.MEDIUM, MemoryConfidenceTier.HIGH)
            )
        )
        assertEquals(
            ReviewRating.GOOD,
            MemoryConfidenceRatingGate.apply(
                ReviewRating.EASY,
                projection(MemoryConfidenceTier.HIGH, MemoryConfidenceTier.MEDIUM)
            )
        )
    }

    private fun gate(
        tier: MemoryConfidenceTier,
        reliable: Boolean = true
    ): ReviewRating =
        MemoryConfidenceRatingGate.apply(
            ReviewRating.EASY,
            projection(tier, tier, reliable)
        )

    private fun projection(
        previous: MemoryConfidenceTier,
        projected: MemoryConfidenceTier,
        reliable: Boolean = true
    ): MemoryConfidenceProjection =
        MemoryConfidenceProjection(
            previousConfidence = confidence(previous, reliable),
            projectedConfidence = confidence(projected, reliable),
            delta = 0,
            appliedSpacingBand = null,
            pendingEvidenceApplied = true
        )

    private fun confidence(
        tier: MemoryConfidenceTier,
        reliable: Boolean
    ): MemoryConfidence {
        val score =
            when (tier) {
                MemoryConfidenceTier.VERY_LOW -> 0
                MemoryConfidenceTier.LOW -> 20
                MemoryConfidenceTier.MEDIUM -> 40
                MemoryConfidenceTier.HIGH -> 60
                MemoryConfidenceTier.VERY_HIGH -> 80
            }
        return MemoryConfidence(
            score = MemoryConfidenceScore.of(score),
            tier = tier,
            evaluatedReviewCount = 1,
            reliable = reliable,
            primaryReason = MemoryConfidenceReason.SPACED_SUCCESS
        )
    }
}
