package vn.loi.learning.desktop.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationStateTest {
    @Test
    fun `next navigation follows destination order and wraps`() {
        val state =
            NavigationState(
                NavigationDestination.DASHBOARD
            )

        state.navigateNext()
        assertEquals(
            NavigationDestination.STUDY,
            state.currentDestination
        )

        repeat(5) {
            state.navigateNext()
        }

        assertEquals(
            NavigationDestination.DASHBOARD,
            state.currentDestination
        )
    }

    @Test
    fun `previous navigation wraps from first destination`() {
        val state =
            NavigationState(
                NavigationDestination.DASHBOARD
            )

        state.navigatePrevious()

        assertEquals(
            NavigationDestination.SETTINGS,
            state.currentDestination
        )
    }

    @Test
    fun `direct navigation remains authoritative`() {
        val state =
            NavigationState()

        state.navigateTo(
            NavigationDestination.CONTENT_LIBRARY
        )

        assertEquals(
            NavigationDestination.CONTENT_LIBRARY,
            state.currentDestination
        )
    }
}
