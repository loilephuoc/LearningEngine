package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.persistence.mapper.StudySessionRecordMapper
import vn.loi.learning.infrastructure.persistence.store.StudySessionStore

/**
 * StudySessionRepository backed by a StudySessionStore.
 *
 * Repository làm việc với Domain.
 * Store làm việc với Persistence Record.
 */
class StoreBackedStudySessionRepository(
    private val store: StudySessionStore
) : StudySessionRepository {

    override fun save(
        session: StudySession
    ) {
        val record =
            StudySessionRecordMapper.toRecord(session)

        val updatedRecords =
            store.loadAll()
                .filterNot {
                    it.id == record.id
                } + record

        store.saveAll(updatedRecords)
    }

    override fun findById(
        sessionId: SessionId
    ): StudySession? =
        store.loadAll()
            .firstOrNull {
                it.id == sessionId.toString()
            }
            ?.let(
                StudySessionRecordMapper::toDomain
            )
}