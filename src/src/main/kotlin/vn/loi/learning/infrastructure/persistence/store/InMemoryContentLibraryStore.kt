package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.ContentLibraryRecord

class InMemoryContentLibraryStore(
    initialRecords: List<ContentLibraryRecord> = emptyList()
) : ContentLibraryStore {

    private var records: List<ContentLibraryRecord> =
        initialRecords.toList()

    override fun loadAll(): List<ContentLibraryRecord> =
        records.toList()

    override fun saveAll(
        records: List<ContentLibraryRecord>
    ) {
        this.records = records.toList()
    }
}