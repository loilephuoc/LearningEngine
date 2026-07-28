package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import vn.loi.learning.application.review.ReviewCommand
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
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.LearningEngineFactory

class SessionItemOriginIntegrationTest {
    @Test
    fun `planned New origin drives first completion and Undo`() {
        val fixture = fixture("new")
        fixture.engine.startSession(fixture.startCommand())
        val next = assertNotNull(fixture.engine.getNextSessionItem(fixture.sessionId, Moment(2_100)))
        assertEquals(SessionItemOrigin.NEW, next.origin)
        assertEquals(SessionItemOrigin.NEW, fixture.engine.getStudyQueue(fixture.sessionId)
            ?.originOf(fixture.itemId))

        val reviewed = fixture.engine.reviewSessionItem(fixture.reviewCommand(ReviewRating.AGAIN))
        assertEquals(1, reviewed.session.newItemsReviewed)
        assertEquals(0, reviewed.session.reviewItemsReviewed)

        val undone = fixture.engine.undoLatestSessionReview(fixture.sessionId)
            as UndoLatestSessionReviewResult.Undone
        assertEquals(0, undone.session.newItemsReviewed)
        assertEquals(SessionItemOrigin.NEW, fixture.engine.getStudyQueue(fixture.sessionId)
            ?.currentItemOrigin)
    }

    @Test
    fun `planned Review origin never increments New for any rating`() {
        ReviewRating.entries.forEach { rating ->
            val fixture = fixture("review-${rating.name.lowercase()}")
            fixture.engine.review(
                ReviewCommand(
                    ReviewEventId("prior-${rating.name}"),
                    fixture.learner,
                    fixture.itemId,
                    ReviewRating.GOOD,
                    Moment(1_000)
                )
            )
            fixture.engine.startSession(fixture.startCommand())
            val next = assertNotNull(
                fixture.engine.getNextSessionItem(fixture.sessionId, Moment(2_100))
            )
            assertEquals(SessionItemOrigin.REVIEW, next.origin)

            val reviewed = fixture.engine.reviewSessionItem(fixture.reviewCommand(rating))
            assertEquals(0, reviewed.session.newItemsReviewed, rating.name)
            assertEquals(1, reviewed.session.reviewItemsReviewed, rating.name)

            val undone = fixture.engine.undoLatestSessionReview(fixture.sessionId)
                as UndoLatestSessionReviewResult.Undone
            assertEquals(0, undone.session.newItemsReviewed, rating.name)
            assertEquals(0, undone.session.reviewItemsReviewed, rating.name)
            assertEquals(SessionItemOrigin.REVIEW, fixture.engine.getStudyQueue(fixture.sessionId)
                ?.currentItemOrigin)
        }
    }

    @Test
    fun `same identity can advance a session counter only once`() {
        val fixture = fixture("duplicate")
        val started = fixture.engine.startSession(fixture.startCommand())
        val once = started.recordReview(
            fixture.itemId,
            fixture.contentId,
            wasNewItem = true
        )
        val twice = once.recordReview(
            fixture.itemId,
            fixture.contentId,
            wasNewItem = true
        )
        assertEquals(1, twice.newItemsReviewed)
        assertEquals(0, twice.reviewItemsReviewed)
    }

    @Test
    fun `reviewed sibling makes unseen current item Content-level Review through Undo`() {
        val engine = LearningEngineFactory.createInMemory()
        val learner = LearnerId("sibling-learner")
        val sessionId = SessionId("sibling-session")
        val contentId = ContentId("television")
        val itemA = LearningItemId("television-meaning")
        val itemB = LearningItemId("television-listening")
        engine.registerContent(Content(contentId, ContentType.WORD, ContentText("television")))
        engine.registerLearningItem(
            LearningItem(itemA, contentId, LearningMode.MEANING_RECOGNITION)
        )
        engine.registerLearningItem(
            LearningItem(itemB, contentId, LearningMode.LISTENING_RECOGNITION)
        )
        engine.review(
            ReviewCommand(
                ReviewEventId("prior-good"),
                learner,
                itemA,
                ReviewRating.GOOD,
                Moment(1_000)
            )
        )

        engine.startSession(
            StartStudySessionCommand(
                sessionId,
                learner,
                Moment(2_000),
                SessionPolicy(newItemLimit = 1, reviewItemLimit = 1),
                includedContentIds = setOf(contentId)
            )
        )
        val next = assertNotNull(engine.getNextSessionItem(sessionId, Moment(2_100)))
        assertEquals(itemB, next.item.learningItem.id)
        assertEquals(SessionItemOrigin.REVIEW, next.origin)
        assertEquals(contentId, engine.getStudyQueue(sessionId)?.currentContentId)
        assertEquals(ReviewRating.GOOD, engine.getContentLearningState(learner, contentId)
            .latestEffectiveRating)

        val reviewed = engine.reviewSessionItem(
            ReviewSessionItemCommand(
                sessionId,
                ReviewEventId("current-hard"),
                itemB,
                ReviewRating.HARD,
                Moment(3_000)
            )
        )
        assertEquals(0, reviewed.session.newItemsReviewed)
        assertEquals(1, reviewed.session.reviewItemsReviewed)
        assertEquals(ReviewRating.HARD, engine.getContentLearningState(learner, contentId)
            .latestEffectiveRating)

        val undone = engine.undoLatestSessionReview(sessionId)
            as UndoLatestSessionReviewResult.Undone
        assertEquals(0, undone.session.newItemsReviewed)
        assertEquals(0, undone.session.reviewItemsReviewed)
        assertEquals(ReviewRating.GOOD, engine.getContentLearningState(learner, contentId)
            .latestEffectiveRating)
        assertEquals(SessionItemOrigin.REVIEW, engine.getNextSessionItem(sessionId, Moment(3_100))
            ?.origin)
    }

    @Test
    fun `two technical siblings increment New once per Content`() {
        val fixture = fixture("content-counter")
        val sibling = LearningItemId("item-content-counter-listening")
        fixture.engine.registerLearningItem(
            LearningItem(sibling, fixture.contentId, LearningMode.LISTENING_RECOGNITION)
        )
        val started = fixture.engine.startSession(fixture.startCommand())
        val afterFirst = started.recordReview(
            fixture.itemId,
            fixture.contentId,
            wasNewItem = true
        )
        val afterSibling = afterFirst.recordReview(
            sibling,
            fixture.contentId,
            wasNewItem = true
        )
        assertEquals(1, afterSibling.newItemsReviewed)
        assertEquals(setOf(fixture.itemId, sibling), afterSibling.reviewedItemIds)
        assertEquals(setOf(fixture.contentId), afterSibling.reviewedContentIds)
    }

    @Test
    fun `all unseen siblings consume one New quota and Continue Learning treats sibling as Review`() {
        val fixture = fixture("unseen-siblings")
        val sibling = LearningItemId("item-unseen-siblings-listening")
        fixture.engine.registerLearningItem(
            LearningItem(sibling, fixture.contentId, LearningMode.LISTENING_RECOGNITION)
        )
        fixture.engine.startSession(fixture.startCommand())
        val first = assertNotNull(
            fixture.engine.getNextSessionItem(fixture.sessionId, Moment(2_100))
        )
        assertEquals(SessionItemOrigin.NEW, first.origin)
        val afterFirst = fixture.engine.reviewSessionItem(
            ReviewSessionItemCommand(
                fixture.sessionId,
                ReviewEventId("first-sibling"),
                first.item.learningItem.id,
                ReviewRating.GOOD,
                Moment(3_000)
            )
        )
        assertEquals(1, afterFirst.session.newItemsReviewed)

        assertEquals(
            null,
            fixture.engine.getNextSessionItem(fixture.sessionId, Moment(3_100))
        )
        val continuedSessionId = SessionId("continued-unseen-siblings")
        fixture.engine.startSession(
            StartStudySessionCommand(
                continuedSessionId,
                fixture.learner,
                Moment(3_200),
                SessionPolicy(newItemLimit = 1, reviewItemLimit = 1),
                includedContentIds = setOf(fixture.contentId)
            )
        )
        val second = assertNotNull(
            fixture.engine.getNextSessionItem(continuedSessionId, Moment(3_300))
        )
        assertEquals(SessionItemOrigin.REVIEW, second.origin)
        val afterSecond = fixture.engine.reviewSessionItem(
            ReviewSessionItemCommand(
                continuedSessionId,
                ReviewEventId("second-sibling"),
                second.item.learningItem.id,
                ReviewRating.EASY,
                Moment(4_000)
            )
        )
        assertEquals(0, afterSecond.session.newItemsReviewed)
        assertEquals(1, afterSecond.session.reviewItemsReviewed)
    }

    private fun fixture(suffix: String): Fixture {
        val engine = LearningEngineFactory.createInMemory()
        val contentId = ContentId("content-$suffix")
        val itemId = LearningItemId("item-$suffix")
        engine.registerContent(Content(contentId, ContentType.WORD, ContentText("word")))
        engine.registerLearningItem(
            LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION)
        )
        return Fixture(
            engine,
            LearnerId("learner-$suffix"),
            SessionId("session-$suffix"),
            contentId,
            itemId
        )
    }

    private data class Fixture(
        val engine: vn.loi.learning.application.LearningEngine,
        val learner: LearnerId,
        val sessionId: SessionId,
        val contentId: ContentId,
        val itemId: LearningItemId
    ) {
        fun startCommand() = StartStudySessionCommand(
            sessionId,
            learner,
            Moment(2_000),
            SessionPolicy(newItemLimit = 1, reviewItemLimit = 1),
            includedContentIds = setOf(contentId)
        )

        fun reviewCommand(rating: ReviewRating) = ReviewSessionItemCommand(
            sessionId,
            ReviewEventId("current-${rating.name}-$sessionId"),
            itemId,
            rating,
            Moment(3_000)
        )
    }
}
