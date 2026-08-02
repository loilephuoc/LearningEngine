package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.LearningTrajectoryRecord

interface LearningTrajectoryStore {
    fun loadAll(): List<LearningTrajectoryRecord>
    fun saveAll(records: List<LearningTrajectoryRecord>)
}
