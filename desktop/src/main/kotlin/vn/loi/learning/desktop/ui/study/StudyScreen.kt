package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StudyScreen(
    uiState: StudyUiState,
    onRefresh: () -> Unit,
    onStartStudy: () -> Unit,
    onRevealAnswer: () -> Unit,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onEasy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester =
        remember {
            FocusRequester()
        }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun performKeyboardAction(
        action: StudyKeyboardAction
    ) {
        when (action) {
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
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (
                        event.type !=
                        KeyEventType.KeyDown
                    ) {
                        return@onPreviewKeyEvent false
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

                            else -> null
                        }

                    val action =
                        shortcutKey?.let { key ->
                            resolveStudyKeyboardAction(
                                uiState = uiState,
                                key = key
                            )
                        }

                    if (action == null) {
                        false
                    } else {
                        performKeyboardAction(action)
                        true
                    }
                }
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(24.dp),
        verticalArrangement =
            Arrangement.spacedBy(20.dp)
    ) {
        StudyHeader(
            uiState = uiState
        )

        uiState.loadError?.let { error ->
            StudyLoadErrorCard(
                message = error,
                onRetry = onRefresh
            )
        }

        if (
            uiState.isLessonStudy &&
            uiState.hasKnownTotal
        ) {
            LessonProgressCard(
                uiState = uiState
            )
        }

        Row(
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
                onStartStudy = onStartStudy
            )
        } else {
            val idlePresentation =
                resolveStudyIdlePresentation(uiState)

            if (idlePresentation != null) {
                StudyIdleCard(
                    presentation = idlePresentation,
                    onStartStudy = onStartStudy
                )
            } else {
                StudyItemCard(
                    uiState = uiState,
                    onRevealAnswer = onRevealAnswer,
                    onAgain = onAgain,
                    onHard = onHard,
                    onGood = onGood,
                    onEasy = onEasy
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

@Composable
private fun SessionSummaryCard(
    uiState: StudyUiState,
    onStartStudy: () -> Unit
) {
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

            Button(
                onClick = onStartStudy
            ) {
                Text("Start General Study  [Enter]")
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
    onStartStudy: () -> Unit
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

            Button(onClick = onStartStudy) {
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
    onRevealAnswer: () -> Unit,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onEasy: () -> Unit
) {
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

            Text(
                text =
                    uiState.contentText,
                style =
                    MaterialTheme
                        .typography
                        .headlineLarge,
                fontWeight =
                    FontWeight.Bold
            )

            if (uiState.canReview) {
                Text(
                    text =
                        uiState
                            .translationText,
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            when {
                uiState.canRevealAnswer -> {
                    OutlinedButton(
                        onClick =
                            onRevealAnswer
                    ) {
                        Text("Reveal Answer  [Space]")
                    }
                }

                uiState.canReview -> {
                    Row(
                        horizontalArrangement =
                            Arrangement
                                .spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAgain
                        ) {
                            Text("Again  [1]")
                        }

                        OutlinedButton(
                            onClick = onHard
                        ) {
                            Text("Hard  [2]")
                        }

                        Button(
                            onClick = onGood
                        ) {
                            Text("Good  [3]")
                        }

                        Button(
                            onClick = onEasy
                        ) {
                            Text("Easy  [4]")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchedulerFeedbackCard(
    feedback: StudySchedulerFeedback
) {
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
    message: String,
    onRetry: () -> Unit
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
                text = "STUDY DATA ERROR",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun StudyHeader(
    uiState: StudyUiState
) {
    Column(
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
private fun LessonProgressCard(
    uiState: StudyUiState
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
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
                    text = "Lesson progress",
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