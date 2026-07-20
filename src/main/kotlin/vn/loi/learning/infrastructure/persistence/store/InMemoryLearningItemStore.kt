package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.LearningItemRecord

class InMemoryLearningItemStore(
    initialRecords: List<LearningItemRecord> = emptyList()
 ) : LearningItemStore {

    private var records =
        initialRecords.toList()

    override fun loadAll(): List<LearningItemRecord> =
        records.toList()

    override fun saveAll(
        records: List<LearningItemRecord>
    ) {
        this.records = records.toList()
    }
}
