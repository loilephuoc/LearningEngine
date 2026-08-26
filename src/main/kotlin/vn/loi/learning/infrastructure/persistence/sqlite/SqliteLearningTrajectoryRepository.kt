package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.application.port.LearningTrajectoryRepository
import vn.loi.learning.application.port.LearningTrajectorySnapshot
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.LearningTrajectory as DomainLearningTrajectory
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.infrastructure.persistence.mapper.LearningTrajectoryRecordMapper
import vn.loi.learning.infrastructure.persistence.record.EvidenceChainRecord
import vn.loi.learning.infrastructure.persistence.record.LearningTrajectoryRecord
import vn.loi.learning.infrastructure.persistence.store.LearningTrajectoryStore

fun Learning_trajectory.toRecord(): LearningTrajectoryRecord = LearningTrajectoryRecord(
    schemaVersion = schemaVersion.toInt(),
    learnerId = learnerId,
    contentId = contentId,
    chains = SqliteJsonUtils.decodeOrDefault(chainsJson, emptyList<EvidenceChainRecord>())
)

class SqliteLearningTrajectoryRepository(
    private val database: LearningEngineDatabase
) : LearningTrajectoryRepository, LearningTrajectoryStore {

    private val queries = database.learningTrajectoryQueries

    override fun find(
        learnerId: LearnerId,
        contentId: ContentId
    ): DomainLearningTrajectory? {
        val row = queries.find(learnerId.value, contentId.value).executeAsOneOrNull() ?: return null
        return LearningTrajectoryRecordMapper.toDomain(row.toRecord()).second
    }

    override fun findAll(): List<LearningTrajectorySnapshot> {
        return queries.findAll().executeAsList().map { row ->
            val (learnerId, trajectory) = LearningTrajectoryRecordMapper.toDomain(row.toRecord())
            LearningTrajectorySnapshot(
                learnerId = learnerId,
                trajectory = trajectory
            )
        }
    }

    override fun save(
        learnerId: LearnerId,
        trajectory: DomainLearningTrajectory
    ) {
        val record = LearningTrajectoryRecordMapper.toRecord(learnerId, trajectory)
        insertOrReplaceRecord(record)
    }

    override fun delete(
        learnerId: LearnerId,
        contentId: ContentId
    ) {
        queries.delete(learnerId.value, contentId.value)
    }

    override fun loadAll(): List<LearningTrajectoryRecord> {
        return queries.findAll().executeAsList().map { it.toRecord() }
    }

    override fun saveAll(records: List<LearningTrajectoryRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: LearningTrajectoryRecord) {
        queries.insertOrReplace(
            learnerId = record.learnerId,
            contentId = record.contentId,
            schemaVersion = record.schemaVersion.toLong(),
            chainsJson = SqliteJsonUtils.encode(record.chains)
        )
    }
}
