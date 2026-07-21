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

class SessionInterleavedQueueStrategyIntegrationTest {

    private val learnerId =
        LearnerId("interleaved-learner")

    @Test
    fun `interleaved session alternates review and new items`() {
        val fixture =
            createMixedFixture(
                reviewCount = 3,
                newCount = 3
            )

        val sessionId =
            SessionId(
                "interleaved-balanced-session"
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
                        newItemLimit = 3,
                        reviewItemLimit = 3,
                        queueStrategy =
                            StudyQueueStrategyType
                                .INTERLEAVED
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
            expected = 6,
            actual =
                queue.learningItemIds.size
        )

        queue.learningItemIds
            .forEachIndexed { index, itemId ->
                if (index % 2 == 0) {
                    assertTrue(
                        itemId in
                                fixture.reviewItemIds
                    )
                } else {
                    assertTrue(
                        itemId in
                                fixture.newItemIds
                    )
                }
            }
    }

    @Test
    fun `interleaved session continues with remaining group`() {
        val fixture =
            createMixedFixture(
                reviewCount = 3,
                newCount = 1
            )

        val sessionId =
            SessionId(
                "interleaved-unbalanced-session"
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
                                .INTERLEAVED
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
            queue.learningItemIds[0] in
                    fixture.reviewItemIds
        )

        assertTrue(
            queue.learningItemIds[1] in
                    fixture.newItemIds
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
    fun `interleaved strategy respects independent limits`() {
        val fixture =
            createMixedFixture(
                reviewCount = 4,
                newCount = 4
            )

        val sessionId =
            SessionId(
                "interleaved-limited-session"
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
                        reviewItemLimit = 3,
                        queueStrategy =
                            StudyQueueStrategyType
                                .INTERLEAVED
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
            expected = 5,
            actual =
                queue.learningItemIds.size
        )

        assertEquals(
            expected = 3,
            actual =
                queue.learningItemIds.count { itemId ->
                    itemId in
                            fixture.reviewItemIds
                }
        )

        assertEquals(
            expected = 2,
            actual =
                queue.learningItemIds.count { itemId ->
                    itemId in
                            fixture.newItemIds
                }
        )

        assertTrue(
            queue.learningItemIds[0] in
                    fixture.reviewItemIds
        )

        assertTrue(
            queue.learningItemIds[1] in
                    fixture.newItemIds
        )

        assertTrue(
            queue.learningItemIds[2] in
                    fixture.reviewItemIds
        )

        assertTrue(
            queue.learningItemIds[3] in
                    fixture.newItemIds
        )

        assertTrue(
            queue.learningItemIds[4] in
                    fixture.reviewItemIds
        )
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
                            "interleaved-review-$number",
                        contentId =
                            "interleaved-review-content-$number"
                    )
                }
                .toSet()

        val newItemIds =
            (1..newCount)
                .map { number ->
                    registerItem(
                        engine = engine,
                        itemId =
                            "interleaved-new-$number",
                        contentId =
                            "interleaved-new-content-$number"
                    )
                }
                .toSet()

        val reviewedAt =
            Moment(1_000L)

        val dueTimes =
            reviewItemIds.mapIndexed { index, itemId ->
                engine.review(
                    ReviewCommand(
                        reviewEventId =
                            ReviewEventId(
                                "interleaved-preparation-" +
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