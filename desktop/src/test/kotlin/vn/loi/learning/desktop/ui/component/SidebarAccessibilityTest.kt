package vn.loi.learning.desktop.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.navigation.NavigationDestination

class SidebarAccessibilityTest {
    @Test
    fun `current destination is announced as selected`() {
        val accessibility =
            resolveSidebarDestinationAccessibility(
                destination = NavigationDestination.DASHBOARD,
                currentDestination = NavigationDestination.DASHBOARD
            )

        assertTrue(accessibility.selected)
        assertEquals(
            "Home, selected navigation destination.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `inactive destination is announced without selected state`() {
        val accessibility =
            resolveSidebarDestinationAccessibility(
                destination = NavigationDestination.STUDY,
                currentDestination = NavigationDestination.DASHBOARD
            )

        assertFalse(accessibility.selected)
        assertEquals(
            "Learn, navigation destination.",
            accessibility.contentDescription
        )
    }

    @Test
    fun `visible label is preserved from destination model`() {
        val accessibility =
            resolveSidebarDestinationAccessibility(
                destination = NavigationDestination.SETTINGS,
                currentDestination = NavigationDestination.DASHBOARD
            )

        assertEquals(
            NavigationDestination.SETTINGS.label,
            accessibility.label
        )
    }
}
