package vn.loi.learning.infrastructure.persistence.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.persistence.record.ContentLibraryRecord
import vn.loi.learning.infrastructure.persistence.record.ContentRecord
import vn.loi.learning.infrastructure.persistence.record.LearningItemRecord
import vn.loi.learning.infrastructure.persistence.store.ContentLibraryStore
import vn.loi.learning.infrastructure.persistence.store.ContentStore
import vn.loi.learning.infrastructure.persistence.store.LearningItemStore

class StoreBackedBatchSaveOptimizationTest {

    @Test
    fun `saving identical content batch does not write store again`() {
        val store =
            CountingContentStore()

        val repository =
            StoreBackedContentRepository(
                store
            )

        val contents =
            listOf(
                createContent(
                    id = "content-1",
                    text = "One"
                ),
                createContent(
                    id = "content-2",
                    text = "Two"
                )
            )

        repository.saveAll(
            contents
        )

        repository.saveAll(
            contents
        )

        assertEquals(
            1,
            store.saveCount
        )
    }

    @Test
    fun `changing one content in batch writes store once more`() {
        val store =
            CountingContentStore()

        val repository =
            StoreBackedContentRepository(
                store
            )

        repository.saveAll(
            listOf(
                createContent(
                    id = "content-1",
                    text = "Original"
                ),
                createContent(
                    id = "content-2",
                    text = "Preserved"
                )
            )
        )

        repository.saveAll(
            listOf(
                createContent(
                    id = "content-1",
                    text = "Updated"
                )
            )
        )

        assertEquals(
            2,
            store.saveCount
        )
    }

    @Test
    fun `saving identical library batch does not write store again`() {
        val store =
            CountingContentLibraryStore()

        val repository =
            StoreBackedContentLibraryRepository(
                store
            )

        val libraries =
            listOf(
                createLibrary(
                    id = "library-1",
                    name = "One"
                ),
                createLibrary(
                    id = "library-2",
                    name = "Two"
                )
            )

        repository.saveAll(
            libraries
        )

        repository.saveAll(
            libraries
        )

        assertEquals(
            1,
            store.saveCount
        )
    }

    @Test
    fun `changing one library in batch writes store once more`() {
        val store =
            CountingContentLibraryStore()

        val repository =
            StoreBackedContentLibraryRepository(
                store
            )

        repository.saveAll(
            listOf(
                createLibrary(
                    id = "library-1",
                    name = "Original"
                ),
                createLibrary(
                    id = "library-2",
                    name = "Preserved"
                )
            )
        )

        repository.saveAll(
            listOf(
                createLibrary(
                    id = "library-1",
                    name = "Updated"
                )
            )
        )

        assertEquals(
            2,
            store.saveCount
        )
    }

    @Test
    fun `saving identical learning item batch does not write store again`() {
        val store =
            CountingLearningItemStore()

        val repository =
            StoreBackedLearningItemRepository(
                store
            )

        val learningItems =
            listOf(
                createLearningItem(
                    id = "item-1",
                    contentId = "content-1"
                ),
                createLearningItem(
                    id = "item-2",
                    contentId = "content-2"
                )
            )

        repository.saveAll(
            learningItems
        )

        repository.saveAll(
            learningItems
        )

        assertEquals(
            1,
            store.saveCount
        )
    }

    @Test
    fun `changing one learning item in batch writes store once more`() {
        val store =
            CountingLearningItemStore()

        val repository =
            StoreBackedLearningItemRepository(
                store
            )

        repository.saveAll(
            listOf(
                createLearningItem(
                    id = "item-1",
                    contentId = "content-1",
                    enabled = true
                ),
                createLearningItem(
                    id = "item-2",
                    contentId = "content-2",
                    enabled = true
                )
            )
        )

        repository.saveAll(
            listOf(
                createLearningItem(
                    id = "item-1",
                    contentId = "content-1",
                    enabled = false
                )
            )
        )

        assertEquals(
            2,
            store.saveCount
        )
    }

    private fun createContent(
        id: String,
        text: String
    ): Content =
        Content(
            id = ContentId(id),
            type = ContentType.WORD,
            text = ContentText(
                primaryText = text
            )
        )

    private fun createLibrary(
        id: String,
        name: String
    ): ContentLibrary =
        ContentLibrary(
            id = ContentLibraryId(id),
            descriptor = LibraryDescriptor(
                name = name
            )
        )

    private fun createLearningItem(
        id: String,
        contentId: String,
        enabled: Boolean = true
    ): LearningItem =
        LearningItem(
            id = LearningItemId(id),
            contentId = ContentId(contentId),
            mode = LearningMode.MEANING_RECOGNITION,
            isEnabled = enabled
        )

    private class CountingContentStore : ContentStore {

        private var records =
            emptyList<ContentRecord>()

        var saveCount: Int = 0
            private set

        override fun loadAll(): List<ContentRecord> =
            records.toList()

        override fun saveAll(
            records: List<ContentRecord>
        ) {
            saveCount += 1
            this.records = records.toList()
        }
    }

    private class CountingContentLibraryStore : ContentLibraryStore {

        private var records =
            emptyList<ContentLibraryRecord>()

        var saveCount: Int = 0
            private set

        override fun loadAll(): List<ContentLibraryRecord> =
            records.toList()

        override fun saveAll(
            records: List<ContentLibraryRecord>
        ) {
            saveCount += 1
            this.records = records.toList()
        }
    }

    private class CountingLearningItemStore : LearningItemStore {

        private var records =
            emptyList<LearningItemRecord>()

        var saveCount: Int = 0
            private set

        override fun loadAll(): List<LearningItemRecord> =
            records.toList()

        override fun saveAll(
            records: List<LearningItemRecord>
        ) {
            saveCount += 1
            this.records = records.toList()
        }
    }
}