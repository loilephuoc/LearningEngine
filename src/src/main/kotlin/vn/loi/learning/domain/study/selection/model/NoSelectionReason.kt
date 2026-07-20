package vn.loi.learning.domain.study.selection.model

/**
 * Lý do Session Selection Engine không chọn được LearningItem tiếp theo.
 */
enum class NoSelectionReason {

    /**
     * StudySession đã kết thúc.
     */
    SESSION_FINISHED,

    /**
     * Session đã đạt cả giới hạn new item và review item.
     */
    SESSION_LIMIT_REACHED,

    /**
     * Không có candidate nào đang enabled và đến hạn.
     */
    NO_ELIGIBLE_CANDIDATES,

    /**
     * Các candidate phù hợp đều đã xuất hiện trong session,
     * trong khi session không cho phép lặp lại.
     */
    REPEAT_NOT_ALLOWED,

    /**
     * Chỉ còn sibling của content vừa xuất hiện,
     * nhưng policy không cho phép sibling fallback.
     */
    SIBLING_BLOCKED
}