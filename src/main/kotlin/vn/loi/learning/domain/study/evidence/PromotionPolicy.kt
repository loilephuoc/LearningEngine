package vn.loi.learning.domain.study.evidence

import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan

data class EvidenceWindow(
    val minimumElapsed: TimeSpan,
    val minimumCorrectRecalls: Int
) {
    init {
        require(minimumCorrectRecalls > 0) {
            "minimumCorrectRecalls must be positive."
        }
    }
}

data class PromotionPolicy(
    val againToHard: EvidenceWindow = EvidenceWindow(TimeSpan.hours(24), 1),
    val hardToGood: EvidenceWindow = EvidenceWindow(TimeSpan.hours(72), 2),
    val goodToEasy: EvidenceWindow = EvidenceWindow(TimeSpan.days(14), 3)
) {
    fun windowFor(currentRating: ReviewRating): EvidenceWindow? =
        when (currentRating) {
            ReviewRating.AGAIN -> againToHard
            ReviewRating.HARD -> hardToGood
            ReviewRating.GOOD -> goodToEasy
            ReviewRating.EASY -> null
        }
}
