package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.contentlibrary.LessonProgressUiModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonStudyActionType
import vn.loi.learning.desktop.ui.contentlibrary.PackageLearningRecommendation
import vn.loi.learning.desktop.ui.contentlibrary.RecommendationReasonType
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class SessionCompletionProjectionPolicyTest {

    private fun createStudyUiState(
        completed: Boolean = true,
        hasActiveSession: Boolean = false,
        pkgId: InstalledPackageId? = InstalledPackageId("pkg-1"),
        studyTitle: String = "Lesson 1",
        reviewedCount: Int = 10,
        newItems: Int = 4,
        reviewItems: Int = 6,
        totalItems: Int = 10,
        contentId: ContentId? = ContentId("cnt-1")
    ): StudyUiState {
        return StudyUiState(
            hasActiveSession = hasActiveSession,
            sessionCompleted = completed,
            activeInstalledPackageId = pkgId,
            activeContentId = contentId,
            studyTitle = studyTitle,
            reviewedCount = reviewedCount,
            newItemsReviewed = newItems,
            reviewItemsReviewed = reviewItems,
            totalItems = totalItems,
            currentLearningItemId = "item-123"
        )
    }

    private fun createLessonProgress(
        total: Int = 10,
        started: Int = 10,
        mastered: Int = 8,
        due: Int = 2
    ): LessonProgressUiModel {
        return LessonProgressUiModel(
            totalLearningItemCount = total,
            unseenItemCount = total - started,
            newStateItemCount = 0,
            startedItemCount = started,
            masteredItemCount = mastered,
            dueItemCount = due,
            suspendedItemCount = 0,
            completionPercent = (mastered * 100) / total,
            startedPercent = (started * 100) / total
        )
    }

    @Test
    fun `1 completed session maps to completed presentation`() {
        val state = createStudyUiState(completed = true)
        val completion = SessionCompletionProjectionPolicy.create(state)

        assertEquals(SessionCompletionStatus.COMPLETED, completion.status)
        assertEquals("Session Completed", completion.statusLabel)
    }

    @Test
    fun `2 non-completed status does not claim completed`() {
        val statePaused = createStudyUiState(completed = false, hasActiveSession = true)
        val completionPaused = SessionCompletionProjectionPolicy.create(statePaused)
        assertEquals(SessionCompletionStatus.PAUSED, completionPaused.status)
        assertEquals("Session Paused", completionPaused.statusLabel)

        val stateStopped = createStudyUiState(completed = false, hasActiveSession = false)
        val completionStopped = SessionCompletionProjectionPolicy.create(stateStopped)
        assertEquals(SessionCompletionStatus.STOPPED, completionStopped.status)
        assertEquals("Session Stopped", completionStopped.statusLabel)
    }

    @Test
    fun `3 real InstalledPackageId preserved`() {
        val pkgId = InstalledPackageId("pkg-real-99")
        val state = createStudyUiState(pkgId = pkgId)
        val completion = SessionCompletionProjectionPolicy.create(state)

        assertEquals(pkgId, completion.installedPackageId)
    }

    @Test
    fun `4 real ContentId preserved`() {
        val contentId = ContentId("cnt-real-888")
        val state = createStudyUiState(contentId = contentId)
        val completion = SessionCompletionProjectionPolicy.create(state)

        assertEquals(contentId, completion.contentId)
    }

    @Test
    fun `5 packageName preserved`() {
        val state = createStudyUiState()
        val completion = SessionCompletionProjectionPolicy.create(state)

        assertNull(completion.packageName) // Null when sessionOverview absent
    }

    @Test
    fun `6 lessonTitle preserved`() {
        val state = createStudyUiState(studyTitle = "Japanese N5 Vocab")
        val completion = SessionCompletionProjectionPolicy.create(state)

        assertEquals("Japanese N5 Vocab", completion.lessonTitle)
    }

    @Test
    fun `7 authoritative processed count preserved`() {
        val state = createStudyUiState(reviewedCount = 15, newItems = 5, reviewItems = 10)
        val completion = SessionCompletionProjectionPolicy.create(state)

        assertEquals(15, completion.reviewedCount)
        assertEquals(5, completion.newItemsReviewed)
        assertEquals(10, completion.reviewItemsReviewed)
    }

    @Test
    fun `8 progress metrics passed through unchanged`() {
        val progress = createLessonProgress(total = 20, started = 15, mastered = 12, due = 3)
        val state = createStudyUiState()
        val completion = SessionCompletionProjectionPolicy.create(state, lessonProgress = progress)

        assertNotNull(completion.lessonProgress)
        assertEquals(20, completion.lessonProgress!!.totalLearningItemCount)
        assertEquals(12, completion.lessonProgress!!.masteredItemCount)
        assertEquals(3, completion.lessonProgress!!.dueItemCount)
    }

    @Test
    fun `9 next action comes from LessonStudyAction`() {
        val progress = createLessonProgress(total = 10, started = 10, mastered = 5, due = 2)
        val state = createStudyUiState()
        val completion = SessionCompletionProjectionPolicy.create(state, lessonProgress = progress)

        assertNotNull(completion.nextAction)
        assertEquals(LessonStudyActionType.CONTINUE, completion.nextAction!!.type)
        assertEquals("Continue Lesson", completion.nextAction!!.label)
    }

    @Test
    fun `10 recommendation only shown for correct package context`() {
        val contentId = ContentId("cnt-1")
        val state = createStudyUiState(contentId = contentId)
        val reco = PackageLearningRecommendation(
            contentId = contentId,
            lessonTitle = "Lesson 1",
            reasonType = RecommendationReasonType.DUE_NOW,
            actionLabel = "Review due items",
            reasonText = "2 item(s) are due now",
            totalItemCount = 10,
            masteredItemCount = 5,
            dueItemCount = 2
        )

        val completionRec = SessionCompletionProjectionPolicy.create(state, recommendation = reco)
        assertTrue(completionRec.isRecommended)
        assertEquals("2 item(s) are due now", completionRec.recommendationReason)

        val otherReco = reco.copy(contentId = ContentId("cnt-other"))
        val completionOther = SessionCompletionProjectionPolicy.create(state, recommendation = otherReco)
        assertFalse(completionOther.isRecommended)
        assertNull(completionOther.recommendationReason)
    }

    @Test
    fun `11 missing optional metrics produces valid degraded state`() {
        val state = createStudyUiState(pkgId = null)
        val completion = SessionCompletionProjectionPolicy.create(state, lessonProgress = null)

        assertNotNull(completion)
        assertNull(completion.installedPackageId)
        assertNull(completion.lessonProgress)
        assertNull(completion.nextAction)
        assertFalse(completion.isRecommended)
    }

    @Test
    fun `12 repeated projection deterministic`() {
        val state = createStudyUiState()
        val progress = createLessonProgress()

        val c1 = SessionCompletionProjectionPolicy.create(state, progress)
        val c2 = SessionCompletionProjectionPolicy.create(state, progress)
        assertEquals(c1, c2)
    }

    @Test
    fun `13 source input not mutated`() {
        val state = createStudyUiState(reviewedCount = 8)
        val progress = createLessonProgress()

        SessionCompletionProjectionPolicy.create(state, progress)

        assertEquals(8, state.reviewedCount)
        assertEquals(10, progress.totalLearningItemCount)
    }

    @Test
    fun `14 LearningItemId is never used as ContentId and explicit authoritative ContentId is projected unchanged`() {
        val stateWithAuthoritativeContent = createStudyUiState(
            contentId = ContentId("cnt-authoritative-55")
        ).copy(currentLearningItemId = "item-999")

        val completionWithContent = SessionCompletionProjectionPolicy.create(stateWithAuthoritativeContent)
        assertEquals(ContentId("cnt-authoritative-55"), completionWithContent.contentId)

        val stateWithoutAuthoritativeContent = createStudyUiState(
            contentId = null
        ).copy(currentLearningItemId = "item-999")

        val completionNullContent = SessionCompletionProjectionPolicy.create(stateWithoutAuthoritativeContent)
        assertNull(completionNullContent.contentId)
    }

    @Test
    fun `15 general completion can continue without content ID`() {
        val completion = SessionCompletionProjectionPolicy.create(
            createStudyUiState(contentId = null).copy(isLessonStudy = false)
        )

        assertTrue(completion.canContinueGeneralStudy)
        assertNull(completion.contentId)
    }

    @Test
    fun `16 lesson completion keeps content-scoped continuation`() {
        val contentId = ContentId("lesson-content")
        val completion = SessionCompletionProjectionPolicy.create(
            createStudyUiState(contentId = contentId).copy(isLessonStudy = true),
            lessonProgress = createLessonProgress()
        )

        assertFalse(completion.canContinueGeneralStudy)
        assertEquals(contentId, completion.contentId)
        assertNotNull(completion.nextAction)
    }
}
