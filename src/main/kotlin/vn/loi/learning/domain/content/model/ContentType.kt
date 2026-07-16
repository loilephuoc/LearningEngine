package vn.loi.learning.domain.content.model

/**
 * Bản chất của nội dung học.
 *
 * ContentType không mô tả cách học.
 * Ví dụ SENTENCE có thể tạo LearningItem cho nghe, nói,
 * dịch, chính tả hoặc phát âm.
 */
enum class ContentType {
    WORD,
    PHRASE,
    SENTENCE,
    DIALOGUE,
    STORY,
    LISTENING_PASSAGE,
    GRAMMAR_POINT,
    ARTICLE,
    CUSTOM
}