package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState as DomainMemoryState
import vn.loi.learning.infrastructure.persistence.mapper.MemoryStateRecordMapper
import vn.loi.learning.infrastructure.persistence.record.MemoryStateRecord
import vn.loi.learning.infrastructure.persistence.store.MemoryStateStore

fun Memory_state.toRecord(): MemoryStateRecord = MemoryStateRecord(
    schemaVersion = schemaVersion.toInt(),
    learnerId = learnerId,
    learningItemId = learningItemId,
    stage = stage,
    difficulty = difficulty,
    stabilityDays = stabilityDays,
    dueAtEpochMillis = dueAtEpochMillis,
    lastReviewedAtEpochMillis = lastReviewedAtEpochMillis,
    reviewCount = reviewCount.toInt(),
    lapseCount = lapseCount.toInt()
)

fun Memory_state.toDomain(): DomainMemoryState =
    MemoryStateRecordMapper.toDomain(toRecord())

class SqliteMemoryStateRepository(
    private val database: LearningEngineDatabase
) : MemoryStateRepository, MemoryStateQuery, MemoryStateStore {

    private val queries = database.memoryStateQueries

    override fun find(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): DomainMemoryState? {
        return queries.find(learnerId.value, learningItemId.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findAll(learnerId: LearnerId): List<DomainMemoryState> {
        return queries.findAllByLearner(learnerId.value)
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun findAll(): List<DomainMemoryState> {
        return queries.findAll()
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun save(memoryState: DomainMemoryState) {
        val record = MemoryStateRecordMapper.toRecord(memoryState)
        insertOrReplaceRecord(record)
    }

    override fun delete(learnerId: LearnerId, learningItemId: LearningItemId) {
        queries.delete(learnerId.value, learningItemId.value)
    }

    override fun deleteByLearningItemIds(learningItemIds: Set<LearningItemId>) {
        if (learningItemIds.isEmpty()) return
        queries.deleteByLearningItemIds(learningItemIds.map { it.value })
    }

    override fun load(): List<MemoryStateRecord> {
        return queries.findAll()
            .executeAsList()
            .map { it.toRecord() }
    }

    fun loadAll(): List<MemoryStateRecord> = load()

    override fun save(records: List<MemoryStateRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: MemoryStateRecord) {
        queries.insertOrReplace(
            learnerId = record.learnerId,
            learningItemId = record.learningItemId,
            schemaVersion = record.schemaVersion.toLong(),
            stage = record.stage,
            difficulty = record.difficulty,
            stabilityDays = record.stabilityDays,
            dueAtEpochMillis = record.dueAtEpochMillis,
            lastReviewedAtEpochMillis = record.lastReviewedAtEpochMillis,
            reviewCount = record.reviewCount.toLong(),
            lapseCount = record.lapseCount.toLong()
        )
    }
}
