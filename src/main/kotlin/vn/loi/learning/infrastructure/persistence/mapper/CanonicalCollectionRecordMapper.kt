package vn.loi.learning.infrastructure.persistence.mapper

import java.time.Instant
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.CollectionState
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.infrastructure.persistence.record.CanonicalCollectionRecord

object CanonicalCollectionRecordMapper {

    fun toRecord(collection: Collection): CanonicalCollectionRecord =
        CanonicalCollectionRecord(
            id = collection.id.value,
            libraryId = collection.libraryId.value,
            name = collection.name.trimmedValue,
            description = collection.description,
            assignedPackageIds = collection.assignedPackageIds.map { it.value },
            state = collection.state.name,
            createdAt = collection.createdAt.toString()
        )

    fun toDomain(record: CanonicalCollectionRecord): Collection =
        mapPersistedRecord(
            recordType = "canonical-collection",
            recordId = record.id
        ) {
            Collection.reconstitute(
                id = CollectionId(record.id),
                libraryId = LibraryId(record.libraryId),
                name = CollectionName(record.name),
                description = record.description,
                assignedPackageIds = record.assignedPackageIds.mapTo(linkedSetOf()) { InstalledPackageId(it) },
                state = CollectionState.valueOf(record.state),
                createdAt = Instant.parse(record.createdAt)
            )
        }
}
