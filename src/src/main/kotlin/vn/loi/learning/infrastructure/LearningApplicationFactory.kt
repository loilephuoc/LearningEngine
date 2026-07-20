package vn.loi.learning.infrastructure

import vn.loi.learning.application.LearningEngine
import vn.loi.learning.domain.study.memory.FsrsForgettingCurve
import vn.loi.learning.domain.study.scheduling.FsrsScheduler
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

/**
 * Composition root cấp ứng dụng cho cấu hình in-memory.
 *
 * Factory này tạo:
 * - command-side entry point: LearningEngine;
 * - query-side entry point: LearningDashboardQueryService;
 * - các repository dùng chung giữa command side và query side.
 *
 * Nhờ dùng chung repository instance:
 * - review được ghi qua LearningEngine;
 * - dashboard đọc được ngay MemoryState và ReviewEvent vừa cập nhật;
 * - UI hoặc adapter bên ngoài không cần tự lắp ráp dependency.
 *
 * Factory không chứa business logic.
 */
object LearningApplicationFactory {

    fun createInMemory(): LearningApplicationContext {
        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val memoryStateRepository =
            InMemoryMemoryStateRepository()

        val reviewEventRepository =
            InMemoryReviewEventRepository()

        val studySessionRepository =
            InMemoryStudySessionRepository()

        val transactionRunner =
            InMemoryTransactionRunner()

        val engine =
            LearningEngine(
                contentRepository =
                    contentRepository,
                learningItemRepository =
                    learningItemRepository,
                memoryStateRepository =
                    memoryStateRepository,
                reviewEventRepository =
                    reviewEventRepository,
                sessionRepository =
                    studySessionRepository,
                transactionRunner =
                    transactionRunner,
                scheduler =
                    FsrsScheduler()
            )

        val dashboard =
            LearningDashboardQueryServiceFactory.create(
                memoryStateQuery =
                    memoryStateRepository,
                reviewEventRepository =
                    reviewEventRepository,
                forgettingCurve =
                    FsrsForgettingCurve()
            )

        return LearningApplicationContext(
            engine = engine,
            dashboard = dashboard
        )
    }
}




