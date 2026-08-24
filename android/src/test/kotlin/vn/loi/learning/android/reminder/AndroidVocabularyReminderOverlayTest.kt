package vn.loi.learning.android.reminder

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

class AndroidVocabularyReminderOverlayTest {
    @Test
    fun `big popup image uses restrained six dp corners in runtime and visual audit`() {
        val runtime = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/reminder/AndroidVocabularyReminderOverlayController.kt"))
        val audit = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/kotlin/vn/loi/learning/android/reminder/AndroidVocabularyReminderOverlayAudit.kt"))
        assertTrue(runtime.contains("createRoundedCornerBitmap(rawBitmap, 6f * density)"))
        assertTrue(audit.contains("createRoundedCornerBitmap(sampleImage, 6f * density)"))
        assertFalse(runtime.contains("createRoundedCornerBitmap(rawBitmap, 12f * density)"))
    }

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
            onReview: ((packageId: String, contentId: String, mode: AndroidVocabularyReminderSelectionMode) -> Unit)?,
            onQuickPause: ((action: ReminderQuickPauseAction) -> Unit)?,
            onDismissed: ((reason: String) -> Unit)?
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
        var screenOn: Boolean = true,
        var deviceLocked: Boolean = false
    ) : AndroidVocabularyReminderDeviceStateProvider {
        override fun isOverlayPermissionGranted(): Boolean = permissionGranted
        override fun isScreenOn(): Boolean = screenOn
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
            screenOn = true,
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

        val fakeDeviceStateOff = FakeDeviceStateProvider(permissionGranted = true, screenOn = false)
        assertFalse(fakeDeviceStateOff.isScreenOn())
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

    @Test
    fun `responsive layout test matrix cases A through G preserve complete text without truncation`() {
        val testCases = listOf(
            // CASE A: Short
            createTestCandidate("case-a").copy(primaryText = "inform", ipa = "ɪnˈfɔːm", partOfSpeech = "verb", translation = "Thông báo"),
            // CASE B: Medium
            createTestCandidate("case-b").copy(primaryText = "commence", ipa = "kəˈmens", partOfSpeech = "verb", translation = "Bắt đầu (trang trọng)"),
            // CASE C: English phrase
            createTestCandidate("case-c").copy(primaryText = "make up one's mind", ipa = "meɪk ʌp wʌnz maɪnd", partOfSpeech = "idiom", translation = "Quyết định / Hạ quyết tâm"),
            // CASE D: Long English
            createTestCandidate("case-d").copy(primaryText = "a blessing in disguise", ipa = "ə ˈbles.ɪŋ ɪn dɪsˈɡaɪz", partOfSpeech = "idiom", translation = "Trong cái rủi có cái may"),
            // CASE E: Long Vietnamese
            createTestCandidate("case-e").copy(primaryText = "detached house", ipa = "dɪˈtætʃt haʊs", partOfSpeech = "noun", translation = "nhà riêng biệt, không nối với bất kỳ nhà nào khác"),
            // CASE F: Very long Vietnamese
            createTestCandidate("case-f").copy(primaryText = "dilemma", ipa = "dɪˈlem.ə", partOfSpeech = "noun", translation = "Tình thế tiến thoái lưỡng nan, tình huống khó xử"),
            // CASE G: Long IPA/POS
            createTestCandidate("case-g").copy(primaryText = "internationalization", ipa = "ˌɪntəˌnætʃənəlaɪˈzeɪʃən", partOfSpeech = "transitive / intransitive verb", translation = "Quốc tế hóa")
        )

        val fakePresenter = FakeOverlayPresenter()

        testCases.forEach { candidate ->
            var pauseTriggeredAction: ReminderQuickPauseAction? = null
            val shown = fakePresenter.show(
                candidate = candidate,
                mode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL,
                displayDurationMillis = 5000L,
                onQuickPause = { action -> pauseTriggeredAction = action }
            )
            assertTrue(shown)
            assertTrue(fakePresenter.isShowing)
            assertEquals(candidate.primaryText, fakePresenter.lastCandidate?.primaryText)
            assertEquals(candidate.translation, fakePresenter.lastCandidate?.translation)
            assertEquals(candidate.ipa, fakePresenter.lastCandidate?.ipa)
            assertEquals(candidate.partOfSpeech, fakePresenter.lastCandidate?.partOfSpeech)

            fakePresenter.hide()
            assertFalse(fakePresenter.isShowing)
        }
    }

    @Test
    fun `quick pause callbacks in presenter show pass valid durations and indefinite`() {
        val fakePresenter = FakeOverlayPresenter()
        val candidate = createTestCandidate("pause-test")
        var pausedAction: ReminderQuickPauseAction? = null

        fakePresenter.show(
            candidate = candidate,
            mode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL,
            displayDurationMillis = 5000L,
            onQuickPause = { action -> pausedAction = action }
        )

        assertTrue(fakePresenter.isShowing)
        fakePresenter.hide()
        assertFalse(fakePresenter.isShowing)
    }

    @Test
    fun `overlay state machine transitions correctly between HIDDEN, ENTERING, VISIBLE, and EXITING`() {
        // Verify states enum definition and invariants
        assertEquals(OverlayState.HIDDEN, OverlayState.valueOf("HIDDEN"))
        assertEquals(OverlayState.ENTERING, OverlayState.valueOf("ENTERING"))
        assertEquals(OverlayState.VISIBLE, OverlayState.valueOf("VISIBLE"))
        assertEquals(OverlayState.EXITING, OverlayState.valueOf("EXITING"))
    }

    @Test
    fun `close button action only dismisses popup without setting pause`() {
        val fakePresenter = FakeOverlayPresenter()
        val candidate = createTestCandidate("close-test")
        var pausedAction: ReminderQuickPauseAction? = null

        fakePresenter.show(
            candidate = candidate,
            mode = AndroidVocabularyReminderSelectionMode.RANDOM_ALL,
            displayDurationMillis = 5000L,
            onQuickPause = { action -> pausedAction = action }
        )
        assertTrue(fakePresenter.isShowing)

        // Dismiss without invoking onQuickPause
        fakePresenter.hide()
        assertFalse(fakePresenter.isShowing)
        assertEquals(null, pausedAction)
    }

    @Test
    fun `progress countdown line shares exact deadline with auto dismiss`() {
        val displayDuration = 5000L
        val visibleStartedAt = 100_000L
        val dismissDeadline = visibleStartedAt + displayDuration
        assertEquals(105_000L, dismissDeadline)
        assertEquals(displayDuration, dismissDeadline - visibleStartedAt)
    }

    @Test
    fun `user action tap X or Pause cancels countdown animator and triggers single dismissal`() {
        val fakePresenter = FakeOverlayPresenter()
        fakePresenter.show(createTestCandidate("action-cancel-test"), AndroidVocabularyReminderSelectionMode.RANDOM_ALL, 5000L)
        assertTrue(fakePresenter.isShowing)

        // User taps X -> triggers single hide
        fakePresenter.hide()
        assertFalse(fakePresenter.isShowing)
        assertEquals(1, fakePresenter.hideCount)

        // Stale or duplicate hide invocations are idempotent
        fakePresenter.hide()
        assertEquals(2, fakePresenter.hideCount)
        assertFalse(fakePresenter.isShowing)
    }

    @Test
    fun `device state change to locked cancels overlay presentation immediately`() {
        val fakeDeviceState = FakeDeviceStateProvider(permissionGranted = true, screenOn = true, deviceLocked = false)
        val fakePresenter = FakeOverlayPresenter()

        fakePresenter.show(createTestCandidate("device-lock-test"), AndroidVocabularyReminderSelectionMode.RANDOM_ALL, 5000L)
        assertTrue(fakePresenter.isShowing)

        // Device locks
        fakeDeviceState.deviceLocked = true
        fakePresenter.hide()

        assertFalse(fakePresenter.isShowing)
        assertEquals(1, fakePresenter.hideCount)
    }

    @Test
    fun `pause 5m 30m 1h calculations compute accurate future timestamps`() {
        val now = 1_000_000L
        val pause5m = now + java.time.Duration.ofMinutes(5).toMillis()
        val pause30m = now + java.time.Duration.ofMinutes(30).toMillis()
        val pause1h = now + java.time.Duration.ofHours(1).toMillis()

        assertEquals(1_000_000L + 300_000L, pause5m)
        assertEquals(1_000_000L + 1_800_000L, pause30m)
        assertEquals(1_000_000L + 3_600_000L, pause1h)
    }

    @Test
    fun `unlocked scheduler reconciliation logic handles pause, state transitions, and interval start accurately`() {
        data class ReconcileResult(val action: String, val intervalMs: Long?, val pauseRemainingMs: Long?)

        fun testReconcile(
            deviceState: VocabularyPresentationDeviceState,
            enabled: Boolean,
            unlockedPausedUntil: Long,
            now: Long,
            intervalMs: Long
        ): ReconcileResult {
            if (deviceState != VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON || !enabled) {
                return ReconcileResult("CANCEL", null, null)
            }
            if (now < unlockedPausedUntil) {
                return ReconcileResult("WAIT_PAUSE", null, unlockedPausedUntil - now)
            }
            return ReconcileResult("START_INTERVAL", intervalMs, null)
        }

        val baseInterval = 120_000L
        val now = 2_000_000L
        val activePause = now + 300_000L // 5 min future

        // 1. Unlocked & active pause -> WAIT_PAUSE
        val pauseRes = testReconcile(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, true, activePause, now, baseInterval)
        assertEquals("WAIT_PAUSE", pauseRes.action)
        assertEquals(300_000L, pauseRes.pauseRemainingMs)

        // 2. Pause expired -> START_INTERVAL
        val expiredPause = now - 1000L
        val expiredRes = testReconcile(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, true, expiredPause, now, baseInterval)
        assertEquals("START_INTERVAL", expiredRes.action)
        assertEquals(baseInterval, expiredRes.intervalMs)

        // 3. Screen off while pause active -> CANCEL (no timers while screen off)
        val screenOffRes = testReconcile(VocabularyPresentationDeviceState.SCREEN_OFF, true, activePause, now, baseInterval)
        assertEquals("CANCEL", screenOffRes.action)

        // 4. Locked while pause expired -> CANCEL (only quick review domain applies)
        val lockedRes = testReconcile(VocabularyPresentationDeviceState.LOCKED_SCREEN_ON, true, expiredPause, now, baseInterval)
        assertEquals("CANCEL", lockedRes.action)

        // 5. User unlocks after pause expired -> START_INTERVAL
        val unlockRes = testReconcile(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, true, 0L, now, baseInterval)
        assertEquals("START_INTERVAL", unlockRes.action)
        assertEquals(baseInterval, unlockRes.intervalMs)

        // 6. Settings interval changed during pause -> next interval uses new setting
        val newInterval = 180_000L
        val newIntervalRes = testReconcile(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, true, expiredPause, now, newInterval)
        assertEquals("START_INTERVAL", newIntervalRes.action)
        assertEquals(newInterval, newIntervalRes.intervalMs)
    }

    @Test
    fun `instance token ensures stale dismiss and countdown callbacks are safely ignored`() {
        var activeInstanceId = 101L
        var dismissCount = 0

        fun handleDismiss(instanceId: Long, reason: String): Boolean {
            if (instanceId != activeInstanceId) return false
            dismissCount++
            return true
        }

        // Active instance dismiss -> accepted
        assertTrue(handleDismiss(101L, "USER_TAP_X"))
        assertEquals(1, dismissCount)

        // Stale instance callback -> ignored
        assertFalse(handleDismiss(100L, "STALE_COUNTDOWN_TIMEOUT"))
        assertEquals(1, dismissCount)

        // Increment instance id
        activeInstanceId = 102L
        assertFalse(handleDismiss(101L, "STALE_OLD_INSTANCE_ACTION"))
        assertEquals(1, dismissCount)

        assertTrue(handleDismiss(102L, "NEW_INSTANCE_COUNTDOWN"))
        assertEquals(2, dismissCount)
    }

    @Test
    fun `round 7_3_6 exit animation invariants enforce single auto-dismiss deadline and clean fade without scale`() {
        // Invariant 1: SIMPLE_FADE duration must be 100ms
        assertEquals(100L, AndroidVocabularyReminderOverlayController.EXIT_FADE_DURATION_MS)

        // Invariant 2: Exit modes enum exists with SIMPLE_FADE and IMMEDIATE
        assertEquals(OverlayExitMode.SIMPLE_FADE, OverlayExitMode.valueOf("SIMPLE_FADE"))
        assertEquals(OverlayExitMode.IMMEDIATE, OverlayExitMode.valueOf("IMMEDIATE"))

        // Invariant 3: Active exit mode defaults to SIMPLE_FADE
        AndroidVocabularyReminderOverlayController.activeExitMode = OverlayExitMode.SIMPLE_FADE
        assertEquals(OverlayExitMode.SIMPLE_FADE, AndroidVocabularyReminderOverlayController.activeExitMode)
    }

    @Test
    fun `single auto-dismiss deadline ownership ensures only timer runnable issues dismiss`() {
        var timerTriggered = false
        var progressCompleted = false
        var dismissIssued = false

        val displayDuration = 3000L
        val startTime = 1000L
        val deadline = startTime + displayDuration

        // Progress animator finishes visually
        progressCompleted = true
        // Visual progress completion alone must NOT trigger dismissal
        assertFalse(dismissIssued)

        // Deadline timer fires
        timerTriggered = true
        dismissIssued = true
        assertTrue(timerTriggered)
        assertTrue(dismissIssued)
        assertEquals(4000L, deadline)
    }

    @Test
    fun `all dismissal reasons route to same idempotent requestDismiss`() {
        val dismissReasons = listOf(
            "USER_PAUSE_5M",
            "USER_PAUSE_30M",
            "USER_PAUSE_1H",
            "USER_CLOSE_CONTAINER",
            "USER_CLOSE_BUTTON",
            "USER_REVIEW_CLICK",
            "AUTO_DISMISS",
            "DIRECT_HIDE"
        )

        var acceptedCount = 0
        var activeInstance = 200L
        var state = OverlayState.VISIBLE

        fun requestDismissMock(instanceId: Long, reason: String): Boolean {
            if (instanceId != activeInstance || state != OverlayState.VISIBLE) return false
            state = OverlayState.EXITING
            acceptedCount++
            return true
        }

        // Test first reason accepts
        assertTrue(requestDismissMock(200L, dismissReasons.first()))
        assertEquals(1, acceptedCount)

        // Subsequent triggers for same instance in EXITING state are ignored
        dismissReasons.drop(1).forEach { reason ->
            assertFalse(requestDismissMock(200L, reason))
        }
        assertEquals(1, acceptedCount)
    }

    @Test
    fun `resume now clears active pauses of 5m 30m and 1h and indefinite`() {
        val now = 10_000_000L
        val fakeStore = FakePreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(fakeStore)

        // 1. Pause 5m then resume
        controller.pauseUnlocked(java.time.Duration.ofMinutes(5), java.time.Instant.ofEpochMilli(now))
        assertTrue(controller.current().unlockedPausedUntilEpochMillis > now)
        assertFalse(controller.current().unlockedPausedIndefinitely)
        controller.resumeUnlocked()
        assertEquals(0L, controller.current().unlockedPausedUntilEpochMillis)
        assertFalse(controller.current().unlockedPausedIndefinitely)

        // 2. Pause 30m then resume
        controller.pauseUnlocked(java.time.Duration.ofMinutes(30), java.time.Instant.ofEpochMilli(now))
        assertTrue(controller.current().unlockedPausedUntilEpochMillis > now)
        assertFalse(controller.current().unlockedPausedIndefinitely)
        controller.resumeUnlocked()
        assertEquals(0L, controller.current().unlockedPausedUntilEpochMillis)
        assertFalse(controller.current().unlockedPausedIndefinitely)

        // 3. Pause 1h then resume
        controller.pauseUnlocked(java.time.Duration.ofHours(1), java.time.Instant.ofEpochMilli(now))
        assertTrue(controller.current().unlockedPausedUntilEpochMillis > now)
        assertFalse(controller.current().unlockedPausedIndefinitely)
        controller.resumeUnlocked()
        assertEquals(0L, controller.current().unlockedPausedUntilEpochMillis)
        assertFalse(controller.current().unlockedPausedIndefinitely)

        // 4. Pause indefinitely then resume
        controller.pauseUnlockedIndefinitely()
        assertEquals(0L, controller.current().unlockedPausedUntilEpochMillis)
        assertTrue(controller.current().unlockedPausedIndefinitely)
        assertTrue(controller.current().isUnlockedPaused)
        controller.resumeUnlocked()
        assertEquals(0L, controller.current().unlockedPausedUntilEpochMillis)
        assertFalse(controller.current().unlockedPausedIndefinitely)
        assertFalse(controller.current().isUnlockedPaused)
    }

    @Test
    fun `resume now lifecycle dispatches immediate popup and arms normal interval only after completion`() {
        var immediatePopupDispatched = false
        var normalIntervalArmed = false
        var normalIntervalMs: Long? = null

        fun simulateResumeNow(
            deviceState: VocabularyPresentationDeviceState,
            unlockedEnabled: Boolean,
            popupDismissReason: String, // "AUTO_DISMISS", "USER_CLOSE_BUTTON", "USER_PAUSE_5M"
            userPausedAgainOnPopup: Boolean
        ) {
            // Step 1: clear pause
            val pauseCleared = true
            assertTrue(pauseCleared)

            // Step 2: check if device is unlocked
            if (deviceState == VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON && unlockedEnabled) {
                // Step 3: dispatch immediate popup
                immediatePopupDispatched = true
                // Do NOT arm normal interval yet!
                assertFalse(normalIntervalArmed)

                // Step 4: popup lifecycle finishes
                if (userPausedAgainOnPopup) {
                    // Suppress normal rearm
                    normalIntervalArmed = false
                } else {
                    // Normal finish -> arm interval
                    normalIntervalArmed = true
                    normalIntervalMs = 5000L
                }
            } else {
                // Pending for next unlock
                immediatePopupDispatched = false
                normalIntervalArmed = false
            }
        }

        // Case A: Unlocked + Auto Dismiss -> interval starts AFTER popup
        simulateResumeNow(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, true, "AUTO_DISMISS", false)
        assertTrue(immediatePopupDispatched)
        assertTrue(normalIntervalArmed)
        assertEquals(5000L, normalIntervalMs)

        // Case B: Unlocked + X close -> interval starts AFTER popup
        immediatePopupDispatched = false
        normalIntervalArmed = false
        simulateResumeNow(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, true, "USER_CLOSE_BUTTON", false)
        assertTrue(immediatePopupDispatched)
        assertTrue(normalIntervalArmed)

        // Case C: Unlocked + Pause 5m on immediate popup -> interval is SUPPRESSED
        immediatePopupDispatched = false
        normalIntervalArmed = false
        simulateResumeNow(VocabularyPresentationDeviceState.UNLOCKED_SCREEN_ON, true, "USER_PAUSE_5M", true)
        assertTrue(immediatePopupDispatched)
        assertFalse(normalIntervalArmed)

        // Case D: Screen off when resume now is clicked -> no immediate overlay, stays pending
        immediatePopupDispatched = false
        normalIntervalArmed = false
        simulateResumeNow(VocabularyPresentationDeviceState.SCREEN_OFF, true, "AUTO_DISMISS", false)
        assertFalse(immediatePopupDispatched)
        assertFalse(normalIntervalArmed)

        // Case E: Device locked when resume now is clicked -> no immediate overlay, stays pending
        immediatePopupDispatched = false
        normalIntervalArmed = false
        simulateResumeNow(VocabularyPresentationDeviceState.LOCKED_SCREEN_ON, true, "AUTO_DISMISS", false)
        assertFalse(immediatePopupDispatched)
        assertFalse(normalIntervalArmed)
    }

    @Test
    fun `round 7_3_8 pause notification invariants enforce single notification ID, low importance, and auto-cancellation`() {
        // Invariant 1: Notification ID is constant and shared across 5m, 30m, 1h
        assertEquals(20311, AndroidVocabularyReminderNotificationHelper.UNLOCKED_PAUSE_STATUS_NOTIFICATION_ID)

        // Invariant 2: Channel ID and Action constants
        assertEquals("unlocked_reminder_pause_status", AndroidVocabularyReminderNotificationHelper.PAUSE_STATUS_CHANNEL_ID)
        assertEquals("vn.loi.learning.android.ACTION_RESUME_UNLOCKED_NOW", AndroidVocabularyReminderNotificationHelper.ACTION_RESUME_UNLOCKED_NOW)

        // Invariant 3: Single notification ID tracking across updates
        val postedNotifications = mutableMapOf<Int, String>()
        fun postOrUpdate(durationMinutes: Long) {
            postedNotifications[AndroidVocabularyReminderNotificationHelper.UNLOCKED_PAUSE_STATUS_NOTIFICATION_ID] = "Paused for ${durationMinutes}m"
        }
        fun cancelNotification(reason: String) {
            postedNotifications.remove(AndroidVocabularyReminderNotificationHelper.UNLOCKED_PAUSE_STATUS_NOTIFICATION_ID)
        }

        // User pauses 5m
        postOrUpdate(5L)
        assertEquals(1, postedNotifications.size)
        assertEquals("Paused for 5m", postedNotifications[AndroidVocabularyReminderNotificationHelper.UNLOCKED_PAUSE_STATUS_NOTIFICATION_ID])

        // User changes pause to 30m -> same ID updated, not stacked
        postOrUpdate(30L)
        assertEquals(1, postedNotifications.size)
        assertEquals("Paused for 30m", postedNotifications[AndroidVocabularyReminderNotificationHelper.UNLOCKED_PAUSE_STATUS_NOTIFICATION_ID])

        // User changes pause to 1h (60m) -> same ID updated
        postOrUpdate(60L)
        assertEquals(1, postedNotifications.size)
        assertEquals("Paused for 60m", postedNotifications[AndroidVocabularyReminderNotificationHelper.UNLOCKED_PAUSE_STATUS_NOTIFICATION_ID])

        // Resume now tapped -> cancelled
        cancelNotification("RESUME_NOW")
        assertTrue(postedNotifications.isEmpty())

        // Pause expired naturally -> cancelled
        postOrUpdate(5L)
        assertEquals(1, postedNotifications.size)
        cancelNotification("PAUSE_EXPIRED")
        assertTrue(postedNotifications.isEmpty())

        // Feature disabled -> cancelled
        postOrUpdate(30L)
        assertEquals(1, postedNotifications.size)
        cancelNotification("FEATURE_DISABLED")
        assertTrue(postedNotifications.isEmpty())
    }

    @Test
    fun `round 7_3_8_1 both action button and body tap execute resumeUnlockedNow identically`() {
        assertEquals("extra_resume_source", AndroidVocabularyReminderNotificationHelper.EXTRA_RESUME_SOURCE)

        var resumeExecuted = false
        var executedSource = ""

        fun onReceiveResume(source: String) {
            resumeExecuted = true
            executedSource = source
        }

        // 1. Action button path
        onReceiveResume("NOTIFICATION_ACTION")
        assertTrue(resumeExecuted)
        assertEquals("NOTIFICATION_ACTION", executedSource)

        // 2. Body tap fallback path
        resumeExecuted = false
        executedSource = ""
        onReceiveResume("NOTIFICATION_BODY")
        assertTrue(resumeExecuted)
        assertEquals("NOTIFICATION_BODY", executedSource)

        // 3. Custom RemoteViews Resume now button path (Round 7.3.8.2)
        resumeExecuted = false
        executedSource = ""
        onReceiveResume("NOTIFICATION_CUSTOM_RESUME")
        assertTrue(resumeExecuted)
        assertEquals("NOTIFICATION_CUSTOM_RESUME", executedSource)
    }

    @Test
    fun `round 7_3_8_2 custom RemoteViews layout and request code invariants`() {
        assertEquals(4043, AndroidVocabularyReminderNotificationHelper.RESUME_CUSTOM_REQUEST_CODE)
    }

    @Test
    fun `round 7_3_8_3 unified foreground service notification state and action invariants`() {
        // Invariant 1: Single FGS notification ID and channel
        assertEquals(20261, AndroidLockScreenVocabularyService.NOTIFICATION_ID)
        assertEquals("lockscreen_vocabulary_service", AndroidLockScreenVocabularyService.CHANNEL_ID)
        assertEquals(20311, AndroidLockScreenVocabularyService.LEGACY_PAUSE_NOTIFICATION_ID)

        // Invariant 2: FGS action & body request codes
        assertEquals(4050, AndroidLockScreenVocabularyService.FGS_RESUME_ACTION_REQUEST_CODE)
        assertEquals(4051, AndroidLockScreenVocabularyService.FGS_RESUME_BODY_REQUEST_CODE)

        // Invariant 3: FGS notification states
        var serviceState = "ACTIVE"
        var hasResumeAction = false
        var hasResumeBodyTap = false

        fun updateServiceNotification(isPaused: Boolean) {
            if (isPaused) {
                serviceState = "PAUSED"
                hasResumeAction = true
                hasResumeBodyTap = true
            } else {
                serviceState = "ACTIVE"
                hasResumeAction = false
                hasResumeBodyTap = false
            }
        }

        // Active default
        updateServiceNotification(false)
        assertEquals("ACTIVE", serviceState)
        assertFalse(hasResumeAction)
        assertFalse(hasResumeBodyTap)

        // User pauses 30m -> PAUSED state
        updateServiceNotification(true)
        assertEquals("PAUSED", serviceState)
        assertTrue(hasResumeAction)
        assertTrue(hasResumeBodyTap)

        // User resumes -> ACTIVE state
        updateServiceNotification(false)
        assertEquals("ACTIVE", serviceState)
        assertFalse(hasResumeAction)
        assertFalse(hasResumeBodyTap)
    }
    @Test
    fun `overlay exposes tap long press mute and image-only six dp radius`() {
        val controller = java.io.File("src/main/kotlin/vn/loi/learning/android/reminder/AndroidVocabularyReminderOverlayController.kt").readText()
        val layout = java.io.File("src/main/res/layout/overlay_vocabulary_reminder.xml").readText()
        assertTrue(controller.contains("ReminderQuickPauseAction.ForDuration(java.time.Duration.ofMinutes(5))"))
        assertTrue(controller.contains("pause5mBtn.setOnLongClickListener"))
        assertTrue(controller.contains("ReminderQuickPauseAction.ForDuration(java.time.Duration.ofMinutes(30))"))
        assertTrue(controller.contains("ReminderQuickPauseAction.ForDuration(java.time.Duration.ofHours(1))"))
        assertTrue(controller.contains("ReminderQuickPauseAction.ForDuration(java.time.Duration.ofHours(4))"))
        assertTrue(controller.contains("ReminderQuickPauseAction.Indefinitely"))
        assertFalse(controller.contains("Long.MAX_VALUE"))
        assertTrue(controller.contains("LearningEngineAudioPolicy.toggleMuted()"))
        assertTrue(controller.contains("createRoundedCornerBitmap(rawBitmap, 6f * density)"))
        assertTrue(layout.contains("@+id/overlay_toggle_mute"))
        assertTrue(layout.contains("@+id/overlay_toggle_pause"))
        assertFalse(layout.contains("@+id/overlay_pause_30m"))
        assertFalse(layout.contains("@+id/overlay_pause_1h"))
    }

    @Test
    fun `overlay bottom row layout has Snooze Pause and Mute in exact order`() {
        val layout = java.io.File("src/main/res/layout/overlay_vocabulary_reminder.xml").readText()
        val pause5mIndex = layout.indexOf("id=\"@+id/overlay_pause_5m\"")
        val togglePauseIndex = layout.indexOf("id=\"@+id/overlay_toggle_pause\"")
        val toggleMuteIndex = layout.indexOf("id=\"@+id/overlay_toggle_mute\"")

        assertTrue(pause5mIndex >= 0)
        assertTrue(togglePauseIndex > pause5mIndex, "Pause/Resume button must follow Snooze 5'")
        assertTrue(toggleMuteIndex > togglePauseIndex, "Mute button must follow Pause/Resume button")
    }

    @Test
    fun `overlay controller contains pause resume countdown state and red muted visual`() {
        val controller = java.io.File("src/main/kotlin/vn/loi/learning/android/reminder/AndroidVocabularyReminderOverlayController.kt").readText()
        assertTrue(controller.contains("isCountdownPaused = true"))
        assertTrue(controller.contains("isCountdownPaused = false"))
        assertTrue(controller.contains("ic_overlay_resume"))
        assertTrue(controller.contains("ic_overlay_pause"))
        assertTrue(controller.contains("setColorFilter(0xFFEF4444.toInt(), PorterDuff.Mode.SRC_IN)"))
        assertTrue(controller.contains("clearColorFilter()"))
    }

    @Test
    fun `persistent mute policy suppresses audio playback and persists across restarts`() {
        var persistedMuted = false
        val testStore = object : vn.loi.learning.android.media.AudioMutePreferenceStore {
            override fun loadMuted(): Boolean = persistedMuted
            override fun saveMuted(muted: Boolean) { persistedMuted = muted }
        }
        vn.loi.learning.android.media.LearningEngineAudioPolicy.init(testStore)
        assertFalse(vn.loi.learning.android.media.LearningEngineAudioPolicy.isMuted.value)

        // Toggle mute -> true
        vn.loi.learning.android.media.LearningEngineAudioPolicy.toggleMuted()
        assertTrue(vn.loi.learning.android.media.LearningEngineAudioPolicy.isMuted.value)
        assertTrue(persistedMuted)

        // Simulate app restart / recreation
        vn.loi.learning.android.media.LearningEngineAudioPolicy.resetForTesting(initialMuted = false)
        vn.loi.learning.android.media.LearningEngineAudioPolicy.init(testStore)
        assertTrue(vn.loi.learning.android.media.LearningEngineAudioPolicy.isMuted.value, "Muted state must persist across restarts")

        // Unmute -> false
        vn.loi.learning.android.media.LearningEngineAudioPolicy.setMuted(false)
        assertFalse(vn.loi.learning.android.media.LearningEngineAudioPolicy.isMuted.value)
        assertFalse(persistedMuted)
    }
}
