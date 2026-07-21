package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.selection.model.SelectionCandidate

class QueueTransformationMetricsCalculatorTest {

    private val calculator =
        QueueTransformationMetricsCalculator()

    @Test
    fun `calculates moved candidate count for every stage`() {
        val first =
            candidate(
                suffix = "first"
            )

        val second =
            candidate(
                suffix = "second"
            )

        val third =
            candidate(
                suffix = "third"
            )

        val fourth =
            candidate(
                suffix = "fourth"
            )

        val report =
            QueueTransformationReport.create(
                inputCandidates =
                    listOf(
                        first,
                        second,
                        third,
                        fourth
                    ),
                strategyCandidates =
                    listOf(
                        second,
                        first,
                        third,
                        fourth
                    ),
                initiallyDiversifiedCandidates =
                    listOf(
                        second,
                        third,
                        first,
                        fourth
                    ),
                balancedCandidates =
                    listOf(
                        second,
                        third,
                        first,
                        fourth
                    ),
                finalCandidates =
                    listOf(
                        second,
                        third,
                        fourth,
                        first
                    )
            )

        val metrics =
            calculator.calculate(
                report
            )

        assertEquals(
            expected =
                4,
            actual =
                metrics.candidateCount
        )

        assertEquals(
            expected =
                2,
            actual =
                metrics
                    .strategyMovedCandidateCount
        )

        assertEquals(
            expected =
                2,
            actual =
                metrics
                    .initialDiversityMovedCandidateCount
        )

        assertEquals(
            expected =
                0,
            actual =
                metrics
                    .balancerMovedCandidateCount
        )

        assertEquals(
            expected =
                2,
            actual =
                metrics
                    .finalDiversityMovedCandidateCount
        )

        assertEquals(
            expected =
                4,
            actual =
                metrics
                    .pipelineMovedCandidateCount
        )

        assertEquals(
            expected =
                6,
            actual =
                metrics.totalStageMovementCount
        )
    }

    @Test
    fun `calculates zero metrics for unchanged report`() {
        val candidates =
            listOf(
                candidate(
                    suffix = "first"
                ),
                candidate(
                    suffix = "second"
                )
            )

        val report =
            QueueTransformationReport.create(
                inputCandidates =
                    candidates,
                strategyCandidates =
                    candidates,
                initiallyDiversifiedCandidates =
                    candidates,
                balancedCandidates =
                    candidates,
                finalCandidates =
                    candidates
            )

        val metrics =
            calculator.calculate(
                report
            )

        assertEquals(
            expected =
                0,
            actual =
                metrics
                    .strategyMovedCandidateCount
        )

        assertEquals(
            expected =
                0,
            actual =
                metrics
                    .initialDiversityMovedCandidateCount
        )

        assertEquals(
            expected =
                0,
            actual =
                metrics
                    .balancerMovedCandidateCount
        )

        assertEquals(
            expected =
                0,
            actual =
                metrics
                    .finalDiversityMovedCandidateCount
        )

        assertEquals(
            expected =
                0,
            actual =
                metrics
                    .pipelineMovedCandidateCount
        )
    }

    private fun candidate(
        suffix: String
    ): SelectionCandidate {
        val learningItemId =
            LearningItemId(
                "metrics-item-$suffix"
            )

        return SelectionCandidate(
            learningItem =
                LearningItem(
                    id =
                        learningItemId,
                    contentId =
                        ContentId(
                            "metrics-content-$suffix"
                        ),
                    mode =
                        LearningMode
                            .MEANING_RECOGNITION
                ),
            memoryState =
                MemoryState(
                    learnerId =
                        LearnerId(
                            "metrics-learner"
                        ),
                    learningItemId =
                        learningItemId,
                    stage =
                        LearningStage.REVIEW,
                    difficulty =
                        5.0,
                    stabilityDays =
                        10.0,
                    dueAt =
                        Moment(1_000L),
                    lastReviewedAt =
                        Moment(500L),
                    reviewCount =
                        1,
                    lapseCount =
                        0
                )
        )
    }
}