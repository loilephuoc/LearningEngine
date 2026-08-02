package vn.loi.learning.domain.study.session.model

/**
 * Chính sách giới hạn, ordering, diversity và cân bằng độ khó
 * của một phiên học.
 */
data class SessionPolicy(
    val newItemLimit: Int = 20,
    val reviewItemLimit: Int = 100,
    val allowRepeatInSameSession: Boolean = false,
    val queueStrategy:
    StudyQueueStrategyType =
        StudyQueueStrategyType.REVIEW_FIRST,
    val queueDiversityPolicy:
    QueueDiversityPolicyType =
        QueueDiversityPolicyType
            .CONTENT_DIVERSITY,
    val difficultyBalancePolicy:
    DifficultyBalancePolicyType =
        DifficultyBalancePolicyType.NONE,
    val evaluationPolicy: SessionEvaluationPolicy =
        SessionEvaluationPolicy.EVALUATIVE
) {

    init {
        require(newItemLimit >= 0) {
            "New item limit must not be negative."
        }

        require(reviewItemLimit >= 0) {
            "Review item limit must not be negative."
        }

        require(
            newItemLimit > 0 ||
                    reviewItemLimit > 0
        ) {
            "A session must allow at least one new or review item."
        }
    }
}
