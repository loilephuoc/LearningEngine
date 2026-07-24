package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.repository.LibraryRepository
import vn.loi.learning.infrastructure.persistence.mapper.CanonicalLibraryRecordMapper
import vn.loi.learning.infrastructure.persistence.record.CanonicalLibraryRecord
import vn.loi.learning.infrastructure.persistence.store.CanonicalLibraryStore

/**
 * Store-backed persistent implementation của [LibraryRepository] cho Canonical Library Domain.
 * Áp dụng đúng load-all / merge / save-all pattern của hệ thống.
 */
class StoreBackedCanonicalLibraryRepository(
    private val store: CanonicalLibraryStore
) : LibraryRepository {

    override fun findById(id: LibraryId): Library? =
        store.loadAll()
            .firstOrNull { it.id == id.value }
            ?.let(CanonicalLibraryRecordMapper::toDomain)

    override fun save(library: Library) {
        val existingRecords = store.loadAll()
        val recordsById = linkedMapOf<String, CanonicalLibraryRecord>()
        existingRecords.forEach { record -> recordsById[record.id] = record }

        val newRecord = CanonicalLibraryRecordMapper.toRecord(library)
        recordsById[newRecord.id] = newRecord

        val updatedRecords = recordsById.values.toList()
        if (updatedRecords == existingRecords) return
        store.saveAll(updatedRecords)
    }

    override fun existsById(id: LibraryId): Boolean =
        store.loadAll().any { it.id == id.value }
}
