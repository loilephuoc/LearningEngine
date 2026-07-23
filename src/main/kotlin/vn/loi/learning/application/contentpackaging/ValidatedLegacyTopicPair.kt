package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.topic.model.TopicId

data class ValidatedLegacyTopicPair(
    val logicalTopicName: String,
    val topicId: TopicId,
    val jsonSource: String,
    val packageSource: String
) {

    init {
        require(logicalTopicName.isNotBlank()) {
            "Legacy topic logical name must not be blank."
        }
        require(jsonSource.isNotBlank()) {
            "Legacy topic JSON source must not be blank."
        }
        require(packageSource.isNotBlank()) {
            "Legacy topic PKG source must not be blank."
        }
    }

    fun toLegacyPackageCandidate(): LegacyPackageCandidate =
        LegacyPackageCandidate(
            jsonSource = jsonSource,
            mediaSource = packageSource
        )
}
