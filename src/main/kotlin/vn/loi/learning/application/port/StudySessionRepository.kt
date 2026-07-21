package vn.loi.learning.application.port

import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.StudySession

interface StudySessionRepository {

    fun findById(sessionId: SessionId): StudySession?

    fun findActiveByLearner(learnerId: LearnerId): StudySession?

    fun save(session: StudySession)
}