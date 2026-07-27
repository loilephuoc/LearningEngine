package vn.loi.learning.infrastructure.persistence.memory

import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState

class InMemoryMemoryStateRepository :
    MemoryStateRepository,
    MemoryStateQuery {

    private val states =
        linkedMapOf<Key, MemoryState>()

    override fun find(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): MemoryState? =
        states[
            Key(
                learnerId = learnerId,
                learningItemId = learningItemId
            )
        ]

    override fun findAll(
        learnerId: LearnerId
    ): List<MemoryState> =
        states.values
            .filter { memoryState ->
                memoryState.learnerId == learnerId
            }

    override fun save(
        memoryState: MemoryState
    ) {
        states[
            Key(
                learnerId = memoryState.learnerId,
                learningItemId =
                    memoryState.learningItemId
            )
        ] = memoryState
    }

    override fun delete(learnerId: LearnerId, learningItemId: LearningItemId) {
        states.remove(Key(learnerId, learningItemId))
    }

    override fun deleteByLearningItemIds(learningItemIds: Set<LearningItemId>) {
        val idSet = learningItemIds.map { it.toString() }.toSet()
        states.entries.removeIf { it.key.learningItemId in learningItemIds || it.key.learningItemId.toString() in idSet }
    }


    fun count(): Int =
        states.size

    fun clear() {
        states.clear()
    }

    private data class Key(
        val learnerId: LearnerId,
        val learningItemId: LearningItemId
    )
}
