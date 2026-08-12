package vn.loi.learning.application.progress

import vn.loi.learning.domain.study.analytics.model.StudyPeriod
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.learning.model.LearningItemId

/**
 * Điều kiện truy vấn tiến độ học tập của một learner.
 *
 * Query nhận toàn bộ yếu tố thời gian từ bên ngoài,
 * không tự đọc đồng hồ hệ thống và không tự xác định múi giờ.
 *
 * [activityPeriod] là khoảng thời gian dùng để thống kê
 * toàn bộ hoạt động học tập.
 *
 * [dailyPeriods] là các khoảng ngày thuộc activityPeriod,
 * dùng để xác định số ngày thực sự có hoạt động học.
 *
 * Khi dailyPeriods không rỗng, các period phải:
 * - nằm hoàn toàn trong activityPeriod;
 * - được sắp xếp tăng dần;
 * - liên tiếp, không chồng lấn và không có khoảng trống.
 *
 * dailyPeriods không bắt buộc phải bao phủ toàn bộ activityPeriod.
 * Điều này cho phép caller chỉ yêu cầu active-day statistics
 * cho một phần cụ thể của khoảng hoạt động.
 *
 * [evaluatedAt] là thời điểm dùng để đánh giá trạng thái
 * hiện tại của dữ liệu học tập.
 *
 * Query này:
 * - chỉ mô tả nhu cầu truy vấn;
 * - không truy cập repository;
 * - không thực hiện analytics;
 * - không sửa đổi trạng thái domain.
 */
data class LearningProgressQuery(
    val learnerId: LearnerId,
    val activityPeriod: StudyPeriod,
    val evaluatedAt: Moment,
    val dailyPeriods: List<StudyPeriod> = emptyList(),
    val learningItemIds: Set<LearningItemId>? = null
) {

    init {
        require(
            evaluatedAt >= activityPeriod.startInclusive
        ) {
            "Evaluation moment must not be before activity period."
        }

        require(
            dailyPeriods.all { dailyPeriod ->
                activityPeriod.containsPeriod(dailyPeriod)
            }
        ) {
            "Every daily period must be contained in activity period."
        }

        require(
            dailyPeriods.isContiguousAndOrdered()
        ) {
            "Daily periods must be ordered and contiguous."
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
