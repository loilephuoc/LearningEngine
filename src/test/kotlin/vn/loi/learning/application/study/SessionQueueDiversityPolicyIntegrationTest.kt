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
import vn.loi.learning.domain.study.session.model.QueueDiversityPolicyType
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudyQueueStrategyType
import vn.loi.learning.infrastructure.LearningEngineFactory

class SessionQueueDiversityPolicyIntegrationTest {

    private val learnerId =
        LearnerId(
            "queue-diversity-policy-learner"
        )

    @Test
    fun `default policy keeps content diversity enabled`() {
        val fixture =
            createFixture()

        val queue =
            startSession(
                fixture = fixture,
                sessionId =
                    "default-diversity-session",
                diversityPolicy =
                    QueueDiversityPolicyType
                        .CONTENT_DIVERSITY
            )

        assertEquals(
            expected = 3,
            actual = queue.size
        )

        assertEquals(
            expected =
                fixture.allItemIds,
            actual =
                queue.toSet()
        )

        val contentIds =
            queue.map { itemId ->
                requireNotNull(
                    fixture.contentByItemId[
                        itemId
                    ]
                )
            }

        assertTrue(
            contentIds[0] !=
                    contentIds[1]
        )

        assertTrue(
            contentIds[1] !=
                    contentIds[2]
        )
    }

    @Test
    fun `none policy preserves strategy ordering without diversity`() {
        val fixture =
            createFixture()

        val queue =
            startSession(
                fixture = fixture,
                sessionId =
                    "disabled-diversity-session",
                diversityPolicy =
                    QueueDiversityPolicyType
                        .NONE
            )

        assertEquals(
            expected = 3,
            actual = queue.size
        )

        assertEquals(
            expected =
                fixture.allItemIds,
            actual =
                queue.toSet()
        )

        assertEquals(
            expected =
                fixture.contentA,
            actual =
                requireNotNull(
                    fixture.contentByItemId[
                        queue[0]
                    ]
                )
        )

        assertEquals(
            expected =
                fixture.contentA,
            actual =
                requireNotNull(
                    fixture.contentByItemId[
                        queue[1]
                    ]
                )
        )

        assertEquals(
            expected =
                fixture.contentB,
            actual =
                requireNotNull(
                    fixture.contentByItemId[
                        queue[2]
                    ]
                )
        )
    }

    private fun startSession(
        fixture: Fixture,
        sessionId: String,
        diversityPolicy:
        QueueDiversityPolicyType
    ): List<LearningItemId> {
        val resolvedSessionId =
            SessionId(sessionId)

        fixture.engine.startSession(
            StartStudySessionCommand(
                sessionId =
                    resolvedSessionId,
                learnerId =
                    learnerId,
                startedAt =
                    fixture.dueAt,
                policy =
                    SessionPolicy(
                        newItemLimit = 0,
                        reviewItemLimit = 3,
                        queueStrategy =
                            StudyQueueStrategyType
                                .REVIEW_FIRST,
                        queueDiversityPolicy =
                            diversityPolicy
                    )
            )
        )

        return requireNotNull(
            fixture.engine.getStudyQueue(
                resolvedSessionId
            )
        ).learningItemIds
    }

    private fun createFixture(): Fixture {
        val engine =
            LearningEngineFactory.createInMemory()

        val contentA =
            ContentId(
                "policy-content-a"
            )

        val contentB =
            ContentId(
                "policy-content-b"
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
                    "policy-item-a-1",
                contentId =
                    contentA,
                mode =
                    LearningMode
                        .MEANING_RECOGNITION
            )

        val itemA2 =
            registerLearningItem(
                engine = engine,
                itemId =
                    "policy-item-a-2",
                contentId =
                    contentA,
                mode =
                    LearningMode
                        .MEANING_RECALL
            )

        val itemB1 =
            registerLearningItem(
                engine = engine,
                itemId =
                    "policy-item-b-1",
                contentId =
                    contentB,
                mode =
                    LearningMode
                        .MEANING_RECOGNITION
            )

        val orderedItemIds =
            listOf(
                itemA1,
                itemA2,
                itemB1
            )

        val reviewedAt =
            Moment(1_000L)

        val dueTimes =
            orderedItemIds.mapIndexed { index, itemId ->
                engine.review(
                    ReviewCommand(
                        reviewEventId =
                            ReviewEventId(
                                "policy-preparation-" +
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

        return Fixture(
            engine = engine,
            contentA = contentA,
            contentB = contentB,
            allItemIds =
                orderedItemIds.toSet(),
            contentByItemId =
                mapOf(
                    itemA1 to contentA,
                    itemA2 to contentA,
                    itemB1 to contentB
                ),
            dueAt =
                dueTimes.maxOrNull()
                    ?: reviewedAt
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
        contentId: ContentId,
        mode: LearningMode
    ): LearningItemId {
        val learningItemId =
            LearningItemId(itemId)

        engine.registerLearningItem(
            LearningItem(
                id = learningItemId,
                contentId = contentId,
                mode = mode
            )
        )

        return learningItemId
    }

    private data class Fixture(
        val engine: LearningEngine,
        val contentA: ContentId,
        val contentB: ContentId,
        val allItemIds:
        Set<LearningItemId>,
        val contentByItemId:
        Map<LearningItemId, ContentId>,
        val dueAt: Moment
    )
}