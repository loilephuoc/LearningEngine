package vn.loi.learning.desktop.ui.reviewhistory

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ReviewHistoryAccessibilityTest {
    @Test
    fun `formats singular and plural review counts`() {
        assertEquals(
            "0 reviews",
            formatReviewHistoryCount(0)
        )
        assertEquals(
            "1 review",
            formatReviewHistoryCount(1)
        )
        assertEquals(
            "2 reviews",
            formatReviewHistoryCount(2)
        )
    }

    @Test
    fun `empty state announces context and next action`() {
        val accessibility =
            resolveReviewHistoryEmptyAccessibility()

        assertEquals(
            "No review history",
            accessibility.title
        )
        assertContains(
            accessibility.contentDescription,
            "Review History is empty"
        )
        assertContains(
            accessibility.contentDescription,
            "Complete a study review"
        )
    }

    @Test
    fun `review item exposes one ordered semantic summary`() {
        val description =
            resolveReviewHistoryItemContentDescription(
                ReviewHistoryItemUi(
                    reviewedAt = "21 Jul 2026, 10:30",
                    rating = "Good",
                    responseTime = "2.4 s",
                    stability = "6.0 days",
                    difficulty = "4.2"
                )
            )

        assertEquals(
            "Review rated Good. " +
                "Reviewed 21 Jul 2026, 10:30. " +
                "Response time 2.4 s. " +
                "Stability 6.0 days. " +
                "Difficulty 4.2.",
            description
        )
    }
}
