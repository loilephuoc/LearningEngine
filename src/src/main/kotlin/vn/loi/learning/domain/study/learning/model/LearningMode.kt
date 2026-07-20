package vn.loi.learning.domain.study.learning.model

/**
 * Kỹ năng hoặc nhiệm vụ học được tạo ra từ một Content.
 *
 * ContentType mô tả nội dung là gì.
 * LearningMode mô tả người học phải làm gì với nội dung đó.
 */
enum class LearningMode {
    MEANING_RECOGNITION,
    MEANING_RECALL,
    LISTENING_RECOGNITION,
    DICTATION,
    SPEAKING_RECALL,
    PRONUNCIATION,
    SHADOWING
}