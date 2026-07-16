package vn.loi.learning.application.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
import vn.loi.learning.infrastructure.content.InMemoryContentRepository
import vn.loi.learning.infrastructure.learning.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.memory.InMemoryMemoryStateRepository
import vn.loi.learning.infrastructure.memory.InMemoryReviewEventRepository
import vn.loi.learning.infrastructure.session.InMemoryStudySessionRepository
import vn.loi.learning.application.review.ReviewLearningItemUseCase

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

        val getNextItemUseCase = GetNextLearningItemUseCase(
            contentRepository = contentRepository,
            learningItemRepository = itemRepository,
            memoryStateRepository = memoryRepository
        )

        val reviewUseCase = ReviewLearningItemUseCase(
            memoryStateRepository = memoryRepository,
            reviewEventRepository = eventRepository,
            scheduler = SimpleScheduler()
        )

        val startUseCase =
            StartStudySessionUseCase(sessionRepository)

        val nextUseCase = GetNextSessionItemUseCase(
            sessionRepository = sessionRepository,
            getNextLearningItemUseCase = getNextItemUseCase
        )

        val reviewSessionUseCase =
            ReviewSessionItemUseCase(
                sessionRepository = sessionRepository,
                learningItemRepository = itemRepository,
                reviewLearningItemUseCase = reviewUseCase
            )

        val finishUseCase =
            FinishStudySessionUseCase(sessionRepository)

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

        assertEquals(1, reviewed.session.newItemsReviewed)
        assertEquals(1, reviewed.session.totalReviews)

        val next = nextUseCase.execute(
            sessionId = sessionId,
            now = now
        )

        assertNull(next)

        val finished = finishUseCase.execute(
            sessionId = sessionId,
            finishedAt = Moment(2_000_000L)
        )

        assertEquals(SessionStatus.FINISHED, finished.status)
        assertEquals(Moment(2_000_000L), finished.finishedAt)
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

        val getNextItemUseCase = GetNextLearningItemUseCase(
            contentRepository = contentRepository,
            learningItemRepository = itemRepository,
            memoryStateRepository = memoryRepository
        )

        val reviewUseCase = ReviewLearningItemUseCase(
            memoryStateRepository = memoryRepository,
            reviewEventRepository = eventRepository,
            scheduler = SimpleScheduler()
        )

        val startUseCase =
            StartStudySessionUseCase(sessionRepository)

        val nextUseCase = GetNextSessionItemUseCase(
            sessionRepository = sessionRepository,
            getNextLearningItemUseCase = getNextItemUseCase
        )

        val reviewSessionUseCase =
            ReviewSessionItemUseCase(
                sessionRepository = sessionRepository,
                learningItemRepository = itemRepository,
                reviewLearningItemUseCase = reviewUseCase
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
            auntContent.id,
            first.item.content.id
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
            brotherContent.id,
            second.item.content.id
        )
    }



}