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

class NewFirstStudyQueueStrategyTest {

    private val strategy =
        NewFirstStudyQueueStrategy()

    @Test
    fun `orders new candidates before review candidates`() {
        val reviewCandidate1 =
            candidate(
                id = "review-1",
                stage = LearningStage.REVIEW
            )

        val newCandidate1 =
            candidate(
                id = "new-1",
                stage = LearningStage.NEW
            )

        val reviewCandidate2 =
            candidate(
                id = "review-2",
                stage = LearningStage.REVIEW
            )

        val newCandidate2 =
            candidate(
                id = "new-2",
                stage = LearningStage.NEW
            )

        val result =
            strategy.order(
                listOf(
                    reviewCandidate1,
                    newCandidate1,
                    reviewCandidate2,
                    newCandidate2
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
                    candidate.isNew
                }
        )

        assertTrue(
            result
                .drop(2)
                .all { candidate ->
                    !candidate.isNew
                }
        )
    }

    @Test
    fun `does not remove or duplicate candidates`() {
        val candidates =
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

        val result =
            strategy.order(candidates)

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

    @Test
    fun `preserves relative order inside the new group`() {
        val newCandidate1 =
            candidate(
                id = "new-1",
                stage = LearningStage.NEW
            )

        val newCandidate2 =
            candidate(
                id = "new-2",
                stage = LearningStage.NEW
            )

        val newCandidate3 =
            candidate(
                id = "new-3",
                stage = LearningStage.NEW
            )

        val result =
            strategy.order(
                listOf(
                    newCandidate1,
                    candidate(
                        id = "review-1",
                        stage = LearningStage.REVIEW
                    ),
                    newCandidate2,
                    candidate(
                        id = "review-2",
                        stage = LearningStage.REVIEW
                    ),
                    newCandidate3
                )
            )

        assertEquals(
            expected =
                listOf(
                    LearningItemId("new-1"),
                    LearningItemId("new-2"),
                    LearningItemId("new-3")
                ),
            actual =
                result
                    .filter { candidate ->
                        candidate.isNew
                    }
                    .map { candidate ->
                        candidate.learningItemId
                    }
        )
    }

    @Test
    fun `preserves relative order inside the review group`() {
        val reviewCandidate1 =
            candidate(
                id = "review-1",
                stage = LearningStage.REVIEW
            )

        val reviewCandidate2 =
            candidate(
                id = "review-2",
                stage = LearningStage.REVIEW
            )

        val reviewCandidate3 =
            candidate(
                id = "review-3",
                stage = LearningStage.REVIEW
            )

        val result =
            strategy.order(
                listOf(
                    reviewCandidate1,
                    candidate(
                        id = "new-1",
                        stage = LearningStage.NEW
                    ),
                    reviewCandidate2,
                    candidate(
                        id = "new-2",
                        stage = LearningStage.NEW
                    ),
                    reviewCandidate3
                )
            )

        assertEquals(
            expected =
                listOf(
                    LearningItemId("review-1"),
                    LearningItemId("review-2"),
                    LearningItemId("review-3")
                ),
            actual =
                result
                    .filter { candidate ->
                        !candidate.isNew
                    }
                    .map { candidate ->
                        candidate.learningItemId
                    }
        )
    }

    @Test
    fun `empty candidates produce empty result`() {
        assertEquals(
            expected =
                emptyList(),
            actual =
                strategy.order(
                    emptyList()
                )
        )
    }

    @Test
    fun `all new candidates remain unchanged`() {
        val candidates =
            listOf(
                candidate(
                    id = "new-1",
                    stage = LearningStage.NEW
                ),
                candidate(
                    id = "new-2",
                    stage = LearningStage.NEW
                ),
                candidate(
                    id = "new-3",
                    stage = LearningStage.NEW
                )
            )

        assertEquals(
            expected =
                candidates,
            actual =
                strategy.order(
                    candidates
                )
        )
    }

    @Test
    fun `all review candidates remain unchanged`() {
        val candidates =
            listOf(
                candidate(
                    id = "review-1",
                    stage = LearningStage.REVIEW
                ),
                candidate(
                    id = "review-2",
                    stage = LearningStage.REVIEW
                ),
                candidate(
                    id = "review-3",
                    stage = LearningStage.REVIEW
                )
            )

        assertEquals(
            expected =
                candidates,
            actual =
                strategy.order(
                    candidates
                )
        )
    }

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