package vn.loi.learning.android.family

import java.time.LocalDate
import java.time.LocalDateTime

enum class PersonGroup {
    FAMILY,
    FRIEND,
    COLLEAGUE,
    OTHER
}

data class Person(
    val id: String,
    val fullName: String,
    val nickname: String? = null,
    val group: PersonGroup = PersonGroup.FAMILY,
    val relationshipLabel: String? = null,
    val birthDateSolar: LocalDate? = null,
    val phone: String? = null,
    val address: String? = null,
    val note: String? = null,
    val avatarRef: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
)


enum class PersonContactFieldType {
    PHONE,
    EMAIL,
    ADDRESS,
    WEBSITE,
    COMPANY,
    JOB_TITLE,
    CUSTOM
}

data class PersonContactField(
    val id: String,
    val personId: String,
    val type: PersonContactFieldType,
    val label: String? = null,
    val value: String,
    val isPrimary: Boolean = false,
    val sortOrder: Int = 0,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
) {
    init {
        require(value.isNotBlank()) { "Person contact field value must not be blank" }
        if (type == PersonContactFieldType.CUSTOM) {
            require(!label.isNullOrBlank()) { "CUSTOM person contact field requires a label" }
        }
    }
}

data class EventCategory(
    val id: String,
    val name: String,
    val builtInKey: String? = null,
    val builtIn: Boolean = false,
    val iconKey: String? = null,
    val sortOrder: Int = 0,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
)

val DEFAULT_EVENT_CATEGORIES: List<EventCategory> = listOf(
    EventCategory(id = "cat_birthday", name = "Sinh nhật", builtInKey = "BIRTHDAY", builtIn = true, iconKey = "🎂", sortOrder = 0, createdAtEpochMillis = 0L, updatedAtEpochMillis = 0L),
    EventCategory(id = "cat_memorial", name = "Đám giỗ", builtInKey = "MEMORIAL", builtIn = true, iconKey = "🕯", sortOrder = 1, createdAtEpochMillis = 0L, updatedAtEpochMillis = 0L),
    EventCategory(id = "cat_wedding", name = "Ngày cưới", builtInKey = "WEDDING", builtIn = true, iconKey = "💍", sortOrder = 2, createdAtEpochMillis = 0L, updatedAtEpochMillis = 0L),
    EventCategory(id = "cat_anniversary", name = "Kỷ niệm", builtInKey = "ANNIVERSARY", builtIn = true, iconKey = "🎉", sortOrder = 3, createdAtEpochMillis = 0L, updatedAtEpochMillis = 0L),
    EventCategory(id = "cat_inauguration", name = "Khánh thành", builtInKey = "INAUGURATION", builtIn = true, iconKey = "🏠", sortOrder = 4, createdAtEpochMillis = 0L, updatedAtEpochMillis = 0L),
    EventCategory(id = "cat_milestone", name = "Cột mốc", builtInKey = "MILESTONE", builtIn = true, iconKey = "🚩", sortOrder = 5, createdAtEpochMillis = 0L, updatedAtEpochMillis = 0L),
    EventCategory(id = "cat_other", name = "Khác", builtInKey = "OTHER", builtIn = true, iconKey = "📌", sortOrder = 6, createdAtEpochMillis = 0L, updatedAtEpochMillis = 0L)
)

enum class CalendarType {
    SOLAR,
    LUNAR
}

enum class RecurrenceType {
    NONE,
    MONTHLY,
    YEARLY
}

data class ImportantEvent(
    val id: String,
    val categoryId: String,
    val title: String,

    val relatedPersonId: String? = null,
    val relatedPersonName: String? = null,

    val calendarType: CalendarType,

    val solarDate: LocalDate? = null,

    val lunarDay: Int? = null,
    val lunarMonth: Int? = null,
    val lunarLeapMonth: Boolean = false,
    val sourceYear: Int? = null,

    val recurrence: RecurrenceType,

    val note: String? = null,

    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
) {
    init {
        when (calendarType) {
            CalendarType.SOLAR -> {
                require(solarDate != null) { "SOLAR event requires solarDate" }
            }
            CalendarType.LUNAR -> {
                require(lunarDay != null && lunarDay in 1..30) { "LUNAR event requires lunarDay in 1..30" }
                require(lunarMonth != null && lunarMonth in 1..12) { "LUNAR event requires lunarMonth in 1..12" }
                if (recurrence == RecurrenceType.NONE) {
                    require(sourceYear != null) { "LUNAR non-recurring event requires sourceYear" }
                }
            }
        }
    }
}

enum class ReminderTargetType {
    PERSON_BIRTHDAY,
    EVENT,
    TASK
}

enum class ReminderOffsetUnit {
    DAY,
    WEEK,
    MONTH
}

enum class ReminderRepeatMode {
    FOLLOW_TARGET,
    ONCE
}

data class ReminderRule(
    val id: String,

    val targetType: ReminderTargetType,
    val targetId: String,

    val amount: Int,
    val unit: ReminderOffsetUnit,

    val remindHour: Int,
    val remindMinute: Int,

    val enabled: Boolean = true,

    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,

    val repeatMode: ReminderRepeatMode = ReminderRepeatMode.FOLLOW_TARGET,
    val occurrenceDate: LocalDate? = null
) {
    init {
        require(amount >= 0) { "amount must be non-negative" }
        require(remindHour in 0..23) { "remindHour must be 0..23" }
        require(remindMinute in 0..59) { "remindMinute must be 0..59" }
        if (repeatMode == ReminderRepeatMode.ONCE) {
            require(occurrenceDate != null) { "ONCE reminder requires non-null occurrenceDate" }
        }
    }
}

enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED
}

enum class TaskPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

data class Task(
    val id: String,
    val title: String,
    val description: String? = null,

    val startAt: LocalDateTime? = null,
    val dueAt: LocalDateTime? = null,

    val recurrence: RecurrenceType = RecurrenceType.NONE,

    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.NORMAL,

    val relatedEventId: String? = null,
    val relatedPersonId: String? = null,

    val completedAt: LocalDateTime? = null,

    val note: String? = null,

    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
) {
    init {
        require(title.isNotBlank()) { "Task title must not be blank" }
        if (startAt != null && dueAt != null) {
            require(!startAt.isAfter(dueAt)) { "startAt must not be after dueAt" }
        }
    }
}

data class ChecklistItem(
    val id: String,
    val taskId: String,
    val text: String,
    val completed: Boolean = false,
    val sortOrder: Int = 0,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
) {
    init {
        require(text.isNotBlank()) { "Checklist item text must not be blank" }
    }
}

data class TaskOccurrenceCompletion(
    val id: String,
    val taskId: String,
    val occurrenceDateTime: LocalDateTime,
    val completedAt: LocalDateTime,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
)

data class FamilyLocalSnapshot(
    val schemaVersion: Int = 5,
    val persons: List<Person> = emptyList(),
    val categories: List<EventCategory> = DEFAULT_EVENT_CATEGORIES,
    val events: List<ImportantEvent> = emptyList(),
    val reminderRules: List<ReminderRule> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val checklistItems: List<ChecklistItem> = emptyList(),
    val taskOccurrenceCompletions: List<TaskOccurrenceCompletion> = emptyList(),
    val personContactFields: List<PersonContactField> = emptyList()
)

enum class CalendarItemType {
    BIRTHDAY,
    EVENT,
    TASK_DUE,
    TASK_COMPLETION
}

data class CalendarOccurrence(
    val id: String,
    val sourceType: CalendarItemType,
    val sourceId: String,
    val title: String,
    val categoryId: String? = null,
    val date: LocalDate,
    val time: java.time.LocalTime? = null,
    val allDay: Boolean,
    val lunarDate: VietnameseLunarDate? = null,
    val completed: Boolean = false,
    val overdue: Boolean = false,
    val relatedPersonId: String? = null,
    val relatedEventId: String? = null,
    val iconKey: String? = null
)
