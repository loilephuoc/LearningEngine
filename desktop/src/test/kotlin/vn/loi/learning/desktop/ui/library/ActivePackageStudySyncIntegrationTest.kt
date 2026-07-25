package vn.loi.learning.desktop.ui.library

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import vn.loi.learning.application.session.StartPackageLessonStudyRequest
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.shell.createCanonicalLibraryFacade
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ActivePackageStudySyncIntegrationTest {

    private fun getPkgId(appContext: vn.loi.learning.infrastructure.LearningApplicationContext, packageName: String): InstalledPackageId {
        val summary = appContext.libraryQuery?.getInstalledPackages(appContext.defaultLibraryId!!)?.firstOrNull { it.name == packageName }
        if (summary != null) return summary.id
        val item = appContext.installedPackages.query().first { it.name == packageName }
        return InstalledPackageId(item.id)
    }

    @Test
    fun `1 changing active package in Library updates idle Study state to newly active package B`() {
        val tempDir = Files.createTempDirectory("sync-test-pkg")
        val persistenceDir = Files.createTempDirectory("sync-test-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")

            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)!!

            val studyViewModel = StudyViewModel(
                facade = StudyFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )

            var callbackTriggered = false
            val libraryViewModel = LibraryViewModel(
                facade = libraryFacade,
                taskRunner = ImmediateDesktopTaskRunner,
                onLibraryDataChanged = {
                    callbackTriggered = true
                    studyViewModel.refresh()
                }
            )

            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val pkgIdA = getPkgId(appContext, "Topic A")
            val pkgIdB = getPkgId(appContext, "Topic B")

            // 1. Set Pkg A active in Library and load/cache in StudyFacade
            libraryViewModel.setActivePackage(pkgIdA)
            studyViewModel.refresh()
            assertEquals(pkgIdA.value, studyViewModel.uiState.activeInstalledPackageId?.value, "Study must initially project Package A")

            // 2. Change Library active package to B
            libraryViewModel.setActivePackage(pkgIdB)
            assertEquals(true, callbackTriggered, "onLibraryDataChanged callback must be triggered on active package change")

            // 3. Verify idle Study now projects Package B, with zero stale A identity remaining
            val studyState = studyViewModel.uiState
            assertEquals(pkgIdB.value, studyState.activeInstalledPackageId?.value, "Idle Study must project newly active Package B")
            assertEquals(false, studyState.isLessonStudy, "Idle Study must reset lesson study flag when package changes")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `2 active session for Package A is NOT destroyed or rebound when Package B becomes active`() {
        val tempDir = Files.createTempDirectory("session-safety-pkg")
        val persistenceDir = Files.createTempDirectory("session-safety-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")

            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)!!

            val studyFacade = StudyFacade(appContext)
            val studyViewModel = StudyViewModel(
                facade = studyFacade,
                taskRunner = ImmediateDesktopTaskRunner
            )

            val libraryViewModel = LibraryViewModel(
                facade = libraryFacade,
                taskRunner = ImmediateDesktopTaskRunner,
                onLibraryDataChanged = {
                    studyViewModel.refresh()
                }
            )

            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val pkgIdA = getPkgId(appContext, "Topic A")
            val pkgIdB = getPkgId(appContext, "Topic B")

            // Set Pkg A active
            libraryViewModel.setActivePackage(pkgIdA)

            // Start lesson study for Pkg A
            studyViewModel.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = pkgIdA,
                    contentId = ContentId("cnt-a-1")
                )
            )
            assertEquals(pkgIdA.value, studyViewModel.uiState.activeInstalledPackageId?.value)
            assertEquals(true, studyViewModel.uiState.sessionStarted)

            // User sets Pkg B as active in Library
            libraryViewModel.setActivePackage(pkgIdB)

            // Verify active session for Pkg A is preserved and not silently rebound
            assertEquals(pkgIdA.value, studyViewModel.uiState.activeInstalledPackageId?.value, "Active session for Package A must be preserved")
            assertEquals("cnt-a-1", studyViewModel.uiState.activeContentId?.value, "Active session content must remain Package A content")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `3 explicit launch of Package B lesson starts B session with B identity without silent mutation`() {
        val tempDir = Files.createTempDirectory("explicit-b-launch-pkg")
        val persistenceDir = Files.createTempDirectory("explicit-b-launch-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")

            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)!!

            val studyFacade = StudyFacade(appContext)
            val studyViewModel = StudyViewModel(
                facade = studyFacade,
                taskRunner = ImmediateDesktopTaskRunner
            )

            val libraryViewModel = LibraryViewModel(
                facade = libraryFacade,
                taskRunner = ImmediateDesktopTaskRunner,
                onLibraryDataChanged = {
                    studyViewModel.refresh()
                }
            )

            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val pkgIdA = getPkgId(appContext, "Topic A")
            val pkgIdB = getPkgId(appContext, "Topic B")

            // 1. Start lesson study for Pkg A
            studyViewModel.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = pkgIdA,
                    contentId = ContentId("cnt-a-1")
                )
            )
            assertEquals(pkgIdA.value, studyViewModel.uiState.activeInstalledPackageId?.value)

            // 2. Explicitly launch lesson study for Package B
            studyViewModel.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = pkgIdB,
                    contentId = ContentId("cnt-b-1")
                )
            )

            // 3. Verify session B uses Package B identity and Package B content
            val uiStateB = studyViewModel.uiState
            assertEquals(pkgIdB.value, uiStateB.activeInstalledPackageId?.value, "Session for B must use Package B identity")
            assertEquals("cnt-b-1", uiStateB.activeContentId?.value, "Session for B must use Package B content ID")
            assertEquals("Topic B Greetings", uiStateB.studyTitle, "Session title must reflect Package B content metadata")
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
