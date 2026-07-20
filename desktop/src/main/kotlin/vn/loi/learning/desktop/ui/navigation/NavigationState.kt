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
}
