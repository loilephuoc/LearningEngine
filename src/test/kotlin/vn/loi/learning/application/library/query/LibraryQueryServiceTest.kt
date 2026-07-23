package vn.loi.learning.application.library.query

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.CollectionState
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryEntry
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.library.repository.CollectionRepository
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.library.repository.LibraryRepository

class LibraryQueryServiceTest {

    private val libId = LibraryId("lib-main")
    private val instId1 = InstalledPackageId("pkg-inst-1")
    private val instId2 = InstalledPackageId("pkg-inst-2")
    private val instId3 = InstalledPackageId("pkg-inst-3")
    private val colId1 = CollectionId("col-1")
    private val colId2 = CollectionId("col-2")

    private class InMemoryLibraryRepository : LibraryRepository {
        private val storage = mutableMapOf<LibraryId, Library>()
        override fun findById(id: LibraryId): Library? = storage[id]
        override fun save(library: Library) { storage[library.id] = library }
        override fun existsById(id: LibraryId): Boolean = storage.containsKey(id)
    }

    private class InMemoryInstalledPackageRepository : InstalledPackageRepository {
        private val storage = mutableMapOf<InstalledPackageId, InstalledPackage>()
        override fun findById(id: InstalledPackageId): InstalledPackage? = storage[id]
        override fun findByPackageId(packageId: PackageId): InstalledPackage? = storage.values.firstOrNull { it.packageId == packageId }
        override fun findByTopicId(topicId: TopicId): InstalledPackage? = storage.values.firstOrNull { it.topicId == topicId }
        override fun findAllByState(state: PackageState): List<InstalledPackage> = storage.values.filter { it.state == state }
        override fun findAll(): List<InstalledPackage> = storage.values.toList()
        override fun save(installedPackage: InstalledPackage) { storage[installedPackage.id] = installedPackage }
        override fun delete(id: InstalledPackageId) { storage.remove(id) }
    }

    private class InMemoryCollectionRepository : CollectionRepository {
        private val storage = mutableMapOf<CollectionId, Collection>()
        override fun findById(id: CollectionId): Collection? = storage[id]
        override fun findByName(libraryId: LibraryId, name: CollectionName): Collection? = storage.values.firstOrNull { it.libraryId == libraryId && it.name == name }
        override fun findAllByLibraryId(libraryId: LibraryId): List<Collection> = storage.values.filter { it.libraryId == libraryId }
        override fun save(collection: Collection) { storage[collection.id] = collection }
        override fun delete(id: CollectionId) { storage.remove(id) }
        override fun existsByName(libraryId: LibraryId, name: CollectionName): Boolean = findByName(libraryId, name) != null
    }

    @Test
    fun `getNavigationTree returns null when library does not exist`() {
        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()
        val queryService = LibraryQueryService(libRepo, pkgRepo, colRepo)

        assertNull(queryService.getNavigationTree(LibraryId("non-existent")))
        assertNull(queryService.getStatistics(LibraryId("non-existent")))
    }

    @Test
    fun `getNavigationTree builds complete read model hierarchy`() {
        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()

        val library = Library.reconstitute(
            id = libId,
            name = "Main Library",
            entries = listOf(
                LibraryEntry(instId1, PackageId("pkg-1")),
                LibraryEntry(instId2, PackageId("pkg-2")),
                LibraryEntry(instId3, PackageId("pkg-3"))
            )
        )
        libRepo.save(library)

        val pkg1 = InstalledPackage.reconstitute(
            id = instId1,
            libraryId = libId,
            packageId = PackageId("pkg-1"),
            topicId = TopicId("topic-1"),
            name = PackageName("Vocabulary N1"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 100,
            learningItemCount = 300
        )
        val pkg2 = InstalledPackage.reconstitute(
            id = instId2,
            libraryId = libId,
            packageId = PackageId("pkg-2"),
            topicId = TopicId("topic-2"),
            name = PackageName("Grammar N1"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ARCHIVED,
            installedAt = Instant.now(),
            contentCount = 50,
            learningItemCount = 150
        )
        val pkg3 = InstalledPackage.reconstitute(
            id = instId3,
            libraryId = libId,
            packageId = PackageId("pkg-3"),
            topicId = TopicId("topic-3"),
            name = PackageName("Kanji N1"),
            version = PackageVersion("1.0.0"),
            state = PackageState.REMOVED,
            installedAt = Instant.now(),
            contentCount = 20,
            learningItemCount = 60
        )
        pkgRepo.save(pkg1)
        pkgRepo.save(pkg2)
        pkgRepo.save(pkg3)

        val col1 = Collection.reconstitute(
            id = colId1,
            libraryId = libId,
            name = CollectionName("JLPT Prep"),
            assignedPackageIds = setOf(instId1, instId2),
            state = CollectionState.ACTIVE
        )
        val col2 = Collection.reconstitute(
            id = colId2,
            libraryId = libId,
            name = CollectionName("Old Collection"),
            assignedPackageIds = setOf(instId3),
            state = CollectionState.DELETED
        )
        colRepo.save(col1)
        colRepo.save(col2)

        val queryService = LibraryQueryService(libRepo, pkgRepo, colRepo)
        val tree = queryService.getNavigationTree(libId)

        assertNotNull(tree)
        assertEquals(libId, tree.libraryId)
        assertEquals("Main Library", tree.libraryName)

        // Installed packages (excluding REMOVED) -> pkg1, pkg2
        assertEquals(2, tree.installedPackages.size)

        // Active packages -> pkg1
        assertEquals(1, tree.activePackages.size)
        assertEquals("Vocabulary N1", tree.activePackages[0].name)

        // Archived packages -> pkg2
        assertEquals(1, tree.archivedPackages.size)
        assertEquals("Grammar N1", tree.archivedPackages[0].name)

        // Collections -> col1 (ACTIVE), contains only ACTIVE assigned packages (pkg1)
        assertEquals(1, tree.collections.size)
        assertEquals("JLPT Prep", tree.collections[0].collection.name)
        assertEquals(1, tree.collections[0].assignedPackages.size)
        assertEquals("Vocabulary N1", tree.collections[0].assignedPackages[0].name)

        // Deleted collections -> col2
        assertEquals(1, tree.deletedCollections.size)
        assertEquals("Old Collection", tree.deletedCollections[0].name)

        // Statistics assertion
        val stats = tree.statistics
        assertEquals(2, stats.totalInstalledPackagesCount)
        assertEquals(1, stats.activePackagesCount)
        assertEquals(1, stats.archivedPackagesCount)
        assertEquals(1, stats.removedPackagesCount)
        assertEquals(2, stats.totalCollectionsCount)
        assertEquals(1, stats.activeCollectionsCount)
        assertEquals(1, stats.deletedCollectionsCount)
        assertEquals(100, stats.totalActiveContentCount)
        assertEquals(300, stats.totalActiveLearningItemCount)
    }

    @Test
    fun `query methods return deterministically sorted DTOs`() {
        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()

        libRepo.save(Library.reconstitute(id = libId, name = "Lib"))

        val pkgB = InstalledPackage.reconstitute(
            id = InstalledPackageId("pkg-b"),
            libraryId = libId,
            packageId = PackageId("p2"),
            topicId = TopicId("t2"),
            name = PackageName("B Package"),
            version = PackageVersion("1.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 10,
            learningItemCount = 20
        )
        val pkgA = InstalledPackage.reconstitute(
            id = InstalledPackageId("pkg-a"),
            libraryId = libId,
            packageId = PackageId("p1"),
            topicId = TopicId("t1"),
            name = PackageName("A Package"),
            version = PackageVersion("1.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 10,
            learningItemCount = 20
        )
        pkgRepo.save(pkgB)
        pkgRepo.save(pkgA)

        val queryService = LibraryQueryService(libRepo, pkgRepo, colRepo)
        val activePkgs = queryService.getActivePackages(libId)

        assertEquals(2, activePkgs.size)
        assertEquals("A Package", activePkgs[0].name)
        assertEquals("B Package", activePkgs[1].name)
    }

    @Test
    fun `getPackageSummary and getCollectionNode return single items correctly`() {
        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()

        val pkg = InstalledPackage.reconstitute(
            id = instId1,
            libraryId = libId,
            packageId = PackageId("pkg-1"),
            topicId = TopicId("topic-1"),
            name = PackageName("Vocabulary N1"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 10,
            learningItemCount = 30
        )
        pkgRepo.save(pkg)

        val col = Collection.reconstitute(
            id = colId1,
            libraryId = libId,
            name = CollectionName("JLPT Prep"),
            assignedPackageIds = setOf(instId1),
            state = CollectionState.ACTIVE
        )
        colRepo.save(col)

        val queryService = LibraryQueryService(libRepo, pkgRepo, colRepo)

        val pkgSummary = queryService.getPackageSummary(instId1)
        assertNotNull(pkgSummary)
        assertEquals("Vocabulary N1", pkgSummary.name)

        val colNode = queryService.getCollectionNode(colId1)
        assertNotNull(colNode)
        assertEquals("JLPT Prep", colNode.collection.name)
        assertEquals(1, colNode.assignedPackages.size)
        assertEquals("Vocabulary N1", colNode.assignedPackages[0].name)
    }
}
