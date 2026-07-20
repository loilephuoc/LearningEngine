package vn.loi.learning.application.port

import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageId

/**
 * Port dùng để lưu và truy xuất ContentPackage.
 *
 * Repository quản lý aggregate logic của package.
 * Việc đọc file OPD3, ZIP, JSON hoặc phân giải media thuộc Infrastructure.
 */
interface ContentPackageRepository {

    fun findById(
        packageId: PackageId
    ): ContentPackage?

    fun save(
        contentPackage: ContentPackage
    )

    fun deleteById(
        packageId: PackageId
    )

    fun findAll(): List<ContentPackage>
}

