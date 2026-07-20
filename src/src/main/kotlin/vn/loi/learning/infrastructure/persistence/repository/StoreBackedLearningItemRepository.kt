package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.infrastructure.persistence.mapper.LearningItemRecordMapper
import vn.loi.learning.infrastructure.persistence.store.LearningItemStore

class StoreBackedLearningItemRepository(
    private val store: LearningItemStore
 ) : LearningItemRepository {

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

    override fun findAllEnabled(): List<LearningItem> =
        store.loadAll()
            .filter { record ->
                record.isEnabled
            }
            .map(
                LearningItemRecordMapper::toDomain
            )

    override fun deleteById(
        learningItemId: LearningItemId
    ) {
        val updatedRecords =
            store.loadAll()
                .filterNot { record ->
                    record.id == learningItemId.toString()
                }

        store.saveAll(updatedRecords)
    }

    override fun save(
        learningItem: LearningItem
    ) {
        val newRecord =
            LearningItemRecordMapper.toRecord(learningItem)

        val updatedRecords =
            store.loadAll()
                .filterNot { record ->
                    record.id == newRecord.id
                } + newRecord

        store.saveAll(updatedRecords)
    }
}


