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
    currentDestination: NavigationDestination,
    label: String = destination.label
): SidebarDestinationAccessibility {
    val selected =
        destination == currentDestination

    return SidebarDestinationAccessibility(
        label = label,
        selected = selected,
        contentDescription =
            if (selected) {
                "$label, selected navigation destination."
            } else {
                "$label, navigation destination."
            }
    )
}
