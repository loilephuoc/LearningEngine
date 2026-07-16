package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.infrastructure.content.InMemoryContentRepository
import vn.loi.learning.infrastructure.learning.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.memory.InMemoryMemoryStateRepository

class GetNextLearningItemUseCaseTest {

    private val learnerId = LearnerId("loi")

    @Test
    fun `first registered new item is returned when no reviews are due`() {
        val contentRepository = InMemoryContentRepository()
        val itemRepository = InMemoryLearningItemRepository()
        val memoryRepository = InMemoryMemoryStateRepository()

        val content = createContent(
            id = "content-001",
            text = "First sentence"
        )

        val item = createItem(
            id = "item-001",
            contentId = content.id
        )

        contentRepository.save(content)
        itemRepository.save(item)

        val useCase = GetNextLearningItemUseCase(
            contentRepository = contentRepository,
            learningItemRepository = itemRepository,
            memoryStateRepository = memoryRepository
        )

        val result = useCase.execute(
            GetNextLearningItemQuery(
                learnerId = learnerId,
                now = Moment(1_000L)
            )
        )

        requireNotNull(result)

        assertEquals(item, result.learningItem)
        assertEquals(content, result.content)
        assertTrue(result.isNew)
        assertEquals(Moment(1_000L), result.effectiveDueAt)
    }

    @Test
    fun `overdue review is preferred over a new item`() {
        val contentRepository = InMemoryContentRepository()
        val itemRepository = InMemoryLearningItemRepository()
        val memoryRepository = InMemoryMemoryStateRepository()

        val reviewContent = createContent(
            id = "content-review",
            text = "Review sentence"
        )

        val newContent = createContent(
            id = "content-new",
            text = "New sentence"
        )

        val reviewItem = createItem(
            id = "item-review",
            contentId = reviewContent.id
        )

        val newItem = createItem(
            id = "item-new",
            contentId = newContent.id
        )

        contentRepository.save(reviewContent)
        contentRepository.save(newContent)

        itemRepository.save(reviewItem)
        itemRepository.save(newItem)

        val now = Moment(1_000_000L)

        memoryRepository.save(
            MemoryState.new(
                learnerId = learnerId,
                learningItemId = reviewItem.id,
                availableAt = now
            ).copy(
                dueAt = now
            )
        )

        val useCase = GetNextLearningItemUseCase(
            contentRepository = contentRepository,
            learningItemRepository = itemRepository,
            memoryStateRepository = memoryRepository
        )

        val result = useCase.execute(
            GetNextLearningItemQuery(
                learnerId = learnerId,
                now = now
            )
        )

        requireNotNull(result)

        assertEquals(reviewItem, result.learningItem)
        assertFalse(result.isNew)
    }

    @Test
    fun `future review is not returned when there are no new items`() {
        val contentRepository = InMemoryContentRepository()
        val itemRepository = InMemoryLearningItemRepository()
        val memoryRepository = InMemoryMemoryStateRepository()

        val content = createContent(
            id = "content-001",
            text = "Future sentence"
        )

        val item = createItem(
            id = "item-001",
            contentId = content.id
        )

        contentRepository.save(content)
        itemRepository.save(item)

        val now = Moment(1_000L)

        memoryRepository.save(
            MemoryState.new(
                learnerId = learnerId,
                learningItemId = item.id,
                availableAt = now
            ).copy(
                dueAt = now + TimeSpan.days(1)
            )
        )

        val useCase = GetNextLearningItemUseCase(
            contentRepository = contentRepository,
            learningItemRepository = itemRepository,
            memoryStateRepository = memoryRepository
        )

        val result = useCase.execute(
            GetNextLearningItemQuery(
                learnerId = learnerId,
                now = now
            )
        )

        assertNull(result)
    }

    private fun createContent(
        id: String,
        text: String
    ): Content =
        Content(
            id = ContentId(id),
            type = ContentType.SENTENCE,
            text = ContentText(
                primaryText = text
            )
        )

    private fun createItem(
        id: String,
        contentId: ContentId
    ): LearningItem =
        LearningItem(
            id = LearningItemId(id),
            contentId = contentId,
            mode = LearningMode.LISTENING_RECOGNITION
        )
}