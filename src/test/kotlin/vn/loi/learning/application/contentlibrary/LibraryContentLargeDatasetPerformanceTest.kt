package vn.loi.learning.application.contentlibrary

import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.port.ContentLibraryRepository
import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
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

class LibraryContentLargeDatasetPerformanceTest {
    @Test
    fun `real scale projection uses one bulk read per repository without N plus one scans`() {
        val contents = (0 until 2_425).map { index ->
            Content(
                id = ContentId("content-$index"),
                type = ContentType.SENTENCE,
                text = ContentText(primaryText = "Sentence $index", translatedText = "Cau $index")
            )
        }
        val items = contents.flatMap { content ->
            (0 until 5).map { mode ->
                LearningItem(
                    id = LearningItemId("item-${content.id}-$mode"),
                    contentId = content.id,
                    mode = LearningMode.entries[mode]
                )
            }
        }
        assertEquals(12_125, items.size)
        val libraryId = ContentLibraryId("large-library")
        val contentRepository = CountingContentRepository(contents)
        val itemRepository = CountingLearningItemRepository(items)
        val service = LibraryContentQueryService(
            SingleLibraryRepository(
                ContentLibrary(libraryId, LibraryDescriptor("Large"), contents.map { it.id }.toSet())
            ),
            contentRepository,
            itemRepository
        )

        var projected = emptyList<LibraryContentItem>()
        val elapsed = measureTimeMillis { projected = service.query(libraryId) }

        assertEquals(2_425, projected.size)
        assertEquals(1, contentRepository.findAllCount)
        assertEquals(0, contentRepository.findByIdCount)
        assertEquals(1, itemRepository.findAllEnabledCount)
        assertEquals(0, itemRepository.findByContentIdCount)
        assertTrue(elapsed >= 0)
        println("large-library-projection-ms=$elapsed contents=${projected.size} learningItems=${items.size}")
    }

    private class SingleLibraryRepository(private val library: ContentLibrary) : ContentLibraryRepository {
        override fun findById(libraryId: ContentLibraryId) = library.takeIf { it.id == libraryId }
        override fun findAll() = listOf(library)
        override fun save(library: ContentLibrary) = Unit
        override fun deleteById(libraryId: ContentLibraryId) = Unit
    }

    private class CountingContentRepository(private val contents: List<Content>) : ContentRepository {
        var findAllCount = 0
        var findByIdCount = 0
        override fun findAll(): List<Content> { findAllCount++; return contents }
        override fun findById(contentId: ContentId): Content? { findByIdCount++; return contents.firstOrNull { it.id == contentId } }
        override fun save(content: Content) = Unit
        override fun deleteById(contentId: ContentId) = Unit
    }

    private class CountingLearningItemRepository(private val items: List<LearningItem>) : LearningItemRepository {
        var findAllEnabledCount = 0
        var findByContentIdCount = 0
        override fun findAllEnabled(): List<LearningItem> { findAllEnabledCount++; return items }
        override fun findByContentId(contentId: ContentId): List<LearningItem> { findByContentIdCount++; return items.filter { it.contentId == contentId } }
        override fun findById(learningItemId: LearningItemId) = items.firstOrNull { it.id == learningItemId }
        override fun save(learningItem: LearningItem) = Unit
        override fun deleteById(learningItemId: LearningItemId) = Unit
    }
}
