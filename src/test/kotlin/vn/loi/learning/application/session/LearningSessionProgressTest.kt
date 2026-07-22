package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession

class LearningSessionProgressTest {
    @Test
    fun `known queue distinguishes processed reviewed skipped and remaining counts`() {
        val session = session().copy(newItemsReviewed = 1)
        val queue = StudyQueueProgress.from(
            StudyQueueSnapshot.create(
                session.id, Moment(1),
                listOf(item("one"), item("two"), item("three"), item("four"))
            ).advance().advance()
        )

        val progress = LearningSessionProgress.from(session, queue)

        assertEquals(2, progress.completedItemCount)
        assertEquals(1, progress.reviewedItemCount)
        assertEquals(1, progress.skippedItemCount)
        assertEquals(2, progress.remainingItemCount)
        assertEquals(3, progress.currentPosition)
        assertEquals(0.5, progress.fractionComplete)
        assertFalse(progress.isCompleted)
    }

    @Test
    fun `empty queue is known and deterministically completed`() {
        val session = session()
        val progress = LearningSessionProgress.from(
            session,
            StudyQueueProgress.from(StudyQueueSnapshot.create(session.id, Moment(1), emptyList()))
        )

        assertTrue(progress.totalIsKnown)
        assertTrue(progress.isEmpty)
        assertTrue(progress.isCompleted)
        assertEquals(0, progress.totalItemCount)
        assertEquals(1.0, progress.fractionComplete)
        assertNull(progress.currentPosition)
    }

    @Test
    fun `legacy session explicitly reports unknown denominator`() {
        val progress = LearningSessionProgress.unknown(session().copy(newItemsReviewed = 1))

        assertFalse(progress.totalIsKnown)
        assertNull(progress.totalItemCount)
        assertNull(progress.remainingItemCount)
        assertNull(progress.fractionComplete)
        assertEquals(1, progress.reviewedItemCount)
    }

    private fun session() = StudySession.start(
        SessionId("session"), LearnerId("learner"), Moment(0), SessionPolicy(10, 10)
    )

    private fun item(value: String) = LearningItemId(value)
}
