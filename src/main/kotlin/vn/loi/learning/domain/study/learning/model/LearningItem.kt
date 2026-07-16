package vn.loi.learning.domain.study.learning.model

import vn.loi.learning.domain.content.model.ContentId

/**
 * Đơn vị nhỏ nhất được Memory và Scheduler theo dõi.
 *
 * Một Content có thể tạo ra nhiều LearningItem khác nhau,
 * mỗi item đại diện cho một kỹ năng học cụ thể.
 *
 * LearningItem không chứa:
 * - due time
 * - difficulty
 * - stability
 * - review count
 * - learning stage
 *
 * Những trạng thái thay đổi theo người học thuộc MemoryState.
 */
data class LearningItem(
    val id: LearningItemId,
    val contentId: ContentId,
    val mode: LearningMode,
    val isEnabled: Boolean = true
)