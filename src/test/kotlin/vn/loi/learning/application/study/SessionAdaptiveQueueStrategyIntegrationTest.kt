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

class SessionAdaptiveQueueStrategyIntegrationTest {

    private val learnerId =
        LearnerId("adaptive-learner")

    @Test
    fun `adaptive strategy chooses review first when review backlog dominates`() {
        val fixture =
            createMixedFixture(
                reviewCount = 4,
                newCount = 2
            )

        val queue =
            startAdaptiveSession(
                fixture = fixture,
                sessionId =
                    "adaptive-review-dominant"
            )

        assertEquals(
            expected = 6,
            actual =
                queue.size
        )

        assertTrue(
            queue.take(4).all { itemId ->
                itemId in fixture.reviewItemIds
            }
        )

        assertTrue(
            queue.drop(4).all { itemId ->
                itemId in fixture.newItemIds
            }
        )
    }

    @Test
    fun `adaptive strategy chooses new first when new backlog dominates`() {
        val fixture =
            createMixedFixture(
                reviewCount = 2,
                newCount = 4
            )

        val queue =
            startAdaptiveSession(
                fixture = fixture,
                sessionId =
                    "adaptive-new-dominant"
            )

        assertEquals(
            expected = 6,
            actual =
                queue.size
        )

        assertTrue(
            queue.take(4).all { itemId ->
                itemId in fixture.newItemIds
            }
        )

        assertTrue(
            queue.drop(4).all { itemId ->
                itemId in fixture.reviewItemIds
            }
        )
    }

    @Test
    fun `adaptive strategy interleaves when backlogs are balanced`() {
        val fixture =
            createMixedFixture(
                reviewCount = 3,
                newCount = 3
            )

        val queue =
            startAdaptiveSession(
                fixture = fixture,
                sessionId =
                    "adaptive-balanced"
            )

        assertEquals(
            expected = 6,
            actual =
                queue.size
        )

        queue.forEachIndexed { index, itemId ->
            if (index % 2 == 0) {
                assertTrue(
                    itemId in fixture.reviewItemIds
                )
            } else {
                assertTrue(
                    itemId in fixture.newItemIds
                )
            }
        }
    }

    @Test
    fun `adaptive strategy respects policy limits after ordering`() {
        val fixture =
            createMixedFixture(
                reviewCount = 6,
                newCount = 3
            )

        val sessionId =
            SessionId(
                "adaptive-limited"
            )

        fixture.engine.startSession(
            StartStudySessionCommand(
                sessionId =
                    sessionId,
                learnerId =
                    learnerId,
                startedAt =
                    fixture.dueAt,
                policy =
                    SessionPolicy(
                        newItemLimit = 1,
                        reviewItemLimit = 2,
                        queueStrategy =
                            StudyQueueStrategyType.ADAPTIVE
                    )
            )
        )

        val queue =
            requireNotNull(
                fixture.engine.getStudyQueue(
                    sessionId
                )
            ).learningItemIds

        assertEquals(
            expected = 3,
            actual =
                queue.size
        )

        assertEquals(
            expected = 2,
            actual =
                queue.count { itemId ->
                    itemId in fixture.reviewItemIds
                }
        )

        assertEquals(
            expected = 1,
            actual =
                queue.count { itemId ->
                    itemId in fixture.newItemIds
                }
        )

        assertTrue(
            queue.take(2).all { itemId ->
                itemId in fixture.reviewItemIds
            }
        )

        assertTrue(
            queue.last() in fixture.newItemIds
        )
    }

    private fun startAdaptiveSession(
        fixture: MixedFixture,
        sessionId: String
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
                        newItemLimit =
                            fixture.newItemIds.size,
                        reviewItemLimit =
                            fixture.reviewItemIds.size,
                        queueStrategy =
                            StudyQueueStrategyType.ADAPTIVE
                    )
            )
        )

        return requireNotNull(
            fixture.engine.getStudyQueue(
                resolvedSessionId
            )
        ).learningItemIds
    }

    private fun createMixedFixture(
        reviewCount: Int,
        newCount: Int
    ): MixedFixture {
        val engine =
            LearningEngineFactory.createInMemory()

        val reviewItemIds =
            (1..reviewCount)
                .map { number ->
                    registerItem(
                        engine = engine,
                        itemId =
                            "adaptive-review-$number",
                        contentId =
                            "adaptive-review-content-$number"
                    )
                }
                .toSet()

        val newItemIds =
            (1..newCount)
                .map { number ->
                    registerItem(
                        engine = engine,
                        itemId =
                            "adaptive-new-$number",
                        contentId =
                            "adaptive-new-content-$number"
                    )
                }
                .toSet()

        val reviewedAt =
            Moment(1_000L)

        val dueTimes =
            reviewItemIds
                .mapIndexed { index, itemId ->
                    engine.review(
                        ReviewCommand(
                            reviewEventId =
                                ReviewEventId(
                                    "adaptive-preparation-" +
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

        return MixedFixture(
            engine = engine,
            reviewItemIds = reviewItemIds,
            newItemIds = newItemIds,
            dueAt =
                dueTimes.maxOrNull()
                    ?: reviewedAt
        )
    }

    private fun registerItem(
        engine: LearningEngine,
        itemId: String,
        contentId: String
    ): LearningItemId {
        val resolvedContentId =
            ContentId(contentId)

        engine.registerContent(
            Content(
                id = resolvedContentId,
                type = ContentType.SENTENCE,
                text =
                    ContentText(
                        primaryText =
                            "Sentence for $itemId"
                    )
            )
        )

        val resolvedItemId =
            LearningItemId(itemId)

        engine.registerLearningItem(
            LearningItem(
                id = resolvedItemId,
                contentId = resolvedContentId,
                mode =
                    LearningMode
                        .MEANING_RECOGNITION
            )
        )

        return resolvedItemId
    }

    private data class MixedFixture(
        val engine: LearningEngine,
        val reviewItemIds:
        Set<LearningItemId>,
        val newItemIds:
        Set<LearningItemId>,
        val dueAt: Moment
    )
}