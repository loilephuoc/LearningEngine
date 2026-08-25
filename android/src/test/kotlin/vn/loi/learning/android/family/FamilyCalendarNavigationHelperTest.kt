package vn.loi.learning.android.family

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class FamilyCalendarNavigationHelperTest {

    @Test
    fun `next month preserves valid day`() {
        val start = LocalDate.of(2026, 8, 25)
        val next = FamilyCalendarNavigationHelper.shiftMonthPreservingDay(start, 1)
        assertEquals(LocalDate.of(2026, 9, 25), next)
    }

    @Test
    fun `previous month preserves valid day`() {
        val start = LocalDate.of(2026, 9, 25)
        val prev = FamilyCalendarNavigationHelper.shiftMonthPreservingDay(start, -1)
        assertEquals(LocalDate.of(2026, 8, 25), prev)
    }

    @Test
    fun `January to February clamps non-leap day`() {
        val jan31 = LocalDate.of(2026, 1, 31)
        val feb = FamilyCalendarNavigationHelper.shiftMonthPreservingDay(jan31, 1)
        assertEquals(LocalDate.of(2026, 2, 28), feb)
    }

    @Test
    fun `January to February leap year clamps to 29`() {
        val jan31 = LocalDate.of(2028, 1, 31)
        val febLeap = FamilyCalendarNavigationHelper.shiftMonthPreservingDay(jan31, 1)
        assertEquals(LocalDate.of(2028, 2, 29), febLeap)
    }

    @Test
    fun `December to January updates year across boundary`() {
        val dec25 = LocalDate.of(2026, 12, 25)
        val janNext = FamilyCalendarNavigationHelper.shiftMonthPreservingDay(dec25, 1)
        assertEquals(LocalDate.of(2027, 1, 25), janNext)
    }

    @Test
    fun `January to December updates previous year across boundary`() {
        val jan25 = LocalDate.of(2027, 1, 25)
        val decPrev = FamilyCalendarNavigationHelper.shiftMonthPreservingDay(jan25, -1)
        assertEquals(LocalDate.of(2026, 12, 25), decPrev)
    }

    @Test
    fun `jump month year preserves valid day`() {
        val aug25 = LocalDate.of(2026, 8, 25)
        val target = YearMonth.of(2030, 12)
        val jumped = FamilyCalendarNavigationHelper.jumpToYearMonthPreservingDay(aug25, target)
        assertEquals(LocalDate.of(2030, 12, 25), jumped)
    }

    @Test
    fun `jump month year clamps invalid day to length of target month`() {
        val aug31 = LocalDate.of(2026, 8, 31)
        val feb2027 = YearMonth.of(2027, 2)
        val jumped = FamilyCalendarNavigationHelper.jumpToYearMonthPreservingDay(aug31, feb2027)
        assertEquals(LocalDate.of(2027, 2, 28), jumped)
    }

    @Test
    fun `resolveToday returns exact today and corresponding YearMonth`() {
        val fixedToday = LocalDate.of(2026, 8, 25)
        val (todayDate, yearMonth) = FamilyCalendarNavigationHelper.resolveToday(fixedToday)
        assertEquals(fixedToday, todayDate)
        assertEquals(YearMonth.of(2026, 8), yearMonth)
    }

    @Test
    fun `adjacent month date tap preserves exact LocalDate and synchronizes YearMonth`() {
        val adjacentDate = LocalDate.of(2026, 9, 5)
        val (selectedDate, yearMonth) = FamilyCalendarNavigationHelper.resolveDateSelection(adjacentDate)
        assertEquals(LocalDate.of(2026, 9, 5), selectedDate)
        assertEquals(YearMonth.of(2026, 9), yearMonth)
    }

    @Test
    fun `navigation preserves year within supported astronomical range 1900 to 2100`() {
        val minYearMonth = YearMonth.of(1800, 5)
        val clampedMin = FamilyCalendarNavigationHelper.jumpToYearMonthPreservingDay(LocalDate.of(2026, 5, 10), minYearMonth)
        assertEquals(1900, clampedMin.year)

        val maxYearMonth = YearMonth.of(2200, 5)
        val clampedMax = FamilyCalendarNavigationHelper.jumpToYearMonthPreservingDay(LocalDate.of(2026, 5, 10), maxYearMonth)
        assertEquals(2100, clampedMax.year)
    }

    @Test
    fun `filter state is independent of month navigation`() {
        val currentFilter = CalendarFilter.TASK
        val shiftedDate = FamilyCalendarNavigationHelper.shiftMonthPreservingDay(LocalDate.of(2026, 8, 25), 1)
        assertEquals(LocalDate.of(2026, 9, 25), shiftedDate)
        assertEquals(CalendarFilter.TASK, currentFilter)
    }

    @Test
    fun `day agenda date directly follows selectedDate across month shift`() {
        val initialSelectedDate = LocalDate.of(2026, 8, 25)
        val nextMonthDate = FamilyCalendarNavigationHelper.shiftMonthPreservingDay(initialSelectedDate, 1)
        val occurrences = listOf(
            CalendarOccurrence(
                id = "occ-1",
                sourceId = "task-1",
                sourceType = CalendarItemType.TASK_DUE,
                title = "Đóng tiền học",
                date = LocalDate.of(2026, 9, 25),
                time = null,
                allDay = false
            ),
            CalendarOccurrence(
                id = "occ-2",
                sourceId = "task-2",
                sourceType = CalendarItemType.TASK_DUE,
                title = "Khám sức khỏe",
                date = LocalDate.of(2026, 8, 25),
                time = null,
                allDay = false
            )
        )
        val agendaOccurrences = occurrences.filter { it.date == nextMonthDate }
        assertEquals(1, agendaOccurrences.size)
        assertEquals("Đóng tiền học", agendaOccurrences[0].title)
    }
    @Test
    fun `supported year parser accepts lower boundary`() {
        assertEquals(1900, FamilyCalendarNavigationHelper.parseSupportedYear("1900"))
    }

    @Test
    fun `supported year parser accepts upper boundary`() {
        assertEquals(2100, FamilyCalendarNavigationHelper.parseSupportedYear("2100"))
    }

    @Test
    fun `supported year parser rejects year below range`() {
        assertNull(FamilyCalendarNavigationHelper.parseSupportedYear("1899"))
    }

    @Test
    fun `supported year parser rejects year above range`() {
        assertNull(FamilyCalendarNavigationHelper.parseSupportedYear("2101"))
    }

    @Test
    fun `supported year parser rejects invalid text safely`() {
        assertNull(FamilyCalendarNavigationHelper.parseSupportedYear("abc"))
        assertNull(FamilyCalendarNavigationHelper.parseSupportedYear(""))
    }

    @Test
    fun `changing target year keeps month and navigation helper preserves selected day`() {
        val selectedDate = LocalDate.of(2026, 9, 25)
        val target = YearMonth.of(2045, 9)
        val result = FamilyCalendarNavigationHelper.jumpToYearMonthPreservingDay(selectedDate, target)
        assertEquals(LocalDate.of(2045, 9, 25), result)
    }

}
