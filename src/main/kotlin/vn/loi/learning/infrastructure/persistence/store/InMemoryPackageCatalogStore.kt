package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.PackageCatalogRecord

class InMemoryPackageCatalogStore(
    initialRecords: List<PackageCatalogRecord> = emptyList()
 ) : PackageCatalogStore {

    private var records =
        initialRecords.toList()

    override fun loadAll(): List<PackageCatalogRecord> =
        records.toList()

    override fun saveAll(
        records: List<PackageCatalogRecord>
    ) {
        this.records = records.toList()
    }
}
