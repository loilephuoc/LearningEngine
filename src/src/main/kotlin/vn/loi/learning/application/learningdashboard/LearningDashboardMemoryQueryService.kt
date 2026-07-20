package vn.loi.learning.application.learningdashboard

import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.progress.LearningStageCountsCalculator
import vn.loi.learning.domain.study.memory.model.LearnerId

/**
 * Application service tạo Memory section
 * của Learning Dashboard.
 *
 * Service này:
 * - đọc toàn bộ MemoryState của learner
 *   thông qua MemoryStateQuery;
 * - tái sử dụng LearningStageCountsCalculator;
 * - bọc kết quả thành LearningDashboardMemorySnapshot.
 *
 * Service không:
 * - truy cập persistence implementation trực tiếp;
 * - tự nhóm MemoryState theo stage;
 * - thay đổi MemoryState;
 * - đọc đồng hồ hệ thống;
 * - thực hiện scheduling.
 */
class LearningDashboardMemoryQueryService(
    private val memoryStateQuery: MemoryStateQuery,
    private val learningStageCountsCalculator:
    LearningStageCountsCalculator
) {

    fun query(
        learnerId: LearnerId
    ): LearningDashboardMemorySnapshot {
        val memoryStates =
            memoryStateQuery.findAll(
                learnerId = learnerId
            )

        return LearningDashboardMemorySnapshot(
            stageCounts =
                learningStageCountsCalculator.calculate(
                    memoryStates = memoryStates
                )
        )
    }
}