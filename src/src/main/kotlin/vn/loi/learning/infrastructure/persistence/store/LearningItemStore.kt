package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.LearningItemRecord

interface LearningItemStore {

    fun loadAll(): List<LearningItemRecord>

    fun saveAll(
        records: List<LearningItemRecord>
    )
}
