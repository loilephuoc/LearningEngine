package vn.loi.learning.infrastructure.persistence.record

import kotlinx.serialization.Serializable

/**
 * Dạng dữ liệu bền vững của MemoryState.
 *
 * Record thuộc Infrastructure, không phải Domain.
 * Vì vậy annotation serialization chỉ xuất hiện tại đây.
 */
@Serializable
data class MemoryStateRecord(
    val schemaVersion: Int,
    val learnerId: String,
    val learningItemId: String,
    val stage: String,
    val difficulty: Double,
    val stabilityDays: Double,
    val dueAtEpochMillis: Long,
    val lastReviewedAtEpochMillis: Long?,
    val reviewCount: Int,
    val lapseCount: Int
) {

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}