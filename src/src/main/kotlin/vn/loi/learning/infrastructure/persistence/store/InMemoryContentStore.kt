package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.ContentRecord

class InMemoryContentStore(
    initialRecords: List<ContentRecord> = emptyList()
 ) : ContentStore {

    private var records =
        initialRecords.toList()

    override fun loadAll(): List<ContentRecord> =
        records.toList()

    override fun saveAll(
        records: List<ContentRecord>
    ) {
        this.records = records.toList()
    }
}
