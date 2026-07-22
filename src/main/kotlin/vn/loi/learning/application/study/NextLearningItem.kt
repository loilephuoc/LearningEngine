package vn.loi.learning.application.study

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningcontent.LearningContentProjector
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

/**
 * Kết quả trả về cho UI hoặc Adapter.
 *
 * memoryState bằng null nghĩa là item chưa từng được học.
 */
data class NextLearningItem(
    val content: Content,
    val learningItem: LearningItem,
    val memoryState: MemoryState?,
    val effectiveDueAt: Moment
) {

    val isNew: Boolean
        get() = memoryState == null

    val learningContent: LearningContent
        get() = LearningContentProjector.project(content)
}
