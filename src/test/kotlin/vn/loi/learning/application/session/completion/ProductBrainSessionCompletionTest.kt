package vn.loi.learning.application.session.completion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import vn.loi.learning.application.scene.EvidencePerformance
import vn.loi.learning.domain.study.memory.model.ReviewRating

class ProductBrainSessionCompletionTest {
    private val completion = ProductBrainSessionCompletion()

    @Test
    fun `completion orchestrates outcome and scheduler projection without technical leakage`() {
        val plan = completion.prepare(completionInput())
        val scheduling =
            completion.schedulingOutcome(
                rating = plan.recommendedRating,
                scheduledIntervalMillis = 86_400_000L,
                nextReviewAtEpochMillis = 86_401_000L
            )
        val result = completion.complete(plan, scheduling, "session", 2_000L)

        assertEquals(ReviewRating.EASY, plan.recommendedRating)
        assertFalse(plan.learningOutcome.needsReinforcement)
        assertEquals("session", result.sessionId)
        assertEquals(86_400_000L, result.schedulingOutcome.scheduledIntervalMillis)
        assertFalse(result.summary.overallOutcome.contains("INTERNAL_RULE"))
    }

    @Test
    fun `completion rejects inconsistent source data before persistence`() {
        val input = completionInput().copy(finalDifficultyLevel = 99)
        assertFailsWith<IllegalArgumentException> {
            completion.prepare(input)
        }
    }

    @Test
    fun `completion maps partial and incorrect evidence to existing scheduler ratings`() {
        assertEquals(
            ReviewRating.HARD,
            completion.prepare(completionInput(EvidencePerformance.PARTIAL)).recommendedRating
        )
        assertEquals(
            ReviewRating.AGAIN,
            completion.prepare(completionInput(EvidencePerformance.INCORRECT)).recommendedRating
        )
    }
}
