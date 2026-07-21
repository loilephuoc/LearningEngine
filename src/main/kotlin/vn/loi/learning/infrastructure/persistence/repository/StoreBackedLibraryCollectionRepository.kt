package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.LibraryCollectionRepository
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollection
import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.infrastructure.persistence.mapper.LibraryCollectionRecordMapper
import vn.loi.learning.infrastructure.persistence.record.LibraryCollectionRecord
import vn.loi.learning.infrastructure.persistence.store.LibraryCollectionStore

class StoreBackedLibraryCollectionRepository(
    private val store: LibraryCollectionStore
) : LibraryCollectionRepository {

    override fun findById(
        collectionId: LibraryCollectionId
    ): LibraryCollection? =
        store.loadAll()
            .firstOrNull { record ->
                record.id == collectionId.toString()
            }
            ?.let(
                LibraryCollectionRecordMapper::toDomain
            )

    override fun findAllByLibraryId(
        libraryId: ContentLibraryId
    ): List<LibraryCollection> =
        store.loadAll()
            .filter { record ->
                record.libraryId == libraryId.toString()
            }
            .map(
                LibraryCollectionRecordMapper::toDomain
            )

    override fun save(
        collection: LibraryCollection
    ) {
        saveAll(
            listOf(
                collection
            )
        )
    }

    override fun saveAll(
        collections: List<LibraryCollection>
    ) {
        if (collections.isEmpty()) {
            return
        }

        val existingRecords =
            store.loadAll()

        val recordsById =
            linkedMapOf<String, LibraryCollectionRecord>()

        existingRecords.forEach { record ->
            recordsById[record.id] =
                record
        }

        collections.forEach { collection ->
            val record =
                LibraryCollectionRecordMapper.toRecord(
                    collection
                )

            recordsById[record.id] =
                record
        }

        val updatedRecords =
            recordsById.values.toList()

        if (updatedRecords == existingRecords) {
            return
        }

        store.saveAll(
            updatedRecords
        )
    }

    override fun deleteById(
        collectionId: LibraryCollectionId
    ) {
        deleteAllById(
            setOf(
                collectionId
            )
        )
    }

    override fun deleteAllById(
        collectionIds: Set<LibraryCollectionId>
    ) {
        if (collectionIds.isEmpty()) {
            return
        }

        val ids =
            collectionIds
                .mapTo(
                    hashSetOf()
                ) { collectionId ->
                    collectionId.toString()
                }

        val existingRecords =
            store.loadAll()

        val updatedRecords =
            existingRecords.filterNot { record ->
                record.id in ids
            }

        if (updatedRecords.size == existingRecords.size) {
            return
        }

        store.saveAll(
            updatedRecords
        )
    }

    override fun findAll(): List<LibraryCollection> =
        store.loadAll()
            .map(
                LibraryCollectionRecordMapper::toDomain
            )
}