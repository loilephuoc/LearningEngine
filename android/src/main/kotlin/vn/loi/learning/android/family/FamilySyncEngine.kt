package vn.loi.learning.android.family

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import vn.loi.learning.infrastructure.sync.supabase.SupabaseSessionProvider

sealed interface FamilySyncState {
    data object NotConfigured : FamilySyncState
    data object SignedOut : FamilySyncState
    data class Idle(val email: String?, val pendingCount: Int, val lastSuccessfulSyncAtEpochMillis: Long?) : FamilySyncState
    data class Syncing(val email: String?, val pendingCount: Int) : FamilySyncState
    data class Offline(val email: String?, val pendingCount: Int) : FamilySyncState
    data class Failed(val email: String?, val pendingCount: Int, val code: String) : FamilySyncState
    data class AccountMismatch(val email: String?) : FamilySyncState
}

class FamilySyncEngine(
    private val repository: FamilyRepository,
    private val metadata: FamilySyncMetadataStore,
    private val sessions: SupabaseSessionProvider,
    private val remote: FamilyRemoteTransport,
    private val afterMerge: () -> Unit = {}
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<FamilySyncState>(FamilySyncState.SignedOut)
    val state: StateFlow<FamilySyncState> = mutableState

    fun refreshState() {
        val session = runCatching { sessions.currentSession() }.getOrNull()
        val meta = metadata.read()
        mutableState.value = if (session == null) FamilySyncState.SignedOut else FamilySyncState.Idle(session.userEmail, meta.mutations.size, meta.lastSuccessfulSyncAtEpochMillis)
    }

    suspend fun sync(): FamilySyncState = mutex.withLock {
        withContext(Dispatchers.IO) {
            val session = runCatching { sessions.currentSession() }.getOrNull()
                ?: return@withContext FamilySyncState.SignedOut.also { mutableState.value = it }
            val owner = session.accountId.value
            val initialMetadata = metadata.read()
            try {
                metadata.bindOwnerIfAllowed(owner, repository.snapshot.value.hasUserData())
            } catch (_: FamilyAccountMismatchException) {
                return@withContext FamilySyncState.AccountMismatch(session.userEmail).also { mutableState.value = it }
            }
            if (initialMetadata.ownerUserId == null && initialMetadata.mutations.isEmpty()) enqueueInitialSnapshot(repository.snapshot.value)

            mutableState.value = FamilySyncState.Syncing(session.userEmail, metadata.read().mutations.size)
            var retryableFailure = false
            var failureCode: String? = null
            metadata.read().mutations.forEach { mutation ->
                try {
                    remote.upsert(owner, mutation, repository.snapshot.value)
                    metadata.acknowledge(mutation.key)
                } catch (failure: FamilySyncException) {
                    retryableFailure = retryableFailure || failure.retryable
                    failureCode = failure.code
                }
            }

            val pull = try { remote.pull(owner) }
            catch (failure: FamilySyncException) {
                val pending = metadata.read().mutations.size
                val result = if (failure.retryable || retryableFailure) FamilySyncState.Offline(session.userEmail, pending)
                    else FamilySyncState.Failed(session.userEmail, pending, failure.code)
                return@withContext result.also { mutableState.value = it }
            }
            val pending = metadata.read().mutations.mapTo(hashSetOf()) { it.key }
            repository.replaceSnapshot(FamilySnapshotMerger.merge(repository.snapshot.value, pull.snapshot, pending))
            afterMerge()
            metadata.markSuccessful()
            val remaining = metadata.read().mutations.size
            val result = when {
                pull.malformedRows > 0 -> FamilySyncState.Failed(session.userEmail, remaining, "SYNC_REMOTE_ROWS_MALFORMED")
                failureCode != null -> if (retryableFailure) FamilySyncState.Offline(session.userEmail, remaining) else FamilySyncState.Failed(session.userEmail, remaining, failureCode!!)
                else -> FamilySyncState.Idle(session.userEmail, remaining, metadata.read().lastSuccessfulSyncAtEpochMillis)
            }
            mutableState.value = result
            result
        }
    }

    private fun enqueueInitialSnapshot(snapshot: FamilyLocalSnapshot) {
        snapshot.persons.forEach { metadata.enqueue(FamilySyncEntityKey(FamilySyncEntityType.PERSON, it.id), it.updatedAtEpochMillis) }
        snapshot.personContactFields.forEach { metadata.enqueue(FamilySyncEntityKey(FamilySyncEntityType.PERSON_CONTACT_FIELD, it.id), it.updatedAtEpochMillis) }
        snapshot.categories.filter { !it.builtIn }.forEach { metadata.enqueue(FamilySyncEntityKey(FamilySyncEntityType.CATEGORY, it.id), it.updatedAtEpochMillis) }
        snapshot.events.forEach { metadata.enqueue(FamilySyncEntityKey(FamilySyncEntityType.EVENT, it.id), it.updatedAtEpochMillis) }
        snapshot.reminderRules.forEach { metadata.enqueue(FamilySyncEntityKey(FamilySyncEntityType.REMINDER_RULE, it.id), it.updatedAtEpochMillis) }
        snapshot.tasks.forEach { metadata.enqueue(FamilySyncEntityKey(FamilySyncEntityType.TASK, it.id), it.updatedAtEpochMillis) }
        snapshot.checklistItems.forEach { metadata.enqueue(FamilySyncEntityKey(FamilySyncEntityType.CHECKLIST_ITEM, it.id), it.updatedAtEpochMillis) }
        snapshot.taskOccurrenceCompletions.forEach { metadata.enqueue(FamilySyncEntityKey(FamilySyncEntityType.TASK_COMPLETION, it.id), it.updatedAtEpochMillis) }
    }
}

object FamilySnapshotMerger {
    fun merge(local: FamilyLocalSnapshot, remote: FamilyLocalSnapshot, pending: Set<FamilySyncEntityKey>): FamilyLocalSnapshot = FamilyLocalSnapshot(
        schemaVersion = 5,
        persons = merge(local.persons, remote.persons, FamilySyncEntityType.PERSON, pending, Person::id, Person::deletedAtEpochMillis),
        categories = DEFAULT_EVENT_CATEGORIES.mergeById(merge(local.categories.filter { !it.builtIn }, remote.categories.filter { !it.builtIn }, FamilySyncEntityType.CATEGORY, pending, EventCategory::id, EventCategory::deletedAtEpochMillis), EventCategory::id),
        events = merge(local.events, remote.events, FamilySyncEntityType.EVENT, pending, ImportantEvent::id, ImportantEvent::deletedAtEpochMillis),
        reminderRules = merge(local.reminderRules, remote.reminderRules, FamilySyncEntityType.REMINDER_RULE, pending, ReminderRule::id, ReminderRule::deletedAtEpochMillis),
        tasks = merge(local.tasks, remote.tasks, FamilySyncEntityType.TASK, pending, Task::id, Task::deletedAtEpochMillis),
        checklistItems = merge(local.checklistItems, remote.checklistItems, FamilySyncEntityType.CHECKLIST_ITEM, pending, ChecklistItem::id, ChecklistItem::deletedAtEpochMillis),
        taskOccurrenceCompletions = merge(local.taskOccurrenceCompletions, remote.taskOccurrenceCompletions, FamilySyncEntityType.TASK_COMPLETION, pending, TaskOccurrenceCompletion::id, TaskOccurrenceCompletion::deletedAtEpochMillis),
        personContactFields = merge(local.personContactFields, remote.personContactFields, FamilySyncEntityType.PERSON_CONTACT_FIELD, pending, PersonContactField::id, PersonContactField::deletedAtEpochMillis)
    )

    private fun <T> merge(local: List<T>, remote: List<T>, type: FamilySyncEntityType, pending: Set<FamilySyncEntityKey>, id: (T) -> String, deleted: (T) -> Long?): List<T> {
        val localMap = local.associateBy(id); val remoteMap = remote.associateBy(id)
        return (localMap.keys + remoteMap.keys).sorted().mapNotNull { recordId ->
            val l = localMap[recordId]; val r = remoteMap[recordId]
            when {
                l == null -> r
                r == null -> l
                deleted(r) != null && deleted(l) == null -> r
                FamilySyncEntityKey(type, recordId) in pending -> l
                else -> r
            }
        }
    }
}

private fun <T> List<T>.mergeById(other: List<T>, id: (T) -> String): List<T> = (this + other).associateBy(id).values.toList()
