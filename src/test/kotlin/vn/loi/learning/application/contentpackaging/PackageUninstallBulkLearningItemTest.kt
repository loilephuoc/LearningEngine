package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository

class PackageUninstallBulkLearningItemTest {

    @Test
    fun `large package uninstall uses one exact bulk learning item mutation`() {
        val packageId = PackageId("large-package")
        val libraryId = ContentLibraryId("large-library")
        val contentIds = (0 until 990).mapTo(linkedSetOf()) { ContentId("content-$it") }
        val removableItems = contentIds.flatMapIndexed { contentIndex, contentId ->
            (0 until 5).map { itemIndex ->
                LearningItem(
                    id = LearningItemId("item-$contentIndex-$itemIndex"),
                    contentId = contentId,
                    mode = LearningMode.MEANING_RECOGNITION
                )
            }
        }
        val unrelatedContentId = ContentId("unrelated-content")
        val unrelatedItem = LearningItem(
            id = LearningItemId("unrelated-item"),
            contentId = unrelatedContentId,
            mode = LearningMode.MEANING_RECOGNITION
        )
        val contentLibraries = InMemoryContentLibraryRepository()
        val contents = InMemoryContentRepository()
        val learningItems = CountingBulkLearningItemRepository()
        val packages = InMemoryContentPackageRepository()

        contentLibraries.save(
            ContentLibrary(libraryId, LibraryDescriptor("Large"), contentIds)
        )
        contents.saveAll(
            contentIds.map { id ->
                Content(id, ContentType.WORD, ContentText(id.value))
            } + Content(unrelatedContentId, ContentType.WORD, ContentText("unrelated"))
        )
        learningItems.saveAll(removableItems + unrelatedItem)
        packages.save(
            ContentPackage(
                packageId,
                PackageDescriptor("Large", "1.0", "OPD3"),
                setOf(libraryId)
            )
        )

        PackageUninstallOperation(
            contentLibraryRepository = contentLibraries,
            contentRepository = contents,
            learningItemRepository = learningItems,
            contentPackageRepository = packages,
            packageCatalogRepository = InMemoryPackageCatalogRepository()
        ).execute(UninstallContentPackageCommand(PackageCatalogId("catalog"), packageId))

        assertEquals(4_950, learningItems.bulkDeletedIds.single().size)
        assertEquals(0, learningItems.singleDeleteCount)
        removableItems.forEach { assertNull(learningItems.findById(it.id)) }
        assertNotNull(learningItems.findById(unrelatedItem.id))
    }

    private class CountingBulkLearningItemRepository : LearningItemRepository {
        private val delegate = InMemoryLearningItemRepository()
        val bulkDeletedIds = mutableListOf<Set<LearningItemId>>()
        var singleDeleteCount = 0
            private set

        override fun findById(learningItemId: LearningItemId) = delegate.findById(learningItemId)
        override fun findByContentId(contentId: ContentId) = delegate.findByContentId(contentId)
        override fun findByContentIds(contentIds: Set<ContentId>) = delegate.findByContentIds(contentIds)
        override fun findAllEnabled() = delegate.findAllEnabled()
        override fun save(learningItem: LearningItem) = delegate.save(learningItem)
        override fun saveAll(learningItems: List<LearningItem>) = delegate.saveAll(learningItems)

        override fun deleteById(learningItemId: LearningItemId) {
            singleDeleteCount += 1
            delegate.deleteById(learningItemId)
        }

        override fun deleteAllById(learningItemIds: Set<LearningItemId>) {
            bulkDeletedIds += learningItemIds
            delegate.deleteAllById(learningItemIds)
        }
    }
}
