package vn.loi.learning.domain.library.event

import java.time.Instant
import vn.loi.learning.domain.common.event.DomainEvent
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName

data class CollectionRenamedEvent(
    val collectionId: CollectionId,
    val oldName: CollectionName,
    val newName: CollectionName,
    override val occurredAt: Instant = Instant.now()
) : DomainEvent
