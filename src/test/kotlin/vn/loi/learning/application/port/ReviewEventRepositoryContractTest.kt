package vn.loi.learning.application.port

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedReviewEventRepository
import vn.loi.learning.infrastructure.persistence.store.InMemoryReviewEventStore
import vn.loi.learning.testing.fixtures.ReviewFixtures

class ReviewEventRepositoryContractTest {

    private val repository: ReviewEventRepository =
        StoreBackedReviewEventRepository(
            store = InMemoryReviewEventStore()
        )

    @Test
    fun `findAll returns empty list when no events exist`() {
        val events =
            repository.findAll(
                learnerId = LearnerId("learner-1"),
                learningItemId = LearningItemId("item-1")
            )

        assertTrue(events.isEmpty())
    }

    @Test
    fun `findAll by learner returns empty list when no events exist`() {
        val events =
            repository.findAll(
                learnerId = LearnerId("learner-1")
            )

        assertTrue(events.isEmpty())
    }

    @Test
    fun `append stores review event`() {
        val event =
            ReviewFixtures.event()

        repository.append(event)

        val events =
            repository.findAll(
                learnerId = event.learnerId,
                learningItemId = event.learningItemId
            )

        assertEquals(
            expected = listOf(event),
            actual = events
        )
    }

    @Test
    fun `duplicate review event id is rejected`() {
        val event =
            ReviewFixtures.event()

        repository.append(event)

        assertFailsWith<IllegalArgumentException> {
            repository.append(event)
        }
    }

    @Test
    fun `findAll preserves chronological order`() {
        val learnerId =
            LearnerId("learner-1")

        val learningItemId =
            LearningItemId("item-1")

        val first =
            ReviewFixtures.event(
                id = ReviewEventId("review-1"),
                learnerId = learnerId,
                learningItemId = learningItemId,
                reviewedAt = Moment(1_000L),
                previousReviewCount = 0
            )

        val second =
            ReviewFixtures.event(
                id = ReviewEventId("review-2"),
                learnerId = learnerId,
                learningItemId = learningItemId,
                reviewedAt = Moment(2_000L),
                previousReviewCount = 1
            )

        val third =
            ReviewFixtures.event(
                id = ReviewEventId("review-3"),
                learnerId = learnerId,
                learningItemId = learningItemId,
                reviewedAt = Moment(3_000L),
                previousReviewCount = 2
            )

        repository.append(third)
        repository.append(first)
        repository.append(second)

        val events =
            repository.findAll(
                learnerId = learnerId,
                learningItemId = learningItemId
            )

        assertEquals(
            expected = listOf(first, second, third),
            actual = events
        )
    }

    @Test
    fun `findAll by learner returns events across learning items`() {
        val learnerId =
            LearnerId("learner-1")

        val firstItemEvent =
            ReviewFixtures.event(
                id = ReviewEventId("review-item-1"),
                learnerId = learnerId,
                learningItemId = LearningItemId("item-1"),
                reviewedAt = Moment(1_000L)
            )

        val secondItemEvent =
            ReviewFixtures.event(
                id = ReviewEventId("review-item-2"),
                learnerId = learnerId,
                learningItemId = LearningItemId("item-2"),
                reviewedAt = Moment(2_000L)
            )

        repository.append(secondItemEvent)
        repository.append(firstItemEvent)

        val events =
            repository.findAll(
                learnerId = learnerId
            )

        assertEquals(
            expected = listOf(
                firstItemEvent,
                secondItemEvent
            ),
            actual = events
        )
    }

    @Test
    fun `findAll by learner isolates different learners`() {
        val learnerOneEvent =
            ReviewFixtures.event(
                id = ReviewEventId("review-learner-1"),
                learnerId = LearnerId("learner-1"),
                learningItemId = LearningItemId("item-1")
            )

        val learnerTwoEvent =
            ReviewFixtures.event(
                id = ReviewEventId("review-learner-2"),
                learnerId = LearnerId("learner-2"),
                learningItemId = LearningItemId("item-2")
            )

        repository.append(learnerOneEvent)
        repository.append(learnerTwoEvent)

        val events =
            repository.findAll(
                learnerId = learnerOneEvent.learnerId
            )

        assertEquals(
            expected = listOf(learnerOneEvent),
            actual = events
        )
    }

    @Test
    fun `events of different learners are isolated`() {
        val learningItemId =
            LearningItemId("item-1")

        val learnerOneEvent =
            ReviewFixtures.event(
                id = ReviewEventId("review-learner-1"),
                learnerId = LearnerId("learner-1"),
                learningItemId = learningItemId
            )

        val learnerTwoEvent =
            ReviewFixtures.event(
                id = ReviewEventId("review-learner-2"),
                learnerId = LearnerId("learner-2"),
                learningItemId = learningItemId
            )

        repository.append(learnerOneEvent)
        repository.append(learnerTwoEvent)

        val learnerOneEvents =
            repository.findAll(
                learnerId = learnerOneEvent.learnerId,
                learningItemId = learningItemId
            )

        val learnerTwoEvents =
            repository.findAll(
                learnerId = learnerTwoEvent.learnerId,
                learningItemId = learningItemId
            )

        assertEquals(
            expected = listOf(learnerOneEvent),
            actual = learnerOneEvents
        )

        assertEquals(
            expected = listOf(learnerTwoEvent),
            actual = learnerTwoEvents
        )
    }

    @Test
    fun `events of different learning items are isolated`() {
        val learnerId =
            LearnerId("learner-1")

        val firstItemEvent =
            ReviewFixtures.event(
                id = ReviewEventId("review-item-1"),
                learnerId = learnerId,
                learningItemId = LearningItemId("item-1")
            )

        val secondItemEvent =
            ReviewFixtures.event(
                id = ReviewEventId("review-item-2"),
                learnerId = learnerId,
                learningItemId = LearningItemId("item-2")
            )

        repository.append(firstItemEvent)
        repository.append(secondItemEvent)

        val firstItemEvents =
            repository.findAll(
                learnerId = learnerId,
                learningItemId =
                    firstItemEvent.learningItemId
            )

        val secondItemEvents =
            repository.findAll(
                learnerId = learnerId,
                learningItemId =
                    secondItemEvent.learningItemId
            )

        assertEquals(
            expected = listOf(firstItemEvent),
            actual = firstItemEvents
        )

        assertEquals(
            expected = listOf(secondItemEvent),
            actual = secondItemEvents
        )
    }
}