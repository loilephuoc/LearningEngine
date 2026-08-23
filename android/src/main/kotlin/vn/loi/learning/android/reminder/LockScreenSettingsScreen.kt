package vn.loi.learning.android.reminder

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import vn.loi.learning.android.R
import androidx.compose.ui.res.stringResource
import vn.loi.learning.android.LearningEngineAndroidApplication

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LockScreenSettingsScreen(
    controller: AndroidVocabularyReminderPreferencesController,
    selector: AndroidVocabularyReminderCandidateSelector,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lockScreenSettings by controller.lockScreenSettings.collectAsState()
    var lockScreenDraft by remember(lockScreenSettings) {
        mutableStateOf(AndroidLockScreenVocabularyDraft.from(lockScreenSettings))
    }
    val availablePackages by remember { mutableStateOf(selector.getAvailablePackages()) }

    var lockScreenPackageDropdownExpanded by remember { mutableStateOf(false) }
    var lockScreenModeDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(availablePackages, lockScreenDraft.selectedPackageId) {
        if (lockScreenDraft.selectedPackageId == null && availablePackages.isNotEmpty()) {
            val defaultPkgId = availablePackages.first().id
            val updated = lockScreenDraft.copy(selectedPackageId = defaultPkgId)
            lockScreenDraft = updated
            controller.updateLockScreenSettings(updated.toSettings())
        }
    }

    val app = context.applicationContext as? LearningEngineAndroidApplication

    val applyLockScreenDraft: (AndroidLockScreenVocabularyDraft) -> Unit = { nextDraft ->
        lockScreenDraft = nextDraft
        controller.updateLockScreenSettings(nextDraft.toSettings())
        AndroidLockScreenVocabularyService.reconcile(context, "LOCK_SCREEN_SETTINGS_CHANGED")
        if (nextDraft.enabled) {
            app?.lockScreenVocabularyCoordinator?.reRenderCurrentPresentation("SETTINGS_UPDATED")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lock_screen_settings_title)) },
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
                                text = stringResource(R.string.lock_screen_wallpaper_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.lock_screen_wallpaper_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = lockScreenDraft.enabled,
                            onCheckedChange = { enabled ->
                                applyLockScreenDraft(lockScreenDraft.copy(enabled = enabled))
                            }
                        )
                    }

                    if (lockScreenDraft.enabled) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.lock_screen_note_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        // Lock-Screen Package Selector
                        val noPackagesInstalledLabel = stringResource(R.string.lock_screen_no_packages)
                        val packageUnavailableLabel = stringResource(R.string.lock_screen_package_unavailable)
                        val selectedPackageName = remember(availablePackages, lockScreenDraft.selectedPackageId, noPackagesInstalledLabel, packageUnavailableLabel) {
                            val id = lockScreenDraft.selectedPackageId
                            if (id == null) {
                                availablePackages.firstOrNull()?.name ?: noPackagesInstalledLabel
                            } else {
                                availablePackages.firstOrNull { it.id == id }?.name ?: packageUnavailableLabel
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.lock_screen_package_title),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box {
                                OutlinedButton(
                                    onClick = { lockScreenPackageDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = selectedPackageName,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                DropdownMenu(
                                    expanded = lockScreenPackageDropdownExpanded,
                                    onDismissRequest = { lockScreenPackageDropdownExpanded = false }
                                ) {
                                    if (availablePackages.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.lock_screen_no_active_packages)) },
                                            onClick = { lockScreenPackageDropdownExpanded = false }
                                        )
                                    } else {
                                        availablePackages.forEach { pkg ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(pkg.name, fontWeight = FontWeight.Medium)
                                                        Text(stringResource(R.string.reminder_package_items_count, pkg.totalItemCount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                trailingIcon = {
                                                    if (lockScreenDraft.selectedPackageId == pkg.id || (lockScreenDraft.selectedPackageId == null && pkg == availablePackages.firstOrNull())) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                },
                                                onClick = {
                                                    lockScreenPackageDropdownExpanded = false
                                                    applyLockScreenDraft(lockScreenDraft.copy(selectedPackageId = pkg.id))
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Lock-Screen Selection Mode Dropdown
                        val modeNames = mapOf(
                            AndroidLockScreenVocabularyMode.AGAIN_HARD to stringResource(R.string.reminder_mode_again_hard),
                            AndroidLockScreenVocabularyMode.DUE to stringResource(R.string.reminder_mode_due),
                            AndroidLockScreenVocabularyMode.NEW_UNSEEN to stringResource(R.string.reminder_mode_new_unseen),
                            AndroidLockScreenVocabularyMode.RANDOM_LEARNED to stringResource(R.string.reminder_mode_random_learned),
                            AndroidLockScreenVocabularyMode.MARKED_DIFFICULT to stringResource(R.string.reminder_mode_marked_difficult),
                            AndroidLockScreenVocabularyMode.RANDOM_ALL to stringResource(R.string.reminder_mode_random_all)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.lock_screen_candidate_pool_title),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box {
                                OutlinedButton(
                                    onClick = { lockScreenModeDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = modeNames[lockScreenDraft.selectionMode] ?: lockScreenDraft.selectionMode.name,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                DropdownMenu(
                                    expanded = lockScreenModeDropdownExpanded,
                                    onDismissRequest = { lockScreenModeDropdownExpanded = false }
                                ) {
                                    AndroidLockScreenVocabularyMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(modeNames[mode] ?: mode.name) },
                                            trailingIcon = {
                                                if (lockScreenDraft.selectionMode == mode) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                }
                                            },
                                            onClick = {
                                                lockScreenModeDropdownExpanded = false
                                                applyLockScreenDraft(lockScreenDraft.copy(selectionMode = mode))
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Background Image Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.lock_screen_bg_title),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.lock_screen_bg_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val bgLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.PickVisualMedia()
                            ) { uri ->
                                if (uri != null) {
                                    coroutineScope.launch {
                                        runCatching {
                                            val destFile = File(context.filesDir, "lockscreen_custom_bg.png")
                                            context.contentResolver.openInputStream(uri)?.use { input ->
                                                destFile.outputStream().use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                            applyLockScreenDraft(lockScreenDraft.copy(customBackgroundPath = destFile.absolutePath))
                                        }
                                    }
                                }
                            }

                            if (lockScreenDraft.customBackgroundPath == null) {
                                OutlinedButton(
                                    onClick = {
                                        bgLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.lock_screen_choose_bg))
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            bgLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.lock_screen_change_bg))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            applyLockScreenDraft(lockScreenDraft.copy(customBackgroundPath = null))
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.lock_screen_remove_bg))
                                    }
                                }
                            }
                        }

                        // English word size
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.lock_screen_word_size_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.lock_screen_word_size_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                LockWallpaperWordSize.entries.forEachIndexed { index, size ->
                                    val label = when (size) {
                                        LockWallpaperWordSize.SMALL -> stringResource(R.string.size_small)
                                        LockWallpaperWordSize.MEDIUM -> stringResource(R.string.size_med)
                                        LockWallpaperWordSize.LARGE -> stringResource(R.string.size_large)
                                        LockWallpaperWordSize.EXTRA_LARGE -> stringResource(R.string.size_xl)
                                        LockWallpaperWordSize.HUGE -> stringResource(R.string.size_huge)
                                    }
                                    SegmentedButton(
                                        selected = lockScreenDraft.wordSize == size,
                                        onClick = {
                                            applyLockScreenDraft(lockScreenDraft.copy(wordSize = size))
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = LockWallpaperWordSize.entries.size)
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        // Vietnamese meaning size
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.lock_screen_vn_size_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.lock_screen_vn_size_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                LockWallpaperVietnameseSize.entries.forEachIndexed { index, size ->
                                    val label = when (size) {
                                        LockWallpaperVietnameseSize.SMALL -> stringResource(R.string.size_small)
                                        LockWallpaperVietnameseSize.MEDIUM -> stringResource(R.string.size_med)
                                        LockWallpaperVietnameseSize.LARGE -> stringResource(R.string.size_large)
                                        LockWallpaperVietnameseSize.EXTRA_LARGE -> stringResource(R.string.size_xl)
                                        LockWallpaperVietnameseSize.HUGE -> stringResource(R.string.size_huge)
                                    }
                                    SegmentedButton(
                                        selected = lockScreenDraft.vietnameseSize == size,
                                        onClick = {
                                            applyLockScreenDraft(lockScreenDraft.copy(vietnameseSize = size))
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = LockWallpaperVietnameseSize.entries.size)
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        // Vocabulary image size
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.lock_screen_img_size_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.lock_screen_img_size_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                LockWallpaperImageSize.entries.forEachIndexed { index, size ->
                                    val label = when (size) {
                                        LockWallpaperImageSize.MEDIUM -> stringResource(R.string.size_med)
                                        LockWallpaperImageSize.LARGE -> stringResource(R.string.size_large)
                                        LockWallpaperImageSize.EXTRA_LARGE -> stringResource(R.string.size_xl)
                                        LockWallpaperImageSize.MAXIMUM -> stringResource(R.string.size_max)
                                    }
                                    SegmentedButton(
                                        selected = lockScreenDraft.imageSize == size,
                                        onClick = {
                                            applyLockScreenDraft(lockScreenDraft.copy(imageSize = size))
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = LockWallpaperImageSize.entries.size)
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        // Card background opacity
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.lock_screen_opacity_title),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                val currentPct = (lockScreenDraft.cardBackgroundOpacity * 100).roundToInt()
                                Text(
                                    text = stringResource(R.string.widget_card_opacity_current, currentPct),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = stringResource(R.string.lock_screen_opacity_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = lockScreenDraft.cardBackgroundOpacity,
                                onValueChange = { newOpacity ->
                                    applyLockScreenDraft(lockScreenDraft.copy(cardBackgroundOpacity = newOpacity))
                                },
                                valueRange = 0.20f..1.00f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Autoplay switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = stringResource(R.string.lock_screen_autoplay_title),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = stringResource(R.string.lock_screen_autoplay_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = lockScreenDraft.autoPlayPronunciation,
                                onCheckedChange = { enabled ->
                                    applyLockScreenDraft(lockScreenDraft.copy(autoPlayPronunciation = enabled))
                                }
                            )
                        }

                        // Quick review interval
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.lock_screen_quick_review_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.lock_screen_quick_review_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val intervals = listOf(2000L to "2s", 3000L to "3s", 5000L to "5s", 8000L to "8s", 10000L to "10s")
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                intervals.forEachIndexed { index, (intervalMs, label) ->
                                    SegmentedButton(
                                        selected = lockScreenDraft.quickReviewIntervalMillis == intervalMs,
                                        onClick = {
                                            applyLockScreenDraft(lockScreenDraft.copy(quickReviewIntervalMillis = intervalMs))
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = intervals.size)
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        // Screen-off preparation
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = stringResource(R.string.lock_screen_screen_off_prep_title),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = stringResource(R.string.lock_screen_screen_off_prep_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = lockScreenDraft.screenOffPreparationEnabled,
                                    onCheckedChange = { enabled ->
                                        applyLockScreenDraft(lockScreenDraft.copy(screenOffPreparationEnabled = enabled))
                                    }
                                )
                            }

                            if (lockScreenDraft.screenOffPreparationEnabled) {
                                Text(
                                    text = stringResource(R.string.lock_screen_prep_delay_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val delays = listOf(0L to stringResource(R.string.delay_instant), 10000L to "10s", 30000L to "30s", 60000L to "1m", 120000L to "2m")
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    delays.forEachIndexed { index, (delayMs, label) ->
                                        SegmentedButton(
                                            selected = lockScreenDraft.screenOffPrepareDelayMillis == delayMs,
                                            onClick = {
                                                applyLockScreenDraft(lockScreenDraft.copy(screenOffPrepareDelayMillis = delayMs))
                                            },
                                            shape = SegmentedButtonDefaults.itemShape(index = index, count = delays.size)
                                        ) {
                                            Text(label, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
