package vn.loi.learning.application.port

import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.study.session.model.SessionId

interface StudyQueueRepository {

    fun findBySessionId(
        sessionId: SessionId
    ): StudyQueueSnapshot?

    fun save(
        snapshot: StudyQueueSnapshot
    )

    fun deleteBySessionId(
        sessionId: SessionId
    )
}