package vn.loi.learning.infrastructure.session

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.StudySession

class InMemoryStudySessionRepository : StudySessionRepository {

    private val sessions = linkedMapOf<SessionId, StudySession>()

    override fun findById(
        sessionId: SessionId
    ): StudySession? =
        sessions[sessionId]

    override fun save(session: StudySession) {
        sessions[session.id] = session
    }

    fun count(): Int = sessions.size

    fun clear() {
        sessions.clear()
    }
}