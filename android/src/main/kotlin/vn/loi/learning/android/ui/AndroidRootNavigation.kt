package vn.loi.learning.android.ui

import androidx.compose.foundation.layout.*
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

@Composable fun AndroidRootNavigation(selected:String,onSelect:(AndroidRootDestination)->Unit){NavigationBar{AndroidRootDestination.entries.forEach{destination->NavigationBarItem(selected=selected==destination.route,onClick={onSelect(destination)},icon={Icon(when(destination){AndroidRootDestination.HOME->Icons.Default.Home;AndroidRootDestination.LIBRARY->Icons.AutoMirrored.Filled.MenuBook;AndroidRootDestination.STUDY->Icons.Default.School;AndroidRootDestination.REVIEW->Icons.Default.Refresh;AndroidRootDestination.SETTINGS->Icons.Default.Settings},destination.label)},label={Text(destination.label)})}}}

@Composable
fun StudyHub(
    home: AndroidStudyState.Home,
    onEvent: (AndroidStudyEvent) -> Unit,
    onLibrary: () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(LearningSpacing.screen)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(LearningSpacing.large)
    ) {
        LearningEngineSectionHeader("Study", "Choose your next learning step")
        LearningEngineCard(Modifier.fillMaxWidth()) {
            Text("Session Availability", style = MaterialTheme.typography.titleMedium)
            if (home.availability.canResume) {
                Text("An active study session is waiting for you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LearningEnginePrimaryButton(
                    label = "Continue current session",
                    onClick = { onEvent(AndroidStudyEvent.Resume) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("Select a package or lesson in Library to begin.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LearningEnginePrimaryButton(
                    label = "Open Library",
                    onClick = onLibrary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ReviewHub(
    home: AndroidStudyState.Home,
    onEvent: (AndroidStudyEvent) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(LearningSpacing.screen)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(LearningSpacing.large)
    ) {
        LearningEngineSectionHeader("Review", "Strengthen memory recall across active content")
        val actions = listOf(
            Triple("Review due items", AndroidSessionEntry.REVIEW, home.availability.canStartReview),
            Triple("Practice latest session", AndroidSessionEntry.LATEST_SESSION, home.availability.canStartLatestSessionPractice),
            Triple("Practice Again / Hard", AndroidSessionEntry.DIFFICULT, home.availability.canStartDifficultPractice),
            Triple("Review learned items", AndroidSessionEntry.LEARNED, home.availability.canStartLearnedReview)
        )
        val available = actions.filter { it.third }
        if (available.isNotEmpty()) {
            available.forEach { action ->
                LearningEngineCard(Modifier.fillMaxWidth()) {
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
    Column(
        Modifier
            .fillMaxSize()
            .padding(LearningSpacing.screen)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(LearningSpacing.large)
    ) {
        LearningEngineSectionHeader("Settings", "App preferences and data management")
        LearningEngineCard(Modifier.fillMaxWidth()) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
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
        LearningEngineCard(Modifier.fillMaxWidth()) {
            Text("Data Management", style = MaterialTheme.typography.titleMedium)
            LearningEnginePrimaryButton(
                label = "Import package",
                onClick = { onAction(AndroidOperationKind.IMPORT) },
                modifier = Modifier.fillMaxWidth()
            )
            LearningEngineSecondaryButton(
                label = "Create backup",
                onClick = { onAction(AndroidOperationKind.BACKUP) },
                modifier = Modifier.fillMaxWidth()
            )
            LearningEngineSecondaryButton(
                label = "Restore backup",
                onClick = { onAction(AndroidOperationKind.RESTORE) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
