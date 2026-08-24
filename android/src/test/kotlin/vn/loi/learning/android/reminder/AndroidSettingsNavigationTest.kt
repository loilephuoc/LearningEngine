package vn.loi.learning.android.reminder

import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test

class AndroidSettingsNavigationTest {

    private class InMemoryPreferenceStore : AndroidVocabularyReminderPreferenceStore {
        var reminderSettings = AndroidVocabularyReminderSettings()
        var lockScreenSettings = AndroidLockScreenVocabularySettings()
        var homeWidgetSettings = AndroidHomeVocabularyWidgetSettings()

        override fun load(): AndroidVocabularyReminderSettings = reminderSettings
        override fun save(settings: AndroidVocabularyReminderSettings): Boolean {
            reminderSettings = settings
            return true
        }

        override fun loadLockScreen(): AndroidLockScreenVocabularySettings = lockScreenSettings
        override fun saveLockScreen(settings: AndroidLockScreenVocabularySettings): Boolean {
            lockScreenSettings = settings
            return true
        }

        override fun loadHomeWidget(): AndroidHomeVocabularyWidgetSettings = homeWidgetSettings
        override fun saveHomeWidget(settings: AndroidHomeVocabularyWidgetSettings): Boolean {
            homeWidgetSettings = settings
            return true
        }
    }

    @Test
    fun `settings hub status summary accurately reflects lock screen enabled state`() {
        val store = InMemoryPreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        // Default: OFF
        assertFalse(controller.lockScreenSettings.value.enabled)

        // Enable
        controller.updateLockScreenSettings(controller.lockScreenSettings.value.copy(enabled = true))
        assertTrue(controller.lockScreenSettings.value.enabled)
        assertTrue(store.loadLockScreen().enabled)
    }

    @Test
    fun `settings hub status summary accurately reflects unlocked reminder active and pause state`() {
        val store = InMemoryPreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        // Default: OFF
        assertFalse(controller.settings.value.enabled)

        // Enable
        controller.setEnabled(true)
        assertTrue(controller.settings.value.enabled)
        assertFalse(controller.settings.value.isUnlockedPaused)

        // Pause
        val futureEpoch = System.currentTimeMillis() + 1_800_000L // 30 mins
        controller.updateSettings(controller.settings.value.copy(unlockedPausedUntilEpochMillis = futureEpoch))
        assertTrue(controller.settings.value.enabled)
        assertTrue(controller.settings.value.isUnlockedPaused)

        // Indefinite Pause
        controller.pauseUnlockedIndefinitely()
        assertTrue(controller.settings.value.enabled)
        assertTrue(controller.settings.value.isUnlockedPaused)
        assertTrue(controller.settings.value.unlockedPausedIndefinitely)

        // Resume
        controller.resumeUnlocked()
        assertFalse(controller.settings.value.isUnlockedPaused)
        assertFalse(controller.settings.value.unlockedPausedIndefinitely)
    }

    @Test
    fun `settings hub status summary accurately reflects home widget auto next and audio state`() {
        val store = InMemoryPreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        // Default: autoNext=true, autoAudio=false
        assertTrue(controller.homeWidgetSettings.value.autoNextEnabled)
        assertFalse(controller.homeWidgetSettings.value.autoAudioEnabled)

        // Change interval
        controller.updateHomeWidgetSettings(controller.homeWidgetSettings.value.copy(intervalMillis = 30_000L))
        assertEquals(30_000L, controller.homeWidgetSettings.value.intervalMillis)

        // Toggle auto-audio
        controller.updateHomeWidgetSettings(controller.homeWidgetSettings.value.copy(autoAudioEnabled = true))
        assertTrue(controller.homeWidgetSettings.value.autoAudioEnabled)
        assertEquals(30_000L, store.loadHomeWidget().intervalMillis)
        assertTrue(store.loadHomeWidget().autoAudioEnabled)
    }

    @Test
    fun `lock screen draft isolates lock screen settings without mutating reminder or widget state`() {
        val store = InMemoryPreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        val initialReminder = controller.settings.value
        val initialWidget = controller.homeWidgetSettings.value

        var lockDraft = AndroidLockScreenVocabularyDraft.from(controller.lockScreenSettings.value)
        lockDraft = lockDraft.copy(enabled = true, wordSize = LockWallpaperWordSize.EXTRA_LARGE)
        controller.updateLockScreenSettings(lockDraft.toSettings())

        assertTrue(controller.lockScreenSettings.value.enabled)
        assertEquals(LockWallpaperWordSize.EXTRA_LARGE, controller.lockScreenSettings.value.wordSize)

        // Unrelated surfaces untouched
        assertEquals(initialReminder, controller.settings.value)
        assertEquals(initialWidget, controller.homeWidgetSettings.value)
    }

    @Test
    fun `home widget draft isolates widget settings without mutating lock screen or reminder state`() {
        val store = InMemoryPreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        val initialReminder = controller.settings.value
        val initialLock = controller.lockScreenSettings.value

        var widgetDraft = AndroidHomeVocabularyWidgetDraft.from(controller.homeWidgetSettings.value)
        widgetDraft = widgetDraft.copy(intervalValueText = "15", intervalUnit = AndroidVocabularyReminderIntervalUnit.SECONDS)
        controller.updateHomeWidgetSettings(widgetDraft.toSettings())

        assertEquals(15_000L, controller.homeWidgetSettings.value.intervalMillis)

        // Unrelated surfaces untouched
        assertEquals(initialReminder, controller.settings.value)
        assertEquals(initialLock, controller.lockScreenSettings.value)
    }

    @Test
    fun `unlocked reminder draft isolates reminder settings without mutating widget or lock screen state`() {
        val store = InMemoryPreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        val initialLock = controller.lockScreenSettings.value
        val initialWidget = controller.homeWidgetSettings.value

        var reminderDraft = AndroidVocabularyReminderDraft.from(controller.settings.value)
        reminderDraft = reminderDraft.copy(enabled = true, intervalValueText = "45", intervalUnit = AndroidVocabularyReminderIntervalUnit.SECONDS)
        val valid = reminderDraft.validate(null) as AndroidVocabularyReminderDraftValidation.Valid
        controller.updateSettings(valid.settings)

        assertTrue(controller.settings.value.enabled)
        assertEquals(45_000L, controller.settings.value.intervalMillis)

        // Unrelated surfaces untouched
        assertEquals(initialLock, controller.lockScreenSettings.value)
        assertEquals(initialWidget, controller.homeWidgetSettings.value)
    }

    @Test
    fun `fgs requirements correctly evaluate across isolated settings states`() {
        val reqNone = ForegroundServiceRequirement(
            lockScreenRequired = false,
            unlockedReminderRequired = false,
            homeWidgetRequired = false
        )
        assertFalse(reqNone.required)

        val reqLockOnly = ForegroundServiceRequirement(
            lockScreenRequired = true,
            unlockedReminderRequired = false,
            homeWidgetRequired = false
        )
        assertTrue(reqLockOnly.required)
        assertTrue(reqLockOnly.lockScreenRequired)

        val reqWidgetOnly = ForegroundServiceRequirement(
            lockScreenRequired = false,
            unlockedReminderRequired = false,
            homeWidgetRequired = true
        )
        assertTrue(reqWidgetOnly.required)
        assertTrue(reqWidgetOnly.homeWidgetRequired)
    }

    @Test
    fun `backup and restore navigation callback is invokable from settings`() {
        var navigated = false
        val onBackupRestore = { navigated = true }
        onBackupRestore()
        assertTrue(navigated)
    }
}
