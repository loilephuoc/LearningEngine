package vn.loi.learning.android.study.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.study.design.*
import vn.loi.learning.android.ui.*

@Composable
internal fun StudyRuntimeShell(
    title: String,
    modeLabel: String,
    currentPosition: Int?,
    totalItems: Int?,
    onBack: () -> Unit,
    header: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val policy = LocalLayoutPolicy.current
    Scaffold(topBar = {
        LearningEngineStudyTopBar(title, modeLabel, currentPosition, totalItems, onBack)
    }) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = policy.maxContentWidthDp.dp).fillMaxHeight().fillMaxWidth()
                    .padding(horizontal = policy.horizontalPaddingDp.dp, vertical = policy.verticalPaddingDp.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StudySpacing.controlsGap)
            ) {
                header()
                content()
            }
        }
    }
}

@Composable
internal fun StudyStageCard(
    modifier: Modifier = Modifier,
    feedback: StudyFeedbackVisualState = StudyFeedbackVisualState.NEUTRAL,
    translationY: Float = 0f,
    content: @Composable () -> Unit
) {
    val palette = studyFeedbackPalette(feedback)
    Surface(
        modifier = modifier.graphicsLayer { this.translationY = translationY },
        shape = StudyShapes.stage,
        color = palette?.container ?: MaterialTheme.colorScheme.surface,
        border = palette?.let { BorderStroke(2.dp, it.border) },
        shadowElevation = if (feedback == StudyFeedbackVisualState.NEUTRAL) LearningElevation.flat else LearningElevation.raised,
        content = content
    )
}

@Composable
private fun studyFeedbackPalette(state: StudyFeedbackVisualState): StudyFeedbackPalette? = when (state) {
    StudyFeedbackVisualState.NEUTRAL -> null
    StudyFeedbackVisualState.SELECTED, StudyFeedbackVisualState.AUDIO_ACTIVE -> StudyFeedbackPalette(
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f), MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.onSurface
    )
    StudyFeedbackVisualState.CORRECT, StudyFeedbackVisualState.RATING_GOOD -> StudyRatingColors.good.let { StudyFeedbackPalette(it.background, it.border, it.content) }
    StudyFeedbackVisualState.INCORRECT, StudyFeedbackVisualState.RATING_AGAIN -> StudyRatingColors.again.let { StudyFeedbackPalette(it.background, it.border, it.content) }
    StudyFeedbackVisualState.RATING_HARD -> StudyRatingColors.hard.let { StudyFeedbackPalette(it.background, it.border, it.content) }
    StudyFeedbackVisualState.RATING_EASY -> StudyRatingColors.easy.let { StudyFeedbackPalette(it.background, it.border, it.content) }
}

@Composable
internal fun StudyMedia(
    imagePath: String?,
    role: StudyMediaRole,
    density: StudyContentDensity,
    availableHeightDp: Int,
    onOpenFullscreen: (String) -> Unit,
    modifier: Modifier = Modifier,
    feedbackScale: Float = 1f
) {
    val bounds = resolveStudyMediaBounds(role, density, availableHeightDp, !imagePath.isNullOrBlank()) ?: return
    LearningEngineImage(
        imagePath = imagePath,
        imageUnavailable = false,
        onOpenFullscreen = onOpenFullscreen,
        fillCanvas = true,
        adaptiveFitBounds = LearningImageFitBounds(bounds.minHeightDp, bounds.maxHeightDp),
        modifier = modifier.fillMaxWidth().graphicsLayer { scaleX = feedbackScale; scaleY = feedbackScale }
    )
}

@Composable
internal fun StudyPrompt(
    text: String,
    audioPath: String?,
    isPlaying: Boolean,
    onToggleAudio: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: Boolean = false
) {
    LearningEngineAudioTextRow(
        text = text,
        style = if (supporting) StudyTypography.metadata else StudyTypography.prompt,
        color = if (supporting) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        audioPath = audioPath,
        isPlaying = isPlaying,
        isLooping = true,
        onToggleAudio = onToggleAudio,
        headingSemantics = true,
        modifier = modifier
    )
}

@Composable
internal fun StudyChoiceTile(
    label: String = "Answer",
    isSelected: Boolean,
    enabled: Boolean,
    stateDescriptionText: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick, enabled = enabled, shape = StudyShapes.interactive,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).semantics {
            selected = isSelected; stateDescription = stateDescriptionText
        }
    ) { Text(label, style = StudyTypography.choice, modifier = Modifier.fillMaxWidth()) }
}

@Composable
internal fun StudyAnswerInput(
    planId: String,
    initialAnswer: String,
    enabled: Boolean,
    error: Boolean,
    label: String = "Answer",
    feedback: StudyFeedbackVisualState = StudyFeedbackVisualState.NEUTRAL,
    onAnswerChanged: (String) -> Unit,
    onSubmit: (String) -> Unit
) {
    var value by rememberSaveable(planId, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialAnswer, TextRange(initialAnswer.length)))
    }
    LaunchedEffect(initialAnswer) { if (initialAnswer.isEmpty() && value.text.isNotEmpty()) value = TextFieldValue("") }
    val focusRequester = remember(planId) { FocusRequester() }
    val bringIntoView = remember(planId) { BringIntoViewRequester() }
    var focused by rememberSaveable(planId) { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(planId, enabled) {
        if (enabled && !focused) { focused = true; focusRequester.requestFocus(); bringIntoView.bringIntoView() }
        if (!enabled) { keyboard?.hide(); focusManager.clearFocus() }
    }
    OutlinedTextField(
        value, { if (enabled) { value = it; onAnswerChanged(it.text) } }, enabled = enabled,
        minLines = 1, maxLines = 4, isError = error || feedback == StudyFeedbackVisualState.INCORRECT,
        textStyle = StudyTypography.input,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); focusManager.clearFocus(); onSubmit(value.text) }),
        label = { Text(label) },
        shape = StudyShapes.interactive,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).bringIntoViewRequester(bringIntoView)
            .semantics {
                stateDescription = when (feedback) {
                    StudyFeedbackVisualState.CORRECT -> "Correct"
                    StudyFeedbackVisualState.INCORRECT -> "Incorrect"
                    StudyFeedbackVisualState.SELECTED -> "Submitted"
                    else -> if (enabled) "Editable" else "Disabled"
                }
            }
    )
}
