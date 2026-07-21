package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.study.StudyQueuePlan
import vn.loi.learning.application.study.StudyQueuePlanningService
import vn.loi.learning.domain.study.session.model.StudySession

/**
 * Bắt đầu một StudySession.
 *
 * Khi StudyQueuePlanningService và StudyQueueService được cung cấp,
 * use case đồng thời:
 * - tạo và lưu StudySession;
 * - yêu cầu planning service lập StudyQueuePlan;
 * - persist plan thành StudyQueueSnapshot.
 *
 * Hai dependency queue là nullable để giữ tương thích với các unit test
 * hoặc adapter chỉ kiểm tra vòng đời StudySession.
 */
class StartStudySessionUseCase(
    private val sessionRepository:
    StudySessionRepository,
    private val studyQueuePlanningService:
    StudyQueuePlanningService? = null,
    private val studyQueueService:
    StudyQueueService? = null
) {

    init {
        require(
            (studyQueuePlanningService == null) ==
                    (studyQueueService == null)
        ) {
            "StudyQueuePlanningService and StudyQueueService " +
                    "must either both be provided or both be absent."
        }
    }

    fun execute(
        command: StartStudySessionCommand
    ): StudySession {
        require(
            sessionRepository.findById(
                command.sessionId
            ) == null
        ) {
            "Session ${command.sessionId} already exists."
        }

        val session =
            StudySession.start(
                id =
                    command.sessionId,
                learnerId =
                    command.learnerId,
                startedAt =
                    command.startedAt,
                policy =
                    command.policy,
                includedContentIds =
                    command.includedContentIds
            )

        sessionRepository.save(
            session
        )

        createQueueWhenEnabled(
            session
        )

        return session
    }

    private fun createQueueWhenEnabled(
        session: StudySession
    ) {
        val planningService =
            studyQueuePlanningService
                ?: return

        val queueService =
            requireNotNull(
                studyQueueService
            )

        val plan =
            planningService.plan(
                session
            )

        persistPlan(
            plan = plan,
            queueService = queueService
        )
    }

    private fun persistPlan(
        plan: StudyQueuePlan,
        queueService: StudyQueueService
    ) {
        queueService.create(
            sessionId =
                plan.sessionId,
            createdAt =
                plan.plannedAt,
            learningItemIds =
                plan.learningItemIds
        )
    }
}