package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.infrastructure.persistence.mapper.ContentLibraryRecordMapper
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
        val newRecord =
            ContentLibraryRecordMapper.toRecord(
                library
            )

        val updatedRecords =
            store.loadAll()
                .filterNot { record ->
                    record.id == newRecord.id
                } + newRecord

        store.saveAll(updatedRecords)
    }

    override fun deleteById(
        libraryId: ContentLibraryId
    ) {
        val updatedRecords =
            store.loadAll()
                .filterNot { record ->
                    record.id == libraryId.toString()
                }

        store.saveAll(updatedRecords)
    }

    override fun findAll(): List<ContentLibrary> =
        store.loadAll()
            .map(
                ContentLibraryRecordMapper::toDomain
            )
}