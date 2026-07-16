package vn.loi.learning.application.port

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState

interface MemoryStateRepository {

    fun find(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): MemoryState?

    fun save(memoryState: MemoryState)
}