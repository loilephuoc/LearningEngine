package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.StudyQueueRecord

/**
 * Infrastructure persistence abstraction cho StudyQueueSnapshot.
 *
 * Store chỉ làm việc với Persistence Record.
 */
interface StudyQueueStore {

    fun loadAll(): List<StudyQueueRecord>

    fun saveAll(
        records: List<StudyQueueRecord>
    )
}