package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.ContentLibraryRecord

interface ContentLibraryStore {

    fun loadAll(): List<ContentLibraryRecord>

    fun saveAll(
        records: List<ContentLibraryRecord>
    )
}