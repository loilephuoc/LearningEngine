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
