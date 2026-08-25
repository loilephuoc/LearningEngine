package vn.loi.learning.android.family.widget

import java.time.LocalDate
import java.time.LocalTime
import vn.loi.learning.android.family.CalendarItemType

enum class FamilyWidgetItemSemantic {
    BIRTHDAY,
    EVENT,
    TASK_OVERDUE,
    TASK_DUE_TODAY,
    TASK_UPCOMING
}

data class FamilyWidgetItem(
    val id: String,
    val sourceId: String,
    val sourceType: CalendarItemType,
    val title: String,
    val date: LocalDate,
    val time: LocalTime?,
    val semantic: FamilyWidgetItemSemantic,
    val iconText: String,
    val badgeText: String,
    val isToday: Boolean,
    val isOverdue: Boolean
)

data class FamilyWidgetState(
    val todaySolarWeekday: String,
    val todaySolarDateFormatted: String,
    val todaySolarFull: String,
    val todayLunarFormatted: String,
    val todaySummary: String,
    val todayItems: List<FamilyWidgetItem>,
    val upcomingItems: List<FamilyWidgetItem>,
    val allItemsOrdered: List<FamilyWidgetItem>
)
