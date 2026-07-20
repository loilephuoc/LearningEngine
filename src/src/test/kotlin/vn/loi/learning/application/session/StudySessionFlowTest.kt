package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.application.study.GetNextLearningItemUseCase
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
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryReviewEventRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryStudySessionRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class StudySessionFlowTest {

    @Test
    fun `session respects new item limit and records progress`() {
        val contentRepository = InMemoryContentRepository()
        val itemRepository = InMemoryLearningItemRepository()
        val memoryRepository = InMemoryMemoryStateRepository()
        val eventRepository = InMemoryReviewEventRepository()
        val sessionRepository = InMemoryStudySessionRepository()

        repeat(2) { index ->
            val number = index + 1

            val content = Content(
                id = ContentId("content-$number"),
                type = ContentType.SENTENCE,
                text = ContentText(
                    primaryText = "Sentence $number"
                )
            )

            val item = LearningItem(
                id = LearningItemId("item-$number"),
                contentId = content.id,
                mode = LearningMode.MEANING_RECOGNITION
            )

            contentRepository.save(content)
            itemRepository.save(item)
        }

        val getNextItemUseCase =
            GetNextLearningItemUseCase(
                contentRepository = contentRepository,
                learningItemRepository = itemRepository,
                memoryStateRepository = memoryRepository
            )

        val reviewUseCase =
            ReviewLearningItemUseCase(
                memoryStateRepository = memoryRepository,
                reviewEventRepository = eventRepository,
                scheduler = SimpleScheduler()
            )

        val startUseCase =
            StartStudySessionUseCase(
                sessionRepository = sessionRepository
            )

        val nextUseCase =
            GetNextSessionItemUseCase(
                sessionRepository = sessionRepository,
                getNextLearningItemUseCase = getNextItemUseCase
            )

        val reviewSessionUseCase =
            ReviewSessionItemUseCase(
                sessionRepository = sessionRepository,
                learningItemRepository = itemRepository,
                reviewLearningItemUseCase = reviewUseCase,
                transactionRunner = InMemoryTransactionRunner()
            )

        val finishUseCase =
            FinishStudySessionUseCase(
                sessionRepository = sessionRepository
            )

        val sessionId = SessionId("session-001")
        val now = Moment(1_000_000L)

        startUseCase.execute(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = LearnerId("loi"),
                startedAt = now,
                policy = SessionPolicy(
                    newItemLimit = 1,
                    reviewItemLimit = 10
                )
            )
        )

        val first = requireNotNull(
            nextUseCase.execute(
                sessionId = sessionId,
                now = now
            )
        )

        val reviewed = reviewSessionUseCase.execute(
            ReviewSessionItemCommand(
                sessionId = sessionId,
                reviewEventId = ReviewEventId("review-001"),
                learningItemId = first.item.learningItem.id,
                rating = ReviewRating.GOOD,
                reviewedAt = now
            )
        )

        assertEquals(
            expected = 1,
            actual = reviewed.session.newItemsReviewed
        )

        assertEquals(
            expected = 1,
            actual = reviewed.session.totalReviews
        )

        val next = nextUseCase.execute(
            sessionId = sessionId,
            now = now
        )

        assertNull(next)

        val finished = finishUseCase.execute(
            sessionId = sessionId,
            finishedAt = Moment(2_000_000L)
        )

        assertEquals(
            expected = SessionStatus.FINISHED,
            actual = finished.status
        )

        assertEquals(
            expected = Moment(2_000_000L),
            actual = finished.finishedAt
        )
    }

    @Test
    fun `different learning modes of the same content are not repeated in one session`() {
        val contentRepository = InMemoryContentRepository()
        val itemRepository = InMemoryLearningItemRepository()
        val memoryRepository = InMemoryMemoryStateRepository()
        val eventRepository = InMemoryReviewEventRepository()
        val sessionRepository = InMemoryStudySessionRepository()

        val auntContent = Content(
            id = ContentId("content-aunt"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "aunt"
            )
        )

        val brotherContent = Content(
            id = ContentId("content-brother"),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = "brother"
            )
        )

        contentRepository.save(auntContent)
        contentRepository.save(brotherContent)

        itemRepository.save(
            LearningItem(
                id = LearningItemId("aunt-meaning"),
                contentId = auntContent.id,
                mode = LearningMode.MEANING_RECOGNITION
            )
        )

        itemRepository.save(
            LearningItem(
                id = LearningItemId("aunt-listening"),
                contentId = auntContent.id,
                mode = LearningMode.LISTENING_RECOGNITION
            )
        )

        itemRepository.save(
            LearningItem(
                id = LearningItemId("brother-meaning"),
                contentId = brotherContent.id,
                mode = LearningMode.MEANING_RECOGNITION
            )
        )

        val getNextItemUseCase =
            GetNextLearningItemUseCase(
                contentRepository = contentRepository,
                learningItemRepository = itemRepository,
                memoryStateRepository = memoryRepository
            )

        val reviewUseCase =
            ReviewLearningItemUseCase(
                memoryStateRepository = memoryRepository,
                reviewEventRepository = eventRepository,
                scheduler = SimpleScheduler()
            )

        val startUseCase =
            StartStudySessionUseCase(
                sessionRepository = sessionRepository
            )

        val nextUseCase =
            GetNextSessionItemUseCase(
                sessionRepository = sessionRepository,
                getNextLearningItemUseCase = getNextItemUseCase
            )

        val reviewSessionUseCase =
            ReviewSessionItemUseCase(
                sessionRepository = sessionRepository,
                learningItemRepository = itemRepository,
                reviewLearningItemUseCase = reviewUseCase,
                transactionRunner = InMemoryTransactionRunner()
            )

        val sessionId = SessionId("sibling-session")
        val now = Moment(1_000_000L)

        startUseCase.execute(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = LearnerId("loi"),
                startedAt = now,
                policy = SessionPolicy(
                    newItemLimit = 10,
                    reviewItemLimit = 10,
                    allowRepeatInSameSession = false
                )
            )
        )

        val first = requireNotNull(
            nextUseCase.execute(
                sessionId = sessionId,
                now = now
            )
        )

        assertEquals(
            expected = auntContent.id,
            actual = first.item.content.id
        )

        reviewSessionUseCase.execute(
            ReviewSessionItemCommand(
                sessionId = sessionId,
                reviewEventId = ReviewEventId("review-aunt"),
                learningItemId = first.item.learningItem.id,
                rating = ReviewRating.GOOD,
                reviewedAt = now
            )
        )

        val second = requireNotNull(
            nextUseCase.execute(
                sessionId = sessionId,
                now = now
            )
        )

        assertEquals(
            expected = brotherContent.id,
            actual = second.item.content.id
        )
    }
}




