package vn.loi.learning.application.study

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment

data class GetNextLearningItemQuery(
    val learnerId: LearnerId,
    val now: Moment,
    val excludedItemIds: Set<LearningItemId> = emptySet(),
    val excludedContentIds: Set<ContentId> = emptySet(),
    val includedContentIds: Set<ContentId> = emptySet(),
    val includeNewItems: Boolean = true,
    val includeReviewItems: Boolean = true
)