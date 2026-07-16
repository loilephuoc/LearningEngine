package vn.loi.learning.domain.study.memory.model

import vn.loi.learning.domain.study.learning.model.LearningItemId

/**
 * Trạng thái ghi nhớ của một Learner đối với một LearningItem.
 *
 * Đây là Aggregate Root của Memory Domain.
 *
 * MemoryState không chứa nội dung câu, audio hoặc hình ảnh.
 * Nó chỉ chứa trạng thái học và thông số phục vụ Scheduler.
 */
data class MemoryState(
    val learnerId: LearnerId,
    val learningItemId: LearningItemId,
    val stage: LearningStage,
    val difficulty: Double,
    val stabilityDays: Double,
    val dueAt: Moment,
    val lastReviewedAt: Moment?,
    val reviewCount: Int,
    val lapseCount: Int
) {

    init {
        require(difficulty in MIN_DIFFICULTY..MAX_DIFFICULTY) {
            "Difficulty must be between $MIN_DIFFICULTY and $MAX_DIFFICULTY, but was $difficulty."
        }

        require(stabilityDays >= 0.0) {
            "Stability days must not be negative."
        }

        require(reviewCount >= 0) {
            "Review count must not be negative."
        }

        require(lapseCount >= 0) {
            "Lapse count must not be negative."
        }

        require(lapseCount <= reviewCount) {
            "Lapse count must not exceed review count."
        }

        if (reviewCount == 0) {
            require(lastReviewedAt == null) {
                "An unreviewed MemoryState must not have lastReviewedAt."
            }
        }

        if (reviewCount > 0) {
            require(lastReviewedAt != null) {
                "A reviewed MemoryState must have lastReviewedAt."
            }
        }
    }

    val isNew: Boolean
        get() = stage == LearningStage.NEW

    fun isDue(at: Moment): Boolean =
        stage != LearningStage.SUSPENDED && dueAt <= at

    companion object {
        const val MIN_DIFFICULTY: Double = 1.0
        const val MAX_DIFFICULTY: Double = 10.0
        const val DEFAULT_DIFFICULTY: Double = 5.0

        /**
         * Tạo trạng thái ban đầu cho một LearningItem mới.
         *
         * Item mới được đến hạn ngay tại thời điểm nó được đăng ký.
         */
        fun new(
            learnerId: LearnerId,
            learningItemId: LearningItemId,
            availableAt: Moment
        ): MemoryState =
            MemoryState(
                learnerId = learnerId,
                learningItemId = learningItemId,
                stage = LearningStage.NEW,
                difficulty = DEFAULT_DIFFICULTY,
                stabilityDays = 0.0,
                dueAt = availableAt,
                lastReviewedAt = null,
                reviewCount = 0,
                lapseCount = 0
            )
    }
}