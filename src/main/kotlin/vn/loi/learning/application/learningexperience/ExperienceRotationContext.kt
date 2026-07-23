package vn.loi.learning.application.learningexperience

import vn.loi.learning.application.session.NextSessionItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.session.model.SessionId

/**
 * Stable identity and zero-based presentation ordinal for one item in a study session.
 *
 * Queued sessions derive the ordinal from their durable current queue position. The legacy
 * no-queue path falls back to the number of committed reviews before the current item.
 */
data class ExperienceRotationContext(
    val sessionId: SessionId,
    val learningItemId: LearningItemId,
    val ordinal: Long
) {
    init {
        require(ordinal >= 0) {
            "Experience rotation ordinal must not be negative."
        }
    }

    companion object {
        fun from(item: NextSessionItem): ExperienceRotationContext {
            val oneBasedPosition =
                item.progress?.currentPosition
                    ?: (item.session.totalReviews + 1)
            require(oneBasedPosition > 0) {
                "A presented item requires a positive session position."
            }
            return ExperienceRotationContext(
                sessionId = item.session.id,
                learningItemId = item.item.learningItem.id,
                ordinal = oneBasedPosition.toLong() - 1L
            )
        }
    }
}
