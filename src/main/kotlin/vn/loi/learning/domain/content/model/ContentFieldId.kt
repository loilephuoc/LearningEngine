package vn.loi.learning.domain.content.model

/**
 * Định danh ổn định của một custom field trong Content.
 *
 * ID không phụ thuộc tên hiển thị của field, vì tên hiển thị có thể thay đổi.
 */
@JvmInline
value class ContentFieldId(val value: String) {

    init {
        require(value.isNotBlank()) {
            "ContentFieldId must not be blank."
        }
    }

    override fun toString(): String = value
}
