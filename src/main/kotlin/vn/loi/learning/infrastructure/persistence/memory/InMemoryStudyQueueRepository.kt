package vn.loi.learning.infrastructure.persistence.memory

import vn.loi.learning.application.port.StudyQueueRepository
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.study.session.model.SessionId

class InMemoryStudyQueueRepository :
    StudyQueueRepository {

    private val snapshots =
        linkedMapOf<
                SessionId,
                StudyQueueSnapshot
                >()

    override fun findBySessionId(
        sessionId: SessionId
    ): StudyQueueSnapshot? =
        snapshots[sessionId]

    override fun save(
        snapshot: StudyQueueSnapshot
    ) {
        snapshots[snapshot.sessionId] =
            snapshot
    }

    override fun deleteBySessionId(
        sessionId: SessionId
    ) {
        snapshots.remove(sessionId)
    }

    fun count(): Int =
        snapshots.size

    fun clear() {
        snapshots.clear()
    }
}