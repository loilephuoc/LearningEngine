package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Số liệu tổng hợp của một SchedulerDecision.
 *
 * Metrics chỉ mô tả kết quả scheduling đã xảy ra.
 * Nó không quyết định stage, difficulty, stability hoặc interval.
 */
data class SchedulerMetrics(
    val scheduledInterval: TimeSpan,
    val previousStage: LearningStage,
    val nextStage: LearningStage,
    val difficultyDelta: Double,
    val stabilityDeltaDays: Double,
    val reviewCountIncrement: Int,
    val lapseCountIncrement: Int
) {

    init {
        require(difficultyDelta.isFinite()) {
            "Scheduler difficulty delta must be finite."
        }

        require(stabilityDeltaDays.isFinite()) {
            "Scheduler stability delta must be finite."
        }

        require(reviewCountIncrement >= 0) {
            "Scheduler review count increment must not be negative."
        }

        require(lapseCountIncrement >= 0) {
            "Scheduler lapse count increment must not be negative."
        }
    }

    val stageChanged: Boolean
        get() =
            previousStage != nextStage

    val difficultyIncreased: Boolean
        get() =
            difficultyDelta > 0.0

    val difficultyDecreased: Boolean
        get() =
            difficultyDelta < 0.0

    val stabilityIncreased: Boolean
        get() =
            stabilityDeltaDays > 0.0

    val stabilityDecreased: Boolean
        get() =
            stabilityDeltaDays < 0.0

    val lapseOccurred: Boolean
        get() =
            lapseCountIncrement > 0
}