package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudyQueueRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.memory.model.ReviewRating

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
        itemContentIds: Map<LearningItemId, ContentId> = emptyMap(),
        configuredNewTarget: Int = 0,
        effectiveNewWorkload: Int = 0,
        configuredReviewTarget: Int = 0,
        effectiveReviewWorkload: Int = 0
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
            itemContentIds = itemContentIds,
            configuredNewTarget = configuredNewTarget,
            effectiveNewWorkload = effectiveNewWorkload,
            configuredReviewTarget = configuredReviewTarget,
            effectiveReviewWorkload = effectiveReviewWorkload
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

    fun advanceCoverageReview(
        sessionId: SessionId,
        rating: ReviewRating,
        uniqueCoverageComplete: Boolean
    ): StudyQueueSnapshot {
        val advanced =
            require(sessionId).advanceCoverageReview(rating, uniqueCoverageComplete)
        repository.save(advanced)
        return advanced
    }

    fun rewind(sessionId: SessionId, expectedLearningItemId: LearningItemId): StudyQueueSnapshot {
        val current = require(sessionId)
        val rewound =
            if (current.isUniqueCoverageReviewQueue) {
                current.rewindCoverageReview(expectedLearningItemId)
            } else {
                current.rewind(expectedLearningItemId)
            }
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

internal val StudyQueueSnapshot.isUniqueCoverageReviewQueue: Boolean
    get() =
        configuredNewTarget == 0 &&
            configuredReviewTarget > 0 &&
            effectiveReviewWorkload == configuredReviewTarget &&
            itemOrigins.values.all { it == SessionItemOrigin.REVIEW }
