package vn.loi.learning.domain.study.selection.policy

/**
 * Thứ tự ưu tiên giữa item mới và item đã đến hạn review.
 */
enum class SelectionPriority {

    /**
     * Ưu tiên item đã đến hạn review trước item mới.
     */
    REVIEW_FIRST,

    /**
     * Ưu tiên item mới trước item đã đến hạn review.
     */
    NEW_FIRST
}