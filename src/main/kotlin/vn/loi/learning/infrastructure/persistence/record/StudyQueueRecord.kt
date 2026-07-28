package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

/**
 * Dạng dữ liệu bền vững của StudyQueueSnapshot.
 *
 * Record chỉ chứa primitive values và collection của primitive values.
 * Record không chứa Domain objects.
 */
@Serializable
data class StudyQueueRecord(
    val schemaVersion: Int,
    val sessionId: String,
    val createdAtEpochMillis: Long,
    val learningItemIds: List<String>,
    val currentIndex: Int,
    val itemOrigins: Map<String, String> = emptyMap(),
    val itemContentIds: Map<String, String> = emptyMap(),
    val configuredNewTarget: Int = 0,
    val effectiveNewWorkload: Int = 0,
    val configuredReviewTarget: Int = 0,
    val effectiveReviewWorkload: Int = 0
) {

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 4
    }
}
