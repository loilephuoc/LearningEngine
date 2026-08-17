package vn.loi.learning.desktop.ui.browser.imagereuse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.domain.library.model.InstalledPackageId

class ImageReuseReviewViewModelTest {

    private fun createStubCandidate(
        id: String,
        question: String,
        answer: String
    ): ImageReuseSourceCandidate = ImageReuseSourceCandidate(
        sourcePackageId = "src1",
        sourcePackageName = "SourcePackage",
        sourceContentId = id,
        question = question,
        answer = answer,
        translation = answer,
        exampleText = "Example",
        partOfSpeech = "noun",
        imageRef = "media/img_$id.jpg"
    )

    private fun createStubTarget(
        id: String,
        question: String,
        candidates: List<ImageReuseSourceCandidate>
    ): ImageReuseTargetItem = ImageReuseTargetItem(
        targetContentId = id,
        targetLesson = "Unit 1",
        question = question,
        answer = "Nghĩa",
        translation = "Nghĩa",
        exampleText = "Example",
        partOfSpeech = "noun",
        currentImageRef = null,
        candidates = candidates
    )

    @Test
    fun `skip candidate and skip item advance correctly through multi-candidate review state`() {
        val target1 = createStubTarget(
            id = "t1",
            question = "bank",
            candidates = listOf(
                createStubCandidate("c1", "bank", "bờ sông"),
                createStubCandidate("c2", "bank", "ngân hàng")
            )
        )
        val target2 = createStubTarget(
            id = "t2",
            question = "river",
            candidates = listOf(
                createStubCandidate("c3", "river", "con sông")
            )
        )

        val initialStage = ImageReuseReviewStage.Review(
            targetItems = listOf(target1, target2),
            currentTargetIndex = 0,
            currentCandidateIndex = 0,
            appliedCount = 0
        )

        assertEquals("t1", initialStage.currentTarget?.targetContentId)
        assertEquals("c1", initialStage.currentCandidate?.sourceContentId)

        // Advance Candidate (from c1 to c2)
        val nextCandidateStage = initialStage.copy(
            currentCandidateIndex = initialStage.currentCandidateIndex + 1
        )
        assertEquals("t1", nextCandidateStage.currentTarget?.targetContentId)
        assertEquals("c2", nextCandidateStage.currentCandidate?.sourceContentId)

        // Advance Item (from t1 to t2)
        val nextItemStage = nextCandidateStage.copy(
            currentTargetIndex = nextCandidateStage.currentTargetIndex + 1,
            currentCandidateIndex = 0
        )
        assertEquals("t2", nextItemStage.currentTarget?.targetContentId)
        assertEquals("c3", nextItemStage.currentCandidate?.sourceContentId)

        // Advance past end -> complete
        val completeStage = ImageReuseReviewStage.Complete(
            totalReviewedTargets = 2,
            totalAppliedCount = 1
        )
        assertEquals(2, completeStage.totalReviewedTargets)
        assertEquals(1, completeStage.totalAppliedCount)
    }

    @Test
    fun `setup stage toggle select and clear all source packages works`() {
        val options = listOf(
            ImageReusePackageOption("p1", "Package 1", "1.0"),
            ImageReusePackageOption("p2", "Package 2", "2.0")
        )

        var setup = ImageReuseReviewStage.Setup(
            availableSourcePackages = options,
            selectedSourcePackageIds = setOf("p1")
        )

        // Toggle p2 on
        setup = setup.copy(selectedSourcePackageIds = setup.selectedSourcePackageIds + "p2")
        assertEquals(setOf("p1", "p2"), setup.selectedSourcePackageIds)

        // Clear all
        setup = setup.copy(selectedSourcePackageIds = emptySet())
        assertTrue(setup.selectedSourcePackageIds.isEmpty())

        // Select all
        setup = setup.copy(selectedSourcePackageIds = options.map { it.id }.toSet())
        assertEquals(setOf("p1", "p2"), setup.selectedSourcePackageIds)
    }
}
