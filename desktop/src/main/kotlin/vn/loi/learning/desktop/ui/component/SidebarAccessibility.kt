package vn.loi.learning.desktop.ui.component

import vn.loi.learning.desktop.ui.navigation.NavigationDestination
import vn.loi.learning.desktop.ui.shell.shortcutLabel

data class SidebarDestinationAccessibility(
    val label: String,
    val selected: Boolean,
    val contentDescription: String
)

fun resolveSidebarDestinationAccessibility(
    destination: NavigationDestination,
    currentDestination: NavigationDestination
): SidebarDestinationAccessibility {
    val selected =
        destination == currentDestination

    return SidebarDestinationAccessibility(
        label = destination.label,
        selected = selected,
        contentDescription =
            if (selected) {
                "${destination.label}, selected navigation destination."
            } else {
                "${destination.label}, navigation destination."
            }
    )
}
