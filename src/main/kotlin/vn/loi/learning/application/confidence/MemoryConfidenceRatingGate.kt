package vn.loi.learning.application.confidence

import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceProjection
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceTier
import vn.loi.learning.domain.study.memory.model.ReviewRating

object MemoryConfidenceRatingGate {
    fun apply(
        candidate: ReviewRating,
        projection: MemoryConfidenceProjection?
    ): ReviewRating {
        if (candidate != ReviewRating.EASY) return candidate
        val confidence = projection?.projectedConfidence ?: return ReviewRating.GOOD
        if (!confidence.reliable) return ReviewRating.GOOD
        return if (
            confidence.tier in
                setOf(MemoryConfidenceTier.HIGH, MemoryConfidenceTier.VERY_HIGH)
        ) {
            ReviewRating.EASY
        } else {
            ReviewRating.GOOD
        }
    }
}
