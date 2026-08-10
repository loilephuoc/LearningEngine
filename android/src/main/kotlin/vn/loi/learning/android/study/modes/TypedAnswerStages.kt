package vn.loi.learning.android.study.modes

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.study.*
import vn.loi.learning.android.study.components.*
import vn.loi.learning.android.study.design.*
import vn.loi.learning.android.ui.*
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus

@Composable
internal fun TypingStudyStage(
    state: AndroidStudyState.Typing,
    activeRole: AudioRole?,
    baseDensity: StudyContentDensity,
    availableMediaHeightDp: Int,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit,
    feedbackContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentInput by remember(state.plan.planId.value) { mutableStateOf(state.answer) }
    LaunchedEffect(state.answer) { currentInput = state.answer }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val feedbackVisible = state.completed || state.revealed
    val density = resolveTypedModeDensity(
        baseDensity, imeVisible, !state.resolvedImage.isNullOrBlank(),
        state.prompt.length + state.meaning.orEmpty().length + state.example.orEmpty().length,
        !state.example.isNullOrBlank()
    )
    val inputState = resolveStudyInputVisualState(
        enabled = !feedbackVisible,
        focused = !feedbackVisible,
        typingEvaluation = state.evaluation,
        outcome = state.outcome
    )
    TypedAnswerStageFrame(modifier, inputState.feedbackVisual(), density) {
        StudyPrompt(state.prompt, state.resolvedPromptAudio, activeRole == AudioRole.PROMPT,
            { playAudio(AudioRole.PROMPT, state.resolvedPromptAudio, true) })
        state.pronunciation?.takeIf(String::isNotBlank)?.let {
            Text(it, style = StudyTypography.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StudyMedia(
            state.resolvedImage, typedModeMediaRole(false, feedbackVisible), density, availableMediaHeightDp,
            onOpenFullscreenImage
        )
        StudyAnswerInput(
            state.plan.planId.value, state.answer, !feedbackVisible, state.evaluation == TypingAnswerEvaluationStatus.INCORRECT,
            label = "Type your answer", feedback = inputState.feedbackVisual(),
            onAnswerChanged = { currentInput = it; onEvent(AndroidStudyEvent.AnswerChanged(it)) },
            onSubmit = { onEvent(AndroidStudyEvent.Submit(it)) }
        )
        TypedInputActions(
            currentInput, inputState, showRetry = state.evaluation == TypingAnswerEvaluationStatus.INCORRECT && !state.completed,
            onSubmit = { onEvent(AndroidStudyEvent.Submit(currentInput)) },
            onRetry = { onEvent(AndroidStudyEvent.Retry) }
        )
        feedbackContent()
    }
}

@Composable
internal fun ListeningStudyStage(
    state: AndroidStudyState.Listening,
    activeRole: AudioRole?,
    baseDensity: StudyContentDensity,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    onEvent: (AndroidStudyEvent) -> Unit,
    feedbackContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentInput by remember(state.plan.planId.value) { mutableStateOf(state.answer) }
    LaunchedEffect(state.answer) { currentInput = state.answer }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val density = resolveTypedModeDensity(
        baseDensity, imeVisible, false,
        state.meaning.orEmpty().length + state.example.orEmpty().length,
        !state.example.isNullOrBlank()
    )
    val inputState = resolveStudyInputVisualState(!state.completed && !state.audioUnavailable, !state.completed, outcome = state.outcome)
    TypedAnswerStageFrame(modifier, inputState.feedbackVisual(), density) {
        StudyListeningAudioPrompt(
            isPlaying = activeRole == AudioRole.PROMPT,
            audioAvailable = !state.resolvedPromptAudio.isNullOrBlank(),
            onReplay = { playAudio(AudioRole.PROMPT, state.resolvedPromptAudio, true) }
        )
        StudyAnswerInput(
            state.plan.planId.value, state.answer, !state.completed && !state.audioUnavailable, false,
            label = "Type what you hear", feedback = inputState.feedbackVisual(),
            onAnswerChanged = { currentInput = it; onEvent(AndroidStudyEvent.AnswerChanged(it)) },
            onSubmit = { onEvent(AndroidStudyEvent.Submit(it)) }
        )
        TypedInputActions(currentInput, inputState, false,
            { onEvent(AndroidStudyEvent.Submit(currentInput)) }, { onEvent(AndroidStudyEvent.Retry) })
        feedbackContent()
    }
}

@Composable
private fun TypedAnswerStageFrame(
    modifier: Modifier,
    feedback: StudyFeedbackVisualState,
    density: StudyContentDensity,
    content: @Composable ColumnScope.() -> Unit
) {
    val reducedMotion = isReducedMotionEnabled()
    val feedbackScale by animateFloatAsState(
        targetValue = if (feedback == StudyFeedbackVisualState.NEUTRAL) 1f else 1.01f,
        animationSpec = tween(studyMotionDurationMillis(StudyMotionRole.PRESS, reducedMotion)),
        label = "typed answer feedback"
    )
    StudyStageCard(modifier.fillMaxWidth().graphicsLayer { scaleX = feedbackScale; scaleY = feedbackScale }, feedback = feedback) {
        Column(
            Modifier.fillMaxWidth().padding(if (density == StudyContentDensity.DENSE) StudySpacing.group else StudySpacing.section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (density == StudyContentDensity.DENSE) StudySpacing.micro else StudySpacing.group),
            content = content
        )
    }
}

@Composable
internal fun StudyListeningAudioPrompt(isPlaying: Boolean, audioAvailable: Boolean, onReplay: () -> Unit) {
    val reducedMotion = isReducedMotionEnabled()
    val pulse = rememberInfiniteTransition(label = "listening audio pulse")
    val scale by pulse.animateFloat(
        1f, 1.03f,
        infiniteRepeatable(
            tween(studyMotionDurationMillis(StudyMotionRole.AUDIO_PULSE, reducedMotion).coerceAtLeast(1)),
            RepeatMode.Reverse
        ),
        label = "listening audio scale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(StudySpacing.micro)) {
        Surface(
            shape = StudyShapes.interactive,
            color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(if (isPlaying) 2.dp else 1.dp, if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.graphicsLayer { scaleX = if (isPlaying && !reducedMotion) scale else 1f; scaleY = scaleX }
        ) {
            IconButton(
                onClick = onReplay, enabled = audioAvailable,
                modifier = Modifier.size(72.dp).semantics { stateDescription = if (isPlaying) "Audio playing" else "Audio idle" }
            ) { Icon(Icons.AutoMirrored.Filled.VolumeUp, if (isPlaying) "Replay listening audio, playing" else "Replay listening audio", Modifier.size(32.dp)) }
        }
        Text("Listen carefully", style = StudyTypography.prompt)
    }
}

@Composable
private fun TypedInputActions(
    answer: String,
    visualState: StudyInputVisualState,
    showRetry: Boolean,
    onSubmit: () -> Unit,
    onRetry: () -> Unit
) {
    if (visualState == StudyInputVisualState.CORRECT || visualState == StudyInputVisualState.INCORRECT) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StudySpacing.micro), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            Icon(if (visualState == StudyInputVisualState.CORRECT) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null)
            Text(if (visualState == StudyInputVisualState.CORRECT) "Correct" else "Keep trying", style = StudyTypography.feedback)
        }
    }
    if (!showRetry && visualState in setOf(StudyInputVisualState.IDLE, StudyInputVisualState.FOCUSED, StudyInputVisualState.INCORRECT)) {
        Button(onClick = onSubmit, enabled = answer.isNotBlank(), modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)) { Text("Check") }
    }
    if (showRetry) {
        TextButton(onClick = onRetry, modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)) { Text("Retry") }
    }
}
