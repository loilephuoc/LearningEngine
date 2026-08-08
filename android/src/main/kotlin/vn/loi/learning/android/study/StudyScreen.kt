package vn.loi.learning.android.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.recall.RecallOutcome
import vn.loi.learning.domain.study.recall.RecallProvenance
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.AndroidAudioState
import vn.loi.learning.android.platform.*
import vn.loi.learning.android.ui.*

private fun accessibilityStrings() = androidAccessibilityStrings(java.util.Locale.getDefault().language)

internal data class AndroidLearningLandingPresentation(
    val hasActiveSession: Boolean,
    val contextTitle: String?,
    val hasContent: Boolean,
    val dueCount: Int,
    val reviewedToday: Int,
    val accuracyPercent: Int?,
    val activeMemoryCount: Int,
    val totalMemoryCount: Int,
    val learningProgress: Float
)

internal fun resolveLearningLandingPresentation(state: AndroidStudyState.Home) =
    AndroidLearningLandingPresentation(
        hasActiveSession = state.availability.canResume,
        contextTitle = state.model.contextTitle,
        hasContent = state.model.hasContent,
        dueCount = state.model.dueCount,
        reviewedToday = state.model.reviewedToday,
        accuracyPercent = state.model.accuracyPercent,
        activeMemoryCount = state.model.activeMemoryCount,
        totalMemoryCount = state.model.totalMemoryCount,
        learningProgress = state.model.learningProgress
    )

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
    val presentation = resolveLearningLandingPresentation(state)
    LazyColumn(
        Modifier.widthIn(max = 840.dp).fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
            .safeDrawingPadding().imePadding(),
        contentPadding = PaddingValues(horizontal = LearningSpacing.screen, vertical = LearningSpacing.medium),
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
        item("hero") { ContinueLearningCard(model, presentation, onEvent, onLibrary) }
        item("stats") { HomeDashboardStats(presentation) }
        if (presentation.dueCount > 0) item("due-review") { DueReviewCard(model, onReview) }
        if (presentation.totalMemoryCount > 0) item("progress") { HomeLearningProgress(presentation) }
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
        Text("LEARNING ENGINE", style = LearningTextRole.brand, color = MaterialTheme.colorScheme.primary)
        Text("Keep your learning moving", style = LearningTextRole.screenTitle,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
    }
}

@Composable
private fun ContinueLearningCard(
    model: AndroidHomeUiModel,
    presentation: AndroidLearningLandingPresentation,
    onEvent: (AndroidStudyEvent) -> Unit,
    onLibrary: () -> Unit
) {
    val (title, detail, actionLabel) = when (model.primaryAction) {
        is AndroidHomePrimaryAction.Resume -> Triple("Continue learning", "Resume exactly where you left off.", "Continue session")
        AndroidHomePrimaryAction.ReviewDue -> Triple("Review is ready", "Strengthen what is due today.", "Review now")
        AndroidHomePrimaryAction.StartLearning -> Triple("Start learning", "Begin the next canonical Study session.", "Start learning")
        AndroidHomePrimaryAction.OpenLibrary -> Triple("Choose what to learn", "Add or open content in your Library.", "Open Library")
    }
    LearningEngineHeroCard(
        icon = if (presentation.hasActiveSession) Icons.Default.PlayArrow else Icons.Default.School,
        eyebrow = if (presentation.hasActiveSession) "ACTIVE SESSION" else "NEXT STEP",
        title = model.contextTitle ?: title,
        detail = detail,
        actionLabel = actionLabel,
        onAction = {
                when (val action = model.primaryAction) {
                    is AndroidHomePrimaryAction.Resume -> onEvent(AndroidStudyEvent.OpenSession(action.sessionId))
                    AndroidHomePrimaryAction.ReviewDue -> onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW))
                    AndroidHomePrimaryAction.StartLearning -> onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW))
                    AndroidHomePrimaryAction.OpenLibrary -> onLibrary()
                }
            },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun HomeDashboardStats(presentation: AndroidLearningLandingPresentation) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
        LearningEngineStatTile("Due", presentation.dueCount.toString(), Modifier.weight(1f))
        LearningEngineStatTile("Active", presentation.activeMemoryCount.toString(), Modifier.weight(1f), "memories")
        val recallValue = presentation.accuracyPercent?.let { "$it%" } ?: presentation.reviewedToday.toString()
        val recallLabel = if (presentation.accuracyPercent != null) "Recall" else "Reviewed"
        LearningEngineStatTile(recallLabel, recallValue, Modifier.weight(1f),
            if (presentation.accuracyPercent != null) "today" else null)
    }
}

@Composable
private fun DueReviewCard(model: AndroidHomeUiModel, onReview: () -> Unit) {
    val detail = if (model.overdueCount > 0) {
        "${model.dueCount} waiting · ${model.overdueCount} overdue"
    } else "${model.dueCount} item(s) ready now"
    LearningEngineActionCard(Icons.Default.AutoStories, "Review due", detail, "Review", onReview, Modifier.fillMaxWidth())
}

@Composable
private fun HomeLearningProgress(presentation: AndroidLearningLandingPresentation) {
    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
        LearningEngineSectionHeader("Learning progress")
        LearningEngineProgress(
            presentation.learningProgress,
            "${presentation.activeMemoryCount} of ${presentation.totalMemoryCount} memories active"
        )
    }
}

@Composable
fun StudyScreen(
    state: AndroidStudyState,
    onEvent: (AndroidStudyEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    val reducedMotion = isReducedMotionEnabled()

    BackHandler(enabled = fullscreenImageUri != null) {
        fullscreenImageUri = null
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val policy = androidLayoutPolicy(maxWidth.value.toInt(), maxHeight.value.toInt())
        CompositionLocalProvider(LocalLayoutPolicy provides policy) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val transitionSpec: AnimatedContentTransitionScope<AndroidStudyState>.() -> ContentTransform = {
                    if (reducedMotion) {
                        fadeIn(animationSpec = tween(durationMillis = 50)) togetherWith fadeOut(animationSpec = tween(durationMillis = 50))
                    } else {
                        (fadeIn(animationSpec = tween(durationMillis = 200)) + slideInHorizontally(animationSpec = tween(durationMillis = 200)) { fullWidth -> fullWidth / 12 })
                            .togetherWith(fadeOut(animationSpec = tween(durationMillis = 200)) + slideOutHorizontally(animationSpec = tween(durationMillis = 200)) { fullWidth -> -fullWidth / 12 })
                    }
                }

                AnimatedContent(
                    targetState = state,
                    contentKey = ::studyPresentationKey,
                    transitionSpec = transitionSpec,
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
    is AndroidStudyState.Introduction -> "runtime-intro-${state.learningItemId}"
    is AndroidStudyState.Runtime -> "runtime-${state.requireRecallPlan().planId.value}"
    is AndroidStudyState.Completion -> "completion"
    is AndroidStudyState.Failed -> "failure"
    is AndroidStudyState.Home -> "home"
}

private fun AndroidStudyState.Runtime.requireRecallPlan() =
    requireNotNull(plan) { "Recall runtime state must provide a RecallPlan" }

@Composable
private fun LoadingStudy() {
    LearningEngineLoadingState(label = "Preparing Study…")
}

private enum class AudioRole { PROMPT, EXPECTED_ANSWER, MEANING, EXAMPLE_ENGLISH, EXAMPLE_VIETNAMESE }

@Composable
private fun StudyRuntimeScreen(
    state: AndroidStudyState.Runtime,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit
) {
    val modeLabel = when (state) {
        is AndroidStudyState.Introduction -> "New Content"
        is AndroidStudyState.Typing -> "Typing"
        is AndroidStudyState.MultipleChoice -> "Multiple Choice"
        is AndroidStudyState.Listening -> "Listening"
        is AndroidStudyState.ImageRecall -> "Image Recall"
        is AndroidStudyState.ExampleCompletion -> "Example Completion"
    }

    val context = LocalContext.current
    val audioController = remember(context) { AndroidAudioController(context) }
    val itemKey = when (state) {
        is AndroidStudyState.Introduction -> state.learningItemId
        else -> state.requireRecallPlan().planId.value
    }
    var activeRole by remember(itemKey) { mutableStateOf<AudioRole?>(null) }

    DisposableEffect(audioController, itemKey) {
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

    LaunchedEffect(itemKey) {
        if (state is AndroidStudyState.Introduction && !state.revealed && !state.resolvedMeaningAudio.isNullOrBlank()) {
            playAudio(AudioRole.MEANING, state.resolvedMeaningAudio, false)
        }
    }

    val isRevealed = when (state) {
        is AndroidStudyState.Introduction -> state.revealed
        is AndroidStudyState.Typing -> state.revealed
        is AndroidStudyState.ExampleCompletion -> state.revealed
        else -> false
    }
    val isEnded = state.completed || isRevealed

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scrollState = rememberScrollState()
    val reducedMotion = isReducedMotionEnabled()

    LaunchedEffect(isEnded, itemKey) {
        if (isEnded) {
            keyboardController?.hide()
            focusManager.clearFocus()
            bringIntoViewRequester.bringIntoView()
        }
    }

    val animatedImageHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isEnded) 110.dp else 200.dp,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = if (reducedMotion) 0 else LearningMotion.standardMillis
        ),
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
        },
        bottomBar = {
            if (state is AndroidStudyState.Introduction && state.revealed) {
                IntroductionRatingDock(onEvent = stopAudioAndDispatch)
            }
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
                    .padding(horizontal = LearningSpacing.screen, vertical = LearningSpacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)
            ) {
                state.hud?.let { StudySessionHud(it) }

                StudyMainCard(
                    state = state,
                    activeRole = activeRole,
                    playAudio = playAudio,
                    imageHeight = animatedImageHeight,
                    onEvent = stopAudioAndDispatch,
                    onOpenFullscreenImage = onOpenFullscreenImage
                )

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
private fun IntroductionRatingDock(onEvent: (AndroidStudyEvent) -> Unit) {
    Surface(tonalElevation = LearningElevation.raised, shadowElevation = LearningElevation.overlay) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = LearningSpacing.small, vertical = LearningSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
        ) {
            RatingDockButton("Again", "Start over", ReviewRating.AGAIN, onEvent, Modifier.weight(1f))
            RatingDockButton("Hard", "Hard to recall", ReviewRating.HARD, onEvent, Modifier.weight(1f))
            RatingDockButton("Good", "Recalled well", ReviewRating.GOOD, onEvent, Modifier.weight(1f))
            RatingDockButton("Easy", "Effortless recall", ReviewRating.EASY, onEvent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RatingDockButton(
    label: String,
    supporting: String,
    rating: ReviewRating,
    onEvent: (AndroidStudyEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = when (rating) {
        ReviewRating.AGAIN -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
        ReviewRating.HARD -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        ReviewRating.GOOD -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        ReviewRating.EASY -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
    FilledTonalButton(
        onClick = { onEvent(AndroidStudyEvent.RateIntroduction(rating)) },
        colors = colors,
        contentPadding = PaddingValues(horizontal = LearningSpacing.extraSmall),
        modifier = modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget).semantics {
            contentDescription = "$label, $supporting"
        }
    ) { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
}

@Composable
private fun StudySessionHud(hud: AndroidStudySessionHud) {
    val newDescription = progressDescription("New", hud.newCompleted, hud.newTarget, hud.newConfiguredTarget)
    val reviewDescription = progressDescription(
        "Review", hud.reviewCompleted, hud.reviewTarget, hud.reviewConfiguredTarget
    )
    Surface(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "$newDescription. $reviewDescription. Total learned ${hud.totalLearned}. " +
                "Again ${hud.againCount}, Hard ${hud.hardCount}, Good ${hud.goodCount}, Easy ${hud.easyCount}."
        },
        shape = LearningEngineShapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = LearningSpacing.medium, vertical = LearningSpacing.extraSmall),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HudMetric("NEW", "${hud.newCompleted}/${hud.newTarget}")
            HudMetric("REVIEW", "${hud.reviewCompleted}/${hud.reviewTarget}")
            HudMetric("TOTAL", hud.totalLearned.toString())
        }
    }
}

@Composable
private fun HudMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

private fun progressDescription(label: String, completed: Int, target: Int, configuredTarget: Int): String =
    if (target == configuredTarget) "$label $completed of $target"
    else "$label $completed of $target available, configured target $configuredTarget"

@Composable
private fun StudyMainCard(
    state: AndroidStudyState.Runtime,
    activeRole: AudioRole?,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    imageHeight: androidx.compose.ui.unit.Dp,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit
) {
    if (state is AndroidStudyState.Introduction) {
        StudyIntroductionCard(
            state = state,
            activeRole = activeRole,
            playAudio = playAudio,
            imageHeight = imageHeight,
            onEvent = onEvent,
            onOpenFullscreenImage = onOpenFullscreenImage
        )
        return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LearningEngineShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier.padding(horizontal = LearningSpacing.large, vertical = LearningSpacing.medium),
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
private fun StudyIntroductionCard(
    state: AndroidStudyState.Introduction,
    activeRole: AudioRole?,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    imageHeight: androidx.compose.ui.unit.Dp,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit
) {
    val isPlayingPrompt = activeRole == AudioRole.PROMPT || activeRole == AudioRole.MEANING
    val isPlayingExpected = activeRole == AudioRole.EXPECTED_ANSWER
    val isPlayingMeaning = activeRole == AudioRole.MEANING
    val isPlayingExampleEng = activeRole == AudioRole.EXAMPLE_ENGLISH
    val isPlayingExampleVie = activeRole == AudioRole.EXAMPLE_VIETNAMESE

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LearningEngineShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier.padding(horizontal = LearningSpacing.large, vertical = LearningSpacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.large)
        ) {
            if (!state.revealed) {
                LearningEngineAudioTextRow(
                    text = state.meaning ?: "Nghĩa tiếng Việt",
                    style = LearningContentTypography.vocabulary,
                    audioPath = state.resolvedMeaningAudio,
                    isPlaying = isPlayingPrompt,
                    isLooping = false,
                    onToggleAudio = { playAudio(AudioRole.MEANING, state.resolvedMeaningAudio, false) },
                    headingSemantics = true
                )

                if (state.resolvedImage != null) {
                    LearningEngineImage(
                        imagePath = state.resolvedImage,
                        imageUnavailable = false,
                        onOpenFullscreen = { onEvent(AndroidStudyEvent.RevealIntroduction) },
                        interactionDescription = "Learning image, tap to discover",
                        modifier = Modifier.height(imageHeight)
                    )
                } else {
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 112.dp)
                            .clickable { onEvent(AndroidStudyEvent.RevealIntroduction) }
                            .semantics { contentDescription = "Tap to discover the English word" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tap to discover", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }

                TextButton(
                    onClick = { onEvent(AndroidStudyEvent.RevealIntroduction) },
                    modifier = Modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget)
                ) { Text("Tap to discover") }
            } else {
                LearningEngineAudioTextRow(
                    text = state.answer,
                    style = LearningContentTypography.vocabulary,
                    audioPath = state.resolvedExpectedAnswerAudio ?: state.resolvedPromptAudio,
                    isPlaying = isPlayingExpected,
                    isLooping = true,
                    onToggleAudio = { playAudio(AudioRole.EXPECTED_ANSWER, state.resolvedExpectedAnswerAudio ?: state.resolvedPromptAudio, true) },
                    headingSemantics = true
                )

                if (!state.pronunciation.isNullOrBlank() || !state.partOfSpeech.isNullOrBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.pronunciation?.takeIf { it.isNotBlank() }?.let { pron ->
                            Text(
                                text = pron,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        state.partOfSpeech?.takeIf { it.isNotBlank() }?.let { pos ->
                            Text(
                                text = "($pos)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                state.meaning?.takeIf { it.isNotBlank() }?.let { meaning ->
                    LearningEngineAudioTextRow(
                        text = meaning,
                        style = MaterialTheme.typography.titleMedium,
                        audioPath = state.resolvedMeaningAudio,
                        isPlaying = isPlayingMeaning,
                        isLooping = false,
                        onToggleAudio = { playAudio(AudioRole.MEANING, state.resolvedMeaningAudio, false) }
                    )
                }

                state.resolvedImage?.let { imageUri ->
                    LearningEngineImage(
                        imagePath = imageUri,
                        imageUnavailable = false,
                        onOpenFullscreen = onOpenFullscreenImage,
                        modifier = Modifier.height(110.dp)
                    )
                }

                if (!state.example.isNullOrBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                        LearningEngineAudioTextRow(
                            text = state.example,
                            style = MaterialTheme.typography.bodyLarge,
                            audioPath = state.resolvedExampleEnglishAudio,
                            isPlaying = isPlayingExampleEng,
                            isLooping = true,
                            onToggleAudio = { playAudio(AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio, true) }
                        )
                        state.translation?.takeIf { it.isNotBlank() }?.let { trans ->
                            LearningEngineAudioTextRow(
                                text = trans,
                                style = MaterialTheme.typography.bodyMedium,
                                audioPath = state.resolvedExampleVietnameseAudio,
                                isPlaying = isPlayingExampleVie,
                                isLooping = false,
                                onToggleAudio = { playAudio(AudioRole.EXAMPLE_VIETNAMESE, state.resolvedExampleVietnameseAudio, false) }
                            )
                        }
                    }
                }

            }
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
            is AndroidStudyState.Introduction -> {}
            is AndroidStudyState.Typing -> {
                LearningEngineAudioTextRow(
                    text = state.prompt,
                    style = LearningContentTypography.vocabulary,
                    audioPath = state.resolvedPromptAudio,
                    isPlaying = isPlayingPrompt,
                    isLooping = true,
                    onToggleAudio = onTogglePromptAudio,
                    headingSemantics = true
                )
            }
            is AndroidStudyState.MultipleChoice -> {
                LearningEngineAudioTextRow(
                    text = state.question,
                    style = LearningContentTypography.sectionTitle,
                    audioPath = state.resolvedPromptAudio,
                    isPlaying = isPlayingPrompt,
                    isLooping = true,
                    onToggleAudio = onTogglePromptAudio,
                    headingSemantics = true
                )
            }
            is AndroidStudyState.Listening -> {
                LearningEngineAudioTextRow(
                    text = "Listen and type the answer",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    audioPath = state.resolvedPromptAudio,
                    isPlaying = isPlayingPrompt,
                    isLooping = true,
                    onToggleAudio = onTogglePromptAudio,
                    headingSemantics = true
                )
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
                val annotatedPrompt = buildAnnotatedString {
                    append(state.prefix)
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                        append(if (isRevealed) state.blank else " ".repeat(state.blank.length.coerceAtLeast(3)))
                    }
                    append(state.suffix)
                }
                val desc = if (isRevealed) "${state.prefix} ${state.blank} ${state.suffix}"
                else "${state.prefix} ${accessibilityStrings().blank} ${state.suffix}"

                LearningEngineAudioTextRow(
                    annotatedText = annotatedPrompt,
                    style = MaterialTheme.typography.headlineSmall,
                    audioPath = state.resolvedPromptAudio,
                    isPlaying = isPlayingPrompt,
                    isLooping = true,
                    onToggleAudio = onTogglePromptAudio,
                    headingSemantics = true,
                    contentDescriptionOverride = desc
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
        is AndroidStudyState.Introduction -> {}
        is AndroidStudyState.Typing -> {
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                AnswerField(
                    planId = state.plan.planId.value,
                    initialAnswer = state.answer,
                    enabled = !state.completed && !state.revealed,
                    error = state.evaluation == TypingAnswerEvaluationStatus.INCORRECT,
                    onAnswerChanged = { onEvent(AndroidStudyEvent.AnswerChanged(it)) },
                    onSubmit = { onEvent(AndroidStudyEvent.Submit(it)) }
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
                    FilledTonalButton(
                        onClick = { onEvent(AndroidStudyEvent.Choose(choice.id)) },
                        enabled = !state.completed,
                        shape = LearningEngineShapes.large,
                        colors = if (isSelected) ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) else ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
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
            var currentInputText by remember(state.plan.planId.value) { mutableStateOf(state.answer) }
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                AnswerField(
                    planId = state.plan.planId.value,
                    initialAnswer = state.answer,
                    enabled = !state.completed && !state.audioUnavailable,
                    error = false,
                    onAnswerChanged = {
                        currentInputText = it
                        onEvent(AndroidStudyEvent.AnswerChanged(it))
                    },
                    onSubmit = { onEvent(AndroidStudyEvent.Submit(it)) }
                )
                if (!state.completed) {
                    LearningEnginePrimaryButton(
                        label = "Submit answer",
                        onClick = { onEvent(AndroidStudyEvent.Submit(currentInputText)) },
                        enabled = currentInputText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        is AndroidStudyState.ImageRecall -> {
            val imageReady = !state.imageUnavailable
            var currentInputText by remember(state.plan.planId.value) { mutableStateOf(state.answer) }
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                AnswerField(
                    planId = state.plan.planId.value,
                    initialAnswer = state.answer,
                    enabled = !state.completed && imageReady,
                    error = false,
                    onAnswerChanged = {
                        currentInputText = it
                        onEvent(AndroidStudyEvent.AnswerChanged(it))
                    },
                    onSubmit = { onEvent(AndroidStudyEvent.Submit(it)) }
                )
                if (!state.completed) {
                    LearningEnginePrimaryButton(
                        label = "Submit answer",
                        onClick = { onEvent(AndroidStudyEvent.Submit(currentInputText)) },
                        enabled = currentInputText.isNotBlank() && imageReady,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        is AndroidStudyState.ExampleCompletion -> {
            var currentInputText by remember(state.plan.planId.value) { mutableStateOf(state.answer) }
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                AnswerField(
                    planId = state.plan.planId.value,
                    initialAnswer = state.answer,
                    enabled = !state.completed && !state.revealed,
                    error = false,
                    onAnswerChanged = {
                        currentInputText = it
                        onEvent(AndroidStudyEvent.AnswerChanged(it))
                    },
                    onSubmit = { onEvent(AndroidStudyEvent.Submit(it)) }
                )
                if (!state.completed) {
                    LearningEnginePrimaryButton(
                        label = "Submit answer",
                        onClick = { onEvent(AndroidStudyEvent.Submit(currentInputText)) },
                        enabled = currentInputText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerField(
    planId: String,
    initialAnswer: String,
    enabled: Boolean,
    error: Boolean,
    onAnswerChanged: (String) -> Unit,
    onSubmit: (String) -> Unit
) {
    var textFieldValue by rememberSaveable(planId, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = initialAnswer, selection = TextRange(initialAnswer.length)))
    }

    LaunchedEffect(initialAnswer) {
        if (initialAnswer.isEmpty() && textFieldValue.text.isNotEmpty()) {
            textFieldValue = TextFieldValue("")
        }
    }

    val focusRequester = remember(planId) { FocusRequester() }
    val bringIntoView = remember(planId) { BringIntoViewRequester() }
    var focusedForPlan by rememberSaveable(planId) { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(planId, enabled) {
        if (enabled && !focusedForPlan) {
            focusedForPlan = true
            focusRequester.requestFocus()
            bringIntoView.bringIntoView()
        }
        if (!enabled) {
            keyboard?.hide()
            focusManager.clearFocus()
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            if (enabled) {
                textFieldValue = newValue
                onAnswerChanged(newValue.text)
            }
        },
        enabled = enabled,
        singleLine = false,
        minLines = 1,
        maxLines = 4,
        isError = error,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            keyboard?.hide()
            focusManager.clearFocus()
            onSubmit(textFieldValue.text)
        }),
        label = { Text(accessibilityStrings().answer) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .bringIntoViewRequester(bringIntoView)
    )
}

@Composable
private fun StudyRevealAndFeedbackSection(
    state: AndroidStudyState.Runtime,
    activeRole: AudioRole?,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    onEvent: (AndroidStudyEvent) -> Unit
) {
    if (state is AndroidStudyState.Introduction) return
    val plan = state.plan ?: return
    val isRevealed = when (state) {
        is AndroidStudyState.Typing -> state.revealed
        is AndroidStudyState.ExampleCompletion -> state.revealed
        else -> false
    }

    val allowReveal = when (state) {
        is AndroidStudyState.Typing, is AndroidStudyState.ExampleCompletion -> true
        else -> false
    }

    val badgeScale by animateFloatAsState(
        targetValue = if (state.completed || isRevealed) 1.0f else 0.96f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "badge scale"
    )

    AnimatedVisibility(
        visible = state.completed || isRevealed,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) +
                slideInVertically(animationSpec = androidx.compose.animation.core.tween(200)) { fullHeight -> fullHeight / 10 },
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) + shrinkVertically(animationSpec = androidx.compose.animation.core.tween(200))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = LearningEngineShapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow
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
                Box(modifier = Modifier.graphicsLayer { scaleX = badgeScale; scaleY = badgeScale }) {
                    LearningEngineStatusBadge(label = badgeText, tone = tone)
                }

                Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                    LearningEngineAudioTextRow(
                        text = plan.answerContract.canonicalAnswer,
                        style = LearningContentTypography.vocabulary,
                        color = MaterialTheme.colorScheme.primary,
                        audioPath = state.resolvedExpectedAnswerAudio,
                        isPlaying = activeRole == AudioRole.EXPECTED_ANSWER,
                        isLooping = true,
                        onToggleAudio = { playAudio(AudioRole.EXPECTED_ANSWER, state.resolvedExpectedAnswerAudio, true) }
                    )
                }

                state.meaning?.takeIf { it.isNotBlank() }?.let { m ->
                    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                        LearningEngineAudioTextRow(
                            text = m,
                            style = MaterialTheme.typography.bodyLarge,
                            audioPath = state.resolvedMeaningAudio,
                            isPlaying = activeRole == AudioRole.MEANING,
                            isLooping = false,
                            onToggleAudio = { playAudio(AudioRole.MEANING, state.resolvedMeaningAudio, false) }
                        )
                    }
                }

                state.example?.takeIf { it.isNotBlank() }?.let { ex ->
                    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                        LearningEngineAudioTextRow(
                            text = ex,
                            style = LearningContentTypography.example,
                            audioPath = state.resolvedExampleEnglishAudio,
                            isPlaying = activeRole == AudioRole.EXAMPLE_ENGLISH,
                            isLooping = true,
                            onToggleAudio = { playAudio(AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio, true) }
                        )
                    }
                }

                state.translation?.takeIf { it.isNotBlank() }?.let { tr ->
                    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                        LearningEngineAudioTextRow(
                            text = tr,
                            style = LearningContentTypography.translation,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            audioPath = state.resolvedExampleVietnameseAudio,
                            isPlaying = activeRole == AudioRole.EXAMPLE_VIETNAMESE,
                            isLooping = false,
                            onToggleAudio = { playAudio(AudioRole.EXAMPLE_VIETNAMESE, state.resolvedExampleVietnameseAudio, false) }
                        )
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

                    if (plan.provenance == RecallProvenance.PRACTICE) {
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
            onClick = { onEvent(AndroidStudyEvent.Reveal()) },
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
