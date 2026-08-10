package vn.loi.learning.android.study.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
internal fun StudyUnavailableNotice(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = StudyTypography.metadata,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { stateDescription = text }
    )
}

@Composable
internal fun StudyChoiceTile(
    anchor: String,
    text: String,
    visualState: StudyChoiceVisualState,
    enabled: Boolean,
    stateDescriptionText: String,
    onClick: () -> Unit
) {
    val reducedMotion = isReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = when {
            reducedMotion -> 1f
            pressed -> 0.985f
            visualState == StudyChoiceVisualState.CORRECT -> 1.01f
            visualState == StudyChoiceVisualState.INCORRECT -> 0.995f
            else -> 1f
        },
        animationSpec = androidx.compose.animation.core.tween(
            multipleChoiceMotionDurationMillis(visualState, reducedMotion)
        ),
        label = "choice press"
    )
    val container = when (visualState) {
        StudyChoiceVisualState.SELECTED -> MaterialTheme.colorScheme.primaryContainer
        StudyChoiceVisualState.CORRECT -> StudyRatingColors.good.background
        StudyChoiceVisualState.INCORRECT -> StudyRatingColors.again.background
        StudyChoiceVisualState.IDLE, StudyChoiceVisualState.DISABLED -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when (visualState) {
        StudyChoiceVisualState.SELECTED -> MaterialTheme.colorScheme.onPrimaryContainer
        StudyChoiceVisualState.CORRECT -> StudyRatingColors.good.content
        StudyChoiceVisualState.INCORRECT -> StudyRatingColors.again.content
        StudyChoiceVisualState.IDLE, StudyChoiceVisualState.DISABLED -> MaterialTheme.colorScheme.onSurface
    }
    val border = when (visualState) {
        StudyChoiceVisualState.SELECTED -> MaterialTheme.colorScheme.primary
        StudyChoiceVisualState.CORRECT -> StudyRatingColors.good.border
        StudyChoiceVisualState.INCORRECT -> StudyRatingColors.again.border
        StudyChoiceVisualState.IDLE, StudyChoiceVisualState.DISABLED -> MaterialTheme.colorScheme.outlineVariant
    }
    FilledTonalButton(
        onClick, enabled = enabled, shape = StudyShapes.interactive, interactionSource = interactionSource,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = container, contentColor = content,
            disabledContainerColor = container,
            disabledContentColor = content.copy(
                alpha = if (visualState == StudyChoiceVisualState.DISABLED) 0.76f else 1f
            )
        ),
        border = BorderStroke(
            if (visualState in setOf(StudyChoiceVisualState.CORRECT, StudyChoiceVisualState.INCORRECT)) 2.dp else 1.dp,
            border
        ),
        contentPadding = PaddingValues(horizontal = StudySpacing.group, vertical = StudySpacing.micro),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics {
                selected = visualState == StudyChoiceVisualState.SELECTED
                stateDescription = stateDescriptionText
            }
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StudySpacing.group)
        ) {
            Surface(shape = StudyShapes.semanticSurface, color = content.copy(alpha = 0.10f)) {
                Text(
                    anchor,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Text(text, style = StudyTypography.choice, modifier = Modifier.weight(1f))
            when (visualState) {
                StudyChoiceVisualState.CORRECT -> Icon(Icons.Default.CheckCircle, "Correct choice")
                StudyChoiceVisualState.INCORRECT -> Icon(Icons.Default.Close, "Incorrect choice")
                else -> Unit
            }
        }
    }
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
