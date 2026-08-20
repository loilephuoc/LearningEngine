package vn.loi.learning.android.reminder

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class AndroidHomeVocabularyWidgetSettingsTest {

    private class InMemoryStore : AndroidVocabularyReminderPreferenceStore {
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
    fun `default widget settings meet Round 9 requirements`() {
        val defaults = AndroidHomeVocabularyWidgetSettings()
        assertTrue(defaults.autoNextEnabled)
        assertEquals(60_000L, defaults.intervalMillis)
        assertEquals(AndroidVocabularyReminderSelectionMode.RANDOM_ALL, defaults.selectionMode)
        assertEquals(LockWallpaperWordSize.LARGE, defaults.wordSize)
        assertEquals(LockWallpaperVietnameseSize.MEDIUM, defaults.vietnameseSize)
        assertEquals(LockWallpaperImageSize.LARGE, defaults.imageSize)
        assertEquals(0.92f, defaults.cardBackgroundOpacity)
        assertTrue(defaults.updateOnlyScreenOn)
        assertFalse(defaults.autoAudioEnabled)
    }

    @Test
    fun `card background opacity is clamped to 20 percent to 100 percent`() {
        val low = AndroidHomeVocabularyWidgetSettings(cardBackgroundOpacity = 0.05f)
        assertEquals(0.20f, low.clampedCardBackgroundOpacity)

        val high = AndroidHomeVocabularyWidgetSettings(cardBackgroundOpacity = 1.50f)
        assertEquals(1.00f, high.clampedCardBackgroundOpacity)

        val mid = AndroidHomeVocabularyWidgetSettings(cardBackgroundOpacity = 0.45f)
        assertEquals(0.45f, mid.clampedCardBackgroundOpacity)
    }

    @Test
    fun `widget preferences controller persists and updates independently`() {
        val store = InMemoryStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        val custom = AndroidHomeVocabularyWidgetSettings(
            autoNextEnabled = false,
            intervalMillis = 12_000L,
            selectedPackageId = "pkg-home-1",
            selectionMode = AndroidVocabularyReminderSelectionMode.DUE,
            wordSize = LockWallpaperWordSize.HUGE,
            vietnameseSize = LockWallpaperVietnameseSize.EXTRA_LARGE,
            imageSize = LockWallpaperImageSize.MAXIMUM,
            cardBackgroundOpacity = 0.85f,
            updateOnlyScreenOn = false,
            currentCandidateId = "content-101",
            autoAudioEnabled = true
        )

        assertTrue(controller.updateHomeWidgetSettings(custom))

        val loaded = controller.currentHomeWidget()
        assertFalse(loaded.autoNextEnabled)
        assertEquals(12_000L, loaded.intervalMillis)
        assertEquals("pkg-home-1", loaded.selectedPackageId)
        assertEquals(AndroidVocabularyReminderSelectionMode.DUE, loaded.selectionMode)
        assertEquals(LockWallpaperWordSize.HUGE, loaded.wordSize)
        assertEquals(LockWallpaperVietnameseSize.EXTRA_LARGE, loaded.vietnameseSize)
        assertEquals(LockWallpaperImageSize.MAXIMUM, loaded.imageSize)
        assertEquals(0.85f, loaded.cardBackgroundOpacity)
        assertFalse(loaded.updateOnlyScreenOn)
        assertEquals("content-101", loaded.currentCandidateId)
        assertTrue(loaded.autoAudioEnabled)
    }

    @Test
    fun `draft custom interval conversion and validation tests`() {
        // 1. Seconds input
        val draftSec = AndroidHomeVocabularyWidgetDraft(
            intervalValueText = "12",
            intervalUnit = AndroidVocabularyReminderIntervalUnit.SECONDS
        )
        assertNull(draftSec.intervalValidationMessage)
        assertEquals(12_000L, draftSec.calculatedIntervalMillis)
        assertEquals(12_000L, draftSec.toSettings().intervalMillis)

        // 2. Seconds min boundary
        val draftMinSec = AndroidHomeVocabularyWidgetDraft(
            intervalValueText = "2",
            intervalUnit = AndroidVocabularyReminderIntervalUnit.SECONDS
        )
        assertNull(draftMinSec.intervalValidationMessage)
        assertEquals(2_000L, draftMinSec.calculatedIntervalMillis)

        // 3. Seconds below min boundary invalid
        val draftBelowMinSec = AndroidHomeVocabularyWidgetDraft(
            intervalValueText = "1",
            intervalUnit = AndroidVocabularyReminderIntervalUnit.SECONDS
        )
        assertNotNull(draftBelowMinSec.intervalValidationMessage)

        // 4. Minutes input
        val draftMin = AndroidHomeVocabularyWidgetDraft(
            intervalValueText = "5",
            intervalUnit = AndroidVocabularyReminderIntervalUnit.MINUTES
        )
        assertNull(draftMin.intervalValidationMessage)
        assertEquals(300_000L, draftMin.calculatedIntervalMillis)

        // 5. Minutes max boundary
        val draftMaxMin = AndroidHomeVocabularyWidgetDraft(
            intervalValueText = "1440",
            intervalUnit = AndroidVocabularyReminderIntervalUnit.MINUTES
        )
        assertNull(draftMaxMin.intervalValidationMessage)
        assertEquals(86_400_000L, draftMaxMin.calculatedIntervalMillis)

        // 6. Non-numeric input
        val draftInvalid = AndroidHomeVocabularyWidgetDraft(
            intervalValueText = "abc",
            intervalUnit = AndroidVocabularyReminderIntervalUnit.SECONDS
        )
        assertNotNull(draftInvalid.intervalValidationMessage)
    }

    @Test
    fun `draft conversion preserves all widget settings accurately`() {
        val settings = AndroidHomeVocabularyWidgetSettings(
            autoNextEnabled = true,
            intervalMillis = 300_000L,
            selectedPackageId = "pkg-2",
            selectionMode = AndroidVocabularyReminderSelectionMode.AGAIN_HARD,
            wordSize = LockWallpaperWordSize.SMALL,
            vietnameseSize = LockWallpaperVietnameseSize.LARGE,
            imageSize = LockWallpaperImageSize.MEDIUM,
            cardBackgroundOpacity = 0.50f,
            updateOnlyScreenOn = true,
            currentCandidateId = "cand-xyz",
            autoAudioEnabled = true
        )

        val draft = AndroidHomeVocabularyWidgetDraft.from(settings)
        assertEquals("5", draft.intervalValueText)
        assertEquals(AndroidVocabularyReminderIntervalUnit.MINUTES, draft.intervalUnit)
        assertEquals(AndroidVocabularyReminderSelectionMode.AGAIN_HARD, draft.selectionMode)
        assertEquals(LockWallpaperWordSize.SMALL, draft.wordSize)
        assertTrue(draft.autoAudioEnabled)

        val convertedBack = draft.toSettings("cand-xyz")
        assertEquals(settings, convertedBack)
    }
}
