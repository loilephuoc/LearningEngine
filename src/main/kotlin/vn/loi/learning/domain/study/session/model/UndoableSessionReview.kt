package vn.loi.learning.domain.study.session.model

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId

/** Durable before-state required to reverse exactly the latest committed session review. */
data class UndoableSessionReview(
    val reviewEventId: ReviewEventId,
    val learningItemId: LearningItemId,
    val contentId: ContentId,
    val memoryStateBefore: MemoryState,
    val memoryStateExistedBefore: Boolean,
    val reviewedItemIdsBefore: Set<LearningItemId>,
    val reviewedContentIdsBefore: Set<ContentId>,
    val lapsedContentIdsBefore: Set<ContentId> = emptySet(),
    val newItemsReviewedBefore: Int,
    val reviewItemsReviewedBefore: Int,
    val currentItemPresentedAtBefore: Moment?,
    val answerRevealedBefore: Boolean
)
