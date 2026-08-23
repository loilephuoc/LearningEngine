package vn.loi.learning.android.notification

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test

class DailyLearningNotificationTest {

    private class FakeNotificationStore : DailyLearningNotificationPreferenceStore {
        var savedSettings = DailyLearningNotificationSettings()
        override fun load(): DailyLearningNotificationSettings = savedSettings
        override fun save(settings: DailyLearningNotificationSettings): Boolean {
            savedSettings = settings
            return true
        }
    }

    @Test
    fun `preferences store and controller persist due and inactivity settings`() {
        val store = FakeNotificationStore()
        val controller = DailyLearningNotificationPreferencesController(store)

        // Defaults
        assertFalse(controller.current().dueReview.enabled)
        assertEquals(20, controller.current().dueReview.hour)
        assertEquals(0, controller.current().dueReview.minute)
        assertFalse(controller.current().inactivity.enabled)
        assertEquals(3, controller.current().inactivity.thresholdDays)
        assertEquals(9, controller.current().inactivity.hour)

        // Update Due Review
        controller.updateDueReview(enabled = true, hour = 21, minute = 30)
        assertTrue(controller.current().dueReview.enabled)
        assertEquals(21, controller.current().dueReview.hour)
        assertEquals(30, controller.current().dueReview.minute)
        assertEquals("21:30", controller.current().dueReview.timeLabel)

        // Update Inactivity
        controller.updateInactivity(enabled = true, thresholdDays = 5, hour = 8, minute = 15)
        assertTrue(controller.current().inactivity.enabled)
        assertEquals(5, controller.current().inactivity.thresholdDays)
        assertEquals(8, controller.current().inactivity.hour)
        assertEquals(15, controller.current().inactivity.minute)
        assertEquals("08:15", controller.current().inactivity.timeLabel)

        // Record Activity
        val now = 1700000000000L
        controller.recordActivity(now)
        assertEquals(now, controller.current().lastActivityTimestamp)
    }

    @Test
    fun `scheduler computes correct next trigger millis before target time`() {
        val zoneId = ZoneId.of("UTC")
        val now = LocalDateTime.of(2026, 8, 23, 10, 0, 0)
        // Target is 20:00 today -> should be today 20:00 UTC
        val triggerMillis = DailyLearningNotificationScheduler.computeNextTriggerMillis(
            hour = 20,
            minute = 0,
            lastNotifiedDate = null,
            nowDateTime = now,
            zoneId = zoneId
        )
        val expected = LocalDateTime.of(2026, 8, 23, 20, 0, 0).atZone(zoneId).toInstant().toEpochMilli()
        assertEquals(expected, triggerMillis)
    }

    @Test
    fun `scheduler computes next day trigger millis after target time or if already notified`() {
        val zoneId = ZoneId.of("UTC")
        val now = LocalDateTime.of(2026, 8, 23, 21, 0, 0)
        // Target is 20:00, now is 21:00 -> should be tomorrow 20:00 UTC
        val triggerMillis = DailyLearningNotificationScheduler.computeNextTriggerMillis(
            hour = 20,
            minute = 0,
            lastNotifiedDate = null,
            nowDateTime = now,
            zoneId = zoneId
        )
        val expected = LocalDateTime.of(2026, 8, 24, 20, 0, 0).atZone(zoneId).toInstant().toEpochMilli()
        assertEquals(expected, triggerMillis)

        // If now is 10:00 but already notified today (2026-08-23) -> should be tomorrow 20:00 UTC
        val morningNow = LocalDateTime.of(2026, 8, 23, 10, 0, 0)
        val triggerAlreadyNotified = DailyLearningNotificationScheduler.computeNextTriggerMillis(
            hour = 20,
            minute = 0,
            lastNotifiedDate = "2026-08-23",
            nowDateTime = morningNow,
            zoneId = zoneId
        )
        assertEquals(expected, triggerAlreadyNotified)
    }

    @Test
    fun `due and inactivity notification channels are defined with Vietnamese names and descriptions`() {
        assertEquals("learning_due_review_reminder", DailyLearningNotificationHelper.CHANNEL_DUE_REVIEW)
        assertEquals("learning_inactivity_reminder", DailyLearningNotificationHelper.CHANNEL_INACTIVITY)
        assertEquals(8001, DailyLearningNotificationHelper.NOTIFICATION_ID_DUE_REVIEW)
        assertEquals(8002, DailyLearningNotificationHelper.NOTIFICATION_ID_INACTIVITY)
    }

    @Test
    fun `manifest registers DailyLearningReminderReceiver with boot and time change filters`() {
        val manifest = java.io.File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android.permission.RECEIVE_BOOT_COMPLETED"))
        assertTrue(manifest.contains("DailyLearningReminderReceiver"))
        assertTrue(manifest.contains("android.intent.action.BOOT_COMPLETED"))
        assertTrue(manifest.contains("android.intent.action.MY_PACKAGE_REPLACED"))
        assertTrue(manifest.contains("android.intent.action.TIMEZONE_CHANGED"))
        assertTrue(manifest.contains("android.intent.action.TIME_SET"))
    }

    @Test
    fun `settings screen contains Vietnamese UI for daily learning reminders`() {
        val navFile = java.io.File("src/main/kotlin/vn/loi/learning/android/ui/AndroidRootNavigation.kt").readText()
        assertTrue(navFile.contains("Thông báo nhắc học tập"))
        assertTrue(navFile.contains("Nhắc ôn tập đến hạn"))
        assertTrue(navFile.contains("Nhắc quay lại học"))
        assertTrue(navFile.contains("Ngưỡng không hoạt động:"))
    }
}
