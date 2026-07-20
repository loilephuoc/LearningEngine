package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId

/**
 * Tạo định danh ổn định cho một Content Package.
 *
 * Application chỉ phụ thuộc contract này.
 * Quy tắc băm cụ thể thuộc Infrastructure.
 */
fun interface PackageIdGenerator {

    fun generate(
        descriptor: PackageDescriptor
    ): PackageId
}
