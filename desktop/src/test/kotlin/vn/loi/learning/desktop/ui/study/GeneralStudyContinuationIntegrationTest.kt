package vn.loi.learning.desktop.ui.study

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
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
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class GeneralStudyContinuationIntegrationTest {

    @Test
    fun `general completion starts a new session from durable memory and next new item`() {
        val context = LearningApplicationFactory.createInMemory()
        val itemIds = registerPackage(context, itemCount = 5)
        val learnerId = LearnerId("default-learner")
        val completedSessionId = SessionId("completed-general-session")
        context.engine.startSession(
            StartStudySessionCommand(
                sessionId = completedSessionId,
                learnerId = learnerId,
                startedAt = Moment(1_000L),
                policy = SessionPolicy(newItemLimit = 4, reviewItemLimit = 100),
                installedPackageId = InstalledPackageId("general-package"),
                topicId = TopicId("general-topic")
            )
        )
        val facade = StudyFacade(context)
        var state = facade.load()

        repeat(4) {
            assertEquals(LearningStage.NEW, state.learningStage)
            val diagnostics = assertNotNull(state.learningStageDiagnostics)
            assertEquals(state.currentLearningItemId, diagnostics.learningItemId)
            assertEquals(LearningStage.NEW, diagnostics.stage)
            assertEquals(0, diagnostics.reviewCount)
            assertFalse(diagnostics.hasPersistedMemoryState)
            state = facade.revealAnswer()
            state = facade.review(ReviewRating.GOOD)
        }

        assertTrue(state.sessionCompleted)
        assertEquals(4, state.newItemsReviewed)
        assertEquals(0, state.reviewItemsReviewed)
        assertTrue(state.schedulerFeedback?.stageTransition?.contains("NEW", ignoreCase = true) == true)
        assertTrue(state.schedulerFeedback?.stageTransition?.contains("REVIEW", ignoreCase = true) == true)

        val continued = facade.continueGeneralStudyAfterCompletion()
        val newSession = assertNotNull(context.engine.getActiveSession(learnerId))

        assertNotEquals(completedSessionId, newSession.id)
        assertNull(context.engine.getSession(completedSessionId))
        assertNull(context.engine.getStudyQueue(completedSessionId))
        assertFalse(continued.sessionCompleted)
        assertTrue(continued.hasActiveSession)
        assertEquals(LearningStage.NEW, continued.learningStage)
        assertEquals(itemIds.last(), LearningItemId(assertNotNull(continued.currentLearningItemId)))
        itemIds.take(4).forEach { itemId ->
            assertEquals(LearningStage.REVIEW, context.engine.getMemoryState(learnerId, itemId)?.stage)
        }

        val refreshed = facade.load()
        assertFalse(refreshed.sessionCompleted)
        assertEquals(newSession.id, context.engine.getActiveSession(learnerId)?.id)
    }

    @Test
    fun `general continuation with no candidate returns idle without completion loop`() {
        val context = LearningApplicationFactory.createInMemory()
        val itemId = registerPackage(context, itemCount = 1).single()
        val facade = StudyFacade(context)
        var state = facade.startStudy()
        state = facade.revealAnswer()
        state = facade.review(ReviewRating.GOOD)
        assertTrue(state.sessionCompleted)
        val completedItem = assertNotNull(context.learningItemRepository?.findById(itemId))
        context.learningItemRepository?.save(completedItem.copy(isEnabled = false))

        val continued = facade.continueGeneralStudyAfterCompletion()

        assertFalse(continued.sessionCompleted)
        assertFalse(continued.hasActiveSession)
        assertTrue(continued.message.contains("No learning items"))
        assertNull(context.engine.getActiveSession(LearnerId("default-learner")))
        val reloaded = facade.load()
        assertFalse(reloaded.sessionCompleted)
        assertFalse(reloaded.hasActiveSession)
    }

    private fun registerPackage(
        context: LearningApplicationContext,
        itemCount: Int
    ): List<LearningItemId> {
        val packageId = PackageId("general-package")
        val installedPackageId = InstalledPackageId(packageId.value)
        val topicId = TopicId("general-topic")
        val libraryId = ContentLibraryId("general-library")
        val contentIds = (1..itemCount).map { ContentId("content-$it") }
        context.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = installedPackageId,
                libraryId = LibraryId("default-library"),
                packageId = packageId,
                topicId = topicId,
                name = PackageName("General"),
                version = PackageVersion("1.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.ofEpochMilli(500L),
                contentCount = itemCount,
                learningItemCount = itemCount
            )
        )
        context.contentLibraryRepository!!.save(
            ContentLibrary(libraryId, LibraryDescriptor("General"), contentIds.toSet())
        )
        context.contentPackageRepository!!.save(
            ContentPackage(
                packageId,
                PackageDescriptor("General", "1.0", "OPD3"),
                setOf(libraryId),
                topicId
            )
        )
        return contentIds.mapIndexed { index, contentId ->
            context.engine.registerContent(
                Content(contentId, ContentType.WORD, ContentText("Question ${index + 1}", "Answer"))
            )
            LearningItemId("item-${index + 1}").also { itemId ->
                context.engine.registerLearningItem(
                    LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION)
                )
            }
        }
    }
}
