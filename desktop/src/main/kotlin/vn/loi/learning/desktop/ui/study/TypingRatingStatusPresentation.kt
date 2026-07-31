package vn.loi.learning.desktop.ui.study

internal enum class TypingRatingStatus {
    CURRENT,
    UPCOMING,
    AVAILABLE
}

internal data class TypingRatingStatusSegment(
    val control: StudyActionControl,
    val status: TypingRatingStatus
) {
    val isActive: Boolean
        get() = status == TypingRatingStatus.CURRENT
}

internal fun resolveTypingRatingStatusPresentation(
    reviewContext: CurrentStudyItemReviewContext?
): List<TypingRatingStatusSegment> {
    val activeIndex = studyRatingOrder.indexOfFirst { control ->
        isPreviousRatingIndicator(control, reviewContext)
    }
    if (activeIndex < 0) return emptyList()

    return studyRatingOrder.mapIndexed { index, control ->
        TypingRatingStatusSegment(
            control = control,
            status = when (index) {
                activeIndex -> TypingRatingStatus.CURRENT
                activeIndex + 1 -> TypingRatingStatus.UPCOMING
                else -> TypingRatingStatus.AVAILABLE
            }
        )
    }
}
