package vn.loi.learning.android.reminder

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AndroidVocabularyReminderPreferenceStore {
    fun load(): AndroidVocabularyReminderSettings
    fun save(settings: AndroidVocabularyReminderSettings): Boolean
    fun loadLockScreen(): AndroidLockScreenVocabularySettings = AndroidLockScreenVocabularySettings()
    fun saveLockScreen(settings: AndroidLockScreenVocabularySettings): Boolean = true
    fun loadHomeWidget(): AndroidHomeVocabularyWidgetSettings = AndroidHomeVocabularyWidgetSettings()
    fun saveHomeWidget(settings: AndroidHomeVocabularyWidgetSettings): Boolean = true
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

        val quickPauseActionsEnabled = prefs.getBoolean(KEY_REMINDER_QUICK_PAUSE_ACTIONS_ENABLED, defaults.quickPauseActionsEnabled)
        val unlockedPausedUntilEpochMillis = prefs.getLong(KEY_REMINDER_UNLOCKED_PAUSED_UNTIL, defaults.unlockedPausedUntilEpochMillis)

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
            pausedUntil = pausedUntil,
            quickPauseActionsEnabled = quickPauseActionsEnabled,
            unlockedPausedUntilEpochMillis = unlockedPausedUntilEpochMillis
        )
    }

    override fun save(settings: AndroidVocabularyReminderSettings): Boolean {
        val pausedUntilMillis = settings.pausedUntil?.toEpochMilli() ?: -1L
        return prefs.edit().apply {
            putBoolean(KEY_ENABLED, settings.enabled)
            putString(KEY_SELECTED_PACKAGE_ID, settings.selectedPackageId)
            putString(KEY_SELECTION_MODE, settings.selectionMode.name)
            putLong(KEY_INTERVAL_MILLIS, settings.intervalMillis)
            putInt(KEY_INTERVAL_MINUTES, (settings.intervalMillis / 60_000L).toInt().coerceAtLeast(1))
            putString(KEY_ACTIVE_START, settings.activeStart.toString())
            putString(KEY_ACTIVE_END, settings.activeEnd.toString())
            putLong(KEY_DISPLAY_DURATION_MILLIS, settings.displayDurationMillis)
            putBoolean(KEY_AUTOPLAY_PRONUNCIATION, settings.autoPlayPronunciation)
            putBoolean(KEY_OVERLAY_POPUP_ENABLED, settings.overlayPopupEnabled)
            if (pausedUntilMillis > 0) {
                putLong(KEY_PAUSED_UNTIL, pausedUntilMillis)
            } else {
                remove(KEY_PAUSED_UNTIL)
            }
            putBoolean(KEY_REMINDER_QUICK_PAUSE_ACTIONS_ENABLED, settings.quickPauseActionsEnabled)
            putLong(KEY_REMINDER_UNLOCKED_PAUSED_UNTIL, settings.unlockedPausedUntilEpochMillis)
        }.commit()
    }

    override fun loadLockScreen(): AndroidLockScreenVocabularySettings {
        val defaults = AndroidLockScreenVocabularySettings()
        val enabled = prefs.getBoolean(KEY_LOCKSCREEN_ENABLED, defaults.enabled)
        val selectedPackageId = prefs.getString(KEY_LOCKSCREEN_PACKAGE_ID, null)?.takeIf { it.isNotBlank() }
        val modeName = prefs.getString(KEY_LOCKSCREEN_SELECTION_MODE, defaults.selectionMode.name)
        val selectionMode = runCatching {
            AndroidLockScreenVocabularyMode.valueOf(modeName ?: defaults.selectionMode.name)
        }.getOrDefault(defaults.selectionMode)

        val autoPlay = prefs.getBoolean(KEY_LOCKSCREEN_AUTOPLAY_PRONUNCIATION, defaults.autoPlayPronunciation)
        val customBackgroundPath = prefs.getString(KEY_LOCKSCREEN_CUSTOM_BACKGROUND_PATH, null)?.takeIf { it.isNotBlank() }

        val wordSizeName = prefs.getString(KEY_LOCKSCREEN_WORD_SIZE, defaults.wordSize.name)
        val wordSize = runCatching {
            LockWallpaperWordSize.valueOf(wordSizeName ?: defaults.wordSize.name)
        }.getOrDefault(defaults.wordSize)

        val vietnameseSizeName = prefs.getString(KEY_LOCKSCREEN_VIETNAMESE_SIZE, defaults.vietnameseSize.name)
        val vietnameseSize = runCatching {
            LockWallpaperVietnameseSize.valueOf(vietnameseSizeName ?: defaults.vietnameseSize.name)
        }.getOrDefault(defaults.vietnameseSize)

        val imageSizeName = prefs.getString(KEY_LOCKSCREEN_IMAGE_SIZE, defaults.imageSize.name)
        val imageSize = runCatching {
            LockWallpaperImageSize.valueOf(imageSizeName ?: defaults.imageSize.name)
        }.getOrDefault(defaults.imageSize)

        val opacity = prefs.getFloat(KEY_LOCKSCREEN_CARD_BACKGROUND_OPACITY, defaults.cardBackgroundOpacity)
            .coerceIn(0.0f, 1.0f)

        val quickReviewIntervalMillis = prefs.getLong(KEY_LOCKSCREEN_QUICK_REVIEW_INTERVAL_MS, defaults.quickReviewIntervalMillis)
        val screenOffPrepEnabled = prefs.getBoolean(KEY_LOCKSCREEN_SCREEN_OFF_PREP_ENABLED, defaults.screenOffPreparationEnabled)
        val screenOffPrepareDelayMillis = prefs.getLong(KEY_LOCKSCREEN_SCREEN_OFF_PREPARE_DELAY_MS, defaults.screenOffPrepareDelayMillis)

        return AndroidLockScreenVocabularySettings(
            enabled = enabled,
            selectedPackageId = selectedPackageId,
            selectionMode = selectionMode,
            autoPlayPronunciation = autoPlay,
            customBackgroundPath = customBackgroundPath,
            wordSize = wordSize,
            vietnameseSize = vietnameseSize,
            imageSize = imageSize,
            cardBackgroundOpacity = opacity,
            quickReviewIntervalMillis = quickReviewIntervalMillis,
            screenOffPreparationEnabled = screenOffPrepEnabled,
            screenOffPrepareDelayMillis = screenOffPrepareDelayMillis
        )
    }

    override fun saveLockScreen(settings: AndroidLockScreenVocabularySettings): Boolean {
        return prefs.edit().apply {
            putBoolean(KEY_LOCKSCREEN_ENABLED, settings.enabled)
            putString(KEY_LOCKSCREEN_PACKAGE_ID, settings.selectedPackageId)
            putString(KEY_LOCKSCREEN_SELECTION_MODE, settings.selectionMode.name)
            putBoolean(KEY_LOCKSCREEN_AUTOPLAY_PRONUNCIATION, settings.autoPlayPronunciation)
            if (settings.customBackgroundPath != null) {
                putString(KEY_LOCKSCREEN_CUSTOM_BACKGROUND_PATH, settings.customBackgroundPath)
            } else {
                remove(KEY_LOCKSCREEN_CUSTOM_BACKGROUND_PATH)
            }
            putString(KEY_LOCKSCREEN_WORD_SIZE, settings.wordSize.name)
            putString(KEY_LOCKSCREEN_VIETNAMESE_SIZE, settings.vietnameseSize.name)
            putString(KEY_LOCKSCREEN_IMAGE_SIZE, settings.imageSize.name)
            putFloat(KEY_LOCKSCREEN_CARD_BACKGROUND_OPACITY, settings.clampedCardBackgroundOpacity)
            putLong(KEY_LOCKSCREEN_QUICK_REVIEW_INTERVAL_MS, settings.quickReviewIntervalMillis)
            putBoolean(KEY_LOCKSCREEN_SCREEN_OFF_PREP_ENABLED, settings.screenOffPreparationEnabled)
            putLong(KEY_LOCKSCREEN_SCREEN_OFF_PREPARE_DELAY_MS, settings.screenOffPrepareDelayMillis)
        }.commit()
    }

    override fun loadHomeWidget(): AndroidHomeVocabularyWidgetSettings {
        val defaults = AndroidHomeVocabularyWidgetSettings()
        val autoNext = prefs.getBoolean(KEY_HOME_WIDGET_AUTONEXT_ENABLED, defaults.autoNextEnabled)
        val interval = prefs.getLong(KEY_HOME_WIDGET_INTERVAL_MS, defaults.intervalMillis)
        val selectedPackageId = prefs.getString(KEY_HOME_WIDGET_PACKAGE_ID, null)?.takeIf { it.isNotBlank() }
        val modeName = prefs.getString(KEY_HOME_WIDGET_SELECTION_MODE, defaults.selectionMode.name)
        val selectionMode = runCatching {
            AndroidVocabularyReminderSelectionMode.valueOf(modeName ?: defaults.selectionMode.name)
        }.getOrDefault(defaults.selectionMode)

        val wordSizeName = prefs.getString(KEY_HOME_WIDGET_WORD_SIZE, defaults.wordSize.name)
        val wordSize = runCatching {
            LockWallpaperWordSize.valueOf(wordSizeName ?: defaults.wordSize.name)
        }.getOrDefault(defaults.wordSize)

        val vietnameseSizeName = prefs.getString(KEY_HOME_WIDGET_VIETNAMESE_SIZE, defaults.vietnameseSize.name)
        val vietnameseSize = runCatching {
            LockWallpaperVietnameseSize.valueOf(vietnameseSizeName ?: defaults.vietnameseSize.name)
        }.getOrDefault(defaults.vietnameseSize)

        val imageSizeName = prefs.getString(KEY_HOME_WIDGET_IMAGE_SIZE, defaults.imageSize.name)
        val imageSize = runCatching {
            LockWallpaperImageSize.valueOf(imageSizeName ?: defaults.imageSize.name)
        }.getOrDefault(defaults.imageSize)

        val opacity = prefs.getFloat(KEY_HOME_WIDGET_CARD_BACKGROUND_OPACITY, defaults.cardBackgroundOpacity)
            .coerceIn(0.20f, 1.0f)
        val updateOnlyScreenOn = prefs.getBoolean(KEY_HOME_WIDGET_UPDATE_ONLY_SCREEN_ON, defaults.updateOnlyScreenOn)
        val currentCandidateId = prefs.getString(KEY_HOME_WIDGET_CURRENT_CANDIDATE_ID, null)?.takeIf { it.isNotBlank() }
        val autoAudioEnabled = prefs.getBoolean(KEY_HOME_WIDGET_AUTOAUDIO_ENABLED, defaults.autoAudioEnabled)

        return AndroidHomeVocabularyWidgetSettings(
            autoNextEnabled = autoNext,
            intervalMillis = interval,
            selectedPackageId = selectedPackageId,
            selectionMode = selectionMode,
            wordSize = wordSize,
            vietnameseSize = vietnameseSize,
            imageSize = imageSize,
            cardBackgroundOpacity = opacity,
            updateOnlyScreenOn = updateOnlyScreenOn,
            currentCandidateId = currentCandidateId,
            autoAudioEnabled = autoAudioEnabled
        )
    }

    override fun saveHomeWidget(settings: AndroidHomeVocabularyWidgetSettings): Boolean {
        return prefs.edit().apply {
            putBoolean(KEY_HOME_WIDGET_AUTONEXT_ENABLED, settings.autoNextEnabled)
            putLong(KEY_HOME_WIDGET_INTERVAL_MS, settings.intervalMillis)
            putString(KEY_HOME_WIDGET_PACKAGE_ID, settings.selectedPackageId)
            putString(KEY_HOME_WIDGET_SELECTION_MODE, settings.selectionMode.name)
            putString(KEY_HOME_WIDGET_WORD_SIZE, settings.wordSize.name)
            putString(KEY_HOME_WIDGET_VIETNAMESE_SIZE, settings.vietnameseSize.name)
            putString(KEY_HOME_WIDGET_IMAGE_SIZE, settings.imageSize.name)
            putFloat(KEY_HOME_WIDGET_CARD_BACKGROUND_OPACITY, settings.clampedCardBackgroundOpacity)
            putBoolean(KEY_HOME_WIDGET_UPDATE_ONLY_SCREEN_ON, settings.updateOnlyScreenOn)
            if (settings.currentCandidateId != null) {
                putString(KEY_HOME_WIDGET_CURRENT_CANDIDATE_ID, settings.currentCandidateId)
            } else {
                remove(KEY_HOME_WIDGET_CURRENT_CANDIDATE_ID)
            }
            putBoolean(KEY_HOME_WIDGET_AUTOAUDIO_ENABLED, settings.autoAudioEnabled)
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
        private const val KEY_REMINDER_QUICK_PAUSE_ACTIONS_ENABLED = "reminder.quick_pause_actions_enabled"
        private const val KEY_REMINDER_UNLOCKED_PAUSED_UNTIL = "reminder.unlocked_paused_until_epoch_millis"

        private const val KEY_LOCKSCREEN_ENABLED = "lockscreen.enabled"
        private const val KEY_LOCKSCREEN_PACKAGE_ID = "lockscreen.selected_package_id"
        private const val KEY_LOCKSCREEN_SELECTION_MODE = "lockscreen.selection_mode"
        private const val KEY_LOCKSCREEN_AUTOPLAY_PRONUNCIATION = "lockscreen.autoplay_pronunciation"
        private const val KEY_LOCKSCREEN_CUSTOM_BACKGROUND_PATH = "lockscreen.custom_background_path"
        private const val KEY_LOCKSCREEN_WORD_SIZE = "lockscreen.word_size"
        private const val KEY_LOCKSCREEN_VIETNAMESE_SIZE = "lockscreen.vietnamese_size"
        private const val KEY_LOCKSCREEN_IMAGE_SIZE = "lockscreen.image_size"
        private const val KEY_LOCKSCREEN_CARD_BACKGROUND_OPACITY = "lockscreen.card_background_opacity"
        private const val KEY_LOCKSCREEN_QUICK_REVIEW_INTERVAL_MS = "lockscreen.quick_review_interval_ms"
        private const val KEY_LOCKSCREEN_SCREEN_OFF_PREP_ENABLED = "lockscreen.screen_off_prep_enabled"
        private const val KEY_LOCKSCREEN_SCREEN_OFF_PREPARE_DELAY_MS = "lockscreen.screen_off_prepare_delay_ms"

        private const val KEY_HOME_WIDGET_AUTONEXT_ENABLED = "home_widget.autonext_enabled"
        private const val KEY_HOME_WIDGET_INTERVAL_MS = "home_widget.interval_ms"
        private const val KEY_HOME_WIDGET_PACKAGE_ID = "home_widget.selected_package_id"
        private const val KEY_HOME_WIDGET_SELECTION_MODE = "home_widget.selection_mode"
        private const val KEY_HOME_WIDGET_WORD_SIZE = "home_widget.word_size"
        private const val KEY_HOME_WIDGET_VIETNAMESE_SIZE = "home_widget.vietnamese_size"
        private const val KEY_HOME_WIDGET_IMAGE_SIZE = "home_widget.image_size"
        private const val KEY_HOME_WIDGET_CARD_BACKGROUND_OPACITY = "home_widget.card_background_opacity"
        private const val KEY_HOME_WIDGET_UPDATE_ONLY_SCREEN_ON = "home_widget.update_only_screen_on"
        private const val KEY_HOME_WIDGET_CURRENT_CANDIDATE_ID = "home_widget.current_candidate_id"
        private const val KEY_HOME_WIDGET_AUTOAUDIO_ENABLED = "home_widget.autoaudio_enabled"
    }
}

class AndroidVocabularyReminderPreferencesController(
    private val store: AndroidVocabularyReminderPreferenceStore
) {
    private val mutableSettings = MutableStateFlow(store.load())
    val settings: StateFlow<AndroidVocabularyReminderSettings> = mutableSettings.asStateFlow()

    private val mutableLockScreenSettings = MutableStateFlow(store.loadLockScreen())
    val lockScreenSettings: StateFlow<AndroidLockScreenVocabularySettings> = mutableLockScreenSettings.asStateFlow()

    private val mutableHomeWidgetSettings = MutableStateFlow(store.loadHomeWidget())
    val homeWidgetSettings: StateFlow<AndroidHomeVocabularyWidgetSettings> = mutableHomeWidgetSettings.asStateFlow()

    fun current(): AndroidVocabularyReminderSettings = mutableSettings.value
    fun currentLockScreen(): AndroidLockScreenVocabularySettings = mutableLockScreenSettings.value
    fun currentHomeWidget(): AndroidHomeVocabularyWidgetSettings = mutableHomeWidgetSettings.value

    fun updateSettings(newSettings: AndroidVocabularyReminderSettings): Boolean {
        if (mutableSettings.value == newSettings) return true
        val saved = store.save(newSettings)
        if (saved) {
            mutableSettings.value = newSettings
        }
        return saved
    }

    fun updateLockScreenSettings(newSettings: AndroidLockScreenVocabularySettings): Boolean {
        if (mutableLockScreenSettings.value == newSettings) return true
        val saved = store.saveLockScreen(newSettings)
        if (saved) {
            mutableLockScreenSettings.value = newSettings
        }
        return saved
    }

    fun updateHomeWidgetSettings(newSettings: AndroidHomeVocabularyWidgetSettings): Boolean {
        if (mutableHomeWidgetSettings.value == newSettings) return true
        val saved = store.saveHomeWidget(newSettings)
        if (saved) {
            mutableHomeWidgetSettings.value = newSettings
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

    fun pauseUnlocked(duration: Duration, now: Instant = Instant.now()): Boolean {
        val untilEpoch = now.toEpochMilli() + duration.toMillis()
        Log.i("UnlockedPause", "[UnlockedPause] duration=${duration.toMinutes()}m pausedUntil=$untilEpoch")
        return updateSettings(mutableSettings.value.copy(unlockedPausedUntilEpochMillis = untilEpoch))
    }

    fun pauseUnlocked5Minutes(): Boolean = pauseUnlocked(Duration.ofMinutes(5))
    fun pauseUnlocked30Minutes(): Boolean = pauseUnlocked(Duration.ofMinutes(30))
    fun pauseUnlockedOneHour(): Boolean = pauseUnlocked(Duration.ofHours(1))
    fun resumeUnlocked(): Boolean {
        Log.i("UnlockedPause", "[UnlockedPause] duration=0m pausedUntil=0 (RESUME)")
        return updateSettings(mutableSettings.value.copy(unlockedPausedUntilEpochMillis = 0L))
    }

    private fun pauseFor(duration: Duration, now: Instant = Instant.now()): Boolean {
        val until = now.plus(duration)
        return updateSettings(mutableSettings.value.copy(pausedUntil = until))
    }
}
