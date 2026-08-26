package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.application.continuousreview.ContinuousReviewIntent
import vn.loi.learning.application.port.ContinuousReviewIntentRepository
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.infrastructure.persistence.record.ContinuousReviewIntentRecord

fun Continuous_review_intent.toRecord(): ContinuousReviewIntentRecord = ContinuousReviewIntentRecord(
    learnerId = learnerId,
    installedPackageId = installedPackageId,
    topicId = topicId,
    enabled = enabled != 0L,
    updatedAtEpochMillis = updatedAtEpochMillis,
    lastNoWorkPredecessorId = lastNoWorkPredecessorId
)

fun ContinuousReviewIntentRecord.toDomain(): ContinuousReviewIntent = ContinuousReviewIntent(
    learnerId = LearnerId(learnerId),
    installedPackageId = InstalledPackageId(installedPackageId),
    topicId = topicId?.let(::TopicId),
    enabled = enabled,
    updatedAt = Moment(updatedAtEpochMillis),
    lastNoWorkPredecessorId = lastNoWorkPredecessorId?.let(::SessionId)
)

class SqliteContinuousReviewIntentRepository(
    private val database: LearningEngineDatabase
) : ContinuousReviewIntentRepository {

    private val queries = database.continuousReviewIntentQueries

    override fun findByLearner(learnerId: LearnerId): ContinuousReviewIntent? {
        return queries.findByLearner(learnerId.value)
            .executeAsOneOrNull()
            ?.toRecord()
            ?.toDomain()
    }

    override fun save(intent: ContinuousReviewIntent) {
        val record = ContinuousReviewIntentRecord(
            learnerId = intent.learnerId.value,
            installedPackageId = intent.installedPackageId.value,
            topicId = intent.topicId?.value,
            enabled = intent.enabled,
            updatedAtEpochMillis = intent.updatedAt.epochMillis,
            lastNoWorkPredecessorId = intent.lastNoWorkPredecessorId?.value
        )
        insertOrReplaceRecord(record)
    }

    fun loadAll(): List<ContinuousReviewIntentRecord> {
        return queries.findAll()
            .executeAsList()
            .map { it.toRecord() }
    }

    fun saveAll(records: List<ContinuousReviewIntentRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: ContinuousReviewIntentRecord) {
        queries.insertOrReplace(
            learnerId = record.learnerId,
            installedPackageId = record.installedPackageId,
            topicId = record.topicId,
            enabled = if (record.enabled) 1L else 0L,
            updatedAtEpochMillis = record.updatedAtEpochMillis,
            lastNoWorkPredecessorId = record.lastNoWorkPredecessorId
        )
    }
}
