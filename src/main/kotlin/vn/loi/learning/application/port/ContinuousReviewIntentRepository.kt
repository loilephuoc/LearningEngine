package vn.loi.learning.application.port

import vn.loi.learning.application.continuousreview.ContinuousReviewIntent
import vn.loi.learning.domain.study.memory.model.LearnerId

interface ContinuousReviewIntentRepository {
    fun findByLearner(learnerId: LearnerId): ContinuousReviewIntent?
    fun save(intent: ContinuousReviewIntent)
}
