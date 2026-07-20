package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.record.PackageCatalogRecord

object PackageCatalogRecordMapper {

    fun toRecord(
        catalog: PackageCatalog
    ): PackageCatalogRecord =
        PackageCatalogRecord(
            id = catalog.id.toString(),
            packageIds = catalog.packageIds
                .map(PackageId::toString)
        )

    fun toDomain(
        record: PackageCatalogRecord
    ): PackageCatalog =
        PackageCatalog(
            id = PackageCatalogId(record.id),
            packageIds = record.packageIds
                .map(::PackageId)
                .toSet()
        )
}
