package vn.loi.learning.android.study.design

import vn.loi.learning.domain.study.recall.RecallChoice
import vn.loi.learning.domain.study.recall.RecallOutcome

internal enum class StudyChoiceVisualState { IDLE, SELECTED, CORRECT, INCORRECT, DISABLED }

internal data class StudyChoicePresentation(
    val id: String,
    val text: String,
    val anchor: String,
    val visualState: StudyChoiceVisualState,
    val enabled: Boolean
)

internal fun resolveChoicePresentations(
    choices: List<RecallChoice>,
    selectedChoiceId: String?,
    completed: Boolean,
    outcome: RecallOutcome?
): List<StudyChoicePresentation> = choices.mapIndexed { index, choice ->
    val visualState = when {
        completed && choice.correct -> StudyChoiceVisualState.CORRECT
        completed && choice.id == selectedChoiceId && outcome == RecallOutcome.INCORRECT ->
            StudyChoiceVisualState.INCORRECT
        completed -> StudyChoiceVisualState.DISABLED
        choice.id == selectedChoiceId -> StudyChoiceVisualState.SELECTED
        else -> StudyChoiceVisualState.IDLE
    }
    StudyChoicePresentation(
        id = choice.id,
        text = choice.text,
        anchor = choiceAnchor(index),
        visualState = visualState,
        enabled = !completed && selectedChoiceId == null
    )
}

internal fun resolveMultipleChoiceDensity(
    base: StudyContentDensity,
    choiceCount: Int,
    contentLength: Int,
    hasImage: Boolean
): StudyContentDensity = when {
    choiceCount >= 5 || contentLength > 180 || (hasImage && choiceCount >= 4 && contentLength > 100) ->
        StudyContentDensity.DENSE
    base == StudyContentDensity.RELAXED && choiceCount <= 3 && contentLength < 100 -> StudyContentDensity.RELAXED
    else -> StudyContentDensity.STANDARD
}

internal fun multipleChoiceMediaRole(): StudyMediaRole = StudyMediaRole.SUPPORTING

internal fun multipleChoiceMotionDurationMillis(
    visualState: StudyChoiceVisualState,
    reducedMotion: Boolean
): Int = when {
    reducedMotion -> 0
    visualState == StudyChoiceVisualState.SELECTED -> 140
    visualState == StudyChoiceVisualState.CORRECT || visualState == StudyChoiceVisualState.INCORRECT -> 160
    else -> 120
}

private fun choiceAnchor(index: Int): String =
    if (index in 0..25) ('A'.code + index).toChar().toString() else (index + 1).toString()
