package vn.loi.learning.application.library.command

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
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
import vn.loi.learning.infrastructure.persistence.memory.InMemoryCollectionRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class LibraryCommandServiceTest {

    private val libraryId = LibraryId("lib-1")
    private val library = Library.reconstitute(id = libraryId, name = "Test Library")

    private val libraryRepo = InMemoryLibraryRepository().apply { save(library) }
    private val packageRepo = InMemoryInstalledPackageRepository()
    private val collectionRepo = InMemoryCollectionRepository()
    private val transactionRunner = InMemoryTransactionRunner()

    private val service = LibraryCommandService(
        libraryRepository = libraryRepo,
        installedPackageRepository = packageRepo,
        collectionRepository = collectionRepo,
        transactionRunner = transactionRunner
    )

    private fun createTestPackage(
        id: String,
        pkgId: String = "pkg-1",
        state: PackageState = PackageState.ACTIVE,
        versionStr: String = "1.0.0"
    ): InstalledPackage {
        val installedPackageId = InstalledPackageId(id)
        val packageId = PackageId(pkgId)
        val topicId = TopicId("topic-$id")
        val pkg = InstalledPackage.reconstitute(
            id = installedPackageId,
            libraryId = libraryId,
            packageId = packageId,
            topicId = topicId,
            name = PackageName("Package $id"),
            version = PackageVersion(versionStr),
            state = state,
            installedAt = Instant.now(),
            contentCount = 10,
            learningItemCount = 20
        )
        val currentLib = libraryRepo.findById(libraryId) ?: library
        libraryRepo.save(currentLib.registerEntry(installedPackageId, packageId))
        packageRepo.save(pkg)
        return pkg
    }

    // 1. ARCHIVE PACKAGE
    @Test
    fun `archive active package changes state to ARCHIVED and returns Success`() {
        val pkg = createTestPackage("pkg-active", state = PackageState.ACTIVE)

        val result = service.archivePackage(libraryId, pkg.id)

        val success = assertIs<LibraryCommandResult.Success<InstalledPackage>>(result)
        assertEquals(PackageState.ARCHIVED, success.value.state)
        assertEquals(PackageState.ARCHIVED, packageRepo.findById(pkg.id)?.state)
    }

    @Test
    fun `archive non-active package returns InvalidState`() {
        val pkg = createTestPackage("pkg-archived", state = PackageState.ARCHIVED)

        val result = service.archivePackage(libraryId, pkg.id)

        assertIs<LibraryCommandResult.InvalidState>(result)
    }

    @Test
    fun `archive non-existent package returns PackageNotFound`() {
        val result = service.archivePackage(libraryId, InstalledPackageId("missing"))

        assertIs<LibraryCommandResult.PackageNotFound>(result)
    }

    @Test
    fun `archive package in non-existent library returns LibraryNotFound`() {
        val result = service.archivePackage(LibraryId("missing-lib"), InstalledPackageId("pkg-1"))

        assertIs<LibraryCommandResult.LibraryNotFound>(result)
    }

    @Test
    fun `archive package belonging to another library returns CrossLibraryConflict`() {
        val otherLibId = LibraryId("lib-other")
        val pkg = InstalledPackage.reconstitute(
            id = InstalledPackageId("pkg-other"),
            libraryId = otherLibId,
            packageId = PackageId("pkg-x"),
            topicId = TopicId("topic-x"),
            name = PackageName("Other"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 5,
            learningItemCount = 5
        )
        packageRepo.save(pkg)

        val result = service.archivePackage(libraryId, pkg.id)

        assertIs<LibraryCommandResult.CrossLibraryConflict>(result)
    }

    // 2. RESTORE PACKAGE
    @Test
    fun `restore archived package changes state to ACTIVE and returns Success`() {
        val pkg = createTestPackage("pkg-to-restore", state = PackageState.ARCHIVED)

        val result = service.restorePackage(libraryId, pkg.id)

        val success = assertIs<LibraryCommandResult.Success<InstalledPackage>>(result)
        assertEquals(PackageState.ACTIVE, success.value.state)
        assertEquals(PackageState.ACTIVE, packageRepo.findById(pkg.id)?.state)
    }

    @Test
    fun `restore archived package when another active package with same PackageId exists returns ActiveVersionConflict`() {
        val activePkg = createTestPackage("pkg-active-v1", pkgId = "same-pkg-id", state = PackageState.ACTIVE, versionStr = "1.0.0")
        val archivedPkg = createTestPackage("pkg-archived-v2", pkgId = "same-pkg-id", state = PackageState.ARCHIVED, versionStr = "2.0.0")

        val result = service.restorePackage(libraryId, archivedPkg.id)

        val conflict = assertIs<LibraryCommandResult.ActiveVersionConflict>(result)
        assertEquals("same-pkg-id", conflict.packageId)
    }

    @Test
    fun `restore non-archived package returns InvalidState`() {
        val pkg = createTestPackage("pkg-already-active", state = PackageState.ACTIVE)

        val result = service.restorePackage(libraryId, pkg.id)

        assertIs<LibraryCommandResult.InvalidState>(result)
    }

    // 3. CREATE COLLECTION
    @Test
    fun `create collection with unique name returns Success`() {
        val name = CollectionName("Grammar")

        val result = service.createCollection(libraryId, name, "Grammar lessons")

        val success = assertIs<LibraryCommandResult.Success<Collection>>(result)
        assertEquals("Grammar", success.value.name.trimmedValue)
        assertTrue(success.value.isActive)
        assertNotNull(collectionRepo.findById(success.value.id))
    }

    @Test
    fun `create collection with duplicate name in same library returns DuplicateCollection`() {
        val name = CollectionName("Vocabulary")
        service.createCollection(libraryId, name)

        val result = service.createCollection(libraryId, name)

        val duplicate = assertIs<LibraryCommandResult.DuplicateCollection>(result)
        assertEquals("Vocabulary", duplicate.collectionName)
    }

    // 4. RENAME COLLECTION
    @Test
    fun `rename collection with valid name returns Success and preserves collection ID`() {
        val createResult = service.createCollection(libraryId, CollectionName("Old Name"))
        val collection = (createResult as LibraryCommandResult.Success).value

        val newName = CollectionName("New Name")
        val renameResult = service.renameCollection(libraryId, collection.id, newName)

        val success = assertIs<LibraryCommandResult.Success<Collection>>(renameResult)
        assertEquals(collection.id, success.value.id)
        assertEquals("New Name", success.value.name.trimmedValue)
    }

    @Test
    fun `rename collection to duplicate name returns DuplicateCollection`() {
        service.createCollection(libraryId, CollectionName("Col A"))
        val colB = (service.createCollection(libraryId, CollectionName("Col B")) as LibraryCommandResult.Success).value

        val result = service.renameCollection(libraryId, colB.id, CollectionName("Col A"))

        val duplicate = assertIs<LibraryCommandResult.DuplicateCollection>(result)
        assertEquals("Col A", duplicate.collectionName)
    }

    @Test
    fun `rename deleted collection returns InvalidState`() {
        val col = (service.createCollection(libraryId, CollectionName("To Delete")) as LibraryCommandResult.Success).value
        service.deleteCollection(libraryId, col.id)

        val result = service.renameCollection(libraryId, col.id, CollectionName("Renamed"))

        assertIs<LibraryCommandResult.InvalidState>(result)
    }

    // 5. DELETE COLLECTION
    @Test
    fun `delete collection soft deletes to DELETED state and preserves installed packages`() {
        val pkg = createTestPackage("pkg-assigned")
        val col = (service.createCollection(libraryId, CollectionName("Collection 1")) as LibraryCommandResult.Success).value
        service.assignPackageToCollection(libraryId, col.id, pkg.id)

        val result = service.deleteCollection(libraryId, col.id)

        val success = assertIs<LibraryCommandResult.Success<Collection>>(result)
        assertTrue(success.value.isDeleted)
        // Verify installed package remains active in package repository
        assertEquals(PackageState.ACTIVE, packageRepo.findById(pkg.id)?.state)
    }

    @Test
    fun `delete already deleted collection returns InvalidState`() {
        val col = (service.createCollection(libraryId, CollectionName("Collection 2")) as LibraryCommandResult.Success).value
        service.deleteCollection(libraryId, col.id)

        val result = service.deleteCollection(libraryId, col.id)

        assertIs<LibraryCommandResult.InvalidState>(result)
    }

    // 6. ASSIGN PACKAGE TO COLLECTION
    @Test
    fun `assign active package to active collection returns Success`() {
        val pkg = createTestPackage("pkg-assign")
        val col = (service.createCollection(libraryId, CollectionName("Favorites")) as LibraryCommandResult.Success).value

        val result = service.assignPackageToCollection(libraryId, col.id, pkg.id)

        val success = assertIs<LibraryCommandResult.Success<Collection>>(result)
        assertTrue(success.value.containsPackage(pkg.id))
    }

    @Test
    fun `assign non-active package to collection returns InvalidState`() {
        val pkg = createTestPackage("pkg-archived-assign", state = PackageState.ARCHIVED)
        val col = (service.createCollection(libraryId, CollectionName("Favorites 2")) as LibraryCommandResult.Success).value

        val result = service.assignPackageToCollection(libraryId, col.id, pkg.id)

        assertIs<LibraryCommandResult.InvalidState>(result)
    }

    @Test
    fun `assign already assigned package returns AlreadyAssigned`() {
        val pkg = createTestPackage("pkg-double-assign")
        val col = (service.createCollection(libraryId, CollectionName("Favorites 3")) as LibraryCommandResult.Success).value
        service.assignPackageToCollection(libraryId, col.id, pkg.id)

        val result = service.assignPackageToCollection(libraryId, col.id, pkg.id)

        val already = assertIs<LibraryCommandResult.AlreadyAssigned>(result)
        assertEquals(pkg.id.value, already.packageId)
        assertEquals(col.id.value, already.collectionId)
    }

    // 7. REMOVE PACKAGE FROM COLLECTION
    @Test
    fun `remove package from collection returns Success`() {
        val pkg = createTestPackage("pkg-to-remove")
        val col = (service.createCollection(libraryId, CollectionName("Temp Collection")) as LibraryCommandResult.Success).value
        service.assignPackageToCollection(libraryId, col.id, pkg.id)

        val result = service.removePackageFromCollection(libraryId, col.id, pkg.id)

        val success = assertIs<LibraryCommandResult.Success<Collection>>(result)
        assertFalse(success.value.containsPackage(pkg.id))
    }

    @Test
    fun `remove unassigned package from collection returns NotAssigned`() {
        val pkg = createTestPackage("pkg-never-assigned")
        val col = (service.createCollection(libraryId, CollectionName("Empty Collection")) as LibraryCommandResult.Success).value

        val result = service.removePackageFromCollection(libraryId, col.id, pkg.id)

        val notAssigned = assertIs<LibraryCommandResult.NotAssigned>(result)
        assertEquals(pkg.id.value, notAssigned.packageId)
        assertEquals(col.id.value, notAssigned.collectionId)
    }

    // 8. CROSS LIBRARY CONFLICTS
    @Test
    fun `cross library collection operation returns CrossLibraryConflict`() {
        val otherLibId = LibraryId("lib-other-2")
        val otherCol = Collection.reconstitute(
            id = CollectionId("col-other"),
            libraryId = otherLibId,
            name = CollectionName("Other Collection")
        )
        collectionRepo.save(otherCol)

        val result = service.renameCollection(libraryId, otherCol.id, CollectionName("Rename Other"))

        assertIs<LibraryCommandResult.CrossLibraryConflict>(result)
    }

    // 9. PERSISTENCE FAILURE ROLLBACK
    @Test
    fun `failing transaction returns PersistenceFailure`() {
        val failingRunner = object : vn.loi.learning.application.port.TransactionRunner {
            override fun <T> runInTransaction(block: () -> T): T {
                throw RuntimeException("Database connection lost")
            }
        }
        val failingService = LibraryCommandService(
            libraryRepository = libraryRepo,
            installedPackageRepository = packageRepo,
            collectionRepository = collectionRepo,
            transactionRunner = failingRunner
        )

        val result = failingService.createCollection(libraryId, CollectionName("Failing Col"))

        val failure = assertIs<LibraryCommandResult.PersistenceFailure>(result)
        assertTrue(failure.message.contains("Database connection lost"))
    }
}
