package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.LearningTrajectoryRepository
import vn.loi.learning.application.port.LearningTrajectorySnapshot
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.LearningTrajectory
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.infrastructure.persistence.mapper.LearningTrajectoryRecordMapper
import vn.loi.learning.infrastructure.persistence.store.LearningTrajectoryStore

class StoreBackedLearningTrajectoryRepository(private val store: LearningTrajectoryStore) : LearningTrajectoryRepository {
    override fun findAll(): List<LearningTrajectorySnapshot> = store.loadAll().map {
        val (learnerId, trajectory) = LearningTrajectoryRecordMapper.toDomain(it)
        LearningTrajectorySnapshot(learnerId, trajectory)
    }
    override fun find(learnerId: LearnerId, contentId: ContentId): LearningTrajectory? = store.loadAll()
        .firstOrNull { it.learnerId == learnerId.value && it.contentId == contentId.value }
        ?.let { LearningTrajectoryRecordMapper.toDomain(it).second }
    override fun save(learnerId: LearnerId, trajectory: LearningTrajectory) {
        val records = store.loadAll().filterNot { it.learnerId == learnerId.value && it.contentId == trajectory.contentId.value }
        store.saveAll((records + LearningTrajectoryRecordMapper.toRecord(learnerId, trajectory))
            .sortedWith(compareBy({ it.learnerId }, { it.contentId })))
    }
    override fun delete(learnerId: LearnerId, contentId: ContentId) = store.saveAll(
        store.loadAll().filterNot { it.learnerId == learnerId.value && it.contentId == contentId.value })
}
