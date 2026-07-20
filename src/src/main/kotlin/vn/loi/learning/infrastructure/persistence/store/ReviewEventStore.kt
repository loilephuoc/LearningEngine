package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.ReviewEventRecord

/**
 * Infrastructure persistence abstraction cho ReviewEvent.
 *
 * Store chỉ làm việc với Persistence Record.
 *
 * Store không biết Domain,
 * không biết Repository,
 * không biết LearningEngine.
 */
interface ReviewEventStore {

    /**
     * Đọc toàn bộ ReviewEventRecord.
     */
    fun loadAll(): List<ReviewEventRecord>

    /**
     * Ghi đè toàn bộ snapshot.
     */
    fun saveAll(
        records: List<ReviewEventRecord>
    )
}