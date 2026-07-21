package vn.loi.learning.desktop.ui.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals

class DashboardVisualizationAccessibilityTest {
    @Test
    fun `visualization with data exposes concise chart identity`() {
        val accessibility =
            resolveDashboardVisualizationAccessibility(
                title = "Review forecast",
                hasData = true
            )

        assertEquals(
            "Review forecast visualization.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `visualization without data announces unavailable state`() {
        val accessibility =
            resolveDashboardVisualizationAccessibility(
                title = "Memory distribution",
                hasData = false
            )

        assertEquals(
            "Memory distribution visualization. No data available.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `empty chart message is exposed as one ordered unit`() {
        val accessibility =
            resolveDashboardChartEmptyAccessibility(
                title = "No forecast data yet",
                description = "Complete reviews to generate a forecast."
            )

        assertEquals(
            "No forecast data yet. Complete reviews to generate a forecast.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `blank empty chart text receives stable fallbacks`() {
        val accessibility =
            resolveDashboardChartEmptyAccessibility(
                title = " ",
                description = ""
            )

        assertEquals(
            "No data available",
            accessibility.title
        )
        assertEquals(
            "Complete more learning activity to generate this visualization.",
            accessibility.description
        )
    }

    @Test
    fun `retention value is clamped and announced as a percentage`() {
        val accessibility =
            resolveDashboardRetentionAccessibility(
                retentionLabel = "125%",
                retentionValue = 1.25f
            )

        assertEquals(
            "Average retention: 100 percent.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `blank retention label falls back to calculated percentage`() {
        val accessibility =
            resolveDashboardRetentionAccessibility(
                retentionLabel = " ",
                retentionValue = 0.82f
            )

        assertEquals(
            "82%",
            accessibility.label
        )
        assertEquals(
            "Average retention: 82 percent.",
            accessibility.contentDescription
        )
    }
}
