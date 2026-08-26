package vn.loi.learning.infrastructure.persistence.sqlite

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.StudySession as DomainStudySession
import vn.loi.learning.infrastructure.persistence.mapper.StudySessionRecordMapper
import vn.loi.learning.infrastructure.persistence.record.StudySessionRecord
import vn.loi.learning.infrastructure.persistence.store.StudySessionStore

fun Study_session.toRecord(): StudySessionRecord = StudySessionRecord(
    schemaVersion = schemaVersion.toInt(),
    id = id,
    learnerId = learnerId,
    startedAtEpochMillis = startedAtEpochMillis,
    status = status,
    policyNewItemLimit = policyNewItemLimit.toInt(),
    policyReviewItemLimit = policyReviewItemLimit.toInt(),
    policyAllowRepeatInSameSession = policyAllowRepeatInSameSession != 0L,
    policyEvaluation = policyEvaluation,
    policyPracticeLoop = policyPracticeLoop,
    policyFocusedPracticeKind = policyFocusedPracticeKind,
    includedContentIds = SqliteJsonUtils.decodeOrDefault(includedContentIdsJson, emptyList()),
    reviewedItemIds = SqliteJsonUtils.decodeOrDefault(reviewedItemIdsJson, emptyList()),
    reviewedContentIds = SqliteJsonUtils.decodeOrDefault(reviewedContentIdsJson, emptyList()),
    introducedContentIds = SqliteJsonUtils.decodeOrDefault(introducedContentIdsJson, emptyList()),
    lapsedContentIds = SqliteJsonUtils.decodeOrDefault(lapsedContentIdsJson, emptyList()),
    newItemsReviewed = newItemsReviewed.toInt(),
    reviewItemsReviewed = reviewItemsReviewed.toInt(),
    finishedAtEpochMillis = finishedAtEpochMillis,
    currentLearningItemId = currentLearningItemId,
    currentItemPresentedAtEpochMillis = currentItemPresentedAtEpochMillis,
    answerRevealed = answerRevealed != 0L,
    pendingReviewEventId = pendingReviewEventId,
    pendingReviewLearningItemId = pendingReviewLearningItemId,
    pendingReviewRating = pendingReviewRating,
    pendingReviewReviewedAtEpochMillis = pendingReviewReviewedAtEpochMillis,
    pendingReviewResponseTimeMillis = pendingReviewResponseTimeMillis,
    pendingReviewRatingSource = pendingReviewRatingSource,
    pendingRecallResult = pendingRecallResult,
    pendingRecallRevealUsed = pendingRecallRevealUsed != 0L,
    pendingRecallTypingLatencyMillis = pendingRecallTypingLatencyMillis,
    undoableReview = undoableReviewJson?.let { SqliteJsonUtils.decode(it) },
    completionSnapshot = completionSnapshotJson?.let { SqliteJsonUtils.decode(it) },
    completionProvenance = completionProvenance,
    topicId = topicId,
    installedPackageId = installedPackageId,
    studyMode = studyMode,
    recallModeHistory = SqliteJsonUtils.decodeOrDefault(recallModeHistoryJson, emptyList())
)

fun Study_session.toDomain(): DomainStudySession =
    StudySessionRecordMapper.toDomain(toRecord())

class SqliteStudySessionRepository(
    private val database: LearningEngineDatabase
) : StudySessionRepository, StudySessionStore {

    private val queries = database.studySessionQueries

    override fun findById(sessionId: SessionId): DomainStudySession? {
        return queries.findById(sessionId.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findActiveByLearner(learnerId: LearnerId): DomainStudySession? {
        return queries.findActiveByLearner(learnerId.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findActiveByLearnerAndTopic(
        learnerId: LearnerId,
        topicId: TopicId
    ): DomainStudySession? {
        return queries.findActiveByLearnerAndTopic(learnerId.value, topicId.value)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override fun findLatestUndoableByLearner(learnerId: LearnerId): DomainStudySession? {
        return queries.findLatestUndoableByLearner(learnerId.value)
            .executeAsOneOrNull()
            ?.let { row ->
                Study_session(
                    id = row.id,
                    learnerId = row.learnerId,
                    schemaVersion = row.schemaVersion,
                    startedAtEpochMillis = row.startedAtEpochMillis,
                    status = row.status,
                    policyNewItemLimit = row.policyNewItemLimit,
                    policyReviewItemLimit = row.policyReviewItemLimit,
                    policyAllowRepeatInSameSession = row.policyAllowRepeatInSameSession,
                    policyEvaluation = row.policyEvaluation,
                    policyPracticeLoop = row.policyPracticeLoop,
                    policyFocusedPracticeKind = row.policyFocusedPracticeKind,
                    includedContentIdsJson = row.includedContentIdsJson,
                    reviewedItemIdsJson = row.reviewedItemIdsJson,
                    reviewedContentIdsJson = row.reviewedContentIdsJson,
                    introducedContentIdsJson = row.introducedContentIdsJson,
                    lapsedContentIdsJson = row.lapsedContentIdsJson,
                    newItemsReviewed = row.newItemsReviewed,
                    reviewItemsReviewed = row.reviewItemsReviewed,
                    finishedAtEpochMillis = row.finishedAtEpochMillis,
                    currentLearningItemId = row.currentLearningItemId,
                    currentItemPresentedAtEpochMillis = row.currentItemPresentedAtEpochMillis,
                    answerRevealed = row.answerRevealed,
                    pendingReviewEventId = row.pendingReviewEventId,
                    pendingReviewLearningItemId = row.pendingReviewLearningItemId,
                    pendingReviewRating = row.pendingReviewRating,
                    pendingReviewReviewedAtEpochMillis = row.pendingReviewReviewedAtEpochMillis,
                    pendingReviewResponseTimeMillis = row.pendingReviewResponseTimeMillis,
                    pendingReviewRatingSource = row.pendingReviewRatingSource,
                    pendingRecallResult = row.pendingRecallResult,
                    pendingRecallRevealUsed = row.pendingRecallRevealUsed,
                    pendingRecallTypingLatencyMillis = row.pendingRecallTypingLatencyMillis,
                    undoableReviewJson = row.undoableReviewJson,
                    completionSnapshotJson = row.completionSnapshotJson,
                    completionProvenance = row.completionProvenance,
                    topicId = row.topicId,
                    installedPackageId = row.installedPackageId,
                    studyMode = row.studyMode,
                    recallModeHistoryJson = row.recallModeHistoryJson
                ).toDomain()
            }
    }

    override fun findAll(): List<DomainStudySession> {
        return queries.findAll()
            .executeAsList()
            .map { it.toDomain() }
    }

    override fun save(session: DomainStudySession) {
        val record = StudySessionRecordMapper.toRecord(session)
        insertOrReplaceRecord(record)
    }

    override fun deleteById(sessionId: SessionId) {
        queries.deleteById(sessionId.value)
    }

    override fun deleteForTopic(learnerId: LearnerId?, topicId: TopicId) {
        if (learnerId == null) {
            queries.deleteForTopic(topicId.value)
        } else {
            queries.deleteForTopicAndLearner(learnerId.value, topicId.value)
        }
    }

    override fun loadAll(): List<StudySessionRecord> {
        return queries.findAll()
            .executeAsList()
            .map { it.toRecord() }
    }

    override fun saveAll(records: List<StudySessionRecord>) {
        if (records.isEmpty()) return
        database.transaction {
            for (record in records) {
                insertOrReplaceRecord(record)
            }
        }
    }

    private fun insertOrReplaceRecord(record: StudySessionRecord) {
        queries.insertOrReplace(
            id = record.id,
            learnerId = record.learnerId,
            schemaVersion = record.schemaVersion.toLong(),
            startedAtEpochMillis = record.startedAtEpochMillis,
            status = record.status,
            policyNewItemLimit = record.policyNewItemLimit.toLong(),
            policyReviewItemLimit = record.policyReviewItemLimit.toLong(),
            policyAllowRepeatInSameSession = if (record.policyAllowRepeatInSameSession) 1L else 0L,
            policyEvaluation = record.policyEvaluation,
            policyPracticeLoop = record.policyPracticeLoop,
            policyFocusedPracticeKind = record.policyFocusedPracticeKind,
            includedContentIdsJson = SqliteJsonUtils.encode(record.includedContentIds),
            reviewedItemIdsJson = SqliteJsonUtils.encode(record.reviewedItemIds),
            reviewedContentIdsJson = SqliteJsonUtils.encode(record.reviewedContentIds),
            introducedContentIdsJson = SqliteJsonUtils.encode(record.introducedContentIds),
            lapsedContentIdsJson = SqliteJsonUtils.encode(record.lapsedContentIds),
            newItemsReviewed = record.newItemsReviewed.toLong(),
            reviewItemsReviewed = record.reviewItemsReviewed.toLong(),
            finishedAtEpochMillis = record.finishedAtEpochMillis,
            currentLearningItemId = record.currentLearningItemId,
            currentItemPresentedAtEpochMillis = record.currentItemPresentedAtEpochMillis,
            answerRevealed = if (record.answerRevealed) 1L else 0L,
            pendingReviewEventId = record.pendingReviewEventId,
            pendingReviewLearningItemId = record.pendingReviewLearningItemId,
            pendingReviewRating = record.pendingReviewRating,
            pendingReviewReviewedAtEpochMillis = record.pendingReviewReviewedAtEpochMillis,
            pendingReviewResponseTimeMillis = record.pendingReviewResponseTimeMillis,
            pendingReviewRatingSource = record.pendingReviewRatingSource,
            pendingRecallResult = record.pendingRecallResult,
            pendingRecallRevealUsed = if (record.pendingRecallRevealUsed) 1L else 0L,
            pendingRecallTypingLatencyMillis = record.pendingRecallTypingLatencyMillis,
            undoableReviewJson = record.undoableReview?.let { SqliteJsonUtils.encode(it) },
            completionSnapshotJson = record.completionSnapshot?.let { SqliteJsonUtils.encode(it) },
            completionProvenance = record.completionProvenance,
            topicId = record.topicId,
            installedPackageId = record.installedPackageId,
            studyMode = record.studyMode,
            recallModeHistoryJson = SqliteJsonUtils.encode(record.recallModeHistory)
        )
    }
}
