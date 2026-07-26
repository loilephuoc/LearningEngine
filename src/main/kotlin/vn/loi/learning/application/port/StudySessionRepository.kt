package vn.loi.learning.application.port

import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.StudySession

interface StudySessionRepository {

    fun findById(sessionId: SessionId): StudySession?

    fun findActiveByLearner(learnerId: LearnerId): StudySession?

    fun findActiveByLearnerAndTopic(
        learnerId: LearnerId,
        topicId: TopicId
    ): StudySession? =
        findActiveByLearner(learnerId)
            ?.takeIf { session ->
                session.topicId == topicId
            }

    fun findLatestUndoableByLearner(learnerId: LearnerId): StudySession? = null

    fun save(session: StudySession)

    fun deleteForTopic(learnerId: LearnerId, topicId: TopicId) {}
}
