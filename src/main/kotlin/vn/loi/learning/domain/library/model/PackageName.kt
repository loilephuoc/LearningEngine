package vn.loi.learning.domain.library.model

/**
 * Tên hiển thị của tệp gói nội dung đã được cài đặt.
 */
@JvmInline
value class PackageName(val value: String) {
    init {
        require(value.isNotBlank()) {
            "PackageName value must not be blank."
        }
        require(value.length <= 200) {
            "PackageName length must not exceed 200 characters."
        }
    }

    override fun toString(): String = value
}
