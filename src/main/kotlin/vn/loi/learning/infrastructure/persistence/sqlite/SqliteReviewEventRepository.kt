package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewEvent as DomainReviewEvent
import vn.loi.learning.infrastructure.persistence.mapper.ReviewEventRecordMapper
import vn.loi.learning.infrastructure.persistence.record.MemoryStateRecord
import vn.loi.learning.infrastructure.persistence.record.ReviewEventRecord
import vn.loi.learning.infrastructure.persistence.store.ReviewEventStore

fun Review_event.toRecord(): ReviewEventRecord = ReviewEventRecord(
    schemaVersion = schemaVersion.toInt(),
    id = id,
    rating = rating,
    reviewedAtEpochMillis = reviewedAtEpochMillis,
    responseTimeMillis = responseTimeMillis,
    stateBefore = SqliteJsonUtils.decode<MemoryStateRecord>(stateBeforeJson),
    stateAfter = SqliteJsonUtils.decode<MemoryStateRecord>(stateAfterJson),
    source = source
)

fun Review_event.toDomain(): DomainReviewEvent =
    ReviewEventRecordMapper.toDomain(toRecord())

class SqliteReviewEventRepository(
    private val database: LearningEngineDatabase
) : ReviewEventRepository, ReviewEventStore {

    private val queries = database.reviewEventQueries

    override fun append(event: DomainReviewEvent) {
        require(queries.selectById(event.id.value).executeAsOneOrNull() == null) {
            "ReviewEvent with ID ${event.id} already exists."
        }
        val record = ReviewEventRecordMapper.toRecord(event)
        insertRecord(record)
    }

    override fun findAll(learnerId: LearnerId): List<DomainReviewEvent> {
        return queries.findAllByLearner(learnerId.value)
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun findAll(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): List<DomainReviewEvent> {
        return queries.findAllByLearnerAndItem(learnerId.value, learningItemId.value)
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun findAll(): List<DomainReviewEvent> {
        return queries.findAll()
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun removeLatest(event: DomainReviewEvent) {
        val latest = queries.findLatestByLearner(event.stateAfter.learnerId.value).executeAsOneOrNull()
        require(latest != null && latest.id == event.id.value) {
            "Only the latest committed review event for learner ${event.stateAfter.learnerId.value} can be removed."
        }
        queries.deleteById(event.id.value)
    }

    override fun deleteByLearningItemIds(learningItemIds: Set<LearningItemId>) {
        if (learningItemIds.isEmpty()) return
        queries.deleteByLearningItemIds(learningItemIds.map { it.value })
    }

    override fun loadAll(): List<ReviewEventRecord> {
        return queries.findAll()
            .executeAsList()
            .map { it.toRecord() }
    }

    override fun saveAll(records: List<ReviewEventRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertRecord(record)
            }
        }
    }

    private fun insertRecord(record: ReviewEventRecord) {
        queries.insert(
            id = record.id,
            learnerId = record.stateAfter.learnerId,
            learningItemId = record.stateAfter.learningItemId,
            schemaVersion = record.schemaVersion.toLong(),
            rating = record.rating,
            reviewedAtEpochMillis = record.reviewedAtEpochMillis,
            responseTimeMillis = record.responseTimeMillis,
            stateBeforeJson = SqliteJsonUtils.encode(record.stateBefore),
            stateAfterJson = SqliteJsonUtils.encode(record.stateAfter),
            source = record.source
        )
    }
}
