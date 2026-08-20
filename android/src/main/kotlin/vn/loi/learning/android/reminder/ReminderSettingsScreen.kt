package vn.loi.learning.android.reminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import vn.loi.learning.android.LearningEngineAndroidApplication

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderSettingsScreen(
    controller: AndroidVocabularyReminderPreferencesController,
    runtime: AndroidVocabularyReminderRuntime,
    selector: AndroidVocabularyReminderCandidateSelector,
    notificationHelper: AndroidVocabularyReminderNotificationHelper,
    onLockScreenSettings: () -> Unit = {},
    onHomeWidgetSettings: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by controller.settings.collectAsState()
    var draft by remember(settings) {
        mutableStateOf(AndroidVocabularyReminderDraft.from(settings))
    }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasPermission by remember {
        mutableStateOf(notificationHelper.hasNotificationPermission())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPermission = notificationHelper.hasNotificationPermission()
                hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val openOverlaySettings = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            )
            runCatching {
                context.startActivity(intent)
            }.onFailure {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                runCatching { context.startActivity(fallbackIntent) }
            }
        }
    }

    val availablePackages = remember { selector.getAvailablePackages() }
    var packageDropdownExpanded by remember { mutableStateOf(false) }

    // Auto-select package if none selected but packages exist
    LaunchedEffect(availablePackages, draft.selectedPackageId) {
        if (draft.selectedPackageId == null && availablePackages.isNotEmpty()) {
            draft = draft.copy(selectedPackageId = availablePackages.first().id)
            draft.validate(settings.pausedUntil).let { validation ->
                if (validation is AndroidVocabularyReminderDraftValidation.Valid) {
                    controller.updateSettings(validation.settings)
                }
            }
        }
    }

    val applyDraft: (AndroidVocabularyReminderDraft) -> Unit = { nextDraft ->
        draft = nextDraft
        val validation = nextDraft.validate(settings.pausedUntil)
        if (validation is AndroidVocabularyReminderDraftValidation.Valid) {
            controller.updateSettings(validation.settings)
            AndroidLockScreenVocabularyService.reconcile(context, "REMINDER_SETTINGS_CHANGED")
        }
    }

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back from Unlocked Reminder Settings"
                        )
                    }
                    Text(
                        text = "Unlocked Reminder Popup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Banner if missing
            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification permission required",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Grant permission to receive periodic vocabulary reminders.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Grant")
                        }
                    }
                }
            }

            // 1. Enable Switch Card
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (settings.enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (settings.enabled) Icons.Filled.NotificationsActive else Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = if (settings.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column {
                            Text(
                                text = "Enable Unlocked Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (settings.enabled) "Reminders are actively scheduled" else "Reminders are paused/off",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            controller.setEnabled(enabled)
                        }
                    )
                }
            }

            // Active Pause Banner with Resume Now button
            val nowEpoch = System.currentTimeMillis()
            val isPaused = settings.unlockedPausedUntilEpochMillis > nowEpoch
            if (settings.enabled && isPaused) {
                val remainingMinutes = ((settings.unlockedPausedUntilEpochMillis - nowEpoch) / 60_000L).coerceAtLeast(1L)
                val pausedUntilTimeStr = Instant.ofEpochMilli(settings.unlockedPausedUntilEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("HH:mm"))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsPaused,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Column {
                                Text(
                                    text = "Paused ($remainingMinutes min remaining)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Paused until $pausedUntilTimeStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                val appInstance = context.applicationContext as? LearningEngineAndroidApplication
                                appInstance?.lockScreenVocabularyCoordinator?.resumeUnlockedNow()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Resume now")
                        }
                    }
                }
            }

            // 2. Package Selector
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Learning Package",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    ExposedDropdownMenuBox(
                        expanded = packageDropdownExpanded,
                        onExpandedChange = { packageDropdownExpanded = it }
                    ) {
                        val currentPkgName = availablePackages.firstOrNull { it.id == draft.selectedPackageId }?.name
                            ?: draft.selectedPackageId ?: "Select a package"

                        OutlinedTextField(
                            value = currentPkgName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = packageDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = packageDropdownExpanded,
                            onDismissRequest = { packageDropdownExpanded = false }
                        ) {
                            if (availablePackages.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No active packages installed") },
                                    onClick = { packageDropdownExpanded = false }
                                )
                            } else {
                                availablePackages.forEach { pkg ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(pkg.name, fontWeight = FontWeight.SemiBold)
                                                Text("${pkg.totalItemCount} items", style = MaterialTheme.typography.bodySmall)
                                            }
                                        },
                                        onClick = {
                                            packageDropdownExpanded = false
                                            applyDraft(draft.copy(selectedPackageId = pkg.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Reminder Mode Selector
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Reminder Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AndroidVocabularyReminderSelectionMode.entries.forEach { mode ->
                            val label = when (mode) {
                                AndroidVocabularyReminderSelectionMode.AGAIN_HARD -> "Again / Hard"
                                AndroidVocabularyReminderSelectionMode.DUE -> "Due items"
                                AndroidVocabularyReminderSelectionMode.RANDOM_LEARNED -> "Random learned"
                                AndroidVocabularyReminderSelectionMode.RANDOM_ALL -> "Random all"
                                AndroidVocabularyReminderSelectionMode.MARKED_DIFFICULT -> "Marked difficult"
                            }
                            FilterChip(
                                selected = draft.selectionMode == mode,
                                onClick = { applyDraft(draft.copy(selectionMode = mode)) },
                                label = { Text(label) },
                                leadingIcon = if (draft.selectionMode == mode) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // 4. Interval Configuration
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Reminder Interval",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draft.intervalValueText,
                            onValueChange = { nextVal ->
                                applyDraft(draft.copy(intervalValueText = nextVal))
                            },
                            label = { Text("Value") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = draft.intervalUnit == AndroidVocabularyReminderIntervalUnit.SECONDS,
                                onClick = {
                                    applyDraft(draft.copy(intervalUnit = AndroidVocabularyReminderIntervalUnit.SECONDS))
                                },
                                label = { Text("Sec") }
                            )
                            FilterChip(
                                selected = draft.intervalUnit == AndroidVocabularyReminderIntervalUnit.MINUTES,
                                onClick = {
                                    applyDraft(draft.copy(intervalUnit = AndroidVocabularyReminderIntervalUnit.MINUTES))
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
                            "20" to AndroidVocabularyReminderIntervalUnit.SECONDS,
                            "30" to AndroidVocabularyReminderIntervalUnit.SECONDS,
                            "45" to AndroidVocabularyReminderIntervalUnit.SECONDS,
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
                                    applyDraft(draft.copy(intervalValueText = v, intervalUnit = u))
                                },
                                label = { Text("$v$unitStr") }
                            )
                        }
                    }
                }
            }

            // 5. Active Time Window & Display Duration
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Active Time Window & Duration",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = draft.activeStartText,
                            onValueChange = { applyDraft(draft.copy(activeStartText = it)) },
                            label = { Text("Active from") },
                            placeholder = { Text("08:00") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = draft.activeEndText,
                            onValueChange = { applyDraft(draft.copy(activeEndText = it)) },
                            label = { Text("Active until") },
                            placeholder = { Text("22:00") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = draft.displayDurationText,
                        onValueChange = { applyDraft(draft.copy(displayDurationText = it)) },
                        label = { Text("Notification duration (seconds)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Play pronunciation on reminder",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = draft.autoPlayPronunciation,
                            onCheckedChange = { applyDraft(draft.copy(autoPlayPronunciation = it)) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Large reminder popup",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Show a large vocabulary card over other apps when the screen is unlocked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = draft.overlayPopupEnabled,
                            onCheckedChange = { enabled ->
                                applyDraft(draft.copy(overlayPopupEnabled = enabled))
                                if (enabled && !hasOverlayPermission) {
                                    openOverlaySettings()
                                }
                            }
                        )
                    }

                    if (draft.overlayPopupEnabled && !hasOverlayPermission) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "Large popup permission required",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Grant 'Display over other apps' to allow large vocabulary popup cards.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Button(
                                    onClick = { openOverlaySettings() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Grant permission", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Quick pause actions switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Quick pause actions",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Show Pause 5m, 30m, 1h buttons on the unlocked reminder overlay card.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = draft.quickPauseActionsEnabled,
                            onCheckedChange = { enabled ->
                                applyDraft(draft.copy(quickPauseActionsEnabled = enabled))
                            }
                        )
                    }
                }
            }

            // 6. Pause quick actions
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Quick Pause Reminders",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { runtime.pause30Minutes() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("Pause 30m", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { runtime.pauseOneHour() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("Pause 1h", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { runtime.pauseToday() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("Pause today", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 7. Heads-up / Floating notification system setting helper
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Heads-up / Floating Banners",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "On Xiaomi / HyperOS, enable 'Floating notifications' (Thông báo nổi) in system channel settings for instant popups.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    putExtra(Settings.EXTRA_CHANNEL_ID, AndroidVocabularyReminderNotificationHelper.CHANNEL_ID)
                                }
                            } else {
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    ) {
                        Text("Configure")
                    }
                }
            }

            // 8. Preview notification button
            Button(
                onClick = {
                    if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    val result = runtime.preview(draft)
                    if (result is AndroidVocabularyReminderActionResult.Failure) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(result.message)
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Preview notification sent.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Preview notification")
            }

            // 9. Other surfaces cross-navigation cards
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Other Vocabulary Surfaces",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLockScreenSettings)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lock Screen Vocabulary", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Configure lock screen wallpaper cards", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onHomeWidgetSettings)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Home-Screen Vocabulary Widget", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Configure Home screen widget card & auto-rotation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
