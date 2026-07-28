package vn.loi.learning.desktop.ui.study

import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.application.study.ContentLearningState

internal fun resolveCurrentStudyItemReviewContext(
    origin: SessionItemOrigin,
    contentLearningState: ContentLearningState
): CurrentStudyItemReviewContext =
    CurrentStudyItemReviewContext(
        origin = origin,
        previousRating =
            if (origin == SessionItemOrigin.REVIEW) {
                contentLearningState.latestEffectiveRating
            } else {
                null
            }
    )

internal fun resolveCurrentStudyItemReviewContext(
    origin: SessionItemOrigin,
    eventsInAuthoritativeOrder: List<ReviewEvent>
): CurrentStudyItemReviewContext =
    CurrentStudyItemReviewContext(
        origin = origin,
        previousRating =
            if (origin == SessionItemOrigin.REVIEW) {
                eventsInAuthoritativeOrder.lastOrNull()?.rating
            } else {
                null
            }
    )

internal fun StudyActionControl.ratingOrNull(): ReviewRating? = when (this) {
    StudyActionControl.REVIEW_AGAIN -> ReviewRating.AGAIN
    StudyActionControl.REVIEW_HARD -> ReviewRating.HARD
    StudyActionControl.REVIEW_GOOD -> ReviewRating.GOOD
    StudyActionControl.REVIEW_EASY -> ReviewRating.EASY
    else -> null
}

internal fun isPreviousRatingIndicator(
    control: StudyActionControl,
    context: CurrentStudyItemReviewContext?
): Boolean =
    context?.origin == SessionItemOrigin.REVIEW &&
        control.ratingOrNull() == context.previousRating
