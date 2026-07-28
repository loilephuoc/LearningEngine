package vn.loi.learning.desktop.ui.study

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class StudyFacadeStageRehydrationTest {

    @Test
    fun `StudyFacade load rehydrates cached current item with latest authoritative memory stage`() {
        val context = LearningApplicationFactory.createInMemory()
        val learnerId = LearnerId("default-learner")
        val itemId = registerPackage(context, itemCount = 1).single()

        val sessionId = SessionId("rehydration-test-session")
        context.engine.startSession(
            StartStudySessionCommand(
                sessionId = sessionId,
                learnerId = learnerId,
                startedAt = Moment(1_000L),
                policy = SessionPolicy(newItemLimit = 5, reviewItemLimit = 100),
                installedPackageId = InstalledPackageId("rehydrate-package"),
                topicId = TopicId("rehydrate-topic")
            )
        )

        val facade = StudyFacade(context)
        val initialState = facade.startStudy()

        assertEquals(LearningStage.NEW, initialState.learningStage)
        assertEquals(itemId.value, initialState.currentLearningItemId)
        assertNotNull(initialState.domainContent)

        // Authoritative repository updates MemoryState to REVIEW
        val updatedMemoryState = MemoryState(
            learnerId = learnerId,
            learningItemId = itemId,
            stage = LearningStage.REVIEW,
            difficulty = 5.0,
            stabilityDays = 2.0,
            dueAt = Moment(5_000L),
            lastReviewedAt = Moment(1_000L),
            reviewCount = 1,
            lapseCount = 0
        )
        context.memoryStateRepository!!.save(updatedMemoryState)

        // Calling load() on facade must rehydrate to the new authoritative MemoryState stage
        val rehydratedState = facade.load()

        assertEquals(LearningStage.REVIEW, rehydratedState.learningStage)
        assertEquals(itemId.value, rehydratedState.currentLearningItemId)
        assertFalse(rehydratedState.sessionCompleted)

        // Subsequent reload or continuation does not revert to NEW
        val subsequentReload = facade.load()
        assertEquals(LearningStage.REVIEW, subsequentReload.learningStage)
    }

    private fun registerPackage(
        context: LearningApplicationContext,
        itemCount: Int
    ): List<LearningItemId> {
        val packageId = PackageId("rehydrate-package")
        val installedPackageId = InstalledPackageId(packageId.value)
        val topicId = TopicId("rehydrate-topic")
        val libraryId = ContentLibraryId("rehydrate-library")
        val contentIds = (1..itemCount).map { ContentId("content-$it") }
        context.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = installedPackageId,
                libraryId = LibraryId("default-library"),
                packageId = packageId,
                topicId = topicId,
                name = PackageName("Rehydrate"),
                version = PackageVersion("1.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.ofEpochMilli(500L),
                contentCount = itemCount,
                learningItemCount = itemCount
            )
        )
        context.contentLibraryRepository!!.save(
            ContentLibrary(libraryId, LibraryDescriptor("Rehydrate"), contentIds.toSet())
        )
        context.contentPackageRepository!!.save(
            ContentPackage(
                packageId,
                PackageDescriptor("Rehydrate", "1.0", "OPD3"),
                setOf(libraryId),
                topicId
            )
        )
        return contentIds.mapIndexed { index, contentId ->
            context.engine.registerContent(
                Content(contentId, ContentType.WORD, ContentText("Word ${index + 1}", "Meaning"))
            )
            LearningItemId("item-${index + 1}").also { itemId ->
                context.engine.registerLearningItem(
                    LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION)
                )
            }
        }
    }
}
