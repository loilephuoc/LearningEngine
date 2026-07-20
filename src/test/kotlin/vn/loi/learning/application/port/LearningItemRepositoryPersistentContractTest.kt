package vn.loi.learning.application.port

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedLearningItemRepository
import vn.loi.learning.infrastructure.persistence.store.InMemoryLearningItemStore

class LearningItemRepositoryPersistentContractTest {

    private fun createRepository(): LearningItemRepository =
        StoreBackedLearningItemRepository(
            store = InMemoryLearningItemStore()
        )

    @Test
    fun `findById returns null when item does not exist`() {
        val repository =
            createRepository()

        assertNull(
            repository.findById(
                LearningItemId("missing-item")
            )
        )
    }

    @Test
    fun `save stores item and findById returns it`() {
        val repository =
            createRepository()

        val item =
            createItem(
                id = "item-1"
            )

        repository.save(item)

        assertEquals(
            item,
            repository.findById(item.id)
        )
    }

    @Test
    fun `saveAll stores multiple items`() {
        val repository =
            createRepository()

        val first =
            createItem(
                id = "item-1",
                contentId = "content-1"
            )

        val second =
            createItem(
                id = "item-2",
                contentId = "content-2"
            )

        repository.saveAll(
            listOf(
                first,
                second
            )
        )

        assertEquals(
            listOf(first, second),
            repository.findAllEnabled()
        )
    }

    @Test
    fun `saveAll replaces matching item and preserves others`() {
        val repository =
            createRepository()

        val original =
            createItem(
                id = "item-1",
                contentId = "content-1",
                enabled = true
            )

        val preserved =
            createItem(
                id = "item-2",
                contentId = "content-2",
                enabled = true
            )

        val updated =
            createItem(
                id = "item-1",
                contentId = "content-1",
                enabled = false
            )

        repository.saveAll(
            listOf(
                original,
                preserved
            )
        )

        repository.saveAll(
            listOf(
                updated
            )
        )

        assertEquals(
            updated,
            repository.findById(updated.id)
        )

        assertEquals(
            listOf(preserved),
            repository.findAllEnabled()
        )
    }

    @Test
    fun `findByContentId returns matching items`() {
        val repository =
            createRepository()

        val first =
            createItem(
                id = "item-1",
                contentId = "content-1"
            )

        val second =
            createItem(
                id = "item-2",
                contentId = "content-2"
            )

        repository.saveAll(
            listOf(
                first,
                second
            )
        )

        assertEquals(
            listOf(first),
            repository.findByContentId(
                ContentId("content-1")
            )
        )
    }

    @Test
    fun `findAllEnabled excludes disabled items`() {
        val repository =
            createRepository()

        val enabled =
            createItem(
                id = "item-1",
                enabled = true
            )

        val disabled =
            createItem(
                id = "item-2",
                enabled = false
            )

        repository.saveAll(
            listOf(
                enabled,
                disabled
            )
        )

        assertEquals(
            listOf(enabled),
            repository.findAllEnabled()
        )
    }

    @Test
    fun `deleteByContentIds removes every mode belonging to selected contents`() {
        val repository =
            createRepository()

        val meaningItem =
            createItem(
                id = "content-1-meaning",
                contentId = "content-1",
                mode = LearningMode.MEANING_RECOGNITION
            )

        val listeningItem =
            createItem(
                id = "content-1-listening",
                contentId = "content-1",
                mode = LearningMode.LISTENING_RECOGNITION
            )

        val preservedItem =
            createItem(
                id = "content-2-meaning",
                contentId = "content-2",
                mode = LearningMode.MEANING_RECOGNITION
            )

        repository.saveAll(
            listOf(
                meaningItem,
                listeningItem,
                preservedItem
            )
        )

        repository.deleteByContentIds(
            setOf(
                ContentId("content-1")
            )
        )

        assertNull(
            repository.findById(meaningItem.id)
        )

        assertNull(
            repository.findById(listeningItem.id)
        )

        assertEquals(
            listOf(preservedItem),
            repository.findAllEnabled()
        )
    }

    @Test
    fun `deleteByContentIds removes items for multiple contents`() {
        val repository =
            createRepository()

        val first =
            createItem(
                id = "item-1",
                contentId = "content-1"
            )

        val second =
            createItem(
                id = "item-2",
                contentId = "content-2"
            )

        val preserved =
            createItem(
                id = "item-3",
                contentId = "content-3"
            )

        repository.saveAll(
            listOf(
                first,
                second,
                preserved
            )
        )

        repository.deleteByContentIds(
            setOf(
                ContentId("content-1"),
                ContentId("content-2")
            )
        )

        assertNull(
            repository.findById(first.id)
        )

        assertNull(
            repository.findById(second.id)
        )

        assertEquals(
            listOf(preserved),
            repository.findAllEnabled()
        )
    }

    @Test
    fun `deleteByContentIds accepts empty set without changing repository`() {
        val repository =
            createRepository()

        val item =
            createItem(
                id = "item-1"
            )

        repository.save(item)

        repository.deleteByContentIds(
            emptySet()
        )

        assertEquals(
            item,
            repository.findById(item.id)
        )
    }

    private fun createItem(
        id: String,
        contentId: String = "content-1",
        enabled: Boolean = true,
        mode: LearningMode = LearningMode.MEANING_RECOGNITION
    ): LearningItem =
        LearningItem(
            id = LearningItemId(id),
            contentId = ContentId(contentId),
            mode = mode,
            isEnabled = enabled
        )
}