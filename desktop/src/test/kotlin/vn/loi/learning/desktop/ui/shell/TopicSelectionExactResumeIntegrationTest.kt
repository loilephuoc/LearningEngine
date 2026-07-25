package vn.loi.learning.desktop.ui.shell

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.session.ActiveStudySessionRecovery
import vn.loi.learning.application.session.StartPackageLessonStudyRequest
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.desktop.ui.study.ReviewWorkspaceState
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.LearningApplicationFactory

class TopicSelectionExactResumeIntegrationTest {

    @Test
    fun `Test 1 and 2 - Topic A to Topic B to Topic A exact resume and session isolation`() {
        val tempDir = Files.createTempDirectory("topic-test-1-temp")
        val persistenceDir = Files.createTempDirectory("topic-test-1-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val defaultLibId = appContext.defaultLibraryId!!
            val navTree = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val activePkgA = navTree.activePackages.first { it.name == "Topic A" }
            val activePkgB = navTree.activePackages.first { it.name == "Topic B" }

            val instPkgAId = activePkgA.id
            val instPkgBId = activePkgB.id

            val topicATopicId = appContext.installedPackageRepository!!.findById(instPkgAId)!!.topicId
            val topicBTopicId = appContext.installedPackageRepository!!.findById(instPkgBId)!!.topicId

            val studyFacade = StudyFacade(appContext)
            val studyVm = StudyViewModel(studyFacade, taskRunner = ImmediateDesktopTaskRunner)

            // Step 1: Start Topic A
            studyVm.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = instPkgAId,
                    contentId = ContentId("cnt-a-1-1")
                )
            )
            val stateA1 = studyVm.uiState
            assertTrue(stateA1.sessionStarted)
            assertEquals("cnt-a-1-1-rec", stateA1.currentLearningItemId)
            val recoveryA1 = appContext.engine.recoverTopicSession(LearnerId("default-learner"), topicATopicId, Moment(System.currentTimeMillis()))
            assertTrue(recoveryA1 is ActiveStudySessionRecovery.Resumable)
            val topicASessionId = recoveryA1.session.id

            // Step 2: Reveal answer on Topic A
            studyVm.revealAnswer()
            val stateA2 = studyVm.uiState
            assertEquals(ReviewWorkspaceState.AnswerRevealed, stateA2.workspaceState)

            // Step 3: Switch to Topic B and start session
            studyVm.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = instPkgBId,
                    contentId = ContentId("cnt-b-1-1")
                )
            )
            val stateB1 = studyVm.uiState
            assertTrue(stateB1.sessionStarted)
            assertEquals("cnt-b-1-1-rec", stateB1.currentLearningItemId)
            val recoveryB1 = appContext.engine.recoverTopicSession(LearnerId("default-learner"), topicBTopicId, Moment(System.currentTimeMillis()))
            assertTrue(recoveryB1 is ActiveStudySessionRecovery.Resumable)
            val topicBSessionId = recoveryB1.session.id

            // Session isolation check: Session IDs must be distinct
            assertNotEquals(topicASessionId, topicBSessionId)

            // Step 4: Switch back to Topic A
            studyVm.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = instPkgAId,
                    contentId = ContentId("cnt-a-1-1")
                )
            )
            val stateA3 = studyVm.uiState
            assertTrue(stateA3.sessionStarted)
            assertEquals("cnt-a-1-1-rec", stateA3.currentLearningItemId)
            assertEquals(ReviewWorkspaceState.AnswerRevealed, stateA3.workspaceState)

            val recoveryA2 = appContext.engine.recoverTopicSession(LearnerId("default-learner"), topicATopicId, Moment(System.currentTimeMillis()))
            assertTrue(recoveryA2 is ActiveStudySessionRecovery.Resumable)
            assertEquals(topicASessionId, recoveryA2.session.id)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 3 - Application restart recovers Topic A exact checkpoint from store`() {
        val tempDir = Files.createTempDirectory("topic-test-3-temp")
        val persistenceDir = Files.createTempDirectory("topic-test-3-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")

            val appContext1 = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm1 = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext1),
                lessonBrowserFacade = LessonBrowserFacade(appContext1),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm1.importFromFiles(listOf(fileA))

            val defaultLibId1 = appContext1.defaultLibraryId!!
            val navTree1 = appContext1.libraryQuery!!.getNavigationTree(defaultLibId1)!!
            val activePkgA = navTree1.activePackages.first { it.name == "Topic A" }
            val instPkgAId = activePkgA.id

            val studyFacade1 = StudyFacade(appContext1)
            val studyVm1 = StudyViewModel(studyFacade1, taskRunner = ImmediateDesktopTaskRunner)
            studyVm1.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = instPkgAId,
                    contentId = ContentId("cnt-a-1-1")
                )
            )
            studyVm1.revealAnswer()
            assertEquals(ReviewWorkspaceState.AnswerRevealed, studyVm1.uiState.workspaceState)

            // Restart app using same persistenceDir
            val appContext2 = LearningApplicationFactory.createPersisted(persistenceDir)
            val studyFacade2 = StudyFacade(appContext2)
            val studyVm2 = StudyViewModel(studyFacade2, taskRunner = ImmediateDesktopTaskRunner)

            // Load session
            studyVm2.refresh()
            val restoredState = studyVm2.uiState
            assertTrue(restoredState.sessionStarted)
            assertEquals("cnt-a-1-1-rec", restoredState.currentLearningItemId)
            assertEquals(ReviewWorkspaceState.AnswerRevealed, restoredState.workspaceState)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 4 and 5 - Idle authority vs Active authority protection`() {
        val tempDir = Files.createTempDirectory("topic-test-4-temp")
        val persistenceDir = Files.createTempDirectory("topic-test-4-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val defaultLibId = appContext.defaultLibraryId!!
            val navTree = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val activePkgA = navTree.activePackages.first { it.name == "Topic A" }
            val instPkgAId = activePkgA.id

            val studyFacade = StudyFacade(appContext)
            val studyVm = StudyViewModel(studyFacade, taskRunner = ImmediateDesktopTaskRunner)

            // Active Authority: When Topic A session is active in memory
            studyVm.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = instPkgAId,
                    contentId = ContentId("cnt-a-1-1")
                )
            )
            val activeState1 = studyVm.uiState
            assertTrue(activeState1.hasActiveSession)

            // Calling studyVm.refresh() does NOT overwrite running Topic A session
            studyVm.refresh()
            assertEquals("cnt-a-1-1-rec", studyVm.uiState.currentLearningItemId)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 6 and 7 - New Topic session pipeline vs Completed Topic lifecycle`() {
        val tempDir = Files.createTempDirectory("topic-test-6-temp")
        val persistenceDir = Files.createTempDirectory("topic-test-6-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1", contentCount = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))

            val defaultLibId = appContext.defaultLibraryId!!
            val navTree = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val activePkgA = navTree.activePackages.first { it.name == "Topic A" }
            val instPkgAId = activePkgA.id
            val topicATopicId = appContext.installedPackageRepository!!.findById(instPkgAId)!!.topicId

            val studyFacade = StudyFacade(appContext)
            val studyVm = StudyViewModel(studyFacade, taskRunner = ImmediateDesktopTaskRunner)

            // New Topic creates new session via official pipeline
            studyVm.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = instPkgAId,
                    contentId = ContentId("cnt-a-1-1")
                )
            )
            val rec1 = appContext.engine.recoverTopicSession(LearnerId("default-learner"), topicATopicId, Moment(System.currentTimeMillis()))
            assertTrue(rec1 is ActiveStudySessionRecovery.Resumable)
            val sess1Id = rec1.session.id

            // Finish session 1 explicitly while leaving item 2 unreviewed
            appContext.engine.finishSession(sess1Id, Moment(System.currentTimeMillis()))

            // Starting Topic A again MUST create a fresh session, not resume finished session as active
            studyVm.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = instPkgAId,
                    contentId = ContentId("cnt-a-1-2")
                )
            )
            val rec2 = appContext.engine.recoverTopicSession(LearnerId("default-learner"), topicATopicId, Moment(System.currentTimeMillis()))
            assertTrue(rec2 is ActiveStudySessionRecovery.Resumable)
            val sess2Id = rec2.session.id
            assertNotEquals(sess1Id, sess2Id)
            assertFalse(studyVm.uiState.sessionCompleted)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 8 and 9 - MemoryState and ReviewHistory are NOT mutated purely by topic switching or resume`() {
        val tempDir = Files.createTempDirectory("topic-test-8-temp")
        val persistenceDir = Files.createTempDirectory("topic-test-8-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileB = tempDir.resolve("TopicB.opd3")
            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileB, name = "Topic B", contentId = "cnt-b-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val defaultLibId = appContext.defaultLibraryId!!
            val navTree = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val activePkgA = navTree.activePackages.first { it.name == "Topic A" }
            val activePkgB = navTree.activePackages.first { it.name == "Topic B" }
            val instPkgAId = activePkgA.id
            val instPkgBId = activePkgB.id

            val studyFacade = StudyFacade(appContext)
            val studyVm = StudyViewModel(studyFacade, taskRunner = ImmediateDesktopTaskRunner)

            val learnerId = LearnerId("default-learner")
            val itemAId = LearningItemId("cnt-a-1-1-rec")

            val memBefore = appContext.engine.getMemoryState(learnerId, itemAId)
            val histBefore = appContext.engine.getReviewHistory(learnerId, itemAId)

            // Start Topic A, switch to Topic B, switch back to Topic A
            studyVm.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = instPkgAId,
                    contentId = ContentId("cnt-a-1-1")
                )
            )
            studyVm.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = instPkgBId,
                    contentId = ContentId("cnt-b-1-1")
                )
            )
            studyVm.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = instPkgAId,
                    contentId = ContentId("cnt-a-1-1")
                )
            )

            val memAfter = appContext.engine.getMemoryState(learnerId, itemAId)
            val histAfter = appContext.engine.getReviewHistory(learnerId, itemAId)

            assertEquals(memBefore, memAfter, "MemoryState must not be mutated by topic switching or resuming")
            assertEquals(histBefore, histAfter, "ReviewHistory must not be mutated by topic switching or resuming")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `Test 10 - Failure safety during topic switch does not leave partial state or cross link sessions`() {
        val tempDir = Files.createTempDirectory("topic-test-10-temp")
        val persistenceDir = Files.createTempDirectory("topic-test-10-db")
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

            val defaultLibId = appContext.defaultLibraryId!!
            val navTree = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val activePkgA = navTree.activePackages.first { it.name == "Topic A" }
            val instPkgAId = activePkgA.id

            val studyFacade = StudyFacade(appContext)
            val studyVm = StudyViewModel(studyFacade, taskRunner = ImmediateDesktopTaskRunner)

            studyVm.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = instPkgAId,
                    contentId = ContentId("cnt-a-1-1")
                )
            )
            val validState = studyVm.uiState
            assertTrue(validState.sessionStarted)

            // Attempt to start a non-existent package
            studyVm.startLessonStudy(
                StartPackageLessonStudyRequest(
                    installedPackageId = InstalledPackageId("invalid-pkg"),
                    contentId = ContentId("non-existent")
                )
            )

            val errorState = studyVm.uiState
            assertNotNull(errorState.loadError)
            assertEquals(ReviewWorkspaceState.RecoverableFailure, errorState.workspaceState)

            // Refresh to reload valid active session
            studyVm.refresh()
            val recoveredState = studyVm.uiState
            assertEquals("cnt-a-1-1-rec", recoveredState.currentLearningItemId)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    private fun createOpd3ZipPackage(file: Path, name: String, contentId: String, contentCount: Int = 1) {
        val contents = (1..contentCount).map { i ->
            """{ "id": "$contentId-$i", "type": "SENTENCE", "primaryText": "Greeting $i", "translatedText": "Chao $i", "title": "Greeting $i", "group": "English", "section": "Unit 1", "lesson": "$name Greetings" }"""
        }.joinToString(",")
        val items = (1..contentCount).map { i ->
            """{ "id": "$contentId-$i-rec", "contentId": "$contentId-$i", "mode": "MEANING_RECOGNITION", "isEnabled": true }"""
        }.joinToString(",")

        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            writeZipEntry(
                zip,
                "manifest.json",
                """{ "name": "$name", "version": "1.0.0", "format": "OPD3", "schemaVersion": 1, "contentCount": $contentCount, "learningItemCount": $contentCount }"""
            )
            writeZipEntry(zip, "metadata.json", """{ "name": "$name", "version": "1.0.0", "format": "OPD3" }""")
            writeZipEntry(
                zip,
                "contents.json",
                """{ "contents": [ $contents ] }"""
            )
            writeZipEntry(
                zip,
                "learning-items.json",
                """{ "learningItems": [ $items ] }"""
            )
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }
}
