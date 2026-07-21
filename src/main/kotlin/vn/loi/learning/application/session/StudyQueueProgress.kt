package vn.loi.learning.application.session

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId

/**
 * Read model chỉ đọc dành cho UI hoặc adapter bên ngoài.
 *
 * Model này không cho phép thay đổi queue và không chứa repository,
 * scheduling hoặc selection logic.
 */
data class StudyQueueProgress(
    val sessionId: SessionId,
    val createdAt: Moment,
    val totalItemCount: Int,
    val completedItemCount: Int,
    val remainingItemCount: Int,
    val currentIndex: Int,
    val currentLearningItemId: LearningItemId?,
    val previousLearningItemId: LearningItemId?,
    val nextLearningItemId: LearningItemId?,
    val completedLearningItemIds:
    List<LearningItemId>,
    val remainingLearningItemIds:
    List<LearningItemId>,
    val pendingLearningItemIds:
    List<LearningItemId>,
    val isEmpty: Boolean,
    val isAtStart: Boolean,
    val isLastItem: Boolean,
    val isCompleted: Boolean,
    val progress: Double,
    val percentComplete: Int
) {

    init {
        require(totalItemCount >= 0) {
            "Total item count must not be negative."
        }

        require(completedItemCount >= 0) {
            "Completed item count must not be negative."
        }

        require(remainingItemCount >= 0) {
            "Remaining item count must not be negative."
        }

        require(currentIndex >= 0) {
            "Current index must not be negative."
        }

        require(
            completedItemCount +
                    remainingItemCount ==
                    totalItemCount
        ) {
            "Completed and remaining counts must equal total item count."
        }

        require(
            currentIndex ==
                    completedItemCount
        ) {
            "Current index must equal completed item count."
        }

        require(progress in 0.0..1.0) {
            "Progress must be between 0.0 and 1.0."
        }

        require(percentComplete in 0..100) {
            "Percent complete must be between 0 and 100."
        }

        require(
            completedLearningItemIds.size ==
                    completedItemCount
        ) {
            "Completed item list size must match completed item count."
        }

        require(
            remainingLearningItemIds.size ==
                    remainingItemCount
        ) {
            "Remaining item list size must match remaining item count."
        }

        require(
            if (isCompleted) {
                currentLearningItemId == null
            } else {
                currentLearningItemId != null
            }
        ) {
            "Current item must match queue completion state."
        }

        require(
            !isLastItem ||
                    (
                            !isCompleted &&
                                    remainingItemCount == 1
                            )
        ) {
            "Last-item state must contain exactly one remaining item."
        }
    }

    companion object {

        fun from(
            snapshot: StudyQueueSnapshot
        ): StudyQueueProgress =
            StudyQueueProgress(
                sessionId =
                    snapshot.sessionId,
                createdAt =
                    snapshot.createdAt,
                totalItemCount =
                    snapshot.totalItemCount,
                completedItemCount =
                    snapshot.completedItemCount,
                remainingItemCount =
                    snapshot.remainingItemCount,
                currentIndex =
                    snapshot.currentIndex,
                currentLearningItemId =
                    snapshot.currentLearningItemId,
                previousLearningItemId =
                    snapshot.previousLearningItemId,
                nextLearningItemId =
                    snapshot.nextLearningItemId,
                completedLearningItemIds =
                    snapshot
                        .completedLearningItemIds
                        .toList(),
                remainingLearningItemIds =
                    snapshot
                        .remainingLearningItemIds
                        .toList(),
                pendingLearningItemIds =
                    snapshot
                        .pendingLearningItemIds
                        .toList(),
                isEmpty =
                    snapshot.isEmpty,
                isAtStart =
                    snapshot.isAtStart,
                isLastItem =
                    snapshot.isLastItem,
                isCompleted =
                    snapshot.isCompleted,
                progress =
                    snapshot.progress,
                percentComplete =
                    snapshot.percentComplete
            )
    }
}