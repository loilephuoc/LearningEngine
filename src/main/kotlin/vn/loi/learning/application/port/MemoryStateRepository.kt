package vn.loi.learning.application.port

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState

interface MemoryStateRepository {

    /** Read-only diagnostic snapshot; repositories without enumeration may return an empty list. */
    fun findAll(): List<MemoryState> = emptyList()

    fun find(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): MemoryState?

    fun save(memoryState: MemoryState)

    fun delete(learnerId: LearnerId, learningItemId: LearningItemId) {
        throw UnsupportedOperationException("MemoryState deletion is not supported by this repository.")
    }

    fun deleteByLearningItemIds(learningItemIds: Set<LearningItemId>) {}
}
