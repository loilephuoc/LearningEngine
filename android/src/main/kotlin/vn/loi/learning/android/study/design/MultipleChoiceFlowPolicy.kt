package vn.loi.learning.android.study.design

import vn.loi.learning.domain.study.recall.RecallOutcome

internal const val MULTIPLE_CHOICE_MINIMUM_FEEDBACK_MILLIS = 350L
internal const val MULTIPLE_CHOICE_AUDIO_WATCHDOG_MILLIS = 3_000L

internal enum class MultipleChoiceCompletionPath {
    QUESTION,
    CORRECT_FAST_ADVANCE,
    WRONG_OPTION_FEEDBACK,
    WRONG_FULL_ANSWER
}

internal fun multipleChoiceCompletionPath(
    completed: Boolean,
    outcome: RecallOutcome?,
    wrongRevealReady: Boolean
): MultipleChoiceCompletionPath = when {
    !completed -> MultipleChoiceCompletionPath.QUESTION
    outcome == RecallOutcome.CORRECT -> MultipleChoiceCompletionPath.CORRECT_FAST_ADVANCE
    outcome == RecallOutcome.INCORRECT && wrongRevealReady -> MultipleChoiceCompletionPath.WRONG_FULL_ANSWER
    else -> MultipleChoiceCompletionPath.WRONG_OPTION_FEEDBACK
}

internal fun multipleChoiceAllowsManualRating(): Boolean = false

internal fun shouldAutoplayMultipleChoiceQuestion(completed: Boolean, audioPath: String?): Boolean =
    !completed && !audioPath.isNullOrBlank()
