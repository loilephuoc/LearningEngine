package vn.loi.learning.android.family

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface FamilyRepository {
    val snapshot: StateFlow<FamilyLocalSnapshot>
    suspend fun reload(): FamilyLocalSnapshot
    suspend fun replaceSnapshot(snapshot: FamilyLocalSnapshot)

    suspend fun upsertPerson(person: Person)
    suspend fun deletePerson(id: String, atEpochMillis: Long)

    suspend fun upsertCategory(category: EventCategory)
    suspend fun deleteCategory(id: String, atEpochMillis: Long)

    suspend fun upsertEvent(event: ImportantEvent)
    suspend fun deleteEvent(id: String, atEpochMillis: Long)

    suspend fun upsertReminderRule(rule: ReminderRule)
    suspend fun deleteReminderRule(id: String)
    suspend fun setReminderRulesForTarget(targetType: ReminderTargetType, targetId: String, rules: List<ReminderRule>)

    suspend fun upsertTask(task: Task)
    suspend fun deleteTask(id: String, atEpochMillis: Long)

    suspend fun upsertChecklistItem(item: ChecklistItem)
    suspend fun deleteChecklistItem(id: String, atEpochMillis: Long)
    suspend fun setChecklistItemsForTask(taskId: String, items: List<ChecklistItem>)

    suspend fun completeTaskOccurrence(completion: TaskOccurrenceCompletion)
    suspend fun undoTaskOccurrenceCompletion(completionId: String, atEpochMillis: Long)
}

class JsonFamilyRepository(
    private val file: Path,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : FamilyRepository {
    private val mutex = Mutex()
    private val state = MutableStateFlow(loadFromDisk())
    override val snapshot: StateFlow<FamilyLocalSnapshot> = state

    override suspend fun reload(): FamilyLocalSnapshot = mutex.withLock { loadFromDisk().also { state.value = it } }
    override suspend fun replaceSnapshot(snapshot: FamilyLocalSnapshot) = mutex.withLock {
        require(snapshot.schemaVersion == 4)
        save(snapshot)
        state.value = snapshot
    }

    override suspend fun upsertPerson(person: Person) = mutate {
        it.copy(persons = it.persons.upsert(person) { value -> value.id })
    }

    override suspend fun deletePerson(id: String, atEpochMillis: Long) = mutate { snap ->
        snap.copy(persons = snap.persons.map { if (it.id == id) it.copy(updatedAtEpochMillis = atEpochMillis, deletedAtEpochMillis = atEpochMillis) else it })
    }

    override suspend fun upsertCategory(category: EventCategory) = mutate {
        it.copy(categories = it.categories.upsert(category) { value -> value.id })
    }

    override suspend fun deleteCategory(id: String, atEpochMillis: Long) = mutate { snap ->
        snap.copy(categories = snap.categories.map { if (it.id == id) it.copy(updatedAtEpochMillis = atEpochMillis, deletedAtEpochMillis = atEpochMillis) else it })
    }

    override suspend fun upsertEvent(event: ImportantEvent) = mutate {
        it.copy(events = it.events.upsert(event) { value -> value.id })
    }

    override suspend fun deleteEvent(id: String, atEpochMillis: Long) = mutate { snap ->
        snap.copy(events = snap.events.map { if (it.id == id) it.copy(updatedAtEpochMillis = atEpochMillis, deletedAtEpochMillis = atEpochMillis) else it })
    }

    override suspend fun upsertReminderRule(rule: ReminderRule) = mutate {
        it.copy(reminderRules = it.reminderRules.upsert(rule) { value -> value.id })
    }

    override suspend fun deleteReminderRule(id: String) = mutate {
        val now = System.currentTimeMillis()
        it.copy(reminderRules = it.reminderRules.map { rule ->
            if (rule.id == id) rule.copy(updatedAtEpochMillis = now, deletedAtEpochMillis = now) else rule
        })
    }

    override suspend fun setReminderRulesForTarget(targetType: ReminderTargetType, targetId: String, rules: List<ReminderRule>) = mutate { snap ->
        val now = System.currentTimeMillis()
        val incomingIds = rules.mapTo(hashSetOf()) { it.id }
        val retained = snap.reminderRules.map { current ->
            if (current.targetType == targetType && current.targetId == targetId && current.id !in incomingIds && current.deletedAtEpochMillis == null) {
                current.copy(updatedAtEpochMillis = now, deletedAtEpochMillis = now)
            } else current
        }
        snap.copy(reminderRules = retained.upsertAll(rules) { it.id })
    }

    override suspend fun upsertTask(task: Task) = mutate {
        it.copy(tasks = it.tasks.upsert(task) { value -> value.id })
    }

    override suspend fun deleteTask(id: String, atEpochMillis: Long) = mutate { snap ->
        snap.copy(tasks = snap.tasks.map { if (it.id == id) it.copy(updatedAtEpochMillis = atEpochMillis, deletedAtEpochMillis = atEpochMillis) else it })
    }

    override suspend fun upsertChecklistItem(item: ChecklistItem) = mutate {
        it.copy(checklistItems = it.checklistItems.upsert(item) { value -> value.id })
    }

    override suspend fun deleteChecklistItem(id: String, atEpochMillis: Long) = mutate { snap ->
        snap.copy(checklistItems = snap.checklistItems.map { if (it.id == id) it.copy(updatedAtEpochMillis = atEpochMillis, deletedAtEpochMillis = atEpochMillis) else it })
    }

    override suspend fun setChecklistItemsForTask(taskId: String, items: List<ChecklistItem>) = mutate { snap ->
        val now = System.currentTimeMillis()
        val incomingIds = items.mapTo(hashSetOf()) { it.id }
        val retained = snap.checklistItems.map { current ->
            if (current.taskId == taskId && current.id !in incomingIds && current.deletedAtEpochMillis == null) {
                current.copy(updatedAtEpochMillis = now, deletedAtEpochMillis = now)
            } else current
        }
        snap.copy(checklistItems = retained.upsertAll(items) { it.id })
    }

    override suspend fun completeTaskOccurrence(completion: TaskOccurrenceCompletion) = mutate {
        it.copy(taskOccurrenceCompletions = it.taskOccurrenceCompletions.upsert(completion) { value -> value.id })
    }

    override suspend fun undoTaskOccurrenceCompletion(completionId: String, atEpochMillis: Long) = mutate { snap ->
        snap.copy(taskOccurrenceCompletions = snap.taskOccurrenceCompletions.map {
            if (it.id == completionId) it.copy(updatedAtEpochMillis = atEpochMillis, deletedAtEpochMillis = atEpochMillis) else it
        })
    }

    private suspend fun mutate(block: (FamilyLocalSnapshot) -> FamilyLocalSnapshot) = mutex.withLock {
        val next = block(state.value)
        save(next)
        state.value = next
    }

    private fun loadFromDisk(): FamilyLocalSnapshot {
        if (!Files.exists(file)) return FamilyLocalSnapshot()
        val stored = Files.newBufferedReader(file, StandardCharsets.UTF_8).use {
            json.decodeFromString<StoredSnapshotReader>(it.readText())
        }
        require(stored.schemaVersion in 1..4) { "Unsupported Family schema ${stored.schemaVersion}" }
        return stored.toDomain()
    }

    private fun save(snapshot: FamilyLocalSnapshot) {
        Files.createDirectories(file.parent)
        val temporary = file.resolveSibling("${file.fileName}.tmp")
        Files.newBufferedWriter(temporary, StandardCharsets.UTF_8).use {
            it.write(json.encodeToString(StoredSnapshotV4.from(snapshot)))
        }
        runCatching { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            .getOrElse { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING) }
    }
}

private fun <T> List<T>.upsert(value: T, id: (T) -> String): List<T> =
    if (any { id(it) == id(value) }) map { if (id(it) == id(value)) value else it } else this + value

private fun <T> List<T>.upsertAll(values: List<T>, id: (T) -> String): List<T> =
    values.fold(this) { result, value -> result.upsert(value, id) }

@Serializable
private data class StoredSnapshotV4(
    val schemaVersion: Int = 4,
    val persons: List<StoredPerson> = emptyList(),
    val categories: List<StoredCategory> = emptyList(),
    val events: List<StoredEvent> = emptyList(),
    val reminderRules: List<StoredReminderRuleV3> = emptyList(),
    val tasks: List<StoredTask> = emptyList(),
    val checklistItems: List<StoredChecklistItem> = emptyList(),
    val taskOccurrenceCompletions: List<StoredTaskOccurrenceCompletion> = emptyList()
) {
    companion object {
        fun from(s: FamilyLocalSnapshot) = StoredSnapshotV4(
            schemaVersion = 4,
            persons = s.persons.map(StoredPerson::from),
            categories = s.categories.map(StoredCategory::from),
            events = s.events.map(StoredEvent::from),
            reminderRules = s.reminderRules.map(StoredReminderRuleV3::from),
            tasks = s.tasks.map(StoredTask::from),
            checklistItems = s.checklistItems.map(StoredChecklistItem::from),
            taskOccurrenceCompletions = s.taskOccurrenceCompletions.map(StoredTaskOccurrenceCompletion::from)
        )
    }
}

@Serializable
private data class StoredSnapshotReader(
    val schemaVersion: Int = 4,
    val persons: List<StoredPerson> = emptyList(),
    val members: List<StoredLegacyMember> = emptyList(),
    val categories: List<StoredCategory> = emptyList(),
    val events: List<StoredEvent> = emptyList(),
    val memorials: List<StoredLegacyMemorial> = emptyList(),
    val reminderRules: List<StoredFlexibleRule> = emptyList(),
    val tasks: List<StoredTask> = emptyList(),
    val checklistItems: List<StoredChecklistItem> = emptyList(),
    val taskOccurrenceCompletions: List<StoredTaskOccurrenceCompletion> = emptyList()
) {
    fun toDomain(): FamilyLocalSnapshot {
        val mergedPersons: List<Person> = when (schemaVersion) {
            1 -> members.map { it.toPerson() }
            else -> persons.map { it.toDomain() }
        }

        val mergedEvents: List<ImportantEvent> = when (schemaVersion) {
            1, 2 -> memorials.map { it.toImportantEvent() }
            else -> events.map { it.toDomain() }
        }

        val loadedCategories = categories.map { it.toDomain() }
        val categoryList: List<EventCategory> = if (loadedCategories.isEmpty()) {
            DEFAULT_EVENT_CATEGORIES
        } else {
            val custom = loadedCategories.filter { !it.builtIn }
            val builtInIds = DEFAULT_EVENT_CATEGORIES.map { it.id }.toSet()
            val existingBuiltIns = loadedCategories.filter { it.id in builtInIds }
            val missingBuiltIns = DEFAULT_EVENT_CATEGORIES.filter { d -> loadedCategories.none { it.id == d.id } }
            (existingBuiltIns + missingBuiltIns + custom).sortedBy { it.sortOrder }
        }

        val domainRules: List<ReminderRule> = reminderRules.map { it.toV3Rule() }
        val domainTasks: List<Task> = tasks.map { it.toDomain() }
        val domainChecklist: List<ChecklistItem> = checklistItems.map { it.toDomain() }
        val domainCompletions: List<TaskOccurrenceCompletion> = taskOccurrenceCompletions.map { it.toDomain() }

        // Ensure default same-day reminder for dated targets that have existing rules but lack a same-day rule
        val backfilledRules = domainRules.toMutableList()
        for (p in mergedPersons) {
            if (p.birthDateSolar != null && p.deletedAtEpochMillis == null) {
                val pRules = backfilledRules.filter { it.targetType == ReminderTargetType.PERSON_BIRTHDAY && it.targetId == p.id && it.deletedAtEpochMillis == null }
                if (pRules.isNotEmpty() && pRules.none { it.amount == 0 && it.unit == ReminderOffsetUnit.DAY }) {
                    backfilledRules.add(createDefaultSameDayReminderRule(ReminderTargetType.PERSON_BIRTHDAY, p.id, p.createdAtEpochMillis))
                }
            }
        }
        for (e in mergedEvents) {
            if (e.deletedAtEpochMillis == null) {
                val eRules = backfilledRules.filter { it.targetType == ReminderTargetType.EVENT && it.targetId == e.id && it.deletedAtEpochMillis == null }
                if (eRules.isNotEmpty() && eRules.none { it.amount == 0 && it.unit == ReminderOffsetUnit.DAY }) {
                    backfilledRules.add(createDefaultSameDayReminderRule(ReminderTargetType.EVENT, e.id, e.createdAtEpochMillis))
                }
            }
        }
        for (t in domainTasks) {
            if (t.dueAt != null && t.deletedAtEpochMillis == null) {
                val tRules = backfilledRules.filter { it.targetType == ReminderTargetType.TASK && it.targetId == t.id && it.deletedAtEpochMillis == null }
                if (tRules.isNotEmpty() && tRules.none { it.amount == 0 && it.unit == ReminderOffsetUnit.DAY }) {
                    backfilledRules.add(createDefaultSameDayReminderRule(ReminderTargetType.TASK, t.id, t.createdAtEpochMillis))
                }
            }
        }

        return FamilyLocalSnapshot(
            schemaVersion = 4,
            persons = mergedPersons,
            categories = categoryList,
            events = mergedEvents,
            reminderRules = backfilledRules,
            tasks = domainTasks,
            checklistItems = domainChecklist,
            taskOccurrenceCompletions = domainCompletions
        )
    }
}

@Serializable
private data class StoredLegacyMember(
    val id: String,
    val fullName: String,
    val nickname: String? = null,
    val relationship: String? = null,
    val birthDateSolar: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val note: String? = null,
    val avatarRef: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
) {
    fun toPerson() = Person(
        id = id,
        fullName = fullName,
        nickname = nickname,
        group = PersonGroup.FAMILY,
        relationshipLabel = relationship,
        birthDateSolar = birthDateSolar?.let(LocalDate::parse),
        phone = phone,
        address = address,
        note = note,
        avatarRef = avatarRef,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis
    )
}

@Serializable
private data class StoredPerson(
    val id: String,
    val fullName: String,
    val nickname: String? = null,
    val group: String = PersonGroup.FAMILY.name,
    val relationshipLabel: String? = null,
    val birthDateSolar: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val note: String? = null,
    val avatarRef: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
) {
    fun toDomain() = Person(
        id = id,
        fullName = fullName,
        nickname = nickname,
        group = runCatching { PersonGroup.valueOf(group) }.getOrDefault(PersonGroup.OTHER),
        relationshipLabel = relationshipLabel,
        birthDateSolar = birthDateSolar?.let(LocalDate::parse),
        phone = phone,
        address = address,
        note = note,
        avatarRef = avatarRef,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis
    )
    companion object {
        fun from(v: Person) = StoredPerson(
            v.id,
            v.fullName,
            v.nickname,
            v.group.name,
            v.relationshipLabel,
            v.birthDateSolar?.toString(),
            v.phone,
            v.address,
            v.note,
            v.avatarRef,
            v.createdAtEpochMillis,
            v.updatedAtEpochMillis,
            v.deletedAtEpochMillis
        )
    }
}

@Serializable
private data class StoredCategory(
    val id: String,
    val name: String,
    val builtInKey: String? = null,
    val builtIn: Boolean = false,
    val iconKey: String? = null,
    val sortOrder: Int = 0,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
) {
    fun toDomain() = EventCategory(id, name, builtInKey, builtIn, iconKey, sortOrder, createdAtEpochMillis, updatedAtEpochMillis, deletedAtEpochMillis)
    companion object {
        fun from(c: EventCategory) = StoredCategory(c.id, c.name, c.builtInKey, c.builtIn, c.iconKey, c.sortOrder, c.createdAtEpochMillis, c.updatedAtEpochMillis, c.deletedAtEpochMillis)
    }
}

@Serializable
private data class StoredEvent(
    val id: String,
    val categoryId: String,
    val title: String,
    val relatedPersonId: String? = null,
    val relatedPersonName: String? = null,
    val calendarType: String,
    val solarDate: String? = null,
    val lunarDay: Int? = null,
    val lunarMonth: Int? = null,
    val lunarLeapMonth: Boolean = false,
    val sourceYear: Int? = null,
    val recurrence: String = RecurrenceType.YEARLY.name,
    val note: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
) {
    fun toDomain() = ImportantEvent(
        id = id,
        categoryId = categoryId,
        title = title,
        relatedPersonId = relatedPersonId,
        relatedPersonName = relatedPersonName,
        calendarType = runCatching { CalendarType.valueOf(calendarType) }.getOrDefault(CalendarType.SOLAR),
        solarDate = solarDate?.let(LocalDate::parse),
        lunarDay = lunarDay,
        lunarMonth = lunarMonth,
        lunarLeapMonth = lunarLeapMonth,
        sourceYear = sourceYear,
        recurrence = runCatching { RecurrenceType.valueOf(recurrence) }.getOrDefault(RecurrenceType.NONE),
        note = note,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis
    )
    companion object {
        fun from(e: ImportantEvent) = StoredEvent(
            id = e.id,
            categoryId = e.categoryId,
            title = e.title,
            relatedPersonId = e.relatedPersonId,
            relatedPersonName = e.relatedPersonName,
            calendarType = e.calendarType.name,
            solarDate = e.solarDate?.toString(),
            lunarDay = e.lunarDay,
            lunarMonth = e.lunarMonth,
            lunarLeapMonth = e.lunarLeapMonth,
            sourceYear = e.sourceYear,
            recurrence = e.recurrence.name,
            note = e.note,
            createdAtEpochMillis = e.createdAtEpochMillis,
            updatedAtEpochMillis = e.updatedAtEpochMillis,
            deletedAtEpochMillis = e.deletedAtEpochMillis
        )
    }
}

@Serializable
private data class StoredLegacyMemorial(
    val id: String,
    val fullName: String,
    val relationship: String? = null,
    val lunarDay: Int,
    val lunarMonth: Int,
    val lunarLeapMonth: Boolean = false,
    val note: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
) {
    fun toImportantEvent(): ImportantEvent = ImportantEvent(
        id = id,
        categoryId = "cat_memorial",
        title = fullName,
        relatedPersonName = relationship,
        calendarType = CalendarType.LUNAR,
        lunarDay = lunarDay,
        lunarMonth = lunarMonth,
        lunarLeapMonth = lunarLeapMonth,
        sourceYear = null,
        recurrence = RecurrenceType.YEARLY,
        note = note,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis
    )
}

@Serializable
private data class StoredFlexibleRule(
    val id: String,
    val targetType: String,
    val targetId: String,
    val amount: Int = 0,
    val unit: String = ReminderOffsetUnit.DAY.name,
    val daysBefore: Int? = null,
    val remindHour: Int = 7,
    val remindMinute: Int = 0,
    val enabled: Boolean = true,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,
    val repeatMode: String = ReminderRepeatMode.FOLLOW_TARGET.name,
    val occurrenceDate: String? = null
) {
    fun toV3Rule(): ReminderRule {
        val mappedType = when (targetType) {
            "BIRTHDAY" -> ReminderTargetType.PERSON_BIRTHDAY
            "MEMORIAL" -> ReminderTargetType.EVENT
            else -> runCatching { ReminderTargetType.valueOf(targetType) }.getOrDefault(ReminderTargetType.EVENT)
        }
        val actualAmount = daysBefore ?: amount
        val actualUnit = runCatching { ReminderOffsetUnit.valueOf(unit) }.getOrDefault(ReminderOffsetUnit.DAY)
        return ReminderRule(
            id = id,
            targetType = mappedType,
            targetId = targetId,
            amount = actualAmount,
            unit = actualUnit,
            remindHour = remindHour,
            remindMinute = remindMinute,
            enabled = enabled,
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
            deletedAtEpochMillis = deletedAtEpochMillis,
            repeatMode = runCatching { ReminderRepeatMode.valueOf(repeatMode) }.getOrDefault(ReminderRepeatMode.FOLLOW_TARGET),
            occurrenceDate = occurrenceDate?.let(LocalDate::parse)
        )
    }
}

@Serializable
private data class StoredReminderRuleV3(
    val id: String,
    val targetType: String,
    val targetId: String,
    val amount: Int,
    val unit: String,
    val remindHour: Int = 7,
    val remindMinute: Int = 0,
    val enabled: Boolean = true,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,
    val repeatMode: String = ReminderRepeatMode.FOLLOW_TARGET.name,
    val occurrenceDate: String? = null
) {
    fun toDomain() = ReminderRule(
        id = id,
        targetType = runCatching { ReminderTargetType.valueOf(targetType) }.getOrDefault(ReminderTargetType.EVENT),
        targetId = targetId,
        amount = amount,
        unit = runCatching { ReminderOffsetUnit.valueOf(unit) }.getOrDefault(ReminderOffsetUnit.DAY),
        remindHour = remindHour,
        remindMinute = remindMinute,
        enabled = enabled,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis,
        repeatMode = runCatching { ReminderRepeatMode.valueOf(repeatMode) }.getOrDefault(ReminderRepeatMode.FOLLOW_TARGET),
        occurrenceDate = occurrenceDate?.let(LocalDate::parse)
    )
    companion object {
        fun from(r: ReminderRule) = StoredReminderRuleV3(
            id = r.id,
            targetType = r.targetType.name,
            targetId = r.targetId,
            amount = r.amount,
            unit = r.unit.name,
            remindHour = r.remindHour,
            remindMinute = r.remindMinute,
            enabled = r.enabled,
            createdAtEpochMillis = r.createdAtEpochMillis,
            updatedAtEpochMillis = r.updatedAtEpochMillis,
            deletedAtEpochMillis = r.deletedAtEpochMillis,
            repeatMode = r.repeatMode.name,
            occurrenceDate = r.occurrenceDate?.toString()
        )
    }
}

@Serializable
private data class StoredTask(
    val id: String,
    val title: String,
    val description: String? = null,
    val startAt: String? = null,
    val dueAt: String? = null,
    val recurrence: String = RecurrenceType.NONE.name,
    val status: String = TaskStatus.TODO.name,
    val priority: String = TaskPriority.NORMAL.name,
    val relatedEventId: String? = null,
    val relatedPersonId: String? = null,
    val completedAt: String? = null,
    val note: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
) {
    fun toDomain() = Task(
        id = id,
        title = title,
        description = description,
        startAt = startAt?.let(LocalDateTime::parse),
        dueAt = dueAt?.let(LocalDateTime::parse),
        recurrence = runCatching { RecurrenceType.valueOf(recurrence) }.getOrDefault(RecurrenceType.NONE),
        status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.TODO),
        priority = runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.NORMAL),
        relatedEventId = relatedEventId,
        relatedPersonId = relatedPersonId,
        completedAt = completedAt?.let(LocalDateTime::parse),
        note = note,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis
    )
    companion object {
        fun from(t: Task) = StoredTask(
            id = t.id,
            title = t.title,
            description = t.description,
            startAt = t.startAt?.toString(),
            dueAt = t.dueAt?.toString(),
            recurrence = t.recurrence.name,
            status = t.status.name,
            priority = t.priority.name,
            relatedEventId = t.relatedEventId,
            relatedPersonId = t.relatedPersonId,
            completedAt = t.completedAt?.toString(),
            note = t.note,
            createdAtEpochMillis = t.createdAtEpochMillis,
            updatedAtEpochMillis = t.updatedAtEpochMillis,
            deletedAtEpochMillis = t.deletedAtEpochMillis
        )
    }
}

@Serializable
private data class StoredChecklistItem(
    val id: String,
    val taskId: String,
    val text: String,
    val completed: Boolean = false,
    val sortOrder: Int = 0,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
) {
    fun toDomain() = ChecklistItem(
        id = id,
        taskId = taskId,
        text = text,
        completed = completed,
        sortOrder = sortOrder,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis
    )
    companion object {
        fun from(c: ChecklistItem) = StoredChecklistItem(
            id = c.id,
            taskId = c.taskId,
            text = c.text,
            completed = c.completed,
            sortOrder = c.sortOrder,
            createdAtEpochMillis = c.createdAtEpochMillis,
            updatedAtEpochMillis = c.updatedAtEpochMillis,
            deletedAtEpochMillis = c.deletedAtEpochMillis
        )
    }
}

@Serializable
private data class StoredTaskOccurrenceCompletion(
    val id: String,
    val taskId: String,
    val occurrenceDateTime: String,
    val completedAt: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null
) {
    fun toDomain() = TaskOccurrenceCompletion(
        id = id,
        taskId = taskId,
        occurrenceDateTime = LocalDateTime.parse(occurrenceDateTime),
        completedAt = LocalDateTime.parse(completedAt),
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis
    )
    companion object {
        fun from(c: TaskOccurrenceCompletion) = StoredTaskOccurrenceCompletion(
            id = c.id,
            taskId = c.taskId,
            occurrenceDateTime = c.occurrenceDateTime.toString(),
            completedAt = c.completedAt.toString(),
            createdAtEpochMillis = c.createdAtEpochMillis,
            updatedAtEpochMillis = c.updatedAtEpochMillis,
            deletedAtEpochMillis = c.deletedAtEpochMillis
        )
    }
}
