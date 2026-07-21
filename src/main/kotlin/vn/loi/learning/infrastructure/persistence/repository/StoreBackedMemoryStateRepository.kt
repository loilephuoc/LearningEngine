package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.infrastructure.persistence.mapper.MemoryStateRecordMapper
import vn.loi.learning.infrastructure.persistence.store.MemoryStateStore

class StoreBackedMemoryStateRepository(
    private val store: MemoryStateStore
) : MemoryStateRepository, MemoryStateQuery {

    override fun find(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): MemoryState? =
        store.load()
            .firstOrNull { record ->
                record.learnerId ==
                        learnerId.toString() &&
                        record.learningItemId ==
                        learningItemId.toString()
            }
            ?.let(
                MemoryStateRecordMapper::toDomain
            )

    override fun findAll(
        learnerId: LearnerId
    ): List<MemoryState> =
        store.load()
            .asSequence()
            .filter { record ->
                record.learnerId ==
                        learnerId.toString()
            }
            .map(
                MemoryStateRecordMapper::toDomain
            )
            .toList()

    override fun save(
        memoryState: MemoryState
    ) {
        val newRecord =
            MemoryStateRecordMapper.toRecord(
                memoryState
            )

        val currentRecords =
            store.load()

        val existingRecord =
            currentRecords.firstOrNull { record ->
                record.learnerId ==
                        newRecord.learnerId &&
                        record.learningItemId ==
                        newRecord.learningItemId
            }

        if (existingRecord == newRecord) {
            return
        }

        val updatedRecords =
            currentRecords.filterNot { record ->
                record.learnerId ==
                        newRecord.learnerId &&
                        record.learningItemId ==
                        newRecord.learningItemId
            } + newRecord

        store.save(
            updatedRecords
        )
    }
}