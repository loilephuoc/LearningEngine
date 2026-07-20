package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.PackageCatalogRecord

interface PackageCatalogStore {

    fun loadAll(): List<PackageCatalogRecord>

    fun saveAll(
        records: List<PackageCatalogRecord>
    )
}
