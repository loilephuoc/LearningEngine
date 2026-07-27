package vn.loi.learning.desktop.ui.study

data class StudyIdlePresentation(
    val title: String,
    val description: String,
    val actionLabel: String,
    val shortcutHint: String
)

fun resolveStudyIdlePresentation(
    uiState: StudyUiState
): StudyIdlePresentation? {
    if (
        uiState.hasActiveSession ||
        uiState.sessionCompleted ||
        uiState.loadError != null
    ) {
        return null
    }

    if (uiState.message?.contains("Chưa có chủ đề đang hoạt động") == true) {
        return StudyIdlePresentation(
            title = "Chưa có chủ đề đang hoạt động",
            description = "Hãy vào Thư viện và đặt một chủ đề làm Active trước khi bắt đầu học.",
            actionLabel = "Đi tới Thư viện",
            shortcutHint = "F5"
        )
    }

    return StudyIdlePresentation(
        title = "Ready to study",
        description =
            "Start a general study session to review due items and learn new material.",
        actionLabel = "Start Study",
        shortcutHint = "Enter or Space"
    )
}
