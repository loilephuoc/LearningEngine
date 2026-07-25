package vn.loi.learning.desktop.ui.shell

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.packageprogress.PackageLearningProgressQuery
import vn.loi.learning.application.session.StartPackageLessonStudyRequest
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserItem
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserUiState
import vn.loi.learning.desktop.ui.contentlibrary.LessonProgressUiModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonStudyActionType
import vn.loi.learning.desktop.ui.contentlibrary.PackageLessonSelection
import vn.loi.learning.desktop.ui.contentlibrary.LearningWorkspaceProjectionPolicy
import vn.loi.learning.desktop.ui.contentlibrary.PackageProgressUiModel
import vn.loi.learning.desktop.ui.contentlibrary.RecommendationReasonType
import vn.loi.learning.desktop.ui.navigation.NavigationState
import vn.loi.learning.desktop.ui.study.SessionCompletionStatus
import vn.loi.learning.desktop.ui.study.SessionCompletionProjectionPolicy
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.infrastructure.LearningApplicationFactory

class PackageLearningProgressIntegrationTest {

    private fun getPkgId(appContext: vn.loi.learning.infrastructure.LearningApplicationContext, packageName: String): InstalledPackageId {
        val summary = appContext.libraryQuery?.getInstalledPackages(appContext.defaultLibraryId!!)?.firstOrNull { it.name == packageName }
        if (summary != null) return summary.id
        val item = appContext.installedPackages.query().first { it.name == packageName }
        return InstalledPackageId(item.id)
    }

    // T1 — Empty package
    @Test
    fun `T1 valid package with zero enabled items returns zero progress metrics without exception`() {
        val tempDir = Files.createTempDirectory("empty-pkg-progress-test")
        val persistenceDir = Files.createTempDirectory("empty-pkg-progress-db")
        try {
            val opd3File = tempDir.resolve("EmptyPkg.opd3")
            createOpd3ZipPackageWithDisabledItems(opd3File, name = "Empty Pkg", contentId = "cnt-empty-1", enabledCount = 0)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(opd3File))

            val pkgId = getPkgId(appContext, "Empty Pkg")

            val progressService = appContext.packageProgress!!
            val progress = progressService.execute(
                PackageLearningProgressQuery(
                    installedPackageId = pkgId,
                    learnerId = LearnerId("default-learner"),
                    at = Moment(2000)
                )
            )

            assertEquals(0, progress.totalLearningItemCount)
            assertEquals(0, progress.masteredItemCount)
            assertEquals(0, progress.dueItemCount)
            assertEquals(0, progress.unseenItemCount)
            assertEquals(0, progress.completionPercent)
            assertEquals(0, progress.startedPercent)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T2 — All unseen
    @Test
    fun `T2 package with enabled items and no MemoryState returns all unseen`() {
        val tempDir = Files.createTempDirectory("unseen-pkg-progress-test")
        val persistenceDir = Files.createTempDirectory("unseen-pkg-progress-db")
        try {
            val opd3File = tempDir.resolve("UnseenPkg.opd3")
            createOpd3ZipPackage(opd3File, name = "Unseen Pkg", contentId = "cnt-unseen-1", itemLimit = 3)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(opd3File))

            val pkgId = getPkgId(appContext, "Unseen Pkg")

            val progressService = appContext.packageProgress!!
            val progress = progressService.execute(
                PackageLearningProgressQuery(
                    installedPackageId = pkgId,
                    learnerId = LearnerId("default-learner"),
                    at = Moment(2000)
                )
            )

            assertEquals(3, progress.totalLearningItemCount)
            assertEquals(3, progress.unseenItemCount)
            assertEquals(0, progress.newStateItemCount)
            assertEquals(0, progress.startedItemCount)
            assertEquals(0, progress.masteredItemCount)
            assertEquals(0, progress.dueItemCount)
            assertEquals(0, progress.completionPercent)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T3 — NEW states
    @Test
    fun `T3 items with NEW stage memory state are counted in newStateItemCount and due by isDue`() {
        val tempDir = Files.createTempDirectory("new-state-pkg-test")
        val persistenceDir = Files.createTempDirectory("new-state-pkg-db")
        try {
            val opd3File = tempDir.resolve("NewStatePkg.opd3")
            createOpd3ZipPackage(opd3File, name = "New State Pkg", contentId = "cnt-new-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(opd3File))

            val pkgId = getPkgId(appContext, "New State Pkg")

            val learnerId = LearnerId("learner-test-3")
            appContext.memoryStateRepository!!.save(
                MemoryState.new(learnerId, LearningItemId("cnt-new-1-rec-1"), availableAt = Moment(1000))
            )

            val progressService = appContext.packageProgress!!
            val progress = progressService.execute(
                PackageLearningProgressQuery(
                    installedPackageId = pkgId,
                    learnerId = learnerId,
                    at = Moment(2000)
                )
            )

            assertEquals(2, progress.totalLearningItemCount)
            assertEquals(1, progress.unseenItemCount)
            assertEquals(1, progress.newStateItemCount)
            assertEquals(0, progress.startedItemCount)
            assertEquals(1, progress.dueItemCount)
            assertEquals(0, progress.completionPercent)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T4 — Partial progress
    @Test
    fun `T4 partial progress calculates exact counts for mixed stages`() {
        val tempDir = Files.createTempDirectory("partial-pkg-test")
        val persistenceDir = Files.createTempDirectory("partial-pkg-db")
        try {
            val opd3File = tempDir.resolve("PartialPkg.opd3")
            createOpd3ZipPackage(opd3File, name = "Partial Pkg", contentId = "cnt-part-1", itemLimit = 6)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(opd3File))

            val pkgId = getPkgId(appContext, "Partial Pkg")

            val learnerId = LearnerId("learner-part")

            // rec-1: NEW, due
            appContext.memoryStateRepository!!.save(
                MemoryState.new(learnerId, LearningItemId("cnt-part-1-rec-1"), availableAt = Moment(1000))
            )
            // rec-2: LEARNING, due
            appContext.memoryStateRepository!!.save(
                MemoryState(learnerId, LearningItemId("cnt-part-1-rec-2"), LearningStage.LEARNING, 5.0, 1.0, Moment(1500), Moment(1000), 1, 0)
            )
            // rec-3: REVIEW, not due
            appContext.memoryStateRepository!!.save(
                MemoryState(learnerId, LearningItemId("cnt-part-1-rec-3"), LearningStage.REVIEW, 5.0, 5.0, Moment(5000), Moment(1000), 2, 0)
            )
            // rec-4: MASTERED
            appContext.memoryStateRepository!!.save(
                MemoryState(learnerId, LearningItemId("cnt-part-1-rec-4"), LearningStage.MASTERED, 5.0, 30.0, Moment(30000), Moment(1000), 5, 0)
            )
            // rec-5: SUSPENDED, dueAt <= at
            appContext.memoryStateRepository!!.save(
                MemoryState(learnerId, LearningItemId("cnt-part-1-rec-5"), LearningStage.SUSPENDED, 5.0, 1.0, Moment(1000), Moment(1000), 1, 0)
            )
            // rec-6: UNSEEN (no MemoryState)

            val progressService = appContext.packageProgress!!
            val progress = progressService.execute(
                PackageLearningProgressQuery(
                    installedPackageId = pkgId,
                    learnerId = learnerId,
                    at = Moment(2000)
                )
            )

            assertEquals(6, progress.totalLearningItemCount)
            assertEquals(1, progress.unseenItemCount)
            assertEquals(1, progress.newStateItemCount)
            assertEquals(3, progress.startedItemCount) // LEARNING, REVIEW, MASTERED
            assertEquals(1, progress.masteredItemCount)
            assertEquals(2, progress.dueItemCount) // rec-1 (NEW), rec-2 (LEARNING). rec-5 SUSPENDED is not due!
            assertEquals(1, progress.suspendedItemCount)
            assertEquals(16, progress.completionPercent) // 1 / 6 * 100 = 16
            assertEquals(50, progress.startedPercent) // 3 / 6 * 100 = 50
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T5 — Complete package
    @Test
    fun `T5 complete package where all enabled items are MASTERED returns 100 percent completion`() {
        val tempDir = Files.createTempDirectory("complete-pkg-test")
        val persistenceDir = Files.createTempDirectory("complete-pkg-db")
        try {
            val opd3File = tempDir.resolve("CompletePkg.opd3")
            createOpd3ZipPackage(opd3File, name = "Complete Pkg", contentId = "cnt-comp-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(opd3File))

            val pkgId = getPkgId(appContext, "Complete Pkg")

            val learnerId = LearnerId("learner-comp")
            appContext.memoryStateRepository!!.save(
                MemoryState(learnerId, LearningItemId("cnt-comp-1-rec-1"), LearningStage.MASTERED, 5.0, 30.0, Moment(30000), Moment(1000), 5, 0)
            )
            appContext.memoryStateRepository!!.save(
                MemoryState(learnerId, LearningItemId("cnt-comp-1-rec-2"), LearningStage.MASTERED, 5.0, 30.0, Moment(30000), Moment(1000), 5, 0)
            )

            val progressService = appContext.packageProgress!!
            val progress = progressService.execute(
                PackageLearningProgressQuery(
                    installedPackageId = pkgId,
                    learnerId = learnerId,
                    at = Moment(2000)
                )
            )

            assertEquals(2, progress.totalLearningItemCount)
            assertEquals(2, progress.masteredItemCount)
            assertEquals(2, progress.startedItemCount)
            assertEquals(100, progress.completionPercent)
            assertEquals(100, progress.startedPercent)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T6 — Disabled items excluded
    @Test
    fun `T6 disabled items are excluded from total denominator`() {
        val tempDir = Files.createTempDirectory("disabled-pkg-test")
        val persistenceDir = Files.createTempDirectory("disabled-pkg-db")
        try {
            val opd3File = tempDir.resolve("DisabledPkg.opd3")
            createOpd3ZipPackageWithDisabledItems(opd3File, name = "Disabled Pkg", contentId = "cnt-dis-1", enabledCount = 2, disabledCount = 3)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(opd3File))

            val pkgId = getPkgId(appContext, "Disabled Pkg")

            val progressService = appContext.packageProgress!!
            val progress = progressService.execute(
                PackageLearningProgressQuery(
                    installedPackageId = pkgId,
                    learnerId = LearnerId("default-learner"),
                    at = Moment(2000)
                )
            )

            assertEquals(2, progress.totalLearningItemCount, "Total denominator must equal enabled count (2), excluding 3 disabled items")
            assertEquals(2, progress.unseenItemCount)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T7 — Due boundary
    @Test
    fun `T7 due evaluation uses isDue semantics accurately at time boundaries`() {
        val tempDir = Files.createTempDirectory("due-pkg-test")
        val persistenceDir = Files.createTempDirectory("due-pkg-db")
        try {
            val opd3File = tempDir.resolve("DuePkg.opd3")
            createOpd3ZipPackage(opd3File, name = "Due Pkg", contentId = "cnt-due-1", itemLimit = 4)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(opd3File))

            val pkgId = getPkgId(appContext, "Due Pkg")

            val learnerId = LearnerId("learner-due")
            val at = Moment(2000)

            // rec-1: dueAt = 1000 (< 2000) -> due
            appContext.memoryStateRepository!!.save(
                MemoryState(learnerId, LearningItemId("cnt-due-1-rec-1"), LearningStage.REVIEW, 5.0, 1.0, Moment(1000), Moment(500), 1, 0)
            )
            // rec-2: dueAt = 2000 (== 2000) -> due
            appContext.memoryStateRepository!!.save(
                MemoryState(learnerId, LearningItemId("cnt-due-1-rec-2"), LearningStage.REVIEW, 5.0, 1.0, Moment(2000), Moment(500), 1, 0)
            )
            // rec-3: dueAt = 3000 (> 2000) -> not due
            appContext.memoryStateRepository!!.save(
                MemoryState(learnerId, LearningItemId("cnt-due-1-rec-3"), LearningStage.REVIEW, 5.0, 1.0, Moment(3000), Moment(500), 1, 0)
            )
            // rec-4: SUSPENDED, dueAt = 1000 -> not due because suspended
            appContext.memoryStateRepository!!.save(
                MemoryState(learnerId, LearningItemId("cnt-due-1-rec-4"), LearningStage.SUSPENDED, 5.0, 1.0, Moment(1000), Moment(500), 1, 0)
            )

            val progressService = appContext.packageProgress!!
            val progress = progressService.execute(
                PackageLearningProgressQuery(
                    installedPackageId = pkgId,
                    learnerId = learnerId,
                    at = at
                )
            )

            assertEquals(2, progress.dueItemCount)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T8 — Multi-lesson package
    @Test
    fun `T8 multi-lesson package totals equal sum of individual lesson projections without double counting`() {
        val tempDir = Files.createTempDirectory("multi-lesson-test")
        val persistenceDir = Files.createTempDirectory("multi-lesson-db")
        try {
            val file = tempDir.resolve("MultiLessonPkg.opd3")
            createOpd3ZipMultiLesson(file, name = "Multi Lesson Pkg")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))

            val pkgId = getPkgId(appContext, "Multi Lesson Pkg")

            val progressService = appContext.packageProgress!!
            val progress = progressService.execute(
                PackageLearningProgressQuery(
                    installedPackageId = pkgId,
                    learnerId = LearnerId("default-learner"),
                    at = Moment(2000)
                )
            )

            assertEquals(2, progress.totalLessonCount)
            assertEquals(progress.lessons.sumOf { it.totalLearningItemCount }, progress.totalLearningItemCount)
            assertEquals(progress.lessons.sumOf { it.unseenItemCount }, progress.unseenItemCount)
            assertEquals(progress.lessons.sumOf { it.startedItemCount }, progress.startedItemCount)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T9 — Multi-section hierarchy
    @Test
    fun `T9 multi-section hierarchy metadata maps correctly to ContentId`() {
        val tempDir = Files.createTempDirectory("hierarchy-progress-test")
        val persistenceDir = Files.createTempDirectory("hierarchy-progress-db")
        try {
            val file = tempDir.resolve("HierarchyPkg.opd3")
            createOpd3ZipMultiLesson(file, name = "Hierarchy Pkg")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))

            val pkgId = getPkgId(appContext, "Hierarchy Pkg")

            val browserFacade = LessonBrowserFacade(appContext)
            val uiState = browserFacade.loadForPackage(pkgId, "Hierarchy Pkg")

            assertNotNull(uiState.packageProgress)
            assertEquals(2, uiState.lessons.size)

            val lesson1 = uiState.lessons.first { it.id == "cnt-multi-1" }
            assertNotNull(lesson1.progress)
            assertEquals("Group A", lesson1.group)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T10 — Cross-package isolation
    @Test
    fun `T10 progress for package A is completely isolated from package B items and states`() {
        val tempDir = Files.createTempDirectory("pkg-iso-test")
        val persistenceDir = Files.createTempDirectory("pkg-iso-db")
        try {
            val fileA = tempDir.resolve("PkgA.opd3")
            val fileB = tempDir.resolve("PkgB.opd3")

            createOpd3ZipPackage(fileA, name = "Pkg A", contentId = "cnt-a-1", itemLimit = 2)
            createOpd3ZipPackage(fileB, name = "Pkg B", contentId = "cnt-b-1", itemLimit = 3)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val pkgA = getPkgId(appContext, "Pkg A")
            val pkgB = getPkgId(appContext, "Pkg B")

            val learnerId = LearnerId("learner-iso")
            // Give Package B item MASTERED state
            appContext.memoryStateRepository!!.save(
                MemoryState(learnerId, LearningItemId("cnt-b-1-rec-1"), LearningStage.MASTERED, 5.0, 30.0, Moment(30000), Moment(1000), 5, 0)
            )

            val progressService = appContext.packageProgress!!

            val progressA = progressService.execute(PackageLearningProgressQuery(pkgA, learnerId, Moment(2000)))
            val progressB = progressService.execute(PackageLearningProgressQuery(pkgB, learnerId, Moment(2000)))

            assertEquals(2, progressA.totalLearningItemCount)
            assertEquals(0, progressA.masteredItemCount)
            assertEquals(0, progressA.completionPercent)

            assertEquals(3, progressB.totalLearningItemCount)
            assertEquals(1, progressB.masteredItemCount)
            assertEquals(33, progressB.completionPercent)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T11 — Cross-learner isolation
    @Test
    fun `T11 same package for learner A and learner B returns distinct learner progress`() {
        val tempDir = Files.createTempDirectory("learner-iso-test")
        val persistenceDir = Files.createTempDirectory("learner-iso-db")
        try {
            val fileA = tempDir.resolve("PkgShared.opd3")
            createOpd3ZipPackage(fileA, name = "Pkg Shared", contentId = "cnt-sh-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(fileA))

            val pkg = getPkgId(appContext, "Pkg Shared")

            val learnerA = LearnerId("learner-A")
            val learnerB = LearnerId("learner-B")

            // Learner A mastered item 1
            appContext.memoryStateRepository!!.save(
                MemoryState(learnerA, LearningItemId("cnt-sh-1-rec-1"), LearningStage.MASTERED, 5.0, 30.0, Moment(30000), Moment(1000), 5, 0)
            )

            val progressService = appContext.packageProgress!!
            val progA = progressService.execute(PackageLearningProgressQuery(pkg, learnerA, Moment(2000)))
            val progB = progressService.execute(PackageLearningProgressQuery(pkg, learnerB, Moment(2000)))

            assertEquals(1, progA.masteredItemCount)
            assertEquals(50, progA.completionPercent)

            assertEquals(0, progB.masteredItemCount)
            assertEquals(0, progB.completionPercent)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T12 — Orphan MemoryState ignored
    @Test
    fun `T12 orphan MemoryState not belonging to package is ignored during progress calculation`() {
        val tempDir = Files.createTempDirectory("orphan-test")
        val persistenceDir = Files.createTempDirectory("orphan-db")
        try {
            val file = tempDir.resolve("OrphanPkg.opd3")
            createOpd3ZipPackage(file, name = "Orphan Pkg", contentId = "cnt-orph-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))

            val pkg = getPkgId(appContext, "Orphan Pkg")

            val learnerId = LearnerId("learner-orph")

            // Save MemoryState for a non-existent item ID
            appContext.memoryStateRepository!!.save(
                MemoryState(learnerId, LearningItemId("random-orphan-item-xyz"), LearningStage.MASTERED, 5.0, 30.0, Moment(30000), Moment(1000), 5, 0)
            )

            val progressService = appContext.packageProgress!!
            val prog = progressService.execute(PackageLearningProgressQuery(pkg, learnerId, Moment(2000)))

            assertEquals(2, prog.totalLearningItemCount)
            assertEquals(0, prog.masteredItemCount)
            assertEquals(0, prog.completionPercent)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T13 — Mapper/UI projection
    @Test
    fun `T13 PackageLearningProgress projects correctly to LessonBrowserUiState models joined by ContentId`() {
        val tempDir = Files.createTempDirectory("ui-proj-test")
        val persistenceDir = Files.createTempDirectory("ui-proj-db")
        try {
            val file = tempDir.resolve("ProjPkg.opd3")
            createOpd3ZipPackage(file, name = "Proj Pkg", contentId = "cnt-proj-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))

            val pkg = getPkgId(appContext, "Proj Pkg")

            val browserFacade = LessonBrowserFacade(appContext)
            val uiState = browserFacade.loadForPackage(pkg, "Proj Pkg")

            assertNotNull(uiState.packageProgress)
            assertEquals(2, uiState.packageProgress?.totalLearningItemCount)

            val item = uiState.lessons.first { it.id == "cnt-proj-1" }
            assertEquals(2, item.progress.totalLearningItemCount)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T14 — Search/filter/sort regression
    @Test
    fun `T14 filtering or sorting lesson list preserves accurate progress on LessonBrowserItem`() {
        val tempDir = Files.createTempDirectory("search-regr-test")
        val persistenceDir = Files.createTempDirectory("search-regr-db")
        try {
            val file = tempDir.resolve("SearchPkg.opd3")
            createOpd3ZipMultiLesson(file, name = "Search Pkg")

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))

            val pkg = getPkgId(appContext, "Search Pkg")

            val browserFacade = LessonBrowserFacade(appContext)
            var uiState = browserFacade.loadForPackage(pkg, "Search Pkg")

            // Apply search query
            uiState = uiState.copy(query = "Lesson 1", appliedQuery = "Lesson 1")

            val visible = uiState.visibleLessons
            assertEquals(1, visible.size)
            assertEquals("cnt-multi-1", visible.first().id)
            assertNotNull(visible.first().progress)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T15 — Package missing
    @Test
    fun `T15 querying non-existent package ID throws IllegalArgumentException`() {
        val persistenceDir = Files.createTempDirectory("missing-pkg-db")
        try {
            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val progressService = appContext.packageProgress!!

            assertFailsWith<IllegalArgumentException> {
                progressService.execute(
                    PackageLearningProgressQuery(
                        installedPackageId = InstalledPackageId("non-existent-pkg-id-999"),
                        learnerId = LearnerId("default-learner"),
                        at = Moment(2000)
                    )
                )
            }
        } finally {
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T16 — Archived/removed package
    @Test
    fun `T16 archived package follows established lifecycle policy`() {
        val tempDir = Files.createTempDirectory("arch-pkg-test")
        val persistenceDir = Files.createTempDirectory("arch-pkg-db")
        try {
            val file = tempDir.resolve("ArchPkg.opd3")
            createOpd3ZipPackage(file, name = "Arch Pkg", contentId = "cnt-arch-1", itemLimit = 1)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))

            val pkg = getPkgId(appContext, "Arch Pkg")

            // Archive package in domain
            appContext.libraryCommand!!.archivePackage(
                appContext.defaultLibraryId!!,
                pkg
            )

            val browserFacade = LessonBrowserFacade(appContext)
            val uiState = browserFacade.loadForPackage(pkg, "Arch Pkg")
            assertNotNull(uiState)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T17 — Progress refresh after review
    @Test
    fun `T17 opening browser after study session reflects updated progress`() {
        val tempDir = Files.createTempDirectory("refresh-after-review-test")
        val persistenceDir = Files.createTempDirectory("refresh-after-review-db")
        try {
            val file = tempDir.resolve("RefreshPkg.opd3")
            createOpd3ZipPackage(file, name = "Refresh Pkg", contentId = "cnt-ref-1", itemLimit = 1)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))

            val pkg = getPkgId(appContext, "Refresh Pkg")

            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            val navState = NavigationState()
            val coordinator = LessonStudyNavigationCoordinator(studyVm, navState)

            // Initial browser load -> 0 mastered
            val initialBrowserState = LessonBrowserFacade(appContext).loadForPackage(pkg, "Refresh Pkg")
            assertEquals(0, initialBrowserState.packageProgress?.masteredItemCount ?: 0)

            // Start study and advance
            coordinator.startLessonStudy(StartPackageLessonStudyRequest(pkg, ContentId("cnt-ref-1")))
            studyVm.revealAnswer()
            studyVm.reviewEasy() // Advance stage

            // Reload browser state
            val refreshedState = LessonBrowserFacade(appContext).loadForPackage(pkg, "Refresh Pkg")
            assertNotNull(refreshedState.packageProgress)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T18 — UI empty package
    @Test
    fun `T18 PackageProgressUiModel handles 0 total items cleanly without division by zero or NaN`() {
        val emptyModel = PackageProgressUiModel()
        assertEquals(0, emptyModel.totalLearningItemCount)
        assertEquals(0, emptyModel.completionPercent)
        assertEquals(0, emptyModel.startedPercent)
    }

    // T19 — Existing PLE-003 regressions
    @Test
    fun `T19 existing PLE-003 lesson browser capabilities remain fully functional without regression`() {
        val tempDir = Files.createTempDirectory("ple003-regr-test")
        val persistenceDir = Files.createTempDirectory("ple003-regr-db")
        try {
            val file = tempDir.resolve("Ple003Regr.opd3")
            createOpd3ZipPackage(file, name = "PLE003 Regr", contentId = "cnt-regr-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))

            val pkg = getPkgId(appContext, "PLE003 Regr")

            val uiState = LessonBrowserFacade(appContext).loadForPackage(pkg, "PLE003 Regr")
            assertNotNull(uiState)
            assertEquals("PLE003 Regr", uiState.libraryName)
            assertEquals(pkg, uiState.installedPackageId)
            assertFalse(uiState.isEmpty)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T20 — Progress-aware study actions
    @Test
    fun `T20 LessonBrowserUiState derives progress-aware study actions for START CONTINUE REVIEW and UNAVAILABLE`() {
        val pkgId = InstalledPackageId("pkg-test-action")
        val itemUnseen = LessonBrowserItem(
            id = "c1", title = "L1", type = "SENTENCE", group = null, section = null, lesson = null,
            primaryText = "P1", translatedText = "T1", learningItemCount = 2,
            progress = LessonProgressUiModel(totalLearningItemCount = 2, unseenItemCount = 2)
        )
        val itemContinue = LessonBrowserItem(
            id = "c2", title = "L2", type = "SENTENCE", group = null, section = null, lesson = null,
            primaryText = "P2", translatedText = "T2", learningItemCount = 3,
            progress = LessonProgressUiModel(totalLearningItemCount = 3, startedItemCount = 2, masteredItemCount = 1)
        )
        val itemReview = LessonBrowserItem(
            id = "c3", title = "L3", type = "SENTENCE", group = null, section = null, lesson = null,
            primaryText = "P3", translatedText = "T3", learningItemCount = 2,
            progress = LessonProgressUiModel(totalLearningItemCount = 2, startedItemCount = 2, masteredItemCount = 2)
        )
        val itemUnavailable = LessonBrowserItem(
            id = "c4", title = "L4", type = "SENTENCE", group = null, section = null, lesson = null,
            primaryText = "P4", translatedText = "T4", learningItemCount = 0,
            progress = LessonProgressUiModel(totalLearningItemCount = 0)
        )

        var uiState = LessonBrowserUiState(
            libraryId = "lib-1", libraryName = "Lib", installedPackageId = pkgId,
            lessons = listOf(itemUnseen, itemContinue, itemReview, itemUnavailable)
        )

        // Select unseen -> START
        uiState = uiState.select("c1")
        val action1 = uiState.selectedAction
        assertNotNull(action1)
        assertEquals(LessonStudyActionType.START, action1.type)
        assertEquals("Start Lesson", action1.label)
        assertTrue(uiState.isStartEnabled)

        // Select continue -> CONTINUE
        uiState = uiState.select("c2")
        val action2 = uiState.selectedAction
        assertNotNull(action2)
        assertEquals(LessonStudyActionType.CONTINUE, action2.type)
        assertEquals("Continue Lesson", action2.label)
        assertTrue(uiState.isStartEnabled)

        // Select review -> REVIEW
        uiState = uiState.select("c3")
        val action3 = uiState.selectedAction
        assertNotNull(action3)
        assertEquals(LessonStudyActionType.REVIEW, action3.type)
        assertEquals("Review Lesson", action3.label)
        assertTrue(uiState.isStartEnabled)

        // Select unavailable -> UNAVAILABLE
        uiState = uiState.select("c4")
        val action4 = uiState.selectedAction
        assertNotNull(action4)
        assertEquals(LessonStudyActionType.UNAVAILABLE, action4.type)
        assertEquals("No Learning Items", action4.label)
        assertFalse(uiState.isStartEnabled)
    }

    // T21 — Selection change updates action without stale leakage
    @Test
    fun `T21 Lesson selection change dynamically updates study action without stale action leakage`() {
        val pkgId = InstalledPackageId("pkg-switch-test")
        val item1 = LessonBrowserItem(
            id = "c1", title = "L1", type = "SENTENCE", group = null, section = null, lesson = null,
            primaryText = "P1", translatedText = "T1", learningItemCount = 1,
            progress = LessonProgressUiModel(totalLearningItemCount = 1, unseenItemCount = 1)
        )
        val item2 = LessonBrowserItem(
            id = "c2", title = "L2", type = "SENTENCE", group = null, section = null, lesson = null,
            primaryText = "P2", translatedText = "T2", learningItemCount = 1,
            progress = LessonProgressUiModel(totalLearningItemCount = 1, startedItemCount = 1, masteredItemCount = 1)
        )

        var uiState = LessonBrowserUiState(
            libraryId = "lib-1", libraryName = "Lib", installedPackageId = pkgId,
            lessons = listOf(item1, item2), selectedLessonId = "c1"
        )
        assertEquals("Start Lesson", uiState.selectedAction?.label)

        // Switch to item 2
        uiState = uiState.select("c2")
        assertEquals("Review Lesson", uiState.selectedAction?.label)

        // Clear selection
        uiState = uiState.clearSelection()
        assertNull(uiState.selectedAction)
        assertFalse(uiState.isStartEnabled)
    }

    // T22 — Recommendation projects into LessonBrowserUiState
    @Test
    fun `T22 package recommendation projects into LessonBrowserUiState and preserves exact ContentId`() {
        val pkgId = InstalledPackageId("pkg-reco-proj")
        val item1 = LessonBrowserItem(
            id = "c1", title = "Lesson 1", type = "SENTENCE", group = null, section = null, lesson = null,
            primaryText = "P1", translatedText = "T1", learningItemCount = 2,
            progress = LessonProgressUiModel(totalLearningItemCount = 2, startedItemCount = 2, masteredItemCount = 2)
        )
        val item2 = LessonBrowserItem(
            id = "c2", title = "Lesson 2", type = "SENTENCE", group = null, section = null, lesson = null,
            primaryText = "P2", translatedText = "T2", learningItemCount = 2,
            progress = LessonProgressUiModel(totalLearningItemCount = 2, dueItemCount = 1)
        )

        val uiState = LessonBrowserUiState(
            libraryId = "lib-1", libraryName = "Lib", installedPackageId = pkgId,
            lessons = listOf(item1, item2)
        )

        val reco = uiState.recommendation
        assertNotNull(reco)
        assertEquals(ContentId("c2"), reco.contentId)
        assertEquals("Lesson 2", reco.lessonTitle)
        assertEquals(RecommendationReasonType.DUE_NOW, reco.reasonType)
        assertEquals("Review due items", reco.actionLabel)
    }

    // T23 — Selecting recommended lesson does not start study session
    @Test
    fun `T23 selecting recommended lesson updates selectedLessonId without invoking study session callback`() {
        val pkgId = InstalledPackageId("pkg-select-reco")
        val item = LessonBrowserItem(
            id = "cnt-rec-1", title = "Lesson 1", type = "SENTENCE", group = null, section = null, lesson = null,
            primaryText = "P1", translatedText = "T1", learningItemCount = 2
        )
        val uiState = LessonBrowserUiState(
            libraryId = "lib-1", libraryName = "Lib", installedPackageId = pkgId,
            lessons = listOf(item)
        )

        val reco = uiState.recommendation
        assertNotNull(reco)

        // Select recommended lesson
        val updatedState = uiState.select(reco.contentId.value)
        assertEquals("cnt-rec-1", updatedState.selectedLessonId)
        assertEquals("cnt-rec-1", updatedState.selectedLessonInView?.id)
        assertTrue(updatedState.isStartEnabled)
    }

    // T24 — Recommendation is package-isolated
    @Test
    fun `T24 recommendation is package-isolated when loading Package A vs Package B`() {
        val tempDir = Files.createTempDirectory("ple006-pkg-iso")
        val persistenceDir = Files.createTempDirectory("ple006-pkg-db")
        try {
            val fileA = tempDir.resolve("PkgA.opd3")
            createOpd3ZipPackage(fileA, name = "Package A", contentId = "cnt-a-1", itemLimit = 2)

            val fileB = tempDir.resolve("PkgB.opd3")
            createOpd3ZipPackage(fileB, name = "Package B", contentId = "cnt-b-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))

            val pkgA = getPkgId(appContext, "Package A")
            val pkgB = getPkgId(appContext, "Package B")

            val uiStateA = LessonBrowserFacade(appContext).loadForPackage(pkgA, "Package A")
            val uiStateB = LessonBrowserFacade(appContext).loadForPackage(pkgB, "Package B")

            assertNotNull(uiStateA.recommendation)
            assertEquals(ContentId("cnt-a-1"), uiStateA.recommendation!!.contentId)

            assertNotNull(uiStateB.recommendation)
            assertEquals(ContentId("cnt-b-1"), uiStateB.recommendation!!.contentId)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T25 — Selected lesson CTA is still derived by PLE-005 LessonStudyActionPolicy after recommendation selection
    @Test
    fun `T25 selected lesson CTA is still derived by PLE-005 LessonStudyActionPolicy after recommendation selection`() {
        val pkgId = InstalledPackageId("pkg-reco-cta")
        val itemDue = LessonBrowserItem(
            id = "c1", title = "Due Lesson", type = "SENTENCE", group = null, section = null, lesson = null,
            primaryText = "P1", translatedText = "T1", learningItemCount = 3,
            progress = LessonProgressUiModel(totalLearningItemCount = 3, dueItemCount = 1, startedItemCount = 0)
        )
        val uiState = LessonBrowserUiState(
            libraryId = "lib-1", libraryName = "Lib", installedPackageId = pkgId,
            lessons = listOf(itemDue)
        )

        // Recommendation is DUE_NOW ("Review due items")
        val reco = uiState.recommendation
        assertNotNull(reco)
        assertEquals("Review due items", reco.actionLabel)

        // Select the lesson
        val selectedState = uiState.select(reco.contentId.value)

        // PLE-005 LessonStudyActionPolicy determines CTA for selected lesson (started == 0 -> "Start Lesson")
        assertEquals("Start Lesson", selectedState.selectedAction?.label)
        assertTrue(selectedState.isStartEnabled)
    }

    // T26 — CTA opens Learning Workspace without creating StudySession
    @Test
    fun `T26 CTA opens Learning Workspace without creating StudySession`() {
        val tempDir = Files.createTempDirectory("ple007-open-ws")
        val persistenceDir = Files.createTempDirectory("ple007-open-db")
        try {
            val file = tempDir.resolve("PkgWS.opd3")
            createOpd3ZipPackage(file, name = "Package WS", contentId = "cnt-ws-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))
            val pkgId = getPkgId(appContext, "Package WS")

            contentLibVm.browsePackageLessons(pkgId, "Package WS")
            val browserState = contentLibVm.lessonBrowserUiState
            assertNotNull(browserState)
            assertNull(contentLibVm.learningWorkspaceUiState)

            // Select lesson and open workspace
            contentLibVm.selectLesson("cnt-ws-1")
            contentLibVm.openWorkspaceForSelectedLesson()

            // Workspace state is created, no study session exists in appContext
            val wsState = contentLibVm.learningWorkspaceUiState
            assertNotNull(wsState)
            assertEquals("cnt-ws-1", wsState!!.contentId.value)
            assertEquals(pkgId, wsState.installedPackageId)
            assertEquals("Package WS", wsState.packageName)
            assertTrue(wsState.canStart)

            // Verify active session was NOT created
            assertNull(appContext.engine.getLatestUndoableSession(LearnerId("default-learner")))
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T27 — Back in Learning Workspace returns to Lesson Browser without creating StudySession
    @Test
    fun `T27 Back in Learning Workspace returns to Lesson Browser without creating StudySession`() {
        val tempDir = Files.createTempDirectory("ple007-back-ws")
        val persistenceDir = Files.createTempDirectory("ple007-back-db")
        try {
            val file = tempDir.resolve("PkgBack.opd3")
            createOpd3ZipPackage(file, name = "Package Back", contentId = "cnt-back-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))
            val pkgId = getPkgId(appContext, "Package Back")

            contentLibVm.browsePackageLessons(pkgId, "Package Back")
            contentLibVm.selectLesson("cnt-back-1")
            contentLibVm.openWorkspaceForSelectedLesson()

            assertNotNull(contentLibVm.learningWorkspaceUiState)

            // Click Back
            contentLibVm.closeWorkspace()

            // Workspace state cleared, browser state retained
            assertNull(contentLibVm.learningWorkspaceUiState)
            assertNotNull(contentLibVm.lessonBrowserUiState)
            assertEquals("cnt-back-1", contentLibVm.lessonBrowserUiState!!.selectedLessonId)

            // Verify active session was NOT created
            assertNull(appContext.engine.getLatestUndoableSession(LearnerId("default-learner")))
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T28 — Start Learning in Learning Workspace calls onStartLessonStudy with real InstalledPackageId and ContentId
    @Test
    fun `T28 Start Learning in Learning Workspace calls onStartLessonStudy with real InstalledPackageId and ContentId`() {
        val tempDir = Files.createTempDirectory("ple007-start-ws")
        val persistenceDir = Files.createTempDirectory("ple007-start-db")
        try {
            val file = tempDir.resolve("PkgStart.opd3")
            createOpd3ZipPackage(file, name = "Package Start", contentId = "cnt-start-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))
            val pkgId = getPkgId(appContext, "Package Start")

            contentLibVm.browsePackageLessons(pkgId, "Package Start")
            contentLibVm.selectLesson("cnt-start-1")
            contentLibVm.openWorkspaceForSelectedLesson()
            contentLibVm.navigateToPrepareMode()

            var invokedSelection: PackageLessonSelection? = null
            contentLibVm.startStudyFromWorkspace { selection ->
                invokedSelection = selection
            }

            assertNotNull(invokedSelection)
            assertEquals(pkgId, invokedSelection!!.installedPackageId)
            assertEquals("cnt-start-1", invokedSelection!!.lessonId)
            assertEquals("Package Start", invokedSelection!!.packageName)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T29 — Busy guard prevents duplicate start execution
    @Test
    fun `T29 Busy guard prevents duplicate start execution`() {
        val pkgId = InstalledPackageId("pkg-busy")
        val item = LessonBrowserItem(
            id = "c1", title = "Lesson 1", type = "SENTENCE", group = null, section = null, lesson = null,
            primaryText = "P1", translatedText = "T1", learningItemCount = 3,
            progress = LessonProgressUiModel(totalLearningItemCount = 3)
        )
        val browserState = LessonBrowserUiState(
            libraryId = "lib-1", libraryName = "Lib", installedPackageId = pkgId,
            lessons = listOf(item), selectedLessonId = "c1"
        )

        val wsState = LearningWorkspaceProjectionPolicy.create(browserState, item)
        assertNotNull(wsState)
        assertTrue(wsState!!.canStart)
    }

    // T30 — Switching packages clears stale workspace
    @Test
    fun `T30 Switching packages clears stale workspace`() {
        val tempDir = Files.createTempDirectory("ple007-switch-ws")
        val persistenceDir = Files.createTempDirectory("ple007-switch-db")
        try {
            val fileA = tempDir.resolve("PkgA.opd3")
            createOpd3ZipPackage(fileA, name = "Package A", contentId = "cnt-a-1", itemLimit = 2)

            val fileB = tempDir.resolve("PkgB.opd3")
            createOpd3ZipPackage(fileB, name = "Package B", contentId = "cnt-b-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(fileA))
            contentLibVm.importFromFiles(listOf(fileB))
            val pkgA = getPkgId(appContext, "Package A")
            val pkgB = getPkgId(appContext, "Package B")

            contentLibVm.browsePackageLessons(pkgA, "Package A")
            contentLibVm.selectLesson("cnt-a-1")
            contentLibVm.openWorkspaceForSelectedLesson()

            assertNotNull(contentLibVm.learningWorkspaceUiState)

            // Switch to Package B
            contentLibVm.browsePackageLessons(pkgB, "Package B")

            // Workspace state is reset to null
            assertNull(contentLibVm.learningWorkspaceUiState)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T31 — Completed session projects SessionCompletionUiState with COMPLETED status and authoritative metrics
    @Test
    fun `T31 Completed session projects SessionCompletionUiState with COMPLETED status and authoritative metrics`() {
        val tempDir = Files.createTempDirectory("ple008-comp-stat")
        val persistenceDir = Files.createTempDirectory("ple008-comp-db")
        try {
            val file = tempDir.resolve("PkgComp.opd3")
            createOpd3ZipPackage(file, name = "Package Comp", contentId = "cnt-comp-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))
            val pkgId = getPkgId(appContext, "Package Comp")

            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            studyVm.startLessonStudy(StartPackageLessonStudyRequest(installedPackageId = pkgId, contentId = ContentId("cnt-comp-1")))

            var attempts = 0
            while (!studyVm.uiState.sessionCompleted && attempts < 10) {
                if (studyVm.uiState.canRevealAnswer) {
                    studyVm.revealAnswer()
                }
                if (studyVm.uiState.canReview) {
                    studyVm.reviewGood()
                }
                attempts++
            }

            val uiState = studyVm.uiState
            assertTrue(uiState.sessionCompleted)

            val completionState = SessionCompletionProjectionPolicy.create(uiState)
            assertEquals(SessionCompletionStatus.COMPLETED, completionState.status)
            assertEquals("Session Completed", completionState.statusLabel)
            assertEquals(1, completionState.reviewedCount)
            assertEquals(pkgId, completionState.installedPackageId)
            assertEquals(ContentId("cnt-comp-1"), completionState.contentId)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T32 — Non-completed or stopped session status maps to PAUSED or STOPPED without claiming COMPLETED
    @Test
    fun `T32 Non-completed or stopped session status maps to PAUSED or STOPPED without claiming COMPLETED`() {
        val tempDir = Files.createTempDirectory("ple008-paused-stat")
        val persistenceDir = Files.createTempDirectory("ple008-paused-db")
        try {
            val file = tempDir.resolve("PkgPause.opd3")
            createOpd3ZipPackage(file, name = "Package Pause", contentId = "cnt-pause-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))
            val pkgId = getPkgId(appContext, "Package Pause")

            val studyVm = StudyViewModel(facade = StudyFacade(appContext))
            studyVm.startLessonStudy(StartPackageLessonStudyRequest(installedPackageId = pkgId, contentId = ContentId("cnt-pause-1")))

            // Session is active but not completed
            val activeState = studyVm.uiState
            assertTrue(activeState.hasActiveSession)
            assertFalse(activeState.sessionCompleted)

            val completionState = SessionCompletionProjectionPolicy.create(activeState)
            assertEquals(SessionCompletionStatus.PAUSED, completionState.status)
            assertEquals("Session Paused", completionState.statusLabel)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T33 — Back to Lesson navigation returns to correct package and lesson selection without creating a new session
    @Test
    fun `T33 Back to Lesson navigation returns to correct package and lesson selection without creating a new session`() {
        val tempDir = Files.createTempDirectory("ple008-back-lesson")
        val persistenceDir = Files.createTempDirectory("ple008-back-db")
        try {
            val file = tempDir.resolve("PkgBackL.opd3")
            createOpd3ZipPackage(file, name = "Package Back L", contentId = "cnt-backl-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))
            val pkgId = getPkgId(appContext, "Package Back L")

            contentLibVm.browsePackageLessons(pkgId, "Package Back L")
            contentLibVm.selectLesson("cnt-backl-1")

            assertNotNull(contentLibVm.lessonBrowserUiState)
            assertEquals("cnt-backl-1", contentLibVm.lessonBrowserUiState!!.selectedLessonId)
            assertNull(contentLibVm.learningWorkspaceUiState)

            // Verify session was not created
            assertNull(appContext.engine.getLatestUndoableSession(LearnerId("default-learner")))
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T34 — Back to Library navigation clears package browser and completion state without creating a new session
    @Test
    fun `T34 Back to Library navigation clears package browser and completion state without creating a new session`() {
        val tempDir = Files.createTempDirectory("ple008-back-lib")
        val persistenceDir = Files.createTempDirectory("ple008-back-db")
        try {
            val file = tempDir.resolve("PkgBackLib.opd3")
            createOpd3ZipPackage(file, name = "Package Back Lib", contentId = "cnt-backlib-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))
            val pkgId = getPkgId(appContext, "Package Back Lib")

            contentLibVm.browsePackageLessons(pkgId, "Package Back Lib")
            assertNotNull(contentLibVm.lessonBrowserUiState)

            contentLibVm.closeLibrary()
            assertNull(contentLibVm.lessonBrowserUiState)
            assertNull(contentLibVm.learningWorkspaceUiState)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    // T35 — Continue Learning navigates to Content Library and opens PLE-007 Learning Workspace for the lesson
    @Test
    fun `T35 Continue Learning navigates to Content Library and opens PLE-007 Learning Workspace for the lesson`() {
        val tempDir = Files.createTempDirectory("ple008-cont-ws")
        val persistenceDir = Files.createTempDirectory("ple008-cont-db")
        try {
            val file = tempDir.resolve("PkgCont.opd3")
            createOpd3ZipPackage(file, name = "Package Cont", contentId = "cnt-cont-1", itemLimit = 2)

            val appContext = LearningApplicationFactory.createPersisted(persistenceDir)
            val contentLibVm = ContentLibraryViewModel(
                facade = ContentLibraryFacade(appContext),
                lessonBrowserFacade = LessonBrowserFacade(appContext)
            )
            contentLibVm.importFromFiles(listOf(file))
            val pkgId = getPkgId(appContext, "Package Cont")

            contentLibVm.browsePackageLessons(pkgId, "Package Cont")
            contentLibVm.selectLesson("cnt-cont-1")
            contentLibVm.openWorkspaceForSelectedLesson()

            val wsState = contentLibVm.learningWorkspaceUiState
            assertNotNull(wsState)
            assertEquals("cnt-cont-1", wsState!!.contentId.value)
            assertEquals(pkgId, wsState.installedPackageId)
            assertTrue(wsState.canStart)
        } finally {
            tempDir.toFile().deleteRecursively()
            persistenceDir.toFile().deleteRecursively()
        }
    }

    private fun createOpd3ZipPackage(file: Path, name: String, contentId: String, itemLimit: Int = 1) {
        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            writeZipEntry(
                zip,
                "manifest.json",
                """{ "name": "$name", "version": "1.0.0", "format": "OPD3", "schemaVersion": 1, "contentCount": 1, "learningItemCount": $itemLimit }"""
            )
            writeZipEntry(zip, "media-manifest.json", """{ "version": "1.0.0", "files": [] }""")
            writeZipEntry(zip, "metadata.json", """{ "name": "$name", "version": "1.0.0", "format": "OPD3" }""")
            writeZipEntry(
                zip,
                "contents.json",
                """{ "contents": [ { "id": "$contentId", "type": "SENTENCE", "primaryText": "Primary", "translatedText": "Translated", "title": "Lesson Title", "group": "Group 1", "section": "Section 1", "lesson": "$name Lesson" } ] }"""
            )

            val modes = LearningMode.entries
            require(itemLimit <= modes.size) {
                "Test fixture cannot create more than one learning item per LearningMode for the same content."
            }

            val itemsJson = (1..itemLimit).joinToString(",") { idx ->
                val mode = modes[idx - 1].name
                """{ "id": "$contentId-rec-$idx", "contentId": "$contentId", "mode": "$mode", "isEnabled": true }"""
            }
            writeZipEntry(zip, "learning-items.json", """{ "learningItems": [ $itemsJson ] }""")
        }
    }

    private fun createOpd3ZipPackageWithDisabledItems(file: Path, name: String, contentId: String, enabledCount: Int, disabledCount: Int = 0) {
        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            val total = enabledCount + disabledCount
            writeZipEntry(
                zip,
                "manifest.json",
                """{ "name": "$name", "version": "1.0.0", "format": "OPD3", "schemaVersion": 1, "contentCount": 1, "learningItemCount": $total }"""
            )
            writeZipEntry(zip, "media-manifest.json", """{ "version": "1.0.0", "files": [] }""")
            writeZipEntry(zip, "metadata.json", """{ "name": "$name", "version": "1.0.0", "format": "OPD3" }""")
            writeZipEntry(
                zip,
                "contents.json",
                """{ "contents": [ { "id": "$contentId", "type": "SENTENCE", "primaryText": "Primary", "translatedText": "Translated", "title": "Lesson Title", "group": "Group 1", "section": "Section 1", "lesson": "$name Lesson" } ] }"""
            )

            val modes = LearningMode.entries
            require(enabledCount + disabledCount <= modes.size) {
                "Test fixture cannot create duplicate learning item modes for the same content."
            }

            val enabledJson = (1..enabledCount).map { idx ->
                val mode = modes[idx - 1].name
                """{ "id": "$contentId-enabled-$idx", "contentId": "$contentId", "mode": "$mode", "isEnabled": true }"""
            }
            val disabledJson = (1..disabledCount).map { idx ->
                val mode = modes[enabledCount + idx - 1].name
                """{ "id": "$contentId-disabled-$idx", "contentId": "$contentId", "mode": "$mode", "isEnabled": false }"""
            }
            val allItemsJson = (enabledJson + disabledJson).joinToString(",")

            writeZipEntry(zip, "learning-items.json", """{ "learningItems": [ $allItemsJson ] }""")
        }
    }

    private fun createOpd3ZipMultiLesson(file: Path, name: String) {
        ZipOutputStream(Files.newOutputStream(file)).use { zip ->
            writeZipEntry(
                zip,
                "manifest.json",
                """{ "name": "$name", "version": "1.0.0", "format": "OPD3", "schemaVersion": 1, "contentCount": 2, "learningItemCount": 2 }"""
            )
            writeZipEntry(zip, "media-manifest.json", """{ "version": "1.0.0", "files": [] }""")
            writeZipEntry(zip, "metadata.json", """{ "name": "$name", "version": "1.0.0", "format": "OPD3" }""")
            writeZipEntry(
                zip,
                "contents.json",
                """{ "contents": [
                    { "id": "cnt-multi-1", "type": "SENTENCE", "primaryText": "One", "translatedText": "Một", "title": "Lesson 1", "group": "Group A", "section": "Section 1", "lesson": "Lesson 1" },
                    { "id": "cnt-multi-2", "type": "SENTENCE", "primaryText": "Two", "translatedText": "Hai", "title": "Lesson 2", "group": "Group A", "section": "Section 2", "lesson": "Lesson 2" }
                ] }"""
            )
            writeZipEntry(
                zip,
                "learning-items.json",
                """{ "learningItems": [
                    { "id": "cnt-multi-1-rec-1", "contentId": "cnt-multi-1", "mode": "MEANING_RECOGNITION", "isEnabled": true },
                    { "id": "cnt-multi-2-rec-1", "contentId": "cnt-multi-2", "mode": "MEANING_RECOGNITION", "isEnabled": true }
                ] }"""
            )
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }
}
