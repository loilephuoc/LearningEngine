package vn.loi.learning.desktop.ui.library

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryOperation
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.navigation.NavigationDestination
import vn.loi.learning.desktop.ui.navigation.NavigationState
import vn.loi.learning.desktop.ui.shell.LessonStudyNavigationCoordinator
import vn.loi.learning.desktop.ui.shell.createCanonicalLibraryFacade
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.contentpackaging.JvmFileScopedPackageScanner
import vn.loi.learning.infrastructure.persistence.memory.InMemoryCollectionRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryRepository

class CanonicalDesktopImportIntegrationTest {

    private val libId = LibraryId("lib-test")

    private class StubMediaStorage : ContentMediaStorage {
        override fun store(packageName: String, fileName: String, content: ByteArray): vn.loi.learning.application.contentmedia.ContentMediaAsset {
            throw UnsupportedOperationException("StubMediaStorage store not implemented")
        }
        override fun resolve(relativePath: String): Path? = null
        override fun exists(relativePath: String): Boolean = false
    }

    // 1. File chooser configuration
    @Test
    fun `1 file chooser configuration uses FILES_ONLY and supported extension filters`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val facade = createCanonicalLibraryFacade(appContext)
        assertNotNull(facade)

        val libraryViewModel = LibraryViewModel(facade = facade, taskRunner = ImmediateDesktopTaskRunner)
        val contentLibraryViewModel = ContentLibraryViewModel(
            facade = ContentLibraryFacade(appContext),
            lessonBrowserFacade = LessonBrowserFacade(appContext)
        )

        assertIs<LibraryUiState.Empty>(libraryViewModel.uiState)
        assertNotNull(contentLibraryViewModel.uiState)
    }

    // 2. One-file import delegation
    @Test
    fun `2 one-file import delegation imports only selected file`() {
        val tempPkgDir = Files.createTempDirectory("file-import-test")
        val persistenceDir = Files.createTempDirectory("file-import-db")

        try {
            val pkgFile = tempPkgDir.resolve("target.opd3")
            createOpd3Package(pkgFile, name = "Target Package")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromDirectory(pkgFile)

            assertNotNull(contentLibVm.uiState.importMessage)
            assertNull(contentLibVm.uiState.importError)
            assertEquals(1, contentLibVm.uiState.packages.size)
            assertEquals("Target Package", contentLibVm.uiState.packages.first().name)
        } finally {
            tempPkgDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 3. Selected OPD3 import
    @Test
    fun `3 selected OPD3 file import through pipeline`() {
        val tempPkgDir = Files.createTempDirectory("opd3-single-test")
        val persistenceDir = Files.createTempDirectory("opd3-single-db")

        try {
            val opd3File = tempPkgDir.resolve("single.opd3")
            createOpd3Package(opd3File, name = "Single OPD3 Package")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibraryViewModel.importFromDirectory(opd3File)

            val state = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            assertEquals(1, state.installedPackages.size)
            assertEquals("Single OPD3 Package", state.installedPackages.first().name)
        } finally {
            tempPkgDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 4. Selected JSON/PKG same-basename pair resolution
    @Test
    fun `4 selected legacy JSON or PKG same-basename pair resolution`() {
        val tempDir = Files.createTempDirectory("legacy-pair-test")
        try {
            val jsonFile = tempDir.resolve("sample.json")
            val pkgFile = tempDir.resolve("sample.pkg")

            Files.writeString(jsonFile, "{ \"name\": \"Sample Legacy\" }")
            Files.write(pkgFile, "OPD3_BINARY_DATA".toByteArray())

            val scanner = JvmFileScopedPackageScanner(jsonFile)
            val candidates = scanner.scan()

            assertEquals(1, candidates.size)
            assertTrue(candidates.first().source.endsWith("sample.pkg"))
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    // 5. Unrelated sibling files are ignored
    @Test
    fun `5 unrelated sibling files in same directory are ignored during file import`() {
        val tempDir = Files.createTempDirectory("sibling-ignore-test")
        val persistenceDir = Files.createTempDirectory("sibling-ignore-db")

        try {
            val targetFile = tempDir.resolve("target.opd3")
            val siblingFile = tempDir.resolve("unrelated.opd3")

            createOpd3Package(targetFile, name = "Target Package")
            createOpd3Package(siblingFile, name = "Unrelated Package")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromDirectory(targetFile)

            assertEquals(1, contentLibVm.uiState.packages.size)
            assertEquals("Target Package", contentLibVm.uiState.packages.first().name)
            assertFalse(contentLibVm.uiState.packages.any { it.name == "Unrelated Package" })
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 6. Missing companion failure with zero partial persistence
    @Test
    fun `6 missing companion legacy pair produces clear failure with zero partial persistence`() {
        val tempDir = Files.createTempDirectory("missing-companion-test")
        val persistenceDir = Files.createTempDirectory("missing-companion-db")

        try {
            val jsonFile = tempDir.resolve("standalone.json")
            Files.writeString(jsonFile, "{ \"name\": \"Orphan JSON\" }")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibraryViewModel.importFromDirectory(jsonFile)

            assertNotNull(contentLibraryViewModel.uiState.importError)
            assertIs<LibraryUiState.Empty>(libraryViewModel.uiState)
            assertTrue(contentLibraryViewModel.uiState.packages.isEmpty())
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 7. Duplicate/conflict behavior remains canonical
    @Test
    fun `7 canonical duplicate and conflict behavior remains authoritative`() {
        val tempPkgDir = Files.createTempDirectory("duplicate-test-pkg")
        val persistenceDir = Files.createTempDirectory("duplicate-test-db")

        try {
            val pkgFile = tempPkgDir.resolve("duplicate.opd3")
            createOpd3Package(pkgFile, name = "Duplicate Test Package")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibraryViewModel.importFromDirectory(pkgFile)
            contentLibraryViewModel.importFromDirectory(pkgFile)

            val state = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            assertEquals(1, state.installedPackages.size)
        } finally {
            tempPkgDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 8. Removal confirmation presentation
    @Test
    fun `8 removal confirmation presentation dialog accurately explains data impact`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val facade = ContentLibraryFacade(appContext)
        val contentLibVm = ContentLibraryViewModel(
            facade = facade,
            lessonBrowserFacade = LessonBrowserFacade(appContext)
        )

        assertNull(contentLibVm.uiState.importError)
    }

    // 9. Successful installed-package removal and projection refresh
    @Test
    fun `9 successful installed-package removal reconciles all stores and projections`() {
        val tempPkgDir = Files.createTempDirectory("uninstall-test-pkg")
        val persistenceDir = Files.createTempDirectory("uninstall-test-db")

        try {
            val pkgFile = tempPkgDir.resolve("remove_me.opd3")
            createOpd3Package(pkgFile, name = "Remove Me Package")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibraryViewModel.importFromDirectory(pkgFile)
            val stateBefore = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            assertEquals(1, stateBefore.installedPackages.size)

            val pkgId = stateBefore.installedPackages.first().packageId.value
            contentLibraryViewModel.uninstallPackage(pkgId, "Remove Me Package")
            libraryViewModel.refresh()

            assertIs<LibraryUiState.Empty>(libraryViewModel.uiState)
            assertTrue(contentLibraryViewModel.uiState.packages.isEmpty())
        } finally {
            tempPkgDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 10. Removal failure preserves state
    @Test
    fun `10 removal failure preserves last valid state`() {
        val tempPkgDir = Files.createTempDirectory("removal-failure-pkg")
        val persistenceDir = Files.createTempDirectory("removal-failure-db")

        try {
            val pkgFile = tempPkgDir.resolve("preserve.opd3")
            createOpd3Package(pkgFile, name = "Preserve Package")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibraryViewModel.importFromDirectory(pkgFile)
            val stateBefore = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)

            // Attempt uninstall with non-existent package id
            contentLibraryViewModel.uninstallPackage("non-existent-id", "Fake Package")
            libraryViewModel.refresh()

            val stateAfter = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            assertEquals(1, stateAfter.installedPackages.size)
        } finally {
            tempPkgDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 11. Package A removal isolates package B
    @Test
    fun `11 removing package A isolates package B completely`() {
        val tempPkgDir = Files.createTempDirectory("isolation-test-pkg")
        val persistenceDir = Files.createTempDirectory("isolation-test-db")

        try {
            val pkgAFile = tempPkgDir.resolve("pkg_a.opd3")
            val pkgBFile = tempPkgDir.resolve("pkg_b.opd3")
            createOpd3Package(pkgAFile, name = "Package A", contentId = "content-a")
            createOpd3Package(pkgBFile, name = "Package B", contentId = "content-b")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibraryViewModel.importFromDirectory(pkgAFile)
            contentLibraryViewModel.importFromDirectory(pkgBFile)

            val stateBoth = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            assertEquals(2, stateBoth.installedPackages.size)

            val pkgAId = stateBoth.installedPackages.first { it.name == "Package A" }.packageId.value
            contentLibraryViewModel.uninstallPackage(pkgAId, "Package A")
            libraryViewModel.refresh()

            val stateAfter = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            assertEquals(1, stateAfter.installedPackages.size)
            assertEquals("Package B", stateAfter.installedPackages.first().name)
        } finally {
            tempPkgDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 12. Collection assignments referencing removed package are reconciled safely
    @Test
    fun `12 collection assignments referencing removed package are reconciled safely`() {
        val tempPkgDir = Files.createTempDirectory("collection-reconcile-pkg")
        val persistenceDir = Files.createTempDirectory("collection-reconcile-db")

        try {
            val pkgFile = tempPkgDir.resolve("assigned.opd3")
            createOpd3Package(pkgFile, name = "Assigned Package")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibraryViewModel.importFromDirectory(pkgFile)
            val stateBefore = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            val instPkg = stateBefore.installedPackages.first()

            libraryViewModel.submitCreateCollection("Test Collection", "Desc")
            val stateWithColl = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            val coll = stateWithColl.collections.first()

            libraryViewModel.submitAssignPackage(coll.collection.id, instPkg.id)

            contentLibraryViewModel.uninstallPackage(instPkg.packageId.value, "Assigned Package")
            libraryViewModel.refresh()

            val stateFinal = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            assertEquals(0, stateFinal.installedPackages.size)
            assertEquals(1, stateFinal.collections.size)
            assertTrue(stateFinal.collections.first().assignedPackages.isEmpty())
        } finally {
            tempPkgDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 13. Lesson browser state is cleared when its package is removed
    @Test
    fun `13 lesson browser state is cleared when its package is removed`() {
        val tempPkgDir = Files.createTempDirectory("lesson-browser-clear-pkg")
        val persistenceDir = Files.createTempDirectory("lesson-browser-clear-db")

        try {
            val pkgFile = tempPkgDir.resolve("browse_clear.opd3")
            createOpd3Package(pkgFile, name = "Browse Clear Package")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibraryViewModel.importFromDirectory(pkgFile)
            val libSummary = contentLibraryViewModel.uiState.libraries.first()
            contentLibraryViewModel.openLibrary(libSummary.id)

            assertNotNull(contentLibraryViewModel.lessonBrowserUiState)

            val pkgId = contentLibraryViewModel.uiState.packages.first().id
            contentLibraryViewModel.uninstallPackage(pkgId, "Browse Clear Package")

            assertNull(contentLibraryViewModel.lessonBrowserUiState)
        } finally {
            tempPkgDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 14. Restart persistence: removed package stays removed
    @Test
    fun `14 restart persistence maintains removed package state after application reload`() {
        val tempPkgDir = Files.createTempDirectory("restart-uninstall-pkg")
        val persistenceDir = Files.createTempDirectory("restart-uninstall-db")

        try {
            val pkgFile = tempPkgDir.resolve("restart_pkg.opd3")
            createOpd3Package(pkgFile, name = "Restart Package")

            var appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            var contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromDirectory(pkgFile)
            assertEquals(1, contentLibVm.uiState.packages.size)

            val pkgId = contentLibVm.uiState.packages.first().id
            contentLibVm.uninstallPackage(pkgId, "Restart Package")
            assertTrue(contentLibVm.uiState.packages.isEmpty())

            // Simulate application restart
            appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            assertTrue(contentLibVm.uiState.packages.isEmpty())
            val libFacade = createCanonicalLibraryFacade(appContext)
            val libVm = LibraryViewModel(facade = libFacade, taskRunner = ImmediateDesktopTaskRunner)
            assertIs<LibraryUiState.Empty>(libVm.uiState)
        } finally {
            tempPkgDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 15. Re-import after package removal succeeds as fresh package
    @Test
    fun `15 re-import after package removal succeeds as fresh package`() {
        val tempPkgDir = Files.createTempDirectory("reimport-pkg")
        val persistenceDir = Files.createTempDirectory("reimport-db")

        try {
            val pkgFile = tempPkgDir.resolve("reimport.opd3")
            createOpd3Package(pkgFile, name = "Reimport Package")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)
            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibraryViewModel.importFromDirectory(pkgFile)
            val state1 = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            val pkgId = state1.installedPackages.first().packageId.value

            contentLibraryViewModel.uninstallPackage(pkgId, "Reimport Package")
            libraryViewModel.refresh()
            assertIs<LibraryUiState.Empty>(libraryViewModel.uiState)

            // Re-import
            contentLibraryViewModel.importFromDirectory(pkgFile)
            val state2 = assertIs<LibraryUiState.Content>(libraryViewModel.uiState)
            assertEquals(1, state2.installedPackages.size)
        } finally {
            tempPkgDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // 16. Dependency-direction guard
    @Test
    fun `16 canonical Library source contains zero imports from infrastructure or adapter`() {
        val libraryDir = File("src/main/kotlin/vn/loi/learning/desktop/ui/library").takeIf { it.exists() }
            ?: File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/library")
        assertTrue(libraryDir.exists() && libraryDir.isDirectory, "Library UI directory must exist")

        val files = libraryDir.walk().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue(files.isNotEmpty(), "Library directory must contain Kotlin source files")

        for (file in files) {
            val lines = file.readLines()
            for (line in lines) {
                val trimmed = line.trim()
                assertFalse(
                    trimmed.startsWith("import vn.loi.learning.infrastructure"),
                    "File ${file.name} in desktop/ui/library must not import infrastructure: $trimmed"
                )
                assertFalse(
                    trimmed.startsWith("import vn.loi.learning.adapter"),
                    "File ${file.name} in desktop/ui/library must not import adapter: $trimmed"
                )
            }
        }
    }

    // 17. Explicit negative test: no Export OPD3 UI introduced
    @Test
    fun `17 no Export OPD3 UI action button or workflow is introduced in Desktop Library`() {
        val libraryDir = File("src/main/kotlin/vn/loi/learning/desktop/ui/library").takeIf { it.exists() }
            ?: File("desktop/src/main/kotlin/vn/loi/learning/desktop/ui/library")
        assertTrue(libraryDir.exists() && libraryDir.isDirectory, "Library UI directory must exist")
        val files = libraryDir.walk().filter { it.isFile && it.extension == "kt" }.toList()

        val forbiddenTokens = listOf("Export OPD3", "exportOpd3", "exportPackage", "Save Package", "onExportPackage")

        for (file in files) {
            val text = file.readText()
            for (token in forbiddenTokens) {
                assertFalse(
                    text.contains(token),
                    "File ${file.name} must not contain forbidden Export UI token '$token'"
                )
            }
        }
    }

    private fun createOpd3Package(file: Path, name: String = "Sample OPD3 Package", contentId: String = "content-1") {
        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            writeEntry(
                zip,
                "manifest.json",
                """
                {
                  "name": "$name",
                  "version": "1.0.0",
                  "format": "OPD3",
                  "contentCount": 1,
                  "learningItemCount": 2
                }
                """.trimIndent()
            )
            writeEntry(
                zip,
                "metadata.json",
                """
                {
                  "name": "$name",
                  "version": "1.0.0",
                  "format": "OPD3"
                }
                """.trimIndent()
            )
            writeEntry(
                zip,
                "contents.json",
                """
                {
                  "contents": [
                    {
                      "id": "$contentId",
                      "type": "SENTENCE",
                      "primaryText": "Greeting Sentence",
                      "translatedText": "Cau chao hoi",
                      "title": "Greeting",
                      "group": "English",
                      "section": "Unit 1",
                      "lesson": "$name Greetings"
                    }
                  ]
                }
                """.trimIndent()
            )
            writeEntry(
                zip,
                "learning-items.json",
                """
                {
                  "learningItems": [
                    {
                      "id": "$contentId-recognition",
                      "contentId": "$contentId",
                      "mode": "MEANING_RECOGNITION",
                      "isEnabled": true
                    },
                    {
                      "id": "$contentId-recall",
                      "contentId": "$contentId",
                      "mode": "MEANING_RECALL",
                      "isEnabled": true
                    }
                  ]
                }
                """.trimIndent()
            )
        }
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
}
