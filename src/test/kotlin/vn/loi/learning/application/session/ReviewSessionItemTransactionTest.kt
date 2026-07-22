package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    fun `interruption keeps one pending intent that can be resumed exactly once`() {
        val learningItems = InMemoryLearningItemRepository()
        val memoryStates = InMemoryMemoryStateRepository()
        val events = InMemoryReviewEventRepository()
        val sessions = InMemoryStudySessionRepository()
        val item = LearningItem(
            id = LearningItemId("item-interrupted"),
            contentId = ContentId("content-interrupted"),
            mode = LearningMode.MEANING_RECOGNITION
        )
        learningItems.save(item)
        val sessionId = SessionId("session-interrupted")
        sessions.save(
            vn.loi.learning.domain.study.session.model.StudySession.start(
                id = sessionId,
                learnerId = LearnerId("learner-interrupted"),
                startedAt = Moment(1_000L),
                policy = SessionPolicy(newItemLimit = 2, reviewItemLimit = 2)
            ).presentItem(item.id, Moment(1_100L))
        )
        fun useCase(runner: TransactionRunner) = ReviewSessionItemUseCase(
            sessionRepository = sessions,
            learningItemRepository = learningItems,
            reviewLearningItemUseCase = ReviewLearningItemUseCase(
                memoryStateRepository = memoryStates,
                reviewEventRepository = events,
                scheduler = SimpleScheduler()
            ),
            transactionRunner = runner
        )
        val command = ReviewSessionItemCommand(
            sessionId = sessionId,
            reviewEventId = ReviewEventId("review-interrupted"),
            learningItemId = item.id,
            rating = ReviewRating.GOOD,
            reviewedAt = Moment(2_000L)
        )

        assertFailsWith<InterruptedException> {
            useCase(InterruptingTransactionRunner())
                .execute(command)
        }
        assertNotNull(sessions.findById(sessionId)?.pendingReview)
        assertTrue(events.findAll(LearnerId("learner-interrupted"), item.id).isEmpty())

        useCase(DirectTransactionRunner()).resumePending(sessionId)

        assertEquals(1, events.findAll(LearnerId("learner-interrupted"), item.id).size)
        assertNull(sessions.findById(sessionId)?.pendingReview)
        assertEquals(1, sessions.findById(sessionId)?.totalReviews)
    }

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

    private class InterruptingTransactionRunner : TransactionRunner {
        override fun <T> runInTransaction(block: () -> T): T =
            throw InterruptedException("simulated")
    }

    private class DirectTransactionRunner : TransactionRunner {
        override fun <T> runInTransaction(block: () -> T): T = block()
    }
}




