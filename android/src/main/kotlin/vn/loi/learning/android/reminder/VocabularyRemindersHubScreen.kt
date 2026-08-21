package vn.loi.learning.android.reminder

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import vn.loi.learning.android.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyRemindersHubScreen(
    controller: AndroidVocabularyReminderPreferencesController,
    onLockScreenSettings: () -> Unit,
    onUnlockedReminderSettings: () -> Unit,
    onHomeWidgetSettings: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val reminderSettings by controller.settings.collectAsState()
    val lockScreenSettings by controller.lockScreenSettings.collectAsState()
    val homeWidgetSettings by controller.homeWidgetSettings.collectAsState()

    val activeWidgetCount = remember {
        runCatching {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, AndroidHomeVocabularyWidgetProvider::class.java)
            manager.getAppWidgetIds(provider).size
        }.getOrDefault(0)
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
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_reminders_hub_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.reminder_hub_header_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 1. Lock Screen Vocabulary
            val lockStatus = if (lockScreenSettings.enabled) {
                stringResource(R.string.reminder_status_on)
            } else {
                stringResource(R.string.reminder_status_off)
            }
            HubNavigationCard(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.lock_screen_settings_title),
                subtitle = stringResource(R.string.reminder_hub_lock_desc),
                statusText = lockStatus,
                statusPositive = lockScreenSettings.enabled,
                onClick = onLockScreenSettings
            )

            // 2. Unlocked Reminder Popup
            val now = System.currentTimeMillis()
            val isPaused = reminderSettings.isUnlockedPaused || (reminderSettings.pausedUntil?.toEpochMilli() ?: 0L) > now
            val pausedUntilEpoch = if (reminderSettings.unlockedPausedUntilEpochMillis > now) {
                reminderSettings.unlockedPausedUntilEpochMillis
            } else {
                reminderSettings.pausedUntil?.toEpochMilli() ?: 0L
            }
            val reminderStatus = when {
                !reminderSettings.enabled -> stringResource(R.string.reminder_status_off)
                isPaused && pausedUntilEpoch > 0L -> {
                    val timeStr = Instant.ofEpochMilli(pausedUntilEpoch)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                    stringResource(R.string.reminder_status_paused_until, timeStr)
                }
                else -> stringResource(R.string.reminder_status_every_minutes, reminderSettings.intervalMinutes)
            }
            HubNavigationCard(
                icon = Icons.Default.NotificationsActive,
                title = stringResource(R.string.popup_settings_title),
                subtitle = stringResource(R.string.reminder_hub_popup_desc),
                statusText = reminderStatus,
                statusPositive = reminderSettings.enabled && !isPaused,
                statusWarning = isPaused,
                onClick = onUnlockedReminderSettings
            )

            // 3. Home-Screen Vocabulary Widget
            val widgetIntervalSeconds = homeWidgetSettings.intervalMillis / 1000L
            val widgetIntervalLabel = if (widgetIntervalSeconds % 60L == 0L) {
                "${widgetIntervalSeconds / 60L}m"
            } else {
                "${widgetIntervalSeconds}s"
            }
            val widgetStatus = when {
                activeWidgetCount == 0 -> stringResource(R.string.reminder_status_widget_none)
                !homeWidgetSettings.autoNextEnabled -> stringResource(R.string.reminder_status_widget_auto_off)
                !homeWidgetSettings.autoAudioEnabled -> stringResource(R.string.reminder_status_widget_muted, widgetIntervalLabel)
                else -> stringResource(R.string.reminder_status_widget_audio, widgetIntervalLabel)
            }
            HubNavigationCard(
                icon = Icons.Default.Widgets,
                title = stringResource(R.string.widget_settings_title),
                subtitle = stringResource(R.string.reminder_hub_widget_desc),
                statusText = widgetStatus,
                statusPositive = activeWidgetCount > 0 && homeWidgetSettings.autoNextEnabled,
                onClick = onHomeWidgetSettings
            )
        }
    }
}

@Composable
private fun HubNavigationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    statusText: String,
    statusPositive: Boolean = false,
    statusWarning: Boolean = false,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (statusPositive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (statusPositive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        statusPositive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        statusWarning -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            statusPositive -> MaterialTheme.colorScheme.primary
                            statusWarning -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
