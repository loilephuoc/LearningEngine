package vn.loi.learning.desktop.ui.component

data class AppHeaderAccessibility(
    val productName: String,
    val editionName: String,
    val contentDescription: String
)

data class StatusBarAccessibility(
    val engineName: String,
    val dashboardName: String,
    val contentDescription: String
)

fun resolveAppHeaderAccessibility():
    AppHeaderAccessibility =
    AppHeaderAccessibility(
        productName = "Learning Engine 2.0",
        editionName = "Desktop Edition",
        contentDescription =
            "Learning Engine 2.0. Desktop Edition."
    )

fun resolveStatusBarAccessibility(
    engineName: String,
    dashboardName: String
): StatusBarAccessibility {
    val normalizedEngineName =
        engineName.trim().ifBlank {
            "Engine unavailable"
        }

    val normalizedDashboardName =
        dashboardName.trim().ifBlank {
            "Dashboard unavailable"
        }

    return StatusBarAccessibility(
        engineName = normalizedEngineName,
        dashboardName = normalizedDashboardName,
        contentDescription =
            "Application status. " +
                "Engine: $normalizedEngineName. " +
                "Dashboard: $normalizedDashboardName."
    )
}
