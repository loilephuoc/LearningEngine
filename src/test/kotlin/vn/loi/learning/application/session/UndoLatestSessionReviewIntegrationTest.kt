package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
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
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.infrastructure.LearningEngineFactory

class UndoLatestSessionReviewIntegrationTest {
    @Test
    fun `undo restores first review memory queue session and is idempotent`() {
        val engine = LearningEngineFactory.createInMemory()
        val learner = LearnerId("learner")
        val sessionId = SessionId("session")
        val itemId = LearningItemId("item")
        engine.registerContent(Content(ContentId("content"), ContentType.WORD, ContentText("word")))
        engine.registerLearningItem(LearningItem(itemId, ContentId("content"), LearningMode.MEANING_RECOGNITION))
        engine.startSession(StartStudySessionCommand(sessionId, learner, Moment(1_000), SessionPolicy(1, 1)))
        engine.getNextSessionItem(sessionId, Moment(1_100))
        engine.revealSessionItem(sessionId, itemId)
        engine.reviewSessionItem(
            ReviewSessionItemCommand(sessionId, ReviewEventId("review"), itemId, ReviewRating.GOOD, Moment(2_000))
        )

        val undone = assertIs<UndoLatestSessionReviewResult.Undone>(engine.undoLatestSessionReview(sessionId))
        assertEquals(SessionStatus.ACTIVE, undone.session.status)
        assertEquals(0, undone.session.totalReviews)
        assertEquals(itemId, undone.session.currentLearningItemId)
        assertEquals(true, undone.session.answerRevealed)
        assertEquals(0, engine.requireStudyQueueProgress(sessionId).currentIndex)
        assertNull(engine.getMemoryState(learner, itemId))
        assertEquals(emptyList(), engine.getReviewHistory(learner, itemId))
        assertEquals(UndoLatestSessionReviewResult.NothingToUndo, engine.undoLatestSessionReview(sessionId))
    }

    @Test
    fun `undo reopens completion after the final review`() {
        val engine = LearningEngineFactory.createInMemory()
        val learner = LearnerId("learner")
        val sessionId = SessionId("completed-session")
        val itemId = LearningItemId("completed-item")
        engine.registerContent(Content(ContentId("content"), ContentType.WORD, ContentText("word")))
        engine.registerLearningItem(LearningItem(itemId, ContentId("content"), LearningMode.MEANING_RECOGNITION))
        engine.startSession(StartStudySessionCommand(sessionId, learner, Moment(1_000), SessionPolicy(1, 1)))
        engine.getNextSessionItem(sessionId, Moment(1_100))
        engine.reviewSessionItem(
            ReviewSessionItemCommand(sessionId, ReviewEventId("review"), itemId, ReviewRating.GOOD, Moment(2_000))
        )
        engine.finishSession(sessionId, Moment(2_100))

        val undone = assertIs<UndoLatestSessionReviewResult.Undone>(engine.undoLatestSessionReview(sessionId))
        assertEquals(SessionStatus.ACTIVE, undone.session.status)
        assertEquals(itemId, engine.getStudyQueue(sessionId)?.currentLearningItemId)
    }
}
