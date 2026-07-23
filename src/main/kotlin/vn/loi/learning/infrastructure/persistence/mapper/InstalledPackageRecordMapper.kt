package vn.loi.learning.infrastructure.persistence.mapper

import java.time.Instant
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.infrastructure.persistence.record.InstalledPackageRecord

object InstalledPackageRecordMapper {

    fun toRecord(pkg: InstalledPackage): InstalledPackageRecord =
        InstalledPackageRecord(
            id = pkg.id.value,
            libraryId = pkg.libraryId.value,
            packageId = pkg.packageId.value,
            topicId = pkg.topicId.value,
            name = pkg.name.value,
            version = pkg.version.value,
            state = pkg.state.name,
            installedAt = pkg.installedAt.toString(),
            contentCount = pkg.contentCount,
            learningItemCount = pkg.learningItemCount,
            contentChecksum = pkg.contentChecksum
        )

    fun toDomain(record: InstalledPackageRecord): InstalledPackage =
        mapPersistedRecord(
            recordType = "installed-package",
            recordId = record.id
        ) {
            InstalledPackage.reconstitute(
                id = InstalledPackageId(record.id),
                libraryId = LibraryId(record.libraryId),
                packageId = PackageId(record.packageId),
                topicId = TopicId(record.topicId),
                name = PackageName(record.name),
                version = PackageVersion(record.version),
                state = PackageState.valueOf(record.state),
                installedAt = Instant.parse(record.installedAt),
                contentCount = record.contentCount,
                learningItemCount = record.learningItemCount,
                // Backward compatible: null when record was created before LP-004R.1
                contentChecksum = record.contentChecksum
            )
        }
}
