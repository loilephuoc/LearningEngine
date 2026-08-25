package vn.loi.learning.android.family

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.*
import org.junit.Test

class FamilyReminderSchedulingTest {
    private val projector = FamilyReminderScheduleProjector(
        CalendarProjectionService(AstronomicalVietnameseLunarCalendar())
    )

    @Test fun `birthday same-day and advance reminders resolve next yearly occurrence`() {
        val person = Person("p", "Lê Văn Tĩnh", birthDateSolar = LocalDate.of(1965, 6, 12), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val sameDay = rule("same", ReminderTargetType.PERSON_BIRTHDAY, "p", 0, 7)
        val advance = rule("advance", ReminderTargetType.PERSON_BIRTHDAY, "p", 7, 8)
        val result = projector.project(snapshot(persons = listOf(person), rules = listOf(sameDay, advance)), LocalDateTime.of(2027, 6, 1, 9, 0))
        assertEquals(2, result.size)
        assertEquals(LocalDateTime.of(2027, 6, 5, 8, 0), result.first { it.ruleId == "advance" }.triggerAt)
        assertEquals(LocalDateTime.of(2027, 6, 12, 7, 0), result.first { it.ruleId == "same" }.triggerAt)
    }

    @Test fun `past same-day birthday trigger skips to next year without catch-up`() {
        val person = Person("p", "An", birthDateSolar = LocalDate.of(1990, 6, 12), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val result = projector.project(snapshot(persons = listOf(person), rules = listOf(rule("r", ReminderTargetType.PERSON_BIRTHDAY, "p", 0, 7))), LocalDateTime.of(2027, 6, 12, 8, 0))
        assertEquals(LocalDate.of(2028, 6, 12), result.single().occurrenceDate)
        assertEquals(LocalDateTime.of(2028, 6, 12, 7, 0), result.single().triggerAt)
    }

    @Test fun `one-time monthly yearly and lunar events use existing occurrence projection`() {
        val events = listOf(
            solarEvent("once", LocalDate.of(2027, 9, 10), RecurrenceType.NONE),
            solarEvent("monthly", LocalDate.of(2027, 1, 31), RecurrenceType.MONTHLY),
            solarEvent("yearly", LocalDate.of(2020, 9, 12), RecurrenceType.YEARLY),
            ImportantEvent("lunar", "cat_memorial", "Giỗ Ông Ngoại", calendarType = CalendarType.LUNAR, lunarDay = 24, lunarMonth = 7, recurrence = RecurrenceType.YEARLY, createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        )
        val rules = events.map { rule("r-${it.id}", ReminderTargetType.EVENT, it.id, 0, 7) }
        val result = projector.project(snapshot(events = events, rules = rules), LocalDateTime.of(2027, 8, 20, 0, 0))
        assertEquals(setOf("once", "monthly", "yearly", "lunar"), result.map { it.targetId }.toSet())
        assertEquals(LocalDate.of(2027, 8, 25), result.first { it.targetId == "lunar" }.occurrenceDate)
        assertEquals(LocalDate.of(2027, 8, 31), result.first { it.targetId == "monthly" }.occurrenceDate)
    }

    @Test fun `once reminder only schedules its anchored occurrence`() {
        val event = solarEvent("annual", LocalDate.of(2020, 9, 12), RecurrenceType.YEARLY)
        val once = rule("once", ReminderTargetType.EVENT, "annual", 0, 7).copy(
            repeatMode = ReminderRepeatMode.ONCE,
            occurrenceDate = LocalDate.of(2028, 9, 12)
        )
        val result = projector.project(snapshot(events = listOf(event), rules = listOf(once)), LocalDateTime.of(2027, 1, 1, 0, 0))
        assertEquals(LocalDate.of(2028, 9, 12), result.single().occurrenceDate)
    }

    @Test fun `task schedules one-time monthly and yearly while completed occurrence advances`() {
        val tasks = listOf(
            task("once", LocalDateTime.of(2027, 9, 10, 18, 0), RecurrenceType.NONE),
            task("monthly", LocalDateTime.of(2027, 8, 15, 17, 0), RecurrenceType.MONTHLY),
            task("yearly", LocalDateTime.of(2020, 9, 20, 9, 0), RecurrenceType.YEARLY),
            Task("no-due", "Không hạn", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        )
        val completion = TaskOccurrenceCompletion("c", "monthly", LocalDateTime.of(2027, 8, 15, 17, 0), LocalDateTime.of(2027, 8, 14, 8, 0), 1, 1)
        val rules = tasks.map { rule("r-${it.id}", ReminderTargetType.TASK, it.id, 0, 7) }
        val result = projector.project(snapshot(tasks = tasks, completions = listOf(completion), rules = rules), LocalDateTime.of(2027, 8, 1, 0, 0))
        assertEquals(setOf("once", "monthly", "yearly"), result.map { it.targetId }.toSet())
        assertEquals(LocalDate.of(2027, 9, 15), result.first { it.targetId == "monthly" }.occurrenceDate)
    }

    @Test fun `multiple rules coexist while disabled and deleted rules are omitted`() {
        val event = solarEvent("event", LocalDate.of(2027, 9, 20), RecurrenceType.NONE)
        val active1 = rule("a", ReminderTargetType.EVENT, "event", 0, 7)
        val active2 = rule("b", ReminderTargetType.EVENT, "event", 3, 8)
        val disabled = rule("c", ReminderTargetType.EVENT, "event", 1, 9).copy(enabled = false)
        val deleted = rule("d", ReminderTargetType.EVENT, "event", 2, 9).copy(deletedAtEpochMillis = 2)
        val result = projector.project(snapshot(events = listOf(event), rules = listOf(active1, active2, disabled, deleted)), LocalDateTime.of(2027, 9, 1, 0, 0))
        assertEquals(setOf("a", "b"), result.map { it.ruleId }.toSet())
    }

    @Test fun `schedule identity is deterministic and time changes desired key`() {
        val rule = rule("r", ReminderTargetType.EVENT, "e", 0, 7)
        val occurrence = LocalDate.of(2027, 9, 20)
        val first = FamilyReminderIdentity.scheduleKey(rule, occurrence, occurrence.atTime(7, 0))
        val same = FamilyReminderIdentity.scheduleKey(rule, occurrence, occurrence.atTime(7, 0))
        val changed = FamilyReminderIdentity.scheduleKey(rule.copy(remindHour = 8), occurrence, occurrence.atTime(8, 0))
        assertEquals(first, same)
        assertEquals(FamilyReminderIdentity.stableId(first), FamilyReminderIdentity.stableId(same))
        assertNotEquals(first, changed)
    }

    @Test fun `reconciliation is idempotent and cancels obsolete schedule`() {
        var current = snapshot(
            events = listOf(solarEvent("e", LocalDate.of(2027, 9, 20), RecurrenceType.NONE)),
            rules = listOf(rule("r", ReminderTargetType.EVENT, "e", 0, 7))
        )
        val scheduler = FakeScheduler()
        val reconciler = ReminderScheduleReconciler({ current }, projector, scheduler) { LocalDateTime.of(2027, 9, 1, 0, 0) }
        val oldKey = reconciler.reconcile().single().scheduleKey
        reconciler.reconcile()
        assertEquals(1, scheduler.scheduleCalls)
        current = current.copy(reminderRules = listOf(rule("r", ReminderTargetType.EVENT, "e", 0, 8)))
        val newKey = reconciler.reconcile().single().scheduleKey
        assertNotEquals(oldKey, newKey)
        assertEquals(listOf(oldKey), scheduler.cancelled)
        assertEquals(setOf(newKey), scheduler.keys)
    }

    @Test fun `alarm precision uses graceful fallback on modern Android without exact access`() {
        assertEquals(FamilyAlarmPrecision.EXACT, selectFamilyAlarmPrecision(30, false))
        assertEquals(FamilyAlarmPrecision.EXACT, selectFamilyAlarmPrecision(31, true))
        assertEquals(FamilyAlarmPrecision.INEXACT_FALLBACK, selectFamilyAlarmPrecision(31, false))
    }

    private fun snapshot(
        persons: List<Person> = emptyList(), events: List<ImportantEvent> = emptyList(), tasks: List<Task> = emptyList(),
        completions: List<TaskOccurrenceCompletion> = emptyList(), rules: List<ReminderRule> = emptyList()
    ) = FamilyLocalSnapshot(persons = persons, events = events, tasks = tasks, taskOccurrenceCompletions = completions, reminderRules = rules)

    private fun rule(id: String, type: ReminderTargetType, target: String, amount: Int, hour: Int) = ReminderRule(
        id, type, target, amount, ReminderOffsetUnit.DAY, hour, 0, true, 1, 1
    )

    private fun solarEvent(id: String, date: LocalDate, recurrence: RecurrenceType) = ImportantEvent(
        id, "cat_other", id, calendarType = CalendarType.SOLAR, solarDate = date, recurrence = recurrence,
        createdAtEpochMillis = 1, updatedAtEpochMillis = 1
    )

    private fun task(id: String, due: LocalDateTime, recurrence: RecurrenceType) = Task(
        id, id, dueAt = due, recurrence = recurrence, createdAtEpochMillis = 1, updatedAtEpochMillis = 1
    )

    private class FakeScheduler : FamilyReminderScheduler {
        val keys = linkedSetOf<String>()
        val cancelled = mutableListOf<String>()
        var scheduleCalls = 0
        override fun scheduledKeys(): Set<String> = keys.toSet()
        override fun schedule(reminder: ScheduledReminder) { scheduleCalls++; keys += reminder.scheduleKey }
        override fun cancel(scheduleKey: String) { cancelled += scheduleKey; keys -= scheduleKey }
    }
}
