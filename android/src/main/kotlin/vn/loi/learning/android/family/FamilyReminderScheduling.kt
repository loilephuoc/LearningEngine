package vn.loi.learning.android.family

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ScheduledReminder(
    val scheduleKey: String,
    val ruleId: String,
    val targetType: ReminderTargetType,
    val targetId: String,
    val occurrenceDate: LocalDate,
    val triggerAt: LocalDateTime,
    val title: String,
    val body: String,
    val notificationId: Int
) {
    val requestCode: Int get() = stablePositiveInt(scheduleKey)
}

object FamilyReminderIdentity {
    fun scheduleKey(rule: ReminderRule, occurrenceDate: LocalDate, triggerAt: LocalDateTime): String =
        listOf(rule.targetType.name, rule.targetId, rule.id, occurrenceDate, triggerAt).joinToString("|")

    fun stableId(value: String): Int = stablePositiveInt(value)
}

private fun stablePositiveInt(value: String): Int {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return (ByteBuffer.wrap(digest).int and Int.MAX_VALUE).coerceAtLeast(1)
}

class FamilyReminderScheduleProjector(
    private val calendarProjection: CalendarProjectionService
) {
    fun project(snapshot: FamilyLocalSnapshot, now: LocalDateTime): List<ScheduledReminder> {
        val activeRules = snapshot.reminderRules.filter {
            it.enabled && it.deletedAtEpochMillis == null
        }
        if (activeRules.isEmpty()) return emptyList()

        val maxOffsetDays = activeRules.maxOf { rule ->
            when (rule.unit) {
                ReminderOffsetUnit.DAY -> rule.amount.toLong()
                ReminderOffsetUnit.WEEK -> rule.amount * 7L
                ReminderOffsetUnit.MONTH -> rule.amount * 31L
            }
        }
        val start = now.toLocalDate()
        val rollingEnd = start.plusDays(maxOffsetDays + 400L)
        val onceEnd = activeRules.asSequence()
            .filter { it.repeatMode == ReminderRepeatMode.ONCE }
            .mapNotNull { it.occurrenceDate }
            .maxOrNull()
        val end = if (onceEnd != null && onceEnd.isAfter(rollingEnd)) onceEnd else rollingEnd
        val occurrences = calendarProjection.project(
            persons = snapshot.persons,
            events = snapshot.events,
            tasks = snapshot.tasks,
            completions = snapshot.taskOccurrenceCompletions,
            categories = snapshot.categories,
            startDate = start,
            endDate = end,
            now = now
        ).filter { it.sourceType != CalendarItemType.TASK_COMPLETION && !it.completed }

        return activeRules.mapNotNull { rule ->
            occurrences.asSequence()
                .filter { it.matches(rule) }
                .mapNotNull { occurrence ->
                    ReminderOccurrenceResolver.resolve(occurrence.date, rule)?.let { occurrence to it }
                }
                .filter { (_, triggerAt) -> triggerAt.isAfter(now) }
                .minByOrNull { (_, triggerAt) -> triggerAt }
                ?.let { (occurrence, triggerAt) -> scheduled(rule, occurrence, triggerAt) }
        }.sortedWith(compareBy<ScheduledReminder> { it.triggerAt }.thenBy { it.scheduleKey })
    }

    private fun CalendarOccurrence.matches(rule: ReminderRule): Boolean = when (rule.targetType) {
        ReminderTargetType.PERSON_BIRTHDAY -> sourceType == CalendarItemType.BIRTHDAY && sourceId == rule.targetId
        ReminderTargetType.EVENT -> sourceType == CalendarItemType.EVENT && sourceId == rule.targetId
        ReminderTargetType.TASK -> sourceType == CalendarItemType.TASK_DUE && sourceId == rule.targetId
    }

    private fun scheduled(rule: ReminderRule, occurrence: CalendarOccurrence, triggerAt: LocalDateTime): ScheduledReminder {
        val key = FamilyReminderIdentity.scheduleKey(rule, occurrence.date, triggerAt)
        val days = ChronoUnit.DAYS.between(triggerAt.toLocalDate(), occurrence.date)
        val title = when (rule.targetType) {
            ReminderTargetType.PERSON_BIRTHDAY -> "🎂 ${occurrence.title}"
            ReminderTargetType.EVENT -> "${occurrence.iconKey ?: "📌"} ${occurrence.title}"
            ReminderTargetType.TASK -> "☐ ${occurrence.title}"
        }
        val body = when (rule.targetType) {
            ReminderTargetType.PERSON_BIRTHDAY -> {
                val name = occurrence.title.removePrefix("Sinh nhật ")
                if (days == 0L) "Hôm nay là sinh nhật $name." else "Còn $days ngày đến sinh nhật $name."
            }
            ReminderTargetType.EVENT -> {
                if (days == 0L) "Hôm nay là ${occurrence.title.lowercase()}." else "Còn $days ngày đến ${occurrence.title.lowercase()}."
            }
            ReminderTargetType.TASK -> {
                val time = occurrence.time?.let { " lúc ${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" }.orEmpty()
                if (days == 0L) "Công việc đến hạn hôm nay$time." else "Còn $days ngày đến hạn ${occurrence.title}."
            }
        }
        return ScheduledReminder(key, rule.id, rule.targetType, rule.targetId, occurrence.date, triggerAt, title, body, FamilyReminderIdentity.stableId(key))
    }
}

interface FamilyReminderScheduler {
    fun scheduledKeys(): Set<String>
    fun schedule(reminder: ScheduledReminder)
    fun cancel(scheduleKey: String)
}

class ReminderScheduleReconciler(
    private val snapshot: () -> FamilyLocalSnapshot,
    private val projector: FamilyReminderScheduleProjector,
    private val scheduler: FamilyReminderScheduler,
    private val now: () -> LocalDateTime = LocalDateTime::now
) {
    @Synchronized
    fun reconcile(forceReschedule: Boolean = false): List<ScheduledReminder> {
        val desired = projector.project(snapshot(), now())
        val desiredKeys = desired.mapTo(linkedSetOf()) { it.scheduleKey }
        val existing = scheduler.scheduledKeys()
        (existing - desiredKeys).forEach(scheduler::cancel)
        desired.filter { forceReschedule || it.scheduleKey !in existing }.forEach(scheduler::schedule)
        return desired
    }
}
