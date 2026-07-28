package vn.loi.learning.application.study

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewRating

data class ContentLearningState(
    val contentId: ContentId,
    val siblingLearningItemIds: Set<LearningItemId>,
    val latestEffectiveReviewEvent: ReviewEvent?
) {
    val isLearned: Boolean get() = latestEffectiveReviewEvent != null
    val latestEffectiveRating: ReviewRating? get() = latestEffectiveReviewEvent?.rating
    val latestReviewedAt: Moment? get() = latestEffectiveReviewEvent?.reviewedAt
}

class ContentLearningStateQueryService(
    private val learningItems: LearningItemRepository,
    private val reviewEvents: ReviewEventRepository
) {
    fun resolve(learnerId: LearnerId, contentId: ContentId): ContentLearningState =
        resolveAll(learnerId, setOf(contentId)).getValue(contentId)

    fun resolveAll(
        learnerId: LearnerId,
        contentIds: Set<ContentId>
    ): Map<ContentId, ContentLearningState> {
        if (contentIds.isEmpty()) return emptyMap()
        val items = learningItems.findByContentIds(contentIds)
        val itemToContent = items.associate { it.id to it.contentId }
        val latestByContent = linkedMapOf<ContentId, ReviewEvent>()
        reviewEvents.findAll(learnerId).forEach { event ->
            itemToContent[event.learningItemId]?.let { contentId ->
                latestByContent[contentId] = event
            }
        }
        val itemIdsByContent = items.groupBy { it.contentId }
            .mapValues { (_, siblings) -> siblings.mapTo(linkedSetOf()) { it.id } }
        return contentIds.associateWith { contentId ->
            ContentLearningState(
                contentId = contentId,
                siblingLearningItemIds = itemIdsByContent[contentId].orEmpty(),
                latestEffectiveReviewEvent = latestByContent[contentId]
            )
        }
    }
}
