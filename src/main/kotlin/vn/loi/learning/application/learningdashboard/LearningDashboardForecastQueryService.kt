package vn.loi.learning.application.learningdashboard

import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.MemoryState

/**
 * Application service tạo Forecast section
 * của Learning Dashboard.
 *
 * Service này:
 * - đọc toàn bộ MemoryState của learner
 *   thông qua MemoryStateQuery;
 * - chuyển MemoryState, thời điểm bắt đầu và các mốc cửa sổ
 *   cho LearningDashboardForecastCalculator;
 * - bọc kết quả thành LearningDashboardForecastSnapshot.
 *
 * Service không:
 * - truy cập persistence implementation trực tiếp;
 * - tự tạo forecast bucket;
 * - tự đếm MemoryState theo cửa sổ;
 * - tự loại bỏ suspended hoặc already-due memories;
 * - thay đổi MemoryState;
 * - tự đọc đồng hồ hệ thống.
 */
class LearningDashboardForecastQueryService(
    private val memoryStateQuery: MemoryStateQuery,
    private val forecastCalculator:
    LearningDashboardForecastCalculator
) {

    fun query(
        learnerId: LearnerId,
        forecastStart: Moment,
        windowEnds: List<Moment>
    ): LearningDashboardForecastSnapshot {
        val memoryStates =
            memoryStateQuery.findAll(
                learnerId = learnerId
            )

        return query(memoryStates, forecastStart, windowEnds)
    }

    internal fun query(
        memoryStates: List<MemoryState>,
        forecastStart: Moment,
        windowEnds: List<Moment>
    ) = LearningDashboardForecastSnapshot(
        forecast = forecastCalculator.calculate(memoryStates, forecastStart, windowEnds)
    )
}
