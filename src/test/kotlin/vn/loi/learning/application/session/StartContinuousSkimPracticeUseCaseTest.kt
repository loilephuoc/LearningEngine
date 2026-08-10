package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudyQueueRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository

class StartContinuousSkimPracticeUseCaseTest {
    @Test
    fun `completed mixed coverage becomes shuffled practice without resetting daily targets`() {
        val sessions = InMemoryStudySessionRepository()
        val queues = StudyQueueService(InMemoryStudyQueueRepository())
        val items = (1..11).map { LearningItemId("item-$it") }
        val origins = items.mapIndexed { index, id ->
            id to if (index == 0) SessionItemOrigin.NEW else SessionItemOrigin.REVIEW
        }.toMap()
        val contents = items.associateWith { ContentId("content-${it.value}") }
        val predecessor = StudySession.start(
            SessionId("coverage"), LearnerId("learner"), Moment(1),
            SessionPolicy(newItemLimit = 1, reviewItemLimit = 10)
        ).finish(Moment(2))
        sessions.save(predecessor)
        queues.create(
            predecessor.id, Moment(1), items, origins, contents,
            configuredNewTarget = 1, effectiveNewWorkload = 1,
            configuredReviewTarget = 10, effectiveReviewWorkload = 10
        )

        val accepted = assertIs<StartContinuousSkimPracticeResult.Accepted>(
            StartContinuousSkimPracticeUseCase(sessions, queues).execute(
                StartContinuousSkimPracticeRequest(predecessor.id, Moment(3))
            )
        )

        assertEquals(SessionEvaluationPolicy.PRACTICE_ONLY, accepted.session.policy.evaluationPolicy)
        assertEquals(PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED, accepted.queue.practiceLoopPolicy)
        assertEquals(items.toSet(), accepted.queue.learningItemIds.toSet())
        assertNotEquals(items, accepted.queue.learningItemIds)
        assertEquals(0, accepted.queue.configuredNewTarget)
        assertEquals(items.size, accepted.queue.configuredReviewTarget)
        assertTrue(accepted.queue.practiceRound >= 1)
    }

    @Test
    fun `practice feedback reuses graduated Again and Hard spacing across rounds`() {
        val ids = (1..20).map { LearningItemId("item-$it") }
        var queue = StudyQueueSnapshot.create(
            SessionId("practice"), Moment(1), ids,
            practiceSeed = 42,
            practiceLoopPolicy = PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED
        )
        val again = requireNotNull(queue.currentLearningItemId)
        queue = queue.advancePractice(PracticeRecallResult.INCORRECT)
        assertEquals(2, queue.practiceReinforcementStates[again]?.previousGap)

        val hard = requireNotNull(queue.currentLearningItemId)
        queue = queue.advancePractice(PracticeRecallResult.ALMOST_CORRECT)
        assertEquals(4, queue.practiceReinforcementStates[hard]?.previousGap)
    }
}
