package vn.loi.learning.domain.study.selection.policy

/**
 * Chính sách điều khiển cách Session Selection Engine lựa chọn LearningItem.
 *
 * Policy này không chứa giới hạn số lượng item của phiên học.
 * Các giới hạn đó thuộc về SessionPolicy.
 *
 * SessionSelectionPolicy chỉ mô tả:
 * - nhóm item nào được ưu tiên;
 * - có cần tránh sibling liên tiếp hay không;
 * - cách xử lý khi không còn candidate khác sibling.
 */
data class SessionSelectionPolicy(
    val priority: SelectionPriority = SelectionPriority.REVIEW_FIRST,
    val avoidConsecutiveSiblings: Boolean = true,
    val allowSiblingFallback: Boolean = true
)