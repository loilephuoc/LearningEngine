package vn.loi.learning.domain.study.memory.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId

class MemoryStateTest {

    private val learnerId = LearnerId("loi")
    private val itemId = LearningItemId("sentence-001-listening")
    private val availableAt = Moment(1_000_000L)

    @Test
    fun `new memory state starts with safe defaults`() {
        val state = MemoryState.new(
            learnerId = learnerId,
            learningItemId = itemId,
            availableAt = availableAt
        )

        assertEquals(LearningStage.NEW, state.stage)
        assertEquals(
            Difficulty.DEFAULT,
            state.difficultyValue
        )
        assertEquals(
            Stability.ZERO,
            state.stability
        )
        assertEquals(availableAt, state.dueAt)
        assertNull(state.lastReviewedAt)
        assertEquals(0, state.reviewCount)
        assertEquals(0, state.lapseCount)
        assertTrue(state.isNew)
    }

    @Test
    fun `new item becomes due at its available time`() {
        val state = MemoryState.new(
            learnerId = learnerId,
            learningItemId = itemId,
            availableAt = availableAt
        )

        assertFalse(state.isDue(Moment(999_999L)))
        assertTrue(state.isDue(Moment(1_000_000L)))
        assertTrue(state.isDue(Moment(1_000_001L)))
    }

    @Test
    fun `suspended item is never due`() {
        val state = MemoryState.new(
            learnerId = learnerId,
            learningItemId = itemId,
            availableAt = availableAt
        ).copy(
            stage = LearningStage.SUSPENDED
        )

        assertFalse(state.isDue(Moment(9_999_999L)))
    }

    @Test
    fun `difficulty outside accepted range is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            MemoryState.new(
                learnerId = learnerId,
                learningItemId = itemId,
                availableAt = availableAt
            ).copy(
                difficulty = 11.0
            )
        }
    }

    @Test
    fun `lapse count cannot exceed review count`() {
        assertFailsWith<IllegalArgumentException> {
            MemoryState(
                learnerId = learnerId,
                learningItemId = itemId,
                stage = LearningStage.RELEARNING,
                difficulty = 7.0,
                stabilityDays = 1.0,
                dueAt = availableAt,
                lastReviewedAt = availableAt,
                reviewCount = 1,
                lapseCount = 2
            )
        }
    }

    @Test
    fun `moment supports portable time arithmetic`() {
        val start = Moment(1_000L)
        val end = start + TimeSpan.minutes(5)

        assertEquals(Moment(301_000L), end)
        assertEquals(TimeSpan.minutes(5), end - start)
    }
}
