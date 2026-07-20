package vn.loi.learning.domain.study.fsrs.calibration.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.testing.fixtures.ReviewFixtures

class ReviewHistoryDatasetTest {

    private val learnerId =
        LearnerId("learner-1")

    @Test
    fun `dataset preserves valid ordered review events`() {
        val first =
            ReviewFixtures.event(
                id = ReviewEventId("review-1"),
                learnerId = learnerId,
                reviewedAt = Moment(1_000L)
            )

        val second =
            ReviewFixtures.event(
                id = ReviewEventId("review-2"),
                learnerId = learnerId,
                reviewedAt = Moment(2_000L),
                previousReviewCount = 1
            )

        val dataset =
            ReviewHistoryDataset(
                learnerId = learnerId,
                events = listOf(first, second)
            )

        assertEquals(
            listOf(first, second),
            dataset.events
        )

        assertEquals(
            2,
            dataset.totalReviews
        )

        assertFalse(dataset.isEmpty)

        assertEquals(
            Moment(1_000L),
            dataset.startedAt
        )

        assertEquals(
            Moment(2_000L),
            dataset.endedAt
        )
    }

    @Test
    fun `empty dataset has no review time range`() {
        val dataset =
            ReviewHistoryDataset.empty(
                learnerId
            )

        assertTrue(dataset.isEmpty)

        assertEquals(
            0,
            dataset.totalReviews
        )

        assertNull(dataset.startedAt)
        assertNull(dataset.endedAt)
    }

    @Test
    fun `dataset rejects event belonging to another learner`() {
        val event =
            ReviewFixtures.event(
                learnerId =
                    LearnerId("another-learner")
            )

        assertFailsWith<IllegalArgumentException> {
            ReviewHistoryDataset(
                learnerId = learnerId,
                events = listOf(event)
            )
        }
    }

    @Test
    fun `dataset rejects duplicate review event IDs`() {
        val first =
            ReviewFixtures.event(
                id = ReviewEventId("duplicate-review"),
                learnerId = learnerId,
                reviewedAt = Moment(1_000L)
            )

        val second =
            ReviewFixtures.event(
                id = ReviewEventId("duplicate-review"),
                learnerId = learnerId,
                reviewedAt = Moment(2_000L),
                previousReviewCount = 1
            )

        assertFailsWith<IllegalArgumentException> {
            ReviewHistoryDataset(
                learnerId = learnerId,
                events = listOf(first, second)
            )
        }
    }

    @Test
    fun `dataset rejects events outside chronological order`() {
        val later =
            ReviewFixtures.event(
                id = ReviewEventId("review-later"),
                learnerId = learnerId,
                reviewedAt = Moment(2_000L)
            )

        val earlier =
            ReviewFixtures.event(
                id = ReviewEventId("review-earlier"),
                learnerId = learnerId,
                reviewedAt = Moment(1_000L)
            )

        assertFailsWith<IllegalArgumentException> {
            ReviewHistoryDataset(
                learnerId = learnerId,
                events = listOf(later, earlier)
            )
        }
    }

    @Test
    fun `from keeps only events belonging to requested learner`() {
        val matching =
            ReviewFixtures.event(
                id = ReviewEventId("matching-review"),
                learnerId = learnerId
            )

        val anotherLearner =
            ReviewFixtures.event(
                id = ReviewEventId("another-review"),
                learnerId =
                    LearnerId("learner-2")
            )

        val dataset =
            ReviewHistoryDataset.from(
                learnerId = learnerId,
                events =
                    listOf(
                        anotherLearner,
                        matching
                    )
            )

        assertEquals(
            listOf(matching),
            dataset.events
        )
    }

    @Test
    fun `from sorts matching events chronologically`() {
        val later =
            ReviewFixtures.event(
                id = ReviewEventId("later-review"),
                learnerId = learnerId,
                reviewedAt = Moment(2_000L),
                previousReviewCount = 1
            )

        val earlier =
            ReviewFixtures.event(
                id = ReviewEventId("earlier-review"),
                learnerId = learnerId,
                reviewedAt = Moment(1_000L)
            )

        val dataset =
            ReviewHistoryDataset.from(
                learnerId = learnerId,
                events = listOf(later, earlier)
            )

        assertEquals(
            listOf(earlier, later),
            dataset.events
        )
    }

    @Test
    fun `from returns empty dataset when learner has no events`() {
        val event =
            ReviewFixtures.event(
                learnerId =
                    LearnerId("another-learner")
            )

        val dataset =
            ReviewHistoryDataset.from(
                learnerId = learnerId,
                events = listOf(event)
            )

        assertTrue(dataset.isEmpty)
    }

    @Test
    fun `from rejects duplicate event IDs for requested learner`() {
        val first =
            ReviewFixtures.event(
                id = ReviewEventId("duplicate-review"),
                learnerId = learnerId,
                reviewedAt = Moment(1_000L)
            )

        val second =
            ReviewFixtures.event(
                id = ReviewEventId("duplicate-review"),
                learnerId = learnerId,
                reviewedAt = Moment(2_000L),
                previousReviewCount = 1
            )

        assertFailsWith<IllegalArgumentException> {
            ReviewHistoryDataset.from(
                learnerId = learnerId,
                events = listOf(first, second)
            )
        }
    }

    @Test
    fun `eventsForLearningItem returns only matching reviews`() {
        val firstItemId =
            LearningItemId("item-1")

        val secondItemId =
            LearningItemId("item-2")

        val firstItemReview =
            ReviewFixtures.event(
                id = ReviewEventId("review-item-1"),
                learnerId = learnerId,
                learningItemId = firstItemId,
                reviewedAt = Moment(1_000L)
            )

        val secondItemReview =
            ReviewFixtures.event(
                id = ReviewEventId("review-item-2"),
                learnerId = learnerId,
                learningItemId = secondItemId,
                reviewedAt = Moment(2_000L)
            )

        val dataset =
            ReviewHistoryDataset(
                learnerId = learnerId,
                events =
                    listOf(
                        firstItemReview,
                        secondItemReview
                    )
            )

        assertEquals(
            listOf(firstItemReview),
            dataset.eventsForLearningItem(
                firstItemId
            )
        )
    }
}