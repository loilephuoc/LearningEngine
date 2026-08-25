package vn.loi.learning.android.family.widget

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.loi.learning.android.family.*

class FamilyWidgetModelTest {

    private val calendar = AstronomicalVietnameseLunarCalendar()
    private val projectionService = CalendarProjectionService(calendar)

    @Test
    fun `today solar and lunar dates are formatted correctly`() {
        val testNow = LocalDateTime.of(2026, 8, 25, 10, 0)
        val snapshot = FamilyLocalSnapshot()

        val state = FamilyWidgetModelBuilder.build(snapshot, calendar, projectionService, testNow)

        assertEquals("Thứ Ba", state.todaySolarWeekday)
        assertEquals("25 tháng 8", state.todaySolarDateFormatted)
        assertEquals("25/08/2026", state.todaySolarFull)
        assertEquals("13/7 Âm lịch", state.todayLunarFormatted)
        assertEquals("Hôm nay chưa có lịch", state.todaySummary)
        assertTrue(state.allItemsOrdered.isEmpty())
    }

    @Test
    fun `ordering policy prioritizes overdue then due today then today events then upcoming`() {
        val testNow = LocalDateTime.of(2026, 8, 25, 10, 0)

        val overdueTask = Task(
            id = "t_overdue",
            title = "Việc quá hạn",
            recurrence = RecurrenceType.NONE,
            dueAt = LocalDateTime.of(2026, 8, 23, 14, 0),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val dueTodayTask = Task(
            id = "t_today",
            title = "Việc hôm nay chiều",
            recurrence = RecurrenceType.NONE,
            dueAt = LocalDateTime.of(2026, 8, 25, 17, 0),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val birthdayToday = Person(
            id = "p_today",
            fullName = "Nguyễn Văn Ba",
            birthDateSolar = LocalDate.of(1960, 8, 25),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val upcomingEvent = ImportantEvent(
            id = "e_upcoming",
            categoryId = "cat_anniversary",
            title = "Kỷ niệm ngày cưới",
            calendarType = CalendarType.SOLAR,
            solarDate = LocalDate.of(2026, 8, 28),
            recurrence = RecurrenceType.NONE,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val snapshot = FamilyLocalSnapshot(
            persons = listOf(birthdayToday),
            events = listOf(upcomingEvent),
            tasks = listOf(overdueTask, dueTodayTask)
        )

        val state = FamilyWidgetModelBuilder.build(snapshot, calendar, projectionService, testNow)

        assertEquals(4, state.allItemsOrdered.size)
        // 1. Overdue task
        assertEquals("Việc quá hạn", state.allItemsOrdered[0].title)
        assertEquals(FamilyWidgetItemSemantic.TASK_OVERDUE, state.allItemsOrdered[0].semantic)
        assertEquals("Quá hạn 2 ngày", state.allItemsOrdered[0].badgeText)

        // 2. Due today task
        assertEquals("Việc hôm nay chiều", state.allItemsOrdered[1].title)
        assertEquals(FamilyWidgetItemSemantic.TASK_DUE_TODAY, state.allItemsOrdered[1].semantic)
        assertEquals("17:00", state.allItemsOrdered[1].badgeText)

        // 3. Birthday today
        assertEquals("Sinh nhật Nguyễn Văn Ba", state.allItemsOrdered[2].title)
        assertEquals(FamilyWidgetItemSemantic.BIRTHDAY, state.allItemsOrdered[2].semantic)
        assertEquals("Hôm nay", state.allItemsOrdered[2].badgeText)

        // 4. Upcoming event
        assertEquals("Kỷ niệm ngày cưới", state.allItemsOrdered[3].title)
        assertEquals(FamilyWidgetItemSemantic.EVENT, state.allItemsOrdered[3].semantic)
        assertEquals("Còn 3 ngày", state.allItemsOrdered[3].badgeText)

        // Workload summary
        assertEquals("3 mục · 2 việc, 1 sinh nhật", state.todaySummary)
    }

    @Test
    fun `completed recurring task occurrence is excluded while future occurrence remains active`() {
        val testNow = LocalDateTime.of(2026, 8, 25, 10, 0)

        val recurringTask = Task(
            id = "t_rec",
            title = "Báo cáo định kỳ",
            recurrence = RecurrenceType.MONTHLY,
            dueAt = LocalDateTime.of(2026, 8, 25, 9, 0),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        // August 25 occurrence completed
        val augCompletion = TaskOccurrenceCompletion(
            id = "comp_aug",
            taskId = "t_rec",
            occurrenceDateTime = LocalDateTime.of(2026, 8, 25, 9, 0),
            completedAt = LocalDateTime.of(2026, 8, 25, 9, 30),
            createdAtEpochMillis = 2000L,
            updatedAtEpochMillis = 2000L
        )

        val snapshot = FamilyLocalSnapshot(
            tasks = listOf(recurringTask),
            taskOccurrenceCompletions = listOf(augCompletion)
        )

        val state = FamilyWidgetModelBuilder.build(snapshot, calendar, projectionService, testNow)

        // August 25 is completed, so it must not appear in active items
        assertFalse(state.allItemsOrdered.any { it.date == LocalDate.of(2026, 8, 25) })

        // September 25 occurrence should be in upcoming items!
        val sepOcc = state.allItemsOrdered.find { it.date == LocalDate.of(2026, 9, 25) }
        assertTrue(sepOcc != null)
        assertEquals("Báo cáo định kỳ", sepOcc?.title)
        assertEquals(FamilyWidgetItemSemantic.TASK_UPCOMING, sepOcc?.semantic)
        assertEquals("25/09", sepOcc?.badgeText)
    }

    @Test
    fun `soft-deleted entities are strictly excluded from widget`() {
        val testNow = LocalDateTime.of(2026, 8, 25, 10, 0)

        val deletedPerson = Person(
            id = "p_del",
            fullName = "Người Đã Xóa",
            birthDateSolar = LocalDate.of(1990, 8, 25),
            deletedAtEpochMillis = 5000L,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 5000L
        )

        val deletedEvent = ImportantEvent(
            id = "e_del",
            categoryId = "cat_other",
            title = "Sự kiện đã xóa",
            calendarType = CalendarType.SOLAR,
            solarDate = LocalDate.of(2026, 8, 25),
            recurrence = RecurrenceType.NONE,
            deletedAtEpochMillis = 5000L,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 5000L
        )

        val deletedTask = Task(
            id = "t_del",
            title = "Việc đã xóa",
            recurrence = RecurrenceType.NONE,
            dueAt = LocalDateTime.of(2026, 8, 25, 15, 0),
            deletedAtEpochMillis = 5000L,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 5000L
        )

        val snapshot = FamilyLocalSnapshot(
            persons = listOf(deletedPerson),
            events = listOf(deletedEvent),
            tasks = listOf(deletedTask)
        )

        val state = FamilyWidgetModelBuilder.build(snapshot, calendar, projectionService, testNow)

        assertTrue(state.allItemsOrdered.isEmpty())
        assertEquals("Hôm nay chưa có lịch", state.todaySummary)
    }

    @Test
    fun `long Vietnamese title is preserved safely without crash`() {
        val testNow = LocalDateTime.of(2026, 8, 25, 10, 0)
        val longTitle = "Chuẩn bị hồ sơ kiểm tra chất lượng hệ thống xạ trị và thiết bị chẩn đoán hình ảnh bệnh viện"

        val task = Task(
            id = "t_long",
            title = longTitle,
            recurrence = RecurrenceType.NONE,
            dueAt = LocalDateTime.of(2026, 8, 25, 16, 0),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val snapshot = FamilyLocalSnapshot(tasks = listOf(task))
        val state = FamilyWidgetModelBuilder.build(snapshot, calendar, projectionService, testNow)

        assertEquals(1, state.allItemsOrdered.size)
        assertEquals(longTitle, state.allItemsOrdered[0].title)
        assertEquals("16:00", state.allItemsOrdered[0].badgeText)
    }
}
