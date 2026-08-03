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
    val effectiveReviewWorkload: Int = 0,
    val fixedPracticeMembership: List<String> = emptyList(),
    val practiceSeed: Long? = null,
    val practiceRound: Int = 0,
    val coverageReinforcementStates: Map<String, CoverageReinforcementStateRecord> = emptyMap(),
    val coverageReinforcementUndo: CoverageReinforcementUndoRecord? = null
) {

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 6
    }
}

@Serializable
data class CoverageReinforcementStateRecord(
    val reinforcementCount: Int,
    val previousGap: Int? = null,
    val lastInsertionIndex: Int? = null,
    val deferred: Boolean = false,
    val schemaVersion: Int = 1
)

@Serializable
data class CoverageReinforcementUndoRecord(
    val learningItemId: String,
    val previousState: CoverageReinforcementStateRecord? = null,
    val discardedTail: List<String> = emptyList(),
    val completionTruncation: Boolean = false
)
