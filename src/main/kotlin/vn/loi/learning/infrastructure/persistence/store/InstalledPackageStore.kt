package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.InstalledPackageRecord

interface InstalledPackageStore {
    fun loadAll(): List<InstalledPackageRecord>
    fun saveAll(records: List<InstalledPackageRecord>)
}
