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

class SessionQueueStrategyIntegrationTest {

    private val learnerId =
        LearnerId("learner-1")

    @Test
    fun `active session limit update replans the canonical queue immediately`() {
        val fixture = createMixedFixture()
        val sessionId = SessionId("live-limit-session")
        fixture.engine.startSession(StartStudySessionCommand(
            sessionId, learnerId, fixture.dueAt, SessionPolicy(newItemLimit = 2, reviewItemLimit = 2)
        ))
        assertEquals(4, fixture.engine.getStudyQueue(sessionId)?.learningItemIds?.size)

        val lowered = fixture.engine.updateActiveSessionLimits(sessionId, newLimit = 1, reviewLimit = 1)
        val loweredQueue = requireNotNull(fixture.engine.getStudyQueue(sessionId))
        assertEquals(1, lowered.policy.newItemLimit)
        assertEquals(1, lowered.policy.reviewItemLimit)
        assertEquals(1, loweredQueue.configuredNewTarget)
        assertEquals(1, loweredQueue.configuredReviewTarget)
        assertEquals(2, loweredQueue.learningItemIds.size)

        fixture.engine.updateActiveSessionLimits(sessionId, newLimit = 2, reviewLimit = 2)
        assertEquals(4, fixture.engine.getStudyQueue(sessionId)?.learningItemIds?.size)
    }

    @Test
    fun `default session policy keeps review first behavior`() {
        val fixture =
            createMixedFixture()

        val sessionId =
            SessionId(
                "default-review-first-session"
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
                        newItemLimit = 2,
                        reviewItemLimit = 2
                    )
            )
        )

        val queue =
            requireNotNull(
                fixture.engine.getStudyQueue(
                    sessionId
                )
            )

        assertEquals(
            expected = 4,
            actual =
                queue.learningItemIds.size
        )

        assertTrue(
            queue.learningItemIds
                .take(2)
                .all { itemId ->
                    itemId in
                            fixture.reviewItemIds
                }
        )

        assertTrue(
            queue.learningItemIds
                .drop(2)
                .all { itemId ->
                    itemId in
                            fixture.newItemIds
                }
        )
    }

    @Test
    fun `new first session places new items before review items`() {
        val fixture =
            createMixedFixture()

        val sessionId =
            SessionId(
                "new-first-session"
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
                        newItemLimit = 2,
                        reviewItemLimit = 2,
                        queueStrategy =
                            StudyQueueStrategyType
                                .NEW_FIRST
                    )
            )
        )

        val queue =
            requireNotNull(
                fixture.engine.getStudyQueue(
                    sessionId
                )
            )

        assertEquals(
            expected = 4,
            actual =
                queue.learningItemIds.size
        )

        assertTrue(
            queue.learningItemIds
                .take(2)
                .all { itemId ->
                    itemId in
                            fixture.newItemIds
                }
        )

        assertTrue(
            queue.learningItemIds
                .drop(2)
                .all { itemId ->
                    itemId in
                            fixture.reviewItemIds
                }
        )
    }

    @Test
    fun `new first strategy still respects independent limits`() {
        val fixture =
            createMixedFixture(
                reviewCount = 4,
                newCount = 4
            )

        val sessionId =
            SessionId(
                "new-first-limited-session"
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
                        reviewItemLimit = 3,
                        queueStrategy =
                            StudyQueueStrategyType
                                .NEW_FIRST
                    )
            )
        )

        val queue =
            requireNotNull(
                fixture.engine.getStudyQueue(
                    sessionId
                )
            )

        assertEquals(
            expected = 4,
            actual =
                queue.learningItemIds.size
        )

        assertTrue(
            queue.learningItemIds
                .first() in
                    fixture.newItemIds
        )

        assertEquals(
            expected = 1,
            actual =
                queue.learningItemIds
                    .count { itemId ->
                        itemId in
                                fixture.newItemIds
                    }
        )

        assertEquals(
            expected = 3,
            actual =
                queue.learningItemIds
                    .count { itemId ->
                        itemId in
                                fixture.reviewItemIds
                    }
        )
    }

    private fun createMixedFixture(
        reviewCount: Int = 3,
        newCount: Int = 3
    ): MixedFixture {
        val engine =
            LearningEngineFactory
                .createInMemory()

        val reviewItemIds =
            (1..reviewCount)
                .map { number ->
                    registerItem(
                        engine = engine,
                        itemId =
                            "review-item-$number",
                        contentId =
                            "review-content-$number"
                    )
                }
                .toSet()

        val newItemIds =
            (1..newCount)
                .map { number ->
                    registerItem(
                        engine = engine,
                        itemId =
                            "new-item-$number",
                        contentId =
                            "new-content-$number"
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
                                    "preparation-review-" +
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
            engine =
                engine,
            reviewItemIds =
                reviewItemIds,
            newItemIds =
                newItemIds,
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
                id =
                    resolvedContentId,
                type =
                    ContentType.SENTENCE,
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
                id =
                    resolvedItemId,
                contentId =
                    resolvedContentId,
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
