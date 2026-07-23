package vn.loi.learning.domain.library.event

import java.time.Instant
import vn.loi.learning.domain.common.event.DomainEvent
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageVersion

data class PackageInstalledEvent(
    val installedPackageId: InstalledPackageId,
    val libraryId: LibraryId,
    val packageId: PackageId,
    val topicId: TopicId,
    val version: PackageVersion,
    override val occurredAt: Instant = Instant.now()
) : DomainEvent
