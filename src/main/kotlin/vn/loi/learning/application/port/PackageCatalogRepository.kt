package vn.loi.learning.application.port

import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId

/**
 * Port dùng để lưu và truy xuất PackageCatalog.
 *
 * Repository chỉ quản lý aggregate catalog.
 * Việc quét file package, đọc OPD3 hoặc phân giải media thuộc Infrastructure.
 */
interface PackageCatalogRepository {

    fun findById(
        catalogId: PackageCatalogId
    ): PackageCatalog?

    fun save(
        catalog: PackageCatalog
    )

    fun deleteById(
        catalogId: PackageCatalogId
    )

    fun findAll(): List<PackageCatalog>
}

