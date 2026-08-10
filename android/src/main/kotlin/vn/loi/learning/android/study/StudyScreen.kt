package vn.loi.learning.android.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.recall.RecallOutcome
import vn.loi.learning.domain.study.recall.RecallProvenance
import vn.loi.learning.domain.study.recall.StudyMode
import vn.loi.learning.android.media.AndroidAudioController
import vn.loi.learning.android.media.AndroidAudioState
import vn.loi.learning.android.platform.*
import vn.loi.learning.android.ui.*
import vn.loi.learning.android.study.components.StudyActionDock
import vn.loi.learning.android.study.components.StudyRatingBar
import vn.loi.learning.android.study.components.PartOfSpeechBadge
import vn.loi.learning.android.study.components.StudyAnswerSection
import vn.loi.learning.android.study.components.StudyAudioTextTarget
import vn.loi.learning.android.study.components.StudyRuntimeShell
import vn.loi.learning.android.study.components.StudyStageCard
import vn.loi.learning.android.study.components.StudyMedia
import vn.loi.learning.android.study.components.StudyAnswerInput
import vn.loi.learning.android.study.components.StudyPrompt
import vn.loi.learning.android.study.design.*
import vn.loi.learning.android.study.modes.ListeningStudyStage
import vn.loi.learning.android.study.modes.MultipleChoiceStudyStage
import vn.loi.learning.android.study.modes.ExampleCompletionStudyStage
import vn.loi.learning.android.study.modes.ImageRecallStudyStage
import vn.loi.learning.android.study.modes.TypingStudyStage

private fun accessibilityStrings() = androidAccessibilityStrings(java.util.Locale.getDefault().language)

internal data class AndroidLearningLandingPresentation(
    val primaryAction: AndroidHomePrimaryAction,
    val hasActiveSession: Boolean,
    val contextTitle: String?,
    val hasContent: Boolean,
    val dueCount: Int,
    val reviewedToday: Int,
    val accuracyPercent: Int?,
    val activeMemoryCount: Int,
    val totalMemoryCount: Int,
    val learningProgress: Float,
    val dailyBudget: vn.loi.learning.application.study.DailyStudyBudgetSnapshot?
)

internal fun resolveLearningLandingPresentation(state: AndroidStudyState.Home) =
    AndroidLearningLandingPresentation(
        primaryAction = state.model.primaryAction,
        hasActiveSession = state.availability.canResume,
        contextTitle = state.model.contextTitle,
        hasContent = state.model.hasContent,
        dueCount = state.model.dueCount,
        reviewedToday = state.model.reviewedToday,
        accuracyPercent = state.model.accuracyPercent,
        activeMemoryCount = state.model.activeMemoryCount,
        totalMemoryCount = state.model.totalMemoryCount,
        learningProgress = state.model.learningProgress,
        dailyBudget = state.model.dailyBudget
    )

@Composable
fun HomeScreen(
    state: AndroidStudyState.Home,
    contentState: AndroidContentOperationState = AndroidContentOperationState.Idle,
    onEvent: (AndroidStudyEvent) -> Unit,
    onContentAction: (AndroidOperationKind) -> Unit = {},
    onContentDismiss: () -> Unit = {},
    onLibrary: () -> Unit = {},
    onReview: () -> Unit = {},
    onStudyLauncher: () -> Unit = {}
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
        item("hero") { ContinueLearningCard(model, presentation, onEvent, onLibrary, onReview, onStudyLauncher) }
        item("stats") { HomeDashboardStats(presentation) }
        if (state.availability.canStartReview && presentation.dueCount > 0) {
            item("due-review") { DueReviewCard(model, onReview) }
        }
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
    onLibrary: () -> Unit,
    onReview: () -> Unit,
    onStudyLauncher: () -> Unit
) {
    val (title, detail, actionLabel) = when (model.primaryAction) {
        is AndroidHomePrimaryAction.Resume -> Triple("Continue learning", "Resume exactly where you left off.", "Continue session")
        AndroidHomePrimaryAction.ReviewDue -> Triple("Review is ready", "Choose Adaptive study or another explicit mode.", "Choose study mode")
        AndroidHomePrimaryAction.StartLearning -> Triple("Start learning", "Choose Learn new, Adaptive study, or Typing practice.", "Choose study mode")
        AndroidHomePrimaryAction.DailyComplete -> Triple("Today's study complete", "Your configured daily workload is complete.", "Open Library")
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
                AndroidHomePrimaryAction.ReviewDue,
                AndroidHomePrimaryAction.StartLearning -> onStudyLauncher()
                AndroidHomePrimaryAction.DailyComplete -> onLibrary()
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
        presentation.dailyBudget?.let { daily ->
            Text(
                "Today · NEW ${daily.newCompletedToday}/${daily.limits.newPerDay} · " +
                        "REVIEW ${daily.reviewCompletedToday}/${daily.limits.reviewPerDay}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StudyScreen(
    state: AndroidStudyState,
    onEvent: (AndroidStudyEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state is AndroidStudyState.Typing) {
        val observer = LifecycleEventObserver { _, event ->
            if (state is AndroidStudyState.Typing) when (event) {
                Lifecycle.Event.ON_STOP -> onEvent(AndroidStudyEvent.PauseTyping)
                Lifecycle.Event.ON_START -> onEvent(AndroidStudyEvent.ResumeTyping)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var outgoingFeedback by remember { mutableStateOf<OutgoingStudyFeedback?>(null) }
    var frozenIntroduction by remember { mutableStateOf<AndroidStudyState.Introduction?>(null) }
    var pendingIntroductionHudRating by remember { mutableStateOf<PendingIntroductionHudRating?>(null) }
    val reducedMotion = isReducedMotionEnabled()
    val context = LocalContext.current
    val feedbackAudioController = remember(context) { AndroidAudioController(context) }

    DisposableEffect(feedbackAudioController) {
        onDispose { feedbackAudioController.close() }
    }

    LaunchedEffect(outgoingFeedback?.feedbackId) {
        val feedback = outgoingFeedback ?: return@LaunchedEffect
        val audioFinished = CompletableDeferred<Unit>()
        val initialState = feedbackAudioController.replay(feedback.audio.path, isLooping = false) { audioState ->
            if (audioState is AndroidAudioState.Idle || audioState is AndroidAudioState.Failed) {
                audioFinished.complete(Unit)
            }
        }
        if (initialState is AndroidAudioState.Unavailable || initialState is AndroidAudioState.Failed) {
            audioFinished.complete(Unit)
        }
        withTimeoutOrNull(StudyRatingFeedbackPolicy.timeoutMillis) { audioFinished.await() }
        feedbackAudioController.stop()
        if (outgoingFeedback?.feedbackId == feedback.feedbackId) {
            outgoingFeedback = null
            frozenIntroduction = null
        }
    }

    LaunchedEffect(state, outgoingFeedback?.feedbackId, pendingIntroductionHudRating) {
        val pending = pendingIntroductionHudRating ?: return@LaunchedEffect
        val canonicalHud = (state as? AndroidStudyState.Runtime)?.hud
        if (outgoingFeedback == null && canonicalHud?.let(pending::isAcknowledgedBy) == true) {
            pendingIntroductionHudRating = null
        }
    }

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
                    if (initialState is AndroidStudyState.Runtime && targetState is AndroidStudyState.Runtime) {
                        val enterDuration = studyMotionDurationMillis(StudyMotionRole.CARD_ENTER, reducedMotion)
                        val exitDuration = studyMotionDurationMillis(StudyMotionRole.CARD_EXIT, reducedMotion)
                        (fadeIn(tween(enterDuration)) + slideInVertically(tween(enterDuration)) { it / 14 }) togetherWith
                                (fadeOut(tween(exitDuration)) + slideOutVertically(tween(exitDuration)) { -it / 14 })
                    } else if (reducedMotion) {
                        fadeIn(animationSpec = tween(durationMillis = 50)) togetherWith fadeOut(animationSpec = tween(durationMillis = 50))
                    } else {
                        val duration = studyMotionDurationMillis(StudyMotionRole.FEEDBACK, reducedMotion)
                        (fadeIn(animationSpec = tween(durationMillis = duration)) + slideInHorizontally(animationSpec = tween(durationMillis = duration)) { fullWidth -> fullWidth / 12 })
                            .togetherWith(fadeOut(animationSpec = tween(durationMillis = duration)) + slideOutHorizontally(animationSpec = tween(durationMillis = duration)) { fullWidth -> -fullWidth / 12 })
                    }
                }

                val presentedState = frozenIntroduction ?: state
                AnimatedContent(
                    targetState = presentedState,
                    contentKey = ::studyPresentationKey,
                    transitionSpec = transitionSpec,
                    label = "study destination"
                ) { target ->
                    when (target) {
                        AndroidStudyState.Loading -> LoadingStudy()
                        is AndroidStudyState.PreparingMode -> PreparingStudyMode(target)
                        is AndroidStudyState.Home -> HomeScreen(target, onEvent = onEvent)
                        is AndroidStudyState.Completion -> Completion(target, onEvent)
                        is AndroidStudyState.Failed -> StudyFailureState(target, onEvent)
                        is AndroidStudyState.Runtime -> {
                            StudyRuntimeScreen(
                                state = target,
                                onEvent = onEvent,
                                introductionAutoplayEnabled = outgoingFeedback == null,
                                feedbackRating = outgoingFeedback
                                    ?.takeIf { it.learningItemId == (target as? AndroidStudyState.Introduction)?.learningItemId }
                                    ?.selectedRating,
                                feedbackOrigin = outgoingFeedback?.origin,
                                pendingIntroductionHudRating = pendingIntroductionHudRating,
                                onIntroductionRatingWithFeedback = { introduction, rating, focus, origin ->
                                    frozenIntroduction = introduction.copy(
                                        revealedStage = true,
                                        compactRatingExit = !introduction.revealed
                                    )
                                    val feedback = outgoingStudyFeedback(introduction, rating, focus, origin)
                                    outgoingFeedback = feedback
                                    introduction.hud?.let { hud ->
                                        pendingIntroductionHudRating = optimisticIntroductionRating(hud, feedback)
                                    }
                                    onEvent(AndroidStudyEvent.RateIntroduction(rating))
                                },
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
    is AndroidStudyState.PreparingMode -> "preparing-${state.mode.name}"
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

@Composable
private fun PreparingStudyMode(state: AndroidStudyState.PreparingMode) {
    val label = when (state.mode) {
        StudyMode.LEARN_NEW -> "Preparing Learn new…"
        StudyMode.ADAPTIVE -> "Preparing Adaptive study…"
        StudyMode.TYPING -> "Preparing Typing practice…"
    }
    LearningEngineLoadingState(label = label)
}

internal enum class AudioRole { PROMPT, EXPECTED_ANSWER, MEANING, EXAMPLE_ENGLISH, EXAMPLE_VIETNAMESE }

internal data class IntroductionExampleAudioRoute(
    val role: AudioRole,
    val path: String?,
    val isLooping: Boolean
)

internal fun introductionExampleAudioRoute(
    vietnamese: Boolean,
    englishPath: String?,
    vietnamesePath: String?
): IntroductionExampleAudioRoute = if (vietnamese) {
    IntroductionExampleAudioRoute(AudioRole.EXAMPLE_VIETNAMESE, vietnamesePath, false)
} else {
    IntroductionExampleAudioRoute(AudioRole.EXAMPLE_ENGLISH, englishPath, true)
}

private fun Modifier.introductionStageGestures(
    itemKey: String,
    alreadySubmitted: Boolean,
    ratingEnabled: Boolean,
    onDragOffset: (Float) -> Unit,
    onHorizontalDragOffset: (Float) -> Unit,
    onGestureEnd: (IntroductionStageGesture) -> Unit,
    onSwipeGood: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) = pointerInput(itemKey, alreadySubmitted, ratingEnabled) {
    val swipeThresholdPx = 72.dp.toPx()
    val tapSlopPx = 12.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var end = down.position
        var childConsumed = down.isConsumed
        var pressed = true
        var ownsUpwardDrag = false
        var ownsHorizontalDrag = false
        while (pressed) {
            // Observe the completed dispatch pass so child click targets can mark the event
            // consumed before the card-level toggle decides whether this was whitespace.
            val event = awaitPointerEvent(PointerEventPass.Final)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            end = change.position
            childConsumed = childConsumed || change.isConsumed
            val deltaX = end.x - down.position.x
            val deltaY = end.y - down.position.y
            if (!ownsUpwardDrag && kotlin.math.abs(deltaX) > tapSlopPx &&
                kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) * 1.35f
            ) {
                ownsHorizontalDrag = true
            }
            if (ownsHorizontalDrag) {
                change.consume()
                onHorizontalDragOffset(deltaX)
            } else if (ratingEnabled && deltaY < -tapSlopPx && kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) * 1.35f) {
                ownsUpwardDrag = true
                change.consume()
                onDragOffset(deltaY.coerceAtMost(0f))
            }
            pressed = change.pressed
        }
        val gesture = resolveIntroductionStageGesture(
            deltaX = end.x - down.position.x,
            deltaY = end.y - down.position.y,
            swipeThresholdPx = swipeThresholdPx,
            tapSlopPx = tapSlopPx,
            scrollRequired = false,
            childConsumed = childConsumed && !ownsUpwardDrag && !ownsHorizontalDrag,
            alreadySubmitted = alreadySubmitted,
            ratingEnabled = ratingEnabled
        )
        when (gesture) {
            IntroductionStageGesture.TAP -> {
                onGestureEnd(gesture)
            }
            IntroductionStageGesture.SWIPE_GOOD -> {
                onSwipeGood()
                onGestureEnd(gesture)
            }
            IntroductionStageGesture.NONE -> onGestureEnd(gesture)
            IntroductionStageGesture.PREVIOUS -> { onGestureEnd(gesture); onPrevious() }
            IntroductionStageGesture.NEXT -> { onGestureEnd(gesture); onNext() }
        }
    }
}

@Composable
private fun StudyRuntimeScreen(
    state: AndroidStudyState.Runtime,
    onEvent: (AndroidStudyEvent) -> Unit,
    introductionAutoplayEnabled: Boolean,
    feedbackRating: ReviewRating?,
    feedbackOrigin: IntroductionRatingFeedbackOrigin?,
    pendingIntroductionHudRating: PendingIntroductionHudRating?,
    onIntroductionRatingWithFeedback: (
        AndroidStudyState.Introduction,
        ReviewRating,
        IntroductionPlaybackFocus,
        IntroductionRatingFeedbackOrigin
    ) -> Unit,
    onOpenFullscreenImage: (String) -> Unit
) {
    val modeLabel = when (state) {
        is AndroidStudyState.Introduction -> "NEW"
        is AndroidStudyState.Typing -> "Typing"
        is AndroidStudyState.MultipleChoice -> "MCQ"
        is AndroidStudyState.Listening -> "Listening"
        is AndroidStudyState.ImageRecall -> "Image"
        is AndroidStudyState.ExampleCompletion -> "Cloze"
    }

    val context = LocalContext.current
    val audioController = remember(context) { AndroidAudioController(context) }
    val itemKey = when (state) {
        is AndroidStudyState.Introduction -> state.learningItemId
        else -> state.requireRecallPlan().planId.value
    }
    var activeRole by remember(itemKey) { mutableStateOf<AudioRole?>(null) }
    var introductionPlaybackFocus by remember(itemKey) { mutableStateOf(IntroductionPlaybackFocus.WORD) }
    var introductionImageExpanded by remember(itemKey) { mutableStateOf(false) }
    var swipeRatingSubmitted by remember(itemKey) { mutableStateOf(false) }
    var revealAudioStarted by remember(itemKey) { mutableStateOf(false) }

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
                when (role) {
                    AudioRole.EXPECTED_ANSWER -> {
                        introductionPlaybackFocus = IntroductionPlaybackFocus.WORD
                    }
                    AudioRole.EXAMPLE_ENGLISH -> {
                        introductionPlaybackFocus = IntroductionPlaybackFocus.EXAMPLE
                    }
                    else -> Unit
                }
                audioController.replay(path, isLooping = isLooping) { state ->
                    if (state is AndroidAudioState.Idle || state is AndroidAudioState.Failed) {
                        if (activeRole == role) activeRole = null
                    }
                }
            }
        }
    }

    val restartAudio: (AudioRole, String?, Boolean) -> Unit = { role, path, isLooping ->
        if (!path.isNullOrBlank()) {
            audioController.stop()
            activeRole = role
            when (role) {
                AudioRole.EXPECTED_ANSWER -> {
                    introductionPlaybackFocus = IntroductionPlaybackFocus.WORD
                }
                AudioRole.EXAMPLE_ENGLISH -> {
                    introductionPlaybackFocus = IntroductionPlaybackFocus.EXAMPLE
                }
                else -> Unit
            }
            audioController.replay(path, isLooping = isLooping) { playbackState ->
                if (playbackState is AndroidAudioState.Idle || playbackState is AndroidAudioState.Failed) {
                    if (activeRole == role) activeRole = null
                }
            }
        }
    }

    val toggleIntroductionEnglishLoop: () -> Unit = {
        val introduction = state as? AndroidStudyState.Introduction
        if (introduction != null && introduction.revealed) {
            nextIntroductionPlaybackFocus(
                introductionPlaybackFocus,
                hasWordAudio = !introduction.resolvedExpectedAnswerAudio.isNullOrBlank() ||
                        !introduction.resolvedPromptAudio.isNullOrBlank(),
                hasExampleAudio = !introduction.resolvedExampleEnglishAudio.isNullOrBlank()
            )?.let { nextFocus ->
                if (nextFocus == IntroductionPlaybackFocus.WORD) {
                    restartAudio(
                        AudioRole.EXPECTED_ANSWER,
                        introduction.resolvedExpectedAnswerAudio ?: introduction.resolvedPromptAudio,
                        true
                    )
                } else {
                    restartAudio(AudioRole.EXAMPLE_ENGLISH, introduction.resolvedExampleEnglishAudio, true)
                }
            }
        }
    }

    val stopAudioAndDispatch: (AndroidStudyEvent) -> Unit = { event ->
        audioController.stop()
        activeRole = null
        onEvent(event)
    }
    val submitIntroductionRating: (ReviewRating, IntroductionRatingFeedbackOrigin) -> Unit = { rating, origin ->
        if (state is AndroidStudyState.Introduction && !swipeRatingSubmitted) {
            swipeRatingSubmitted = true
            onIntroductionRatingWithFeedback(state, rating, introductionPlaybackFocus, origin)
            audioController.stop()
            activeRole = null
        }
    }

    LaunchedEffect(itemKey, introductionAutoplayEnabled) {
        if (introductionAutoplayEnabled && state is AndroidStudyState.Introduction &&
            !state.revealed && !state.resolvedMeaningAudio.isNullOrBlank()
        ) {
            playAudio(AudioRole.MEANING, state.resolvedMeaningAudio, false)
        }
    }


    LaunchedEffect(itemKey, (state as? AndroidStudyState.Introduction)?.revealed) {
        val introduction = state as? AndroidStudyState.Introduction ?: return@LaunchedEffect
        if (introduction.revealed && !revealAudioStarted) {
            revealAudioStarted = true
            restartAudio(
                AudioRole.EXPECTED_ANSWER,
                introduction.resolvedExpectedAnswerAudio ?: introduction.resolvedPromptAudio,
                true
            )
        }
    }

    LaunchedEffect(itemKey, (state as? AndroidStudyState.Typing)?.completionPending) {
        val typing = state as? AndroidStudyState.Typing ?: return@LaunchedEffect
        if (!typing.completionPending) return@LaunchedEffect
        audioController.stop()
        activeRole = AudioRole.EXPECTED_ANSWER
        val audioFinished = CompletableDeferred<Unit>()
        val initial = audioController.replay(typing.resolvedExpectedAnswerAudio, isLooping = false) { playback ->
            if (playback is AndroidAudioState.Idle || playback is AndroidAudioState.Failed) {
                activeRole = null
                audioFinished.complete(Unit)
            }
        }
        if (initial is AndroidAudioState.Unavailable || initial is AndroidAudioState.Failed) {
            activeRole = null
            audioFinished.complete(Unit)
        }
        withTimeoutOrNull(120_000L) { audioFinished.await() }
        onEvent(AndroidStudyEvent.TypingSuccessAudioCompleted)
    }
    LaunchedEffect(itemKey) {
        val typing = state as? AndroidStudyState.Typing ?: return@LaunchedEffect
        if (!typing.viAutoplayMuted && !typing.resolvedMeaningAudio.isNullOrBlank()) {
            restartAudio(AudioRole.MEANING, typing.resolvedMeaningAudio, false)
        }
    }

    val isRevealed = when (state) {
        is AndroidStudyState.Introduction -> state.revealed
        is AndroidStudyState.Typing -> state.revealed
        is AndroidStudyState.ExampleCompletion -> state.revealed
        else -> false
    }
    val typingSuccessPending = (state as? AndroidStudyState.Typing)?.completionPending == true
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
            if (state is AndroidStudyState.Typing && state.outcome == RecallOutcome.CORRECT) {
                scrollState.scrollTo(0)
            } else {
                bringIntoViewRequester.bringIntoView()
            }
        }
    }

    val layoutPolicy = LocalLayoutPolicy.current
    val contentDensity = when {
        layoutPolicy.maxMediaHeightDp <= 180 -> StudyContentDensity.DENSE
        layoutPolicy.maxMediaHeightDp >= 420 -> StudyContentDensity.RELAXED
        else -> StudyContentDensity.STANDARD
    }

    StudyRuntimeShell(
        title = state.contextTitle ?: "Study",
        modeLabel = modeLabel,
        currentPosition = (state as? AndroidStudyState.Introduction)?.packagePosition ?: state.currentPosition,
        totalItems = (state as? AndroidStudyState.Introduction)?.packageTotal ?: state.totalItems,
        onBack = { stopAudioAndDispatch(AndroidStudyEvent.Home) },
        header = {
            state.hud?.let { hud ->
                if (state is AndroidStudyState.Introduction) {
                    LearnNewProgressHeader(state, hud, pendingIntroductionHudRating)
                }
                else LearningEngineCompactHud(hud)
            }
        }
    ) {
        LearningEngineLearningStage(
            modifier = if (state is AndroidStudyState.Introduction) Modifier.fillMaxWidth().weight(1f)
            else Modifier.fillMaxWidth().verticalScroll(scrollState),
            state = state,
            activeRole = activeRole,
            playAudio = playAudio,
            restartAudio = restartAudio,
            contentDensity = contentDensity,
            availableMediaHeightDp = layoutPolicy.maxMediaHeightDp * 2,
            revealBringIntoViewRequester = bringIntoViewRequester,
            introductionImageExpanded = introductionImageExpanded,
            swipeRatingSubmitted = swipeRatingSubmitted,
            feedbackRating = feedbackRating,
            feedbackOrigin = feedbackOrigin,
            onIntroductionImageExpandedChange = { introductionImageExpanded = it },
            onIntroductionStageTap = {
                if (state is AndroidStudyState.Introduction) {
                    if (!state.revealed) {
                        stopAudioAndDispatch(AndroidStudyEvent.RevealIntroduction)
                    } else {
                        toggleIntroductionEnglishLoop()
                    }
                }
            },
            onIntroductionSwipeGood = {
                submitIntroductionRating(ReviewRating.GOOD, IntroductionRatingFeedbackOrigin.SWIPE_GOOD)
            },
            onIntroductionPrevious = { stopAudioAndDispatch(AndroidStudyEvent.PreviousVisited) },
            onIntroductionNext = { stopAudioAndDispatch(AndroidStudyEvent.NextVisited) },
            onIntroductionRating = { submitIntroductionRating(it, IntroductionRatingFeedbackOrigin.MANUAL_BUTTON) },
            onEvent = stopAudioAndDispatch,
            onOpenFullscreenImage = onOpenFullscreenImage
        )
    }
}

@Composable
private fun LearningEngineCompactHud(hud: AndroidStudySessionHud) {
    val newDescription = progressDescription("New", hud.newCompleted, hud.newTarget, hud.newConfiguredTarget)
    val reviewDescription = progressDescription(
        "Review", hud.reviewCompleted, hud.reviewTarget, hud.reviewConfiguredTarget
    )
    Surface(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "$newDescription. $reviewDescription. Total learned ${hud.totalLearned}. " +
                    "Due ${hud.dueCount}. Again ${hud.againCount}, Hard ${hud.hardCount}, " +
                    "Good ${hud.goodCount}, Easy ${hud.easyCount}."
        },
        shape = LearningEngineShapes.extraSmall,
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = LearningSpacing.extraSmall, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HudInlineMetric("TOTAL", hud.totalLearned.toString())
                HudInlineMetric("NEW", "${hud.newCompleted}/${hud.newTarget}")
                HudInlineMetric("REVIEW", "${hud.reviewCompleted}/${hud.reviewTarget}")
                HudInlineMetric("DUE", hud.dueCount.toString())
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HudRating("A", hud.againCount)
                HudRating("H", hud.hardCount)
                HudRating("G", hud.goodCount)
                HudRating("E", hud.easyCount)
            }
        }
    }
}

@Composable
private fun LearnNewProgressHeader(
    state: AndroidStudyState.Introduction,
    hud: AndroidStudySessionHud,
    pendingRating: PendingIntroductionHudRating?
) {
    val position = state.currentPosition?.coerceAtLeast(1)
    val total = state.totalItems?.coerceAtLeast(position ?: 1)
    val progress = if (position != null && total != null && total > 0) position.toFloat() / total else 0f
    Surface(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "Learn new. Item ${position ?: 1} of ${total ?: hud.newTarget}. " +
                    "Today ${hud.newCompleted} of ${hud.newConfiguredTarget}. Due ${hud.dueCount}."
        },
        color = Color.Transparent
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CompactLearnMetric("Total", hud.totalLearned.toString())
                CompactLearnMetric("New", "${hud.newCompleted}/${hud.newConfiguredTarget}")
                CompactLearnMetric("Review", "${hud.reviewCompleted}/${hud.reviewConfiguredTarget}")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CompactLearnMetric("Due", hud.dueCount.toString())
                CompactLearnMetric("Again", displayedIntroductionRatingCount(hud, ReviewRating.AGAIN, pendingRating).toString(), MaterialTheme.colorScheme.error, pendingRating?.takeIf { it.rating == ReviewRating.AGAIN }?.feedbackId)
                CompactLearnMetric("Hard", displayedIntroductionRatingCount(hud, ReviewRating.HARD, pendingRating).toString(), LearningEngineThemeTokens.semanticColors.warning, pendingRating?.takeIf { it.rating == ReviewRating.HARD }?.feedbackId)
                CompactLearnMetric("Good", displayedIntroductionRatingCount(hud, ReviewRating.GOOD, pendingRating).toString(), LearningEngineThemeTokens.semanticColors.success, pendingRating?.takeIf { it.rating == ReviewRating.GOOD }?.feedbackId)
                CompactLearnMetric("Easy", displayedIntroductionRatingCount(hud, ReviewRating.EASY, pendingRating).toString(), MaterialTheme.colorScheme.tertiary, pendingRating?.takeIf { it.rating == ReviewRating.EASY }?.feedbackId)
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }
}

@Composable
private fun CompactLearnMetric(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    pulseKey: String? = null
) {
    val reducedMotion = isReducedMotionEnabled()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pulseKey) {
        if (pulseKey != null && !reducedMotion) {
            scale.snapTo(1f)
            scale.animateTo(1.14f, tween(180))
            scale.animateTo(1f, tween(220))
        }
    }
    Row(
        modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun HudInlineMetric(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HudRating(label: String, value: Int) {
    Text(
        "$label $value",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LearningEngineLearningStage(
    modifier: Modifier = Modifier,
    state: AndroidStudyState.Runtime,
    activeRole: AudioRole?,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    restartAudio: (AudioRole, String?, Boolean) -> Unit,
    contentDensity: StudyContentDensity,
    availableMediaHeightDp: Int,
    revealBringIntoViewRequester: BringIntoViewRequester,
    introductionImageExpanded: Boolean,
    swipeRatingSubmitted: Boolean,
    feedbackRating: ReviewRating?,
    feedbackOrigin: IntroductionRatingFeedbackOrigin?,
    onIntroductionImageExpandedChange: (Boolean) -> Unit,
    onIntroductionStageTap: () -> Unit,
    onIntroductionSwipeGood: () -> Unit,
    onIntroductionPrevious: () -> Unit,
    onIntroductionNext: () -> Unit,
    onIntroductionRating: (ReviewRating) -> Unit,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit
) {
    if (state is AndroidStudyState.Introduction) {
        IntroductionLearningStage(
            modifier = modifier,
            state = state,
            activeRole = activeRole,
            playAudio = playAudio,
            restartAudio = restartAudio,
            imageExpanded = introductionImageExpanded,
            swipeRatingSubmitted = swipeRatingSubmitted,
            feedbackRating = feedbackRating,
            feedbackOrigin = feedbackOrigin,
            onImageExpandedChange = onIntroductionImageExpandedChange,
            onGenericStageTap = onIntroductionStageTap,
            onSwipeGood = onIntroductionSwipeGood,
            onPrevious = onIntroductionPrevious,
            onNext = onIntroductionNext,
            onRating = onIntroductionRating,
            onEvent = onEvent,
            onOpenFullscreenImage = onOpenFullscreenImage
        )
        return
    }
    val feedbackContent: @Composable () -> Unit = {
        Box(modifier = Modifier.bringIntoViewRequester(revealBringIntoViewRequester)) {
            StudyRevealAndFeedbackContent(state, activeRole, playAudio, onEvent)
        }
    }
    if (state is AndroidStudyState.Typing) {
        TypingStudyStage(
            state = state,
            activeRole = activeRole,
            baseDensity = contentDensity,
            availableMediaHeightDp = availableMediaHeightDp,
            playAudio = playAudio,
            onEvent = onEvent,
            onOpenFullscreenImage = onOpenFullscreenImage,
            feedbackContent = feedbackContent,
            modifier = modifier
        )
        return
    }
    if (state is AndroidStudyState.Listening) {
        ListeningStudyStage(
            state = state,
            activeRole = activeRole,
            baseDensity = contentDensity,
            playAudio = playAudio,
            onEvent = onEvent,
            feedbackContent = feedbackContent,
            modifier = modifier
        )
        return
    }
    if (state is AndroidStudyState.MultipleChoice) {
        MultipleChoiceStudyStage(
            state = state,
            activeRole = activeRole,
            baseDensity = contentDensity,
            availableMediaHeightDp = availableMediaHeightDp,
            playAudio = playAudio,
            onEvent = onEvent,
            onOpenFullscreenImage = onOpenFullscreenImage,
            feedbackContent = feedbackContent,
            modifier = modifier
        )
        return
    }
    if (state is AndroidStudyState.ImageRecall) {
        ImageRecallStudyStage(
            state = state,
            baseDensity = contentDensity,
            availableMediaHeightDp = availableMediaHeightDp,
            onEvent = onEvent,
            onOpenFullscreenImage = onOpenFullscreenImage,
            feedbackContent = feedbackContent,
            modifier = modifier
        )
        return
    }
    if (state is AndroidStudyState.ExampleCompletion) {
        ExampleCompletionStudyStage(
            state = state,
            activeRole = activeRole,
            baseDensity = contentDensity,
            playAudio = playAudio,
            onEvent = onEvent,
            feedbackContent = feedbackContent,
            modifier = modifier
        )
        return
    }
}

@Composable
private fun IntroductionLearningStage(
    modifier: Modifier = Modifier,
    state: AndroidStudyState.Introduction,
    activeRole: AudioRole?,
    playAudio: (AudioRole, String?, Boolean) -> Unit,
    restartAudio: (AudioRole, String?, Boolean) -> Unit,
    imageExpanded: Boolean,
    swipeRatingSubmitted: Boolean,
    feedbackRating: ReviewRating?,
    feedbackOrigin: IntroductionRatingFeedbackOrigin?,
    onImageExpandedChange: (Boolean) -> Unit,
    onGenericStageTap: () -> Unit,
    onSwipeGood: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRating: (ReviewRating) -> Unit,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit
) {
    val isPlayingExpected = activeRole == AudioRole.EXPECTED_ANSWER
    val isPlayingMeaning = activeRole == AudioRole.MEANING
    val isPlayingExampleEng = activeRole == AudioRole.EXAMPLE_ENGLISH
    val isPlayingExampleVie = activeRole == AudioRole.EXAMPLE_VIETNAMESE
    val reducedMotion = isReducedMotionEnabled()
    val feedbackVisual = when (feedbackRating) {
        ReviewRating.AGAIN -> StudyFeedbackVisualState.RATING_AGAIN
        ReviewRating.HARD -> StudyFeedbackVisualState.RATING_HARD
        ReviewRating.GOOD -> StudyFeedbackVisualState.RATING_GOOD
        ReviewRating.EASY -> StudyFeedbackVisualState.RATING_EASY
        null -> StudyFeedbackVisualState.NEUTRAL
    }
    var swipeOffsetTarget by remember(state.learningItemId) { mutableFloatStateOf(0f) }
    var horizontalOffsetTarget by remember(state.learningItemId) { mutableFloatStateOf(0f) }
    var swipeCommitPending by remember(state.learningItemId) { mutableStateOf(false) }
    val backgroundInteraction = remember(state.learningItemId) { MutableInteractionSource() }
    val stagePressed by backgroundInteraction.collectIsPressedAsState()
    val swipeOffset by animateFloatAsState(
        targetValue = swipeOffsetTarget,
        animationSpec = tween(studyMotionDurationMillis(StudyMotionRole.PRESS, reducedMotion)),
        label = "Learn new swipe position"
    )
    val horizontalOffset by animateFloatAsState(
        targetValue = horizontalOffsetTarget,
        animationSpec = tween(if (reducedMotion) 0 else 170),
        label = "Introduction horizontal traversal"
    )
    val stageScale by animateFloatAsState(
        targetValue = if (stagePressed && !reducedMotion) 0.994f else 1f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 100),
        label = "Learn new press feedback"
    )
    val imageFeedbackScale by animateFloatAsState(
        targetValue = if (feedbackRating != null && !reducedMotion) 1.02f else 1f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 190),
        label = "Introduction rating image feedback"
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val bounds = resolveIntroductionImageBounds(maxHeight.value.toInt())
        val maximumContentOffsetPx = with(LocalDensity.current) { 8.dp.toPx() }
        val maximumHorizontalOffsetPx = with(LocalDensity.current) { 48.dp.toPx() }
        val introductionScrollState = rememberLazyListState()
        val targetMaxHeightDp = when {
            !state.revealed || imageExpanded -> bounds.frontMaxHeightDp
            else -> bounds.revealMaxHeightDp
        }
        val imageMaxHeight by animateDpAsState(
            targetValue = targetMaxHeightDp.dp,
            animationSpec = tween(studyMotionDurationMillis(StudyMotionRole.MEDIA_RESIZE, reducedMotion)),
            label = "Introduction hero transformation"
        )
        StudyStageCard(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = stageScale
                scaleY = stageScale
            },
            feedback = feedbackVisual
        ) {
            Column(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = introductionScrollState,
                    modifier = Modifier.fillMaxWidth().weight(1f).graphicsLayer {
                        translationY = swipeOffset.coerceIn(-maximumContentOffsetPx, 0f)
                        translationX = if (reducedMotion) 0f else horizontalOffset.coerceIn(
                            -maximumHorizontalOffsetPx, maximumHorizontalOffsetPx
                        )
                    }.clickable(
                        interactionSource = backgroundInteraction,
                        indication = null,
                        onClick = onGenericStageTap
                    ).introductionStageGestures(
                        itemKey = state.learningItemId,
                        alreadySubmitted = swipeRatingSubmitted || swipeCommitPending,
                        ratingEnabled = true,
                        onDragOffset = { swipeOffsetTarget = it },
                        onHorizontalDragOffset = { horizontalOffsetTarget = it },
                        onGestureEnd = { gesture ->
                            swipeOffsetTarget = 0f
                            horizontalOffsetTarget = 0f
                        },
                        onSwipeGood = {
                            if (!swipeCommitPending && !swipeRatingSubmitted) {
                                swipeCommitPending = true
                                onSwipeGood()
                            }
                        },
                        onPrevious = onPrevious,
                        onNext = onNext
                    ),
                    contentPadding = PaddingValues(horizontal = LearningSpacing.medium, vertical = LearningSpacing.extraSmall),
                    verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
                ) {
                    item("introduction-content") {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
                        ) {
                            val meaning = state.meaning ?: "Nghĩa tiếng Việt"
                            if (!state.revealed) state.resolvedImage?.let { imageUri ->
                                LearningEngineImage(
                                    imagePath = imageUri,
                                    imageUnavailable = false,
                                    onOpenFullscreen = { onEvent(AndroidStudyEvent.RevealIntroduction) },
                                    fillCanvas = true,
                                    adaptiveFitBounds = LearningImageFitBounds(120, imageMaxHeight.value.toInt()),
                                    interactionDescription = "Learning image, tap to discover",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // FRONT hierarchy:
                            // IMAGE -> reveal hint -> 20dp -> VI meaning -> 10dp -> POS
                            AnimatedVisibility(
                                visible = !state.revealed,
                                enter = fadeIn(tween(studyMotionDurationMillis(StudyMotionRole.REVEAL, reducedMotion))),
                                exit = fadeOut(tween(studyMotionDurationMillis(StudyMotionRole.CARD_EXIT, reducedMotion)))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    IntroductionInteractionHint(
                                        primary = "Tap to reveal",
                                        secondary = "Recall the English word",
                                        emphasized = true
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    StudyAudioTextTarget(
                                        text = meaning,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontSize = introductionClueTextSizeSp(meaning.length).sp,
                                            lineHeight = (introductionClueTextSizeSp(meaning.length) + 6).sp
                                        ),
                                        audioPath = state.resolvedMeaningAudio,
                                        isPlaying = isPlayingMeaning,
                                        isLooping = false,
                                        onToggleAudio = { playAudio(AudioRole.MEANING, state.resolvedMeaningAudio, false) },
                                        centered = true,
                                        maxLines = 3,
                                        headingSemantics = true
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    partOfSpeechPresentation(state.partOfSpeech)?.let { pos ->
                                        PartOfSpeechBadge(pos)
                                    }
                                }
                            }

                            if (state.revealed) state.resolvedImage?.let { imageUri ->
                                LearningEngineImage(
                                    imagePath = imageUri,
                                    imageUnavailable = false,
                                    onOpenFullscreen = {
                                        if (state.revealed) {
                                            onImageExpandedChange(!imageExpanded)
                                            onGenericStageTap()
                                        } else {
                                            onEvent(AndroidStudyEvent.RevealIntroduction)
                                        }
                                    },
                                    onOpenFullscreenSecondary = if (state.revealed) onOpenFullscreenImage else null,
                                    fillCanvas = true,
                                    adaptiveFitBounds = LearningImageFitBounds(
                                        minHeightDp = 120,
                                        maxHeightDp = imageMaxHeight.value.toInt()
                                    ),
                                    interactionDescription = when {
                                        !state.revealed -> "Learning image, tap to discover"
                                        imageExpanded -> "Learning image expanded, tap to reduce"
                                        else -> "Learning image, tap to expand"
                                    },
                                    modifier = Modifier.fillMaxWidth().graphicsLayer {
                                        scaleX = imageFeedbackScale
                                        scaleY = imageFeedbackScale
                                    }
                                )
                            } ?: Box(
                                Modifier.fillMaxWidth().heightIn(min = 120.dp, max = imageMaxHeight)
                                    .semantics { contentDescription = "Learning canvas, tap to discover the English word" }
                            )

                            AnimatedVisibility(
                                visible = state.revealed,
                                enter = fadeIn(tween(studyMotionDurationMillis(StudyMotionRole.REVEAL, reducedMotion))) +
                                        slideInVertically(tween(studyMotionDurationMillis(StudyMotionRole.REVEAL, reducedMotion))) { it / 14 },
                                exit = fadeOut(tween(studyMotionDurationMillis(StudyMotionRole.CARD_EXIT, reducedMotion)))
                            ) {
                                StudyAnswerSection(
                                    englishAnswer = state.answer,
                                    pronunciation = normalizedIntroductionPronunciation(state.partOfSpeech, state.pronunciation),
                                    partOfSpeech = partOfSpeechPresentation(state.partOfSpeech),
                                    vietnameseAnswer = meaning,
                                    englishExample = if (state.compactRatingExit) null else state.example,
                                    vietnameseExample = if (state.compactRatingExit) null else state.translation,
                                    answerAudioPath = state.resolvedExpectedAnswerAudio ?: state.resolvedPromptAudio,
                                    vietnameseAudioPath = state.resolvedMeaningAudio,
                                    englishExampleAudioPath = state.resolvedExampleEnglishAudio,
                                    vietnameseExampleAudioPath = state.resolvedExampleVietnameseAudio,
                                    isPlayingAnswer = isPlayingExpected,
                                    isPlayingVietnamese = isPlayingMeaning,
                                    isPlayingEnglishExample = isPlayingExampleEng,
                                    isPlayingVietnameseExample = isPlayingExampleVie,
                                    onAnswerAudio = {
                                        onGenericStageTap()
                                    },
                                    onVietnameseAudio = { playAudio(AudioRole.MEANING, state.resolvedMeaningAudio, false) },
                                    onEnglishExampleAudio = {
                                        restartAudio(AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio, true)
                                    },
                                    onVietnameseExampleAudio = {
                                        playAudio(AudioRole.EXAMPLE_VIETNAMESE, state.resolvedExampleVietnameseAudio, false)
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = StudyContentSpacing.imageToAnswer),
                                    answerHero = true
                                )
                            }

                        }
                    }
                }
                StudyRatingBar(
                    onRating = onRating,
                    selectedRating = feedbackRating,
                    enabled = !state.historyPreview,
                    modifier = Modifier.fillMaxWidth().padding(
                        start = LearningSpacing.medium,
                        end = LearningSpacing.medium,
                        top = StudyContentSpacing.examplesToRating,
                        bottom = StudyContentSpacing.ratingToActions
                    )
                )
                if (state.revealed && !state.compactRatingExit) {
                    StudyActionDock(
                        hasWordAudio = !state.resolvedExpectedAnswerAudio.isNullOrBlank() ||
                                !state.resolvedPromptAudio.isNullOrBlank(),
                        hasExampleAudio = !state.resolvedExampleEnglishAudio.isNullOrBlank(),
                        hasImage = !state.resolvedImage.isNullOrBlank(),
                        isWordPlaying = isPlayingExpected,
                        isExamplePlaying = isPlayingExampleEng,
                        onWordAudio = {
                            playAudio(
                                AudioRole.EXPECTED_ANSWER,
                                state.resolvedExpectedAnswerAudio ?: state.resolvedPromptAudio,
                                true
                            )
                        },
                        onReplay = {
                            restartAudio(
                                AudioRole.EXPECTED_ANSWER,
                                state.resolvedExpectedAnswerAudio ?: state.resolvedPromptAudio,
                                true
                            )
                        },
                        onExampleAudio = {
                            playAudio(AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio, true)
                        },
                        onFullscreenImage = { state.resolvedImage?.let(onOpenFullscreenImage) },
                        modifier = Modifier.fillMaxWidth().padding(
                            start = LearningSpacing.medium,
                            end = LearningSpacing.medium,
                            bottom = LearningSpacing.extraSmall
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroductionInteractionHint(
    primary: String,
    secondary: String,
    emphasized: Boolean
) {
    Surface(
        shape = LearningEngineShapes.large,
        color = if (emphasized) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        contentColor = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$primary. $secondary"
        }
    ) {
        Column(
            Modifier.padding(horizontal = LearningSpacing.medium, vertical = LearningSpacing.extraSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                secondary,
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.78f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
private fun StudyRevealAndFeedbackContent(
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
    val typingSuccessPending = (state as? AndroidStudyState.Typing)?.completionPending == true

    val allowReveal = when (state) {
        is AndroidStudyState.ExampleCompletion -> true
        else -> false
    }
    val revealMotionDuration = studyMotionDurationMillis(StudyMotionRole.REVEAL, isReducedMotionEnabled())

    val badgeScale by animateFloatAsState(
        targetValue = if (state.completed || isRevealed) 1.0f else 0.96f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = revealMotionDuration),
        label = "badge scale"
    )

    AnimatedVisibility(
        visible = state.completed || isRevealed,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(revealMotionDuration)) +
                slideInVertically(animationSpec = androidx.compose.animation.core.tween(revealMotionDuration)) { fullHeight -> fullHeight / 10 },
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(revealMotionDuration)) +
                shrinkVertically(animationSpec = androidx.compose.animation.core.tween(revealMotionDuration))
    ) {
        Column(
            Modifier.fillMaxWidth(),
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
            if (!typingSuccessPending) {
                Box(modifier = Modifier.graphicsLayer { scaleX = badgeScale; scaleY = badgeScale }) {
                    LearningEngineStatusBadge(
                        label = badgeText,
                        tone = tone,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }

            val clozePresentation = (state as? AndroidStudyState.ExampleCompletion)?.let {
                resolveClozePresentation(it.prefix, it.blank, it.suffix, it.example, it.translation)
            }
            val answerExample = when {
                state is AndroidStudyState.Typing -> null
                clozePresentation != null -> clozePresentation.supportingExample
                else -> state.example
            }
            val answerExampleTranslation = if (state is AndroidStudyState.Typing) null else if (clozePresentation != null) {
                clozePresentation.supportingExampleTranslation
            } else state.translation
            StudyAnswerSection(
                englishAnswer = plan.answerContract.canonicalAnswer,
                pronunciation = if (state is AndroidStudyState.Typing) null else state.pronunciation,
                partOfSpeech = (state as? AndroidStudyState.Typing)?.partOfSpeech?.let(::partOfSpeechPresentation),
                vietnameseAnswer = state.meaning,
                englishExample = answerExample,
                vietnameseExample = answerExampleTranslation,
                answerAudioPath = state.resolvedExpectedAnswerAudio,
                vietnameseAudioPath = state.resolvedMeaningAudio,
                englishExampleAudioPath = state.resolvedExampleEnglishAudio,
                vietnameseExampleAudioPath = state.resolvedExampleVietnameseAudio,
                isPlayingAnswer = activeRole == AudioRole.EXPECTED_ANSWER,
                isPlayingVietnamese = activeRole == AudioRole.MEANING,
                isPlayingEnglishExample = activeRole == AudioRole.EXAMPLE_ENGLISH,
                isPlayingVietnameseExample = activeRole == AudioRole.EXAMPLE_VIETNAMESE,
                onAnswerAudio = { playAudio(AudioRole.EXPECTED_ANSWER, state.resolvedExpectedAnswerAudio, true) },
                onVietnameseAudio = { playAudio(AudioRole.MEANING, state.resolvedMeaningAudio, false) },
                onEnglishExampleAudio = { playAudio(AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio, true) },
                onVietnameseExampleAudio = { playAudio(AudioRole.EXAMPLE_VIETNAMESE, state.resolvedExampleVietnameseAudio, false) },
                answerHero = state is AndroidStudyState.Typing
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Response Actions: Continue, Undo, Rating override
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                if (typingSuccessPending) {
                    val typing = state as AndroidStudyState.Typing
                    Text(
                        if (typing.manualRating == null) "Automatic rating pending" else "Manual rating selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
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
            detail = state.dailyBudget?.let { daily ->
                when {
                    daily.targetsComplete -> "Today's NEW and REVIEW targets are complete."
                    daily.newRemainingToday == 0 -> "This session is complete. Today's NEW target is reached; REVIEW may remain."
                    daily.reviewRemainingToday == 0 -> "This session is complete. Today's REVIEW target is reached; NEW may remain."
                    daily.hasEligibleWork -> "This session is complete. More daily Study work is available."
                    else -> "This session is complete. No eligible content is currently available."
                }
            } ?: "Great work! You have completed all items in this study session.",
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
