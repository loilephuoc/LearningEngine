package vn.loi.learning.domain.library.model

/**
 * Biểu diễn phiên bản của gói nội dung OPD3.
 */
@JvmInline
value class PackageVersion(val value: String) {
    init {
        require(value.isNotBlank()) {
            "PackageVersion value must not be blank."
        }
    }

    override fun toString(): String = value
}
