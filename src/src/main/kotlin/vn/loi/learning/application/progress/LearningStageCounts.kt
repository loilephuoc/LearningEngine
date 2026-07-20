package vn.loi.learning.application.progress

import vn.loi.learning.domain.study.memory.model.LearningStage

/**
 * Phân bố MemoryState theo LearningStage.
 *
 * Model này giữ đầy đủ mọi stage hiện có trong domain:
 * - NEW;
 * - LEARNING;
 * - REVIEW;
 * - RELEARNING;
 * - MASTERED;
 * - SUSPENDED.
 *
 * Nhờ giữ đầy đủ các stage, [totalMemories] luôn phản ánh
 * chính xác tổng số MemoryState được thống kê.
 *
 * Đây là immutable result model của Application Layer.
 * Nó không truy cập repository và không thay đổi MemoryState.
 */
data class LearningStageCounts(
    val newCount: Int,
    val learningCount: Int,
    val reviewCount: Int,
    val relearningCount: Int,
    val masteredCount: Int,
    val suspendedCount: Int
) {

    init {
        require(newCount >= 0) {
            "New count must not be negative."
        }

        require(learningCount >= 0) {
            "Learning count must not be negative."
        }

        require(reviewCount >= 0) {
            "Review count must not be negative."
        }

        require(relearningCount >= 0) {
            "Relearning count must not be negative."
        }

        require(masteredCount >= 0) {
            "Mastered count must not be negative."
        }

        require(suspendedCount >= 0) {
            "Suspended count must not be negative."
        }
    }

    val totalMemories: Int
        get() =
            newCount +
                    learningCount +
                    reviewCount +
                    relearningCount +
                    masteredCount +
                    suspendedCount

    val activeMemories: Int
        get() =
            totalMemories - suspendedCount

    val hasMemories: Boolean
        get() = totalMemories > 0

    operator fun get(
        stage: LearningStage
    ): Int =
        when (stage) {
            LearningStage.NEW ->
                newCount

            LearningStage.LEARNING ->
                learningCount

            LearningStage.REVIEW ->
                reviewCount

            LearningStage.RELEARNING ->
                relearningCount

            LearningStage.MASTERED ->
                masteredCount

            LearningStage.SUSPENDED ->
                suspendedCount
        }

    companion object {

        val EMPTY =
            LearningStageCounts(
                newCount = 0,
                learningCount = 0,
                reviewCount = 0,
                relearningCount = 0,
                masteredCount = 0,
                suspendedCount = 0
            )
    }
}