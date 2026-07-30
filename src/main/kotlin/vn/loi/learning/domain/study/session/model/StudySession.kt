package vn.loi.learning.domain.study.session.model

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
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
 *
 * includedContentIds là phạm vi nội dung bất biến của session.
 * Set rỗng có nghĩa là session không bị giới hạn theo Content.
 */
data class StudySession(
    val id: SessionId,
    val learnerId: LearnerId,
    val startedAt: Moment,
    val status: SessionStatus,
    val policy: SessionPolicy,
    val includedContentIds: Set<ContentId>,
    val reviewedItemIds: Set<LearningItemId>,
    val reviewedContentIds: Set<ContentId>,
    val newItemsReviewed: Int,
    val reviewItemsReviewed: Int,
    val finishedAt: Moment?,
    val currentLearningItemId: LearningItemId? = null,
    val currentItemPresentedAt: Moment? = null,
    val answerRevealed: Boolean = false,
    val pendingReview: PendingSessionReview? = null,
    val undoableReview: UndoableSessionReview? = null,
    val completionSnapshot: SessionCompletionSnapshot? = null,
    val topicId: TopicId? = null,
    val installedPackageId: InstalledPackageId? = null,
    val introducedContentIds: Set<ContentId> = emptySet()
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

        require(currentLearningItemId != null || currentItemPresentedAt == null) {
            "A presentation timestamp requires a current learning item."
        }
        require(currentLearningItemId != null || !answerRevealed) {
            "A revealed answer requires a current learning item."
        }
        require(pendingReview == null || pendingReview.learningItemId == currentLearningItemId) {
            "A pending review must target the current learning item."
        }
        require(status == SessionStatus.ACTIVE || currentLearningItemId == null) {
            "A finished session must not retain a current learning item."
        }
        require(status == SessionStatus.FINISHED || completionSnapshot == null) {
            "Only a finished session may retain a completion snapshot."
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
        wasNewItem: Boolean,
        undoableReview: UndoableSessionReview? = null
    ): StudySession {
        require(status == SessionStatus.ACTIVE) {
            "Cannot record a review in a finished session."
        }

        val firstContentCompletionInSession = contentId !in reviewedContentIds

        if (wasNewItem && firstContentCompletionInSession) {
            require(canReviewNewItem) {
                "The session new item limit has been reached."
            }
        } else if (!wasNewItem && firstContentCompletionInSession) {
            require(canReviewDueItem) {
                "The session review item limit has been reached."
            }
        }

        return copy(
            reviewedItemIds =
                reviewedItemIds + learningItemId,

            reviewedContentIds =
                reviewedContentIds + contentId,
            introducedContentIds =
                introducedContentIds + contentId,

            newItemsReviewed =
                newItemsReviewed + if (wasNewItem && firstContentCompletionInSession) 1 else 0,

            reviewItemsReviewed =
                reviewItemsReviewed + if (!wasNewItem && firstContentCompletionInSession) 1 else 0,
            currentLearningItemId = null,
            currentItemPresentedAt = null,
            answerRevealed = false,
            pendingReview = null,
            undoableReview = undoableReview
        )
    }

    fun undoLatestReview(): StudySession {
        val undo = requireNotNull(undoableReview) { "There is no review to undo." }
        val expectedCounterDelta =
            if (undo.contentId in undo.reviewedContentIdsBefore) 0 else 1
        require(
            totalReviews ==
                undo.newItemsReviewedBefore +
                undo.reviewItemsReviewedBefore +
                expectedCounterDelta
        ) {
            "Only the latest review can be undone."
        }
        return copy(
            status = SessionStatus.ACTIVE,
            reviewedItemIds = undo.reviewedItemIdsBefore,
            reviewedContentIds = undo.reviewedContentIdsBefore,
            newItemsReviewed = undo.newItemsReviewedBefore,
            reviewItemsReviewed = undo.reviewItemsReviewedBefore,
            finishedAt = null,
            currentLearningItemId = undo.learningItemId,
            currentItemPresentedAt = undo.currentItemPresentedAtBefore,
            answerRevealed = undo.answerRevealedBefore,
            pendingReview = null,
            undoableReview = null,
            completionSnapshot = null
        )
    }

    fun presentItem(
        learningItemId: LearningItemId,
        presentedAt: Moment
    ): StudySession {
        require(status == SessionStatus.ACTIVE) {
            "Cannot present an item in a finished session."
        }
        require(pendingReview == null) {
            "Cannot replace an item while its review is pending."
        }
        if (currentLearningItemId == learningItemId) return this
        return copy(
            currentLearningItemId = learningItemId,
            currentItemPresentedAt = presentedAt,
            answerRevealed = false
        )
    }

    fun revealCurrentItem(learningItemId: LearningItemId): StudySession {
        require(currentLearningItemId == learningItemId) {
            "Only the current learning item can be revealed."
        }
        return copy(answerRevealed = true)
    }

    fun completeIntroduction(contentId: ContentId): StudySession {
        require(status == SessionStatus.ACTIVE) {
            "Cannot complete an introduction in a finished session."
        }
        require(currentLearningItemId != null) {
            "An introduction requires a current learning item."
        }
        return copy(introducedContentIds = introducedContentIds + contentId)
    }

    fun completeIntroductionAndReveal(
        contentId: ContentId,
        learningItemId: LearningItemId
    ): StudySession {
        require(currentLearningItemId == learningItemId) {
            "Only the current learning item can complete Introduction."
        }
        return completeIntroduction(contentId).copy(answerRevealed = true)
    }

    fun stageReview(review: PendingSessionReview): StudySession {
        require(status == SessionStatus.ACTIVE) {
            "Cannot review an item in a finished session."
        }
        require(review.learningItemId == currentLearningItemId) {
            "Only the current learning item can be reviewed."
        }
        require(pendingReview == null || pendingReview == review) {
            "Another review is already pending."
        }
        return copy(pendingReview = review)
    }

    fun finish(
        at: Moment,
        completionSnapshot: SessionCompletionSnapshot? = null
    ): StudySession {
        require(status == SessionStatus.ACTIVE) {
            "Session is already finished."
        }

        require(at >= startedAt) {
            "Session cannot finish before it starts."
        }

        return copy(
            status = SessionStatus.FINISHED,
            finishedAt = at,
            currentLearningItemId = null,
            currentItemPresentedAt = null,
            answerRevealed = false,
            pendingReview = null,
            completionSnapshot = completionSnapshot
        )
    }

    /**
     * Closes an intentionally left practice session.
     *
     * Committed reviews remain part of history. Navigation-only queue state and the single-level
     * Undo checkpoint are released because neither may cross into a replacement practice source.
     */
    fun leave(at: Moment): StudySession =
        finish(at).copy(undoableReview = null)

    companion object {

        fun start(
            id: SessionId,
            learnerId: LearnerId,
            startedAt: Moment,
            policy: SessionPolicy,
            includedContentIds: Set<ContentId> = emptySet(),
            topicId: TopicId? = null,
            installedPackageId: InstalledPackageId? = null
        ): StudySession =
            StudySession(
                id = id,
                learnerId = learnerId,
                startedAt = startedAt,
                status = SessionStatus.ACTIVE,
                policy = policy,
                includedContentIds =
                    includedContentIds.toSet(),
                topicId = topicId,
                installedPackageId = installedPackageId,
                reviewedItemIds = emptySet(),
                reviewedContentIds = emptySet(),
                introducedContentIds = emptySet(),
                newItemsReviewed = 0,
                reviewItemsReviewed = 0,
                finishedAt = null
            )
    }
}
