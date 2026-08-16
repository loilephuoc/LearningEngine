package vn.loi.learning.android.study.design

import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.domain.study.recall.RecallOutcome

internal enum class StudyInputVisualState { IDLE, FOCUSED, CORRECT, INCORRECT, DISABLED }

internal fun resolveStudyInputVisualState(
    enabled: Boolean,
    focused: Boolean,
    typingEvaluation: TypingAnswerEvaluationStatus? = null,
    outcome: RecallOutcome? = null
): StudyInputVisualState = when {
    outcome == RecallOutcome.CORRECT || typingEvaluation == TypingAnswerEvaluationStatus.CORRECT -> StudyInputVisualState.CORRECT
    outcome == RecallOutcome.INCORRECT || typingEvaluation == TypingAnswerEvaluationStatus.INCORRECT -> StudyInputVisualState.INCORRECT
    !enabled -> StudyInputVisualState.DISABLED
    focused -> StudyInputVisualState.FOCUSED
    else -> StudyInputVisualState.IDLE
}

internal fun StudyInputVisualState.feedbackVisual(): StudyFeedbackVisualState = when (this) {
    StudyInputVisualState.CORRECT -> StudyFeedbackVisualState.CORRECT
    StudyInputVisualState.INCORRECT -> StudyFeedbackVisualState.INCORRECT
    StudyInputVisualState.DISABLED, StudyInputVisualState.FOCUSED -> StudyFeedbackVisualState.NEUTRAL
    StudyInputVisualState.IDLE -> StudyFeedbackVisualState.NEUTRAL
}

internal fun resolveTypedModeDensity(
    base: StudyContentDensity,
    imeVisible: Boolean,
    hasImage: Boolean,
    contentLength: Int,
    hasExamples: Boolean
): StudyContentDensity = if (imeVisible) StudyContentDensity.DENSE else when {
    contentLength > 180 && (hasImage || hasExamples) -> StudyContentDensity.DENSE
    base == StudyContentDensity.RELAXED && contentLength < 80 -> StudyContentDensity.RELAXED
    else -> StudyContentDensity.STANDARD
}

internal fun typedModeMediaRole(
    listening: Boolean,
    feedbackVisible: Boolean,
    imeVisible: Boolean = false,
    inputSessionActive: Boolean = false
): StudyMediaRole = when {
    listening -> StudyMediaRole.COMPACT
    (inputSessionActive || imeVisible) && !feedbackVisible -> StudyMediaRole.SUPPORTING
    feedbackVisible -> StudyMediaRole.STANDARD
    else -> StudyMediaRole.STANDARD
}

internal fun resolveTypingFrontMediaBounds(
    density: StudyContentDensity,
    availableHeightDp: Int,
    hasMedia: Boolean = true
): StudyMediaBounds? {
    if (!hasMedia) return null
    val targetMax = 320
    val densityScale = when (density) {
        StudyContentDensity.RELAXED -> 1f
        StudyContentDensity.STANDARD -> 0.92f
        StudyContentDensity.DENSE -> 0.85f
    }
    val minimum = 128
    val viewportCap = (availableHeightDp * 0.55f).toInt().coerceAtLeast(minimum)
    val computedMax = (targetMax * densityScale).toInt().coerceIn(minimum, viewportCap)
    return StudyMediaBounds(minimum, computedMax)
}
