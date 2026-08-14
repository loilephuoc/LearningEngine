package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.infrastructure.persistence.mapper.LearningItemRecordMapper
import vn.loi.learning.infrastructure.persistence.record.LearningItemRecord
import vn.loi.learning.infrastructure.persistence.store.LearningItemStore

class StoreBackedLearningItemRepository(
    private val store: LearningItemStore
) : LearningItemRepository {

    override fun findAll(): List<LearningItem> =
        store.loadAll().map(LearningItemRecordMapper::toDomain)

    override fun findById(
        learningItemId: LearningItemId
    ): LearningItem? =
        store.loadAll()
            .firstOrNull { record ->
                record.id == learningItemId.toString()
            }
            ?.let(
                LearningItemRecordMapper::toDomain
            )

    override fun findByContentId(
        contentId: ContentId
    ): List<LearningItem> =
        store.loadAll()
            .filter { record ->
                record.contentId == contentId.toString()
            }
            .map(
                LearningItemRecordMapper::toDomain
            )

    override fun findByContentIds(
        contentIds: Set<ContentId>
    ): List<LearningItem> {
        if (contentIds.isEmpty()) return emptyList()
        val strIds = contentIds.mapTo(hashSetOf()) { it.value }
        return store.loadAll()
            .filter { record -> record.contentId in strIds }
            .map(LearningItemRecordMapper::toDomain)
    }

    override fun findAllEnabled(): List<LearningItem> =
        store.loadAll()
            .filter { record ->
                record.isEnabled
            }
            .map(
                LearningItemRecordMapper::toDomain
            )

    override fun save(
        learningItem: LearningItem
    ) {
        saveAll(
            listOf(
                learningItem
            )
        )
    }

    override fun saveAll(
        learningItems: List<LearningItem>
    ) {
        if (learningItems.isEmpty()) {
            return
        }

        val existingRecords =
            store.loadAll()

        val recordsById =
            linkedMapOf<String, LearningItemRecord>()

        existingRecords.forEach { record ->
            recordsById[record.id] =
                record
        }

        learningItems.forEach { learningItem ->
            val record =
                LearningItemRecordMapper.toRecord(
                    learningItem
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
        learningItemId: LearningItemId
    ) {
        deleteAllById(setOf(learningItemId))
    }

    override fun deleteAllById(
        learningItemIds: Set<LearningItemId>
    ) {
        if (learningItemIds.isEmpty()) {
            return
        }
        val ids = learningItemIds.mapTo(hashSetOf()) { it.value }
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

    override fun deleteByContentIds(
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
                record.contentId in ids
            }

        if (updatedRecords.size == existingRecords.size) {
            return
        }

        store.saveAll(
            updatedRecords
        )
    }
}
