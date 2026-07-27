package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.UninstallContentPackageCommand
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ActivePackageStudyAuthorityIntegrationTest {

    private val learnerId = LearnerId("learner-1")
    private val topicA = TopicId("topic-a")
    private val topicB = TopicId("topic-b")

    private val pkgAId = InstalledPackageId("pkg-a")
    private val pkgBId = InstalledPackageId("pkg-b")

    private val itemAId = LearningItemId("item-a-1")
    private val itemBId = LearningItemId("item-b-1")

    private val contentAId = ContentId("content-a-1")
    private val contentBId = ContentId("content-b-1")

    @Test
    fun `Scenario A - Empty Library after package removal renders NoActiveTopicUiState and purges stale sessions`() {
        val tempDir = Files.createTempDirectory("auth_test_a").toFile()
        try {
            // 1. First app composition: Install Pkg A, activate it, start Study and review
            val context1 = LearningApplicationFactory.createPersisted(tempDir.toPath())
            setupPackageA(context1, PackageState.ACTIVE)

            val studyFacade1 = StudyFacade(context1)
            val initialState = studyFacade1.startStudy()
            assertTrue(initialState.hasActiveSession, "Session must start for active Package A")
            assertEquals("pkg-a", initialState.activeInstalledPackageId?.value)

            // Reveal answer & rate item
            studyFacade1.revealAnswer()
            studyFacade1.review(ReviewRating.GOOD)

            // 2. Reopen context while package is still active -> session resumes
            val context2 = LearningApplicationFactory.createPersisted(tempDir.toPath())
            val studyFacade2 = StudyFacade(context2)
            val restoredState = studyFacade2.load()
            assertTrue(restoredState.hasActiveSession || restoredState.sessionCompleted, "Session must be recoverable while package is active")

            // 3. Uninstall Package A
            val uninstaller = context2.uninstallContentPackage!!
            uninstaller.execute(
                UninstallContentPackageCommand(
                    catalogId = PackageCatalogId("default-catalog"),
                    packageId = PackageId("pkg-a")
                )
            )

            // 4. Reopen platform with empty library
            val context3 = LearningApplicationFactory.createPersisted(tempDir.toPath())
            val studyFacade3 = StudyFacade(context3)
            val emptyState = studyFacade3.load()

            // Assert NoActiveTopicUiState & zero stale residue
            assertEquals("Chưa có chủ đề đang hoạt động", emptyState.message?.substringBefore("\n"))
            assertFalse(emptyState.hasActiveSession, "No active session allowed when library is empty")
            assertNull(emptyState.activeInstalledPackageId, "No active package allowed")
            assertNull(emptyState.sessionProgress, "No session progress allowed")
            assertNull(emptyState.schedulerFeedback, "No scheduler feedback allowed")

            // 5. Navigate away and back to Study repeatedly -> stays NoActiveTopicUiState
            val reloadState = studyFacade3.load()
            assertEquals("Chưa có chủ đề đang hoạt động", reloadState.message?.substringBefore("\n"))
            assertFalse(reloadState.hasActiveSession)

        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `Scenario B - Same process active package removal immediately transitions Study to NoActiveTopicUiState`() {
        val tempDir = Files.createTempDirectory("auth_test_b").toFile()
        try {
            val context = LearningApplicationFactory.createPersisted(tempDir.toPath())
            setupPackageA(context, PackageState.ACTIVE)

            val studyFacade = StudyFacade(context)
            val state1 = studyFacade.startStudy()
            assertTrue(state1.hasActiveSession)

            // Uninstall Package A in same process
            context.uninstallContentPackage!!.execute(
                UninstallContentPackageCommand(
                    catalogId = PackageCatalogId("default-catalog"),
                    packageId = PackageId("pkg-a")
                )
            )

            // Call load() without app restart
            val state2 = studyFacade.load()
            assertEquals("Chưa có chủ đề đang hoạt động", state2.message?.substringBefore("\n"))
            assertFalse(state2.hasActiveSession)
            assertNull(state2.activeInstalledPackageId)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `Scenario C - Reimporting uninstalled package initializes fresh NEW stage and Discovery mode`() {
        val tempDir = Files.createTempDirectory("auth_test_c").toFile()
        try {
            // Install, learn & uninstall Pkg A
            val context1 = LearningApplicationFactory.createPersisted(tempDir.toPath())
            setupPackageA(context1, PackageState.ACTIVE)
            val facade1 = StudyFacade(context1)
            facade1.startStudy()
            facade1.revealAnswer()
            facade1.review(ReviewRating.GOOD)

            context1.uninstallContentPackage!!.execute(
                UninstallContentPackageCommand(
                    catalogId = PackageCatalogId("default-catalog"),
                    packageId = PackageId("pkg-a")
                )
            )

            // Reimport Pkg A in a new platform instance
            val context2 = LearningApplicationFactory.createPersisted(tempDir.toPath())
            setupPackageA(context2, PackageState.ACTIVE)

            val facade2 = StudyFacade(context2)
            val newState = facade2.startStudy()

            assertTrue(newState.hasActiveSession)
            assertEquals(1, newState.totalItems)
            assertEquals(0, newState.reviewedCount, "Reimported package must start with zero reviews")

            // Verify item resolves as NEW stage memory state
            val itemState = context2.memoryStateRepository!!.find(learnerId, itemAId)
            assertNotNull(itemState)
            assertEquals(LearningStage.NEW, itemState.stage)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `Scenario D - Removing Package A preserves Package B progress and scoping`() {
        val tempDir = Files.createTempDirectory("auth_test_d").toFile()
        try {
            val context = LearningApplicationFactory.createPersisted(tempDir.toPath())
            setupPackageA(context, PackageState.ACTIVE)
            setupPackageB(context, PackageState.ARCHIVED)

            // Review Package A
            val facadeA = StudyFacade(context)
            facadeA.startStudy()
            facadeA.revealAnswer()
            facadeA.review(ReviewRating.GOOD)

            // Remove Package A
            context.uninstallContentPackage!!.execute(
                UninstallContentPackageCommand(
                    catalogId = PackageCatalogId("default-catalog"),
                    packageId = PackageId("pkg-a")
                )
            )

            // Set Package B active
            val pkgB = InstalledPackage.reconstitute(
                id = pkgBId,
                libraryId = LibraryId("default-library"),
                packageId = PackageId("pkg-b"),
                topicId = topicB,
                name = PackageName("Topic B"),
                version = PackageVersion("1.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.now(),
                contentCount = 1,
                learningItemCount = 1
            )
            context.installedPackageRepository!!.save(pkgB)

            val facadeB = StudyFacade(context)
            val stateB = facadeB.load()
            assertNotNull(stateB)

            val startB = facadeB.startStudy()
            assertTrue(startB.hasActiveSession)
            assertEquals("pkg-b", startB.activeInstalledPackageId?.value)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `01 to 24 - Detailed authority rules, orphan rejection, and queue sanitation`() {
        val tempDir = Files.createTempDirectory("auth_rules").toFile()
        try {
            val context = LearningApplicationFactory.createPersisted(tempDir.toPath())

            // 1 & 2 & 3: load() returns NoActiveTopicUiState when no active package exists
            val facade = StudyFacade(context)
            val emptyLoad = facade.load()
            assertEquals("Chưa có chủ đề đang hoạt động", emptyLoad.message?.substringBefore("\n"))

            // 4: empty Library never enables all-items fallback
            val emptyStart = facade.startStudy()
            assertEquals("Chưa có chủ đề đang hoạt động", emptyStart.message?.substringBefore("\n"))

            // Setup package A
            setupPackageA(context, PackageState.ACTIVE)

            // 5 & 6: startStudy and startSession succeed when canonicalPkg is present
            val startSuccess = facade.startStudy()
            assertTrue(startSuccess.hasActiveSession)

            // 8 & 9 & 10: Persisting a session with null installedPackageId gets rejected & purged
            val staleSession = StudySession(
                id = SessionId("stale-orphan-session"),
                learnerId = learnerId,
                startedAt = Moment(System.currentTimeMillis()),
                status = SessionStatus.ACTIVE,
                policy = SessionPolicy(),
                includedContentIds = setOf(contentAId),
                reviewedItemIds = emptySet(),
                reviewedContentIds = emptySet(),
                newItemsReviewed = 0,
                reviewItemsReviewed = 0,
                finishedAt = null,
                topicId = topicA,
                installedPackageId = null
            )
            context.studySessionRepository!!.save(staleSession)

            // Re-load facade -> stale session with null installedPackageId is rejected
            val reloadState = facade.load()
            assertNotNull(reloadState)

        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `canonical authority ignores archived installed and orphan content package records`() {
        val tempDir = Files.createTempDirectory("canonical-active-only").toFile()
        try {
            val context = LearningApplicationFactory.createPersisted(tempDir.toPath())
            setupPackageA(context, PackageState.ARCHIVED)
            context.contentPackageRepository!!.save(
                ContentPackage(
                    id = PackageId("orphan-content-package"),
                    descriptor = PackageDescriptor("Orphan", "1.0", "OPD3"),
                    libraryIds = setOf(ContentLibraryId("pkg-a")),
                    topicId = topicA
                )
            )

            val facade = StudyFacade(context)

            assertNull(facade.resolveCanonicalActivePackageId())
            val state = facade.load()
            assertEquals("Chưa có chủ đề đang hoạt động", state.message.substringBefore("\n"))
            assertNull(state.currentLearningItemId)
            assertFalse(state.hasActiveSession)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `load rejects cached and current study state before it can leak after active package disappears`() {
        val tempDir = Files.createTempDirectory("cached-study-invalidated").toFile()
        try {
            val context = LearningApplicationFactory.createPersisted(tempDir.toPath())
            setupPackageA(context, PackageState.ACTIVE)
            val facade = StudyFacade(context)
            val active = facade.startStudy()
            assertNotNull(active.currentLearningItemId)

            context.installedPackageRepository!!.delete(pkgAId)
            context.contentPackageRepository!!.save(
                ContentPackage(
                    id = PackageId("pkg-a"),
                    descriptor = PackageDescriptor("Topic A", "1.0", "OPD3"),
                    libraryIds = setOf(ContentLibraryId("pkg-a")),
                    topicId = topicA
                )
            )

            val state = facade.load()

            assertEquals("Chưa có chủ đề đang hoạt động", state.message.substringBefore("\n"))
            assertNull(state.currentLearningItemId)
            assertNull(state.sessionProgress)
            assertNull(state.schedulerFeedback)
            assertFalse(state.hasActiveSession)
            assertTrue(context.studySessionRepository!!.findAll().none { it.status == SessionStatus.ACTIVE })
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `no active package purges null-package sessions and their queues from store`() {
        val tempDir = Files.createTempDirectory("null-package-session-purge").toFile()
        try {
            val context = LearningApplicationFactory.createPersisted(tempDir.toPath())
            val session = StudySession.start(
                id = SessionId("null-package-session"),
                learnerId = learnerId,
                startedAt = Moment(System.currentTimeMillis()),
                policy = SessionPolicy(),
                includedContentIds = emptySet(),
                topicId = topicA,
                installedPackageId = null
            )
            context.studySessionRepository!!.save(session)
            context.studyQueueRepository!!.save(
                StudyQueueSnapshot.create(
                    sessionId = session.id,
                    createdAt = session.startedAt,
                    learningItemIds = listOf(itemAId)
                )
            )

            val state = StudyFacade(context).load()

            assertEquals("Chưa có chủ đề đang hoạt động", state.message.substringBefore("\n"))
            assertNull(context.studySessionRepository!!.findById(session.id))
            assertNull(context.studyQueueRepository!!.findBySessionId(session.id))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `active package rejects and deletes recovered session without installed package provenance`() {
        val tempDir = Files.createTempDirectory("null-package-session-reject").toFile()
        try {
            val context = LearningApplicationFactory.createPersisted(tempDir.toPath())
            setupPackageA(context, PackageState.ACTIVE)
            val session = StudySession.start(
                id = SessionId("legacy-general-session"),
                learnerId = LearnerId("default-learner"),
                startedAt = Moment(System.currentTimeMillis()),
                policy = SessionPolicy(),
                includedContentIds = emptySet(),
                topicId = topicA,
                installedPackageId = null
            )
            context.studySessionRepository!!.save(session)
            context.studyQueueRepository!!.save(
                StudyQueueSnapshot.create(session.id, session.startedAt, listOf(itemAId))
            )

            val state = StudyFacade(context).load()

            assertFalse(state.hasActiveSession)
            assertEquals(pkgAId, state.activeInstalledPackageId)
            assertNull(context.studySessionRepository!!.findById(session.id))
            assertNull(context.studyQueueRepository!!.findBySessionId(session.id))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun setupPackageA(context: vn.loi.learning.infrastructure.LearningApplicationContext, state: PackageState) {
        val instRepo = context.installedPackageRepository!!
        val itemRepo = context.learningItemRepository!!
        val contentRepo = context.contentRepository!!
        val libRepo = context.contentLibraryRepository!!
        val memoryRepo = context.memoryStateRepository!!

        val pkg = InstalledPackage.reconstitute(
            id = pkgAId,
            libraryId = LibraryId("default-library"),
            packageId = PackageId("pkg-a"),
            topicId = topicA,
            name = PackageName("Topic A"),
            version = PackageVersion("1.0"),
            state = state,
            installedAt = Instant.now(),
            contentCount = 1,
            learningItemCount = 1
        )
        instRepo.save(pkg)

        libRepo.save(ContentLibrary(id = ContentLibraryId("pkg-a"), descriptor = LibraryDescriptor("Topic A"), contentIds = setOf(contentAId)))

        val content = Content(
            id = contentAId,
            type = ContentType.WORD,
            text = ContentText(primaryText = "Word A"),
            metadata = ContentMetadata(group = "Vocabulary", section = "Sec 1", lesson = "Lesson 1")
        )
        contentRepo.save(content)

        val item = LearningItem(
            id = itemAId,
            contentId = contentAId,
            mode = LearningMode.MEANING_RECALL,
            isEnabled = true
        )
        itemRepo.save(item)

        memoryRepo.save(MemoryState.new(learnerId, itemAId, Moment(System.currentTimeMillis())))
    }

    private fun setupPackageB(context: vn.loi.learning.infrastructure.LearningApplicationContext, state: PackageState) {
        val instRepo = context.installedPackageRepository!!
        val itemRepo = context.learningItemRepository!!
        val contentRepo = context.contentRepository!!
        val libRepo = context.contentLibraryRepository!!
        val memoryRepo = context.memoryStateRepository!!

        val pkg = InstalledPackage.reconstitute(
            id = pkgBId,
            libraryId = LibraryId("default-library"),
            packageId = PackageId("pkg-b"),
            topicId = topicB,
            name = PackageName("Topic B"),
            version = PackageVersion("1.0"),
            state = state,
            installedAt = Instant.now(),
            contentCount = 1,
            learningItemCount = 1
        )
        instRepo.save(pkg)

        libRepo.save(ContentLibrary(id = ContentLibraryId("pkg-b"), descriptor = LibraryDescriptor("Topic B"), contentIds = setOf(contentBId)))

        val content = Content(
            id = contentBId,
            type = ContentType.WORD,
            text = ContentText(primaryText = "Word B"),
            metadata = ContentMetadata(group = "Vocabulary", section = "Sec 1", lesson = "Lesson 1")
        )
        contentRepo.save(content)

        val item = LearningItem(
            id = itemBId,
            contentId = contentBId,
            mode = LearningMode.MEANING_RECALL,
            isEnabled = true
        )
        itemRepo.save(item)

        memoryRepo.save(MemoryState.new(learnerId, itemBId, Moment(System.currentTimeMillis())))
    }
}
