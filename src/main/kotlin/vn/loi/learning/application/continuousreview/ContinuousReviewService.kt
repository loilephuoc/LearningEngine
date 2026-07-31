package vn.loi.learning.application.continuousreview

import vn.loi.learning.application.port.ContinuousReviewIntentRepository
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId

class ContinuousReviewService(
    private val intents: ContinuousReviewIntentRepository
) {
    fun query(learnerId: LearnerId): ContinuousReviewIntent? =
        intents.findByLearner(learnerId)

    fun enable(
        learnerId: LearnerId,
        installedPackageId: InstalledPackageId,
        topicId: TopicId?,
        updatedAt: Moment
    ): ContinuousReviewIntent =
        ContinuousReviewIntent(learnerId, installedPackageId, topicId, true, updatedAt)
            .also(intents::save)

    fun disable(learnerId: LearnerId, updatedAt: Moment): ContinuousReviewIntent? {
        val current = intents.findByLearner(learnerId) ?: return null
        return current.copy(enabled = false, updatedAt = updatedAt).also(intents::save)
    }

    fun recordNoWork(
        intent: ContinuousReviewIntent,
        predecessorId: SessionId,
        updatedAt: Moment
    ) {
        intents.save(
            intent.copy(lastNoWorkPredecessorId = predecessorId, updatedAt = updatedAt)
        )
    }
}
