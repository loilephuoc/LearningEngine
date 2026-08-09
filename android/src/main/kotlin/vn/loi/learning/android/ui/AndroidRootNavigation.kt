package vn.loi.learning.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.platform.AndroidOperationKind
import vn.loi.learning.android.platform.AndroidApplicationGraph
import vn.loi.learning.android.study.*

sealed interface AndroidRootState {
    data object Bootstrapping : AndroidRootState
    data class Ready(val graph: AndroidApplicationGraph) : AndroidRootState
    data class Failed(val message: String, val retryable: Boolean = true) : AndroidRootState
}

@Composable
fun AndroidStartupShell() {
    LearningEngineLoadingState("Opening Learning Engine")
}

@Composable
fun AndroidRootFailure(state: AndroidRootState.Failed, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(LearningSpacing.screen), contentAlignment = androidx.compose.ui.Alignment.Center) {
        LearningEngineErrorState(
            title = "Learning Engine could not open",
            message = state.message,
            onRetry = onRetry.takeIf { state.retryable }
        )
    }
}

@Composable
fun AndroidFeatureLoading(label: String) {
    LearningEngineLoadingState(label)
}

@Composable
fun AndroidFeatureFailure(title: String, message: String, onRetry: (() -> Unit)?) {
    Box(Modifier.fillMaxSize().padding(LearningSpacing.screen), contentAlignment = androidx.compose.ui.Alignment.Center) {
        LearningEngineErrorState(title, message, onRetry = onRetry)
    }
}

enum class AndroidRootDestination(val route:String,val label:String) {
    HOME("home","Home"), LIBRARY("library","Library"), STUDY("study","Study"),
    REVIEW("review","Review"), SETTINGS("settings","Settings");

    companion object {
        fun fromRoute(route: String?): AndroidRootDestination =
            entries.firstOrNull { it.route == route } ?: HOME
    }
}

@Composable
fun AndroidRootNavigation(selectedRoute: String, onSelect: (AndroidRootDestination) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = LearningElevation.raised,
        modifier = Modifier.height(68.dp)
    ) {
        AndroidRootDestination.entries.forEach { destination ->
            val isSelected = selectedRoute == destination.route
            val icon = when (destination) {
                AndroidRootDestination.HOME -> Icons.Default.Home
                AndroidRootDestination.LIBRARY -> Icons.AutoMirrored.Filled.MenuBook
                AndroidRootDestination.STUDY -> Icons.Default.School
                AndroidRootDestination.REVIEW -> Icons.Default.Refresh
                AndroidRootDestination.SETTINGS -> Icons.Default.Settings
            }
            Box(
                Modifier.weight(1f).fillMaxHeight().clickable { onSelect(destination) }
                    .semantics(mergeDescendants = true) {
                        selected = isSelected
                        stateDescription = if (isSelected) "Selected" else "Not selected"
                    },
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                        shape = LearningEngineShapes.medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ) { Icon(icon, destination.label, Modifier.padding(horizontal = 12.dp, vertical = 4.dp).size(LearningIconSize.navigation)) }
                    Text(destination.label, style = LearningTextRole.navigation,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun StudyHub(
    home: AndroidStudyState.Home,
    onEvent: (AndroidStudyEvent) -> Unit,
    onLibrary: () -> Unit = {},
    onReview: () -> Unit = {}
) {
    val presentation = resolveLearningLandingPresentation(home)
    val primaryAction = presentation.primaryAction
    LearningEngineScreenShell("Study", "Continue learning from your active content",
        Modifier.verticalScroll(rememberScrollState())) {
        LearningEngineHeroCard(
            icon = if (primaryAction is AndroidHomePrimaryAction.Resume) Icons.Default.PlayArrow else Icons.Default.School,
            eyebrow = if (primaryAction is AndroidHomePrimaryAction.Resume) "ACTIVE LEARNING" else "READY TO STUDY",
            title = presentation.contextTitle ?: "Choose learning content",
            detail = when (primaryAction) {
                is AndroidHomePrimaryAction.Resume -> "Continue exactly where you stopped."
                AndroidHomePrimaryAction.ReviewDue -> "Strengthen what is due in your current package."
                AndroidHomePrimaryAction.StartLearning -> "Start a canonical Study session from your current package."
                AndroidHomePrimaryAction.OpenLibrary -> "Open Library to choose your current learning package."
            },
            actionLabel = when (primaryAction) {
                is AndroidHomePrimaryAction.Resume -> "Continue session"
                AndroidHomePrimaryAction.ReviewDue -> "Review now"
                AndroidHomePrimaryAction.StartLearning -> "Start study"
                AndroidHomePrimaryAction.OpenLibrary -> "Open Library"
            },
            onAction = {
                when (primaryAction) {
                    is AndroidHomePrimaryAction.Resume -> onEvent(AndroidStudyEvent.OpenSession(primaryAction.sessionId))
                    AndroidHomePrimaryAction.ReviewDue,
                    AndroidHomePrimaryAction.StartLearning -> onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW))
                    AndroidHomePrimaryAction.OpenLibrary -> onLibrary()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            supportingContent = {
                if (presentation.totalMemoryCount > 0) LearningEngineProgress(
                    presentation.learningProgress,
                    "${presentation.activeMemoryCount} of ${presentation.totalMemoryCount} memories active"
                )
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            LearningEngineStatTile("Due", presentation.dueCount.toString(), Modifier.weight(1f))
            LearningEngineStatTile("Reviewed", presentation.reviewedToday.toString(), Modifier.weight(1f), "today")
            LearningEngineStatTile("Active", presentation.activeMemoryCount.toString(), Modifier.weight(1f), "memories")
        }
        if (home.availability.canStartReview && presentation.dueCount > 0) {
            LearningEngineActionCard(
                Icons.Default.Refresh,
                "Review due",
                "${presentation.dueCount} item(s) are waiting",
                "Review",
                onReview,
                Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ReviewHub(
    home: AndroidStudyState.Home,
    onEvent: (AndroidStudyEvent) -> Unit
) {
    LearningEngineScreenShell("Review", "Strengthen memory across active content",
        Modifier.verticalScroll(rememberScrollState())) {
        val actions = listOf(
            Triple("Review due items", AndroidSessionEntry.REVIEW, home.availability.canStartReview),
            Triple("Practice latest session", AndroidSessionEntry.LATEST_SESSION, home.availability.canStartLatestSessionPractice),
            Triple("Practice Again / Hard", AndroidSessionEntry.DIFFICULT, home.availability.canStartDifficultPractice),
            Triple("Review learned items", AndroidSessionEntry.LEARNED, home.availability.canStartLearnedReview)
        )
        val available = actions.filter { it.third }
        if (available.isNotEmpty()) {
            available.forEach { action ->
                LearningEngineCompactCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(action.first, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        LearningEnginePrimaryButton(
                            label = "Start",
                            onClick = { onEvent(AndroidStudyEvent.Start(action.second)) }
                        )
                    }
                }
            }
            if (home.availability.canStartReview) {
                LearningEngineCompactCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("Typing practice", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        LearningEnginePrimaryButton(
                            label = "Start",
                            onClick = { onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, vn.loi.learning.domain.study.recall.StudyMode.TYPING)) }
                        )
                    }
                }
            }
        } else {
            LearningEngineEmptyState(
                title = "No review ready",
                detail = "No review or practice session is available right now. Complete a Study session first."
            )
        }
    }
}

@Composable
fun SettingsScreen(
    themeMode: AndroidThemeMode,
    onThemeMode: (AndroidThemeMode) -> Unit,
    onAction: (AndroidOperationKind) -> Unit
) {
    LearningEngineScreenShell("Settings", "Appearance and local data",
        Modifier.verticalScroll(rememberScrollState())) {
        Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            Text("Appearance", style = LearningTextRole.sectionTitle)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small),
                verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
            ) {
                AndroidThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeMode(mode) },
                        label = { Text(mode.label) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                            .semantics { stateDescription = if (themeMode == mode) "Selected" else "Not selected" }
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            Text("Data management", style = LearningTextRole.sectionTitle)
            LearningEngineSettingsRow(Icons.Default.Download, "Import package", "Add learning content from a package",
                { onAction(AndroidOperationKind.IMPORT) })
            LearningEngineSettingsRow(Icons.Default.Backup, "Create backup", "Save a portable copy of local learning data",
                { onAction(AndroidOperationKind.BACKUP) })
            LearningEngineSettingsRow(Icons.Default.Restore, "Restore backup", "Restore from an existing backup file",
                { onAction(AndroidOperationKind.RESTORE) })
        }
    }
}
