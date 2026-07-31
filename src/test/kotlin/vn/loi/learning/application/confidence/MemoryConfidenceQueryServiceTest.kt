package vn.loi.learning.application.confidence

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.confidence.policy.MemoryConfidenceProjector
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.testing.fixtures.ReviewFixtures

class MemoryConfidenceQueryServiceTest {
    @Test
    fun `query uses exact durable learner and item authority`() {
        val repository = InMemoryReviewEventRepository()
        val learner = LearnerId("selected")
        val item = LearningItemId("selected")
        val selected =
            ReviewFixtures.event(
                ReviewEventId("selected"),
                learner,
                item,
                reviewedAt = Moment(1_000L)
            )
        repository.append(
            ReviewFixtures.event(
                ReviewEventId("other"),
                LearnerId("other"),
                LearningItemId("other"),
                reviewedAt = Moment(2_000L)
            )
        )
        repository.append(selected)

        val result = MemoryConfidenceQueryService(repository).query(learner, item)

        assertEquals(MemoryConfidenceProjector.project(listOf(selected)), result)
        assertEquals(1, result.projectedConfidence.evaluatedReviewCount)
    }

    @Test
    fun `empty and restart-equivalent repositories project identically`() {
        val learner = LearnerId("learner")
        val item = LearningItemId("item")
        val first = InMemoryReviewEventRepository()
        val second = InMemoryReviewEventRepository()

        assertEquals(
            MemoryConfidenceQueryService(first).query(learner, item),
            MemoryConfidenceQueryService(second).query(learner, item)
        )
    }
}
