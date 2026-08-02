package vn.loi.learning.infrastructure.persistence.memory

import vn.loi.learning.application.port.LearningTrajectoryRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.LearningTrajectory
import vn.loi.learning.domain.study.memory.model.LearnerId

class InMemoryLearningTrajectoryRepository : LearningTrajectoryRepository {
    private val values = linkedMapOf<Pair<LearnerId, ContentId>, LearningTrajectory>()
    override fun find(learnerId: LearnerId, contentId: ContentId) = values[learnerId to contentId]
    override fun save(learnerId: LearnerId, trajectory: LearningTrajectory) { values[learnerId to trajectory.contentId] = trajectory }
    override fun delete(learnerId: LearnerId, contentId: ContentId) { values.remove(learnerId to contentId) }
}
