package vn.loi.learning.domain.library.event

import java.time.Instant
import vn.loi.learning.domain.common.event.DomainEvent
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.library.model.InstalledPackageId

data class PackageRestoredEvent(
    val installedPackageId: InstalledPackageId,
    val packageId: PackageId,
    override val occurredAt: Instant = Instant.now()
) : DomainEvent
