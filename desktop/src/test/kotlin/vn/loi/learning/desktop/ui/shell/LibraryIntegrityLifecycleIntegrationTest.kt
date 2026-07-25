package vn.loi.learning.desktop.ui.shell

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.UninstallContentPackageCommand
import vn.loi.learning.application.reviewhistory.ReviewHistoryQuery
import vn.loi.learning.application.session.StartPackageLessonStudyRequest
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.library.LibraryViewModel
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.infrastructure.LearningApplicationFactory

class LibraryIntegrityLifecycleIntegrationTest {

    @Test
    fun `Test 1 - Missing Catalog Entry corruption fixture enables full 9-boundary reconciliation and re-import`() {
        val tempDir = Files.createTempDirectory("scen-1-temp")
        val persistenceDir = Files.createTempDirectory("scen-1-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )

            // 1. Import Topic A
            contentLibVm.importFromFiles(listOf(fileA))
            val catalogId = PackageCatalogId("desktop-content-library")
            val navTree1 = appContext.libraryQuery?.getNavigationTree(appContext.defaultLibraryId!!)
            assertNotNull(navTree1)
            val pkgId = navTree1.installedPackages.first().packageId

            // 2. Corrupt state: Manually delete PackageId from PackageCatalogRepository while keeping other 8 boundaries
            val catalogRepo = appContext.packageCatalog
            assertNotNull(catalogRepo)
            val catalog = catalogRepo.findById(catalogId)
            assertNotNull(catalog)
            val corruptedCatalog = catalog.remove(pkgId)
            catalogRepo.save(corruptedCatalog)
            assertFalse(catalogRepo.findById(catalogId)!!.contains(pkgId), "Catalog record must be deleted to simulate catalog corruption")

            val contentPkgRepo = appContext.contentPackageRepository
            assertNotNull(contentPkgRepo)
            assertNotNull(contentPkgRepo.findById(pkgId), "ContentPackage must still exist in repo")
            assertNotNull(appContext.installedPackageRepository?.findAll()?.firstOrNull { it.packageId == pkgId }, "InstalledPackage must still exist")

            // 3. Confirm duplicate import fails with conflict error while catalog entry is missing
            contentLibVm.importFromFiles(listOf(fileA))
            val importError = contentLibVm.uiState.importError
            assertNotNull(importError, "Duplicate import must fail even when catalog entry is missing")

            // 4. Execute uninstall operation -> must reconcile ALL 9 boundaries despite missing catalog entry
            val uninstallOp = appContext.uninstallContentPackage
            assertNotNull(uninstallOp)
            uninstallOp.execute(UninstallContentPackageCommand(catalogId, pkgId))

            // 5. Assert 9-boundary reconciliation is complete
            assertNull(contentPkgRepo.findById(pkgId), "ContentPackage must be deleted")
            assertTrue(appContext.installedPackageRepository?.findAll()?.none { it.packageId == pkgId } == true, "InstalledPackage must be deleted")
            assertTrue(appContext.domainLibraryRepository?.findById(appContext.defaultLibraryId!!)?.entries?.none { it.packageId == pkgId } == true, "Canonical Library entry must be deleted")
            assertTrue(appContext.contentLibraryRepository?.findAll()?.isEmpty() == true, "ContentLibrary must be deleted")
            assertTrue(appContext.contentRepository?.findAll()?.isEmpty() == true, "Content must be deleted")
            assertTrue(appContext.learningItemRepository?.findByContentId(vn.loi.learning.domain.content.model.ContentId("cnt-a-1"))?.isEmpty() == true, "LearningItem must be deleted")

            // 6. Re-import Topic A successfully
            contentLibVm.importFromFiles(listOf(fileA))
            assertNull(contentLibVm.uiState.importError, "Re-import after catalog-corrupted uninstall must succeed cleanly")
            assertEquals(1, contentLibVm.uiState.packages.size, "Package count must be 1 after successful re-import")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 2 - Missing Library Membership corruption fixture is surfaced by LibraryQueryService`() {
        val tempDir = Files.createTempDirectory("scen-2-temp")
        val persistenceDir = Files.createTempDirectory("scen-2-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )

            // 1. Import Topic A
            contentLibVm.importFromFiles(listOf(fileA))
            val defaultLibId = appContext.defaultLibraryId!!
            val navTree1 = appContext.libraryQuery?.getNavigationTree(defaultLibId)
            assertNotNull(navTree1)
            val instPkgId = navTree1.installedPackages.first().id

            // 2. Corrupt state: Unregister entry from canonical Library aggregate using reconstitute, leaving InstalledPackage record intact
            val libRepo = appContext.domainLibraryRepository
            assertNotNull(libRepo)
            val library = libRepo.findById(defaultLibId)
            assertNotNull(library)

            val corruptedEntries = library.entries.filterNot { it.installedPackageId == instPkgId }
            val corruptedLibrary = Library.reconstitute(
                id = library.id,
                name = library.name,
                entries = corruptedEntries,
                activePackageId = if (library.activePackageId == instPkgId) null else library.activePackageId,
                createdAt = library.createdAt
            )
            libRepo.save(corruptedLibrary)
            assertFalse(libRepo.findById(defaultLibId)!!.hasPackage(instPkgId), "Library aggregate entries must not contain unregistered package entry")

            // 3. Query LibraryNavigationTree via LibraryQueryService -> must surface installed package so it is NOT invisible
            val navTree2 = appContext.libraryQuery?.getNavigationTree(defaultLibId)
            assertNotNull(navTree2)
            assertTrue(
                navTree2.installedPackages.any { it.id == instPkgId },
                "LibraryQueryService MUST surface InstalledPackage record in navigation tree even if canonical Library.entries entry is missing"
            )

            // 4. Uninstall package and re-import
            val pkgId = navTree1.installedPackages.first().packageId
            assertNotNull(appContext.uninstallContentPackage)
            appContext.uninstallContentPackage!!.execute(UninstallContentPackageCommand(PackageCatalogId("desktop-content-library"), pkgId))
            contentLibVm.importFromFiles(listOf(fileA))
            assertNull(contentLibVm.uiState.importError, "Re-import after uninstall must succeed")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 3 - Duplicate import message of ARCHIVED topic contains topic name, ARCHIVED state, Library, and Restore or Remove`() {
        val tempDir = Files.createTempDirectory("scen-3-temp")
        val persistenceDir = Files.createTempDirectory("scen-3-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )

            // 1. Import Topic A
            contentLibVm.importFromFiles(listOf(fileA))
            val defaultLibId = appContext.defaultLibraryId!!
            val navTree1 = appContext.libraryQuery?.getNavigationTree(defaultLibId)
            assertNotNull(navTree1)
            val instPkgId = navTree1.installedPackages.first().id

            // 2. Archive Topic A
            assertNotNull(appContext.libraryCommand)
            appContext.libraryCommand!!.archivePackage(defaultLibId, instPkgId)
            val navTreeArchived = appContext.libraryQuery?.getNavigationTree(defaultLibId)
            assertNotNull(navTreeArchived)
            assertTrue(navTreeArchived.archivedPackages.any { it.id == instPkgId })

            // 3. Attempt duplicate import of Topic A
            contentLibVm.importFromFiles(listOf(fileA))
            val importError = contentLibVm.uiState.importError
            assertNotNull(importError, "Import error must be set")

            // 4. Strict assertions for actionable message requirements
            assertTrue(importError.contains("Topic A"), "Duplicate error message MUST contain topic name 'Topic A', but was: $importError")
            assertTrue(importError.contains("ARCHIVED"), "Duplicate error message MUST mention 'ARCHIVED' state, but was: $importError")
            assertTrue(importError.contains("Library"), "Duplicate error message MUST mention 'Library', but was: $importError")
            assertTrue(
                importError.contains("Restore") || importError.contains("Remove"),
                "Duplicate error message MUST offer 'Restore' or 'Remove' action, but was: $importError"
            )
            assertFalse(importError == "CONTENT_ID_ALREADY_INSTALLED", "Duplicate error message MUST NOT be a raw code string")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 4 - Shared library and shared content safety ensures package uninstall does not delete shared data`() {
        val persistenceDir = Files.createTempDirectory("scen-4-db")
        try {
            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val sharedLibId = ContentLibraryId("shared-lib-1")
            val sharedContentId = ContentId("shared-cnt-1")
            val sharedItemId = LearningItemId("shared-item-1")

            val pkgIdA = PackageId("package-a")
            val pkgIdB = PackageId("package-b")

            // Register shared ContentLibrary, Content, and LearningItem
            val sharedLibrary = ContentLibrary(sharedLibId, vn.loi.learning.domain.content.library.model.LibraryDescriptor(name = "Shared Library"), setOf(sharedContentId))
            val sharedContent = Content(
                id = sharedContentId,
                type = ContentType.SENTENCE,
                text = ContentText(primaryText = "Hello", translatedText = "Chao"),
                metadata = ContentMetadata(title = "Greeting", group = "Eng", section = "Sec1", lesson = "Les1")
            )
            val sharedItem = LearningItem(sharedItemId, sharedContentId, LearningMode.MEANING_RECOGNITION, true)

            assertNotNull(appContext.contentLibraryRepository)
            assertNotNull(appContext.contentRepository)
            assertNotNull(appContext.learningItemRepository)
            assertNotNull(appContext.contentPackageRepository)

            appContext.contentLibraryRepository!!.save(sharedLibrary)
            appContext.contentRepository!!.saveAll(listOf(sharedContent))
            appContext.learningItemRepository!!.saveAll(listOf(sharedItem))

            // Register Package A and Package B both referencing sharedLibId
            val pkgA = ContentPackage(pkgIdA, PackageDescriptor(name = "Package A", version = "1.0.0", format = "OPD3"), setOf(sharedLibId))
            val pkgB = ContentPackage(pkgIdB, PackageDescriptor(name = "Package B", version = "1.0.0", format = "OPD3"), setOf(sharedLibId))

            appContext.contentPackageRepository!!.save(pkgA)
            appContext.contentPackageRepository!!.save(pkgB)

            // Execute uninstall of Package A
            assertNotNull(appContext.uninstallContentPackage)
            appContext.uninstallContentPackage!!.execute(UninstallContentPackageCommand(PackageCatalogId("desktop-content-library"), pkgIdA))

            // Assert Package A deleted, but Package B, shared library, content, and learning items remain intact
            assertNull(appContext.contentPackageRepository!!.findById(pkgIdA), "Package A must be deleted")
            assertNotNull(appContext.contentPackageRepository!!.findById(pkgIdB), "Package B must NOT be deleted")
            assertNotNull(appContext.contentLibraryRepository!!.findById(sharedLibId), "Shared library MUST NOT be deleted when owned by another package")
            assertNotNull(appContext.contentRepository!!.findById(sharedContentId), "Shared content MUST NOT be deleted when owned by another package")
            assertNotNull(appContext.learningItemRepository!!.findById(sharedItemId), "Shared learning item MUST NOT be deleted when owned by another package")
        } finally {
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 5 - MemoryState and ReviewEvent history persistence and reconnection on re-import`() {
        val tempDir = Files.createTempDirectory("scen-5-temp")
        val persistenceDir = Files.createTempDirectory("scen-5-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )

            // 1. Import Topic A
            contentLibVm.importFromFiles(listOf(fileA))
            val defaultLibId = appContext.defaultLibraryId!!
            val navTree1 = appContext.libraryQuery?.getNavigationTree(defaultLibId)
            assertNotNull(navTree1)
            val instPkgSummary = navTree1.installedPackages.first()
            val instPkgId = instPkgSummary.id
            val pkgId = instPkgSummary.packageId

            // 2. Perform study review to establish MemoryState and ReviewEvent history
            val studyFacade = StudyFacade(appContext)
            studyFacade.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = instPkgId,
                    contentId = ContentId("cnt-a-1")
                )
            )
            studyFacade.revealAnswer()
            studyFacade.review(ReviewRating.GOOD)

            val learnerId = LearnerId("default-learner")
            val reviewEventsBefore = appContext.reviewHistory.query(ReviewHistoryQuery(learnerId = learnerId))
            assertTrue(reviewEventsBefore.isNotEmpty(), "Review history must exist before uninstall")

            assertNotNull(appContext.memoryStateRepository)
            val memoryStateBefore = appContext.memoryStateRepository!!.find(learnerId, LearningItemId("cnt-a-1-rec"))
            assertNotNull(memoryStateBefore, "MemoryState must exist before uninstall")

            // 3. Uninstall Topic A
            assertNotNull(appContext.uninstallContentPackage)
            appContext.uninstallContentPackage!!.execute(UninstallContentPackageCommand(PackageCatalogId("desktop-content-library"), pkgId))

            // 4. Assert MemoryState and ReviewEvent history PERSIST after uninstall
            val reviewEventsAfter = appContext.reviewHistory.query(ReviewHistoryQuery(learnerId = learnerId))
            assertEquals(reviewEventsBefore.size, reviewEventsAfter.size, "ReviewEvent history MUST NOT be deleted upon package uninstall")

            val memoryStateAfter = appContext.memoryStateRepository!!.find(learnerId, LearningItemId("cnt-a-1-rec"))
            assertNotNull(memoryStateAfter, "MemoryState MUST NOT be deleted upon package uninstall")
            assertEquals(memoryStateBefore.stage, memoryStateAfter.stage, "MemoryState stage must remain unchanged")
            assertEquals(memoryStateBefore.difficulty, memoryStateAfter.difficulty, "MemoryState difficulty must remain unchanged")
            assertEquals(memoryStateBefore.stabilityDays, memoryStateAfter.stabilityDays, "MemoryState stabilityDays must remain unchanged")
            assertEquals(memoryStateBefore.dueAt, memoryStateAfter.dueAt, "MemoryState dueAt must remain unchanged")
            assertEquals(memoryStateBefore.lastReviewedAt, memoryStateAfter.lastReviewedAt, "MemoryState lastReviewedAt must remain unchanged")
            assertEquals(memoryStateBefore.reviewCount, memoryStateAfter.reviewCount, "MemoryState reviewCount must remain unchanged")
            assertEquals(memoryStateBefore.lapseCount, memoryStateAfter.lapseCount, "MemoryState lapseCount must remain unchanged")

            // 5. Re-import Topic A -> assert MemoryState reconnects seamlessly
            contentLibVm.importFromFiles(listOf(fileA))
            assertNull(contentLibVm.uiState.importError, "Re-import must succeed")

            val reconnectedMemoryState = appContext.memoryStateRepository!!.find(learnerId, LearningItemId("cnt-a-1-rec"))
            assertNotNull(reconnectedMemoryState, "Re-imported learning item must reconnect to persisted MemoryState")
            assertEquals(memoryStateBefore, reconnectedMemoryState, "Reconnected MemoryState must match exact original state")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 6 - Remove Topic clears active package reference and closes opened Lesson Browser and Workspace`() {
        val tempDir = Files.createTempDirectory("scen-6-temp")
        val persistenceDir = Files.createTempDirectory("scen-6-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)!!
            val studyViewModel = StudyViewModel(
                facade = StudyFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            val libraryViewModel = LibraryViewModel(
                facade = libraryFacade,
                taskRunner = ImmediateDesktopTaskRunner,
                onLibraryDataChanged = { studyViewModel.refresh() }
            )
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() },
                taskRunner = ImmediateDesktopTaskRunner
            )

            // 1. Import Topic A and set active package
            contentLibVm.importFromFiles(listOf(fileA))
            val defaultLibId = appContext.defaultLibraryId!!
            val navTree1 = libraryFacade.loadNavigationTree()!!
            val instPkgId = navTree1.installedPackages.first().id
            val pkgId = navTree1.installedPackages.first().packageId

            libraryViewModel.setActivePackage(instPkgId)
            studyViewModel.refresh()
            assertEquals(instPkgId.value, studyViewModel.uiState.activeInstalledPackageId?.value)

            // 2. Open Lesson Browser for Topic A
            contentLibVm.browsePackageLessons(instPkgId, "Topic A")
            assertNotNull(contentLibVm.lessonBrowserUiState, "Lesson Browser must be open")

            // 3. Uninstall Topic A via ContentLibraryViewModel
            val uninstalled = contentLibVm.uninstallPackage(pkgId.value, "Topic A")
            assertTrue(uninstalled, "Package uninstall via ContentLibraryViewModel must return true")
            studyViewModel.refresh()

            // 4. Assert active package reference is sanitized (cleared)
            val navTreeAfter = libraryFacade.loadNavigationTree()!!
            assertNull(navTreeAfter.activePackageId, "Active package reference in navigation tree MUST be sanitized to null")
            assertNull(studyViewModel.uiState.activeInstalledPackageId, "StudyViewModel active package ID MUST be cleared when package is removed")

            // 5. Assert opened Lesson Browser and Learning Workspace are closed (null)
            assertNull(contentLibVm.lessonBrowserUiState, "Lesson Browser MUST be closed when package is uninstalled")
            assertNull(contentLibVm.learningWorkspaceUiState, "Learning Workspace MUST be closed when package is uninstalled")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 7 - Failure-injection during package uninstall rolls back all mutated boundaries`() {
        val tempDir = Files.createTempDirectory("scen-7-temp")
        val persistenceDir = Files.createTempDirectory("scen-7-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )

            // 1. Import Topic A
            contentLibVm.importFromFiles(listOf(fileA))
            val catalogId = PackageCatalogId("desktop-content-library")
            val defaultLibId = appContext.defaultLibraryId!!
            val navTree1 = appContext.libraryQuery?.getNavigationTree(defaultLibId)
            assertNotNull(navTree1)
            val instPkgId = navTree1!!.installedPackages.first().id
            val pkgId = navTree1.installedPackages.first().packageId

            // 2. Perform study review (MemoryState & ReviewEvent) & attach package to Collection
            val studyFacade = StudyFacade(appContext)
            studyFacade.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = instPkgId,
                    contentId = ContentId("cnt-a-1")
                )
            )
            studyFacade.revealAnswer()
            studyFacade.review(ReviewRating.GOOD)

            val learnerId = LearnerId("default-learner")
            assertNotNull(appContext.memoryStateRepository!!.find(learnerId, LearningItemId("cnt-a-1-rec")))

            assertNotNull(appContext.libraryCommand)
            val createColRes = appContext.libraryCommand!!.createCollection(defaultLibId, vn.loi.learning.domain.library.model.CollectionName("Test Collection"))
            assertTrue(createColRes is vn.loi.learning.application.library.command.LibraryCommandResult.Success)
            val colId = (createColRes as vn.loi.learning.application.library.command.LibraryCommandResult.Success).value.id
            appContext.libraryCommand!!.assignPackageToCollection(defaultLibId, colId, instPkgId)

            // 3. Construct faulty operation that throws during deletion
            val faultyUninstallOp = vn.loi.learning.application.contentpackaging.PackageUninstallOperation(
                contentLibraryRepository = appContext.contentLibraryRepository!!,
                contentRepository = appContext.contentRepository!!,
                learningItemRepository = appContext.learningItemRepository!!,
                contentPackageRepository = object : vn.loi.learning.application.port.ContentPackageRepository by appContext.contentPackageRepository!! {
                    override fun deleteById(packageId: PackageId) {
                        throw RuntimeException("Simulated repository failure during deletion")
                    }
                },
                packageCatalogRepository = appContext.packageCatalog!!,
                installedPackageRepository = appContext.installedPackageRepository!!,
                libraryRepository = appContext.domainLibraryRepository!!
            )

            val faultyUseCase = vn.loi.learning.application.contentpackaging.UninstallContentPackageUseCase(
                uninstallOperation = faultyUninstallOp,
                transactionRunner = vn.loi.learning.infrastructure.transaction.JsonFileTransactionRunner(
                    listOf(
                        persistenceDir.resolve("installed-packages.json"),
                        persistenceDir.resolve("canonical-libraries.json"),
                        persistenceDir.resolve("canonical-library-collections.json"),
                        persistenceDir.resolve("content-libraries.json"),
                        persistenceDir.resolve("library-collections.json"),
                        persistenceDir.resolve("contents.json"),
                        persistenceDir.resolve("learning-items.json"),
                        persistenceDir.resolve("memory-states.json"),
                        persistenceDir.resolve("review-events.json"),
                        persistenceDir.resolve("study-sessions.json"),
                        persistenceDir.resolve("study-queues.json"),
                        persistenceDir.resolve("content-packages.json"),
                        persistenceDir.resolve("package-catalogs.json")
                    )
                )
            )

            // 4. Execute faulty uninstall and verify exception is thrown
            var exceptionThrown = false
            try {
                faultyUseCase.execute(UninstallContentPackageCommand(catalogId, pkgId))
            } catch (ex: Exception) {
                exceptionThrown = true
                assertTrue(ex.message?.contains("Simulated repository failure") == true)
            }
            assertTrue(exceptionThrown, "Exception must be thrown by faulty operation")

            // 5. Assert ALL 9 boundaries + Collection + MemoryState + ReviewEvent are restored intact after rollback
            val reloadedContext = LearningApplicationFactory.createPersisted(persistenceDir)
            assertNotNull(reloadedContext.contentPackageRepository?.findById(pkgId), "1. ContentPackage must exist after transaction rollback")
            assertTrue(reloadedContext.packageCatalog?.findById(catalogId)?.contains(pkgId) == true, "2. PackageCatalog must contain package after transaction rollback")
            assertNotNull(reloadedContext.installedPackageRepository?.findAll()?.firstOrNull { it.packageId == pkgId }, "3. InstalledPackage must exist after transaction rollback")
            assertTrue(reloadedContext.domainLibraryRepository?.findById(defaultLibId)?.hasPackage(instPkgId) == true, "4. Canonical Library entry must exist after transaction rollback")
            assertTrue(reloadedContext.contentLibraryRepository?.findAll()?.isNotEmpty() == true, "5. ContentLibrary must exist after transaction rollback")
            assertNotNull(reloadedContext.contentRepository?.findById(ContentId("cnt-a-1")), "6. Content must exist after transaction rollback")
            assertTrue(reloadedContext.learningItemRepository?.findByContentId(ContentId("cnt-a-1"))?.isNotEmpty() == true, "7. LearningItem must exist after transaction rollback")

            val reloadedNavTree = reloadedContext.libraryQuery?.getNavigationTree(defaultLibId)
            assertNotNull(reloadedNavTree)
            val reloadedCol = reloadedNavTree.collections.firstOrNull()
            assertNotNull(reloadedCol, "8. Collection must exist after transaction rollback")
            assertTrue(reloadedCol.assignedPackages.any { it.packageId == pkgId }, "Collection package assignment must be preserved after transaction rollback")

            assertNotNull(reloadedContext.memoryStateRepository?.find(learnerId, LearningItemId("cnt-a-1-rec")), "9. MemoryState must exist after transaction rollback")
            assertTrue(reloadedContext.reviewHistory.query(vn.loi.learning.application.reviewhistory.ReviewHistoryQuery(learnerId = learnerId)).isNotEmpty(), "10. ReviewHistory must exist after transaction rollback")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 8 - Canonical LibraryId sharing raw value string with ContentLibraryId does not cause invalid deletion or preservation`() {
        val persistenceDir = Files.createTempDirectory("scen-8-db")
        try {
            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val sharedRawId = "default-library" // Matches defaultLibraryId.value ("default-library")

            val contentLibId = ContentLibraryId(sharedRawId)
            val contentId = ContentId("cnt-8-1")
            val itemId = LearningItemId("cnt-8-1-rec")
            val pkgId = PackageId("package-8")

            // Create a ContentLibrary whose ID string happens to be "default-library"
            val contentLibrary = ContentLibrary(contentLibId, vn.loi.learning.domain.content.library.model.LibraryDescriptor("Matched ID Library"), setOf(contentId))
            val content = Content(contentId, ContentType.SENTENCE, ContentText("Hi", "Xin chao"), metadata = ContentMetadata("Hi", "Eng", "Sec1", "Les1"))
            val item = LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION, true)
            val contentPkg = ContentPackage(pkgId, PackageDescriptor("Package 8", "1.0.0", "OPD3"), setOf(contentLibId))

            appContext.contentLibraryRepository!!.save(contentLibrary)
            appContext.contentRepository!!.saveAll(listOf(content))
            appContext.learningItemRepository!!.saveAll(listOf(item))
            appContext.contentPackageRepository!!.save(contentPkg)

            // Uninstall Package 8
            assertNotNull(appContext.uninstallContentPackage)
            appContext.uninstallContentPackage!!.execute(UninstallContentPackageCommand(PackageCatalogId("desktop-content-library"), pkgId))

            // ContentPackage 8 is deleted, and its ContentLibrary "default-library" is properly deleted (since Package 8 owned it)
            // But canonical Library aggregate "default-library" is preserved intact!
            assertNull(appContext.contentPackageRepository!!.findById(pkgId), "Package 8 must be deleted")
            assertNull(appContext.contentLibraryRepository!!.findById(contentLibId), "ContentLibrary default-library owned by package 8 must be deleted")
            assertNotNull(appContext.domainLibraryRepository!!.findById(vn.loi.learning.domain.library.model.LibraryId("default-library")), "Canonical Library default-library MUST NOT be affected or deleted")
        } finally {
            persistenceDir.toFile().deleteRecursively()
        }
    }

    private fun createOpd3ZipPackage(file: Path, name: String, contentId: String) {
        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            writeZipEntry(
                zip,
                "manifest.json",
                """{ "name": "$name", "version": "1.0.0", "format": "OPD3", "schemaVersion": 1, "contentCount": 1, "learningItemCount": 1 }"""
            )
            writeZipEntry(zip, "metadata.json", """{ "name": "$name", "version": "1.0.0", "format": "OPD3" }""")
            writeZipEntry(
                zip,
                "contents.json",
                """{ "contents": [ { "id": "$contentId", "type": "SENTENCE", "primaryText": "Greeting", "translatedText": "Chao", "title": "Greeting", "group": "English", "section": "Unit 1", "lesson": "$name Greetings" } ] }"""
            )
            writeZipEntry(
                zip,
                "learning-items.json",
                """{ "learningItems": [ { "id": "$contentId-rec", "contentId": "$contentId", "mode": "MEANING_RECOGNITION", "isEnabled": true } ] }"""
            )
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }
}
