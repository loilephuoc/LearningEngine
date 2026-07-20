package vn.loi.learning.application.reviewhistory

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedReviewEventRepository
import vn.loi.learning.infrastructure.persistence.store.InMemoryReviewEventStore
import vn.loi.learning.testing.fixtures.ReviewFixtures

class ReviewHistoryQueryServicePersistenceTest {

    private val repository =
        StoreBackedReviewEventRepository(
            store = InMemoryReviewEventStore()
        )

    private val service =
        ReviewHistoryQueryService(
            reviewEventRepository = repository
        )

    @Test
    fun `query reads review history through persistence repository`() {
        val learnerId =
            LearnerId("learner-1")

        val first =
            ReviewFixtures.event(
                id = ReviewEventId("review-1"),
                learnerId = learnerId,
                learningItemId = LearningItemId("item-1"),
                reviewedAt = Moment(1_000L),
                rating = ReviewRating.AGAIN
            )

        val second =
            ReviewFixtures.event(
                id = ReviewEventId("review-2"),
                learnerId = learnerId,
                learningItemId = LearningItemId("item-2"),
                reviewedAt = Moment(2_000L),
                rating = ReviewRating.GOOD
            )

        repository.append(second)
        repository.append(first)

        val result =
            service.query(
                ReviewHistoryQuery(
                    learnerId = learnerId
                )
            )

        assertEquals(
            expected = listOf(
                first,
                second
            ),
            actual = result
        )
    }

    @Test
    fun `combined query conditions work through persistence repository`() {
        val learnerId =
            LearnerId("learner-1")

        val selectedItemId =
            LearningItemId("item-1")

        val matching =
            ReviewFixtures.event(
                id = ReviewEventId("matching-review"),
                learnerId = learnerId,
                learningItemId = selectedItemId,
                reviewedAt = Moment(1_500L),
                rating = ReviewRating.GOOD
            )

        val wrongRating =
            ReviewFixtures.event(
                id = ReviewEventId("wrong-rating"),
                learnerId = learnerId,
                learningItemId = selectedItemId,
                reviewedAt = Moment(1_600L),
                rating = ReviewRating.AGAIN
            )

        val wrongItem =
            ReviewFixtures.event(
                id = ReviewEventId("wrong-item"),
                learnerId = learnerId,
                learningItemId = LearningItemId("item-2"),
                reviewedAt = Moment(1_700L),
                rating = ReviewRating.GOOD
            )

        val outsidePeriod =
            ReviewFixtures.event(
                id = ReviewEventId("outside-period"),
                learnerId = learnerId,
                learningItemId = selectedItemId,
                reviewedAt = Moment(2_000L),
                rating = ReviewRating.GOOD
            )

        val otherLearner =
            ReviewFixtures.event(
                id = ReviewEventId("other-learner"),
                learnerId = LearnerId("learner-2"),
                learningItemId = selectedItemId,
                reviewedAt = Moment(1_800L),
                rating = ReviewRating.GOOD
            )

        repository.append(wrongItem)
        repository.append(outsidePeriod)
        repository.append(matching)
        repository.append(otherLearner)
        repository.append(wrongRating)

        val result =
            service.query(
                ReviewHistoryQuery(
                    learnerId = learnerId,
                    learningItemId = selectedItemId,
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
}