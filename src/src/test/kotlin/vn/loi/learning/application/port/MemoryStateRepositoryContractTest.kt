package vn.loi.learning.application.port

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository

class MemoryStateRepositoryContractTest {

    private val repository: MemoryStateRepository =
        InMemoryMemoryStateRepository()

    @Test
    fun `find returns null when state does not exist`() {
        val learnerId = LearnerId("learner-1")
        val learningItemId = LearningItemId("item-1")

        val result = repository.find(
            learnerId = learnerId,
            learningItemId = learningItemId
        )

        assertNull(result)
    }

    @Test
    fun `save stores memory state`() {
        val state = createNewState(
            learnerId = LearnerId("learner-1"),
            learningItemId = LearningItemId("item-1")
        )

        repository.save(state)

        val result = repository.find(
            learnerId = state.learnerId,
            learningItemId = state.learningItemId
        )

        assertEquals(state, result)
    }

    @Test
    fun `save replaces existing state with same learner and learning item`() {
        val learnerId = LearnerId("learner-1")
        val learningItemId = LearningItemId("item-1")

        val initialState = createNewState(
            learnerId = learnerId,
            learningItemId = learningItemId
        )

        val reviewedState = MemoryState(
            learnerId = learnerId,
            learningItemId = learningItemId,
            stage = LearningStage.REVIEW,
            difficulty = 4.5,
            stabilityDays = 3.0,
            dueAt = Moment(2_000L),
            lastReviewedAt = Moment(1_000L),
            reviewCount = 1,
            lapseCount = 0
        )

        repository.save(initialState)
        repository.save(reviewedState)

        val result = repository.find(
            learnerId = learnerId,
            learningItemId = learningItemId
        )

        assertEquals(reviewedState, result)
    }

    @Test
    fun `states of different learners do not overwrite each other`() {
        val learningItemId = LearningItemId("item-1")

        val learnerOneState = createNewState(
            learnerId = LearnerId("learner-1"),
            learningItemId = learningItemId
        )

        val learnerTwoState = createNewState(
            learnerId = LearnerId("learner-2"),
            learningItemId = learningItemId
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

        assertEquals(learnerOneState, learnerOneResult)
        assertEquals(learnerTwoState, learnerTwoResult)
    }

    private fun createNewState(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): MemoryState =
        MemoryState.new(
            learnerId = learnerId,
            learningItemId = learningItemId,
            availableAt = Moment(0L)
        )
}
