package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.PackageRecord

interface ContentPackageStore {

    fun loadAll(): List<PackageRecord>

    fun saveAll(
        records: List<PackageRecord>
    )
}
