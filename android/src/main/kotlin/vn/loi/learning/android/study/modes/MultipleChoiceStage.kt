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
import vn.loi.learning.android.study.AndroidStudyEvent
import vn.loi.learning.android.study.AndroidStudyState
import vn.loi.learning.android.study.AudioRole
import vn.loi.learning.android.study.components.StudyChoiceTile
import vn.loi.learning.android.study.components.StudyMedia
import vn.loi.learning.android.study.components.StudyPrompt
import vn.loi.learning.android.study.components.StudyStageCard
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
                onToggleAudio = { playAudio(AudioRole.PROMPT, state.resolvedPromptAudio, true) }
            )
            StudyMedia(
                imagePath = state.resolvedImage,
                role = multipleChoiceMediaRole(),
                density = density,
                availableHeightDp = availableMediaHeightDp,
                onOpenFullscreen = onOpenFullscreenImage
            )
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
            feedbackContent()
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
