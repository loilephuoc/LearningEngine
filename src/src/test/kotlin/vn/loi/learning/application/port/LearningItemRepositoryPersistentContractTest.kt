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

    private val repository: LearningItemRepository =
        StoreBackedLearningItemRepository(
            store = InMemoryLearningItemStore()
        )

    private fun createItem(
        id: String,
        contentId: String = "content-1",
        enabled: Boolean = true
    ): LearningItem =
        LearningItem(
            id = LearningItemId(id),
            contentId = ContentId(contentId),
            mode = LearningMode.MEANING_RECOGNITION,
            isEnabled = enabled
        )

    @Test
    fun `findById returns null when item does not exist`() {
        assertNull(
            repository.findById(
                LearningItemId("missing-item")
            )
        )
    }

    @Test
    fun `save stores item and findById returns it`() {
        val item = createItem("item-1")

        repository.save(item)

        assertEquals(
            item,
            repository.findById(item.id)
        )
    }

    @Test
    fun `findByContentId returns matching items`() {
        val first = createItem("item-1", "content-1")
        val second = createItem("item-2", "content-2")

        repository.save(first)
        repository.save(second)

        assertEquals(
            listOf(first),
            repository.findByContentId(ContentId("content-1"))
        )
    }

    @Test
    fun `findAllEnabled excludes disabled items`() {
        val enabled = createItem("item-1", enabled = true)
        val disabled = createItem("item-2", enabled = false)

        repository.save(enabled)
        repository.save(disabled)

        assertEquals(
            listOf(enabled),
            repository.findAllEnabled()
        )
    }
}
