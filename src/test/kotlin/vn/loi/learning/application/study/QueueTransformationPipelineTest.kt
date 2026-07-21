package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
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

class QueueTransformationPipelineTest {

    private val pipeline =
        QueueTransformationPipeline()

    @Test
    fun `applies strategy diversity balancer and final diversity guard in order`() {
        val first =
            candidate(
                suffix = "first",
                contentSuffix = "a"
            )

        val second =
            candidate(
                suffix = "second",
                contentSuffix = "a"
            )

        val third =
            candidate(
                suffix = "third",
                contentSuffix = "b"
            )

        val calls =
            mutableListOf<String>()

        val strategy =
            StudyQueueStrategy { candidates ->
                calls.add(
                    "strategy"
                )

                candidates
            }

        var diversityCallCount =
            0

        val queueDiversifier =
            QueueDiversifier { candidates ->
                diversityCallCount += 1

                calls.add(
                    "diversity-$diversityCallCount"
                )

                when (diversityCallCount) {
                    1 ->
                        listOf(
                            first,
                            third,
                            second
                        )

                    else ->
                        listOf(
                            first,
                            third,
                            second
                        )
                }
            }

        val queueBalancer =
            QueueBalancer { candidates ->
                calls.add(
                    "balancer"
                )

                assertEquals(
                    expected =
                        listOf(
                            first,
                            third,
                            second
                        ),
                    actual =
                        candidates
                )

                listOf(
                    first,
                    second,
                    third
                )
            }

        val result =
            pipeline.transform(
                candidates =
                    listOf(
                        first,
                        second,
                        third
                    ),
                strategy =
                    strategy,
                queueDiversifier =
                    queueDiversifier,
                queueBalancer =
                    queueBalancer
            )

        assertEquals(
            expected =
                listOf(
                    "strategy",
                    "diversity-1",
                    "balancer",
                    "diversity-2"
                ),
            actual =
                calls
        )

        assertEquals(
            expected =
                listOf(
                    first,
                    third,
                    second
                ),
            actual =
                result
        )
    }

    @Test
    fun `final content diversity guard repairs adjacency introduced by balancer`() {
        val contentA =
            ContentId(
                "pipeline-content-a"
            )

        val contentB =
            ContentId(
                "pipeline-content-b"
            )

        val firstA =
            candidate(
                suffix = "a-1",
                contentId = contentA
            )

        val secondA =
            candidate(
                suffix = "a-2",
                contentId = contentA
            )

        val itemB =
            candidate(
                suffix = "b-1",
                contentId = contentB
            )

        val result =
            pipeline.transform(
                candidates =
                    listOf(
                        firstA,
                        secondA,
                        itemB
                    ),
                strategy =
                    StudyQueueStrategy { candidates ->
                        candidates
                    },
                queueDiversifier =
                    ContentDiversityQueueDiversifier(),
                queueBalancer =
                    QueueBalancer {
                        listOf(
                            firstA,
                            secondA,
                            itemB
                        )
                    }
            )

        assertEquals(
            expected =
                listOf(
                    firstA,
                    itemB,
                    secondA
                ),
            actual =
                result
        )
    }

    @Test
    fun `no op transformations preserve strategy result`() {
        val first =
            candidate(
                suffix = "first",
                contentSuffix = "first"
            )

        val second =
            candidate(
                suffix = "second",
                contentSuffix = "second"
            )

        val third =
            candidate(
                suffix = "third",
                contentSuffix = "third"
            )

        val result =
            pipeline.transform(
                candidates =
                    listOf(
                        first,
                        second,
                        third
                    ),
                strategy =
                    StudyQueueStrategy {
                        listOf(
                            third,
                            second,
                            first
                        )
                    },
                queueDiversifier =
                    NoOpQueueDiversifier(),
                queueBalancer =
                    NoOpQueueBalancer()
            )

        assertEquals(
            expected =
                listOf(
                    third,
                    second,
                    first
                ),
            actual =
                result
        )
    }

    @Test
    fun `pipeline rejects strategy that drops a candidate`() {
        val first =
            candidate(
                suffix = "first",
                contentSuffix = "first"
            )

        val second =
            candidate(
                suffix = "second",
                contentSuffix = "second"
            )

        val exception =
            assertFailsWith<
                    IllegalStateException
                    > {
                pipeline.transform(
                    candidates =
                        listOf(
                            first,
                            second
                        ),
                    strategy =
                        StudyQueueStrategy {
                            listOf(
                                first
                            )
                        },
                    queueDiversifier =
                        NoOpQueueDiversifier(),
                    queueBalancer =
                        NoOpQueueBalancer()
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "STRATEGY"
        )
    }

    @Test
    fun `pipeline rejects diversifier that duplicates a candidate`() {
        val first =
            candidate(
                suffix = "first",
                contentSuffix = "first"
            )

        val second =
            candidate(
                suffix = "second",
                contentSuffix = "second"
            )

        val exception =
            assertFailsWith<
                    IllegalStateException
                    > {
                pipeline.transform(
                    candidates =
                        listOf(
                            first,
                            second
                        ),
                    strategy =
                        StudyQueueStrategy { candidates ->
                            candidates
                        },
                    queueDiversifier =
                        QueueDiversifier {
                            listOf(
                                first,
                                first
                            )
                        },
                    queueBalancer =
                        NoOpQueueBalancer()
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
    fun `pipeline rejects balancer that replaces a candidate`() {
        val first =
            candidate(
                suffix = "first",
                contentSuffix = "first"
            )

        val second =
            candidate(
                suffix = "second",
                contentSuffix = "second"
            )

        val replacement =
            candidate(
                suffix = "replacement",
                contentSuffix = "replacement"
            )

        val exception =
            assertFailsWith<
                    IllegalStateException
                    > {
                pipeline.transform(
                    candidates =
                        listOf(
                            first,
                            second
                        ),
                    strategy =
                        StudyQueueStrategy { candidates ->
                            candidates
                        },
                    queueDiversifier =
                        NoOpQueueDiversifier(),
                    queueBalancer =
                        QueueBalancer {
                            listOf(
                                first,
                                replacement
                            )
                        }
                )
            }

        assertContains(
            charSequence =
                exception.message.orEmpty(),
            other =
                "BALANCER"
        )
    }

    @Test
    fun `pipeline preserves candidate collection`() {
        val candidates =
            listOf(
                candidate(
                    suffix = "one",
                    contentSuffix = "a"
                ),
                candidate(
                    suffix = "two",
                    contentSuffix = "a"
                ),
                candidate(
                    suffix = "three",
                    contentSuffix = "b"
                ),
                candidate(
                    suffix = "four",
                    contentSuffix = "c"
                )
            )

        val result =
            pipeline.transform(
                candidates =
                    candidates,
                strategy =
                    ReviewFirstStudyQueueStrategy(),
                queueDiversifier =
                    ContentDiversityQueueDiversifier(),
                queueBalancer =
                    DifficultyQueueBalancer()
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
        contentSuffix: String
    ): SelectionCandidate =
        candidate(
            suffix =
                suffix,
            contentId =
                ContentId(
                    "pipeline-content-$contentSuffix"
                )
        )

    private fun candidate(
        suffix: String,
        contentId: ContentId
    ): SelectionCandidate {
        val learningItemId =
            LearningItemId(
                "pipeline-item-$suffix"
            )

        return SelectionCandidate(
            learningItem =
                LearningItem(
                    id =
                        learningItemId,
                    contentId =
                        contentId,
                    mode =
                        LearningMode
                            .MEANING_RECOGNITION
                ),
            memoryState =
                MemoryState(
                    learnerId =
                        LearnerId(
                            "pipeline-learner"
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