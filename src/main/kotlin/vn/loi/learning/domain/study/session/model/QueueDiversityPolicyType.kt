package vn.loi.learning.domain.study.session.model

/**
 * Chính sách diversity áp dụng sau StudyQueueStrategy.
 *
 * Enum chỉ biểu diễn lựa chọn trong SessionPolicy.
 * Implementation cụ thể thuộc application layer.
 */
enum class QueueDiversityPolicyType {

    /**
     * Giữ nguyên hoàn toàn ordering do StudyQueueStrategy tạo ra.
     */
    NONE,

    /**
     * Tránh hai LearningItem có cùng ContentId xuất hiện liên tiếp
     * khi vẫn còn candidate thuộc ContentId khác.
     */
    CONTENT_DIVERSITY
}