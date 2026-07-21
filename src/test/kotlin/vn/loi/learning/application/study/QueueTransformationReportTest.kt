package vn.loi.learning.application.study

import kotlin.test.Test
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

class QueueTransformationReportTest {

    @Test
    fun `reports which stages changed ordering`() {
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

        val report =
            QueueTransformationReport.create(
                inputCandidates =
                    listOf(
                        first,
                        second,
                        third
                    ),
                strategyCandidates =
                    listOf(
                        third,
                        second,
                        first
                    ),
                initiallyDiversifiedCandidates =
                    listOf(
                        third,
                        first,
                        second
                    ),
                balancedCandidates =
                    listOf(
                        third,
                        first,
                        second
                    ),
                finalCandidates =
                    listOf(
                        first,
                        third,
                        second
                    )
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

        assertTrue(
            report.finalDiversityChangedOrdering
        )

        assertTrue(
            report.pipelineChangedOrdering
        )
    }

    @Test
    fun `reports unchanged pipeline when every snapshot has same ordering`() {
        val first =
            candidate(
                suffix = "first"
            )

        val second =
            candidate(
                suffix = "second"
            )

        val candidates =
            listOf(
                first,
                second
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

        assertFalse(
            report.strategyChangedOrdering
        )

        assertFalse(
            report.initialDiversityChangedOrdering
        )

        assertFalse(
            report.balancerChangedOrdering
        )

        assertFalse(
            report.finalDiversityChangedOrdering
        )

        assertFalse(
            report.pipelineChangedOrdering
        )
    }

    @Test
    fun `report copies supplied candidate lists`() {
        val first =
            candidate(
                suffix = "first"
            )

        val second =
            candidate(
                suffix = "second"
            )

        val mutableCandidates =
            mutableListOf(
                first,
                second
            )

        val report =
            QueueTransformationReport.create(
                inputCandidates =
                    mutableCandidates,
                strategyCandidates =
                    mutableCandidates,
                initiallyDiversifiedCandidates =
                    mutableCandidates,
                balancedCandidates =
                    mutableCandidates,
                finalCandidates =
                    mutableCandidates
            )

        mutableCandidates.reverse()

        assertTrue(
            report.inputCandidates ==
                    listOf(
                        first,
                        second
                    )
        )

        assertTrue(
            report.finalCandidates ==
                    listOf(
                        first,
                        second
                    )
        )
    }

    private fun candidate(
        suffix: String
    ): SelectionCandidate {
        val learningItemId =
            LearningItemId(
                "report-item-$suffix"
            )

        return SelectionCandidate(
            learningItem =
                LearningItem(
                    id =
                        learningItemId,
                    contentId =
                        ContentId(
                            "report-content-$suffix"
                        ),
                    mode =
                        LearningMode
                            .MEANING_RECOGNITION
                ),
            memoryState =
                MemoryState(
                    learnerId =
                        LearnerId(
                            "report-learner"
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