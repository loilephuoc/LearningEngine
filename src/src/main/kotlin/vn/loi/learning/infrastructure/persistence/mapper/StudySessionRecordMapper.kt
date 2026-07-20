package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.record.StudySessionRecord

/**
 * Chuyển đổi giữa StudySession thuộc Domain
 * và StudySessionRecord thuộc Infrastructure.
 *
 * Mapper chỉ chịu trách nhiệm chuyển đổi định dạng dữ liệu.
 *
 * Các invariant nghiệp vụ vẫn được kiểm tra bởi:
 * - các Value Object;
 * - SessionPolicy;
 * - StudySession.
 */
object StudySessionRecordMapper {

    fun toRecord(
        studySession: StudySession
    ): StudySessionRecord =
        StudySessionRecord(
            schemaVersion =
                StudySessionRecord.CURRENT_SCHEMA_VERSION,
            id =
                studySession.id.toString(),
            learnerId =
                studySession.learnerId.toString(),
            startedAtEpochMillis =
                studySession.startedAt.epochMillis,
            status =
                studySession.status.name,

            policyNewItemLimit =
                studySession.policy.newItemLimit,
            policyReviewItemLimit =
                studySession.policy.reviewItemLimit,
            policyAllowRepeatInSameSession =
                studySession.policy.allowRepeatInSameSession,

            reviewedItemIds =
                studySession.reviewedItemIds
                    .map(LearningItemId::toString),
            reviewedContentIds =
                studySession.reviewedContentIds
                    .map(ContentId::toString),

            newItemsReviewed =
                studySession.newItemsReviewed,
            reviewItemsReviewed =
                studySession.reviewItemsReviewed,
            finishedAtEpochMillis =
                studySession.finishedAt?.epochMillis
        )

    fun toDomain(
        record: StudySessionRecord
    ): StudySession {
        require(
            record.schemaVersion ==
                    StudySessionRecord.CURRENT_SCHEMA_VERSION
        ) {
            "Unsupported StudySessionRecord schema version: " +
                    "${record.schemaVersion}."
        }

        val status =
            try {
                SessionStatus.valueOf(record.status)
            } catch (exception: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Unknown SessionStatus '${record.status}'.",
                    exception
                )
            }

        val policy =
            SessionPolicy(
                newItemLimit =
                    record.policyNewItemLimit,
                reviewItemLimit =
                    record.policyReviewItemLimit,
                allowRepeatInSameSession =
                    record.policyAllowRepeatInSameSession
            )

        return StudySession(
            id =
                SessionId(record.id),
            learnerId =
                LearnerId(record.learnerId),
            startedAt =
                Moment(record.startedAtEpochMillis),
            status =
                status,
            policy =
                policy,
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
                record.finishedAtEpochMillis?.let(::Moment)
        )
    }
}