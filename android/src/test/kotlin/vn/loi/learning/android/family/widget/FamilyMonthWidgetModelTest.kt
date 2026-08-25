package vn.loi.learning.android.family.widget

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test
import vn.loi.learning.android.family.*

class FamilyMonthWidgetModelTest {

    private val calendar = AstronomicalVietnameseLunarCalendar()
    private val projectionService = CalendarProjectionService(calendar)

    @Test
    fun `month date grid generates exactly 42 cells starting on Monday`() {
        val testNow = LocalDateTime.of(2026, 8, 25, 10, 0)
        val snapshot = FamilyLocalSnapshot()

        val state = FamilyMonthWidgetModelBuilder.build(
            snapshot = snapshot,
            calendar = calendar,
            projectionService = projectionService,
            now = testNow,
            targetYearMonth = YearMonth.of(2026, 8)
        )

        assertEquals("Tháng 8/2026", state.monthHeaderTitle)
        assertEquals(42, state.cells.size)

        // August 1, 2026 is Saturday -> Start Monday is July 27, 2026
        val firstCell = state.cells[0]
        assertEquals(LocalDate.of(2026, 7, 27), firstCell.date)
        assertEquals("27", firstCell.solarDayText)
        assertFalse(firstCell.isCurrentMonth)

        // Cell index 5 should be August 1 (Saturday)
        val aug1Cell = state.cells[5]
        assertEquals(LocalDate.of(2026, 8, 1), aug1Cell.date)
        assertEquals("1", aug1Cell.solarDayText)
        assertTrue(aug1Cell.isCurrentMonth)

        // Cell index 29 should be August 25 (Today)
        val todayCell = state.cells[29]
        assertEquals(LocalDate.of(2026, 8, 25), todayCell.date)
        assertTrue(todayCell.isToday)
        assertTrue(todayCell.isCurrentMonth)

        // Last cell (index 41) is Sept 6, 2026
        val lastCell = state.cells[41]
        assertEquals(LocalDate.of(2026, 9, 6), lastCell.date)
        assertFalse(lastCell.isCurrentMonth)
    }

    @Test
    fun `lunar date formats mùng 1 as day slash month and others as day number`() {
        val testNow = LocalDateTime.of(2026, 8, 25, 10, 0)
        val snapshot = FamilyLocalSnapshot()

        val state = FamilyMonthWidgetModelBuilder.build(
            snapshot = snapshot,
            calendar = calendar,
            projectionService = projectionService,
            now = testNow,
            targetYearMonth = YearMonth.of(2026, 8)
        )

        // On August 13, 2026 -> Lunar date is 1/7 (Mùng 1 tháng 7)
        val aug13Cell = state.cells[17]
        assertEquals(LocalDate.of(2026, 8, 13), aug13Cell.date)
        assertEquals("1/7", aug13Cell.lunarDayText)

        // Aug 25 -> Lunar date is 13/7 -> lunarDayText is "13"
        val aug25Cell = state.cells[29]
        assertEquals("13", aug25Cell.lunarDayText)
    }

    @Test
    fun `occupied days show correct markers for birthday event and pending task`() {
        val testNow = LocalDateTime.of(2026, 8, 25, 10, 0)

        val person = Person(
            id = "p1",
            fullName = "Nguyễn Văn A",
            birthDateSolar = LocalDate.of(1990, 8, 15),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val event = ImportantEvent(
            id = "e1",
            categoryId = "cat_anniversary",
            title = "Kỷ niệm ngày cưới",
            calendarType = CalendarType.SOLAR,
            solarDate = LocalDate.of(2026, 8, 20),
            recurrence = RecurrenceType.YEARLY,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val activeTask = Task(
            id = "t1",
            title = "Mua quà sinh nhật",
            recurrence = RecurrenceType.NONE,
            dueAt = LocalDateTime.of(2026, 8, 25, 15, 0),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val snapshot = FamilyLocalSnapshot(
            persons = listOf(person),
            events = listOf(event),
            tasks = listOf(activeTask)
        )

        val state = FamilyMonthWidgetModelBuilder.build(
            snapshot = snapshot,
            calendar = calendar,
            projectionService = projectionService,
            now = testNow,
            targetYearMonth = YearMonth.of(2026, 8)
        )

        // 1. Aug 15: Birthday occupied marker
        val bdayCell = state.cells.first { it.date == LocalDate.of(2026, 8, 15) }
        assertTrue(bdayCell.hasBirthday)
        assertTrue(bdayCell.isOccupied)
        assertEquals("●", bdayCell.markerText)

        // 2. Aug 20: Event occupied marker
        val eventCell = state.cells.first { it.date == LocalDate.of(2026, 8, 20) }
        assertTrue(eventCell.hasEvent)
        assertTrue(eventCell.isOccupied)
        assertEquals("●", eventCell.markerText)

        // 3. Aug 25: Pending Task occupied marker (TODAY + OCCUPIED coexist)
        val taskCell = state.cells.first { it.date == LocalDate.of(2026, 8, 25) }
        assertTrue(taskCell.hasTask)
        assertTrue(taskCell.isToday)
        assertTrue(taskCell.isOccupied)
        assertEquals("●", taskCell.markerText)

        // 4. Empty Day (Aug 10): No marker
        val emptyCell = state.cells.first { it.date == LocalDate.of(2026, 8, 10) }
        assertFalse(emptyCell.isOccupied)
        assertEquals("", emptyCell.markerText)
    }

    @Test
    fun `tombstoned and completed recurring tasks are excluded from occupied markers`() {
        val testNow = LocalDateTime.of(2026, 8, 25, 10, 0)

        val deletedPerson = Person(
            id = "p_del",
            fullName = "Người Đã Xóa",
            birthDateSolar = LocalDate.of(1990, 8, 10),
            deletedAtEpochMillis = 5000L,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 5000L
        )

        val recurringTask = Task(
            id = "t_recurring",
            title = "Bảo trì máy lạnh",
            recurrence = RecurrenceType.MONTHLY,
            dueAt = LocalDateTime.of(2026, 8, 25, 9, 0),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        // Mark Aug 25 completed
        val completion = TaskOccurrenceCompletion(
            id = "comp_aug",
            taskId = "t_recurring",
            occurrenceDateTime = LocalDateTime.of(2026, 8, 25, 9, 0),
            completedAt = LocalDateTime.of(2026, 8, 25, 9, 30),
            createdAtEpochMillis = 2000L,
            updatedAtEpochMillis = 2000L
        )

        val snapshot = FamilyLocalSnapshot(
            persons = listOf(deletedPerson),
            tasks = listOf(recurringTask),
            taskOccurrenceCompletions = listOf(completion)
        )

        val state = FamilyMonthWidgetModelBuilder.build(
            snapshot = snapshot,
            calendar = calendar,
            projectionService = projectionService,
            now = testNow,
            targetYearMonth = YearMonth.of(2026, 8)
        )

        // 6. Aug 10: Soft-deleted person is excluded
        val aug10Cell = state.cells.first { it.date == LocalDate.of(2026, 8, 10) }
        assertFalse(aug10Cell.isOccupied)

        // 5. Aug 25: Completed recurring task occurrence is not pending
        val aug25Cell = state.cells.first { it.date == LocalDate.of(2026, 8, 25) }
        assertFalse(aug25Cell.hasTask)
        assertFalse(aug25Cell.isOccupied)
    }

    @Test
    fun `mixed occurrences on same date produce bounded dots summary`() {
        val testNow = LocalDateTime.of(2026, 8, 25, 10, 0)

        val person = Person(
            id = "p1",
            fullName = "Bé Bi",
            birthDateSolar = LocalDate.of(2020, 8, 18),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val event = ImportantEvent(
            id = "e1",
            categoryId = "cat_anniversary",
            title = "Tiệc mừng",
            calendarType = CalendarType.SOLAR,
            solarDate = LocalDate.of(2026, 8, 18),
            recurrence = RecurrenceType.NONE,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val task = Task(
            id = "t1",
            title = "Mua bánh kem",
            recurrence = RecurrenceType.NONE,
            dueAt = LocalDateTime.of(2026, 8, 18, 14, 0),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val snapshot = FamilyLocalSnapshot(
            persons = listOf(person),
            events = listOf(event),
            tasks = listOf(task)
        )

        val state = FamilyMonthWidgetModelBuilder.build(
            snapshot = snapshot,
            calendar = calendar,
            projectionService = projectionService,
            now = testNow,
            targetYearMonth = YearMonth.of(2026, 8)
        )

        // Aug 18: Birthday + Event + Task -> 3 dots (bounded max)
        val aug18Cell = state.cells.first { it.date == LocalDate.of(2026, 8, 18) }
        assertTrue(aug18Cell.hasBirthday)
        assertTrue(aug18Cell.hasEvent)
        assertTrue(aug18Cell.hasTask)
        assertTrue(aug18Cell.isOccupied)
        assertEquals("● ● ●", aug18Cell.markerText)
    }

    @Test
    fun `recurring lunar event projects onto correct solar date in month grid`() {
        val testNow = LocalDateTime.of(2026, 8, 25, 10, 0)

        // Giỗ Ông Ngoại: 15/7 Âm lịch (Rằm tháng 7)
        val lunarEvent = ImportantEvent(
            id = "e_lunar",
            categoryId = "cat_memorial",
            title = "Giỗ Ông Ngoại",
            calendarType = CalendarType.LUNAR,
            lunarDay = 15,
            lunarMonth = 7,
            recurrence = RecurrenceType.YEARLY,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val snapshot = FamilyLocalSnapshot(
            events = listOf(lunarEvent)
        )

        val state = FamilyMonthWidgetModelBuilder.build(
            snapshot = snapshot,
            calendar = calendar,
            projectionService = projectionService,
            now = testNow,
            targetYearMonth = YearMonth.of(2026, 8)
        )

        // 15/7 Âm năm 2026 falls on August 27, 2026 (Solar)
        val aug27Cell = state.cells.first { it.date == LocalDate.of(2026, 8, 27) }
        assertTrue(aug27Cell.hasEvent)
        assertTrue(aug27Cell.isOccupied)
        assertEquals("●", aug27Cell.markerText)
    }

    @Test
    fun `previous month calculation across year boundary Jan 2027 to Dec 2026`() {
        val currentMonth = YearMonth.of(2027, 1)
        val prevMonth = currentMonth.minusMonths(1)
        assertEquals(YearMonth.of(2026, 12), prevMonth)
        assertEquals(2026, prevMonth.year)
        assertEquals(12, prevMonth.monthValue)

        val state = FamilyMonthWidgetModelBuilder.build(
            snapshot = FamilyLocalSnapshot(),
            calendar = calendar,
            projectionService = projectionService,
            now = LocalDateTime.of(2027, 1, 15, 10, 0),
            targetYearMonth = prevMonth
        )
        assertEquals("Tháng 12/2026", state.monthHeaderTitle)
        assertEquals(LocalDate.of(2026, 12, 1), state.yearMonth.atDay(1))
    }

    @Test
    fun `next month calculation across year boundary Dec 2026 to Jan 2027`() {
        val currentMonth = YearMonth.of(2026, 12)
        val nextMonth = currentMonth.plusMonths(1)
        assertEquals(YearMonth.of(2027, 1), nextMonth)
        assertEquals(2027, nextMonth.year)
        assertEquals(1, nextMonth.monthValue)

        val state = FamilyMonthWidgetModelBuilder.build(
            snapshot = FamilyLocalSnapshot(),
            calendar = calendar,
            projectionService = projectionService,
            now = LocalDateTime.of(2026, 12, 15, 10, 0),
            targetYearMonth = nextMonth
        )
        assertEquals("Tháng 1/2027", state.monthHeaderTitle)
        assertEquals(LocalDate.of(2027, 1, 1), state.yearMonth.atDay(1))
    }

    @Test
    fun `clicked date LocalDate remains exact when browsing September 2026`() {
        val sep2026 = YearMonth.of(2026, 9)
        val event = ImportantEvent(
            id = "e_sep",
            categoryId = "cat_memorial",
            title = "Ông Ngoại",
            calendarType = CalendarType.SOLAR,
            solarDate = LocalDate.of(2026, 9, 5),
            recurrence = RecurrenceType.NONE,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val state = FamilyMonthWidgetModelBuilder.build(
            snapshot = FamilyLocalSnapshot(events = listOf(event)),
            calendar = calendar,
            projectionService = projectionService,
            now = LocalDateTime.of(2026, 8, 25, 10, 0),
            targetYearMonth = sep2026
        )

        assertEquals("Tháng 9/2026", state.monthHeaderTitle)
        val sep5Cell = state.cells.first { it.date == LocalDate.of(2026, 9, 5) }
        assertEquals(LocalDate.of(2026, 9, 5), sep5Cell.date)
        assertTrue(sep5Cell.isCurrentMonth)
        assertTrue(sep5Cell.hasEvent)
        assertTrue(sep5Cell.isOccupied)
        assertEquals("●", sep5Cell.markerText)
    }
}
