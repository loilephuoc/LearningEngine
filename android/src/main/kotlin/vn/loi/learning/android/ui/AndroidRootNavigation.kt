package vn.loi.learning.android.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.R
import vn.loi.learning.android.platform.AndroidOperationKind
import vn.loi.learning.android.platform.AndroidApplicationGraph
import vn.loi.learning.android.platform.AppLanguage
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

enum class AndroidRootDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val fallbackLabel: String
) {
    HOME("home", R.string.nav_home, "Home"),
    LIBRARY("library", R.string.nav_library, "Library"),
    STUDY("study", R.string.nav_study, "Study"),
    REVIEW("review", R.string.nav_review, "Review"),
    SETTINGS("settings", R.string.nav_settings, "Settings");

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
            val label = stringResource(destination.labelRes)
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
                    ) { Icon(icon, label, Modifier.padding(horizontal = 12.dp, vertical = 4.dp).size(LearningIconSize.navigation)) }
                    Text(label, style = LearningTextRole.navigation,
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
    LearningEngineScreenShell(stringResource(R.string.study_hub_title), stringResource(R.string.study_hub_subtitle),
        Modifier.verticalScroll(rememberScrollState())) {
        LearningEngineHeroCard(
            icon = if (primaryAction is AndroidHomePrimaryAction.Resume) Icons.Default.PlayArrow else Icons.Default.School,
            eyebrow = if (primaryAction is AndroidHomePrimaryAction.Resume) stringResource(R.string.home_active_learning) else stringResource(R.string.home_ready_to_study),
            title = presentation.contextTitle ?: stringResource(R.string.home_choose_content),
            detail = when (primaryAction) {
                is AndroidHomePrimaryAction.Resume -> stringResource(R.string.study_resume_detail)
                AndroidHomePrimaryAction.ReviewDue -> stringResource(R.string.study_review_due_detail)
                AndroidHomePrimaryAction.StartLearning -> stringResource(R.string.study_start_learning_detail)
                AndroidHomePrimaryAction.DailyComplete -> presentation.dailyBudget?.let { daily ->
                    when {
                        daily.targetsComplete -> stringResource(R.string.study_daily_complete_detail)
                        daily.newRemainingToday == 0 && daily.dueReviewCount == 0 ->
                            "Today's NEW target is complete and no REVIEW work is due."
                        daily.reviewRemainingToday == 0 && daily.eligibleNewContentCount == 0 ->
                            "Today's REVIEW target is complete and no NEW content is available."
                        else -> "No eligible Study content is currently available."
                    }
                } ?: stringResource(R.string.study_daily_complete_detail)
                AndroidHomePrimaryAction.OpenLibrary -> stringResource(R.string.study_open_library_detail)
            },
            actionLabel = when (primaryAction) {
                is AndroidHomePrimaryAction.Resume -> stringResource(R.string.home_continue_session)
                AndroidHomePrimaryAction.ReviewDue -> stringResource(R.string.home_adaptive_study)
                AndroidHomePrimaryAction.StartLearning -> stringResource(R.string.home_learn_new)
                AndroidHomePrimaryAction.DailyComplete -> stringResource(R.string.home_open_library)
                AndroidHomePrimaryAction.OpenLibrary -> stringResource(R.string.home_open_library)
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
                        stringResource(
                            R.string.home_daily_new_progress,
                            daily.newCompletedToday,
                            daily.limits.newPerDay,
                            daily.reviewCompletedToday,
                            daily.limits.reviewPerDay
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (presentation.totalMemoryCount > 0) LearningEngineProgress(
                    presentation.learningProgress,
                    stringResource(
                        R.string.home_active_memory_count,
                        presentation.activeMemoryCount,
                        presentation.totalMemoryCount
                    )
                )
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            LearningEngineStatTile(stringResource(R.string.home_due_label), presentation.dueCount.toString(), Modifier.weight(1f))
            LearningEngineStatTile(stringResource(R.string.home_reviewed_today), presentation.reviewedToday.toString(), Modifier.weight(1f))
            LearningEngineStatTile(stringResource(R.string.home_active_memories), presentation.activeMemoryCount.toString(), Modifier.weight(1f))
        }
        val learnNewDesc = when {
            home.availability.canLearnNew && home.availability.canResume ->
                stringResource(R.string.study_learn_new_desc_switch)
            home.availability.canLearnNew -> stringResource(R.string.study_learn_new_desc)
            else -> stringResource(R.string.study_learn_new_unavailable)
        }
        LearningEngineCompactCard(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                .clickable(enabled = home.availability.canLearnNew) {
                    onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW))
                }
                .semantics {
                    stateDescription = if (home.availability.canLearnNew) "Available" else "Unavailable"
                }
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(LearningIconSize.card),
                tint = if (home.availability.canLearnNew) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.study_mode_learn_new), style = LearningTextRole.cardTitle, maxLines = 2)
                Text(
                    learnNewDesc,
                    style = LearningTextRole.metadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val adaptiveDesc = when {
            home.availability.canStartAdaptive && !home.availability.canStartReview ->
                stringResource(R.string.study_adaptive_desc_switch)
            home.availability.canStartAdaptive && home.availability.canResume ->
                stringResource(R.string.study_adaptive_desc_recall)
            home.availability.canStartAdaptive -> stringResource(R.string.study_adaptive_desc)
            else -> stringResource(R.string.study_adaptive_unavailable)
        }
        LearningEngineCompactCard(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                .clickable(enabled = home.availability.canStartAdaptive) {
                    onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.ADAPTIVE))
                }
                .semantics {
                    stateDescription = if (home.availability.canStartAdaptive) "Available" else "Unavailable"
                }
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(LearningIconSize.card),
                tint = if (home.availability.canStartAdaptive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.study_mode_adaptive), style = LearningTextRole.cardTitle, maxLines = 2)
                Text(
                    adaptiveDesc,
                    style = LearningTextRole.metadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val typingDesc = when {
            home.availability.canStartTyping && home.availability.canResume ->
                stringResource(R.string.study_typing_desc_switch)
            home.availability.canStartTyping -> stringResource(R.string.study_typing_desc)
            else -> stringResource(R.string.study_typing_unavailable)
        }
        LearningEngineCompactCard(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                .clickable(enabled = home.availability.canStartTyping) {
                    onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW, StudyMode.TYPING))
                }
                .semantics {
                    stateDescription = if (home.availability.canStartTyping) "Available" else "Unavailable"
                }
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(LearningIconSize.card),
                tint = if (home.availability.canStartTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.study_mode_typing), style = LearningTextRole.cardTitle, maxLines = 2)
                Text(
                    typingDesc,
                    style = LearningTextRole.metadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReviewHub(
    home: AndroidStudyState.Home,
    quickReviewSummary: QuickReviewSessionInsights? = null,
    onEvent: (AndroidStudyEvent) -> Unit,
    onAutoPlayEntry: ((AndroidSessionEntry) -> Unit)? = null
) {
    LearningEngineScreenShell(stringResource(R.string.review_hub_title), stringResource(R.string.review_hub_subtitle),
        Modifier.verticalScroll(rememberScrollState())) {
        val actions = listOf(
            ReviewHubAction(
                stringResource(R.string.review_quick_title),
                stringResource(R.string.review_quick_desc),
                Icons.Default.Bolt,
                AndroidSessionEntry.QUICK_REVIEW,
                home.availability.canStartLearnedReview,
                badge = stringResource(R.string.review_quick_badge)
            ),
            ReviewHubAction(
                stringResource(R.string.review_recent_title),
                stringResource(R.string.review_recent_desc),
                Icons.Default.History,
                AndroidSessionEntry.LATEST_SESSION,
                home.availability.canStartLatestSessionPractice
            ),
            ReviewHubAction(
                stringResource(R.string.review_difficult_title),
                stringResource(R.string.review_difficult_desc),
                Icons.Default.Psychology,
                AndroidSessionEntry.DIFFICULT,
                home.availability.canStartDifficultPractice
            ),
            ReviewHubAction(
                stringResource(R.string.review_learned_title),
                stringResource(R.string.review_learned_desc),
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
            val modeContainerColor = when {
                !action.available -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                action.entry == AndroidSessionEntry.QUICK_REVIEW -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                action.entry == AndroidSessionEntry.LATEST_SESSION -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                action.entry == AndroidSessionEntry.DIFFICULT -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                action.entry == AndroidSessionEntry.LEARNED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
            val modeIconTint = when {
                !action.available -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                action.entry == AndroidSessionEntry.QUICK_REVIEW -> MaterialTheme.colorScheme.primary
                action.entry == AndroidSessionEntry.LATEST_SESSION -> MaterialTheme.colorScheme.secondary
                action.entry == AndroidSessionEntry.DIFFICULT -> MaterialTheme.colorScheme.tertiary
                action.entry == AndroidSessionEntry.LEARNED -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.primary
            }
            LearningEngineCompactCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                    .clickable(enabled = action.available) {
                        onEvent(AndroidStudyEvent.Start(action.entry))
                    }
                    .semantics {
                        stateDescription = if (action.available) "Available" else "Unavailable"
                    },
                containerColor = modeContainerColor
            ) {
                Icon(
                    action.icon,
                    contentDescription = null,
                    modifier = Modifier.size(LearningIconSize.card),
                    tint = modeIconTint
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    action.badge?.let {
                        Text(
                            it,
                            style = LearningTextRole.caption,
                            color = if (action.available) modeIconTint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    Text(
                        action.title,
                        style = LearningTextRole.cardTitle,
                        color = if (action.available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        action.description,
                        style = LearningTextRole.metadata,
                        color = if (action.available) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                if (action.available) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
    val title = stringResource(R.string.review_recent_summary_title)
    val ratingAgain = stringResource(R.string.rating_again)
    val ratingHard = stringResource(R.string.rating_hard)
    val ratingGood = stringResource(R.string.rating_good)
    val ratingEasy = stringResource(R.string.rating_easy)
    val semanticSummary = "$title. ${summary.totalExposures} lượt. " +
        "Lướt ${summary.skipped}. $ratingAgain ${summary.again}. $ratingHard ${summary.hard}. " +
        "$ratingGood ${summary.good}. $ratingEasy ${summary.easy}."
    LearningEngineCompactCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.weight(1f).clearAndSetSemantics { contentDescription = semanticSummary },
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = LearningTextRole.cardTitle)
            Text(
                "${summary.totalExposures} lượt · Lướt ${summary.skipped}",
                style = LearningTextRole.metadata,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$ratingAgain ${summary.again} · $ratingHard ${summary.hard} · $ratingGood ${summary.good} · $ratingEasy ${summary.easy}",
                style = LearningTextRole.metadata,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LearningEngineSecondaryButton(
            label = stringResource(R.string.review_difficult_title),
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
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onLanguage: (AppLanguage) -> Unit = {},
    onAction: (AndroidOperationKind) -> Unit
) {
    LearningEngineScreenShell(stringResource(R.string.nav_settings), stringResource(R.string.settings_appearance_desc),
        Modifier.verticalScroll(rememberScrollState())) {
        Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            Text(stringResource(R.string.settings_appearance_title), style = LearningTextRole.sectionTitle)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small),
                verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
            ) {
                AndroidThemeMode.entries.forEach { mode ->
                    val label = stringResource(mode.labelRes)
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeMode(mode) },
                        label = { Text(label) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                            .semantics { stateDescription = if (themeMode == mode) "Selected" else "Not selected" }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.settings_app_language), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small),
                verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)
            ) {
                AppLanguage.entries.forEach { lang ->
                    val label = stringResource(lang.labelRes)
                    FilterChip(
                        selected = currentLanguage == lang,
                        onClick = { onLanguage(lang) },
                        label = { Text(label) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                            .semantics { stateDescription = if (currentLanguage == lang) "Selected" else "Not selected" }
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            Text(stringResource(R.string.settings_reminders_title), style = LearningTextRole.sectionTitle)
            LearningEngineSettingsRow(
                Icons.Default.Widgets,
                stringResource(R.string.settings_reminders_hub_label),
                stringResource(R.string.settings_reminders_hub_desc),
                if (onVocabularyReminders != {}) onVocabularyReminders else onReminderSettings
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            Text(stringResource(R.string.settings_study_title), style = LearningTextRole.sectionTitle)
            StudyDailyLimitField(stringResource(R.string.settings_new_items_per_day), studyLimits.newPerDay, onNewDailyLimit)
            StudyDailyLimitField(stringResource(R.string.settings_review_items_per_day), studyLimits.reviewPerDay, onReviewDailyLimit)
            LearningEngineSettingsRow(
                Icons.Default.AutoStories,
                stringResource(R.string.settings_continuous_skim),
                stringResource(R.string.settings_continuous_skim_desc),
                { onContinuousSkim(!continuousSkim) },
                trailing = {
                    Switch(checked = continuousSkim, onCheckedChange = onContinuousSkim)
                }
            )
            Text(stringResource(R.string.settings_daily_progress_desc),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            Text(stringResource(R.string.settings_controller_title), style = LearningTextRole.sectionTitle)
            LearningEngineSettingsRow(
                Icons.Default.Gamepad,
                stringResource(R.string.settings_controller_settings),
                stringResource(R.string.settings_controller_settings_desc),
                onControllerSettings
            )
            LearningEngineSettingsRow(
                Icons.Default.Mic,
                stringResource(R.string.settings_voice_recordings),
                stringResource(R.string.settings_voice_recordings_desc),
                onVoiceRecordings
            )
            LearningEngineSettingsRow(
                Icons.Default.BugReport,
                stringResource(R.string.settings_controller_diagnostics),
                stringResource(R.string.settings_controller_diagnostics_desc),
                onControllerDiagnostics
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.small)) {
            Text(stringResource(R.string.settings_data_title), style = LearningTextRole.sectionTitle)
            LearningEngineSettingsRow(Icons.Default.Backup, stringResource(R.string.settings_backup_restore), stringResource(R.string.settings_backup_restore_desc),
                onBackupRestore)
            LearningEngineSettingsRow(Icons.Default.Download, stringResource(R.string.settings_import_package), stringResource(R.string.settings_import_package_desc),
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
