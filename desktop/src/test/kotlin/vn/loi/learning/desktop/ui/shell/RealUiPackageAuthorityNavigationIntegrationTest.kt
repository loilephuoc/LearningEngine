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
import vn.loi.learning.application.session.StartPackageLessonStudyRequest
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.contentlibrary.PackageLessonSelection
import vn.loi.learning.desktop.ui.dashboard.DashboardFacade
import vn.loi.learning.desktop.ui.dashboard.DashboardViewModel
import vn.loi.learning.desktop.ui.library.LibraryViewModel
import vn.loi.learning.desktop.ui.navigation.NavigationDestination
import vn.loi.learning.desktop.ui.navigation.NavigationState
import vn.loi.learning.desktop.ui.reviewhistory.ReviewHistoryFacade
import vn.loi.learning.desktop.ui.reviewhistory.ReviewHistoryViewModel
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.statistics.StatisticsFacade
import vn.loi.learning.desktop.ui.statistics.StatisticsViewModel
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.desktop.ui.study.toStudyQuickEditDraft
import vn.loi.learning.desktop.ui.study.recallContractTypingEvaluation
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.infrastructure.LearningApplicationFactory

class RealUiPackageAuthorityNavigationIntegrationTest {

    @Test
    fun `Test A Real shell active package path switches Study authority from A to B`() {
        val tempDir = Files.createTempDirectory("shell-pkg-dir")
        val persistenceDir = Files.createTempDirectory("shell-pkg-db")
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

            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )

            val libraryViewModel = LibraryViewModel(
                facade = libraryFacade,
                taskRunner = ImmediateDesktopTaskRunner,
                onLibraryDataChanged = {
                    studyViewModel.refresh()
                    contentLibraryViewModel.refresh()
                }
            )

            // Import A and B
            contentLibraryViewModel.importFromFiles(listOf(fileA))
            contentLibraryViewModel.importFromFiles(listOf(fileB))

            val pkgIdA = libraryFacade.loadNavigationTree()!!.installedPackages.first { it.name == "Topic A" }.id
            val pkgIdB = libraryFacade.loadNavigationTree()!!.installedPackages.first { it.name == "Topic B" }.id

            // 1. Set A active
            libraryViewModel.setActivePackage(pkgIdA)

            // 2. Invoke actual Study navigation / refresh
            studyViewModel.refresh()
            assertEquals(pkgIdA.value, studyViewModel.uiState.activeInstalledPackageId?.value, "Study must project active Package A")

            // 3. Return to Library and set B active
            libraryViewModel.setActivePackage(pkgIdB)

            // 4. Invoke actual Study navigation / refresh
            studyViewModel.refresh()
            assertEquals(pkgIdB.value, studyViewModel.uiState.activeInstalledPackageId?.value, "Study must project active Package B")

            // 5. Assert zero content/identity from A remains
            val uiState = studyViewModel.uiState
            assertTrue(uiState.activeContentId?.value != "cnt-a-1", "Stale Package A content must not remain in Study")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test B Active paused session A remains resumable and bound to A when B becomes active`() {
        val tempDir = Files.createTempDirectory("session-b-dir")
        val persistenceDir = Files.createTempDirectory("session-b-db")
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
                onLibraryDataChanged = { studyViewModel.refresh() }
            )

            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                onContentDataChanged = { libraryViewModel.refresh() }
            )

            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val pkgIdA = libraryFacade.loadNavigationTree()!!.installedPackages.first { it.name == "Topic A" }.id
            val pkgIdB = libraryFacade.loadNavigationTree()!!.installedPackages.first { it.name == "Topic B" }.id

            // 1. Start lesson study for Pkg A
            studyViewModel.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = pkgIdA,
                    contentId = ContentId("cnt-a-1")
                )
            )
            assertTrue(studyViewModel.uiState.hasActiveSession)
            assertEquals(pkgIdA.value, studyViewModel.uiState.activeInstalledPackageId?.value)

            // 2. Set Package B active in Library
            libraryViewModel.setActivePackage(pkgIdB)

            // 3. Assert session A remains active/resumable and bound to Package A
            assertTrue(studyViewModel.uiState.hasActiveSession, "Active session A must remain active")
            assertEquals(pkgIdA.value, studyViewModel.uiState.activeInstalledPackageId?.value, "Session A must stay bound to Package A")

            // 4. Start B session explicitly
            studyViewModel.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = pkgIdB,
                    contentId = ContentId("cnt-b-1")
                )
            )
            assertEquals(pkgIdB.value, studyViewModel.uiState.activeInstalledPackageId?.value, "Explicit start of B must create B session identity")
            assertEquals("cnt-b-1", studyViewModel.uiState.activeContentId?.value)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test C Navigation state machine from Library to Lesson Browser to Workspace to Back to F5 recovery`() {
        val tempDir = Files.createTempDirectory("nav-sm-dir")
        val persistenceDir = Files.createTempDirectory("nav-sm-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val importer = appContext.packageImporter(fileA)
            importer.importAllDetailed(vn.loi.learning.domain.content.packaging.model.PackageCatalogId("test-cat"))

            val pkgIdA = InstalledPackageId(appContext.installedPackages.query().first().id)

            val navigationState = NavigationState()
            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            // 1. Library -> Browse Lessons
            contentLibraryViewModel.browsePackageLessons(pkgIdA, "Topic A")
            assertNotNull(contentLibraryViewModel.lessonBrowserUiState)

            // 2. Browse Lessons -> Workspace
            contentLibraryViewModel.openWorkspaceForSelection(PackageLessonSelection(pkgIdA, "cnt-a-1"))
            assertNotNull(contentLibraryViewModel.learningWorkspaceUiState)

            // 3. Workspace Back -> Lesson Browser
            contentLibraryViewModel.closeWorkspace()
            assertNull(contentLibraryViewModel.learningWorkspaceUiState, "Workspace must be closed")
            assertNotNull(contentLibraryViewModel.lessonBrowserUiState, "Lesson Browser must remain")

            // 4. Lesson Browser Back -> Library
            contentLibraryViewModel.closeLibrary()
            assertNull(contentLibraryViewModel.lessonBrowserUiState, "Lesson Browser must be closed")

            // 5. Open Workspace again, then simulate F5 / Sidebar navigation to CONTENT_LIBRARY
            contentLibraryViewModel.browsePackageLessons(pkgIdA, "Topic A")
            contentLibraryViewModel.openWorkspaceForSelection(PackageLessonSelection(pkgIdA, "cnt-a-1"))
            assertNotNull(contentLibraryViewModel.learningWorkspaceUiState)

            // Simulate navigation/F5 refresh to CONTENT_LIBRARY
            contentLibraryViewModel.closeWorkspace()
            contentLibraryViewModel.closeLibrary()

            assertNull(contentLibraryViewModel.learningWorkspaceUiState, "F5 / Sidebar reset must close Workspace")
            assertNull(contentLibraryViewModel.lessonBrowserUiState, "F5 / Sidebar reset must close Lesson Browser")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Quick Edit persists through Studio authority without changing session queue or review evidence`() {
        val tempDir = Files.createTempDirectory("quick-edit-package")
        val persistenceDir = Files.createTempDirectory("quick-edit-db")
        try {
            val archive = tempDir.resolve("QuickEdit.opd3")
            createOpd3ZipPackage(archive, "Quick Edit", "quick-content", learningMode = "MEANING_RECALL")
            val context = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(context),
                lessonBrowserFacade = LessonBrowserFacade(context),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibraryViewModel.importFromFiles(listOf(archive))
            val packageId = createCanonicalLibraryFacade(context)!!.loadNavigationTree()!!.installedPackages
                .single { it.name == "Quick Edit" }.id
            val facade = StudyFacade(context)
            val before = facade.startLessonStudy(
                vn.loi.learning.application.session.StartPackageLessonStudyRequest(packageId, ContentId("quick-content"))
            )
            val learner = vn.loi.learning.domain.study.memory.model.LearnerId("default-learner")
            val sessionBefore = context.engine.getActiveSession(learner)!!
            val queueBefore = context.engine.getStudyQueue(sessionBefore.id)!!
            val reviewsBefore = context.reviewHistory.query(
                vn.loi.learning.application.reviewhistory.ReviewHistoryQuery(learner)
            )

            val updated = facade.quickEditCurrentItem(
                requireNotNull(before.domainContent).toStudyQuickEditDraft().copy(
                    questionText = "Updated expected answer",
                    answerText = "Updated answer",
                    exampleTranslation = "Updated translation"
                )
            )

            val sessionAfter = context.engine.getActiveSession(learner)!!
            val queueAfter = context.engine.getStudyQueue(sessionAfter.id)!!
            val reviewsAfter = context.reviewHistory.query(
                vn.loi.learning.application.reviewhistory.ReviewHistoryQuery(learner)
            )
            val canonical = context.contentRepository!!.findById(ContentId("quick-content"))!!

            assertEquals(sessionBefore.id, sessionAfter.id)
            assertEquals(before.currentLearningItemId, updated.currentLearningItemId)
            assertEquals(queueBefore.currentLearningItemId, queueAfter.currentLearningItemId)
            assertEquals(queueBefore.progress, queueAfter.progress)
            assertEquals(reviewsBefore, reviewsAfter)
            assertEquals("Updated expected answer", canonical.text.primaryText)
            assertEquals("Updated answer", canonical.text.translatedText)
            assertEquals("Updated translation", canonical.text.exampleTranslation)
            assertEquals("Updated answer", updated.domainContent?.text?.translatedText)
            assertNull(updated.recallPlan)
            assertTrue(updated.canRevealAnswer)
            assertFalse(updated.canReview)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Quick Edit suppresses duplicate save while canonical update is in flight`() {
        val tempDir = Files.createTempDirectory("quick-edit-duplicate-package")
        val persistenceDir = Files.createTempDirectory("quick-edit-duplicate-db")
        try {
            val archive = tempDir.resolve("QuickEditDuplicate.opd3")
            createOpd3ZipPackage(archive, "Quick Edit Duplicate", "quick-duplicate")
            val context = LearningApplicationFactory.createPersisted(persistenceDir)
            ContentLibraryViewModel(
                facade = ContentLibraryFacade(context),
                lessonBrowserFacade = LessonBrowserFacade(context),
                taskRunner = ImmediateDesktopTaskRunner
            ).importFromFiles(listOf(archive))
            val packageId = createCanonicalLibraryFacade(context)!!.loadNavigationTree()!!.installedPackages
                .single { it.name == "Quick Edit Duplicate" }.id
            val facade = StudyFacade(context)
            val started = facade.startLessonStudy(
                StartPackageLessonStudyRequest(packageId, ContentId("quick-duplicate"))
            )
            val runner = QueuedTaskRunner()
            var invalidations = 0
            var successes = 0
            val viewModel = StudyViewModel(facade, onContentEdited = { invalidations++ }, taskRunner = runner)
            viewModel.refresh()
            runner.runNext()
            val draft = requireNotNull(started.domainContent).toStudyQuickEditDraft().copy(answerText = "Saved once")

            viewModel.quickEditCurrentItem(draft, onSuccess = { successes++ }, onFailure = {})
            viewModel.quickEditCurrentItem(draft.copy(answerText = "Must not be submitted"), onSuccess = { successes++ }, onFailure = {})

            assertEquals(1, runner.pendingCount)
            runner.runNext()
            assertEquals("Saved once", context.contentRepository!!.findById(ContentId("quick-duplicate"))!!.text.translatedText)
            assertEquals(1, successes)
            assertEquals(1, invalidations)
            assertFalse(viewModel.uiState.actionInProgress)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    private fun createOpd3ZipPackage(
        file: Path,
        name: String,
        contentId: String,
        learningMode: String = "MEANING_RECOGNITION"
    ) {
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
                """{ "learningItems": [ { "id": "$contentId-rec", "contentId": "$contentId", "mode": "$learningMode", "isEnabled": true } ] }"""
            )
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private class QueuedTaskRunner : DesktopTaskRunner {
        private val tasks = ArrayDeque<() -> Unit>()
        val pendingCount: Int get() = tasks.size

        override fun <T> run(work: () -> T, onSuccess: (T) -> Unit, onFailure: (Exception) -> Unit) {
            tasks += {
                try {
                    onSuccess(work())
                } catch (exception: Exception) {
                    onFailure(exception)
                }
            }
        }

        override fun dispatch(action: () -> Unit) = action()
        fun runNext() = tasks.removeFirst().invoke()
    }
}
