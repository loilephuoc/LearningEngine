package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.persistence.record.MemoryStateRecord

object MemoryStateRecordMapper {

    fun toRecord(
        memoryState: MemoryState
    ): MemoryStateRecord =
        MemoryStateRecord(
            schemaVersion =
                MemoryStateRecord.CURRENT_SCHEMA_VERSION,
            learnerId =
                memoryState.learnerId.toString(),
            learningItemId =
                memoryState.learningItemId.toString(),
            stage =
                memoryState.stage.name,
            difficulty =
                memoryState.difficulty,
            stabilityDays =
                memoryState.stabilityDays,
            dueAtEpochMillis =
                memoryState.dueAt.epochMillis,
            lastReviewedAtEpochMillis =
                memoryState.lastReviewedAt?.epochMillis,
            reviewCount =
                memoryState.reviewCount,
            lapseCount =
                memoryState.lapseCount
        )

    fun toDomain(
        record: MemoryStateRecord
    ): MemoryState {
        require(
            record.schemaVersion ==
                    MemoryStateRecord.CURRENT_SCHEMA_VERSION
        ) {
            "Unsupported MemoryStateRecord schema version: " +
                    "${record.schemaVersion}."
        }

        val stage =
            try {
                LearningStage.valueOf(record.stage)
            } catch (exception: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Unknown LearningStage '${record.stage}'.",
                    exception
                )
            }

        return MemoryState(
            learnerId =
                LearnerId(record.learnerId),
            learningItemId =
                LearningItemId(record.learningItemId),
            stage =
                stage,
            difficulty =
                record.difficulty,
            stabilityDays =
                record.stabilityDays,
            dueAt =
                Moment(record.dueAtEpochMillis),
            lastReviewedAt =
                record.lastReviewedAtEpochMillis?.let(::Moment),
            reviewCount =
                record.reviewCount,
            lapseCount =
                record.lapseCount
        )
    }
}