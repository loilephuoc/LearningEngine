package vn.loi.learning.infrastructure

import vn.loi.learning.application.LearningEngine
import vn.loi.learning.domain.study.scheduling.SimpleScheduler
import vn.loi.learning.infrastructure.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.memory.InMemoryReviewEventRepository

/**
 * Factory tiện lợi để khởi tạo Engine chạy hoàn toàn trong bộ nhớ.
 *
 * Phiên bản này phù hợp cho:
 * - demo;
 * - test;
 * - CLI;
 * - phát triển ban đầu.
 */
object LearningEngineFactory {

    fun createInMemory(): LearningEngine =
        LearningEngine(
            memoryStateRepository = InMemoryMemoryStateRepository(),
            reviewEventRepository = InMemoryReviewEventRepository(),
            scheduler = SimpleScheduler()
        )
}