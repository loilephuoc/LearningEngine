package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.infrastructure.persistence.record.ReviewEventRecord

/**
 * Chuyển đổi giữa ReviewEvent thuộc Domain
 * và ReviewEventRecord thuộc Infrastructure.
 *
 * Mapper chỉ chịu trách nhiệm chuyển đổi định dạng dữ liệu.
 *
 * Các invariant nghiệp vụ vẫn được kiểm tra bởi:
 * - MemoryStateRecordMapper;
 * - constructor của MemoryState;
 * - constructor của ReviewEvent.
 */
object ReviewEventRecordMapper {

    fun toRecord(
        reviewEvent: ReviewEvent
    ): ReviewEventRecord =
        ReviewEventRecord(
            schemaVersion =
                ReviewEventRecord.CURRENT_SCHEMA_VERSION,
            id =
                reviewEvent.id.toString(),
            rating =
                reviewEvent.rating.name,
            reviewedAtEpochMillis =
                reviewEvent.reviewedAt.epochMillis,
            responseTimeMillis =
                reviewEvent.responseTime?.millis,
            stateBefore =
                MemoryStateRecordMapper.toRecord(
                    reviewEvent.stateBefore
                ),
            stateAfter =
                MemoryStateRecordMapper.toRecord(
                    reviewEvent.stateAfter
                )
        )

    fun toDomain(
        record: ReviewEventRecord
    ): ReviewEvent {
        require(
            record.schemaVersion ==
                    ReviewEventRecord.CURRENT_SCHEMA_VERSION
        ) {
            "Unsupported ReviewEventRecord schema version: " +
                    "${record.schemaVersion}."
        }

        val rating =
            try {
                ReviewRating.valueOf(record.rating)
            } catch (exception: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Unknown ReviewRating '${record.rating}'.",
                    exception
                )
            }

        val stateBefore =
            MemoryStateRecordMapper.toDomain(
                record.stateBefore
            )

        val stateAfter =
            MemoryStateRecordMapper.toDomain(
                record.stateAfter
            )

        return ReviewEvent(
            id =
                ReviewEventId(record.id),
            rating =
                rating,
            reviewedAt =
                Moment(record.reviewedAtEpochMillis),
            responseTime =
                record.responseTimeMillis?.let(::TimeSpan),
            stateBefore =
                stateBefore,
            stateAfter =
                stateAfter
        )
    }
}