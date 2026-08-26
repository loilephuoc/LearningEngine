package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.application.port.StudyQueueRepository
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.infrastructure.persistence.mapper.StudyQueueRecordMapper
import vn.loi.learning.infrastructure.persistence.record.StudyQueueRecord
import vn.loi.learning.infrastructure.persistence.store.StudyQueueStore

fun Study_queue.toRecord(): StudyQueueRecord = StudyQueueRecord(
    schemaVersion = schemaVersion.toInt(),
    sessionId = sessionId,
    createdAtEpochMillis = createdAtEpochMillis,
    learningItemIds = SqliteJsonUtils.decode(learningItemIdsJson),
    currentIndex = currentIndex.toInt(),
    itemOrigins = SqliteJsonUtils.decodeOrDefault(itemOriginsJson, emptyMap()),
    itemContentIds = SqliteJsonUtils.decodeOrDefault(itemContentIdsJson, emptyMap()),
    configuredNewTarget = configuredNewTarget.toInt(),
    effectiveNewWorkload = effectiveNewWorkload.toInt(),
    configuredReviewTarget = configuredReviewTarget.toInt(),
    effectiveReviewWorkload = effectiveReviewWorkload.toInt(),
    fixedPracticeMembership = SqliteJsonUtils.decodeOrDefault(fixedPracticeMembershipJson, emptyList()),
    practiceSeed = practiceSeed,
    practiceRound = practiceRound.toInt(),
    practiceLoopPolicy = practiceLoopPolicy,
    practiceReinforcementStates = SqliteJsonUtils.decodeOrDefault(practiceReinforcementStatesJson, emptyMap()),
    practiceExposureSequence = practiceExposureSequence.toInt(),
    practiceMembershipUndo = practiceMembershipUndoJson?.let { SqliteJsonUtils.decode(it) },
    coverageReinforcementStates = SqliteJsonUtils.decodeOrDefault(coverageReinforcementStatesJson, emptyMap()),
    coverageReinforcementUndo = coverageReinforcementUndoJson?.let { SqliteJsonUtils.decode(it) }
)

fun Study_queue.toDomain(): StudyQueueSnapshot =
    StudyQueueRecordMapper.toDomain(toRecord())

class SqliteStudyQueueRepository(
    private val database: LearningEngineDatabase
) : StudyQueueRepository, StudyQueueStore {

    private val queries = database.studyQueueQueries

    override fun findBySessionId(sessionId: SessionId): StudyQueueSnapshot? {
        return queries.findBySessionId(sessionId.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findAll(): List<StudyQueueSnapshot> {
        return queries.findAll()
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun save(snapshot: StudyQueueSnapshot) {
        val record = StudyQueueRecordMapper.toRecord(snapshot)
        insertOrReplaceRecord(record)
    }

    override fun deleteBySessionId(sessionId: SessionId) {
        queries.deleteBySessionId(sessionId.value)
    }

    override fun loadAll(): List<StudyQueueRecord> {
        return queries.findAll()
            .executeAsList()
            .map { it.toRecord() }
    }

    override fun saveAll(records: List<StudyQueueRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: StudyQueueRecord) {
        queries.insertOrReplace(
            sessionId = record.sessionId,
            schemaVersion = record.schemaVersion.toLong(),
            createdAtEpochMillis = record.createdAtEpochMillis,
            learningItemIdsJson = SqliteJsonUtils.encode(record.learningItemIds),
            currentIndex = record.currentIndex.toLong(),
            itemOriginsJson = SqliteJsonUtils.encode(record.itemOrigins),
            itemContentIdsJson = SqliteJsonUtils.encode(record.itemContentIds),
            configuredNewTarget = record.configuredNewTarget.toLong(),
            effectiveNewWorkload = record.effectiveNewWorkload.toLong(),
            configuredReviewTarget = record.configuredReviewTarget.toLong(),
            effectiveReviewWorkload = record.effectiveReviewWorkload.toLong(),
            fixedPracticeMembershipJson = SqliteJsonUtils.encode(record.fixedPracticeMembership),
            practiceSeed = record.practiceSeed,
            practiceRound = record.practiceRound.toLong(),
            practiceLoopPolicy = record.practiceLoopPolicy,
            practiceReinforcementStatesJson = SqliteJsonUtils.encode(record.practiceReinforcementStates),
            practiceExposureSequence = record.practiceExposureSequence.toLong(),
            practiceMembershipUndoJson = record.practiceMembershipUndo?.let { SqliteJsonUtils.encode(it) },
            coverageReinforcementStatesJson = SqliteJsonUtils.encode(record.coverageReinforcementStates),
            coverageReinforcementUndoJson = record.coverageReinforcementUndo?.let { SqliteJsonUtils.encode(it) }
        )
    }
}
