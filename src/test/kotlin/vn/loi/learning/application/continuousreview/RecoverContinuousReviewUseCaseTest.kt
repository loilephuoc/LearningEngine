package vn.loi.learning.application.continuousreview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import vn.loi.learning.application.session.ActiveStudySessionRecovery
import vn.loi.learning.application.session.ContinueGeneralStudyUseCase
import vn.loi.learning.application.session.FinishStudySessionUseCase
import vn.loi.learning.application.session.RecoverActiveStudySessionUseCase
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.application.session.StartStudySessionUseCase
import vn.loi.learning.application.session.StudyQueueService
import vn.loi.learning.application.study.StudyQueuePlanningService
import vn.loi.learning.application.study.StudyQueuePlanner
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContinuousReviewIntentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudyQueueRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository

class RecoverContinuousReviewUseCaseTest {
    private val learner = LearnerId("learner")

    @Test
    fun `disabled intent creates no continuation`() {
        val fixture = fixture()
        assertEquals(
            ContinuousReviewRecoveryResult.Disabled,
            fixture.useCase.execute(learner, Moment(100L))
        )
        assertEquals(emptyList(), fixture.sessions.findAll())
    }

    @Test
    fun `active resumable session wins before intent lookup`() {
        val fixture = fixture()
        val session = StudySession.start(
            SessionId("active"), learner, Moment(10L), SessionPolicy(1, 1),
            topicId = TopicId("topic"), installedPackageId = InstalledPackageId("package")
        ).also(fixture.sessions::save)
        fixture.queues.create(
            session.id, Moment(10L),
            listOf(vn.loi.learning.domain.study.learning.model.LearningItemId("item"))
        )

        val result = assertIs<ContinuousReviewRecoveryResult.ResumedExisting>(
            fixture.useCase.execute(learner, Moment(100L))
        )

        assertEquals(session.id, result.recovery.session.id)
        assertEquals(1, fixture.sessions.findAll().size)
    }

    @Test
    fun `enabled intent without completed predecessor returns explicit no eligible outcome`() {
        val fixture = fixture()
        fixture.intents.enable(
            learner, InstalledPackageId("package"), TopicId("topic"), Moment(1L)
        )

        assertEquals(
            ContinuousReviewRecoveryResult.NoEligiblePredecessor,
            fixture.useCase.execute(learner, Moment(100L))
        )
    }

    private fun fixture(): Fixture {
        val sessions = InMemoryStudySessionRepository()
        val queues = StudyQueueService(InMemoryStudyQueueRepository())
        val planner = StudyQueuePlanner(
            InMemoryContentRepository(), InMemoryLearningItemRepository(),
            InMemoryMemoryStateRepository()
        )
        val start = StartStudySessionUseCase(
            sessions,
            StudyQueuePlanningService(planner),
            queues
        )
        val finish = FinishStudySessionUseCase(sessions, queues)
        val recover = RecoverActiveStudySessionUseCase(sessions, queues, finish)
        val intents = ContinuousReviewService(InMemoryContinuousReviewIntentRepository())
        val continuation = ContinueGeneralStudyUseCase(sessions, queues, start)
        return Fixture(
            sessions, queues, start, intents,
            RecoverContinuousReviewUseCase(intents, sessions, recover, continuation)
        )
    }

    private data class Fixture(
        val sessions: InMemoryStudySessionRepository,
        val queues: StudyQueueService,
        val start: StartStudySessionUseCase,
        val intents: ContinuousReviewService,
        val useCase: RecoverContinuousReviewUseCase
    )
}
