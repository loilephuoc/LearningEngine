package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.focusable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import vn.loi.learning.application.decision.DecisionExplanation
import vn.loi.learning.application.learningexperience.LearningExperienceKind
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluationStatus
import vn.loi.learning.application.learningexperience.TypingAnswerEvaluator
import vn.loi.learning.application.learningflow.LearningFlowStage
import vn.loi.learning.desktop.ui.designsystem.*
import vn.loi.learning.desktop.ui.designsystem.components.*

@Composable
fun StudyScreen(
    uiState: StudyUiState,
    contentPresenter: LearningContentPresenter,
    contentStrings: LearningContentRendererStrings,
    workspaceStrings: StudyWorkspaceStrings,
    onRefresh: () -> Unit,
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
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val accessibilityPresentation = resolveStudyAccessibilityPresentation(uiState)
    val workspacePresentation = resolveFocusedStudyWorkspace(uiState.hasActiveSession)
    val focusTransitionKey = resolveStudyFocusTransitionKey(uiState)
    val contentPresentation = remember(uiState.learningContent, uiState.workspaceState, contentPresenter) {
        contentPresenter.present(uiState.learningContent, uiState.workspaceState)
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
    val audioController = remember {
        LearningContentAudioController(JavaSoundLearningContentAudioPlayer())
    }

    LaunchedEffect(learningScene) {
        audioController.bind(learningScene)
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
            StudyKeyboardAction.REPLAY_PRIMARY_AUDIO -> audioController.replayPrimary()
            StudyKeyboardAction.UNDO_LATEST -> onUndo()
            StudyKeyboardAction.PAUSE_WORKSPACE -> {
                audioController.stop()
                onPause()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false

                val shortcutKey = when (event.key) {
                    Key.Enter, Key.NumPadEnter -> StudyKeyboardKey.ENTER
                    Key.Spacebar -> StudyKeyboardKey.SPACE
                    Key.One, Key.NumPad1 -> StudyKeyboardKey.ONE
                    Key.Two, Key.NumPad2 -> StudyKeyboardKey.TWO
                    Key.Three, Key.NumPad3 -> StudyKeyboardKey.THREE
                    Key.Four, Key.NumPad4 -> StudyKeyboardKey.FOUR
                    Key.R -> StudyKeyboardKey.R
                    Key.Z -> StudyKeyboardKey.Z
                    Key.Escape -> StudyKeyboardKey.ESCAPE
                    else -> null
                }

                val action = shortcutKey?.let { key ->
                    resolveStudyKeyboardAction(
                        uiState,
                        StudyKeyboardInput(
                            key = key,
                            controlPressed = event.isCtrlPressed,
                            textInputFocused = typingInputFocused
                        )
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
                onUndo = onUndo,
                onPause = onPause,
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
                        contentStrings = contentStrings,
                        audioController = audioController,
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
                        workspaceStrings = workspaceStrings
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
                onEasy = onEasy
            )

            // 5. StatusStrip (Fixed Bottom Status Bar)
            StatusStrip(uiState = uiState)
        }
    }
}

/** 1. SessionHeader Composable */
@Composable
private fun SessionHeader(
    uiState: StudyUiState,
    accessibilityPresentation: StudyAccessibilityPresentation,
    workspaceStrings: StudyWorkspaceStrings,
    onUndo: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.hasActiveSession) {
        ActiveSessionChrome(
            uiState = uiState,
            accessibilityPresentation = accessibilityPresentation,
            workspaceStrings = workspaceStrings,
            onUndo = onUndo,
            onPause = onPause,
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
    contentStrings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
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
    modifier: Modifier = Modifier
) {
    StudyItemCard(
        uiState = uiState,
        learningScene = learningScene,
        contentStrings = contentStrings,
        audioController = audioController,
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
                onStartStudy = onStartStudy
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

        uiState.schedulerFeedback?.let { feedback ->
            SchedulerFeedbackCard(feedback = feedback)
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
    modifier: Modifier = Modifier
) {
    if (uiState.sessionCompleted || uiState.loadError != null) return

    val showDock = (uiState.canReview && uiState.learningFlowProgress?.isRatingReady == true) ||
        (uiState.canRevealAnswer && uiState.learningFlowCurrentStage is LearningFlowStage.AnswerReveal) ||
        (uiState.canRevealAnswer && uiState.learningFlowCurrentStage is LearningFlowStage.Experience &&
            uiState.learningFlowCurrentStage.selection.selectedKind != LearningExperienceKind.TYPING_RECALL) ||
        (!uiState.hasActiveSession && resolveStudyIdlePresentation(uiState) != null)

    if (!showDock) return

    Surface(
        color = LEColors.surfaceElevated,
        tonalElevation = LEElevation.card,
        border = LEBorder.subtle,
        shape = LERadius.md,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LESpacing.lg, vertical = LESpacing.xs)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LESpacing.md, vertical = LESpacing.sm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                uiState.canReview && uiState.learningFlowProgress?.isRatingReady == true -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                    ) {
                        StudyRatingButton(
                            control = StudyActionControl.REVIEW_AGAIN,
                            onClick = onAgain,
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.actionInProgress,
                            workspaceStrings = workspaceStrings
                        )
                        StudyRatingButton(
                            control = StudyActionControl.REVIEW_HARD,
                            onClick = onHard,
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.actionInProgress,
                            workspaceStrings = workspaceStrings
                        )
                        StudyRatingButton(
                            control = StudyActionControl.REVIEW_GOOD,
                            onClick = onGood,
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.actionInProgress,
                            workspaceStrings = workspaceStrings
                        )
                        StudyRatingButton(
                            control = StudyActionControl.REVIEW_EASY,
                            onClick = onEasy,
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.actionInProgress,
                            workspaceStrings = workspaceStrings
                        )
                    }
                }
                uiState.canRevealAnswer && uiState.learningFlowCurrentStage is LearningFlowStage.AnswerReveal -> {
                    LEPrimaryButton(
                        text = "${contentStrings.flowRetryReveal}  [Space]",
                        onClick = onCompleteFlowStage,
                        enabled = !uiState.actionInProgress
                    )
                }
                uiState.canRevealAnswer && uiState.learningFlowCurrentStage is LearningFlowStage.Experience &&
                    uiState.learningFlowCurrentStage.selection.selectedKind != LearningExperienceKind.TYPING_RECALL -> {
                    LEPrimaryButton(
                        text = "${contentStrings.nextFlowStage}  [Space]",
                        onClick = onCompleteFlowStage,
                        enabled = !uiState.actionInProgress
                    )
                }
                !uiState.hasActiveSession && resolveStudyIdlePresentation(uiState) != null -> {
                    val idle = resolveStudyIdlePresentation(uiState)!!
                    LEPrimaryButton(
                        text = "${idle.actionLabel}  [${idle.shortcutHint}]",
                        onClick = onStartStudy,
                        enabled = !uiState.actionInProgress
                    )
                }
            }
        }
    }
}

/** 5. StatusStrip Composable (Fixed Status Strip) */
@Composable
private fun StatusStrip(
    uiState: StudyUiState,
    modifier: Modifier = Modifier
) {
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
                    text = if (uiState.learningFlowProgress?.isRatingReady == true) "[1] Again  [2] Hard  [3] Good  [4] Easy  [R] Replay  [Z] Undo"
                    else "[Space] Reveal/Next  [R] Replay  [Z] Undo  [Esc] Pause",
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = if (visible) "Why Product Brain made this decision" else "Decision explanation hidden"
            },
        shape = LERadius.lg,
        colors = CardDefaults.cardColors(containerColor = LEColors.surfaceElevated)
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
    strings: StudyWorkspaceStrings = StudyWorkspaceStrings.ENGLISH
): Modifier {
    val presentation = resolveStudyActionAccessibility(control, strings)
    return semantics {
        contentDescription = presentation.contentDescription
    }
}

@Composable
private fun SessionSummaryCard(
    uiState: StudyUiState,
    onStartStudy: () -> Unit,
    workspaceStrings: StudyWorkspaceStrings
) {
    val accessibility = resolveStudySessionSummaryAccessibility(uiState)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibility.contentDescription
            },
        shape = LERadius.lg,
        colors = CardDefaults.cardColors(containerColor = LEColors.surfaceElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.card)
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
                    append(" learning item")
                    if (uiState.reviewedCount != 1) append("s")
                    append(" reviewed")
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            uiState.sessionProgress
                ?.takeIf { progress -> progress.skippedItemCount > 0 }
                ?.let { progress ->
                    Text(
                        text = "${progress.skippedItemCount} planned item" +
                            if (progress.skippedItemCount == 1) " was not reviewed" else "s were not reviewed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StudyIdleCard(
    presentation: StudyIdlePresentation,
    onStartStudy: () -> Unit,
    workspaceStrings: StudyWorkspaceStrings
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = LERadius.lg,
        colors = CardDefaults.cardColors(containerColor = LEColors.surfaceElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.card)
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
    contentStrings: LearningContentRendererStrings,
    audioController: LearningContentAudioController,
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
    modifier: Modifier = Modifier
) {
    val contentAccessibility = resolveStudyContentAccessibility(uiState)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = LERadius.lg,
        colors = CardDefaults.cardColors(containerColor = LEColors.surface),
        border = LEBorder.subtle,
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.card)
    ) {
        Column(
            modifier = Modifier.padding(LESpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LESpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.hasActiveSession) {
                LEStatusBadge(
                    variant = if (uiState.message == "New learning item") StatusBadgeVariant.Present else StatusBadgeVariant.NotEvaluated,
                    customText = if (uiState.message == "New learning item") "NEW" else "REVIEW"
                )
            }

            FlowProgressIndicator(uiState, contentStrings)

            if (learningScene == null) {
                Text(uiState.contentText, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            } else {
                LearningSceneRenderer(
                    scene = learningScene,
                    strings = contentStrings,
                    audioController = audioController,
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
                    color = if (evaluation.isCorrect) LEColors.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = feedback
                    }
                )
            }
        }
    }
}

@Composable
private fun FlowProgressIndicator(
    uiState: StudyUiState,
    strings: LearningContentRendererStrings
) {
    val progress = uiState.learningFlowProgress ?: return
    val stage = uiState.learningFlowCurrentStage ?: return
    val label = when (stage) {
        is LearningFlowStage.Experience -> when (stage.selection.selectedKind) {
            LearningExperienceKind.IMAGE_RECALL -> strings.flowImageRecall
            LearningExperienceKind.LISTENING_RECALL -> strings.flowListeningRecall
            LearningExperienceKind.PROMPT_RECALL -> strings.flowPromptRecall
            LearningExperienceKind.TYPING_RECALL -> strings.flowTypingRecall
        }

        is LearningFlowStage.AnswerReveal -> strings.flowPreparingAnswer
        is LearningFlowStage.RatingReady -> strings.flowAnswerReady
    }
    val text = progress.currentExperienceNumber?.let { number ->
        strings.flowStageTemplate(number, progress.totalExperienceCount, label)
    } ?: label
    Text(
        text = text,
        style = LETypography.caption,
        color = LEColors.primary,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    workspaceStrings: StudyWorkspaceStrings
) {
    val action = resolveStudyActionAccessibility(control, workspaceStrings)
    LESecondaryButton(
        text = "${action.visibleLabel}  [${action.shortcutHint}]",
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.studyActionSemantics(control, workspaceStrings)
    )
}

@Composable
private fun SchedulerFeedbackCard(
    feedback: StudySchedulerFeedback
) {
    val accessibility = resolveStudySchedulerFeedbackAccessibility(feedback)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibility.detailsDescription
            },
        shape = LERadius.lg,
        colors = CardDefaults.cardColors(containerColor = LEColors.surfaceElevated),
        border = LEBorder.subtle,
        elevation = CardDefaults.cardElevation(defaultElevation = LEElevation.flat)
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
            containerColor = MaterialTheme.colorScheme.errorContainer
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
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = presentation.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = presentation.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = presentation.guidance,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
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
private fun ActiveSessionChrome(
    uiState: StudyUiState,
    accessibilityPresentation: StudyAccessibilityPresentation,
    workspaceStrings: StudyWorkspaceStrings,
    onUndo: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {
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
                Text(uiState.progressLabel, style = LETypography.caption, color = LEColors.textMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)) {
                if (uiState.canUndo) {
                    val undo = resolveStudyActionAccessibility(StudyActionControl.UNDO_LATEST, workspaceStrings)
                    LESecondaryButton(
                        text = "${undo.visibleLabel} [${undo.shortcutHint}]",
                        onClick = onUndo,
                        enabled = !uiState.actionInProgress,
                        modifier = Modifier.studyActionSemantics(StudyActionControl.UNDO_LATEST, workspaceStrings)
                    )
                }
                val pause = resolveStudyActionAccessibility(StudyActionControl.PAUSE_WORKSPACE, workspaceStrings)
                LESecondaryButton(
                    text = "${pause.visibleLabel} [${pause.shortcutHint}]",
                    onClick = onPause,
                    enabled = !uiState.actionInProgress,
                    modifier = Modifier.studyActionSemantics(StudyActionControl.PAUSE_WORKSPACE, workspaceStrings)
                )
            }
        }
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
private fun LessonProgressCard(
    uiState: StudyUiState,
    accessibilityPresentation: StudyAccessibilityPresentation
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Session progress"
                accessibilityPresentation.progressDescription?.let { description ->
                    stateDescription = description
                }
            },
        shape = LERadius.md,
        colors = CardDefaults.cardColors(containerColor = LEColors.surfaceElevated),
        border = LEBorder.subtle
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
