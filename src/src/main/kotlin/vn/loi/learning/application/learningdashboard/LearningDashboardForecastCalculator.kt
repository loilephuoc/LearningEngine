package vn.loi.learning.application.learningdashboard

import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

/**
 * Tạo scheduling forecast từ các MemoryState.
 *
 * Forecast bắt đầu sau [forecastStart].
 *
 * [windowEnds] chứa các mốc kết thúc bucket theo thứ tự
 * tăng dần. Từ đó calculator tạo các bucket liên tiếp:
 *
 *     (forecastStart, windowEnds[0]]
 *     (windowEnds[0], windowEnds[1]]
 *     ...
 *
 * MemoryState chỉ tham gia forecast khi:
 * - không ở trạng thái SUSPENDED;
 * - dueAt nằm sau forecastStart;
 * - dueAt thuộc một trong các bucket được yêu cầu.
 *
 * Memory đến hạn tại hoặc trước forecastStart không được
 * tính vào forecast vì chúng thuộc Due Statistics hiện tại.
 *
 * Calculator này:
 * - không truy cập repository;
 * - không đọc đồng hồ hệ thống;
 * - không thay đổi MemoryState;
 * - không thực hiện rescheduling;
 * - không suy đoán các khoảng forecast mặc định.
 */
class LearningDashboardForecastCalculator {

    fun calculate(
        memoryStates: List<MemoryState>,
        forecastStart: Moment,
        windowEnds: List<Moment>
    ): LearningDashboardForecast {
        if (windowEnds.isEmpty()) {
            return LearningDashboardForecast.EMPTY
        }

        requireWindowEndsAreStrictlyIncreasing(
            forecastStart = forecastStart,
            windowEnds = windowEnds
        )

        val dueCounts =
            IntArray(windowEnds.size)

        memoryStates.forEach { memoryState ->
            if (memoryState.stage == LearningStage.SUSPENDED) {
                return@forEach
            }

            if (memoryState.dueAt <= forecastStart) {
                return@forEach
            }

            val bucketIndex =
                windowEnds.indexOfFirst { windowEnd ->
                    memoryState.dueAt <= windowEnd
                }

            if (bucketIndex >= 0) {
                dueCounts[bucketIndex]++
            }
        }

        val buckets =
            windowEnds.mapIndexed { index, windowEnd ->
                LearningDashboardForecastBucket(
                    windowStart =
                        if (index == 0) {
                            forecastStart
                        } else {
                            windowEnds[index - 1]
                        },
                    windowEnd = windowEnd,
                    dueCount = dueCounts[index]
                )
            }

        return LearningDashboardForecast(
            buckets = buckets
        )
    }

    private fun requireWindowEndsAreStrictlyIncreasing(
        forecastStart: Moment,
        windowEnds: List<Moment>
    ) {
        var previous =
            forecastStart

        windowEnds.forEach { windowEnd ->
            require(windowEnd > previous) {
                "Forecast window ends must be strictly increasing and after forecast start."
            }

            previous =
                windowEnd
        }
    }
}