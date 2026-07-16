package vn.loi.learning.infrastructure.learning

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId

class InMemoryLearningItemRepository : LearningItemRepository {

    private val items = linkedMapOf<LearningItemId, LearningItem>()

    override fun findById(
        learningItemId: LearningItemId
    ): LearningItem? =
        items[learningItemId]

    override fun findByContentId(
        contentId: ContentId
    ): List<LearningItem> =
        items.values.filter { it.contentId == contentId }

    override fun findAllEnabled(): List<LearningItem> =
        items.values.filter { it.isEnabled }

    override fun save(learningItem: LearningItem) {
        items[learningItem.id] = learningItem
    }

    fun count(): Int = items.size

    fun clear() {
        items.clear()
    }
}