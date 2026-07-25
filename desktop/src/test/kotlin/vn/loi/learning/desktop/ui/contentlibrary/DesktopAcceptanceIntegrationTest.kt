package vn.loi.learning.desktop.ui.contentlibrary

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.library.LibraryViewModel
import vn.loi.learning.desktop.ui.shell.createCanonicalLibraryFacade
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.infrastructure.LearningApplicationFactory

class DesktopAcceptanceIntegrationTest {

    @Test
    fun `A Browse Lessons command starts exactly one load, shows observable loading state, ignores duplicate click, and loads clicked package`() {
        val tempDir = Files.createTempDirectory("browse-cmd-dir")
        val persistenceDir = Files.createTempDirectory("browse-cmd-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val importer = appContext.packageImporter(fileA)
            importer.importAllDetailed(vn.loi.learning.domain.content.packaging.model.PackageCatalogId("test-cat"))

            val pkgIdA = InstalledPackageId(appContext.installedPackages.query().first().id)

            var loadCount = 0
            val countingTaskRunner = object : DesktopTaskRunner {
                override fun <T> run(work: () -> T, onSuccess: (T) -> Unit, onFailure: (Exception) -> Unit) {
                    loadCount++
                    val result = work()
                    onSuccess(result)
                }
                override fun dispatch(action: () -> Unit) = action()
            }

            val viewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = countingTaskRunner
            )

            // 1. Initial state is Idle
            assertTrue(viewModel.uiState.operation is ContentLibraryOperation.Idle)

            val initialLoadCount = loadCount
            // 2. Trigger Browse Lessons for Package A
            viewModel.browsePackageLessons(pkgIdA, "Topic A")

            // 3. Assert load executed exactly once and resulted in Lesson Browser for Package A
            assertEquals(initialLoadCount + 1, loadCount, "Browse lessons must start exactly one load operation")
            assertNotNull(viewModel.lessonBrowserUiState)
            assertEquals("Topic A", viewModel.lessonBrowserUiState?.libraryName)
            assertEquals(pkgIdA, viewModel.lessonBrowserUiState?.installedPackageId)

            // 4. Test busy guard during loading with precise TaskRunner run() invocation counting and callback completion
            var runInvocationCount = 0
            val manualRunner = object : DesktopTaskRunner {
                var pendingWork: (() -> Any?)? = null
                var pendingOnSuccess: ((Any?) -> Unit)? = null
                var pendingOnFailure: ((Exception) -> Unit)? = null

                override fun <T> run(work: () -> T, onSuccess: (T) -> Unit, onFailure: (Exception) -> Unit) {
                    runInvocationCount++
                    pendingWork = work as (() -> Any?)
                    pendingOnSuccess = onSuccess as ((Any?) -> Unit)
                    pendingOnFailure = onFailure
                }
                override fun dispatch(action: () -> Unit) = action()
            }

            val busyVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = manualRunner
            )

            // Complete initial refresh from ViewModel init
            manualRunner.pendingWork?.let { work ->
                val res = work()
                manualRunner.pendingOnSuccess?.invoke(res)
            }

            val countBeforeBrowse = runInvocationCount

            // Start first Browse Lessons load
            busyVm.browsePackageLessons(pkgIdA, "Topic A")
            val countAfterFirstLoad = runInvocationCount
            assertEquals(countBeforeBrowse + 1, countAfterFirstLoad, "First browse command must invoke taskRunner.run()")
            assertTrue(busyVm.uiState.operation is ContentLibraryOperation.Loading, "Loading state must be observable while busy")

            // Attempt duplicate click while busy
            busyVm.browsePackageLessons(pkgIdA, "Topic A")
            assertEquals(countAfterFirstLoad, runInvocationCount, "Duplicate click while Loading must NOT invoke taskRunner.run() a second time")

            // Complete the pending Browse Lessons task properly via captured callbacks
            manualRunner.pendingWork?.let { work ->
                val result = work()
                manualRunner.pendingOnSuccess?.invoke(result)
            }

            // Verify operation transitions back to Idle after completion
            assertTrue(busyVm.uiState.operation is ContentLibraryOperation.Idle, "Operation must return to Idle after completion")
            assertNotNull(busyVm.lessonBrowserUiState, "Lesson Browser state must be loaded")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `B Successful import adds package exactly once, triggers refresh callback once, and preserves active package`() {
        val tempDir = Files.createTempDirectory("succ-import-dir")
        val persistenceDir = Files.createTempDirectory("succ-import-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)!!

            var callbackCount = 0
            val libraryViewModel = LibraryViewModel(
                facade = libraryFacade,
                taskRunner = ImmediateDesktopTaskRunner
            )

            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = {
                    callbackCount++
                    libraryViewModel.refresh()
                }
            )

            // 1. Import Topic A and set Topic A active
            contentLibVm.importFromFiles(listOf(fileA))
            val pkgIdA = libraryFacade.loadNavigationTree()!!.installedPackages.first { it.name == "Topic A" }.id
            libraryViewModel.setActivePackage(pkgIdA)
            assertEquals(1, callbackCount)

            // 2. Import Topic B
            contentLibVm.importFromFiles(listOf(fileB))

            // 3. Assert refresh callback occurred exactly once for Topic B import (total 2)
            assertEquals(2, callbackCount, "Refresh callback must occur exactly once per import")

            // 4. Assert package appears exactly once in catalog
            val tree = libraryFacade.loadNavigationTree()!!
            assertEquals(2, tree.installedPackages.size)
            val matchingBCount = tree.installedPackages.count { pkg -> pkg.name == "Topic B" }
            assertEquals(1, matchingBCount, "Imported package must appear exactly once in catalog")

            // 5. Assert active package is NOT replaced automatically by new import
            assertEquals(pkgIdA, tree.activePackageId, "Successful import must not replace active package unless explicitly requested")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `C Navigation flow Library to Lesson Browser to Workspace to Back to Lesson Browser to Library preserves package context`() {
        val tempDir = Files.createTempDirectory("nav-flow-dir")
        val persistenceDir = Files.createTempDirectory("nav-flow-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val importer = appContext.packageImporter(fileA)
            importer.importAllDetailed(vn.loi.learning.domain.content.packaging.model.PackageCatalogId("test-cat"))

            val pkgIdA = InstalledPackageId(appContext.installedPackages.query().first().id)

            val viewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            // 1. Library -> Browse Lessons
            viewModel.browsePackageLessons(pkgIdA, "Topic A")
            assertNotNull(viewModel.lessonBrowserUiState)
            assertEquals(pkgIdA, viewModel.lessonBrowserUiState?.installedPackageId)

            // 2. Lesson Browser -> Open Workspace
            val lessonSelection = PackageLessonSelection(pkgIdA, "cnt-a-1")
            viewModel.openWorkspaceForSelection(lessonSelection)
            assertNotNull(viewModel.learningWorkspaceUiState)
            assertEquals("cnt-a-1", viewModel.learningWorkspaceUiState?.contentId?.value)

            // 3. Workspace -> Back to Lesson Browser
            viewModel.closeWorkspace()
            assertNull(viewModel.learningWorkspaceUiState, "Workspace must be closed")
            assertNotNull(viewModel.lessonBrowserUiState, "Lesson Browser must remain active")
            assertEquals(pkgIdA, viewModel.lessonBrowserUiState?.installedPackageId, "Package context must remain intact")

            // 4. Lesson Browser -> Back to Library
            viewModel.closeLibrary()
            assertNull(viewModel.lessonBrowserUiState, "Lesson Browser must be closed")
            assertNull(viewModel.learningWorkspaceUiState, "Workspace must be closed")
        } finally {
            tempDir.toFile().deleteRecursively()
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
