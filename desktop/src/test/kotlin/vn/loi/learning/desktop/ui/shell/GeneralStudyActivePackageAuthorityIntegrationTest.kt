package vn.loi.learning.desktop.ui.shell

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.UninstallContentPackageCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.LearningApplicationFactory

class GeneralStudyActivePackageAuthorityIntegrationTest {

    @Test
    fun `AC-08 - General Study active package authority, stale session rejection, and app restart`() {
        val tempDir = Files.createTempDirectory("general-study-authority-temp")
        val persistenceDir = Files.createTempDirectory("general-study-authority-db")
        try {
            val dirA = Files.createDirectory(tempDir.resolve("pkgA"))
            val dirB = Files.createDirectory(tempDir.resolve("pkgB"))
            val dirC = Files.createDirectory(tempDir.resolve("pkgC"))

            val fileA = dirA.resolve("PackageA.opd3")
            val fileB = dirB.resolve("PackageB.opd3")
            val fileC = dirC.resolve("PackageC.opd3")

            createOpd3ZipPackage(fileA, name = "Package A", contentId = "cnt-pkg-a")
            createOpd3ZipPackage(fileB, name = "Package B", contentId = "cnt-pkg-b")
            createOpd3ZipPackage(fileC, name = "Package C", contentId = "cnt-pkg-c", contentCount = 3)

            // Phase 1: Initialize context and import A, B, C
            val appContext1 = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm1 = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext1),
                lessonBrowserFacade = LessonBrowserFacade(appContext1),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm1.importFromDirectory(dirA)
            contentLibVm1.importFromDirectory(dirB)
            contentLibVm1.importFromDirectory(dirC)

            val defaultLibId = appContext1.defaultLibraryId!!
            val navTree1 = appContext1.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val pkgAInfo = navTree1.activePackages.first { it.name == "Package A" }
            val pkgBInfo = navTree1.activePackages.first { it.name == "Package B" }
            val pkgCInfo = navTree1.activePackages.first { it.name == "Package C" }

            val pkgAId = pkgAInfo.id
            val pkgBId = pkgBInfo.id
            val pkgCId = pkgCInfo.id

            // Simulate historical active session for Package C, then uninstall Package C
            val learnerId = LearnerId("default-learner")
            val staleSessionIdC = SessionId(UUID.randomUUID().toString())
            appContext1.engine.startSession(
                StartStudySessionCommand(
                    sessionId = staleSessionIdC,
                    learnerId = learnerId,
                    startedAt = Moment(System.currentTimeMillis()),
                    installedPackageId = pkgCId
                )
            )

            // Uninstall Package C so it becomes a stale historical package
            appContext1.uninstallContentPackage!!.execute(
                UninstallContentPackageCommand(
                    catalogId = PackageCatalogId("desktop-content-library"),
                    packageId = pkgCInfo.packageId
                )
            )
            val navTreeAfterUninstall = appContext1.libraryQuery!!.getNavigationTree(defaultLibId)!!
            assertFalse(navTreeAfterUninstall.activePackages.any { it.name == "Package C" })

            // Set Package A as current active package in Library
            appContext1.libraryCommand!!.setActivePackage(defaultLibId, pkgAId)
            val navTreeActiveA = appContext1.libraryQuery!!.getNavigationTree(defaultLibId)!!
            assertEquals(pkgAId, navTreeActiveA.activePackageId)

            val studyFacade1 = StudyFacade(appContext1)
            val studyVm1 = StudyViewModel(studyFacade1, taskRunner = ImmediateDesktopTaskRunner)

            // Step 1: Open Study while Package A is active -> Start Study must enter Package A, NOT Package C
            studyVm1.refresh()
            val idleStateA = studyVm1.uiState
            assertEquals(pkgAId, idleStateA.activeInstalledPackageId)

            studyVm1.startStudy()
            val activeStateA = studyVm1.uiState
            assertTrue(activeStateA.sessionStarted)
            assertEquals(pkgAId, activeStateA.activeInstalledPackageId)
            val itemAId = activeStateA.currentLearningItemId
            assertNotNull(itemAId)
            assertTrue(itemAId.startsWith("cnt-pkg-a"))
            assertFalse(itemAId.contains("cnt-pkg-c"))

            // Reveal answer and finish session A so Study returns to idle
            studyVm1.revealAnswer()
            studyVm1.reviewGood()

            // Step 2: Switch Library Active Package to B -> Open Study and Start Study -> must enter Package B
            appContext1.libraryCommand!!.setActivePackage(defaultLibId, pkgBId)
            val navTreeActiveB = appContext1.libraryQuery!!.getNavigationTree(defaultLibId)!!
            assertEquals(pkgBId, navTreeActiveB.activePackageId)

            studyVm1.refresh()
            val idleStateB = studyVm1.uiState
            assertEquals(pkgBId, idleStateB.activeInstalledPackageId)

            studyVm1.startStudy()
            val activeStateB = studyVm1.uiState
            assertTrue(activeStateB.sessionStarted)
            assertEquals(pkgBId, activeStateB.activeInstalledPackageId)
            val itemBId = activeStateB.currentLearningItemId
            assertNotNull(itemBId)
            assertTrue(itemBId.startsWith("cnt-pkg-b"))
            assertFalse(itemBId.contains("cnt-pkg-c"))

            // Step 3: Switching canonical ACTIVE package invalidates the in-memory session from B.
            appContext1.libraryCommand!!.setActivePackage(defaultLibId, pkgAId)
            studyVm1.refresh()
            val canonicalIdleStateA = studyVm1.uiState
            assertFalse(canonicalIdleStateA.sessionStarted)
            assertEquals(pkgAId, canonicalIdleStateA.activeInstalledPackageId)
            assertNull(canonicalIdleStateA.currentLearningItemId)

            // Step 4: Same-runtime paused session + Library active package switch (PO UAT Scenario)
            // Start Session A, then simulate clicking Pause (leaving Study screen to go to Library)
            studyVm1.startStudy()
            val activeStateA2 = studyVm1.uiState
            assertTrue(activeStateA2.sessionStarted)
            assertEquals(pkgAId, activeStateA2.activeInstalledPackageId)

            // User clicks "Tạm dừng" / leaves Study screen (clear active session state for General Study)
            studyFacade1.dismissCompletionPresentation()

            // Set Package B as active in Library
            appContext1.libraryCommand!!.setActivePackage(defaultLibId, pkgBId)
            // Re-navigate to Study and refresh in SAME runtime (NO restart)
            studyVm1.refresh()
            val sameRuntimeIdleState = studyVm1.uiState
            assertEquals(pkgBId, sameRuntimeIdleState.activeInstalledPackageId)

            studyVm1.startStudy()
            val sameRuntimeActiveState = studyVm1.uiState
            assertTrue(sameRuntimeActiveState.sessionStarted)
            assertEquals(pkgBId, sameRuntimeActiveState.activeInstalledPackageId)
            val sameRuntimeItemId = sameRuntimeActiveState.currentLearningItemId
            assertNotNull(sameRuntimeItemId)
            assertTrue(sameRuntimeItemId.startsWith("cnt-pkg-b"))

            // Step 5: App restart -> Reload fresh context and verify active package authority
            val appContext2 = LearningApplicationFactory.createPersisted(persistenceDir)
            val studyFacade2 = StudyFacade(appContext2)
            val studyVm2 = StudyViewModel(studyFacade2, taskRunner = ImmediateDesktopTaskRunner)

            studyVm2.refresh()
            val restartedIdleState = studyVm2.uiState
            assertEquals(pkgBId, restartedIdleState.activeInstalledPackageId)
            assertTrue(restartedIdleState.sessionStarted)

            // Step 6: AC-06 - Test hidden installed package / orphaned content re-import lifecycle
            // Uninstall Package B so it is removed from Library
            appContext2.uninstallContentPackage!!.execute(
                UninstallContentPackageCommand(
                    catalogId = PackageCatalogId("desktop-content-library"),
                    packageId = pkgBInfo.packageId
                )
            )
            val navTreeAfterRemoveB = appContext2.libraryQuery!!.getNavigationTree(defaultLibId)!!
            assertFalse(navTreeAfterRemoveB.activePackages.any { it.name == "Package B" })
            assertFalse(navTreeAfterRemoveB.installedPackages.any { it.name == "Package B" })

            // Re-import Package B from fileB
            val contentLibVm2 = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext2),
                lessonBrowserFacade = LessonBrowserFacade(appContext2),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm2.importFromDirectory(dirB)

            // Re-import MUST succeed cleanly and Package B MUST appear in installed/active packages!
            val navTreeAfterReimportB = appContext2.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val reimportedB = navTreeAfterReimportB.installedPackages.firstOrNull { it.name == "Package B" }
            assertNotNull(reimportedB)
            assertTrue(navTreeAfterReimportB.activePackages.any { it.name == "Package B" })
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `AC-06 - Complete removed package reimport lifecycle with progress reconnection`() {
        val tempDir = Files.createTempDirectory("removed-pkg-reimport-temp")
        val persistenceDir = Files.createTempDirectory("removed-pkg-reimport-db")
        try {
            val dirA = Files.createDirectory(tempDir.resolve("pkgA"))
            val dirB = Files.createDirectory(tempDir.resolve("pkgB"))
            val dirC = Files.createDirectory(tempDir.resolve("pkgC"))

            val fileA = dirA.resolve("PackageA.opd3")
            val fileB = dirB.resolve("PackageB.opd3")
            val fileC = dirC.resolve("PackageC.opd3")

            createOpd3ZipPackage(fileA, name = "Package A", contentId = "cnt-pkg-a")
            createOpd3ZipPackage(fileB, name = "Package B", contentId = "cnt-pkg-b")
            createOpd3ZipPackage(fileC, name = "Package C", contentId = "cnt-pkg-c", contentCount = 3)

            // 1. Import Packages A, B, C
            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            contentLibVm.importFromDirectory(dirA)
            contentLibVm.importFromDirectory(dirB)
            contentLibVm.importFromDirectory(dirC)

            val defaultLibId = appContext.defaultLibraryId!!
            val navTreeInitial = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val pkgCInfo = navTreeInitial.installedPackages.first { it.name == "Package C" }
            val pkgCId = pkgCInfo.id

            // 2. Study item in Package C to generate MemoryState and ReviewHistory
            appContext.libraryCommand!!.setActivePackage(defaultLibId, pkgCId)
            val studyFacade = StudyFacade(appContext)
            val studyVm = StudyViewModel(studyFacade, taskRunner = ImmediateDesktopTaskRunner)
            studyVm.refresh()
            studyVm.startStudy()
            val activeState = studyVm.uiState
            assertTrue(activeState.sessionStarted)
            assertEquals(pkgCId, activeState.activeInstalledPackageId)

            studyVm.revealAnswer()
            studyVm.reviewGood()

            // Verify MemoryState exists for Package C item
            val learnerId = LearnerId("default-learner")
            val itemId = vn.loi.learning.domain.study.learning.model.LearningItemId("cnt-pkg-c-1-rec")
            val initialMemoryState = appContext.engine.getMemoryState(learnerId, itemId)
            assertNotNull(initialMemoryState)

            // 3. Remove package C via real composition boundary (UninstallContentPackageUseCase)
            appContext.uninstallContentPackage!!.execute(
                UninstallContentPackageCommand(
                    catalogId = PackageCatalogId("desktop-content-library"),
                    packageId = pkgCInfo.packageId
                )
            )

            // 4. Verify Library only shows A and B; C is not in Active, Installed, or Archived
            val navTreeAfterRemove = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            assertFalse(navTreeAfterRemove.installedPackages.any { it.name == "Package C" })
            assertFalse(navTreeAfterRemove.activePackages.any { it.name == "Package C" })
            assertFalse(navTreeAfterRemove.archivedPackages.any { it.name == "Package C" })
            assertEquals(2, navTreeAfterRemove.installedPackages.size)

            // 5. Verify persisted installedPackageRepository no longer has Package C
            val instPkgC = appContext.installedPackageRepository!!.findById(pkgCId)
            assertTrue(instPkgC == null || instPkgC.state == vn.loi.learning.domain.library.model.PackageState.REMOVED)

            // 6. Import Package C again in the SAME data directory
            contentLibVm.importFromDirectory(dirC)

            // 7. Import MUST succeed cleanly
            val navTreeAfterReimport = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val reimportedC = navTreeAfterReimport.installedPackages.firstOrNull { it.name == "Package C" }
            assertNotNull(reimportedC)

            // 8. C appears in Library
            assertTrue(navTreeAfterReimport.activePackages.any { it.name == "Package C" })

            // 9. MemoryState and ReviewHistory are wiped on topic removal, reimport starts fresh
            val reconnectedMemoryState = appContext.engine.getMemoryState(learnerId, itemId)
            assertNull(reconnectedMemoryState, "Re-imported package must start as fresh NEW state without prior MemoryState")


            // 10. No duplicate Content or LearningItem
            val cContents = appContext.engine.getAllContent().filter { it.id.value.startsWith("cnt-pkg-c") }
            assertEquals(3, cContents.size)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `reconcile orphan content ownership during package reimport and reject active package duplicates`() {
        val tempDir = Files.createTempDirectory("orphan-reimport-temp")
        val persistenceDir = Files.createTempDirectory("orphan-reimport-db")
        try {
            val dirA = Files.createDirectory(tempDir.resolve("pkgA"))
            val dirOrphan = Files.createDirectory(tempDir.resolve("pkgOrphan"))

            val fileA = dirA.resolve("PackageA.opd3")
            val fileOrphan = dirOrphan.resolve("PackageOrphan.opd3")

            createOpd3ZipPackage(fileA, name = "Package A", contentId = "cnt-pkg-a")
            createOpd3ZipPackage(fileOrphan, name = "Package Orphan", contentId = "cnt-pkg-orphan", contentCount = 3)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext),
                taskRunner = ImmediateDesktopTaskRunner
            )

            // Import Package A (Active) and Package Orphan
            contentLibVm.importFromDirectory(dirA)
            contentLibVm.importFromDirectory(dirOrphan)

            val defaultLibId = appContext.defaultLibraryId!!
            val navTreeInitial = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            val orphanInstPkg = navTreeInitial.installedPackages.first { it.name == "Package Orphan" }

            val instPkgRepo = appContext.installedPackageRepository!!
            instPkgRepo.delete(orphanInstPkg.id)
            val libRepo = appContext.domainLibraryRepository!!
            val currentLib = libRepo.findById(defaultLibId)!!
            val updatedLib = vn.loi.learning.domain.library.model.Library.reconstitute(
                id = currentLib.id,
                name = currentLib.name,
                entries = currentLib.entries.filterNot { it.installedPackageId == orphanInstPkg.id },
                activePackageId = currentLib.activePackageId?.takeIf { it != orphanInstPkg.id },
                createdAt = currentLib.createdAt
            )
            libRepo.save(updatedLib)

            // Verify Legacy Orphan State:
            // InstalledPackage: absent
            // Library: absent
            // ContentPackage: present
            // ContentLibrary: present
            // Content: present
            // LearningItems: present
            assertNull(instPkgRepo.findById(orphanInstPkg.id))
            val navTreeOrphanState = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            assertNotNull(appContext.contentPackageRepository!!.findById(orphanInstPkg.packageId))
            assertTrue(appContext.engine.getAllContent().any { it.id.value.startsWith("cnt-pkg-orphan") })

            // Test 1: Re-import Package Orphan -> MUST PASS cleanly
            contentLibVm.importFromDirectory(dirOrphan)

            val navTreeAfterOrphanReimport = appContext.libraryQuery!!.getNavigationTree(defaultLibId)!!
            assertTrue(navTreeAfterOrphanReimport.installedPackages.any { it.name == "Package Orphan" })
            assertNull(contentLibVm.uiState.importError)

            // Test 2: Re-import Package A (which IS ACTIVE in InstalledPackageRepository with duplicate content) -> MUST FAIL
            contentLibVm.importFromDirectory(dirA)

            val errorMsg = contentLibVm.uiState.importError
            assertNotNull(errorMsg)
            assertTrue(errorMsg.contains("(State: ACTIVE)"))
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `orphan reimport purges stale completed lifecycle and starts fresh discovery`() {
        val packageDirectory = Files.createTempDirectory("stale-completion-package")
        val persistenceDirectory = Files.createTempDirectory("stale-completion-db")
        try {
            createOpd3ZipPackage(
                file = packageDirectory.resolve("PackageA.opd3"),
                name = "Package A",
                contentId = "cnt-pkg-a",
                contentCount = 4
            )
            val initialContext = LearningApplicationFactory.createPersisted(persistenceDirectory)
            val initialLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(initialContext),
                lessonBrowserFacade = LessonBrowserFacade(initialContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            initialLibraryViewModel.importFromDirectory(packageDirectory)

            val defaultLibraryId = initialContext.defaultLibraryId!!
            val initialPackage = initialContext.libraryQuery!!
                .getNavigationTree(defaultLibraryId)!!
                .installedPackages
                .single { it.name == "Package A" }
            initialContext.libraryCommand!!.setActivePackage(defaultLibraryId, initialPackage.id)

            val initialStudy = StudyViewModel(
                StudyFacade(initialContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            initialStudy.refresh()
            initialStudy.startStudy()
            repeat(4) {
                assertFalse(initialStudy.uiState.sessionCompleted)
                initialStudy.revealAnswer()
                initialStudy.reviewGood()
            }
            assertTrue(initialStudy.uiState.sessionCompleted)
            assertEquals(4, initialStudy.uiState.reviewedCount)
            assertTrue(initialStudy.uiState.canUndo)

            val staleSession = initialContext.studySessionRepository!!.findAll()
                .single { it.status == SessionStatus.FINISHED && it.installedPackageId == initialPackage.id }
            assertNotNull(initialContext.studyQueueRepository!!.findBySessionId(staleSession.id))
            val unrelatedSessionId = SessionId("unrelated-package-b-completion")
            initialContext.studySessionRepository!!.save(
                StudySession.start(
                    id = unrelatedSessionId,
                    learnerId = LearnerId("package-b-learner"),
                    startedAt = staleSession.startedAt,
                    policy = SessionPolicy(),
                    topicId = vn.loi.learning.domain.content.topic.model.TopicId("package-b-topic"),
                    installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId(
                        "inst-package-b"
                    )
                ).finish(Moment(staleSession.startedAt.epochMillis + 1))
            )
            initialContext.studyQueueRepository!!.save(
                StudyQueueSnapshot(
                    sessionId = unrelatedSessionId,
                    createdAt = staleSession.startedAt,
                    learningItemIds = emptyList()
                )
            )

            initialContext.installedPackageRepository!!.delete(initialPackage.id)
            val canonicalLibrary = initialContext.domainLibraryRepository!!.findById(defaultLibraryId)!!
            initialContext.domainLibraryRepository!!.save(
                vn.loi.learning.domain.library.model.Library.reconstitute(
                    id = canonicalLibrary.id,
                    name = canonicalLibrary.name,
                    entries = canonicalLibrary.entries.filterNot { it.installedPackageId == initialPackage.id },
                    activePackageId = canonicalLibrary.activePackageId?.takeIf { it != initialPackage.id },
                    createdAt = canonicalLibrary.createdAt
                )
            )

            val restartedContext = LearningApplicationFactory.createPersisted(persistenceDirectory)
            val restartedLibraryViewModel = ContentLibraryViewModel(
                facade = ContentLibraryFacade(restartedContext),
                lessonBrowserFacade = LessonBrowserFacade(restartedContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            restartedLibraryViewModel.importFromDirectory(packageDirectory)
            assertNull(restartedLibraryViewModel.uiState.importError)

            val reinstalledPackage = restartedContext.libraryQuery!!
                .getNavigationTree(defaultLibraryId)!!
                .installedPackages
                .single { it.name == "Package A" }
            assertEquals(initialPackage.id, reinstalledPackage.id)
            assertTrue(reinstalledPackage.installedAt.toEpochMilli() > staleSession.startedAt.epochMillis)
            assertNull(restartedContext.studySessionRepository!!.findById(staleSession.id))
            assertNull(restartedContext.studyQueueRepository!!.findBySessionId(staleSession.id))
            assertNotNull(restartedContext.studySessionRepository!!.findById(unrelatedSessionId))
            assertNotNull(restartedContext.studyQueueRepository!!.findBySessionId(unrelatedSessionId))

            val learnerId = LearnerId("default-learner")
            (1..4).forEach { index ->
                val learningItemId = vn.loi.learning.domain.study.learning.model.LearningItemId(
                    "cnt-pkg-a-$index-rec"
                )
                assertNull(restartedContext.engine.getMemoryState(learnerId, learningItemId))
            }
            assertTrue(restartedContext.reviewEventRepository!!.findAll(learnerId).isEmpty())

            val restoredStudy = StudyViewModel(
                StudyFacade(restartedContext),
                taskRunner = ImmediateDesktopTaskRunner
            )
            restoredStudy.refresh()
            assertFalse(restoredStudy.uiState.sessionCompleted)
            assertEquals(0, restoredStudy.uiState.reviewedCount)
            assertFalse(restoredStudy.uiState.message.contains("previous study session", ignoreCase = true))
            assertFalse(restoredStudy.uiState.canUndo)

            restoredStudy.startStudy()
            assertEquals(LearningStage.NEW, restoredStudy.uiState.learningStage)
            assertFalse(restoredStudy.uiState.sessionCompleted)
            assertFalse(restoredStudy.uiState.canUndo)
        } finally {
            packageDirectory.toFile().deleteRecursively()
            persistenceDirectory.toFile().deleteRecursively()
        }
    }

    private fun createOpd3ZipPackage(file: Path, name: String, contentId: String, contentCount: Int = 1) {
        val contents = (1..contentCount).map { i ->
            """{ "id": "$contentId-$i", "type": "SENTENCE", "primaryText": "Sentence $i for $name", "translatedText": "Cau $i", "title": "$name Lesson $i", "group": "English", "section": "Unit 1", "lesson": "$name Lesson" }"""
        }.joinToString(",")
        val items = (1..contentCount).map { i ->
            """{ "id": "$contentId-$i-rec", "contentId": "$contentId-$i", "mode": "MEANING_RECOGNITION", "isEnabled": true }"""
        }.joinToString(",")

        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            writeZipEntry(
                zip,
                "manifest.json",
                """{ "id": "$name", "name": "$name", "version": "1.0.0", "format": "OPD3", "schemaVersion": 1, "contentCount": $contentCount, "learningItemCount": $contentCount }"""
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
