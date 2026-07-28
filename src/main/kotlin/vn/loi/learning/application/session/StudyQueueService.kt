package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudyQueueRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

/**
 * Application service quản lý vòng đời StudyQueueSnapshot.
 *
 * Service không quyết định item nào đủ điều kiện học.
 * Danh sách LearningItemId phải được selection engine hoặc caller
 * chuẩn bị trước khi tạo queue.
 */
class StudyQueueService(
    private val repository:
    StudyQueueRepository
) {

    fun create(
        sessionId: SessionId,
        createdAt: Moment,
        learningItemIds:
        List<LearningItemId>,
        itemOrigins: Map<LearningItemId, SessionItemOrigin> = emptyMap(),
        itemContentIds: Map<LearningItemId, ContentId> = emptyMap()
    ): StudyQueueSnapshot {
        require(
            repository.findBySessionId(
                sessionId
            ) == null
        ) {
            "Study queue for session " +
                    "$sessionId already exists."
        }

        val snapshot =
            StudyQueueSnapshot.create(
                sessionId = sessionId,
                createdAt = createdAt,
                learningItemIds =
                    learningItemIds,
                itemOrigins = itemOrigins,
                itemContentIds = itemContentIds
            )

        repository.save(snapshot)

        return snapshot
    }

    fun get(
        sessionId: SessionId
    ): StudyQueueSnapshot? =
        repository.findBySessionId(
            sessionId
        )

    fun require(
        sessionId: SessionId
    ): StudyQueueSnapshot =
        requireNotNull(
            repository.findBySessionId(
                sessionId
            )
        ) {
            "Study queue for session " +
                    "$sessionId does not exist."
        }

    fun advance(
        sessionId: SessionId
    ): StudyQueueSnapshot {
        val current =
            require(sessionId)

        val advanced =
            current.advance()

        repository.save(advanced)

        return advanced
    }

    fun rewind(sessionId: SessionId, expectedLearningItemId: LearningItemId): StudyQueueSnapshot {
        val rewound = require(sessionId).rewind(expectedLearningItemId)
        repository.save(rewound)
        return rewound
    }

    fun delete(
        sessionId: SessionId
    ) {
        repository.deleteBySessionId(
            sessionId
        )
    }
}
