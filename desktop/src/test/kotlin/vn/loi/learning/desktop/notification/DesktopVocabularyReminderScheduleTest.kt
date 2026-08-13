package vn.loi.learning.desktop.notification

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopVocabularyReminderScheduleTest {
    @Test
    fun `same day window is half open`() {
        val start = LocalTime.of(8, 0)
        val end = LocalTime.of(22, 0)
        assertTrue(DesktopVocabularyReminderSchedule.isInsideActiveWindow(start, start, end))
        assertTrue(DesktopVocabularyReminderSchedule.isInsideActiveWindow(LocalTime.NOON, start, end))
        assertFalse(DesktopVocabularyReminderSchedule.isInsideActiveWindow(end, start, end))
        assertFalse(DesktopVocabularyReminderSchedule.isInsideActiveWindow(LocalTime.of(7, 59), start, end))
    }

    @Test
    fun `overnight window includes both sides of midnight and excludes daytime`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(6, 0)
        assertTrue(DesktopVocabularyReminderSchedule.isInsideActiveWindow(LocalTime.of(23, 59), start, end))
        assertTrue(DesktopVocabularyReminderSchedule.isInsideActiveWindow(LocalTime.of(5, 59), start, end))
        assertFalse(DesktopVocabularyReminderSchedule.isInsideActiveWindow(LocalTime.of(6, 0), start, end))
        assertFalse(DesktopVocabularyReminderSchedule.isInsideActiveWindow(LocalTime.NOON, start, end))
    }

    @Test
    fun `equal start and end is active all day`() {
        assertTrue(
            DesktopVocabularyReminderSchedule.isInsideActiveWindow(
                LocalTime.MIDNIGHT,
                LocalTime.NOON,
                LocalTime.NOON
            )
        )
    }

    @Test
    fun `pause suppresses until instant and expiry is inclusive`() {
        val now = Instant.parse("2026-08-13T10:00:00Z")
        val settings = DesktopVocabularyReminderSettings(
            activeStart = LocalTime.MIDNIGHT,
            activeEnd = LocalTime.MIDNIGHT,
            pausedUntil = now.plusSeconds(60)
        )
        assertFalse(DesktopVocabularyReminderSchedule.isActiveAt(settings, now, ZoneOffset.UTC))
        assertTrue(
            DesktopVocabularyReminderSchedule.isActiveAt(
                settings,
                now.plusSeconds(60),
                ZoneOffset.UTC
            )
        )
    }

    @Test
    fun `duration pauses and pause today are deterministic and zone aware`() {
        val now = Instant.parse("2026-08-13T16:30:00Z")
        val defaults = DesktopVocabularyReminderSettings()
        assertEquals(
            now.plus(Duration.ofMinutes(30)),
            DesktopVocabularyReminderSchedule.pauseFor(defaults, now, Duration.ofMinutes(30)).pausedUntil
        )
        assertEquals(
            now.plus(Duration.ofHours(1)),
            DesktopVocabularyReminderSchedule.pauseFor(defaults, now, Duration.ofHours(1)).pausedUntil
        )
        assertEquals(
            Instant.parse("2026-08-13T17:00:00Z"),
            DesktopVocabularyReminderSchedule.pauseToday(
                defaults,
                now,
                ZoneId.of("Asia/Ho_Chi_Minh")
            ).pausedUntil
        )
    }
}
