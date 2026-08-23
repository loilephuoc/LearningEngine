package vn.loi.learning.application.session

import vn.loi.learning.domain.study.session.model.StudySession

/** Renderer-neutral progress projected from the durable session and its immutable queue. */
data class LearningSessionProgress(
    val completedItemCount: Int,
    val reviewedItemCount: Int,
    val skippedItemCount: Int,
    val remainingItemCount: Int?,
    val totalItemCount: Int?,
    val currentPosition: Int?,
    val totalIsKnown: Boolean,
    val isEmpty: Boolean,
    val isCompleted: Boolean
) {
    init {
        require(completedItemCount >= 0)
        require(reviewedItemCount >= 0)
        require(skippedItemCount >= 0)
        require(reviewedItemCount + skippedItemCount == completedItemCount)
        require(totalIsKnown == (totalItemCount != null && remainingItemCount != null))
        totalItemCount?.let { require(completedItemCount <= it) }
    }

    val fractionComplete: Double?
        get() = totalItemCount?.let { total ->
            if (total == 0) 1.0 else completedItemCount.toDouble() / total.toDouble()
        }

    companion object {
        fun from(
            session: StudySession,
            queue: StudyQueueProgress
        ): LearningSessionProgress {
            require(session.id == queue.sessionId) {
                "Session and queue progress must have the same session ID."
            }
            val completed = maxOf(session.totalReviews, queue.completedItemCount)
            val total = maxOf(queue.totalItemCount, completed)
            val remaining = maxOf(0, total - completed)
            val isQueueCompleted = queue.isCompleted || completed >= total
            return LearningSessionProgress(
                completedItemCount = completed,
                reviewedItemCount = session.totalReviews,
                skippedItemCount = maxOf(0, completed - session.totalReviews),
                remainingItemCount = remaining,
                totalItemCount = total,
                currentPosition = if (isQueueCompleted) null else queue.currentIndex + 1,
                totalIsKnown = true,
                isEmpty = queue.isEmpty && completed == 0,
                isCompleted = isQueueCompleted
            )
        }

        fun unknown(session: StudySession): LearningSessionProgress =
            LearningSessionProgress(
                completedItemCount = session.totalReviews,
                reviewedItemCount = session.totalReviews,
                skippedItemCount = 0,
                remainingItemCount = null,
                totalItemCount = null,
                currentPosition = null,
                totalIsKnown = false,
                isEmpty = false,
                isCompleted = false
            )
    }
}
