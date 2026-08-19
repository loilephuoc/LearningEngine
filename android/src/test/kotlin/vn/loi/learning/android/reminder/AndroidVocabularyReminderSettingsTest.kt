package vn.loi.learning.android.reminder

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class AndroidVocabularyReminderSettingsTest {

    private class InMemoryPreferenceStore(
        private var settings: AndroidVocabularyReminderSettings = AndroidVocabularyReminderSettings()
    ) : AndroidVocabularyReminderPreferenceStore {
        override fun load(): AndroidVocabularyReminderSettings = settings
        override fun save(settings: AndroidVocabularyReminderSettings): Boolean {
            this.settings = settings
            return true
        }
    }

    @Test
    fun `1 enabled persists correctly`() {
        val store = InMemoryPreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        assertFalse(controller.current().enabled)
        assertTrue(controller.setEnabled(true))
        assertTrue(controller.current().enabled)
        assertTrue(store.load().enabled)
    }

    @Test
    fun `2 packageId persists correctly`() {
        val store = InMemoryPreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        assertNull(controller.current().selectedPackageId)
        val updated = controller.current().copy(selectedPackageId = "pkg-intermediate-123")
        assertTrue(controller.updateSettings(updated))
        assertEquals("pkg-intermediate-123", controller.current().selectedPackageId)
        assertEquals("pkg-intermediate-123", store.load().selectedPackageId)
    }

    @Test
    fun `3 selection mode persists across all enum values`() {
        val store = InMemoryPreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        for (mode in AndroidVocabularyReminderSelectionMode.entries) {
            assertTrue(controller.updateSettings(controller.current().copy(selectionMode = mode)))
            assertEquals(mode, controller.current().selectionMode)
            assertEquals(mode, store.load().selectionMode)
        }
    }

    @Test
    fun `4 seconds interval persists and computes intervalMinutes correctly`() {
        val store = InMemoryPreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        val secondsSettings = controller.current().copy(intervalMillis = 20_000L)
        assertTrue(controller.updateSettings(secondsSettings))
        assertEquals(20_000L, controller.current().intervalMillis)
        assertEquals(1, controller.current().intervalMinutes)
    }

    @Test
    fun `5 minutes interval persists correctly`() {
        val store = InMemoryPreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        val minutesSettings = controller.current().copy(intervalMillis = 30 * 60_000L)
        assertTrue(controller.updateSettings(minutesSettings))
        assertEquals(30 * 60_000L, controller.current().intervalMillis)
        assertEquals(30, controller.current().intervalMinutes)
    }

    @Test
    fun `6 minimum 5 seconds is enforced by settings and draft validation`() {
        val validDraft = AndroidVocabularyReminderDraft(
            enabled = true,
            selectedPackageId = "pkg-1",
            selectionMode = AndroidVocabularyReminderSelectionMode.AGAIN_HARD,
            intervalValueText = "5",
            intervalUnit = AndroidVocabularyReminderIntervalUnit.SECONDS,
            activeStartText = "08:00",
            activeEndText = "22:00",
            displayDurationText = "5.0",
            autoPlayPronunciation = true
        )
        val validResult = validDraft.validate(null)
        assertTrue(validResult is AndroidVocabularyReminderDraftValidation.Valid)
        assertEquals(5_000L, validResult.settings.intervalMillis)

        val tooLowDraft = validDraft.copy(intervalValueText = "4")
        val invalidResult = tooLowDraft.validate(null)
        assertTrue(invalidResult is AndroidVocabularyReminderDraftValidation.Invalid)
        assertTrue(invalidResult.message.contains("5 seconds"))
    }

    @Test
    fun `7 invalid intervals rejected in draft validation`() {
        val baseDraft = AndroidVocabularyReminderDraft(
            enabled = true,
            selectedPackageId = "pkg-1",
            selectionMode = AndroidVocabularyReminderSelectionMode.AGAIN_HARD,
            intervalValueText = "invalid",
            intervalUnit = AndroidVocabularyReminderIntervalUnit.MINUTES,
            activeStartText = "08:00",
            activeEndText = "22:00",
            displayDurationText = "5.0",
            autoPlayPronunciation = true
        )
        val result = baseDraft.validate(null)
        assertTrue(result is AndroidVocabularyReminderDraftValidation.Invalid)
    }

    @Test
    fun `8 notification display duration validates between 1_5 and 60 seconds`() {
        val baseDraft = AndroidVocabularyReminderDraft(
            enabled = true,
            selectedPackageId = "pkg-1",
            selectionMode = AndroidVocabularyReminderSelectionMode.AGAIN_HARD,
            intervalValueText = "15",
            intervalUnit = AndroidVocabularyReminderIntervalUnit.MINUTES,
            activeStartText = "08:00",
            activeEndText = "22:00",
            displayDurationText = "1.5",
            autoPlayPronunciation = true
        )
        assertTrue(baseDraft.validate(null) is AndroidVocabularyReminderDraftValidation.Valid)

        val tooLow = baseDraft.copy(displayDurationText = "1.4")
        assertTrue(tooLow.validate(null) is AndroidVocabularyReminderDraftValidation.Invalid)

        val maxValid = baseDraft.copy(displayDurationText = "60.0")
        assertTrue(maxValid.validate(null) is AndroidVocabularyReminderDraftValidation.Valid)

        val tooHigh = baseDraft.copy(displayDurationText = "60.1")
        assertTrue(tooHigh.validate(null) is AndroidVocabularyReminderDraftValidation.Invalid)
    }

    @Test
    fun `9 normal daytime active window evaluation`() {
        val settings = AndroidVocabularyReminderSettings(
            enabled = true,
            activeStart = LocalTime.of(8, 0),
            activeEnd = LocalTime.of(22, 0)
        )
        val zone = ZoneId.of("UTC")

        // 07:59 -> inactive
        val t1 = Instant.parse("2026-08-18T07:59:00Z")
        assertFalse(AndroidVocabularyReminderRuntime.isActiveAt(settings, t1, zone))

        // 08:00 -> active
        val t2 = Instant.parse("2026-08-18T08:00:00Z")
        assertTrue(AndroidVocabularyReminderRuntime.isActiveAt(settings, t2, zone))

        // 15:30 -> active
        val t3 = Instant.parse("2026-08-18T15:30:00Z")
        assertTrue(AndroidVocabularyReminderRuntime.isActiveAt(settings, t3, zone))

        // 22:00 -> active
        val t4 = Instant.parse("2026-08-18T22:00:00Z")
        assertTrue(AndroidVocabularyReminderRuntime.isActiveAt(settings, t4, zone))

        // 22:01 -> inactive
        val t5 = Instant.parse("2026-08-18T22:01:00Z")
        assertFalse(AndroidVocabularyReminderRuntime.isActiveAt(settings, t5, zone))
    }

    @Test
    fun `10 overnight active window evaluation`() {
        // Active from 22:00 to 07:00 next morning
        val settings = AndroidVocabularyReminderSettings(
            enabled = true,
            activeStart = LocalTime.of(22, 0),
            activeEnd = LocalTime.of(7, 0)
        )
        val zone = ZoneId.of("UTC")

        // 21:59 -> inactive
        val t1 = Instant.parse("2026-08-18T21:59:00Z")
        assertFalse(AndroidVocabularyReminderRuntime.isActiveAt(settings, t1, zone))

        // 22:00 -> active
        val t2 = Instant.parse("2026-08-18T22:00:00Z")
        assertTrue(AndroidVocabularyReminderRuntime.isActiveAt(settings, t2, zone))

        // 03:00 -> active
        val t3 = Instant.parse("2026-08-18T03:00:00Z")
        assertTrue(AndroidVocabularyReminderRuntime.isActiveAt(settings, t3, zone))

        // 07:00 -> active
        val t4 = Instant.parse("2026-08-18T07:00:00Z")
        assertTrue(AndroidVocabularyReminderRuntime.isActiveAt(settings, t4, zone))

        // 07:01 -> inactive
        val t5 = Instant.parse("2026-08-18T07:01:00Z")
        assertFalse(AndroidVocabularyReminderRuntime.isActiveAt(settings, t5, zone))
    }

    @Test
    fun `11 autoplay pronunciation setting persists`() {
        val store = InMemoryPreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        assertFalse(controller.current().autoPlayPronunciation)
        assertTrue(controller.updateSettings(controller.current().copy(autoPlayPronunciation = true)))
        assertTrue(controller.current().autoPlayPronunciation)
        assertTrue(store.load().autoPlayPronunciation)
    }

    @Test
    fun `12 pause controls set and clear pausedUntil`() {
        val store = InMemoryPreferenceStore()
        val controller = AndroidVocabularyReminderPreferencesController(store)

        assertNull(controller.current().pausedUntil)
        assertTrue(controller.pause30Minutes())
        assertTrue(controller.current().pausedUntil != null)

        val pausedSettings = controller.current()
        val zone = ZoneId.of("UTC")
        val now = Instant.now()
        // During pause window, isActiveAt returns false even if enabled
        assertFalse(AndroidVocabularyReminderRuntime.isActiveAt(pausedSettings.copy(enabled = true), now, zone))

        assertTrue(controller.resumeNow())
        assertNull(controller.current().pausedUntil)
    }
}
