package vn.loi.learning.application.packageprogress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

class StudyHeaderStatisticsQueryServiceTest {
    private val learner = LearnerId("learner")

    @Test
    fun `raw inventory and unseen items are excluded from Total`() {
        val raw = (1..4_950).map { LearningItemId("raw-$it") }.toSet()
        val result = project(raw, emptyMap(), emptyList(), source(remaining = raw))
        assertEquals(0, result.total)
        assertEquals(0, result.againCount + result.hardCount + result.goodCount + result.easyCount)
        assertEquals(20, result.newEffectiveWorkload)
        assertEquals(0, result.newCompleted)
    }

    @Test
    fun `Total equals latest buckets and rerating changes only bucket`() {
        val item = LearningItemId("item")
        val good = event(item, ReviewRating.GOOD, 50, 60)
        val hard = event(item, ReviewRating.HARD, 70, 200, 1)
        val result = project(
            setOf(item), mapOf(item to hard.stateAfter), listOf(good, hard), source()
        )
        assertEquals(1, result.total)
        assertEquals(0, result.goodCount)
        assertEquals(1, result.hardCount)
    }

    @Test
    fun `New completed uses session counter and repeated review does not increment it`() {
        val first = LearningItemId("first")
        val second = LearningItemId("second")
        val firstEvent = event(first, ReviewRating.GOOD, 50, 200)
        val secondEvent = event(second, ReviewRating.EASY, 60, 200)
        val one = project(
            setOf(first, second), mapOf(first to firstEvent.stateAfter),
            listOf(firstEvent), source(newCompleted = 1, remaining = setOf(second))
        )
        assertEquals(1, one.newCompleted)
        assertEquals(20, one.newConfiguredTarget)
        val two = project(
            setOf(first, second), mapOf(first to firstEvent.stateAfter, second to secondEvent.stateAfter),
            listOf(firstEvent, secondEvent), source(newCompleted = 2)
        )
        assertEquals(2, two.newCompleted)
        val rereview = project(
            setOf(first, second), mapOf(first to firstEvent.stateAfter, second to secondEvent.stateAfter),
            listOf(firstEvent, secondEvent, event(first, ReviewRating.HARD, 70, 300, 1)),
            source(newCompleted = 2)
        )
        assertEquals(2, rereview.newCompleted)
    }

    @Test
    fun `Undo restores New and learned bucket from authoritative session and event state`() {
        val item = LearningItemId("new")
        val reviewed = event(item, ReviewRating.GOOD, 50, 200)
        val after = project(
            setOf(item), mapOf(item to reviewed.stateAfter), listOf(reviewed),
            source(newCompleted = 1)
        )
        assertEquals(1, after.total)
        assertEquals(1, after.newCompleted)
        val undone = project(
            setOf(item), emptyMap(), emptyList(), source(remaining = setOf(item))
        )
        assertEquals(0, undone.total)
        assertEquals(0, undone.newCompleted)
    }

    @Test
    fun `Review remaining comes from planned identities and is capped independently of Due`() {
        val reviewItems = (1..180).map { LearningItemId("review-$it") }
        val events = reviewItems.mapIndexed { index, id ->
            event(id, ReviewRating.GOOD, index.toLong() + 1, 0)
        }
        val states = events.associate { it.learningItemId to it.stateAfter }
        val capped = project(
            reviewItems.toSet(), states, events,
            source(remaining = reviewItems.take(100).toSet())
        )
        assertEquals(100, capped.reviewRemaining)
        assertEquals(100, capped.reviewEffectiveWorkload)
        assertEquals(100, capped.reviewConfiguredTarget)
        assertEquals(180, capped.dueCount)

        val available45 = reviewItems.take(45).toSet()
        val fortyFive = project(
            reviewItems.toSet(), states, events,
            source(remaining = available45)
        )
        assertEquals(45, fortyFive.reviewRemaining)
        assertEquals(45, fortyFive.reviewEffectiveWorkload)
        assertEquals(100, fortyFive.reviewConfiguredTarget)
    }

    @Test
    fun `review completion and Undo use exact queue remaining identities`() {
        val first = LearningItemId("review-1")
        val second = LearningItemId("review-2")
        val events = listOf(event(first, ReviewRating.AGAIN, 1, 200), event(second, ReviewRating.EASY, 2, 200))
        val states = events.associate { it.learningItemId to it.stateAfter }
        val started = project(setOf(first, second), states, events, source(remaining = setOf(first, second)))
        assertEquals(2, started.reviewRemaining)
        val completed = project(
            setOf(first, second), states, events,
            source(reviewCompleted = 1, remaining = setOf(second))
        )
        assertEquals(1, completed.reviewRemaining)
        assertEquals(2, completed.reviewEffectiveWorkload)
        val undone = project(setOf(first, second), states, events, source(remaining = setOf(first, second)))
        assertEquals(2, undone.reviewRemaining)
    }

    @Test
    fun `Due uses injected time semantics and nearest transition independently`() {
        val due = LearningItemId("due")
        val future = LearningItemId("future")
        val dueEvent = event(due, ReviewRating.AGAIN, 20, 90)
        val futureEvent = event(future, ReviewRating.EASY, 30, 150)
        val states = mapOf(due to dueEvent.stateAfter, future to futureEvent.stateAfter)
        val result = project(
            setOf(due, future), states, listOf(dueEvent, futureEvent),
            source(remaining = setOf(due)), Moment(100)
        )
        assertEquals(1, result.dueCount)
        assertEquals(1, result.reviewRemaining)
        assertEquals(Moment(150), result.nearestFutureDueAt)
        val advanced = project(
            setOf(due, future), states, listOf(dueEvent, futureEvent),
            source(remaining = setOf(due)), Moment(150)
        )
        assertEquals(2, advanced.dueCount)
        assertNull(advanced.nearestFutureDueAt)
    }

    @Test
    fun `scope and duplicate history preserve unique latest identity`() {
        val inside = LearningItemId("inside")
        val outside = LearningItemId("outside")
        val insideEvent = event(inside, ReviewRating.EASY, 50, 200)
        val outsideEvent = event(outside, ReviewRating.AGAIN, 50, 60)
        val result = project(
            setOf(inside),
            mapOf(inside to insideEvent.stateAfter, outside to outsideEvent.stateAfter),
            listOf(insideEvent, insideEvent, outsideEvent),
            source()
        )
        assertEquals(1, result.total)
        assertEquals(1, result.easyCount)
        assertEquals(0, result.againCount)
    }

    @Test
    fun `learned Content history overrides stale New admission while explicit Review remains`() {
        val admittedNewWithHistory = LearningItemId("admitted-new")
        val admittedReviewWithoutHistory = LearningItemId("admitted-review")
        val prior = event(admittedNewWithHistory, ReviewRating.GOOD, 50, 200)
        val result = project(
            setOf(admittedNewWithHistory, admittedReviewWithoutHistory),
            mapOf(admittedNewWithHistory to prior.stateAfter),
            listOf(prior),
            source(
                remaining = setOf(admittedNewWithHistory, admittedReviewWithoutHistory),
                origins = mapOf(
                    admittedNewWithHistory to SessionItemOrigin.NEW,
                    admittedReviewWithoutHistory to SessionItemOrigin.REVIEW
                )
            )
        )

        assertEquals(0, result.newEffectiveWorkload)
        assertEquals(2, result.reviewRemaining)
        assertEquals(2, result.reviewEffectiveWorkload)
    }

    @Test
    fun `Total and latest bucket aggregate sibling LearningItems by Content`() {
        val content = ContentId("television")
        val itemA = LearningItemId("television-meaning")
        val itemB = LearningItemId("television-listening")
        val olderGood = event(itemA, ReviewRating.GOOD, 50, 200)
        val newerHard = event(itemB, ReviewRating.HARD, 70, 200)
        val result = projectStudyHeaderStatistics(
            "scope",
            Moment(100),
            setOf(itemA, itemB),
            mapOf(itemA to olderGood.stateAfter, itemB to newerHard.stateAfter),
            listOf(olderGood, newerHard),
            source(),
            mapOf(itemA to content, itemB to content)
        )

        assertEquals(1, result.total)
        assertEquals(0, result.goodCount)
        assertEquals(1, result.hardCount)
        assertEquals(result.total, result.againCount + result.hardCount +
            result.goodCount + result.easyCount)
    }

    @Test
    fun `Review remaining counts sibling queue entries once per Content`() {
        val content = ContentId("television")
        val itemA = LearningItemId("television-meaning")
        val itemB = LearningItemId("television-listening")
        val result = projectStudyHeaderStatistics(
            "scope",
            Moment(100),
            setOf(itemA, itemB),
            emptyMap(),
            emptyList(),
            source(
                remaining = setOf(itemA, itemB),
                origins = mapOf(
                    itemA to SessionItemOrigin.REVIEW,
                    itemB to SessionItemOrigin.REVIEW
                ),
                contentIds = mapOf(itemA to content, itemB to content)
            ),
            mapOf(itemA to content, itemB to content)
        )

        assertEquals(1, result.reviewRemaining)
        assertEquals(1, result.reviewEffectiveWorkload)
    }

    private fun project(
        items: Set<LearningItemId>,
        states: Map<LearningItemId, MemoryState>,
        events: List<ReviewEvent>,
        source: StudySessionProgressSource,
        at: Moment = Moment(100)
    ) = projectStudyHeaderStatistics("scope", at, items, states, events, source)

    private fun source(
        newCompleted: Int = 0,
        reviewCompleted: Int = 0,
        remaining: Set<LearningItemId> = emptySet(),
        origins: Map<LearningItemId, SessionItemOrigin> = emptyMap(),
        contentIds: Map<LearningItemId, ContentId> = emptyMap()
    ) = StudySessionProgressSource(
        "session", 20, 100, newCompleted, reviewCompleted, remaining, origins, contentIds
    )

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
            stage = LearningStage.REVIEW, dueAt = Moment(dueAt),
            lastReviewedAt = Moment(reviewedAt), reviewCount = reviewCountBefore + 1
        )
        return ReviewEvent(
            ReviewEventId("${item.value}-$reviewedAt"), rating, Moment(reviewedAt), null, before, after
        )
    }
}
