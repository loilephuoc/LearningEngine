package vn.loi.learning.desktop.ui.contentlibrary

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
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class LearningWorkspaceExploreModeTest {

    private fun createTestEnvironment(tempDir: Path, persistenceDir: Path): Pair<LearningApplicationContext, ContentLibraryViewModel> {
        val fileA = tempDir.resolve("PackageA.opd3")
        createOpd3ZipPackage(fileA, name = "Package A", contentId = "cnt-lesson-1")

        val fileB = tempDir.resolve("PackageB.opd3")
        createOpd3ZipPackage(fileB, name = "Package B", contentId = "cnt-lesson-2")

        val appContext = LearningApplicationFactory.createPersisted(persistenceDir)

        val catId = PackageCatalogId("test-cat")
        appContext.packageImporter(fileA).importAllDetailed(catId)
        appContext.packageImporter(fileB).importAllDetailed(catId)

        val facade = ContentLibraryFacade(appContext)
        val lessonFacade = LessonBrowserFacade(appContext)

        val viewModel = ContentLibraryViewModel(
            facade = facade,
            lessonBrowserFacade = lessonFacade
        )

        return appContext to viewModel
    }

    private fun createOpd3ZipPackage(file: Path, name: String, contentId: String) {
        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            writeZipEntry(
                zip,
                "manifest.json",
                """{ "name": "$name", "version": "1.0.0", "format": "OPD3", "schemaVersion": 1, "contentCount": 1, "learningItemCount": 2 }"""
            )
            writeZipEntry(zip, "metadata.json", """{ "name": "$name", "version": "1.0.0", "format": "OPD3" }""")
            writeZipEntry(
                zip,
                "contents.json",
                """{ "contents": [ { "id": "$contentId", "type": "SENTENCE", "primaryText": "$name Primary", "translatedText": "$name Translation", "title": "Lesson 1", "group": "Group A", "section": "Section 1", "lesson": "Lesson 1" } ] }"""
            )
            writeZipEntry(
                zip,
                "learning-items.json",
                """{ "learningItems": [ { "id": "$contentId-item1", "contentId": "$contentId", "mode": "MEANING_RECALL", "isEnabled": true }, { "id": "$contentId-item2", "contentId": "$contentId", "mode": "DICTATION", "isEnabled": true } ] }"""
            )
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    @Test
    fun `1 Explore mode displays only items of the target Lesson`() {
        val tempDir = Files.createTempDirectory("explore-test-tmp-1")
        val persistenceDir = Files.createTempDirectory("explore-test-db-1")
        try {
            val (appContext, viewModel) = createTestEnvironment(tempDir, persistenceDir)
            val pkgId = InstalledPackageId(appContext.installedPackages.query().first { it.name == "Package A" }.id)
            viewModel.browsePackageLessons(pkgId, "Package A")

            val selection = PackageLessonSelection(
                installedPackageId = pkgId,
                lessonId = "cnt-lesson-1",
                packageName = "Package A",
                lessonTitle = "Lesson 1"
            )

            viewModel.openWorkspaceForSelection(selection)

            val workspace = viewModel.learningWorkspaceUiState
            assertNotNull(workspace)
            assertEquals(2, workspace!!.exploreItems.size)
            assertTrue(workspace.exploreItems.all { it.contentId == "cnt-lesson-1" })
            assertEquals(setOf("cnt-lesson-1-item1", "cnt-lesson-1-item2"), workspace.exploreItems.map { it.id }.toSet())
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `2 Explore mode excludes items from other Lessons or Packages`() {
        val tempDir = Files.createTempDirectory("explore-test-tmp-2")
        val persistenceDir = Files.createTempDirectory("explore-test-db-2")
        try {
            val (appContext, viewModel) = createTestEnvironment(tempDir, persistenceDir)
            val pkgId = InstalledPackageId(appContext.installedPackages.query().first { it.name == "Package A" }.id)
            viewModel.browsePackageLessons(pkgId, "Package A")

            val selection = PackageLessonSelection(
                installedPackageId = pkgId,
                lessonId = "cnt-lesson-1",
                packageName = "Package A",
                lessonTitle = "Lesson 1"
            )

            viewModel.openWorkspaceForSelection(selection)

            val workspace = viewModel.learningWorkspaceUiState
            assertNotNull(workspace)
            assertFalse(workspace!!.exploreItems.any { it.contentId == "cnt-lesson-2" })
            assertFalse(workspace.exploreItems.any { it.id == "cnt-lesson-2-item1" })
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `3 Opening Explore mode workspace creates zero StudySessions`() {
        val tempDir = Files.createTempDirectory("explore-test-tmp-3")
        val persistenceDir = Files.createTempDirectory("explore-test-db-3")
        try {
            val (appContext, viewModel) = createTestEnvironment(tempDir, persistenceDir)
            val pkgId = InstalledPackageId(appContext.installedPackages.query().first { it.name == "Package A" }.id)
            viewModel.browsePackageLessons(pkgId, "Package A")

            val selection = PackageLessonSelection(
                installedPackageId = pkgId,
                lessonId = "cnt-lesson-1",
                packageName = "Package A",
                lessonTitle = "Lesson 1"
            )

            viewModel.openWorkspaceForSelection(selection)

            val activeSession = appContext.engine.getActiveSession(LearnerId("default-learner"))
            assertNull(activeSession)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `4 Prepare mode functionality remains fully active alongside explore mode`() {
        val tempDir = Files.createTempDirectory("explore-test-tmp-4")
        val persistenceDir = Files.createTempDirectory("explore-test-db-4")
        try {
            val (appContext, viewModel) = createTestEnvironment(tempDir, persistenceDir)
            val pkgId = InstalledPackageId(appContext.installedPackages.query().first { it.name == "Package A" }.id)
            viewModel.browsePackageLessons(pkgId, "Package A")

            val selection = PackageLessonSelection(
                installedPackageId = pkgId,
                lessonId = "cnt-lesson-1",
                packageName = "Package A",
                lessonTitle = "Lesson 1"
            )

            viewModel.openWorkspaceForSelection(selection)

            val workspace = viewModel.learningWorkspaceUiState
            assertNotNull(workspace)
            assertTrue(workspace!!.canStart)
            assertEquals(LessonStudyActionType.START, workspace.action.type)
            assertEquals(3, workspace.previewStages.size)
            assertEquals(2, workspace.totalItemCount)
            assertNull(workspace.unavailableReason)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `5 Start Study from workspace reuses original PackageLessonSelection contract`() {
        val tempDir = Files.createTempDirectory("explore-test-tmp-5")
        val persistenceDir = Files.createTempDirectory("explore-test-db-5")
        try {
            val (appContext, viewModel) = createTestEnvironment(tempDir, persistenceDir)
            val pkgId = InstalledPackageId(appContext.installedPackages.query().first { it.name == "Package A" }.id)
            viewModel.browsePackageLessons(pkgId, "Package A")

            val selection = PackageLessonSelection(
                installedPackageId = pkgId,
                lessonId = "cnt-lesson-1",
                packageName = "Package A",
                lessonTitle = "Lesson 1"
            )

            viewModel.openWorkspaceForSelection(selection)
            viewModel.navigateToPrepareMode()

            var receivedSelection: PackageLessonSelection? = null
            viewModel.startStudyFromWorkspace { targetSelection ->
                receivedSelection = targetSelection
            }

            assertNotNull(receivedSelection)
            assertEquals(pkgId, receivedSelection!!.installedPackageId)
            assertEquals("cnt-lesson-1", receivedSelection!!.lessonId)
            assertEquals("Lesson 1", receivedSelection!!.lessonTitle)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `6 Workspace opens in default EXPLORE mode`() {
        val tempDir = Files.createTempDirectory("explore-test-tmp-6")
        val persistenceDir = Files.createTempDirectory("explore-test-db-6")
        try {
            val (appContext, viewModel) = createTestEnvironment(tempDir, persistenceDir)
            val pkgId = InstalledPackageId(appContext.installedPackages.query().first { it.name == "Package A" }.id)
            viewModel.browsePackageLessons(pkgId, "Package A")

            val selection = PackageLessonSelection(
                installedPackageId = pkgId,
                lessonId = "cnt-lesson-1",
                packageName = "Package A",
                lessonTitle = "Lesson 1"
            )

            viewModel.openWorkspaceForSelection(selection)

            val workspace = viewModel.learningWorkspaceUiState
            assertNotNull(workspace)
            assertEquals(WorkspaceMode.EXPLORE, workspace!!.mode)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `7 Mode transition from EXPLORE to PREPARE`() {
        val tempDir = Files.createTempDirectory("explore-test-tmp-7")
        val persistenceDir = Files.createTempDirectory("explore-test-db-7")
        try {
            val (appContext, viewModel) = createTestEnvironment(tempDir, persistenceDir)
            val pkgId = InstalledPackageId(appContext.installedPackages.query().first { it.name == "Package A" }.id)
            viewModel.browsePackageLessons(pkgId, "Package A")

            val selection = PackageLessonSelection(
                installedPackageId = pkgId,
                lessonId = "cnt-lesson-1",
                packageName = "Package A",
                lessonTitle = "Lesson 1"
            )

            viewModel.openWorkspaceForSelection(selection)
            assertEquals(WorkspaceMode.EXPLORE, viewModel.learningWorkspaceUiState!!.mode)

            viewModel.navigateToPrepareMode()
            assertEquals(WorkspaceMode.PREPARE, viewModel.learningWorkspaceUiState!!.mode)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `8 Back transition from PREPARE to EXPLORE and from EXPLORE to Closed`() {
        val tempDir = Files.createTempDirectory("explore-test-tmp-8")
        val persistenceDir = Files.createTempDirectory("explore-test-db-8")
        try {
            val (appContext, viewModel) = createTestEnvironment(tempDir, persistenceDir)
            val pkgId = InstalledPackageId(appContext.installedPackages.query().first { it.name == "Package A" }.id)
            viewModel.browsePackageLessons(pkgId, "Package A")

            val selection = PackageLessonSelection(
                installedPackageId = pkgId,
                lessonId = "cnt-lesson-1",
                packageName = "Package A",
                lessonTitle = "Lesson 1"
            )

            viewModel.openWorkspaceForSelection(selection)
            viewModel.navigateToPrepareMode()
            assertEquals(WorkspaceMode.PREPARE, viewModel.learningWorkspaceUiState!!.mode)

            // Back from PREPARE -> EXPLORE
            viewModel.handleWorkspaceBack()
            assertEquals(WorkspaceMode.EXPLORE, viewModel.learningWorkspaceUiState!!.mode)

            // Back from EXPLORE -> Workspace closed
            viewModel.handleWorkspaceBack()
            assertNull(viewModel.learningWorkspaceUiState)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `9 Start study is blocked in EXPLORE mode and allowed in PREPARE mode`() {
        val tempDir = Files.createTempDirectory("explore-test-tmp-9")
        val persistenceDir = Files.createTempDirectory("explore-test-db-9")
        try {
            val (appContext, viewModel) = createTestEnvironment(tempDir, persistenceDir)
            val pkgId = InstalledPackageId(appContext.installedPackages.query().first { it.name == "Package A" }.id)
            viewModel.browsePackageLessons(pkgId, "Package A")

            val selection = PackageLessonSelection(
                installedPackageId = pkgId,
                lessonId = "cnt-lesson-1",
                packageName = "Package A",
                lessonTitle = "Lesson 1"
            )

            viewModel.openWorkspaceForSelection(selection)
            assertEquals(WorkspaceMode.EXPLORE, viewModel.learningWorkspaceUiState!!.mode)

            var started = false
            viewModel.startStudyFromWorkspace { started = true }
            assertFalse(started, "Start study must be blocked while in EXPLORE mode")

            viewModel.navigateToPrepareMode()
            viewModel.startStudyFromWorkspace { started = true }
            assertTrue(started, "Start study must be allowed in PREPARE mode")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `10 Next navigation updates exploreIndex without reloading workspace`() {
        val tempDir = Files.createTempDirectory("explore-test-tmp-10")
        val persistenceDir = Files.createTempDirectory("explore-test-db-10")
        try {
            val (appContext, viewModel) = createTestEnvironment(tempDir, persistenceDir)
            val pkgId = InstalledPackageId(appContext.installedPackages.query().first { it.name == "Package A" }.id)
            viewModel.browsePackageLessons(pkgId, "Package A")

            val selection = PackageLessonSelection(
                installedPackageId = pkgId,
                lessonId = "cnt-lesson-1",
                packageName = "Package A",
                lessonTitle = "Lesson 1"
            )

            viewModel.openWorkspaceForSelection(selection)
            val initialWorkspace = viewModel.learningWorkspaceUiState
            assertNotNull(initialWorkspace)
            assertEquals(0, initialWorkspace!!.exploreIndex)
            assertEquals("cnt-lesson-1-item1", initialWorkspace.currentExploreItem!!.id)

            viewModel.nextExploreItem()

            val updatedWorkspace = viewModel.learningWorkspaceUiState
            assertNotNull(updatedWorkspace)
            assertEquals(1, updatedWorkspace!!.exploreIndex)
            assertEquals("cnt-lesson-1-item2", updatedWorkspace.currentExploreItem!!.id)
            assertEquals(initialWorkspace.contentId, updatedWorkspace.contentId)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `11 Previous navigation updates exploreIndex without reloading workspace`() {
        val tempDir = Files.createTempDirectory("explore-test-tmp-11")
        val persistenceDir = Files.createTempDirectory("explore-test-db-11")
        try {
            val (appContext, viewModel) = createTestEnvironment(tempDir, persistenceDir)
            val pkgId = InstalledPackageId(appContext.installedPackages.query().first { it.name == "Package A" }.id)
            viewModel.browsePackageLessons(pkgId, "Package A")

            val selection = PackageLessonSelection(
                installedPackageId = pkgId,
                lessonId = "cnt-lesson-1",
                packageName = "Package A",
                lessonTitle = "Lesson 1"
            )

            viewModel.openWorkspaceForSelection(selection)
            viewModel.nextExploreItem()
            assertEquals(1, viewModel.learningWorkspaceUiState!!.exploreIndex)

            viewModel.previousExploreItem()
            assertEquals(0, viewModel.learningWorkspaceUiState!!.exploreIndex)
            assertEquals("cnt-lesson-1-item1", viewModel.learningWorkspaceUiState!!.currentExploreItem!!.id)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `12 Navigation boundaries disable Previous on first item and Next on last item`() {
        val tempDir = Files.createTempDirectory("explore-test-tmp-12")
        val persistenceDir = Files.createTempDirectory("explore-test-db-12")
        try {
            val (appContext, viewModel) = createTestEnvironment(tempDir, persistenceDir)
            val pkgId = InstalledPackageId(appContext.installedPackages.query().first { it.name == "Package A" }.id)
            viewModel.browsePackageLessons(pkgId, "Package A")

            val selection = PackageLessonSelection(
                installedPackageId = pkgId,
                lessonId = "cnt-lesson-1",
                packageName = "Package A",
                lessonTitle = "Lesson 1"
            )

            viewModel.openWorkspaceForSelection(selection)

            // At item 0: Previous disabled, Next enabled
            val w0 = viewModel.learningWorkspaceUiState!!
            assertFalse(w0.canNavigatePrevious)
            assertTrue(w0.canNavigateNext)

            // Attempt previous on first item (should remain 0)
            viewModel.previousExploreItem()
            assertEquals(0, viewModel.learningWorkspaceUiState!!.exploreIndex)

            // Move to last item (index 1)
            viewModel.nextExploreItem()

            // At item 1 (last): Previous enabled, Next disabled
            val w1 = viewModel.learningWorkspaceUiState!!
            assertTrue(w1.canNavigatePrevious)
            assertFalse(w1.canNavigateNext)

            // Attempt next on last item (should remain 1)
            viewModel.nextExploreItem()
            assertEquals(1, viewModel.learningWorkspaceUiState!!.exploreIndex)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `13 WorkspaceProjectionAssembler populates content preview and metadata correctly`() {
        val tempDir = Files.createTempDirectory("explore-test-tmp-13")
        val persistenceDir = Files.createTempDirectory("explore-test-db-13")
        try {
            val (appContext, viewModel) = createTestEnvironment(tempDir, persistenceDir)
            val assembler = WorkspaceProjectionAssembler(appContext)

            val items = assembler.assembleExploreItems("cnt-lesson-1")
            assertEquals(2, items.size)

            val item1 = items.first { it.id == "cnt-lesson-1-item1" }
            assertEquals("Package A Primary", item1.primaryText)
            assertEquals("Package A Translation", item1.translatedText)
            assertEquals("SENTENCE", item1.contentType)
            assertEquals("MEANING_RECALL", item1.mode)
            assertEquals("UNSEEN", item1.stage)
            assertFalse(item1.isDue)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }
}
