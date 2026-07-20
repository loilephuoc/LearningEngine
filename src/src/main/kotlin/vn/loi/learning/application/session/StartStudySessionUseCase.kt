package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.study.session.model.StudySession

class StartStudySessionUseCase(
    private val sessionRepository: StudySessionRepository
) {

    fun execute(
        command: StartStudySessionCommand
    ): StudySession {
        require(
            sessionRepository.findById(command.sessionId) == null
        ) {
            "Session ${command.sessionId} already exists."
        }

        val session = StudySession.start(
            id = command.sessionId,
            learnerId = command.learnerId,
            startedAt = command.startedAt,
            policy = command.policy
        )

        sessionRepository.save(session)

        return session
    }
}