package vn.loi.learning.application.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.LearningStage

class LearningStageCountsTest {

    @Test
    fun `counts expose complete learning stage distribution`() {
        val counts =
            LearningStageCounts(
                newCount = 1,
                learningCount = 2,
                reviewCount = 3,
                relearningCount = 4,
                masteredCount = 5,
                suspendedCount = 6
            )

        assertEquals(
            expected = 1,
            actual = counts[LearningStage.NEW]
        )

        assertEquals(
            expected = 2,
            actual = counts[LearningStage.LEARNING]
        )

        assertEquals(
            expected = 3,
            actual = counts[LearningStage.REVIEW]
        )

        assertEquals(
            expected = 4,
            actual = counts[LearningStage.RELEARNING]
        )

        assertEquals(
            expected = 5,
            actual = counts[LearningStage.MASTERED]
        )

        assertEquals(
            expected = 6,
            actual = counts[LearningStage.SUSPENDED]
        )
    }

    @Test
    fun `total memories includes every learning stage`() {
        val counts =
            LearningStageCounts(
                newCount = 1,
                learningCount = 2,
                reviewCount = 3,
                relearningCount = 4,
                masteredCount = 5,
                suspendedCount = 6
            )

        assertEquals(
            expected = 21,
            actual = counts.totalMemories
        )
    }

    @Test
    fun `active memories exclude suspended memories`() {
        val counts =
            LearningStageCounts(
                newCount = 1,
                learningCount = 2,
                reviewCount = 3,
                relearningCount = 4,
                masteredCount = 5,
                suspendedCount = 6
            )

        assertEquals(
            expected = 15,
            actual = counts.activeMemories
        )
    }

    @Test
    fun `counts report whether memories exist`() {
        assertTrue(
            actual =
                LearningStageCounts(
                    newCount = 1,
                    learningCount = 0,
                    reviewCount = 0,
                    relearningCount = 0,
                    masteredCount = 0,
                    suspendedCount = 0
                ).hasMemories
        )

        assertFalse(
            actual =
                LearningStageCounts.EMPTY.hasMemories
        )
    }

    @Test
    fun `empty counts contain no memories`() {
        val counts =
            LearningStageCounts.EMPTY

        assertEquals(
            expected = 0,
            actual = counts.totalMemories
        )

        assertEquals(
            expected = 0,
            actual = counts.activeMemories
        )

        LearningStage.entries.forEach { stage ->
            assertEquals(
                expected = 0,
                actual = counts[stage]
            )
        }
    }

    @Test
    fun `counts reject negative new count`() {
        assertRejected(
            expectedMessage =
                "New count must not be negative."
        ) {
            validCounts(
                newCount = -1
            )
        }
    }

    @Test
    fun `counts reject negative learning count`() {
        assertRejected(
            expectedMessage =
                "Learning count must not be negative."
        ) {
            validCounts(
                learningCount = -1
            )
        }
    }

    @Test
    fun `counts reject negative review count`() {
        assertRejected(
            expectedMessage =
                "Review count must not be negative."
        ) {
            validCounts(
                reviewCount = -1
            )
        }
    }

    @Test
    fun `counts reject negative relearning count`() {
        assertRejected(
            expectedMessage =
                "Relearning count must not be negative."
        ) {
            validCounts(
                relearningCount = -1
            )
        }
    }

    @Test
    fun `counts reject negative mastered count`() {
        assertRejected(
            expectedMessage =
                "Mastered count must not be negative."
        ) {
            validCounts(
                masteredCount = -1
            )
        }
    }

    @Test
    fun `counts reject negative suspended count`() {
        assertRejected(
            expectedMessage =
                "Suspended count must not be negative."
        ) {
            validCounts(
                suspendedCount = -1
            )
        }
    }

    private fun validCounts(
        newCount: Int = 0,
        learningCount: Int = 0,
        reviewCount: Int = 0,
        relearningCount: Int = 0,
        masteredCount: Int = 0,
        suspendedCount: Int = 0
    ): LearningStageCounts =
        LearningStageCounts(
            newCount = newCount,
            learningCount = learningCount,
            reviewCount = reviewCount,
            relearningCount = relearningCount,
            masteredCount = masteredCount,
            suspendedCount = suspendedCount
        )

    private fun assertRejected(
        expectedMessage: String,
        block: () -> Unit
    ) {
        val exception =
            assertFailsWith<IllegalArgumentException>(
                block = block
            )

        assertEquals(
            expected = expectedMessage,
            actual = exception.message
        )
    }
}