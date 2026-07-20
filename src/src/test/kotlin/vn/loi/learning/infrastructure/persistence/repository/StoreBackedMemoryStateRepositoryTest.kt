package vn.loi.learning.infrastructure.persistence.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.persistence.store.InMemoryMemoryStateStore
import vn.loi.learning.testing.fixtures.MemoryFixtures

class StoreBackedMemoryStateRepositoryTest {

    private val repository: MemoryStateRepository =
        StoreBackedMemoryStateRepository(
            store = InMemoryMemoryStateStore()
        )

    @Test
    fun `find returns null when state does not exist`() {
        val result = repository.find(
            learnerId = LearnerId("learner-1"),
            learningItemId = LearningItemId("item-1")
        )

        assertNull(result)
    }

    @Test
    fun `save stores memory state`() {
        val state = MemoryFixtures.newState(
            learnerId = LearnerId("learner-1"),
            learningItemId = LearningItemId("item-1"),
            availableAt = Moment(1_000L)
        )

        repository.save(state)

        val result = repository.find(
            learnerId = state.learnerId,
            learningItemId = state.learningItemId
        )

        assertEquals(
            expected = state,
            actual = result
        )
    }

    @Test
    fun `save replaces existing state with same learner and learning item`() {
        val learnerId = LearnerId("learner-1")
        val learningItemId = LearningItemId("item-1")

        val initialState = MemoryFixtures.newState(
            learnerId = learnerId,
            learningItemId = learningItemId,
            availableAt = Moment(1_000L)
        )

        val reviewedState = MemoryState(
            learnerId = learnerId,
            learningItemId = learningItemId,
            stage = LearningStage.REVIEW,
            difficulty = 4.5,
            stabilityDays = 3.0,
            dueAt = Moment(2_000L),
            lastReviewedAt = Moment(1_500L),
            reviewCount = 1,
            lapseCount = 0
        )

        repository.save(initialState)
        repository.save(reviewedState)

        val result = repository.find(
            learnerId = learnerId,
            learningItemId = learningItemId
        )

        assertEquals(
            expected = reviewedState,
            actual = result
        )
    }

    @Test
    fun `states of different learners do not overwrite each other`() {
        val learningItemId = LearningItemId("item-1")

        val learnerOneState = MemoryFixtures.newState(
            learnerId = LearnerId("learner-1"),
            learningItemId = learningItemId,
            availableAt = Moment(1_000L)
        )

        val learnerTwoState = MemoryFixtures.newState(
            learnerId = LearnerId("learner-2"),
            learningItemId = learningItemId,
            availableAt = Moment(1_000L)
        )

        repository.save(learnerOneState)
        repository.save(learnerTwoState)

        val learnerOneResult = repository.find(
            learnerId = learnerOneState.learnerId,
            learningItemId = learningItemId
        )

        val learnerTwoResult = repository.find(
            learnerId = learnerTwoState.learnerId,
            learningItemId = learningItemId
        )

        assertEquals(
            expected = learnerOneState,
            actual = learnerOneResult
        )

        assertEquals(
            expected = learnerTwoState,
            actual = learnerTwoResult
        )
    }
}