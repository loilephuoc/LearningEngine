package vn.loi.learning.domain.content.packaging.model

/**
 * Định danh bất biến của một Package Catalog.
 *
 * Package Catalog là danh mục quản lý các ContentPackage đã được đăng ký.
 */
@JvmInline
value class PackageCatalogId(
    val value: String
) {

    init {
        require(value.isNotBlank()) {
            "PackageCatalogId must not be blank."
        }
    }

    override fun toString(): String =
        value
}
