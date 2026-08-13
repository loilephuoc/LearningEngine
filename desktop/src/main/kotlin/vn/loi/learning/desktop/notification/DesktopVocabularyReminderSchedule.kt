package vn.loi.learning.desktop.notification

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

object DesktopVocabularyReminderSchedule {
    fun isActiveAt(settings: DesktopVocabularyReminderSettings, now: Instant, zoneId: ZoneId): Boolean {
        if (settings.pausedUntil?.let { now < it } == true) return false
        return isInsideActiveWindow(now.atZone(zoneId).toLocalTime(), settings.activeStart, settings.activeEnd)
    }

    fun isInsideActiveWindow(now: LocalTime, start: LocalTime, end: LocalTime): Boolean = when {
        start == end -> true
        start < end -> now >= start && now < end
        else -> now >= start || now < end
    }

    fun pauseFor(settings: DesktopVocabularyReminderSettings, now: Instant, duration: Duration) =
        settings.copy(pausedUntil = now.plus(duration))

    fun pauseToday(settings: DesktopVocabularyReminderSettings, now: Instant, zoneId: ZoneId) =
        settings.copy(
            pausedUntil = now.atZone(zoneId).toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant()
        )
}
