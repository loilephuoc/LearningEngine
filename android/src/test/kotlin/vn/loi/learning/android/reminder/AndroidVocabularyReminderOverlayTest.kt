package vn.loi.learning.android.reminder

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class AndroidVocabularyReminderOverlayTest {

    private class FakeOverlayPresenter : AndroidVocabularyReminderOverlayPresenter {
        var showCount = 0
        var hideCount = 0
        var shutdownCount = 0
        var shouldSucceed = true
        var lastCandidate: AndroidVocabularyCandidate? = null
        var lastMode: AndroidVocabularyReminderSelectionMode? = null
        var lastDurationMillis: Long = 0
        private val _isShowing = AtomicBoolean(false)

        override fun show(
            candidate: AndroidVocabularyCandidate,
            mode: AndroidVocabularyReminderSelectionMode,
            displayDurationMillis: Long,
            onReview: ((packageId: String, contentId: String, mode: AndroidVocabularyReminderSelectionMode) -> Unit)?
        ): Boolean {
            showCount++
            lastCandidate = candidate
            lastMode = mode
            lastDurationMillis = displayDurationMillis
            if (shouldSucceed) {
                _isShowing.set(true)
                return true
            }
            return false
        }

        override fun hide() {
            hideCount++
            _isShowing.set(false)
        }

        override val isShowing: Boolean
            get() = _isShowing.get()

        override fun shutdown() {
            shutdownCount++
            hide()
        }
    }

    private class FakeDeviceStateProvider(
        var permissionGranted: Boolean = true,
        var screenInteractive: Boolean = true,
        var deviceLocked: Boolean = false
    ) : AndroidVocabularyReminderDeviceStateProvider {
        override fun isOverlayPermissionGranted(): Boolean = permissionGranted
        override fun isScreenInteractive(): Boolean = screenInteractive
        override fun isDeviceLocked(): Boolean = deviceLocked
    }

    private class FakePreferenceStore(
        var currentSettings: AndroidVocabularyReminderSettings = AndroidVocabularyReminderSettings()
    ) : AndroidVocabularyReminderPreferenceStore {
        override fun load(): AndroidVocabularyReminderSettings = currentSettings
        override fun save(settings: AndroidVocabularyReminderSettings): Boolean {
            currentSettings = settings
            return true
        }
    }

    private fun createTestCandidate(id: String = "test-1"): AndroidVocabularyCandidate {
        return AndroidVocabularyCandidate(
            contentId = ContentId(id),
            packageId = InstalledPackageId("pkg-1"),
            packageName = "Package 1",
            primaryText = "resilience",
            answer = "capacity to recover quickly",
            translation = "kha nang phuc hoi",
            ipa = "rɪˈzɪl.jəns",
            partOfSpeech = "noun",
            imageReference = "images/resilience.jpg",
            primaryAudioReference = "audio/resilience.mp3"
        )
    }

    @Test
    fun `overlay setting persists in preference store and draft correctly`() {
        val store = FakePreferenceStore(
            AndroidVocabularyReminderSettings(
                enabled = true,
                selectedPackageId = "pkg-1",
                overlayPopupEnabled = true
            )
        )
        val controller = AndroidVocabularyReminderPreferencesController(store)
        assertTrue(controller.current().overlayPopupEnabled)

        val draft = AndroidVocabularyReminderDraft.from(controller.current())
        assertTrue(draft.overlayPopupEnabled)

        val validated = draft.validate(null)
        assertTrue(validated is AndroidVocabularyReminderDraftValidation.Valid)
        assertTrue(validated.settings.overlayPopupEnabled)
    }

    @Test
    fun `when overlay is enabled and permission granted on unlocked screen then overlay is presented without notification`() {
        val testCandidate = createTestCandidate()
        val fakeOverlay = FakeOverlayPresenter()
        val fakeDeviceState = FakeDeviceStateProvider(
            permissionGranted = true,
            screenInteractive = true,
            deviceLocked = false
        )

        val shown = fakeOverlay.show(
            candidate = testCandidate,
            mode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL,
            displayDurationMillis = 5000L
        )
        assertTrue(shown)
        assertEquals(1, fakeOverlay.showCount)
        assertEquals("resilience", fakeOverlay.lastCandidate?.primaryText)
        assertEquals(5000L, fakeOverlay.lastDurationMillis)
    }

    @Test
    fun `when overlay is disabled or permission is missing then fallback condition is true`() {
        val fakeDeviceStateNoPerm = FakeDeviceStateProvider(permissionGranted = false)
        assertFalse(fakeDeviceStateNoPerm.isOverlayPermissionGranted())

        val fakeDeviceStateLocked = FakeDeviceStateProvider(permissionGranted = true, deviceLocked = true)
        assertTrue(fakeDeviceStateLocked.isDeviceLocked())

        val fakeDeviceStateOff = FakeDeviceStateProvider(permissionGranted = true, screenInteractive = false)
        assertFalse(fakeDeviceStateOff.isScreenInteractive())
    }

    @Test
    fun `when overlay addView fails then failure is reported gracefully`() {
        val fakeOverlay = FakeOverlayPresenter().apply { shouldSucceed = false }
        val shown = fakeOverlay.show(
            candidate = createTestCandidate(),
            mode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL,
            displayDurationMillis = 5000L
        )
        assertFalse(shown)
        assertFalse(fakeOverlay.isShowing)
    }

    @Test
    fun `overlay shutdown and hide cleanly clears showing state`() {
        val fakeOverlay = FakeOverlayPresenter()
        fakeOverlay.show(createTestCandidate(), AndroidVocabularyReminderSelectionMode.RANDOM_ALL, 5000L)
        assertTrue(fakeOverlay.isShowing)

        fakeOverlay.hide()
        assertFalse(fakeOverlay.isShowing)
        assertEquals(1, fakeOverlay.hideCount)

        fakeOverlay.shutdown()
        assertEquals(1, fakeOverlay.shutdownCount)
    }

    @Test
    fun `canOverlay condition matrix evaluates correctly across all states`() {
        fun evaluateCanOverlay(
            overlayEnabled: Boolean,
            hasPermission: Boolean,
            isInteractive: Boolean,
            isLocked: Boolean,
            isReviewActive: Boolean
        ): Boolean {
            return overlayEnabled && hasPermission && isInteractive && !isLocked && !isReviewActive
        }

        // All optimal: overlay should show
        assertTrue(evaluateCanOverlay(overlayEnabled = true, hasPermission = true, isInteractive = true, isLocked = false, isReviewActive = false))

        // Overlay disabled: should NOT show overlay (fallback)
        assertFalse(evaluateCanOverlay(overlayEnabled = false, hasPermission = true, isInteractive = true, isLocked = false, isReviewActive = false))

        // Permission missing: should NOT show overlay (fallback)
        assertFalse(evaluateCanOverlay(overlayEnabled = true, hasPermission = false, isInteractive = true, isLocked = false, isReviewActive = false))

        // Screen not interactive: should NOT show overlay (fallback)
        assertFalse(evaluateCanOverlay(overlayEnabled = true, hasPermission = true, isInteractive = false, isLocked = false, isReviewActive = false))

        // Screen locked: should NOT show overlay (fallback)
        assertFalse(evaluateCanOverlay(overlayEnabled = true, hasPermission = true, isInteractive = true, isLocked = true, isReviewActive = false))

        // Review screen active: should NOT show overlay (suppressed)
        assertFalse(evaluateCanOverlay(overlayEnabled = true, hasPermission = true, isInteractive = true, isLocked = false, isReviewActive = true))
    }

    @Test
    fun `new install default has overlayPopupEnabled false`() {
        val store = FakePreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)
        assertFalse(controller.current().overlayPopupEnabled)
        val draft = AndroidVocabularyReminderDraft.from(controller.current())
        assertFalse(draft.overlayPopupEnabled)
    }

    @Test
    fun `user explicitly enabling overlay persists across reload and unrelated setting updates`() {
        val store = FakePreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)
        assertFalse(controller.current().overlayPopupEnabled)

        // User enables overlay
        val updated = controller.updateSettings(controller.current().copy(overlayPopupEnabled = true))
        assertTrue(updated)
        assertTrue(controller.current().overlayPopupEnabled)

        // Reload from store
        val reloadedController = AndroidVocabularyReminderPreferencesController(store)
        assertTrue(reloadedController.current().overlayPopupEnabled)

        // Draft reflects true
        val draft = AndroidVocabularyReminderDraft.from(reloadedController.current())
        assertTrue(draft.overlayPopupEnabled)

        // Unrelated setting change (e.g. interval) does not overwrite overlayPopupEnabled
        reloadedController.updateSettings(reloadedController.current().copy(intervalMillis = 60_000L))
        assertTrue(reloadedController.current().overlayPopupEnabled)
        assertEquals(60_000L, reloadedController.current().intervalMillis)

        // Draft validation preserves overlayPopupEnabled
        val validated = draft.copy(intervalValueText = "10").validate(null)
        assertTrue(validated is AndroidVocabularyReminderDraftValidation.Valid)
        assertTrue(validated.settings.overlayPopupEnabled)
    }
}
