package vn.loi.learning.infrastructure.library

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.infrastructure.LearningApplicationContext
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
        var context1: LearningApplicationContext? = null
        var context1b: LearningApplicationContext? = null
        var context2: LearningApplicationContext? = null
        try {
            context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!

            // 1. Seed installed package into default library
            val pkg = InstalledPackage(
                id = InstalledPackageId("pkg-math-1"),
                libraryId = defaultLibraryId,
                packageId = PackageId("package-math-1"),
                topicId = TopicId("topic-math-1"),
                name = PackageName("Math Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 10,
                learningItemCount = 20
            )
            context1.installedPackageRepository!!.save(pkg)
            val lib = context1.domainLibraryRepository!!.findById(defaultLibraryId)!!
            context1.domainLibraryRepository!!.save(lib.registerEntry(pkg.id, pkg.packageId, pkg.installedAt))
            context1.close()

            // Re-create context
            context1b = LearningApplicationFactory.createPersisted(tempDir)
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
            context1b.close()

            // 5. Restart context
            context2 = LearningApplicationFactory.createPersisted(tempDir)
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
            context1?.close()
            context1b?.close()
            context2?.close()
            deleteDirectory(tempDir)
        }
    }

    // Test B — Soft-delete round-trip (R2-05 & AC-04, AC-05)
    @Test
    fun `test B - soft deleted collection survives restart in DELETED state and is omitted from active navigation`() {
        val tempDir = Files.createTempDirectory("library-integ-test-b")
        var context1: LearningApplicationContext? = null
        var context2: LearningApplicationContext? = null
        try {
            context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!
            val cmd1 = context1.libraryCommand!!

            // 1. Create collection "To Delete"
            val createRes = cmd1.createCollection(defaultLibraryId, CollectionName("To Delete"))
            val col = (assertIs<LibraryCommandResult.Success<Collection>>(createRes)).value

            // 2. Soft-delete the collection
            val deleteRes = cmd1.deleteCollection(defaultLibraryId, col.id)
            val deletedCol = (assertIs<LibraryCommandResult.Success<Collection>>(deleteRes)).value
            assertEquals(CollectionState.DELETED, deletedCol.state, "Collection state must be DELETED")
            context1.close()

            // 3. Restart context
            context2 = LearningApplicationFactory.createPersisted(tempDir)
            val query2 = context2.libraryQuery!!

            // 4. Verify collection is omitted from active navigation tree
            val tree = query2.getNavigationTree(defaultLibraryId)
            assertNotNull(tree)
            val colNode = tree.collections.firstOrNull { it.collection.id == col.id }
            assertNull(colNode, "Soft-deleted collection must not appear in active navigation tree")

            // 5. Verify direct node query returns DELETED state
            val node = query2.getCollectionNode(col.id)
            assertNotNull(node, "Collection record must still exist on disk")
            assertEquals(CollectionState.DELETED, node.collection.state, "Collection must retain DELETED state on disk")
        } finally {
            context1?.close()
            context2?.close()
            deleteDirectory(tempDir)
        }
    }

    // Test C — Library entries round-trip (R2-05)
    @Test
    fun `test C - library entries survive restart`() {
        val tempDir = Files.createTempDirectory("library-integ-test-c")
        var context1: LearningApplicationContext? = null
        var context2: LearningApplicationContext? = null
        try {
            context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!

            // Seed package into installed package store
            val pkgId = InstalledPackageId("pkg-reg-1")
            val pkg = InstalledPackage(
                id = pkgId,
                libraryId = defaultLibraryId,
                packageId = PackageId("package-reg-1"),
                topicId = TopicId("topic-reg-1"),
                name = PackageName("Registered Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 5,
                learningItemCount = 5
            )
            context1.installedPackageRepository!!.save(pkg)
            val lib = context1.domainLibraryRepository!!.findById(defaultLibraryId)!!
            context1.domainLibraryRepository!!.save(lib.registerEntry(pkg.id, pkg.packageId, pkg.installedAt))
            context1.close()

            // Restart context
            context2 = LearningApplicationFactory.createPersisted(tempDir)
            val persistedLib = context2.domainLibraryRepository!!.findById(defaultLibraryId)
            assertNotNull(persistedLib, "Canonical Library aggregate must exist")
            assertTrue(persistedLib.hasPackage(pkgId), "Library must retain registered package entry after restart")
        } finally {
            context1?.close()
            context2?.close()
            deleteDirectory(tempDir)
        }
    }

    // Test D — Real Command-Level Rollback (R2-05 & AC-08)
    @Test
    fun `test D - command-level persistence failure rolls back all canonical files without partial state`() {
        val tempDir = Files.createTempDirectory("library-integ-test-d")
        try {
            val defaultLibraryId = LibraryId("default-library")
            val packagesFile = tempDir.resolve("installed-packages.json")
            val librariesFile = tempDir.resolve("canonical-libraries.json")
            val collectionsFile = tempDir.resolve("canonical-library-collections.json")

            val packageStore = JsonInstalledPackageStore(packagesFile)
            val libStore = JsonCanonicalLibraryStore(librariesFile)
            val colStore = JsonCanonicalCollectionStore(collectionsFile)

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

            val libRecord = vn.loi.learning.infrastructure.persistence.record.CanonicalLibraryRecord(
                id = defaultLibraryId.value,
                name = "Default Library",
                entries = listOf(
                    vn.loi.learning.infrastructure.persistence.record.LibraryEntryRecord(
                        installedPackageId = "pkg-rollback-1",
                        packageId = "package-roll-1",
                        registeredAt = Instant.now().toString()
                    )
                ),
                createdAt = Instant.now().toString()
            )
            libStore.saveAll(listOf(libRecord))

            val colRecord = vn.loi.learning.infrastructure.persistence.record.CanonicalCollectionRecord(
                id = "col-initial",
                libraryId = defaultLibraryId.value,
                name = "Initial Col",
                description = "",
                assignedPackageIds = emptyList(),
                state = CollectionState.ACTIVE.name,
                createdAt = Instant.now().toString()
            )
            colStore.saveAll(listOf(colRecord))

            val initialPackagesContent = Files.readString(packagesFile)
            val initialLibrariesContent = Files.readString(librariesFile)
            val initialCollectionsContent = Files.readString(collectionsFile)

            val transactionRunner = JsonFileTransactionRunner(listOf(packagesFile, librariesFile, collectionsFile))
            val failingRunner = object : vn.loi.learning.application.port.TransactionRunner {
                override fun <T> runInTransaction(block: () -> T): T {
                    return transactionRunner.runInTransaction {
                        val result = block()
                        throw java.io.IOException("Disk write failure during transaction")
                    }
                }
            }

            val failingLibraryRepo = StoreBackedCanonicalLibraryRepository(libStore)
            val failingPackageRepo = StoreBackedInstalledPackageRepository(packageStore)
            val failingCollectionRepo = StoreBackedCanonicalCollectionRepository(colStore)

            val failingCmdService = LibraryCommandService(
                libraryRepository = failingLibraryRepo,
                installedPackageRepository = failingPackageRepo,
                collectionRepository = failingCollectionRepo,
                transactionRunner = failingRunner
            )

            val failResult = failingCmdService.renameCollection(defaultLibraryId, CollectionId("col-initial"), CollectionName("Failed Rename"))

            val failure = assertIs<LibraryCommandResult.PersistenceFailure>(failResult)
            assertTrue(failure.message.contains("Disk write failure"))

            assertEquals(initialPackagesContent, Files.readString(packagesFile))
            assertEquals(initialLibrariesContent, Files.readString(librariesFile))
            assertEquals(initialCollectionsContent, Files.readString(collectionsFile))
        } finally {
            deleteDirectory(tempDir)
        }
    }

    // Test E — Default Library Bootstrap policy (AC-07)
    @Test
    fun `test E - default library is not overwritten on context restart`() {
        val tempDir = Files.createTempDirectory("library-integ-test-e")
        var context1: LearningApplicationContext? = null
        var context2: LearningApplicationContext? = null
        try {
            context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!
            val initialLib = context1.domainLibraryRepository!!.findById(defaultLibraryId)
            assertNotNull(initialLib, "Canonical library must exist")
            context1.close()

            // Restart context
            context2 = LearningApplicationFactory.createPersisted(tempDir)
            val reloadedLib = context2.domainLibraryRepository!!.findById(defaultLibraryId)
            assertNotNull(reloadedLib, "Canonical library must exist after restart")

            assertEquals(initialLib.id, reloadedLib.id)
            assertEquals(initialLib.name, reloadedLib.name)
            assertEquals(initialLib.createdAt, reloadedLib.createdAt, "Default Library must retain original creation timestamp without overwrite")
        } finally {
            context1?.close()
            context2?.close()
            deleteDirectory(tempDir)
        }
    }

    // Test F — Active package selection and restart persistence (AC-04, AC-05)
    @Test
    fun `test F - set active package persists across restart`() {
        val tempDir = Files.createTempDirectory("library-integ-test-f")
        var context1: LearningApplicationContext? = null
        var context1b: LearningApplicationContext? = null
        var context2: LearningApplicationContext? = null
        try {
            context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!

            // Seed active package
            val pkgId = InstalledPackageId("pkg-active-1")
            val pkg = InstalledPackage(
                id = pkgId,
                libraryId = defaultLibraryId,
                packageId = PackageId("package-act-1"),
                topicId = TopicId("topic-act-1"),
                name = PackageName("Active Test Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 5,
                learningItemCount = 5
            )
            context1.installedPackageRepository!!.save(pkg)
            val lib = context1.domainLibraryRepository!!.findById(defaultLibraryId)!!
            context1.domainLibraryRepository!!.save(lib.registerEntry(pkg.id, pkg.packageId, pkg.installedAt))
            context1.close()

            context1b = LearningApplicationFactory.createPersisted(tempDir)
            val cmd1b = context1b.libraryCommand!!

            // 1. Set active package
            val setActiveRes = cmd1b.setActivePackage(defaultLibraryId, pkgId)
            assertIs<LibraryCommandResult.Success<Library>>(setActiveRes)
            context1b.close()

            // 2. Restart context
            context2 = LearningApplicationFactory.createPersisted(tempDir)
            val query2 = context2.libraryQuery!!
            val tree = query2.getNavigationTree(defaultLibraryId)
            assertNotNull(tree)

            // 3. Verify activePackageId persists after restart
            assertEquals(pkgId, tree.activePackageId, "Active package ID must persist across restart")
        } finally {
            context1?.close()
            context1b?.close()
            context2?.close()
            deleteDirectory(tempDir)
        }
    }

    // Test G — Package move up and move down ordering persistence (AC-06, AC-07, AC-08)
    @Test
    fun `test G - move package up and down changes order and persists across restart`() {
        val tempDir = Files.createTempDirectory("library-integ-test-g")
        var context1: LearningApplicationContext? = null
        var context1b: LearningApplicationContext? = null
        var context2: LearningApplicationContext? = null
        var context3: LearningApplicationContext? = null
        try {
            context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!

            val pkgId1 = InstalledPackageId("pkg-ord-1")
            val pkg1 = InstalledPackage(
                id = pkgId1,
                libraryId = defaultLibraryId,
                packageId = PackageId("package-ord-1"),
                topicId = TopicId("topic-ord-1"),
                name = PackageName("Alpha Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 5,
                learningItemCount = 5
            )
            val pkgId2 = InstalledPackageId("pkg-ord-2")
            val pkg2 = InstalledPackage(
                id = pkgId2,
                libraryId = defaultLibraryId,
                packageId = PackageId("package-ord-2"),
                topicId = TopicId("topic-ord-2"),
                name = PackageName("Beta Package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 5,
                learningItemCount = 5
            )
            context1.installedPackageRepository!!.save(pkg1)
            context1.installedPackageRepository!!.save(pkg2)
            var lib = context1.domainLibraryRepository!!.findById(defaultLibraryId)!!
            lib = lib.registerEntry(pkg1.id, pkg1.packageId, pkg1.installedAt)
            lib = lib.registerEntry(pkg2.id, pkg2.packageId, pkg2.installedAt)
            context1.domainLibraryRepository!!.save(lib)
            context1.close()

            context1b = LearningApplicationFactory.createPersisted(tempDir)
            val cmd1b = context1b.libraryCommand!!

            // Move pkg2 up (above pkg1)
            val moveRes = cmd1b.movePackageUp(defaultLibraryId, pkgId2)
            assertIs<LibraryCommandResult.Success<Library>>(moveRes)
            context1b.close()

            // Restart context
            context2 = LearningApplicationFactory.createPersisted(tempDir)
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
            context2.close()

            // Restart context again
            context3 = LearningApplicationFactory.createPersisted(tempDir)
            val query3 = context3.libraryQuery!!
            val tree3 = query3.getNavigationTree(defaultLibraryId)
            assertNotNull(tree3)

            // Verify order after move down: pkg1 should come first
            assertEquals(pkgId1.value, tree3.activePackages[0].id.value)
            assertEquals(pkgId2.value, tree3.activePackages[1].id.value)
        } finally {
            context1?.close()
            context1b?.close()
            context2?.close()
            context3?.close()
            deleteDirectory(tempDir)
        }
    }

    // Test H1 — Archive Current Active Package clears activePackageId atomically (B1)
    @Test
    fun `test H1 - archive current active package clears activePackageId atomically and persists after restart`() {
        val tempDir = Files.createTempDirectory("library-integ-test-h1")
        var context1: LearningApplicationContext? = null
        var context1b: LearningApplicationContext? = null
        var context2: LearningApplicationContext? = null
        try {
            context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!

            val pkgId = InstalledPackageId("pkg-h1-1")
            val pkg = InstalledPackage(
                id = pkgId,
                libraryId = defaultLibraryId,
                packageId = PackageId("package-h1-1"),
                topicId = TopicId("topic-h1-1"),
                name = PackageName("Active Package H1"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 5,
                learningItemCount = 5
            )
            context1.installedPackageRepository!!.save(pkg)
            val lib = context1.domainLibraryRepository!!.findById(defaultLibraryId)!!
            context1.domainLibraryRepository!!.save(lib.registerEntry(pkg.id, pkg.packageId, pkg.installedAt))
            context1.close()

            context1b = LearningApplicationFactory.createPersisted(tempDir)
            val cmd1b = context1b.libraryCommand!!

            // 1. Set A active
            cmd1b.setActivePackage(defaultLibraryId, pkgId)
            val tree1 = context1b.libraryQuery?.getNavigationTree(defaultLibraryId)
            assertEquals(pkgId, tree1?.activePackageId)

            // 2. Archive A
            val archiveRes = cmd1b.archivePackage(defaultLibraryId, pkgId)
            val archivedPkg = (assertIs<LibraryCommandResult.Success<InstalledPackage>>(archiveRes)).value
            assertEquals(PackageState.ARCHIVED, archivedPkg.state)

            // Assert activePackageId cleared immediately
            val treeAfterArchive = context1b.libraryQuery?.getNavigationTree(defaultLibraryId)
            assertNull(treeAfterArchive?.activePackageId, "activePackageId must be null after archiving current active package")
            context1b.close()

            // 3. Restart context
            context2 = LearningApplicationFactory.createPersisted(tempDir)
            val tree2 = context2.libraryQuery?.getNavigationTree(defaultLibraryId)
            assertNull(tree2?.activePackageId, "activePackageId must remain null after restart")
        } finally {
            context1?.close()
            context1b?.close()
            context2?.close()
            deleteDirectory(tempDir)
        }
    }

    // Test H2 — Archive non-active package does not clear activePackageId (B2)
    @Test
    fun `test H2 - archive non-active package keeps current active package intact`() {
        val tempDir = Files.createTempDirectory("library-integ-test-h2")
        var context1: LearningApplicationContext? = null
        var context1b: LearningApplicationContext? = null
        try {
            context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!

            val pkgIdA = InstalledPackageId("pkg-h2-a")
            val pkgA = InstalledPackage(
                id = pkgIdA,
                libraryId = defaultLibraryId,
                packageId = PackageId("package-h2-a"),
                topicId = TopicId("topic-h2-a"),
                name = PackageName("Package A"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 5,
                learningItemCount = 5
            )
            val pkgIdB = InstalledPackageId("pkg-h2-b")
            val pkgB = InstalledPackage(
                id = pkgIdB,
                libraryId = defaultLibraryId,
                packageId = PackageId("package-h2-b"),
                topicId = TopicId("topic-h2-b"),
                name = PackageName("Package B"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 5,
                learningItemCount = 5
            )
            context1.installedPackageRepository!!.save(pkgA)
            context1.installedPackageRepository!!.save(pkgB)
            var lib = context1.domainLibraryRepository!!.findById(defaultLibraryId)!!
            lib = lib.registerEntry(pkgA.id, pkgA.packageId, pkgA.installedAt)
            lib = lib.registerEntry(pkgB.id, pkgB.packageId, pkgB.installedAt)
            context1.domainLibraryRepository!!.save(lib)
            context1.close()

            context1b = LearningApplicationFactory.createPersisted(tempDir)
            val cmd1b = context1b.libraryCommand!!

            // 1. Set A as active
            cmd1b.setActivePackage(defaultLibraryId, pkgIdA)

            // 2. Archive B
            val archiveBRes = cmd1b.archivePackage(defaultLibraryId, pkgIdB)
            val archivedB = (assertIs<LibraryCommandResult.Success<InstalledPackage>>(archiveBRes)).value
            assertEquals(PackageState.ARCHIVED, archivedB.state)

            // 3. Assert A remains active
            val tree = context1b.libraryQuery!!.getNavigationTree(defaultLibraryId)
            assertEquals(pkgIdA, tree?.activePackageId, "Active package A must remain active after archiving package B")
        } finally {
            context1?.close()
            context1b?.close()
            deleteDirectory(tempDir)
        }
    }

    // Test H3 — Restore package does not automatically set active (B3)
    @Test
    fun `test H3 - restore package does not automatically set package as current active`() {
        val tempDir = Files.createTempDirectory("library-integ-test-h3")
        var context1: LearningApplicationContext? = null
        var context1b: LearningApplicationContext? = null
        try {
            context1 = LearningApplicationFactory.createPersisted(tempDir)
            val defaultLibraryId = context1.defaultLibraryId!!

            val pkgIdA = InstalledPackageId("pkg-h3-a")
            val pkgA = InstalledPackage(
                id = pkgIdA,
                libraryId = defaultLibraryId,
                packageId = PackageId("package-h3-a"),
                topicId = TopicId("topic-h3-a"),
                name = PackageName("Package A"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ARCHIVED,
                installedAt = Instant.now(),
                contentCount = 5,
                learningItemCount = 5
            )
            context1.installedPackageRepository!!.save(pkgA)
            val lib = context1.domainLibraryRepository!!.findById(defaultLibraryId)!!
            context1.domainLibraryRepository!!.save(lib.registerEntry(pkgA.id, pkgA.packageId, pkgA.installedAt))
            context1.close()

            context1b = LearningApplicationFactory.createPersisted(tempDir)
            val cmd1b = context1b.libraryCommand!!

            // Restore package A
            val restoreRes = cmd1b.restorePackage(defaultLibraryId, pkgIdA)
            val restoredPkg = (assertIs<LibraryCommandResult.Success<InstalledPackage>>(restoreRes)).value
            assertEquals(PackageState.ACTIVE, restoredPkg.state)

            // Assert activePackageId is still null
            val tree = context1b.libraryQuery!!.getNavigationTree(defaultLibraryId)
            assertNull(tree?.activePackageId, "Restored package must NOT automatically become current active")
        } finally {
            context1?.close()
            context1b?.close()
            deleteDirectory(tempDir)
        }
    }

    // Test H4 — Legacy persisted state with activePackageId pointing to ARCHIVED package is sanitized (B4)
    @Test
    fun `test H4 - legacy persisted activePackageId pointing to archived package is sanitized to null without crash`() {
        val tempDir = Files.createTempDirectory("library-integ-test-h4")
        var context2: LearningApplicationContext? = null
        try {
            val defaultLibraryId = LibraryId("default-library")

            // Manually save an ARCHIVED package and a CanonicalLibraryRecord with activePackageId pointing to it
            val packageStore = JsonInstalledPackageStore(tempDir.resolve("installed-packages.json"))
            val pkgArchived = InstalledPackageRecord(
                id = "pkg-h4-arch",
                libraryId = defaultLibraryId.value,
                packageId = "package-h4-arch",
                topicId = "topic-h4-arch",
                name = "Archived Package H4",
                version = "1.0.0",
                state = PackageState.ARCHIVED.name,
                installedAt = Instant.now().toString(),
                contentCount = 5,
                learningItemCount = 5
            )
            packageStore.saveAll(listOf(pkgArchived))

            val libStore = JsonCanonicalLibraryStore(tempDir.resolve("canonical-libraries.json"))
            val libRecord = vn.loi.learning.infrastructure.persistence.record.CanonicalLibraryRecord(
                id = defaultLibraryId.value,
                name = "Default Library",
                entries = listOf(
                    vn.loi.learning.infrastructure.persistence.record.LibraryEntryRecord(
                        installedPackageId = "pkg-h4-arch",
                        packageId = "package-h4-arch",
                        registeredAt = Instant.now().toString()
                    )
                ),
                createdAt = Instant.now().toString(),
                activePackageId = "pkg-h4-arch" // Inconsistent legacy persisted state!
            )
            libStore.saveAll(listOf(libRecord))

            // Load context
            context2 = LearningApplicationFactory.createPersisted(tempDir)
            val query2 = context2.libraryQuery!!

            val tree = query2.getNavigationTree(defaultLibraryId)
            assertNotNull(tree, "Navigation tree must load without exception")
            assertNull(tree.activePackageId, "Query boundary must sanitize activePackageId to null for archived package")
        } finally {
            context2?.close()
            deleteDirectory(tempDir)
        }
    }

    private fun deleteDirectory(dir: java.nio.file.Path) {
        if (Files.notExists(dir)) return
        dir.toFile().deleteRecursively()
    }
}
