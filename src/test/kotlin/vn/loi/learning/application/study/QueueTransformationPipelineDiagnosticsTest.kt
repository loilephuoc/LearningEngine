package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

class QueueTransformationPipelineDiagnosticsTest {

    private val pipeline =
        QueueTransformationPipeline()

    @Test
    fun `returns snapshots for every transformation stage`() {
        val first =
            candidate(
                suffix = "first",
                contentSuffix = "a",
                difficulty = 8.0
            )

        val second =
            candidate(
                suffix = "second",
                contentSuffix = "a",
                difficulty = 9.0
            )

        val third =
            candidate(
                suffix = "third",
                contentSuffix = "b",
                difficulty = 2.0
            )

        val input =
            listOf(
                first,
                second,
                third
            )

        val report =
            pipeline.transformWithReport(
                candidates =
                    input,
                strategy =
                    StudyQueueStrategy {
                        listOf(
                            second,
                            first,
                            third
                        )
                    },
                queueDiversifier =
                    ContentDiversityQueueDiversifier(),
                queueBalancer =
                    DifficultyQueueBalancer()
            )

        assertEquals(
            expected =
                input,
            actual =
                report.inputCandidates
        )

        assertEquals(
            expected =
                listOf(
                    second,
                    first,
                    third
                ),
            actual =
                report.strategyCandidates
        )

        assertEquals(
            expected =
                listOf(
                    second,
                    third,
                    first
                ),
            actual =
                report
                    .initiallyDiversifiedCandidates
        )

        assertEquals(
            expected =
                listOf(
                    second,
                    third,
                    first
                ),
            actual =
                report.balancedCandidates
        )

        assertEquals(
            expected =
                listOf(
                    second,
                    third,
                    first
                ),
            actual =
                report.finalCandidates
        )

        assertTrue(
            report.strategyChangedOrdering
        )

        assertTrue(
            report.initialDiversityChangedOrdering
        )

        assertFalse(
            report.balancerChangedOrdering
        )

        assertFalse(
            report.finalDiversityChangedOrdering
        )

        assertTrue(
            report.pipelineChangedOrdering
        )
    }

    @Test
    fun `legacy transform returns same final candidates as report api`() {
        val first =
            candidate(
                suffix = "first",
                contentSuffix = "a",
                difficulty = 8.0
            )

        val second =
            candidate(
                suffix = "second",
                contentSuffix = "a",
                difficulty = 2.0
            )

        val third =
            candidate(
                suffix = "third",
                contentSuffix = "b",
                difficulty = 5.0
            )

        val candidates =
            listOf(
                first,
                second,
                third
            )

        val strategy =
            ReviewFirstStudyQueueStrategy()

        val diversifier =
            ContentDiversityQueueDiversifier()

        val balancer =
            DifficultyQueueBalancer()

        val legacyResult =
            pipeline.transform(
                candidates =
                    candidates,
                strategy =
                    strategy,
                queueDiversifier =
                    diversifier,
                queueBalancer =
                    balancer
            )

        val diagnosticResult =
            pipeline.transformWithReport(
                candidates =
                    candidates,
                strategy =
                    strategy,
                queueDiversifier =
                    diversifier,
                queueBalancer =
                    balancer
            )

        assertEquals(
            expected =
                diagnosticResult.finalCandidates,
            actual =
                legacyResult
        )
    }

    @Test
    fun `no op pipeline produces unchanged diagnostic snapshots`() {
        val first =
            candidate(
                suffix = "first",
                contentSuffix = "a",
                difficulty = 5.0
            )

        val second =
            candidate(
                suffix = "second",
                contentSuffix = "b",
                difficulty = 5.0
            )

        val candidates =
            listOf(
                first,
                second
            )

        val report =
            pipeline.transformWithReport(
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
                report.strategyCandidates
        )

        assertEquals(
            expected =
                candidates,
            actual =
                report
                    .initiallyDiversifiedCandidates
        )

        assertEquals(
            expected =
                candidates,
            actual =
                report.balancedCandidates
        )

        assertEquals(
            expected =
                candidates,
            actual =
                report.finalCandidates
        )

        assertFalse(
            report.pipelineChangedOrdering
        )
    }

    private fun candidate(
        suffix: String,
        contentSuffix: String,
        difficulty: Double
    ): SelectionCandidate {
        val learningItemId =
            LearningItemId(
                "diagnostic-item-$suffix"
            )

        return SelectionCandidate(
            learningItem =
                LearningItem(
                    id =
                        learningItemId,
                    contentId =
                        ContentId(
                            "diagnostic-content-" +
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
                            "diagnostic-learner"
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