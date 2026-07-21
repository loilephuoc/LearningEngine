package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.LibraryCollectionRecord

class InMemoryLibraryCollectionStore(
    initialRecords: List<LibraryCollectionRecord> = emptyList()
) : LibraryCollectionStore {

    private var records: List<LibraryCollectionRecord> =
        initialRecords.toList()

    override fun loadAll(): List<LibraryCollectionRecord> =
        records.toList()

    override fun saveAll(
        records: List<LibraryCollectionRecord>
    ) {
        this.records = records.toList()
    }
}