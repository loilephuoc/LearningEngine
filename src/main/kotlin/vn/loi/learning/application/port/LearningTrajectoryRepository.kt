package vn.loi.learning.application.port

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.LearningTrajectory
import vn.loi.learning.domain.study.memory.model.LearnerId

interface LearningTrajectoryRepository {
    fun findAll(): List<LearningTrajectorySnapshot> = emptyList()
    fun find(learnerId: LearnerId, contentId: ContentId): LearningTrajectory?
    fun save(learnerId: LearnerId, trajectory: LearningTrajectory)
    fun delete(learnerId: LearnerId, contentId: ContentId)
}

data class LearningTrajectorySnapshot(
    val learnerId: LearnerId,
    val trajectory: LearningTrajectory
)
