package vn.loi.learning.android.study

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
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
import vn.loi.learning.android.study.components.StudyRatingFeedbackOverlay
import vn.loi.learning.android.study.components.IntroductionAnswerSection
import vn.loi.learning.android.study.components.StudyAudioTextTarget

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
    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var outgoingFeedback by remember { mutableStateOf<OutgoingStudyFeedback?>(null) }
    var feedbackVisible by remember { mutableStateOf(false) }
    val reducedMotion = isReducedMotionEnabled()
    val context = LocalContext.current
    val feedbackAudioController = remember(context) { AndroidAudioController(context) }

    DisposableEffect(feedbackAudioController) {
        onDispose { feedbackAudioController.close() }
    }

    LaunchedEffect(outgoingFeedback?.feedbackId) {
        val feedback = outgoingFeedback ?: return@LaunchedEffect
        feedbackVisible = true
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
        feedbackVisible = false
        delay(if (reducedMotion) 0 else StudyRatingFeedbackPolicy.exitMillis.toLong())
        if (outgoingFeedback?.feedbackId == feedback.feedbackId) outgoingFeedback = null
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
                        (fadeIn(tween(if (reducedMotion) 0 else 150)) +
                            slideInVertically(tween(if (reducedMotion) 0 else 150)) { it / 14 }) togetherWith
                            (fadeOut(tween(if (reducedMotion) 0 else 130)) +
                                slideOutVertically(tween(if (reducedMotion) 0 else 130)) { -it / 14 })
                    } else if (reducedMotion) {
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
                        is AndroidStudyState.PreparingMode -> PreparingStudyMode(target)
                        is AndroidStudyState.Home -> HomeScreen(target, onEvent = onEvent)
                        is AndroidStudyState.Completion -> Completion(target, onEvent)
                        is AndroidStudyState.Failed -> StudyFailureState(target, onEvent)
                        is AndroidStudyState.Runtime -> {
                            StudyRuntimeScreen(
                                state = target,
                                onEvent = onEvent,
                                introductionAutoplayEnabled = outgoingFeedback == null,
                                onIntroductionRatingWithFeedback = { introduction, rating, focus, resumableFocus ->
                                    outgoingFeedback = outgoingStudyFeedback(introduction, rating, focus, resumableFocus)
                                    onEvent(AndroidStudyEvent.RateIntroduction(rating))
                                },
                                onOpenFullscreenImage = { fullscreenImageUri = it }
                            )
                        }
                    }
                }

                outgoingFeedback?.let { feedback ->
                    StudyRatingFeedbackOverlay(feedback, feedbackVisible, reducedMotion)
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
    onGestureEnd: (IntroductionStageGesture) -> Unit,
    onTap: () -> Unit,
    onSwipeGood: () -> Unit
) = pointerInput(itemKey, alreadySubmitted, ratingEnabled) {
    val swipeThresholdPx = 72.dp.toPx()
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
            if (ratingEnabled && deltaY < -tapSlopPx && kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) * 1.35f) {
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
            childConsumed = childConsumed && !ownsUpwardDrag,
            alreadySubmitted = alreadySubmitted,
            ratingEnabled = ratingEnabled
        )
        when (gesture) {
            IntroductionStageGesture.TAP -> {
                onGestureEnd(gesture)
                onTap()
            }
            IntroductionStageGesture.SWIPE_GOOD -> {
                onSwipeGood()
                onGestureEnd(gesture)
            }
            IntroductionStageGesture.NONE -> onGestureEnd(gesture)
        }
    }
}

@Composable
private fun StudyRuntimeScreen(
    state: AndroidStudyState.Runtime,
    onEvent: (AndroidStudyEvent) -> Unit,
    introductionAutoplayEnabled: Boolean,
    onIntroductionRatingWithFeedback: (
        AndroidStudyState.Introduction,
        ReviewRating,
        IntroductionPlaybackFocus,
        IntroductionPlaybackFocus?
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
    var resumableLoopFocus by remember(itemKey) { mutableStateOf<IntroductionPlaybackFocus?>(null) }
    var introductionImageExpanded by remember(itemKey) { mutableStateOf(false) }
    var swipeRatingSubmitted by remember(itemKey) { mutableStateOf(false) }
    var revealAudioStarted by remember(itemKey) { mutableStateOf(false) }

    val resumeLoopAfterTemporary: (AudioRole) -> Unit = { completedRole ->
        val introduction = state as? AndroidStudyState.Introduction
        val wordPath = introduction?.resolvedExpectedAnswerAudio ?: introduction?.resolvedPromptAudio
        val examplePath = introduction?.resolvedExampleEnglishAudio
        val loopRole = loopRoleAfterTemporaryAudio(
            completedRole = completedRole,
            focus = resumableLoopFocus,
            revealed = introduction?.revealed == true,
            hasWordAudio = !wordPath.isNullOrBlank(),
            hasExampleAudio = !examplePath.isNullOrBlank()
        )
        if (activeRole != completedRole) Unit
        else if (loopRole == null) activeRole = null
        else {
            val loopPath = if (loopRole == AudioRole.EXPECTED_ANSWER) wordPath else examplePath
            activeRole = loopRole
            resumableLoopFocus = if (loopRole == AudioRole.EXPECTED_ANSWER) IntroductionPlaybackFocus.WORD
                else IntroductionPlaybackFocus.EXAMPLE
            audioController.replay(loopPath, isLooping = true) { loopState ->
                if ((loopState is AndroidAudioState.Idle || loopState is AndroidAudioState.Failed) &&
                    activeRole == loopRole
                ) activeRole = null
            }
        }
    }

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
                if (role == AudioRole.EXPECTED_ANSWER || role == AudioRole.EXAMPLE_ENGLISH) resumableLoopFocus = null
            } else {
                audioController.stop()
                activeRole = role
                when (role) {
                    AudioRole.EXPECTED_ANSWER -> {
                        introductionPlaybackFocus = IntroductionPlaybackFocus.WORD
                        resumableLoopFocus = IntroductionPlaybackFocus.WORD
                    }
                    AudioRole.EXAMPLE_ENGLISH -> {
                        introductionPlaybackFocus = IntroductionPlaybackFocus.EXAMPLE
                        resumableLoopFocus = IntroductionPlaybackFocus.EXAMPLE
                    }
                    else -> Unit
                }
                audioController.replay(path, isLooping = isLooping) { state ->
                    if (state is AndroidAudioState.Idle || state is AndroidAudioState.Failed) {
                        resumeLoopAfterTemporary(role)
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
                    resumableLoopFocus = IntroductionPlaybackFocus.WORD
                }
                AudioRole.EXAMPLE_ENGLISH -> {
                    introductionPlaybackFocus = IntroductionPlaybackFocus.EXAMPLE
                    resumableLoopFocus = IntroductionPlaybackFocus.EXAMPLE
                }
                else -> Unit
            }
            audioController.replay(path, isLooping = isLooping) { playbackState ->
                if (playbackState is AndroidAudioState.Idle || playbackState is AndroidAudioState.Failed) {
                    resumeLoopAfterTemporary(role)
                }
            }
        }
    }

    val stopAudioAndDispatch: (AndroidStudyEvent) -> Unit = { event ->
        audioController.stop()
        activeRole = null
        resumableLoopFocus = null
        onEvent(event)
    }
    val submitIntroductionRating: (ReviewRating) -> Unit = { rating ->
        if (state is AndroidStudyState.Introduction && !swipeRatingSubmitted) {
            swipeRatingSubmitted = true
            onIntroductionRatingWithFeedback(state, rating, introductionPlaybackFocus, resumableLoopFocus)
            audioController.stop()
            activeRole = null
            resumableLoopFocus = null
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

    val reviewImageHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = when {
            isEnded -> 110.dp
            state is AndroidStudyState.ImageRecall -> 220.dp
            else -> 128.dp
        },
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
                currentPosition = (state as? AndroidStudyState.Introduction)?.packagePosition ?: state.currentPosition,
                totalItems = (state as? AndroidStudyState.Introduction)?.packageTotal ?: state.totalItems,
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
                Modifier.fillMaxSize()
                    .padding(horizontal = LearningSpacing.screen, vertical = LearningSpacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)
            ) {
                state.hud?.let { hud ->
                    if (state is AndroidStudyState.Introduction) {
                        LearnNewProgressHeader(state, hud)
                    } else {
                        LearningEngineCompactHud(hud)
                    }
                }

                LearningEngineLearningStage(
                    modifier = if (state is AndroidStudyState.Introduction) Modifier.fillMaxWidth().weight(1f)
                        else Modifier.fillMaxWidth().verticalScroll(scrollState),
                    state = state,
                    activeRole = activeRole,
                    playAudio = playAudio,
                    restartAudio = restartAudio,
                    imageHeight = reviewImageHeight,
                    revealBringIntoViewRequester = bringIntoViewRequester,
                    introductionImageExpanded = introductionImageExpanded,
                    swipeRatingSubmitted = swipeRatingSubmitted,
                    onIntroductionImageExpandedChange = { introductionImageExpanded = it },
                    onIntroductionStageTap = {
                        if (state is AndroidStudyState.Introduction) {
                            if (!state.revealed) {
                                stopAudioAndDispatch(AndroidStudyEvent.RevealIntroduction)
                            } else {
                                nextIntroductionPlaybackFocus(
                                    introductionPlaybackFocus,
                                    hasWordAudio = !state.resolvedExpectedAnswerAudio.isNullOrBlank() ||
                                        !state.resolvedPromptAudio.isNullOrBlank(),
                                    hasExampleAudio = !state.resolvedExampleEnglishAudio.isNullOrBlank()
                                )?.let { focus ->
                                    introductionPlaybackFocus = focus
                                    if (focus == IntroductionPlaybackFocus.WORD) {
                                        restartAudio(
                                            AudioRole.EXPECTED_ANSWER,
                                            state.resolvedExpectedAnswerAudio ?: state.resolvedPromptAudio,
                                            true
                                        )
                                    } else {
                                        restartAudio(AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio, true)
                                    }
                                }
                            }
                        }
                    },
                    onIntroductionSwipeGood = {
                        submitIntroductionRating(ReviewRating.GOOD)
                    },
                    onIntroductionRating = submitIntroductionRating,
                    onEvent = stopAudioAndDispatch,
                    onOpenFullscreenImage = onOpenFullscreenImage
                )
            }
        }
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
    hud: AndroidStudySessionHud
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
                CompactLearnMetric("Again", hud.againCount.toString(), MaterialTheme.colorScheme.error)
                CompactLearnMetric("Hard", hud.hardCount.toString(), LearningEngineThemeTokens.semanticColors.warning)
                CompactLearnMetric("Good", hud.goodCount.toString(), LearningEngineThemeTokens.semanticColors.success)
                CompactLearnMetric("Easy", hud.easyCount.toString(), MaterialTheme.colorScheme.tertiary)
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
private fun CompactLearnMetric(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
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
    imageHeight: androidx.compose.ui.unit.Dp,
    revealBringIntoViewRequester: BringIntoViewRequester,
    introductionImageExpanded: Boolean,
    swipeRatingSubmitted: Boolean,
    onIntroductionImageExpandedChange: (Boolean) -> Unit,
    onIntroductionStageTap: () -> Unit,
    onIntroductionSwipeGood: () -> Unit,
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
            onImageExpandedChange = onIntroductionImageExpandedChange,
            onGenericStageTap = onIntroductionStageTap,
            onSwipeGood = onIntroductionSwipeGood,
            onRating = onIntroductionRating,
            onEvent = onEvent,
            onOpenFullscreenImage = onOpenFullscreenImage
        )
        return
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
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

            Box(modifier = Modifier.bringIntoViewRequester(revealBringIntoViewRequester)) {
                StudyRevealAndFeedbackContent(
                    state = state,
                    activeRole = activeRole,
                    playAudio = playAudio,
                    onEvent = onEvent
                )
            }
        }
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
    onImageExpandedChange: (Boolean) -> Unit,
    onGenericStageTap: () -> Unit,
    onSwipeGood: () -> Unit,
    onRating: (ReviewRating) -> Unit,
    onEvent: (AndroidStudyEvent) -> Unit,
    onOpenFullscreenImage: (String) -> Unit
) {
    val isPlayingExpected = activeRole == AudioRole.EXPECTED_ANSWER
    val isPlayingMeaning = activeRole == AudioRole.MEANING
    val isPlayingExampleEng = activeRole == AudioRole.EXAMPLE_ENGLISH
    val isPlayingExampleVie = activeRole == AudioRole.EXAMPLE_VIETNAMESE
    val reducedMotion = isReducedMotionEnabled()
    var swipeOffsetTarget by remember(state.learningItemId) { mutableFloatStateOf(0f) }
    var swipeCommitPending by remember(state.learningItemId) { mutableStateOf(false) }
    val swipeOffset by animateFloatAsState(
        targetValue = swipeOffsetTarget,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 120),
        label = "Learn new swipe position"
    )
    val frontMotion = rememberInfiniteTransition(label = "learn new front motion")
    val frontPulseScale by frontMotion.animateFloat(
        initialValue = 1f,
        targetValue = 1.012f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "learn new image breathing"
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val bounds = resolveIntroductionImageBounds(maxHeight.value.toInt())
        val swipeExitOffsetPx = -constraints.maxHeight * 0.06f
        val introductionScrollState = rememberLazyListState()
        val targetMaxHeightDp = when {
            !state.revealed || imageExpanded -> bounds.frontMaxHeightDp
            else -> bounds.revealMaxHeightDp
        }
        val imageMaxHeight by animateDpAsState(
            targetValue = targetMaxHeightDp.dp,
            animationSpec = tween(durationMillis = if (reducedMotion) 0 else 200),
            label = "Introduction hero transformation"
        )
        Surface(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                translationY = swipeOffset
                alpha = (1f - (-swipeOffset / size.height.coerceAtLeast(1f)) * 0.38f).coerceIn(0.62f, 1f)
            },
            shape = LearningEngineShapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.fillMaxSize()) {
                LazyColumn(
                state = introductionScrollState,
                modifier = Modifier.fillMaxWidth().weight(1f).introductionStageGestures(
                itemKey = state.learningItemId,
                alreadySubmitted = swipeRatingSubmitted || swipeCommitPending,
                ratingEnabled = state.revealed,
                onDragOffset = { swipeOffsetTarget = it },
                onGestureEnd = { gesture ->
                    swipeOffsetTarget = if (gesture == IntroductionStageGesture.SWIPE_GOOD) {
                        swipeExitOffsetPx
                    } else 0f
                },
                onTap = onGenericStageTap,
                onSwipeGood = {
                    if (!swipeCommitPending && !swipeRatingSubmitted) {
                        swipeCommitPending = true
                        onSwipeGood()
                    }
                }
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
                if (!state.revealed) StudyAudioTextTarget(
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
                if (!state.revealed) partOfSpeechPresentation(state.partOfSpeech)?.let { pos ->
                    PartOfSpeechBadge(pos)
                }

                state.resolvedImage?.let { imageUri ->
                    LearningEngineImage(
                        imagePath = imageUri,
                        imageUnavailable = false,
                        onOpenFullscreen = {
                            if (state.revealed) {
                                onImageExpandedChange(!imageExpanded)
                                restartAudio(
                                    AudioRole.EXPECTED_ANSWER,
                                    state.resolvedExpectedAnswerAudio ?: state.resolvedPromptAudio,
                                    true
                                )
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
                            val scale = if (!state.revealed && !reducedMotion) frontPulseScale else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                    )
                } ?: Box(
                    Modifier.fillMaxWidth().heightIn(min = 120.dp, max = imageMaxHeight)
                        .semantics { contentDescription = "Learning canvas, tap to discover the English word" }
                )

                AnimatedVisibility(
                    visible = !state.revealed,
                    enter = fadeIn(tween(if (reducedMotion) 0 else 180, delayMillis = if (reducedMotion) 0 else 80)),
                    exit = fadeOut(tween(if (reducedMotion) 0 else 100))
                ) {
                    IntroductionInteractionHint(
                        primary = "Tap to reveal",
                        secondary = "Recall the English word",
                        emphasized = true
                    )
                }

                AnimatedVisibility(
                    visible = state.revealed,
                    enter = fadeIn(tween(if (reducedMotion) 0 else 160)) +
                        slideInVertically(tween(if (reducedMotion) 0 else 160)) { it / 14 },
                    exit = fadeOut(tween(if (reducedMotion) 0 else 100))
                ) {
                    IntroductionAnswerSection(
                        englishAnswer = state.answer,
                        pronunciation = normalizedIntroductionPronunciation(state.partOfSpeech, state.pronunciation),
                        partOfSpeech = partOfSpeechPresentation(state.partOfSpeech),
                        vietnameseAnswer = meaning,
                        englishExample = state.example,
                        vietnameseExample = state.translation,
                        answerAudioPath = state.resolvedExpectedAnswerAudio ?: state.resolvedPromptAudio,
                        vietnameseAudioPath = state.resolvedMeaningAudio,
                        englishExampleAudioPath = state.resolvedExampleEnglishAudio,
                        vietnameseExampleAudioPath = state.resolvedExampleVietnameseAudio,
                        isPlayingAnswer = isPlayingExpected,
                        isPlayingVietnamese = isPlayingMeaning,
                        isPlayingEnglishExample = isPlayingExampleEng,
                        isPlayingVietnameseExample = isPlayingExampleVie,
                        onAnswerAudio = {
                            playAudio(
                                AudioRole.EXPECTED_ANSWER,
                                state.resolvedExpectedAnswerAudio ?: state.resolvedPromptAudio,
                                true
                            )
                        },
                        onVietnameseAudio = { playAudio(AudioRole.MEANING, state.resolvedMeaningAudio, false) },
                        onEnglishExampleAudio = {
                            playAudio(AudioRole.EXAMPLE_ENGLISH, state.resolvedExampleEnglishAudio, true)
                        },
                        onVietnameseExampleAudio = {
                            playAudio(AudioRole.EXAMPLE_VIETNAMESE, state.resolvedExampleVietnameseAudio, false)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = StudyContentSpacing.imageToAnswer)
                    )
                }

                    }
                }
                }
                StudyRatingBar(
                    onRating = onRating,
                    modifier = Modifier.fillMaxWidth().padding(
                        start = LearningSpacing.medium,
                        end = LearningSpacing.medium,
                        top = StudyContentSpacing.examplesToRating,
                        bottom = StudyContentSpacing.ratingToActions
                    )
                )
                if (state.revealed) {
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
                    LearningEngineChoiceTile(
                        label = "${index + 1}. ${choice.text}",
                        isSelected = isSelected,
                        enabled = !state.completed,
                        stateDescriptionText = accessibilityStrings().option(index + 1, state.choices.size, isSelected),
                        onClick = { onEvent(AndroidStudyEvent.Choose(choice.id)) }
                    )
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
private fun LearningEngineChoiceTile(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    stateDescriptionText: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = LearningEngineShapes.large,
        colors = if (isSelected) ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) else ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).semantics {
            selected = isSelected
            stateDescription = stateDescriptionText
        }
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
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
                Box(modifier = Modifier.graphicsLayer { scaleX = badgeScale; scaleY = badgeScale }) {
                    LearningEngineStatusBadge(
                        label = badgeText,
                        tone = tone,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
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
