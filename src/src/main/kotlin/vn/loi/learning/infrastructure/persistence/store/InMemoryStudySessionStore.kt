package vn.loi.learning.infrastructure.persistence.store

import vn.loi.learning.infrastructure.persistence.record.StudySessionRecord

/**
 * StudySessionStore chạy hoàn toàn trong bộ nhớ.
 *
 * Chỉ lưu Persistence Record.
 * Không biết Domain.
 */
class InMemoryStudySessionStore : StudySessionStore {

    private var records =
        emptyList<StudySessionRecord>()

    override fun loadAll(): List<StudySessionRecord> =
        records

    override fun saveAll(
        records: List<StudySessionRecord>
    ) {
        this.records = records.toList()
    }
}