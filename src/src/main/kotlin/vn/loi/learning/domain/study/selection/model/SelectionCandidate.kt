package vn.loi.learning.domain.study.selection.model

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

/**
 * Một LearningItem đã được ghép với MemoryState tương ứng
 * để tham gia Session Selection Engine.
 *
 * Candidate chỉ là dữ liệu đầu vào cho quá trình lựa chọn.
 * Nó không thay đổi MemoryState và không thực hiện scheduling.
 */
data class SelectionCandidate(
    val learningItem: LearningItem,
    val memoryState: MemoryState
) {

    init {
        require(learningItem.id == memoryState.learningItemId) {
            "LearningItem and MemoryState must reference the same LearningItemId."
        }
    }

    val learningItemId: LearningItemId
        get() = learningItem.id

    val contentId: ContentId
        get() = learningItem.contentId

    val isNew: Boolean
        get() = memoryState.isNew

    val isEnabled: Boolean
        get() = learningItem.isEnabled

    fun isDueAt(at: Moment): Boolean =
        memoryState.isDue(at)

    /**
     * Candidate có thể tham gia lựa chọn khi:
     * - LearningItem đang được bật;
     * - MemoryState đã đến hạn tại thời điểm xét.
     *
     * Item NEW cũng phải thỏa dueAt <= at.
     * Điều này cho phép hỗ trợ item mới được mở khóa trong tương lai.
     */
    fun isEligibleAt(at: Moment): Boolean =
        isEnabled && isDueAt(at)
}