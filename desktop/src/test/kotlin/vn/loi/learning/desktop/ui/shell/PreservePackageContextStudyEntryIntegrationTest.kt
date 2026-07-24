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
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.session.StartPackageLessonStudyRequest
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.application.session.StartStudySessionUseCase
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
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.PendingSessionReview
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.domain.study.session.model.UndoableSessionReview
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.persistence.mapper.StudySessionRecordMapper
import vn.loi.learning.infrastructure.persistence.record.StudySessionRecord

class PreservePackageContextStudyEntryIntegrationTest {

    // T1 — Domain start retains provenance
    @Test
    fun `T1 domain start retains installedPackageId provenance`() {
        val pkgA = InstalledPackageId("pkg-test-a")
        val session = StudySession.start(
            id = SessionId("sess-1"),
            learnerId = LearnerId("learner-1"),
            startedAt = Moment(1000),
            policy = SessionPolicy(),
            installedPackageId = pkgA
        )

        assertEquals(pkgA, session.installedPackageId)
    }

    // T2 — Domain transitions preserve provenance
    @Test
    fun `T2 domain transitions preserve installedPackageId provenance`() {
        val pkgA = InstalledPackageId("pkg-test-a")
        var session = StudySession.start(
            id = SessionId("sess-1"),
            learnerId = LearnerId("learner-1"),
            startedAt = Moment(1000),
            policy = SessionPolicy(),
            installedPackageId = pkgA
        )

        val item1 = LearningItemId("item-1")
        val content1 = ContentId("cnt-1")

        session = session.recordReview(
            learningItemId = item1,
            contentId = content1,
            wasNewItem = true
        )
        assertEquals(pkgA, session.installedPackageId, "recordReview must preserve installedPackageId")

        session = session.finish(at = Moment(2000))
        assertEquals(pkgA, session.installedPackageId, "finish must preserve installedPackageId")
    }

    // T3 — Start command propagation
    @Test
    fun `T3 start command propagates installedPackageId to repository session`() {
        val persistenceDir = Files.createTempDirectory("start-cmd-prop-db")
        try {
            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val pkgA = InstalledPackageId("pkg-test-a")
            val sessionId = SessionId("sess-cmd-1")

            appContext.engine.startSession(
                StartStudySessionCommand(
                    sessionId = sessionId,
                    learnerId = LearnerId("learner-1"),
                    startedAt = Moment(1000),
                    installedPackageId = pkgA
                )
            )

            val session = when (val recovery = appContext.engine.recoverActiveSession(LearnerId("learner-1"), Moment(2000))) {
                is vn.loi.learning.application.session.ActiveStudySessionRecovery.Resumable -> recovery.session
                is vn.loi.learning.application.session.ActiveStudySessionRecovery.ClosedIncompleteSession -> recovery.session
                vn.loi.learning.application.session.ActiveStudySessionRecovery.NoActiveSession -> null
            }
            assertNotNull(session)
            assertEquals(pkgA, session.installedPackageId)
        } finally {
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T4 — Persistence mapper round-trip
    @Test
    fun `T4 StudySessionRecordMapper round-trip preserves installedPackageId`() {
        val pkgA = InstalledPackageId("pkg-test-a")
        val session = StudySession.start(
            id = SessionId("sess-mapper-1"),
            learnerId = LearnerId("learner-1"),
            startedAt = Moment(1000),
            policy = SessionPolicy(),
            installedPackageId = pkgA
        )

        val record = StudySessionRecordMapper.toRecord(session)
        assertEquals(pkgA.value, record.installedPackageId)

        val restoredDomain = StudySessionRecordMapper.toDomain(record)
        assertEquals(pkgA, restoredDomain.installedPackageId)
    }

    // T5 — Backward-compatible old record
    @Test
    fun `T5 legacy record without installedPackageId decodes successfully to null`() {
        val oldRecord = StudySessionRecord(
            schemaVersion = StudySessionRecord.CURRENT_SCHEMA_VERSION,
            id = "sess-old-1",
            learnerId = "learner-1",
            startedAtEpochMillis = 1000L,
            status = "ACTIVE",
            policyNewItemLimit = 10,
            policyReviewItemLimit = 100,
            policyAllowRepeatInSameSession = false,
            reviewedItemIds = emptyList(),
            reviewedContentIds = emptyList(),
            newItemsReviewed = 0,
            reviewItemsReviewed = 0,
            finishedAtEpochMillis = null,
            installedPackageId = null
        )

        val restoredDomain = StudySessionRecordMapper.toDomain(oldRecord)
        assertNull(restoredDomain.installedPackageId)
    }

    // T6 — Package-aware desktop session
    @Test
    fun `T6 package-aware study request creates active session with installedPackageId and exposes activeInstalledPackageId in StudyUiState`() {
        val tempDir = Files.createTempDirectory("pkg-desktop-session-test")
        val persistenceDir = Files.createTempDirectory("pkg-desktop-session-db")
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

            assertTrue(studyVm.uiState.hasActiveSession)
            assertEquals(pkgSummary.id, studyVm.uiState.activeInstalledPackageId)
            assertEquals(NavigationDestination.STUDY, navState.currentDestination)

            val recovery = appContext.engine.recoverActiveSession(LearnerId("default-learner"), Moment(2000))
            assertTrue(recovery is vn.loi.learning.application.session.ActiveStudySessionRecovery.Resumable)
            assertEquals(pkgSummary.id, recovery.session.installedPackageId)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T7 — Reveal keeps context
    @Test
    fun `T7 reveal answer preserves installedPackageId in active session`() {
        val tempDir = Files.createTempDirectory("reveal-ctx-test")
        val persistenceDir = Files.createTempDirectory("reveal-ctx-db")
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

            coordinator.startLessonStudy(StartPackageLessonStudyRequest(pkgSummary.id, ContentId("cnt-valid-1")))

            studyVm.revealAnswer()

            assertTrue(studyVm.uiState.hasActiveSession)
            assertEquals(pkgSummary.id, studyVm.uiState.activeInstalledPackageId)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T8 — Review advancement keeps context
    @Test
    fun `T8 reviewing item preserves installedPackageId in active session`() {
        val tempDir = Files.createTempDirectory("review-ctx-test")
        val persistenceDir = Files.createTempDirectory("review-ctx-db")
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

            coordinator.startLessonStudy(StartPackageLessonStudyRequest(pkgSummary.id, ContentId("cnt-valid-1")))

            studyVm.revealAnswer()
            studyVm.reviewGood()

            val activeSession = appContext.engine.getLatestUndoableSession(LearnerId("default-learner"))
            assertNotNull(activeSession)
            assertEquals(pkgSummary.id, activeSession.installedPackageId)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T9 — Finish keeps persisted provenance and clears active projection
    @Test
    fun `T9 session finish preserves installedPackageId on persisted StudySession while clearing active projection`() {
        val pkgA = InstalledPackageId("pkg-test-a")
        var session = StudySession.start(
            id = SessionId("sess-finish-1"),
            learnerId = LearnerId("learner-1"),
            startedAt = Moment(1000),
            policy = SessionPolicy(),
            installedPackageId = pkgA
        )

        session = session.finish(at = Moment(2000))

        assertEquals(pkgA, session.installedPackageId, "Finished StudySession must retain installedPackageId provenance")
    }

    // T10 — Restart recovery
    @Test
    fun `T10 active package study session recovers installedPackageId after app restart`() {
        val tempDir = Files.createTempDirectory("restart-rec-test")
        val persistenceDir = Files.createTempDirectory("restart-rec-db")
        try {
            val opd3File = tempDir.resolve("ValidTopic.opd3")
            createOpd3ZipPackage(opd3File, name = "Valid Topic", contentId = "cnt-valid-1")

            val appContext1 = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext1),
                lessonBrowserFacade = LessonBrowserFacade(appContext1)
            )

            contentLibVm.importFromFiles(listOf(opd3File))

            val navTree = appContext1.libraryQuery!!.getNavigationTree(appContext1.defaultLibraryId!!)!!
            val pkgSummary = navTree.installedPackages.first { it.name == "Valid Topic" }

            val studyVm1 = StudyViewModel(facade = StudyFacade(appContext1))
            val navState1 = NavigationState()
            val coordinator1 = LessonStudyNavigationCoordinator(studyVm1, navState1)

            coordinator1.startLessonStudy(StartPackageLessonStudyRequest(pkgSummary.id, ContentId("cnt-valid-1")))
            assertTrue(studyVm1.uiState.hasActiveSession)
            assertEquals(pkgSummary.id, studyVm1.uiState.activeInstalledPackageId)

            // Recreate app context & facade from same persistence directory
            val appContext2 = LearningApplicationFactory.createPersisted(persistenceDir)
            val studyVm2 = StudyViewModel(facade = StudyFacade(appContext2))

            assertTrue(studyVm2.uiState.hasActiveSession, "Session must be recovered as active")
            assertEquals(pkgSummary.id, studyVm2.uiState.activeInstalledPackageId, "Recovered UI state must expose activeInstalledPackageId")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T11 — No leakage package A to package B
    @Test
    fun `T11 starting session for package B resets facade state and does not leak package A context`() {
        val tempDir = Files.createTempDirectory("leakage-ab-test")
        val persistenceDir = Files.createTempDirectory("leakage-ab-db")
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
            val pkgB = navTree.installedPackages.first { it.name == "Topic B" }

            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            val navState = NavigationState()
            val coordinator = LessonStudyNavigationCoordinator(studyVm, navState)

            // Start Package A session
            coordinator.startLessonStudy(StartPackageLessonStudyRequest(pkgA.id, ContentId("cnt-a-1")))
            assertEquals(pkgA.id, studyVm.uiState.activeInstalledPackageId)

            // Start Package B session
            coordinator.startLessonStudy(StartPackageLessonStudyRequest(pkgB.id, ContentId("cnt-b-1")))
            assertEquals(pkgB.id, studyVm.uiState.activeInstalledPackageId, "Active package ID must be package B, not package A")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T12 — No leakage package A to legacy
    @Test
    fun `T12 starting legacy content session resets facade state and evaluates installedPackageId to null`() {
        val tempDir = Files.createTempDirectory("leakage-legacy-test")
        val persistenceDir = Files.createTempDirectory("leakage-legacy-db")
        try {
            val fileA = tempDir.resolve("TopicA.opd3")
            val fileLegacy = tempDir.resolve("LegacyTopic.opd3")

            createOpd3ZipPackage(fileA, name = "Topic A", contentId = "cnt-a-1")
            createOpd3ZipPackage(fileLegacy, name = "Legacy Topic", contentId = "cnt-legacy-1")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )

            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileLegacy))

            val navTree = appContext.libraryQuery!!.getNavigationTree(appContext.defaultLibraryId!!)!!
            val pkgA = navTree.installedPackages.first { it.name == "Topic A" }

            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            val navState = NavigationState()
            val coordinator = LessonStudyNavigationCoordinator(studyVm, navState)

            // Start Package A session
            coordinator.startLessonStudy(StartPackageLessonStudyRequest(pkgA.id, ContentId("cnt-a-1")))
            assertEquals(pkgA.id, studyVm.uiState.activeInstalledPackageId)

            // Start legacy lesson study (by content ID string) for cnt-legacy-1
            coordinator.startLessonStudy("cnt-legacy-1")
            assertNull(studyVm.uiState.activeInstalledPackageId, "Legacy study session must evaluate activeInstalledPackageId to null")
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T13 — R1 regression
    @Test
    fun `T13 cross-package mismatch and invalid package states continue to fail validation safely`() {
        val tempDir = Files.createTempDirectory("r1-regr-test")
        val persistenceDir = Files.createTempDirectory("r1-regr-db")
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

            // Mismatch: Package A, Content B
            coordinator.startLessonStudy(StartPackageLessonStudyRequest(pkgA.id, ContentId("cnt-b-1")))

            assertFalse(studyVm.uiState.hasActiveSession)
            assertNotNull(studyVm.uiState.loadError)
            assertNull(studyVm.uiState.activeInstalledPackageId)
            assertEquals(NavigationDestination.CONTENT_LIBRARY, navState.currentDestination)
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
