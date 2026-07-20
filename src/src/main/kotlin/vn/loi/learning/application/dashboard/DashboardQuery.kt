package vn.loi.learning.application.dashboard

import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.memory.model.LearnerId

/**
 * Điều kiện truy vấn dữ liệu Dashboard của một learner.
 *
 * Query nhận các khoảng thời gian đã được xác định từ bên ngoài
 * thay vì tự đọc đồng hồ hệ thống hoặc tự suy đoán múi giờ.
 *
 * Quy ước:
 * - todayPeriod: khoảng thời gian của ngày học hiện tại;
 * - currentWeekPeriod: khoảng thời gian của tuần hiện tại;
 * - currentMonthPeriod: khoảng thời gian của tháng hiện tại;
 * - dailyPeriods: chuỗi ngày liên tiếp kết thúc tại todayPeriod,
 *   dùng cho daily statistics, rating trend và current streak.
 *
 * Khi dailyPeriods không rỗng, các period phải:
 * - nằm hoàn toàn trong currentMonthPeriod;
 * - được sắp xếp tăng dần;
 * - liên tiếp, không chồng lấn và không có khoảng trống;
 * - kết thúc bằng todayPeriod.
 *
 * Query này:
 * - chỉ mô tả nhu cầu truy vấn;
 * - không truy cập repository;
 * - không truy vấn ReviewHistoryQueryService;
 * - không thực hiện tính toán dashboard.
 */
data class DashboardQuery(
    val learnerId: LearnerId,
    val todayPeriod: StudyPeriod,
    val currentWeekPeriod: StudyPeriod,
    val currentMonthPeriod: StudyPeriod,
    val dailyPeriods: List<StudyPeriod> = emptyList()
) {

    init {
        require(
            currentWeekPeriod.containsPeriod(todayPeriod)
        ) {
            "Current week period must contain today period."
        }

        require(
            currentMonthPeriod.containsPeriod(todayPeriod)
        ) {
            "Current month period must contain today period."
        }

        require(
            dailyPeriods.all { dailyPeriod ->
                currentMonthPeriod.containsPeriod(dailyPeriod)
            }
        ) {
            "Every daily period must be contained in current month period."
        }

        require(
            dailyPeriods.isContiguousAndOrdered()
        ) {
            "Daily periods must be ordered and contiguous."
        }

        require(
            dailyPeriods.isEmpty() ||
                    dailyPeriods.last() == todayPeriod
        ) {
            "Daily periods must end with today period."
        }
    }

    private fun StudyPeriod.containsPeriod(
        period: StudyPeriod
    ): Boolean =
        period.startInclusive >= startInclusive &&
                period.endExclusive <= endExclusive

    private fun List<StudyPeriod>.isContiguousAndOrdered():
            Boolean =
        zipWithNext().all { (current, next) ->
            current.endExclusive == next.startInclusive
        }
}