package vn.loi.learning.domain.library.event

import java.time.Instant
import vn.loi.learning.domain.common.event.DomainEvent
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.InstalledPackageId

data class PackageRemovedFromCollectionEvent(
    val collectionId: CollectionId,
    val installedPackageId: InstalledPackageId,
    override val occurredAt: Instant = Instant.now()
) : DomainEvent
