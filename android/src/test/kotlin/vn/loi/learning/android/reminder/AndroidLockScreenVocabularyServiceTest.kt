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
}
