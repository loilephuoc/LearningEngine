package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

/**
 * Dạng dữ liệu bền vững của ReviewEvent.
 *
 * ReviewEvent lưu cả stateBefore và stateAfter để phục vụ:
 * - audit;
 * - debug Scheduler;
 * - replay;
 * - phân tích thuật toán;
 * - AI trong tương lai.
 */
@Serializable
data class ReviewEventRecord(
    val schemaVersion: Int,
    val id: String,
    val rating: String,
    val reviewedAtEpochMillis: Long,
    val responseTimeMillis: Long?,
    val stateBefore: MemoryStateRecord,
    val stateAfter: MemoryStateRecord,
    val source: String = "STANDARD_REVIEW"
) {

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 2
    }
}
