package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.scheduling.SimpleScheduler
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository

class ReviewSessionItemTransactionTest {

    @Test
    fun `review session item runs inside exactly one transaction`() {
        val contentRepository = InMemoryContentRepository()
        val learningItemRepository =
            InMemoryLearningItemRepository()
        val memoryStateRepository =
            InMemoryMemoryStateRepository()
        val reviewEventRepository =
            InMemoryReviewEventRepository()
        val sessionRepository =
            InMemoryStudySessionRepository()

        val transactionRunner =
            RecordingTransactionRunner()

        val content = Content(
            id = ContentId("content-1"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "aunt"
            )
        )

        val learningItem = LearningItem(
            id = LearningItemId("item-1"),
            contentId = content.id,
            mode = LearningMode.MEANING_RECOGNITION
        )

        contentRepository.save(content)
        learningItemRepository.save(learningItem)

        val reviewLearningItemUseCase =
            ReviewLearningItemUseCase(
                memoryStateRepository =
                    memoryStateRepository,
                reviewEventRepository =
                    reviewEventRepository,
                scheduler = SimpleScheduler()
            )

        val startSessionUseCase =
            StartStudySessionUseCase(
                sessionRepository = sessionRepository
            )

        val reviewSessionItemUseCase =
            ReviewSessionItemUseCase(
                sessionRepository = sessionRepository,
                learningItemRepository =
                    learningItemRepository,
                reviewLearningItemUseCase =
                    reviewLearningItemUseCase,
                transactionRunner = transactionRunner
            )

        val sessionId = SessionId("session-1")
        val learnerId = LearnerId("learner-1")
        val reviewedAt = Moment(1_000L)

        startSessionUseCase.execute(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = learnerId,
                startedAt = reviewedAt,
                policy = SessionPolicy(
                    newItemLimit = 10,
                    reviewItemLimit = 10
                )
            )
        )

        reviewSessionItemUseCase.execute(
            ReviewSessionItemCommand(
                sessionId = sessionId,
                reviewEventId =
                    ReviewEventId("review-1"),
                learningItemId = learningItem.id,
                rating = ReviewRating.GOOD,
                reviewedAt = reviewedAt
            )
        )

        assertEquals(
            expected = 1,
            actual = transactionRunner.executionCount
        )

        assertTrue(
            transactionRunner.blockWasExecuted
        )
    }

    private class RecordingTransactionRunner :
        TransactionRunner {

        var executionCount: Int = 0
            private set

        var blockWasExecuted: Boolean = false
            private set

        override fun <T> runInTransaction(
            block: () -> T
        ): T {
            executionCount += 1
            blockWasExecuted = true

            return block()
        }
    }
}




