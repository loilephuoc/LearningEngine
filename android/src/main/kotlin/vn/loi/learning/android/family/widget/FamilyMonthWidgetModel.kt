package vn.loi.learning.android.family.widget

import java.time.LocalDate
import java.time.YearMonth

data class MonthWidgetDayCell(
    val date: LocalDate,
    val solarDayText: String,
    val lunarDayText: String,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val hasBirthday: Boolean,
    val hasEvent: Boolean,
    val hasTask: Boolean,
    val isOccupied: Boolean,
    val markerText: String = ""
)

data class FamilyMonthWidgetState(
    val monthHeaderTitle: String,
    val yearMonth: YearMonth,
    val today: LocalDate,
    val cells: List<MonthWidgetDayCell>
)
