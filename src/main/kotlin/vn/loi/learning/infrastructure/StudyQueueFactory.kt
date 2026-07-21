package vn.loi.learning.infrastructure

import vn.loi.learning.application.port.StudyQueueRepository
import vn.loi.learning.application.session.StudyQueueService
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudyQueueRepository

/**
 * Composition factory cho Study Queue.
 *
 * Factory chỉ chịu trách nhiệm lắp ráp dependency.
 * Không chứa queue planning, session selection
 * hoặc scheduling business logic.
 */
object StudyQueueFactory {

    fun createInMemory():
            StudyQueueService =
        create(
            repository =
                InMemoryStudyQueueRepository()
        )

    fun create(
        repository:
        StudyQueueRepository
    ): StudyQueueService =
        StudyQueueService(
            repository = repository
        )
}