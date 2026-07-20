package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.MemoryStateRecord

class InMemoryMemoryStateStore : MemoryStateStore {

    private var records =
        emptyList<MemoryStateRecord>()

    override fun load(): List<MemoryStateRecord> =
        records

    override fun save(
        records: List<MemoryStateRecord>
    ) {
        this.records = records.toList()
    }
}