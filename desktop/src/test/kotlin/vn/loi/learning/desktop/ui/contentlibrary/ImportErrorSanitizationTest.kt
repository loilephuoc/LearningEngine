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
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.PackageImportProgressEvent
import vn.loi.learning.application.contentpackaging.PackageImportProgressStage
import vn.loi.learning.desktop.ui.library.LibraryViewModel
import vn.loi.learning.desktop.ui.shell.createCanonicalLibraryFacade
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ImportErrorSanitizationTest {

    @Test
    fun `1 duplicate import error message is sanitized into bounded summary`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val viewModel = ContentLibraryViewModel(
            facade = ContentLibraryFacade(appContext),
            lessonBrowserFacade = LessonBrowserFacade(appContext)
        )

        // Build simulated raw error string containing 500 lines of CONTENT_ID_ALREADY_INSTALLED
        val rawIssues = (1..500).joinToString("\n") { i ->
            "[CONTENT_ID_ALREADY_INSTALLED] Content with id 'legacy-content-$i' is already installed."
        }

        val importResult = ContentLibraryImportResult(
            discoveredPackageCount = 1,
            importedPackageCount = 0,
            importedLibraryCount = 0,
            importedContentCount = 0,
            importedLearningItemCount = 0,
            failures = listOf(
                ContentLibraryImportFailure(
                    source = "DuplicatePkg.json",
                    message = rawIssues
                )
            )
        )

        val errorText = viewModel.let { vm ->
            val method = vm.javaClass.getDeclaredMethod("buildImportError", ContentLibraryImportResult::class.java)
            method.isAccessible = true
            method.invoke(vm, importResult) as String
        }

        assertNotNull(errorText)
        assertTrue(errorText.contains("500 content conflict(s) detected"), "Error text must include total conflict count")
        assertTrue(errorText.contains("CONTENT_ID_ALREADY_INSTALLED"), "Error text must mention error code")
        assertFalse(errorText.lines().size > 15, "UI error text must be bounded and concise, but was ${errorText.lines().size} lines")
    }

    @Test
    fun `2 duplicate package import failure preserves all authoritative state and emits no COMPLETED stage`() {
        val tempDir = Files.createTempDirectory("dup-import-atomicity-dir")
        val persistenceDir = Files.createTempDirectory("dup-import-atomicity-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileDuplicateA = tempDir.resolve("DuplicateA.opd3")

            // Package A and Duplicate Package A share identical content ID "cnt-a-1"
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileDuplicateA, name = "Duplicate Topic A", contentId = "cnt-a-1")

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
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            // 1. Successfully import Package A
            contentLibVm.importFromFiles(listOf(fileA))
            val initialNavTree = libraryFacade.loadNavigationTree()!!
            assertEquals(1, initialNavTree.installedPackages.size)

            val pkgIdA = initialNavTree.installedPackages.first().id
            libraryViewModel.setActivePackage(pkgIdA)
            studyViewModel.refresh()

            val initialActivePkg = libraryFacade.loadNavigationTree()?.activePackageId
            assertEquals(pkgIdA, initialActivePkg)

            // 2. Track events during duplicate import attempt
            val events = mutableListOf<PackageImportProgressEvent>()
            val importerService = appContext.packageImporterWithProgress(fileDuplicateA) { event ->
                events += event
            }

            // Attempt duplicate import
            val batchResult = importerService.importAllDetailed(PackageCatalogId("desktop-content-library"))

            // 3. Assert failure
            assertEquals(0, batchResult.successfulImports.size, "Duplicate import must produce 0 successful imports")
            assertEquals(1, batchResult.failures.size, "Duplicate import must record 1 failure")

            // 4. Assert NO COMPLETED stage was emitted
            assertFalse(
                events.any { it.stage == PackageImportProgressStage.COMPLETED },
                "PackageImportProgressStage.COMPLETED must NOT be emitted when duplicate import candidate fails"
            )

            // 5. Assert all authoritative state remains unchanged
            val postNavTree = libraryFacade.loadNavigationTree()!!
            assertEquals(1, postNavTree.installedPackages.size, "Catalog count must remain exactly 1")
            assertEquals(pkgIdA, postNavTree.activePackageId, "Active package must remain Package A")
            assertEquals(pkgIdA.value, studyViewModel.uiState.activeInstalledPackageId?.value, "Study state must remain Package A")
            assertFalse(
                postNavTree.installedPackages.any { it.name == "Duplicate Topic A" },
                "Failed candidate must NOT become visible in catalog or active"
            )
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
