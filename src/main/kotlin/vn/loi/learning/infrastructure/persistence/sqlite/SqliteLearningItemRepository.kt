package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem as DomainLearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.infrastructure.persistence.mapper.LearningItemRecordMapper
import vn.loi.learning.infrastructure.persistence.record.LearningItemRecord
import vn.loi.learning.infrastructure.persistence.store.LearningItemStore

fun Learning_item.toRecord(): LearningItemRecord = LearningItemRecord(
    id = id,
    contentId = contentId,
    mode = mode,
    isEnabled = isEnabled != 0L
)

fun Learning_item.toDomain(): DomainLearningItem =
    LearningItemRecordMapper.toDomain(toRecord())

class SqliteLearningItemStore(
    private val database: LearningEngineDatabase
) : LearningItemStore {

    private val queries = database.learningItemQueries

    override fun loadAll(): List<LearningItemRecord> {
        return queries.selectAll()
            .executeAsList()
            .map { it.toRecord() }
    }

    override fun saveAll(records: List<LearningItemRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: LearningItemRecord) {
        queries.insertOrReplace(
            id = record.id,
            contentId = record.contentId,
            mode = record.mode,
            isEnabled = if (record.isEnabled) 1L else 0L
        )
    }
}

class SqliteLearningItemRepository(
    private val database: LearningEngineDatabase
) : LearningItemRepository {

    private val queries = database.learningItemQueries
    val store: LearningItemStore = SqliteLearningItemStore(database)

    override fun findById(learningItemId: LearningItemId): DomainLearningItem? {
        return queries.selectById(learningItemId.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findByContentId(contentId: ContentId): List<DomainLearningItem> {
        return queries.selectByContentId(contentId.value)
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun findByContentIds(contentIds: Set<ContentId>): List<DomainLearningItem> {
        if (contentIds.isEmpty()) return emptyList()
        return queries.selectByContentIds(contentIds.map { it.value })
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun findContentIdsByLearningItemIds(
        learningItemIds: Set<LearningItemId>
    ): Map<LearningItemId, ContentId> {
        if (learningItemIds.isEmpty()) return emptyMap()
        return queries.selectContentIdsByItemIds(learningItemIds.map { it.value })
            .executeAsList()
            .associate { LearningItemId(it.id) to ContentId(it.contentId) }
    }

    override fun findAll(): List<DomainLearningItem> {
        return queries.selectAll()
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun findAllEnabled(): List<DomainLearningItem> {
        return queries.selectAllEnabled()
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun save(learningItem: DomainLearningItem) {
        val record = LearningItemRecordMapper.toRecord(learningItem)
        insertOrReplaceRecord(record)
    }

    override fun saveAll(learningItems: List<DomainLearningItem>) {
        if (learningItems.isEmpty()) return
        database.transaction {
            for (item in learningItems) {
                save(item)
            }
        }
    }

    override fun deleteById(learningItemId: LearningItemId) {
        queries.deleteById(learningItemId.value)
    }

    override fun deleteAllById(learningItemIds: Set<LearningItemId>) {
        if (learningItemIds.isEmpty()) return
        queries.deleteByIds(learningItemIds.map { it.value })
    }

    override fun deleteByContentIds(contentIds: Set<ContentId>) {
        if (contentIds.isEmpty()) return
        queries.deleteByContentIds(contentIds.map { it.value })
    }

    private fun insertOrReplaceRecord(record: LearningItemRecord) {
        queries.insertOrReplace(
            id = record.id,
            contentId = record.contentId,
            mode = record.mode,
            isEnabled = if (record.isEnabled) 1L else 0L
        )
    }
}
