package vn.loi.learning.application.contentpackaging.browser

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.InstalledPackageQueryService
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository

class PackageContentBrowserQueryServiceTest {

    @Test
    fun `enforces one-row-per-content contract representation option A`() {
        val (service, pkgId) = createFixtureWithMultiItemContent()
        val items = service.getBrowserItemsForPackage(pkgId)

        assertEquals(1, items.size)
        val item = items.first()
        assertEquals("Apple", item.questionText)
        assertEquals(5, item.learningItemCount)
        assertEquals(5, item.learningItemIds.size)
        assertEquals(5, item.learningModes.size)
    }

    @Test
    fun `enforces canonical package ownership isolation between Package A and Package B`() {
        val (service, pkgAId, pkgBId) = createFixtureWithTwoPackages()

        val itemsA = service.getBrowserItemsForPackage(pkgAId)
        val itemsB = service.getBrowserItemsForPackage(pkgBId)

        assertEquals(2, itemsA.size)
        assertTrue(itemsA.all { it.packageName == "Package A" })
        assertTrue(itemsA.none { it.questionText.contains("Package B") })

        assertEquals(1, itemsB.size)
        assertTrue(itemsB.all { it.packageName == "Package B" })
        assertTrue(itemsB.none { it.questionText.contains("Package A") })
    }

    @Test
    fun `allows browsing ACTIVE package`() {
        val (service, pkgId) = createFixtureWithState(PackageState.ACTIVE)
        val items = service.getBrowserItemsForPackage(pkgId)
        assertEquals(1, items.size)
    }

    @Test
    fun `allows browsing ARCHIVED package`() {
        val (service, pkgId) = createFixtureWithState(PackageState.ARCHIVED)
        val items = service.getBrowserItemsForPackage(pkgId)
        assertEquals(1, items.size)
    }

    @Test
    fun `rejects browsing REMOVED or non-existent package`() {
        val (service, _) = createFixtureWithState(PackageState.ACTIVE)
        assertFailsWith<IllegalArgumentException> {
            service.getBrowserItemsForPackage(InstalledPackageId("non-existent-pkg"))
        }
    }

    private fun createFixtureWithMultiItemContent(): Pair<PackageContentBrowserQueryService, InstalledPackageId> {
        val instRepo = InMemoryInstalledPackageRepository()
        val pkgRepo = InMemoryContentPackageRepository()
        val libRepo = InMemoryContentLibraryRepository()
        val contentRepo = InMemoryContentRepository()
        val itemRepo = InMemoryLearningItemRepository()

        val instId = InstalledPackageId("inst-pkg-1")
        val pkgId = PackageId("pkg-1")
        val libId = ContentLibraryId("lib-1")

        instRepo.save(
            InstalledPackage.reconstitute(
                id = instId,
                libraryId = LibraryId("def-lib"),
                packageId = pkgId,
                topicId = TopicId.deriveForLegacyPackage("Vocabulary Topic", "OPD3"),
                name = PackageName("Vocabulary Topic"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 1,
                learningItemCount = 5
            )
        )
        pkgRepo.save(
            ContentPackage(
                id = pkgId,
                descriptor = PackageDescriptor(name = "Vocabulary Topic", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(libId)
            )
        )
        libRepo.save(
            ContentLibrary(
                id = libId,
                descriptor = LibraryDescriptor(name = "Vocabulary Library"),
                contentIds = setOf(ContentId("cnt-1"))
            )
        )

        val content1 = Content(
            id = ContentId("cnt-1"),
            type = ContentType.WORD,
            text = ContentText(primaryText = "Apple", translatedText = "Qua tao", pronunciation = "ˈæp.əl"),
            media = ContentMedia(primaryAudio = "apple.mp3", image = "apple.jpg"),
            metadata = ContentMetadata(lesson = "Fruit", group = "Food", section = "Sec 1", tags = setOf("noun"))
        )
        contentRepo.save(content1)

        val modes = listOf(
            LearningMode.MEANING_RECOGNITION,
            LearningMode.MEANING_RECALL,
            LearningMode.LISTENING_RECOGNITION,
            LearningMode.DICTATION,
            LearningMode.SPEAKING_RECALL
        )
        modes.forEachIndexed { i, mode ->
            itemRepo.save(LearningItem(id = LearningItemId("item-1-$i"), contentId = ContentId("cnt-1"), mode = mode))
        }

        val service = PackageContentBrowserQueryService(
            installedPackageRepository = instRepo,
            installedPackages = InstalledPackageQueryService(pkgRepo),
            contentPackageRepository = pkgRepo,
            contentLibraryRepository = libRepo,
            contentRepository = contentRepo,
            learningItemRepository = itemRepo
        )

        return service to instId
    }

    private fun createFixtureWithTwoPackages(): Triple<PackageContentBrowserQueryService, InstalledPackageId, InstalledPackageId> {
        val instRepo = InMemoryInstalledPackageRepository()
        val pkgRepo = InMemoryContentPackageRepository()
        val libRepo = InMemoryContentLibraryRepository()
        val contentRepo = InMemoryContentRepository()
        val itemRepo = InMemoryLearningItemRepository()

        val instA = InstalledPackageId("inst-pkg-A")
        val pkgA = PackageId("pkg-A")
        val libA = ContentLibraryId("lib-A")

        val instB = InstalledPackageId("inst-pkg-B")
        val pkgB = PackageId("pkg-B")
        val libB = ContentLibraryId("lib-B")

        instRepo.save(InstalledPackage.reconstitute(id = instA, libraryId = LibraryId("def-lib"), packageId = pkgA, topicId = TopicId.deriveForLegacyPackage("Package A", "OPD3"), name = PackageName("Package A"), version = PackageVersion("1.0.0"), state = PackageState.ACTIVE, installedAt = Instant.now(), contentCount = 2, learningItemCount = 2))
        pkgRepo.save(ContentPackage(id = pkgA, descriptor = PackageDescriptor(name = "Package A", version = "1.0.0", format = "OPD3"), libraryIds = setOf(libA)))
        libRepo.save(ContentLibrary(id = libA, descriptor = LibraryDescriptor(name = "Library A"), contentIds = setOf(ContentId("cnt-A1"), ContentId("cnt-A2"))))

        instRepo.save(InstalledPackage.reconstitute(id = instB, libraryId = LibraryId("def-lib"), packageId = pkgB, topicId = TopicId.deriveForLegacyPackage("Package B", "OPD3"), name = PackageName("Package B"), version = PackageVersion("1.0.0"), state = PackageState.ACTIVE, installedAt = Instant.now(), contentCount = 1, learningItemCount = 1))
        pkgRepo.save(ContentPackage(id = pkgB, descriptor = PackageDescriptor(name = "Package B", version = "1.0.0", format = "OPD3"), libraryIds = setOf(libB)))
        libRepo.save(ContentLibrary(id = libB, descriptor = LibraryDescriptor(name = "Library B"), contentIds = setOf(ContentId("cnt-B1"))))

        contentRepo.save(Content(id = ContentId("cnt-A1"), type = ContentType.WORD, text = ContentText("Word A1 in Package A")))
        contentRepo.save(Content(id = ContentId("cnt-A2"), type = ContentType.WORD, text = ContentText("Word A2 in Package A")))
        contentRepo.save(Content(id = ContentId("cnt-B1"), type = ContentType.WORD, text = ContentText("Word B1 in Package B")))

        itemRepo.save(LearningItem(id = LearningItemId("item-A1"), contentId = ContentId("cnt-A1"), mode = LearningMode.MEANING_RECOGNITION))
        itemRepo.save(LearningItem(id = LearningItemId("item-A2"), contentId = ContentId("cnt-A2"), mode = LearningMode.MEANING_RECOGNITION))
        itemRepo.save(LearningItem(id = LearningItemId("item-B1"), contentId = ContentId("cnt-B1"), mode = LearningMode.MEANING_RECOGNITION))

        val service = PackageContentBrowserQueryService(
            installedPackageRepository = instRepo,
            installedPackages = InstalledPackageQueryService(pkgRepo),
            contentPackageRepository = pkgRepo,
            contentLibraryRepository = libRepo,
            contentRepository = contentRepo,
            learningItemRepository = itemRepo
        )

        return Triple(service, instA, instB)
    }

    private fun createFixtureWithState(state: PackageState): Pair<PackageContentBrowserQueryService, InstalledPackageId> {
        val instRepo = InMemoryInstalledPackageRepository()
        val pkgRepo = InMemoryContentPackageRepository()
        val libRepo = InMemoryContentLibraryRepository()
        val contentRepo = InMemoryContentRepository()
        val itemRepo = InMemoryLearningItemRepository()

        val instId = InstalledPackageId("inst-pkg-state")
        val pkgId = PackageId("pkg-state")
        val libId = ContentLibraryId("lib-state")

        instRepo.save(InstalledPackage.reconstitute(id = instId, libraryId = LibraryId("def-lib"), packageId = pkgId, topicId = TopicId.deriveForLegacyPackage("State Package", "OPD3"), name = PackageName("State Package"), version = PackageVersion("1.0.0"), state = state, installedAt = Instant.now(), contentCount = 1, learningItemCount = 1))
        pkgRepo.save(ContentPackage(id = pkgId, descriptor = PackageDescriptor(name = "State Package", version = "1.0.0", format = "OPD3"), libraryIds = setOf(libId)))
        libRepo.save(ContentLibrary(id = libId, descriptor = LibraryDescriptor(name = "State Library"), contentIds = setOf(ContentId("cnt-s1"))))
        contentRepo.save(Content(id = ContentId("cnt-s1"), type = ContentType.WORD, text = ContentText("State Item")))
        itemRepo.save(LearningItem(id = LearningItemId("item-s1"), contentId = ContentId("cnt-s1"), mode = LearningMode.MEANING_RECOGNITION))

        val service = PackageContentBrowserQueryService(
            installedPackageRepository = instRepo,
            installedPackages = InstalledPackageQueryService(pkgRepo),
            contentPackageRepository = pkgRepo,
            contentLibraryRepository = libRepo,
            contentRepository = contentRepo,
            learningItemRepository = itemRepo
        )

        return service to instId
    }
}
