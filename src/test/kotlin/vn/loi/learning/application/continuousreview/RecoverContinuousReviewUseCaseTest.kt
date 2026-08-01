package vn.loi.learning.application.continuousreview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
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
import vn.loi.learning.domain.study.session.model.SessionCompletionProvenance
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
            fixture.useCase.execute(request())
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
            fixture.useCase.execute(request())
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
            fixture.useCase.execute(request())
        )
    }

    @Test
    fun `scope mismatch returns typed inactive result without artifacts`() {
        val fixture = fixture()
        fixture.intents.enable(
            learner, InstalledPackageId("package-a"), TopicId("topic-a"), Moment(1L)
        )

        val first = assertIs<ContinuousReviewRecoveryResult.ScopeInactive>(
            fixture.useCase.execute(request(InstalledPackageId("package-b"), TopicId("topic-b")))
        )
        val second = assertIs<ContinuousReviewRecoveryResult.ScopeInactive>(
            fixture.useCase.execute(request(InstalledPackageId("package-b"), TopicId("topic-b")))
        )

        assertEquals(first, second)
        assertEquals(emptyList(), fixture.sessions.findAll())
        assertNull(fixture.sessions.findActiveByLearner(learner))
    }

    @Test
    fun `ordinary completion without presentation snapshot is eligible and no-work is suppressed`() {
        val fixture = fixture()
        fixture.intents.enable(
            learner, InstalledPackageId("package"), TopicId("topic"), Moment(1L)
        )
        fixture.start.execute(
            StartStudySessionCommand(
                SessionId("ordinary"), learner, Moment(10L), SessionPolicy(1, 1),
                topicId = TopicId("topic"), installedPackageId = InstalledPackageId("package")
            )
        ).finish(Moment(20L)).also(fixture.sessions::save)

        assertEquals(ContinuousReviewRecoveryResult.NoWork, fixture.useCase.execute(request()))
        assertEquals(ContinuousReviewRecoveryResult.NoWork, fixture.useCase.execute(request()))
        assertEquals(1, fixture.sessions.findAll().size)
    }

    @Test
    fun `reconciliation and unknown completions are not eligible`() {
        val fixture = fixture()
        fixture.intents.enable(
            learner, InstalledPackageId("package"), TopicId("topic"), Moment(1L)
        )
        listOf(
            SessionCompletionProvenance.RECOVERY_RECONCILIATION,
            SessionCompletionProvenance.UNKNOWN,
            SessionCompletionProvenance.REPLACED_OR_LEFT
        ).forEachIndexed { index, provenance ->
            StudySession.start(
                SessionId("excluded-$index"), learner, Moment(10L + index), SessionPolicy(1, 1),
                topicId = TopicId("topic"), installedPackageId = InstalledPackageId("package")
            ).finish(Moment(20L + index), completionProvenance = provenance)
                .also(fixture.sessions::save)
        }

        assertEquals(
            ContinuousReviewRecoveryResult.NoEligiblePredecessor,
            fixture.useCase.execute(request())
        )
    }

    private fun request(
        packageId: InstalledPackageId = InstalledPackageId("package"),
        topicId: TopicId? = TopicId("topic")
    ) = ContinuousReviewRecoveryRequest(learner, packageId, topicId, Moment(100L))

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
