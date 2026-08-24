package vn.loi.learning.android.reminder

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import vn.loi.learning.android.R
import androidx.compose.ui.res.stringResource
import vn.loi.learning.android.LearningEngineAndroidApplication

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeWidgetSettingsScreen(
    controller: AndroidVocabularyReminderPreferencesController,
    selector: AndroidVocabularyReminderCandidateSelector,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val homeWidgetSettings by controller.homeWidgetSettings.collectAsState()
    var homeWidgetDraft by remember(homeWidgetSettings) {
        mutableStateOf(AndroidHomeVocabularyWidgetDraft.from(homeWidgetSettings))
    }
    var intervalTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = homeWidgetDraft.intervalValueText,
                selection = TextRange(homeWidgetDraft.intervalValueText.length)
            )
        )
    }

    LaunchedEffect(homeWidgetDraft.intervalValueText) {
        if (intervalTextFieldValue.text != homeWidgetDraft.intervalValueText) {
            intervalTextFieldValue = TextFieldValue(
                text = homeWidgetDraft.intervalValueText,
                selection = TextRange(homeWidgetDraft.intervalValueText.length)
            )
        }
    }
    val availablePackages by remember { mutableStateOf(selector.getAvailablePackages()) }

    var homeWidgetPackageDropdownExpanded by remember { mutableStateOf(false) }
    var homeWidgetModeDropdownExpanded by remember { mutableStateOf(false) }

    var hasUsageAccess by remember {
        mutableStateOf(HomeWidgetForegroundAppDetector.checkUsageAccess(context))
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = HomeWidgetForegroundAppDetector.checkUsageAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(availablePackages, homeWidgetDraft.selectedPackageId) {
        if (homeWidgetDraft.selectedPackageId == null && availablePackages.isNotEmpty()) {
            val defaultPkgId = availablePackages.first().id
            val updated = homeWidgetDraft.copy(selectedPackageId = defaultPkgId)
            homeWidgetDraft = updated
            controller.updateHomeWidgetSettings(updated.toSettings(homeWidgetSettings.currentCandidateId))
        }
    }

    val applyHomeWidgetDraft: (AndroidHomeVocabularyWidgetDraft) -> Unit = { nextDraft ->
        homeWidgetDraft = nextDraft
        controller.updateHomeWidgetSettings(nextDraft.toSettings(homeWidgetSettings.currentCandidateId))
        AndroidLockScreenVocabularyService.reconcile(context, "HOME_WIDGET_SETTINGS_CHANGED")
        val app = context.applicationContext as? LearningEngineAndroidApplication
        app?.homeVocabularyWidgetCoordinator?.reconcileAutoNextTimer("SETTINGS_UPDATED")
        app?.homeVocabularyWidgetCoordinator?.reRenderAllWidgets("SETTINGS_UPDATED")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = stringResource(R.string.widget_auto_next_rotation_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (homeWidgetDraft.autoNextEnabled) stringResource(R.string.widget_auto_next_cycles_desc) else stringResource(R.string.widget_auto_next_paused_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = homeWidgetDraft.autoNextEnabled,
                            onCheckedChange = { enabled ->
                                applyHomeWidgetDraft(homeWidgetDraft.copy(autoNextEnabled = enabled))
                            }
                        )
                    }

                    // Package Selector
                    val allPackagesAutoLabel = stringResource(R.string.widget_all_packages_auto)
                    val selectedWidgetPackageName = remember(availablePackages, homeWidgetDraft.selectedPackageId, allPackagesAutoLabel) {
                        val id = homeWidgetDraft.selectedPackageId
                        if (id == null) {
                            allPackagesAutoLabel
                        } else {
                            availablePackages.find { it.id == id }?.name ?: id
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.widget_package_title),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { homeWidgetPackageDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = selectedWidgetPackageName,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            DropdownMenu(
                                expanded = homeWidgetPackageDropdownExpanded,
                                onDismissRequest = { homeWidgetPackageDropdownExpanded = false }
                            ) {
                                availablePackages.forEach { pkg ->
                                    DropdownMenuItem(
                                        text = { Text(pkg.name) },
                                        onClick = {
                                            homeWidgetPackageDropdownExpanded = false
                                            applyHomeWidgetDraft(homeWidgetDraft.copy(selectedPackageId = pkg.id))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Selection Mode
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.widget_selection_mode_title),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { homeWidgetModeDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = when (homeWidgetDraft.selectionMode) {
                                        AndroidVocabularyReminderSelectionMode.RANDOM_ALL -> stringResource(R.string.reminder_mode_random_all)
                                        AndroidVocabularyReminderSelectionMode.DUE -> stringResource(R.string.reminder_mode_due)
                                        AndroidVocabularyReminderSelectionMode.AGAIN_HARD -> stringResource(R.string.reminder_mode_again_hard)
                                        AndroidVocabularyReminderSelectionMode.RANDOM_LEARNED -> stringResource(R.string.reminder_mode_random_learned)
                                        AndroidVocabularyReminderSelectionMode.MARKED_DIFFICULT -> stringResource(R.string.reminder_mode_marked_difficult)
                                    },
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            DropdownMenu(
                                expanded = homeWidgetModeDropdownExpanded,
                                onDismissRequest = { homeWidgetModeDropdownExpanded = false }
                            ) {
                                AndroidVocabularyReminderSelectionMode.values().forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                when (mode) {
                                                    AndroidVocabularyReminderSelectionMode.RANDOM_ALL -> stringResource(R.string.reminder_mode_random_all)
                                                    AndroidVocabularyReminderSelectionMode.DUE -> stringResource(R.string.reminder_mode_due)
                                                    AndroidVocabularyReminderSelectionMode.AGAIN_HARD -> stringResource(R.string.reminder_mode_again_hard)
                                                    AndroidVocabularyReminderSelectionMode.RANDOM_LEARNED -> stringResource(R.string.reminder_mode_random_learned)
                                                    AndroidVocabularyReminderSelectionMode.MARKED_DIFFICULT -> stringResource(R.string.reminder_mode_marked_difficult)
                                                }
                                            )
                                        },
                                        onClick = {
                                            homeWidgetModeDropdownExpanded = false
                                            applyHomeWidgetDraft(homeWidgetDraft.copy(selectionMode = mode))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Interval Configuration (Custom Input + Presets)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.widget_interval_label),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = intervalTextFieldValue,
                                onValueChange = { nextTfv ->
                                    val filteredText = nextTfv.text.filter { it.isDigit() }
                                    val newSelection = if (filteredText == nextTfv.text) {
                                        nextTfv.selection
                                    } else {
                                        TextRange(filteredText.length)
                                    }
                                    intervalTextFieldValue = nextTfv.copy(text = filteredText, selection = newSelection)
                                    applyHomeWidgetDraft(homeWidgetDraft.copy(intervalValueText = filteredText))
                                },
                                label = { Text(stringResource(R.string.widget_input_value_label)) },
                                isError = homeWidgetDraft.intervalValidationMessage != null,
                                supportingText = homeWidgetDraft.intervalValidationMessage?.let { msg -> { Text(msg) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            intervalTextFieldValue = intervalTextFieldValue.copy(
                                                selection = TextRange(0, intervalTextFieldValue.text.length)
                                            )
                                        }
                                    }
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = homeWidgetDraft.intervalUnit == AndroidVocabularyReminderIntervalUnit.SECONDS,
                                    onClick = {
                                        applyHomeWidgetDraft(homeWidgetDraft.copy(intervalUnit = AndroidVocabularyReminderIntervalUnit.SECONDS))
                                    },
                                    label = { Text(stringResource(R.string.unit_sec)) }
                                )
                                FilterChip(
                                    selected = homeWidgetDraft.intervalUnit == AndroidVocabularyReminderIntervalUnit.MINUTES,
                                    onClick = {
                                        applyHomeWidgetDraft(homeWidgetDraft.copy(intervalUnit = AndroidVocabularyReminderIntervalUnit.MINUTES))
                                    },
                                    label = { Text(stringResource(R.string.unit_min)) }
                                )
                            }
                        }

                        // Presets
                        Text(
                            text = stringResource(R.string.reminder_presets_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "5" to AndroidVocabularyReminderIntervalUnit.SECONDS,
                                "10" to AndroidVocabularyReminderIntervalUnit.SECONDS,
                                "20" to AndroidVocabularyReminderIntervalUnit.SECONDS,
                                "30" to AndroidVocabularyReminderIntervalUnit.SECONDS,
                                "1" to AndroidVocabularyReminderIntervalUnit.MINUTES,
                                "2" to AndroidVocabularyReminderIntervalUnit.MINUTES,
                                "5" to AndroidVocabularyReminderIntervalUnit.MINUTES,
                                "10" to AndroidVocabularyReminderIntervalUnit.MINUTES,
                                "15" to AndroidVocabularyReminderIntervalUnit.MINUTES,
                                "30" to AndroidVocabularyReminderIntervalUnit.MINUTES,
                                "60" to AndroidVocabularyReminderIntervalUnit.MINUTES
                            ).forEach { (v, u) ->
                                val unitStr = if (u == AndroidVocabularyReminderIntervalUnit.SECONDS) "s" else "m"
                                AssistChip(
                                    onClick = {
                                        applyHomeWidgetDraft(homeWidgetDraft.copy(intervalValueText = v, intervalUnit = u))
                                    },
                                    label = { Text("$v$unitStr") }
                                )
                            }
                        }
                    }

                    // Word Size
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.widget_word_size_title),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val wordSizes = listOf(
                            LockWallpaperWordSize.SMALL to stringResource(R.string.size_small),
                            LockWallpaperWordSize.MEDIUM to stringResource(R.string.size_med),
                            LockWallpaperWordSize.LARGE to stringResource(R.string.size_large),
                            LockWallpaperWordSize.EXTRA_LARGE to stringResource(R.string.size_xl),
                            LockWallpaperWordSize.HUGE to stringResource(R.string.size_huge)
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            wordSizes.forEachIndexed { index, (size, label) ->
                                SegmentedButton(
                                    selected = homeWidgetDraft.wordSize == size,
                                    onClick = { applyHomeWidgetDraft(homeWidgetDraft.copy(wordSize = size)) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = wordSizes.size)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Vietnamese Size
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.widget_vn_size_title),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val vnSizes = listOf(
                            LockWallpaperVietnameseSize.SMALL to stringResource(R.string.size_small),
                            LockWallpaperVietnameseSize.MEDIUM to stringResource(R.string.size_med),
                            LockWallpaperVietnameseSize.LARGE to stringResource(R.string.size_large),
                            LockWallpaperVietnameseSize.EXTRA_LARGE to stringResource(R.string.size_xl),
                            LockWallpaperVietnameseSize.HUGE to stringResource(R.string.size_huge)
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            vnSizes.forEachIndexed { index, (size, label) ->
                                SegmentedButton(
                                    selected = homeWidgetDraft.vietnameseSize == size,
                                    onClick = { applyHomeWidgetDraft(homeWidgetDraft.copy(vietnameseSize = size)) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = vnSizes.size)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Card Background Opacity Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.widget_card_opacity_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val currentPct = (homeWidgetDraft.cardBackgroundOpacity * 100).roundToInt()
                            Text(
                                text = stringResource(R.string.widget_card_opacity_current, currentPct),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Slider(
                            value = homeWidgetDraft.cardBackgroundOpacity,
                            onValueChange = { newOpacity ->
                                applyHomeWidgetDraft(homeWidgetDraft.copy(cardBackgroundOpacity = newOpacity))
                            },
                            valueRange = 0.20f..1.00f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Update Only When Screen On
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = stringResource(R.string.widget_screen_on_only_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.widget_screen_on_only_explanation),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = homeWidgetDraft.updateOnlyScreenOn,
                            onCheckedChange = { enabled ->
                                applyHomeWidgetDraft(homeWidgetDraft.copy(updateOnlyScreenOn = enabled))
                            }
                        )
                    }

                    if (homeWidgetDraft.updateOnlyScreenOn && !hasUsageAccess) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.widget_usage_access_required_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Button(
                                    onClick = {
                                        HomeWidgetForegroundAppDetector.openUsageAccessSettings(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        text = stringResource(R.string.widget_grant_usage_access),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
