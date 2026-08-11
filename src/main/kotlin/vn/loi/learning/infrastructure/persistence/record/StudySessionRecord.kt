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
    val policyEvaluation: String = "EVALUATIVE",
    val policyPracticeLoop: String = "NONE",
    val policyFocusedPracticeKind: String = "NONE",

    val includedContentIds: List<String> = emptyList(),
    val reviewedItemIds: List<String>,
    val reviewedContentIds: List<String>,
    val introducedContentIds: List<String> = emptyList(),
    val lapsedContentIds: List<String> = emptyList(),

    val newItemsReviewed: Int,
    val reviewItemsReviewed: Int,
    val finishedAtEpochMillis: Long?,
    val currentLearningItemId: String? = null,
    val currentItemPresentedAtEpochMillis: Long? = null,
    val answerRevealed: Boolean = false,
    val pendingReviewEventId: String? = null,
    val pendingReviewLearningItemId: String? = null,
    val pendingReviewRating: String? = null,
    val pendingReviewReviewedAtEpochMillis: Long? = null,
    val pendingReviewResponseTimeMillis: Long? = null,
    val pendingReviewRatingSource: String = "STANDARD_REVIEW",
    val pendingRecallResult: String? = null,
    val pendingRecallRevealUsed: Boolean = false,
    val pendingRecallTypingLatencyMillis: Long? = null,
    val undoableReview: UndoableSessionReviewRecord? = null,
    val completionSnapshot: SessionCompletionSnapshotRecord? = null,
    val completionProvenance: String? = null,
    val topicId: String? = null,
    val installedPackageId: String? = null,
    val studyMode: String = "ADAPTIVE",
    val recallModeHistory: List<RecallModeHistoryEntryRecord> = emptyList()
) {

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}

@Serializable
data class RecallModeHistoryEntryRecord(
    val mode: String,
    val direction: String,
    val outcome: String? = null,
    val usedAtEpochMillis: Long,
    val assistanceUsed: Boolean = false
)

@Serializable
data class SessionCompletionSnapshotRecord(
    val whatWasLearned: String,
    val overallOutcome: String,
    val reflection: String,
    val reinforcement: String,
    val whatHappensNext: String,
    val schedulingGuidance: String,
    val scheduledIntervalMillis: Long,
    val nextReviewAtEpochMillis: Long
)

@Serializable
data class UndoableSessionReviewRecord(
    val reviewEventId: String,
    val learningItemId: String,
    val contentId: String,
    val memoryStateBefore: MemoryStateRecord,
    val memoryStateExistedBefore: Boolean,
    val reviewedItemIdsBefore: List<String>,
    val reviewedContentIdsBefore: List<String>,
    val lapsedContentIdsBefore: List<String> = emptyList(),
    val newItemsReviewedBefore: Int,
    val reviewItemsReviewedBefore: Int,
    val currentItemPresentedAtBeforeEpochMillis: Long?,
    val answerRevealedBefore: Boolean,
    val advancesSessionProgress: Boolean = true,
    val trajectoryChanged: Boolean = false,
    val learningTrajectoryBefore: LearningTrajectoryRecord? = null
)
