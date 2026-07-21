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

class StudyQueueStrategyContractTest {

    private val strategies =
        listOf(
            ReviewFirstStudyQueueStrategy(),
            NewFirstStudyQueueStrategy()
        )

    @Test
    fun `all strategies preserve candidate membership`() {
        val candidates =
            candidates()

        strategies.forEach { strategy ->
            val result =
                strategy.order(
                    candidates
                )

            assertEquals(
                expected =
                    candidates.size,
                actual =
                    result.size
            )

            assertEquals(
                expected =
                    candidates
                        .map { candidate ->
                            candidate.learningItemId
                        }
                        .toSet(),
                actual =
                    result
                        .map { candidate ->
                            candidate.learningItemId
                        }
                        .toSet()
            )
        }
    }

    @Test
    fun `all strategies are deterministic`() {
        val candidates =
            candidates()

        strategies.forEach { strategy ->
            val firstResult =
                strategy.order(
                    candidates
                )

            val secondResult =
                strategy.order(
                    candidates
                )

            assertEquals(
                expected =
                    firstResult,
                actual =
                    secondResult
            )
        }
    }

    @Test
    fun `all strategies leave input list unchanged`() {
        val candidates =
            candidates()

        val originalIds =
            candidates.map { candidate ->
                candidate.learningItemId
            }

        strategies.forEach { strategy ->
            strategy.order(
                candidates
            )

            assertEquals(
                expected =
                    originalIds,
                actual =
                    candidates.map { candidate ->
                        candidate.learningItemId
                    }
            )
        }
    }

    private fun candidates():
            List<SelectionCandidate> =
        listOf(
            candidate(
                id = "review-1",
                stage = LearningStage.REVIEW
            ),
            candidate(
                id = "new-1",
                stage = LearningStage.NEW
            ),
            candidate(
                id = "review-2",
                stage = LearningStage.REVIEW
            ),
            candidate(
                id = "new-2",
                stage = LearningStage.NEW
            )
        )

    private fun candidate(
        id: String,
        stage: LearningStage
    ): SelectionCandidate {
        val learningItemId =
            LearningItemId(id)

        val isNew =
            stage == LearningStage.NEW

        return SelectionCandidate(
            learningItem =
                LearningItem(
                    id =
                        learningItemId,
                    contentId =
                        ContentId(
                            "content-$id"
                        ),
                    mode =
                        LearningMode
                            .MEANING_RECOGNITION
                ),
            memoryState =
                MemoryState(
                    learnerId =
                        LearnerId("learner-1"),
                    learningItemId =
                        learningItemId,
                    stage =
                        stage,
                    difficulty =
                        MemoryState
                            .DEFAULT_DIFFICULTY,
                    stabilityDays =
                        if (isNew) {
                            0.0
                        } else {
                            1.0
                        },
                    dueAt =
                        Moment(1_000L),
                    lastReviewedAt =
                        if (isNew) {
                            null
                        } else {
                            Moment(500L)
                        },
                    reviewCount =
                        if (isNew) {
                            0
                        } else {
                            1
                        },
                    lapseCount = 0
                )
        )
    }
}