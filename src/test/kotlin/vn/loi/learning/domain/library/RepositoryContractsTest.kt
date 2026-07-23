package vn.loi.learning.domain.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.library.repository.CollectionRepository
import vn.loi.learning.domain.library.repository.InstalledPackageRepository
import vn.loi.learning.domain.library.repository.LibraryRepository

class RepositoryContractsTest {

    private val libId = LibraryId("lib-test")
    private val instId = InstalledPackageId("inst-pkg-1")
    private val pkgId = PackageId("pkg-1")
    private val topicId = TopicId("topic-1")
    private val colId = CollectionId("col-1")
    private val colName = CollectionName("Test Collection")

    // In-memory test doubles verifying repository interface contracts
    private class InMemoryLibraryRepository : LibraryRepository {
        private val storage = mutableMapOf<LibraryId, Library>()

        override fun findById(id: LibraryId): Library? = storage[id]

        override fun save(library: Library) {
            storage[library.id] = library
        }

        override fun existsById(id: LibraryId): Boolean = storage.containsKey(id)
    }

    private class InMemoryInstalledPackageRepository : InstalledPackageRepository {
        private val storage = mutableMapOf<InstalledPackageId, InstalledPackage>()

        override fun findById(id: InstalledPackageId): InstalledPackage? = storage[id]

        override fun findByPackageId(packageId: PackageId): InstalledPackage? =
            storage.values.firstOrNull { it.packageId == packageId && it.state != PackageState.REMOVED }

        override fun findByTopicId(topicId: TopicId): InstalledPackage? =
            storage.values.firstOrNull { it.topicId == topicId && it.state != PackageState.REMOVED }

        override fun findAllByState(state: PackageState): List<InstalledPackage> =
            storage.values.filter { it.state == state }

        override fun findAll(): List<InstalledPackage> = storage.values.toList()

        override fun save(installedPackage: InstalledPackage) {
            storage[installedPackage.id] = installedPackage
        }

        override fun delete(id: InstalledPackageId) {
            storage.remove(id)
        }
    }

    private class InMemoryCollectionRepository : CollectionRepository {
        private val storage = mutableMapOf<CollectionId, Collection>()

        override fun findById(id: CollectionId): Collection? = storage[id]

        override fun findByName(libraryId: LibraryId, name: CollectionName): Collection? =
            storage.values.firstOrNull { it.libraryId == libraryId && it.name == name }

        override fun findAllByLibraryId(libraryId: LibraryId): List<Collection> =
            storage.values.filter { it.libraryId == libraryId }

        override fun save(collection: Collection) {
            storage[collection.id] = collection
        }

        override fun delete(id: CollectionId) {
            storage.remove(id)
        }

        override fun existsByName(libraryId: LibraryId, name: CollectionName): Boolean =
            findByName(libraryId, name) != null
    }

    @Test
    fun `LibraryRepository contract operations behave correctly`() {
        val repo: LibraryRepository = InMemoryLibraryRepository()
        assertFalse(repo.existsById(libId))
        assertNull(repo.findById(libId))

        val library = Library(id = libId, name = "Test Library")
        repo.save(library)

        assertTrue(repo.existsById(libId))
        assertEquals(library, repo.findById(libId))
    }

    @Test
    fun `InstalledPackageRepository contract operations behave correctly`() {
        val repo: InstalledPackageRepository = InMemoryInstalledPackageRepository()
        val pkg = InstalledPackage(
            id = instId,
            libraryId = libId,
            packageId = pkgId,
            topicId = topicId,
            name = PackageName("Kanji N1"),
            version = PackageVersion("1.0"),
            contentCount = 10,
            learningItemCount = 20
        )

        repo.save(pkg)
        assertEquals(pkg, repo.findById(instId))
        assertEquals(pkg, repo.findByPackageId(pkgId))
        assertEquals(pkg, repo.findByTopicId(topicId))
        assertEquals(1, repo.findAllByState(PackageState.ACTIVE).size)

        repo.delete(instId)
        assertNull(repo.findById(instId))
    }

    @Test
    fun `CollectionRepository contract operations behave correctly`() {
        val repo: CollectionRepository = InMemoryCollectionRepository()
        val collection = Collection(id = colId, libraryId = libId, name = colName)

        assertFalse(repo.existsByName(libId, colName))
        repo.save(collection)

        assertTrue(repo.existsByName(libId, colName))
        assertEquals(collection, repo.findById(colId))
        assertEquals(collection, repo.findByName(libId, colName))
        assertEquals(1, repo.findAllByLibraryId(libId).size)

        repo.delete(colId)
        assertNull(repo.findById(colId))
    }
}
