package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.infrastructure.persistence.mapper.ContentRecordMapper
import vn.loi.learning.infrastructure.persistence.store.ContentStore

class StoreBackedContentRepository(
    private val store: ContentStore
 ) : ContentRepository {

    override fun findById(
        contentId: ContentId
    ): Content? =
        store.loadAll()
            .firstOrNull { record ->
                record.id == contentId.toString()
            }
            ?.let(
                ContentRecordMapper::toDomain
            )

    override fun save(
        content: Content
    ) {
        val newRecord =
            ContentRecordMapper.toRecord(content)

        val updatedRecords =
            store.loadAll()
                .filterNot { record ->
                    record.id == newRecord.id
                } + newRecord

        store.saveAll(updatedRecords)
    }

    override fun deleteById(
        contentId: ContentId
    ) {
        val updatedRecords =
            store.loadAll()
                .filterNot { record ->
                    record.id == contentId.toString()
                }

        store.saveAll(updatedRecords)
    }

    override fun findAll(): List<Content> =
        store.loadAll()
            .map(
                ContentRecordMapper::toDomain
            )
}

