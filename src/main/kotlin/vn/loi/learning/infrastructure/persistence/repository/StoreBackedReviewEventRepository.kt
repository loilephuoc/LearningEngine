package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.infrastructure.persistence.mapper.ReviewEventRecordMapper
import vn.loi.learning.infrastructure.persistence.store.ReviewEventStore

/**
 * ReviewEventRepository được hỗ trợ bởi một ReviewEventStore.
 *
 * Repository làm việc với Domain ReviewEvent.
 * Store làm việc với persistence ReviewEventRecord.
 *
 * Quy tắc append-only được thực thi tại adapter này:
 * các record cũ được giữ nguyên và event mới được thêm vào cuối.
 */
class StoreBackedReviewEventRepository(
    private val store: ReviewEventStore
) : ReviewEventRepository {

    override fun append(
        event: ReviewEvent
    ) {
        val existingRecords =
            store.loadAll()

        require(existingRecords.none { record ->
            record.id == event.id.toString()
        }) {
            "ReviewEvent with ID ${event.id} already exists."
        }

        val newRecord =
            ReviewEventRecordMapper.toRecord(event)

        store.saveAll(
            existingRecords + newRecord
        )
    }

    override fun findAll(
        learnerId: LearnerId
    ): List<ReviewEvent> {
        val learnerIdValue =
            learnerId.toString()

        return store
            .loadAll()
            .asSequence()
            .filter { record ->
                record.stateAfter.learnerId ==
                        learnerIdValue
            }
            .map(ReviewEventRecordMapper::toDomain)
            .sortedBy { event ->
                event.reviewedAt.epochMillis
            }
            .toList()
    }

    override fun findAll(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): List<ReviewEvent> {
        val learnerIdValue =
            learnerId.toString()

        val learningItemIdValue =
            learningItemId.toString()

        return store
            .loadAll()
            .asSequence()
            .filter { record ->
                record.stateAfter.learnerId ==
                        learnerIdValue
            }
            .filter { record ->
                record.stateAfter.learningItemId ==
                        learningItemIdValue
            }
            .map(ReviewEventRecordMapper::toDomain)
            .sortedBy { event ->
                event.reviewedAt.epochMillis
            }
            .toList()
    }

    override fun removeLatest(event: ReviewEvent) {
        val records = store.loadAll()
        require(records.lastOrNull()?.id == event.id.toString()) {
            "Only the latest ReviewEvent can be removed."
        }
        store.saveAll(records.dropLast(1))
    }

    override fun deleteByLearningItemIds(learningItemIds: Set<LearningItemId>) {
        val idSet = learningItemIds.map { it.toString() }.toSet()
        val current = store.loadAll()
        val updated = current.filterNot { it.stateAfter.learningItemId in idSet }
        if (updated.size != current.size) {
            store.saveAll(updated)
        }
    }
}
