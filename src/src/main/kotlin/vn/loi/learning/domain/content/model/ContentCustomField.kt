package vn.loi.learning.domain.content.model

/**
 * Một custom field đơn giản của Content.
 *
 * Vertical slice đầu tiên chỉ hỗ trợ giá trị text.
 */
data class ContentCustomField(
    val id: ContentFieldId,
    val value: String
) {

    init {
        require(value.isNotBlank()) {
            "Content custom field value must not be blank."
        }
    }
}
