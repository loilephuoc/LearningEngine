package vn.loi.learning.infrastructure.persistence.mapper

import java.time.Instant
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryEntry
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.infrastructure.persistence.record.CanonicalLibraryRecord
import vn.loi.learning.infrastructure.persistence.record.LibraryEntryRecord

object CanonicalLibraryRecordMapper {

    fun toRecord(library: Library): CanonicalLibraryRecord =
        CanonicalLibraryRecord(
            id = library.id.value,
            name = library.name,
            entries = library.entries.map { entry ->
                LibraryEntryRecord(
                    installedPackageId = entry.installedPackageId.value,
                    packageId = entry.packageId.value,
                    registeredAt = entry.registeredAt.toString()
                )
            },
            activePackageId = library.activePackageId?.value,
            createdAt = library.createdAt.toString()
        )

    fun toDomain(record: CanonicalLibraryRecord): Library =
        mapPersistedRecord(
            recordType = "canonical-library",
            recordId = record.id
        ) {
            Library.reconstitute(
                id = LibraryId(record.id),
                name = record.name,
                entries = record.entries.map { entryRecord ->
                    LibraryEntry(
                        installedPackageId = InstalledPackageId(entryRecord.installedPackageId),
                        packageId = PackageId(entryRecord.packageId),
                        registeredAt = Instant.parse(entryRecord.registeredAt)
                    )
                },
                activePackageId = record.activePackageId?.let { InstalledPackageId(it) },
                createdAt = Instant.parse(record.createdAt)
            )
        }
}
