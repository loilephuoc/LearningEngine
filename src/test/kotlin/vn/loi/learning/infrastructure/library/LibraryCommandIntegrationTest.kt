package vn.loi.learning.infrastructure.library

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.library.command.LibraryCommandResult
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.persistence.json.JsonInstalledPackageStore
import vn.loi.learning.infrastructure.persistence.record.InstalledPackageRecord
import vn.loi.learning.infrastructure.persistence.repository.StoreBackedInstalledPackageRepository

class LibraryCommandIntegrationTest {

    @Test
    fun `persisted context wires libraryCommand and supports full command lifecycle and query round-trip`() {
        val tempDir = Files.createTempDirectory("library-command-integration-test")

        try {
            val context1 = LearningApplicationFactory.createPersisted(tempDir)
            val libraryCommand = context1.libraryCommand
            val libraryQuery = context1.libraryQuery
            val defaultLibraryId = context1.defaultLibraryId

            assertNotNull(libraryCommand, "libraryCommand must be wired in LearningApplicationContext")
            assertNotNull(libraryQuery, "libraryQuery must be wired in LearningApplicationContext")
            assertNotNull(defaultLibraryId, "defaultLibraryId must be wired in LearningApplicationContext")

            // Seed an InstalledPackage into persisted store
            val packageStore = JsonInstalledPackageStore(tempDir.resolve("installed-packages.json"))
            val pkgRecord = InstalledPackageRecord(
                id = "pkg-integration-1",
                libraryId = defaultLibraryId.value,
                packageId = "package-integ-1",
                topicId = "topic-integ-1",
                name = "Integration Package",
                version = "1.0.0",
                state = PackageState.ACTIVE.name,
                installedAt = Instant.now().toString(),
                contentCount = 5,
                learningItemCount = 10
            )
            packageStore.saveAll(listOf(pkgRecord))

            // Re-create context to pick up seeded record
            val context2 = LearningApplicationFactory.createPersisted(tempDir)
            val cmd = context2.libraryCommand!!
            val query = context2.libraryQuery!!

            val pkgId = InstalledPackageId("pkg-integration-1")

            // 1. Create collection
            val colResult = cmd.createCollection(defaultLibraryId, CollectionName("Science"))
            val colSuccess = assertIs<LibraryCommandResult.Success<Collection>>(colResult)
            val col = colSuccess.value
            assertEquals("Science", col.name.trimmedValue)

            // 2. Assign package to collection
            val assignResult = cmd.assignPackageToCollection(defaultLibraryId, col.id, pkgId)
            val assignSuccess = assertIs<LibraryCommandResult.Success<Collection>>(assignResult)
            assertTrue(assignSuccess.value.containsPackage(pkgId))

            // 3. Archive package
            val archiveResult = cmd.archivePackage(defaultLibraryId, pkgId)
            val archiveSuccess = assertIs<LibraryCommandResult.Success<InstalledPackage>>(archiveResult)
            assertEquals(PackageState.ARCHIVED, archiveSuccess.value.state)

            // 4. Restore package
            val restoreResult = cmd.restorePackage(defaultLibraryId, pkgId)
            val restoreSuccess = assertIs<LibraryCommandResult.Success<InstalledPackage>>(restoreResult)
            assertEquals(PackageState.ACTIVE, restoreSuccess.value.state)

            // 5. Rename collection
            val renameResult = cmd.renameCollection(defaultLibraryId, col.id, CollectionName("Advanced Science"))
            val renameSuccess = assertIs<LibraryCommandResult.Success<Collection>>(renameResult)
            assertEquals("Advanced Science", renameSuccess.value.name.trimmedValue)

            // 6. Remove package assignment
            val unassignResult = cmd.removePackageFromCollection(defaultLibraryId, col.id, pkgId)
            val unassignSuccess = assertIs<LibraryCommandResult.Success<Collection>>(unassignResult)
            assertFalse(unassignSuccess.value.containsPackage(pkgId))

            // 7. Delete collection
            val deleteResult = cmd.deleteCollection(defaultLibraryId, col.id)
            val deleteSuccess = assertIs<LibraryCommandResult.Success<Collection>>(deleteResult)
            assertTrue(deleteSuccess.value.isDeleted)

            // 8. Verify persistence across restart
            val contextRestart = LearningApplicationFactory.createPersisted(tempDir)
            val queryRestart = contextRestart.libraryQuery!!

            val pkgSummary = queryRestart.getPackageSummary(pkgId)
            assertNotNull(pkgSummary)
            assertEquals("ACTIVE", pkgSummary.state.name)

            val tree = queryRestart.getNavigationTree(defaultLibraryId)
            assertNotNull(tree)
            // Deleted collection should not appear as active in collection list
            assertTrue(tree.collections.none { it.collection.id == col.id })

        } finally {
            Files.walk(tempDir).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }

    @Test
    fun `failing transaction during persisted operation rolls back file changes cleanly`() {
        val tempDir = Files.createTempDirectory("library-command-rollback-test")

        try {
            val context = LearningApplicationFactory.createPersisted(tempDir)
            val libraryCommand = context.libraryCommand!!
            val defaultLibraryId = context.defaultLibraryId!!

            val packagesFile = tempDir.resolve("installed-packages.json")
            val pkgRecord = InstalledPackageRecord(
                id = "pkg-rollback-1",
                libraryId = defaultLibraryId.value,
                packageId = "package-rollback-1",
                topicId = "topic-rollback-1",
                name = "Rollback Package",
                version = "1.0.0",
                state = PackageState.ACTIVE.name,
                installedAt = Instant.now().toString(),
                contentCount = 5,
                learningItemCount = 10
            )
            JsonInstalledPackageStore(packagesFile).saveAll(listOf(pkgRecord))

            assertTrue(Files.exists(packagesFile))
            val initialContent = Files.readString(packagesFile)

            val transactionRunner = vn.loi.learning.infrastructure.transaction.JsonFileTransactionRunner(listOf(packagesFile))

            try {
                transactionRunner.runInTransaction {
                    Files.writeString(packagesFile, "[ { malformed json content } ]")
                    throw RuntimeException("Simulated IO failure")
                }
            } catch (expected: RuntimeException) {
                assertEquals("Simulated IO failure", expected.message)
            }

            val contentAfterRollback = Files.readString(packagesFile)
            assertEquals(initialContent, contentAfterRollback)

        } finally {
            Files.walk(tempDir).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }
}
