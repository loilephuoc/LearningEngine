package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vn.loi.learning.application.session.LearningSessionProgress

class StudyAccessibilityPresentationTest {

    @Test
    fun `idle state announces the start shortcut`() {
        val presentation = resolveStudyAccessibilityPresentation(StudyUiState())
        assertContains(presentation.statusAnnouncement, "Study is ready")
        assertContains(presentation.statusAnnouncement, "Enter or Space")
        assertNull(presentation.progressDescription)
    }

    @Test
    fun `question state announces hidden answer and lesson progress`() {
        val presentation =
            resolveStudyAccessibilityPresentation(
                StudyUiState(
                    hasActiveSession = true,
                    sessionStarted = true,
                    canRevealAnswer = true,
                    reviewedCount = 2,
                    totalItems = 5,
                    currentItemPosition = 3
                )
            )
        assertContains(presentation.statusAnnouncement, "Question ready")
        assertContains(presentation.statusAnnouncement, "Answer hidden")
        assertEquals("Item 3 of 5; 2 completed", presentation.progressDescription)
    }

    @Test
    fun `next question announces the latest persisted scheduling result`() {
        val presentation =
            resolveStudyAccessibilityPresentation(
                StudyUiState(
                    hasActiveSession = true,
                    sessionStarted = true,
                    canRevealAnswer = true,
                    reviewedCount = 1,
                    totalItems = 4,
                    currentItemPosition = 2,
                    schedulerFeedback =
                        StudySchedulerFeedback(
                            rating = "Good",
                            stageTransition = "Learning to Review",
                            scheduledInterval = "3 days",
                            nextReviewAt = "2026-07-24 08:00",
                            difficultyBefore = "5.00",
                            difficultyAfter = "4.80",
                            stabilityBefore = "1.20",
                            stabilityAfter = "3.40",
                            reviewCount = 4,
                            lapseCount = 1
                        )
                )
            )
        assertContains(presentation.statusAnnouncement, "Latest review saved")
        assertContains(presentation.statusAnnouncement, "Good rating")
        assertContains(presentation.statusAnnouncement, "next interval 3 days")
        assertContains(presentation.statusAnnouncement, "Question ready")
    }

    @Test
    fun `revealed answer announces all rating shortcuts`() {
        val presentation =
            resolveStudyAccessibilityPresentation(
                StudyUiState(
                    hasActiveSession = true,
                    sessionStarted = true,
                    canReview = true,
                    reviewedCount = 1,
                    totalItems = 4,
                    currentItemPosition = 2
                )
            )
        assertContains(presentation.statusAnnouncement, "Answer revealed")
        assertContains(presentation.statusAnnouncement, "1 Again")
        assertContains(presentation.statusAnnouncement, "4 Easy")
    }

    @Test
    fun `completed state announces completed total and restart shortcut`() {
        val presentation =
            resolveStudyAccessibilityPresentation(
                StudyUiState(
                    hasActiveSession = false,
                    sessionCompleted = true,
                    reviewedCount = 3,
                    totalItems = 3,
                    currentItemPosition = 3
                )
            )
        assertContains(presentation.statusAnnouncement, "session completed")
        assertContains(presentation.statusAnnouncement, "3 of 3 items completed")
        assertContains(presentation.statusAnnouncement, "start general study")
    }

    @Test
    fun `authoritative progress announces remaining and reviewed separately`() {
        val presentation = resolveStudyAccessibilityPresentation(
            StudyUiState(
                hasActiveSession = true,
                canRevealAnswer = true,
                sessionProgress = LearningSessionProgress(
                    completedItemCount = 2,
                    reviewedItemCount = 1,
                    skippedItemCount = 1,
                    remainingItemCount = 3,
                    totalItemCount = 5,
                    currentPosition = 3,
                    totalIsKnown = true,
                    isEmpty = false,
                    isCompleted = false
                )
            )
        )

        assertEquals("Item 3 of 5; 2 completed; 3 remaining", presentation.progressDescription)
    }

    @Test
    fun `recoverable error has priority over retained study state`() {
        val presentation =
            resolveStudyAccessibilityPresentation(
                StudyUiState(
                    hasActiveSession = true,
                    canReview = true,
                    loadError = "  Invalid persisted queue.  "
                )
            )
        assertContains(presentation.statusAnnouncement, "Study data error")
        assertContains(presentation.statusAnnouncement, "Invalid persisted queue")
        assertContains(presentation.statusAnnouncement, "retry loading")
    }
}
