package vn.loi.learning.android.family

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

enum class EventSourceType {
    BIRTHDAY,
    IMPORTANT_EVENT
}

data class ImportantEventOccurrence(
    val sourceType: EventSourceType,
    val sourceId: String,
    val categoryId: String,
    val title: String,
    val occurrenceDateSolar: LocalDate,
    val calendarLabel: String? = null,
    val daysUntil: Long,
    val iconKey: String? = null
)

// Backwards-compatible typealias if needed
typealias FamilyOccurrence = ImportantEventOccurrence
typealias FamilyEventOccurrenceResolver = ImportantEventOccurrenceResolver

object ReminderOccurrenceResolver {
    fun appliesTo(occurrenceDate: LocalDate, rule: ReminderRule): Boolean {
        if (!rule.enabled || rule.deletedAtEpochMillis != null) return false
        return when (rule.repeatMode) {
            ReminderRepeatMode.FOLLOW_TARGET -> true
            ReminderRepeatMode.ONCE -> rule.occurrenceDate == occurrenceDate
        }
    }

    fun resolve(occurrenceDate: LocalDate, rule: ReminderRule): LocalDateTime? {
        if (!appliesTo(occurrenceDate, rule)) return null
        val targetDate = when (rule.unit) {
            ReminderOffsetUnit.DAY -> occurrenceDate.minusDays(rule.amount.toLong())
            ReminderOffsetUnit.WEEK -> occurrenceDate.minusWeeks(rule.amount.toLong())
            ReminderOffsetUnit.MONTH -> occurrenceDate.minusMonths(rule.amount.toLong())
        }
        return targetDate.atTime(rule.remindHour, rule.remindMinute)
    }
}

fun createDefaultSameDayReminderRule(
    targetType: ReminderTargetType,
    targetId: String,
    now: Long = System.currentTimeMillis()
): ReminderRule = ReminderRule(
    id = UUID.randomUUID().toString(),
    targetType = targetType,
    targetId = targetId,
    amount = 0,
    unit = ReminderOffsetUnit.DAY,
    remindHour = 7,
    remindMinute = 0,
    enabled = true,
    createdAtEpochMillis = now,
    updatedAtEpochMillis = now
)

fun hasSameDayReminder(rules: List<ReminderRule>, targetType: ReminderTargetType, targetId: String): Boolean =
    rules.any { it.targetType == targetType && it.targetId == targetId && it.amount == 0 && it.unit == ReminderOffsetUnit.DAY && it.deletedAtEpochMillis == null }

class ImportantEventOccurrenceResolver(private val lunarCalendar: VietnameseLunarCalendar) {

    fun birthday(person: Person, requestedYear: Int, today: LocalDate): ImportantEventOccurrence? {
        val birth = person.birthDateSolar ?: return null
        val day = if (birth.monthValue == 2 && birth.dayOfMonth == 29 && !java.time.Year.isLeap(requestedYear.toLong())) 28 else birth.dayOfMonth
        val date = LocalDate.of(requestedYear, birth.monthValue, day)
        return ImportantEventOccurrence(
            sourceType = EventSourceType.BIRTHDAY,
            sourceId = person.id,
            categoryId = "cat_birthday",
            title = person.fullName,
            occurrenceDateSolar = date,
            calendarLabel = null,
            daysUntil = ChronoUnit.DAYS.between(today, date),
            iconKey = "🎂"
        )
    }

    fun nextBirthday(person: Person, today: LocalDate): ImportantEventOccurrence? =
        birthday(person, today.year, today)?.let { if (it.daysUntil >= 0) it else birthday(person, today.year + 1, today) }

    fun eventOccurrence(event: ImportantEvent, requestedYear: Int, today: LocalDate, categoryIcon: String? = null): ImportantEventOccurrence? {
        val icon = categoryIcon ?: when (event.categoryId) {
            "cat_memorial" -> "🕯"
            "cat_wedding" -> "💍"
            "cat_anniversary" -> "🎉"
            "cat_inauguration" -> "🏠"
            "cat_milestone" -> "🚩"
            else -> "📌"
        }

        return when (event.calendarType) {
            CalendarType.SOLAR -> {
                val solar = event.solarDate ?: return null
                val (date, days) = when (event.recurrence) {
                    RecurrenceType.NONE -> solar to ChronoUnit.DAYS.between(today, solar)
                    RecurrenceType.MONTHLY -> {
                        val candidate = solarMonthlyOccurrence(solar, today.year, today.monthValue)
                        val target = if (!candidate.isBefore(today)) candidate else {
                            val next = today.plusMonths(1)
                            solarMonthlyOccurrence(solar, next.year, next.monthValue)
                        }
                        target to ChronoUnit.DAYS.between(today, target)
                    }
                    RecurrenceType.YEARLY -> {
                        val day = if (solar.monthValue == 2 && solar.dayOfMonth == 29 && !java.time.Year.isLeap(requestedYear.toLong())) 28 else solar.dayOfMonth
                        val targetDate = LocalDate.of(requestedYear, solar.monthValue, day)
                        targetDate to ChronoUnit.DAYS.between(today, targetDate)
                    }
                }
                ImportantEventOccurrence(
                    sourceType = EventSourceType.IMPORTANT_EVENT,
                    sourceId = event.id,
                    categoryId = event.categoryId,
                    title = event.title,
                    occurrenceDateSolar = date,
                    calendarLabel = null,
                    daysUntil = days,
                    iconKey = icon
                )
            }
            CalendarType.LUNAR -> {
                val lDay = event.lunarDay ?: return null
                val lMonth = event.lunarMonth ?: return null
                val leap = event.lunarLeapMonth
                val label = "$lDay/$lMonth${if (leap) " nhuận" else ""} âm lịch"
                val (date, days) = when (event.recurrence) {
                    RecurrenceType.NONE -> {
                        val sYear = event.sourceYear ?: return null
                        val solar = runCatching { lunarCalendar.lunarToSolar(sYear, lMonth, lDay, leap) }.getOrNull() ?: return null
                        solar to ChronoUnit.DAYS.between(today, solar)
                    }
                    RecurrenceType.MONTHLY -> {
                        // Monthly lunar not active in v4 UI; fallback to yearly conversion
                        val solar = runCatching { lunarCalendar.lunarToSolar(requestedYear, lMonth, lDay, leap) }.getOrNull() ?: return null
                        solar to ChronoUnit.DAYS.between(today, solar)
                    }
                    RecurrenceType.YEARLY -> {
                        val solar = runCatching { lunarCalendar.lunarToSolar(requestedYear, lMonth, lDay, leap) }.getOrNull() ?: return null
                        solar to ChronoUnit.DAYS.between(today, solar)
                    }
                }
                ImportantEventOccurrence(
                    sourceType = EventSourceType.IMPORTANT_EVENT,
                    sourceId = event.id,
                    categoryId = event.categoryId,
                    title = event.title,
                    occurrenceDateSolar = date,
                    calendarLabel = label,
                    daysUntil = days,
                    iconKey = icon
                )
            }
        }
    }

    fun nextEvent(event: ImportantEvent, today: LocalDate, categoryIcon: String? = null): ImportantEventOccurrence? {
        return when (event.calendarType) {
            CalendarType.SOLAR -> {
                when (event.recurrence) {
                    RecurrenceType.NONE -> eventOccurrence(event, event.solarDate?.year ?: today.year, today, categoryIcon)
                    RecurrenceType.MONTHLY -> eventOccurrence(event, today.year, today, categoryIcon)
                    RecurrenceType.YEARLY -> {
                        eventOccurrence(event, today.year, today, categoryIcon)?.let {
                            if (it.daysUntil >= 0) it else eventOccurrence(event, today.year + 1, today, categoryIcon)
                        }
                    }
                }
            }
            CalendarType.LUNAR -> {
                when (event.recurrence) {
                    RecurrenceType.NONE -> {
                        val sYear = event.sourceYear ?: return null
                        eventOccurrence(event, sYear, today, categoryIcon)
                    }
                    RecurrenceType.MONTHLY, RecurrenceType.YEARLY -> {
                        val currentLunarYear = lunarCalendar.solarToLunar(today).year
                        val current = runCatching { eventOccurrence(event, currentLunarYear, today, categoryIcon) }.getOrNull()
                        if (current != null && current.daysUntil >= 0) current else eventOccurrence(event, currentLunarYear + 1, today, categoryIcon)
                    }
                }
            }
        }
    }

    fun allUpcoming(
        persons: List<Person>,
        events: List<ImportantEvent>,
        today: LocalDate,
        categories: List<EventCategory> = DEFAULT_EVENT_CATEGORIES
    ): List<ImportantEventOccurrence> {
        val categoryMap = categories.associateBy { it.id }
        val activePersons = persons.filter { it.deletedAtEpochMillis == null }
        val activeEvents = events.filter { it.deletedAtEpochMillis == null }

        val bdays = activePersons.mapNotNull { nextBirthday(it, today) }
        val evs = activeEvents.mapNotNull { ev ->
            val icon = categoryMap[ev.categoryId]?.iconKey
            nextEvent(ev, today, icon)
        }

        return (bdays + evs).sortedWith(
            compareBy<ImportantEventOccurrence> { if (it.daysUntil < 0) 1 else 0 }
                .thenBy { it.occurrenceDateSolar }
        )
    }

    companion object {
        fun solarMonthlyOccurrence(sourceDate: LocalDate, year: Int, month: Int): LocalDate {
            val maxDay = LocalDate.of(year, month, 1).lengthOfMonth()
            val day = minOf(sourceDate.dayOfMonth, maxDay)
            return LocalDate.of(year, month, day)
        }
    }
}

data class TaskOccurrence(
    val task: Task,
    val occurrenceDueAt: LocalDateTime?,
    val isCompleted: Boolean,
    val completedAt: LocalDateTime? = null,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val daysUntil: Long? = null
)

data class CompletedTaskHistoryItem(
    val taskId: String,
    val title: String,
    val isRecurring: Boolean,
    val occurrenceDateTime: LocalDateTime?,
    val completedAt: LocalDateTime,
    val completionId: String? = null
)

object TaskOccurrenceResolver {
    fun monthlyOccurrenceDateTime(due: LocalDateTime, year: Int, month: Int): LocalDateTime {
        val ym = java.time.YearMonth.of(year, month)
        val maxDay = ym.lengthOfMonth()
        val day = minOf(due.dayOfMonth, maxDay)
        return LocalDateTime.of(LocalDate.of(year, month, day), due.toLocalTime())
    }

    fun yearlyOccurrenceDateTime(due: LocalDateTime, year: Int): LocalDateTime {
        val maxDay = if (due.monthValue == 2 && due.dayOfMonth == 29 && !java.time.Year.isLeap(year.toLong())) 28 else due.dayOfMonth
        return LocalDateTime.of(LocalDate.of(year, due.monthValue, maxDay), due.toLocalTime())
    }

    fun resolveNextOccurrence(
        task: Task,
        now: LocalDateTime,
        completions: List<TaskOccurrenceCompletion> = emptyList(),
        checklistItems: List<ChecklistItem> = emptyList()
    ): TaskOccurrence {
        val taskChecklist = checklistItems.filter { it.taskId == task.id && it.deletedAtEpochMillis == null }.sortedBy { it.sortOrder }
        val due = task.dueAt
            ?: return TaskOccurrence(
                task = task,
                occurrenceDueAt = null,
                isCompleted = task.status == TaskStatus.DONE,
                completedAt = task.completedAt,
                checklistItems = taskChecklist,
                daysUntil = null
            )

        val taskCompletions = completions.filter { it.taskId == task.id && it.deletedAtEpochMillis == null }
        val completedSet = taskCompletions.map { it.occurrenceDateTime }.toSet()

        return when (task.recurrence) {
            RecurrenceType.NONE -> {
                val isDone = task.status == TaskStatus.DONE
                TaskOccurrence(
                    task = task,
                    occurrenceDueAt = due,
                    isCompleted = isDone,
                    completedAt = task.completedAt,
                    checklistItems = taskChecklist,
                    daysUntil = ChronoUnit.DAYS.between(now.toLocalDate(), due.toLocalDate())
                )
            }
            RecurrenceType.MONTHLY -> {
                // Find earliest uncompleted occurrence, starting from due month
                val startYearMonth = java.time.YearMonth.from(due.toLocalDate())
                val nowYearMonth = java.time.YearMonth.from(now.toLocalDate())
                val baseMonth = if (nowYearMonth.isBefore(startYearMonth)) startYearMonth else startYearMonth

                var candidateOcc: LocalDateTime? = null
                var offset = 0
                while (offset < 240) { // search up to 20 years
                    val currentYM = baseMonth.plusMonths(offset.toLong())
                    val maxDay = currentYM.lengthOfMonth()
                    val day = minOf(due.dayOfMonth, maxDay)
                    val occDate = currentYM.atDay(day)
                    val occDateTime = LocalDateTime.of(occDate, due.toLocalTime())

                    if (occDateTime !in completedSet) {
                        // If it's the first uncompleted occurrence or it's on/after now
                        if (candidateOcc == null || !occDateTime.isBefore(now)) {
                            candidateOcc = occDateTime
                            if (!occDateTime.isBefore(now) || offset >= 12) break
                        }
                    }
                    offset++
                }

                val finalDue = candidateOcc ?: due
                TaskOccurrence(
                    task = task,
                    occurrenceDueAt = finalDue,
                    isCompleted = false,
                    completedAt = null,
                    checklistItems = taskChecklist,
                    daysUntil = ChronoUnit.DAYS.between(now.toLocalDate(), finalDue.toLocalDate())
                )
            }
            RecurrenceType.YEARLY -> {
                val startYear = due.year
                var candidateOcc: LocalDateTime? = null
                var offset = 0
                while (offset < 50) {
                    val currentYear = startYear + offset
                    val maxDay = if (due.monthValue == 2 && due.dayOfMonth == 29 && !java.time.Year.isLeap(currentYear.toLong())) 28 else due.dayOfMonth
                    val occDate = LocalDate.of(currentYear, due.monthValue, maxDay)
                    val occDateTime = LocalDateTime.of(occDate, due.toLocalTime())

                    if (occDateTime !in completedSet) {
                        if (candidateOcc == null || !occDateTime.isBefore(now)) {
                            candidateOcc = occDateTime
                            if (!occDateTime.isBefore(now)) break
                        }
                    }
                    offset++
                }
                val finalDue = candidateOcc ?: due
                TaskOccurrence(
                    task = task,
                    occurrenceDueAt = finalDue,
                    isCompleted = false,
                    completedAt = null,
                    checklistItems = taskChecklist,
                    daysUntil = ChronoUnit.DAYS.between(now.toLocalDate(), finalDue.toLocalDate())
                )
            }
        }
    }

    fun allOccurrences(
        tasks: List<Task>,
        now: LocalDateTime,
        completions: List<TaskOccurrenceCompletion> = emptyList(),
        checklistItems: List<ChecklistItem> = emptyList()
    ): List<TaskOccurrence> {
        val activeTasks = tasks.filter { it.deletedAtEpochMillis == null }
        return activeTasks.map { resolveNextOccurrence(it, now, completions, checklistItems) }
            .sortedWith(
                compareBy<TaskOccurrence> { it.occurrenceDueAt == null }
                    .thenBy { it.occurrenceDueAt }
            )
    }
}

object TaskQueryHelper {
    fun filterToday(occurrences: List<TaskOccurrence>, today: LocalDate): List<TaskOccurrence> =
        occurrences.filter { !it.isCompleted && it.occurrenceDueAt?.toLocalDate() == today }

    fun filterUpcoming(occurrences: List<TaskOccurrence>, now: LocalDateTime): List<TaskOccurrence> =
        occurrences.filter { !it.isCompleted && it.occurrenceDueAt != null && it.occurrenceDueAt.isAfter(now) }

    fun filterOverdue(occurrences: List<TaskOccurrence>, now: LocalDateTime): List<TaskOccurrence> =
        occurrences.filter { !it.isCompleted && it.task.status != TaskStatus.CANCELLED && it.occurrenceDueAt != null && it.occurrenceDueAt.isBefore(now) }

    fun filterActive(occurrences: List<TaskOccurrence>): List<TaskOccurrence> =
        occurrences.filter { !it.isCompleted && it.task.status != TaskStatus.CANCELLED }

    fun filterCompleted(
        tasks: List<Task>,
        completions: List<TaskOccurrenceCompletion>
    ): List<CompletedTaskHistoryItem> {
        val activeTasks = tasks.filter { it.deletedAtEpochMillis == null }
        val nonRecurringCompleted = activeTasks.filter {
            it.recurrence == RecurrenceType.NONE && it.status == TaskStatus.DONE
        }.map {
            CompletedTaskHistoryItem(
                taskId = it.id,
                title = it.title,
                isRecurring = false,
                occurrenceDateTime = it.dueAt,
                completedAt = it.completedAt ?: LocalDateTime.now(),
                completionId = null
            )
        }

        val taskMap = activeTasks.associateBy { it.id }
        val recurringCompleted = completions.filter { it.deletedAtEpochMillis == null }.mapNotNull { c ->
            val t = taskMap[c.taskId] ?: return@mapNotNull null
            CompletedTaskHistoryItem(
                taskId = t.id,
                title = t.title,
                isRecurring = true,
                occurrenceDateTime = c.occurrenceDateTime,
                completedAt = c.completedAt,
                completionId = c.id
            )
        }

        return (nonRecurringCompleted + recurringCompleted).sortedByDescending { it.completedAt }
    }
}


object DateInputHelper {
    private val DD_MM_YYYY = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    fun formatDate(date: LocalDate?): String = date?.format(DD_MM_YYYY).orEmpty()

    fun parseDate(input: String): LocalDate? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null
        return runCatching {
            if (trimmed.contains("-") && trimmed.length == 10) {
                if (trimmed[2] == '-' && trimmed[5] == '-') {
                    val parts = trimmed.split("-")
                    val day = parts[0].toInt()
                    val month = parts[1].toInt()
                    val year = parts[2].toInt()
                    LocalDate.of(year, month, day)
                } else {
                    LocalDate.parse(trimmed)
                }
            } else if (trimmed.contains("/") && trimmed.length == 10) {
                val parts = trimmed.split("/")
                val day = parts[0].toInt()
                val month = parts[1].toInt()
                val year = parts[2].toInt()
                LocalDate.of(year, month, day)
            } else {
                null
            }
        }.getOrNull()
    }

    fun formatDigits(digits: String): String {
        val d = digits.filter(Char::isDigit).take(8)
        return when {
            d.isEmpty() -> ""
            d.length == 1 -> d
            d.length == 2 -> "$d-"
            d.length == 3 -> "${d.substring(0, 2)}-${d.substring(2)}"
            d.length == 4 -> "${d.substring(0, 2)}-${d.substring(2)}-"
            d.length in 5..8 -> "${d.substring(0, 2)}-${d.substring(2, 4)}-${d.substring(4)}"
            else -> d
        }
    }

    fun digitIndexToFormattedIndex(digitCount: Int, totalDigits: Int): Int {
        return when {
            digitCount <= 0 -> 0
            digitCount == 1 -> 1
            digitCount == 2 -> 3
            digitCount == 3 -> 4
            digitCount == 4 -> 6
            digitCount in 5..8 -> digitCount + 2
            else -> 10
        }
    }

    fun formatTextFieldValue(prev: TextFieldValue, next: TextFieldValue): TextFieldValue {
        if (prev.text == next.text) {
            return next
        }

        val isSingleCharBackspace = prev.selection.collapsed &&
                next.text.length == prev.text.length - 1 &&
                prev.selection.end > 0 &&
                next.selection.end == prev.selection.end - 1

        val deletedChar = if (isSingleCharBackspace) prev.text[prev.selection.end - 1] else null

        val targetText = if (isSingleCharBackspace && deletedChar == '-') {
            val deleteIdx = prev.selection.end - 1
            val beforeDash = prev.text.substring(0, deleteIdx)
            val afterDash = prev.text.substring(deleteIdx + 1)
            val trimmedBefore = beforeDash.dropLast(1)
            trimmedBefore + afterDash
        } else {
            next.text
        }

        val nextDigits = targetText.filter(Char::isDigit).take(8)
        val cursorInNext = next.selection.end.coerceIn(0, targetText.length)
        val digitsBeforeCursor = targetText.substring(0, cursorInNext).filter(Char::isDigit).length.coerceAtMost(nextDigits.length)

        val formatted = formatDigits(nextDigits)
        val targetCursor = digitIndexToFormattedIndex(digitsBeforeCursor, nextDigits.length).coerceIn(0, formatted.length)

        return TextFieldValue(
            text = formatted,
            selection = TextRange(targetCursor)
        )
    }

    fun formatInput(prev: String, next: String): String {
        return formatTextFieldValue(
            TextFieldValue(prev, TextRange(prev.length)),
            TextFieldValue(next, TextRange(next.length))
        ).text
    }
}

class CalendarProjectionService(private val lunarCalendar: VietnameseLunarCalendar) {

    fun project(
        persons: List<Person>,
        events: List<ImportantEvent>,
        tasks: List<Task>,
        completions: List<TaskOccurrenceCompletion>,
        categories: List<EventCategory> = DEFAULT_EVENT_CATEGORIES,
        startDate: LocalDate,
        endDate: LocalDate,
        now: LocalDateTime = LocalDateTime.now()
    ): List<CalendarOccurrence> {
        val categoryMap = categories.associateBy { it.id }
        val activePersons = persons.filter { it.deletedAtEpochMillis == null }
        val activeEvents = events.filter { it.deletedAtEpochMillis == null }
        val activeTasks = tasks.filter { it.deletedAtEpochMillis == null && it.status != TaskStatus.CANCELLED }
        val activeCompletions = completions.filter { it.deletedAtEpochMillis == null }

        val occurrences = mutableListOf<CalendarOccurrence>()

        // 1. Birthdays
        for (person in activePersons) {
            val birth = person.birthDateSolar ?: continue
            for (year in startDate.year..endDate.year) {
                val maxDay = LocalDate.of(year, birth.month, 1).lengthOfMonth()
                val day = minOf(birth.dayOfMonth, maxDay)
                val date = LocalDate.of(year, birth.month, day)
                if (year >= birth.year && date in startDate..endDate) {
                    occurrences.add(
                        CalendarOccurrence(
                            id = "bday_${person.id}_$year",
                            sourceType = CalendarItemType.BIRTHDAY,
                            sourceId = person.id,
                            title = "Sinh nhật ${person.fullName}",
                            categoryId = "cat_birthday",
                            date = date,
                            time = null,
                            allDay = true,
                            lunarDate = runCatching { lunarCalendar.solarToLunar(date) }.getOrNull(),
                            completed = false,
                            overdue = false,
                            relatedPersonId = person.id,
                            relatedEventId = null,
                            iconKey = "🎂"
                        )
                    )
                }
            }
        }

        // 2. Important Events
        for (event in activeEvents) {
            val icon = categoryMap[event.categoryId]?.iconKey ?: "📌"
            when (event.calendarType) {
                CalendarType.SOLAR -> {
                    val sDate = event.solarDate ?: continue
                    when (event.recurrence) {
                        RecurrenceType.NONE -> {
                            if (sDate in startDate..endDate) {
                                occurrences.add(
                                    CalendarOccurrence(
                                        id = "ev_${event.id}_${sDate}",
                                        sourceType = CalendarItemType.EVENT,
                                        sourceId = event.id,
                                        title = event.title,
                                        categoryId = event.categoryId,
                                        date = sDate,
                                        time = null,
                                        allDay = true,
                                        lunarDate = runCatching { lunarCalendar.solarToLunar(sDate) }.getOrNull(),
                                        completed = false,
                                        overdue = false,
                                        relatedPersonId = event.relatedPersonId,
                                        relatedEventId = null,
                                        iconKey = icon
                                    )
                                )
                            }
                        }
                        RecurrenceType.MONTHLY -> {
                            var currentMonth = startDate.withDayOfMonth(1)
                            val endMonth = endDate.withDayOfMonth(1)
                            while (!currentMonth.isAfter(endMonth)) {
                                val date = ImportantEventOccurrenceResolver.solarMonthlyOccurrence(sDate, currentMonth.year, currentMonth.monthValue)
                                if (!date.isBefore(sDate) && date in startDate..endDate) {
                                    occurrences.add(
                                        CalendarOccurrence(
                                            id = "ev_${event.id}_${date}",
                                            sourceType = CalendarItemType.EVENT,
                                            sourceId = event.id,
                                            title = event.title,
                                            categoryId = event.categoryId,
                                            date = date,
                                            time = null,
                                            allDay = true,
                                            lunarDate = runCatching { lunarCalendar.solarToLunar(date) }.getOrNull(),
                                            completed = false,
                                            overdue = false,
                                            relatedPersonId = event.relatedPersonId,
                                            relatedEventId = null,
                                            iconKey = icon
                                        )
                                    )
                                }
                                currentMonth = currentMonth.plusMonths(1)
                            }
                        }
                        RecurrenceType.YEARLY -> {
                            for (year in startDate.year..endDate.year) {
                                val maxDay = LocalDate.of(year, sDate.month, 1).lengthOfMonth()
                                val day = minOf(sDate.dayOfMonth, maxDay)
                                val date = LocalDate.of(year, sDate.month, day)
                                if (!date.isBefore(sDate) && date in startDate..endDate) {
                                    occurrences.add(
                                        CalendarOccurrence(
                                            id = "ev_${event.id}_$year",
                                            sourceType = CalendarItemType.EVENT,
                                            sourceId = event.id,
                                            title = event.title,
                                            categoryId = event.categoryId,
                                            date = date,
                                            time = null,
                                            allDay = true,
                                            lunarDate = runCatching { lunarCalendar.solarToLunar(date) }.getOrNull(),
                                            completed = false,
                                            overdue = false,
                                            relatedPersonId = event.relatedPersonId,
                                            relatedEventId = null,
                                            iconKey = icon
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                CalendarType.LUNAR -> {
                    val lDay = event.lunarDay ?: continue
                    val lMonth = event.lunarMonth ?: continue
                    val leap = event.lunarLeapMonth
                    when (event.recurrence) {
                        RecurrenceType.NONE -> {
                            val sYear = event.sourceYear ?: continue
                            val sDate = runCatching { lunarCalendar.lunarToSolar(sYear, lMonth, lDay, leap) }.getOrNull()
                            if (sDate != null && sDate in startDate..endDate) {
                                occurrences.add(
                                    CalendarOccurrence(
                                        id = "ev_${event.id}_${sDate}",
                                        sourceType = CalendarItemType.EVENT,
                                        sourceId = event.id,
                                        title = event.title,
                                        categoryId = event.categoryId,
                                        date = sDate,
                                        time = null,
                                        allDay = true,
                                        lunarDate = runCatching { lunarCalendar.solarToLunar(sDate) }.getOrNull(),
                                        completed = false,
                                        overdue = false,
                                        relatedPersonId = event.relatedPersonId,
                                        relatedEventId = null,
                                        iconKey = icon
                                    )
                                )
                            }
                        }
                        RecurrenceType.MONTHLY, RecurrenceType.YEARLY -> {
                            val startLunarYear = runCatching { lunarCalendar.solarToLunar(startDate).year }.getOrDefault(startDate.year) - 1
                            val endLunarYear = runCatching { lunarCalendar.solarToLunar(endDate).year }.getOrDefault(endDate.year) + 1
                            for (lYear in startLunarYear..endLunarYear) {
                                val sDate = runCatching { lunarCalendar.lunarToSolar(lYear, lMonth, lDay, leap) }.getOrNull()
                                if (sDate != null && sDate in startDate..endDate) {
                                    occurrences.add(
                                        CalendarOccurrence(
                                            id = "ev_${event.id}_${lYear}",
                                            sourceType = CalendarItemType.EVENT,
                                            sourceId = event.id,
                                            title = event.title,
                                            categoryId = event.categoryId,
                                            date = sDate,
                                            time = null,
                                            allDay = true,
                                            lunarDate = runCatching { lunarCalendar.solarToLunar(sDate) }.getOrNull(),
                                            completed = false,
                                            overdue = false,
                                            relatedPersonId = event.relatedPersonId,
                                            relatedEventId = null,
                                            iconKey = icon
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Tasks Due
        for (task in activeTasks) {
            val due = task.dueAt ?: continue
            when (task.recurrence) {
                RecurrenceType.NONE -> {
                    val dueDate = due.toLocalDate()
                    if (dueDate in startDate..endDate) {
                        val isDone = task.status == TaskStatus.DONE
                        occurrences.add(
                            CalendarOccurrence(
                                id = "task_due_${task.id}_$dueDate",
                                sourceType = CalendarItemType.TASK_DUE,
                                sourceId = task.id,
                                title = task.title,
                                categoryId = null,
                                date = dueDate,
                                time = due.toLocalTime(),
                                allDay = false,
                                lunarDate = runCatching { lunarCalendar.solarToLunar(dueDate) }.getOrNull(),
                                completed = isDone,
                                overdue = !isDone && due.isBefore(now),
                                relatedPersonId = task.relatedPersonId,
                                relatedEventId = task.relatedEventId,
                                iconKey = if (isDone) "✓" else "☐"
                            )
                        )
                    }
                }
                RecurrenceType.MONTHLY -> {
                    var currentMonth = startDate.withDayOfMonth(1)
                    val endMonth = endDate.withDayOfMonth(1)
                    while (!currentMonth.isAfter(endMonth)) {
                        val occurrenceDue = TaskOccurrenceResolver.monthlyOccurrenceDateTime(due, currentMonth.year, currentMonth.monthValue)
                        val dueDate = occurrenceDue.toLocalDate()
                        if (!occurrenceDue.isBefore(due) && dueDate in startDate..endDate) {
                            val isCompleted = activeCompletions.any { it.taskId == task.id && it.occurrenceDateTime.toLocalDate() == dueDate }
                            occurrences.add(
                                CalendarOccurrence(
                                    id = "task_due_${task.id}_$dueDate",
                                    sourceType = CalendarItemType.TASK_DUE,
                                    sourceId = task.id,
                                    title = task.title,
                                    categoryId = null,
                                    date = dueDate,
                                    time = occurrenceDue.toLocalTime(),
                                    allDay = false,
                                    lunarDate = runCatching { lunarCalendar.solarToLunar(dueDate) }.getOrNull(),
                                    completed = isCompleted,
                                    overdue = !isCompleted && occurrenceDue.isBefore(now),
                                    relatedPersonId = task.relatedPersonId,
                                    relatedEventId = task.relatedEventId,
                                    iconKey = if (isCompleted) "✓" else "☐"
                                )
                            )
                        }
                        currentMonth = currentMonth.plusMonths(1)
                    }
                }
                RecurrenceType.YEARLY -> {
                    for (year in startDate.year..endDate.year) {
                        val occurrenceDue = TaskOccurrenceResolver.yearlyOccurrenceDateTime(due, year)
                        val dueDate = occurrenceDue.toLocalDate()
                        if (!occurrenceDue.isBefore(due) && dueDate in startDate..endDate) {
                            val isCompleted = activeCompletions.any { it.taskId == task.id && it.occurrenceDateTime.toLocalDate() == dueDate }
                            occurrences.add(
                                CalendarOccurrence(
                                    id = "task_due_${task.id}_$dueDate",
                                    sourceType = CalendarItemType.TASK_DUE,
                                    sourceId = task.id,
                                    title = task.title,
                                    categoryId = null,
                                    date = dueDate,
                                    time = occurrenceDue.toLocalTime(),
                                    allDay = false,
                                    lunarDate = runCatching { lunarCalendar.solarToLunar(dueDate) }.getOrNull(),
                                    completed = isCompleted,
                                    overdue = !isCompleted && occurrenceDue.isBefore(now),
                                    relatedPersonId = task.relatedPersonId,
                                    relatedEventId = task.relatedEventId,
                                    iconKey = if (isCompleted) "✓" else "☐"
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Task Completion History
        for (comp in activeCompletions) {
            val compDate = comp.completedAt.toLocalDate()
            if (compDate in startDate..endDate) {
                val task = activeTasks.firstOrNull { it.id == comp.taskId }
                if (task != null) {
                    occurrences.add(
                        CalendarOccurrence(
                            id = "task_comp_${comp.id}",
                            sourceType = CalendarItemType.TASK_COMPLETION,
                            sourceId = comp.taskId,
                            title = task.title,
                            categoryId = null,
                            date = compDate,
                            time = comp.completedAt.toLocalTime(),
                            allDay = false,
                            lunarDate = runCatching { lunarCalendar.solarToLunar(compDate) }.getOrNull(),
                            completed = true,
                            overdue = false,
                            relatedPersonId = task.relatedPersonId,
                            relatedEventId = task.relatedEventId,
                            iconKey = "✓"
                        )
                    )
                }
            }
        }

        return occurrences.sortedWith(
            compareBy<CalendarOccurrence> { it.date }
                .thenBy { !it.allDay }
                .thenBy { it.sourceType == CalendarItemType.TASK_COMPLETION }
                .thenBy { it.time }
                .thenBy { it.title }
                .thenBy { it.id }
        )
    }
}
