package vn.loi.learning.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.platform.AndroidOperationKind
import vn.loi.learning.android.platform.AndroidApplicationGraph
import vn.loi.learning.android.study.*
import vn.loi.learning.domain.study.recall.StudyMode

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
                AndroidHomePrimaryAction.DailyComplete -> presentation.dailyBudget?.let { daily ->
                    when {
                        daily.targetsComplete -> "Your configured NEW and REVIEW workload is complete for today."
                        daily.newRemainingToday == 0 && daily.dueReviewCount == 0 ->
                            "Today's NEW target is complete and no REVIEW work is due."
                        daily.reviewRemainingToday == 0 && daily.eligibleNewContentCount == 0 ->
                            "Today's REVIEW target is complete and no NEW content is available."
                        else -> "No eligible Study content is currently available."
                    }
                } ?: "Today's configured Study workload is complete."
                AndroidHomePrimaryAction.OpenLibrary -> "Open Library to choose your current learning package."
            },
            actionLabel = when (primaryAction) {
                is AndroidHomePrimaryAction.Resume -> "Continue session"
                AndroidHomePrimaryAction.ReviewDue -> "Adaptive study"
                AndroidHomePrimaryAction.StartLearning -> "Learn new"
                AndroidHomePrimaryAction.DailyComplete -> "Open Library"
                AndroidHomePrimaryAction.OpenLibrary -> "Open Library"
            },
            onAction = {
                when (primaryAction) {
                    is AndroidHomePrimaryAction.Resume -> onEvent(AndroidStudyEvent.OpenSession(primaryAction.sessionId))
                    AndroidHomePrimaryAction.ReviewDue ->
                        onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.ADAPTIVE))
                    AndroidHomePrimaryAction.StartLearning ->
                        onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW))
                    AndroidHomePrimaryAction.DailyComplete -> onLibrary()
                    AndroidHomePrimaryAction.OpenLibrary -> onLibrary()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            supportingContent = {
                presentation.dailyBudget?.let { daily ->
                    Text(
                        "NEW ${daily.newCompletedToday}/${daily.limits.newPerDay} · " +
                            "REVIEW ${daily.reviewCompletedToday}/${daily.limits.reviewPerDay}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
        LearningEngineActionCard(
            Icons.Default.School,
            "Learn new",
            when {
                home.availability.canLearnNew && home.availability.canResume ->
                    "Switch from the current session to unseen vocabulary through Introduction"
                home.availability.canLearnNew -> "Learn unseen vocabulary through Introduction"
                else -> "Today's NEW quota is complete or no unseen vocabulary remains"
            },
            when {
                home.availability.canLearnNew && home.availability.canResume -> "Switch to Learn new"
                home.availability.canLearnNew -> "Learn new"
                else -> "Unavailable"
            },
            { onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)) },
            Modifier.fillMaxWidth(),
            enabled = home.availability.canLearnNew
        )
        LearningEngineActionCard(
            Icons.Default.Refresh,
            "Adaptive study",
            when {
                home.availability.canStartAdaptive && !home.availability.canStartReview ->
                    "Continue smart skim practice across learned vocabulary"
                home.availability.canStartAdaptive && home.availability.canResume ->
                    "Switch from the current session to due recall with adaptive planning"
                home.availability.canStartAdaptive -> "Recall introduced vocabulary with adaptive planning"
                else -> "No adaptive review is currently due"
            },
            if (home.availability.canStartAdaptive && home.availability.canResume) "Switch to Adaptive" else "Adaptive study",
            { onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.ADAPTIVE)) },
            Modifier.fillMaxWidth(),
            enabled = home.availability.canStartAdaptive
        )
        LearningEngineActionCard(
            Icons.Default.Edit,
            "Typing practice",
            when {
                home.availability.canStartTyping && home.availability.canResume ->
                    "Switch from the current session to typing-only practice"
                home.availability.canStartTyping -> "Typing-only practice for introduced vocabulary"
                else -> "No introduced vocabulary is available for typing practice"
            },
            if (home.availability.canStartTyping && home.availability.canResume) "Switch to Typing" else "Typing practice",
            { onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.TYPING)) },
            Modifier.fillMaxWidth(),
            enabled = home.availability.canStartTyping
        )
    }
}

@Composable
fun ReviewHub(
    home: AndroidStudyState.Home,
    quickReviewSummary: QuickReviewSessionInsights? = null,
    onEvent: (AndroidStudyEvent) -> Unit,
    onAutoPlayEntry: ((AndroidSessionEntry) -> Unit)? = null
) {
    LearningEngineScreenShell("Review", "Strengthen memory across active content",
        Modifier.verticalScroll(rememberScrollState())) {
        val actions = listOf(
            ReviewHubAction(
                "Quick Review",
                "Ôn nhanh không giới hạn. Vuốt lên để lướt, hoặc chấm Again / Hard / Good / Easy sau khi xem đáp án.",
                Icons.Default.Bolt,
                AndroidSessionEntry.QUICK_REVIEW,
                home.availability.canStartLearnedReview,
                badge = "NHANH · KHÔNG GIỚI HẠN"
            ),
            ReviewHubAction(
                "Ôn từ vừa học",
                "Xem lại các từ New trong phiên học hoàn tất gần nhất.",
                Icons.Default.History,
                AndroidSessionEntry.LATEST_SESSION,
                home.availability.canStartLatestSessionPractice
            ),
            ReviewHubAction(
                "Ôn Again / Hard",
                "Tập trung vào các từ hiện được đánh giá Again hoặc Hard.",
                Icons.Default.Psychology,
                AndroidSessionEntry.DIFFICULT,
                home.availability.canStartDifficultPractice
            ),
            ReviewHubAction(
                "Ôn tất cả đã học",
                "Phiên ôn hữu hạn có đánh giá cho toàn bộ từ đã học.",
                Icons.Default.School,
                AndroidSessionEntry.LEARNED,
                home.availability.canStartLearnedReview
            )
        )
        quickReviewSummary?.takeIf { it.totalExposures > 0 }?.let { summary ->
            QuickReviewInsightsCard(
                summary = summary,
                difficultAvailable = home.availability.canStartDifficultPractice,
                onReviewDifficult = { onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.DIFFICULT)) }
            )
        }
        actions.forEach { action ->
            LearningEngineCompactCard(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = LearningSpacing.touchTarget).semantics {
                    stateDescription = if (action.available) "Available" else "Unavailable"
                }
            ) {
                Icon(
                    action.icon,
                    contentDescription = null,
                    modifier = Modifier.size(LearningIconSize.card),
                    tint = if (action.entry == AndroidSessionEntry.QUICK_REVIEW) {
                        MaterialTheme.colorScheme.primary
                    } else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    action.badge?.let {
                        Text(it, style = LearningTextRole.caption, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(action.title, style = LearningTextRole.cardTitle)
                    Text(
                        action.description,
                        style = LearningTextRole.metadata,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onAutoPlayEntry != null && action.available) {
                    IconButton(
                        onClick = { onAutoPlayEntry(action.entry) },
                        modifier = Modifier.size(LearningSpacing.touchTarget)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Auto Play ${action.title}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(LearningIconSize.card)
                        )
                    }
                }
                LearningEngineSecondaryButton(
                    label = if (home.availability.hasActiveSession && action.available) "Switch" else "Start",
                    onClick = { onEvent(AndroidStudyEvent.Start(action.entry)) },
                    enabled = action.available
                )
            }
        }
    }
}

@Composable
private fun QuickReviewInsightsCard(
    summary: QuickReviewSessionInsights,
    difficultAvailable: Boolean,
    onReviewDifficult: () -> Unit
) {
    val semanticSummary = "Quick Review vừa rồi. ${summary.totalExposures} lượt. " +
        "Lướt ${summary.skipped}. Again ${summary.again}. Hard ${summary.hard}. " +
        "Good ${summary.good}. Easy ${summary.easy}."
    LearningEngineCompactCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.weight(1f).clearAndSetSemantics { contentDescription = semanticSummary },
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("Quick Review vừa rồi", style = LearningTextRole.cardTitle)
            Text(
                "${summary.totalExposures} lượt · Lướt ${summary.skipped}",
                style = LearningTextRole.metadata,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Again ${summary.again} · Hard ${summary.hard} · Good ${summary.good} · Easy ${summary.easy}",
                style = LearningTextRole.metadata,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LearningEngineSecondaryButton(
            label = "Ôn Again / Hard",
            onClick = onReviewDifficult,
            enabled = difficultAvailable
        )
    }
}

private data class ReviewHubAction(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val entry: AndroidSessionEntry,
    val available: Boolean,
    val badge: String? = null
)

@Composable
fun SettingsScreen(
    themeMode: AndroidThemeMode,
    onThemeMode: (AndroidThemeMode) -> Unit,
    studyLimits: vn.loi.learning.application.study.DailyStudyBudgetLimits,
    onNewDailyLimit: (Int) -> Boolean,
    onReviewDailyLimit: (Int) -> Boolean,
    continuousSkim: Boolean,
    onContinuousSkim: (Boolean) -> Unit,
    onControllerSettings: () -> Unit = {},
    onControllerDiagnostics: () -> Unit = {},
    onVoiceRecordings: () -> Unit = {},
    onVocabularyReminders: () -> Unit = {},
    onReminderSettings: () -> Unit = {},
    onHomeWidgetSettings: () -> Unit = {},
    onBackupRestore: () -> Unit = {},
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
            Text("Reminders & Surfaces", style = LearningTextRole.sectionTitle)
            LearningEngineSettingsRow(
                Icons.Default.Widgets,
                "Vocabulary Reminders & Surfaces",
                "Lock screen, popup reminders, and Home-screen widget",
                if (onVocabularyReminders != {}) onVocabularyReminders else onReminderSettings
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            Text("Study", style = LearningTextRole.sectionTitle)
            StudyDailyLimitField("New items per day", studyLimits.newPerDay, onNewDailyLimit)
            StudyDailyLimitField("Review items per day", studyLimits.reviewPerDay, onReviewDailyLimit)
            LearningEngineSettingsRow(
                Icons.Default.AutoStories,
                "Continuous skim",
                "Continue with shuffled practice after daily coverage",
                { onContinuousSkim(!continuousSkim) },
                trailing = {
                    Switch(checked = continuousSkim, onCheckedChange = onContinuousSkim)
                }
            )
            Text("Daily progress follows your local calendar day and is learner-wide.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            Text("External Controller & Voice", style = LearningTextRole.sectionTitle)
            LearningEngineSettingsRow(
                Icons.Default.Gamepad,
                "8BitDo / Controller Settings",
                "Configure button bindings, active profile, and modifier button",
                onControllerSettings
            )
            LearningEngineSettingsRow(
                Icons.Default.Mic,
                "Quick Voice Recordings",
                "Manage and listen to your practice voice recordings",
                onVoiceRecordings
            )
            LearningEngineSettingsRow(
                Icons.Default.BugReport,
                "Controller Diagnostics",
                "Test S/D/K modes, keycodes, and accessibility filtering",
                onControllerDiagnostics
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            Text("Data management", style = LearningTextRole.sectionTitle)
            LearningEngineSettingsRow(Icons.Default.Backup, "Backup & Restore", "Full backup and restore for learning data",
                onBackupRestore)
            LearningEngineSettingsRow(Icons.Default.Download, "Import package", "Add learning content from a package",
                { onAction(AndroidOperationKind.IMPORT) })
        }
    }
}

@Composable
private fun StudyDailyLimitField(label: String, value: Int, onValue: (Int) -> Boolean) {
    var draft by rememberSaveable(value) { mutableStateOf(value.toString()) }
    val parsed = draft.toIntOrNull()
    val valid = parsed != null && parsed in 1..999
    OutlinedTextField(
        value = draft,
        onValueChange = { next ->
            if (next.length <= 3 && next.all(Char::isDigit)) {
                draft = next
                next.toIntOrNull()?.takeIf { it in 1..999 }?.let(onValue)
            }
        },
        label = { Text(label) },
        supportingText = { Text(if (valid) "1–999" else "Enter a value from 1 to 999") },
        isError = draft.isNotEmpty() && !valid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "$label, current value $value"
            stateDescription = if (valid) "$parsed per day" else "Invalid value"
        }
    )
}
