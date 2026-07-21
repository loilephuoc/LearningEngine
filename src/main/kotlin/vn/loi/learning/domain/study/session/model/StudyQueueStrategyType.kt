package vn.loi.learning.domain.study.session.model

/**
 * Kiểu chiến lược dùng để sắp xếp StudyQueue của một phiên học.
 *
 * Enum chỉ biểu diễn lựa chọn trong SessionPolicy.
 * Việc ánh xạ sang implementation cụ thể thuộc application layer.
 */
enum class StudyQueueStrategyType {

    /**
     * Ưu tiên toàn bộ REVIEW trước NEW.
     */
    REVIEW_FIRST,

    /**
     * Ưu tiên toàn bộ NEW trước REVIEW.
     */
    NEW_FIRST,

    /**
     * Xen kẽ REVIEW và NEW, bắt đầu bằng REVIEW.
     */
    INTERLEAVED,

    /**
     * Tự động lựa chọn REVIEW_FIRST, NEW_FIRST hoặc INTERLEAVED
     * dựa trên tỷ lệ candidate hiện có.
     */
    ADAPTIVE
}