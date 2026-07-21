package vn.loi.learning.domain.study.session.model

/**
 * Chính sách cân bằng độ khó của StudyQueue.
 *
 * Chính sách được áp dụng sau strategy và diversity,
 * trước SessionPolicyLimiter.
 */
enum class DifficultyBalancePolicyType {

    /**
     * Không thay đổi ordering.
     */
    NONE,

    /**
     * Tránh các candidate thuộc cùng nhóm độ khó xuất hiện liên tiếp
     * khi vẫn còn candidate thuộc nhóm khác.
     */
    ALTERNATE_DIFFICULTY
}