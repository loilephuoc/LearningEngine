package vn.loi.learning.domain.library.event

import java.time.Instant
import vn.loi.learning.domain.common.event.DomainEvent
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.LibraryId

data class CollectionCreatedEvent(
    val collectionId: CollectionId,
    val libraryId: LibraryId,
    val name: CollectionName,
    override val occurredAt: Instant = Instant.now()
) : DomainEvent
