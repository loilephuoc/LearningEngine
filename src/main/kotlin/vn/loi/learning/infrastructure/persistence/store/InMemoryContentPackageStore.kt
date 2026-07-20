package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.PackageRecord

class InMemoryContentPackageStore(
    initialRecords: List<PackageRecord> = emptyList()
) : ContentPackageStore {

    private var records: List<PackageRecord> =
        initialRecords.toList()

    override fun loadAll(): List<PackageRecord> =
        records.toList()

    override fun saveAll(
        records: List<PackageRecord>
    ) {
        this.records = records.toList()
    }
}
