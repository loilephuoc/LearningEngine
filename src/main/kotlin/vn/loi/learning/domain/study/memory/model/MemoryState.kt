package vn.loi.learning.domain.study.memory.model

import vn.loi.learning.domain.study.learning.model.LearningItemId

/**
 * Trạng thái ghi nhớ của một Learner đối với một LearningItem.
 *
 * Đây là Aggregate Root của Memory Domain.
 *
 * MemoryState không chứa nội dung câu, audio hoặc hình ảnh.
 * Nó chỉ chứa trạng thái học và thông số phục vụ Scheduler.
 *
 * difficulty và stabilityDays hiện vẫn được giữ dưới dạng Double
 * để bảo toàn API tương thích trong quá trình migration.
 *
 * Code domain mới nên sử dụng difficultyValue và stability để
 * làm việc với các Value Object chính thức.
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

    /**
     * Biểu diễn domain chính thức của difficulty.
     *
     * Thuộc tính primitive difficulty được giữ tạm thời để tương thích
     * với các call site, fixture và persistence mapper hiện có.
     */
    val difficultyValue: Difficulty =
        Difficulty.of(difficulty)

    /**
     * Biểu diễn domain chính thức của stability.
     *
     * Thuộc tính primitive stabilityDays được giữ tạm thời để tương thích
     * với các call site, fixture và persistence mapper hiện có.
     */
    val stability: Stability =
        Stability.of(stabilityDays)

    init {
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

        /**
         * Alias tạm thời để giữ tương thích.
         *
         * Nguồn định nghĩa chính thức nằm trong Difficulty.
         * Các hằng này sẽ được loại bỏ sau khi MemoryState
         * chuyển hoàn toàn sang Difficulty Value Object.
         */
        const val MIN_DIFFICULTY: Double =
            Difficulty.MIN_VALUE

        const val MAX_DIFFICULTY: Double =
            Difficulty.MAX_VALUE

        const val DEFAULT_DIFFICULTY: Double =
            Difficulty.DEFAULT_VALUE

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
                difficulty = Difficulty.DEFAULT_VALUE,
                stabilityDays = 0.0,
                dueAt = availableAt,
                lastReviewedAt = null,
                reviewCount = 0,
                lapseCount = 0
            )
    }
}