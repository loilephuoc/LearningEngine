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

class DifficultyQueueBalancerTest {

    private val balancer =
        DifficultyQueueBalancer()

    @Test
    fun `alternates difficulty bands when alternatives exist`() {
        val hardOne =
            candidate(
                suffix = "hard-1",
                difficulty = 8.0
            )

        val hardTwo =
            candidate(
                suffix = "hard-2",
                difficulty = 9.0
            )

        val easy =
            candidate(
                suffix = "easy",
                difficulty = 2.0
            )

        val medium =
            candidate(
                suffix = "medium",
                difficulty = 5.0
            )

        val result =
            balancer.balance(
                listOf(
                    hardOne,
                    hardTwo,
                    easy,
                    medium
                )
            )

        assertEquals(
            expected =
                listOf(
                    hardOne,
                    easy,
                    hardTwo,
                    medium
                ),
            actual =
                result
        )
    }

    @Test
    fun `keeps unavoidable same band candidates`() {
        val first =
            candidate(
                suffix = "first",
                difficulty = 8.0
            )

        val second =
            candidate(
                suffix = "second",
                difficulty = 9.0
            )

        val result =
            balancer.balance(
                listOf(
                    first,
                    second
                )
            )

        assertEquals(
            expected =
                listOf(
                    first,
                    second
                ),
            actual =
                result
        )
    }

    @Test
    fun `preserves every candidate exactly once`() {
        val candidates =
            listOf(
                candidate(
                    suffix = "hard-1",
                    difficulty = 8.0
                ),
                candidate(
                    suffix = "hard-2",
                    difficulty = 9.0
                ),
                candidate(
                    suffix = "easy-1",
                    difficulty = 2.0
                ),
                candidate(
                    suffix = "medium-1",
                    difficulty = 5.0
                ),
                candidate(
                    suffix = "easy-2",
                    difficulty = 3.0
                )
            )

        val result =
            balancer.balance(
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
                candidates.toSet(),
            actual =
                result.toSet()
        )
    }

    private fun candidate(
        suffix: String,
        difficulty: Double
    ): SelectionCandidate {
        val itemId =
            LearningItemId(
                "difficulty-item-$suffix"
            )

        val learningItem =
            LearningItem(
                id = itemId,
                contentId =
                    ContentId(
                        "difficulty-content-$suffix"
                    ),
                mode =
                    LearningMode
                        .MEANING_RECOGNITION
            )

        val memoryState =
            MemoryState(
                learnerId =
                    LearnerId(
                        "difficulty-learner"
                    ),
                learningItemId =
                    itemId,
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

        return SelectionCandidate(
            learningItem =
                learningItem,
            memoryState =
                memoryState
        )
    }
}