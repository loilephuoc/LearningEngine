package vn.loi.learning.infrastructure.persistence.memory

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId

class InMemoryLearningItemRepository : LearningItemRepository {

    private val items =
        linkedMapOf<LearningItemId, LearningItem>()

    override fun findAll(): List<LearningItem> =
        items.values.toList()

    override fun findById(
        learningItemId: LearningItemId
    ): LearningItem? =
        items[learningItemId]

    override fun findByContentId(
        contentId: ContentId
    ): List<LearningItem> =
        items.values.filter {
            it.contentId == contentId
        }

    override fun findByContentIds(
        contentIds: Set<ContentId>
    ): List<LearningItem> {
        if (contentIds.isEmpty()) {
            return emptyList()
        }

        return items.values.filter {
            it.contentId in contentIds
        }
    }

    override fun findContentIdsByLearningItemIds(
        learningItemIds: Set<LearningItemId>
    ): Map<LearningItemId, ContentId> {
        if (learningItemIds.isEmpty()) {
            return emptyMap()
        }

        return learningItemIds
            .mapNotNull { learningItemId ->
                items[learningItemId]
                    ?.let { learningItem ->
                        learningItemId to learningItem.contentId
                    }
            }
            .toMap()
    }

    override fun findAllEnabled(): List<LearningItem> =
        items.values.filter {
            it.isEnabled
        }

    override fun save(
        learningItem: LearningItem
    ) {
        items[learningItem.id] =
            learningItem
    }

    override fun deleteById(
        learningItemId: LearningItemId
    ) {
        items.remove(
            learningItemId
        )
    }

    override fun deleteAllById(
        learningItemIds: Set<LearningItemId>
    ) {
        learningItemIds.forEach(
            items::remove
        )
    }

    fun count(): Int =
        items.size

    fun clear() {
        items.clear()
    }
}