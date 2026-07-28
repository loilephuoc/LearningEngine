package vn.loi.learning.desktop.ui.study

data class StudyAccessibilityPresentation(
    val statusAnnouncement: String,
    val progressDescription: String?
)

fun resolveStudyAccessibilityPresentation(
    uiState: StudyUiState
): StudyAccessibilityPresentation {
    val progressDescription = uiState.sessionProgress?.let { progress ->
        val total = progress.totalItemCount
        when {
            total == null -> "${progress.reviewedItemCount} items reviewed; total unknown"
            progress.isCompleted ->
                "${progress.completedItemCount} of $total technical experiences completed; " +
                    "${progress.reviewedItemCount} reviewed"
            else ->
                "Technical experience ${progress.currentPosition} of $total; " +
                    "${progress.completedItemCount} completed; " +
                    "${progress.remainingItemCount} remaining"
        }
    } ?: if (uiState.hasKnownTotal) {
        if (uiState.sessionCompleted) "${uiState.totalItems} of ${uiState.totalItems} items completed"
        else "Technical experience ${uiState.currentItemPosition} of ${uiState.totalItems}; " +
            "${uiState.reviewedCount} completed"
    } else null

    val error =
        uiState.loadError
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    val schedulerAnnouncement =
        uiState.schedulerFeedback
            ?.let(::resolveStudySchedulerFeedbackAccessibility)
            ?.announcement

    val statusAnnouncement =
        when {
            error != null ->
                "Study data error. $error. Press Enter or Space to retry loading."

            uiState.sessionCompleted ->
                buildString {
                    append("Study session completed")
                    progressDescription?.let {
                        append(". ")
                        append(it)
                    }
                    schedulerAnnouncement?.let {
                        append(". ")
                        append(it)
                    }
                    append(". Press Enter or Space to start general study.")
                }

            !uiState.hasActiveSession ->
                "Study is ready. Press Enter or Space to start a general study session."

            uiState.canRevealAnswer ->
                buildString {
                    schedulerAnnouncement?.let {
                        append(it)
                        append(" ")
                    }
                    append("Question ready")
                    progressDescription?.let {
                        append(". ")
                        append(it)
                    }
                    append(". Answer hidden. Press Enter or Space to reveal the answer.")
                }

            uiState.canReview ->
                buildString {
                    append("Answer revealed")
                    progressDescription?.let {
                        append(". ")
                        append(it)
                    }
                    append(". Choose a rating: 1 Again, 2 Hard, 3 Good, or 4 Easy.")
                }

            else ->
                buildString {
                    append("Study session active")
                    progressDescription?.let {
                        append(". ")
                        append(it)
                    }
                    schedulerAnnouncement?.let {
                        append(". ")
                        append(it)
                    }
                    append(". Preparing the next action.")
                }
        }

    return StudyAccessibilityPresentation(
        statusAnnouncement = statusAnnouncement,
        progressDescription = progressDescription
    )
}
