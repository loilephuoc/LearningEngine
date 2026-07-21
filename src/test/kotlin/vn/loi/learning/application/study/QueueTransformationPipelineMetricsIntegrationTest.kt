package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.selection.model.SelectionCandidate

class QueueTransformationPipelineMetricsIntegrationTest {

    private val pipeline =
        QueueTransformationPipeline()

    @Test
    fun `returns final candidates and matching pipeline metrics`() {
        val firstA =
            candidate(
                suffix = "a-1",
                contentSuffix = "a",
                difficulty = 8.0
            )

        val secondA =
            candidate(
                suffix = "a-2",
                contentSuffix = "a",
                difficulty = 9.0
            )

        val itemB =
            candidate(
                suffix = "b-1",
                contentSuffix = "b",
                difficulty = 2.0
            )

        val diagnostics =
            pipeline.transformWithDiagnostics(
                candidates =
                    listOf(
                        firstA,
                        secondA,
                        itemB
                    ),
                strategy =
                    StudyQueueStrategy {
                        listOf(
                            secondA,
                            firstA,
                            itemB
                        )
                    },
                queueDiversifier =
                    ContentDiversityQueueDiversifier(),
                queueBalancer =
                    DifficultyQueueBalancer()
            )

        assertEquals(
            expected =
                diagnostics.report.finalCandidates,
            actual =
                diagnostics.finalCandidates
        )

        assertEquals(
            expected =
                3,
            actual =
                diagnostics.metrics.candidateCount
        )

        assertEquals(
            expected =
                2,
            actual =
                diagnostics.metrics
                    .strategyMovedCandidateCount
        )

        assertEquals(
            expected =
                2,
            actual =
                diagnostics.metrics
                    .initialDiversityMovedCandidateCount
        )

        assertTrue(
            diagnostics.metrics
                .pipelineChangedOrdering
        )

        assertEquals(
            expected =
                listOf(
                    secondA,
                    itemB,
                    firstA
                ),
            actual =
                diagnostics.finalCandidates
        )
    }

    @Test
    fun `no op transformations return zero movement metrics`() {
        val candidates =
            listOf(
                candidate(
                    suffix = "first",
                    contentSuffix = "first",
                    difficulty = 5.0
                ),
                candidate(
                    suffix = "second",
                    contentSuffix = "second",
                    difficulty = 5.0
                )
            )

        val diagnostics =
            pipeline.transformWithDiagnostics(
                candidates =
                    candidates,
                strategy =
                    StudyQueueStrategy {
                        it
                    },
                queueDiversifier =
                    NoOpQueueDiversifier(),
                queueBalancer =
                    NoOpQueueBalancer()
            )

        assertEquals(
            expected =
                candidates,
            actual =
                diagnostics.finalCandidates
        )

        assertEquals(
            expected =
                0,
            actual =
                diagnostics.metrics
                    .totalStageMovementCount
        )

        assertEquals(
            expected =
                0,
            actual =
                diagnostics.metrics
                    .pipelineMovedCandidateCount
        )
    }

    private fun candidate(
        suffix: String,
        contentSuffix: String,
        difficulty: Double
    ): SelectionCandidate {
        val learningItemId =
            LearningItemId(
                "pipeline-metrics-item-$suffix"
            )

        return SelectionCandidate(
            learningItem =
                LearningItem(
                    id =
                        learningItemId,
                    contentId =
                        ContentId(
                            "pipeline-metrics-content-" +
                                    contentSuffix
                        ),
                    mode =
                        LearningMode
                            .MEANING_RECOGNITION
                ),
            memoryState =
                MemoryState(
                    learnerId =
                        LearnerId(
                            "pipeline-metrics-learner"
                        ),
                    learningItemId =
                        learningItemId,
                    stage =
                        LearningStage.REVIEW,
                    difficulty =
                        difficulty,
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