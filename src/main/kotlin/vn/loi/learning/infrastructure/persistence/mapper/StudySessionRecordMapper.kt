package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.PendingSessionReview
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionCompletionSnapshot
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.domain.study.session.model.UndoableSessionReview
import vn.loi.learning.infrastructure.persistence.record.StudySessionRecord
import vn.loi.learning.infrastructure.persistence.record.SessionCompletionSnapshotRecord
import vn.loi.learning.infrastructure.persistence.record.UndoableSessionReviewRecord

object StudySessionRecordMapper {

    fun toRecord(
        session: StudySession
    ): StudySessionRecord =
        StudySessionRecord(
            schemaVersion =
                StudySessionRecord
                    .CURRENT_SCHEMA_VERSION,

            id =
                session.id.value,

            learnerId =
                session.learnerId.value,

            startedAtEpochMillis =
                session.startedAt.epochMillis,

            status =
                session.status.name,

            policyNewItemLimit =
                session.policy.newItemLimit,

            policyReviewItemLimit =
                session.policy.reviewItemLimit,

            policyAllowRepeatInSameSession =
                session.policy
                    .allowRepeatInSameSession,

            includedContentIds =
                session.includedContentIds
                    .map { contentId ->
                        contentId.value
                    }
                    .sorted(),

            reviewedItemIds =
                session.reviewedItemIds
                    .map { learningItemId ->
                        learningItemId.value
                    }
                    .sorted(),

            reviewedContentIds =
                session.reviewedContentIds
                    .map { contentId ->
                        contentId.value
                    }
                    .sorted(),

            newItemsReviewed =
                session.newItemsReviewed,

            reviewItemsReviewed =
                session.reviewItemsReviewed,

            finishedAtEpochMillis =
                session.finishedAt
                    ?.epochMillis,
            currentLearningItemId = session.currentLearningItemId?.value,
            currentItemPresentedAtEpochMillis = session.currentItemPresentedAt?.epochMillis,
            answerRevealed = session.answerRevealed,
            pendingReviewEventId = session.pendingReview?.reviewEventId?.value,
            pendingReviewLearningItemId = session.pendingReview?.learningItemId?.value,
            pendingReviewRating = session.pendingReview?.rating?.name,
            pendingReviewReviewedAtEpochMillis = session.pendingReview?.reviewedAt?.epochMillis,
            pendingReviewResponseTimeMillis = session.pendingReview?.responseTime?.millis,
            undoableReview = session.undoableReview?.let(::toUndoRecord),
            completionSnapshot = session.completionSnapshot?.let(::toCompletionRecord)
        )

    fun toDomain(
        record: StudySessionRecord
    ): StudySession {
        require(
            record.schemaVersion ==
                    StudySessionRecord
                        .CURRENT_SCHEMA_VERSION
        ) {
            "Unsupported StudySessionRecord schema version: " +
                    "${record.schemaVersion}."
        }

        return StudySession(
            id =
                SessionId(record.id),

            learnerId =
                LearnerId(record.learnerId),

            startedAt =
                Moment(
                    record.startedAtEpochMillis
                ),

            status =
                SessionStatus.valueOf(
                    record.status
                ),

            policy =
                SessionPolicy(
                    newItemLimit =
                        record.policyNewItemLimit,

                    reviewItemLimit =
                        record.policyReviewItemLimit,

                    allowRepeatInSameSession =
                        record
                            .policyAllowRepeatInSameSession
                ),

            includedContentIds =
                record.includedContentIds
                    .map(::ContentId)
                    .toSet(),

            reviewedItemIds =
                record.reviewedItemIds
                    .map(::LearningItemId)
                    .toSet(),

            reviewedContentIds =
                record.reviewedContentIds
                    .map(::ContentId)
                    .toSet(),

            newItemsReviewed =
                record.newItemsReviewed,

            reviewItemsReviewed =
                record.reviewItemsReviewed,

            finishedAt =
                record.finishedAtEpochMillis
                    ?.let(::Moment),
            currentLearningItemId = record.currentLearningItemId?.let(::LearningItemId),
            currentItemPresentedAt = record.currentItemPresentedAtEpochMillis?.let(::Moment),
            answerRevealed = record.answerRevealed,
            pendingReview = toPendingReview(record),
            undoableReview = record.undoableReview?.let(::toUndoDomain),
            completionSnapshot = record.completionSnapshot?.let(::toCompletionDomain)
        )
    }

    private fun toPendingReview(record: StudySessionRecord): PendingSessionReview? {
        val eventId = record.pendingReviewEventId ?: return null
        return PendingSessionReview(
            reviewEventId = ReviewEventId(eventId),
            learningItemId = LearningItemId(requireNotNull(record.pendingReviewLearningItemId)),
            rating = ReviewRating.valueOf(requireNotNull(record.pendingReviewRating)),
            reviewedAt = Moment(requireNotNull(record.pendingReviewReviewedAtEpochMillis)),
            responseTime = record.pendingReviewResponseTimeMillis?.let(::TimeSpan)
        )
    }

    private fun toUndoRecord(undo: UndoableSessionReview) = UndoableSessionReviewRecord(
        reviewEventId = undo.reviewEventId.value,
        learningItemId = undo.learningItemId.value,
        contentId = undo.contentId.value,
        memoryStateBefore = MemoryStateRecordMapper.toRecord(undo.memoryStateBefore),
        memoryStateExistedBefore = undo.memoryStateExistedBefore,
        reviewedItemIdsBefore = undo.reviewedItemIdsBefore.map { it.value }.sorted(),
        reviewedContentIdsBefore = undo.reviewedContentIdsBefore.map { it.value }.sorted(),
        newItemsReviewedBefore = undo.newItemsReviewedBefore,
        reviewItemsReviewedBefore = undo.reviewItemsReviewedBefore,
        currentItemPresentedAtBeforeEpochMillis = undo.currentItemPresentedAtBefore?.epochMillis,
        answerRevealedBefore = undo.answerRevealedBefore
    )

    private fun toUndoDomain(record: UndoableSessionReviewRecord) = UndoableSessionReview(
        reviewEventId = ReviewEventId(record.reviewEventId),
        learningItemId = LearningItemId(record.learningItemId),
        contentId = ContentId(record.contentId),
        memoryStateBefore = MemoryStateRecordMapper.toDomain(record.memoryStateBefore),
        memoryStateExistedBefore = record.memoryStateExistedBefore,
        reviewedItemIdsBefore = record.reviewedItemIdsBefore.map(::LearningItemId).toSet(),
        reviewedContentIdsBefore = record.reviewedContentIdsBefore.map(::ContentId).toSet(),
        newItemsReviewedBefore = record.newItemsReviewedBefore,
        reviewItemsReviewedBefore = record.reviewItemsReviewedBefore,
        currentItemPresentedAtBefore = record.currentItemPresentedAtBeforeEpochMillis?.let(::Moment),
        answerRevealedBefore = record.answerRevealedBefore
    )

    private fun toCompletionRecord(snapshot: SessionCompletionSnapshot) =
        SessionCompletionSnapshotRecord(
            whatWasLearned = snapshot.whatWasLearned,
            overallOutcome = snapshot.overallOutcome,
            reflection = snapshot.reflection,
            reinforcement = snapshot.reinforcement,
            whatHappensNext = snapshot.whatHappensNext,
            schedulingGuidance = snapshot.schedulingGuidance,
            scheduledIntervalMillis = snapshot.scheduledIntervalMillis,
            nextReviewAtEpochMillis = snapshot.nextReviewAtEpochMillis
        )

    private fun toCompletionDomain(record: SessionCompletionSnapshotRecord) =
        SessionCompletionSnapshot(
            whatWasLearned = record.whatWasLearned,
            overallOutcome = record.overallOutcome,
            reflection = record.reflection,
            reinforcement = record.reinforcement,
            whatHappensNext = record.whatHappensNext,
            schedulingGuidance = record.schedulingGuidance,
            scheduledIntervalMillis = record.scheduledIntervalMillis,
            nextReviewAtEpochMillis = record.nextReviewAtEpochMillis
        )
}
