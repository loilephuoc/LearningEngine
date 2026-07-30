package vn.loi.learning.desktop.ui.study

data class StudyIdlePresentation(
    val title: String,
    val description: String,
    val actionLabel: String,
    val shortcutHint: String,
    val actions: List<StudyIdleActionPresentation> = emptyList()
)

enum class StudyIdleAction {
    CONTINUE,
    REPLAY_LATEST,
    REVIEW_ALL_LEARNED,
    BACK_TO_LIBRARY
}

data class StudyIdleActionPresentation(
    val action: StudyIdleAction,
    val label: String,
    val description: String,
    val enabled: Boolean,
    val supportingCount: Int? = null
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

    val availability = uiState.learnEntryReviewAvailability
    val latest =
        availability?.latestCompletedSession
            as? vn.loi.learning.application.session.LatestCompletedSessionAvailability.Available
    val learned =
        availability?.learnedItems
            as? vn.loi.learning.application.session.LearnedItemsReviewAvailability.Available
    return StudyIdlePresentation(
        title = "Bạn muốn học gì?",
        description =
            "Chọn cách bắt đầu phiên học trong phạm vi hiện tại.",
        actionLabel = "Học tiếp",
        shortcutHint = "Enter or Space",
        actions = listOf(
            StudyIdleActionPresentation(
                StudyIdleAction.CONTINUE,
                "Học tiếp",
                "Học item mới và ôn tập theo cấu hình Session hiện tại.",
                enabled = true
            ),
            StudyIdleActionPresentation(
                StudyIdleAction.REPLAY_LATEST,
                "Ôn lại phiên gần nhất",
                if (latest == null) {
                    "Chưa có Session hoàn tất phù hợp trong phạm vi hiện tại."
                } else {
                    "Ôn lại ${latest.itemCount} item đã được đánh giá trong Session hoàn tất gần nhất."
                },
                enabled = latest != null,
                supportingCount = latest?.itemCount
            ),
            StudyIdleActionPresentation(
                StudyIdleAction.REVIEW_ALL_LEARNED,
                "Ôn lại tất cả đã học",
                if (learned == null) {
                    "Chưa có item đã học để ôn lại."
                } else {
                    "Ôn ${learned.sessionItemCount} trong ${learned.totalLearnedCount} item đã học trong scope hiện tại."
                },
                enabled = learned != null,
                supportingCount = learned?.sessionItemCount
            ),
            StudyIdleActionPresentation(
                StudyIdleAction.BACK_TO_LIBRARY,
                "Back to Library",
                "Quay lại Thư viện nội dung.",
                enabled = true
            )
        )
    )
}
