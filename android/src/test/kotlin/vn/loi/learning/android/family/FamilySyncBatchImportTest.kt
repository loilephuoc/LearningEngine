package vn.loi.learning.android.family

import java.nio.file.Files
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FamilySyncBatchImportTest {

    @Test
    fun `enqueueAll queues imported persons and fields`() {
        val dir = Files.createTempDirectory("family-sync-batch")
        val metadata =
            FamilySyncMetadataStore(
                dir.resolve("family-sync-v1.json")
            )

        metadata.enqueueAll(
            listOf(
                FamilySyncEntityKey(
                    FamilySyncEntityType.PERSON,
                    "person-1"
                ) to 100L,
                FamilySyncEntityKey(
                    FamilySyncEntityType.PERSON_CONTACT_FIELD,
                    "field-1"
                ) to 101L,
                FamilySyncEntityKey(
                    FamilySyncEntityType.PERSON_CONTACT_FIELD,
                    "field-2"
                ) to 102L
            )
        )

        val stored = metadata.read()

        assertEquals(3, stored.mutations.size)

        assertTrue(
            stored.mutations.any {
                it.entityType == FamilySyncEntityType.PERSON.name &&
                    it.entityId == "person-1" &&
                    it.localUpdatedAtEpochMillis == 100L
            }
        )

        assertTrue(
            stored.mutations.any {
                it.entityType ==
                    FamilySyncEntityType.PERSON_CONTACT_FIELD.name &&
                    it.entityId == "field-1" &&
                    it.localUpdatedAtEpochMillis == 101L
            }
        )

        assertTrue(
            stored.mutations.any {
                it.entityType ==
                    FamilySyncEntityType.PERSON_CONTACT_FIELD.name &&
                    it.entityId == "field-2" &&
                    it.localUpdatedAtEpochMillis == 102L
            }
        )
    }

    @Test
    fun `enqueueAll replaces duplicate mutation keys instead of duplicating`() {
        val dir = Files.createTempDirectory("family-sync-batch")
        val metadata =
            FamilySyncMetadataStore(
                dir.resolve("family-sync-v1.json")
            )

        metadata.enqueue(
            FamilySyncEntityKey(
                FamilySyncEntityType.PERSON,
                "person-1"
            ),
            updatedAt = 10L
        )

        metadata.enqueueAll(
            listOf(
                FamilySyncEntityKey(
                    FamilySyncEntityType.PERSON,
                    "person-1"
                ) to 20L
            )
        )

        val stored = metadata.read()

        assertEquals(1, stored.mutations.size)
        assertEquals("person-1", stored.mutations.single().entityId)
        assertEquals(
            20L,
            stored.mutations.single().localUpdatedAtEpochMillis
        )
    }

    @Test
    fun `replaceSnapshotAndQueueImportedPeople queues batch and triggers once`() =
        runTest {
            val dir = Files.createTempDirectory("family-sync-batch")

            val delegate = RecordingRepository()
            val metadata =
                FamilySyncMetadataStore(
                    dir.resolve("family-sync-v1.json")
                )

            var triggerCount = 0

            val repository =
                SyncAwareFamilyRepository(
                    delegate = delegate,
                    metadata = metadata
                ) {
                    triggerCount += 1
                }

            val person = Person(
                id = "person-1",
                fullName = "Người nhập thử",
                createdAtEpochMillis = 100L,
                updatedAtEpochMillis = 100L
            )

            val field = PersonContactField(
                id = "field-1",
                personId = person.id,
                type = PersonContactFieldType.PHONE,
                label = "Điện thoại",
                value = "0901234567",
                createdAtEpochMillis = 100L,
                updatedAtEpochMillis = 100L
            )

            val next =
                FamilyLocalSnapshot(
                    persons = listOf(person),
                    personContactFields = listOf(field)
                )

            repository.replaceSnapshotAndQueueImportedPeople(
                snapshot = next,
                importedPersons = listOf(person),
                importedFields = listOf(field)
            )

            assertEquals(1, delegate.replaceSnapshotCalls)
            assertEquals(next, delegate.snapshot.value)

            assertEquals(1, triggerCount)

            val queued = metadata.read().mutations

            assertEquals(2, queued.size)

            assertTrue(
                queued.any {
                    it.entityType ==
                        FamilySyncEntityType.PERSON.name &&
                        it.entityId == person.id
                }
            )

            assertTrue(
                queued.any {
                    it.entityType ==
                        FamilySyncEntityType.PERSON_CONTACT_FIELD.name &&
                        it.entityId == field.id
                }
            )
        }

    @Test
    fun `generic replaceSnapshot keeps original no-sync semantics`() =
        runTest {
            val dir = Files.createTempDirectory("family-sync-batch")

            val delegate = RecordingRepository()
            val metadata =
                FamilySyncMetadataStore(
                    dir.resolve("family-sync-v1.json")
                )

            var triggerCount = 0

            val repository =
                SyncAwareFamilyRepository(
                    delegate = delegate,
                    metadata = metadata
                ) {
                    triggerCount += 1
                }

            val next =
                FamilyLocalSnapshot(
                    persons = listOf(
                        Person(
                            id = "person-1",
                            fullName = "Người thử nghiệm",
                            createdAtEpochMillis = 100L,
                            updatedAtEpochMillis = 100L
                        )
                    )
                )

            repository.replaceSnapshot(next)

            assertEquals(1, delegate.replaceSnapshotCalls)
            assertEquals(0, triggerCount)
            assertTrue(metadata.read().mutations.isEmpty())
        }

    @Test
    fun `delegate snapshot failure queues nothing and triggers nothing`() =
        runTest {
            val dir = Files.createTempDirectory("family-sync-batch")

            val delegate =
                RecordingRepository(
                    failReplaceSnapshot = true
                )

            val metadata =
                FamilySyncMetadataStore(
                    dir.resolve("family-sync-v1.json")
                )

            var triggerCount = 0

            val repository =
                SyncAwareFamilyRepository(
                    delegate = delegate,
                    metadata = metadata
                ) {
                    triggerCount += 1
                }

            val person = Person(
                id = "person-1",
                fullName = "Người thử nghiệm",
                createdAtEpochMillis = 100L,
                updatedAtEpochMillis = 100L
            )

            assertFailsWith<IllegalStateException> {
                repository.replaceSnapshotAndQueueImportedPeople(
                    snapshot =
                        FamilyLocalSnapshot(
                            persons = listOf(person)
                        ),
                    importedPersons = listOf(person),
                    importedFields = emptyList()
                )
            }

            assertEquals(1, delegate.replaceSnapshotCalls)
            assertEquals(0, triggerCount)
            assertTrue(metadata.read().mutations.isEmpty())
        }

    @Test
    fun `import batch rejects fields outside imported person set`() =
        runTest {
            val dir = Files.createTempDirectory("family-sync-batch")

            val delegate = RecordingRepository()

            val metadata =
                FamilySyncMetadataStore(
                    dir.resolve("family-sync-v1.json")
                )

            var triggerCount = 0

            val repository =
                SyncAwareFamilyRepository(
                    delegate = delegate,
                    metadata = metadata
                ) {
                    triggerCount += 1
                }

            val person = Person(
                id = "person-1",
                fullName = "Người thử nghiệm",
                createdAtEpochMillis = 100L,
                updatedAtEpochMillis = 100L
            )

            val invalidField =
                PersonContactField(
                    id = "field-1",
                    personId = "different-person",
                    type = PersonContactFieldType.CUSTOM,
                    label = "Mã thử nghiệm",
                    value = "ABC123",
                    createdAtEpochMillis = 100L,
                    updatedAtEpochMillis = 100L
                )

            assertFailsWith<IllegalArgumentException> {
                repository.replaceSnapshotAndQueueImportedPeople(
                    snapshot =
                        FamilyLocalSnapshot(
                            persons = listOf(person),
                            personContactFields =
                                listOf(invalidField)
                        ),
                    importedPersons = listOf(person),
                    importedFields = listOf(invalidField)
                )
            }

            assertEquals(0, delegate.replaceSnapshotCalls)
            assertEquals(0, triggerCount)
            assertTrue(metadata.read().mutations.isEmpty())
        }

    private class RecordingRepository(
        initial: FamilyLocalSnapshot = FamilyLocalSnapshot(),
        private val failReplaceSnapshot: Boolean = false
    ) : FamilyRepository {

        private val mutableSnapshot =
            MutableStateFlow(initial)

        override val snapshot: StateFlow<FamilyLocalSnapshot>
            get() = mutableSnapshot

        var replaceSnapshotCalls: Int = 0

        override suspend fun reload(): FamilyLocalSnapshot =
            mutableSnapshot.value

        override suspend fun replaceSnapshot(
            snapshot: FamilyLocalSnapshot
        ) {
            replaceSnapshotCalls += 1

            if (failReplaceSnapshot) {
                throw IllegalStateException(
                    "synthetic replaceSnapshot failure"
                )
            }

            mutableSnapshot.value = snapshot
        }

        override suspend fun upsertPerson(
            person: Person
        ) = unsupported()

        override suspend fun deletePerson(
            id: String,
            atEpochMillis: Long
        ) = unsupported()

        override suspend fun upsertPersonContactField(
            field: PersonContactField
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
