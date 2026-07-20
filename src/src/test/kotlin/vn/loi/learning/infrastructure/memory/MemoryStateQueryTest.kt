package vn.loi.learning.infrastructure.memory

import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

class MemoryStateQueryTest {

    private val repository =
        InMemoryMemoryStateRepository()

    private val query: MemoryStateQuery =
        repository

    @Test
    fun `find all returns memories belonging to learner`() {
        val learnerId =
            LearnerId("learner-1")

        val first =
            memory(
                learnerId = learnerId,
                learningItemId =
                    LearningItemId("item-1"),
                availableAt = Moment(1_000L)
            )

        val second =
            memory(
                learnerId = learnerId,
                learningItemId =
                    LearningItemId("item-2"),
                availableAt = Moment(2_000L)
            )

        repository.save(first)
        repository.save(second)

        assertEquals(
            expected = listOf(first, second),
            actual = query.findAll(learnerId)
        )
    }

    @Test
    fun `find all isolates memories of different learners`() {
        val learnerOne =
            LearnerId("learner-1")

        val learnerTwo =
            LearnerId("learner-2")

        val learnerOneMemory =
            memory(
                learnerId = learnerOne,
                learningItemId =
                    LearningItemId("item-1"),
                availableAt = Moment(1_000L)
            )

        val learnerTwoMemory =
            memory(
                learnerId = learnerTwo,
                learningItemId =
                    LearningItemId("item-2"),
                availableAt = Moment(1_000L)
            )

        repository.save(learnerOneMemory)
        repository.save(learnerTwoMemory)

        assertEquals(
            expected =
                listOf(learnerOneMemory),
            actual =
                query.findAll(learnerOne)
        )

        assertEquals(
            expected =
                listOf(learnerTwoMemory),
            actual =
                query.findAll(learnerTwo)
        )
    }

    @Test
    fun `find all returns empty list for unknown learner`() {
        repository.save(
            memory(
                learnerId =
                    LearnerId("learner-1"),
                learningItemId =
                    LearningItemId("item-1"),
                availableAt =
                    Moment(1_000L)
            )
        )

        val result =
            query.findAll(
                LearnerId("unknown-learner")
            )

        assertTrue(
            actual = result.isEmpty()
        )
    }

    @Test
    fun `find all returns latest saved aggregate`() {
        val learnerId =
            LearnerId("learner-1")

        val learningItemId =
            LearningItemId("item-1")

        repository.save(
            memory(
                learnerId = learnerId,
                learningItemId = learningItemId,
                availableAt = Moment(1_000L)
            )
        )

        val latest =
            memory(
                learnerId = learnerId,
                learningItemId = learningItemId,
                availableAt = Moment(2_000L)
            )

        repository.save(latest)

        assertEquals(
            expected = listOf(latest),
            actual = query.findAll(learnerId)
        )
    }

    private fun memory(
        learnerId: LearnerId,
        learningItemId: LearningItemId,
        availableAt: Moment
    ): MemoryState =
        MemoryState.new(
            learnerId = learnerId,
            learningItemId = learningItemId,
            availableAt = availableAt
        )
}
