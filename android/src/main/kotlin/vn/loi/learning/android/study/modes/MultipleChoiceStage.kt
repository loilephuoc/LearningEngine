package vn.loi.learning.android.study.modes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import vn.loi.learning.android.study.AndroidStudyEvent
import vn.loi.learning.android.study.AndroidStudyState
import vn.loi.learning.android.study.AudioRole
import vn.loi.learning.android.study.components.StudyChoiceTile
import vn.loi.learning.android.study.components.StudyMedia
import vn.loi.learning.android.study.components.StudyPrompt
import vn.loi.learning.android.study.components.StudyStageCard
import vn.loi.learning.android.study.components.ReviewImageNavigationOverlay
import vn.loi.learning.android.study.design.*

@Composable
internal fun MultipleChoiceStudyStage(
    state: AndroidStudyState.MultipleChoice,
    activeRole: AudioRole?,
    baseDensity: StudyContentDensity,
    availableMediaHeightDp: Int,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit,
    feedbackContent: @Composable () -> Unit,
    showFullAnswer: Boolean,
    modifier: Modifier = Modifier
) {
    var pendingChoiceId by remember(state.plan.planId.value) { mutableStateOf<String?>(null) }
    LaunchedEffect(state.selectedChoiceId, state.completed) {
        if (state.selectedChoiceId != null || state.completed) pendingChoiceId = null
    }
    val selectedChoiceId = state.selectedChoiceId ?: pendingChoiceId
    val presentations = resolveChoicePresentations(
        choices = state.choices,
        selectedChoiceId = selectedChoiceId,
        completed = state.completed,
        outcome = state.outcome
    )
    val contentLength = state.question.length + state.choices.sumOf { it.text.length }
    val density = resolveMultipleChoiceDensity(
        baseDensity, state.choices.size, contentLength, !state.resolvedImage.isNullOrBlank()
    )

    StudyStageCard(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(
                if (density == StudyContentDensity.DENSE) StudySpacing.group else StudySpacing.section
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                if (density == StudyContentDensity.DENSE) StudySpacing.group else StudySpacing.section
            )
        ) {
            StudyPrompt(
                text = state.question,
                audioPath = state.resolvedPromptAudio,
                isPlaying = activeRole == AudioRole.PROMPT,
                onToggleAudio = { playAudio(AudioRole.PROMPT, state.resolvedPromptAudio, false) },
                textAlign = TextAlign.Center,
                isLooping = false
            )
            if (state.completed) {
                ReviewImageNavigationOverlay(
                    canPrevious = state.navigation.canPrevious,
                    canNext = state.navigation.canNext,
                    onPrevious = { onEvent(AndroidStudyEvent.PreviousVisited) },
                    onNext = { onEvent(AndroidStudyEvent.NextVisited) },
                    gesturesEnabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StudyMedia(state.resolvedImage, multipleChoiceMediaRole(), density,
                        availableMediaHeightDp, onOpenFullscreenImage)
                }
            } else {
                StudyMedia(state.resolvedImage, multipleChoiceMediaRole(), density,
                    availableMediaHeightDp, onOpenFullscreenImage)
            }
            if (!showFullAnswer) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(StudySpacing.choiceGap)
                ) {
                    presentations.forEach { choice ->
                        StudyChoiceTile(
                            anchor = choice.anchor,
                            text = choice.text,
                            visualState = choice.visualState,
                            enabled = choice.enabled,
                            stateDescriptionText = choice.stateDescription(presentations.size),
                            onClick = {
                                if (pendingChoiceId == null && !state.completed) {
                                    pendingChoiceId = choice.id
                                    onEvent(AndroidStudyEvent.Choose(choice.id))
                                }
                            }
                        )
                    }
                }
            }
            if (showFullAnswer) feedbackContent()
        }
    }
}

private fun StudyChoicePresentation.stateDescription(total: Int): String = buildString {
    append("Option $anchor of $total")
    when (visualState) {
        StudyChoiceVisualState.SELECTED -> append(", selected")
        StudyChoiceVisualState.CORRECT -> append(", correct")
        StudyChoiceVisualState.INCORRECT -> append(", incorrect")
        StudyChoiceVisualState.DISABLED -> append(", unavailable")
        StudyChoiceVisualState.IDLE -> Unit
    }
}
