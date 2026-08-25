package vn.loi.learning.android.family

import java.time.LocalDate
import java.time.YearMonth

object FamilyCalendarNavigationHelper {

    val SUPPORTED_YEAR_RANGE = 1900..2100

    /**
     * Parses a four-digit year and accepts it only when it is inside the lunar calendar's safe range.
     * Returns null for blank, non-numeric or out-of-range input.
     */
    fun parseSupportedYear(input: String): Int? {
        val year = input.trim().toIntOrNull() ?: return null
        return year.takeIf { it in SUPPORTED_YEAR_RANGE }
    }

    /**
     * Shifts selected date by deltaMonths, preserving day-of-month and clamping to target month's length.
     * e.g. 31/01/2026 + 1 month -> 28/02/2026.
     * e.g. 25/08/2026 + 1 month -> 25/09/2026.
     */
    fun shiftMonthPreservingDay(selectedDate: LocalDate, deltaMonths: Long): LocalDate {
        val currentYearMonth = YearMonth.from(selectedDate)
        val targetYearMonth = currentYearMonth.plusMonths(deltaMonths)
        return jumpToYearMonthPreservingDay(selectedDate, targetYearMonth)
    }

    /**
     * Jumps to targetYearMonth while preserving selectedDate's day-of-month, clamped to target month's length.
     * e.g. 25/08/2026 -> Dec 2030 -> 25/12/2030.
     * e.g. 31/01/2028 -> Feb 2028 -> 29/02/2028 (leap year).
     */
    fun jumpToYearMonthPreservingDay(selectedDate: LocalDate, targetYearMonth: YearMonth): LocalDate {
        val clampedYear = targetYearMonth.year.coerceIn(SUPPORTED_YEAR_RANGE)
        val safeYearMonth = YearMonth.of(clampedYear, targetYearMonth.monthValue)
        val clampedDay = minOf(selectedDate.dayOfMonth, safeYearMonth.lengthOfMonth())
        return safeYearMonth.atDay(clampedDay)
    }

    /**
     * Resolves date selection, returning the target date and its enclosing YearMonth.
     */
    fun resolveDateSelection(targetDate: LocalDate): Pair<LocalDate, YearMonth> {
        val clampedYear = targetDate.year.coerceIn(SUPPORTED_YEAR_RANGE)
        val safeDate = if (clampedYear != targetDate.year) {
            targetDate.withYear(clampedYear)
        } else {
            targetDate
        }
        return safeDate to YearMonth.from(safeDate)
    }

    /**
     * Resolves reset to exact today.
     */
    fun resolveToday(today: LocalDate = LocalDate.now()): Pair<LocalDate, YearMonth> {
        return resolveDateSelection(today)
    }
}
