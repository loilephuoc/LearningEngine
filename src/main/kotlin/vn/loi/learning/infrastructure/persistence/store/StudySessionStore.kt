package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.StudySessionRecord

/**
 * Infrastructure persistence abstraction cho StudySession.
 *
 * Store chỉ làm việc với Persistence Record.
 *
 * Store không biết Domain,
 * không biết Repository,
 * không biết LearningEngine.
 */
interface StudySessionStore {
    /**
     * Đọc toàn bộ StudySessionRecord.
     */
    fun loadAll(): List<StudySessionRecord>

    /**
     * Ghi đè toàn bộ snapshot.
     */
    fun saveAll(
        records: List<StudySessionRecord>
    )
}
