package vn.loi.learning.application.reviewhistory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating

class ReviewHistoryQueryTest {

    @Test
    fun `query may target all review history of one learner`() {
        val learnerId =
            LearnerId("learner-1")

        val query =
            ReviewHistoryQuery(
                learnerId = learnerId
            )

        assertEquals(
            expected = learnerId,
            actual = query.learnerId
        )

        assertNull(query.learningItemId)
        assertNull(query.period)
        assertTrue(query.ratings.isEmpty())
    }

    @Test
    fun `query may target one learning item`() {
        val learningItemId =
            LearningItemId("item-1")

        val query =
            ReviewHistoryQuery(
                learnerId = LearnerId("learner-1"),
                learningItemId = learningItemId
            )

        assertEquals(
            expected = learningItemId,
            actual = query.learningItemId
        )
    }

    @Test
    fun `query may limit review history by period`() {
        val period =
            StudyPeriod(
                startInclusive = Moment(1_000L),
                endExclusive = Moment(2_000L)
            )

        val query =
            ReviewHistoryQuery(
                learnerId = LearnerId("learner-1"),
                period = period
            )

        assertEquals(
            expected = period,
            actual = query.period
        )
    }

    @Test
    fun `query may limit review history by ratings`() {
        val ratings =
            setOf(
                ReviewRating.AGAIN,
                ReviewRating.HARD
            )

        val query =
            ReviewHistoryQuery(
                learnerId = LearnerId("learner-1"),
                ratings = ratings
            )

        assertEquals(
            expected = ratings,
            actual = query.ratings
        )
    }

    @Test
    fun `query supports combined conditions`() {
        val learnerId =
            LearnerId("learner-1")

        val learningItemId =
            LearningItemId("item-1")

        val period =
            StudyPeriod(
                startInclusive = Moment(1_000L),
                endExclusive = Moment(2_000L)
            )

        val ratings =
            setOf(
                ReviewRating.GOOD,
                ReviewRating.EASY
            )

        val query =
            ReviewHistoryQuery(
                learnerId = learnerId,
                learningItemId = learningItemId,
                period = period,
                ratings = ratings
            )

        assertEquals(
            expected = learnerId,
            actual = query.learnerId
        )

        assertEquals(
            expected = learningItemId,
            actual = query.learningItemId
        )

        assertEquals(
            expected = period,
            actual = query.period
        )

        assertEquals(
            expected = ratings,
            actual = query.ratings
        )
    }
}