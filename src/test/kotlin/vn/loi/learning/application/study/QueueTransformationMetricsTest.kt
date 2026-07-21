package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueueTransformationMetricsTest {

    @Test
    fun `exposes stage change flags and total movement`() {
        val metrics =
            QueueTransformationMetrics(
                candidateCount =
                    5,
                strategyMovedCandidateCount =
                    4,
                initialDiversityMovedCandidateCount =
                    2,
                balancerMovedCandidateCount =
                    0,
                finalDiversityMovedCandidateCount =
                    2,
                pipelineMovedCandidateCount =
                    3
            )

        assertTrue(
            metrics.strategyChangedOrdering
        )

        assertTrue(
            metrics.initialDiversityChangedOrdering
        )

        assertFalse(
            metrics.balancerChangedOrdering
        )

        assertTrue(
            metrics.finalDiversityChangedOrdering
        )

        assertTrue(
            metrics.pipelineChangedOrdering
        )

        assertEquals(
            expected =
                8,
            actual =
                metrics.totalStageMovementCount
        )
    }

    @Test
    fun `zero movement reports unchanged ordering`() {
        val metrics =
            QueueTransformationMetrics(
                candidateCount =
                    3,
                strategyMovedCandidateCount =
                    0,
                initialDiversityMovedCandidateCount =
                    0,
                balancerMovedCandidateCount =
                    0,
                finalDiversityMovedCandidateCount =
                    0,
                pipelineMovedCandidateCount =
                    0
            )

        assertFalse(
            metrics.strategyChangedOrdering
        )

        assertFalse(
            metrics.initialDiversityChangedOrdering
        )

        assertFalse(
            metrics.balancerChangedOrdering
        )

        assertFalse(
            metrics.finalDiversityChangedOrdering
        )

        assertFalse(
            metrics.pipelineChangedOrdering
        )

        assertEquals(
            expected =
                0,
            actual =
                metrics.totalStageMovementCount
        )
    }
}