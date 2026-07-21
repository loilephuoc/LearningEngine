package vn.loi.learning.domain.content.library.model

/**
 * Định danh duy nhất và ổn định của một collection trong Learning Library.
 *
 * Collection ID không phụ thuộc tên hiển thị vì collection có thể được đổi tên
 * trong suốt vòng đời mà không làm thay đổi danh tính của nó.
 */
@JvmInline
value class LibraryCollectionId(
    val value: String
) {

    init {
        require(value.isNotBlank()) {
            "LibraryCollectionId must not be blank."
        }
    }

    override fun toString(): String = value
}
