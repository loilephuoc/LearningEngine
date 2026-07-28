package vn.loi.learning.desktop.ui.study

import vn.loi.learning.domain.study.session.model.SessionItemOrigin

internal enum class RatingDockMode {
    QUESTION_CONTEXT,
    ANSWER_ACTIONS
}

internal data class RatingSegmentPresentation(
    val control: StudyActionControl,
    val isPreviousRating: Boolean,
    val isSubdued: Boolean
)

internal fun resolveRatingDockPresentation(
    mode: RatingDockMode,
    context: CurrentStudyItemReviewContext?
): List<RatingSegmentPresentation> =
    studyRatingOrder.map { control ->
        val previous =
            context?.origin == SessionItemOrigin.REVIEW &&
                control.ratingOrNull() == context.previousRating
        RatingSegmentPresentation(
            control = control,
            isPreviousRating = previous,
            isSubdued =
                mode == RatingDockMode.QUESTION_CONTEXT &&
                    context?.origin == SessionItemOrigin.REVIEW &&
                    context.previousRating != null &&
                    !previous
        )
    }
