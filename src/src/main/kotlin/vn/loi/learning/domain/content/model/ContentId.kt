package vn.loi.learning.domain.content.model

/**
 * Định danh duy nhất và ổn định của một Content.
 *
 * ID không nên được tạo từ nội dung tiếng Anh, tên lesson hoặc đường dẫn file
 * vì các dữ liệu đó có thể thay đổi.
 */
@JvmInline
value class ContentId(val value: String) {

    init {
        require(value.isNotBlank()) {
            "ContentId must not be blank."
        }
    }

    override fun toString(): String = value
}