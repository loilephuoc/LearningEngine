package vn.loi.learning.desktop.ui.study

data class StudyIdlePresentation(
    val title: String,
    val description: String,
    val actionLabel: String,
    val shortcutHint: String,
    val actions: List<StudyLearningActionPresentation> = emptyList()
)

enum class StudyLearningAction {
    CONTINUE,
    REPLAY_LATEST,
    REVIEW_ALL_LEARNED,
    BACK_TO_LIBRARY
}

data class StudyLearningActionPresentation(
    val action: StudyLearningAction,
    val label: String,
    val description: String,
    val enabled: Boolean,
    val supportingCount: Int? = null
)

data class StudyLearningActionCallbacks(
    val continueLearning: () -> Unit,
    val replayLatestCompletedSession: () -> Unit,
    val reviewAllLearned: () -> Unit,
    val backToLibrary: () -> Unit
)

fun dispatchStudyLearningAction(
    action: StudyLearningAction,
    callbacks: StudyLearningActionCallbacks
) {
    when (action) {
        StudyLearningAction.CONTINUE -> callbacks.continueLearning()
        StudyLearningAction.REPLAY_LATEST -> callbacks.replayLatestCompletedSession()
        StudyLearningAction.REVIEW_ALL_LEARNED -> callbacks.reviewAllLearned()
        StudyLearningAction.BACK_TO_LIBRARY -> callbacks.backToLibrary()
    }
}

fun resolveStudyLearningActions(
    uiState: StudyUiState,
    continueEnabled: Boolean = true,
    replayEnabled: Boolean? = null
): List<StudyLearningActionPresentation> {
    val availability = uiState.learnEntryReviewAvailability
    val latest =
        availability?.latestCompletedSession
            as? vn.loi.learning.application.session.LatestCompletedSessionAvailability.Available
    val learned =
        availability?.learnedItems
            as? vn.loi.learning.application.session.LearnedItemsReviewAvailability.Available
    return listOf(
        StudyLearningActionPresentation(
            StudyLearningAction.CONTINUE,
            if (uiState.hasActiveSession) "Tiếp tục phiên đang học" else "Học tiếp",
            if (uiState.hasActiveSession) {
                "Tiếp tục đúng item và queue của phiên đang hoạt động."
            } else {
                "Học item mới và ôn tập theo cấu hình Session hiện tại."
            },
            enabled = continueEnabled
        ),
        StudyLearningActionPresentation(
            StudyLearningAction.REPLAY_LATEST,
            "Ôn lại phiên vừa học",
            if (latest == null) {
                "Chưa có Session hoàn tất phù hợp trong phạm vi hiện tại."
            } else {
                "Ôn lại ${latest.itemCount} item đã được đánh giá trong Session hoàn tất gần nhất."
            },
            enabled = replayEnabled ?: (latest != null),
            supportingCount = latest?.itemCount
        ),
        StudyLearningActionPresentation(
            StudyLearningAction.REVIEW_ALL_LEARNED,
            "Ôn lại tất cả đã học",
            if (learned == null) {
                "Chưa có item đã học để ôn lại."
            } else {
                "Ôn toàn bộ ${learned.totalLearnedCount} item đã học trong phạm vi hiện tại."
            },
            enabled = learned != null,
            supportingCount = learned?.sessionItemCount
        ),
        StudyLearningActionPresentation(
            StudyLearningAction.BACK_TO_LIBRARY,
            "Back to Library",
            "Quay lại Thư viện nội dung.",
            enabled = true
        )
    )
}

fun resolveStudyIdlePresentation(
    uiState: StudyUiState
): StudyIdlePresentation? {
    if (
        (uiState.hasActiveSession && !uiState.learnEntryChooserVisible) ||
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
        title = "Bạn muốn học gì?",
        description =
            "Chọn cách bắt đầu phiên học trong phạm vi hiện tại.",
        actionLabel = if (uiState.hasActiveSession) "Tiếp tục phiên đang học" else "Học tiếp",
        shortcutHint = "Enter or Space",
        actions = resolveStudyLearningActions(uiState)
    )
}
