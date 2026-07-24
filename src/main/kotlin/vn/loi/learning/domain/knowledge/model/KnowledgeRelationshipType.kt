package vn.loi.learning.domain.knowledge.model

/**
 * Loại quan hệ có kiểu giữa các nút trong knowledge graph.
 *
 * Mỗi quan hệ là có hướng (directed): source → target.
 */
enum class KnowledgeRelationshipType {
    /**
     * Source chứa target (ví dụ: PACKAGE CONTAINS TOPIC).
     * Ngược lại: BELONGS_TO.
     */
    CONTAINS,

    /**
     * Target là container của source (ví dụ: TOPIC BELONGS_TO PACKAGE).
     * Ngược lại: CONTAINS.
     */
    BELONGS_TO,

    /**
     * Source phụ thuộc vào target để hoạt động.
     */
    DEPENDS_ON,

    /**
     * Source yêu cầu target trước khi có thể truy cập.
     */
    REQUIRES,

    /**
     * Source là điều kiện tiên quyết của target (target cần source trước).
     * Ngược lại logic của REQUIRES.
     */
    PREREQUISITE_OF,

    /**
     * Source được khuyến nghị học sau khi đã học target.
     */
    RECOMMENDED_AFTER,

    /**
     * Source tham chiếu đến target (ví dụ: LESSON REFERENCES RESOURCE).
     */
    REFERENCES
}
