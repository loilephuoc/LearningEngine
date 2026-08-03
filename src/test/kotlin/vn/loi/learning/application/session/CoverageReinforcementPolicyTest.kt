package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId

class CoverageReinforcementPolicyTest {
    @Test
    fun `Again spacing grows from early to progressively wider reinforcement`() {
        val item = LearningItemId("target")
        var queue = queue(100, item)

        queue = queue.advanceCoverageReview(ReviewRating.AGAIN, false)
        assertState(queue, item, 1, 2)
        queue = queue.copy(currentIndex = queue.learningItemIds.indexOf(item, 1))

        queue = queue.advanceCoverageReview(ReviewRating.AGAIN, false)
        assertState(queue, item, 2, 8)
        queue = queue.copy(currentIndex = queue.learningItemIds.indexOf(item, queue.currentIndex))

        queue = queue.advanceCoverageReview(ReviewRating.AGAIN, false)
        assertState(queue, item, 3, 20)
        queue = queue.copy(currentIndex = queue.learningItemIds.indexOf(item, queue.currentIndex))

        queue = queue.advanceCoverageReview(ReviewRating.AGAIN, false)
        assertState(queue, item, 4, 40)
        queue = queue.copy(currentIndex = queue.learningItemIds.indexOf(item, queue.currentIndex))
        val sizeAtLimit = queue.learningItemIds.size

        queue = queue.advanceCoverageReview(ReviewRating.AGAIN, false)
        assertEquals(sizeAtLimit, queue.learningItemIds.size)
        assertState(queue, item, 4, 40)
    }

    @Test
    fun `Hard spacing grows and stops after its maximum reinforcement count`() {
        val item = LearningItemId("target")
        var queue = queue(100, item)
        listOf(4, 12, 30).forEachIndexed { index, expectedGap ->
            queue = queue.advanceCoverageReview(ReviewRating.HARD, false)
            assertState(queue, item, index + 1, expectedGap)
            queue = queue.copy(currentIndex = queue.learningItemIds.indexOf(item, queue.currentIndex))
        }
        val sizeAtLimit = queue.learningItemIds.size
        queue = queue.advanceCoverageReview(ReviewRating.HARD, false)
        assertEquals(sizeAtLimit, queue.learningItemIds.size)
        assertState(queue, item, 3, 30)
    }

    @Test
    fun `Good and Easy do not schedule reinforcement`() {
        listOf(ReviewRating.GOOD, ReviewRating.EASY).forEach { rating ->
            val item = LearningItemId("target")
            val advanced = queue(4, item).advanceCoverageReview(rating, false)
            assertEquals(4, advanced.learningItemIds.size)
            assertFalse(item in advanced.coverageReinforcementStates)
        }
    }

    @Test
    fun `queue delegates spacing to injected policy`() {
        val item = LearningItemId("target")
        val custom = CoverageReinforcementPolicy {
            CoverageReinforcementDecision.Schedule(6, it.currentIndex + 6)
        }
        val advanced = queue(10, item).advanceCoverageReview(ReviewRating.AGAIN, false, custom)
        assertEquals(item, advanced.learningItemIds[6])
        assertEquals(6, advanced.coverageReinforcementStates.getValue(item).previousGap)
    }

    @Test
    fun `near-end reinforcement is deferred instead of compressed`() {
        val item = LearningItemId("target")
        val advanced = queue(1, item).advanceCoverageReview(ReviewRating.AGAIN, false)
        val state = advanced.coverageReinforcementStates.getValue(item)
        assertEquals(listOf(item), advanced.learningItemIds)
        assertEquals(0, state.reinforcementCount)
        assertTrue(state.deferred)
        assertEquals(null, state.previousGap)
    }

    @Test
    fun `Undo restores scheduled and deferred reinforcement state`() {
        val item = LearningItemId("target")
        val scheduled = queue(4, item).advanceCoverageReview(ReviewRating.AGAIN, false)
        assertEquals(queue(4, item), scheduled.rewindCoverageReview(item))

        val deferred = queue(1, item).advanceCoverageReview(ReviewRating.AGAIN, false)
        assertEquals(queue(1, item), deferred.rewindCoverageReview(item))
    }

    @Test
    fun `Undo restores pending tail discarded by coverage completion`() {
        val first = LearningItemId("first")
        val second = LearningItemId("second")
        val original = StudyQueueSnapshot.create(
            SessionId("coverage-session"), Moment(1_000L), listOf(first, second)
        ).advanceCoverageReview(ReviewRating.AGAIN, false)
        val atSecond = original.copy(currentIndex = 1)

        val completed = atSecond.advanceCoverageReview(ReviewRating.GOOD, true)
        assertTrue(completed.isCompleted)
        val restored = completed.rewindCoverageReview(second)
        assertEquals(atSecond.learningItemIds, restored.learningItemIds)
        assertEquals(atSecond.currentIndex, restored.currentIndex)
        assertEquals(atSecond.coverageReinforcementStates, restored.coverageReinforcementStates)
    }

    @Test
    fun `policy decisions are deterministic and wall-clock independent`() {
        val request = CoverageReinforcementRequest(
            ReviewRating.AGAIN,
            CoverageReinforcementState(reinforcementCount = 2, previousGap = 8),
            currentIndex = 5,
            queueSize = 50
        )
        val first = CoverageReinforcementPolicy.DEFAULT.decide(request)
        val second = CoverageReinforcementPolicy.DEFAULT.decide(request)
        assertEquals(first, second)
        assertEquals(20, assertIs<CoverageReinforcementDecision.Schedule>(first).gap)
    }

    private fun queue(size: Int, target: LearningItemId) = StudyQueueSnapshot.create(
        SessionId("coverage-session"),
        Moment(1_000L),
        listOf(target) + (1 until size).map { LearningItemId("filler-$it") }
    )

    private fun assertState(
        queue: StudyQueueSnapshot,
        item: LearningItemId,
        count: Int,
        gap: Int
    ) {
        val state = queue.coverageReinforcementStates.getValue(item)
        assertEquals(count, state.reinforcementCount)
        assertEquals(gap, state.previousGap)
        assertFalse(state.deferred)
    }
}

private fun List<LearningItemId>.indexOf(item: LearningItemId, startIndex: Int): Int {
    for (index in startIndex until size) if (this[index] == item) return index
    error("Missing reinforcement occurrence for $item")
}
