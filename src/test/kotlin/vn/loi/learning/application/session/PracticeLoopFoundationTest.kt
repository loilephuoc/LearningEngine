package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.application.study.ContentLearningStateQueryService
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.scheduling.SimpleScheduler
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.mapper.StudyQueueRecordMapper
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudyQueueRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository

class PracticeLoopFoundationTest {
    private val direct = object : TransactionRunner {
        override fun <T> runInTransaction(block: () -> T): T = block()
    }

    @Test
    fun `fixed membership loops through deterministic full shuffled rounds without queue growth`() {
        val ids = (1..20).map { LearningItemId("item-$it") }
        var queue = StudyQueueSnapshot.create(
            SessionId("practice"), Moment(1), ids, practiceSeed = 42L
        )
        val roundOne = queue.learningItemIds
        assertEquals(ids.toSet(), roundOne.toSet())
        repeat(ids.size) { queue = queue.advancePractice() }

        assertEquals(2, queue.practiceRound)
        assertEquals(0, queue.currentIndex)
        assertEquals(ids.size, queue.learningItemIds.size)
        assertEquals(ids.toSet(), queue.learningItemIds.toSet())
        assertNotEquals(roundOne, queue.learningItemIds)
        assertNotEquals(roundOne.last(), queue.learningItemIds.first())
        assertEquals(PracticeProgress(2, 1, 20), queue.practiceProgress)

        val restored = StudyQueueRecordMapper.toDomain(StudyQueueRecordMapper.toRecord(queue))
        assertEquals(queue, restored)
    }

    @Test
    fun `practice completion advances only local loop state`() {
        val sessions = InMemoryStudySessionRepository()
        val queues = StudyQueueService(InMemoryStudyQueueRepository())
        val id = LearningItemId("practice-item")
        val sessionId = SessionId("practice-session")
        val session = practiceSession(sessionId).presentItem(id, Moment(2))
        sessions.save(session)
        queues.create(sessionId, Moment(1), listOf(id), practiceSeed = 7)

        val result = CompletePracticeItemUseCase(sessions, queues, direct).execute(
            CompletePracticeItemCommand(sessionId, id, PracticeRecallResult.CORRECT)
        )

        assertEquals(0, result.session.totalReviews)
        assertEquals(2, result.progress.round)
        assertEquals(1, queues.require(sessionId).learningItemIds.size)
        assertTrue(result.session.currentLearningItemId == null)
    }

    @Test
    fun `manual override commits provenance without changing practice policy or membership and undo restores state`() {
        val sessions = InMemoryStudySessionRepository()
        val queues = StudyQueueService(InMemoryStudyQueueRepository())
        val items = InMemoryLearningItemRepository()
        val memories = InMemoryMemoryStateRepository()
        val events = InMemoryReviewEventRepository()
        val item = LearningItem(LearningItemId("override-item"), ContentId("override-content"), LearningMode.MEANING_RECALL)
        items.save(item)
        val sessionId = SessionId("override-session")
        sessions.save(practiceSession(sessionId).presentItem(item.id, Moment(2)))
        queues.create(sessionId, Moment(1), listOf(item.id), practiceSeed = 11)
        val review = ReviewLearningItemUseCase(memories, events, SimpleScheduler())
        val useCase = ManualRatingOverrideUseCase(
            sessions, items, ContentLearningStateQueryService(items, events), review, direct
        )

        val overridden = useCase.execute(
            ManualRatingOverrideCommand(
                sessionId, ReviewEventId("override-event"), item.id, null,
                ReviewRating.GOOD, Moment(10)
            )
        )

        assertEquals(RatingSource.MANUAL_USER_OVERRIDE, overridden.reviewResult.reviewEvent.source)
        assertEquals(SessionEvaluationPolicy.PRACTICE_ONLY, overridden.session.policy.evaluationPolicy)
        assertEquals(listOf(item.id), queues.require(sessionId).fixedPracticeMembership)
        assertEquals(0, overridden.session.totalReviews)

        UndoLatestSessionReviewUseCase(sessions, queues, memories, events, direct).execute(sessionId)
        assertTrue(events.findAll(overridden.session.learnerId).isEmpty())
        assertEquals(null, memories.find(overridden.session.learnerId, item.id))
        assertEquals(0, queues.require(sessionId).currentIndex)
    }

    private fun practiceSession(id: SessionId) = StudySession.start(
        id,
        LearnerId("learner"),
        Moment(1),
        SessionPolicy(
            evaluationPolicy = SessionEvaluationPolicy.PRACTICE_ONLY,
            practiceLoopPolicy = PracticeLoopPolicy.LOOP_FIXED_MEMBERSHIP_SHUFFLED
        )
    )
}
