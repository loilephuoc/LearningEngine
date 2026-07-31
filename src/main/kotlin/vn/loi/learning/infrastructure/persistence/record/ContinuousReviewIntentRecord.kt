package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

@Serializable
data class ContinuousReviewIntentRecord(
    val learnerId: String,
    val installedPackageId: String,
    val topicId: String? = null,
    val enabled: Boolean = false,
    val updatedAtEpochMillis: Long,
    val lastNoWorkPredecessorId: String? = null
)
