package vn.loi.learning.infrastructure.persistence.memory

import vn.loi.learning.application.continuousreview.ContinuousReviewIntent
import vn.loi.learning.application.port.ContinuousReviewIntentRepository
import vn.loi.learning.domain.study.memory.model.LearnerId

class InMemoryContinuousReviewIntentRepository : ContinuousReviewIntentRepository {
    private val values = linkedMapOf<LearnerId, ContinuousReviewIntent>()
    override fun findByLearner(learnerId: LearnerId): ContinuousReviewIntent? = values[learnerId]
    override fun save(intent: ContinuousReviewIntent) { values[intent.learnerId] = intent }
}
