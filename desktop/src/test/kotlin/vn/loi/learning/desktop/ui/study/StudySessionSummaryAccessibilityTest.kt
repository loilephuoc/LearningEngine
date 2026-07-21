package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertContains

class StudySessionSummaryAccessibilityTest {
    @Test
    fun `general completion summary exposes counts and next action`() {
        val description =
            resolveStudySessionSummaryAccessibility(
                StudyUiState(
                    sessionCompleted = true,
                    studyTitle = "Daily study",
                    reviewedCount = 4,
                    newItemsReviewed = 2,
                    reviewItemsReviewed = 2
                )
            ).contentDescription

        assertContains(description, "Study session completed")
        assertContains(description, "Daily study")
        assertContains(description, "4 learning items reviewed")
        assertContains(description, "New items 2")
        assertContains(description, "Scheduled review items 2")
        assertContains(description, "Start another study session with Enter")
    }

    @Test
    fun `single reviewed item uses singular wording`() {
        val description =
            resolveStudySessionSummaryAccessibility(
                StudyUiState(
                    sessionCompleted = true,
                    studyTitle = "Quick review",
                    reviewedCount = 1,
                    newItemsReviewed = 1
                )
            ).contentDescription

        assertContains(description, "1 learning item reviewed")
    }

    @Test
    fun `lesson completion includes known lesson progress`() {
        val description =
            resolveStudySessionSummaryAccessibility(
                StudyUiState(
                    sessionCompleted = true,
                    studyTitle = "Lesson 3",
                    reviewedCount = 7,
                    newItemsReviewed = 3,
                    reviewItemsReviewed = 4,
                    isLessonStudy = true,
                    totalItems = 7
                )
            ).contentDescription

        assertContains(description, "Lesson progress 7 of 7 items completed")
    }
}
