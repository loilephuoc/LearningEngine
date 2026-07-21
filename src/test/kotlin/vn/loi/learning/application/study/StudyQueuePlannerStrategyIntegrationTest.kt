package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.selection.model.SelectionCandidate

class StudyQueuePlannerStrategyIntegrationTest {

    @Test
    fun `custom strategy controls ordering without removing planner metadata`() {
        val strategy =
            StudyQueueStrategy { candidates ->
                candidates.reversed()
            }

        val original =
            listOf(
                candidateId("item-1"),
                candidateId("item-2"),
                candidateId("item-3")
            )

        val reordered =
            strategy.order(
                original.map { id ->
                    FakeCandidateFactory
                        .create(id)
                }
            )

        assertEquals(
            expected =
                listOf(
                    candidateId("item-3"),
                    candidateId("item-2"),
                    candidateId("item-1")
                ),
            actual =
                reordered.map(
                    SelectionCandidate::learningItemId
                )
        )
    }

    private fun candidateId(
        value: String
    ) =
        vn.loi.learning.domain.study
            .learning.model
            .LearningItemId(value)

    private object FakeCandidateFactory {

        fun create(
            id:
            vn.loi.learning.domain.study
            .learning.model
            .LearningItemId
        ): SelectionCandidate {
            val item =
                vn.loi.learning.domain.study
                    .learning.model
                    .LearningItem(
                        id = id,
                        contentId =
                            vn.loi.learning.domain
                                .content.model
                                .ContentId(
                                    "content-${id.value}"
                                ),
                        mode =
                            vn.loi.learning.domain.study
                                .learning.model
                                .LearningMode
                                .MEANING_RECOGNITION
                    )

            return SelectionCandidate(
                learningItem = item,
                memoryState =
                    vn.loi.learning.domain.study
                        .memory.model
                        .MemoryState.new(
                            learnerId =
                                vn.loi.learning.domain
                                    .study.memory.model
                                    .LearnerId(
                                        "learner-1"
                                    ),
                            learningItemId =
                                id,
                            availableAt =
                                vn.loi.learning.domain
                                    .study.memory.model
                                    .Moment(1_000L)
                        )
            )
        }
    }
}