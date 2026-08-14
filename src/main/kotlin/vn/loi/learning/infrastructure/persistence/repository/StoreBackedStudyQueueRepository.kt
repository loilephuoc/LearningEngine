package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.StudyQueueRepository
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.infrastructure.persistence.mapper.StudyQueueRecordMapper
import vn.loi.learning.infrastructure.persistence.store.StudyQueueStore

/**
 * StudyQueueRepository backed by a StudyQueueStore.
 *
 * Repository làm việc với StudyQueueSnapshot.
 * Store làm việc với Persistence Record.
 */
class StoreBackedStudyQueueRepository(
    private val store: StudyQueueStore
) : StudyQueueRepository {

    override fun findAll(): List<StudyQueueSnapshot> =
        store.loadAll().map(StudyQueueRecordMapper::toDomain)

    override fun findBySessionId(
        sessionId: SessionId
    ): StudyQueueSnapshot? =
        store.loadAll()
            .firstOrNull { record ->
                record.sessionId == sessionId.toString()
            }
            ?.let(StudyQueueRecordMapper::toDomain)

    override fun save(
        snapshot: StudyQueueSnapshot
    ) {
        val record =
            StudyQueueRecordMapper.toRecord(snapshot)

        val existingRecords =
            store.loadAll()

        val existingRecord =
            existingRecords.firstOrNull { existing ->
                existing.sessionId == record.sessionId
            }

        if (existingRecord == record) {
            return
        }

        val updatedRecords =
            existingRecords.filterNot { existing ->
                existing.sessionId == record.sessionId
            } + record

        store.saveAll(updatedRecords)
    }

    override fun deleteBySessionId(
        sessionId: SessionId
    ) {
        val existingRecords =
            store.loadAll()

        val updatedRecords =
            existingRecords.filterNot { existing ->
                existing.sessionId == sessionId.toString()
            }

        if (updatedRecords.size == existingRecords.size) {
            return
        }

        store.saveAll(updatedRecords)
    }
}
