package vn.loi.learning.android.study.modes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.study.*
import vn.loi.learning.android.study.components.*
import vn.loi.learning.android.study.design.*
import vn.loi.learning.android.ui.LearningSpacing

@Composable
internal fun ExampleCompletionStudyStage(
    state: AndroidStudyState.ExampleCompletion,
    activeRole: AudioRole?,
    baseDensity: StudyContentDensity,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    onEvent: (AndroidStudyEvent) -> Unit,
    feedbackContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentInput by remember(state.plan.planId.value) { mutableStateOf(state.answer) }
    LaunchedEffect(state.answer) { currentInput = state.answer }
    val revealed = state.completed || state.revealed
    val presentation = resolveClozePresentation(
        state.prefix, state.blank, state.suffix, state.example, state.translation
    )
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val density = resolveExampleCompletionDensity(
        baseDensity, imeVisible, presentation.completedSentence.length, state.translation.orEmpty().length
    )
    val feedback = exampleCompletionFeedback(state.outcome)
    StudyStageCard(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(if (density == StudyContentDensity.DENSE) StudySpacing.group else StudySpacing.section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                if (density == StudyContentDensity.DENSE) StudySpacing.group else StudySpacing.section
            )
        ) {
            Text("Complete the sentence", style = StudyTypography.metadata)
            StudyClozeSentence(
                state.prefix,
                state.blank,
                state.suffix,
                revealed,
                feedback,
                state.resolvedPromptAudio,
                activeRole == AudioRole.PROMPT,
                { playAudio(AudioRole.PROMPT, state.resolvedPromptAudio, true) }
            )
            if (revealed) {
                presentation.clozeTranslation?.let {
                    Surface(
                        shape = StudyShapes.semanticSurface,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(it, style = StudyTypography.exampleVietnamese, modifier = Modifier.padding(StudySpacing.group))
                    }
                }
            }
            StudyAnswerInput(
                state.plan.planId.value,
                state.answer,
                !revealed,
                state.outcome == vn.loi.learning.domain.study.recall.RecallOutcome.INCORRECT,
                label = "Missing answer",
                feedback = feedback,
                onAnswerChanged = { currentInput = it; onEvent(AndroidStudyEvent.AnswerChanged(it)) },
                onSubmit = { onEvent(AndroidStudyEvent.Submit(it)) }
            )
            if (!revealed) {
                Button(
                    onClick = { onEvent(AndroidStudyEvent.Submit(currentInput)) },
                    enabled = currentInput.isNotBlank(),
                    modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
                ) { Text("Check") }
            }
            feedbackContent()
        }
    }
}
