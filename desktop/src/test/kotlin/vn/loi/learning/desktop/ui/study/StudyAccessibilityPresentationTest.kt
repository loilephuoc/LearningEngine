package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StudyAccessibilityPresentationTest {

    @Test
    fun `idle state announces the start shortcut`() {
        val presentation =
            resolveStudyAccessibilityPresentation(
                StudyUiState()
            )

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
        assertEquals(
            "Item 3 of 5; 2 completed",
            presentation.progressDescription
        )
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
