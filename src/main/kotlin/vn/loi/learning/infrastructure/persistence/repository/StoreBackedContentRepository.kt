package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.infrastructure.persistence.mapper.ContentRecordMapper
import vn.loi.learning.infrastructure.persistence.record.ContentRecord
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
        saveAll(
            listOf(
                content
            )
        )
    }

    override fun saveAll(
        contents: List<Content>
    ) {
        if (contents.isEmpty()) {
            return
        }

        val existingRecords =
            store.loadAll()

        val recordsById =
            linkedMapOf<String, ContentRecord>()

        existingRecords.forEach { record ->
            recordsById[record.id] =
                record
        }

        contents.forEach { content ->
            val record =
                ContentRecordMapper.toRecord(
                    content
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
        contentId: ContentId
    ) {
        deleteAllById(
            setOf(
                contentId
            )
        )
    }

    override fun deleteAllById(
        contentIds: Set<ContentId>
    ) {
        if (contentIds.isEmpty()) {
            return
        }

        val ids =
            contentIds
                .mapTo(
                    hashSetOf()
                ) { contentId ->
                    contentId.toString()
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

    override fun findAll(): List<Content> =
        store.loadAll()
            .map(
                ContentRecordMapper::toDomain
            )
}