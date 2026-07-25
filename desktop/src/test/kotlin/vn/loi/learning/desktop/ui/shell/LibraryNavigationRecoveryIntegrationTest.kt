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
import vn.loi.learning.application.reviewhistory.ReviewHistoryQuery
import vn.loi.learning.application.session.StartPackageLessonStudyRequest
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.contentlibrary.PackageLessonSelection
import vn.loi.learning.desktop.ui.library.LibraryViewModel
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.infrastructure.LearningApplicationFactory

class LibraryNavigationRecoveryIntegrationTest {

    @Test
    fun `T1 Lesson Browser to sidebar Library or F5 restores Library Overview`() {
        val tempDir = Files.createTempDirectory("t1-pkg-dir")
        val persistenceDir = Files.createTempDirectory("t1-pkg-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val importer = appContext.packageImporter(fileA)
            importer.importAllDetailed(vn.loi.learning.domain.content.packaging.model.PackageCatalogId("test-cat"))

            val pkgIdA = InstalledPackageId(appContext.installedPackages.query().first().id)

            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )

            // Open Lesson Browser
            contentLibraryViewModel.browsePackageLessons(pkgIdA, "Topic A")
            assertNotNull(contentLibraryViewModel.lessonBrowserUiState)

            // Simulate canonical reset (Sidebar Library click / F5)
            contentLibraryViewModel.resetLibraryNavigationState()
            assertNull(contentLibraryViewModel.lessonBrowserUiState, "Lesson Browser must be closed")
            assertNull(contentLibraryViewModel.learningWorkspaceUiState, "Learning Workspace must be closed")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `T2 Learning Workspace to sidebar Library or F5 restores Library Overview`() {
        val tempDir = Files.createTempDirectory("t2-pkg-dir")
        val persistenceDir = Files.createTempDirectory("t2-pkg-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val importer = appContext.packageImporter(fileA)
            importer.importAllDetailed(vn.loi.learning.domain.content.packaging.model.PackageCatalogId("test-cat"))

            val pkgIdA = InstalledPackageId(appContext.installedPackages.query().first().id)

            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )

            // Open Workspace
            contentLibraryViewModel.browsePackageLessons(pkgIdA, "Topic A")
            contentLibraryViewModel.openWorkspaceForSelection(PackageLessonSelection(pkgIdA, "cnt-a-1"))
            assertNotNull(contentLibraryViewModel.learningWorkspaceUiState)

            // Simulate canonical reset
            contentLibraryViewModel.resetLibraryNavigationState()
            assertNull(contentLibraryViewModel.learningWorkspaceUiState, "Learning Workspace must be closed")
            assertNull(contentLibraryViewModel.lessonBrowserUiState, "Lesson Browser must be closed")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `T3 Completed Package A to Library to Package B active does not render completion A in Study`() {
        val tempDir = Files.createTempDirectory("t3-pkg-dir")
        val persistenceDir = Files.createTempDirectory("t3-pkg-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)!!

            val studyFacade = StudyFacade(appContext)

            val libraryViewModel = LibraryViewModel(
                facade = libraryFacade,
                taskRunner = ImmediateDesktopTaskRunner
            )

            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )

            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val pkgIdA = libraryFacade.loadNavigationTree()!!.installedPackages.first { it.name == "Topic A" }.id
            val pkgIdB = libraryFacade.loadNavigationTree()!!.installedPackages.first { it.name == "Topic B" }.id

            // Complete a session for Package A directly via facade
            studyFacade.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = pkgIdA,
                    contentId = ContentId("cnt-a-1")
                )
            )
            studyFacade.revealAnswer()
            val completedUiState = studyFacade.review(ReviewRating.GOOD)
            assertTrue(completedUiState.sessionCompleted, "Session A must be completed")

            // Simulate returning to Library & dismissing completion presentation
            val dismissedState = studyFacade.dismissCompletionPresentation()
            assertFalse(dismissedState.sessionCompleted, "Completion presentation must be dismissed")

            // Set Package B active in Library
            libraryViewModel.setActivePackage(pkgIdB)

            // Refresh Study
            val studyStateForB = studyFacade.load()
            assertEquals(pkgIdB.value, studyStateForB.activeInstalledPackageId?.value, "Study must project Package B")
            assertFalse(studyStateForB.sessionCompleted, "Completion A must not be rendered in Study for B")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `T4 Active or paused Package A session remains resumable when B is active in Library`() {
        val tempDir = Files.createTempDirectory("t4-pkg-dir")
        val persistenceDir = Files.createTempDirectory("t4-pkg-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)!!

            val studyFacade = StudyFacade(appContext)

            val libraryViewModel = LibraryViewModel(
                facade = libraryFacade,
                taskRunner = ImmediateDesktopTaskRunner
            )

            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )

            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val pkgIdA = libraryFacade.loadNavigationTree()!!.installedPackages.first { it.name == "Topic A" }.id
            val pkgIdB = libraryFacade.loadNavigationTree()!!.installedPackages.first { it.name == "Topic B" }.id

            // Start lesson study for Package A
            val activeState = studyFacade.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = pkgIdA,
                    contentId = ContentId("cnt-a-1")
                )
            )
            assertTrue(activeState.hasActiveSession)

            // Set Package B active in Library
            libraryViewModel.setActivePackage(pkgIdB)

            // Refresh Study
            val studyState = studyFacade.load()

            // Assert active session A is still active and bound to Package A
            assertTrue(studyState.hasActiveSession, "Active session A must remain resumable")
            assertEquals(pkgIdA.value, studyState.activeInstalledPackageId?.value, "Active session A must stay bound to Package A")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `T5 Back to Library callbacks and sidebar F5 use unified reset behavior`() {
        val tempDir = Files.createTempDirectory("t5-pkg-dir")
        val persistenceDir = Files.createTempDirectory("t5-pkg-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))

            val libraryFacade = createCanonicalLibraryFacade(appContext)!!
            val pkgIdA = libraryFacade.loadNavigationTree()!!.installedPackages.first().id

            val studyFacade = StudyFacade(appContext)

            // Simulate open overlays and completed session presentation
            contentLibVm.browsePackageLessons(pkgIdA, "Topic A")
            contentLibVm.openWorkspaceForSelection(PackageLessonSelection(pkgIdA, "cnt-a-1"))
            assertNotNull(contentLibVm.learningWorkspaceUiState)

            // Canonical reset flow
            contentLibVm.resetLibraryNavigationState()
            val resetStudyState = studyFacade.dismissCompletionPresentation()

            assertNull(contentLibVm.learningWorkspaceUiState)
            assertNull(contentLibVm.lessonBrowserUiState)
            assertFalse(resetStudyState.sessionCompleted)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `T6 Completion presentation dismissal does not mutate session or domain history state`() {
        val tempDir = Files.createTempDirectory("t6-pkg-dir")
        val persistenceDir = Files.createTempDirectory("t6-pkg-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))

            val libraryFacade = createCanonicalLibraryFacade(appContext)!!
            val pkgIdA = libraryFacade.loadNavigationTree()!!.installedPackages.first().id

            val studyFacade = StudyFacade(appContext)

            // Complete session directly via facade
            studyFacade.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = pkgIdA,
                    contentId = ContentId("cnt-a-1")
                )
            )
            studyFacade.revealAnswer()
            val completedState = studyFacade.review(ReviewRating.GOOD)
            assertTrue(completedState.sessionCompleted, "Session must be completed")

            val defaultLearner = LearnerId("default-learner")

            // Record history count before dismissal
            val historyBefore = appContext.reviewHistory.query(ReviewHistoryQuery(learnerId = defaultLearner)).size
            assertTrue(historyBefore > 0, "Review history event must exist")

            // Dismiss completion presentation
            val dismissedState = studyFacade.dismissCompletionPresentation()
            assertFalse(dismissedState.sessionCompleted, "Completion UI state must be dismissed")

            // Assert domain history and session persistence are intact
            val historyAfter = appContext.reviewHistory.query(ReviewHistoryQuery(learnerId = defaultLearner)).size
            assertEquals(historyBefore, historyAfter, "Dismissal must not mutate review history events")

            val latestSession = appContext.engine.getLatestUndoableSession(defaultLearner)
            assertNotNull(latestSession, "Completed session must remain intact in domain engine")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `T7 Complete A then dismiss then start and complete B renders Completion B normally`() {
        val tempDir = Files.createTempDirectory("t7-pkg-dir")
        val persistenceDir = Files.createTempDirectory("t7-pkg-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)!!
            val studyFacade = StudyFacade(appContext)

            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val pkgIdA = libraryFacade.loadNavigationTree()!!.installedPackages.first { it.name == "Topic A" }.id
            val pkgIdB = libraryFacade.loadNavigationTree()!!.installedPackages.first { it.name == "Topic B" }.id

            // Complete A
            studyFacade.startLessonStudy(StartPackageLessonStudyRequest(pkgIdA, ContentId("cnt-a-1")))
            studyFacade.revealAnswer()
            studyFacade.review(ReviewRating.GOOD)

            // Dismiss completion A
            studyFacade.dismissCompletionPresentation()

            // Start and complete B
            studyFacade.startLessonStudy(StartPackageLessonStudyRequest(pkgIdB, ContentId("cnt-b-1")))
            studyFacade.revealAnswer()
            val completionBState = studyFacade.review(ReviewRating.GOOD)

            // Assert completion B renders normally
            assertTrue(completionBState.sessionCompleted, "Completion B must render normally")
            assertEquals(pkgIdB.value, completionBState.activeInstalledPackageId?.value, "Completion B must belong to Package B")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `T8 Active or paused A on canonical Library navigation resumes A and does not force Idle`() {
        val tempDir = Files.createTempDirectory("t8-pkg-dir")
        val persistenceDir = Files.createTempDirectory("t8-pkg-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)!!
            val studyFacade = StudyFacade(appContext)

            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))
            val pkgIdA = libraryFacade.loadNavigationTree()!!.installedPackages.first().id

            // Start active session A
            val activeState = studyFacade.startLessonStudy(StartPackageLessonStudyRequest(pkgIdA, ContentId("cnt-a-1")))
            assertTrue(activeState.hasActiveSession)

            // Execute canonical reset
            contentLibVm.resetLibraryNavigationState()
            studyFacade.dismissCompletionPresentation()

            // Reload Study
            val reloadedStudyState = studyFacade.load()
            assertTrue(reloadedStudyState.hasActiveSession, "Active session A must remain resumable and not forced to Idle")
            assertEquals(pkgIdA.value, reloadedStudyState.activeInstalledPackageId?.value, "Active session must retain Package A identity")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `T9 Idle after dismiss completion A with active Library package B projects Idle B`() {
        val tempDir = Files.createTempDirectory("t9-pkg-dir")
        val persistenceDir = Files.createTempDirectory("t9-pkg-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val libraryFacade = createCanonicalLibraryFacade(appContext)!!
            val studyFacade = StudyFacade(appContext)

            val libraryViewModel = LibraryViewModel(facade = libraryFacade, taskRunner = ImmediateDesktopTaskRunner)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val pkgIdA = libraryFacade.loadNavigationTree()!!.installedPackages.first { it.name == "Topic A" }.id
            val pkgIdB = libraryFacade.loadNavigationTree()!!.installedPackages.first { it.name == "Topic B" }.id

            // Complete A
            studyFacade.startLessonStudy(StartPackageLessonStudyRequest(pkgIdA, ContentId("cnt-a-1")))
            studyFacade.revealAnswer()
            studyFacade.review(ReviewRating.GOOD)

            // Dismiss completion A
            studyFacade.dismissCompletionPresentation()

            // Set B active in Library
            libraryViewModel.setActivePackage(pkgIdB)

            // Load Study
            val idleStateB = studyFacade.load()
            assertFalse(idleStateB.sessionCompleted, "Must not project completed session A")
            assertEquals(pkgIdB.value, idleStateB.activeInstalledPackageId?.value, "Study Idle projection must belong to B")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `T10 Restart reload preserves existing recovery behavior without persisted dismissal state`() {
        val tempDir = Files.createTempDirectory("t10-pkg-dir")
        val persistenceDir = Files.createTempDirectory("t10-pkg-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))
            val libraryFacade = createCanonicalLibraryFacade(appContext)!!
            val pkgIdA = libraryFacade.loadNavigationTree()!!.installedPackages.first().id

            val studyFacade1 = StudyFacade(appContext)

            // Complete session in facade 1
            studyFacade1.startLessonStudy(StartPackageLessonStudyRequest(pkgIdA, ContentId("cnt-a-1")))
            studyFacade1.revealAnswer()
            studyFacade1.review(ReviewRating.GOOD)

            // Dismiss completion in facade 1 (in-memory only)
            studyFacade1.dismissCompletionPresentation()

            // Simulate app restart by creating new StudyFacade on the same persisted context
            val studyFacade2 = StudyFacade(appContext)
            val reloadedState = studyFacade2.load()

            // Assert existing recovery contract works on fresh facade (restores undoable completion)
            assertTrue(reloadedState.sessionCompleted, "App restart on fresh facade must load undoable completion without breaking recovery contract")
            assertEquals(pkgIdA.value, reloadedState.activeInstalledPackageId?.value)
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
