package vn.loi.learning.application.port

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId

interface LearningItemRepository {

    fun findById(
        learningItemId: LearningItemId
    ): LearningItem?

    fun findByContentId(
        contentId: ContentId
    ): List<LearningItem>

    fun findByContentIds(
        contentIds: Set<ContentId>
    ): List<LearningItem> {
        if (contentIds.isEmpty()) return emptyList()
        return contentIds.flatMap { findByContentId(it) }
    }

    fun findAllEnabled(): List<LearningItem>

    fun save(
        learningItem: LearningItem
    )

    fun saveAll(
        learningItems: List<LearningItem>
    ) {
        learningItems.forEach(::save)
    }

    fun deleteById(
        learningItemId: LearningItemId
    )

    fun deleteAllById(
        learningItemIds: Set<LearningItemId>
    ) {
        learningItemIds.forEach(::deleteById)
    }

    fun deleteByContentIds(
        contentIds: Set<ContentId>
    ) {
        contentIds.forEach { contentId ->
            findByContentId(contentId)
                .forEach { learningItem ->
                    deleteById(learningItem.id)
                }
        }
    }
}
