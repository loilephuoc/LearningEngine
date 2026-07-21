package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudyQueueStrategyType
import vn.loi.learning.infrastructure.LearningEngineFactory

class ContentDiversityQueueIntegrationTest {

    private val learnerId =
        LearnerId(
            "content-diversity-learner"
        )

    @Test
    fun `queue separates sibling items when another content is available`() {
        val engine =
            LearningEngineFactory.createInMemory()

        val contentA =
            ContentId(
                "diversity-content-a"
            )

        val contentB =
            ContentId(
                "diversity-content-b"
            )

        registerContent(
            engine = engine,
            contentId = contentA
        )

        registerContent(
            engine = engine,
            contentId = contentB
        )

        val itemA1 =
            registerLearningItem(
                engine = engine,
                itemId =
                    "diversity-item-a-1",
                contentId =
                    contentA
            )

        val itemA2 =
            registerLearningItem(
                engine = engine,
                itemId =
                    "diversity-item-a-2",
                contentId =
                    contentA
            )

        val itemB1 =
            registerLearningItem(
                engine = engine,
                itemId =
                    "diversity-item-b-1",
                contentId =
                    contentB
            )

        val dueAt =
            prepareReviewItems(
                engine = engine,
                itemIds =
                    listOf(
                        itemA1,
                        itemA2,
                        itemB1
                    )
            )

        val sessionId =
            SessionId(
                "content-diversity-session"
            )

        engine.startSession(
            StartStudySessionCommand(
                sessionId =
                    sessionId,
                learnerId =
                    learnerId,
                startedAt =
                    dueAt,
                policy =
                    SessionPolicy(
                        newItemLimit = 0,
                        reviewItemLimit = 3,
                        queueStrategy =
                            StudyQueueStrategyType
                                .REVIEW_FIRST
                    )
            )
        )

        val queue =
            requireNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            ).learningItemIds

        val contentByItemId =
            mapOf(
                itemA1 to contentA,
                itemA2 to contentA,
                itemB1 to contentB
            )

        assertEquals(
            expected = 3,
            actual = queue.size
        )

        assertEquals(
            expected =
                setOf(
                    itemA1,
                    itemA2,
                    itemB1
                ),
            actual =
                queue.toSet()
        )

        queue.zipWithNext()
            .forEach { pair ->
                val firstContentId =
                    requireNotNull(
                        contentByItemId[
                            pair.first
                        ]
                    )

                val secondContentId =
                    requireNotNull(
                        contentByItemId[
                            pair.second
                        ]
                    )

                assertTrue(
                    firstContentId !=
                            secondContentId
                )
            }
    }

    @Test
    fun `queue keeps unavoidable sibling items without dropping them`() {
        val engine =
            LearningEngineFactory.createInMemory()

        val contentId =
            ContentId(
                "single-diversity-content"
            )

        registerContent(
            engine = engine,
            contentId = contentId
        )

        val firstItemId =
            registerLearningItem(
                engine = engine,
                itemId =
                    "single-content-item-1",
                contentId =
                    contentId
            )

        val secondItemId =
            registerLearningItem(
                engine = engine,
                itemId =
                    "single-content-item-2",
                contentId =
                    contentId
            )

        val dueAt =
            prepareReviewItems(
                engine = engine,
                itemIds =
                    listOf(
                        firstItemId,
                        secondItemId
                    )
            )

        val sessionId =
            SessionId(
                "unavoidable-siblings-session"
            )

        engine.startSession(
            StartStudySessionCommand(
                sessionId =
                    sessionId,
                learnerId =
                    learnerId,
                startedAt =
                    dueAt,
                policy =
                    SessionPolicy(
                        newItemLimit = 0,
                        reviewItemLimit = 2,
                        queueStrategy =
                            StudyQueueStrategyType
                                .REVIEW_FIRST
                    )
            )
        )

        val queue =
            requireNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            ).learningItemIds

        assertEquals(
            expected = 2,
            actual = queue.size
        )

        assertEquals(
            expected =
                setOf(
                    firstItemId,
                    secondItemId
                ),
            actual =
                queue.toSet()
        )
    }

    private fun registerContent(
        engine: LearningEngine,
        contentId: ContentId
    ) {
        engine.registerContent(
            Content(
                id = contentId,
                type =
                    ContentType.SENTENCE,
                text =
                    ContentText(
                        primaryText =
                            "Content $contentId"
                    )
            )
        )
    }

    private fun registerLearningItem(
        engine: LearningEngine,
        itemId: String,
        contentId: ContentId
    ): LearningItemId {
        val learningItemId =
            LearningItemId(itemId)

        engine.registerLearningItem(
            LearningItem(
                id =
                    learningItemId,
                contentId =
                    contentId,
                mode =
                    LearningMode
                        .MEANING_RECOGNITION
            )
        )

        return learningItemId
    }

    private fun prepareReviewItems(
        engine: LearningEngine,
        itemIds: List<LearningItemId>
    ): Moment {
        val reviewedAt =
            Moment(1_000L)

        val dueTimes =
            itemIds.mapIndexed { index, itemId ->
                engine.review(
                    ReviewCommand(
                        reviewEventId =
                            ReviewEventId(
                                "diversity-preparation-" +
                                        "${index + 1}"
                            ),
                        learnerId =
                            learnerId,
                        learningItemId =
                            itemId,
                        rating =
                            ReviewRating.GOOD,
                        reviewedAt =
                            reviewedAt
                    )
                ).memoryState.dueAt
            }

        return dueTimes.maxOrNull()
            ?: reviewedAt
    }
}