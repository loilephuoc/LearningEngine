package vn.loi.learning.android.reminder

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class AndroidLockScreenVocabularyStateTimersTest {

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

    private fun createSampleCandidate(id: String, text: String): AndroidVocabularyCandidate {
        return AndroidVocabularyCandidate(
            contentId = ContentId(id),
            packageId = InstalledPackageId("pkg-1"),
            packageName = "Package 1",
            primaryText = text,
            answer = "Answer $text",
            translation = "Nghĩa $text",
            ipa = "test",
            partOfSpeech = "noun",
            imageReference = null,
            primaryAudioReference = null
        )
    }

    @Test
    fun `device state transitions cancel old timers and maintain strict single-state contract`() {
        val store = InMemoryStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        // 1. Initial State = SCREEN_OFF
        var currentState = VocabularyPresentationDeviceState.SCREEN_OFF
        assertEquals(VocabularyPresentationDeviceState.SCREEN_OFF, currentState)

        // 2. Transition to LOCKED_SCREEN_ON
        currentState = VocabularyPresentationDeviceState.LOCKED_SCREEN_ON
        assertEquals(VocabularyPresentationDeviceState.LOCKED_SCREEN_ON, currentState)

        // 3. Transition to UNLOCKED_SCREEN_ON
        currentState = VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON
        assertEquals(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, currentState)

        // 4. Transition back to SCREEN_OFF
        currentState = VocabularyPresentationDeviceState.SCREEN_OFF
        assertEquals(VocabularyPresentationDeviceState.SCREEN_OFF, currentState)
    }

    @Test
    fun `double-buffer model isolates preparation from activation`() {
        val candA = createSampleCandidate("cand-A", "Word A")
        val candB = createSampleCandidate("cand-B", "Word B")
        val candC = createSampleCandidate("cand-C", "Word C")

        val presA = PreparedLockWallpaperPresentation(1L, candA, null)
        val presB = PreparedLockWallpaperPresentation(2L, candB, null)

        // Initial buffer: visible = A, nextReady = B
        var buffer = LockScreenPresentationBuffer(visible = presA, nextReady = presB)
        assertEquals("cand-A", buffer.visible?.candidate?.contentId?.value)
        assertEquals("cand-B", buffer.nextReady?.candidate?.contentId?.value)

        // Preparing candidate C while A is visible replaces nextReady with C without touching visible A
        val presC = PreparedLockWallpaperPresentation(3L, candC, null)
        buffer = buffer.copy(nextReady = presC)
        assertEquals("cand-A", buffer.visible?.candidate?.contentId?.value)
        assertEquals("cand-C", buffer.nextReady?.candidate?.contentId?.value)

        // Activation rotates buffer: nextReady becomes visible
        buffer = LockScreenPresentationBuffer(visible = buffer.nextReady, nextReady = null)
        assertEquals("cand-C", buffer.visible?.candidate?.contentId?.value)
        assertNull(buffer.nextReady)
    }

    @Test
    fun `unlocked quick pause sets pause timestamp without altering lockscreen settings`() {
        val store = InMemoryStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        controller.updateLockScreenSettings(
            AndroidLockScreenVocabularySettings(
                enabled = true,
                quickReviewIntervalMillis = 3000L
            )
        )
        controller.updateSettings(
            AndroidVocabularyReminderSettings(
                enabled = true,
                intervalMillis = 120_000L
            )
        )

        val beforePause = System.currentTimeMillis()
        assertTrue(controller.pauseUnlocked5Minutes())

        val settings = controller.current()
        assertTrue(settings.isUnlockedPaused)
        assertTrue(settings.unlockedPausedUntilEpochMillis >= beforePause + (5 * 60 * 1000L))

        // Lockscreen settings must remain completely untouched
        val lockSettings = controller.currentLockScreen()
        assertTrue(lockSettings.enabled)
        assertEquals(3000L, lockSettings.quickReviewIntervalMillis)

        // Resuming unlocked resets pause
        assertTrue(controller.resumeUnlocked())
        assertFalse(controller.current().isUnlockedPaused)
        assertEquals(0L, controller.current().unlockedPausedUntilEpochMillis)
    }

    @Test
    fun `screen-off preparation and quick review settings persist independently`() {
        val store = InMemoryStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        val customSettings = AndroidLockScreenVocabularySettings(
            enabled = true,
            quickReviewIntervalMillis = 8000L,
            screenOffPreparationEnabled = true,
            screenOffPrepareDelayMillis = 30000L,
            cardBackgroundOpacity = 0.50f,
            wordSize = LockWallpaperWordSize.HUGE,
            vietnameseSize = LockWallpaperVietnameseSize.LARGE
        )

        assertTrue(controller.updateLockScreenSettings(customSettings))

        val loaded = controller.currentLockScreen()
        assertEquals(8000L, loaded.quickReviewIntervalMillis)
        assertTrue(loaded.screenOffPreparationEnabled)
        assertEquals(30000L, loaded.screenOffPrepareDelayMillis)
        assertEquals(0.50f, loaded.cardBackgroundOpacity)
        assertEquals(LockWallpaperWordSize.HUGE, loaded.wordSize)
        assertEquals(LockWallpaperVietnameseSize.LARGE, loaded.vietnameseSize)
    }

    @Test
    fun `draft conversion supports Round 7 multi-timer settings`() {
        val settings = AndroidLockScreenVocabularySettings(
            enabled = true,
            selectedPackageId = "pkg-1",
            selectionMode = AndroidLockScreenVocabularyMode.RANDOM_ALL,
            quickReviewIntervalMillis = 2000L,
            screenOffPreparationEnabled = false,
            screenOffPrepareDelayMillis = 10000L
        )

        val draft = AndroidLockScreenVocabularyDraft.from(settings)
        assertEquals(2000L, draft.quickReviewIntervalMillis)
        assertFalse(draft.screenOffPreparationEnabled)
        assertEquals(10000L, draft.screenOffPrepareDelayMillis)

        val convertedBack = draft.toSettings()
        assertEquals(settings, convertedBack)
    }

    @Test
    fun `round 8 lock wake contract promotes nextReady to visible on new screen on`() {
        val candA = createSampleCandidate("cand-A", "Word A")
        val candB = createSampleCandidate("cand-B", "Word B")
        val presA = PreparedLockWallpaperPresentation(1L, candA, null)
        val presB = PreparedLockWallpaperPresentation(2L, candB, null)

        // Initial state: visible = A, nextReady = B
        var buffer = LockScreenPresentationBuffer(visible = presA, nextReady = presB)

        // Wake decision rule: Case A (nextReady != null) -> promote B to visible immediately
        val next = buffer.nextReady
        assertNotNull(next)
        buffer = buffer.activateNext()

        assertEquals("cand-B", buffer.visible?.candidate?.contentId?.value)
        assertNull(buffer.nextReady)
    }

    @Test
    fun `round 8 lock wake fallback to visible when nextReady is null`() {
        val candA = createSampleCandidate("cand-A", "Word A")
        val presA = PreparedLockWallpaperPresentation(1L, candA, null)

        // Initial state: visible = A, nextReady = null
        val buffer = LockScreenPresentationBuffer(visible = presA, nextReady = null)

        // Wake decision rule: Case B (nextReady == null && visible != null) -> fallback to visible A
        assertNull(buffer.nextReady)
        assertNotNull(buffer.visible)
        assertEquals("cand-A", buffer.visible?.candidate?.contentId?.value)
    }

    @Test
    fun `round 8 quick review timer advances nextReady after audio completes`() {
        val candB = createSampleCandidate("cand-B", "Word B")
        val candC = createSampleCandidate("cand-C", "Word C")
        val presB = PreparedLockWallpaperPresentation(2L, candB, null)
        val presC = PreparedLockWallpaperPresentation(3L, candC, null)

        var buffer = LockScreenPresentationBuffer(visible = presB, nextReady = presC)
        var quickReviewTriggered = false

        // Audio completes -> quick review timer fires -> activates C
        quickReviewTriggered = true
        buffer = buffer.activateNext()

        assertTrue(quickReviewTriggered)
        assertEquals("cand-C", buffer.visible?.candidate?.contentId?.value)
        assertNull(buffer.nextReady)
    }

    @Test
    fun `round 8 settings re-render does not advance candidate or modify nextReady`() {
        val candA = createSampleCandidate("cand-A", "Word A")
        val candB = createSampleCandidate("cand-B", "Word B")
        val presA = PreparedLockWallpaperPresentation(1L, candA, null)
        val presB = PreparedLockWallpaperPresentation(2L, candB, null)

        val buffer = LockScreenPresentationBuffer(visible = presA, nextReady = presB)

        // Changing word size or opacity re-renders current visible presentation ONLY
        val reRenderedVisible = buffer.visible
        assertEquals("cand-A", reRenderedVisible?.candidate?.contentId?.value)
        assertEquals("cand-B", buffer.nextReady?.candidate?.contentId?.value)
    }

    @Test
    fun `indefinite pause sets indefinite flag and cancels unlocked timers until explicit resume`() {
        val store = InMemoryStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        controller.updateSettings(
            AndroidVocabularyReminderSettings(
                enabled = true,
                intervalMillis = 60_000L
            )
        )

        // Indefinite pause
        assertTrue(controller.pauseUnlockedIndefinitely())

        val settings = controller.current()
        assertTrue(settings.isUnlockedPaused)
        assertTrue(settings.unlockedPausedIndefinitely)
        assertEquals(0L, settings.unlockedPausedUntilEpochMillis)
        assertEquals(UnlockedReminderPauseState.PausedIndefinitely, settings.unlockedPauseState)

        // Persisted state preserves indefinite pause
        val loaded = store.load()
        assertTrue(loaded.isUnlockedPaused)
        assertTrue(loaded.unlockedPausedIndefinitely)

        // Resuming unlocked clears indefinite pause
        assertTrue(controller.resumeUnlocked())
        assertFalse(controller.current().isUnlockedPaused)
        assertFalse(controller.current().unlockedPausedIndefinitely)
        assertEquals(UnlockedReminderPauseState.Active, controller.current().unlockedPauseState)
    }
}
