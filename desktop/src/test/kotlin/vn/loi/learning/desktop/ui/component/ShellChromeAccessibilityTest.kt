package vn.loi.learning.desktop.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals

class ShellChromeAccessibilityTest {
    @Test
    fun `header exposes product and edition as one heading`() {
        val accessibility =
            resolveAppHeaderAccessibility()

        assertEquals(
            "Learning Engine 2.0",
            accessibility.productName
        )
        assertEquals(
            "Desktop Edition",
            accessibility.editionName
        )
        assertEquals(
            "Learning Engine 2.0. Desktop Edition.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `status bar exposes engine and dashboard in visual order`() {
        val accessibility =
            resolveStatusBarAccessibility(
                engineName = "FSRS Engine",
                dashboardName = "Dashboard ready"
            )

        assertEquals(
            "Application status. " +
                "Engine: FSRS Engine. " +
                "Dashboard: Dashboard ready.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `blank shell status values receive stable fallbacks`() {
        val accessibility =
            resolveStatusBarAccessibility(
                engineName = " ",
                dashboardName = ""
            )

        assertEquals(
            "Engine unavailable",
            accessibility.engineName
        )
        assertEquals(
            "Dashboard unavailable",
            accessibility.dashboardName
        )
        assertEquals(
            "Application status. " +
                "Engine: Engine unavailable. " +
                "Dashboard: Dashboard unavailable.",
            accessibility.contentDescription
        )
    }
}
