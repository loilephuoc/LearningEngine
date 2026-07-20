package vn.loi.learning.infrastructure

import vn.loi.learning.application.LearningEngine
import vn.loi.learning.domain.study.scheduling.FsrsScheduler
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

object LearningEngineFactory {

    fun createInMemory(): LearningEngine =
        LearningEngine(
            contentRepository =
                InMemoryContentRepository(),
            learningItemRepository =
                InMemoryLearningItemRepository(),
            memoryStateRepository =
                InMemoryMemoryStateRepository(),
            reviewEventRepository =
                InMemoryReviewEventRepository(),
            sessionRepository =
                InMemoryStudySessionRepository(),
            transactionRunner =
                InMemoryTransactionRunner(),
            scheduler =
                FsrsScheduler()
        )
}




