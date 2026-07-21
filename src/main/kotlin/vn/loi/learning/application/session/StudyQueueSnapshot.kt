package vn.loi.learning.application.session

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId

/**
 * Snapshot bất biến của thứ tự LearningItem trong một phiên học.
 *
 * Snapshot chỉ giữ:
 * - danh tính session;
 * - thời điểm tạo queue;
 * - thứ tự LearningItem;
 * - vị trí hiện tại.
 *
 * Snapshot không chứa:
 * - Content;
 * - MemoryState;
 * - dữ liệu scheduling;
 * - business rule lựa chọn item.
 *
 * Mọi thuộc tính điều hướng và tiến độ đều được suy ra từ
 * learningItemIds và currentIndex. Vì vậy các thuộc tính này
 * không cần được persist riêng.
 *
 * Mỗi thao tác chuyển queue đều trả về một snapshot mới.
 */
data class StudyQueueSnapshot(
    val sessionId: SessionId,
    val createdAt: Moment,
    val learningItemIds: List<LearningItemId>,
    val currentIndex: Int = 0
) {

    init {
        require(
            learningItemIds.distinct().size ==
                    learningItemIds.size
        ) {
            "Study queue must not contain duplicate LearningItemIds."
        }

        require(currentIndex >= 0) {
            "Study queue current index must not be negative."
        }

        require(
            currentIndex <= learningItemIds.size
        ) {
            "Study queue current index must not exceed queue size."
        }
    }

    /**
     * Tổng số item ban đầu trong queue.
     */
    val totalItemCount: Int
        get() =
            learningItemIds.size

    /**
     * Số item đã đi qua.
     *
     * currentIndex cũng chính là số lượng item đã hoàn tất.
     */
    val completedItemCount: Int
        get() =
            currentIndex

    /**
     * Số item chưa hoàn tất, bao gồm item hiện tại.
     */
    val remainingItemCount: Int
        get() =
            totalItemCount -
                    completedItemCount

    /**
     * Queue không chứa item nào.
     */
    val isEmpty: Boolean
        get() =
            learningItemIds.isEmpty()

    /**
     * Queue đã đi hết toàn bộ item.
     *
     * Empty queue cũng được xem là completed.
     */
    val isCompleted: Boolean
        get() =
            currentIndex >=
                    learningItemIds.size

    /**
     * Queue đang ở vị trí đầu tiên.
     *
     * Empty queue cũng có currentIndex bằng 0, nên thuộc tính này
     * chỉ true khi queue thực sự có item.
     */
    val isAtStart: Boolean
        get() =
            !isEmpty &&
                    currentIndex == 0

    /**
     * Item hiện tại của queue.
     *
     * Trả về null khi queue đã completed hoặc queue rỗng.
     */
    val currentLearningItemId:
            LearningItemId?
        get() =
            learningItemIds
                .getOrNull(
                    currentIndex
                )

    /**
     * Item ngay trước vị trí hiện tại.
     *
     * Sau khi queue completed, đây là item cuối cùng đã hoàn tất.
     */
    val previousLearningItemId:
            LearningItemId?
        get() =
            learningItemIds
                .getOrNull(
                    currentIndex - 1
                )

    /**
     * Item ngay sau item hiện tại.
     *
     * Trả về null khi:
     * - queue rỗng;
     * - queue completed;
     * - item hiện tại là item cuối cùng.
     */
    val nextLearningItemId:
            LearningItemId?
        get() =
            peekNext(offset = 1)

    /**
     * True khi item hiện tại là item cuối cùng chưa hoàn tất.
     */
    val isLastItem: Boolean
        get() =
            !isCompleted &&
                    currentIndex ==
                    learningItemIds.lastIndex

    /**
     * Danh sách item đã hoàn tất.
     *
     * Thứ tự ban đầu của queue được giữ nguyên.
     */
    val completedLearningItemIds:
            List<LearningItemId>
        get() =
            learningItemIds
                .take(
                    completedItemCount
                )

    /**
     * Danh sách item chưa hoàn tất, bao gồm item hiện tại.
     *
     * Khi queue completed, danh sách này rỗng.
     */
    val remainingLearningItemIds:
            List<LearningItemId>
        get() =
            learningItemIds
                .drop(
                    currentIndex
                )

    /**
     * Danh sách item đứng sau item hiện tại.
     *
     * Không bao gồm item hiện tại.
     */
    val pendingLearningItemIds:
            List<LearningItemId>
        get() =
            if (isCompleted) {
                emptyList()
            } else {
                learningItemIds
                    .drop(
                        currentIndex + 1
                    )
            }

    /**
     * Tiến độ trong khoảng 0.0 đến 1.0.
     *
     * Empty queue được xem là hoàn tất nên có progress bằng 1.0.
     */
    val progress: Double
        get() =
            if (isEmpty) {
                1.0
            } else {
                completedItemCount
                    .toDouble() /
                        totalItemCount
                            .toDouble()
            }

    /**
     * Tiến độ phần trăm nguyên trong khoảng 0 đến 100.
     *
     * Phép chia dùng completedItemCount trước để tránh sai số
     * khiến queue completed trả về nhỏ hơn 100.
     */
    val percentComplete: Int
        get() =
            if (isEmpty) {
                100
            } else {
                completedItemCount *
                        100 /
                        totalItemCount
            }

    /**
     * Đọc item theo khoảng cách tương đối từ item hiện tại.
     *
     * offset = 0:
     * - trả về item hiện tại.
     *
     * offset = 1:
     * - trả về item kế tiếp.
     *
     * offset lớn hơn số item còn lại:
     * - trả về null.
     *
     * Không hỗ trợ offset âm vì việc đọc item trước đó đã có
     * previousLearningItemId và completedLearningItemIds.
     */
    fun peekNext(
        offset: Int = 1
    ): LearningItemId? {
        require(offset >= 0) {
            "Study queue peek offset must not be negative."
        }

        return learningItemIds
            .getOrNull(
                currentIndex + offset
            )
    }

    /**
     * Chuyển queue sang item kế tiếp.
     */
    fun advance(): StudyQueueSnapshot {
        require(!isCompleted) {
            "Cannot advance a completed study queue."
        }

        return copy(
            currentIndex =
                currentIndex + 1
        )
    }

    companion object {

        fun create(
            sessionId: SessionId,
            createdAt: Moment,
            learningItemIds:
            List<LearningItemId>
        ): StudyQueueSnapshot =
            StudyQueueSnapshot(
                sessionId = sessionId,
                createdAt = createdAt,
                learningItemIds =
                    learningItemIds.toList(),
                currentIndex = 0
            )
    }
}