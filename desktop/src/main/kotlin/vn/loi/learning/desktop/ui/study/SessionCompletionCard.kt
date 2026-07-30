package vn.loi.learning.desktop.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

@Composable
fun SessionCompletionCard(
    completionUiState: SessionCompletionUiState,
    onBackToLesson: ((InstalledPackageId, ContentId) -> Unit)? = null,
    onBackToLibrary: (() -> Unit)? = null,
    onContinueLearning: ((InstalledPackageId, ContentId) -> Unit)? = null,
    onContinueGeneralStudy: () -> Unit = {},
    onReplayCompletedStudySession: () -> Unit = {},
    actionsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Status Label
            Text(
                text = completionUiState.statusLabel.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = if (completionUiState.status == SessionCompletionStatus.COMPLETED) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                fontWeight = FontWeight.Bold
            )

            // Lesson Title & Package Name
            Text(
                text = completionUiState.lessonTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            completionUiState.packageName?.let { pkg ->
                Text(
                    text = "Package: $pkg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reflection Summary Banner
            if (completionUiState.reflectionMessage.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Reflection & Progress",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = completionUiState.reflectionMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            HorizontalDivider()

            // Detailed Session Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SessionSummaryMetric(
                    label = "Total reviews",
                    value = completionUiState.reviewedCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                SessionSummaryMetric(
                    label = "New",
                    value = completionUiState.newItemsReviewed.toString(),
                    modifier = Modifier.weight(1f)
                )
                SessionSummaryMetric(
                    label = "Scheduled",
                    value = completionUiState.reviewItemsReviewed.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            // Snapshot Domain Reflection (if snapshot present)
            completionUiState.snapshot?.let { snapshot ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (snapshot.whatWasLearned.isNotBlank()) {
                        CompletionSummarySection("What you learned", snapshot.whatWasLearned)
                    }
                    if (snapshot.overallOutcome.isNotBlank()) {
                        CompletionSummarySection("Overall outcome", snapshot.overallOutcome)
                    }
                    if (snapshot.reflection.isNotBlank()) {
                        CompletionSummarySection("Reflection", snapshot.reflection)
                    }
                }
            }

            HorizontalDivider()

            // Navigation Actions: continue, replay this completed session, then leave Study.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pkgId = completionUiState.installedPackageId
                val contentId = completionUiState.contentId

                if (onContinueLearning != null && pkgId != null && contentId != null && completionUiState.nextAction?.isEnabled == true) {
                    Button(
                        onClick = { onContinueLearning(pkgId, contentId) },
                        enabled = actionsEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Continue Learning")
                    }
                } else if (completionUiState.canContinueGeneralStudy) {
                    Button(
                        onClick = onContinueGeneralStudy,
                        enabled = actionsEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Học tiếp")
                    }
                }

                OutlinedButton(
                    onClick = onReplayCompletedStudySession,
                    enabled = actionsEnabled && completionUiState.canReplayCompletedSession,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ôn lại phiên vừa học")
                }

                if (onBackToLibrary != null) {
                    OutlinedButton(
                        onClick = onBackToLibrary,
                        enabled = actionsEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back to Library")
                    }
                }

                if (onBackToLesson != null && pkgId != null && contentId != null) {
                    OutlinedButton(
                        onClick = { onBackToLesson(pkgId, contentId) },
                        enabled = actionsEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back to Lesson")
                    }
                }
            }
        }
    }
}
