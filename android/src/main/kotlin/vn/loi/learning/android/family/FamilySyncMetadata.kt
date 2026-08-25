package vn.loi.learning.android.family

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class FamilySyncEntityType(val table: String) {
    PERSON("family_persons"),
    PERSON_CONTACT_FIELD("family_person_contact_fields"),
    CATEGORY("family_event_categories"),
    EVENT("family_events"),
    REMINDER_RULE("family_reminder_rules"),
    TASK("family_tasks"),
    CHECKLIST_ITEM("family_checklist_items"),
    TASK_COMPLETION("family_task_occurrence_completions")
}

data class FamilySyncEntityKey(
    val type: FamilySyncEntityType,
    val id: String
)

@Serializable
data class FamilySyncMutation(
    val entityType: String,
    val entityId: String,
    val localUpdatedAtEpochMillis: Long,
    val queuedAtEpochMillis: Long
) {
    val key: FamilySyncEntityKey
        get() = FamilySyncEntityKey(
            FamilySyncEntityType.valueOf(entityType),
            entityId
        )
}

@Serializable
private data class StoredFamilySyncMetadata(
    val version: Int = 1,
    val ownerUserId: String? = null,
    val lastSuccessfulSyncAtEpochMillis: Long? = null,
    val mutations: List<FamilySyncMutation> = emptyList()
)

data class FamilySyncMetadata(
    val ownerUserId: String?,
    val lastSuccessfulSyncAtEpochMillis: Long?,
    val mutations: List<FamilySyncMutation>
)

class FamilySyncMetadataStore(
    private val file: Path
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Synchronized
    fun read(): FamilySyncMetadata =
        load().let {
            FamilySyncMetadata(
                ownerUserId = it.ownerUserId,
                lastSuccessfulSyncAtEpochMillis =
                    it.lastSuccessfulSyncAtEpochMillis,
                mutations = it.mutations
            )
        }

    @Synchronized
    fun bindOwnerIfAllowed(
        userId: String,
        localHasData: Boolean
    ) {
        val current = load()

        if (
            current.ownerUserId != null &&
            current.ownerUserId != userId &&
            localHasData
        ) {
            throw FamilyAccountMismatchException()
        }

        if (current.ownerUserId != userId) {
            save(
                current.copy(
                    ownerUserId = userId
                )
            )
        }
    }

    @Synchronized
    fun enqueue(
        key: FamilySyncEntityKey,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        val current = load()

        val mutation = FamilySyncMutation(
            entityType = key.type.name,
            entityId = key.id,
            localUpdatedAtEpochMillis = updatedAt,
            queuedAtEpochMillis = System.currentTimeMillis()
        )

        save(
            current.copy(
                mutations =
                    current.mutations.filterNot {
                        it.entityType == mutation.entityType &&
                                it.entityId == mutation.entityId
                    } + mutation
            )
        )
    }

    /**
     * Batch version of enqueue().
     *
     * Loads and persists sync metadata only once for the complete batch.
     * Existing pending mutations for the same entity keys are replaced
     * by the newest incoming mutation.
     */
    @Synchronized
    fun enqueueAll(
        mutations: List<Pair<FamilySyncEntityKey, Long>>
    ) {
        if (mutations.isEmpty()) return

        val current = load()
        val queuedAt = System.currentTimeMillis()

        val incoming = mutations.map { (key, updatedAt) ->
            FamilySyncMutation(
                entityType = key.type.name,
                entityId = key.id,
                localUpdatedAtEpochMillis = updatedAt,
                queuedAtEpochMillis = queuedAt
            )
        }

        val incomingKeys =
            incoming.mapTo(hashSetOf()) {
                it.entityType to it.entityId
            }

        save(
            current.copy(
                mutations =
                    current.mutations.filterNot {
                        (it.entityType to it.entityId) in incomingKeys
                    } + incoming
            )
        )
    }

    @Synchronized
    fun acknowledge(
        key: FamilySyncEntityKey
    ) {
        val current = load()

        save(
            current.copy(
                mutations =
                    current.mutations.filterNot {
                        it.key == key
                    }
            )
        )
    }

    @Synchronized
    fun markSuccessful(
        at: Long = System.currentTimeMillis()
    ) {
        val current = load()

        save(
            current.copy(
                lastSuccessfulSyncAtEpochMillis = at
            )
        )
    }

    private fun load(): StoredFamilySyncMetadata {
        if (!Files.exists(file)) {
            return StoredFamilySyncMetadata()
        }

        return Files.newBufferedReader(
            file,
            StandardCharsets.UTF_8
        ).use {
            json.decodeFromString(it.readText())
        }
    }

    private fun save(
        value: StoredFamilySyncMetadata
    ) {
        Files.createDirectories(file.parent)

        val temporary =
            file.resolveSibling("${file.fileName}.tmp")

        Files.newBufferedWriter(
            temporary,
            StandardCharsets.UTF_8
        ).use {
            it.write(json.encodeToString(value))
        }

        runCatching {
            Files.move(
                temporary,
                file,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        }.getOrElse {
            Files.move(
                temporary,
                file,
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}

class FamilyAccountMismatchException :
    IllegalStateException(
        "Thiết bị đang có dữ liệu của tài khoản khác."
    )

class SyncAwareFamilyRepository(
    private val delegate: FamilyRepository,
    private val metadata: FamilySyncMetadataStore,
    private val onMutation: () -> Unit
) : FamilyRepository {

    override val snapshot: StateFlow<FamilyLocalSnapshot> =
        delegate.snapshot

    override suspend fun reload() =
        delegate.reload()

    /**
     * Keep the original semantics unchanged.
     *
     * Generic snapshot replacement does NOT automatically create
     * sync mutations.
     */
    override suspend fun replaceSnapshot(
        snapshot: FamilyLocalSnapshot
    ) =
        delegate.replaceSnapshot(snapshot)

    /**
     * Import-specific atomic local snapshot replacement followed by
     * one batched sync-metadata update.
     *
     * Only the newly imported Person and PersonContactField entities
     * are queued for the existing FAMILY sync engine.
     *
     * This deliberately does NOT change the semantics of the generic
     * replaceSnapshot() method.
     */
    internal suspend fun replaceSnapshotAndQueueImportedPeople(
        snapshot: FamilyLocalSnapshot,
        importedPersons: List<Person>,
        importedFields: List<PersonContactField>
    ) {
        if (
            importedPersons.isEmpty() &&
            importedFields.isEmpty()
        ) {
            return
        }

        val personIds =
            importedPersons.mapTo(hashSetOf()) {
                it.id
            }

        require(
            importedFields.all {
                it.personId in personIds
            }
        ) {
            "Imported contact field references a person outside the import batch"
        }

        /*
         * Persist the complete FAMILY snapshot once.
         *
         * If this throws, no sync mutations are queued.
         */
        delegate.replaceSnapshot(snapshot)

        val mutations =
            buildList<Pair<FamilySyncEntityKey, Long>> {
                importedPersons.forEach { person ->
                    add(
                        FamilySyncEntityKey(
                            type = FamilySyncEntityType.PERSON,
                            id = person.id
                        ) to person.updatedAtEpochMillis
                    )
                }

                importedFields.forEach { field ->
                    add(
                        FamilySyncEntityKey(
                            type =
                                FamilySyncEntityType.PERSON_CONTACT_FIELD,
                            id = field.id
                        ) to field.updatedAtEpochMillis
                    )
                }
            }

        /*
         * Persist the whole sync queue update once instead of doing
         * one metadata file write per imported entity.
         */
        metadata.enqueueAll(mutations)

        /*
         * Schedule/trigger the existing sync mechanism once for the
         * complete import batch.
         */
        onMutation()
    }

    private fun changed(
        type: FamilySyncEntityType,
        id: String,
        updatedAt: Long
    ) {
        metadata.enqueue(
            FamilySyncEntityKey(
                type = type,
                id = id
            ),
            updatedAt
        )

        onMutation()
    }

    override suspend fun upsertPerson(
        person: Person
    ) {
        delegate.upsertPerson(person)

        changed(
            FamilySyncEntityType.PERSON,
            person.id,
            person.updatedAtEpochMillis
        )
    }

    override suspend fun deletePerson(
        id: String,
        atEpochMillis: Long
    ) {
        val fieldIds =
            snapshot.value.personContactFields
                .filter {
                    it.personId == id &&
                            it.deletedAtEpochMillis == null
                }
                .map {
                    it.id
                }

        delegate.deletePerson(
            id,
            atEpochMillis
        )

        changed(
            FamilySyncEntityType.PERSON,
            id,
            atEpochMillis
        )

        fieldIds.forEach { fieldId ->
            changed(
                FamilySyncEntityType.PERSON_CONTACT_FIELD,
                fieldId,
                atEpochMillis
            )
        }
    }

    override suspend fun upsertPersonContactField(
        field: PersonContactField
    ) {
        delegate.upsertPersonContactField(field)

        changed(
            FamilySyncEntityType.PERSON_CONTACT_FIELD,
            field.id,
            field.updatedAtEpochMillis
        )
    }

    override suspend fun deletePersonContactField(
        id: String,
        atEpochMillis: Long
    ) {
        delegate.deletePersonContactField(
            id,
            atEpochMillis
        )

        changed(
            FamilySyncEntityType.PERSON_CONTACT_FIELD,
            id,
            atEpochMillis
        )
    }

    override suspend fun setPersonContactFieldsForPerson(
        personId: String,
        fields: List<PersonContactField>
    ) {
        val before =
            snapshot.value.personContactFields
                .filter {
                    it.personId == personId
                }
                .map {
                    it.id
                }
                .toSet()

        delegate.setPersonContactFieldsForPerson(
            personId,
            fields
        )

        val now = System.currentTimeMillis()

        (before + fields.map { it.id }).forEach { id ->
            val updatedAt =
                snapshot.value.personContactFields
                    .firstOrNull {
                        it.id == id
                    }
                    ?.updatedAtEpochMillis
                    ?: now

            changed(
                FamilySyncEntityType.PERSON_CONTACT_FIELD,
                id,
                updatedAt
            )
        }
    }

    override suspend fun upsertCategory(
        category: EventCategory
    ) {
        delegate.upsertCategory(category)

        if (!category.builtIn) {
            changed(
                FamilySyncEntityType.CATEGORY,
                category.id,
                category.updatedAtEpochMillis
            )
        }
    }

    override suspend fun deleteCategory(
        id: String,
        atEpochMillis: Long
    ) {
        delegate.deleteCategory(
            id,
            atEpochMillis
        )

        changed(
            FamilySyncEntityType.CATEGORY,
            id,
            atEpochMillis
        )
    }

    override suspend fun upsertEvent(
        event: ImportantEvent
    ) {
        delegate.upsertEvent(event)

        changed(
            FamilySyncEntityType.EVENT,
            event.id,
            event.updatedAtEpochMillis
        )
    }

    override suspend fun deleteEvent(
        id: String,
        atEpochMillis: Long
    ) {
        delegate.deleteEvent(
            id,
            atEpochMillis
        )

        changed(
            FamilySyncEntityType.EVENT,
            id,
            atEpochMillis
        )
    }

    override suspend fun upsertReminderRule(
        rule: ReminderRule
    ) {
        delegate.upsertReminderRule(rule)

        changed(
            FamilySyncEntityType.REMINDER_RULE,
            rule.id,
            rule.updatedAtEpochMillis
        )
    }

    override suspend fun deleteReminderRule(
        id: String
    ) {
        delegate.deleteReminderRule(id)

        changed(
            FamilySyncEntityType.REMINDER_RULE,
            id,
            System.currentTimeMillis()
        )
    }

    override suspend fun setReminderRulesForTarget(
        targetType: ReminderTargetType,
        targetId: String,
        rules: List<ReminderRule>
    ) {
        val before =
            snapshot.value.reminderRules
                .filter {
                    it.targetType == targetType &&
                            it.targetId == targetId
                }
                .map {
                    it.id
                }
                .toSet()

        delegate.setReminderRulesForTarget(
            targetType,
            targetId,
            rules
        )

        (before + rules.map { it.id }).forEach { id ->
            changed(
                FamilySyncEntityType.REMINDER_RULE,
                id,
                System.currentTimeMillis()
            )
        }
    }

    override suspend fun upsertTask(
        task: Task
    ) {
        delegate.upsertTask(task)

        changed(
            FamilySyncEntityType.TASK,
            task.id,
            task.updatedAtEpochMillis
        )
    }

    override suspend fun deleteTask(
        id: String,
        atEpochMillis: Long
    ) {
        delegate.deleteTask(
            id,
            atEpochMillis
        )

        changed(
            FamilySyncEntityType.TASK,
            id,
            atEpochMillis
        )
    }

    override suspend fun upsertChecklistItem(
        item: ChecklistItem
    ) {
        delegate.upsertChecklistItem(item)

        changed(
            FamilySyncEntityType.CHECKLIST_ITEM,
            item.id,
            item.updatedAtEpochMillis
        )
    }

    override suspend fun deleteChecklistItem(
        id: String,
        atEpochMillis: Long
    ) {
        delegate.deleteChecklistItem(
            id,
            atEpochMillis
        )

        changed(
            FamilySyncEntityType.CHECKLIST_ITEM,
            id,
            atEpochMillis
        )
    }

    override suspend fun setChecklistItemsForTask(
        taskId: String,
        items: List<ChecklistItem>
    ) {
        val before =
            snapshot.value.checklistItems
                .filter {
                    it.taskId == taskId
                }
                .map {
                    it.id
                }
                .toSet()

        delegate.setChecklistItemsForTask(
            taskId,
            items
        )

        (before + items.map { it.id }).forEach { id ->
            changed(
                FamilySyncEntityType.CHECKLIST_ITEM,
                id,
                System.currentTimeMillis()
            )
        }
    }

    override suspend fun completeTaskOccurrence(
        completion: TaskOccurrenceCompletion
    ) {
        delegate.completeTaskOccurrence(completion)

        changed(
            FamilySyncEntityType.TASK_COMPLETION,
            completion.id,
            completion.updatedAtEpochMillis
        )
    }

    override suspend fun undoTaskOccurrenceCompletion(
        completionId: String,
        atEpochMillis: Long
    ) {
        delegate.undoTaskOccurrenceCompletion(
            completionId,
            atEpochMillis
        )

        changed(
            FamilySyncEntityType.TASK_COMPLETION,
            completionId,
            atEpochMillis
        )
    }
}

fun FamilyLocalSnapshot.hasUserData(): Boolean =
    persons.isNotEmpty() ||
            personContactFields.isNotEmpty() ||
            events.isNotEmpty() ||
            tasks.isNotEmpty() ||
            reminderRules.isNotEmpty() ||
            checklistItems.isNotEmpty() ||
            taskOccurrenceCompletions.isNotEmpty() ||
            categories.any {
                !it.builtIn
            }