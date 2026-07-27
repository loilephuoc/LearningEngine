package vn.loi.learning.infrastructure.persistence.memory

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession

class InMemoryStudySessionRepository : StudySessionRepository {

    private val sessions = linkedMapOf<SessionId, StudySession>()

    override fun findById(
        sessionId: SessionId
    ): StudySession? =
        sessions[sessionId]

    override fun findActiveByLearner(
        learnerId: LearnerId
    ): StudySession? =
        sessions.values
            .asSequence()
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
        sessions.values
            .asSequence()
            .filter { session ->
                session.learnerId == learnerId &&
                    session.topicId == topicId &&
                    session.status == SessionStatus.ACTIVE
            }
            .maxByOrNull { session ->
                session.startedAt.epochMillis
            }

    override fun save(session: StudySession) {
        sessions[session.id] = session
    }

    override fun findLatestUndoableByLearner(learnerId: LearnerId): StudySession? =
        sessions.values.filter { it.learnerId == learnerId && it.undoableReview != null }
            .maxByOrNull { it.startedAt.epochMillis }

    override fun deleteForTopic(learnerId: LearnerId?, topicId: TopicId) {
        sessions.entries.removeIf { (learnerId == null || it.value.learnerId == learnerId) && it.value.topicId == topicId }
    }


    fun count(): Int = sessions.size

    fun clear() {
        sessions.clear()
    }
}
