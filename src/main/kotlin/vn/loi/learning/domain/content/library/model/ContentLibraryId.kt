package vn.loi.learning.domain.content.library.model

/**
 * Định danh duy nhất và ổn định của một Content Library.
 *
 * Library ID không phụ thuộc tên hiển thị, đường dẫn file hoặc phiên bản
 * vì các thông tin đó có thể thay đổi trong vòng đời của library.
 */
@JvmInline
value class ContentLibraryId(
    val value: String
) {

    init {
        require(value.isNotBlank()) {
            "ContentLibraryId must not be blank."
        }
    }

    override fun toString(): String = value
}
