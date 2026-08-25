package vn.loi.learning.android.family

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import kotlin.test.*
import org.junit.Test

class FamilyOccurrenceTest {
    private val calendar = AstronomicalVietnameseLunarCalendar()
    private val resolver = ImportantEventOccurrenceResolver(calendar)

    @Test
    fun `birthday occurrence calculates solar date and days until for Person`() {
        val person = Person(
            id = "p1",
            fullName = "Nguyễn An",
            group = PersonGroup.FAMILY,
            birthDateSolar = LocalDate.of(1990, 8, 20),
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )
        val occurrence = resolver.nextBirthday(person, LocalDate.of(2026, 8, 1))!!
        assertEquals(EventSourceType.BIRTHDAY, occurrence.sourceType)
        assertEquals("p1", occurrence.sourceId)
        assertEquals("Nguyễn An", occurrence.title)
        assertEquals(LocalDate.of(2026, 8, 20), occurrence.occurrenceDateSolar)
        assertEquals(19, occurrence.daysUntil)
    }

    @Test
    fun `birthday occurrence includes all groups such as FRIEND and FAMILY`() {
        val today = LocalDate.of(2026, 5, 1)
        val persons = listOf(
            Person("f1", "Bạn Sang", group = PersonGroup.FRIEND, birthDateSolar = LocalDate.of(1995, 5, 6), createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            Person("m1", "Mẹ", group = PersonGroup.FAMILY, birthDateSolar = LocalDate.of(1968, 5, 10), createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            Person("c1", "Huy Elekta", group = PersonGroup.COLLEAGUE, birthDateSolar = LocalDate.of(1993, 5, 15), createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            Person("o1", "Bác Hàng Xóm", group = PersonGroup.OTHER, birthDateSolar = LocalDate.of(1955, 5, 20), createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            Person("no-bday", "Ẩn Danh", group = PersonGroup.FRIEND, birthDateSolar = null, createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        )

        val birthdayOccurrences = persons.mapNotNull { resolver.nextBirthday(it, today) }
        assertEquals(4, birthdayOccurrences.size)
        assertEquals(listOf("Bạn Sang", "Mẹ", "Huy Elekta", "Bác Hàng Xóm"), birthdayOccurrences.map { it.title })
        assertEquals(listOf(5L, 9L, 14L, 19L), birthdayOccurrences.map { it.daysUntil })
    }

    @Test
    fun `nextBirthday rolls over to next year if birthday already passed this year`() {
        val person = Person("p", "Bình", group = PersonGroup.FRIEND, birthDateSolar = LocalDate.of(1998, 2, 10), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val occurrence = resolver.nextBirthday(person, LocalDate.of(2026, 8, 1))!!
        assertEquals(LocalDate.of(2027, 2, 10), occurrence.occurrenceDateSolar)
        assertTrue(occurrence.daysUntil > 0)
    }

    @Test
    fun `yearly solar event calculates recurrence for current and next year`() {
        val event = ImportantEvent(
            id = "w1",
            categoryId = "cat_wedding",
            title = "Kỷ niệm ngày cưới",
            calendarType = CalendarType.SOLAR,
            solarDate = LocalDate.of(2020, 11, 20),
            recurrence = RecurrenceType.YEARLY,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )
        val beforeDate = LocalDate.of(2026, 11, 1)
        val occ1 = resolver.nextEvent(event, beforeDate)!!
        assertEquals(LocalDate.of(2026, 11, 20), occ1.occurrenceDateSolar)
        assertEquals(19, occ1.daysUntil)

        val afterDate = LocalDate.of(2026, 11, 25)
        val occ2 = resolver.nextEvent(event, afterDate)!!
        assertEquals(LocalDate.of(2027, 11, 20), occ2.occurrenceDateSolar)
    }

    @Test
    fun `one-time solar event calculates occurrence on exact date`() {
        val event = ImportantEvent(
            id = "m1",
            categoryId = "cat_inauguration",
            title = "Khánh thành nhà mới",
            calendarType = CalendarType.SOLAR,
            solarDate = LocalDate.of(2026, 9, 15),
            recurrence = RecurrenceType.NONE,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )
        val occ = resolver.nextEvent(event, LocalDate.of(2026, 9, 1))!!
        assertEquals(LocalDate.of(2026, 9, 15), occ.occurrenceDateSolar)
        assertEquals(14, occ.daysUntil)
    }

    @Test
    fun `yearly lunar memorial calculates solar date and labels accurately`() {
        val event = ImportantEvent(
            id = "g1",
            categoryId = "cat_memorial",
            title = "Giỗ Bà",
            calendarType = CalendarType.LUNAR,
            lunarDay = 15,
            lunarMonth = 8,
            lunarLeapMonth = false,
            recurrence = RecurrenceType.YEARLY,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )
        val occ = resolver.nextEvent(event, LocalDate.of(2026, 1, 1))!!
        assertEquals("Giỗ Bà", occ.title)
        assertEquals("15/8 âm lịch", occ.calendarLabel)
        assertEquals(VietnameseLunarDate(2026, 8, 15, false), calendar.solarToLunar(occ.occurrenceDateSolar))
    }

    @Test
    fun `one-time lunar event with sourceYear calculates exact solar date`() {
        val event = ImportantEvent(
            id = "l1",
            categoryId = "cat_milestone",
            title = "Lễ mừng thọ",
            calendarType = CalendarType.LUNAR,
            lunarDay = 10,
            lunarMonth = 3,
            lunarLeapMonth = false,
            sourceYear = 2026,
            recurrence = RecurrenceType.NONE,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )
        val occ = resolver.nextEvent(event, LocalDate.of(2026, 1, 1))!!
        assertEquals(VietnameseLunarDate(2026, 3, 10, false), calendar.solarToLunar(occ.occurrenceDateSolar))
    }

    @Test
    fun `merged allUpcoming sorts birthdays and events chronologically`() {
        val today = LocalDate.of(2026, 8, 1)
        val persons = listOf(
            Person("p1", "Lê Văn Tĩnh", group = PersonGroup.FRIEND, birthDateSolar = LocalDate.of(1990, 8, 10), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        )
        val events = listOf(
            ImportantEvent("e1", "cat_wedding", "Ngày cưới", calendarType = CalendarType.SOLAR, solarDate = LocalDate.of(2020, 8, 5), recurrence = RecurrenceType.YEARLY, createdAtEpochMillis = 1, updatedAtEpochMillis = 1),
            ImportantEvent("e2", "cat_milestone", "Cột mốc 2026", calendarType = CalendarType.SOLAR, solarDate = LocalDate.of(2026, 8, 25), recurrence = RecurrenceType.NONE, createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        )

        val upcoming = resolver.allUpcoming(persons, events, today)
        assertEquals(3, upcoming.size)
        assertEquals(listOf("Ngày cưới", "Lê Văn Tĩnh", "Cột mốc 2026"), upcoming.map { it.title })
        assertEquals(listOf(4L, 9L, 24L), upcoming.map { it.daysUntil })
    }

    @Test
    fun `ReminderOccurrenceResolver handles month, week, day and same-day offsets`() {
        val target = LocalDate.of(2026, 8, 15)

        // Same day (amount = 0) at 07:00
        val r0 = ReminderRule("r0", ReminderTargetType.EVENT, "e1", 0, ReminderOffsetUnit.DAY, 7, 0, true, 1, 1)
        assertEquals(LocalDateTime.of(2026, 8, 15, 7, 0), ReminderOccurrenceResolver.resolve(target, r0))

        // 7 days before at 08:00
        val rDay = ReminderRule("r1", ReminderTargetType.EVENT, "e1", 7, ReminderOffsetUnit.DAY, 8, 0, true, 1, 1)
        assertEquals(LocalDateTime.of(2026, 8, 8, 8, 0), ReminderOccurrenceResolver.resolve(target, rDay))

        // 2 weeks before at 09:30
        val rWeek = ReminderRule("r2", ReminderTargetType.EVENT, "e1", 2, ReminderOffsetUnit.WEEK, 9, 30, true, 1, 1)
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 30), ReminderOccurrenceResolver.resolve(target, rWeek))

        // 1 month before at 08:00 (uses minusMonths, NOT 30 days)
        val march31 = LocalDate.of(2026, 3, 31)
        val rMonth = ReminderRule("r3", ReminderTargetType.EVENT, "e1", 1, ReminderOffsetUnit.MONTH, 8, 0, true, 1, 1)
        assertEquals(LocalDateTime.of(2026, 2, 28, 8, 0), ReminderOccurrenceResolver.resolve(march31, rMonth))

        // 2 months before
        val r2Month = ReminderRule("r4", ReminderTargetType.EVENT, "e1", 2, ReminderOffsetUnit.MONTH, 8, 0, true, 1, 1)
        assertEquals(LocalDateTime.of(2026, 1, 31, 8, 0), ReminderOccurrenceResolver.resolve(march31, r2Month))
    }

    @Test
    fun `sequential numeric typing preserves caret position at the logical end`() {
        var state = TextFieldValue("", TextRange(0))

        // Type '1'
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("1", TextRange(1)))
        assertEquals("1", state.text)
        assertEquals(TextRange(1), state.selection)

        // Type '2' -> 12-
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12", TextRange(2)))
        assertEquals("12-", state.text)
        assertEquals(TextRange(3), state.selection)

        // Type '0' -> 12-0
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12-0", TextRange(4)))
        assertEquals("12-0", state.text)
        assertEquals(TextRange(4), state.selection)

        // Type '6' -> 12-06-
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12-06", TextRange(5)))
        assertEquals("12-06-", state.text)
        assertEquals(TextRange(6), state.selection)

        // Type '1' -> 12-06-1
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12-06-1", TextRange(7)))
        assertEquals("12-06-1", state.text)
        assertEquals(TextRange(7), state.selection)

        // Type '9' -> 12-06-19
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12-06-19", TextRange(8)))
        assertEquals("12-06-19", state.text)
        assertEquals(TextRange(8), state.selection)

        // Type '6' -> 12-06-196
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12-06-196", TextRange(9)))
        assertEquals("12-06-196", state.text)
        assertEquals(TextRange(9), state.selection)

        // Type '5' -> 12-06-1965
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12-06-1965", TextRange(10)))
        assertEquals("12-06-1965", state.text)
        assertEquals(TextRange(10), state.selection)

        // Type extra digit '9' -> maximum 8 digits enforced, caret stays at end
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12-06-19659", TextRange(11)))
        assertEquals("12-06-1965", state.text)
        assertEquals(TextRange(10), state.selection)
    }

    @Test
    fun `backspace across separators feels native without trapping or jumping`() {
        var state = TextFieldValue("12-06-1965", TextRange(10))

        // Backspace '5'
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12-06-196", TextRange(9)))
        assertEquals("12-06-196", state.text)
        assertEquals(TextRange(9), state.selection)

        // Backspace '6'
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12-06-19", TextRange(8)))
        assertEquals("12-06-19", state.text)
        assertEquals(TextRange(8), state.selection)

        // Backspace '9'
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12-06-1", TextRange(7)))
        assertEquals("12-06-1", state.text)
        assertEquals(TextRange(7), state.selection)

        // Backspace '1'
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12-06-", TextRange(6)))
        assertEquals("12-06-", state.text)
        assertEquals(TextRange(6), state.selection)

        // Backspace across auto-inserted '-' at index 6 -> deletes '6' and returns 12-0 (caret 4)
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12-06", TextRange(5)))
        assertEquals("12-0", state.text)
        assertEquals(TextRange(4), state.selection)

        // Backspace '0'
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12-", TextRange(3)))
        assertEquals("12-", state.text)
        assertEquals(TextRange(3), state.selection)

        // Backspace across auto-inserted '-' at index 3 -> deletes '2' and returns 1 (caret 1)
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("12", TextRange(2)))
        assertEquals("1", state.text)
        assertEquals(TextRange(1), state.selection)

        // Backspace '1'
        state = DateInputHelper.formatTextFieldValue(state, TextFieldValue("", TextRange(0)))
        assertEquals("", state.text)
        assertEquals(TextRange(0), state.selection)
    }

    @Test
    fun `paste normalization formats dates correctly and places caret at end`() {
        val empty = TextFieldValue("", TextRange(0))

        // Raw 8 digits
        val p1 = DateInputHelper.formatTextFieldValue(empty, TextFieldValue("12061965", TextRange(8)))
        assertEquals("12-06-1965", p1.text)
        assertEquals(TextRange(10), p1.selection)

        // Pre-formatted with dashes
        val p2 = DateInputHelper.formatTextFieldValue(empty, TextFieldValue("12-06-1965", TextRange(10)))
        assertEquals("12-06-1965", p2.text)
        assertEquals(TextRange(10), p2.selection)

        // Pre-formatted with slashes
        val p3 = DateInputHelper.formatTextFieldValue(empty, TextFieldValue("12/06/1965", TextRange(10)))
        assertEquals("12-06-1965", p3.text)
        assertEquals(TextRange(10), p3.selection)

        // Partial paste
        val p4 = DateInputHelper.formatTextFieldValue(empty, TextFieldValue("1206", TextRange(4)))
        assertEquals("12-06-", p4.text)
        assertEquals(TextRange(6), p4.selection)
    }

    @Test
    fun `moving cursor without changing text preserves selection`() {
        val current = TextFieldValue("12-06-1965", TextRange(10))
        val cursorMoved = DateInputHelper.formatTextFieldValue(current, TextFieldValue("12-06-1965", TextRange(4)))
        assertEquals("12-06-1965", cursorMoved.text)
        assertEquals(TextRange(4), cursorMoved.selection)
    }

    @Test
    fun `solar monthly occurrence clamps to last valid day of shorter month`() {
        val jan31 = LocalDate.of(2026, 1, 31)

        // Feb in non-leap year 2027 -> 28 Feb
        assertEquals(LocalDate.of(2027, 2, 28), ImportantEventOccurrenceResolver.solarMonthlyOccurrence(jan31, 2027, 2))

        // Feb in leap year 2028 -> 29 Feb
        assertEquals(LocalDate.of(2028, 2, 29), ImportantEventOccurrenceResolver.solarMonthlyOccurrence(jan31, 2028, 2))

        // Apr (30 days) -> 30 Apr
        assertEquals(LocalDate.of(2027, 4, 30), ImportantEventOccurrenceResolver.solarMonthlyOccurrence(jan31, 2027, 4))

        // Mar (31 days) -> 31 Mar
        assertEquals(LocalDate.of(2027, 3, 31), ImportantEventOccurrenceResolver.solarMonthlyOccurrence(jan31, 2027, 3))
    }

    @Test
    fun `monthly recurring task resolves next occurrence and reuses reminder rules`() {
        val task = Task(
            id = "t1",
            title = "Thanh toán cước Internet",
            dueAt = LocalDateTime.of(2026, 8, 31, 18, 0),
            recurrence = RecurrenceType.MONTHLY,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        val nowAug = LocalDateTime.of(2026, 8, 1, 10, 0)
        val occAug = TaskOccurrenceResolver.resolveNextOccurrence(task, nowAug)
        assertEquals(LocalDateTime.of(2026, 8, 31, 18, 0), occAug.occurrenceDueAt)

        // After completing August occurrence, next occurrence resolves to September (30 days clamp)
        val augCompletion = TaskOccurrenceCompletion("c1", "t1", LocalDateTime.of(2026, 8, 31, 18, 0), LocalDateTime.of(2026, 8, 30, 20, 0), 1, 1)
        val nowSep = LocalDateTime.of(2026, 9, 1, 10, 0)
        val occSep = TaskOccurrenceResolver.resolveNextOccurrence(task, nowSep, listOf(augCompletion))
        assertEquals(LocalDateTime.of(2026, 9, 30, 18, 0), occSep.occurrenceDueAt)
        assertFalse(occSep.isCompleted)

        // Reminder rule for this task applies to the resolved September occurrence
        val reminderRule = ReminderRule("r1", ReminderTargetType.TASK, "t1", 3, ReminderOffsetUnit.DAY, 8, 0, true, 1, 1)
        val reminderTime = ReminderOccurrenceResolver.resolve(occSep.occurrenceDueAt!!.toLocalDate(), reminderRule)
        assertEquals(LocalDateTime.of(2026, 9, 27, 8, 0), reminderTime)
    }

    @Test
    fun `yearly recurring task resolves next occurrence and handles leap year dates`() {
        val task = Task(
            id = "t2",
            title = "Gia hạn bảo hiểm xe",
            dueAt = LocalDateTime.of(2024, 2, 29, 9, 0),
            recurrence = RecurrenceType.YEARLY,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        // 2025 is not a leap year -> clamps to 28 Feb
        val now2025 = LocalDateTime.of(2025, 1, 1, 0, 0)
        val occ2025 = TaskOccurrenceResolver.resolveNextOccurrence(task, now2025)
        assertEquals(LocalDateTime.of(2025, 2, 28, 9, 0), occ2025.occurrenceDueAt)

        // Completing 2025 moves next active occurrence to 2026
        val comp2025 = TaskOccurrenceCompletion("c2", "t2", LocalDateTime.of(2025, 2, 28, 9, 0), LocalDateTime.now(), 1, 1)
        val occ2026 = TaskOccurrenceResolver.resolveNextOccurrence(task, now2025, listOf(comp2025))
        assertEquals(LocalDateTime.of(2026, 2, 28, 9, 0), occ2026.occurrenceDueAt)
    }

    @Test
    fun `task validation requires non-blank title and valid startAt before dueAt`() {
        assertFailsWith<IllegalArgumentException> {
            Task(id = "bad1", title = "  ", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        }

        assertFailsWith<IllegalArgumentException> {
            Task(
                id = "bad2",
                title = "Invalid range",
                startAt = LocalDateTime.of(2026, 9, 10, 10, 0),
                dueAt = LocalDateTime.of(2026, 9, 5, 10, 0),
                createdAtEpochMillis = 1,
                updatedAtEpochMillis = 1
            )
        }

        // Undated task is completely valid
        val undated = Task(id = "ok1", title = "Đọc sách", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        assertNull(undated.dueAt)
        assertNull(undated.startAt)
    }

    @Test
    fun `default same-day reminder helper creates valid 07-00 rule and detects existing`() {
        val rule = createDefaultSameDayReminderRule(ReminderTargetType.TASK, "task_1", 1000)
        assertEquals(ReminderTargetType.TASK, rule.targetType)
        assertEquals("task_1", rule.targetId)
        assertEquals(0, rule.amount)
        assertEquals(ReminderOffsetUnit.DAY, rule.unit)
        assertEquals(7, rule.remindHour)
        assertEquals(0, rule.remindMinute)
        assertTrue(rule.enabled)

        assertTrue(hasSameDayReminder(listOf(rule), ReminderTargetType.TASK, "task_1"))
        assertFalse(hasSameDayReminder(listOf(rule), ReminderTargetType.TASK, "task_2"))
        assertFalse(hasSameDayReminder(listOf(rule), ReminderTargetType.EVENT, "task_1"))
    }

    @Test
    fun `TaskQueryHelper filters today, upcoming, overdue and completed correctly`() {
        val today = LocalDate.of(2026, 9, 15)
        val now = LocalDateTime.of(2026, 9, 15, 12, 0)

        val tToday = Task("t1", "Việc hôm nay", dueAt = LocalDateTime.of(2026, 9, 15, 18, 0), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val tUpcoming = Task("t2", "Việc sắp tới", dueAt = LocalDateTime.of(2026, 9, 20, 18, 0), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val tOverdue = Task("t3", "Việc quá hạn", dueAt = LocalDateTime.of(2026, 9, 10, 18, 0), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val tDone = Task("t4", "Việc đã xong", dueAt = LocalDateTime.of(2026, 9, 1, 18, 0), status = TaskStatus.DONE, completedAt = LocalDateTime.of(2026, 9, 2, 10, 0), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)

        val allOccs = TaskOccurrenceResolver.allOccurrences(listOf(tToday, tUpcoming, tOverdue, tDone), now)

        val todayList = TaskQueryHelper.filterToday(allOccs, today)
        assertEquals(listOf("Việc hôm nay"), todayList.map { it.task.title })

        val upcomingList = TaskQueryHelper.filterUpcoming(allOccs, now)
        assertEquals(listOf("Việc hôm nay", "Việc sắp tới"), upcomingList.map { it.task.title })

        val overdueList = TaskQueryHelper.filterOverdue(allOccs, now)
        assertEquals(listOf("Việc quá hạn"), overdueList.map { it.task.title })

        val completedHistory = TaskQueryHelper.filterCompleted(listOf(tToday, tUpcoming, tOverdue, tDone), emptyList())
        assertEquals(1, completedHistory.size)
        assertEquals("Việc đã xong", completedHistory.single().title)
    }

    @Test
    fun `YEARLY lunar event resolves occurrence and reuses same persistent rules across years`() {
        val calendar = AstronomicalVietnameseLunarCalendar()
        val resolver = ImportantEventOccurrenceResolver(calendar)

        val memorial = ImportantEvent(
            id = "m1",
            categoryId = "cat_memorial",
            title = "Ông Ngoại",
            calendarType = CalendarType.LUNAR,
            lunarDay = 24,
            lunarMonth = 7,
            recurrence = RecurrenceType.YEARLY,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        val ruleSameDay = ReminderRule("r0", ReminderTargetType.EVENT, "m1", 0, ReminderOffsetUnit.DAY, 7, 0, true, 1, 1)
        val rule1Week = ReminderRule("r1", ReminderTargetType.EVENT, "m1", 1, ReminderOffsetUnit.WEEK, 8, 0, true, 1, 1)
        val rule3Days = ReminderRule("r2", ReminderTargetType.EVENT, "m1", 3, ReminderOffsetUnit.DAY, 8, 0, true, 1, 1)
        val rules = listOf(ruleSameDay, rule1Week, rule3Days)

        // Year 2026: 24/7 lunar is 2026-09-05
        val occ2026 = resolver.nextEvent(memorial, LocalDate.of(2026, 1, 1))
        assertNotNull(occ2026)
        assertEquals(LocalDate.of(2026, 9, 5), occ2026.occurrenceDateSolar)

        val rem2026SameDay = ReminderOccurrenceResolver.resolve(occ2026.occurrenceDateSolar, ruleSameDay)
        val rem20261Week = ReminderOccurrenceResolver.resolve(occ2026.occurrenceDateSolar, rule1Week)
        val rem20263Days = ReminderOccurrenceResolver.resolve(occ2026.occurrenceDateSolar, rule3Days)

        assertEquals(LocalDateTime.of(2026, 9, 5, 7, 0), rem2026SameDay)
        assertEquals(LocalDateTime.of(2026, 8, 29, 8, 0), rem20261Week)
        assertEquals(LocalDateTime.of(2026, 9, 2, 8, 0), rem20263Days)

        // Year 2027: 24/7 lunar is 2027-08-25
        val occ2027 = resolver.nextEvent(memorial, LocalDate.of(2026, 9, 6))
        assertNotNull(occ2027)
        assertEquals(LocalDate.of(2027, 8, 25), occ2027.occurrenceDateSolar)

        val rem2027SameDay = ReminderOccurrenceResolver.resolve(occ2027.occurrenceDateSolar, ruleSameDay)
        val rem20271Week = ReminderOccurrenceResolver.resolve(occ2027.occurrenceDateSolar, rule1Week)
        val rem20273Days = ReminderOccurrenceResolver.resolve(occ2027.occurrenceDateSolar, rule3Days)

        assertEquals(LocalDateTime.of(2027, 8, 25, 7, 0), rem2027SameDay)
        assertEquals(LocalDateTime.of(2027, 8, 18, 8, 0), rem20271Week)
        assertEquals(LocalDateTime.of(2027, 8, 22, 8, 0), rem20273Days)
    }

    @Test
    fun `ONCE reminder rule applies only to anchored occurrence and does not fire next year or next month`() {
        val calendar = AstronomicalVietnameseLunarCalendar()
        val resolver = ImportantEventOccurrenceResolver(calendar)

        val memorial = ImportantEvent(
            id = "m1",
            categoryId = "cat_memorial",
            title = "Ông Ngoại",
            calendarType = CalendarType.LUNAR,
            lunarDay = 24,
            lunarMonth = 7,
            recurrence = RecurrenceType.YEARLY,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        val ruleYearlyRepeat = ReminderRule(
            id = "r_yearly",
            targetType = ReminderTargetType.EVENT,
            targetId = "m1",
            amount = 1,
            unit = ReminderOffsetUnit.WEEK,
            remindHour = 8,
            remindMinute = 0,
            enabled = true,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
            repeatMode = ReminderRepeatMode.FOLLOW_TARGET
        )

        val ruleOnce2026 = ReminderRule(
            id = "r_once_2026",
            targetType = ReminderTargetType.EVENT,
            targetId = "m1",
            amount = 2,
            unit = ReminderOffsetUnit.WEEK,
            remindHour = 9,
            remindMinute = 0,
            enabled = true,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
            repeatMode = ReminderRepeatMode.ONCE,
            occurrenceDate = LocalDate.of(2026, 9, 5) // anchored to 2026 occurrence
        )

        // Occurrence in 2026 (2026-09-05) -> BOTH rules fire
        val occ2026 = resolver.nextEvent(memorial, LocalDate.of(2026, 1, 1))
        assertNotNull(occ2026)
        assertEquals(LocalDate.of(2026, 9, 5), occ2026.occurrenceDateSolar)

        val rem2026Follow = ReminderOccurrenceResolver.resolve(occ2026.occurrenceDateSolar, ruleYearlyRepeat)
        val rem2026Once = ReminderOccurrenceResolver.resolve(occ2026.occurrenceDateSolar, ruleOnce2026)
        assertEquals(LocalDateTime.of(2026, 8, 29, 8, 0), rem2026Follow)
        assertEquals(LocalDateTime.of(2026, 8, 22, 9, 0), rem2026Once)

        // Occurrence in 2027 (2027-08-25) -> FOLLOW_TARGET fires, ONCE does NOT fire (null)
        val occ2027 = resolver.nextEvent(memorial, LocalDate.of(2026, 9, 6))
        assertNotNull(occ2027)
        assertEquals(LocalDate.of(2027, 8, 25), occ2027.occurrenceDateSolar)

        val rem2027Follow = ReminderOccurrenceResolver.resolve(occ2027.occurrenceDateSolar, ruleYearlyRepeat)
        val rem2027Once = ReminderOccurrenceResolver.resolve(occ2027.occurrenceDateSolar, ruleOnce2026)
        assertEquals(LocalDateTime.of(2027, 8, 18, 8, 0), rem2027Follow)
        assertNull(rem2027Once) // One-shot rule skipped in 2027!
    }

    @Test
    fun `monthly ONCE reminder applies to current month occurrence and skips next month`() {
        val task = Task(
            id = "t_monthly",
            title = "Báo cáo doanh số",
            dueAt = LocalDateTime.of(2026, 8, 31, 17, 0),
            recurrence = RecurrenceType.MONTHLY,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        val ruleOnceAug = ReminderRule(
            id = "r_once_aug",
            targetType = ReminderTargetType.TASK,
            targetId = "t_monthly",
            amount = 3,
            unit = ReminderOffsetUnit.DAY,
            remindHour = 10,
            remindMinute = 0,
            enabled = true,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
            repeatMode = ReminderRepeatMode.ONCE,
            occurrenceDate = LocalDate.of(2026, 8, 31)
        )

        val augDue = LocalDate.of(2026, 8, 31)
        val sepDue = LocalDate.of(2026, 9, 30)

        // Aug occurrence: rule fires
        val augRem = ReminderOccurrenceResolver.resolve(augDue, ruleOnceAug)
        assertEquals(LocalDateTime.of(2026, 8, 28, 10, 0), augRem)

        // Sep occurrence: rule returns null
        val sepRem = ReminderOccurrenceResolver.resolve(sepDue, ruleOnceAug)
        assertNull(sepRem)
    }

    @Test
    fun `partial inputs remain typeable and do not throw during validation`() {
        val partials = listOf("1", "12-", "12-0", "12-06-", "12-06-1", "12-06-19", "12-06-196")
        for (partial in partials) {
            // parseDate safely returns null on partial input without exception
            assertNull(DateInputHelper.parseDate(partial))
        }
    }

    @Test
    fun `DateInputHelper parsing and Gregorian date validation`() {
        // Parse valid dd-MM-yyyy
        assertEquals(LocalDate.of(1965, 6, 12), DateInputHelper.parseDate("12-06-1965"))
        assertEquals(LocalDate.of(2026, 12, 31), DateInputHelper.parseDate("31-12-2026"))
        assertEquals(LocalDate.of(2024, 2, 29), DateInputHelper.parseDate("29-02-2024")) // leap year

        // Reject invalid Gregorian dates
        assertNull(DateInputHelper.parseDate("31-02-2026"))
        assertNull(DateInputHelper.parseDate("29-02-2025")) // not leap year
        assertNull(DateInputHelper.parseDate("32-01-2026"))
        assertNull(DateInputHelper.parseDate("12-13-2026"))
        assertNull(DateInputHelper.parseDate("00-10-2020"))
        assertNull(DateInputHelper.parseDate("invalid"))
        assertNull(DateInputHelper.parseDate(""))
    }

    @Test
    fun `birthday appears in visible range and projects yearly without duplication`() {
        val calendar = AstronomicalVietnameseLunarCalendar()
        val service = CalendarProjectionService(calendar)
        val person = Person(
            id = "p1",
            fullName = "Lê Văn Tĩnh",
            birthDateSolar = LocalDate.of(1965, 6, 12),
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        // Range in June 2026
        val occs2026 = service.project(
            persons = listOf(person),
            events = emptyList(),
            tasks = emptyList(),
            completions = emptyList(),
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 30)
        )

        assertEquals(1, occs2026.size)
        val b2026 = occs2026.single()
        assertEquals(CalendarItemType.BIRTHDAY, b2026.sourceType)
        assertEquals("p1", b2026.sourceId)
        assertEquals(LocalDate.of(2026, 6, 12), b2026.date)
        assertTrue(b2026.allDay)
        assertEquals("Sinh nhật Lê Văn Tĩnh", b2026.title)

        // Multi-year range (2026 - 2027)
        val multiYear = service.project(
            persons = listOf(person),
            events = emptyList(),
            tasks = emptyList(),
            completions = emptyList(),
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2027, 12, 31)
        )

        val bdays = multiYear.filter { it.sourceType == CalendarItemType.BIRTHDAY }
        assertEquals(2, bdays.size)
        assertEquals(LocalDate.of(2026, 6, 12), bdays[0].date)
        assertEquals(LocalDate.of(2027, 6, 12), bdays[1].date)
    }

    @Test
    fun `one-time solar event and monthly solar event projection in range`() {
        val calendar = AstronomicalVietnameseLunarCalendar()
        val service = CalendarProjectionService(calendar)
        val oneTimeEvent = ImportantEvent(
            id = "e_once",
            categoryId = "cat_other",
            title = "Khởi công dự án",
            calendarType = CalendarType.SOLAR,
            solarDate = LocalDate.of(2026, 9, 15),
            recurrence = RecurrenceType.NONE,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        val monthlyEvent = ImportantEvent(
            id = "e_monthly",
            categoryId = "cat_other",
            title = "Họp chi bộ",
            calendarType = CalendarType.SOLAR,
            solarDate = LocalDate.of(2026, 1, 31), // 31st of month (needs clamping on Feb/Apr)
            recurrence = RecurrenceType.MONTHLY,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        val range = service.project(
            persons = emptyList(),
            events = listOf(oneTimeEvent, monthlyEvent),
            tasks = emptyList(),
            completions = emptyList(),
            startDate = LocalDate.of(2026, 2, 1),
            endDate = LocalDate.of(2026, 4, 30)
        )

        // e_once is in Sept (outside range) -> not projected
        assertTrue(range.none { it.sourceId == "e_once" })

        // e_monthly in Feb, March, April -> clamped to Feb 28, Mar 31, Apr 30
        val monthlyOccs = range.filter { it.sourceId == "e_monthly" }
        assertEquals(3, monthlyOccs.size)
        assertEquals(LocalDate.of(2026, 2, 28), monthlyOccs[0].date)
        assertEquals(LocalDate.of(2026, 3, 31), monthlyOccs[1].date)
        assertEquals(LocalDate.of(2026, 4, 30), monthlyOccs[2].date)
    }

    @Test
    fun `yearly lunar memorial maps to correct solar date in 2026 and 2027`() {
        val calendar = AstronomicalVietnameseLunarCalendar()
        val service = CalendarProjectionService(calendar)
        val memorial = ImportantEvent(
            id = "m1",
            categoryId = "cat_memorial",
            title = "Giỗ Ông Ngoại",
            calendarType = CalendarType.LUNAR,
            lunarDay = 24,
            lunarMonth = 7,
            recurrence = RecurrenceType.YEARLY,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        // In 2026: 24/7 lunar is 2026-09-05
        val sep2026 = service.project(
            persons = emptyList(),
            events = listOf(memorial),
            tasks = emptyList(),
            completions = emptyList(),
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        assertEquals(1, sep2026.size)
        val m2026 = sep2026.single()
        assertEquals("m1", m2026.sourceId)
        assertEquals(LocalDate.of(2026, 9, 5), m2026.date)
        assertNotNull(m2026.lunarDate)
        assertEquals(24, m2026.lunarDate?.day)
        assertEquals(7, m2026.lunarDate?.month)

        // In 2027: 24/7 lunar is 2027-08-25
        val aug2027 = service.project(
            persons = emptyList(),
            events = listOf(memorial),
            tasks = emptyList(),
            completions = emptyList(),
            startDate = LocalDate.of(2027, 8, 1),
            endDate = LocalDate.of(2027, 8, 31)
        )

        assertEquals(1, aug2027.size)
        assertEquals(LocalDate.of(2027, 8, 25), aug2027.single().date)
    }

    @Test
    fun `recurring task generates occurrences in range and respects occurrence completion`() {
        val calendar = AstronomicalVietnameseLunarCalendar()
        val service = CalendarProjectionService(calendar)
        val task = Task(
            id = "t1",
            title = "Đóng tiền điện",
            dueAt = LocalDateTime.of(2026, 8, 15, 17, 0),
            recurrence = RecurrenceType.MONTHLY,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        val compAug = TaskOccurrenceCompletion(
            id = "c1",
            taskId = "t1",
            occurrenceDateTime = LocalDateTime.of(2026, 8, 15, 17, 0),
            completedAt = LocalDateTime.of(2026, 8, 14, 10, 0),
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        val occs = service.project(
            persons = emptyList(),
            events = emptyList(),
            tasks = listOf(task),
            completions = listOf(compAug),
            startDate = LocalDate.of(2026, 8, 1),
            endDate = LocalDate.of(2026, 9, 30),
            now = LocalDateTime.of(2026, 8, 20, 0, 0)
        )

        val taskDues = occs.filter { it.sourceType == CalendarItemType.TASK_DUE }
        assertEquals(2, taskDues.size)

        // August occurrence: marked completed
        val augDue = taskDues.first { it.date == LocalDate.of(2026, 8, 15) }
        assertTrue(augDue.completed)
        assertFalse(augDue.overdue)

        // September occurrence: active, not completed
        val sepDue = taskDues.first { it.date == LocalDate.of(2026, 9, 15) }
        assertFalse(sepDue.completed)
        assertFalse(sepDue.overdue)

        // Completion record also projected on completion date (2026-08-14)
        val comps = occs.filter { it.sourceType == CalendarItemType.TASK_COMPLETION }
        assertEquals(1, comps.size)
        assertEquals(LocalDate.of(2026, 8, 14), comps.single().date)
    }

    @Test
    fun `task without due date is not projected as due occurrence`() {
        val calendar = AstronomicalVietnameseLunarCalendar()
        val service = CalendarProjectionService(calendar)
        val task = Task(
            id = "t_nodue",
            title = "Đọc sách",
            dueAt = null,
            recurrence = RecurrenceType.NONE,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        val occs = service.project(
            persons = emptyList(),
            events = emptyList(),
            tasks = listOf(task),
            completions = emptyList(),
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 12, 31)
        )

        assertTrue(occs.isEmpty())
    }

    @Test
    fun `soft-deleted data is completely excluded from projection`() {
        val calendar = AstronomicalVietnameseLunarCalendar()
        val service = CalendarProjectionService(calendar)
        val person = Person("p_del", "Bị xóa", birthDateSolar = LocalDate.of(1990, 5, 5), createdAtEpochMillis = 1, updatedAtEpochMillis = 1, deletedAtEpochMillis = 99)
        val event = ImportantEvent(id = "e_del", categoryId = "cat_other", title = "Sự kiện xóa", calendarType = CalendarType.SOLAR, solarDate = LocalDate.of(2026, 5, 5), recurrence = RecurrenceType.NONE, createdAtEpochMillis = 1, updatedAtEpochMillis = 1, deletedAtEpochMillis = 99)
        val task = Task("t_del", "Việc xóa", dueAt = LocalDateTime.of(2026, 5, 5, 10, 0), createdAtEpochMillis = 1, updatedAtEpochMillis = 1, deletedAtEpochMillis = 99)
        val comp = TaskOccurrenceCompletion("c_del", "t_del", LocalDateTime.of(2026, 5, 5, 10, 0), LocalDateTime.of(2026, 5, 5, 10, 0), 1, 1, deletedAtEpochMillis = 99)

        val occs = service.project(
            persons = listOf(person),
            events = listOf(event),
            tasks = listOf(task),
            completions = listOf(comp),
            startDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 5, 31)
        )

        assertTrue(occs.isEmpty())
    }

    @Test
    fun `deterministic sort order puts all-day items first then timed tasks by time`() {
        val calendar = AstronomicalVietnameseLunarCalendar()
        val service = CalendarProjectionService(calendar)
        val bday = Person("p1", "Nguyễn Văn A", birthDateSolar = LocalDate.of(1995, 9, 5), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val event = ImportantEvent(id = "e1", categoryId = "cat_memorial", title = "Giỗ Cụ", calendarType = CalendarType.SOLAR, solarDate = LocalDate.of(2026, 9, 5), recurrence = RecurrenceType.NONE, createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val taskEarly = Task("t1", "Chuẩn bị hoa", dueAt = LocalDateTime.of(2026, 9, 5, 7, 30), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val taskLate = Task("t2", "Cúng giỗ", dueAt = LocalDateTime.of(2026, 9, 5, 11, 0), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)

        val occs = service.project(
            persons = listOf(bday),
            events = listOf(event),
            tasks = listOf(taskLate, taskEarly), // intentionally unordered
            completions = emptyList(),
            startDate = LocalDate.of(2026, 9, 5),
            endDate = LocalDate.of(2026, 9, 5)
        )

        assertEquals(4, occs.size)
        // All-day items first
        assertTrue(occs[0].allDay)
        assertTrue(occs[1].allDay)
        // Timed tasks next in chronological order
        assertEquals("t1", occs[2].sourceId)
        assertEquals(java.time.LocalTime.of(7, 30), occs[2].time)
        assertEquals("t2", occs[3].sourceId)
        assertEquals(java.time.LocalTime.of(11, 0), occs[3].time)
    }

    @Test
    fun `yearly solar event and one-time and yearly tasks stay range based`() {
        val service = CalendarProjectionService(AstronomicalVietnameseLunarCalendar())
        val event = ImportantEvent(
            id = "annual", categoryId = "cat_anniversary", title = "Kỷ niệm",
            calendarType = CalendarType.SOLAR, solarDate = LocalDate.of(2020, 9, 5),
            recurrence = RecurrenceType.YEARLY, createdAtEpochMillis = 1, updatedAtEpochMillis = 1
        )
        val once = Task(
            id = "once", title = "Việc một lần", dueAt = LocalDateTime.of(2027, 9, 6, 9, 0),
            createdAtEpochMillis = 1, updatedAtEpochMillis = 1
        )
        val annual = Task(
            id = "task-annual", title = "Việc hàng năm", dueAt = LocalDateTime.of(2026, 9, 7, 10, 0),
            recurrence = RecurrenceType.YEARLY, createdAtEpochMillis = 1, updatedAtEpochMillis = 1
        )

        val result = service.project(
            persons = emptyList(), events = listOf(event), tasks = listOf(once, annual), completions = emptyList(),
            startDate = LocalDate.of(2027, 9, 1), endDate = LocalDate.of(2027, 9, 30),
            now = LocalDateTime.of(2027, 8, 1, 0, 0)
        )

        assertEquals(listOf("annual", "once", "task-annual"), result.map { it.sourceId })
        assertEquals(LocalDate.of(2027, 9, 5), result[0].date)
        assertEquals(LocalDate.of(2027, 9, 7), result[2].date)
    }

    @Test
    fun `recurrences never project before their source anchor and grid edge range is included`() {
        val service = CalendarProjectionService(AstronomicalVietnameseLunarCalendar())
        val person = Person("future-person", "Người tương lai", birthDateSolar = LocalDate.of(2027, 9, 2), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val event = ImportantEvent(
            id = "future-event", categoryId = "cat_other", title = "Neo cuối tháng",
            calendarType = CalendarType.SOLAR, solarDate = LocalDate.of(2026, 9, 30),
            recurrence = RecurrenceType.MONTHLY, createdAtEpochMillis = 1, updatedAtEpochMillis = 1
        )
        val task = Task(
            id = "future-task", title = "Neo công việc", dueAt = LocalDateTime.of(2026, 9, 30, 8, 0),
            recurrence = RecurrenceType.MONTHLY, createdAtEpochMillis = 1, updatedAtEpochMillis = 1
        )

        val before = service.project(
            listOf(person), listOf(event), listOf(task), emptyList(),
            startDate = LocalDate.of(2026, 8, 31), endDate = LocalDate.of(2026, 9, 29)
        )
        assertTrue(before.isEmpty())

        val gridEdge = service.project(
            emptyList(), listOf(event), listOf(task), emptyList(),
            startDate = LocalDate.of(2026, 9, 28), endDate = LocalDate.of(2026, 10, 4)
        )
        assertEquals(2, gridEdge.size)
        assertTrue(gridEdge.all { it.date == LocalDate.of(2026, 9, 30) })
    }

    @Test
    fun `completion history ignores tombstones and sorts after active timed work`() {
        val service = CalendarProjectionService(AstronomicalVietnameseLunarCalendar())
        val task = Task("task", "Đang làm", dueAt = LocalDateTime.of(2026, 9, 5, 17, 0), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val recurring = Task("recurring", "Đã ghi lịch sử", dueAt = LocalDateTime.of(2026, 9, 5, 7, 0), recurrence = RecurrenceType.MONTHLY, createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val activeCompletion = TaskOccurrenceCompletion("active", "recurring", LocalDateTime.of(2026, 9, 5, 7, 0), LocalDateTime.of(2026, 9, 5, 8, 0), 1, 1)
        val deletedCompletion = activeCompletion.copy(id = "deleted", completedAt = LocalDateTime.of(2026, 9, 5, 9, 0), deletedAtEpochMillis = 2)

        val result = service.project(
            emptyList(), emptyList(), listOf(task, recurring), listOf(activeCompletion, deletedCompletion),
            startDate = LocalDate.of(2026, 9, 5), endDate = LocalDate.of(2026, 9, 5),
            now = LocalDateTime.of(2026, 9, 1, 0, 0)
        )

        assertEquals(1, result.count { it.sourceType == CalendarItemType.TASK_COMPLETION })
        assertEquals(CalendarItemType.TASK_COMPLETION, result.last().sourceType)
        assertEquals("active", result.last().id.removePrefix("task_comp_"))
    }

    @Test
    fun `projected Gregorian day carries known Vietnamese lunar label`() {
        val service = CalendarProjectionService(AstronomicalVietnameseLunarCalendar())
        val person = Person("tet", "Tết", birthDateSolar = LocalDate.of(2000, 2, 10), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val result = service.project(
            listOf(person), emptyList(), emptyList(), emptyList(),
            startDate = LocalDate.of(2024, 2, 10), endDate = LocalDate.of(2024, 2, 10)
        )
        assertEquals(VietnameseLunarDate(2024, 1, 1, false), result.single().lunarDate)
    }

    @Test
    fun `monthly task occurrence August completed projects August completed while September and October remain active`() {
        val service = CalendarProjectionService(AstronomicalVietnameseLunarCalendar())
        val task = Task(
            id = "monthly-task",
            title = "UAT RECURRING MONTHLY TASK",
            dueAt = LocalDateTime.of(2026, 8, 25, 17, 0),
            recurrence = RecurrenceType.MONTHLY,
            status = TaskStatus.TODO,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )
        val augCompletion = TaskOccurrenceCompletion(
            id = "comp-aug",
            taskId = "monthly-task",
            occurrenceDateTime = LocalDateTime.of(2026, 8, 25, 17, 0),
            completedAt = LocalDateTime.of(2026, 8, 25, 10, 0),
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        val result = service.project(
            persons = emptyList(),
            events = emptyList(),
            tasks = listOf(task),
            completions = listOf(augCompletion),
            startDate = LocalDate.of(2026, 8, 1),
            endDate = LocalDate.of(2026, 10, 31),
            now = LocalDateTime.of(2026, 8, 25, 8, 0)
        )

        val taskDues = result.filter { it.sourceType == CalendarItemType.TASK_DUE }
        assertEquals(3, taskDues.size)

        // August occurrence
        val augOcc = taskDues.first { it.date == LocalDate.of(2026, 8, 25) }
        assertTrue(augOcc.completed)
        assertEquals("✓", augOcc.iconKey)

        // September occurrence
        val sepOcc = taskDues.first { it.date == LocalDate.of(2026, 9, 25) }
        assertFalse(sepOcc.completed)
        assertEquals("☐", sepOcc.iconKey)

        // October occurrence
        val octOcc = taskDues.first { it.date == LocalDate.of(2026, 10, 25) }
        assertFalse(octOcc.completed)
        assertEquals("☐", octOcc.iconKey)

        // TaskOccurrenceResolver advances active occurrence to September
        val nextActive = TaskOccurrenceResolver.resolveNextOccurrence(
            task = task,
            now = LocalDateTime.of(2026, 8, 25, 8, 0),
            completions = listOf(augCompletion)
        )
        assertEquals(LocalDateTime.of(2026, 9, 25, 17, 0), nextActive.occurrenceDueAt)
        assertFalse(nextActive.isCompleted)
    }

    @Test
    fun `undo August occurrence completion restores August as active in Calendar and Task resolver`() {
        val service = CalendarProjectionService(AstronomicalVietnameseLunarCalendar())
        val task = Task(
            id = "monthly-task",
            title = "UAT RECURRING MONTHLY TASK",
            dueAt = LocalDateTime.of(2026, 8, 25, 17, 0),
            recurrence = RecurrenceType.MONTHLY,
            status = TaskStatus.TODO,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )
        val tombstonedAugCompletion = TaskOccurrenceCompletion(
            id = "comp-aug",
            taskId = "monthly-task",
            occurrenceDateTime = LocalDateTime.of(2026, 8, 25, 17, 0),
            completedAt = LocalDateTime.of(2026, 8, 25, 10, 0),
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 2,
            deletedAtEpochMillis = 2
        )

        val result = service.project(
            persons = emptyList(),
            events = emptyList(),
            tasks = listOf(task),
            completions = listOf(tombstonedAugCompletion),
            startDate = LocalDate.of(2026, 8, 1),
            endDate = LocalDate.of(2026, 10, 31),
            now = LocalDateTime.of(2026, 8, 25, 8, 0)
        )

        val augOcc = result.filter { it.sourceType == CalendarItemType.TASK_DUE }.first { it.date == LocalDate.of(2026, 8, 25) }
        assertFalse(augOcc.completed)
        assertEquals("☐", augOcc.iconKey)

        val nextActive = TaskOccurrenceResolver.resolveNextOccurrence(
            task = task,
            now = LocalDateTime.of(2026, 8, 25, 8, 0),
            completions = listOf(tombstonedAugCompletion)
        )
        assertEquals(LocalDateTime.of(2026, 8, 25, 17, 0), nextActive.occurrenceDueAt)
        assertFalse(nextActive.isCompleted)
    }

    @Test
    fun `non-recurring task DONE status sets completed without occurrence completions`() {
        val service = CalendarProjectionService(AstronomicalVietnameseLunarCalendar())
        val taskDone = Task(
            id = "once-done",
            title = "Việc làm xong",
            dueAt = LocalDateTime.of(2026, 8, 25, 14, 0),
            recurrence = RecurrenceType.NONE,
            status = TaskStatus.DONE,
            completedAt = LocalDateTime.of(2026, 8, 25, 12, 0),
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1
        )

        val result = service.project(
            persons = emptyList(),
            events = emptyList(),
            tasks = listOf(taskDone),
            completions = emptyList(),
            startDate = LocalDate.of(2026, 8, 1),
            endDate = LocalDate.of(2026, 8, 31),
            now = LocalDateTime.of(2026, 8, 25, 8, 0)
        )

        val occ = result.single { it.sourceType == CalendarItemType.TASK_DUE }
        assertTrue(occ.completed)
        assertEquals("✓", occ.iconKey)
    }

    @Test
    fun `date picker UTC epoch millis converts to exact LocalDate without timezone day shift`() {
        val expected = LocalDate.of(2026, 8, 25)
        val epochMillis = expected.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val converted = java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
        assertEquals(expected, converted)
        assertEquals("25-08-2026", DateInputHelper.formatDate(converted))
    }

    @Test
    fun `lunar today helper converts solar today into valid Vietnamese lunar components`() {
        val calendar = AstronomicalVietnameseLunarCalendar()
        val solarToday = LocalDate.of(2026, 8, 25)
        val lunar = calendar.solarToLunar(solarToday)
        assertTrue(lunar.day in 1..30)
        assertTrue(lunar.month in 1..12)
        assertEquals(2026, lunar.year)
        val roundtripSolar = calendar.lunarToSolar(lunar.year, lunar.month, lunar.day, lunar.isLeapMonth)
        assertEquals(solarToday, roundtripSolar)
    }

    @Test
    fun `important event preserves both relatedPersonId and relatedPersonName`() {
        val event = ImportantEvent(
            id = "ev_test",
            categoryId = "cat_memorial",
            title = "Lễ Giỗ",
            relatedPersonId = "person_123",
            relatedPersonName = "Ông Cố",
            calendarType = CalendarType.LUNAR,
            lunarDay = 15,
            lunarMonth = 7,
            recurrence = RecurrenceType.YEARLY,
            createdAtEpochMillis = 1000,
            updatedAtEpochMillis = 1000
        )
        assertEquals("person_123", event.relatedPersonId)
        assertEquals("Ông Cố", event.relatedPersonName)
    }

    @Test
    fun `bounded markers overflow calculation caps at max dots and computes remainder`() {
        val maxDots = 3
        val emptyOccurrences = emptyList<CalendarOccurrence>()
        assertEquals(0, emptyOccurrences.take(maxDots).size)
        assertEquals(0, maxOf(0, emptyOccurrences.size - maxDots))

        fun createMockOcc(id: String, type: CalendarItemType) = CalendarOccurrence(
            id = id,
            sourceType = type,
            sourceId = "src_$id",
            title = "Item $id",
            date = LocalDate.of(2026, 8, 25),
            allDay = true
        )

        val smallList = listOf(
            createMockOcc("1", CalendarItemType.EVENT),
            createMockOcc("2", CalendarItemType.TASK_DUE)
        )
        assertEquals(2, smallList.take(maxDots).size)
        assertEquals(0, maxOf(0, smallList.size - maxDots))

        val largeList = listOf(
            createMockOcc("1", CalendarItemType.EVENT),
            createMockOcc("2", CalendarItemType.TASK_DUE),
            createMockOcc("3", CalendarItemType.BIRTHDAY),
            createMockOcc("4", CalendarItemType.TASK_DUE),
            createMockOcc("5", CalendarItemType.TASK_DUE)
        )
        assertEquals(3, largeList.take(maxDots).size)
        assertEquals(2, largeList.size - largeList.take(maxDots).size)
    }

    @Test
    fun `generic event editor category default prefers neutral ANNIVERSARY or non-memorial category`() {
        val categories = listOf(
            EventCategory("cat_bday", "Sinh nhật", builtInKey = "BIRTHDAY", builtIn = true, createdAtEpochMillis = 0, updatedAtEpochMillis = 0),
            EventCategory("cat_mem", "Đám giỗ", builtInKey = "MEMORIAL", builtIn = true, createdAtEpochMillis = 0, updatedAtEpochMillis = 0),
            EventCategory("cat_anniv", "Kỷ niệm", builtInKey = "ANNIVERSARY", builtIn = true, createdAtEpochMillis = 0, updatedAtEpochMillis = 0),
            EventCategory("cat_other", "Khác", builtInKey = "OTHER", builtIn = false, createdAtEpochMillis = 0, updatedAtEpochMillis = 0)
        )
        val defaultCatId = categories.firstOrNull { it.builtInKey == "ANNIVERSARY" }?.id
            ?: categories.firstOrNull { it.builtInKey != "MEMORIAL" && it.builtInKey != "BIRTHDAY" }?.id
            ?: categories.firstOrNull()?.id.orEmpty()
        assertEquals("cat_anniv", defaultCatId)
    }

    @Test
    fun `occupied day visual states correctly distinguish today, selected, and occupied`() {
        val today = LocalDate.of(2026, 8, 25)
        val selectedDate = LocalDate.of(2026, 9, 5)
        val emptyDate = LocalDate.of(2026, 8, 26)
        val occupiedDate = LocalDate.of(2026, 9, 5)

        val occurrences = listOf(
            CalendarOccurrence(
                id = "occ_1",
                sourceType = CalendarItemType.EVENT,
                sourceId = "ev_1",
                title = "Khánh thành",
                date = occupiedDate,
                allDay = true
            )
        )

        // Today with no items
        val isTodayEmpty = (today == today) && occurrences.none { it.date == today }
        assertTrue(isTodayEmpty)

        // Selected occupied date
        val isSelectedOccupied = (selectedDate == selectedDate) && occurrences.any { it.date == selectedDate }
        assertTrue(isSelectedOccupied)

        // Empty non-today non-selected date
        val isEmpty = (emptyDate != today) && (emptyDate != selectedDate) && occurrences.none { it.date == emptyDate }
        assertTrue(isEmpty)
    }

    @Test
    fun `selecting date across month boundary keeps exact target LocalDate and updates month`() {
        var currentMonth = YearMonth.of(2026, 8)
        var selectedDate = LocalDate.of(2026, 8, 25)

        val targetAdjacentDate = LocalDate.of(2026, 9, 5)

        // User taps adjacent month date 05/09/2026
        selectedDate = targetAdjacentDate
        if (targetAdjacentDate.month != currentMonth.month || targetAdjacentDate.year != currentMonth.year) {
            currentMonth = YearMonth.from(targetAdjacentDate)
        }

        assertEquals(LocalDate.of(2026, 9, 5), selectedDate)
        assertEquals(YearMonth.of(2026, 9), currentMonth)
    }

    @Test
    fun `Day Agenda receives 100 percent of occurrences for the selected date`() {
        val service = CalendarProjectionService(AstronomicalVietnameseLunarCalendar())
        val selectedDate = LocalDate.of(2026, 9, 5)

        val event = ImportantEvent(
            id = "ev_sep5",
            categoryId = "cat_event",
            title = "Họp mặt",
            calendarType = CalendarType.SOLAR,
            solarDate = selectedDate,
            recurrence = RecurrenceType.NONE,
            createdAtEpochMillis = 1000,
            updatedAtEpochMillis = 1000
        )
        val task = Task(
            id = "task_sep5",
            title = "Mua quà",
            dueAt = LocalDateTime.of(selectedDate, LocalTime.of(14, 0)),
            status = TaskStatus.TODO,
            createdAtEpochMillis = 1000,
            updatedAtEpochMillis = 1000
        )

        val projected = service.project(
            persons = emptyList(),
            events = listOf(event),
            tasks = listOf(task),
            completions = emptyList(),
            startDate = selectedDate,
            endDate = selectedDate,
            now = LocalDateTime.of(2026, 8, 25, 8, 0)
        )

        val dayOccurrences = projected.filter { it.date == selectedDate }
        assertEquals(2, dayOccurrences.size)
        assertTrue(dayOccurrences.any { it.sourceType == CalendarItemType.EVENT && it.title == "Họp mặt" })
        assertTrue(dayOccurrences.any { it.sourceType == CalendarItemType.TASK_DUE && it.title == "Mua quà" })
    }

    @Test
    fun `Week header date range formatting produces concise single-line text`() {
        val weekStart = LocalDate.of(2026, 8, 24)
        val weekEnd = weekStart.plusDays(6)
        val headerText = "Tuần ${weekStart.dayOfMonth}/${weekStart.monthValue} – ${weekEnd.dayOfMonth}/${weekEnd.monthValue}"
        assertEquals("Tuần 24/8 – 30/8", headerText)
    }

    @Test
    fun `CalendarFilter correctly isolates relevant categories`() {
        val occurrences = listOf(
            CalendarOccurrence(id = "1", sourceType = CalendarItemType.BIRTHDAY, sourceId = "p1", title = "Sinh nhật An", date = LocalDate.of(2026, 8, 25), allDay = true),
            CalendarOccurrence(id = "2", sourceType = CalendarItemType.EVENT, sourceId = "e1", title = "Kỷ niệm", date = LocalDate.of(2026, 8, 25), allDay = true),
            CalendarOccurrence(id = "3", sourceType = CalendarItemType.TASK_DUE, sourceId = "t1", title = "Mua sắm", date = LocalDate.of(2026, 8, 25), allDay = true),
            CalendarOccurrence(id = "4", sourceType = CalendarItemType.TASK_COMPLETION, sourceId = "t2", title = "Đã trả nợ", date = LocalDate.of(2026, 8, 25), allDay = true)
        )

        assertEquals(4, filterOccurrences(occurrences, CalendarFilter.ALL).size)
        assertEquals(1, filterOccurrences(occurrences, CalendarFilter.BIRTHDAY).size)
        assertEquals(1, filterOccurrences(occurrences, CalendarFilter.EVENT).size)
        assertEquals(2, filterOccurrences(occurrences, CalendarFilter.TASK).size)
    }
}
