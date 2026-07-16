package vn.loi.learning.infrastructure.memory

import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewEvent

class InMemoryReviewEventRepository : ReviewEventRepository {

    private val events = mutableListOf<ReviewEvent>()

    override fun append(event: ReviewEvent) {
        require(events.none { it.id == event.id }) {
            "ReviewEvent with ID ${event.id} already exists."
        }

        events += event
    }

    override fun findAll(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): List<ReviewEvent> =
        events
            .asSequence()
            .filter { it.learnerId == learnerId }
            .filter { it.learningItemId == learningItemId }
            .sortedBy { it.reviewedAt.epochMillis }
            .toList()

    fun count(): Int = events.size

    fun clear() {
        events.clear()
    }
}