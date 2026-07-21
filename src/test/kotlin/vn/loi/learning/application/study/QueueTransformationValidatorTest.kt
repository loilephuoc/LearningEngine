package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.selection.model.SelectionCandidate

class QueueTransformationValidatorTest {

    private val validator =
        QueueTransformationValidator()

    @Test
    fun `accepts ordering-only transformation`() {
        val first =
            candidate(
                suffix = "first"
            )

        val second =
            candidate(
                suffix = "second"
            )

        validator.validate(
            stage =
                QueueTransformationStage.STRATEGY,
            before =
                listOf(
                    first,
                    second
                ),
            after =
                listOf(
                    second,
                    first
                )
        )
    }

    @Test
    fun `rejects removed candidate`() {
        val first =
            candidate(
                suffix = "first"
            )

        val second =
            candidate(
                suffix = "second"
            )

        val exception =
            assertFailsWith<
                    IllegalStateException
                    > {
                validator.validate(
                    stage =
                        QueueTransformationStage
                            .BALANCER,
                    before =
                        listOf(
                            first,
                            second
                        ),
                    after =
                        listOf(
                            first
                        )
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "BALANCER"
        )

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "Candidate count changed"
        )
    }

    @Test
    fun `rejects added candidate`() {
        val first =
            candidate(
                suffix = "first"
            )

        val second =
            candidate(
                suffix = "second"
            )

        val exception =
            assertFailsWith<
                    IllegalStateException
                    > {
                validator.validate(
                    stage =
                        QueueTransformationStage
                            .INITIAL_DIVERSITY,
                    before =
                        listOf(
                            first
                        ),
                    after =
                        listOf(
                            first,
                            second
                        )
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "INITIAL_DIVERSITY"
        )
    }

    @Test
    fun `rejects duplicated candidate even when count is unchanged`() {
        val first =
            candidate(
                suffix = "first"
            )

        val second =
            candidate(
                suffix = "second"
            )

        val exception =
            assertFailsWith<
                    IllegalStateException
                    > {
                validator.validate(
                    stage =
                        QueueTransformationStage
                            .FINAL_DIVERSITY,
                    before =
                        listOf(
                            first,
                            second
                        ),
                    after =
                        listOf(
                            first,
                            first
                        )
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "FINAL_DIVERSITY"
        )

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "candidate collection was modified"
        )
    }

    @Test
    fun `rejects replacement candidate even when count is unchanged`() {
        val first =
            candidate(
                suffix = "first"
            )

        val second =
            candidate(
                suffix = "second"
            )

        val replacement =
            candidate(
                suffix = "replacement"
            )

        val exception =
            assertFailsWith<
                    IllegalStateException
                    > {
                validator.validate(
                    stage =
                        QueueTransformationStage
                            .STRATEGY,
                    before =
                        listOf(
                            first,
                            second
                        ),
                    after =
                        listOf(
                            first,
                            replacement
                        )
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "STRATEGY"
        )
    }

    private fun candidate(
        suffix: String
    ): SelectionCandidate {
        val learningItemId =
            LearningItemId(
                "validator-item-$suffix"
            )

        return SelectionCandidate(
            learningItem =
                LearningItem(
                    id =
                        learningItemId,
                    contentId =
                        ContentId(
                            "validator-content-$suffix"
                        ),
                    mode =
                        LearningMode
                            .MEANING_RECOGNITION
                ),
            memoryState =
                MemoryState(
                    learnerId =
                        LearnerId(
                            "validator-learner"
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