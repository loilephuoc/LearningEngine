package vn.loi.learning.android.family.widget

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import vn.loi.learning.android.family.AstronomicalVietnameseLunarCalendar
import vn.loi.learning.android.family.CalendarItemType
import vn.loi.learning.android.family.CalendarProjectionService
import vn.loi.learning.android.family.FamilyLocalSnapshot

object FamilyMonthWidgetModelBuilder {

    fun build(
        snapshot: FamilyLocalSnapshot,
        calendar: AstronomicalVietnameseLunarCalendar,
        projectionService: CalendarProjectionService,
        now: LocalDateTime = LocalDateTime.now(),
        targetYearMonth: YearMonth = YearMonth.from(now.toLocalDate())
    ): FamilyMonthWidgetState {
        val today = now.toLocalDate()
        val firstDayOfMonth = targetYearMonth.atDay(1)
        val startDate = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endDate = startDate.plusDays(41) // 42 cells = 6 rows x 7 days

        val activePersons = snapshot.persons.filter { it.deletedAtEpochMillis == null }
        val activeEvents = snapshot.events.filter { it.deletedAtEpochMillis == null }
        val activeTasks = snapshot.tasks.filter { it.deletedAtEpochMillis == null }
        val activeCompletions = snapshot.taskOccurrenceCompletions.filter { it.deletedAtEpochMillis == null }
        val activeCategories = snapshot.categories.filter { it.deletedAtEpochMillis == null }

        val occurrences = projectionService.project(
            persons = activePersons,
            events = activeEvents,
            tasks = activeTasks,
            completions = activeCompletions,
            categories = activeCategories,
            startDate = startDate,
            endDate = endDate,
            now = now
        )

        val occurrencesByDate = occurrences.groupBy { it.date }

        val cells = (0 until 42).map { offset ->
            val cellDate = startDate.plusDays(offset.toLong())
            val isCurrentMonth = cellDate.monthValue == targetYearMonth.monthValue && cellDate.year == targetYearMonth.year
            val isToday = cellDate == today

            val lunar = calendar.solarToLunar(cellDate)
            val lunarDayText = if (lunar.day == 1) "${lunar.day}/${lunar.month}" else "${lunar.day}"

            val dayOccurrences = occurrencesByDate[cellDate].orEmpty()
            val hasBirthday = dayOccurrences.any { it.sourceType == CalendarItemType.BIRTHDAY }
            val hasEvent = dayOccurrences.any { it.sourceType == CalendarItemType.EVENT }
            val hasTask = dayOccurrences.any { it.sourceType == CalendarItemType.TASK_DUE && !it.completed }
            val isOccupied = hasBirthday || hasEvent || hasTask

            val totalActiveMarkers = (if (hasBirthday) 1 else 0) + (if (hasEvent) 1 else 0) + (if (hasTask) 1 else 0)
            val markerText = when {
                totalActiveMarkers >= 3 -> "● ● ●"
                totalActiveMarkers == 2 -> "● ●"
                totalActiveMarkers == 1 -> "●"
                else -> ""
            }

            MonthWidgetDayCell(
                date = cellDate,
                solarDayText = cellDate.dayOfMonth.toString(),
                lunarDayText = lunarDayText,
                isCurrentMonth = isCurrentMonth,
                isToday = isToday,
                hasBirthday = hasBirthday,
                hasEvent = hasEvent,
                hasTask = hasTask,
                isOccupied = isOccupied,
                markerText = markerText
            )
        }

        val monthHeaderTitle = "Tháng ${targetYearMonth.monthValue}/${targetYearMonth.year}"

        return FamilyMonthWidgetState(
            monthHeaderTitle = monthHeaderTitle,
            yearMonth = targetYearMonth,
            today = today,
            cells = cells
        )
    }
}
