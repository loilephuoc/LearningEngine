package vn.loi.learning.android.family

import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import org.junit.Test
import vn.loi.learning.domain.sync.protocol.SyncAccountId
import vn.loi.learning.infrastructure.sync.supabase.SupabaseSession
import vn.loi.learning.infrastructure.sync.supabase.SupabaseSessionProvider

class FamilySyncEngineTest {
    @Test fun `remote mapper roundtrips every FAMILY entity and nullable recurrence fields`() {
        val source = completeSnapshot()
        val builder = RemoteSnapshotBuilder()
        val keys = listOf(
            FamilySyncEntityKey(FamilySyncEntityType.PERSON, "p"), FamilySyncEntityKey(FamilySyncEntityType.PERSON_CONTACT_FIELD, "pf"), FamilySyncEntityKey(FamilySyncEntityType.CATEGORY, "custom"),
            FamilySyncEntityKey(FamilySyncEntityType.EVENT, "e"), FamilySyncEntityKey(FamilySyncEntityType.REMINDER_RULE, "r"),
            FamilySyncEntityKey(FamilySyncEntityType.TASK, "t"), FamilySyncEntityKey(FamilySyncEntityType.CHECKLIST_ITEM, "i"),
            FamilySyncEntityKey(FamilySyncEntityType.TASK_COMPLETION, "c")
        )
        keys.forEach { key -> FamilyRemoteMapper.read(key.type, FamilyRemoteMapper.toRow(key, USER_A, source)!!, builder) }
        val result = builder.build()
        assertEquals(source.persons.single(), result.persons.single())
        assertEquals(source.personContactFields.single(), result.personContactFields.single())
        assertEquals(source.events.single(), result.events.single())
        assertEquals(source.reminderRules.single(), result.reminderRules.single())
        assertEquals(ReminderRepeatMode.ONCE, result.reminderRules.single().repeatMode)
        assertEquals(LocalDate.of(2027, 9, 5), result.reminderRules.single().occurrenceDate)
        assertEquals(source.tasks.single(), result.tasks.single())
        assertEquals(source.checklistItems.single(), result.checklistItems.single())
        assertEquals(source.taskOccurrenceCompletions.single(), result.taskOccurrenceCompletions.single())
    }

    @Test fun `merge keeps independent records and remote tombstone defeats older active copy`() {
        val localPerson = person("local", "Local")
        val active = task("shared", "Old active")
        val remoteEvent = event("remote", "Remote")
        val tombstone = active.copy(title = "Deleted", updatedAtEpochMillis = 20, deletedAtEpochMillis = 20)
        val merged = FamilySnapshotMerger.merge(
            FamilyLocalSnapshot(persons = listOf(localPerson), tasks = listOf(active)),
            FamilyLocalSnapshot(events = listOf(remoteEvent), tasks = listOf(tombstone)),
            setOf(FamilySyncEntityKey(FamilySyncEntityType.TASK, "shared"))
        )
        assertEquals("Local", merged.persons.single().fullName)
        assertEquals("Remote", merged.events.single().title)
        assertEquals(20, merged.tasks.single().deletedAtEpochMillis)
    }

    @Test fun `pending local edit wins non-deleted remote row`() {
        val key = FamilySyncEntityKey(FamilySyncEntityType.EVENT, "same")
        val merged = FamilySnapshotMerger.merge(
            FamilyLocalSnapshot(events = listOf(event("same", "Local edit"))),
            FamilyLocalSnapshot(events = listOf(event("same", "Remote older"))),
            setOf(key)
        )
        assertEquals("Local edit", merged.events.single().title)
    }

    @Test fun `durable outbox coalesces and survives restart`() {
        val file = Files.createTempDirectory("family-sync-meta").resolve("outbox.json")
        val first = FamilySyncMetadataStore(file)
        val key = FamilySyncEntityKey(FamilySyncEntityType.EVENT, "e")
        first.enqueue(key, 1); first.enqueue(key, 2)
        val restored = FamilySyncMetadataStore(file).read()
        assertEquals(1, restored.mutations.size)
        assertEquals(2, restored.mutations.single().localUpdatedAtEpochMillis)
    }

    @Test fun `local data and empty remote uploads without clearing local and repeated sync is idempotent`() = runTest {
        val fixture = fixture(FamilyLocalSnapshot(persons = listOf(person("p", "Existing local"))))
        fixture.metadata.enqueue(FamilySyncEntityKey(FamilySyncEntityType.PERSON, "p"), 1)
        fixture.engine.sync(); fixture.engine.sync()
        assertEquals(listOf("p"), fixture.remote.upserts.map { it.entityId })
        assertEquals("Existing local", fixture.repository.snapshot.value.persons.single().fullName)
        assertTrue(fixture.metadata.read().mutations.isEmpty())
    }

    @Test fun `empty local downloads remote and triggers reminder reconciliation`() = runTest {
        val remote = FakeRemote(FamilyLocalSnapshot(events = listOf(event("e", "Cloud event"))))
        val fixture = fixture(FamilyLocalSnapshot(), remote)
        fixture.engine.sync()
        assertEquals("Cloud event", fixture.repository.snapshot.value.events.single().title)
        assertEquals(1, fixture.afterMergeCount())
    }

    @Test fun `failed upload remains pending and local repository remains usable`() = runTest {
        val remote = FakeRemote().apply { fail = FamilySyncEntityKey(FamilySyncEntityType.TASK, "t") }
        val fixture = fixture(FamilyLocalSnapshot(tasks = listOf(task("t", "Offline task"))), remote)
        fixture.metadata.enqueue(remote.fail!!, 1)
        assertIs<FamilySyncState.Offline>(fixture.engine.sync())
        assertEquals(1, fixture.metadata.read().mutations.size)
        assertEquals("Offline task", fixture.repository.snapshot.value.tasks.single().title)
    }

    @Test fun `account ownership guard blocks cross-account upload`() = runTest {
        val fixture = fixture(FamilyLocalSnapshot(persons = listOf(person("p", "Private"))))
        fixture.metadata.bindOwnerIfAllowed(USER_A, true)
        val engineB = FamilySyncEngine(fixture.repository, fixture.metadata, session(USER_B), fixture.remote)
        assertIs<FamilySyncState.AccountMismatch>(engineB.sync())
        assertTrue(fixture.remote.upserts.isEmpty())
    }

    @Test fun `sync aware repository persists local first then enqueues mutation`() = runTest {
        val dir = Files.createTempDirectory("family-aware")
        val metadata = FamilySyncMetadataStore(dir.resolve("sync.json"))
        var scheduled = 0
        val repository = SyncAwareFamilyRepository(JsonFamilyRepository(dir.resolve("family.json")), metadata) { scheduled++ }
        repository.upsertTask(task("t", "Immediate local"))
        assertEquals("Immediate local", JsonFamilyRepository(dir.resolve("family.json")).snapshot.value.tasks.single().title)
        assertEquals(FamilySyncEntityKey(FamilySyncEntityType.TASK, "t"), metadata.read().mutations.single().key)
        assertEquals(1, scheduled)
    }

    private data class Fixture(val repository: FamilyRepository, val metadata: FamilySyncMetadataStore, val remote: FakeRemote, val engine: FamilySyncEngine, val afterMergeCount: () -> Int)
    private fun fixture(local: FamilyLocalSnapshot, remote: FakeRemote = FakeRemote()): Fixture {
        val dir = Files.createTempDirectory("family-sync-engine")
        val repository = JsonFamilyRepository(dir.resolve("family.json"))
        runBlocking { repository.replaceSnapshot(local) }
        val metadata = FamilySyncMetadataStore(dir.resolve("sync.json"))
        var reconciles = 0
        val engine = FamilySyncEngine(repository, metadata, session(USER_A), remote) { reconciles++ }
        return Fixture(repository, metadata, remote, engine) { reconciles }
    }

    private class FakeRemote(var remoteSnapshot: FamilyLocalSnapshot = FamilyLocalSnapshot()) : FamilyRemoteTransport {
        val upserts = mutableListOf<FamilySyncMutation>(); var fail: FamilySyncEntityKey? = null
        override fun upsert(ownerUserId: String, mutation: FamilySyncMutation, snapshot: FamilyLocalSnapshot) {
            if (mutation.key == fail) throw FamilySyncException("SYNC_NETWORK_ERROR", true)
            upserts += mutation
        }
        override fun pull(ownerUserId: String) = FamilyRemotePull(remoteSnapshot)
    }

    private fun session(user: String) = SupabaseSessionProvider { SupabaseSession(SyncAccountId(user), "public-test-token", expiresAtEpochSeconds = Long.MAX_VALUE, userEmail = "test@example.com") }
    private fun person(id: String, name: String) = Person(id, name, birthDateSolar = LocalDate.of(1990, 1, 2), createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
    private fun event(id: String, title: String) = ImportantEvent(id, "cat_other", title, calendarType = CalendarType.SOLAR, solarDate = LocalDate.of(2027, 9, 5), recurrence = RecurrenceType.YEARLY, createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
    private fun task(id: String, title: String) = Task(id, title, dueAt = LocalDateTime.of(2027, 9, 5, 8, 0), recurrence = RecurrenceType.MONTHLY, createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
    private fun completeSnapshot(): FamilyLocalSnapshot {
        val rule = ReminderRule("r", ReminderTargetType.EVENT, "e", 2, ReminderOffsetUnit.WEEK, 9, 30, true, 1, 2, null, ReminderRepeatMode.ONCE, LocalDate.of(2027, 9, 5))
        return FamilyLocalSnapshot(
            persons = listOf(person("p", "An").copy(nickname = null, deletedAtEpochMillis = 9)),
            categories = DEFAULT_EVENT_CATEGORIES + EventCategory("custom", "Riêng", iconKey = "★", createdAtEpochMillis = 1, updatedAtEpochMillis = 2),
            events = listOf(ImportantEvent("e", "custom", "Âm lịch", relatedPersonId = "p", calendarType = CalendarType.LUNAR, lunarDay = 24, lunarMonth = 7, lunarLeapMonth = false, recurrence = RecurrenceType.YEARLY, createdAtEpochMillis = 1, updatedAtEpochMillis = 2)),
            reminderRules = listOf(rule), tasks = listOf(task("t", "Task").copy(description = null, priority = TaskPriority.HIGH)),
            checklistItems = listOf(ChecklistItem("i", "t", "Mục", true, 1, 1, 2)),
            taskOccurrenceCompletions = listOf(TaskOccurrenceCompletion("c", "t", LocalDateTime.of(2027, 9, 5, 8, 0), LocalDateTime.of(2027, 9, 4, 7, 0), 1, 2)),
            personContactFields = listOf(PersonContactField("pf", "p", PersonContactFieldType.CUSTOM, "Mã hồ sơ", "HS-001", false, 0, 1, 2))
        )
    }

    @Test fun `independent person contact fields merge without overwriting sibling field`() {
        val localPhone = PersonContactField("phone", "p", PersonContactFieldType.PHONE, "Di động", "0901", true, 0, 1, 3)
        val localEmail = PersonContactField("email", "p", PersonContactFieldType.EMAIL, "Cá nhân", "local@example.com", true, 1, 1, 3)
        val remotePhone = localPhone.copy(value = "0902", updatedAtEpochMillis = 4)
        val remoteEmail = localEmail.copy(value = "remote@example.com", updatedAtEpochMillis = 4)
        val merged = FamilySnapshotMerger.merge(
            FamilyLocalSnapshot(personContactFields = listOf(localPhone, localEmail)),
            FamilyLocalSnapshot(personContactFields = listOf(remotePhone, remoteEmail)),
            setOf(FamilySyncEntityKey(FamilySyncEntityType.PERSON_CONTACT_FIELD, "phone"))
        )
        assertEquals("0901", merged.personContactFields.single { it.id == "phone" }.value)
        assertEquals("remote@example.com", merged.personContactFields.single { it.id == "email" }.value)
    }

    companion object { private const val USER_A = "00000000-0000-0000-0000-000000000001"; private const val USER_B = "00000000-0000-0000-0000-000000000002" }
}
