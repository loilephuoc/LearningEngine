package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.CanonicalCollectionRecord

interface CanonicalCollectionStore {
    fun loadAll(): List<CanonicalCollectionRecord>
    fun saveAll(records: List<CanonicalCollectionRecord>)
}
