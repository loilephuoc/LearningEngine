package vn.loi.learning.application.port

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId

interface LearningItemRepository {

    fun findById(learningItemId: LearningItemId): LearningItem?

    fun findByContentId(contentId: ContentId): List<LearningItem>

    fun findAllEnabled(): List<LearningItem>

    fun save(learningItem: LearningItem)

    fun deleteById(learningItemId: LearningItemId)
}

