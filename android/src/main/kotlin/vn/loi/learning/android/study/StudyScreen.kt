package vn.loi.learning.android.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.recall.RecallOutcome
import vn.loi.learning.domain.study.recall.RecallProvenance
import androidx.compose.ui.platform.LocalContext
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.AndroidAudioState
import vn.loi.learning.android.platform.*
import vn.loi.learning.android.ui.*

private fun accessibilityStrings() = androidAccessibilityStrings(java.util.Locale.getDefault().language)

@Composable
fun HomeScreen(
    state: AndroidStudyState.Home,
    contentState: AndroidContentOperationState = AndroidContentOperationState.Idle,
    onEvent: (AndroidStudyEvent) -> Unit,
    onContentAction: (AndroidOperationKind) -> Unit = {},
    onContentDismiss: () -> Unit = {},
    onLibrary: () -> Unit = {},
    onReview: () -> Unit = {}
) {
    val model = state.model
    LazyColumn(
        Modifier.widthIn(max = 840.dp).fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
            .safeDrawingPadding().imePadding(),
        contentPadding = PaddingValues(LearningSpacing.large),
        verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
    ) {
        item("header") { HomeHeader() }
        item("content-operation") {
            when (contentState) {
                is AndroidContentOperationState.Running -> LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = accessibilityStrings().loading })
                is AndroidContentOperationState.Succeeded -> Text(contentState.detail, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                is AndroidContentOperationState.Failed -> {
                    Text(contentState.failure.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onContentAction(contentState.kind) }) { Text("Retry") }
                        TextButton(onClick = onContentDismiss) { Text("Dismiss") }
                    }
                }
                AndroidContentOperationState.Idle -> Unit
            }
        }
        item("hero") { ContinueLearningCard(model, onEvent, onLibrary) }
        if (model.hasDueReview) item("due-review") { DueReviewCard(model, onReview) }
        if (model.reviewedToday > 0) item("today") { TodaySummary(model) }
        if (model.totalMemoryCount > 0) item("progress") { ProgressSummary(model) }
        item("navigation") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                LearningEngineSecondaryButton("Library", onLibrary, Modifier.weight(1f))
                if (state.availability.canStartReview) LearningEngineSecondaryButton("Review", onReview, Modifier.weight(1f))
            }
        }
        if (!model.hasContent) item("empty") {
            LearningEngineEmptyState(
                title = "Your library is ready for content",
                detail = "Import a learning package, then return here to start.",
                actionLabel = "Import package",
                onAction = { onContentAction(AndroidOperationKind.IMPORT) }
            )
        }
    }
}

@Composable
private fun HomeHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
        Text("Learning Engine", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Ready for your next step?", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    }
}

@Composable
private fun ContinueLearningCard(model: AndroidHomeUiModel, onEvent: (AndroidStudyEvent) -> Unit, onLibrary: () -> Unit) {
    val (title, detail, actionLabel) = when (model.primaryAction) {
        is AndroidHomePrimaryAction.Resume -> Triple("Continue learning", "Resume exactly where you left off.", "Continue session")
        AndroidHomePrimaryAction.ReviewDue -> Triple("Review is ready", "Strengthen what is due today.", "Review now")
        AndroidHomePrimaryAction.StartLearning -> Triple("Start learning", "Begin the next canonical Study session.", "Start learning")
        AndroidHomePrimaryAction.OpenLibrary -> Triple("Choose what to learn", "Add or open content in your Library.", "Open Library")
    }
    LearningEngineCard(Modifier.fillMaxWidth().semantics { contentDescription = "$title. $detail" }) {
        Column(Modifier.padding(LearningSpacing.large), verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            model.contextTitle?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LearningEnginePrimaryButton(actionLabel, onClick = {
                when (val action = model.primaryAction) {
                    is AndroidHomePrimaryAction.Resume -> onEvent(AndroidStudyEvent.OpenSession(action.sessionId))
                    AndroidHomePrimaryAction.ReviewDue -> onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW))
                    AndroidHomePrimaryAction.StartLearning -> onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW))
                    AndroidHomePrimaryAction.OpenLibrary -> onLibrary()
                }
            }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DueReviewCard(model: AndroidHomeUiModel, onReview: () -> Unit) {
    val tone = if (model.overdueCount > 0) LearningStatusTone.OVERDUE else LearningStatusTone.DUE
    LearningEngineCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(LearningSpacing.large), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                Text("Due review", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                LearningEngineStatusBadge(if (model.overdueCount > 0) "${model.overdueCount} overdue" else "Due today", tone)
            }
            Text("${model.dueCount} item(s) are ready to review.")
            LearningEngineSecondaryButton("Review options", onReview)
        }
    }
}

@Composable
private fun TodaySummary(model: AndroidHomeUiModel) {
    LearningEngineCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(LearningSpacing.large), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            LearningEngineSectionHeader("Today")
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LearningSpacing.large), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                HomeMetric("Reviewed", model.reviewedToday.toString(), Modifier.weight(1f))
                model.accuracyPercent?.let { HomeMetric("Recall", "$it%", Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ProgressSummary(model: AndroidHomeUiModel) {
    LearningEngineCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(LearningSpacing.large), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            LearningEngineSectionHeader("Learning progress")
            LearningEngineProgress(model.learningProgress, "${model.activeMemoryCount} of ${model.totalMemoryCount} memories active")
        }
    }
}

@Composable
private fun HomeMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "$label, $value" }) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StudyScreen(
    state: AndroidStudyState,
    onEvent: (AndroidStudyEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val policy = androidLayoutPolicy(maxWidth.value.toInt(), maxHeight.value.toInt())
        CompositionLocalProvider(LocalLayoutPolicy provides policy) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AnimatedContent(
                    targetState = state,
                    contentKey = ::studyPresentationKey,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "study destination"
                ) { target ->
                    when (target) {
                        AndroidStudyState.Loading -> LoadingStudy()
                        is AndroidStudyState.Home -> HomeScreen(target, onEvent = onEvent)
                        is AndroidStudyState.Completion -> Completion(target, onEvent)
                        is AndroidStudyState.Failed -> StudyFailureState(target, onEvent)
                        is AndroidStudyState.Runtime -> {
                            StudyRuntimeScreen(
                                state = target,
                                onEvent = onEvent,
                                onOpenFullscreenImage = { fullscreenImageUri = it }
                            )
                        }
                    }
                }

                fullscreenImageUri?.let { imagePath ->
                    FullscreenLearningImage(
                        imagePath = imagePath,
                        onDismiss = { fullscreenImageUri = null }
                    )
                }
            }
        }
    }
}

private fun studyPresentationKey(state: AndroidStudyState): String = when (state) {
    AndroidStudyState.Loading -> "loading"
    is AndroidStudyState.Runtime -> "runtime-${state.plan.planId.value}"
    is AndroidStudyState.Completion -> "completion"
    is AndroidStudyState.Failed -> "failure"
    is AndroidStudyState.Home -> "home"
}

@Composable
private fun LoadingStudy() {
    LearningEngineLoadingState(label = "Preparing Study…")
}

private enum class AudioRole { PROMPT, ANSWER, MEANING, EXAMPLE, TRANSLATION }

@Composable
private fun StudyRuntimeScreen(
    state: AndroidStudyState.Runtime,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit
) {
    val modeLabel = when (state) {
        is AndroidStudyState.Typing -> "Typing"
        is AndroidStudyState.MultipleChoice -> "Multiple Choice"
        is AndroidStudyState.Listening -> "Listening"
        is AndroidStudyState.ImageRecall -> "Image Recall"
        is AndroidStudyState.ExampleCompletion -> "Example Completion"
    }

    val context = LocalContext.current
    val audioController = remember(context) { AndroidAudioController(context) }
    var activeRole by remember(state.plan.planId) { mutableStateOf<AudioRole?>(null) }

    DisposableEffect(audioController, state.plan.planId) {
        onDispose {
            audioController.stop()
        }
    }

    val playAudio: (AudioRole, String?, Boolean) -> Unit = { role, path, isLooping ->
        if (!path.isNullOrBlank()) {
            if (activeRole == role) {
                audioController.stop()
                activeRole = null
            } else {
                audioController.stop()
                activeRole = role
                audioController.replay(path, isLooping = isLooping) { state ->
                    if (state is AndroidAudioState.Idle || state is AndroidAudioState.Failed) {
                        if (activeRole == role) activeRole = null
                    }
                }
            }
        }
    }

    val stopAudioAndDispatch: (AndroidStudyEvent) -> Unit = { event ->
        audioController.stop()
        activeRole = null
        onEvent(event)
    }

    val isRevealed = when (state) {
        is AndroidStudyState.Typing -> state.revealed
        is AndroidStudyState.ExampleCompletion -> state.revealed
        else -> false
    }
    val isEnded = state.completed || isRevealed

    val keyboardController = LocalSoftwareKeyboardController.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(isEnded, state.plan.planId) {
        if (isEnded) {
            keyboardController?.hide()
            bringIntoViewRequester.bringIntoView()
        }
    }

    val animatedImageHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isEnded) 110.dp else 200.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "image height"
    )

    Scaffold(
        topBar = {
            LearningEngineStudyTopBar(
                title = state.contextTitle ?: "Study",
                modeLabel = modeLabel,
                currentPosition = state.currentPosition,
                totalItems = state.totalItems,
                onBack = { stopAudioAndDispatch(AndroidStudyEvent.Home) }
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = LearningSpacing.screen, vertical = LearningSpacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
            ) {
                // Main Learning Card
                StudyMainCard(
                    state = state,
                    activeRole = activeRole,
                    playAudio = playAudio,
                    imageHeight = animatedImageHeight,
                    onEvent = stopAudioAndDispatch,
                    onOpenFullscreenImage = onOpenFullscreenImage
                )

                // Revealed Answer & Feedback Section
                Box(modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)) {
                    StudyRevealAndFeedbackSection(
                        state = state,
                        activeRole = activeRole,
                        playAudio = playAudio,
                        onEvent = stopAudioAndDispatch
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyMainCard(
    state: AndroidStudyState.Runtime,
    activeRole: AudioRole?,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    imageHeight: androidx.compose.ui.unit.Dp,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = LearningEngineShapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = LearningElevation.card)
    ) {
        Column(
            Modifier.padding(LearningSpacing.large),
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
        ) {
            // Prompt Header (Word / Prompt Text / Listening / Image / Example)
            StudyPromptHeader(
                state = state,
                isPlayingPrompt = activeRole == AudioRole.PROMPT,
                onTogglePromptAudio = { playAudio(AudioRole.PROMPT, state.resolvedPromptAudio, true) }
            )

            // Image Content (if available)
            val imageUri = state.resolvedImage
            if (imageUri != null) {
                LearningEngineImage(
                    imagePath = imageUri,
                    imageUnavailable = false,
                    onOpenFullscreen = onOpenFullscreenImage,
                    modifier = Modifier.height(imageHeight)
                )
            }

            // Mode-specific Input & Options
            StudyModeInputArea(
                state = state,
                onEvent = onEvent
            )
        }
    }
}

@Composable
private fun StudyPromptHeader(
    state: AndroidStudyState.Runtime,
    isPlayingPrompt: Boolean,
    onTogglePromptAudio: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
        when (state) {
            is AndroidStudyState.Typing -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        state.prompt,
                        style = LearningContentTypography.vocabulary,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable { onTogglePromptAudio() }
                            .semantics { heading() }
                    )
                    state.resolvedPromptAudio?.let {
                        LearningEngineAudioIndicator(
                            isPlaying = isPlayingPrompt,
                            isLooping = true,
                            onClick = onTogglePromptAudio
                        )
                    }
                }
            }
            is AndroidStudyState.MultipleChoice -> {
                Text(
                    state.question,
                    style = LearningContentTypography.sectionTitle,
                    modifier = Modifier.semantics { heading() }
                )
            }
            is AndroidStudyState.Listening -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
                ) {
                    Text(
                        "Listen and type the answer",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onTogglePromptAudio() }
                            .semantics { heading() }
                    )
                    state.resolvedPromptAudio?.let {
                        LearningEngineAudioIndicator(
                            isPlaying = isPlayingPrompt,
                            isLooping = true,
                            onClick = onTogglePromptAudio
                        )
                    }
                }
            }
            is AndroidStudyState.ImageRecall -> {
                Text(
                    "Name the item shown",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() }
                )
            }
            is AndroidStudyState.ExampleCompletion -> {
                val isRevealed = state.revealed || state.completed
                Text(
                    buildAnnotatedString {
                        append(state.prefix)
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                            append(if (isRevealed) state.blank else " ".repeat(state.blank.length.coerceAtLeast(3)))
                        }
                        append(state.suffix)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics {
                        heading()
                        contentDescription = if (isRevealed) "${state.prefix} ${state.blank} ${state.suffix}"
                        else "${state.prefix} ${accessibilityStrings().blank} ${state.suffix}"
                    }
                )
            }
        }

        // Pronunciation display if available
        state.pronunciation?.takeIf { it.isNotBlank() }?.let { pron ->
            Text(
                pron,
                style = LearningContentTypography.pronunciation,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StudyAudioButton(audioPath: String?) = LearningEngineAudioButton(audioPath = audioPath)

@Composable
fun StudyImage(
    imagePath: String?,
    imageUnavailable: Boolean = imagePath == null,
    onOpenFullscreen: (String) -> Unit = {}
) = LearningEngineImage(imagePath = imagePath, imageUnavailable = imageUnavailable, onOpenFullscreen = onOpenFullscreen)

@Composable
fun FullscreenStudyImage(
    imagePath: String,
    onDismiss: () -> Unit
) = FullscreenLearningImage(imagePath = imagePath, onDismiss = onDismiss)

@Composable
private fun StudyModeInputArea(
    state: AndroidStudyState.Runtime,
    onEvent: (AndroidStudyEvent) -> Unit
) {
    when (state) {
        is AndroidStudyState.Typing -> {
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                AnswerField(
                    planId = state.plan.planId.value,
                    answer = state.answer,
                    enabled = !state.completed && !state.revealed,
                    error = state.evaluation == TypingAnswerEvaluationStatus.INCORRECT,
                    onEvent = onEvent
                )
                if (state.evaluation == TypingAnswerEvaluationStatus.INCORRECT && !state.completed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Keep trying",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                        TextButton(
                            onClick = { onEvent(AndroidStudyEvent.Retry) },
                            modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
                        ) { Text("Retry") }
                    }
                }
            }
        }
        is AndroidStudyState.MultipleChoice -> {
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
                state.choices.forEachIndexed { index, choice ->
                    val isSelected = state.selectedChoiceId == choice.id
                    OutlinedButton(
                        onClick = { onEvent(AndroidStudyEvent.Choose(choice.id)) },
                        enabled = !state.completed,
                        colors = if (isSelected) ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                            .semantics {
                                selected = isSelected
                                stateDescription = accessibilityStrings().option(index + 1, state.choices.size, isSelected)
                            }
                    ) {
                        Text(
                            "${index + 1}. ${choice.text}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        is AndroidStudyState.Listening -> {
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                AnswerField(
                    planId = state.plan.planId.value,
                    answer = state.answer,
                    enabled = !state.completed && !state.audioUnavailable,
                    error = false,
                    onEvent = onEvent
                )
                if (!state.completed) {
                    LearningEnginePrimaryButton(
                        label = "Submit answer",
                        onClick = { onEvent(AndroidStudyEvent.Submit) },
                        enabled = state.answer.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        is AndroidStudyState.ImageRecall -> {
            val imageReady = !state.imageUnavailable
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                AnswerField(
                    planId = state.plan.planId.value,
                    answer = state.answer,
                    enabled = !state.completed && imageReady,
                    error = false,
                    onEvent = onEvent
                )
                if (!state.completed) {
                    LearningEnginePrimaryButton(
                        label = "Submit answer",
                        onClick = { onEvent(AndroidStudyEvent.Submit) },
                        enabled = state.answer.isNotBlank() && imageReady,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        is AndroidStudyState.ExampleCompletion -> {
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                AnswerField(
                    planId = state.plan.planId.value,
                    answer = state.answer,
                    enabled = !state.completed && !state.revealed,
                    error = false,
                    onEvent = onEvent
                )
                if (!state.completed) {
                    LearningEnginePrimaryButton(
                        label = "Submit answer",
                        onClick = { onEvent(AndroidStudyEvent.Submit) },
                        enabled = state.answer.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerField(planId: String, answer: String, enabled: Boolean, error: Boolean, onEvent: (AndroidStudyEvent) -> Unit) {
    val focusRequester = remember(planId) { FocusRequester() }
    val bringIntoView = remember(planId) { BringIntoViewRequester() }
    var focusedForPlan by rememberSaveable(planId) { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(planId, enabled) {
        if (enabled && !focusedForPlan) { focusedForPlan = true; focusRequester.requestFocus(); bringIntoView.bringIntoView() }
        if (!enabled) keyboard?.hide()
    }
    OutlinedTextField(
        value = answer, onValueChange = { onEvent(AndroidStudyEvent.AnswerChanged(it)) },
        enabled = enabled, singleLine = false, minLines = 1, maxLines = 4, isError = error,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onEvent(AndroidStudyEvent.Submit) }),
        label = { Text(accessibilityStrings().answer) }, modifier = Modifier.fillMaxWidth()
            .focusRequester(focusRequester).bringIntoViewRequester(bringIntoView)
    )
}

@Composable
private fun StudyRevealAndFeedbackSection(
    state: AndroidStudyState.Runtime,
    activeRole: AudioRole?,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    onEvent: (AndroidStudyEvent) -> Unit
) {
    val isRevealed = when (state) {
        is AndroidStudyState.Typing -> state.revealed
        is AndroidStudyState.ExampleCompletion -> state.revealed
        else -> false
    }

    val allowReveal = when (state) {
        is AndroidStudyState.Typing, is AndroidStudyState.ExampleCompletion -> true
        else -> false
    }

    AnimatedVisibility(
        visible = state.completed || isRevealed,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) + expandVertically(animationSpec = androidx.compose.animation.core.tween(200)),
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) + shrinkVertically(animationSpec = androidx.compose.animation.core.tween(200))
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = LearningEngineShapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(
                Modifier.padding(LearningSpacing.large),
                verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
            ) {
                // Feedback badge
                val tone = when (state.outcome) {
                    RecallOutcome.CORRECT -> LearningStatusTone.SUCCESS
                    RecallOutcome.INCORRECT -> LearningStatusTone.ERROR
                    RecallOutcome.REVEALED -> LearningStatusTone.WARNING
                    else -> LearningStatusTone.INFO
                }
                val badgeText = when {
                    isRevealed -> "Answer revealed"
                    state.outcome == RecallOutcome.CORRECT -> "Correct"
                    state.outcome == RecallOutcome.INCORRECT -> "Incorrect"
                    else -> "Answer recorded"
                }
                LearningEngineStatusBadge(label = badgeText, tone = tone)

                // Expected Answer (Tap to play English answer audio loop)
                Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                    Text(
                        "Expected Answer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
                    ) {
                        Text(
                            state.plan.answerContract.canonicalAnswer,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable { playAudio(AudioRole.ANSWER, state.resolvedAnswerAudio, true) }
                        )
                        state.resolvedAnswerAudio?.let {
                            LearningEngineAudioIndicator(
                                isPlaying = activeRole == AudioRole.ANSWER,
                                isLooping = true,
                                onClick = { playAudio(AudioRole.ANSWER, state.resolvedAnswerAudio, true) }
                            )
                        }
                    }
                }

                // Meaning (Tap to play Vietnamese audio single play)
                state.meaning?.takeIf { it.isNotBlank() }?.let { m ->
                    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                        Text(
                            "Meaning",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
                        ) {
                            Text(
                                m,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clickable { playAudio(AudioRole.MEANING, state.resolvedAnswerAudio, false) }
                            )
                            state.resolvedAnswerAudio?.let {
                                LearningEngineAudioIndicator(
                                    isPlaying = activeRole == AudioRole.MEANING,
                                    isLooping = false,
                                    onClick = { playAudio(AudioRole.MEANING, state.resolvedAnswerAudio, false) }
                                )
                            }
                        }
                    }
                }

                // Example & Translation
                state.example?.takeIf { it.isNotBlank() }?.let { ex ->
                    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                        Text(
                            "Example",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
                        ) {
                            Text(
                                ex,
                                style = LearningContentTypography.example,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clickable { playAudio(AudioRole.EXAMPLE, state.resolvedExampleAudio, true) }
                            )
                            state.resolvedExampleAudio?.let {
                                LearningEngineAudioIndicator(
                                    isPlaying = activeRole == AudioRole.EXAMPLE,
                                    isLooping = true,
                                    onClick = { playAudio(AudioRole.EXAMPLE, state.resolvedExampleAudio, true) }
                                )
                            }
                        }
                        state.translation?.takeIf { it.isNotBlank() }?.let { tr ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
                            ) {
                                Text(
                                    tr,
                                    style = LearningContentTypography.translation,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .clickable { playAudio(AudioRole.TRANSLATION, state.resolvedExampleTranslationAudio, false) }
                                )
                                state.resolvedExampleTranslationAudio?.let { trAudio ->
                                    LearningEngineAudioIndicator(
                                        isPlaying = activeRole == AudioRole.TRANSLATION,
                                        isLooping = false,
                                        onClick = { playAudio(AudioRole.TRANSLATION, trAudio, false) }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Response Actions: Continue, Undo, Rating override
                Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
                    ) {
                        LearningEnginePrimaryButton(
                            label = "Continue",
                            onClick = { onEvent(AndroidStudyEvent.Next) },
                            modifier = Modifier.weight(1f)
                        )
                        LearningEngineSecondaryButton(
                            label = "Undo",
                            onClick = { onEvent(AndroidStudyEvent.Undo) },
                            modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
                        )
                    }

                    if (state.plan.provenance == RecallProvenance.PRACTICE) {
                        Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                            Text(
                                "Rate your recall:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf(ReviewRating.AGAIN, ReviewRating.HARD, ReviewRating.GOOD, ReviewRating.EASY).forEach { rating ->
                                    val ratingTone = when (rating) {
                                        ReviewRating.AGAIN, ReviewRating.HARD -> LearningDifficultyTone.DIFFICULT
                                        ReviewRating.GOOD -> LearningDifficultyTone.MEDIUM
                                        ReviewRating.EASY -> LearningDifficultyTone.EASY
                                    }
                                    LearningEngineFeedbackBadge(
                                        tone = ratingTone,
                                        modifier = Modifier.clickable { onEvent(AndroidStudyEvent.OverrideRating(rating)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (!state.completed && !isRevealed && allowReveal) {
        TextButton(
            onClick = { onEvent(AndroidStudyEvent.Reveal) },
            modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
        ) {
            Text("Reveal answer")
        }
    }
}

@Composable
private fun Completion(state: AndroidStudyState.Completion, onEvent: (AndroidStudyEvent) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(LearningSpacing.screen),
        contentAlignment = Alignment.Center
    ) {
        LearningEngineCompletionCard(
            title = "Session complete",
            detail = "Great work! You have completed all items in this study session.",
            canUndo = state.canUndo,
            onUndo = { onEvent(AndroidStudyEvent.Undo) },
            onHome = { onEvent(AndroidStudyEvent.Home) }
        )
    }
}

@Composable
private fun StudyFailureState(
    state: AndroidStudyState.Failed,
    onEvent: (AndroidStudyEvent) -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(LearningSpacing.screen),
        contentAlignment = Alignment.Center
    ) {
        LearningEngineErrorState(
            title = "Study session unavailable",
            message = state.message,
            onRetry = if (state.retryable) { { onEvent(AndroidStudyEvent.Retry) } } else null
        )
    }
}
