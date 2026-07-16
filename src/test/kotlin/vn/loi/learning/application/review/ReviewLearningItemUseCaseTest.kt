package vn.loi.learning.application.review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.scheduling.SimpleScheduler
import vn.loi.learning.infrastructure.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.memory.InMemoryReviewEventRepository

class ReviewLearningItemUseCaseTest {

    private val learnerId = LearnerId("loi")
    private val itemId = LearningItemId("sentence-001-listening")

    @Test
    fun `first good review creates memory and history`() {
        val memoryRepository = InMemoryMemoryStateRepository()
        val eventRepository = InMemoryReviewEventRepository()

        val useCase = ReviewLearningItemUseCase(
            memoryStateRepository = memoryRepository,
            reviewEventRepository = eventRepository,
            scheduler = SimpleScheduler()
        )

        val reviewedAt = Moment(1_000_000L)

        val result = useCase.execute(
            ReviewCommand(
                reviewEventId = ReviewEventId("review-001"),
                learnerId = learnerId,
                learningItemId = itemId,
                rating = ReviewRating.GOOD,
                reviewedAt = reviewedAt,
                responseTime = TimeSpan.seconds(3)
            )
        )

        assertEquals(LearningStage.REVIEW, result.memoryState.stage)
        assertEquals(1, result.memoryState.reviewCount)
        assertEquals(0, result.memoryState.lapseCount)
        assertEquals(1.0, result.memoryState.stabilityDays)
        assertEquals(4.8, result.memoryState.difficulty)
        assertEquals(TimeSpan.days(1), result.scheduledInterval)
        assertEquals(
            reviewedAt + TimeSpan.days(1),
            result.memoryState.dueAt
        )

        assertEquals(1, memoryRepository.count())
        assertEquals(1, eventRepository.count())
    }

    @Test
    fun `again after a successful review creates a lapse`() {
        val memoryRepository = InMemoryMemoryStateRepository()
        val eventRepository = InMemoryReviewEventRepository()

        val useCase = ReviewLearningItemUseCase(
            memoryStateRepository = memoryRepository,
            reviewEventRepository = eventRepository,
            scheduler = SimpleScheduler()
        )

        val firstReviewAt = Moment(1_000_000L)

        useCase.execute(
            ReviewCommand(
                reviewEventId = ReviewEventId("review-001"),
                learnerId = learnerId,
                learningItemId = itemId,
                rating = ReviewRating.GOOD,
                reviewedAt = firstReviewAt
            )
        )

        val secondReviewAt = firstReviewAt + TimeSpan.days(1)

        val secondResult = useCase.execute(
            ReviewCommand(
                reviewEventId = ReviewEventId("review-002"),
                learnerId = learnerId,
                learningItemId = itemId,
                rating = ReviewRating.AGAIN,
                reviewedAt = secondReviewAt
            )
        )

        assertEquals(
            LearningStage.RELEARNING,
            secondResult.memoryState.stage
        )
        assertEquals(2, secondResult.memoryState.reviewCount)
        assertEquals(1, secondResult.memoryState.lapseCount)
        assertEquals(TimeSpan.minutes(10), secondResult.scheduledInterval)
        assertEquals(2, eventRepository.count())

        val history = eventRepository.findAll(
            learnerId = learnerId,
            learningItemId = itemId
        )

        assertEquals(2, history.size)
        assertEquals(ReviewRating.GOOD, history[0].rating)
        assertEquals(ReviewRating.AGAIN, history[1].rating)
        assertTrue(
            history[1].stateAfter.difficulty >
                    history[1].stateBefore.difficulty
        )
    }
}