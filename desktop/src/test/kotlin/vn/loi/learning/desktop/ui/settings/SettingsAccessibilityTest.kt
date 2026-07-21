package vn.loi.learning.desktop.ui.settings

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SettingsAccessibilityTest {
    @Test
    fun `property exposes label and value as one semantic unit`() {
        val accessibility =
            resolveSettingsPropertyAccessibility(
                label = "Scheduler",
                value = "FSRS"
            )

        assertEquals(
            "Scheduler: FSRS.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `blank property value receives stable fallback`() {
        val accessibility =
            resolveSettingsPropertyAccessibility(
                label = "Runtime",
                value = "   "
            )

        assertEquals(
            "Unavailable",
            accessibility.value
        )
        assertEquals(
            "Runtime: Unavailable.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `section summary follows visible property order`() {
        val description =
            resolveSettingsSectionContentDescription(
                title = "Learning Engine",
                properties =
                    listOf(
                        "Scheduler" to "FSRS",
                        "Architecture" to "Clean Architecture + DDD",
                        "Persistence" to "JSON"
                    )
            )

        assertEquals(
            "Learning Engine settings. " +
                "Scheduler: FSRS. " +
                "Architecture: Clean Architecture + DDD. " +
                "Persistence: JSON.",
            description
        )
        assertContains(
            description,
            "Learning Engine settings"
        )
    }
}
