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
import vn.loi.learning.application.session.LearnEntryReviewAvailability
import vn.loi.learning.application.session.LearnedItemsReviewAvailability
import vn.loi.learning.application.session.LatestCompletedSessionAvailability
import vn.loi.learning.domain.study.session.model.SessionId

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
        assertTrue(completion.canReplayCompletedSession)
    }

    @Test
    fun `2 non-completed status does not claim completed`() {
        val statePaused = createStudyUiState(completed = false, hasActiveSession = true)
        val completionPaused = SessionCompletionProjectionPolicy.create(statePaused)
        assertEquals(SessionCompletionStatus.PAUSED, completionPaused.status)
        assertEquals("Session Paused", completionPaused.statusLabel)
        assertFalse(completionPaused.canReplayCompletedSession)

        val stateStopped = createStudyUiState(completed = false, hasActiveSession = false)
        val completionStopped = SessionCompletionProjectionPolicy.create(stateStopped)
        assertEquals(SessionCompletionStatus.STOPPED, completionStopped.status)
        assertEquals("Session Stopped", completionStopped.statusLabel)
    }

    @Test
    fun `completed session replay requires durable non-empty session membership projection`() {
        assertTrue(
            SessionCompletionProjectionPolicy.create(createStudyUiState(totalItems = 10))
                .canReplayCompletedSession
        )
        assertFalse(
            SessionCompletionProjectionPolicy.create(createStudyUiState(totalItems = 0))
                .canReplayCompletedSession
        )
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

    @Test
    fun `17 completion and idle use one semantic learning action set`() {
        val availability = LearnEntryReviewAvailability(
            LatestCompletedSessionAvailability.Available(SessionId("latest"), 4),
            LearnedItemsReviewAvailability.Available(12, 5)
        )
        val completedState = createStudyUiState().copy(
            isLessonStudy = false,
            learnEntryReviewAvailability = availability
        )
        val completion = SessionCompletionProjectionPolicy.create(completedState)
        val idle = resolveStudyIdlePresentation(
            completedState.copy(sessionCompleted = false)
        )!!

        assertEquals(idle.actions.map { it.action }, completion.learningActions.map { it.action })
        assertEquals(StudyLearningAction.entries, completion.learningActions.map { it.action })
        assertTrue(completion.learningActions.all { it.enabled })
    }

    @Test
    fun `18 completion keeps replay and review-all safely unavailable without evidence`() {
        val completion = SessionCompletionProjectionPolicy.create(
            createStudyUiState(totalItems = 0).copy(
                learnEntryReviewAvailability = LearnEntryReviewAvailability(
                    LatestCompletedSessionAvailability.Unavailable,
                    LearnedItemsReviewAvailability.Unavailable
                )
            )
        )

        assertFalse(
            completion.learningActions.single {
                it.action == StudyLearningAction.REPLAY_LATEST
            }.enabled
        )
        assertFalse(
            completion.learningActions.single {
                it.action == StudyLearningAction.REVIEW_ALL_LEARNED
            }.enabled
        )
        assertTrue(completion.reflectionMessage.isNotBlank())
    }

    @Test
    fun `19 shared dispatcher invokes exactly one matching callback`() {
        val calls = mutableListOf<String>()
        val callbacks = StudyLearningActionCallbacks(
            continueLearning = { calls += "continue" },
            replayLatestCompletedSession = { calls += "replay" },
            reviewAllLearned = { calls += "review-all" },
            backToLibrary = { calls += "library" }
        )

        StudyLearningAction.entries.forEach {
            dispatchStudyLearningAction(it, callbacks)
        }

        assertEquals(listOf("continue", "replay", "review-all", "library"), calls)
    }
}
