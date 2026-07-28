package vn.loi.learning.desktop.ui.study

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import vn.loi.learning.application.study.ContentStageQueryService
import vn.loi.learning.application.review.ReviewCommand
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
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ContentStagePresentationTest {

    private val learnerId = LearnerId("default-learner")

    @Test
    fun `A - Application projection resolves content stage from latest lastReviewedAt without writing state`() {
        val context = LearningApplicationFactory.createInMemory()
        val contentId = ContentId("content-path")
        val itemA = LearningItem(LearningItemId("path-meaning-rec"), contentId, LearningMode.MEANING_RECOGNITION)
        val itemB = LearningItem(LearningItemId("path-listening-rec"), contentId, LearningMode.LISTENING_RECOGNITION)
        context.learningItemRepository!!.save(itemA)
        context.learningItemRepository!!.save(itemB)

        val queryService = ContentStageQueryService(
            context.learningItemRepository!!,
            context.memoryStateRepository!!
        )

        // 1. All unpersisted -> NEW
        assertEquals(LearningStage.NEW, queryService.resolveContentStage(learnerId, contentId))
        assertNull(context.memoryStateRepository!!.find(learnerId, itemA.id))
        assertNull(context.memoryStateRepository!!.find(learnerId, itemB.id))

        // 2. Sibling itemA has REVIEW -> content presentation stage is REVIEW
        val stateA = MemoryState(learnerId, itemA.id, LearningStage.REVIEW, 5.0, 2.0, Moment(5_000), Moment(1_000), 1, 0)
        context.memoryStateRepository!!.save(stateA)

        assertEquals(LearningStage.REVIEW, queryService.resolveContentStage(learnerId, contentId))
        // Unpersisted itemB remains unpersisted (never constructs effective NEW in repository)
        assertNull(context.memoryStateRepository!!.find(learnerId, itemB.id))

        // 3. Multiple persisted states -> picks MemoryState with latest lastReviewedAt
        val stateB = MemoryState(learnerId, itemB.id, LearningStage.LEARNING, 5.0, 1.0, Moment(3_000), Moment(2_000), 2, 0)
        context.memoryStateRepository!!.save(stateB)

        assertEquals(LearningStage.LEARNING, queryService.resolveContentStage(learnerId, contentId))

        // 4. Deterministic tie handling when lastReviewedAt is equal
        val stateBEqualTime = MemoryState(learnerId, itemB.id, LearningStage.LEARNING, 5.0, 1.0, Moment(3_000), Moment(1_000), 2, 0)
        context.memoryStateRepository!!.save(stateBEqualTime)

        val tiedStage = queryService.resolveContentStage(learnerId, contentId)
        assertNotNull(tiedStage)
    }

    @Test
    fun `B - Desktop regression verifies contentPresentationStage is REVIEW while current learning-item stage remains NEW`() {
        val context = LearningApplicationFactory.createInMemory()
        val (itemA, itemB) = registerMultiModePackage(context)

        context.engine.review(
            ReviewCommand(
                ReviewEventId("content-path-prior-good"),
                learnerId,
                itemA,
                ReviewRating.GOOD,
                Moment(System.currentTimeMillis())
            )
        )

        val facade = StudyFacade(context)
        val uiState = facade.startStudy()

        assertEquals(itemB.value, uiState.currentLearningItemId)
        // Content presentation stage is REVIEW for learner-facing badge
        assertEquals(LearningStage.REVIEW, uiState.contentPresentationStage)
        // Current learning item (itemB) remains NEW in scheduler/item stage & diagnostics
        assertEquals(LearningStage.NEW, uiState.learningStage)
        assertEquals(LearningStage.NEW, uiState.learningStageDiagnostics?.stage)
        assertEquals(SessionItemOrigin.REVIEW, uiState.currentItemReviewContext?.origin)
        assertEquals(ReviewRating.GOOD, uiState.currentItemReviewContext?.previousRating)
        assertEquals(
            true,
            isPreviousRatingIndicator(
                StudyActionControl.REVIEW_GOOD,
                uiState.currentItemReviewContext
            )
        )
        assertEquals(0, uiState.newItemsReviewed)

        // Verify no extra MemoryState was written for itemB
        assertNull(context.memoryStateRepository!!.find(learnerId, itemB))

        facade.revealAnswer()
        val afterHard = facade.review(ReviewRating.HARD)
        assertEquals(0, afterHard.newItemsReviewed)
        assertEquals(1, afterHard.reviewItemsReviewed)
        val afterHardHeader = facade.refreshHeaderStatistics(afterHard).headerStatistics
            as StudyHeaderStatisticsState.Available
        assertEquals(1, afterHardHeader.value.total)
        assertEquals(1, afterHardHeader.value.hardCount)
        assertEquals(0, afterHardHeader.value.goodCount)

        val undone = facade.undoLatestReview()
        assertEquals(0, undone.newItemsReviewed)
        assertEquals(0, undone.reviewItemsReviewed)
        assertEquals(ReviewRating.GOOD, undone.currentItemReviewContext?.previousRating)
        val undoHeader = facade.refreshHeaderStatistics(undone).headerStatistics
            as StudyHeaderStatisticsState.Available
        assertEquals(1, undoHeader.value.total)
        assertEquals(0, undoHeader.value.hardCount)
        assertEquals(1, undoHeader.value.goodCount)
    }

    @Test
    fun `C - Restart persisted composition retains content presentation stage REVIEW across application restarts`() {
        val tempDir = Files.createTempDirectory("content_stage_restart").toFile()
        try {
            // 1. Initial platform composition: Save sibling REVIEW state to StoreBackedMemoryStateRepository
            val context1 = LearningApplicationFactory.createPersisted(tempDir.toPath())
            val (itemA, itemB) = registerMultiModePackage(context1)
            context1.engine.review(
                ReviewCommand(
                    ReviewEventId("content-path-restart-good"),
                    learnerId,
                    itemA,
                    ReviewRating.GOOD,
                    Moment(System.currentTimeMillis())
                )
            )

            // 2. Reconstruct application / Desktop composition after restart
            val context2 = LearningApplicationFactory.createPersisted(tempDir.toPath())
            val facade2 = StudyFacade(context2)
            val restoredUiState = facade2.startStudy()

            assertEquals(itemB.value, restoredUiState.currentLearningItemId)
            // Learner-facing badge displays content presentation stage REVIEW
            assertEquals(LearningStage.REVIEW, restoredUiState.contentPresentationStage)
            assertEquals(LearningStage.NEW, restoredUiState.learningStage)
            assertEquals(SessionItemOrigin.REVIEW, restoredUiState.currentItemReviewContext?.origin)
            assertEquals(ReviewRating.GOOD, restoredUiState.currentItemReviewContext?.previousRating)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `D - Isolation verifies unlearned content Y returns NEW and does not consume content X memory state`() {
        val context = LearningApplicationFactory.createInMemory()
        val contentX = ContentId("content-x")
        val contentY = ContentId("content-y")

        val itemX = LearningItem(LearningItemId("item-x"), contentX, LearningMode.MEANING_RECOGNITION)
        val itemY = LearningItem(LearningItemId("item-y"), contentY, LearningMode.MEANING_RECOGNITION)

        context.learningItemRepository!!.save(itemX)
        context.learningItemRepository!!.save(itemY)

        val stateX = MemoryState(learnerId, itemX.id, LearningStage.REVIEW, 5.0, 2.0, Moment(5_000), Moment(1_000), 1, 0)
        context.memoryStateRepository!!.save(stateX)

        val queryService = ContentStageQueryService(
            context.learningItemRepository!!,
            context.memoryStateRepository!!
        )

        assertEquals(LearningStage.REVIEW, queryService.resolveContentStage(learnerId, contentX))
        assertEquals(LearningStage.NEW, queryService.resolveContentStage(learnerId, contentY))
    }

    private fun registerMultiModePackage(
        context: LearningApplicationContext
    ): Pair<LearningItemId, LearningItemId> {
        val packageId = PackageId("multi-mode-package")
        val installedPackageId = InstalledPackageId(packageId.value)
        val topicId = TopicId("multi-mode-topic")
        val libraryId = ContentLibraryId("multi-mode-library")
        val contentId = ContentId("content-path")

        context.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = installedPackageId,
                libraryId = LibraryId("default-library"),
                packageId = packageId,
                topicId = topicId,
                name = PackageName("Multi Mode"),
                version = PackageVersion("1.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.ofEpochMilli(500L),
                contentCount = 1,
                learningItemCount = 2
            )
        )
        context.contentLibraryRepository!!.save(
            ContentLibrary(libraryId, LibraryDescriptor("Multi Mode"), setOf(contentId))
        )
        context.contentPackageRepository!!.save(
            ContentPackage(
                packageId,
                PackageDescriptor("Multi Mode", "1.0", "OPD3"),
                setOf(libraryId),
                topicId
            )
        )

        context.engine.registerContent(
            Content(contentId, ContentType.WORD, ContentText("Path", "Con đường"))
        )

        val itemAId = LearningItemId("item-mode-a")
        val itemBId = LearningItemId("item-mode-b")

        context.engine.registerLearningItem(
            LearningItem(itemAId, contentId, LearningMode.MEANING_RECOGNITION)
        )
        context.engine.registerLearningItem(
            LearningItem(itemBId, contentId, LearningMode.LISTENING_RECOGNITION)
        )

        return Pair(itemAId, itemBId)
    }
}
