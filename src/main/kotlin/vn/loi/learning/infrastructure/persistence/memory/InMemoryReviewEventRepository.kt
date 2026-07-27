package vn.loi.learning.infrastructure.persistence.memory

import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewEvent

class InMemoryReviewEventRepository : ReviewEventRepository {

    private val events =
        mutableListOf<ReviewEvent>()

    override fun append(
        event: ReviewEvent
    ) {
        require(events.none { existing ->
            existing.id == event.id
        }) {
            "ReviewEvent with ID ${event.id} already exists."
        }

        events += event
    }

    override fun findAll(
        learnerId: LearnerId
    ): List<ReviewEvent> =
        events
            .asSequence()
            .filter { event ->
                event.learnerId == learnerId
            }
            .sortedBy { event ->
                event.reviewedAt.epochMillis
            }
            .toList()

    override fun removeLatest(event: ReviewEvent) {
        require(events.lastOrNull()?.id == event.id) { "Only the latest ReviewEvent can be removed." }
        events.removeAt(events.lastIndex)
    }

    override fun deleteByLearningItemIds(learningItemIds: Set<LearningItemId>) {
        val idSet = learningItemIds.map { it.toString() }.toSet()
        events.removeIf { it.learningItemId in learningItemIds || it.learningItemId.toString() in idSet || it.stateAfter.learningItemId.toString() in idSet }
    }


    override fun findAll(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): List<ReviewEvent> =
        events
            .asSequence()
            .filter { event ->
                event.learnerId == learnerId
            }
            .filter { event ->
                event.learningItemId == learningItemId
            }
            .sortedBy { event ->
                event.reviewedAt.epochMillis
            }
            .toList()

    fun count(): Int =
        events.size

    fun clear() {
        events.clear()
    }
}
