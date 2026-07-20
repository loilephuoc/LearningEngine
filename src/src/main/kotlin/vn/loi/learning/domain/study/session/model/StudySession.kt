package vn.loi.learning.domain.study.session.model

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment

/**
 * Trạng thái của một phiên học.
 *
 * Session theo dõi LearningItem và Content đã xuất hiện để:
 * - tránh lặp lại cùng LearningItem;
 * - tránh các sibling của cùng Content xuất hiện liên tiếp;
 * - giữ giới hạn bài mới và bài review.
 */
data class StudySession(
    val id: SessionId,
    val learnerId: LearnerId,
    val startedAt: Moment,
    val status: SessionStatus,
    val policy: SessionPolicy,
    val reviewedItemIds: Set<LearningItemId>,
    val reviewedContentIds: Set<ContentId>,
    val newItemsReviewed: Int,
    val reviewItemsReviewed: Int,
    val finishedAt: Moment?
) {

    init {
        require(newItemsReviewed >= 0) {
            "New items reviewed must not be negative."
        }

        require(reviewItemsReviewed >= 0) {
            "Review items reviewed must not be negative."
        }

        require(newItemsReviewed <= policy.newItemLimit) {
            "New item count exceeds the session limit."
        }

        require(reviewItemsReviewed <= policy.reviewItemLimit) {
            "Review item count exceeds the session limit."
        }

        when (status) {
            SessionStatus.ACTIVE -> require(finishedAt == null) {
                "An active session must not have finishedAt."
            }

            SessionStatus.FINISHED -> require(finishedAt != null) {
                "A finished session must have finishedAt."
            }
        }

        if (finishedAt != null) {
            require(finishedAt >= startedAt) {
                "Session cannot finish before it starts."
            }
        }
    }

    val totalReviews: Int
        get() = newItemsReviewed + reviewItemsReviewed

    val canReviewNewItem: Boolean
        get() =
            status == SessionStatus.ACTIVE &&
                    newItemsReviewed < policy.newItemLimit

    val canReviewDueItem: Boolean
        get() =
            status == SessionStatus.ACTIVE &&
                    reviewItemsReviewed < policy.reviewItemLimit

    fun recordReview(
        learningItemId: LearningItemId,
        contentId: ContentId,
        wasNewItem: Boolean
    ): StudySession {
        require(status == SessionStatus.ACTIVE) {
            "Cannot record a review in a finished session."
        }

        if (wasNewItem) {
            require(canReviewNewItem) {
                "The session new item limit has been reached."
            }
        } else {
            require(canReviewDueItem) {
                "The session review item limit has been reached."
            }
        }

        return copy(
            reviewedItemIds =
                reviewedItemIds + learningItemId,

            reviewedContentIds =
                reviewedContentIds + contentId,

            newItemsReviewed =
                newItemsReviewed + if (wasNewItem) 1 else 0,

            reviewItemsReviewed =
                reviewItemsReviewed + if (wasNewItem) 0 else 1
        )
    }

    fun finish(at: Moment): StudySession {
        require(status == SessionStatus.ACTIVE) {
            "Session is already finished."
        }

        require(at >= startedAt) {
            "Session cannot finish before it starts."
        }

        return copy(
            status = SessionStatus.FINISHED,
            finishedAt = at
        )
    }

    companion object {

        fun start(
            id: SessionId,
            learnerId: LearnerId,
            startedAt: Moment,
            policy: SessionPolicy
        ): StudySession =
            StudySession(
                id = id,
                learnerId = learnerId,
                startedAt = startedAt,
                status = SessionStatus.ACTIVE,
                policy = policy,
                reviewedItemIds = emptySet(),
                reviewedContentIds = emptySet(),
                newItemsReviewed = 0,
                reviewItemsReviewed = 0,
                finishedAt = null
            )
    }
}