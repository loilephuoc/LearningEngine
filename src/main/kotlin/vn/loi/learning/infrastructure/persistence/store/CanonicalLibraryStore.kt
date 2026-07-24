package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.CanonicalLibraryRecord

interface CanonicalLibraryStore {
    fun loadAll(): List<CanonicalLibraryRecord>
    fun saveAll(records: List<CanonicalLibraryRecord>)
}
