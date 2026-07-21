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
import vn.loi.learning.infrastructure.LearningEngineFactory

class MixedStudyQueuePolicyIntegrationTest {

    private val learnerId =
        LearnerId("learner-1")

    @Test
    fun `mixed queue respects both limits and keeps reviews before new items`() {
        val engine =
            LearningEngineFactory
                .createInMemory()

        val reviewItemIds =
            (1..3)
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
            (1..3)
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

        val dueAt =
            reviewItemIds
                .mapIndexed { index, itemId ->
                    engine.review(
                        ReviewCommand(
                            reviewEventId =
                                ReviewEventId(
                                    "preparation-review-${index + 1}"
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
                    )
                        .memoryState
                        .dueAt
                }
                .first()

        val sessionId =
            SessionId("mixed-session")

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
                        newItemLimit = 2,
                        reviewItemLimit = 2
                    )
            )
        )

        val queue =
            requireNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        assertEquals(
            expected = 4,
            actual =
                queue.learningItemIds.size
        )

        val selectedReviewIds =
            queue.learningItemIds
                .take(2)

        val selectedNewIds =
            queue.learningItemIds
                .drop(2)

        assertEquals(
            expected = 2,
            actual =
                selectedReviewIds.size
        )

        assertEquals(
            expected = 2,
            actual =
                selectedNewIds.size
        )

        assertTrue(
            selectedReviewIds.all { itemId ->
                itemId in reviewItemIds
            }
        )

        assertTrue(
            selectedNewIds.all { itemId ->
                itemId in newItemIds
            }
        )

        assertTrue(
            queue.learningItemIds
                .takeWhile { itemId ->
                    itemId in reviewItemIds
                }
                .size ==
                    selectedReviewIds.size
        )
    }

    @Test
    fun `review limit does not reduce available new item capacity`() {
        val engine =
            LearningEngineFactory
                .createInMemory()

        val reviewItemIds =
            (1..4)
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
            (1..4)
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

        val dueAt =
            reviewItemIds
                .mapIndexed { index, itemId ->
                    engine.review(
                        ReviewCommand(
                            reviewEventId =
                                ReviewEventId(
                                    "preparation-review-${index + 1}"
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
                    )
                        .memoryState
                        .dueAt
                }
                .first()

        val sessionId =
            SessionId("independent-limits-session")

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
                        newItemLimit = 3,
                        reviewItemLimit = 1
                    )
            )
        )

        val queue =
            requireNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        val selectedReviewIds =
            queue.learningItemIds
                .filter { itemId ->
                    itemId in reviewItemIds
                }

        val selectedNewIds =
            queue.learningItemIds
                .filter { itemId ->
                    itemId in newItemIds
                }

        assertEquals(
            expected = 4,
            actual =
                queue.learningItemIds.size
        )

        assertEquals(
            expected = 1,
            actual =
                selectedReviewIds.size
        )

        assertEquals(
            expected = 3,
            actual =
                selectedNewIds.size
        )

        assertTrue(
            queue.learningItemIds
                .first() in
                    reviewItemIds
        )
    }

    @Test
    fun `review item that is not due does not enter mixed queue`() {
        val engine =
            LearningEngineFactory
                .createInMemory()

        val futureReviewItemId =
            registerItem(
                engine = engine,
                itemId =
                    "future-review-item",
                contentId =
                    "future-review-content"
            )

        val newItemIds =
            (1..3)
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

        engine.review(
            ReviewCommand(
                reviewEventId =
                    ReviewEventId(
                        "future-review-preparation"
                    ),
                learnerId =
                    learnerId,
                learningItemId =
                    futureReviewItemId,
                rating =
                    ReviewRating.EASY,
                reviewedAt =
                    reviewedAt
            )
        )

        val sessionId =
            SessionId("not-due-review-session")

        engine.startSession(
            StartStudySessionCommand(
                sessionId =
                    sessionId,
                learnerId =
                    learnerId,
                startedAt =
                    reviewedAt,
                policy =
                    SessionPolicy(
                        newItemLimit = 2,
                        reviewItemLimit = 5
                    )
            )
        )

        val queue =
            requireNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        assertEquals(
            expected = 2,
            actual =
                queue.learningItemIds.size
        )

        assertTrue(
            futureReviewItemId !in
                    queue.learningItemIds
        )

        assertTrue(
            queue.learningItemIds
                .all { itemId ->
                    itemId in newItemIds
                }
        )
    }

    @Test
    fun `new limit does not reduce available review item capacity`() {
        val engine =
            LearningEngineFactory
                .createInMemory()

        val reviewItemIds =
            (1..4)
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
            (1..4)
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

        val dueAt =
            reviewItemIds
                .mapIndexed { index, itemId ->
                    engine.review(
                        ReviewCommand(
                            reviewEventId =
                                ReviewEventId(
                                    "preparation-review-${index + 1}"
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
                    )
                        .memoryState
                        .dueAt
                }
                .first()

        val sessionId =
            SessionId("review-capacity-session")

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
                        newItemLimit = 1,
                        reviewItemLimit = 3
                    )
            )
        )

        val queue =
            requireNotNull(
                engine.getStudyQueue(
                    sessionId
                )
            )

        val selectedReviewIds =
            queue.learningItemIds
                .filter { itemId ->
                    itemId in reviewItemIds
                }

        val selectedNewIds =
            queue.learningItemIds
                .filter { itemId ->
                    itemId in newItemIds
                }

        assertEquals(
            expected = 4,
            actual =
                queue.learningItemIds.size
        )

        assertEquals(
            expected = 3,
            actual =
                selectedReviewIds.size
        )

        assertEquals(
            expected = 1,
            actual =
                selectedNewIds.size
        )

        assertTrue(
            queue.learningItemIds
                .take(3)
                .all { itemId ->
                    itemId in reviewItemIds
                }
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
}