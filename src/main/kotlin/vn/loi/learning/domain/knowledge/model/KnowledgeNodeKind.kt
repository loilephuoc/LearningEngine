package vn.loi.learning.domain.knowledge.model

/**
 * Loại nút trong knowledge graph.
 *
 * Phân loại về mặt cấu trúc kiến thức.
 * Không chứa learner state.
 */
enum class KnowledgeNodeKind {
    /** Gói nội dung — tập hợp nhiều topic */
    PACKAGE,

    /** Chủ đề kiến thức */
    TOPIC,

    /** Bài học */
    LESSON,

    /** Tài nguyên học tập (video, audio, v.v.) */
    RESOURCE,

    /** Bộ sưu tập (tập hợp tùy biến của người dùng) */
    COLLECTION
}
