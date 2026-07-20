package vn.loi.learning.domain.study.fsrs.calibration.model

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent

/**
 * Tập dữ liệu lịch sử review của một người học.
 *
 * Dataset này là đầu vào chuẩn hóa cho quá trình calibration FSRS.
 * Nó không biết dữ liệu được lấy từ JSON, SQLite, CSV hay cloud.
 *
 * Các ReviewEvent trong dataset phải:
 * - thuộc cùng một người học;
 * - có ID không trùng nhau;
 * - được sắp xếp theo thời gian review tăng dần.
 *
 * Dataset chứa trực tiếp ReviewEvent để không nhân bản:
 * - rating;
 * - reviewedAt;
 * - responseTime;
 * - stateBefore;
 * - stateAfter.
 */
data class ReviewHistoryDataset(
    val learnerId: LearnerId,
    val events: List<ReviewEvent>
) {

    init {
        require(
            events.all { event ->
                event.learnerId == learnerId
            }
        ) {
            "All review events must belong to the dataset learner."
        }

        require(
            events
                .map { event -> event.id }
                .distinct()
                .size == events.size
        ) {
            "Review event IDs must be unique within a dataset."
        }

        require(
            events.zipWithNext().all { (previous, next) ->
                previous.reviewedAt <= next.reviewedAt
            }
        ) {
            "Review events must be ordered by reviewedAt."
        }
    }

    /**
     * Tổng số lần review trong dataset.
     */
    val totalReviews: Int
        get() = events.size

    /**
     * Dataset không có review nào.
     */
    val isEmpty: Boolean
        get() = events.isEmpty()

    /**
     * Thời điểm review đầu tiên.
     */
    val startedAt: Moment?
        get() = events.firstOrNull()?.reviewedAt

    /**
     * Thời điểm review gần nhất.
     */
    val endedAt: Moment?
        get() = events.lastOrNull()?.reviewedAt

    /**
     * Trả về các review của một LearningItem,
     * giữ nguyên thứ tự thời gian.
     */
    fun eventsForLearningItem(
        learningItemId: LearningItemId
    ): List<ReviewEvent> =
        events.filter { event ->
            event.learningItemId == learningItemId
        }

    companion object {

        /**
         * Tạo dataset chuẩn hóa cho một người học.
         *
         * Factory sẽ:
         * - chỉ giữ event thuộc requested learner;
         * - sắp xếp event tăng dần theo reviewedAt;
         * - giữ nguyên thứ tự đầu vào nếu nhiều event cùng thời điểm;
         * - để constructor kiểm tra ID trùng và các invariant còn lại.
         */
        fun from(
            learnerId: LearnerId,
            events: Iterable<ReviewEvent>
        ): ReviewHistoryDataset {
            val normalizedEvents =
                events
                    .filter { event ->
                        event.learnerId == learnerId
                    }
                    .sortedBy { event ->
                        event.reviewedAt.epochMillis
                    }

            return ReviewHistoryDataset(
                learnerId = learnerId,
                events = normalizedEvents
            )
        }

        /**
         * Tạo dataset rỗng cho một người học.
         */
        fun empty(
            learnerId: LearnerId
        ): ReviewHistoryDataset =
            ReviewHistoryDataset(
                learnerId = learnerId,
                events = emptyList()
            )
    }
}