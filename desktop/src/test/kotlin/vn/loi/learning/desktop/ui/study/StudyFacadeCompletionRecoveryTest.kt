package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
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
                installedAt = java.time.Instant.now(),
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
}
