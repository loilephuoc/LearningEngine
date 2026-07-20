package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.record.PackageDependencyRecord
import vn.loi.learning.infrastructure.persistence.record.PackageRecord

object PackageRecordMapper {

    fun toRecord(
        contentPackage: ContentPackage
    ): PackageRecord =
        PackageRecord(
            id =
                contentPackage.id.toString(),
            name =
                contentPackage.descriptor.name,
            version =
                contentPackage.descriptor.version,
            format =
                contentPackage.descriptor.format,
            libraryIds =
                contentPackage.libraryIds
                    .map(
                        ContentLibraryId::toString
                    )
                    .toSet(),
            schemaVersion =
                contentPackage.descriptor.schemaVersion,
            minimumEngineVersion =
                contentPackage.descriptor
                    .minimumEngineVersion,
            maximumEngineVersion =
                contentPackage.descriptor
                    .maximumEngineVersion,
            dependencies =
                contentPackage.descriptor.dependencies
                    .map { dependency ->
                        PackageDependencyRecord(
                            packageName =
                                dependency.packageName,
                            minimumVersion =
                                dependency.minimumVersion,
                            maximumVersion =
                                dependency.maximumVersion
                        )
                    }
                    .toSet()
        )

    fun toDomain(
        record: PackageRecord
    ): ContentPackage =
        ContentPackage(
            id =
                PackageId(
                    record.id
                ),
            descriptor =
                PackageDescriptor(
                    name =
                        record.name,
                    version =
                        record.version,
                    format =
                        record.format,
                    schemaVersion =
                        record.schemaVersion,
                    minimumEngineVersion =
                        record.minimumEngineVersion,
                    maximumEngineVersion =
                        record.maximumEngineVersion,
                    dependencies =
                        record.dependencies
                            .map { dependency ->
                                PackageDependency(
                                    packageName =
                                        dependency.packageName,
                                    minimumVersion =
                                        dependency.minimumVersion,
                                    maximumVersion =
                                        dependency.maximumVersion
                                )
                            }
                            .toSet()
                ),
            libraryIds =
                record.libraryIds
                    .map(
                        ::ContentLibraryId
                    )
                    .toSet()
        )
}