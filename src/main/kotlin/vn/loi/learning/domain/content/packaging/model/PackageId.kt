package vn.loi.learning.domain.content.packaging.model

/**
 * Định danh duy nhất của một Content Package.
 *
 * Package là đơn vị phân phối dữ liệu (OPD3, ZIP, JSON...).
 */
@JvmInline
value class PackageId(
    val value: String
) {

    init {
        require(value.isNotBlank()) {
            "Package ID must not be blank."
        }
    }

    override fun toString(): String =
        value
}

