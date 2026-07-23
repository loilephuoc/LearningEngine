package vn.loi.learning.domain.library.model

/**
 * Định danh duy nhất cho một tệp gói nội dung đã được cài đặt (InstalledPackage Aggregate Root).
 */
@JvmInline
value class InstalledPackageId(val value: String) {
    init {
        require(value.isNotBlank()) {
            "InstalledPackageId value must not be blank."
        }
    }

    override fun toString(): String = value
}
