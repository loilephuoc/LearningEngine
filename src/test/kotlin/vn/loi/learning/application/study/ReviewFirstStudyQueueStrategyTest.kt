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

class ReviewFirstStudyQueueStrategyTest {

    private val strategy =
        ReviewFirstStudyQueueStrategy()

    @Test
    fun `orders review candidates before new candidates`() {
        val newCandidate1 =
            candidate(
                id = "new-1",
                stage = LearningStage.NEW
            )

        val reviewCandidate1 =
            candidate(
                id = "review-1",
                stage = LearningStage.REVIEW
            )

        val newCandidate2 =
            candidate(
                id = "new-2",
                stage = LearningStage.NEW
            )

        val reviewCandidate2 =
            candidate(
                id = "review-2",
                stage = LearningStage.REVIEW
            )

        val result =
            strategy.order(
                listOf(
                    newCandidate1,
                    reviewCandidate1,
                    newCandidate2,
                    reviewCandidate2
                )
            )

        assertEquals(
            expected = 4,
            actual = result.size
        )

        assertTrue(
            result
                .take(2)
                .all { candidate ->
                    !candidate.isNew
                }
        )

        assertTrue(
            result
                .drop(2)
                .all { candidate ->
                    candidate.isNew
                }
        )
    }

    @Test
    fun `does not remove candidates`() {
        val candidates =
            listOf(
                candidate(
                    id = "new-1",
                    stage = LearningStage.NEW
                ),
                candidate(
                    id = "review-1",
                    stage = LearningStage.REVIEW
                ),
                candidate(
                    id = "new-2",
                    stage = LearningStage.NEW
                )
            )

        val result =
            strategy.order(candidates)

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

    @Test
    fun `empty candidates produce empty result`() {
        assertEquals(
            expected = emptyList(),
            actual =
                strategy.order(
                    emptyList()
                )
        )
    }

    private fun candidate(
        id: String,
        stage: LearningStage
    ): SelectionCandidate {
        val learningItemId =
            LearningItemId(id)

        val lastReviewedAt =
            if (stage == LearningStage.NEW) {
                null
            } else {
                Moment(500L)
            }

        val reviewCount =
            if (stage == LearningStage.NEW) {
                0
            } else {
                1
            }

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
                        if (
                            stage ==
                            LearningStage.NEW
                        ) {
                            0.0
                        } else {
                            1.0
                        },
                    dueAt =
                        Moment(1_000L),
                    lastReviewedAt =
                        lastReviewedAt,
                    reviewCount =
                        reviewCount,
                    lapseCount = 0
                )
        )
    }
}