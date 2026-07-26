package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.mapper.StudySessionRecordMapper
import vn.loi.learning.infrastructure.persistence.store.StudySessionStore

/**
 * StudySessionRepository backed by a StudySessionStore.
 *
 * Repository làm việc với Domain.
 * Store làm việc với Persistence Record.
 */
class StoreBackedStudySessionRepository(
    private val store: StudySessionStore
) : StudySessionRepository {

    override fun save(
        session: StudySession
    ) {
        val record =
            StudySessionRecordMapper.toRecord(
                session
            )

        val existingRecords =
            store.loadAll()

        val existingRecord =
            existingRecords.firstOrNull { existing ->
                existing.id == record.id
            }

        if (existingRecord == record) {
            return
        }

        val updatedRecords =
            existingRecords.filterNot { existing ->
                existing.id == record.id
            } + record

        store.saveAll(
            updatedRecords
        )
    }

    override fun findById(
        sessionId: SessionId
    ): StudySession? =
        store.loadAll()
            .firstOrNull { record ->
                record.id == sessionId.toString()
            }
            ?.let(
                StudySessionRecordMapper::toDomain
            )

    override fun findActiveByLearner(
        learnerId: LearnerId
    ): StudySession? =
        store.loadAll()
            .asSequence()
            .map(
                StudySessionRecordMapper::toDomain
            )
            .filter { session ->
                session.learnerId == learnerId &&
                        session.status == SessionStatus.ACTIVE
            }
            .maxByOrNull { session ->
                session.startedAt.epochMillis
            }

    override fun findActiveByLearnerAndTopic(
        learnerId: LearnerId,
        topicId: TopicId
    ): StudySession? =
        store.loadAll()
            .asSequence()
            .map(
                StudySessionRecordMapper::toDomain
            )
            .filter { session ->
                session.learnerId == learnerId &&
                    session.topicId == topicId &&
                    session.status == SessionStatus.ACTIVE
            }
            .maxByOrNull { session ->
                session.startedAt.epochMillis
            }

    override fun findLatestUndoableByLearner(learnerId: LearnerId): StudySession? =
        store.loadAll().asSequence()
            .map(StudySessionRecordMapper::toDomain)
            .filter { it.learnerId == learnerId && it.undoableReview != null }
            .maxByOrNull { it.undoableReview!!.memoryStateBefore.lastReviewedAt?.epochMillis ?: it.startedAt.epochMillis }

    override fun deleteForTopic(learnerId: LearnerId, topicId: TopicId) {
        val current = store.loadAll()
        val updated = current.filterNot {
            it.learnerId == learnerId.toString() && it.topicId == topicId.toString()
        }
        if (updated.size != current.size) {
            store.saveAll(updated)
        }
    }
}
