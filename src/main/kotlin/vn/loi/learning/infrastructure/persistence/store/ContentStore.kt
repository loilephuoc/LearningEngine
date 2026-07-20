package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.ContentRecord

interface ContentStore {

    fun loadAll(): List<ContentRecord>

    fun saveAll(
        records: List<ContentRecord>
    )
}
