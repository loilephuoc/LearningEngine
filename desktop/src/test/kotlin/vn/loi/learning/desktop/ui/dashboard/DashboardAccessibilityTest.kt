package vn.loi.learning.desktop.ui.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals

class DashboardAccessibilityTest {
    @Test
    fun `metric exposes title value and support text in visual order`() {
        val accessibility =
            resolveDashboardMetricAccessibility(
                title = "Due today",
                value = "14",
                supportingText = "Cards waiting for review"
            )

        assertEquals(
            "Due today: 14. Cards waiting for review.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `blank metric values receive stable fallbacks`() {
        val accessibility =
            resolveDashboardMetricAccessibility(
                title = "Retention",
                value = " ",
                supportingText = ""
            )

        assertEquals(
            "Unavailable",
            accessibility.value
        )
        assertEquals(
            "No additional details",
            accessibility.supportingText
        )
        assertEquals(
            "Retention: Unavailable. No additional details.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `section header exposes title and description as one heading`() {
        val accessibility =
            resolveDashboardSectionAccessibility(
                title = "Overview",
                description = "Core learning indicators"
            )

        assertEquals(
            "Overview. Core learning indicators.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `dashboard page header has stable semantic wording`() {
        assertEquals(
            "Dashboard. Your learning progress at a glance.",
            resolveDashboardHeaderContentDescription()
        )
    }
}
