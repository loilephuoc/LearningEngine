package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.SessionCompletionProvenance
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudyQueueRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository

class RecoverActiveStudySessionUseCaseTest {

    private val sessionRepository =
        InMemoryStudySessionRepository()

    private val queueRepository =
        InMemoryStudyQueueRepository()

    private val queueService =
        StudyQueueService(
            repository = queueRepository
        )

    private val finishUseCase =
        FinishStudySessionUseCase(
            sessionRepository = sessionRepository,
            studyQueueService = queueService
        )

    private val useCase =
        RecoverActiveStudySessionUseCase(
            sessionRepository = sessionRepository,
            studyQueueService = queueService,
            finishStudySessionUseCase = finishUseCase
        )

    @Test
    fun `returns no active session when learner has none`() {
        val result =
            useCase.execute(
                learnerId = LearnerId("learner-1"),
                recoveredAt = Moment(2_000L)
            )

        assertIs<
                ActiveStudySessionRecovery.NoActiveSession
                >(result)
    }

    @Test
    fun `returns resumable session with queue progress`() {
        val session = createSession()
        sessionRepository.save(session)

        queueService.create(
            sessionId = session.id,
            createdAt = session.startedAt,
            learningItemIds =
                listOf(
                    LearningItemId("item-1"),
                    LearningItemId("item-2")
                )
        )

        queueService.advance(session.id)

        val result =
            assertIs<
                    ActiveStudySessionRecovery.Resumable
                    >(
                useCase.execute(
                    learnerId = session.learnerId,
                    recoveredAt = Moment(2_000L)
                )
            )

        assertEquals(session, result.session)
        assertEquals(2, result.queueProgress.totalItemCount)
        assertEquals(1, result.queueProgress.completedItemCount)
        assertEquals(
            LearningItemId("item-2"),
            result.queueProgress.currentLearningItemId
        )
        assertEquals(
            SessionStatus.ACTIVE,
            sessionRepository
                .findById(session.id)
                ?.status
        )
    }

    @Test
    fun `closes active session when persisted queue is missing`() {
        val session = createSession()
        sessionRepository.save(session)

        val result =
            assertIs<
                    ActiveStudySessionRecovery
                        .ClosedIncompleteSession
                    >(
                useCase.execute(
                    learnerId = session.learnerId,
                    recoveredAt = Moment(2_000L)
                )
            )

        assertEquals(
            ActiveStudySessionRecovery
                .ClosedIncompleteSession
                .Reason
                .MISSING_QUEUE,
            result.reason
        )
        assertEquals(SessionStatus.FINISHED, result.session.status)
        assertEquals(Moment(2_000L), result.session.finishedAt)
        assertEquals(SessionCompletionProvenance.RECOVERY_RECONCILIATION, result.session.completionProvenance)
        assertNull(
            sessionRepository.findActiveByLearner(
                session.learnerId
            )
        )
    }

    @Test
    fun `finalizes active session whose queue already completed`() {
        val session = createSession()
        sessionRepository.save(session)

        queueService.create(
            sessionId = session.id,
            createdAt = session.startedAt,
            learningItemIds =
                listOf(
                    LearningItemId("item-1")
                )
        )
        queueService.advance(session.id)

        val result =
            assertIs<
                    ActiveStudySessionRecovery
                        .ClosedIncompleteSession
                    >(
                useCase.execute(
                    learnerId = session.learnerId,
                    recoveredAt = Moment(2_000L)
                )
            )

        assertEquals(
            ActiveStudySessionRecovery
                .ClosedIncompleteSession
                .Reason
                .COMPLETED_QUEUE,
            result.reason
        )
        assertEquals(SessionStatus.FINISHED, result.session.status)
        assertEquals(SessionCompletionProvenance.RECOVERY_RECONCILIATION, result.session.completionProvenance)
        assertEquals(1, requireNotNull(result.queueProgress).completedItemCount)
        assertTrue(result.queueProgress.isCompleted)
        assertNull(queueService.get(session.id))
    }

    @Test
    fun `uses session start when recovery clock is earlier`() {
        val session = createSession()
        sessionRepository.save(session)

        val result =
            assertIs<
                    ActiveStudySessionRecovery
                        .ClosedIncompleteSession
                    >(
                useCase.execute(
                    learnerId = session.learnerId,
                    recoveredAt = Moment(500L)
                )
            )

        assertEquals(
            session.startedAt,
            result.session.finishedAt
        )
    }

    private fun createSession(): StudySession =
        StudySession.start(
            id = SessionId("session-1"),
            learnerId = LearnerId("learner-1"),
            startedAt = Moment(1_000L),
            policy =
                SessionPolicy(
                    newItemLimit = 10,
                    reviewItemLimit = 10
                )
        )
}
