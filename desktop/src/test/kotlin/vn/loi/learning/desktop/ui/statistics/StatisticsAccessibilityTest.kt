package vn.loi.learning.desktop.ui.statistics

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class StatisticsAccessibilityTest {
    @Test
    fun `statistic card exposes title and value as one semantic unit`() {
        val accessibility =
            resolveStatisticAccessibility(
                title = "Success rate",
                value = "82%"
            )

        assertEquals(
            "Success rate: 82%.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `placeholder value is announced as unavailable`() {
        val accessibility =
            resolveStatisticAccessibility(
                title = "Average response time",
                value = "--"
            )

        assertEquals(
            "--",
            accessibility.value
        )
        assertEquals(
            "Average response time: Unavailable.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `blank value receives stable unavailable fallback`() {
        val accessibility =
            resolveStatisticAccessibility(
                title = "Total reviews",
                value = "   "
            )

        assertEquals(
            "Unavailable",
            accessibility.value
        )
        assertEquals(
            "Total reviews: Unavailable.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `screen summary follows visible statistic order`() {
        val description =
            resolveStatisticsScreenContentDescription(
                StatisticsUiState(
                    totalReviews = "10",
                    successfulReviews = "8",
                    againCount = "2",
                    goodCount = "8",
                    successRate = "80%",
                    averageResponseTime = "2.1 s"
                )
            )

        assertEquals(
            "Statistics summary. " +
                "Total reviews: 10. " +
                "Successful reviews: 8. " +
                "Success rate: 80%. " +
                "Again: 2. " +
                "Good: 8. " +
                "Average response time: 2.1 s.",
            description
        )
        assertContains(
            description,
            "Statistics summary"
        )
    }
}
