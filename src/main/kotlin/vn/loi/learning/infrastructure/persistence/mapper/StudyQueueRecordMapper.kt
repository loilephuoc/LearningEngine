package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.infrastructure.persistence.record.StudyQueueRecord

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
                .mapValues { SessionItemOrigin.valueOf(it.value) }
        )
    }
}
