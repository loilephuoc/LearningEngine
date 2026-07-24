package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.CollectionState
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.repository.CollectionRepository
import vn.loi.learning.infrastructure.persistence.mapper.CanonicalCollectionRecordMapper
import vn.loi.learning.infrastructure.persistence.record.CanonicalCollectionRecord
import vn.loi.learning.infrastructure.persistence.store.CanonicalCollectionStore

/**
 * Store-backed persistent implementation của [CollectionRepository] cho Canonical Library Domain.
 * Bảo tồn đầy đủ tất cả các Collection bao gồm cả trạng thái DELETED (soft-deleted).
 */
class StoreBackedCanonicalCollectionRepository(
    private val store: CanonicalCollectionStore
) : CollectionRepository {

    override fun findById(id: CollectionId): Collection? =
        store.loadAll()
            .firstOrNull { it.id == id.value }
            ?.let(CanonicalCollectionRecordMapper::toDomain)

    override fun findByName(libraryId: LibraryId, name: CollectionName): Collection? =
        store.loadAll()
            .filter { it.libraryId == libraryId.value && it.name == name.trimmedValue }
            .map(CanonicalCollectionRecordMapper::toDomain)
            .firstOrNull()

    override fun findAllByLibraryId(libraryId: LibraryId): List<Collection> =
        store.loadAll()
            .filter { it.libraryId == libraryId.value }
            .map(CanonicalCollectionRecordMapper::toDomain)

    override fun findAllByLibraryIdAndState(
        libraryId: LibraryId,
        state: CollectionState
    ): List<Collection> =
        store.loadAll()
            .filter { it.libraryId == libraryId.value && it.state == state.name }
            .map(CanonicalCollectionRecordMapper::toDomain)

    override fun save(collection: Collection) {
        val existingRecords = store.loadAll()
        val recordsById = linkedMapOf<String, CanonicalCollectionRecord>()
        existingRecords.forEach { record -> recordsById[record.id] = record }

        val newRecord = CanonicalCollectionRecordMapper.toRecord(collection)
        recordsById[newRecord.id] = newRecord

        val updatedRecords = recordsById.values.toList()
        if (updatedRecords == existingRecords) return
        store.saveAll(updatedRecords)
    }

    override fun delete(id: CollectionId) {
        val existingRecords = store.loadAll()
        val updatedRecords = existingRecords.filterNot { it.id == id.value }
        if (updatedRecords.size == existingRecords.size) return
        store.saveAll(updatedRecords)
    }

    override fun existsByName(libraryId: LibraryId, name: CollectionName): Boolean =
        store.loadAll().any {
            it.libraryId == libraryId.value && it.name == name.trimmedValue && it.state == CollectionState.ACTIVE.name
        }
}
