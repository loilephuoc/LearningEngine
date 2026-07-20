package vn.loi.learning.application.progress

import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState

/**
 * Tính phân bố MemoryState theo LearningStage.
 *
 * Calculator này:
 * - không truy cập repository;
 * - không đọc đồng hồ hệ thống;
 * - không thay đổi MemoryState;
 * - giữ đầy đủ mọi LearningStage hiện có.
 */
class LearningStageCountsCalculator {

    fun calculate(
        memoryStates: List<MemoryState>
    ): LearningStageCounts {
        if (memoryStates.isEmpty()) {
            return LearningStageCounts.EMPTY
        }

        val countsByStage =
            memoryStates.groupingBy { memoryState ->
                memoryState.stage
            }.eachCount()

        return LearningStageCounts(
            newCount =
                countsByStage.countOf(
                    LearningStage.NEW
                ),
            learningCount =
                countsByStage.countOf(
                    LearningStage.LEARNING
                ),
            reviewCount =
                countsByStage.countOf(
                    LearningStage.REVIEW
                ),
            relearningCount =
                countsByStage.countOf(
                    LearningStage.RELEARNING
                ),
            masteredCount =
                countsByStage.countOf(
                    LearningStage.MASTERED
                ),
            suspendedCount =
                countsByStage.countOf(
                    LearningStage.SUSPENDED
                )
        )
    }

    private fun Map<LearningStage, Int>.countOf(
        stage: LearningStage
    ): Int =
        getOrDefault(
            key = stage,
            defaultValue = 0
        )
}