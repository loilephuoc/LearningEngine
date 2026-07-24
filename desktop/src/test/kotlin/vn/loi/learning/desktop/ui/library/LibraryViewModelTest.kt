package vn.loi.learning.desktop.ui.library

import java.io.File
import java.io.IOException
import java.sql.SQLException
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
import vn.loi.learning.application.library.query.LibraryQueryService
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
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
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.persistence.memory.InMemoryCollectionRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class LibraryViewModelTest {

    private val libId = LibraryId("lib-test")
    private val instId1 = InstalledPackageId("pkg-1")
    private val instId2 = InstalledPackageId("pkg-2")
    private val colId1 = CollectionId("col-1")
    private val colId2 = CollectionId("col-2")

    private fun createStandardContext(
        libRepo: InMemoryLibraryRepository = InMemoryLibraryRepository(),
        pkgRepo: InMemoryInstalledPackageRepository = InMemoryInstalledPackageRepository(),
        colRepo: InMemoryCollectionRepository = InMemoryCollectionRepository(),
        txRunner: InMemoryTransactionRunner = InMemoryTransactionRunner()
    ): Pair<LibraryFacade, LibraryViewModel> {
        val lib = Library.reconstitute(id = libId, name = "Test Library")
        libRepo.save(lib)

        val queryService = LibraryQueryService(libRepo, pkgRepo, colRepo)
        val commandService = LibraryCommandService(libRepo, pkgRepo, colRepo, txRunner)

        val facade = LibraryFacade(
            queryService = queryService,
            commandService = commandService,
            libraryId = libId
        )
        val viewModel = LibraryViewModel(facade = facade, taskRunner = ImmediateDesktopTaskRunner)
        return facade to viewModel
    }

    private fun seedActivePackage(pkgRepo: InMemoryInstalledPackageRepository, libRepo: InMemoryLibraryRepository, id: String = "pkg-1", name: String = "Package 1"): InstalledPackage {
        val pkgId = InstalledPackageId(id)
        val pkg = InstalledPackage.reconstitute(
            id = pkgId,
            libraryId = libId,
            packageId = PackageId("package-$id"),
            topicId = TopicId("topic-$id"),
            name = PackageName(name),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 5,
            learningItemCount = 10
        )
        val currentLib = libRepo.findById(libId) ?: Library.reconstitute(id = libId, name = "Test Library")
        val newEntry = LibraryEntry(pkgId, PackageId("package-$id"))
        libRepo.save(
            Library.reconstitute(
                id = currentLib.id,
                name = currentLib.name,
                entries = currentLib.entries + newEntry,
                createdAt = currentLib.createdAt
            )
        )
        pkgRepo.save(pkg)
        return pkg
    }

    // 1. Create collection success
    @Test
    fun `1 create collection success executes command refreshes query and clears dialog`() {
        val (facade, viewModel) = createStandardContext()

        viewModel.openCreateCollectionDialog()
        assertIs<LibraryDialogState.CreateCollection>(viewModel.activeDialog)

        viewModel.submitCreateCollection(nameInput = "Grammar", descriptionInput = "Grammar lessons")

        assertIs<LibraryDialogState.None>(viewModel.activeDialog)
        assertFalse(viewModel.isBusy)
        val state = assertIs<LibraryUiState.Content>(viewModel.uiState)
        val created = state.collections.firstOrNull { it.collection.name == "Grammar" }
        assertNotNull(created)
        assertEquals("Grammar lessons", created.collection.description)
    }

    // 2. Create collection typed failure
    @Test
    fun `2 create collection duplicate name typed failure preserves old projection and shows deterministic message`() {
        val (facade, viewModel) = createStandardContext()

        viewModel.submitCreateCollection(nameInput = "Duplicate Col")
        val firstState = assertIs<LibraryUiState.Content>(viewModel.uiState)
        assertEquals(1, firstState.collections.size)

        // Submit second time with duplicate name
        viewModel.openCreateCollectionDialog()
        viewModel.submitCreateCollection(nameInput = "Duplicate Col")

        // Old projection remains intact, no fabricated collection
        val secondState = assertIs<LibraryUiState.Content>(viewModel.uiState)
        assertEquals(1, secondState.collections.size)
        assertFalse(viewModel.isBusy)

        val dialog = assertIs<LibraryDialogState.CreateCollection>(viewModel.activeDialog)
        assertEquals("Duplicate Col", dialog.nameInput)
        assertNotNull(dialog.errorMessage)
        assertTrue(dialog.errorMessage.contains("Duplicate Col"))
    }

    // 3. Rename collection success
    @Test
    fun `3 rename collection success updates refreshed name and preserves selection`() {
        val (facade, viewModel) = createStandardContext()

        viewModel.submitCreateCollection(nameInput = "Old Name")
        val state1 = assertIs<LibraryUiState.Content>(viewModel.uiState)
        val colId = state1.collections.first().collection.id

        viewModel.selectCollection(colId)
        assertEquals(colId, viewModel.selectedCollectionId)

        viewModel.openRenameCollectionDialog(colId, "Old Name")
        viewModel.submitRenameCollection(colId, "New Name")

        val state2 = assertIs<LibraryUiState.Content>(viewModel.uiState)
        assertEquals("New Name", state2.collections.first().collection.name)
        assertEquals(colId, viewModel.selectedCollectionId, "Selection by CollectionId must remain stable")
    }

    // 4. Delete collection success
    @Test
    fun `4 delete collection success removes collection from active navigation and reconciles selection`() {
        val (facade, viewModel) = createStandardContext()

        viewModel.submitCreateCollection(nameInput = "To Delete")
        val state1 = assertIs<LibraryUiState.Content>(viewModel.uiState)
        val colId = state1.collections.first().collection.id

        viewModel.selectCollection(colId)
        viewModel.openDeleteCollectionDialog(colId, "To Delete")
        viewModel.submitDeleteCollection(colId)

        val state2 = assertIs<LibraryUiState.Content>(viewModel.uiState)
        assertTrue(state2.collections.none { it.collection.id == colId })
        assertEquals(1, state2.deletedCollections.size)
        assertNull(viewModel.selectedCollectionId, "Selection must clear when selected collection is deleted")
    }

    // 5. Assign package success
    @Test
    fun `5 assign package success updates refreshed collection projection with assignment`() {
        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()

        val (facade, viewModel) = createStandardContext(libRepo, pkgRepo, colRepo)
        val pkg = seedActivePackage(pkgRepo, libRepo, "pkg-1", "Grammar Package")

        viewModel.refresh()
        viewModel.submitCreateCollection(nameInput = "Verbs")
        val state1 = assertIs<LibraryUiState.Content>(viewModel.uiState)
        val colId = state1.collections.first().collection.id

        viewModel.openAssignPackageDialog(colId, "Verbs")
        viewModel.submitAssignPackage(colId, pkg.id)

        val state2 = assertIs<LibraryUiState.Content>(viewModel.uiState)
        val colNode = state2.collections.first { it.collection.id == colId }
        assertEquals(1, colNode.assignedPackages.size)
        assertEquals("Grammar Package", colNode.assignedPackages.first().name)
    }

    // 6. Remove assignment success
    @Test
    fun `6 remove assignment success updates refreshed projection while keeping package installed`() {
        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()

        val (facade, viewModel) = createStandardContext(libRepo, pkgRepo, colRepo)
        val pkg = seedActivePackage(pkgRepo, libRepo, "pkg-1", "Grammar Package")

        viewModel.refresh()
        viewModel.submitCreateCollection(nameInput = "Verbs")
        val colId = (viewModel.uiState as LibraryUiState.Content).collections.first().collection.id
        viewModel.submitAssignPackage(colId, pkg.id)

        // Verify assignment present
        val state1 = assertIs<LibraryUiState.Content>(viewModel.uiState)
        assertEquals(1, state1.collections.first().assignedPackages.size)

        // Remove assignment
        viewModel.openRemoveAssignmentDialog(colId, "Verbs", pkg.id, pkg.name.value)
        viewModel.submitRemoveAssignment(colId, pkg.id)

        val state2 = assertIs<LibraryUiState.Content>(viewModel.uiState)
        assertEquals(0, state2.collections.first().assignedPackages.size)
        assertEquals(1, state2.activePackages.size, "Package must remain installed and active")
    }

    // 7. Archive package success
    @Test
    fun `7 archive package success moves package from active to archived section`() {
        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()

        val (facade, viewModel) = createStandardContext(libRepo, pkgRepo, colRepo)
        val pkg = seedActivePackage(pkgRepo, libRepo, "pkg-active-1", "Active Package")

        viewModel.refresh()
        val state1 = assertIs<LibraryUiState.Content>(viewModel.uiState)
        assertEquals(1, state1.activePackages.size)

        viewModel.openArchivePackageDialog(pkg.id, pkg.name.value)
        viewModel.submitArchivePackage(pkg.id)

        val state2 = assertIs<LibraryUiState.Content>(viewModel.uiState)
        assertEquals(0, state2.activePackages.size, "Package must leave active surface")
        assertEquals(1, state2.archivedPackages.size, "Package must appear in archived surface")
    }

    // 8. Restore package success
    @Test
    fun `8 restore package success returns archived package to active surface`() {
        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()

        val (facade, viewModel) = createStandardContext(libRepo, pkgRepo, colRepo)
        val pkg = seedActivePackage(pkgRepo, libRepo, "pkg-to-restore", "Archived Package")
        // Manually archive package in repo
        pkgRepo.save(pkg.archive().aggregate)

        viewModel.refresh()
        val state1 = assertIs<LibraryUiState.Content>(viewModel.uiState)
        assertEquals(1, state1.archivedPackages.size)
        assertEquals(0, state1.activePackages.size)

        viewModel.openRestorePackageDialog(pkg.id, pkg.name.value)
        viewModel.submitRestorePackage(pkg.id)

        val state2 = assertIs<LibraryUiState.Content>(viewModel.uiState)
        assertEquals(1, state2.activePackages.size, "Package must return to active surface")
        assertEquals(0, state2.archivedPackages.size)
    }

    // 9. Persistence failure
    @Test
    fun `9 persistence failure retains previous projection clears busy state and displays technical message`() {
        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()
        val failingTxRunner = object : vn.loi.learning.application.port.TransactionRunner {
            override fun <T> runInTransaction(block: () -> T): T {
                throw RuntimeException("Database write failed due to lock timeout")
            }
        }

        seedActivePackage(pkgRepo, libRepo, "pkg-9", "Package Nine")

        val queryService = LibraryQueryService(libRepo, pkgRepo, colRepo)
        val commandService = LibraryCommandService(libRepo, pkgRepo, colRepo, failingTxRunner)

        val facade = LibraryFacade(
            queryService = queryService,
            commandService = commandService,
            libraryId = libId
        )
        val viewModel = LibraryViewModel(facade = facade, taskRunner = ImmediateDesktopTaskRunner)

        val initialContent = assertIs<LibraryUiState.Content>(viewModel.uiState)

        viewModel.openCreateCollectionDialog()
        viewModel.submitCreateCollection(nameInput = "Failing Collection")

        // Previous projection intact
        val afterState = assertIs<LibraryUiState.Content>(viewModel.uiState)
        assertEquals(initialContent.collections, afterState.collections)
        assertFalse(viewModel.isBusy)

        // Feedback shows technical error message
        assertNotNull(viewModel.feedbackMessage)
        assertTrue(viewModel.feedbackMessage!!.contains("storage failure"))
    }

    // 10. Duplicate command submission prevention
    @Test
    fun `10 duplicate command submission while busy does not issue second mutation`() {
        var commandExecutionCount = 0
        val trackingRunner = object : vn.loi.learning.application.port.TransactionRunner {
            override fun <T> runInTransaction(block: () -> T): T {
                commandExecutionCount++
                return block()
            }
        }

        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()

        val lib = Library.reconstitute(id = libId, name = "Test Library")
        libRepo.save(lib)

        val queryService = LibraryQueryService(libRepo, pkgRepo, colRepo)
        val commandService = LibraryCommandService(libRepo, pkgRepo, colRepo, trackingRunner)

        val facade = LibraryFacade(
            queryService = queryService,
            commandService = commandService,
            libraryId = libId
        )

        // Use a task runner that doesn't execute inline immediately so we can test double submission while busy
        val manualTaskRunner = object : DesktopTaskRunner {
            private var pendingWork: (() -> Any?)? = null
            private var pendingSuccess: ((Any?) -> Unit)? = null

            override fun <T> run(work: () -> T, onSuccess: (T) -> Unit, onFailure: (Exception) -> Unit) {
                pendingWork = work
                @Suppress("UNCHECKED_CAST")
                pendingSuccess = onSuccess as (Any?) -> Unit
            }

            override fun dispatch(action: () -> Unit) {
                action()
            }

            fun flush() {
                val work = pendingWork
                val success = pendingSuccess
                pendingWork = null
                pendingSuccess = null
                if (work != null && success != null) {
                    val result = work()
                    success(result)
                }
            }
        }

        val viewModel = LibraryViewModel(facade = facade, taskRunner = manualTaskRunner)
        manualTaskRunner.flush()

        viewModel.openCreateCollectionDialog()
        viewModel.submitCreateCollection(nameInput = "Single Execution Col")

        assertTrue(viewModel.isBusy, "ViewModel must be busy while command is in progress")

        // Try double clicking / submitting second time while busy
        viewModel.submitCreateCollection(nameInput = "Single Execution Col")
        viewModel.submitCreateCollection(nameInput = "Single Execution Col")

        manualTaskRunner.flush()
        assertFalse(viewModel.isBusy)

        assertEquals(1, commandExecutionCount, "Command execution must occur exactly once despite multiple clicks")
    }

    // 11. Restart integration path through LearningApplicationFactory.createPersisted
    @Test
    fun `11 restart integration performs desktop commands and verifies persisted context query projection`() {
        val tempDir = java.nio.file.Files.createTempDirectory("desktop-restart-integ-test")
        try {
            // Context 1: Perform Desktop commands
            val context1 = LearningApplicationFactory.createPersisted(tempDir)
            val libId1 = context1.defaultLibraryId!!
            val facade1 = LibraryFacade(
                queryService = context1.libraryQuery,
                commandService = context1.libraryCommand,
                libraryId = libId1
            )
            val viewModel1 = LibraryViewModel(facade1, ImmediateDesktopTaskRunner)

            viewModel1.submitCreateCollection(nameInput = "Persistent Desktop Col", descriptionInput = "Created from Desktop UI")
            val content1 = assertIs<LibraryUiState.Content>(viewModel1.uiState)
            val colId = content1.collections.first().collection.id

            viewModel1.submitRenameCollection(colId, "Renamed Desktop Col")

            // Context 2: Recreate persisted context (Restart application)
            val context2 = LearningApplicationFactory.createPersisted(tempDir)
            val facade2 = LibraryFacade(
                queryService = context2.libraryQuery,
                commandService = context2.libraryCommand,
                libraryId = libId1
            )
            val viewModel2 = LibraryViewModel(facade2, ImmediateDesktopTaskRunner)

            val content2 = assertIs<LibraryUiState.Content>(viewModel2.uiState)
            assertEquals(1, content2.collections.size)
            assertEquals("Renamed Desktop Col", content2.collections.first().collection.name)
            assertEquals("Created from Desktop UI", content2.collections.first().collection.description)
        } finally {
            java.nio.file.Files.walk(tempDir).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(java.nio.file.Files::deleteIfExists)
            }
        }
    }

    // 12. Dependency guard
    @Test
    fun `12 dependency guard verifies desktop library imports no infrastructure or adapter classes and facade is free of infrastructure details`() {
        val rootDir = if (File("desktop").exists()) File(".") else File("..")
        val desktopLibraryDir = File(rootDir, "desktop/src/main/kotlin/vn/loi/learning/desktop/ui/library")
        assertTrue(desktopLibraryDir.exists(), "Desktop library directory must exist at ${desktopLibraryDir.absolutePath}")

        desktopLibraryDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            text.lines().forEach { line ->
                val trimmed = line.trim()
                assertFalse(
                    trimmed.startsWith("import vn.loi.learning.infrastructure"),
                    "File ${file.name} must not import infrastructure: $trimmed"
                )
                assertFalse(
                    trimmed.startsWith("import vn.loi.learning.adapter"),
                    "File ${file.name} must not import adapter: $trimmed"
                )
            }
        }

        val facadeFile = File(desktopLibraryDir, "LibraryFacade.kt")
        assertTrue(facadeFile.exists(), "LibraryFacade.kt must exist")
        val facadeText = facadeFile.readText()

        val forbiddenFacadeRegexes = listOf(
            "LearningApplicationContext",
            "LearningApplicationFactory",
            "\\binfrastructure\\b",
            "\\brepository\\b",
            "\\bstore\\b",
            "transaction runner"
        )
        forbiddenFacadeRegexes.forEach { pattern ->
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            assertFalse(
                regex.containsMatchIn(facadeText),
                "LibraryFacade.kt source must not contain forbidden mention matching regex: '$pattern'"
            )
        }

        val applicationDir = File(rootDir, "src/main/kotlin/vn/loi/learning/application")
        assertTrue(applicationDir.exists(), "Application directory must exist at ${applicationDir.absolutePath}")

        applicationDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            text.lines().forEach { line ->
                val trimmed = line.trim()
                assertFalse(
                    trimmed.startsWith("import vn.loi.learning.desktop"),
                    "Application file ${file.name} must not import desktop: $trimmed"
                )
                assertFalse(
                    trimmed.startsWith("import vn.loi.learning.infrastructure"),
                    "Application file ${file.name} must not import infrastructure: $trimmed"
                )
                assertFalse(
                    trimmed.startsWith("import vn.loi.learning.adapter"),
                    "Application file ${file.name} must not import adapter: $trimmed"
                )
            }
        }
    }

    // 13. Production composition wiring test
    @Test
    fun `13 production composition wiring creates LibraryFacade with explicit Application dependencies`() {
        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()

        val lib = Library.reconstitute(id = libId, name = "Composition Test Library")
        libRepo.save(lib)

        val queryService = LibraryQueryService(libRepo, pkgRepo, colRepo)
        val commandService = LibraryCommandService(libRepo, pkgRepo, colRepo, InMemoryTransactionRunner())

        // Create LibraryFacade using Application query and command dependencies explicitly
        val facade = LibraryFacade(
            queryService = queryService,
            commandService = commandService,
            libraryId = libId
        )

        val tree = facade.loadNavigationTree()
        assertEquals("Composition Test Library", tree.libraryName)
    }
}
