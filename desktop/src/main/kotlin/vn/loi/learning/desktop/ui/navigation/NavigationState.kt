package vn.loi.learning.desktop.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class NavigationState(
    initialDestination: NavigationDestination = NavigationDestination.DASHBOARD
) {
    var currentDestination by mutableStateOf(initialDestination)
        private set

    fun navigateTo(destination: NavigationDestination) {
        currentDestination = destination
    }

    fun navigatePrevious() {
        currentDestination =
            destinationAtOffset(-1)
    }

    fun navigateNext() {
        currentDestination =
            destinationAtOffset(1)
    }

    private fun destinationAtOffset(
        offset: Int
    ): NavigationDestination {
        val destinations =
            NavigationDestination.entries

        val currentIndex =
            destinations.indexOf(
                currentDestination
            )

        val nextIndex =
            (
                currentIndex +
                    offset +
                    destinations.size
                ) % destinations.size

        return destinations[nextIndex]
    }
}
