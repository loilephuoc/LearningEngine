package vn.loi.learning.application.study

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId

/**
 * Một LearningItem đã được planner lựa chọn và sắp xếp,
 * kèm thông tin phân loại cần thiết để áp dụng SessionPolicy.
 *
 * Entry không chứa toàn bộ SelectionCandidate nhằm tránh làm rò rỉ
 * chi tiết MemoryState ra khỏi bước selection.
 */
data class StudyQueuePlanEntry(
    val learningItemId: LearningItemId,
    val isNew: Boolean,
    val contentId: ContentId? = null
)
