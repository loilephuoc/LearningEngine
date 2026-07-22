package vn.loi.learning.desktop.ui.navigation

enum class NavigationDestination(
    val routeId: String,
    val label: String
) {
    DASHBOARD("home", "Home"),
    STUDY("learn", "Learn"),
    STATISTICS("statistics", "Statistics"),
    REVIEW_HISTORY("review", "Review"),
    CONTENT_LIBRARY("library", "Library"),
    SETTINGS("settings", "Settings")
}
