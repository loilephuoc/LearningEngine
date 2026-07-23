package vn.loi.learning.domain.library.event

import java.time.Instant
import vn.loi.learning.domain.common.event.DomainEvent
import vn.loi.learning.domain.library.model.LibraryId

data class LibraryCreatedEvent(
    val libraryId: LibraryId,
    val name: String,
    override val occurredAt: Instant = Instant.now()
) : DomainEvent
