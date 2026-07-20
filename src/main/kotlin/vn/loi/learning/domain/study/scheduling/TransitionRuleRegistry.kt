package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Registry quản lý toàn bộ TransitionRule.
 *
 * Registry chỉ chịu trách nhiệm tra cứu rule theo ReviewRating.
 * Không thực hiện scheduling.
 */
internal class TransitionRuleRegistry(
    rules: List<TransitionRule>
) {

    private val rulesByRating =
        rules.associateBy { it.rating }

    init {
        require(
            rulesByRating.size == rules.size
        ) {
            "Duplicate TransitionRule detected."
        }
    }

    fun ruleFor(
        rating: ReviewRating
    ): TransitionRule =
        rulesByRating[rating]
            ?: error(
                "No TransitionRule registered for $rating"
            )
}