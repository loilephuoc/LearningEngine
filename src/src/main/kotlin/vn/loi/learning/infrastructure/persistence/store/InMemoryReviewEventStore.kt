package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.ReviewEventRecord

/**
 * ReviewEventStore chạy hoàn toàn trong bộ nhớ.
 *
 * Implementation này được dùng cho:
 * - unit test;
 * - contract test;
 * - development;
 * - repository adapter trước khi có JSON persistence.
 *
 * Store chỉ biết ReviewEventRecord, không biết Domain ReviewEvent.
 */
class InMemoryReviewEventStore : ReviewEventStore {

    private var records =
        emptyList<ReviewEventRecord>()

    override fun loadAll(): List<ReviewEventRecord> =
        records

    override fun saveAll(
        records: List<ReviewEventRecord>
    ) {
        this.records = records.toList()
    }
}