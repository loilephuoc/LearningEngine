package vn.loi.learning.application.topic

import vn.loi.learning.domain.content.topic.model.TopicId

data class TopicSelection(
    val id: TopicId,
    val installedPackageId: String?
)
