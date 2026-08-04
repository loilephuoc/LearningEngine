package vn.loi.learning.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@Composable
fun LearningEngineScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) = Scaffold(modifier = modifier, topBar = topBar, bottomBar = bottomBar, content = content)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningEngineTopAppBar(title: String, navigation: @Composable () -> Unit = {}) =
    TopAppBar(title = { Text(title, style = MaterialTheme.typography.titleLarge) }, navigationIcon = navigation)

@Composable
fun LearningEnginePrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget).semantics {
        role = Role.Button
        stateDescription = if (enabled) "Enabled" else "Disabled"
    }
) { Text(label) }

@Composable
fun LearningEngineSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = FilledTonalButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.defaultMinSize(minHeight = LearningSpacing.touchTarget).semantics { role = Role.Button }
) { Text(label) }

@Composable
fun LearningEngineCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = ElevatedCard(
    modifier = modifier,
    shape = LearningEngineShapes.large,
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = LearningElevation.card)
) { Column(Modifier.padding(LearningSpacing.extraLarge), verticalArrangement = Arrangement.spacedBy(LearningSpacing.small), content = content) }

@Composable
fun LearningEngineSectionHeader(title: String, supporting: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
        Text(title, style = LearningContentTypography.sectionTitle, modifier = Modifier.semantics { heading() })
        supporting?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun LearningEngineLoadingState(label: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().semantics { contentDescription = label; liveRegion = LiveRegionMode.Polite }, contentAlignment = Alignment.Center) {
        LearningEngineCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LearningSpacing.medium)) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun LearningEngineEmptyState(title: String, detail: String, modifier: Modifier = Modifier) {
    LearningEngineCard(modifier.fillMaxWidth().semantics { contentDescription = "$title. $detail" }) {
        LearningEngineSectionHeader(title, detail)
    }
}

@Composable
fun LearningEngineErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    LearningEngineCard(modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive }) {
        LearningEngineSectionHeader(title)
        Text(message, color = MaterialTheme.colorScheme.error)
        onRetry?.let { LearningEnginePrimaryButton("Retry", it) }
    }
}

@Composable
fun LearningEngineProgress(progress: Float, label: String, modifier: Modifier = Modifier) {
    val bounded = progress.coerceIn(0f, 1f)
    val semantics = androidx.compose.ui.semantics.ProgressBarRangeInfo(bounded, 0f..1f)
    Column(modifier.semantics { contentDescription = label; progressBarRangeInfo = semantics }, verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
        Text(label, style = LearningContentTypography.progressMetric)
        LinearProgressIndicator(
            progress = { bounded },
            modifier = Modifier.fillMaxWidth(),
            trackColor = LearningEngineThemeTokens.semanticColors.progressTrack
        )
    }
}

@Composable
fun LearningEngineStatusBadge(label: String, tone: LearningStatusTone, modifier: Modifier = Modifier) {
    val semantic = LearningEngineThemeTokens.semanticColors
    val color = when (tone) {
        LearningStatusTone.SUCCESS -> semantic.success
        LearningStatusTone.WARNING -> semantic.warning
        LearningStatusTone.ERROR -> semantic.overdueReview
        LearningStatusTone.INFO -> semantic.info
        LearningStatusTone.ACTIVE -> semantic.activeLearning
        LearningStatusTone.DUE -> semantic.dueReview
        LearningStatusTone.OVERDUE -> semantic.overdueReview
        LearningStatusTone.COMPLETED -> semantic.completed
    }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        modifier = modifier.semantics { contentDescription = label; stateDescription = tone.name.lowercase() },
        colors = AssistChipDefaults.assistChipColors(labelColor = color),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = color)
    )
}

@Composable
fun LearningEngineFeedbackBadge(tone: LearningDifficultyTone, modifier: Modifier = Modifier) {
    val token = LearningEngineThemeTokens.semanticColors.feedback(tone)
    AssistChip(
        onClick = {},
        label = { Text(token.label) },
        leadingIcon = { Text(if (tone == LearningDifficultyTone.EASY) "✓" else if (tone == LearningDifficultyTone.MEDIUM) "•" else "!") },
        modifier = modifier.semantics { contentDescription = token.iconDescription; stateDescription = token.label },
        colors = AssistChipDefaults.assistChipColors(labelColor = token.color),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = token.color)
    )
}
