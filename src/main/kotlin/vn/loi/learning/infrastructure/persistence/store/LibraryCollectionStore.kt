package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.LibraryCollectionRecord

interface LibraryCollectionStore {

    fun loadAll(): List<LibraryCollectionRecord>

    fun saveAll(
        records: List<LibraryCollectionRecord>
    )
}