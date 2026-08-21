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
                title = { Text("Lock Screen Vocabulary") },
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
                                text = "Lock-Screen Wallpaper Vocabulary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Displays vocabulary on the lock screen via wallpaper. Native fingerprint and SystemUI Keyguard remain 100% functional.",
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
                                text = "Note: Vocabulary is displayed by updating the lock-screen wallpaper. Your lock-screen wallpaper will be updated automatically on screen lock. Home screen wallpaper remains unchanged.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        // Lock-Screen Package Selector
                        val selectedPackageName = remember(availablePackages, lockScreenDraft.selectedPackageId) {
                            val id = lockScreenDraft.selectedPackageId
                            if (id == null) {
                                availablePackages.firstOrNull()?.name ?: "No packages installed"
                            } else {
                                availablePackages.firstOrNull { it.id == id }?.name ?: "Package unavailable"
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Lock-Screen Package",
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
                                            text = { Text("No active packages available") },
                                            onClick = { lockScreenPackageDropdownExpanded = false }
                                        )
                                    } else {
                                        availablePackages.forEach { pkg ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(pkg.name, fontWeight = FontWeight.Medium)
                                                        Text("${pkg.totalItemCount} items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            AndroidLockScreenVocabularyMode.AGAIN_HARD to "Again / Hard",
                            AndroidLockScreenVocabularyMode.DUE to "Due",
                            AndroidLockScreenVocabularyMode.NEW_UNSEEN to "New / Unseen",
                            AndroidLockScreenVocabularyMode.RANDOM_LEARNED to "Random Learned",
                            AndroidLockScreenVocabularyMode.MARKED_DIFFICULT to "Marked Difficult",
                            AndroidLockScreenVocabularyMode.RANDOM_ALL to "Random All"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Candidate Pool",
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
                                text = "Lock Wallpaper Background",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Learning Engine creates the lock-screen wallpaper using your selected background and the current vocabulary card.",
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
                                text = "English word size",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Controls how prominently the English word appears on the lock screen.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                LockWallpaperWordSize.entries.forEachIndexed { index, size ->
                                    val label = when (size) {
                                        LockWallpaperWordSize.SMALL -> "Small"
                                        LockWallpaperWordSize.MEDIUM -> "Med"
                                        LockWallpaperWordSize.LARGE -> "Large"
                                        LockWallpaperWordSize.EXTRA_LARGE -> "XL"
                                        LockWallpaperWordSize.HUGE -> "Huge"
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
                                text = "Vietnamese meaning size",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Controls the size of the Vietnamese translation text on the lock screen.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                LockWallpaperVietnameseSize.entries.forEachIndexed { index, size ->
                                    val label = when (size) {
                                        LockWallpaperVietnameseSize.SMALL -> "Small"
                                        LockWallpaperVietnameseSize.MEDIUM -> "Med"
                                        LockWallpaperVietnameseSize.LARGE -> "Large"
                                        LockWallpaperVietnameseSize.EXTRA_LARGE -> "XL"
                                        LockWallpaperVietnameseSize.HUGE -> "Huge"
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
                                text = "Vocabulary image size",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Controls how much of the available lock-screen card is used by the vocabulary image.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                LockWallpaperImageSize.entries.forEachIndexed { index, size ->
                                    val label = when (size) {
                                        LockWallpaperImageSize.MEDIUM -> "Med"
                                        LockWallpaperImageSize.LARGE -> "Large"
                                        LockWallpaperImageSize.EXTRA_LARGE -> "XL"
                                        LockWallpaperImageSize.MAXIMUM -> "Max"
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
                                    text = "Card background opacity",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                val currentPct = (lockScreenDraft.cardBackgroundOpacity * 100).roundToInt()
                                Text(
                                    text = "Current: $currentPct%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Controls how strongly the vocabulary card background covers your lock-screen wallpaper.",
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
                                    text = "Auto-play pronunciation",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Play the word pronunciation once when the lock screen wakes.",
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
                                text = "Next word every (Quick Review)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Rapidly advances to the next vocabulary word on lock screen after pronunciation completes.",
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
                                        text = "Prepare next word while screen is off",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Prepares the next word in the background while the screen is off. Does not wake screen or show popup.",
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
                                    text = "Prepare after delay",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val delays = listOf(0L to "Instant", 10000L to "10s", 30000L to "30s", 60000L to "1m", 120000L to "2m")
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
