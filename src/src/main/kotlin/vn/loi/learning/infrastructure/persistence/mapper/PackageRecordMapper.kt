package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.record.PackageRecord

object PackageRecordMapper {

    fun toRecord(
        contentPackage: ContentPackage
    ): PackageRecord =
        PackageRecord(
            id = contentPackage.id.toString(),
            name = contentPackage.descriptor.name,
            version = contentPackage.descriptor.version,
            format = contentPackage.descriptor.format,
            libraryIds = contentPackage.libraryIds
                .map(ContentLibraryId::toString)
                .toSet()
        )

    fun toDomain(
        record: PackageRecord
    ): ContentPackage =
        ContentPackage(
            id = PackageId(record.id),
            descriptor = PackageDescriptor(
                name = record.name,
                version = record.version,
                format = record.format
            ),
            libraryIds = record.libraryIds
                .map(::ContentLibraryId)
                .toSet()
        )
}
