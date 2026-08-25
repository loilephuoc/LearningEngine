package vn.loi.learning.android.family

import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FamilyLegacyImportRepositoryAdapterTest {

    @Test
    fun `prepare does not mutate current snapshot`() {
        val existing = Person(
            id = "existing-person",
            fullName = "Người hiện có",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L
        )

        val current = FamilyLocalSnapshot(
            persons = listOf(existing)
        )

        val plan = singlePersonPlan()

        val prepared =
            FamilyLegacyImportRepositoryAdapter.prepare(
                current = current,
                plan = plan,
                nowEpochMillis = 1000L,
                idFactory = deterministicIds(
                    "person-new",
                    "field-1",
                    "field-2"
                )
            )

        assertSame(existing, current.persons.single())
        assertEquals(1, current.persons.size)
        assertTrue(current.personContactFields.isEmpty())

        assertNotSame(current, prepared.snapshot)
        assertEquals(2, prepared.snapshot.persons.size)
        assertEquals(2, prepared.snapshot.personContactFields.size)
    }

    @Test
    fun `prepare creates person and fields linked to generated person id`() {
        val prepared =
            FamilyLegacyImportRepositoryAdapter.prepare(
                current = FamilyLocalSnapshot(),
                plan = singlePersonPlan(),
                nowEpochMillis = 1234L,
                idFactory = deterministicIds(
                    "person-1",
                    "field-phone",
                    "field-email"
                )
            )

        val person = prepared.snapshot.persons.single()
        val fields = prepared.snapshot.personContactFields

        assertEquals("person-1", person.id)
        assertEquals("Người nhập thử", person.fullName)
        assertEquals(LocalDate.of(1990, 1, 2), person.birthDateSolar)
        assertEquals("Ghi chú", person.note)
        assertEquals(PersonGroup.FAMILY, person.group)

        assertEquals(1234L, person.createdAtEpochMillis)
        assertEquals(1234L, person.updatedAtEpochMillis)

        assertEquals(2, fields.size)
        assertTrue(fields.all { it.personId == "person-1" })

        assertEquals("field-phone", fields[0].id)
        assertEquals(PersonContactFieldType.PHONE, fields[0].type)
        assertEquals("Điện thoại", fields[0].label)
        assertEquals("0901234567", fields[0].value)
        assertTrue(fields[0].isPrimary)
        assertEquals(0, fields[0].sortOrder)

        assertEquals("field-email", fields[1].id)
        assertEquals(PersonContactFieldType.EMAIL, fields[1].type)
        assertEquals("Email", fields[1].label)
        assertEquals("person@example.com", fields[1].value)
        assertEquals(1, fields[1].sortOrder)

        assertEquals(1, prepared.peopleCreated)
        assertEquals(2, prepared.fieldsCreated)
    }

    @Test
    fun `prepare preserves all pre-existing family data`() {
        val existingPerson = Person(
            id = "existing-person",
            fullName = "Người hiện có",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L
        )

        val existingField = PersonContactField(
            id = "existing-field",
            personId = "existing-person",
            type = PersonContactFieldType.PHONE,
            label = "Di động",
            value = "0987654321",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L
        )

        val current = FamilyLocalSnapshot(
            persons = listOf(existingPerson),
            personContactFields = listOf(existingField)
        )

        val prepared =
            FamilyLegacyImportRepositoryAdapter.prepare(
                current = current,
                plan = singlePersonPlan(),
                nowEpochMillis = 2000L,
                idFactory = deterministicIds(
                    "new-person",
                    "new-phone",
                    "new-email"
                )
            )

        assertEquals(existingPerson, prepared.snapshot.persons.first())
        assertEquals(
            existingField,
            prepared.snapshot.personContactFields.first()
        )

        assertEquals(2, prepared.snapshot.persons.size)
        assertEquals(3, prepared.snapshot.personContactFields.size)
    }

    @Test
    fun `prepare rejects generated person id collision`() {
        val current = FamilyLocalSnapshot(
            persons = listOf(
                Person(
                    id = "collision",
                    fullName = "Existing",
                    createdAtEpochMillis = 1L,
                    updatedAtEpochMillis = 1L
                )
            )
        )

        assertFailsWith<IllegalArgumentException> {
            FamilyLegacyImportRepositoryAdapter.prepare(
                current = current,
                plan = singlePersonPlan(),
                nowEpochMillis = 1000L,
                idFactory = deterministicIds(
                    "collision",
                    "field-1",
                    "field-2"
                )
            )
        }
    }

    @Test
    fun `prepare rejects generated contact field id collision`() {
        val existingPerson = Person(
            id = "existing-person",
            fullName = "Existing",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L
        )

        val current = FamilyLocalSnapshot(
            persons = listOf(existingPerson),
            personContactFields = listOf(
                PersonContactField(
                    id = "field-collision",
                    personId = existingPerson.id,
                    type = PersonContactFieldType.PHONE,
                    label = "Phone",
                    value = "0987654321",
                    createdAtEpochMillis = 1L,
                    updatedAtEpochMillis = 1L
                )
            )
        )

        assertFailsWith<IllegalArgumentException> {
            FamilyLegacyImportRepositoryAdapter.prepare(
                current = current,
                plan = singlePersonPlan(),
                nowEpochMillis = 1000L,
                idFactory = deterministicIds(
                    "new-person",
                    "field-collision",
                    "field-2"
                )
            )
        }
    }

    @Test
    fun `empty plan produces same snapshot and zero creations`() {
        val current = FamilyLocalSnapshot()

        val prepared =
            FamilyLegacyImportRepositoryAdapter.prepare(
                current = current,
                plan = LegacyImportPlan(
                    entries = emptyList(),
                    skipped = emptyList(),
                    totalCandidates = 0
                )
            )

        assertSame(current, prepared.snapshot)
        assertEquals(0, prepared.peopleCreated)
        assertEquals(0, prepared.fieldsCreated)
    }

    @Test
    fun `commit uses replaceSnapshot exactly once`() = runTest {
        val repository = RecordingFamilyRepository()

        val prepared =
            FamilyLegacyImportRepositoryAdapter.commit(
                repository = repository,
                plan = singlePersonPlan(),
                nowEpochMillis = 1000L,
                idFactory = deterministicIds(
                    "person-1",
                    "field-1",
                    "field-2"
                )
            )

        assertEquals(1, repository.replaceSnapshotCalls)
        assertEquals(0, repository.upsertPersonCalls)
        assertEquals(0, repository.upsertContactFieldCalls)

        assertEquals(1, prepared.peopleCreated)
        assertEquals(2, prepared.fieldsCreated)

        assertEquals(1, repository.snapshot.value.persons.size)
        assertEquals(
            2,
            repository.snapshot.value.personContactFields.size
        )
    }

    @Test
    fun `commit performs zero repository writes for empty plan`() = runTest {
        val repository = RecordingFamilyRepository()

        FamilyLegacyImportRepositoryAdapter.commit(
            repository = repository,
            plan = LegacyImportPlan(
                entries = emptyList(),
                skipped = emptyList(),
                totalCandidates = 0
            )
        )

        assertEquals(0, repository.replaceSnapshotCalls)
        assertEquals(0, repository.upsertPersonCalls)
        assertEquals(0, repository.upsertContactFieldCalls)
    }

    @Test
    fun `preparation failure happens before replaceSnapshot`() = runTest {
        val repository = RecordingFamilyRepository(
            initial = FamilyLocalSnapshot(
                persons = listOf(
                    Person(
                        id = "collision",
                        fullName = "Existing",
                        createdAtEpochMillis = 1L,
                        updatedAtEpochMillis = 1L
                    )
                )
            )
        )

        assertFailsWith<IllegalArgumentException> {
            FamilyLegacyImportRepositoryAdapter.commit(
                repository = repository,
                plan = singlePersonPlan(),
                nowEpochMillis = 1000L,
                idFactory = deterministicIds(
                    "collision",
                    "field-1",
                    "field-2"
                )
            )
        }

        assertEquals(0, repository.replaceSnapshotCalls)
        assertEquals(0, repository.upsertPersonCalls)
        assertEquals(0, repository.upsertContactFieldCalls)
    }


    @Test
    fun `commit through SyncAwareFamilyRepository queues imported entities and triggers sync once`() = runTest {
        val dir = java.nio.file.Files.createTempDirectory("legacy-import-sync")

        val rawRepository = RecordingFamilyRepository()
        val metadata = FamilySyncMetadataStore(
            dir.resolve("family-sync-v1.json")
        )

        var syncTriggerCount = 0

        val repository = SyncAwareFamilyRepository(
            delegate = rawRepository,
            metadata = metadata
        ) {
            syncTriggerCount += 1
        }

        val prepared =
            FamilyLegacyImportRepositoryAdapter.commit(
                repository = repository,
                plan = singlePersonPlan(),
                nowEpochMillis = 1000L,
                idFactory = deterministicIds(
                    "person-imported",
                    "field-phone",
                    "field-email"
                )
            )

        assertEquals(1, rawRepository.replaceSnapshotCalls)
        assertEquals(0, rawRepository.upsertPersonCalls)
        assertEquals(0, rawRepository.upsertContactFieldCalls)

        assertEquals(1, syncTriggerCount)

        assertEquals(1, prepared.peopleCreated)
        assertEquals(2, prepared.fieldsCreated)

        val stored = repository.snapshot.value

        assertEquals(1, stored.persons.size)
        assertEquals(2, stored.personContactFields.size)
        assertEquals("person-imported", stored.persons.single().id)

        assertTrue(
            stored.personContactFields.all {
                it.personId == "person-imported"
            }
        )

        val mutations = metadata.read().mutations

        assertEquals(3, mutations.size)

        assertTrue(
            mutations.any {
                it.entityType == FamilySyncEntityType.PERSON.name &&
                        it.entityId == "person-imported"
            }
        )

        assertTrue(
            mutations.any {
                it.entityType == FamilySyncEntityType.PERSON_CONTACT_FIELD.name &&
                        it.entityId == "field-phone"
            }
        )

        assertTrue(
            mutations.any {
                it.entityType == FamilySyncEntityType.PERSON_CONTACT_FIELD.name &&
                        it.entityId == "field-email"
            }
        )
    }

    private fun singlePersonPlan(): LegacyImportPlan {
        val fields = listOf(
            LegacyImportField(
                source = LegacyColumn.PRIMARY_PHONE,
                type = PersonContactFieldType.PHONE,
                label = "Điện thoại",
                value = "0901234567",
                isPrimary = true,
                disposition = LegacyImportFieldDisposition.READY
            ),
            LegacyImportField(
                source = LegacyColumn.GMAIL,
                type = PersonContactFieldType.EMAIL,
                label = "Email",
                value = "person@example.com",
                disposition = LegacyImportFieldDisposition.READY
            )
        )

        val candidate = LegacyImportCandidate(
            rowIndex = 2,
            fullName = "Người nhập thử",
            birthDateSolar = LocalDate.of(1990, 1, 2),
            note = "Ghi chú",
            fields = fields,
            issues = emptyList(),
            duplicate = null,
            status = LegacyDuplicateStatus.NEW
        )

        return LegacyImportPlan(
            entries = listOf(
                LegacyImportPlanEntry(
                    rowIndex = 2,
                    candidate = candidate,
                    acceptedFields = fields
                )
            ),
            skipped = emptyList(),
            totalCandidates = 1
        )
    }

    private fun deterministicIds(
        vararg ids: String
    ): () -> String {
        val iterator = ids.iterator()

        return {
            check(iterator.hasNext()) {
                "Test idFactory exhausted"
            }
            iterator.next()
        }
    }

    private class RecordingFamilyRepository(
        initial: FamilyLocalSnapshot = FamilyLocalSnapshot()
    ) : FamilyRepository {

        private val mutableSnapshot = MutableStateFlow(initial)

        override val snapshot: StateFlow<FamilyLocalSnapshot>
            get() = mutableSnapshot

        var replaceSnapshotCalls: Int = 0
        var upsertPersonCalls: Int = 0
        var upsertContactFieldCalls: Int = 0

        override suspend fun reload(): FamilyLocalSnapshot {
            return mutableSnapshot.value
        }

        override suspend fun replaceSnapshot(
            snapshot: FamilyLocalSnapshot
        ) {
            replaceSnapshotCalls += 1
            mutableSnapshot.value = snapshot
        }

        override suspend fun upsertPerson(person: Person) {
            upsertPersonCalls += 1
            error("upsertPerson must not be used by atomic importer")
        }

        override suspend fun upsertPersonContactField(
            field: PersonContactField
        ) {
            upsertContactFieldCalls += 1
            error(
                "upsertPersonContactField must not be used by atomic importer"
            )
        }

        override suspend fun deletePerson(
            id: String,
            atEpochMillis: Long
        ) = unsupported()

        override suspend fun deletePersonContactField(
            id: String,
            atEpochMillis: Long
        ) = unsupported()

        override suspend fun setPersonContactFieldsForPerson(
            personId: String,
            fields: List<PersonContactField>
        ) = unsupported()

        override suspend fun upsertCategory(
            category: EventCategory
        ) = unsupported()

        override suspend fun deleteCategory(
            id: String,
            atEpochMillis: Long
        ) = unsupported()

        override suspend fun upsertEvent(
            event: ImportantEvent
        ) = unsupported()

        override suspend fun deleteEvent(
            id: String,
            atEpochMillis: Long
        ) = unsupported()

        override suspend fun upsertReminderRule(
            rule: ReminderRule
        ) = unsupported()

        override suspend fun deleteReminderRule(
            id: String
        ) = unsupported()

        override suspend fun setReminderRulesForTarget(
            targetType: ReminderTargetType,
            targetId: String,
            rules: List<ReminderRule>
        ) = unsupported()

        override suspend fun upsertTask(
            task: Task
        ) = unsupported()

        override suspend fun deleteTask(
            id: String,
            atEpochMillis: Long
        ) = unsupported()

        override suspend fun upsertChecklistItem(
            item: ChecklistItem
        ) = unsupported()

        override suspend fun deleteChecklistItem(
            id: String,
            atEpochMillis: Long
        ) = unsupported()

        override suspend fun setChecklistItemsForTask(
            taskId: String,
            items: List<ChecklistItem>
        ) = unsupported()

        override suspend fun completeTaskOccurrence(
            completion: TaskOccurrenceCompletion
        ) = unsupported()

        override suspend fun undoTaskOccurrenceCompletion(
            completionId: String,
            atEpochMillis: Long
        ) = unsupported()

        private fun unsupported(): Nothing =
            error("Unexpected repository method")
    }




}
