package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.FocusedPracticeKind
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.persistence.mapper.StudyQueueRecordMapper

class QuickReviewQueueCapabilityTest {
    @Test
    fun `only explicit evaluative quick review combination is legal`() {
        val policy = SessionPolicy(
            newItemLimit = 0,
            reviewItemLimit = 3,
            evaluationPolicy = SessionEvaluationPolicy.EVALUATIVE,
            practiceLoopPolicy = PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW,
            focusedPracticeKind = FocusedPracticeKind.QUICK_REVIEW
        )
        assertEquals(SessionEvaluationPolicy.EVALUATIVE, policy.evaluationPolicy)
        assertFailsWith<IllegalArgumentException> {
            policy.copy(evaluationPolicy = SessionEvaluationPolicy.PRACTICE_ONLY)
        }
        assertFailsWith<IllegalArgumentException> {
            SessionPolicy(practiceLoopPolicy = PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED)
        }
    }

    @Test
    fun `unrated advance loops without reinforcement and reshuffles`() {
        val ids = listOf("a", "b", "c").map(::LearningItemId)
        var queue = quickQueue(ids)
        val first = queue.learningItemIds
        repeat(ids.size) { queue = queue.advanceQuickReview(null) }
        assertEquals(2, queue.practiceRound)
        assertEquals(0, queue.currentIndex)
        assertTrue(queue.practiceReinforcementStates.isEmpty())
        assertNotEquals(first, queue.learningItemIds)
    }

    @Test
    fun `priority is again then hard unrated good and easy without starving normal items`() {
        val ids = listOf("a", "b", "c", "d", "e", "f").map(::LearningItemId)
        val again = quickQueue(ids).advanceQuickReview(ReviewRating.AGAIN)
        val hard = quickQueue(ids).advanceQuickReview(ReviewRating.HARD)
        assertEquals(ids[0], again.learningItemIds[3])
        assertEquals(ids[0], hard.learningItemIds[5])

        var good = quickQueue(ids).advanceQuickReview(ReviewRating.GOOD)
        repeat(ids.size - 1) { good = good.advanceQuickReview(null) }
        assertTrue(ids[0] !in good.learningItemIds)
        assertTrue(ids.drop(1).all { it in good.learningItemIds })

        var easy = quickQueue(ids).advanceQuickReview(ReviewRating.EASY)
        repeat(ids.size - 1) { easy = easy.advanceQuickReview(null) }
        assertTrue(ids[0] !in easy.learningItemIds)
        assertTrue(easy.learningItemIds.isNotEmpty())
        repeat(easy.learningItemIds.size) { easy = easy.advanceQuickReview(null) }
        assertTrue(ids[0] !in easy.learningItemIds)
    }

    @Test
    fun `small pools loop without immediate repeat when another item exists`() {
        val one = LearningItemId("only")
        assertEquals(one, quickQueue(listOf(one)).advanceQuickReview(ReviewRating.AGAIN).currentLearningItemId)

        listOf(2, 3).forEach { size ->
            val ids = (1..size).map { LearningItemId("item-$it") }
            val queue = quickQueue(ids)
            val advanced = queue.advanceQuickReview(ReviewRating.AGAIN)
            assertTrue(advanced.currentLearningItemId != queue.currentLearningItemId)
        }
    }

    @Test
    fun `quick review reinforcement is not persisted`() {
        val queue = quickQueue(listOf(LearningItemId("a"), LearningItemId("b")))
            .advanceQuickReview(ReviewRating.AGAIN)
        val restored = StudyQueueRecordMapper.toDomain(StudyQueueRecordMapper.toRecord(queue))
        assertTrue(restored.practiceReinforcementStates.isEmpty())
        assertEquals(queue.practiceLoopPolicy, restored.practiceLoopPolicy)
    }

    private fun quickQueue(ids: List<LearningItemId>) = StudyQueueSnapshot.create(
        SessionId("quick"), Moment(1), ids, practiceSeed = 42,
        practiceLoopPolicy = PracticeLoopPolicy.LOOP_EVALUATIVE_QUICK_REVIEW
    )
}
