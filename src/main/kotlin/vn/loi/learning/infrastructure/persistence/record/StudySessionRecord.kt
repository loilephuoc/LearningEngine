package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

/**
 * Dạng dữ liệu bền vững của StudySession.
 *
 * Record chỉ chứa:
 * - primitive values;
 * - nullable primitive values;
 * - collection của primitive values.
 *
 * Record không chứa Domain objects.
 */
@Serializable
data class StudySessionRecord(
    val schemaVersion: Int,
    val id: String,
    val learnerId: String,
    val startedAtEpochMillis: Long,
    val status: String,

    val policyNewItemLimit: Int,
    val policyReviewItemLimit: Int,
    val policyAllowRepeatInSameSession: Boolean,

    val includedContentIds: List<String> = emptyList(),
    val reviewedItemIds: List<String>,
    val reviewedContentIds: List<String>,

    val newItemsReviewed: Int,
    val reviewItemsReviewed: Int,
    val finishedAtEpochMillis: Long?
) {

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}