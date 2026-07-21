package vn.loi.learning.desktop.ui.study

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class StudySchedulerFeedbackAccessibilityTest {
    private val feedback =
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

    @Test
    fun `concise summary preserves the scheduling decision`() {
        val presentation =
            resolveStudySchedulerFeedbackAccessibility(feedback)

        assertEquals(
            "Good rating; Learning to Review; next interval 3 days; " +
                "next review 2026-07-24 08:00",
            presentation.conciseSummary
        )
    }

    @Test
    fun `announcement includes saved status and counters`() {
        val presentation =
            resolveStudySchedulerFeedbackAccessibility(feedback)

        assertContains(presentation.announcement, "Latest review saved")
        assertContains(presentation.announcement, "Review count 4")
        assertContains(presentation.announcement, "lapse count 1")
    }
}
