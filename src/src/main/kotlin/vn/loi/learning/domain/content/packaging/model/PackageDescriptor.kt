package vn.loi.learning.domain.content.packaging.model

/**
 * Metadata mô tả một Content Package.
 *
 * Descriptor chỉ chứa thông tin logic ổn định của package.
 * Đường dẫn file, cache và trạng thái cài đặt thuộc Infrastructure.
 */
data class PackageDescriptor(
    val name: String,
    val version: String,
    val format: String
) {

    init {
        require(name.isNotBlank()) {
            "Package name must not be blank."
        }

        require(version.isNotBlank()) {
            "Package version must not be blank."
        }

        require(format.isNotBlank()) {
            "Package format must not be blank."
        }
    }
}
