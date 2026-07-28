package vn.loi.learning.application.study

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningcontent.LearningContentProjector
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.Moment

/**
 * Kết quả trả về cho UI hoặc Adapter.
 *
 * memoryState là effective state có thẩm quyền trong production path. Trạng thái persistence
 * được biểu diễn riêng bằng hasPersistedMemoryState, không suy ra từ nullability.
 */
data class NextLearningItem(
    val content: Content,
    val learningItem: LearningItem,
    val memoryState: MemoryState?,
    val effectiveDueAt: Moment,
    val hasPersistedMemoryState: Boolean = memoryState != null
) {

    val learningStage: LearningStage
        get() = memoryState?.stage ?: LearningStage.NEW

    val isNew: Boolean
        get() = learningStage == LearningStage.NEW

    val learningContent: LearningContent
        get() = LearningContentProjector.project(content)
}
