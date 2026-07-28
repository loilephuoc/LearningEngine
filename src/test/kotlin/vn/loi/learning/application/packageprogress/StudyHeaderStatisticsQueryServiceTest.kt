package vn.loi.learning.application.packageprogress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating

class StudyHeaderStatisticsQueryServiceTest {
    private val learner = LearnerId("learner")

    @Test
    fun `empty scope has zero counts`() {
        val result = projectStudyHeaderStatistics("empty", Moment(100), emptySet(), emptyMap(), emptyList())
        assertEquals(0, result.total)
        assertEquals(0, result.newCount)
        assertEquals(0, result.reviewCount)
        assertNull(result.nearestFutureDueAt)
    }

    @Test
    fun `unique eligible items partition into new and reviewed`() {
        val newItem = LearningItemId("new")
        val reviewed = LearningItemId("reviewed")
        val event = event(reviewed, ReviewRating.GOOD, 50, 200)
        val result = projectStudyHeaderStatistics(
            "scope",
            Moment(100),
            setOf(newItem, reviewed),
            mapOf(reviewed to event.stateAfter),
            listOf(event)
        )
        assertEquals(2, result.total)
        assertEquals(1, result.newCount)
        assertEquals(1, result.reviewCount)
        assertEquals(1, result.goodCount)
    }

    @Test
    fun `latest effective rating replaces prior bucket without double count`() {
        val item = LearningItemId("item")
        val good = event(item, ReviewRating.GOOD, 50, 60)
        val hard = event(item, ReviewRating.HARD, 70, 200, reviewCountBefore = 1)
        val result = projectStudyHeaderStatistics(
            "scope", Moment(100), setOf(item), mapOf(item to hard.stateAfter), listOf(good, hard)
        )
        assertEquals(0, result.goodCount)
        assertEquals(1, result.hardCount)
        assertEquals(1, result.reviewCount)
    }

    @Test
    fun `undo projection restores previous bucket and undo first review restores new`() {
        val item = LearningItemId("item")
        val good = event(item, ReviewRating.GOOD, 50, 60)
        val hard = event(item, ReviewRating.HARD, 70, 200, reviewCountBefore = 1)
        val restored = projectStudyHeaderStatistics(
            "scope", Moment(100), setOf(item), mapOf(item to good.stateAfter), listOf(good)
        )
        assertEquals(1, restored.goodCount)
        assertEquals(0, restored.hardCount)

        val newAgain = projectStudyHeaderStatistics(
            "scope", Moment(100), setOf(item), emptyMap(), emptyList()
        )
        assertEquals(1, newAgain.newCount)
        assertEquals(0, newAgain.reviewCount)
    }

    @Test
    fun `due uses memory authority and exposes nearest one shot transition`() {
        val due = LearningItemId("due")
        val future = LearningItemId("future")
        val dueEvent = event(due, ReviewRating.AGAIN, 20, 90)
        val futureEvent = event(future, ReviewRating.EASY, 30, 150)
        val result = projectStudyHeaderStatistics(
            "scope",
            Moment(100),
            setOf(due, future),
            mapOf(due to dueEvent.stateAfter, future to futureEvent.stateAfter),
            listOf(dueEvent, futureEvent)
        )
        assertEquals(1, result.dueCount)
        assertEquals(Moment(150), result.nearestFutureDueAt)

        val advanced = projectStudyHeaderStatistics(
            "scope",
            Moment(150),
            setOf(due, future),
            mapOf(due to dueEvent.stateAfter, future to futureEvent.stateAfter),
            listOf(dueEvent, futureEvent)
        )
        assertEquals(2, advanced.dueCount)
        assertNull(advanced.nearestFutureDueAt)
    }

    @Test
    fun `scope excludes items and duplicate history outside current source`() {
        val inside = LearningItemId("inside")
        val outside = LearningItemId("outside")
        val insideEvent = event(inside, ReviewRating.EASY, 50, 200)
        val outsideEvent = event(outside, ReviewRating.AGAIN, 50, 60)
        val result = projectStudyHeaderStatistics(
            "package-a", Moment(100), setOf(inside),
            mapOf(inside to insideEvent.stateAfter, outside to outsideEvent.stateAfter),
            listOf(insideEvent, insideEvent, outsideEvent)
        )
        assertEquals(1, result.total)
        assertEquals(1, result.easyCount)
        assertEquals(0, result.againCount)
    }

    private fun event(
        item: LearningItemId,
        rating: ReviewRating,
        reviewedAt: Long,
        dueAt: Long,
        reviewCountBefore: Int = 0
    ): ReviewEvent {
        val before = MemoryState(
            learner, item,
            if (reviewCountBefore == 0) LearningStage.NEW else LearningStage.REVIEW,
            5.0, 1.0, Moment(0),
            if (reviewCountBefore == 0) null else Moment(reviewedAt - 1),
            reviewCountBefore, 0
        )
        val after = before.copy(
            stage = LearningStage.REVIEW,
            dueAt = Moment(dueAt),
            lastReviewedAt = Moment(reviewedAt),
            reviewCount = reviewCountBefore + 1
        )
        return ReviewEvent(
            ReviewEventId("${item.value}-$reviewedAt"),
            rating,
            Moment(reviewedAt),
            null,
            before,
            after
        )
    }
}
