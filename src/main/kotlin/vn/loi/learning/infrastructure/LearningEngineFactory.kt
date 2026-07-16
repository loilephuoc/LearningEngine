package vn.loi.learning.infrastructure

import vn.loi.learning.application.LearningEngine
import vn.loi.learning.domain.study.scheduling.SimpleScheduler
import vn.loi.learning.infrastructure.content.InMemoryContentRepository
import vn.loi.learning.infrastructure.learning.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.memory.InMemoryReviewEventRepository

object LearningEngineFactory {

    fun createInMemory(): LearningEngine =
        LearningEngine(
            contentRepository = InMemoryContentRepository(),
            learningItemRepository =
                InMemoryLearningItemRepository(),
            memoryStateRepository =
                InMemoryMemoryStateRepository(),
            reviewEventRepository =
                InMemoryReviewEventRepository(),
            scheduler = SimpleScheduler()
        )
}