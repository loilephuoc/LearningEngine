package vn.loi.learning.domain.study.session.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating

class StudySessionLifecycleTest {

    @Test
    fun `presentation reveal and pending review are durable active-session state`() {
        val itemId = LearningItemId("item-1")
        val pending = PendingSessionReview(
            reviewEventId = ReviewEventId("review-1"),
            learningItemId = itemId,
            rating = ReviewRating.GOOD,
            reviewedAt = Moment(2_000L),
            responseTime = null
        )
        val staged = session().presentItem(itemId, Moment(1_500L))
            .revealCurrentItem(itemId)
            .stageReview(pending)

        assertEquals(itemId, staged.currentLearningItemId)
        assertEquals(Moment(1_500L), staged.currentItemPresentedAt)
        assertEquals(true, staged.answerRevealed)
        assertEquals(pending, staged.pendingReview)
    }

    @Test
    fun `committed review clears transient checkpoint without adding pause state`() {
        val itemId = LearningItemId("item-1")
        val reviewed = session().presentItem(itemId, Moment(1_500L))
            .recordReview(itemId, ContentId("content-1"), wasNewItem = true)

        assertNull(reviewed.currentLearningItemId)
        assertNull(reviewed.currentItemPresentedAt)
        assertFalse(reviewed.answerRevealed)
        assertNull(reviewed.pendingReview)
        assertEquals(SessionStatus.ACTIVE, reviewed.status)
    }

    private fun session() = StudySession.start(
        id = SessionId("session-1"),
        learnerId = LearnerId("learner-1"),
        startedAt = Moment(1_000L),
        policy = SessionPolicy(newItemLimit = 2, reviewItemLimit = 2)
    )
}
