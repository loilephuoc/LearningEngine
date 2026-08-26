package vn.loi.learning.application.continuousreview

import vn.loi.learning.application.port.ContinuousReviewIntentRepository
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId

class ContinuousReviewService(
    private val intents: ContinuousReviewIntentRepository,
    private val transactionRunner: vn.loi.learning.application.port.TransactionRunner? = null
) {
    fun query(learnerId: LearnerId): ContinuousReviewIntent? =
        intents.findByLearner(learnerId)

    fun enable(
        learnerId: LearnerId,
        installedPackageId: InstalledPackageId,
        topicId: TopicId?,
        updatedAt: Moment
    ): ContinuousReviewIntent {
        val intent = ContinuousReviewIntent(learnerId, installedPackageId, topicId, true, updatedAt)
        return if (transactionRunner != null) {
            transactionRunner.runInTransaction {
                intents.save(intent)
                intent
            }
        } else {
            intents.save(intent)
            intent
        }
    }

    fun disable(learnerId: LearnerId, updatedAt: Moment): ContinuousReviewIntent? {
        val current = intents.findByLearner(learnerId) ?: return null
        val updated = current.copy(enabled = false, updatedAt = updatedAt)
        return if (transactionRunner != null) {
            transactionRunner.runInTransaction {
                intents.save(updated)
                updated
            }
        } else {
            intents.save(updated)
            updated
        }
    }

    fun recordNoWork(
        intent: ContinuousReviewIntent,
        predecessorId: SessionId,
        updatedAt: Moment
    ) {
        val updated = intent.copy(lastNoWorkPredecessorId = predecessorId, updatedAt = updatedAt)
        if (transactionRunner != null) {
            transactionRunner.runInTransaction {
                intents.save(updated)
            }
        } else {
            intents.save(updated)
        }
    }
}
