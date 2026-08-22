package vn.loi.learning.android.study.modes

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.study.*
import vn.loi.learning.android.study.components.*
import vn.loi.learning.android.study.design.*
import vn.loi.learning.android.ui.*
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningexperience.TypingDifferenceKind
import vn.loi.learning.application.typing.*
import kotlinx.coroutines.delay
import vn.loi.learning.android.study.components.PartOfSpeechBadge

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
    onStageTap: (() -> Unit)? = null,
    onSwipeNext: (() -> Unit)? = null,
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
    val inputSessionActive = !feedbackVisible
    val mediaRole = typedModeMediaRole(
        listening = false,
        feedbackVisible = feedbackVisible,
        imeVisible = imeVisible,
        inputSessionActive = inputSessionActive
    )
    LaunchedEffect(state.plan.planId.value, imeVisible, density, mediaRole, availableMediaHeightDp) {
        AndroidTypingLayoutTrace.changed(
            planId = state.plan.planId.value,
            imeVisible = imeVisible,
            inputFocused = null,
            density = density,
            mediaRole = mediaRole,
            availableMediaHeightDp = availableMediaHeightDp
        )
    }
    var clockMillis by remember(state.plan.planId.value) { mutableLongStateOf(TypingAttemptTimeSource.MONOTONIC.nowMillis()) }
    LaunchedEffect(state.plan.planId.value, state.completionPending) {
        var timeoutCheckTicks = 0
        while (!state.completed) {
            delay(250)
            clockMillis = TypingAttemptTimeSource.MONOTONIC.nowMillis()
            timeoutCheckTicks++
            if (timeoutCheckTicks % 4 == 0 && state.attempt?.firstInputAtMillis != null) {
                onEvent(AndroidStudyEvent.CheckTypingTimeout)
            }
        }
    }
    val elapsedMillis = state.attempt?.activeTypingElapsedMillis(clockMillis) ?: 0L
    val projectedRating = state.automaticRating ?: state.attempt?.projectedMetrics(clockMillis)?.let(TypingAutomaticRatingResolver::decide)
    val stableActionsRequester = remember(state.plan.planId.value) { BringIntoViewRequester() }
    TypedAnswerStageFrame(
        modifier = modifier,
        feedback = inputState.feedbackVisual(),
        density = density,
        fillViewport = true,
        onStageTap = onStageTap,
        onSwipeNext = onSwipeNext
    ) {
        if (state.completionPending) {
            TypingImeContinuityAnchor(state.plan.planId.value)
            StudyMedia(
                state.resolvedImage,
                StudyMediaRole.COMPACT,
                density,
                availableMediaHeightDp,
                onOpenFullscreenImage
            )
            StudyAnswerSection(
                englishAnswer = state.plan.answerContract.canonicalAnswer,
                pronunciation = normalizedIntroductionPronunciation(state.partOfSpeech, state.pronunciation),
                partOfSpeech = state.partOfSpeech?.let(::partOfSpeechPresentation),
                vietnameseAnswer = state.meaning,
                englishExample = null,
                vietnameseExample = null,
                answerAudioPath = null,
                englishExampleAudioPath = null,
                isPlayingAnswer = false,
                isPlayingVietnamese = false,
                isPlayingEnglishExample = false,
                isPlayingVietnameseExample = false,
                onAnswerAudio = {},
                onEnglishExampleAudio = {},
                answerHero = true
            )
            typingRatingTransitionPresentation(state)?.let { presentation ->
                TypingRatingTransition(presentation, Modifier.fillMaxWidth())
            }
        } else {
        if (!feedbackVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(StudySpacing.micro),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.prompt,
                            style = StudyTypography.prompt,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() }
                        )
                        state.partOfSpeech?.takeIf(String::isNotBlank)?.let(::partOfSpeechPresentation)?.let { pos ->
                            PartOfSpeechBadge(pos)
                        }
                    }
                    IconButton(onClick = {
                        if (activeRole == AudioRole.MEANING) playAudio(AudioRole.MEANING, state.resolvedMeaningAudio, false)
                        onEvent(AndroidStudyEvent.ToggleTypingViAutoplayMute)
                    }) {
                        Icon(
                            if (state.viAutoplayMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            if (state.viAutoplayMuted) "Unmute Vietnamese autoplay" else "Mute Vietnamese autoplay"
                        )
                    }
                }
            }
            if (!state.revealed) {
                val typingFrontBounds = resolveTypingFrontMediaBounds(
                    density = density,
                    availableHeightDp = availableMediaHeightDp,
                    hasMedia = !state.resolvedImage.isNullOrBlank(),
                    imeVisible = imeVisible
                )
                StudyMedia(
                    state.resolvedImage, mediaRole, density, availableMediaHeightDp,
                    onOpenFullscreenImage,
                    customBounds = typingFrontBounds
                )
            }
            if (state.attempt?.firstInputAtMillis != null && !state.revealed) {
                Text(
                    "⏱ ${formatTypingSeconds(elapsedMillis)}   ${if (state.completionPending) "AUTO: " else ""}${projectedRating?.rating?.name ?: "ACTIVE"}",
                    style = StudyTypography.metadata,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!feedbackVisible) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (density == StudyContentDensity.DENSE) StudySpacing.micro else StudySpacing.group),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StudyAnswerInput(
                        state.plan.planId.value, state.answer, true, state.evaluation == TypingAnswerEvaluationStatus.INCORRECT,
                        label = "Type your answer", feedback = inputState.feedbackVisual(),
                        onAnswerChanged = { currentInput = it; onEvent(AndroidStudyEvent.AnswerChanged(it)) },
                        onSubmit = { onEvent(AndroidStudyEvent.Submit(it)) }
                    )
                    TypingInputActions(
                        currentInput, inputState, showRetry = state.evaluation == TypingAnswerEvaluationStatus.INCORRECT,
                        onSubmit = { onEvent(AndroidStudyEvent.Submit(currentInput)) },
                        onRetry = { onEvent(AndroidStudyEvent.Retry) },
                        onReveal = { onEvent(AndroidStudyEvent.Reveal(currentInput)) },
                        modifier = Modifier.bringIntoViewRequester(stableActionsRequester)
                    )
                    if (!imeVisible) {
                        StudyRatingBar(
                            onRating = { onEvent(AndroidStudyEvent.SelectTypingRatingOverride(it)) },
                            selectedRating = state.manualRating
                        )
                    }
                }
            }
            feedbackContent()
        }
    }
}

@Composable
private fun TypingImeContinuityAnchor(planId: String) {
    val focusRequester = remember(planId) { FocusRequester() }
    LaunchedEffect(planId) { focusRequester.requestFocus() }
    BasicTextField(
        value = "",
        onValueChange = {},
        modifier = Modifier.size(1.dp).graphicsLayer { alpha = 0f }.focusRequester(focusRequester),
        singleLine = true
    )
}

@Composable
private fun TypingInputActions(
    answer: String,
    visualState: StudyInputVisualState,
    showRetry: Boolean,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StudySpacing.micro)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (visualState == StudyInputVisualState.INCORRECT) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(StudySpacing.micro),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                ) {
                    Icon(Icons.Default.ErrorOutline, null)
                    Text("Hãy thử lại", style = StudyTypography.feedback)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(StudySpacing.micro),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showRetry) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = LearningSpacing.touchTarget)
                ) { Text("Thử lại") }
            } else {
                Button(
                    onClick = onSubmit,
                    enabled = answer.isNotBlank(),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = LearningSpacing.touchTarget)
                ) { Text("Kiểm tra") }
            }
            OutlinedButton(
                onClick = onReveal,
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = LearningSpacing.touchTarget)
            ) { Text("Hiện đáp án") }
        }
    }
}

@Composable
internal fun TypingDifferenceComparison(actual: String, expected: String) {
    val presentation = remember(actual, expected) { resolveAndroidTypingRevealComparison(actual, expected) }
    val danger = MaterialTheme.colorScheme.error
    val success = LearningEngineThemeTokens.semanticColors.success
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.42f),
        shape = StudyShapes.semanticSurface,
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = presentation.accessibilityDescription
        }
    ) {
        Column(
            Modifier.fillMaxWidth().padding(StudySpacing.group),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StudySpacing.micro)
        ) {
            Text(
                androidTypingComparisonAnnotatedText(presentation.actual, presentation.actualSpans, danger),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(modifier = Modifier.widthIn(max = 48.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                androidTypingComparisonAnnotatedText(presentation.expected, presentation.expectedSpans, success),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

internal data class AndroidTypingDiffSpan(
    val startCodePoint: Int,
    val endCodePoint: Int,
    val kind: TypingDifferenceKind
)

internal data class AndroidTypingRevealComparison(
    val actual: String,
    val expected: String,
    val actualSpans: List<AndroidTypingDiffSpan>,
    val expectedSpans: List<AndroidTypingDiffSpan>,
    val accessibilityDescription: String
)

internal fun resolveAndroidTypingRevealComparison(actual: String, expected: String): AndroidTypingRevealComparison {
    val evaluation = TypingAnswerEvaluator().evaluate(
        vn.loi.learning.application.learningexperience.TypingRecallPrompt(expected), actual
    )
    val actualSpans = mutableListOf<AndroidTypingDiffSpan>()
    val expectedSpans = mutableListOf<AndroidTypingDiffSpan>()
    var actualIndex = 0
    var expectedIndex = 0
    evaluation.differences.forEach { difference ->
        when (difference.kind) {
            TypingDifferenceKind.MATCH -> { actualIndex++; expectedIndex++ }
            TypingDifferenceKind.REPLACEMENT -> {
                actualSpans += AndroidTypingDiffSpan(actualIndex, actualIndex + 1, difference.kind)
                expectedSpans += AndroidTypingDiffSpan(expectedIndex, expectedIndex + 1, difference.kind)
                actualIndex++; expectedIndex++
            }
            TypingDifferenceKind.INSERTION -> {
                actualSpans += AndroidTypingDiffSpan(actualIndex, actualIndex + 1, difference.kind)
                actualIndex++
            }
            TypingDifferenceKind.DELETION -> {
                expectedSpans += AndroidTypingDiffSpan(expectedIndex, expectedIndex + 1, difference.kind)
                expectedIndex++
            }
        }
    }
    val safeActualSpans = actualSpans.takeIf {
        actual.codePointCount(0, actual.length) == evaluation.normalizedAnswer.codePointCount(0, evaluation.normalizedAnswer.length)
    }.orEmpty()
    val safeExpectedSpans = expectedSpans.takeIf {
        expected.codePointCount(0, expected.length) == evaluation.normalizedExpectedAnswer.codePointCount(0, evaluation.normalizedExpectedAnswer.length)
    }.orEmpty()
    return AndroidTypingRevealComparison(
        actual = actual,
        expected = expected,
        actualSpans = safeActualSpans,
        expectedSpans = safeExpectedSpans,
        accessibilityDescription = "Your answer $actual. Correct answer $expected. " +
            androidTypingDifferenceAccessibilityText(evaluation.differences)
    )
}

internal fun androidTypingComparisonAnnotatedText(
    text: String,
    spans: List<AndroidTypingDiffSpan>,
    color: androidx.compose.ui.graphics.Color
): AnnotatedString = buildAnnotatedString {
    append(text)
    spans.forEach { span ->
        val start = text.offsetByCodePoints(0, span.startCodePoint)
        val end = text.offsetByCodePoints(0, span.endCodePoint)
        addStyle(
            SpanStyle(
                color = color,
                textDecoration = if (span.kind == TypingDifferenceKind.INSERTION) {
                    TextDecoration.LineThrough
                } else {
                    TextDecoration.Underline
                }
            ),
            start,
            end
        )
    }
}

internal fun androidTypingDifferenceAccessibilityText(
    differences: List<vn.loi.learning.application.learningexperience.TypingAnswerDifference>
): String = differences.filter { it.kind != TypingDifferenceKind.MATCH }
    .fold(mutableListOf<vn.loi.learning.application.learningexperience.TypingAnswerDifference>()) { groups, difference ->
        val previous = groups.lastOrNull()
        if (previous?.kind == difference.kind) {
            groups[groups.lastIndex] = previous.copy(
                typedText = previous.typedText.orEmpty() + difference.typedText.orEmpty(),
                expectedText = previous.expectedText.orEmpty() + difference.expectedText.orEmpty()
            )
        } else groups += difference
        groups
    }.joinToString(" ") { difference ->
        when (difference.kind) {
            TypingDifferenceKind.REPLACEMENT -> "Replace ${difference.typedText.orEmpty()} with ${difference.expectedText.orEmpty()}."
            TypingDifferenceKind.INSERTION -> "Remove inserted ${difference.typedText.orEmpty()}."
            TypingDifferenceKind.DELETION -> "Missing ${difference.expectedText.orEmpty()}."
            TypingDifferenceKind.MATCH -> error("Matches were filtered")
        }
    }

private fun formatTypingSeconds(milliseconds: Long): String {
    val tenths = milliseconds.coerceAtLeast(0L) / 100L
    return "${tenths / 10}.${tenths % 10}s"
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
            onReplay = { playAudio(AudioRole.PROMPT, state.resolvedPromptAudio, false) }
        )
        if (state.audioUnavailable) StudyUnavailableNotice("Listening audio unavailable")
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
    fillViewport: Boolean = false,
    onStageTap: (() -> Unit)? = null,
    onSwipeNext: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val reducedMotion = isReducedMotionEnabled()
    val feedbackScale by animateFloatAsState(
        targetValue = if (feedback == StudyFeedbackVisualState.NEUTRAL) 1f else 1.01f,
        animationSpec = tween(studyMotionDurationMillis(StudyMotionRole.PRESS, reducedMotion)),
        label = "typed answer feedback"
    )
    val gesturesActive = onStageTap != null && onSwipeNext != null
    val stageModifier = (if (fillViewport) modifier.fillMaxSize() else modifier.fillMaxWidth())
        .typingRevealedGestures(
            enabled = gesturesActive,
            onTap = { onStageTap?.invoke() },
            onSwipeNext = { onSwipeNext?.invoke() }
        )
    val contentModifier = if (fillViewport) {
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    } else {
        Modifier.fillMaxWidth()
    }
    StudyStageCard(stageModifier.graphicsLayer { scaleX = feedbackScale; scaleY = feedbackScale }, feedback = feedback) {
        Column(
            contentModifier.padding(if (density == StudyContentDensity.DENSE) StudySpacing.group else StudySpacing.section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (density == StudyContentDensity.DENSE) StudySpacing.micro else StudySpacing.group),
            content = content
        )
    }
}

internal fun Modifier.typingRevealedGestures(
    enabled: Boolean,
    onTap: () -> Unit,
    onSwipeNext: () -> Unit
): Modifier = if (!enabled) this else pointerInput(Unit) {
    val swipeThresholdPx = 44.dp.toPx()
    val tapSlopPx = 12.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var end = down.position
        var childConsumed = down.isConsumed
        var pressed = true
        var ownsUpwardDrag = false
        while (pressed) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            end = change.position
            childConsumed = childConsumed || change.isConsumed
            val deltaX = end.x - down.position.x
            val deltaY = end.y - down.position.y
            val absX = kotlin.math.abs(deltaX)
            val absY = kotlin.math.abs(deltaY)
            if (!ownsUpwardDrag && deltaY < -tapSlopPx && absY > absX * 1.25f) {
                ownsUpwardDrag = true
                change.consume()
            } else if (ownsUpwardDrag) {
                change.consume()
            }
            pressed = change.pressed
        }
        val deltaX = end.x - down.position.x
        val deltaY = end.y - down.position.y
        val absX = kotlin.math.abs(deltaX)
        val absY = kotlin.math.abs(deltaY)
        if (deltaY <= -swipeThresholdPx && absY > absX * 1.25f) {
            onSwipeNext()
        } else if (!ownsUpwardDrag && !childConsumed && absX <= tapSlopPx && absY <= tapSlopPx) {
            onTap()
        }
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
        Text("Hãy nghe kỹ", style = StudyTypography.prompt)
        if (audioAvailable) Text("Chạm loa để nghe lại", style = StudyTypography.metadata)
    }
}

@Composable
private fun TypedInputActions(
    answer: String,
    visualState: StudyInputVisualState,
    showRetry: Boolean,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onReveal: (() -> Unit)? = null
) {
    if (visualState == StudyInputVisualState.CORRECT || visualState == StudyInputVisualState.INCORRECT) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StudySpacing.micro), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            Icon(if (visualState == StudyInputVisualState.CORRECT) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null)
            Text(if (visualState == StudyInputVisualState.CORRECT) "Correct" else "Keep trying", style = StudyTypography.feedback)
        }
    }
    if (!showRetry && visualState in setOf(StudyInputVisualState.IDLE, StudyInputVisualState.FOCUSED, StudyInputVisualState.INCORRECT)) {
        Button(onClick = onSubmit, enabled = answer.isNotBlank(), modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)) { Text("Kiểm tra") }
    }
    if (showRetry) {
        Row(horizontalArrangement = Arrangement.spacedBy(StudySpacing.micro), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onRetry, modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)) {
                Text("Thử lại")
            }
            onReveal?.let { reveal ->
                OutlinedButton(
                    onClick = reveal,
                    modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
                ) { Text("Hiện đáp án") }
            }
        }
    }
}
