package vn.loi.learning.application.reviewhistory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.testing.fixtures.ReviewFixtures

class ReviewHistoryQueryServiceTest {

    private val repository =
        InMemoryReviewEventRepository()

    private val service =
        ReviewHistoryQueryService(
            reviewEventRepository = repository
        )

    @Test
    fun `query returns all review events of learner`() {
        val learnerId =
            LearnerId("learner-1")

        val first =
            ReviewFixtures.event(
                id = ReviewEventId("review-1"),
                learnerId = learnerId,
                learningItemId = LearningItemId("item-1"),
                reviewedAt = Moment(1_000L)
            )

        val second =
            ReviewFixtures.event(
                id = ReviewEventId("review-2"),
                learnerId = learnerId,
                learningItemId = LearningItemId("item-2"),
                reviewedAt = Moment(2_000L)
            )

        repository.append(first)
        repository.append(second)

        val result =
            service.query(
                ReviewHistoryQuery(
                    learnerId = learnerId
                )
            )

        assertEquals(
            expected = listOf(first, second),
            actual = result
        )
    }

    @Test
    fun `query returns empty list when learner has no review history`() {
        val result =
            service.query(
                ReviewHistoryQuery(
                    learnerId = LearnerId("unknown-learner")
                )
            )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `query isolates different learners`() {
        val learnerOneEvent =
            ReviewFixtures.event(
                id = ReviewEventId("review-learner-1"),
                learnerId = LearnerId("learner-1")
            )

        val learnerTwoEvent =
            ReviewFixtures.event(
                id = ReviewEventId("review-learner-2"),
                learnerId = LearnerId("learner-2")
            )

        repository.append(learnerOneEvent)
        repository.append(learnerTwoEvent)

        val result =
            service.query(
                ReviewHistoryQuery(
                    learnerId = learnerOneEvent.learnerId
                )
            )

        assertEquals(
            expected = listOf(learnerOneEvent),
            actual = result
        )
    }

    @Test
    fun `query may filter by learning item`() {
        val learnerId =
            LearnerId("learner-1")

        val selectedItemId =
            LearningItemId("item-1")

        val selected =
            ReviewFixtures.event(
                id = ReviewEventId("review-selected"),
                learnerId = learnerId,
                learningItemId = selectedItemId
            )

        val other =
            ReviewFixtures.event(
                id = ReviewEventId("review-other"),
                learnerId = learnerId,
                learningItemId = LearningItemId("item-2")
            )

        repository.append(selected)
        repository.append(other)

        val result =
            service.query(
                ReviewHistoryQuery(
                    learnerId = learnerId,
                    learningItemId = selectedItemId
                )
            )

        assertEquals(
            expected = listOf(selected),
            actual = result
        )
    }

    @Test
    fun `query may filter by a learning item scope including an empty scope`() {
        val learnerId = LearnerId("learner-scope")
        val first = ReviewFixtures.event(
            id = ReviewEventId("scope-first"), learnerId = learnerId,
            learningItemId = LearningItemId("item-1")
        )
        val second = ReviewFixtures.event(
            id = ReviewEventId("scope-second"), learnerId = learnerId,
            learningItemId = LearningItemId("item-2")
        )
        repository.append(first)
        repository.append(second)

        assertEquals(
            listOf(second),
            service.query(ReviewHistoryQuery(learnerId, learningItemIds = setOf(second.learningItemId)))
        )
        assertTrue(service.query(ReviewHistoryQuery(learnerId, learningItemIds = emptySet())).isEmpty())
    }

    @Test
    fun `query may filter by period`() {
        val learnerId =
            LearnerId("learner-1")

        val beforePeriod =
            ReviewFixtures.event(
                id = ReviewEventId("review-before"),
                learnerId = learnerId,
                reviewedAt = Moment(999L)
            )

        val atStart =
            ReviewFixtures.event(
                id = ReviewEventId("review-start"),
                learnerId = learnerId,
                reviewedAt = Moment(1_000L)
            )

        val insidePeriod =
            ReviewFixtures.event(
                id = ReviewEventId("review-inside"),
                learnerId = learnerId,
                reviewedAt = Moment(1_500L)
            )

        val atEnd =
            ReviewFixtures.event(
                id = ReviewEventId("review-end"),
                learnerId = learnerId,
                reviewedAt = Moment(2_000L)
            )

        repository.append(beforePeriod)
        repository.append(atStart)
        repository.append(insidePeriod)
        repository.append(atEnd)

        val result =
            service.query(
                ReviewHistoryQuery(
                    learnerId = learnerId,
                    period = StudyPeriod(
                        startInclusive = Moment(1_000L),
                        endExclusive = Moment(2_000L)
                    )
                )
            )

        assertEquals(
            expected = listOf(
                atStart,
                insidePeriod
            ),
            actual = result
        )
    }

    @Test
    fun `query may filter by ratings`() {
        val learnerId =
            LearnerId("learner-1")

        val again =
            ReviewFixtures.event(
                id = ReviewEventId("review-again"),
                learnerId = learnerId,
                rating = ReviewRating.AGAIN
            )

        val hard =
            ReviewFixtures.event(
                id = ReviewEventId("review-hard"),
                learnerId = learnerId,
                rating = ReviewRating.HARD
            )

        val good =
            ReviewFixtures.event(
                id = ReviewEventId("review-good"),
                learnerId = learnerId,
                rating = ReviewRating.GOOD
            )

        repository.append(again)
        repository.append(hard)
        repository.append(good)

        val result =
            service.query(
                ReviewHistoryQuery(
                    learnerId = learnerId,
                    ratings = setOf(
                        ReviewRating.AGAIN,
                        ReviewRating.HARD
                    )
                )
            )

        assertEquals(
            expected = listOf(
                again,
                hard
            ),
            actual = result
        )
    }

    @Test
    fun `empty ratings do not restrict result`() {
        val event =
            ReviewFixtures.event(
                rating = ReviewRating.EASY
            )

        repository.append(event)

        val result =
            service.query(
                ReviewHistoryQuery(
                    learnerId = event.learnerId,
                    ratings = emptySet()
                )
            )

        assertEquals(
            expected = listOf(event),
            actual = result
        )
    }

    @Test
    fun `query supports combined conditions`() {
        val learnerId =
            LearnerId("learner-1")

        val learningItemId =
            LearningItemId("item-1")

        val matching =
            ReviewFixtures.event(
                id = ReviewEventId("review-matching"),
                learnerId = learnerId,
                learningItemId = learningItemId,
                reviewedAt = Moment(1_500L),
                rating = ReviewRating.GOOD
            )

        val wrongRating =
            ReviewFixtures.event(
                id = ReviewEventId("review-wrong-rating"),
                learnerId = learnerId,
                learningItemId = learningItemId,
                reviewedAt = Moment(1_600L),
                rating = ReviewRating.AGAIN
            )

        val wrongItem =
            ReviewFixtures.event(
                id = ReviewEventId("review-wrong-item"),
                learnerId = learnerId,
                learningItemId = LearningItemId("item-2"),
                reviewedAt = Moment(1_700L),
                rating = ReviewRating.GOOD
            )

        val outsidePeriod =
            ReviewFixtures.event(
                id = ReviewEventId("review-outside-period"),
                learnerId = learnerId,
                learningItemId = learningItemId,
                reviewedAt = Moment(2_000L),
                rating = ReviewRating.GOOD
            )

        repository.append(matching)
        repository.append(wrongRating)
        repository.append(wrongItem)
        repository.append(outsidePeriod)

        val result =
            service.query(
                ReviewHistoryQuery(
                    learnerId = learnerId,
                    learningItemId = learningItemId,
                    period = StudyPeriod(
                        startInclusive = Moment(1_000L),
                        endExclusive = Moment(2_000L)
                    ),
                    ratings = setOf(
                        ReviewRating.GOOD
                    )
                )
            )

        assertEquals(
            expected = listOf(matching),
            actual = result
        )
    }

    @Test
    fun `query always returns chronological order`() {
        val learnerId =
            LearnerId("learner-1")

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
                reviewedAt = Moment(2_000L)
            )

        val third =
            ReviewFixtures.event(
                id = ReviewEventId("review-3"),
                learnerId = learnerId,
                reviewedAt = Moment(3_000L)
            )

        repository.append(third)
        repository.append(first)
        repository.append(second)

        val result =
            service.query(
                ReviewHistoryQuery(
                    learnerId = learnerId
                )
            )

        assertEquals(
            expected = listOf(
                first,
                second,
                third
            ),
            actual = result
        )
    }
}
