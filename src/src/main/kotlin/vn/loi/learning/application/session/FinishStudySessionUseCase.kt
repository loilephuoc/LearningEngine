package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.StudySession

class FinishStudySessionUseCase(
    private val sessionRepository: StudySessionRepository
) {

    fun execute(
        sessionId: SessionId,
        finishedAt: Moment
    ): StudySession {
        val session = requireNotNull(
            sessionRepository.findById(sessionId)
        ) {
            "Session $sessionId does not exist."
        }

        val finishedSession = session.finish(finishedAt)

        sessionRepository.save(finishedSession)

        return finishedSession
    }
}