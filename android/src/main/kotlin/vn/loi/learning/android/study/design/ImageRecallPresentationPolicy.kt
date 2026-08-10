package vn.loi.learning.android.study.design

import vn.loi.learning.domain.study.recall.RecallOutcome

internal fun resolveImageRecallDensity(
    base: StudyContentDensity,
    imeVisible: Boolean,
    promptLength: Int,
    feedbackVisible: Boolean
): StudyContentDensity = when {
    imeVisible -> StudyContentDensity.DENSE
    feedbackVisible || promptLength > 100 -> StudyContentDensity.STANDARD
    else -> base
}

internal fun imageRecallMediaRole(
    imeVisible: Boolean,
    feedbackVisible: Boolean
): StudyMediaRole = when {
    feedbackVisible -> StudyMediaRole.SUPPORTING
    imeVisible -> StudyMediaRole.STANDARD
    else -> StudyMediaRole.HERO
}

internal fun imageRecallFeedback(outcome: RecallOutcome?): StudyFeedbackVisualState = when (outcome) {
    RecallOutcome.CORRECT -> StudyFeedbackVisualState.CORRECT
    RecallOutcome.INCORRECT -> StudyFeedbackVisualState.INCORRECT
    RecallOutcome.REVEALED -> StudyFeedbackVisualState.SELECTED
    null -> StudyFeedbackVisualState.NEUTRAL
    else -> StudyFeedbackVisualState.SELECTED
}

internal fun imageRecallMediaMotionMillis(reducedMotion: Boolean): Int =
    studyMotionDurationMillis(StudyMotionRole.MEDIA_RESIZE, reducedMotion)
