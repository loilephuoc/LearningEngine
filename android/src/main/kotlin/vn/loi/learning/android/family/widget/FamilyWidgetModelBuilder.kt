package vn.loi.learning.android.family.widget

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import vn.loi.learning.android.family.*

object FamilyWidgetModelBuilder {

    private fun Int.pad(): String = if (this < 10) "0$this" else "$this"

    fun build(
        snapshot: FamilyLocalSnapshot,
        calendar: AstronomicalVietnameseLunarCalendar,
        projectionService: CalendarProjectionService,
        now: LocalDateTime = LocalDateTime.now()
    ): FamilyWidgetState {
        val today = now.toLocalDate()
        val lunar = calendar.solarToLunar(today)

        val weekdayName = when (today.dayOfWeek) {
            DayOfWeek.MONDAY -> "Thứ Hai"
            DayOfWeek.TUESDAY -> "Thứ Ba"
            DayOfWeek.WEDNESDAY -> "Thứ Tư"
            DayOfWeek.THURSDAY -> "Thứ Năm"
            DayOfWeek.FRIDAY -> "Thứ Sáu"
            DayOfWeek.SATURDAY -> "Thứ Bảy"
            DayOfWeek.SUNDAY -> "Chủ Nhật"
        }

        val solarFormatted = "${today.dayOfMonth} tháng ${today.monthValue}"
        val solarFull = "${today.dayOfMonth.pad()}/${today.monthValue.pad()}/${today.year}"
        val lunarFormatted = "${lunar.day}/${lunar.month} Âm lịch"

        // Active domain entities only (ignore soft-deleted)
        val activePersons = snapshot.persons.filter { it.deletedAtEpochMillis == null }
        val activeEvents = snapshot.events.filter { it.deletedAtEpochMillis == null }
        val activeTasks = snapshot.tasks.filter { it.deletedAtEpochMillis == null }
        val activeCompletions = snapshot.taskOccurrenceCompletions.filter { it.deletedAtEpochMillis == null }
        val activeCategories = snapshot.categories.filter { it.deletedAtEpochMillis == null }

        // Look back 14 days for overdue tasks and forward 60 days for upcoming events
        val startDate = today.minusDays(14)
        val endDate = today.plusDays(60)

        val projectedOccurrences = projectionService.project(
            persons = activePersons,
            events = activeEvents,
            tasks = activeTasks,
            completions = activeCompletions,
            categories = activeCategories,
            startDate = startDate,
            endDate = endDate,
            now = now
        )

        val widgetItems = mutableListOf<FamilyWidgetItem>()

        for (occ in projectedOccurrences) {
            when (occ.sourceType) {
                CalendarItemType.TASK_DUE -> {
                    if (occ.completed) continue // Do not show completed occurrences as pending
                    val isPastDate = occ.date.isBefore(today)
                    val isToday = occ.date == today
                    val isTimeOverdue = isToday && occ.time != null && now.toLocalTime().isAfter(occ.time)
                    val isOverdue = isPastDate || isTimeOverdue

                    val semantic = when {
                        isOverdue -> FamilyWidgetItemSemantic.TASK_OVERDUE
                        isToday -> FamilyWidgetItemSemantic.TASK_DUE_TODAY
                        else -> FamilyWidgetItemSemantic.TASK_UPCOMING
                    }

                    val badge = when {
                        isPastDate -> {
                            val days = ChronoUnit.DAYS.between(occ.date, today)
                            if (days == 1L) "Quá hạn 1 ngày" else "Quá hạn $days ngày"
                        }
                        isTimeOverdue -> "Quá hạn"
                        isToday -> {
                            if (occ.time != null) "${occ.time.hour.pad()}:${occ.time.minute.pad()}" else "Hôm nay"
                        }
                        occ.date == today.plusDays(1) -> {
                            if (occ.time != null) "Ngày mai · ${occ.time.hour.pad()}:${occ.time.minute.pad()}" else "Ngày mai"
                        }
                        occ.date <= today.plusDays(7) -> {
                            val days = ChronoUnit.DAYS.between(today, occ.date)
                            "Còn $days ngày"
                        }
                        else -> "${occ.date.dayOfMonth.pad()}/${occ.date.monthValue.pad()}"
                    }

                    widgetItems.add(
                        FamilyWidgetItem(
                            id = occ.id,
                            sourceId = occ.sourceId,
                            sourceType = occ.sourceType,
                            title = occ.title,
                            date = occ.date,
                            time = occ.time,
                            semantic = semantic,
                            iconText = "📋",
                            badgeText = badge,
                            isToday = isToday,
                            isOverdue = isOverdue
                        )
                    )
                }
                CalendarItemType.BIRTHDAY -> {
                    if (occ.date.isBefore(today)) continue // Past birthday occurrences ignored
                    val isToday = occ.date == today
                    val badge = when {
                        isToday -> "Hôm nay"
                        occ.date == today.plusDays(1) -> "Ngày mai"
                        occ.date <= today.plusDays(7) -> "Còn ${ChronoUnit.DAYS.between(today, occ.date)} ngày"
                        else -> "${occ.date.dayOfMonth.pad()}/${occ.date.monthValue.pad()}"
                    }

                    widgetItems.add(
                        FamilyWidgetItem(
                            id = occ.id,
                            sourceId = occ.sourceId,
                            sourceType = occ.sourceType,
                            title = occ.title,
                            date = occ.date,
                            time = occ.time,
                            semantic = FamilyWidgetItemSemantic.BIRTHDAY,
                            iconText = "🎂",
                            badgeText = badge,
                            isToday = isToday,
                            isOverdue = false
                        )
                    )
                }
                CalendarItemType.EVENT -> {
                    if (occ.date.isBefore(today)) continue // Past event occurrences ignored
                    val isToday = occ.date == today
                    val badge = when {
                        isToday -> "Hôm nay"
                        occ.date == today.plusDays(1) -> "Ngày mai"
                        occ.date <= today.plusDays(7) -> "Còn ${ChronoUnit.DAYS.between(today, occ.date)} ngày"
                        else -> "${occ.date.dayOfMonth.pad()}/${occ.date.monthValue.pad()}"
                    }

                    widgetItems.add(
                        FamilyWidgetItem(
                            id = occ.id,
                            sourceId = occ.sourceId,
                            sourceType = occ.sourceType,
                            title = occ.title,
                            date = occ.date,
                            time = occ.time,
                            semantic = FamilyWidgetItemSemantic.EVENT,
                            iconText = occ.iconKey ?: "📌",
                            badgeText = badge,
                            isToday = isToday,
                            isOverdue = false
                        )
                    )
                }
                CalendarItemType.TASK_COMPLETION -> {
                    // Completions ignored in active pending widget
                }
            }
        }

        // Ordering policy:
        // 1. Overdue tasks (sorted by date asc, time asc)
        // 2. Due today tasks (sorted by time asc)
        // 3. Today events/birthdays (sorted by time asc, title asc)
        // 4. Upcoming items from tomorrow onwards (sorted by date asc, time asc, title asc)
        val overdueTasks = widgetItems.filter { it.semantic == FamilyWidgetItemSemantic.TASK_OVERDUE }
            .sortedWith(compareBy({ it.date }, { it.time ?: LocalTime.MAX }))

        val dueTodayTasks = widgetItems.filter { it.semantic == FamilyWidgetItemSemantic.TASK_DUE_TODAY }
            .sortedWith(compareBy({ it.time ?: LocalTime.MAX }, { it.title }))

        val todayEventsAndBirthdays = widgetItems.filter { (it.semantic == FamilyWidgetItemSemantic.EVENT || it.semantic == FamilyWidgetItemSemantic.BIRTHDAY) && it.isToday }
            .sortedWith(compareBy({ it.time ?: LocalTime.MAX }, { it.title }))

        val upcomingItems = widgetItems.filter { it.date.isAfter(today) }
            .sortedWith(compareBy({ it.date }, { it.time ?: LocalTime.MAX }, { it.title }))

        val allItemsOrdered = overdueTasks + dueTodayTasks + todayEventsAndBirthdays + upcomingItems
        val todayItems = overdueTasks + dueTodayTasks + todayEventsAndBirthdays

        // Build workload summary
        val todayTaskCount = todayItems.count { it.sourceType == CalendarItemType.TASK_DUE }
        val todayEventCount = todayItems.count { it.sourceType == CalendarItemType.EVENT }
        val todayBirthdayCount = todayItems.count { it.sourceType == CalendarItemType.BIRTHDAY }

        val todaySummary = if (todayItems.isEmpty()) {
            "Hôm nay chưa có lịch"
        } else {
            buildString {
                append("${todayItems.size} mục")
                val parts = mutableListOf<String>()
                if (todayTaskCount > 0) parts.add("$todayTaskCount việc")
                if (todayEventCount > 0) parts.add("$todayEventCount sự kiện")
                if (todayBirthdayCount > 0) parts.add("$todayBirthdayCount sinh nhật")
                if (parts.isNotEmpty()) {
                    append(" · ")
                    append(parts.joinToString(", "))
                }
            }
        }

        return FamilyWidgetState(
            todaySolarWeekday = weekdayName,
            todaySolarDateFormatted = solarFormatted,
            todaySolarFull = solarFull,
            todayLunarFormatted = lunarFormatted,
            todaySummary = todaySummary,
            todayItems = todayItems,
            upcomingItems = upcomingItems,
            allItemsOrdered = allItemsOrdered
        )
    }
}
