package vn.loi.learning.domain.content.model

/**
 * Aggregate Root của Content Domain.
 *
 * Content mô tả nội dung gốc, không chứa trạng thái SRS:
 * - không có due date
 * - không có interval
 * - không có difficulty
 * - không có stability
 * - không có review count
 */
data class Content(
    val id: ContentId,
    val type: ContentType,
    val text: ContentText,
    val media: ContentMedia = ContentMedia(),
    val metadata: ContentMetadata = ContentMetadata(),
    val customFields: ContentCustomFields = ContentCustomFields()
) {

    /**
     * Tên dễ đọc dùng cho log, debug và công cụ quản trị.
     *
     * Nếu chưa khai báo title, primaryText sẽ được dùng thay thế.
     */
    val displayName: String
        get() = metadata.title ?: text.primaryText
}
