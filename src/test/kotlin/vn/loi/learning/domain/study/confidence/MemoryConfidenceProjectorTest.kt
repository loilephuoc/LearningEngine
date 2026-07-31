package vn.loi.learning.domain.study.confidence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceEvidence
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceReason
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceScore
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceSpacingBand
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceTier
import vn.loi.learning.domain.study.confidence.policy.MemoryConfidenceProjector
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating

class MemoryConfidenceProjectorTest {
    @Test
    fun `score invariant and tier boundaries are exact`() {
        assertEquals(0, MemoryConfidenceScore.of(0).value)
        assertEquals(100, MemoryConfidenceScore.of(100).value)
        assertFailsWith<IllegalArgumentException> { MemoryConfidenceScore.of(-1) }
        assertFailsWith<IllegalArgumentException> { MemoryConfidenceScore.of(101) }
        mapOf(
            19 to MemoryConfidenceTier.VERY_LOW,
            20 to MemoryConfidenceTier.LOW,
            39 to MemoryConfidenceTier.LOW,
            40 to MemoryConfidenceTier.MEDIUM,
            59 to MemoryConfidenceTier.MEDIUM,
            60 to MemoryConfidenceTier.HIGH,
            79 to MemoryConfidenceTier.HIGH,
            80 to MemoryConfidenceTier.VERY_HIGH,
            100 to MemoryConfidenceTier.VERY_HIGH
        ).forEach { (score, tier) ->
            assertEquals(tier, MemoryConfidenceTier.from(MemoryConfidenceScore.of(score)))
        }
    }

    @Test
    fun `empty and single evidence remain conservative`() {
        val empty = MemoryConfidenceProjector.project(emptyList()).projectedConfidence
        val good = MemoryConfidenceProjector.project(history(ReviewRating.GOOD at 0L)).projectedConfidence
        val easy = MemoryConfidenceProjector.project(history(ReviewRating.EASY at 0L)).projectedConfidence

        assertEquals(0, empty.score.value)
        assertEquals(MemoryConfidenceTier.VERY_LOW, empty.tier)
        assertEquals(8, good.score.value)
        assertEquals(12, easy.score.value)
        assertEquals(MemoryConfidenceTier.VERY_LOW, easy.tier)
    }

    @Test
    fun `calibration patterns reward independent spacing without overvaluing immediate repeats`() {
        val immediateAfterFailure =
            score(ReviewRating.AGAIN at 0L, ReviewRating.GOOD at minutes(2))
        val spacedAfterFailure =
            score(ReviewRating.AGAIN at 0L, ReviewRating.GOOD at days(1))
        val independent =
            score(
                ReviewRating.GOOD at 0L,
                ReviewRating.GOOD at days(3),
                ReviewRating.EASY at days(17)
            )
        val immediate =
            score(
                ReviewRating.GOOD at 0L,
                ReviewRating.GOOD at minutes(1),
                ReviewRating.EASY at minutes(2)
            )
        val long =
            score(
                ReviewRating.EASY at 0L,
                ReviewRating.EASY at days(30),
                ReviewRating.EASY at days(90)
            )

        assertEquals(8, immediateAfterFailure)
        assertEquals(10, spacedAfterFailure)
        assertEquals(49, independent)
        assertEquals(28, immediate)
        assertEquals(68, long)
        assertTrue(spacedAfterFailure > immediateAfterFailure)
        assertTrue(independent > immediate)
    }

    @Test
    fun `failure lowers accumulated confidence without arbitrary reset and reaches floor repeatedly`() {
        val strongHistory =
            history(
                ReviewRating.EASY at 0L,
                ReviewRating.EASY at days(30),
                ReviewRating.EASY at days(90)
            )
        val strong = MemoryConfidenceProjector.project(strongHistory).projectedConfidence
        val failed =
            MemoryConfidenceProjector.project(
                append(strongHistory, ReviewRating.AGAIN at days(91))
            ).projectedConfidence
        val floor =
            MemoryConfidenceProjector.project(
                append(
                    strongHistory,
                    ReviewRating.AGAIN at days(91),
                    ReviewRating.AGAIN at days(92),
                    ReviewRating.AGAIN at days(93),
                    ReviewRating.AGAIN at days(94),
                    ReviewRating.AGAIN at days(95)
                )
            ).projectedConfidence

        assertEquals(68, strong.score.value)
        assertEquals(54, failed.score.value)
        assertEquals(0, floor.score.value)
        assertEquals(MemoryConfidenceReason.RECALL_FAILURE, failed.primaryReason)
    }

    @Test
    fun `canonical ordering is deterministic and equal timestamps use event id`() {
        val canonical =
            history(
                ReviewRating.GOOD at 0L,
                ReviewRating.EASY at days(1),
                ReviewRating.GOOD at days(3)
            )
        val expected = MemoryConfidenceProjector.project(canonical)

        assertEquals(expected, MemoryConfidenceProjector.project(canonical.reversed()))
        assertEquals(expected, MemoryConfidenceProjector.project(listOf(canonical[1], canonical[2], canonical[0])))
    }

    @Test
    fun `mixed identity is rejected and broken chain is marked unreliable`() {
        val valid = history(ReviewRating.GOOD at 0L, ReviewRating.GOOD at days(1))
        val mixedLearner =
            valid + valid.last().copy(
                id = ReviewEventId("other-learner"),
                stateBefore = valid.last().stateBefore.copy(learnerId = LearnerId("other")),
                stateAfter = valid.last().stateAfter.copy(learnerId = LearnerId("other"))
            )
        val broken =
            listOf(
                valid[0],
                valid[1].copy(
                    stateBefore = valid[1].stateBefore.copy(difficulty = 6.0)
                )
            )

        assertFailsWith<IllegalArgumentException> {
            MemoryConfidenceProjector.project(mixedLearner)
        }
        val unreliable = MemoryConfidenceProjector.project(broken).projectedConfidence
        assertFalse(unreliable.reliable)
        assertEquals(MemoryConfidenceReason.UNRELIABLE_HISTORY, unreliable.primaryReason)
    }

    @Test
    fun `pending evidence is pure caller-timed and reports projection delta`() {
        val durable = history(ReviewRating.GOOD at 0L)
        val before = durable.toList()
        val projection =
            MemoryConfidenceProjector.project(
                durable,
                MemoryConfidenceEvidence(ReviewRating.GOOD, Moment(days(3)))
            )

        assertEquals(before, durable)
        assertEquals(8, projection.previousConfidence.score.value)
        assertEquals(23, projection.projectedConfidence.score.value)
        assertEquals(15, projection.delta)
        assertEquals(MemoryConfidenceSpacingBand.MEDIUM, projection.appliedSpacingBand)
        assertTrue(projection.pendingEvidenceApplied)
        assertEquals(projection, MemoryConfidenceProjector.project(durable,
            MemoryConfidenceEvidence(ReviewRating.GOOD, Moment(days(3)))))
    }

    private fun score(vararg ratings: TimedRating): Int =
        MemoryConfidenceProjector.project(history(*ratings)).projectedConfidence.score.value

    private fun history(vararg ratings: TimedRating): List<ReviewEvent> =
        append(emptyList(), *ratings)

    private fun append(
        existing: List<ReviewEvent>,
        vararg ratings: TimedRating
    ): List<ReviewEvent> {
        val events = existing.toMutableList()
        var state =
            events.lastOrNull()?.stateAfter
                ?: MemoryState.new(LEARNER, ITEM, Moment(ratings.firstOrNull()?.at ?: 0L))
        ratings.forEachIndexed { index, timed ->
            val at = Moment(timed.at)
            val nextStage =
                if (timed.rating == ReviewRating.AGAIN) LearningStage.RELEARNING
                else LearningStage.REVIEW
            val after =
                state.copy(
                    stage = nextStage,
                    dueAt = at,
                    lastReviewedAt = at,
                    reviewCount = state.reviewCount + 1,
                    lapseCount =
                        state.lapseCount + if (timed.rating == ReviewRating.AGAIN) 1 else 0
                )
            events +=
                ReviewEvent(
                    id = ReviewEventId("event-${events.size + index}-$at"),
                    rating = timed.rating,
                    reviewedAt = at,
                    responseTime = null,
                    stateBefore = state,
                    stateAfter = after
                )
            state = after
        }
        return events
    }

    private infix fun ReviewRating.at(epochMillis: Long) = TimedRating(this, epochMillis)
    private fun minutes(value: Long) = value * 60_000L
    private fun days(value: Long) = value * 24L * 60L * 60L * 1_000L

    private data class TimedRating(val rating: ReviewRating, val at: Long)

    companion object {
        private val LEARNER = LearnerId("learner")
        private val ITEM = LearningItemId("item")
    }
}
