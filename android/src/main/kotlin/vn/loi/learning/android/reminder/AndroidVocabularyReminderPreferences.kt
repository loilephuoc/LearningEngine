package vn.loi.learning.android.reminder

import android.content.Context
import android.content.SharedPreferences
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AndroidVocabularyReminderPreferenceStore {
    fun load(): AndroidVocabularyReminderSettings
    fun save(settings: AndroidVocabularyReminderSettings): Boolean
}

class SharedPreferencesVocabularyReminderPreferenceStore(
    context: Context,
    private val preferencesName: String = PREFS_NAME
) : AndroidVocabularyReminderPreferenceStore {
    private val prefs: SharedPreferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun load(): AndroidVocabularyReminderSettings {
        val defaults = AndroidVocabularyReminderSettings()
        val enabled = prefs.getBoolean(KEY_ENABLED, defaults.enabled)
        val selectedPackageId = prefs.getString(KEY_SELECTED_PACKAGE_ID, null)?.takeIf { it.isNotBlank() }
        val modeName = prefs.getString(KEY_SELECTION_MODE, defaults.selectionMode.name)
        val selectionMode = runCatching {
            AndroidVocabularyReminderSelectionMode.valueOf(modeName ?: defaults.selectionMode.name)
        }.getOrDefault(defaults.selectionMode)

        val intervalMillis = when {
            prefs.contains(KEY_INTERVAL_MILLIS) -> prefs.getLong(KEY_INTERVAL_MILLIS, defaults.intervalMillis)
            prefs.contains(KEY_INTERVAL_MINUTES) -> prefs.getInt(KEY_INTERVAL_MINUTES, defaults.intervalMinutes) * 60_000L
            else -> defaults.intervalMillis
        }.coerceIn(
            AndroidVocabularyReminderSettings.MIN_INTERVAL_MILLIS,
            AndroidVocabularyReminderSettings.MAX_INTERVAL_MILLIS
        )

        val activeStartStr = prefs.getString(KEY_ACTIVE_START, null)
        val activeStart = runCatching { LocalTime.parse(activeStartStr) }.getOrDefault(defaults.activeStart)

        val activeEndStr = prefs.getString(KEY_ACTIVE_END, null)
        val activeEnd = runCatching { LocalTime.parse(activeEndStr) }.getOrDefault(defaults.activeEnd)

        val displayDurationMillis = prefs.getLong(KEY_DISPLAY_DURATION_MILLIS, defaults.displayDurationMillis)
            .coerceIn(
                AndroidVocabularyReminderSettings.MIN_DISPLAY_DURATION_MILLIS,
                AndroidVocabularyReminderSettings.MAX_DISPLAY_DURATION_MILLIS
            )

        val autoPlayPronunciation = prefs.getBoolean(KEY_AUTOPLAY_PRONUNCIATION, defaults.autoPlayPronunciation)
        val overlayPopupEnabled = prefs.getBoolean(KEY_OVERLAY_POPUP_ENABLED, defaults.overlayPopupEnabled)

        val pausedUntilMillis = if (prefs.contains(KEY_PAUSED_UNTIL)) prefs.getLong(KEY_PAUSED_UNTIL, -1L) else -1L
        val pausedUntil = if (pausedUntilMillis > 0) Instant.ofEpochMilli(pausedUntilMillis) else null

        return AndroidVocabularyReminderSettings(
            enabled = enabled,
            selectedPackageId = selectedPackageId,
            selectionMode = selectionMode,
            intervalMillis = intervalMillis,
            activeStart = activeStart,
            activeEnd = activeEnd,
            displayDurationMillis = displayDurationMillis,
            autoPlayPronunciation = autoPlayPronunciation,
            overlayPopupEnabled = overlayPopupEnabled,
            pausedUntil = pausedUntil
        )
    }

    override fun save(settings: AndroidVocabularyReminderSettings): Boolean {
        return prefs.edit().apply {
            putBoolean(KEY_ENABLED, settings.enabled)
            putString(KEY_SELECTED_PACKAGE_ID, settings.selectedPackageId)
            putString(KEY_SELECTION_MODE, settings.selectionMode.name)
            putLong(KEY_INTERVAL_MILLIS, settings.intervalMillis)
            remove(KEY_INTERVAL_MINUTES)
            putString(KEY_ACTIVE_START, settings.activeStart.toString())
            putString(KEY_ACTIVE_END, settings.activeEnd.toString())
            putLong(KEY_DISPLAY_DURATION_MILLIS, settings.displayDurationMillis)
            putBoolean(KEY_AUTOPLAY_PRONUNCIATION, settings.autoPlayPronunciation)
            putBoolean(KEY_OVERLAY_POPUP_ENABLED, settings.overlayPopupEnabled)
            if (settings.pausedUntil != null) {
                putLong(KEY_PAUSED_UNTIL, settings.pausedUntil.toEpochMilli())
            } else {
                remove(KEY_PAUSED_UNTIL)
            }
        }.commit()
    }

    companion object {
        const val PREFS_NAME = "learning_engine_reminder_prefs"
        private const val KEY_ENABLED = "reminder.enabled"
        private const val KEY_SELECTED_PACKAGE_ID = "reminder.selected_package_id"
        private const val KEY_SELECTION_MODE = "reminder.selection_mode"
        private const val KEY_INTERVAL_MILLIS = "reminder.interval_millis"
        private const val KEY_INTERVAL_MINUTES = "reminder.interval_minutes"
        private const val KEY_ACTIVE_START = "reminder.active_start"
        private const val KEY_ACTIVE_END = "reminder.active_end"
        private const val KEY_DISPLAY_DURATION_MILLIS = "reminder.display_duration_millis"
        private const val KEY_AUTOPLAY_PRONUNCIATION = "reminder.autoplay_pronunciation"
        private const val KEY_OVERLAY_POPUP_ENABLED = "reminder.overlay_popup_enabled"
        private const val KEY_PAUSED_UNTIL = "reminder.paused_until_epoch_millis"
    }
}

class AndroidVocabularyReminderPreferencesController(
    private val store: AndroidVocabularyReminderPreferenceStore
) {
    private val mutableSettings = MutableStateFlow(store.load())
    val settings: StateFlow<AndroidVocabularyReminderSettings> = mutableSettings.asStateFlow()

    fun current(): AndroidVocabularyReminderSettings = mutableSettings.value

    fun updateSettings(newSettings: AndroidVocabularyReminderSettings): Boolean {
        val saved = store.save(newSettings)
        if (saved) {
            mutableSettings.value = newSettings
        }
        return saved
    }

    fun setEnabled(enabled: Boolean): Boolean {
        return updateSettings(mutableSettings.value.copy(enabled = enabled))
    }

    fun pause30Minutes(): Boolean = pauseFor(Duration.ofMinutes(30))
    fun pauseOneHour(): Boolean = pauseFor(Duration.ofHours(1))
    fun pauseToday(now: Instant = Instant.now()): Boolean {
        val nextDay = java.time.LocalDate.now().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
        return updateSettings(mutableSettings.value.copy(pausedUntil = nextDay))
    }
    fun resumeNow(): Boolean = updateSettings(mutableSettings.value.copy(pausedUntil = null))

    private fun pauseFor(duration: Duration, now: Instant = Instant.now()): Boolean {
        return updateSettings(mutableSettings.value.copy(pausedUntil = now.plus(duration)))
    }
}
