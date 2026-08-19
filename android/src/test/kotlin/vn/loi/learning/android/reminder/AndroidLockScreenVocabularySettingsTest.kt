package vn.loi.learning.android.reminder

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class AndroidLockScreenVocabularySettingsTest {

    private class InMemoryStore : AndroidVocabularyReminderPreferenceStore {
        var reminderSettings = AndroidVocabularyReminderSettings()
        var lockScreenSettings = AndroidLockScreenVocabularySettings()

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
    }

    @Test
    fun `default lock-screen settings are disabled with safe defaults including EXTRA_LARGE word and MEDIUM vietnamese`() {
        val store = InMemoryStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        val settings = controller.currentLockScreen()
        assertFalse(settings.enabled)
        assertNull(settings.selectedPackageId)
        assertEquals(AndroidLockScreenVocabularyMode.AGAIN_HARD, settings.selectionMode)
        assertFalse(settings.autoPlayPronunciation)
        assertNull(settings.customBackgroundPath)
        assertEquals(LockWallpaperWordSize.EXTRA_LARGE, settings.wordSize)
        assertEquals(LockWallpaperVietnameseSize.MEDIUM, settings.vietnameseSize)
        assertEquals(LockWallpaperImageSize.EXTRA_LARGE, settings.imageSize)
        assertEquals(0.72f, settings.cardBackgroundOpacity)
    }

    @Test
    fun `lock-screen settings persist independently across updates including all size controls and opacity`() {
        val store = InMemoryStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        val newSettings = AndroidLockScreenVocabularySettings(
            enabled = true,
            selectedPackageId = "pkg-vocab-123",
            selectionMode = AndroidLockScreenVocabularyMode.NEW_UNSEEN,
            autoPlayPronunciation = true,
            customBackgroundPath = "/data/user/0/test/custom_bg.png",
            wordSize = LockWallpaperWordSize.HUGE,
            vietnameseSize = LockWallpaperVietnameseSize.HUGE,
            imageSize = LockWallpaperImageSize.MAXIMUM,
            cardBackgroundOpacity = 0.40f
        )

        assertTrue(controller.updateLockScreenSettings(newSettings))
        assertEquals(newSettings, controller.currentLockScreen())

        // Recreate controller to simulate app restart
        val reloadedController = AndroidVocabularyReminderPreferencesController(store)
        assertEquals(newSettings, reloadedController.currentLockScreen())
        assertEquals(LockWallpaperWordSize.HUGE, reloadedController.currentLockScreen().wordSize)
        assertEquals(LockWallpaperVietnameseSize.HUGE, reloadedController.currentLockScreen().vietnameseSize)
        assertEquals(LockWallpaperImageSize.MAXIMUM, reloadedController.currentLockScreen().imageSize)
        assertEquals(0.40f, reloadedController.currentLockScreen().cardBackgroundOpacity)
    }

    @Test
    fun `cardBackgroundOpacity clamping enforces bounds 0_20 to 1_00`() {
        val lowSettings = AndroidLockScreenVocabularySettings(cardBackgroundOpacity = 0.05f)
        assertEquals(0.20f, lowSettings.clampedCardBackgroundOpacity)

        val highSettings = AndroidLockScreenVocabularySettings(cardBackgroundOpacity = 1.50f)
        assertEquals(1.00f, highSettings.clampedCardBackgroundOpacity)

        val validSettings = AndroidLockScreenVocabularySettings(cardBackgroundOpacity = 0.65f)
        assertEquals(0.65f, validSettings.clampedCardBackgroundOpacity)
    }

    @Test
    fun `lock-screen and regular reminder settings do not interfere with each other`() {
        val store = InMemoryStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        // 1. Configure regular reminder
        controller.updateSettings(
            AndroidVocabularyReminderSettings(
                enabled = true,
                selectedPackageId = "reminder-pkg",
                selectionMode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL
            )
        )

        // 2. Configure lock screen
        controller.updateLockScreenSettings(
            AndroidLockScreenVocabularySettings(
                enabled = true,
                selectedPackageId = "lockscreen-pkg",
                selectionMode = AndroidLockScreenVocabularyMode.AGAIN_HARD,
                wordSize = LockWallpaperWordSize.LARGE,
                vietnameseSize = LockWallpaperVietnameseSize.SMALL,
                imageSize = LockWallpaperImageSize.MEDIUM,
                cardBackgroundOpacity = 0.85f
            )
        )

        // Verify independent state
        assertEquals("reminder-pkg", controller.current().selectedPackageId)
        assertEquals(AndroidVocabularyReminderSelectionMode.RANDOM_ALL, controller.current().selectionMode)

        assertEquals("lockscreen-pkg", controller.currentLockScreen().selectedPackageId)
        assertEquals(AndroidLockScreenVocabularyMode.AGAIN_HARD, controller.currentLockScreen().selectionMode)
        assertEquals(LockWallpaperWordSize.LARGE, controller.currentLockScreen().wordSize)
        assertEquals(LockWallpaperVietnameseSize.SMALL, controller.currentLockScreen().vietnameseSize)
        assertEquals(LockWallpaperImageSize.MEDIUM, controller.currentLockScreen().imageSize)
        assertEquals(0.85f, controller.currentLockScreen().cardBackgroundOpacity)
    }

    @Test
    fun `draft converts faithfully to settings and back`() {
        val draft = AndroidLockScreenVocabularyDraft(
            enabled = true,
            selectedPackageId = "pkg-draft-1",
            selectionMode = AndroidLockScreenVocabularyMode.MARKED_DIFFICULT,
            autoPlayPronunciation = true,
            customBackgroundPath = "/path/to/bg.png",
            wordSize = LockWallpaperWordSize.HUGE,
            vietnameseSize = LockWallpaperVietnameseSize.EXTRA_LARGE,
            imageSize = LockWallpaperImageSize.MAXIMUM,
            cardBackgroundOpacity = 0.35f
        )
        val settings = draft.toSettings()
        assertEquals(draft.enabled, settings.enabled)
        assertEquals(draft.selectedPackageId, settings.selectedPackageId)
        assertEquals(draft.selectionMode, settings.selectionMode)
        assertEquals(draft.autoPlayPronunciation, settings.autoPlayPronunciation)
        assertEquals(draft.customBackgroundPath, settings.customBackgroundPath)
        assertEquals(draft.wordSize, settings.wordSize)
        assertEquals(draft.vietnameseSize, settings.vietnameseSize)
        assertEquals(draft.imageSize, settings.imageSize)
        assertEquals(0.35f, settings.cardBackgroundOpacity)

        val reconstructedDraft = AndroidLockScreenVocabularyDraft.from(settings)
        assertEquals(draft, reconstructedDraft)
    }
}
