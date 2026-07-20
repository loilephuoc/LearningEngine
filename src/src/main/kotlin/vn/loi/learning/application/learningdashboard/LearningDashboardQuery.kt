package vn.loi.learning.application.learningdashboard

import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment

/**
 * Yêu cầu tạo một snapshot của Learning Dashboard.
 *
 * [activityFrom] và [activityUntil] xác định khoảng
 * thời gian dùng để tính Activity section.
 *
 * Khoảng Activity sử dụng quy ước:
 *
 *     [activityFrom, activityUntil]
 *
 * [at] là thời điểm chụp Dashboard và được dùng cho:
 * - due statistics;
 * - retention evaluation;
 * - thời điểm bắt đầu forecast.
 *
 * [forecastWindowEnds] chứa các mốc kết thúc bucket
 * theo thứ tự tăng dần. Query service sẽ tạo:
 *
 *     (at, forecastWindowEnds[0]]
 *     (forecastWindowEnds[0], forecastWindowEnds[1]]
 *     ...
 *
 * Query này:
 * - immutable;
 * - không truy cập repository;
 * - không đọc đồng hồ hệ thống;
 * - không chứa kết quả đã tính;
 * - không thực hiện orchestration.
 */
data class LearningDashboardQuery(
    val learnerId: LearnerId,
    val activityFrom: Moment,
    val activityUntil: Moment,
    val at: Moment,
    val forecastWindowEnds: List<Moment>
) {

    init {
        require(activityUntil >= activityFrom) {
            "Dashboard activity until must not be before activity from."
        }

        requireForecastWindowEndsAreStrictlyIncreasing(
            forecastStart = at,
            windowEnds = forecastWindowEnds
        )
    }

    companion object {

        private fun requireForecastWindowEndsAreStrictlyIncreasing(
            forecastStart: Moment,
            windowEnds: List<Moment>
        ) {
            var previous =
                forecastStart

            windowEnds.forEach { windowEnd ->
                require(windowEnd > previous) {
                    "Dashboard forecast window ends must be strictly increasing and after dashboard time."
                }

                previous =
                    windowEnd
            }
        }
    }
}