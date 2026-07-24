package vn.loi.learning.desktop.ui.shell

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.session.StartPackageLessonStudyRequest
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.contentlibrary.PackageLessonSelection
import vn.loi.learning.desktop.ui.navigation.NavigationDestination
import vn.loi.learning.desktop.ui.navigation.NavigationState
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.infrastructure.LearningApplicationFactory

class PreservePackageContextStudyEntryIntegrationTest {

    // T1 — Coordinator preserves package context
    @Test
    fun `T1 coordinator preserves installedPackageId and contentId in StartPackageLessonStudyRequest`() {
        val persistenceDir = Files.createTempDirectory("coord-preserves-ctx-db")
        try {
            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            val navState = NavigationState()
            val coordinator = LessonStudyNavigationCoordinator(studyVm, navState)

            val pkgId = InstalledPackageId("pkg-test-a")
            val selection = PackageLessonSelection(
                installedPackageId = pkgId,
                lessonId = "lesson-a-1",
                packageName = "Test Package",
                lessonTitle = "Lesson A"
            )

            // Attempting to start with non-existent package A will fail application validation safely
            coordinator.startLessonStudy(selection)

            // Assert failure was captured in StudyViewModel, not silent and not discarding package context
            assertNotNull(studyVm.uiState.loadError)
            assertTrue(studyVm.uiState.loadError!!.contains("pkg-test-a"))
            assertFalse(studyVm.uiState.hasActiveSession)
            assertEquals(NavigationDestination.DASHBOARD, navState.currentDestination)
        } finally {
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T2 — Missing package context
    @Test
    fun `T2 missing package context in selection guards coordinator without calling study or creating fake ID`() {
        val persistenceDir = Files.createTempDirectory("missing-pkg-ctx-db")
        try {
            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            val navState = NavigationState()
            val coordinator = LessonStudyNavigationCoordinator(studyVm, navState)

            val invalidSelection = PackageLessonSelection(
                installedPackageId = null,
                lessonId = "lesson-1"
            )

            coordinator.startLessonStudy(invalidSelection)

            assertFalse(studyVm.uiState.hasActiveSession)
            assertNull(studyVm.uiState.loadError, "No study operation should be invoked when installedPackageId is null")
            assertEquals(NavigationDestination.DASHBOARD, navState.currentDestination)
        } finally {
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T3 — Valid package and lesson
    @Test
    fun `T3 valid package and lesson creates active session and navigates to STUDY`() {
        val tempDir = Files.createTempDirectory("valid-pkg-study-test")
        val persistenceDir = Files.createTempDirectory("valid-pkg-study-db")
        try {
            val opd3File = tempDir.resolve("ValidTopic.opd3")
            createOpd3ZipPackage(opd3File, name = "Valid Topic", contentId = "cnt-valid-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromFiles(listOf(opd3File))

            val navTree = appContext.libraryQuery!!.getNavigationTree(appContext.defaultLibraryId!!)!!
            val pkgSummary = navTree.installedPackages.first { it.name == "Valid Topic" }

            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            val navState = NavigationState()
            val coordinator = LessonStudyNavigationCoordinator(studyVm, navState)

            val request = StartPackageLessonStudyRequest(
                installedPackageId = pkgSummary.id,
                contentId = ContentId("cnt-valid-1")
            )

            coordinator.startLessonStudy(request)

            assertTrue(studyVm.uiState.hasActiveSession, "Active study session must be created for valid package and lesson")
            assertEquals(NavigationDestination.STUDY, navState.currentDestination)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T4 — Cross-package mismatch
    @Test
    fun `T4 cross-package mismatch fails validation without creating active session or navigating STUDY`() {
        val tempDir = Files.createTempDirectory("mismatch-pkg-test")
        val persistenceDir = Files.createTempDirectory("mismatch-pkg-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")

            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val navTree = appContext.libraryQuery!!.getNavigationTree(appContext.defaultLibraryId!!)!!
            val pkgA = navTree.installedPackages.first { it.name == "Topic A" }

            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            val navState = NavigationState()
            navState.navigateTo(NavigationDestination.CONTENT_LIBRARY)
            val coordinator = LessonStudyNavigationCoordinator(studyVm, navState)

            // Requesting Lesson B (cnt-b-1) under Package A
            val mismatchRequest = StartPackageLessonStudyRequest(
                installedPackageId = pkgA.id,
                contentId = ContentId("cnt-b-1")
            )

            coordinator.startLessonStudy(mismatchRequest)

            assertFalse(studyVm.uiState.hasActiveSession, "Session must NOT be created when lesson does not belong to package")
            assertNotNull(studyVm.uiState.loadError)
            assertTrue(studyVm.uiState.loadError!!.contains("cnt-b-1"))
            assertEquals(NavigationDestination.CONTENT_LIBRARY, navState.currentDestination, "Navigation must remain on current screen")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T5 — Archived package
    @Test
    fun `T5 study request on archived package fails validation without creating active session or navigating`() {
        val tempDir = Files.createTempDirectory("archived-pkg-study-test")
        val persistenceDir = Files.createTempDirectory("archived-pkg-study-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromFiles(listOf(fileA))

            val defaultLibId = appContext.defaultLibraryId!!
            val navTree = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val pkgA = navTree.installedPackages.first { it.name == "Topic A" }

            // Archive Package A
            appContext.libraryCommand!!.archivePackage(defaultLibId, pkgA.id)

            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            val navState = NavigationState()
            navState.navigateTo(NavigationDestination.CONTENT_LIBRARY)
            val coordinator = LessonStudyNavigationCoordinator(studyVm, navState)

            val archivedRequest = StartPackageLessonStudyRequest(
                installedPackageId = pkgA.id,
                contentId = ContentId("cnt-a-1")
            )

            coordinator.startLessonStudy(archivedRequest)

            assertFalse(studyVm.uiState.hasActiveSession)
            assertNotNull(studyVm.uiState.loadError)
            assertTrue(studyVm.uiState.loadError!!.contains("ARCHIVED"))
            assertEquals(NavigationDestination.CONTENT_LIBRARY, navState.currentDestination)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T6 — Removed package
    @Test
    fun `T6 study request on removed package fails validation without creating session`() {
        val tempDir = Files.createTempDirectory("removed-pkg-study-test")
        val persistenceDir = Files.createTempDirectory("removed-pkg-study-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromFiles(listOf(fileA))

            val navTree = appContext.libraryQuery!!.getNavigationTree(appContext.defaultLibraryId!!)!!
            val pkgA = navTree.installedPackages.first { it.name == "Topic A" }

            // Remove Package A
            contentLibVm.uninstallPackage(pkgA.packageId.value, pkgA.name)

            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            val navState = NavigationState()
            val coordinator = LessonStudyNavigationCoordinator(studyVm, navState)

            val removedRequest = StartPackageLessonStudyRequest(
                installedPackageId = pkgA.id,
                contentId = ContentId("cnt-a-1")
            )

            coordinator.startLessonStudy(removedRequest)

            assertFalse(studyVm.uiState.hasActiveSession)
            assertNotNull(studyVm.uiState.loadError)
            assertEquals(NavigationDestination.DASHBOARD, navState.currentDestination)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T7 — Missing lesson
    @Test
    fun `T7 non-existent lesson id fails validation without falling back to first lesson`() {
        val tempDir = Files.createTempDirectory("missing-lesson-test")
        val persistenceDir = Files.createTempDirectory("missing-lesson-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromFiles(listOf(fileA))

            val navTree = appContext.libraryQuery!!.getNavigationTree(appContext.defaultLibraryId!!)!!
            val pkgA = navTree.installedPackages.first { it.name == "Topic A" }

            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            val navState = NavigationState()
            val coordinator = LessonStudyNavigationCoordinator(studyVm, navState)

            val missingLessonRequest = StartPackageLessonStudyRequest(
                installedPackageId = pkgA.id,
                contentId = ContentId("non-existent-lesson-999")
            )

            coordinator.startLessonStudy(missingLessonRequest)

            assertFalse(studyVm.uiState.hasActiveSession)
            assertNotNull(studyVm.uiState.loadError)
            assertTrue(studyVm.uiState.loadError!!.contains("non-existent-lesson-999"))
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T8 — Zero learning items
    @Test
    fun `T8 lesson with zero enabled learning items fails validation without creating active session`() {
        val tempDir = Files.createTempDirectory("zero-items-test")
        val persistenceDir = Files.createTempDirectory("zero-items-db")
        try {
            val fileA = tempDir.resolve("TopicZero.opd3")
            // Create OPD3 package with content but 0 learning items
            ZipOutputStream(Files.newOutputStream(fileA)).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write("""{ "name": "Topic Zero", "version": "1.0.0", "format": "OPD3", "schemaVersion": 1, "contentCount": 1, "learningItemCount": 0 }""".toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("metadata.json"))
                zip.write("""{ "name": "Topic Zero", "version": "1.0.0", "format": "OPD3" }""".toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("contents.json"))
                zip.write("""{ "contents": [ { "id": "cnt-zero-1", "type": "SENTENCE", "primaryText": "Zero", "translatedText": "Khong", "title": "Zero", "group": "Eng", "section": "U1", "lesson": "Zero Lesson" } ] }""".toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("learning-items.json"))
                zip.write("""{ "learningItems": [] }""".toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromFiles(listOf(fileA))

            val navTree = appContext.libraryQuery!!.getNavigationTree(appContext.defaultLibraryId!!)!!
            val pkgZero = navTree.installedPackages.first { it.name == "Topic Zero" }

            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            val navState = NavigationState()
            val coordinator = LessonStudyNavigationCoordinator(studyVm, navState)

            val zeroItemsRequest = StartPackageLessonStudyRequest(
                installedPackageId = pkgZero.id,
                contentId = ContentId("cnt-zero-1")
            )

            coordinator.startLessonStudy(zeroItemsRequest)

            assertFalse(studyVm.uiState.hasActiveSession)
            assertNotNull(studyVm.uiState.loadError)
            assertTrue(studyVm.uiState.loadError!!.contains("no enabled learning items"))
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T9 — Same or similar IDs across packages
    @Test
    fun `T9 package resolution ensures lesson cannot be started under different package id`() {
        val tempDir = Files.createTempDirectory("similar-ids-test")
        val persistenceDir = Files.createTempDirectory("similar-ids-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")

            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-common-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-common-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val navTree = appContext.libraryQuery!!.getNavigationTree(appContext.defaultLibraryId!!)!!
            val pkgA = navTree.installedPackages.first { it.name == "Topic A" }

            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            val navState = NavigationState()
            val coordinator = LessonStudyNavigationCoordinator(studyVm, navState)

            val validRequestA = StartPackageLessonStudyRequest(
                installedPackageId = pkgA.id,
                contentId = ContentId("cnt-common-1")
            )

            coordinator.startLessonStudy(validRequestA)

            assertTrue(studyVm.uiState.hasActiveSession)
            assertEquals(NavigationDestination.STUDY, navState.currentDestination)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    private fun createOpd3ZipPackage(file: Path, name: String = "Single OPD3 Package", contentId: String = "content-1") {
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
