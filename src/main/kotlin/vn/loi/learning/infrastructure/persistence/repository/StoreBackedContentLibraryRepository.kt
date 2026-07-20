package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.infrastructure.persistence.mapper.ContentLibraryRecordMapper
import vn.loi.learning.infrastructure.persistence.record.ContentLibraryRecord
import vn.loi.learning.infrastructure.persistence.store.ContentLibraryStore

class StoreBackedContentLibraryRepository(
    private val store: ContentLibraryStore
) : ContentLibraryRepository {

    override fun findById(
        libraryId: ContentLibraryId
    ): ContentLibrary? =
        store.loadAll()
            .firstOrNull { record ->
                record.id == libraryId.toString()
            }
            ?.let(
                ContentLibraryRecordMapper::toDomain
            )

    override fun save(
        library: ContentLibrary
    ) {
        saveAll(
            listOf(
                library
            )
        )
    }

    override fun saveAll(
        libraries: List<ContentLibrary>
    ) {
        if (libraries.isEmpty()) {
            return
        }

        val existingRecords =
            store.loadAll()

        val recordsById =
            linkedMapOf<String, ContentLibraryRecord>()

        existingRecords.forEach { record ->
            recordsById[record.id] =
                record
        }

        libraries.forEach { library ->
            val record =
                ContentLibraryRecordMapper.toRecord(
                    library
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
        libraryId: ContentLibraryId
    ) {
        deleteAllById(
            setOf(
                libraryId
            )
        )
    }

    override fun deleteAllById(
        libraryIds: Set<ContentLibraryId>
    ) {
        if (libraryIds.isEmpty()) {
            return
        }

        val ids =
            libraryIds
                .mapTo(
                    hashSetOf()
                ) { libraryId ->
                    libraryId.toString()
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

    override fun findAll(): List<ContentLibrary> =
        store.loadAll()
            .map(
                ContentLibraryRecordMapper::toDomain
            )
}