package vn.loi.learning.infrastructure.library

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.library.command.LibraryCommandResult
import vn.loi.learning.application.library.command.LibraryCommandService
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.CollectionState
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.persistence.json.JsonCanonicalCollectionStore
import vn.loi.learning.infrastructure.persistence.json.JsonCanonicalLibraryStore
import vn.loi.learning.infrastructure.persistence.json.JsonInstalledPackageStore
import vn.loi.learning.infrastructure.persistence.record.InstalledPackageRecord
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedCanonicalCollectionRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedCanonicalLibraryRepository
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedInstalledPackageRepository
import vn.loi.learning.infrastructure.transaction.JsonFileTransactionRunner

class LibraryCommandIntegrationTest {

    // Test A — Active collection round-trip (R2-05)
    @Test
    fun `test A - active collection create rename and assignment survive restart`() {
        val tempDir = Files.createTempDirectory("library-integ-test-a")
        try {
            val context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!

            // 1. Seed installed package into default library
            val packageStore = JsonInstalledPackageStore(tempDir.resolve("installed-packages.json"))
            val pkgRecord = InstalledPackageRecord(
                id = "pkg-math-1",
                libraryId = defaultLibraryId.value,
                packageId = "package-math-1",
                topicId = "topic-math-1",
                name = "Math Package",
                version = "1.0.0",
                state = PackageState.ACTIVE.name,
                installedAt = Instant.now().toString(),
                contentCount = 10,
                learningItemCount = 20
            )
            packageStore.saveAll(listOf(pkgRecord))

            // Re-create context so default library populates initial entries
            val context1b = LearningApplicationFactory.createPersisted(tempDir)
            val cmd1b = context1b.libraryCommand!!

            // 2. Create collection "Math"
            val createRes = cmd1b.createCollection(defaultLibraryId, CollectionName("Math"))
            val col = (assertIs<LibraryCommandResult.Success<Collection>>(createRes)).value

            // 3. Assign package to collection
            val pkgId = InstalledPackageId("pkg-math-1")
            val assignRes = cmd1b.assignPackageToCollection(defaultLibraryId, col.id, pkgId)
            assertIs<LibraryCommandResult.Success<Collection>>(assignRes)

            // 4. Rename collection to "Advanced Math"
            val renameRes = cmd1b.renameCollection(defaultLibraryId, col.id, CollectionName("Advanced Math"))
            assertIs<LibraryCommandResult.Success<Collection>>(renameRes)

            // 5. Restart context
            val context2 = LearningApplicationFactory.createPersisted(tempDir)
            val query2 = context2.libraryQuery!!

            // 6-8. Verify collection, renamed name, and package assignment survive restart
            val tree = query2.getNavigationTree(defaultLibraryId)
            assertNotNull(tree, "Navigation tree must exist after restart")
            val colNode = tree.collections.firstOrNull { it.collection.id == col.id }
            assertNotNull(colNode, "Collection must exist after restart")
            assertEquals("Advanced Math", colNode.collection.name, "Renamed collection name must persist")
            assertEquals(1, colNode.assignedPackages.size, "Package assignment must persist")
            assertEquals(pkgId.value, colNode.assignedPackages.first().id.value)
        } finally {
            deleteDirectory(tempDir)
        }
    }

    // Test B — Soft-delete round-trip (R2-05 & AC-04, AC-05)
    @Test
    fun `test B - soft deleted collection survives restart in DELETED state and is omitted from active navigation`() {
        val tempDir = Files.createTempDirectory("library-integ-test-b")
        try {
            val context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!
            val cmd1 = context1.libraryCommand!!

            // 1. Create collection
            val createRes = cmd1.createCollection(defaultLibraryId, CollectionName("Temp Collection"))
            val col = (assertIs<LibraryCommandResult.Success<Collection>>(createRes)).value

            // 2. Delete collection
            val deleteRes = cmd1.deleteCollection(defaultLibraryId, col.id)
            val deletedCol = (assertIs<LibraryCommandResult.Success<Collection>>(deleteRes)).value
            assertTrue(deletedCol.isDeleted)

            // 3. Restart context
            val context2 = LearningApplicationFactory.createPersisted(tempDir)
            val query2 = context2.libraryQuery!!

            // 4. Verify raw repository query finds aggregate in DELETED state (AC-04)
            val rawCollectionRepo = StoreBackedCanonicalCollectionRepository(
                JsonCanonicalCollectionStore(tempDir.resolve("canonical-library-collections.json"))
            )
            val persistedCol = rawCollectionRepo.findById(col.id)
            assertNotNull(persistedCol, "Raw canonical repository must find soft-deleted collection aggregate")
            assertEquals(CollectionState.DELETED, persistedCol.state, "Collection state must be DELETED on disk")

            // 5. Active navigation query omits deleted collection (AC-05)
            val tree = query2.getNavigationTree(defaultLibraryId)
            assertNotNull(tree)
            assertTrue(tree.collections.none { it.collection.id == col.id }, "Navigation query must omit DELETED collection")
        } finally {
            deleteDirectory(tempDir)
        }
    }

    // Test C — Library registration round-trip (R2-05 & AC-06)
    @Test
    fun `test C - library entries survive restart`() {
        val tempDir = Files.createTempDirectory("library-integ-test-c")
        try {
            val context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!

            // Seed package into installed package store
            val packageStore = JsonInstalledPackageStore(tempDir.resolve("installed-packages.json"))
            val pkgRecord = InstalledPackageRecord(
                id = "pkg-reg-1",
                libraryId = defaultLibraryId.value,
                packageId = "package-reg-1",
                topicId = "topic-reg-1",
                name = "Registered Package",
                version = "1.0.0",
                state = PackageState.ACTIVE.name,
                installedAt = Instant.now().toString(),
                contentCount = 5,
                learningItemCount = 5
            )
            packageStore.saveAll(listOf(pkgRecord))

            // Re-create context to trigger bootstrap reconciliation
            val context1b = LearningApplicationFactory.createPersisted(tempDir)
            val pkgId = InstalledPackageId("pkg-reg-1")

            // Restart context
            val context2 = LearningApplicationFactory.createPersisted(tempDir)

            // Verify raw library repository loads persisted entry
            val rawLibraryRepo = StoreBackedCanonicalLibraryRepository(
                JsonCanonicalLibraryStore(tempDir.resolve("canonical-libraries.json"))
            )
            val persistedLib = rawLibraryRepo.findById(defaultLibraryId)
            assertNotNull(persistedLib, "Canonical Library aggregate must exist")
            assertTrue(persistedLib.hasPackage(pkgId), "Library must retain registered package entry after restart")
        } finally {
            deleteDirectory(tempDir)
        }
    }

    // Test D — Real Command-Level Rollback (R2-05 & AC-08)
    @Test
    fun `test D - command-level persistence failure rolls back all canonical files without partial state`() {
        val tempDir = Files.createTempDirectory("library-integ-test-d")
        try {
            val context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!
            val cmd1 = context1.libraryCommand!!

            // Seed an active package
            val packageStore = JsonInstalledPackageStore(tempDir.resolve("installed-packages.json"))
            val pkgRecord = InstalledPackageRecord(
                id = "pkg-rollback-1",
                libraryId = defaultLibraryId.value,
                packageId = "package-roll-1",
                topicId = "topic-roll-1",
                name = "Rollback Package",
                version = "1.0.0",
                state = PackageState.ACTIVE.name,
                installedAt = Instant.now().toString(),
                contentCount = 5,
                learningItemCount = 5
            )
            packageStore.saveAll(listOf(pkgRecord))

            val context2 = LearningApplicationFactory.createPersisted(tempDir)
            val cmd2 = context2.libraryCommand!!
            val pkgId = InstalledPackageId("pkg-rollback-1")

            // Create initial collection
            val colRes = cmd2.createCollection(defaultLibraryId, CollectionName("Initial Col"))
            val col = (assertIs<LibraryCommandResult.Success<Collection>>(colRes)).value

            val packagesFile = tempDir.resolve("installed-packages.json")
            val librariesFile = tempDir.resolve("canonical-libraries.json")
            val collectionsFile = tempDir.resolve("canonical-library-collections.json")

            val initialPackagesContent = Files.readString(packagesFile)
            val initialLibrariesContent = Files.readString(librariesFile)
            val initialCollectionsContent = Files.readString(collectionsFile)

            // Create a failing TransactionRunner wrapper that simulates IO failure during command save
            val transactionRunner = JsonFileTransactionRunner(listOf(packagesFile, librariesFile, collectionsFile))
            val failingRunner = object : vn.loi.learning.application.port.TransactionRunner {
                override fun <T> runInTransaction(block: () -> T): T {
                    return transactionRunner.runInTransaction {
                        val result = block()
                        // Simulate IO failure during multi-state transaction
                        throw java.io.IOException("Disk write failure during transaction")
                    }
                }
            }

            val failingLibraryRepo = StoreBackedCanonicalLibraryRepository(JsonCanonicalLibraryStore(librariesFile))
            val failingPackageRepo = StoreBackedInstalledPackageRepository(JsonInstalledPackageStore(packagesFile))
            val failingCollectionRepo = StoreBackedCanonicalCollectionRepository(JsonCanonicalCollectionStore(collectionsFile))

            val failingCmdService = LibraryCommandService(
                libraryRepository = failingLibraryRepo,
                installedPackageRepository = failingPackageRepo,
                collectionRepository = failingCollectionRepo,
                transactionRunner = failingRunner
            )

            // Execute command that fails mid-way
            val failResult = failingCmdService.renameCollection(defaultLibraryId, col.id, CollectionName("Failed Rename"))

            // Verify command returns PersistenceFailure
            val failure = assertIs<LibraryCommandResult.PersistenceFailure>(failResult)
            assertTrue(failure.message.contains("Disk write failure"))

            // Verify all transaction files reverted cleanly on disk
            assertEquals(initialPackagesContent, Files.readString(packagesFile))
            assertEquals(initialLibrariesContent, Files.readString(librariesFile))
            assertEquals(initialCollectionsContent, Files.readString(collectionsFile))

            // Restart context to confirm no partial state exists
            val contextRestart = LearningApplicationFactory.createPersisted(tempDir)
            val queryRestart = contextRestart.libraryQuery!!
            val tree = queryRestart.getNavigationTree(defaultLibraryId)
            assertNotNull(tree)
            val colNode = tree.collections.firstOrNull { it.collection.id == col.id }
            assertNotNull(colNode)
            assertEquals("Initial Col", colNode.collection.name, "Renamed name must NOT exist after rollback")
        } finally {
            deleteDirectory(tempDir)
        }
    }

    // Test E — Default Library Bootstrap policy (AC-07)
    @Test
    fun `test E - default library is not overwritten on context restart`() {
        val tempDir = Files.createTempDirectory("library-integ-test-e")
        try {
            val context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!

            val libStore = JsonCanonicalLibraryStore(tempDir.resolve("canonical-libraries.json"))
            val initialLibRecords = libStore.loadAll()
            assertFalse(initialLibRecords.isEmpty(), "Canonical libraries file must exist and contain default library")

            // Restart context
            val context2 = LearningApplicationFactory.createPersisted(tempDir)
            val reloadedLibRecords = libStore.loadAll()

            assertEquals(initialLibRecords.size, reloadedLibRecords.size)
            assertEquals(initialLibRecords.first().id, reloadedLibRecords.first().id)
            assertEquals(initialLibRecords.first().createdAt, reloadedLibRecords.first().createdAt, "Default Library must retain original creation timestamp without overwrite")
        } finally {
            deleteDirectory(tempDir)
        }
    }

    // Test F — Active package selection and restart persistence (AC-04, AC-05)
    @Test
    fun `test F - set active package persists across restart`() {
        val tempDir = Files.createTempDirectory("library-integ-test-f")
        try {
            val context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!

            // Seed active package
            val packageStore = JsonInstalledPackageStore(tempDir.resolve("installed-packages.json"))
            val pkgRecord = InstalledPackageRecord(
                id = "pkg-active-1",
                libraryId = defaultLibraryId.value,
                packageId = "package-act-1",
                topicId = "topic-act-1",
                name = "Active Test Package",
                version = "1.0.0",
                state = PackageState.ACTIVE.name,
                installedAt = Instant.now().toString(),
                contentCount = 5,
                learningItemCount = 5
            )
            packageStore.saveAll(listOf(pkgRecord))

            val context1b = LearningApplicationFactory.createPersisted(tempDir)
            val cmd1b = context1b.libraryCommand!!
            val pkgId = InstalledPackageId("pkg-active-1")

            // 1. Set active package
            val setActiveRes = cmd1b.setActivePackage(defaultLibraryId, pkgId)
            assertIs<LibraryCommandResult.Success<Library>>(setActiveRes)

            // 2. Restart context
            val context2 = LearningApplicationFactory.createPersisted(tempDir)
            val query2 = context2.libraryQuery!!
            val tree = query2.getNavigationTree(defaultLibraryId)
            assertNotNull(tree)

            // 3. Verify activePackageId persists after restart
            assertEquals(pkgId, tree.activePackageId, "Active package ID must persist across restart")
        } finally {
            deleteDirectory(tempDir)
        }
    }

    // Test G — Package move up and move down ordering persistence (AC-06, AC-07, AC-08)
    @Test
    fun `test G - move package up and down changes order and persists across restart`() {
        val tempDir = Files.createTempDirectory("library-integ-test-g")
        try {
            val context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!

            // Seed two active packages
            val packageStore = JsonInstalledPackageStore(tempDir.resolve("installed-packages.json"))
            val pkg1 = InstalledPackageRecord(
                id = "pkg-ord-1",
                libraryId = defaultLibraryId.value,
                packageId = "package-ord-1",
                topicId = "topic-ord-1",
                name = "Alpha Package",
                version = "1.0.0",
                state = PackageState.ACTIVE.name,
                installedAt = Instant.now().toString(),
                contentCount = 5,
                learningItemCount = 5
            )
            val pkg2 = InstalledPackageRecord(
                id = "pkg-ord-2",
                libraryId = defaultLibraryId.value,
                packageId = "package-ord-2",
                topicId = "topic-ord-2",
                name = "Beta Package",
                version = "1.0.0",
                state = PackageState.ACTIVE.name,
                installedAt = Instant.now().toString(),
                contentCount = 5,
                learningItemCount = 5
            )
            packageStore.saveAll(listOf(pkg1, pkg2))

            val context1b = LearningApplicationFactory.createPersisted(tempDir)
            val cmd1b = context1b.libraryCommand!!
            val pkgId1 = InstalledPackageId("pkg-ord-1")
            val pkgId2 = InstalledPackageId("pkg-ord-2")

            // Move pkg2 up (above pkg1)
            val moveRes = cmd1b.movePackageUp(defaultLibraryId, pkgId2)
            assertIs<LibraryCommandResult.Success<Library>>(moveRes)

            // Restart context
            val context2 = LearningApplicationFactory.createPersisted(tempDir)
            val query2 = context2.libraryQuery!!
            val tree2 = query2.getNavigationTree(defaultLibraryId)
            assertNotNull(tree2)

            // Verify order after move up: pkg2 should come first
            assertEquals(2, tree2.activePackages.size)
            assertEquals(pkgId2.value, tree2.activePackages[0].id.value)
            assertEquals(pkgId1.value, tree2.activePackages[1].id.value)

            // Move pkg2 down (below pkg1)
            val cmd2 = context2.libraryCommand!!
            val moveDownRes = cmd2.movePackageDown(defaultLibraryId, pkgId2)
            assertIs<LibraryCommandResult.Success<Library>>(moveDownRes)

            // Restart context again
            val context3 = LearningApplicationFactory.createPersisted(tempDir)
            val query3 = context3.libraryQuery!!
            val tree3 = query3.getNavigationTree(defaultLibraryId)
            assertNotNull(tree3)

            // Verify order after move down: pkg1 should come first
            assertEquals(pkgId1.value, tree3.activePackages[0].id.value)
            assertEquals(pkgId2.value, tree3.activePackages[1].id.value)
        } finally {
            deleteDirectory(tempDir)
        }
    }

    private fun deleteDirectory(dir: java.nio.file.Path) {
        Files.walk(dir).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
