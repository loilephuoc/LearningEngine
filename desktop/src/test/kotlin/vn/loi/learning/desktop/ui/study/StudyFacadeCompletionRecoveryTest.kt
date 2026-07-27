package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.domain.study.session.model.UndoableSessionReview
import vn.loi.learning.infrastructure.LearningApplicationFactory

class StudyFacadeCompletionRecoveryTest {
    @Test
    fun `restart after final atomic review projects completed workspace`() {
        val context = LearningApplicationFactory.createInMemory()
        val content = Content(ContentId("content"), ContentType.WORD, ContentText("question", "answer"))
        val item = LearningItem(LearningItemId("item"), content.id, LearningMode.MEANING_RECOGNITION)
        context.engine.registerContent(content)
        context.engine.registerLearningItem(item)
        val packageId = vn.loi.learning.domain.content.packaging.model.PackageId("completion-package")
        val topicId = vn.loi.learning.domain.content.topic.model.TopicId("completion-topic")
        context.installedPackageRepository!!.save(
            vn.loi.learning.domain.library.model.InstalledPackage.reconstitute(
                id = vn.loi.learning.domain.library.model.InstalledPackageId(packageId.value),
                libraryId = vn.loi.learning.domain.library.model.LibraryId("default-library"),
                packageId = packageId,
                topicId = topicId,
                name = vn.loi.learning.domain.library.model.PackageName("Completion"),
                version = vn.loi.learning.domain.library.model.PackageVersion("1.0"),
                state = vn.loi.learning.domain.library.model.PackageState.ACTIVE,
                installedAt = java.time.Instant.ofEpochMilli(999),
                contentCount = 1,
                learningItemCount = 1
            )
        )
        val libraryId = vn.loi.learning.domain.content.library.model.ContentLibraryId("completion-library")
        context.contentLibraryRepository!!.save(
            vn.loi.learning.domain.content.library.model.ContentLibrary(
                libraryId,
                vn.loi.learning.domain.content.library.model.LibraryDescriptor("Completion"),
                setOf(content.id)
            )
        )
        context.contentPackageRepository!!.save(
            vn.loi.learning.domain.content.packaging.model.ContentPackage(
                packageId,
                vn.loi.learning.domain.content.packaging.model.PackageDescriptor("Completion", "1.0", "OPD3"),
                setOf(libraryId),
                topicId
            )
        )
        val sessionId = SessionId("session")
        val now = Moment(1_000)
        context.engine.startSession(
            StartStudySessionCommand(
                sessionId,
                LearnerId("default-learner"),
                now,
                installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId(packageId.value),
                topicId = topicId
            )
        )
        val current = assertNotNull(context.engine.getNextSessionItem(sessionId, now))
        context.engine.reviewSessionItem(
            ReviewSessionItemCommand(
                sessionId, ReviewEventId("review"), current.item.learningItem.id,
                ReviewRating.GOOD, now
            )
        )
        val recovered = StudyFacade(context).load()

        assertTrue(recovered.sessionCompleted)
        assertFalse(recovered.hasActiveSession)
        assertTrue(recovered.workspaceState is ReviewWorkspaceState.Completed)
        assertEquals(1, recovered.reviewedCount)
        assertEquals(1, recovered.sessionProgress?.completedItemCount)
        assertTrue(recovered.sessionProgress?.isCompleted == true)
        assertTrue(recovered.canUndo)
        assertEquals(null, context.engine.getActiveSession(LearnerId("default-learner")))

        val reopened = StudyFacade(context).apply { load() }.undoLatestReview()
        assertTrue(reopened.hasActiveSession)
        assertFalse(reopened.sessionCompleted)
        assertEquals(0, reopened.reviewedCount)
        assertEquals(0, reopened.sessionProgress?.completedItemCount)
        assertEquals(0L, assertNotNull(reopened.experienceRotationContext).ordinal)
        assertFalse(reopened.canUndo)

        val secondUndo = StudyFacade(context).apply { load() }.undoLatestReview()
        assertEquals(0, secondUndo.reviewedCount)
        assertFalse(secondUndo.canUndo)
    }

    @Test
    fun `completed session before current deterministic package installation is purged`() {
        val persistenceDirectory = java.nio.file.Files.createTempDirectory("stale-completion-lifecycle")
        try {
        val context = LearningApplicationFactory.createPersisted(persistenceDirectory)
        val content = Content(ContentId("stale-content"), ContentType.WORD, ContentText("question", "answer"))
        val item = LearningItem(
            LearningItemId("stale-item"),
            content.id,
            LearningMode.MEANING_RECOGNITION
        )
        context.engine.registerContent(content)
        context.engine.registerLearningItem(item)
        val packageId = vn.loi.learning.domain.content.packaging.model.PackageId("stale-package")
        val installedPackageId = vn.loi.learning.domain.library.model.InstalledPackageId(packageId.value)
        val topicId = vn.loi.learning.domain.content.topic.model.TopicId("stale-topic")
        context.installedPackageRepository!!.save(
            vn.loi.learning.domain.library.model.InstalledPackage.reconstitute(
                id = installedPackageId,
                libraryId = vn.loi.learning.domain.library.model.LibraryId("default-library"),
                packageId = packageId,
                topicId = topicId,
                name = vn.loi.learning.domain.library.model.PackageName("Stale"),
                version = vn.loi.learning.domain.library.model.PackageVersion("1.0"),
                state = vn.loi.learning.domain.library.model.PackageState.ACTIVE,
                installedAt = java.time.Instant.ofEpochMilli(2_000),
                contentCount = 1,
                learningItemCount = 1
            )
        )
        val libraryId = vn.loi.learning.domain.content.library.model.ContentLibraryId("stale-library")
        context.contentLibraryRepository!!.save(
            vn.loi.learning.domain.content.library.model.ContentLibrary(
                libraryId,
                vn.loi.learning.domain.content.library.model.LibraryDescriptor("Stale"),
                setOf(content.id)
            )
        )
        context.contentPackageRepository!!.save(
            vn.loi.learning.domain.content.packaging.model.ContentPackage(
                packageId,
                vn.loi.learning.domain.content.packaging.model.PackageDescriptor("Stale", "1.0", "OPD3"),
                setOf(libraryId),
                topicId
            )
        )
        val sessionId = SessionId("stale-session")
        val sessionStartedAt = Moment(1_000)
        context.engine.startSession(
            StartStudySessionCommand(
                sessionId,
                LearnerId("default-learner"),
                sessionStartedAt,
                installedPackageId = installedPackageId,
                topicId = topicId
            )
        )
        val current = assertNotNull(context.engine.getNextSessionItem(sessionId, sessionStartedAt))
        context.engine.reviewSessionItem(
            ReviewSessionItemCommand(
                sessionId,
                ReviewEventId("stale-review"),
                current.item.learningItem.id,
                ReviewRating.GOOD,
                sessionStartedAt
            )
        )
        assertEquals(
            2_000,
            context.installedPackageRepository!!.findById(installedPackageId)!!.installedAt.toEpochMilli()
        )
        assertEquals(
            sessionStartedAt,
            context.studySessionRepository!!.findById(sessionId)!!.startedAt
        )
        assertEquals(
            installedPackageId,
            context.studySessionRepository!!.findById(sessionId)!!.installedPackageId
        )

        val recovered = StudyFacade(context).load()

        assertFalse(recovered.sessionCompleted)
        assertEquals(0, recovered.reviewedCount)
        assertFalse(recovered.canUndo)
        assertNull(context.studySessionRepository!!.findById(sessionId))
        assertNull(context.studyQueueRepository!!.findBySessionId(sessionId))
        } finally {
            persistenceDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `legacy completed session without package ownership is purged and returns idle`() {
        listOf(
            null to vn.loi.learning.domain.content.topic.model.TopicId("legacy-topic"),
            vn.loi.learning.domain.library.model.InstalledPackageId("legacy-package") to null
        ).forEachIndexed { index, (installedPackageId, topicId) ->
            val context = LearningApplicationFactory.createInMemory()
            val sessionId = SessionId("legacy-session-$index")
            val learnerId = LearnerId("default-learner")
            context.installedPackageRepository!!.save(
                vn.loi.learning.domain.library.model.InstalledPackage.reconstitute(
                    id = vn.loi.learning.domain.library.model.InstalledPackageId("current-package"),
                    libraryId = vn.loi.learning.domain.library.model.LibraryId("default-library"),
                    packageId = vn.loi.learning.domain.content.packaging.model.PackageId("current-package"),
                    topicId = vn.loi.learning.domain.content.topic.model.TopicId("current-topic"),
                    name = vn.loi.learning.domain.library.model.PackageName("Current"),
                    version = vn.loi.learning.domain.library.model.PackageVersion("1.0"),
                    state = vn.loi.learning.domain.library.model.PackageState.ACTIVE,
                    installedAt = java.time.Instant.ofEpochMilli(500L),
                    contentCount = 0,
                    learningItemCount = 0
                )
            )
            val itemId = LearningItemId("legacy-item-$index")
            val contentId = ContentId("legacy-content-$index")
            val startedAt = Moment(1_000L)
            val undo = UndoableSessionReview(
                reviewEventId = ReviewEventId("legacy-review-$index"),
                learningItemId = itemId,
                contentId = contentId,
                memoryStateBefore = MemoryState.new(learnerId, itemId, startedAt),
                memoryStateExistedBefore = false,
                reviewedItemIdsBefore = emptySet(),
                reviewedContentIdsBefore = emptySet(),
                newItemsReviewedBefore = 0,
                reviewItemsReviewedBefore = 0,
                currentItemPresentedAtBefore = startedAt,
                answerRevealedBefore = true
            )
            val legacySession = StudySession.start(
                id = sessionId,
                learnerId = learnerId,
                startedAt = startedAt,
                policy = SessionPolicy(newItemLimit = 1, reviewItemLimit = 1),
                topicId = topicId,
                installedPackageId = installedPackageId
            ).recordReview(
                learningItemId = itemId,
                contentId = contentId,
                wasNewItem = true,
                undoableReview = undo
            ).finish(Moment(2_000L))
            context.studySessionRepository!!.save(legacySession)
            context.studyQueueRepository!!.save(
                StudyQueueSnapshot(
                    sessionId = sessionId,
                    createdAt = startedAt,
                    learningItemIds = listOf(itemId),
                    currentIndex = 1
                )
            )

            val recovered = StudyFacade(context).load()

            assertFalse(recovered.sessionCompleted)
            assertEquals(0, recovered.reviewedCount)
            assertFalse(recovered.canUndo)
            assertNull(context.studySessionRepository!!.findById(sessionId))
            assertNull(context.studyQueueRepository!!.findBySessionId(sessionId))
        }
    }
}
