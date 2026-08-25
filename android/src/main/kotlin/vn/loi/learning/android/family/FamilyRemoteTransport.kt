package vn.loi.learning.android.family

import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.json.*
import vn.loi.learning.infrastructure.sync.supabase.*

interface FamilyRemoteTransport {
    fun upsert(ownerUserId: String, mutation: FamilySyncMutation, snapshot: FamilyLocalSnapshot)
    fun pull(ownerUserId: String): FamilyRemotePull
}

data class FamilyRemotePull(val snapshot: FamilyLocalSnapshot, val malformedRows: Int = 0)

class SupabaseFamilyRemoteTransport(
    private val configuration: SupabaseConfiguration,
    private val sessions: SupabaseSessionProvider,
    private val http: SupabaseHttpClient = UrlConnectionSupabaseHttpClient()
) : FamilyRemoteTransport {
    override fun upsert(ownerUserId: String, mutation: FamilySyncMutation, snapshot: FamilyLocalSnapshot) {
        val row = FamilyRemoteMapper.toRow(mutation.key, ownerUserId, snapshot)
            ?: throw FamilySyncException("SYNC_LOCAL_RECORD_MISSING", false)
        request(
            "POST",
            "/rest/v1/${mutation.key.type.table}?on_conflict=owner_user_id,id",
            JsonArray(listOf(row)).toString().encodeToByteArray(),
            mapOf("Prefer" to "resolution=merge-duplicates,return=minimal")
        )
    }

    override fun pull(ownerUserId: String): FamilyRemotePull {
        val builder = RemoteSnapshotBuilder()
        FamilySyncEntityType.entries.forEach { type ->
            val response = request("GET", "/rest/v1/${type.table}?select=*&owner_user_id=eq.$ownerUserId")
            val rows = runCatching { Json.parseToJsonElement(response.decodeToString()).jsonArray }
                .getOrElse { throw FamilySyncException("SYNC_REMOTE_RESPONSE_MALFORMED", false, it) }
            rows.forEach { element ->
                runCatching { FamilyRemoteMapper.read(type, element.jsonObject, builder) }
                    .onFailure { builder.malformedRows++ }
            }
        }
        return FamilyRemotePull(builder.build(), builder.malformedRows)
    }

    private fun request(method: String, path: String, body: ByteArray? = null, extraHeaders: Map<String, String> = emptyMap()): ByteArray {
        val session = sessions.currentSession() ?: throw FamilySyncException("SYNC_AUTHENTICATION_REQUIRED", false)
        val response = try {
            http.execute(SupabaseHttpRequest(method, URI(configuration.baseUri.toString() + path), buildMap {
                put("apikey", configuration.publishableKey)
                put("Authorization", "Bearer ${session.accessToken}")
                put("Content-Type", "application/json")
                putAll(extraHeaders)
            }, body, configuration.requestTimeoutMillis))
        } catch (failure: Exception) {
            throw FamilySyncException("SYNC_NETWORK_ERROR", true, failure)
        }
        if (response.status !in 200..299) throw FamilySyncException(
            when (response.status) { 401 -> "SYNC_AUTHENTICATION_REQUIRED"; 403 -> "SYNC_RLS_REJECTED"; else -> "SYNC_REMOTE_HTTP_${response.status}" },
            response.status == 408 || response.status == 429 || response.status >= 500
        )
        return response.body
    }
}

class FamilySyncException(val code: String, val retryable: Boolean, cause: Throwable? = null) : RuntimeException(code, cause)

internal class RemoteSnapshotBuilder {
    val persons = mutableListOf<Person>(); val personContactFields = mutableListOf<PersonContactField>(); val categories = mutableListOf<EventCategory>(); val events = mutableListOf<ImportantEvent>()
    val rules = mutableListOf<ReminderRule>(); val tasks = mutableListOf<Task>(); val checklist = mutableListOf<ChecklistItem>()
    val completions = mutableListOf<TaskOccurrenceCompletion>(); var malformedRows = 0
    fun build(): FamilyLocalSnapshot {
        val compatibleFields = personContactFields.toMutableList()
        persons.forEach { person ->
            if (compatibleFields.none { it.personId == person.id && it.type == PersonContactFieldType.PHONE } && !person.phone.isNullOrBlank()) {
                compatibleFields += PersonContactField(
                    "${person.id}:legacy-phone", person.id, PersonContactFieldType.PHONE, "Di động", person.phone, true, 0,
                    person.createdAtEpochMillis, person.updatedAtEpochMillis, person.deletedAtEpochMillis
                )
            }
            if (compatibleFields.none { it.personId == person.id && it.type == PersonContactFieldType.ADDRESS } && !person.address.isNullOrBlank()) {
                compatibleFields += PersonContactField(
                    "${person.id}:legacy-address", person.id, PersonContactFieldType.ADDRESS, "Nhà", person.address, true, 100,
                    person.createdAtEpochMillis, person.updatedAtEpochMillis, person.deletedAtEpochMillis
                )
            }
        }
        return FamilyLocalSnapshot(5, persons, DEFAULT_EVENT_CATEGORIES.mergeById(categories) { it.id }, events, rules, tasks, checklist, completions, compatibleFields)
    }
}

object FamilyRemoteMapper {
    fun toRow(key: FamilySyncEntityKey, owner: String, snapshot: FamilyLocalSnapshot): JsonObject? = when (key.type) {
        FamilySyncEntityType.PERSON -> snapshot.persons.find { it.id == key.id }?.let { p -> base(p.id, owner, p.createdAtEpochMillis, p.updatedAtEpochMillis, p.deletedAtEpochMillis) {
            put("full_name", p.fullName); nullable("nickname", p.nickname); put("group_type", p.group.name); nullable("relationship_label", p.relationshipLabel)
            nullable("birth_date_solar", p.birthDateSolar?.toString()); nullable("phone", p.phone); nullable("address", p.address); nullable("note", p.note); nullable("avatar_ref", p.avatarRef)
        } }
        FamilySyncEntityType.PERSON_CONTACT_FIELD -> snapshot.personContactFields.find { it.id == key.id }?.let { f -> base(f.id, owner, f.createdAtEpochMillis, f.updatedAtEpochMillis, f.deletedAtEpochMillis) {
            put("person_id", f.personId); put("field_type", f.type.name); nullable("label", f.label); put("field_value", f.value); put("is_primary", f.isPrimary); put("sort_order", f.sortOrder)
        } }
        FamilySyncEntityType.CATEGORY -> snapshot.categories.find { it.id == key.id }?.let { c -> base(c.id, owner, c.createdAtEpochMillis, c.updatedAtEpochMillis, c.deletedAtEpochMillis) {
            put("name", c.name); nullable("built_in_key", c.builtInKey); put("built_in", c.builtIn); nullable("icon_key", c.iconKey); put("sort_order", c.sortOrder)
        } }
        FamilySyncEntityType.EVENT -> snapshot.events.find { it.id == key.id }?.let { e -> base(e.id, owner, e.createdAtEpochMillis, e.updatedAtEpochMillis, e.deletedAtEpochMillis) {
            put("category_id", e.categoryId); put("title", e.title); nullable("related_person_id", e.relatedPersonId); nullable("related_person_name", e.relatedPersonName)
            put("calendar_type", e.calendarType.name); nullable("solar_date", e.solarDate?.toString()); nullable("lunar_day", e.lunarDay); nullable("lunar_month", e.lunarMonth)
            put("lunar_leap_month", e.lunarLeapMonth); nullable("source_year", e.sourceYear); put("recurrence", e.recurrence.name); nullable("note", e.note)
        } }
        FamilySyncEntityType.REMINDER_RULE -> snapshot.reminderRules.find { it.id == key.id }?.let { r -> base(r.id, owner, r.createdAtEpochMillis, r.updatedAtEpochMillis, r.deletedAtEpochMillis) {
            put("target_type", r.targetType.name); put("target_id", r.targetId); put("amount", r.amount); put("offset_unit", r.unit.name); put("remind_hour", r.remindHour)
            put("remind_minute", r.remindMinute); put("enabled", r.enabled); put("repeat_mode", r.repeatMode.name); nullable("occurrence_date", r.occurrenceDate?.toString())
        } }
        FamilySyncEntityType.TASK -> snapshot.tasks.find { it.id == key.id }?.let { t -> base(t.id, owner, t.createdAtEpochMillis, t.updatedAtEpochMillis, t.deletedAtEpochMillis) {
            put("title", t.title); nullable("description", t.description); nullable("start_at", t.startAt?.toString()); nullable("due_at", t.dueAt?.toString()); put("recurrence", t.recurrence.name)
            put("status", t.status.name); put("priority", t.priority.name); nullable("related_person_id", t.relatedPersonId); nullable("related_event_id", t.relatedEventId)
            nullable("completed_at", t.completedAt?.toString()); nullable("note", t.note)
        } }
        FamilySyncEntityType.CHECKLIST_ITEM -> snapshot.checklistItems.find { it.id == key.id }?.let { c -> base(c.id, owner, c.createdAtEpochMillis, c.updatedAtEpochMillis, c.deletedAtEpochMillis) {
            put("task_id", c.taskId); put("item_text", c.text); put("completed", c.completed); put("sort_order", c.sortOrder)
        } }
        FamilySyncEntityType.TASK_COMPLETION -> snapshot.taskOccurrenceCompletions.find { it.id == key.id }?.let { c -> base(c.id, owner, c.createdAtEpochMillis, c.updatedAtEpochMillis, c.deletedAtEpochMillis) {
            put("task_id", c.taskId); put("occurrence_datetime", c.occurrenceDateTime.toString()); put("completed_at", c.completedAt.toString())
        } }
    }

    internal fun read(type: FamilySyncEntityType, row: JsonObject, b: RemoteSnapshotBuilder) {
        val id = row.str("id"); val created = row.long("created_at_epoch_millis"); val updated = row.long("updated_at_epoch_millis"); val deleted = row.longOrNull("deleted_at_epoch_millis")
        when (type) {
            FamilySyncEntityType.PERSON -> b.persons += Person(id, row.str("full_name"), row.strOrNull("nickname"), PersonGroup.valueOf(row.str("group_type")), row.strOrNull("relationship_label"), row.strOrNull("birth_date_solar")?.let(LocalDate::parse), row.strOrNull("phone"), row.strOrNull("address"), row.strOrNull("note"), row.strOrNull("avatar_ref"), created, updated, deleted)
            FamilySyncEntityType.PERSON_CONTACT_FIELD -> b.personContactFields += PersonContactField(id, row.str("person_id"), PersonContactFieldType.valueOf(row.str("field_type")), row.strOrNull("label"), row.str("field_value"), row.bool("is_primary"), row.int("sort_order"), created, updated, deleted)
            FamilySyncEntityType.CATEGORY -> b.categories += EventCategory(id, row.str("name"), row.strOrNull("built_in_key"), row.bool("built_in"), row.strOrNull("icon_key"), row.int("sort_order"), created, updated, deleted)
            FamilySyncEntityType.EVENT -> b.events += ImportantEvent(id, row.str("category_id"), row.str("title"), row.strOrNull("related_person_id"), row.strOrNull("related_person_name"), CalendarType.valueOf(row.str("calendar_type")), row.strOrNull("solar_date")?.let(LocalDate::parse), row.intOrNull("lunar_day"), row.intOrNull("lunar_month"), row.bool("lunar_leap_month"), row.intOrNull("source_year"), RecurrenceType.valueOf(row.str("recurrence")), row.strOrNull("note"), created, updated, deleted)
            FamilySyncEntityType.REMINDER_RULE -> b.rules += ReminderRule(id, ReminderTargetType.valueOf(row.str("target_type")), row.str("target_id"), row.int("amount"), ReminderOffsetUnit.valueOf(row.str("offset_unit")), row.int("remind_hour"), row.int("remind_minute"), row.bool("enabled"), created, updated, deleted, ReminderRepeatMode.valueOf(row.str("repeat_mode")), row.strOrNull("occurrence_date")?.let(LocalDate::parse))
            FamilySyncEntityType.TASK -> b.tasks += Task(id, row.str("title"), row.strOrNull("description"), row.strOrNull("start_at")?.let(LocalDateTime::parse), row.strOrNull("due_at")?.let(LocalDateTime::parse), RecurrenceType.valueOf(row.str("recurrence")), TaskStatus.valueOf(row.str("status")), TaskPriority.valueOf(row.str("priority")), row.strOrNull("related_event_id"), row.strOrNull("related_person_id"), row.strOrNull("completed_at")?.let(LocalDateTime::parse), row.strOrNull("note"), created, updated, deleted)
            FamilySyncEntityType.CHECKLIST_ITEM -> b.checklist += ChecklistItem(id, row.str("task_id"), row.str("item_text"), row.bool("completed"), row.int("sort_order"), created, updated, deleted)
            FamilySyncEntityType.TASK_COMPLETION -> b.completions += TaskOccurrenceCompletion(id, row.str("task_id"), LocalDateTime.parse(row.str("occurrence_datetime")), LocalDateTime.parse(row.str("completed_at")), created, updated, deleted)
        }
    }

    private fun base(id: String, owner: String, created: Long, updated: Long, deleted: Long?, fields: JsonObjectBuilder.() -> Unit) = buildJsonObject {
        put("id", id); put("owner_user_id", owner); put("created_at_epoch_millis", created); put("updated_at_epoch_millis", updated); nullable("deleted_at_epoch_millis", deleted); fields()
    }
}

private fun JsonObjectBuilder.nullable(name: String, value: String?) { if (value == null) put(name, JsonNull) else put(name, value) }
private fun JsonObjectBuilder.nullable(name: String, value: Int?) { if (value == null) put(name, JsonNull) else put(name, value) }
private fun JsonObjectBuilder.nullable(name: String, value: Long?) { if (value == null) put(name, JsonNull) else put(name, value) }
private fun JsonObject.str(name: String) = getValue(name).jsonPrimitive.content
private fun JsonObject.strOrNull(name: String) = get(name)?.jsonPrimitive?.contentOrNull
private fun JsonObject.long(name: String) = getValue(name).jsonPrimitive.long
private fun JsonObject.longOrNull(name: String) = get(name)?.jsonPrimitive?.longOrNull
private fun JsonObject.int(name: String) = getValue(name).jsonPrimitive.int
private fun JsonObject.intOrNull(name: String) = get(name)?.jsonPrimitive?.intOrNull
private fun JsonObject.bool(name: String) = getValue(name).jsonPrimitive.boolean
private fun <T> List<T>.mergeById(other: List<T>, id: (T) -> String): List<T> = (this + other).associateBy(id).values.toList()
