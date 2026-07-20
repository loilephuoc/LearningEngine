package vn.loi.learning.desktop.ui.dashboard

/**
 * Immutable state của Dashboard UI.
 *
 * Chỉ chứa dữ liệu hiển thị, không chứa business logic.
 */
data class DashboardUiState(
    val totalLearningItems: String = "--",
    val dueToday: String = "--",
    val newItems: String = "--",
    val retention: String = "--",
    val studyStreak: String = "--",
    val lastStudy: String = "--"
)
