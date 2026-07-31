package vn.loi.learning.application.confidence

import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceProjection
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceEvidence
import vn.loi.learning.domain.study.confidence.policy.MemoryConfidenceProjector
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId

class MemoryConfidenceQueryService(
    private val reviewEventRepository: ReviewEventRepository
) {
    fun query(
        learnerId: LearnerId,
        learningItemId: LearningItemId,
        pendingEvidence: MemoryConfidenceEvidence? = null
    ): MemoryConfidenceProjection =
        MemoryConfidenceProjector.project(
            reviewEventRepository.findAll(learnerId, learningItemId),
            pendingEvidence
        )
}
