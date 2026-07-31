package vn.loi.learning.desktop.ui.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.focusable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import vn.loi.learning.application.decision.DecisionExplanation
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningflow.LearningFlowStage
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*
import vn.loi.learning.desktop.ui.designsystem.components.base.LEButton
import vn.loi.learning.desktop.ui.designsystem.components.base.LEButtonVariant
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurface
import vn.loi.learning.desktop.ui.designsystem.components.base.LESurfaceVariant
import vn.loi.learning.desktop.ui.theme.LETheme
import vn.loi.learning.desktop.runtime.StudyTypographyPreferences
import vn.loi.learning.desktop.runtime.StudyPresentationPreferences
import vn.loi.learning.desktop.runtime.StudyPresentationControlMode
import vn.loi.learning.desktop.shortcut.ShortcutRegistry
import vn.loi.learning.desktop.shortcut.StudyShortcutCommand
import vn.loi.learning.desktop.shortcut.toDesktopKeyChord
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import vn.loi.learning.domain.study.memory.model.ReviewRating

@Composable
fun StudyScreen(
    uiState: StudyUiState,
    contentPresenter: LearningContentPresenter,
    contentStrings: LearningContentRendererStrings,
    workspaceStrings: StudyWorkspaceStrings,
    onRefresh: () -> Unit,
    onRefreshHeaderStatistics: () -> Unit = {},
    onStartStudy: () -> Unit,
    onReplayLatestCompletedStudySession: () -> Unit = {},
    onStartLearnedItemsReview: () -> Unit = {},
    onRevealAnswer: () -> Unit,
    onCompleteFlowStage: () -> Unit = onRevealAnswer,
    onShowDecisionExplanation: () -> Unit,
    onHideDecisionExplanation: () -> Unit,
    onCompleteAdaptiveSession: () -> Unit,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onTypingCorrectCompleted: (TypingRecallSuccessRequest) -> Unit = {},
    onTypingReveal: (TypingRecallRevealRequest) -> Unit = {},
    onTypingForcedAgain: (TypingRecallRevealRequest) -> Unit = {},
    onEasy: () -> Unit,
    onUndo: () -> Unit,
    onPause: () -> Unit,
    onBackToLesson: ((vn.loi.learning.domain.library.model.InstalledPackageId, vn.loi.learning.domain.content.model.ContentId) -> Unit)? = null,
    onBackToLibrary: (() -> Unit)? = null,
    onContinueLearning: ((vn.loi.learning.domain.library.model.InstalledPackageId, vn.loi.learning.domain.content.model.ContentId) -> Unit)? = null,
    audioLoopDelaySeconds: Double = 0.35,
    typographyPreferences: StudyTypographyPreferences = StudyTypographyPreferences(),
    shortcutRegistry: ShortcutRegistry = ShortcutRegistry.defaults(),
    presentationPreferences: StudyPresentationPreferences = StudyPresentationPreferences(),
    presentationState: StudyPresentationStagingState =
        StudyPresentationStagingState(uiState.currentLearningItemId, presentationPreferences),
    onPresentationPreferencesChanged: (StudyPresentationPreferences) -> Unit = {},
    onOpenPresentationSettings: () -> Unit = {},
    typingAttemptTimeSource: TypingAttemptTimeSource = TypingAttemptTimeSource.MONOTONIC,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val accessibilityPresentation = resolveStudyAccessibilityPresentation(uiState)
    val workspacePresentation =
        resolveFocusedStudyWorkspace(uiState.hasActiveSession && !uiState.learnEntryChooserVisible)
    val focusTransitionKey = resolveStudyFocusTransitionKey(uiState)
    val contentPresentation = remember(uiState.learningContent, contentPresenter) {
        contentPresenter.presentAvailable(uiState.learningContent)
    }
    val experiencePlan = uiState.learningExperiencePlan
    var typingState by remember(uiState.currentLearningItemId) {
        mutableStateOf(TypingRecallInteraction.initial(uiState.currentLearningItemId))
    }
    var typingInputFocused by remember(uiState.currentLearningItemId) {
        mutableStateOf(false)
    }
    var typingElapsedMillis by remember(uiState.currentLearningItemId) {
        mutableStateOf(0L)
    }
    val examplesDisclosureKeyboard =
        remember(uiState.currentLearningItemId) {
            ExamplesDisclosureKeyboardController()
        }
    val experienceSelection = uiState.learningFlowSelection
    val sceneProjector = remember { DesktopLearningSceneProjector() }
    val learningScene = remember(experiencePlan, experienceSelection, contentPresentation) {
        sceneProjector.project(experiencePlan, experienceSelection, contentPresentation)
    }
    val focusedAnswerModel = remember(uiState, learningScene, contentPresentation) {
        FocusedVocabularyAnswerResolver.resolve(uiState, learningScene, contentPresentation)
    }
    val audioAvailability = remember(focusedAnswerModel) {
        focusedAnswerModel.presentationAvailability()
    }
    val shortcutAudioPaths = remember(audioAvailability) {
        StudyShortcutAudioPaths(
            vocabulary = audioAvailability.primaryEnglishAudio,
            englishExample = audioAvailability.englishExampleAudio,
            vietnameseMeaning = audioAvailability.vietnameseMeaningAudio,
            vietnameseExample = audioAvailability.vietnameseExampleAudio
        )
    }
    val audioController = remember {
        LearningContentAudioController(JavaSoundLearningContentAudioPlayer())
    }
    val typingSuccessInProgress =
        typingState.successInProgress
    val typingCanonicalAnswer =
        (learningScene as? TypingScene)?.prompt?.expectedAnswer
    val typingSuccessDecision =
        typingState.attempt
            ?.takeIf { it.phase == TypingAttemptPhase.COMPLETED_EXACTLY }
            ?.let { attempt ->
                runCatching {
                    val metrics = attempt.snapshot(revealUsed = false)
                    metrics to TypingAutomaticRatingResolver.decide(metrics)
                }.getOrNull()
            }
    val latestOnTypingCorrectCompleted by rememberUpdatedState(onTypingCorrectCompleted)
    val latestOnTypingReveal by rememberUpdatedState(onTypingReveal)
    val nextDueAt = when (val statistics = uiState.headerStatistics) {
        is StudyHeaderStatisticsState.Available -> statistics.value.nearestFutureDueAt
        is StudyHeaderStatisticsState.Unavailable -> statistics.lastKnownGood?.nearestFutureDueAt
        StudyHeaderStatisticsState.Loading -> null
    }
    LaunchedEffect(nextDueAt?.epochMillis) {
        nextDueAt?.let {
            delay((it.epochMillis - System.currentTimeMillis()).coerceAtLeast(1L))
            onRefreshHeaderStatistics()
        }
    }
    LaunchedEffect(audioLoopDelaySeconds) {
        audioController.loopDelaySeconds = audioLoopDelaySeconds
    }

    LaunchedEffect(
        uiState.experienceRotationContext,
        learningScene,
        uiState.canRevealAnswer
    ) {
        val context = uiState.experienceRotationContext
        val typingScene = learningScene as? TypingScene
        val reviewContext = uiState.currentItemReviewContext
        if (context != null && typingScene != null && uiState.canRevealAnswer && reviewContext != null) {
            typingState =
                TypingRecallInteraction.beginAttempt(
                    state = typingState,
                    context = context,
                    prompt = typingScene.prompt,
                    itemOrigin = reviewContext.origin,
                    learningStage = uiState.learningStage,
                    previousRating = reviewContext.previousRating,
                    previousReviewAtMillis = reviewContext.previousReviewAtMillis,
                    reviewedEarlierInCurrentSession =
                        reviewContext.reviewedEarlierInCurrentSession,
                    memoryContextReliable = reviewContext.memoryContextReliable,
                    itemPresentedAtEpochMillis = reviewContext.itemPresentedAtEpochMillis,
                    easyConfidenceProjection = reviewContext.easyConfidenceProjection,
                    nowMillis = typingAttemptTimeSource.nowMillis()
                )
        }
    }

    LaunchedEffect(typingState.attempt) {
        val attempt = typingState.attempt
        if (attempt == null) {
            typingElapsedMillis = 0L
            return@LaunchedEffect
        }
        while (attempt.active) {
            val elapsed = attempt.elapsedMillis(typingAttemptTimeSource.nowMillis())
            typingElapsedMillis = elapsed
            delay((1_000L - elapsed % 1_000L).coerceAtLeast(1L))
        }
        typingElapsedMillis = attempt.elapsedMillis(typingAttemptTimeSource.nowMillis())
    }

    LaunchedEffect(
        uiState.currentLearningItemId,
        learningScene,
        uiState.sessionCompleted,
        uiState.contentIntroductionState
    ) {
        synchronizeStudyAudio(audioController, learningScene, uiState.sessionCompleted)
        if (uiState.contentIntroductionState == ContentIntroductionState.REQUIRED) {
            focusedAnswerModel.meaningAudioPath?.let(audioController::playOnce)
        }
    }
    DisposableEffect(Unit) {
        onDispose(audioController::close)
    }

    LaunchedEffect(
        uiState.currentLearningItemId,
        typingState.inputRevision,
        typingState.automaticSuccessRequested
    ) {
        if (!typingState.automaticSuccessRequested || typingState.successInProgress) {
            return@LaunchedEffect
        }
        val expectedRevision = typingState.inputRevision
        awaitTypingRealtimeSuccessDebounce()
        typingState =
            TypingRecallInteraction.confirmRealtimeSuccess(typingState, expectedRevision)
    }

    LaunchedEffect(
        uiState.currentLearningItemId,
        typingState.successInProgress,
        focusedAnswerModel.primaryAudioPath
    ) {
        if (!typingSuccessInProgress) return@LaunchedEffect
        withFrameNanos { }
        val answerAudio = focusedAnswerModel.primaryAudioPath
        if (answerAudio == null) {
            delay(TYPING_SUCCESS_WITHOUT_AUDIO_DWELL_MILLIS)
        } else {
            awaitTypingAnswerAudio(audioController, answerAudio)
            delay(TYPING_SUCCESS_AFTER_AUDIO_DWELL_MILLIS)
        }
        val context = uiState.experienceRotationContext ?: return@LaunchedEffect
        val metrics = typingState.attempt?.snapshot(revealUsed = false)
            ?: return@LaunchedEffect
        val decision = TypingAutomaticRatingResolver.decide(metrics)
        val request =
            TypingRecallSuccessRequest(
                context = context,
                inputRevision = typingState.inputRevision,
                metrics = metrics,
                decision = decision
            )
        typingState = TypingRecallInteraction.cancelAutomaticSuccess(typingState)
        latestOnTypingCorrectCompleted(request)
    }

    LaunchedEffect(
        focusTransitionKey,
        learningScene,
        uiState.canRevealAnswer,
        typingSuccessInProgress
    ) {
        if (
            learningScene !is TypingScene ||
            !uiState.canRevealAnswer ||
            typingSuccessInProgress
        ) {
            focusRequester.requestFocus()
        }
    }

    fun cancelTypingSuccess() {
        if (typingState.automaticSuccessRequested || typingState.successInProgress) {
            audioController.stop()
            typingState = TypingRecallInteraction.cancelAutomaticSuccess(typingState)
        }
    }

    fun requestFlowStageCompletion() {
        val typingScene = learningScene as? TypingScene
        if (typingScene != null && uiState.canRevealAnswer) {
            if (typingState.attempt?.phase == TypingAttemptPhase.COMPLETED_EXACTLY) return
            cancelTypingSuccess()
            val revealedState =
                TypingRecallInteraction.evaluateForReveal(
                    typingState,
                    typingScene.prompt,
                    TypingAnswerEvaluator(),
                    typingAttemptTimeSource.nowMillis()
                )
            val attempt = revealedState.attempt ?: return
            val context = uiState.experienceRotationContext ?: return
            val request =
                TypingRecallRevealRequest(
                    context = context,
                    attemptGeneration = attempt.attemptGeneration,
                    metrics = attempt.snapshot(revealUsed = true)
                )
            typingState = revealedState
            latestOnTypingReveal(request)
            return
        }
        onCompleteFlowStage()
    }

    fun requestUndo() {
        cancelTypingSuccess()
        typingState =
            TypingRecallInteraction.cancelAttempt(
                typingState,
                typingAttemptTimeSource.nowMillis()
            )
        onUndo()
    }

    fun requestPause() {
        cancelTypingSuccess()
        typingState =
            TypingRecallInteraction.cancelAttempt(
                typingState,
                typingAttemptTimeSource.nowMillis()
            )
        audioController.stop()
        onPause()
    }

    fun performKeyboardAction(action: StudyKeyboardAction) {
        if (
            typingSuccessInProgress &&
            action !in
                setOf(
                    StudyKeyboardAction.UNDO_LATEST,
                    StudyKeyboardAction.PAUSE_WORKSPACE
                )
        ) {
            return
        }
        when (action) {
            StudyKeyboardAction.RETRY_LOAD -> onRefresh()
            StudyKeyboardAction.START_STUDY -> onStartStudy()
            StudyKeyboardAction.REVEAL_ANSWER -> requestFlowStageCompletion()
            StudyKeyboardAction.REVIEW_AGAIN -> onAgain()
            StudyKeyboardAction.REVIEW_HARD -> onHard()
            StudyKeyboardAction.REVIEW_GOOD -> onGood()
            StudyKeyboardAction.REVIEW_EASY -> onEasy()
            StudyKeyboardAction.COMPLETE_FORCED_AGAIN ->
                uiState.forcedTypingRevealRequest?.let(onTypingForcedAgain)
            StudyKeyboardAction.RETRY_AUTOMATIC_TYPING ->
                uiState.pendingTypingSuccessRequest?.let(onTypingCorrectCompleted)
            StudyKeyboardAction.REPLAY_PRIMARY_AUDIO,
            StudyKeyboardAction.TOGGLE_VOCABULARY_AUDIO_LOOP,
            StudyKeyboardAction.TOGGLE_EXAMPLE_AUDIO_LOOP,
            StudyKeyboardAction.PLAY_VIETNAMESE_MEANING_AUDIO,
            StudyKeyboardAction.PLAY_VIETNAMESE_EXAMPLE_AUDIO ->
                performStudyAudioKeyboardAction(action, shortcutAudioPaths, audioController)
            StudyKeyboardAction.UNDO_LATEST -> requestUndo()
            StudyKeyboardAction.PAUSE_WORKSPACE -> {
                requestPause()
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LETheme.colors.windowBackground)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false

                val chord = event.toDesktopKeyChord() ?: return@onKeyEvent false
                val disclosureCommand =
                    resolveExamplesDisclosureKeyboardCommand(
                        chord = chord,
                        textInputFocused = typingInputFocused
                    )
                if (
                    disclosureCommand != null &&
                    examplesDisclosureKeyboard.dispatch(disclosureCommand)
                ) {
                    return@onKeyEvent true
                }
                val action =
                    resolveStudyKeyboardAction(
                        uiState,
                        StudyKeyboardInput(
                            chord = chord,
                            textInputFocused = typingInputFocused
                        ),
                        shortcutRegistry
                    )

                if (action == null) {
                    false
                } else {
                    performKeyboardAction(action)
                    true
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        val density = LocalDensity.current
        val displayEnvironment = StudyDisplayEnvironment(
            widthDp = maxWidth.value.toInt().coerceAtLeast(1),
            heightDp = maxHeight.value.toInt().coerceAtLeast(1),
            density = density.density,
            fontScale = density.fontScale
        )
        val visualTraits = remember(uiState, learningScene, contentPresentation) {
            val disclosure = FullAnswerPresentation.resolve(focusedAnswerModel)
            StudyVisualContentTraits(
                hasImage = disclosure.imageAvailable && focusedAnswerModel.imagePath != null,
                hasPronunciation = !disclosure.ipa.isNullOrBlank() || focusedAnswerModel.primaryAudioPath != null,
                hasPartOfSpeech = !disclosure.partOfSpeech.isNullOrBlank(),
                hasExamples = disclosure.examples.isNotEmpty(),
                hasSchedulerFeedback = uiState.schedulerFeedback != null
            )
        }
        val visualLayout = remember(displayEnvironment, visualTraits) {
            StudyVisualLayoutResolver.resolve(
                environment = displayEnvironment,
                traits = visualTraits
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = workspacePresentation.maxContentWidthDp.dp)
                .fillMaxSize()
        ) {
            // 1. SessionHeader
            SessionHeader(
                uiState = uiState,
                accessibilityPresentation = accessibilityPresentation,
                workspaceStrings = workspaceStrings,
                presentationState = presentationState,
                onPresentationPreferencesChanged = onPresentationPreferencesChanged,
                onOpenPresentationSettings = onOpenPresentationSettings,
                onUndo = ::requestUndo,
                onPause = ::requestPause,
                visualLayout = visualLayout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LESpacing.lg, vertical = LESpacing.sm)
            )

            // Scrollable Main Body (LearningWorkspaceSurface + SecondaryWorkspace)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val fullAnswerAvailableBodyHeightDp =
                    (maxHeight - LESpacing.sm * 2 - LETheme.spacing.space5 * 2)
                        .value.toInt()
                        .coerceAtLeast(1)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = LESpacing.lg, vertical = LESpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(LESpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                // 2. LearningWorkspaceSurface (Main Content Card / Active Learning Scene)
                if (!uiState.sessionCompleted && uiState.loadError == null && resolveStudyIdlePresentation(uiState) == null) {
                    LearningWorkspaceSurface(
                        uiState = uiState,
                        learningScene = learningScene,
                        completePresentation = contentPresentation,
                        contentStrings = contentStrings,
                        audioController = audioController,
                        typographyPreferences = typographyPreferences,
                        presentationPreferences = presentationPreferences,
                        onRevealAnswer = onRevealAnswer,
                        onCompleteFlowStage = ::requestFlowStageCompletion,
                        onAgain = onAgain,
                        onHard = onHard,
                        onGood = onGood,
                        onEasy = onEasy,
                        typingState = typingState,
                        typingElapsedMillis = typingElapsedMillis,
                        onTypingInputChanged = { value ->
                            val typingScene = learningScene as? TypingScene
                            if (typingScene != null) {
                                typingState =
                                    TypingRecallInteraction.updateInput(
                                        typingState,
                                        value,
                                        typingScene.prompt,
                                        TypingAnswerEvaluator(),
                                        typingAttemptTimeSource.nowMillis()
                                    )
                            }
                        },
                        onTypingFocusChanged = { focused -> typingInputFocused = focused },
                        workspaceStrings = workspaceStrings,
                        visualLayout = visualLayout,
                        fullAnswerAvailableBodyHeightDp = fullAnswerAvailableBodyHeightDp,
                        examplesDisclosureKeyboard = examplesDisclosureKeyboard
                    )
                }

                // 3. SecondaryWorkspace (Dashboard Metrics, Error Cards, Completion Cards, Feedback & Explanations)
                SecondaryWorkspace(
                    uiState = uiState,
                    workspacePresentation = workspacePresentation,
                    contentStrings = contentStrings,
                    workspaceStrings = workspaceStrings,
                    onRefresh = onRefresh,
                    onStartStudy = onStartStudy,
                    onReplayLatestCompletedStudySession = onReplayLatestCompletedStudySession,
                    onStartLearnedItemsReview = onStartLearnedItemsReview,
                    onShowDecisionExplanation = onShowDecisionExplanation,
                    onHideDecisionExplanation = onHideDecisionExplanation,
                    onCompleteAdaptiveSession = onCompleteAdaptiveSession,
                    onBackToLesson = onBackToLesson,
                    onBackToLibrary = onBackToLibrary,
                    onContinueLearning = onContinueLearning
                )
                }
            }

            // 4. ActionDock (Fixed Action Bar at bottom)
            ActionDock(
                uiState = uiState,
                learningScene = learningScene,
                contentStrings = contentStrings,
                workspaceStrings = workspaceStrings,
                onStartStudy = onStartStudy,
                onCompleteFlowStage = ::requestFlowStageCompletion,
                onAgain = onAgain,
                onHard = onHard,
                onGood = onGood,
                onEasy = onEasy,
                onTypingCorrectCompleted = onTypingCorrectCompleted,
                onTypingForcedAgain = onTypingForcedAgain,
                onBackToLibrary = onBackToLibrary,
                visualLayout = visualLayout,
                suppressForTypingSuccess = typingSuccessInProgress
            )

            // 5. StatusStrip (Fixed Bottom Status Bar)
            StatusStrip(
                uiState = uiState,
                shortcutRegistry = shortcutRegistry,
                onAgain = onAgain,
                onHard = onHard,
                onGood = onGood,
                onEasy = onEasy,
                onReplay = audioController::replayPrimary,
                onUndo = ::requestUndo,
                audioPaths = shortcutAudioPaths,
                onAudioAction = { action ->
                    performStudyAudioKeyboardAction(action, shortcutAudioPaths, audioController)
                }
            )
        }

        AnimatedVisibility(
            visible = typingSuccessInProgress && typingCanonicalAnswer != null,
            enter =
                fadeIn(tween(180)) +
                    scaleIn(
                        animationSpec =
                            keyframes {
                                durationMillis = 420
                                0.90f at 0
                                1.04f at 280
                                1.0f at 420
                            },
                        initialScale = 0.90f
                    ),
            exit = fadeOut(tween(140)),
            modifier = Modifier.fillMaxSize().zIndex(10f)
        ) {
            typingCanonicalAnswer?.let { canonicalAnswer ->
                TypingSuccessFocusOverlay(
                    canonicalAnswer = canonicalAnswer,
                    successMessage = contentStrings.typingCorrectSuccess,
                    previousRating = typingSuccessDecision?.first?.previousRating,
                    finalRating = typingSuccessDecision?.second?.rating,
                    workspaceStrings = workspaceStrings,
                    viewportClass = visualLayout.viewportClass
                )
            }
        }
    }
}

internal fun synchronizeStudyAudio(
    audioController: LearningContentAudioController,
    learningScene: LearningScene?,
    sessionCompleted: Boolean
) {
    if (sessionCompleted) audioController.stop() else audioController.bind(learningScene)
}

internal suspend fun awaitTypingAnswerAudio(
    audioController: LearningContentAudioController,
    path: Path
) {
    val expectedPath = path.toAbsolutePath().normalize()
    suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean(false)
        var registration: AutoCloseable? = null
        fun finish() {
            if (completed.compareAndSet(false, true)) {
                registration?.close()
                continuation.resume(Unit)
            }
        }
        registration =
            audioController.listen { state ->
                when (state) {
                    is LearningContentAudioState.Completed ->
                        if (state.path == expectedPath) finish()
                    is LearningContentAudioState.Failed ->
                        if (state.path == expectedPath) finish()
                    else -> Unit
                }
            }
        continuation.invokeOnCancellation {
            if (completed.compareAndSet(false, true)) {
                registration.close()
                audioController.stop()
            }
        }
        if (continuation.isActive) {
            audioController.playOnce(path)
        }
    }
}

internal suspend fun awaitTypingRealtimeSuccessDebounce() {
    delay(TYPING_REALTIME_SUCCESS_DEBOUNCE_MILLIS)
}

private const val TYPING_SUCCESS_AFTER_AUDIO_DWELL_MILLIS = 350L
private const val TYPING_SUCCESS_WITHOUT_AUDIO_DWELL_MILLIS = 700L
private const val TYPING_REALTIME_SUCCESS_DEBOUNCE_MILLIS = 450L

/** 1. SessionHeader Composable */
@Composable
private fun SessionHeader(
    uiState: StudyUiState,
    accessibilityPresentation: StudyAccessibilityPresentation,
    workspaceStrings: StudyWorkspaceStrings,
    presentationState: StudyPresentationStagingState,
    onPresentationPreferencesChanged: (StudyPresentationPreferences) -> Unit,
    onOpenPresentationSettings: () -> Unit,
    onUndo: () -> Unit,
    onPause: () -> Unit,
    visualLayout: StudyVisualLayout,
    modifier: Modifier = Modifier
) {
    if (uiState.hasActiveSession) {
        ActiveSessionChrome(
            uiState = uiState,
            accessibilityPresentation = accessibilityPresentation,
            workspaceStrings = workspaceStrings,
            presentationState = presentationState,
            onPresentationPreferencesChanged = onPresentationPreferencesChanged,
            onOpenPresentationSettings = onOpenPresentationSettings,
            onUndo = onUndo,
            onPause = onPause,
            visualLayout = visualLayout,
            modifier = modifier
        )
    } else {
        StudyHeader(
            uiState = uiState,
            accessibilityPresentation = accessibilityPresentation,
            modifier = modifier
        )
    }
}

/** 2. LearningWorkspaceSurface Composable */
@Composable
private fun LearningWorkspaceSurface(
    uiState: StudyUiState,
    learningScene: LearningScene?,
    completePresentation: LearningContentPresentation,
    contentStrings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
    typographyPreferences: StudyTypographyPreferences,
    presentationPreferences: StudyPresentationPreferences,
    onRevealAnswer: () -> Unit,
    onCompleteFlowStage: () -> Unit,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onEasy: () -> Unit,
    typingState: TypingRecallUiState,
    typingElapsedMillis: Long,
    onTypingInputChanged: (TextFieldValue) -> Unit,
    onTypingFocusChanged: (Boolean) -> Unit,
    workspaceStrings: StudyWorkspaceStrings,
    visualLayout: StudyVisualLayout,
    fullAnswerAvailableBodyHeightDp: Int,
    examplesDisclosureKeyboard: ExamplesDisclosureKeyboardController,
    modifier: Modifier = Modifier
) {
    StudyItemCard(
        uiState = uiState,
        learningScene = learningScene,
        completePresentation = completePresentation,
        contentStrings = contentStrings,
        audioController = audioController,
        typographyPreferences = typographyPreferences,
        presentationPreferences = presentationPreferences,
        onRevealAnswer = onRevealAnswer,
        onCompleteFlowStage = onCompleteFlowStage,
        onAgain = onAgain,
        onHard = onHard,
        onGood = onGood,
        onEasy = onEasy,
        typingState = typingState,
        typingElapsedMillis = typingElapsedMillis,
        onTypingInputChanged = onTypingInputChanged,
        onTypingFocusChanged = onTypingFocusChanged,
        workspaceStrings = workspaceStrings,
        visualLayout = visualLayout,
        fullAnswerAvailableBodyHeightDp = fullAnswerAvailableBodyHeightDp,
        examplesDisclosureKeyboard = examplesDisclosureKeyboard,
        modifier = modifier
    )
}

/** 3. SecondaryWorkspace Composable */
@Composable
private fun SecondaryWorkspace(
    uiState: StudyUiState,
    workspacePresentation: FocusedStudyWorkspacePresentation,
    contentStrings: LearningContentRendererStrings,
    workspaceStrings: StudyWorkspaceStrings,
    onRefresh: () -> Unit,
    onStartStudy: () -> Unit,
    onReplayLatestCompletedStudySession: () -> Unit,
    onStartLearnedItemsReview: () -> Unit,
    onShowDecisionExplanation: () -> Unit,
    onHideDecisionExplanation: () -> Unit,
    onCompleteAdaptiveSession: () -> Unit,
    onBackToLesson: ((vn.loi.learning.domain.library.model.InstalledPackageId, vn.loi.learning.domain.content.model.ContentId) -> Unit)?,
    onBackToLibrary: (() -> Unit)?,
    onContinueLearning: ((vn.loi.learning.domain.library.model.InstalledPackageId, vn.loi.learning.domain.content.model.ContentId) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val learningActionCallbacks = StudyLearningActionCallbacks(
        continueLearning = onStartStudy,
        replayLatestCompletedSession = onReplayLatestCompletedStudySession,
        reviewAllLearned = onStartLearnedItemsReview,
        backToLibrary = { onBackToLibrary?.invoke() }
    )
    val onLearningAction: (StudyLearningAction) -> Unit = {
        dispatchStudyLearningAction(it, learningActionCallbacks)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LESpacing.md)
    ) {
        resolveStudyLoadErrorPresentation(uiState)?.let { presentation ->
            StudyLoadErrorCard(
                presentation = presentation,
                onRetry = onRefresh,
                workspaceStrings = workspaceStrings
            )
        }

        if (workspacePresentation.showDashboardMetrics) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.md)
            ) {
                StudyMetricCard(
                    label = "Reviewed",
                    value = uiState.reviewedCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                StudyMetricCard(
                    label = "New items",
                    value = uiState.newItemsReviewed.toString(),
                    modifier = Modifier.weight(1f)
                )
                StudyMetricCard(
                    label = "Review items",
                    value = uiState.reviewItemsReviewed.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (uiState.loadError != null) {
            // Preserve last good study state
        } else if (uiState.learnEntryChooserVisible) {
            resolveStudyIdlePresentation(uiState)?.let { presentation ->
                StudyIdleCard(
                    presentation = presentation,
                    onLearningAction = onLearningAction,
                    onBackToLibrary = onBackToLibrary,
                    enabled = !uiState.actionInProgress,
                    workspaceStrings = workspaceStrings
                )
            }
        } else if (uiState.sessionCompleted) {
            val completionState = remember(uiState) {
                SessionCompletionProjectionPolicy.create(uiState)
            }
            SessionCompletionCard(
                completionUiState = completionState,
                onBackToLesson = onBackToLesson,
                onContinueLearning = onContinueLearning,
                onLearningAction = onLearningAction,
                actionsEnabled = !uiState.actionInProgress
            )
        } else {
            val idlePresentation = resolveStudyIdlePresentation(uiState)
            if (idlePresentation != null) {
                StudyIdleCard(
                    presentation = idlePresentation,
                    onLearningAction = onLearningAction,
                    onBackToLibrary = onBackToLibrary,
                    enabled = !uiState.actionInProgress,
                    workspaceStrings = workspaceStrings
                )
            }
        }

        if (!uiState.canReview) uiState.schedulerFeedback?.let { feedback ->
            CompactSchedulerFeedback(feedback = feedback)
        }

        uiState.lastDecisionExplanation?.let { explanation ->
            DecisionExplanationCard(
                explanation = explanation,
                visible = uiState.isDecisionExplanationVisible,
                onShow = onShowDecisionExplanation,
                onHide = onHideDecisionExplanation
            )
        }

        if (uiState.lastDecisionExplanation != null && !uiState.sessionCompleted) {
            Button(
                onClick = onCompleteAdaptiveSession,
                enabled = !uiState.actionInProgress
            ) {
                Text("Complete learning session")
            }
        }
    }
}

/** 4. ActionDock Composable (Fixed Action Dock at bottom) */
@Composable
private fun ActionDock(
    uiState: StudyUiState,
    learningScene: LearningScene?,
    contentStrings: LearningContentRendererStrings,
    workspaceStrings: StudyWorkspaceStrings,
    onStartStudy: () -> Unit,
    onCompleteFlowStage: () -> Unit,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onEasy: () -> Unit,
    onTypingCorrectCompleted: (TypingRecallSuccessRequest) -> Unit,
    onTypingForcedAgain: (TypingRecallRevealRequest) -> Unit,
    onBackToLibrary: (() -> Unit)? = null,
    visualLayout: StudyVisualLayout,
    suppressForTypingSuccess: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (suppressForTypingSuccess) return
    val dockMode = resolveStudyActionDockMode(uiState)
    if (dockMode == StudyActionDockMode.HIDDEN) return

    LESurface(
        variant = StudySurfaceRoles.ratingDock,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = LESpacing.lg,
                vertical = visualLayout.ratingDockVerticalPaddingDp.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = LESpacing.md,
                    vertical = visualLayout.ratingDockVerticalPaddingDp.dp
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                uiState.typingRatingMode == TypingRatingMode.FORCED_AGAIN &&
                    uiState.forcedTypingRevealRequest != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(LESpacing.xs),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            workspaceStrings.typingReviewedAgainMessage,
                            color = LETheme.colors.textSecondary
                        )
                        LEPrimaryButton(
                            text = "${workspaceStrings.typingContinueAgain}  [Enter / Space]",
                            onClick = {
                                onTypingForcedAgain(uiState.forcedTypingRevealRequest)
                            },
                            enabled = !uiState.actionInProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                uiState.typingRatingMode == TypingRatingMode.AUTOMATIC_PENDING &&
                    uiState.pendingTypingSuccessRequest != null -> {
                    LEPrimaryButton(
                        text = "Retry automatic rating  [Enter / Space]",
                        onClick = {
                            onTypingCorrectCompleted(uiState.pendingTypingSuccessRequest)
                        },
                        enabled = !uiState.actionInProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                dockMode == StudyActionDockMode.INTRODUCTION -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                    ) {
                        ReadOnlyRatingContextDock(
                            reviewContext = uiState.currentItemReviewContext,
                            workspaceStrings = workspaceStrings,
                            visualLayout = visualLayout,
                            typingStatusOnly = learningScene is TypingScene
                        )
                        LEPrimaryButton(
                            text = "${contentStrings.nextFlowStage}  [Space]",
                            onClick = onCompleteFlowStage,
                            enabled = !uiState.actionInProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                dockMode == StudyActionDockMode.ANSWER_ACTIONS -> {
                    val callbacks = mapOf(
                        StudyActionControl.REVIEW_AGAIN to onAgain,
                        StudyActionControl.REVIEW_HARD to onHard,
                        StudyActionControl.REVIEW_GOOD to onGood,
                        StudyActionControl.REVIEW_EASY to onEasy
                    )
                    if (visualLayout.ratingArrangement == RatingArrangement.GRID_2X2) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                        ) {
                            studyRatingOrder.chunked(2).forEach { rowActions ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
                                ) {
                                    rowActions.forEach { control ->
                                        StudyRatingButton(
                                            control = control,
                                            onClick = callbacks.getValue(control),
                                            modifier = Modifier.weight(1f),
                                            enabled = !uiState.actionInProgress,
                                            workspaceStrings = workspaceStrings,
                                            reviewContext = uiState.currentItemReviewContext,
                                            visualLayout = visualLayout
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                        ) {
                            studyRatingOrder.forEach { control ->
                                StudyRatingButton(
                                    control = control,
                                    onClick = callbacks.getValue(control),
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.actionInProgress,
                                    workspaceStrings = workspaceStrings,
                                    reviewContext = uiState.currentItemReviewContext,
                                    visualLayout = visualLayout
                                )
                            }
                        }
                    }
                }
                dockMode == StudyActionDockMode.FRONT_CONTEXT -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                    ) {
                        ReadOnlyRatingContextDock(
                            reviewContext = uiState.currentItemReviewContext,
                            workspaceStrings = workspaceStrings,
                            visualLayout = visualLayout,
                            typingStatusOnly = learningScene is TypingScene
                        )
                        LEPrimaryButton(
                            text =
                                if (uiState.learningFlowCurrentStage is LearningFlowStage.AnswerReveal) {
                                    "${contentStrings.flowRetryReveal}  [Space]"
                                } else {
                                    "${contentStrings.nextFlowStage}  [Space]"
                                },
                            onClick = onCompleteFlowStage,
                            enabled = !uiState.actionInProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                dockMode == StudyActionDockMode.REVIEW_CONTEXT -> {
                    ReadOnlyRatingContextDock(
                        reviewContext = uiState.currentItemReviewContext,
                        workspaceStrings = workspaceStrings,
                        visualLayout = visualLayout,
                        typingStatusOnly = learningScene is TypingScene
                    )
                }
                dockMode == StudyActionDockMode.IDLE -> {
                    val idle = resolveStudyIdlePresentation(uiState)!!
                    LEPrimaryButton(
                        text = "${idle.actionLabel}  [${idle.shortcutHint}]",
                        onClick = {
                            if (idle.actionLabel == "Đi tới Thư viện") {
                                onBackToLibrary?.invoke()
                            } else {
                                onStartStudy()
                            }
                        },
                        enabled = !uiState.actionInProgress
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyRatingContextDock(
    reviewContext: CurrentStudyItemReviewContext?,
    workspaceStrings: StudyWorkspaceStrings,
    visualLayout: StudyVisualLayout,
    typingStatusOnly: Boolean
) {
    if (!typingStatusOnly) {
        LegacyReadOnlyRatingContextDock(reviewContext, workspaceStrings, visualLayout)
        return
    }
    val segments = resolveTypingRatingStatusPresentation(reviewContext)
    if (segments.isEmpty()) return
    Column(
        verticalArrangement = Arrangement.spacedBy(LESpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
        ) {
            segments.forEach { segment ->
                    val action = resolveStudyActionAccessibility(segment.control, workspaceStrings)
                    val colors = LETheme.colors
                    val ratingColor = when (segment.control) {
                        StudyActionControl.REVIEW_AGAIN -> colors.danger
                        StudyActionControl.REVIEW_HARD -> colors.warning
                        StudyActionControl.REVIEW_GOOD -> colors.success
                        StudyActionControl.REVIEW_EASY -> colors.info
                        else -> colors.textSecondary
                    }
                    val icon = when (segment.control) {
                        StudyActionControl.REVIEW_AGAIN -> LETheme.icons.StatisticsAgain
                        StudyActionControl.REVIEW_HARD -> LETheme.icons.StatisticsHard
                        StudyActionControl.REVIEW_GOOD -> LETheme.icons.StatisticsGood
                        StudyActionControl.REVIEW_EASY -> LETheme.icons.StatisticsEasy
                        else -> LETheme.icons.Learning
                    }
                    val statusLabel = when (segment.status) {
                        TypingRatingStatus.CURRENT -> workspaceStrings.typingRatingCurrentStatus
                        TypingRatingStatus.UPCOMING -> workspaceStrings.typingRatingUpcomingStatus
                        TypingRatingStatus.AVAILABLE -> workspaceStrings.typingRatingAvailableStatus
                    }
                    val contentColor =
                        if (segment.isActive) ratingColor else ratingColor.copy(alpha = 0.68f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((visualLayout.frontRatingSegmentHeightDp - 4).dp)
                            .semantics {
                                contentDescription =
                                    "${action.visibleLabel}. $statusLabel." +
                                        if (segment.isActive) {
                                            " ${workspaceStrings.previousRatingAccessibility}"
                                        } else {
                                            ""
                                        }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(LESpacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = action.visibleLabel,
                                    style = LETheme.typography.ratingAction.copy(
                                        color = contentColor,
                                        fontWeight =
                                            if (segment.isActive) FontWeight.Bold
                                            else FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = statusLabel,
                                    style = LETypography.caption,
                                    color =
                                        if (segment.isActive) {
                                            colors.textSecondary
                                        } else {
                                            colors.textMuted
                                        }
                                )
                            }
                        }
                        if (segment.isActive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(0.72f)
                                    .height(3.dp)
                                    .background(
                                        color = ratingColor,
                                        shape = RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                    }
            }
        }
        TypingAutomaticRatingInfoCard(
            workspaceStrings = workspaceStrings,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "ⓘ  ${workspaceStrings.typingRatingStatusNote}",
            style = LETypography.caption,
            color = LETheme.colors.textMuted,
            modifier = Modifier.semantics {
                contentDescription = workspaceStrings.typingRatingStatusNote
            }
        )
    }
}

@Composable
private fun TypingAutomaticRatingInfoCard(
    workspaceStrings: StudyWorkspaceStrings,
    modifier: Modifier = Modifier
) {
    val description =
        "${workspaceStrings.typingAutoRatingPrimary} ${workspaceStrings.typingAutoRatingSecondary}"
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
        color = LETheme.colors.infoContainer.copy(alpha = 0.55f),
        contentColor = LETheme.colors.textPrimary,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LESpacing.md, vertical = LESpacing.xs),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = LETheme.icons.Info,
                contentDescription = null,
                tint = LETheme.colors.info,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(LESpacing.sm))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = workspaceStrings.typingAutoRatingPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = workspaceStrings.typingAutoRatingSecondary,
                    style = LETypography.caption,
                    color = LETheme.colors.textMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LegacyReadOnlyRatingContextDock(
    reviewContext: CurrentStudyItemReviewContext?,
    workspaceStrings: StudyWorkspaceStrings,
    visualLayout: StudyVisualLayout
) {
    val segments = resolveRatingDockPresentation(RatingDockMode.QUESTION_CONTEXT, reviewContext)
    if (segments.isEmpty()) return
    val rows =
        if (visualLayout.ratingArrangement == RatingArrangement.GRID_2X2) segments.chunked(2)
        else listOf(segments)
    Column(verticalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
            ) {
                row.forEach { segment ->
                    val action = resolveStudyActionAccessibility(segment.control, workspaceStrings)
                    val colors = LETheme.colors
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(visualLayout.frontRatingSegmentHeightDp.dp)
                            .semantics {
                                contentDescription = action.visibleLabel +
                                    if (segment.isPreviousRating) {
                                        " ${workspaceStrings.previousRatingAccessibility}"
                                    } else {
                                        ""
                                    }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = action.visibleLabel,
                            style = LETheme.typography.ratingAction.copy(
                                color =
                                    if (segment.isSubdued) colors.textMuted
                                    else colors.textPrimary,
                                textDecoration =
                                    if (segment.isPreviousRating) TextDecoration.Underline else null
                            )
                        )
                    }
                }
            }
        }
    }
}

/** 5. StatusStrip Composable (Fixed Status Strip) */
@Composable
private fun StatusStrip(
    uiState: StudyUiState,
    shortcutRegistry: ShortcutRegistry,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onEasy: () -> Unit,
    onReplay: () -> Unit,
    onUndo: () -> Unit,
    audioPaths: StudyShortcutAudioPaths,
    onAudioAction: (StudyKeyboardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val presentation = resolveStudyShortcutStatus(
        ratingReady =
            uiState.learningFlowProgress?.isRatingReady == true &&
                uiState.typingRatingMode == TypingRatingMode.STANDARD,
        registry = shortcutRegistry
    )
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val chrome = resolveStudyChromePresentation(maxWidth.value.toInt().coerceAtLeast(1))
        Surface(
            color = LEColors.surface,
            border = LEBorder.subtle,
            modifier = Modifier.fillMaxWidth().height(chrome.shortcutStripHeightDp.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = chrome.horizontalPaddingDp.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = presentation.accessibleDescription +
                            if (uiState.hasActiveSession) ". Active Session" else ". Idle"
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudyQuickActionToolbar(
                    presentation = presentation,
                    maximumItems = chrome.maximumShortcutItems,
                    composition = chrome.shortcutStripComposition,
                    enabled = !uiState.actionInProgress,
                    canUndo = uiState.canUndo,
                    onAgain = onAgain,
                    onHard = onHard,
                    onGood = onGood,
                    onEasy = onEasy,
                    onReplay = onReplay,
                    onUndo = onUndo,
                    audioPaths = audioPaths,
                    onAudioAction = onAudioAction,
                    modifier = Modifier.weight(1f),
                    horizontalGapDp = chrome.horizontalGapDp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(chrome.horizontalGapDp.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VerticalDivider(modifier = Modifier.height(16.dp))
                    StudySessionStatus(active = uiState.hasActiveSession)
                }
            }
        }
    }
}

@Composable
private fun StudyQuickActionToolbar(
    presentation: StudyShortcutStatusPresentation,
    maximumItems: Int,
    composition: StudyShortcutStripComposition,
    enabled: Boolean,
    canUndo: Boolean,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onEasy: () -> Unit,
    onReplay: () -> Unit,
    onUndo: () -> Unit,
    audioPaths: StudyShortcutAudioPaths,
    onAudioAction: (StudyKeyboardAction) -> Unit,
    horizontalGapDp: Int,
    modifier: Modifier = Modifier
) {
    val ordered = presentation.items
    val visibleItems =
        if (ordered.size <= maximumItems) {
            ordered
        } else {
            val ratings = ordered.filter { it.command.name.startsWith("RATE_") }
            val relevantAudio =
                ordered.firstOrNull { item -> isAvailableAudioCommand(item.command, audioPaths) }
            val undo = ordered.firstOrNull { it.command == StudyShortcutCommand.UNDO }
            (ratings + listOfNotNull(relevantAudio, undo)).distinctBy { it.command }
        }
    val overflowItems = ordered.filterNot { candidate ->
        visibleItems.any { it.command == candidate.command }
    }
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = presentation.accessibleDescription
        },
        horizontalArrangement = Arrangement.spacedBy(horizontalGapDp.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleItems.forEachIndexed { index, item ->
            if (index > 0 && isStudyToolbarGroupBoundary(visibleItems[index - 1], item)) {
                VerticalDivider(modifier = Modifier.height(16.dp))
            }
            when (item.command) {
                StudyShortcutCommand.RATE_AGAIN ->
                    StudyRatingQuickAction(
                        number = "1",
                        color = LEColors.danger,
                        tooltip = "1 = Again",
                        onClick = onAgain,
                        enabled = enabled
                    )
                StudyShortcutCommand.RATE_HARD ->
                    StudyRatingQuickAction(
                        number = "2",
                        color = LEColors.warning,
                        tooltip = "2 = Hard",
                        onClick = onHard,
                        enabled = enabled
                    )
                StudyShortcutCommand.RATE_GOOD ->
                    StudyRatingQuickAction(
                        number = "3",
                        color = LEColors.success,
                        tooltip = "3 = Good",
                        onClick = onGood,
                        enabled = enabled
                    )
                StudyShortcutCommand.RATE_EASY ->
                    StudyRatingQuickAction(
                        number = "4",
                        color = LEColors.info,
                        tooltip = "4 = Easy",
                        onClick = onEasy,
                        enabled = enabled
                    )
                StudyShortcutCommand.REPLAY_PRIMARY_AUDIO ->
                    StudyReplayQuickAction(
                        tooltip = "R = Replay",
                        onClick = onReplay,
                        enabled = enabled
                    )
                StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP ->
                    StudyAudioQuickAction(
                        cue = resolveStudyAudioToolbarCue(
                            item = item,
                            compact = composition != StudyShortcutStripComposition.STANDARD,
                            available = audioPaths.vocabulary != null,
                            unavailableReason = "Vocabulary audio unavailable"
                        ),
                        enabled = enabled && audioPaths.vocabulary != null,
                        onClick = {
                            onAudioAction(StudyKeyboardAction.TOGGLE_VOCABULARY_AUDIO_LOOP)
                        }
                    )
                StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP ->
                    StudyAudioQuickAction(
                        cue = resolveStudyAudioToolbarCue(
                            item = item,
                            compact = composition != StudyShortcutStripComposition.STANDARD,
                            available = audioPaths.englishExample != null,
                            unavailableReason = "Example audio unavailable"
                        ),
                        enabled = enabled && audioPaths.englishExample != null,
                        onClick = {
                            onAudioAction(StudyKeyboardAction.TOGGLE_EXAMPLE_AUDIO_LOOP)
                        }
                    )
                StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO ->
                    StudyAudioQuickAction(
                        cue = resolveStudyAudioToolbarCue(
                            item = item,
                            compact = composition != StudyShortcutStripComposition.STANDARD,
                            available = audioPaths.vietnameseMeaning != null,
                            unavailableReason = "Vietnamese meaning audio unavailable"
                        ),
                        enabled = enabled && audioPaths.vietnameseMeaning != null,
                        onClick = {
                            onAudioAction(StudyKeyboardAction.PLAY_VIETNAMESE_MEANING_AUDIO)
                        }
                    )
                StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO ->
                    StudyAudioQuickAction(
                        cue = resolveStudyAudioToolbarCue(
                            item = item,
                            compact = composition != StudyShortcutStripComposition.STANDARD,
                            available = audioPaths.vietnameseExample != null,
                            unavailableReason = "Vietnamese example audio unavailable"
                        ),
                        enabled = enabled && audioPaths.vietnameseExample != null,
                        onClick = {
                            onAudioAction(StudyKeyboardAction.PLAY_VIETNAMESE_EXAMPLE_AUDIO)
                        }
                    )
                StudyShortcutCommand.UNDO ->
                    StudyIconQuickAction(
                        icon = LEIcons.Undo,
                        tooltip = "Undo latest rating (${item.chordText})",
                        onClick = onUndo,
                        enabled = enabled && canUndo
                    )
                else -> Unit
            }
        }
        if (overflowItems.isNotEmpty()) {
            StudyAudioOverflow(
                items = overflowItems,
                audioPaths = audioPaths,
                enabled = enabled,
                onReplay = onReplay,
                onUndo = onUndo,
                onAudioAction = onAudioAction
            )
        }
    }
}

private fun isAvailableAudioCommand(
    command: StudyShortcutCommand,
    paths: StudyShortcutAudioPaths
): Boolean =
    when (command) {
        StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP -> paths.vocabulary != null
        StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP -> paths.englishExample != null
        StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO -> paths.vietnameseMeaning != null
        StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO -> paths.vietnameseExample != null
        else -> false
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyAudioQuickAction(
    cue: StudyAudioToolbarCue,
    enabled: Boolean,
    onClick: () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(cue.tooltip, maxLines = 3, softWrap = false) } },
        state = rememberTooltipState()
    ) {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            modifier = Modifier.height(28.dp).semantics {
                contentDescription = cue.contentDescription
            }
        ) {
            StudyToolbarSemanticIcon(presentation = cue.semantic)
            Spacer(Modifier.width(4.dp))
            Text(
                cue.visualChord,
                maxLines = 1,
                softWrap = false,
                color = LEColors.textMuted,
                style = LETypography.caption
            )
        }
    }
}

@Composable
private fun StudyToolbarSemanticIcon(
    presentation: StudyToolbarActionIconPresentation,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(22.dp), contentAlignment = Alignment.Center) {
        Icon(
            studyToolbarIconVector(presentation.icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        presentation.localeBadge?.let { badge ->
            Surface(
                color = LEColors.primary,
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    badge,
                    maxLines = 1,
                    softWrap = false,
                    color = LEColors.textOnPrimary,
                    fontSize = 7.sp,
                    lineHeight = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}

private fun studyToolbarIconVector(icon: StudyToolbarSemanticIcon): ImageVector =
    when (icon) {
        StudyToolbarSemanticIcon.REPLAY_AUDIO -> LEIcons.Play
        StudyToolbarSemanticIcon.LOOP_VOCABULARY_AUDIO -> LEIcons.Loop
        StudyToolbarSemanticIcon.LOOP_EXAMPLE_AUDIO -> LEIcons.LoopExample
        StudyToolbarSemanticIcon.PLAY_VIETNAMESE_MEANING -> LEIcons.VietnameseAudio
        StudyToolbarSemanticIcon.PLAY_VIETNAMESE_EXAMPLE -> LEIcons.VietnameseExampleAudio
        StudyToolbarSemanticIcon.UNDO -> LEIcons.Undo
        StudyToolbarSemanticIcon.SESSION_STATUS -> LEIcons.Success
    }

@Composable
private fun StudyAudioOverflow(
    items: List<StudyShortcutStatusItem>,
    audioPaths: StudyShortcutAudioPaths,
    enabled: Boolean,
    onReplay: () -> Unit,
    onUndo: () -> Unit,
    onAudioAction: (StudyKeyboardAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(26.dp).semantics {
                contentDescription =
                    "More Study actions. " +
                        items.joinToString(". ") {
                            "${it.fullAccessibleLabel} — shortcut ${it.chordText}"
                        }
            }
        ) {
            Icon(LEIcons.More, contentDescription = null, modifier = Modifier.size(17.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                val itemEnabled =
                    enabled &&
                        when (item.command) {
                            StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP ->
                                audioPaths.vocabulary != null
                            StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP ->
                                audioPaths.englishExample != null
                            StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO ->
                                audioPaths.vietnameseMeaning != null
                            StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO ->
                                audioPaths.vietnameseExample != null
                            else -> true
                        }
                DropdownMenuItem(
                    text = {
                        Text(
                            "${
                                if (studyAudioAction(item.command) != null) {
                                    studyAudioToolbarActionName(item.command)
                                } else {
                                    item.fullAccessibleLabel
                                }
                            }  ${item.compactLabel}",
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    leadingIcon = {
                        resolveStudyToolbarActionIcon(item.command)?.let { presentation ->
                            StudyToolbarSemanticIcon(presentation)
                        }
                    },
                    enabled = itemEnabled,
                    onClick = {
                        when (item.command) {
                            StudyShortcutCommand.REPLAY_PRIMARY_AUDIO -> onReplay()
                            StudyShortcutCommand.UNDO -> onUndo()
                            else -> studyAudioAction(item.command)?.let(onAudioAction)
                        }
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun studyAudioAction(command: StudyShortcutCommand): StudyKeyboardAction? =
    when (command) {
        StudyShortcutCommand.TOGGLE_VOCABULARY_AUDIO_LOOP ->
            StudyKeyboardAction.TOGGLE_VOCABULARY_AUDIO_LOOP
        StudyShortcutCommand.TOGGLE_EXAMPLE_AUDIO_LOOP ->
            StudyKeyboardAction.TOGGLE_EXAMPLE_AUDIO_LOOP
        StudyShortcutCommand.PLAY_VIETNAMESE_MEANING_AUDIO ->
            StudyKeyboardAction.PLAY_VIETNAMESE_MEANING_AUDIO
        StudyShortcutCommand.PLAY_VIETNAMESE_EXAMPLE_AUDIO ->
            StudyKeyboardAction.PLAY_VIETNAMESE_EXAMPLE_AUDIO
        else -> null
    }

private fun isStudyToolbarGroupBoundary(
    previous: StudyShortcutStatusItem,
    current: StudyShortcutStatusItem
): Boolean =
    previous.command.name.startsWith("RATE_") != current.command.name.startsWith("RATE_") ||
        previous.command == StudyShortcutCommand.REPLAY_PRIMARY_AUDIO ||
        current.command == StudyShortcutCommand.UNDO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyRatingQuickAction(
    number: String,
    color: androidx.compose.ui.graphics.Color,
    tooltip: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip, maxLines = 1, softWrap = false) } },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(26.dp).semantics { contentDescription = tooltip }
        ) {
            Surface(color = color, shape = RoundedCornerShape(50), modifier = Modifier.size(22.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = number,
                        maxLines = 1,
                        softWrap = false,
                        color = LEColors.textOnPrimary,
                        style = LETypography.caption,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyReplayQuickAction(
    tooltip: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip, maxLines = 1, softWrap = false) } },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(26.dp).semantics { contentDescription = tooltip }
        ) {
            Surface(
                color = androidx.compose.ui.graphics.Color.Transparent,
                border = LEBorder.subtle,
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "R",
                        maxLines = 1,
                        softWrap = false,
                        style = LETypography.caption,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyIconQuickAction(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip, maxLines = 1, softWrap = false) } },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(26.dp).semantics { contentDescription = tooltip }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudySessionStatus(active: Boolean) {
    val description = if (active) "Active Session" else "Idle"
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(description, maxLines = 1, softWrap = false) } },
        state = rememberTooltipState()
    ) {
        Surface(
            color =
                if (active) LEColors.success
                else LEColors.surfaceElevated,
            shape = RoundedCornerShape(50),
            modifier = Modifier.size(18.dp).semantics { contentDescription = description }
        ) {
            if (active) {
                Icon(
                    imageVector = LEIcons.Success,
                    contentDescription = null,
                    tint = LEColors.textOnPrimary,
                    modifier = Modifier.padding(3.dp)
                )
            }
        }
    }
}

@Composable
private fun DecisionExplanationCard(
    explanation: DecisionExplanation,
    visible: Boolean,
    onShow: () -> Unit,
    onHide: () -> Unit
) {
    LESurface(
        variant = LESurfaceVariant.SECONDARY,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = if (visible) "Why Product Brain made this decision" else "Decision explanation hidden"
            }
    ) {
        Column(
            modifier = Modifier.padding(LESpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LESpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Why this learning decision?",
                    style = LETypography.paneTitle,
                    fontWeight = FontWeight.Bold
                )
                LESecondaryButton(
                    text = if (visible) "Hide" else "Show explanation",
                    onClick = if (visible) onHide else onShow
                )
            }

            if (visible) {
                DecisionExplanationSection("What Product Brain observed", explanation.observation)
                DecisionExplanationSection("Decision", explanation.decisionSummary)
                DecisionExplanationSection("Why", explanation.pedagogicalReason)
                DecisionExplanationSection("What happens next", explanation.nextStep)
            }
        }
    }
}

@Composable
private fun DecisionExplanationSection(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = LETypography.fieldLabel,
            color = LEColors.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = LETypography.fieldValue
        )
    }
}

private fun Modifier.studyActionSemantics(
    control: StudyActionControl,
    strings: StudyWorkspaceStrings = StudyWorkspaceStrings.ENGLISH,
    previousRating: Boolean = false
): Modifier {
    val presentation = resolveStudyActionAccessibility(control, strings)
    return semantics {
        contentDescription = presentation.contentDescription +
            if (previousRating) " ${strings.previousRatingAccessibility}" else ""
    }
}

@Composable
private fun SessionSummaryCard(
    uiState: StudyUiState,
    onStartStudy: () -> Unit,
    workspaceStrings: StudyWorkspaceStrings
) {
    val accessibility = resolveStudySessionSummaryAccessibility(uiState)

    LESurface(
        variant = LESurfaceVariant.SECONDARY,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibility.contentDescription
            }
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SESSION COMPLETED",
                style = MaterialTheme.typography.labelLarge,
                color = LEColors.primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = uiState.studyTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = buildString {
                    append(uiState.reviewedCount)
                    append(" learning Content")
                    append(" reviewed")
                },
                style = MaterialTheme.typography.titleLarge,
                color = LETheme.colors.textSecondary
            )

            uiState.sessionProgress
                ?.takeIf { progress -> progress.skippedItemCount > 0 }
                ?.let { progress ->
                    Text(
                            text = "${progress.skippedItemCount} planned technical experience" +
                            if (progress.skippedItemCount == 1) " was not reviewed" else "s were not reviewed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LETheme.colors.textSecondary
                    )
                }

            HorizontalDivider()

            uiState.sessionCompletion?.let { completion ->
                CompletionSummarySection("What you learned", completion.whatWasLearned)
                CompletionSummarySection("Overall outcome", completion.overallOutcome)
                CompletionSummarySection("Reflection", completion.reflection)
                CompletionSummarySection("What needs reinforcement", completion.reinforcement)
                CompletionSummarySection("What happens next", completion.whatHappensNext)
                CompletionSummarySection("Next review", completion.schedulingGuidance)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SessionSummaryMetric(
                    label = "Total reviews",
                    value = uiState.reviewedCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                SessionSummaryMetric(
                    label = "New",
                    value = uiState.newItemsReviewed.toString(),
                    modifier = Modifier.weight(1f)
                )
                SessionSummaryMetric(
                    label = "Scheduled",
                    value = uiState.reviewItemsReviewed.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.isLessonStudy && uiState.hasKnownTotal) {
                Text(
                    text = "${uiState.reviewedCount} of ${uiState.totalItems} lesson items completed",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LETheme.colors.textSecondary
                )
            }

            val action = resolveStudyActionAccessibility(StudyActionControl.START_GENERAL_STUDY, workspaceStrings)
            LEPrimaryButton(
                text = "${action.visibleLabel}  [${action.shortcutHint}]",
                onClick = onStartStudy,
                modifier = Modifier.studyActionSemantics(StudyActionControl.START_GENERAL_STUDY, workspaceStrings)
            )
        }
    }
}

@Composable
internal fun CompletionSummarySection(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = LEColors.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
internal fun SessionSummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = LEColors.primary
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = LETheme.colors.textSecondary
        )
    }
}

@Composable
private fun StudyIdleCard(
    presentation: StudyIdlePresentation,
    onLearningAction: (StudyLearningAction) -> Unit,
    onBackToLibrary: (() -> Unit)?,
    enabled: Boolean,
    workspaceStrings: StudyWorkspaceStrings
) {
    LESurface(
        variant = LESurfaceVariant.SECONDARY,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = presentation.title,
                style = LETypography.paneTitle,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = presentation.description,
                style = LETypography.fieldValue,
                color = LEColors.textSecondary
            )

            if (presentation.actions.isEmpty()) {
                LEPrimaryButton(
                    text = "${presentation.actionLabel}  [${presentation.shortcutHint}]",
                    onClick = { onBackToLibrary?.invoke() },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            presentation.actions.forEachIndexed { index, action ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (index == 0) {
                        LEPrimaryButton(
                            text = action.label,
                            onClick = { onLearningAction(action.action) },
                            enabled = enabled && action.enabled,
                            modifier = Modifier.fillMaxWidth()
                                .studyActionSemantics(StudyActionControl.START_STUDY, workspaceStrings)
                        )
                    } else {
                        LEButton(
                            label = action.label,
                            onClick = { onLearningAction(action.action) },
                            enabled = enabled && action.enabled,
                            variant =
                                if (action.action == StudyLearningAction.BACK_TO_LIBRARY) {
                                    LEButtonVariant.QUIET
                                } else {
                                    LEButtonVariant.SECONDARY
                                },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = action.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = LETheme.colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyItemCard(
    uiState: StudyUiState,
    learningScene: LearningScene?,
    completePresentation: LearningContentPresentation,
    contentStrings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
    typographyPreferences: StudyTypographyPreferences,
    presentationPreferences: StudyPresentationPreferences,
    onRevealAnswer: () -> Unit,
    onCompleteFlowStage: () -> Unit,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onEasy: () -> Unit,
    typingState: TypingRecallUiState,
    typingElapsedMillis: Long,
    onTypingInputChanged: (TextFieldValue) -> Unit,
    onTypingFocusChanged: (Boolean) -> Unit,
    workspaceStrings: StudyWorkspaceStrings,
    visualLayout: StudyVisualLayout,
    fullAnswerAvailableBodyHeightDp: Int,
    examplesDisclosureKeyboard: ExamplesDisclosureKeyboardController,
    modifier: Modifier = Modifier
) {
    val contentAccessibility = resolveStudyContentAccessibility(uiState)

    LESurface(
        variant = StudySurfaceRoles.answer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LESpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (
                uiState.hasActiveSession &&
                !uiState.canReview &&
                uiState.contentIntroductionState != ContentIntroductionState.REQUIRED &&
                visualLayout.heightMode == StudyHeightMode.COMFORTABLE
            ) {
                val stageToDisplay = uiState.contentPresentationStage ?: uiState.learningStage
                val learningStageLabel = resolveLearningStageLabel(stageToDisplay)
                val badgeVariant = resolveLearningStageBadgeVariant(stageToDisplay)
                LEStatusBadge(
                    variant = badgeVariant,
                    customText = learningStageLabel
                )
            }

            if (uiState.contentIntroductionState != ContentIntroductionState.REQUIRED) {
                FlowProgressIndicator(uiState, contentStrings)
            }

            val answerModel = remember(uiState, learningScene, completePresentation) {
                FocusedVocabularyAnswerResolver.resolve(
                    uiState,
                    learningScene,
                    completePresentation
                )
            }
            val availability = remember(answerModel) {
                answerModel.presentationAvailability()
            }
            val recommendation = remember(
                uiState.learningExperiencePlan,
                uiState.learningFlowSelection
            ) {
                QuestionPresentationRecommendationResolver.resolve(
                    plan = uiState.learningExperiencePlan,
                    selection = uiState.learningFlowSelection
                )
            }
            val effectivePresentation = remember(
                presentationPreferences,
                availability,
                recommendation
            ) {
                StudyPresentationPolicy.resolve(
                    preferences = presentationPreferences,
                    availability = availability,
                    recommendation = recommendation
                )
            }
            val fullAnswerAudio = remember(answerModel) {
                FullAnswerAudioPresentation.resolve(answerModel)
            }
            val autoplayCoordinator = remember { StudyAutoplayCoordinator() }
            val questionAvailability =
                remember(availability, learningScene, effectivePresentation) {
                    questionTransitionAvailability(
                        availability,
                        learningScene,
                        effectivePresentation
                    )
                }
            LaunchedEffect(uiState.currentLearningItemId, uiState.canReview) {
                autoplayCoordinator.nextAutoplay(
                    transition = StudyAutoplayTransition(
                        itemId = uiState.currentLearningItemId,
                        phase =
                            if (uiState.canReview) StudyAutoplayPhase.ANSWER_REVEALED
                            else StudyAutoplayPhase.QUESTION_BOUND
                    ),
                    questionAvailability = questionAvailability,
                    questionEffective = effectivePresentation,
                    fullAnswerAudio = fullAnswerAudio
                )?.let(audioController::playOnce)
            }
            LaunchedEffect(
                effectivePresentation,
                audioController.activeLoopPath,
                uiState.canReview
            ) {
                val active = audioController.activeLoopPath
                if (
                    !uiState.canReview &&
                    active != null &&
                    active in hiddenLoopPaths(availability, effectivePresentation)
                ) {
                    audioController.stopLoop()
                }
            }

            if (uiState.contentIntroductionState == ContentIntroductionState.REQUIRED) {
                DiscoveryFrontSurface(
                    model = answerModel,
                    strings = contentStrings,
                    audioController = audioController,
                    layout = visualLayout,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (uiState.canReview) {
                val typography = remember(typographyPreferences, visualLayout) {
                    StudyTypographyPresentationResolver.resolve(
                        preferences = typographyPreferences,
                        viewportWidthDp = visualLayout.contentMaxWidthDp
                    )
                }
                val typingComparisonPresentation =
                    resolveTypingRevealComparison(
                        evaluation = typingState.revealEvaluation,
                        userAnswerLabel = contentStrings.typingYourAnswer,
                        correctAnswerLabel = contentStrings.typingCorrectAnswer
                    )
                FocusedAnswerSurface(
                    model = answerModel,
                    disclosure = FullAnswerPresentation.resolve(answerModel),
                    strings = contentStrings,
                    audioController = audioController,
                    schedulerFeedback = uiState.schedulerFeedback,
                    typography = typography,
                    layout = visualLayout,
                    availableBodyHeightDp = fullAnswerAvailableBodyHeightDp,
                    typingComparison = typingComparisonPresentation,
                    currentLearningItemId = uiState.currentLearningItemId,
                    examplesDisclosureKeyboard = examplesDisclosureKeyboard,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (learningScene == null) {
                Text(uiState.contentText, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            } else {
                LearningSceneRenderer(
                    scene = learningScene,
                    strings = contentStrings,
                    audioController = audioController,
                    partOfSpeech = answerModel.partOfSpeech,
                    presentation = effectivePresentation,
                    layout = visualLayout,
                    manualSceneAudioInteraction =
                        if (learningScene is TypingScene) {
                            ManualSceneAudioInteraction.SUPPRESS
                        } else {
                            ManualSceneAudioInteraction.ALLOW
                        },
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = contentAccessibility.promptDescription
                    }
                )
            }

            if (learningScene is TypingScene && typingState.attempt != null) {
                TypingAutoRatingTimerPanel(
                    attempt = typingState.attempt,
                    elapsedMillis = typingElapsedMillis,
                    ratingMode = uiState.typingRatingMode,
                    workspaceStrings = workspaceStrings,
                    layout = visualLayout,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (learningScene is TypingScene && uiState.canRevealAnswer) {
                TypingRecallInput(
                    state = typingState,
                    strings = contentStrings,
                    enabled =
                        !uiState.actionInProgress &&
                            !typingState.successInProgress,
                    onInputChanged = onTypingInputChanged,
                    onReveal = onCompleteFlowStage,
                    onFocusChanged = onTypingFocusChanged,
                    focusIdentity =
                        "${uiState.currentLearningItemId}:${learningScene.prompt.expectedAnswer}",
                    layout = visualLayout
                )
            }

        }
    }
}

internal fun resolveLearningStageLabel(
    learningStage: vn.loi.learning.domain.study.memory.model.LearningStage?
): String =
    learningStage?.name ?: "UNKNOWN"

internal fun resolveLearningStageBadgeVariant(
    learningStage: vn.loi.learning.domain.study.memory.model.LearningStage?
): StatusBadgeVariant = when (learningStage) {
    vn.loi.learning.domain.study.memory.model.LearningStage.NEW -> StatusBadgeVariant.Present
    vn.loi.learning.domain.study.memory.model.LearningStage.LEARNING,
    vn.loi.learning.domain.study.memory.model.LearningStage.RELEARNING -> StatusBadgeVariant.Warning
    vn.loi.learning.domain.study.memory.model.LearningStage.REVIEW,
    vn.loi.learning.domain.study.memory.model.LearningStage.MASTERED -> StatusBadgeVariant.Valid
    vn.loi.learning.domain.study.memory.model.LearningStage.SUSPENDED -> StatusBadgeVariant.Missing
    null -> StatusBadgeVariant.NotEvaluated
}

@Composable
private fun TypingAutoRatingTimerPanel(
    attempt: TypingAttemptState?,
    elapsedMillis: Long,
    ratingMode: TypingRatingMode,
    workspaceStrings: StudyWorkspaceStrings,
    layout: StudyVisualLayout,
    modifier: Modifier = Modifier
) {
    val preview =
        remember(attempt, elapsedMillis, ratingMode) {
            TypingAutoRatingPreviewResolver.resolve(attempt, elapsedMillis, ratingMode)
        } ?: return
    val visual = remember(layout.viewportClass) {
        TypingTimerPresentationResolver.resolve(layout.viewportClass)
    }
    val ratingLabel =
        preview.decision?.rating?.toStudyActionControl()?.let(workspaceStrings::label)
            ?: workspaceStrings.typingTimerReady
    val timerColor = resolveTypingRatingPreviewColor(preview.colorRole, LETheme.colors)

    Column(
        modifier = modifier,
        horizontalAlignment =
            if (layout.viewportClass == StudyViewportClass.WIDE) {
                Alignment.End
            } else {
                Alignment.CenterHorizontally
            },
        verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription =
                    if (preview.state == TypingRatingPreviewState.READY) {
                        workspaceStrings.typingTimerReadyAccessibility
                    } else {
                        workspaceStrings.typingTimerAccessibility(
                            preview.elapsedMillis / 1_000L,
                            ratingLabel
                        )
                    }
            }
        ) {
            Icon(
                imageVector = LETheme.icons.Timer,
                contentDescription = null,
                tint = timerColor,
                modifier = Modifier.size(visual.iconSizeDp.dp)
            )
            Text(
                text = formatTypingElapsed(preview.elapsedMillis),
                fontSize = visual.valueFontSizeSp.sp,
                lineHeight = visual.valueFontSizeSp.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = timerColor
            )
        }
        Text(
            text =
                if (preview.state == TypingRatingPreviewState.READY) {
                    workspaceStrings.typingTimerReady
                } else {
                    workspaceStrings.typingProjectedRating(ratingLabel)
                },
            style = LETypography.caption,
            fontWeight = FontWeight.SemiBold,
            color = timerColor
        )
        TypingRatingLegend(
            preview = preview,
            layout = visual.legendLayout,
            workspaceStrings = workspaceStrings,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TypingRatingLegend(
    preview: TypingRatingPreview,
    layout: TypingLegendLayout,
    workspaceStrings: StudyWorkspaceStrings,
    modifier: Modifier = Modifier
) {
    val easyMaximum = formatTypingThreshold(preview.thresholds.easyMaximumElapsedMillis)
    val hardMinimum = formatTypingThreshold(preview.thresholds.hardMinimumElapsedMillis)
    val entries =
        listOf(
            TypingLegendEntry(
                StudyActionControl.REVIEW_AGAIN,
                TypingRatingColorRole.AGAIN,
                workspaceStrings.typingLegendAgain
            ),
            TypingLegendEntry(
                StudyActionControl.REVIEW_HARD,
                TypingRatingColorRole.HARD,
                workspaceStrings.typingLegendHard(hardMinimum)
            ),
            TypingLegendEntry(
                StudyActionControl.REVIEW_GOOD,
                TypingRatingColorRole.GOOD,
                if (preview.thresholds.easyAvailable) {
                    workspaceStrings.typingLegendGood(easyMaximum, hardMinimum)
                } else {
                    workspaceStrings.typingLegendGoodWithoutEasy(hardMinimum)
                }
            ),
            TypingLegendEntry(
                StudyActionControl.REVIEW_EASY,
                TypingRatingColorRole.EASY,
                if (preview.thresholds.easyAvailable) {
                    workspaceStrings.typingLegendEasy(easyMaximum)
                } else {
                    workspaceStrings.typingLegendEasyUnavailable
                }
            )
        )
    val rows =
        if (layout == TypingLegendLayout.SINGLE_ROW) listOf(entries)
        else entries.chunked(2)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { entry ->
                    val label = workspaceStrings.label(entry.control)
                    val color = resolveTypingRatingPreviewColor(entry.colorRole, LETheme.colors)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "$label. ${entry.detail}."
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, RoundedCornerShape(50))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$label — ${entry.detail}",
                            style = LETypography.caption,
                            color = LETheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class TypingLegendEntry(
    val control: StudyActionControl,
    val colorRole: TypingRatingColorRole,
    val detail: String
)

private fun vn.loi.learning.domain.study.memory.model.ReviewRating.toStudyActionControl():
    StudyActionControl =
    when (this) {
        vn.loi.learning.domain.study.memory.model.ReviewRating.AGAIN ->
            StudyActionControl.REVIEW_AGAIN
        vn.loi.learning.domain.study.memory.model.ReviewRating.HARD ->
            StudyActionControl.REVIEW_HARD
        vn.loi.learning.domain.study.memory.model.ReviewRating.GOOD ->
            StudyActionControl.REVIEW_GOOD
        vn.loi.learning.domain.study.memory.model.ReviewRating.EASY ->
            StudyActionControl.REVIEW_EASY
    }

private fun resolveTypingRatingPreviewColor(
    role: TypingRatingColorRole,
    colors: vn.loi.learning.desktop.ui.theme.LEColors
): Color =
    when (role) {
        TypingRatingColorRole.READY -> colors.accentPrimary
        TypingRatingColorRole.AGAIN -> colors.danger
        TypingRatingColorRole.HARD -> colors.warning
        TypingRatingColorRole.GOOD -> colors.success
        TypingRatingColorRole.EASY -> colors.info
    }

@Composable
private fun FlowProgressIndicator(
    uiState: StudyUiState,
    strings: LearningContentRendererStrings
) {
    val stage = uiState.learningFlowCurrentStage ?: return
    if (stage is LearningFlowStage.RatingReady) return
    val progress = uiState.learningFlowProgress ?: return
    val label = when (stage) {
        is LearningFlowStage.Experience -> when (stage.selection.selectedKind) {
            LearningExperienceKind.IMAGE_RECALL -> strings.flowImageRecall
            LearningExperienceKind.LISTENING_RECALL -> strings.flowListeningRecall
            LearningExperienceKind.PROMPT_RECALL -> strings.flowPromptRecall
            LearningExperienceKind.TYPING_RECALL -> strings.flowTypingRecall
        }

        is LearningFlowStage.AnswerReveal -> strings.flowPreparingAnswer
        is LearningFlowStage.RatingReady -> return
    }
    val text = progress.currentExperienceNumber?.takeIf {
        progress.totalExperienceCount > 1
    }?.let { number ->
        strings.flowStageTemplate(number, progress.totalExperienceCount, label)
    } ?: label
    Text(
        text = text,
        style = LETypography.caption,
        color = resolveStudyReadyStatusColor(LETheme.colors),
        modifier = Modifier.semantics {
            contentDescription = "${strings.flowProgress}: $text"
        }
    )
}

@Composable
private fun TypingRecallInput(
    state: TypingRecallUiState,
    strings: LearningContentRendererStrings,
    enabled: Boolean,
    onInputChanged: (TextFieldValue) -> Unit,
    onReveal: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    focusIdentity: String,
    layout: StudyVisualLayout
) {
    val requester = remember { FocusRequester() }
    val presentation = remember(layout.viewportClass) {
        TypingPresentationResolver.input(layout.viewportClass)
    }
    val linePresentation = TypingPresentationResolver.lineLayout(state.input)
    LaunchedEffect(focusIdentity, enabled) {
        if (shouldRequestTypingInputFocus(enabled, state.successInProgress)) {
            requester.requestFocus()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            strings.flowTypingRecall,
            style = MaterialTheme.typography.labelLarge,
            color = LETheme.colors.textSecondary
        )
        OutlinedTextField(
            value = state.textFieldValue,
            onValueChange = onInputChanged,
            label = {
                Text(
                    strings.typingInputLabel,
                    fontSize = presentation.labelFontSizeSp.sp
                )
            },
            placeholder = {
                Text(
                    strings.typingInputPlaceholder,
                    fontSize = presentation.placeholderFontSizeSp.sp,
                    lineHeight = presentation.placeholderLineHeightSp.sp,
                    fontWeight = FontWeight.Normal,
                    color = LETheme.colors.textMuted.copy(
                        alpha = presentation.placeholderAlpha
                    ),
                    textAlign = presentation.horizontalAlignment,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            enabled = enabled,
            singleLine = linePresentation.singleLine,
            minLines = linePresentation.minimumLines,
            maxLines = linePresentation.maximumLines,
            shape = RoundedCornerShape(16.dp),
            textStyle =
                MaterialTheme.typography.headlineSmall.copy(
                    fontSize = presentation.typedTextFontSizeSp.sp,
                    lineHeight = presentation.typedTextLineHeightSp.sp,
                    fontWeight = presentation.typedTextFontWeight,
                    textAlign = presentation.horizontalAlignment,
                    letterSpacing = presentation.letterSpacingSp.sp
                ),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LETheme.colors.borderFocus,
                    focusedContainerColor = LETheme.colors.surfaceSecondary,
                    unfocusedContainerColor = LETheme.colors.surfaceSecondary,
                    cursorColor = LETheme.colors.accentPrimary
                ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
                KeyboardActions(
                    onDone = {
                        if (
                            state.liveEvaluation?.status !=
                            TypingAnswerEvaluationStatus.CORRECT
                        ) {
                            onReveal()
                        }
                    }
                ),
            visualTransformation =
                typingLiveDiffVisualTransformation(
                    evaluation = state.liveEvaluation,
                    dangerColor = LETheme.colors.danger
                ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = presentation.minimumHeightDp.dp,
                    max = presentation.maximumHeightDp.dp
                )
                .semantics {
                    contentDescription = strings.typingInputLabel
                }
                .focusRequester(requester)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                        if (
                            state.liveEvaluation?.status !=
                            TypingAnswerEvaluationStatus.CORRECT
                        ) {
                            onReveal()
                        }
                        true
                    } else {
                        false
                    }
                }
        )
        Button(
            onClick = onReveal,
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = LETheme.colors.accentPrimary
                ),
            modifier =
                Modifier
                    .fillMaxWidth(presentation.revealWidthFraction)
                    .heightIn(min = 52.dp)
                    .align(Alignment.CenterHorizontally)
                    .semantics {
                        contentDescription = "${strings.typingReveal}. Shortcut: Enter"
                    }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(strings.typingReveal, fontWeight = FontWeight.Bold)
                Text("Enter", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TypingSuccessFocusOverlay(
    canonicalAnswer: String,
    successMessage: String,
    previousRating: ReviewRating?,
    finalRating: ReviewRating?,
    workspaceStrings: StudyWorkspaceStrings,
    viewportClass: StudyViewportClass
) {
    val presentation =
        remember(viewportClass, canonicalAnswer) {
            TypingPresentationResolver.successOverlay(viewportClass, canonicalAnswer)
        }
    val interactionSource = remember { MutableInteractionSource() }
    val previousLabel =
        previousRating?.toStudyActionControl()?.let(workspaceStrings::label)
            ?: workspaceStrings.typingNewRatingLabel
    val finalLabel =
        finalRating?.toStudyActionControl()?.let(workspaceStrings::label)
            ?: workspaceStrings.typingNewRatingLabel

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {}
                )
                .semantics {
                    liveRegion = LiveRegionMode.Assertive
                    contentDescription =
                        "Correct. $canonicalAnswer. " +
                            workspaceStrings.typingRatingTransitionAccessibility(
                                previousLabel,
                                finalLabel
                            )
                }
                .padding(horizontal = presentation.horizontalMarginDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = LETheme.colors.surfacePrimary,
            border = BorderStroke(2.dp, LETheme.colors.success),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = LEIcons.Success,
                    contentDescription = null,
                    tint = LETheme.colors.success,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = canonicalAnswer,
                    style =
                        LETheme.typography.displayWord.copy(
                            fontSize = presentation.answerFontSizeSp.sp,
                            lineHeight = presentation.answerLineHeightSp.sp,
                            fontWeight = FontWeight.Bold
                        ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    softWrap = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = successMessage,
                    style = MaterialTheme.typography.titleMedium,
                    color = LETheme.colors.success
                )
                if (finalRating != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = previousLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color =
                                previousRating?.let {
                                    resolveTypingRatingPreviewColor(it.toColorRole(), LETheme.colors)
                                } ?: LETheme.colors.textSecondary
                        )
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.titleLarge,
                            color = LETheme.colors.accentPrimary
                        )
                        Text(
                            text = finalLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color =
                                resolveTypingRatingPreviewColor(
                                    finalRating.toColorRole(),
                                    LETheme.colors
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun ReviewRating.toColorRole(): TypingRatingColorRole =
    when (this) {
        ReviewRating.AGAIN -> TypingRatingColorRole.AGAIN
        ReviewRating.HARD -> TypingRatingColorRole.HARD
        ReviewRating.GOOD -> TypingRatingColorRole.GOOD
        ReviewRating.EASY -> TypingRatingColorRole.EASY
    }

@Composable
private fun TypingEvaluationFeedback(
    title: String,
    guidance: String?,
    success: Boolean
) {
    val description = listOfNotNull(title, guidance).joinToString(". ")
    Column(
        verticalArrangement = Arrangement.spacedBy(LESpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = description
        }
    ) {
        Text(
            text = if (success) "✓ $title" else title,
            style = MaterialTheme.typography.titleMedium,
            color = if (success) LETheme.colors.success else LETheme.colors.danger
        )
        guidance?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = LETheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun StudyRatingGuidanceCard(workspaceStrings: StudyWorkspaceStrings) {
    val guidance = resolveStudyRatingGuidance()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = resolveStudyRatingGuidanceDescription()
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Choose the rating that matches your recall:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        guidance.forEach { item ->
            val shortcut = resolveStudyActionAccessibility(item.control, workspaceStrings).shortcutHint

            Text(
                text = "$shortcut ${item.label} — ${item.description}",
                style = MaterialTheme.typography.bodyMedium,
                color = LETheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun StudyRatingButton(
    control: StudyActionControl,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    workspaceStrings: StudyWorkspaceStrings,
    reviewContext: CurrentStudyItemReviewContext?,
    visualLayout: StudyVisualLayout
) {
    val action = resolveStudyActionAccessibility(control, workspaceStrings)
    val variant = resolveStudyRatingVariant(control)
    val isPreviousRating = isPreviousRatingIndicator(
        control,
        reviewContext
    )
    LEButton(
        label = ratingButtonLabel(control, action),
        onClick = onClick,
        enabled = enabled,
        variant = variant,
        showPreviousValueIndicator =
            isPreviousRating && control != StudyActionControl.REVIEW_GOOD,
        supportingLabel =
            if (control == StudyActionControl.REVIEW_GOOD) "Space" else null,
        compact = visualLayout.compactChrome,
        modifier = modifier.height(visualLayout.ratingButtonHeightDp.dp).studyActionSemantics(
            control,
            workspaceStrings,
            previousRating = isPreviousRating
        )
    )
}

internal fun ratingButtonLabel(
    control: StudyActionControl,
    action: StudyActionAccessibility
): String =
    if (control == StudyActionControl.REVIEW_GOOD) {
        "[${action.shortcutHint}]  ${action.visibleLabel}"
    } else {
        "[${action.shortcutHint}]  ${action.visibleLabel}"
    }

@Composable
private fun SchedulerFeedbackCard(
    feedback: StudySchedulerFeedback
) {
    val accessibility = resolveStudySchedulerFeedbackAccessibility(feedback)

    LESurface(
        variant = StudySurfaceRoles.scheduler,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibility.detailsDescription
            }
    ) {
        Column(
            modifier = Modifier.padding(LESpacing.md),
            verticalArrangement = Arrangement.spacedBy(LESpacing.sm)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Scheduler Feedback",
                    style = LETypography.paneTitle,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Latest review decision",
                    style = LETypography.caption,
                    color = LEColors.textMuted
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
            ) {
                SchedulerFeedbackMetric(
                    label = "Rating",
                    value = feedback.rating,
                    modifier = Modifier.weight(1f)
                )

                SchedulerFeedbackMetric(
                    label = "Stage",
                    value = feedback.stageTransition,
                    modifier = Modifier.weight(1f)
                )

                SchedulerFeedbackMetric(
                    label = "Interval",
                    value = feedback.scheduledInterval,
                    modifier = Modifier.weight(1f)
                )
            }

            SchedulerFeedbackRow(
                label = "Next review",
                value = feedback.nextReviewAt
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
            ) {
                SchedulerTransitionMetric(
                    label = "Difficulty",
                    before = feedback.difficultyBefore,
                    after = feedback.difficultyAfter,
                    modifier = Modifier.weight(1f)
                )

                SchedulerTransitionMetric(
                    label = "Stability",
                    before = feedback.stabilityBefore,
                    after = feedback.stabilityAfter,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
            ) {
                SchedulerFeedbackMetric(
                    label = "Review count",
                    value = feedback.reviewCount.toString(),
                    modifier = Modifier.weight(1f)
                )

                SchedulerFeedbackMetric(
                    label = "Lapse count",
                    value = feedback.lapseCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SchedulerFeedbackMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = LETypography.caption,
            color = LEColors.textMuted
        )

        Text(
            text = value,
            style = LETypography.fieldValueEmphasized,
            color = LEColors.primary
        )
    }
}

@Composable
private fun SchedulerTransitionMetric(
    label: String,
    before: String,
    after: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = LETypography.caption,
            color = LEColors.textMuted
        )

        Text(
            text = "$before → $after",
            style = LETypography.fieldValueEmphasized
        )
    }
}

@Composable
private fun SchedulerFeedbackRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = LETypography.caption,
            color = LEColors.textMuted
        )

        Text(
            text = value,
            style = LETypography.fieldValue,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StudyLoadErrorCard(
    presentation: StudyLoadErrorPresentation,
    onRetry: () -> Unit,
    workspaceStrings: StudyWorkspaceStrings
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LETheme.colors.dangerContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "RECOVERABLE STUDY ERROR",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = LETheme.colors.dangerText
            )
            Text(
                text = presentation.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LETheme.colors.dangerText
            )
            Text(
                text = presentation.message,
                style = MaterialTheme.typography.bodyLarge,
                color = LETheme.colors.dangerText
            )
            Text(
                text = presentation.guidance,
                style = MaterialTheme.typography.bodyMedium,
                color = LETheme.colors.dangerText
            )
            LEPrimaryButton(
                text = "${presentation.actionLabel}  [${presentation.shortcutHint}]",
                onClick = onRetry,
                modifier = Modifier.studyActionSemantics(
                    StudyActionControl.RETRY_LOAD,
                    workspaceStrings
                )
            )
        }
    }
}

@Composable
private fun StudyHeader(
    uiState: StudyUiState,
    accessibilityPresentation: StudyAccessibilityPresentation,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
            stateDescription = accessibilityPresentation.statusAnnouncement
        },
        verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
    ) {
        LEStatusBadge(
            variant = StatusBadgeVariant.Present,
            customText = if (uiState.isLessonStudy) "Lesson Study" else "Study"
        )

        Text(
            text = uiState.studyTitle,
            style = LETypography.paneTitle,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = uiState.message,
            style = LETypography.fieldValue,
            color = LEColors.textSecondary
        )
    }
}

@Composable
private fun QuickPresentationControl(
    state: StudyPresentationStagingState,
    onPreferencesChanged: (StudyPresentationPreferences) -> Unit,
    onOpenFullSettings: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var draft by remember(state.next) { mutableStateOf(state.next) }
    val status = resolveStudyPresentationHeaderStatus(state)
    Box {
        TextButton(
            onClick = {
                draft = state.next
                expanded = true
            },
            modifier = Modifier.semantics {
                contentDescription =
                    "Presentation: ${status.label}" +
                        if (status.pending) ". Pending for next item." else ""
            }
        ) {
            Text(
                text = status.label + if (status.pending) " • Next" else " ▼",
                fontWeight = FontWeight.SemiBold
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Text(
                "Presentation",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold
            )
            StudyPresentationControlMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (mode) {
                                StudyPresentationControlMode.ADAPTIVE -> "Adaptive"
                                StudyPresentationControlMode.PREFERENCE_GUIDED -> "Preference Guided"
                                StudyPresentationControlMode.MANUAL -> "Manual"
                            }
                        )
                    },
                    leadingIcon = {
                        RadioButton(
                            selected = draft.controlMode == mode,
                            onClick = null
                        )
                    },
                    onClick = { draft = draft.copy(controlMode = mode) }
                )
            }
            HorizontalDivider()
            if (draft.controlMode != StudyPresentationControlMode.ADAPTIVE) {
                PresentationPreferenceMenuItem("English", draft.showEnglish) {
                    draft = draft.copy(showEnglish = it)
                }
                PresentationPreferenceMenuItem("Vietnamese", draft.showVietnamese) {
                    draft = draft.copy(showVietnamese = it)
                }
                PresentationPreferenceMenuItem("English Autoplay", draft.autoplayEnglish) {
                    draft = draft.copy(autoplayEnglish = it)
                }
                PresentationPreferenceMenuItem("Vietnamese Autoplay", draft.autoplayVietnamese) {
                    draft = draft.copy(autoplayVietnamese = it)
                }
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text("Apply for next item") },
                enabled = draft != state.next,
                onClick = {
                    onPreferencesChanged(draft)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Open Full Settings…") },
                onClick = {
                    expanded = false
                    onOpenFullSettings()
                }
            )
        }
    }
}

@Composable
private fun PresentationPreferenceMenuItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = {
            Checkbox(
                checked = checked,
                onCheckedChange = null
            )
        },
        onClick = { onCheckedChange(!checked) }
    )
}

@Composable
private fun ActiveSessionChrome(
    uiState: StudyUiState,
    accessibilityPresentation: StudyAccessibilityPresentation,
    workspaceStrings: StudyWorkspaceStrings,
    presentationState: StudyPresentationStagingState,
    onPresentationPreferencesChanged: (StudyPresentationPreferences) -> Unit,
    onOpenPresentationSettings: () -> Unit,
    onUndo: () -> Unit,
    onPause: () -> Unit,
    visualLayout: StudyVisualLayout,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val chrome = resolveStudyChromePresentation(maxWidth.value.toInt().coerceAtLeast(1))
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    stateDescription = accessibilityPresentation.statusAnnouncement
                },
            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (chrome.showStudyTitle) {
                    Text(
                        uiState.studyTitle,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = LETypography.paneTitle,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(chrome.horizontalGapDp.dp)) {
                    QuickPresentationControl(
                        state = presentationState,
                        onPreferencesChanged = onPresentationPreferencesChanged,
                        onOpenFullSettings = onOpenPresentationSettings
                    )
                    if (uiState.canUndo) {
                        val undo =
                            resolveStudyActionAccessibility(
                                StudyActionControl.UNDO_LATEST,
                                workspaceStrings
                            )
                        if (chrome.topActionComposition == StudyTopActionComposition.STANDARD_TEXT) {
                            LEButton(
                                label = "↶  ${undo.shortcutHint}",
                                onClick = onUndo,
                                enabled = !uiState.actionInProgress,
                                variant = LEButtonVariant.QUIET,
                                compact = visualLayout.compactChrome,
                                modifier = Modifier
                                    .height(visualLayout.topActionHeightDp.dp)
                                    .studyActionSemantics(
                                        StudyActionControl.UNDO_LATEST,
                                        workspaceStrings
                                    )
                            )
                        } else {
                            StudyChromeIconAction(
                                icon = LEIcons.Undo,
                                tooltip = "${undo.visibleLabel} (${undo.shortcutHint})",
                                onClick = onUndo,
                                enabled = !uiState.actionInProgress,
                                sizeDp = chrome.topActionButtonSizeDp,
                                modifier = Modifier.studyActionSemantics(
                                    StudyActionControl.UNDO_LATEST,
                                    workspaceStrings
                                )
                            )
                        }
                    }
                    val pause =
                        resolveStudyActionAccessibility(
                            StudyActionControl.PAUSE_WORKSPACE,
                            workspaceStrings
                        )
                    if (chrome.topActionComposition == StudyTopActionComposition.STANDARD_TEXT) {
                        LEButton(
                            label = pause.visibleLabel,
                            onClick = onPause,
                            enabled = !uiState.actionInProgress,
                            variant = LEButtonVariant.SECONDARY,
                            compact = visualLayout.compactChrome,
                            modifier = Modifier
                                .height(visualLayout.topActionHeightDp.dp)
                                .studyActionSemantics(
                                    StudyActionControl.PAUSE_WORKSPACE,
                                    workspaceStrings
                                )
                        )
                    } else {
                        StudyChromeIconAction(
                            icon = LEIcons.Pause,
                            tooltip = "${pause.visibleLabel} (${pause.shortcutHint})",
                            onClick = onPause,
                            enabled = !uiState.actionInProgress,
                            sizeDp = chrome.topActionButtonSizeDp,
                            modifier = Modifier.studyActionSemantics(
                                StudyActionControl.PAUSE_WORKSPACE,
                                workspaceStrings
                            )
                        )
                    }
                }
            }
            StudyHeaderStatisticsRow(
                state = uiState.headerStatistics,
                strings = workspaceStrings.statistics
            )
            if (uiState.sessionProgress != null) {
                LinearProgressIndicator(
                    progress = { uiState.progress },
                    modifier = Modifier.fillMaxWidth().semantics {
                        accessibilityPresentation.progressDescription?.let {
                            stateDescription = it
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyChromeIconAction(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit,
    enabled: Boolean,
    sizeDp: Int,
    modifier: Modifier = Modifier
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip, maxLines = 1, softWrap = false) } },
        state = rememberTooltipState()
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.size(sizeDp.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StudyHeaderStatisticsRow(
    state: StudyHeaderStatisticsState,
    strings: StudyStatisticsStrings
) {
    val presentation = resolveStudyHeaderStatisticsPresentation(state, strings)
    if (presentation == null) {
        val message =
            if (state is StudyHeaderStatisticsState.Loading) strings.loading else strings.unavailable
        Text(
            text = message,
            style = LETheme.typography.caption,
            color = LETheme.colors.textMuted,
            modifier = Modifier.heightIn(min = 30.dp).semantics { contentDescription = message }
        )
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        StudyStatisticsDashboard(
            presentation = presentation,
            layout = resolveStudyStatisticsLayout(maxWidth.value.toInt().coerceAtLeast(1)),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LessonProgressCard(
    uiState: StudyUiState,
    accessibilityPresentation: StudyAccessibilityPresentation
) {
    LESurface(
        variant = LESurfaceVariant.SECONDARY,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Session progress"
                accessibilityPresentation.progressDescription?.let { description ->
                    stateDescription = description
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(LESpacing.md),
            verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Session progress",
                    style = LETypography.caption,
                    color = LEColors.textMuted
                )

                Text(
                    text = uiState.progressLabel,
                    style = LETypography.caption,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StudyMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = LERadius.md,
        colors = CardDefaults.cardColors(containerColor = LEColors.surface),
        border = LEBorder.subtle,
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.flat)
    ) {
        Column(
            modifier = Modifier.padding(LESpacing.md),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = LETypography.caption,
                color = LEColors.textMuted
            )

            Text(
                text = value,
                style = LETypography.paneTitle,
                fontWeight = FontWeight.Bold,
                color = LEColors.primary
            )
        }
    }
}
