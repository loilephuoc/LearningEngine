package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StudyScreen(
    uiState: StudyUiState,
    contentPresenter: LearningContentPresenter,
    contentStrings: LearningContentRendererStrings,
    workspaceStrings: StudyWorkspaceStrings,
    onRefresh: () -> Unit,
    onStartStudy: () -> Unit,
    onRevealAnswer: () -> Unit,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onEasy: () -> Unit,
    onUndo: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester =
        remember {
            FocusRequester()
        }
    val accessibilityPresentation =
        resolveStudyAccessibilityPresentation(uiState)
    val workspacePresentation = resolveFocusedStudyWorkspace(uiState.hasActiveSession)
    val focusTransitionKey =
        resolveStudyFocusTransitionKey(uiState)
    val contentPresentation = remember(uiState.learningContent, uiState.workspaceState, contentPresenter) {
        contentPresenter.present(uiState.learningContent, uiState.workspaceState)
    }
    val sceneFactory = remember { LearningSceneFactory() }
    val learningScene = remember(contentPresentation, uiState.workspaceState) {
        sceneFactory.generate(contentPresentation, uiState.workspaceState)
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

    fun performKeyboardAction(
        action: StudyKeyboardAction
    ) {
        when (action) {
            StudyKeyboardAction.RETRY_LOAD ->
                onRefresh()

            StudyKeyboardAction.START_STUDY ->
                onStartStudy()

            StudyKeyboardAction.REVEAL_ANSWER ->
                onRevealAnswer()

            StudyKeyboardAction.REVIEW_AGAIN ->
                onAgain()

            StudyKeyboardAction.REVIEW_HARD ->
                onHard()

            StudyKeyboardAction.REVIEW_GOOD ->
                onGood()

            StudyKeyboardAction.REVIEW_EASY ->
                onEasy()

            StudyKeyboardAction.REPLAY_PRIMARY_AUDIO ->
                audioController.replayPrimary()

            StudyKeyboardAction.UNDO_LATEST ->
                onUndo()

            StudyKeyboardAction.PAUSE_WORKSPACE ->
                run {
                    audioController.stop()
                    onPause()
                }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (
                        event.type !=
                        KeyEventType.KeyDown
                    ) {
                        return@onKeyEvent false
                    }

                    val shortcutKey =
                        when (event.key) {
                            Key.Enter,
                            Key.NumPadEnter ->
                                StudyKeyboardKey.ENTER

                            Key.Spacebar ->
                                StudyKeyboardKey.SPACE

                            Key.One,
                            Key.NumPad1 ->
                                StudyKeyboardKey.ONE

                            Key.Two,
                            Key.NumPad2 ->
                                StudyKeyboardKey.TWO

                            Key.Three,
                            Key.NumPad3 ->
                                StudyKeyboardKey.THREE

                            Key.Four,
                            Key.NumPad4 ->
                                StudyKeyboardKey.FOUR

                            Key.R -> StudyKeyboardKey.R

                            Key.Z -> StudyKeyboardKey.Z

                            Key.Escape -> StudyKeyboardKey.ESCAPE

                            else -> null
                        }

                    val action =
                        shortcutKey?.let { key ->
                            resolveStudyKeyboardAction(
                                uiState,
                                StudyKeyboardInput(key = key, controlPressed = event.isCtrlPressed)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        if (uiState.hasActiveSession) {
            ActiveSessionChrome(uiState, accessibilityPresentation, workspaceStrings, onUndo, onPause)
        } else {
            StudyHeader(
                uiState = uiState,
                accessibilityPresentation = accessibilityPresentation
            )
        }

        resolveStudyLoadErrorPresentation(uiState)
            ?.let { presentation ->
                StudyLoadErrorCard(
                    presentation = presentation,
                    onRetry = onRefresh,
                    workspaceStrings = workspaceStrings
                )
            }

        if (workspacePresentation.showDashboardMetrics) Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            StudyMetricCard(
                label = "Reviewed",
                value =
                    uiState
                        .reviewedCount
                        .toString(),
                modifier =
                    Modifier.weight(1f)
            )

            StudyMetricCard(
                label = "New items",
                value =
                    uiState
                        .newItemsReviewed
                        .toString(),
                modifier =
                    Modifier.weight(1f)
            )

            StudyMetricCard(
                label = "Review items",
                value =
                    uiState
                        .reviewItemsReviewed
                        .toString(),
                modifier =
                    Modifier.weight(1f)
            )
        }

        if (uiState.loadError != null) {
            // Preserve the last good study state while recovery guidance is shown.
        } else if (uiState.sessionCompleted) {
            SessionSummaryCard(
                uiState = uiState,
                onStartStudy = onStartStudy,
                workspaceStrings = workspaceStrings
            )
        } else {
            val idlePresentation =
                resolveStudyIdlePresentation(uiState)

            if (idlePresentation != null) {
                StudyIdleCard(
                    presentation = idlePresentation,
                    onStartStudy = onStartStudy,
                    workspaceStrings = workspaceStrings
                )
            } else {
                StudyItemCard(
                    uiState = uiState,
                    learningScene = learningScene,
                    contentStrings = contentStrings,
                    audioController = audioController,
                    onRevealAnswer = onRevealAnswer,
                    onAgain = onAgain,
                    onHard = onHard,
                    onGood = onGood,
                    onEasy = onEasy,
                    workspaceStrings = workspaceStrings
                )
            }
        }

        uiState
            .schedulerFeedback
            ?.let { feedback ->
                SchedulerFeedbackCard(
                    feedback = feedback
                )
            }
        }
    }
}

private fun Modifier.studyActionSemantics(
    control: StudyActionControl,
    strings: StudyWorkspaceStrings = StudyWorkspaceStrings.ENGLISH
): Modifier {
    val presentation =
        resolveStudyActionAccessibility(control, strings)

    return semantics {
        contentDescription =
            presentation.contentDescription
    }
}

@Composable
private fun SessionSummaryCard(
    uiState: StudyUiState,
    onStartStudy: () -> Unit,
    workspaceStrings: StudyWorkspaceStrings
) {
    val accessibility =
        resolveStudySessionSummaryAccessibility(uiState)

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription =
                        accessibility.contentDescription
                },
        shape =
            RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainer
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {
        Column(
            modifier =
                Modifier.padding(28.dp),
            verticalArrangement =
                Arrangement.spacedBy(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = "SESSION COMPLETED",
                style =
                    MaterialTheme
                        .typography
                        .labelLarge,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text = uiState.studyTitle,
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    buildString {
                        append(
                            uiState.reviewedCount
                        )
                        append(" learning item")

                        if (
                            uiState.reviewedCount != 1
                        ) {
                            append("s")
                        }

                        append(" reviewed")
                    },
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
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

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {
                SessionSummaryMetric(
                    label = "Total reviews",
                    value =
                        uiState
                            .reviewedCount
                            .toString(),
                    modifier =
                        Modifier.weight(1f)
                )

                SessionSummaryMetric(
                    label = "New",
                    value =
                        uiState
                            .newItemsReviewed
                            .toString(),
                    modifier =
                        Modifier.weight(1f)
                )

                SessionSummaryMetric(
                    label = "Scheduled",
                    value =
                        uiState
                            .reviewItemsReviewed
                            .toString(),
                    modifier =
                        Modifier.weight(1f)
                )
            }

            if (
                uiState.isLessonStudy &&
                uiState.hasKnownTotal
            ) {
                Text(
                    text =
                        "${uiState.reviewedCount} of " +
                                "${uiState.totalItems} lesson items completed",
                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            val action =
                resolveStudyActionAccessibility(
                    StudyActionControl.START_GENERAL_STUDY,
                    workspaceStrings
                )

            Button(
                onClick = onStartStudy,
                modifier =
                    Modifier.studyActionSemantics(
                        StudyActionControl.START_GENERAL_STUDY,
                        workspaceStrings
                    )
            ) {
                Text(
                    "${action.visibleLabel}  [${action.shortcutHint}]"
                )
            }
        }
    }
}

@Composable
private fun SessionSummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement =
            Arrangement.spacedBy(6.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style =
                MaterialTheme
                    .typography
                    .headlineMedium,
            fontWeight =
                FontWeight.Bold,
            color =
                MaterialTheme
                    .colorScheme
                    .primary
        )

        Text(
            text = label,
            style =
                MaterialTheme
                    .typography
                    .labelLarge,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
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
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainer
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = presentation.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = presentation.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onStartStudy,
                modifier =
                    Modifier.studyActionSemantics(
                        StudyActionControl.START_STUDY,
                        workspaceStrings
                    )
            ) {
                Text(
                    presentation.actionLabel +
                        "  [" +
                        presentation.shortcutHint +
                        "]"
                )
            }
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
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onEasy: () -> Unit,
    workspaceStrings: StudyWorkspaceStrings
) {
    val contentAccessibility =
        resolveStudyContentAccessibility(uiState)

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainer
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {
        Column(
            modifier =
                Modifier.padding(28.dp),
            verticalArrangement =
                Arrangement.spacedBy(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            if (uiState.hasActiveSession) {
                Text(
                    text =
                        if (
                            uiState.message ==
                            "New learning item"
                        ) {
                            "NEW"
                        } else {
                            "REVIEW"
                        },
                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    fontWeight =
                        FontWeight.Bold
                )
            }

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

            when {
                uiState.canRevealAnswer -> {
                    val action =
                        resolveStudyActionAccessibility(
                            StudyActionControl.REVEAL_ANSWER,
                            workspaceStrings
                        )

                    Button(
                        onClick = onRevealAnswer,
                        enabled = !uiState.actionInProgress,
                        modifier =
                            Modifier.studyActionSemantics(
                                StudyActionControl.REVEAL_ANSWER,
                                workspaceStrings
                            )
                    ) {
                        Text(
                            "${action.visibleLabel}  [${action.shortcutHint}]"
                        )
                    }
                }

                uiState.canReview -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement
                                .spacedBy(12.dp)
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
            }
        }
    }
}

@Composable
private fun StudyRatingGuidanceCard(workspaceStrings: StudyWorkspaceStrings) {
    val guidance = resolveStudyRatingGuidance()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription =
                        resolveStudyRatingGuidanceDescription()
                },
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Choose the rating that matches your recall:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        guidance.forEach { item ->
            val shortcut =
                resolveStudyActionAccessibility(item.control, workspaceStrings)
                    .shortcutHint

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
    val action =
        resolveStudyActionAccessibility(control, workspaceStrings)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.studyActionSemantics(control, workspaceStrings)
    ) {
        Text(
            "${action.visibleLabel}  [${action.shortcutHint}]"
        )
    }
}

@Composable
private fun SchedulerFeedbackCard(
    feedback: StudySchedulerFeedback
) {
    val accessibility =
        resolveStudySchedulerFeedbackAccessibility(feedback)

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription =
                        accessibility.detailsDescription
                },
        shape =
            RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainerLow
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {
        Column(
            modifier =
                Modifier.padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Scheduler Feedback",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Latest review decision",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            HorizontalDivider()

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                SchedulerFeedbackMetric(
                    label = "Rating",
                    value = feedback.rating,
                    modifier =
                        Modifier.weight(1f)
                )

                SchedulerFeedbackMetric(
                    label = "Stage",
                    value =
                        feedback.stageTransition,
                    modifier =
                        Modifier.weight(1f)
                )

                SchedulerFeedbackMetric(
                    label = "Interval",
                    value =
                        feedback.scheduledInterval,
                    modifier =
                        Modifier.weight(1f)
                )
            }

            SchedulerFeedbackRow(
                label = "Next review",
                value =
                    feedback.nextReviewAt
            )

            HorizontalDivider()

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                SchedulerTransitionMetric(
                    label = "Difficulty",
                    before =
                        feedback.difficultyBefore,
                    after =
                        feedback.difficultyAfter,
                    modifier =
                        Modifier.weight(1f)
                )

                SchedulerTransitionMetric(
                    label = "Stability",
                    before =
                        feedback.stabilityBefore,
                    after =
                        feedback.stabilityAfter,
                    modifier =
                        Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                SchedulerFeedbackMetric(
                    label = "Review count",
                    value =
                        feedback
                            .reviewCount
                            .toString(),
                    modifier =
                        Modifier.weight(1f)
                )

                SchedulerFeedbackMetric(
                    label = "Lapse count",
                    value =
                        feedback
                            .lapseCount
                            .toString(),
                    modifier =
                        Modifier.weight(1f)
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
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style =
                MaterialTheme
                    .typography
                    .labelMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Text(
            text = value,
            style =
                MaterialTheme
                    .typography
                    .titleMedium,
            fontWeight =
                FontWeight.Bold,
            color =
                MaterialTheme
                    .colorScheme
                    .primary
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
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style =
                MaterialTheme
                    .typography
                    .labelMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Text(
            text = "$before → $after",
            style =
                MaterialTheme
                    .typography
                    .titleMedium,
            fontWeight =
                FontWeight.Bold
        )
    }
}

@Composable
private fun SchedulerFeedbackRow(
    label: String,
    value: String
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style =
                MaterialTheme
                    .typography
                    .labelLarge,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Text(
            text = value,
            style =
                MaterialTheme
                    .typography
                    .bodyLarge,
            fontWeight =
                FontWeight.SemiBold
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
            Button(
                onClick = onRetry,
                modifier =
                    Modifier.studyActionSemantics(
                        StudyActionControl.RETRY_LOAD,
                        workspaceStrings
                    )
            ) {
                Text(
                    presentation.actionLabel +
                        "  [" +
                        presentation.shortcutHint +
                        "]"
                )
            }
        }
    }
}

@Composable
private fun StudyHeader(
    uiState: StudyUiState,
    accessibilityPresentation: StudyAccessibilityPresentation
) {
    Column(
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                stateDescription =
                    accessibilityPresentation.statusAnnouncement
            },
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text =
                if (uiState.isLessonStudy) {
                    "Lesson Study"
                } else {
                    "Study"
                },
            style =
                MaterialTheme
                    .typography
                    .labelLarge,
            color =
                MaterialTheme
                    .colorScheme
                    .primary,
            fontWeight =
                FontWeight.SemiBold
        )

        Text(
            text =
                uiState.studyTitle,
            style =
                MaterialTheme
                    .typography
                    .headlineMedium,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                uiState.message,
            style =
                MaterialTheme
                    .typography
                    .bodyLarge,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun ActiveSessionChrome(
    uiState: StudyUiState,
    accessibilityPresentation: StudyAccessibilityPresentation,
    workspaceStrings: StudyWorkspaceStrings,
    onUndo: () -> Unit,
    onPause: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
            stateDescription = accessibilityPresentation.statusAnnouncement
        },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(uiState.studyTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(uiState.progressLabel, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.canUndo) {
                    val undo = resolveStudyActionAccessibility(StudyActionControl.UNDO_LATEST, workspaceStrings)
                    OutlinedButton(
                        onClick = onUndo,
                        enabled = !uiState.actionInProgress,
                        modifier = Modifier.studyActionSemantics(StudyActionControl.UNDO_LATEST, workspaceStrings)
                    ) { Text("${undo.visibleLabel} [${undo.shortcutHint}]") }
                }
                val pause = resolveStudyActionAccessibility(StudyActionControl.PAUSE_WORKSPACE, workspaceStrings)
                OutlinedButton(
                    onClick = onPause,
                    enabled = !uiState.actionInProgress,
                    modifier = Modifier.studyActionSemantics(StudyActionControl.PAUSE_WORKSPACE, workspaceStrings)
                ) { Text("${pause.visibleLabel} [${pause.shortcutHint}]") }
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
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "Session progress"
                    accessibilityPresentation.progressDescription
                        ?.let { description ->
                            stateDescription = description
                        }
                },
        shape =
            RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainerLow
            )
    ) {
        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Session progress",
                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Text(
                    text =
                        uiState.progressLabel,
                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = {
                    uiState.progress
                },
                modifier =
                    Modifier.fillMaxWidth()
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
        shape =
            RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainerLow
            )
    ) {
        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style =
                    MaterialTheme
                        .typography
                        .labelLarge,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Text(
                text = value,
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }
    }
}
