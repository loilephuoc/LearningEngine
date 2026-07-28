package vn.loi.learning.desktop.ui.study

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
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
import vn.loi.learning.desktop.shortcut.toDesktopKeyChord
import kotlinx.coroutines.delay

@Composable
fun StudyScreen(
    uiState: StudyUiState,
    contentPresenter: LearningContentPresenter,
    contentStrings: LearningContentRendererStrings,
    workspaceStrings: StudyWorkspaceStrings,
    onRefresh: () -> Unit,
    onRefreshHeaderStatistics: () -> Unit = {},
    onStartStudy: () -> Unit,
    onRevealAnswer: () -> Unit,
    onCompleteFlowStage: () -> Unit = onRevealAnswer,
    onShowDecisionExplanation: () -> Unit,
    onHideDecisionExplanation: () -> Unit,
    onCompleteAdaptiveSession: () -> Unit,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
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
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val accessibilityPresentation = resolveStudyAccessibilityPresentation(uiState)
    val workspacePresentation = resolveFocusedStudyWorkspace(uiState.hasActiveSession)
    val focusTransitionKey = resolveStudyFocusTransitionKey(uiState)
    val contentPresentation = remember(uiState.learningContent, contentPresenter) {
        contentPresenter.presentAvailable(uiState.learningContent)
    }
    val experiencePlan = uiState.learningExperiencePlan
    var typingState by remember(uiState.currentLearningItemId, uiState.learningFlowCurrentStage?.id) {
        mutableStateOf(TypingRecallInteraction.initial(uiState.currentLearningItemId))
    }
    var typingInputFocused by remember(uiState.currentLearningItemId) {
        mutableStateOf(false)
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

    LaunchedEffect(focusTransitionKey) {
        focusRequester.requestFocus()
    }

    fun performKeyboardAction(action: StudyKeyboardAction) {
        when (action) {
            StudyKeyboardAction.RETRY_LOAD -> onRefresh()
            StudyKeyboardAction.START_STUDY -> onStartStudy()
            StudyKeyboardAction.REVEAL_ANSWER -> onCompleteFlowStage()
            StudyKeyboardAction.REVIEW_AGAIN -> onAgain()
            StudyKeyboardAction.REVIEW_HARD -> onHard()
            StudyKeyboardAction.REVIEW_GOOD -> onGood()
            StudyKeyboardAction.REVIEW_EASY -> onEasy()
            StudyKeyboardAction.REPLAY_PRIMARY_AUDIO,
            StudyKeyboardAction.TOGGLE_VOCABULARY_AUDIO_LOOP,
            StudyKeyboardAction.TOGGLE_EXAMPLE_AUDIO_LOOP,
            StudyKeyboardAction.PLAY_VIETNAMESE_MEANING_AUDIO,
            StudyKeyboardAction.PLAY_VIETNAMESE_EXAMPLE_AUDIO ->
                performStudyAudioKeyboardAction(action, shortcutAudioPaths, audioController)
            StudyKeyboardAction.UNDO_LATEST -> onUndo()
            StudyKeyboardAction.PAUSE_WORKSPACE -> {
                audioController.stop()
                onPause()
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

                val action = event.toDesktopKeyChord()?.let { chord ->
                    resolveStudyKeyboardAction(
                        uiState,
                        StudyKeyboardInput(
                            chord = chord,
                            textInputFocused = typingInputFocused
                        ),
                        shortcutRegistry
                    )
                }

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
                onUndo = onUndo,
                onPause = onPause,
                visualLayout = visualLayout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LESpacing.lg, vertical = LESpacing.sm)
            )

            // Scrollable Main Body (LearningWorkspaceSurface + SecondaryWorkspace)
            Column(
                modifier = Modifier
                    .weight(1f)
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
                        onCompleteFlowStage = onCompleteFlowStage,
                        onAgain = onAgain,
                        onHard = onHard,
                        onGood = onGood,
                        onEasy = onEasy,
                        typingState = typingState,
                        onTypingInputChanged = { input ->
                            typingState = TypingRecallInteraction.updateInput(typingState, input)
                        },
                        onTypingSubmit = {
                            val typingScene = learningScene as? TypingScene
                            if (typingScene != null) {
                                val outcome = TypingRecallInteraction.submit(
                                    typingState,
                                    typingScene.prompt,
                                    TypingAnswerEvaluator(),
                                    actionInProgress = uiState.actionInProgress
                                )
                                if (outcome != null) {
                                    typingState = outcome.state
                                    if (outcome.shouldRevealAnswer) {
                                        onCompleteFlowStage()
                                    }
                                }
                            }
                        },
                        onTypingFocusChanged = { focused -> typingInputFocused = focused },
                        workspaceStrings = workspaceStrings,
                        visualLayout = visualLayout
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
                    onShowDecisionExplanation = onShowDecisionExplanation,
                    onHideDecisionExplanation = onHideDecisionExplanation,
                    onCompleteAdaptiveSession = onCompleteAdaptiveSession,
                    onBackToLesson = onBackToLesson,
                    onBackToLibrary = onBackToLibrary,
                    onContinueLearning = onContinueLearning
                )
            }

            // 4. ActionDock (Fixed Action Bar at bottom)
            ActionDock(
                uiState = uiState,
                learningScene = learningScene,
                contentStrings = contentStrings,
                workspaceStrings = workspaceStrings,
                onStartStudy = onStartStudy,
                onCompleteFlowStage = onCompleteFlowStage,
                onAgain = onAgain,
                onHard = onHard,
                onGood = onGood,
                onEasy = onEasy,
                onBackToLibrary = onBackToLibrary,
                visualLayout = visualLayout
            )

            // 5. StatusStrip (Fixed Bottom Status Bar)
            StatusStrip(uiState = uiState, shortcutRegistry = shortcutRegistry)
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
    onTypingInputChanged: (String) -> Unit,
    onTypingSubmit: () -> Unit,
    onTypingFocusChanged: (Boolean) -> Unit,
    workspaceStrings: StudyWorkspaceStrings,
    visualLayout: StudyVisualLayout? = null,
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
        onTypingInputChanged = onTypingInputChanged,
        onTypingSubmit = onTypingSubmit,
        onTypingFocusChanged = onTypingFocusChanged,
        workspaceStrings = workspaceStrings,
        visualLayout = visualLayout,
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
    onShowDecisionExplanation: () -> Unit,
    onHideDecisionExplanation: () -> Unit,
    onCompleteAdaptiveSession: () -> Unit,
    onBackToLesson: ((vn.loi.learning.domain.library.model.InstalledPackageId, vn.loi.learning.domain.content.model.ContentId) -> Unit)?,
    onBackToLibrary: (() -> Unit)?,
    onContinueLearning: ((vn.loi.learning.domain.library.model.InstalledPackageId, vn.loi.learning.domain.content.model.ContentId) -> Unit)?,
    modifier: Modifier = Modifier
) {
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
        } else if (uiState.sessionCompleted) {
            val completionState = remember(uiState) {
                SessionCompletionProjectionPolicy.create(uiState)
            }
            SessionCompletionCard(
                completionUiState = completionState,
                onBackToLesson = onBackToLesson,
                onBackToLibrary = onBackToLibrary,
                onContinueLearning = onContinueLearning,
                onContinueGeneralStudy = onStartStudy
            )
        } else {
            val idlePresentation = resolveStudyIdlePresentation(uiState)
            if (idlePresentation != null) {
                StudyIdleCard(
                    presentation = idlePresentation,
                    onStartStudy = onStartStudy,
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
    onBackToLibrary: (() -> Unit)? = null,
    visualLayout: StudyVisualLayout,
    modifier: Modifier = Modifier
) {
    if (uiState.sessionCompleted || uiState.loadError != null) return

    val showDock = uiState.contentIntroductionState == ContentIntroductionState.REQUIRED ||
        (uiState.canReview && uiState.learningFlowProgress?.isRatingReady == true) ||
        (uiState.canRevealAnswer && uiState.learningFlowCurrentStage is LearningFlowStage.AnswerReveal) ||
        (uiState.canRevealAnswer && uiState.learningFlowCurrentStage is LearningFlowStage.Experience &&
            uiState.learningFlowCurrentStage.selection.selectedKind != LearningExperienceKind.TYPING_RECALL) ||
        (!uiState.hasActiveSession && resolveStudyIdlePresentation(uiState) != null)

    if (!showDock) return

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
                uiState.contentIntroductionState == ContentIntroductionState.REQUIRED -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                    ) {
                        ReadOnlyRatingContextDock(
                            reviewContext = uiState.currentItemReviewContext,
                            workspaceStrings = workspaceStrings,
                            visualLayout = visualLayout
                        )
                        LEPrimaryButton(
                            text = "${contentStrings.nextFlowStage}  [Space]",
                            onClick = onCompleteFlowStage,
                            enabled = !uiState.actionInProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                uiState.canReview && uiState.learningFlowProgress?.isRatingReady == true -> {
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
                uiState.canRevealAnswer &&
                    (uiState.learningFlowCurrentStage is LearningFlowStage.AnswerReveal ||
                        (uiState.learningFlowCurrentStage is LearningFlowStage.Experience &&
                            uiState.learningFlowCurrentStage.selection.selectedKind != LearningExperienceKind.TYPING_RECALL)) -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(LESpacing.xs)
                    ) {
                        ReadOnlyRatingContextDock(
                            reviewContext = uiState.currentItemReviewContext,
                            workspaceStrings = workspaceStrings,
                            visualLayout = visualLayout
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
                !uiState.hasActiveSession && resolveStudyIdlePresentation(uiState) != null -> {
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
    visualLayout: StudyVisualLayout
) {
    val segments = resolveRatingDockPresentation(RatingDockMode.QUESTION_CONTEXT, reviewContext)
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
                    val container =
                        if (segment.isPreviousRating) {
                            when (segment.control) {
                                StudyActionControl.REVIEW_AGAIN -> colors.dangerContainer
                                StudyActionControl.REVIEW_HARD -> colors.warningContainer
                                StudyActionControl.REVIEW_GOOD -> colors.successContainer
                                StudyActionControl.REVIEW_EASY -> colors.infoContainer
                                else -> colors.surfaceSecondary
                            }
                        } else {
                            colors.surfaceSecondary
                        }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(visualLayout.frontRatingSegmentHeightDp.dp)
                            .background(container, LETheme.shapes.radiusM)
                            .border(
                                1.dp,
                                if (segment.isPreviousRating) colors.borderFocus else colors.borderSubtle,
                                LETheme.shapes.radiusM
                            )
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
    modifier: Modifier = Modifier
) {
    val presentation = resolveStudyShortcutStatus(
        ratingReady = uiState.learningFlowProgress?.isRatingReady == true,
        registry = shortcutRegistry
    )
    Surface(
        color = LEColors.surface,
        border = LEBorder.subtle,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LESpacing.lg, vertical = LESpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LESpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shortcuts:",
                    style = LETypography.caption,
                    color = LEColors.textMuted
                )
                Text(
                    text = presentation.text,
                    style = LETypography.caption,
                    color = LEColors.textSecondary
                )
            }

            LEStatusBadge(
                variant = if (uiState.hasActiveSession) StatusBadgeVariant.Present else StatusBadgeVariant.NotEvaluated,
                customText = if (uiState.hasActiveSession) "Active Session" else "Idle"
            )
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
    onStartStudy: () -> Unit,
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

            LEPrimaryButton(
                text = "${presentation.actionLabel}  [${presentation.shortcutHint}]",
                onClick = onStartStudy,
                modifier = Modifier.studyActionSemantics(StudyActionControl.START_STUDY, workspaceStrings)
            )
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
    onTypingInputChanged: (String) -> Unit,
    onTypingSubmit: () -> Unit,
    onTypingFocusChanged: (Boolean) -> Unit,
    workspaceStrings: StudyWorkspaceStrings,
    visualLayout: StudyVisualLayout? = null,
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
                visualLayout?.heightMode == StudyHeightMode.COMFORTABLE
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
                        viewportWidthDp = visualLayout?.contentMaxWidthDp ?: 0
                    )
                }
                FocusedAnswerSurface(
                    model = answerModel,
                    disclosure = FullAnswerPresentation.resolve(answerModel),
                    strings = contentStrings,
                    audioController = audioController,
                    schedulerFeedback = uiState.schedulerFeedback,
                    typography = typography,
                    layout = visualLayout,
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
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = contentAccessibility.promptDescription
                    }
                )
            }

            if (learningScene is TypingScene && uiState.canRevealAnswer) {
                TypingRecallInput(
                    state = typingState,
                    strings = contentStrings,
                    enabled = !uiState.actionInProgress,
                    onInputChanged = onTypingInputChanged,
                    onSubmit = onTypingSubmit,
                    onFocusChanged = onTypingFocusChanged
                )
            }

            typingState.evaluation?.let { evaluation ->
                val feedback = when (evaluation.status) {
                    TypingAnswerEvaluationStatus.CORRECT -> contentStrings.typingCorrect
                    TypingAnswerEvaluationStatus.INCORRECT -> contentStrings.typingIncorrect
                    TypingAnswerEvaluationStatus.EMPTY -> contentStrings.typingEmpty
                }
                Text(
                    text = feedback,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (evaluation.isCorrect) LETheme.colors.success else LETheme.colors.textSecondary,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = feedback
                    }
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
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        requester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.input,
            onValueChange = onInputChanged,
            label = { Text(strings.typingInputLabel) },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(requester)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                        onSubmit()
                        true
                    } else {
                        false
                    }
                }
        )
        LEPrimaryButton(
            text = strings.typingSubmit,
            onClick = onSubmit,
            enabled = enabled,
            modifier = Modifier.semantics {
                contentDescription = strings.typingSubmit
            }
        )
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
        label = "[${action.shortcutHint}]  ${action.visibleLabel}",
        onClick = onClick,
        enabled = enabled,
        variant = variant,
        showPreviousValueIndicator = isPreviousRating,
        compact = visualLayout.compactChrome,
        modifier = modifier.height(visualLayout.ratingButtonHeightDp.dp).studyActionSemantics(
            control,
            workspaceStrings,
            previousRating = isPreviousRating
        )
    )
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
    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).semantics(mergeDescendants = true) {
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(uiState.studyTitle, style = LETypography.paneTitle, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                QuickPresentationControl(
                    state = presentationState,
                    onPreferencesChanged = onPresentationPreferencesChanged,
                    onOpenFullSettings = onOpenPresentationSettings
                )
                if (uiState.canUndo) {
                    val undo = resolveStudyActionAccessibility(StudyActionControl.UNDO_LATEST, workspaceStrings)
                    LEButton(
                        label = "↶  ${undo.shortcutHint}",
                        onClick = onUndo,
                        enabled = !uiState.actionInProgress,
                        variant = LEButtonVariant.QUIET,
                        compact = visualLayout.compactChrome,
                        modifier = Modifier
                            .height(visualLayout.topActionHeightDp.dp)
                            .studyActionSemantics(StudyActionControl.UNDO_LATEST, workspaceStrings)
                    )
                }
                val pause = resolveStudyActionAccessibility(StudyActionControl.PAUSE_WORKSPACE, workspaceStrings)
                LEButton(
                    label = "Tạm dừng",
                    onClick = onPause,
                    enabled = !uiState.actionInProgress,
                    variant = LEButtonVariant.SECONDARY,
                    compact = visualLayout.compactChrome,
                    modifier = Modifier
                        .height(visualLayout.topActionHeightDp.dp)
                        .studyActionSemantics(StudyActionControl.PAUSE_WORKSPACE, workspaceStrings)
                )
            }
        }
        StudyHeaderStatisticsRow(
            state = uiState.headerStatistics,
            strings = workspaceStrings.statistics,
            viewportClass = visualLayout.viewportClass
        )
        if (uiState.sessionProgress != null) {
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth().semantics {
                    accessibilityPresentation.progressDescription?.let { stateDescription = it }
                }
            )
        }
    }
}

@Composable
private fun StudyHeaderStatisticsRow(
    state: StudyHeaderStatisticsState,
    strings: StudyStatisticsStrings,
    viewportClass: StudyViewportClass
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
    StudyStatisticsDashboard(
        presentation = presentation,
        layout = resolveStudyStatisticsLayout(viewportClass),
        modifier = Modifier.fillMaxWidth()
    )
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
