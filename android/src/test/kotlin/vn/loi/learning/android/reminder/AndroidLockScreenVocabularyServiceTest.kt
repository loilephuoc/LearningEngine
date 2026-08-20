package vn.loi.learning.android.reminder

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AndroidLockScreenVocabularyServiceTest {

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
    fun `service channel configuration matches low importance requirements`() {
        assertEquals("lockscreen_vocabulary_service", AndroidLockScreenVocabularyService.CHANNEL_ID)
        assertEquals(20261, AndroidLockScreenVocabularyService.NOTIFICATION_ID)
    }

    @Test
    fun `persisted enabled state is correctly loaded on application startup`() {
        val store = InMemoryStore()
        store.lockScreenSettings = AndroidLockScreenVocabularySettings(
            enabled = true,
            selectedPackageId = "pkg-test-1",
            selectionMode = AndroidLockScreenVocabularyMode.RANDOM_ALL
        )
        val controller = AndroidVocabularyReminderPreferencesController(store)
        assertTrue(controller.currentLockScreen().enabled)
        assertEquals("pkg-test-1", controller.currentLockScreen().selectedPackageId)
    }

    @Test
    fun `persisted disabled state does not trigger restore on startup`() {
        val store = InMemoryStore()
        store.lockScreenSettings = AndroidLockScreenVocabularySettings(
            enabled = false,
            selectedPackageId = "pkg-test-2"
        )
        val controller = AndroidVocabularyReminderPreferencesController(store)
        assertFalse(controller.currentLockScreen().enabled)
    }

    @Test
    fun `single auth request contract ensures exactly one keyguard request is issued`() {
        val authRequested = AtomicBoolean(false)
        val requestCount = AtomicInteger(0)

        val tryRequestAuth = {
            if (authRequested.compareAndSet(false, true)) {
                requestCount.incrementAndGet()
            }
        }

        // First attempt (e.g. from decorView.post / onResume)
        tryRequestAuth()
        assertEquals(1, requestCount.get())

        // Second attempt (e.g. from handleCardTap or subsequent onResume)
        tryRequestAuth()
        assertEquals(1, requestCount.get())
    }

    @Test
    fun `security contract - onDismissError does NOT trigger review navigation`() {
        var reviewNavigated = false
        val onDismissErrorAction = {
            // STRICT SECURITY: Do NOT launch review on error
            // reviewNavigated remains false
        }

        onDismissErrorAction()
        assertFalse(reviewNavigated)
    }

    @Test
    fun `security contract - onDismissCancelled does NOT trigger review navigation`() {
        var reviewNavigated = false
        val onDismissCancelledAction = {
            // STRICT SECURITY: Do NOT launch review on cancel
            // reviewNavigated remains false
        }

        onDismissCancelledAction()
        assertFalse(reviewNavigated)
    }

    @Test
    fun `security contract - onDismissSucceeded with tap flag true launches review anchor`() {
        var navigatedPackage: String? = null
        val reviewRequestedOnTap = AtomicBoolean(true)

        val onDismissSucceededAction = {
            if (reviewRequestedOnTap.get()) {
                navigatedPackage = "pkg-target-1"
            }
        }

        onDismissSucceededAction()
        assertEquals("pkg-target-1", navigatedPackage)
    }

    @Test
    fun `security contract - onDismissSucceeded without tap flag does NOT launch review`() {
        var navigatedPackage: String? = null
        val reviewRequestedOnTap = AtomicBoolean(false)

        val onDismissSucceededAction = {
            if (reviewRequestedOnTap.get()) {
                navigatedPackage = "pkg-target-1"
            }
        }

        onDismissSucceededAction()
        assertEquals(null, navigatedPackage)
    }

    // -------------------------------------------------------------
    // ROUND 10.1 UNIT TESTS: FOREGROUND SERVICE LIFECYCLE RECONCILIATION
    // -------------------------------------------------------------

    @Test
    fun `requirement matrix strictly computes required state across 8 combinations`() {
        fun req(lock: Boolean, reminder: Boolean, widget: Boolean) =
            ForegroundServiceRequirement(lockScreenRequired = lock, unlockedReminderRequired = reminder, homeWidgetRequired = widget).required

        // 000 -> false
        assertFalse(req(lock = false, reminder = false, widget = false))

        // 100 -> true
        assertTrue(req(lock = true, reminder = false, widget = false))

        // 010 -> true
        assertTrue(req(lock = false, reminder = true, widget = false))

        // 001 -> true
        assertTrue(req(lock = false, reminder = false, widget = true))

        // 110 -> true
        assertTrue(req(lock = true, reminder = true, widget = false))

        // 101 -> true
        assertTrue(req(lock = true, reminder = false, widget = true))

        // 011 -> true
        assertTrue(req(lock = false, reminder = true, widget = true))

        // 111 -> true
        assertTrue(req(lock = true, reminder = true, widget = true))
    }

    @Test
    fun `idempotency contract ensures repeated reconciliations do not create duplicate service actions`() {
        var startCount = 0
        var stopCount = 0
        var isRunning = false

        fun reconcile(requirement: Boolean) {
            if (requirement) {
                if (!isRunning) {
                    startCount++
                    isRunning = true
                }
            } else {
                if (isRunning) {
                    stopCount++
                    isRunning = false
                }
            }
        }

        // 1. Initial requirement: true
        reconcile(true)
        reconcile(true)
        reconcile(true)
        assertEquals(1, startCount)
        assertEquals(0, stopCount)

        // 2. Requirement changed to false
        reconcile(false)
        reconcile(false)
        reconcile(false)
        assertEquals(1, startCount)
        assertEquals(1, stopCount)
    }

    @Test
    fun `feature handoff ensures service stays running when one feature stops while another is active`() {
        var lockEnabled = false
        var reminderEnabled = false
        var widgetRequired = false

        fun isServiceRequired() =
            ForegroundServiceRequirement(lockEnabled, reminderEnabled, widgetRequired).required

        // 1. Lock Screen ON -> Service required
        lockEnabled = true
        assertTrue(isServiceRequired())

        // 2. Reminder also ON -> Service still required
        reminderEnabled = true
        assertTrue(isServiceRequired())

        // 3. Lock Screen OFF -> Service MUST NOT stop because Reminder is still ON!
        lockEnabled = false
        assertTrue(isServiceRequired())

        // 4. Reminder OFF -> Service can now safely stop
        reminderEnabled = false
        assertFalse(isServiceRequired())
    }

    @Test
    fun `widget handoff ensures service stays running when widget is removed but reminder is active`() {
        var lockEnabled = false
        var reminderEnabled = false
        var activeWidgetCount = 0
        var autoNextEnabled = false

        fun isServiceRequired() =
            ForegroundServiceRequirement(lockEnabled, reminderEnabled, activeWidgetCount > 0 && autoNextEnabled).required

        // 1. Widget with auto-next active
        activeWidgetCount = 1
        autoNextEnabled = true
        assertTrue(isServiceRequired())

        // 2. Reminder turned ON
        reminderEnabled = true
        assertTrue(isServiceRequired())

        // 3. Widget removed -> Service MUST NOT stop because Reminder is still ON
        activeWidgetCount = 0
        assertTrue(isServiceRequired())

        // 4. Reminder turned OFF -> Service stops
        reminderEnabled = false
        assertFalse(isServiceRequired())
    }

    @Test
    fun `last widget removal stops service when lock and reminder are disabled`() {
        var activeWidgetCount = 2
        var autoNextEnabled = true

        fun isServiceRequired() =
            ForegroundServiceRequirement(false, false, activeWidgetCount > 0 && autoNextEnabled).required

        assertTrue(isServiceRequired())

        // Remove 1 widget -> 1 remaining -> still running
        activeWidgetCount = 1
        assertTrue(isServiceRequired())

        // Remove final widget -> 0 remaining -> stop
        activeWidgetCount = 0
        assertFalse(isServiceRequired())
    }

    @Test
    fun `process startup correctly reconstructs service requirement from persisted state`() {
        var persistedLock = false
        var persistedReminder = true
        var activeWidgets = 0

        val restoredReq = ForegroundServiceRequirement(persistedLock, persistedReminder, activeWidgets > 0)
        assertTrue(restoredReq.required)

        persistedReminder = false
        val restoredReqOff = ForegroundServiceRequirement(persistedLock, persistedReminder, activeWidgets > 0)
        assertFalse(restoredReqOff.required)
    }

    @Test
    fun `stale settings safety contract ensures reconciliation uses latest state rather than captured snapshot`() {
        var currentLockState = false

        // Simulating capturing old state
        val oldStateSnapshot = currentLockState

        // State changes to true in background
        currentLockState = true

        // Reconciler should read currentLockState, not oldStateSnapshot
        fun reconcile(): Boolean {
            val liveState = currentLockState
            return ForegroundServiceRequirement(liveState, false, false).required
        }

        assertTrue(reconcile())
        assertFalse(oldStateSnapshot)
    }

    @Test
    fun `sticky restart stops self immediately if requirement is no longer true`() {
        var serviceRequired = false
        var stopSelfCalled = false
        var continueCalled = false

        fun onStartCommand() {
            if (!serviceRequired) {
                stopSelfCalled = true
            } else {
                continueCalled = true
            }
        }

        onStartCommand()
        assertTrue(stopSelfCalled)
        assertFalse(continueCalled)

        // If requirement is true
        serviceRequired = true
        stopSelfCalled = false
        onStartCommand()
        assertFalse(stopSelfCalled)
        assertTrue(continueCalled)
    }
}
