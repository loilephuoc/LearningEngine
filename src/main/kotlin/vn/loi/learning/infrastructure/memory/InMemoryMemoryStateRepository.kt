package vn.loi.learning.infrastructure.memory

import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState

class InMemoryMemoryStateRepository : MemoryStateRepository {

    private val states = linkedMapOf<Key, MemoryState>()

    override fun find(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): MemoryState? =
        states[Key(learnerId, learningItemId)]

    override fun save(memoryState: MemoryState) {
        states[
            Key(
                learnerId = memoryState.learnerId,
                learningItemId = memoryState.learningItemId
            )
        ] = memoryState
    }

    fun count(): Int = states.size

    fun clear() {
        states.clear()
    }

    private data class Key(
        val learnerId: LearnerId,
        val learningItemId: LearningItemId
    )
}