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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
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
    val availablePackages by remember { mutableStateOf(selector.getAvailablePackages()) }

    var homeWidgetPackageDropdownExpanded by remember { mutableStateOf(false) }
    var homeWidgetModeDropdownExpanded by remember { mutableStateOf(false) }

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
        val app = context.applicationContext as? LearningEngineAndroidApplication
        app?.homeVocabularyWidgetCoordinator?.reconcileAutoNextTimer("SETTINGS_UPDATED")
        app?.homeVocabularyWidgetCoordinator?.reRenderAllWidgets("SETTINGS_UPDATED")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home-Screen Vocabulary Widget") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                                text = "Auto-Next Word Rotation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (homeWidgetDraft.autoNextEnabled) "Automatically cycles words while screen is on" else "Rotation paused",
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
                    val selectedWidgetPackageName = remember(availablePackages, homeWidgetDraft.selectedPackageId) {
                        val id = homeWidgetDraft.selectedPackageId
                        if (id == null) {
                            "All packages (Auto)"
                        } else {
                            availablePackages.find { it.id == id }?.name ?: id
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Vocabulary package",
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
                            text = "Selection mode",
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
                                        AndroidVocabularyReminderSelectionMode.RANDOM_ALL -> "Random All"
                                        AndroidVocabularyReminderSelectionMode.DUE -> "Due (FSRS)"
                                        AndroidVocabularyReminderSelectionMode.AGAIN_HARD -> "Again / Hard"
                                        AndroidVocabularyReminderSelectionMode.RANDOM_LEARNED -> "Learned"
                                        AndroidVocabularyReminderSelectionMode.MARKED_DIFFICULT -> "Marked Difficult"
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
                                                    AndroidVocabularyReminderSelectionMode.RANDOM_ALL -> "Random All"
                                                    AndroidVocabularyReminderSelectionMode.DUE -> "Due (FSRS)"
                                                    AndroidVocabularyReminderSelectionMode.AGAIN_HARD -> "Again / Hard"
                                                    AndroidVocabularyReminderSelectionMode.RANDOM_LEARNED -> "Learned"
                                                    AndroidVocabularyReminderSelectionMode.MARKED_DIFFICULT -> "Marked Difficult"
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
                            text = "Change word every",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = homeWidgetDraft.intervalValueText,
                                onValueChange = { nextVal ->
                                    applyHomeWidgetDraft(homeWidgetDraft.copy(intervalValueText = nextVal))
                                },
                                label = { Text("Value") },
                                isError = homeWidgetDraft.intervalValidationMessage != null,
                                supportingText = homeWidgetDraft.intervalValidationMessage?.let { msg -> { Text(msg) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = homeWidgetDraft.intervalUnit == AndroidVocabularyReminderIntervalUnit.SECONDS,
                                    onClick = {
                                        applyHomeWidgetDraft(homeWidgetDraft.copy(intervalUnit = AndroidVocabularyReminderIntervalUnit.SECONDS))
                                    },
                                    label = { Text("Sec") }
                                )
                                FilterChip(
                                    selected = homeWidgetDraft.intervalUnit == AndroidVocabularyReminderIntervalUnit.MINUTES,
                                    onClick = {
                                        applyHomeWidgetDraft(homeWidgetDraft.copy(intervalUnit = AndroidVocabularyReminderIntervalUnit.MINUTES))
                                    },
                                    label = { Text("Min") }
                                )
                            }
                        }

                        // Presets
                        Text(
                            text = "Presets",
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
                            text = "English word size",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val wordSizes = listOf(
                            LockWallpaperWordSize.SMALL to "Small",
                            LockWallpaperWordSize.MEDIUM to "Med",
                            LockWallpaperWordSize.LARGE to "Large",
                            LockWallpaperWordSize.EXTRA_LARGE to "XL",
                            LockWallpaperWordSize.HUGE to "Huge"
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
                            text = "Vietnamese meaning size",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val vnSizes = listOf(
                            LockWallpaperVietnameseSize.SMALL to "Small",
                            LockWallpaperVietnameseSize.MEDIUM to "Med",
                            LockWallpaperVietnameseSize.LARGE to "Large",
                            LockWallpaperVietnameseSize.EXTRA_LARGE to "XL",
                            LockWallpaperVietnameseSize.HUGE to "Huge"
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
                                text = "Widget card opacity",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val currentPct = (homeWidgetDraft.cardBackgroundOpacity * 100).roundToInt()
                            Text(
                                text = "Current: $currentPct%",
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
                                text = "Update only while screen is ON",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Pauses timer when screen is off to save battery.",
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
                }
            }
        }
    }
}
