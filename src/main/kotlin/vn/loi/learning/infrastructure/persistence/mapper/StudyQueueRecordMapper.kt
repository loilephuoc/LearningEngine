package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.infrastructure.persistence.record.StudyQueueRecord
import vn.loi.learning.infrastructure.persistence.record.CoverageReinforcementStateRecord
import vn.loi.learning.infrastructure.persistence.record.CoverageReinforcementUndoRecord
import vn.loi.learning.application.session.CoverageReinforcementState
import vn.loi.learning.application.session.CoverageReinforcementUndo

/**
 * Chuyển đổi giữa StudyQueueSnapshot thuộc Application
 * và StudyQueueRecord thuộc Infrastructure.
 */
object StudyQueueRecordMapper {

    fun toRecord(
        snapshot: StudyQueueSnapshot
    ): StudyQueueRecord =
        StudyQueueRecord(
            schemaVersion =
                StudyQueueRecord.CURRENT_SCHEMA_VERSION,
            sessionId =
                snapshot.sessionId.toString(),
            createdAtEpochMillis =
                snapshot.createdAt.epochMillis,
            learningItemIds =
                snapshot.learningItemIds
                    .map(LearningItemId::toString),
            currentIndex =
                snapshot.currentIndex,
            itemOrigins = snapshot.itemOrigins.entries.associate {
                it.key.value to it.value.name
            },
            itemContentIds = snapshot.itemContentIds.entries.associate {
                it.key.value to it.value.value
            },
            configuredNewTarget = snapshot.configuredNewTarget,
            effectiveNewWorkload = snapshot.effectiveNewWorkload,
            configuredReviewTarget = snapshot.configuredReviewTarget,
            effectiveReviewWorkload = snapshot.effectiveReviewWorkload,
            fixedPracticeMembership = snapshot.fixedPracticeMembership.map { it.value },
            practiceSeed = snapshot.practiceSeed,
            practiceRound = snapshot.practiceRound,
            coverageReinforcementStates = snapshot.coverageReinforcementStates.mapKeys { it.key.value }
                .mapValues { it.value.toRecord() },
            coverageReinforcementUndo = snapshot.coverageReinforcementUndo?.let {
                CoverageReinforcementUndoRecord(
                    it.learningItemId.value,
                    it.previousState?.toRecord(),
                    it.discardedTail.map(LearningItemId::value),
                    it.completionTruncation
                )
            }
        )

    fun toDomain(
        record: StudyQueueRecord
    ): StudyQueueSnapshot {
        require(
            record.schemaVersion in 1..StudyQueueRecord.CURRENT_SCHEMA_VERSION
        ) {
            "Unsupported StudyQueueRecord schema version: " +
                    "${record.schemaVersion}."
        }

        return StudyQueueSnapshot(
            sessionId =
                SessionId(record.sessionId),
            createdAt =
                Moment(record.createdAtEpochMillis),
            learningItemIds =
                record.learningItemIds.map(::LearningItemId),
            currentIndex =
                record.currentIndex,
            itemOrigins = record.itemOrigins.mapKeys { LearningItemId(it.key) }
                .mapValues { SessionItemOrigin.valueOf(it.value) },
            itemContentIds = record.itemContentIds.mapKeys { LearningItemId(it.key) }
                .mapValues { ContentId(it.value) },
            configuredNewTarget = record.configuredNewTarget,
            effectiveNewWorkload = record.effectiveNewWorkload,
            configuredReviewTarget = record.configuredReviewTarget,
            effectiveReviewWorkload = record.effectiveReviewWorkload,
            fixedPracticeMembership = record.fixedPracticeMembership.map(::LearningItemId),
            practiceSeed = record.practiceSeed,
            practiceRound = record.practiceRound,
            coverageReinforcementStates = record.coverageReinforcementStates.mapKeys { LearningItemId(it.key) }
                .mapValues { it.value.toDomain() },
            coverageReinforcementUndo = record.coverageReinforcementUndo?.let {
                CoverageReinforcementUndo(
                    LearningItemId(it.learningItemId),
                    it.previousState?.toDomain(),
                    it.discardedTail.map(::LearningItemId),
                    it.completionTruncation
                )
            }
        )
    }

    private fun CoverageReinforcementState.toRecord() = CoverageReinforcementStateRecord(
        reinforcementCount, previousGap, lastInsertionIndex, deferred, schemaVersion
    )

    private fun CoverageReinforcementStateRecord.toDomain() = CoverageReinforcementState(
        reinforcementCount, previousGap, lastInsertionIndex, deferred, schemaVersion
    )
}
