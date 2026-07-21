package vn.loi.learning.domain.study.scheduling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating

class DiagnosingSchedulerTest {

    private val learnerId =
        LearnerId(
            "diagnosing-scheduler-learner"
        )

    private val learningItemId =
        LearningItemId(
            "diagnosing-scheduler-item"
        )

    @Test
    fun `returns exact delegate decision and publishes diagnostics`() {
        val reviewedAt =
            Moment(
                1_000_000L
            )

        val currentState =
            MemoryState.new(
                learnerId = learnerId,
                learningItemId = learningItemId,
                availableAt = reviewedAt
            )

        val delegate =
            SimpleScheduler()

        var capturedDiagnostics:
                SchedulerDiagnostics? =
            null

        val scheduler =
            DiagnosingScheduler(
                delegate = delegate,
                diagnosticsConsumer = {
                        diagnostics ->
                    capturedDiagnostics =
                        diagnostics
                }
            )

        val decision =
            scheduler.schedule(
                currentState = currentState,
                rating = ReviewRating.GOOD,
                reviewedAt = reviewedAt
            )

        val diagnostics =
            requireNotNull(
                capturedDiagnostics
            )

        assertSame(
            expected = decision,
            actual =
                diagnostics.decision
        )

        assertEquals(
            expected =
                ReviewRating.GOOD,
            actual =
                diagnostics.rating
        )

        assertEquals(
            expected =
                decision.scheduledInterval,
            actual =
                diagnostics.metrics
                    .scheduledInterval
        )

        assertEquals(
            expected = 1,
            actual =
                diagnostics.metrics
                    .reviewCountIncrement
        )

        assertEquals(
            expected = 0,
            actual =
                diagnostics.metrics
                    .lapseCountIncrement
        )
    }

    @Test
    fun `publishes diagnostics for real fsrs scheduler`() {
        val reviewedAt =
            Moment(
                2_000_000L
            )

        val currentState =
            MemoryState.new(
                learnerId = learnerId,
                learningItemId = learningItemId,
                availableAt = reviewedAt
            )

        val capturedDiagnostics =
            mutableListOf<
                    SchedulerDiagnostics
                    >()

        val scheduler =
            DiagnosingScheduler(
                delegate =
                    FsrsScheduler(),
                diagnosticsConsumer =
                    capturedDiagnostics::add
            )

        val decision =
            scheduler.schedule(
                currentState = currentState,
                rating = ReviewRating.EASY,
                reviewedAt = reviewedAt
            )

        assertEquals(
            expected = 1,
            actual =
                capturedDiagnostics.size
        )

        val diagnostics =
            capturedDiagnostics.single()

        assertSame(
            expected = decision,
            actual =
                diagnostics.decision
        )

        assertEquals(
            expected =
                ReviewRating.EASY,
            actual =
                diagnostics.rating
        )

        assertEquals(
            expected =
                decision.previousState.stage,
            actual =
                diagnostics.metrics
                    .previousStage
        )

        assertEquals(
            expected =
                decision.nextState.stage,
            actual =
                diagnostics.metrics
                    .nextStage
        )

        assertTrue(
            diagnostics.metrics
                .stabilityDeltaDays >
                    0.0
        )
    }

    @Test
    fun `publishes one diagnostics result for every scheduling call`() {
        val reviewedAt =
            Moment(
                3_000_000L
            )

        val initialState =
            MemoryState.new(
                learnerId = learnerId,
                learningItemId = learningItemId,
                availableAt = reviewedAt
            )

        val diagnostics =
            mutableListOf<
                    SchedulerDiagnostics
                    >()

        val scheduler =
            DiagnosingScheduler(
                delegate =
                    SimpleScheduler(),
                diagnosticsConsumer =
                    diagnostics::add
            )

        val firstDecision =
            scheduler.schedule(
                currentState = initialState,
                rating = ReviewRating.GOOD,
                reviewedAt = reviewedAt
            )

        scheduler.schedule(
            currentState =
                firstDecision.nextState,
            rating =
                ReviewRating.AGAIN,
            reviewedAt =
                firstDecision.nextState.dueAt
        )

        assertEquals(
            expected = 2,
            actual =
                diagnostics.size
        )

        assertEquals(
            expected =
                ReviewRating.GOOD,
            actual =
                diagnostics[0].rating
        )

        assertEquals(
            expected =
                ReviewRating.AGAIN,
            actual =
                diagnostics[1].rating
        )

        assertTrue(
            diagnostics[1]
                .metrics
                .lapseOccurred
        )
    }
}