package vn.loi.learning.desktop.ui.study

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StudyScreen(
    uiState: StudyUiState,
    onStartStudy: () -> Unit,
    onRevealAnswer: () -> Unit,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onEasy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
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

        StudyItemCard(
            uiState = uiState,
            onStartStudy = onStartStudy,
            onRevealAnswer = onRevealAnswer,
            onAgain = onAgain,
            onHard = onHard,
            onGood = onGood,
            onEasy = onEasy
        )

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
private fun StudyItemCard(
    uiState: StudyUiState,
    onStartStudy: () -> Unit,
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
            if (
                uiState.hasActiveSession &&
                !uiState.sessionCompleted
            ) {
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

            if (
                uiState.canReview ||
                uiState.sessionCompleted
            ) {
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
                !uiState.hasActiveSession ||
                        uiState.sessionCompleted -> {
                    Button(
                        onClick =
                            onStartStudy
                    ) {
                        Text("Start Study")
                    }
                }

                uiState.canRevealAnswer -> {
                    OutlinedButton(
                        onClick =
                            onRevealAnswer
                    ) {
                        Text("Reveal Answer")
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
                            Text("Again")
                        }

                        OutlinedButton(
                            onClick = onHard
                        ) {
                            Text("Hard")
                        }

                        Button(
                            onClick = onGood
                        ) {
                            Text("Good")
                        }

                        Button(
                            onClick = onEasy
                        ) {
                            Text("Easy")
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