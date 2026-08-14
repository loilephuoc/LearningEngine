package vn.loi.learning.android.study

import vn.loi.learning.domain.study.memory.model.ReviewRating

data class QuickReviewSessionInsights(
    val sessionId: String,
    val skipped: Int = 0,
    val again: Int = 0,
    val hard: Int = 0,
    val good: Int = 0,
    val easy: Int = 0
) {
    init {
        require(sessionId.isNotBlank())
        require(listOf(skipped, again, hard, good, easy).all { it >= 0 })
    }

    val totalExposures: Int get() = skipped + again + hard + good + easy

    fun recordSkip(): QuickReviewSessionInsights = copy(skipped = skipped + 1)

    fun record(rating: ReviewRating): QuickReviewSessionInsights = when (rating) {
        ReviewRating.AGAIN -> copy(again = again + 1)
        ReviewRating.HARD -> copy(hard = hard + 1)
        ReviewRating.GOOD -> copy(good = good + 1)
        ReviewRating.EASY -> copy(easy = easy + 1)
    }
}

internal class QuickReviewInsightsAccumulator {
    private var active: QuickReviewSessionInsights? = null
    private val completedVisitIds = mutableSetOf<String>()

    val summary: QuickReviewSessionInsights? get() = active?.takeIf { it.totalExposures > 0 }

    fun begin(sessionId: String) {
        if (active?.sessionId == sessionId) return
        active = QuickReviewSessionInsights(sessionId)
        completedVisitIds.clear()
    }

    fun recordSkip(sessionId: String, visitId: String): QuickReviewSessionInsights? =
        record(sessionId, visitId, QuickReviewSessionInsights::recordSkip)

    fun recordRating(
        sessionId: String,
        visitId: String,
        rating: ReviewRating
    ): QuickReviewSessionInsights? = record(sessionId, visitId) { it.record(rating) }

    private fun record(
        sessionId: String,
        visitId: String,
        update: (QuickReviewSessionInsights) -> QuickReviewSessionInsights
    ): QuickReviewSessionInsights? {
        val current = active?.takeIf { it.sessionId == sessionId } ?: return null
        if (!completedVisitIds.add(visitId)) return null
        return update(current).also { active = it }
    }
}
