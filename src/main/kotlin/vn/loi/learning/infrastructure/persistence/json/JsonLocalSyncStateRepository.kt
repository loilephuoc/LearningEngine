package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.application.sync.LocalSyncStateRepository
import vn.loi.learning.domain.sync.protocol.*

class JsonLocalSyncStateRepository(
    private val filePath: Path,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = false; encodeDefaults = true }
) : LocalSyncStateRepository {
    override fun enqueue(change: OutboundSyncChange) = update { current ->
        val record = change.toRecord()
        val existing = current.outbox.firstOrNull { it.accountId == record.accountId && it.eventId == record.eventId }
        require(existing == null || existing == record) { "Outbox event ID cannot identify different changes." }
        if (existing == null) current.copy(outbox = current.outbox + record) else current
    }

    override fun pendingOutbox(accountId: SyncAccountId): List<OutboundSyncChange> =
        load().outbox.filter { it.accountId == accountId.value }.map(SyncChangeRecord::toDomain)

    override fun acknowledgeOutbox(accountId: SyncAccountId, eventIds: Set<SyncEventId>) = update { current ->
        val ids = eventIds.mapTo(hashSetOf(), SyncEventId::value)
        current.copy(outbox = current.outbox.filterNot { it.accountId == accountId.value && it.eventId in ids })
    }

    override fun hasApplied(accountId: SyncAccountId, eventId: SyncEventId): Boolean =
        load().inbox.any { it.accountId == accountId.value && it.eventId == eventId.value }

    override fun recordApplied(accountId: SyncAccountId, eventId: SyncEventId, cursor: SyncCursor) = update { current ->
        val currentCursor = current.cursors[accountId.value] ?: 0L
        require(cursor.value >= currentCursor) { "Applied cursor must not move backwards." }
        val inbox = InboxRecord(accountId.value, eventId.value)
        current.copy(
            inbox = if (inbox in current.inbox) current.inbox else current.inbox + inbox,
            cursors = current.cursors + (accountId.value to cursor.value)
        )
    }

    override fun cursor(accountId: SyncAccountId): SyncCursor =
        SyncCursor(load().cursors[accountId.value] ?: 0L)

    fun validate() { load() }

    private fun load(): LocalSyncStateRecord = JsonFileReader.read(
        filePath, LocalSyncStateRecord(), "local sync state", "LocalSyncState"
    ) { json.decodeFromString<LocalSyncStateRecord>(it).validated() }

    private fun update(transform: (LocalSyncStateRecord) -> LocalSyncStateRecord) {
        JsonFileWriter.write(filePath, json.encodeToString(transform(load())))
    }
}

@Serializable
private data class LocalSyncStateRecord(
    val schemaVersion: Int = 1,
    val outbox: List<SyncChangeRecord> = emptyList(),
    val inbox: List<InboxRecord> = emptyList(),
    val cursors: Map<String, Long> = emptyMap()
) {
    fun validated(): LocalSyncStateRecord {
        require(schemaVersion == 1) { "Unsupported local sync state schema version: $schemaVersion" }
        require(cursors.values.all { it >= 0L }) { "Local sync cursors must not be negative." }
        require(outbox.map { it.accountId to it.eventId }.distinct().size == outbox.size) {
            "Duplicate local sync outbox event identity."
        }
        require(inbox.distinct().size == inbox.size) { "Duplicate local sync inbox event identity." }
        outbox.forEach { it.toDomain() }
        return this
    }
}

@Serializable
private data class InboxRecord(val accountId: String, val eventId: String)

@Serializable
private data class SyncChangeRecord(
    val accountId: String, val eventId: String, val idempotencyKey: String,
    val sourceDeviceId: String, val entityId: String, val payloadVersion: Int,
    val namespace: String, val kind: String, val operation: String? = null,
    val field: String? = null, val customFieldId: String? = null, val value: String? = null,
    val mediaReference: String? = null, val sha256: String? = null, val sizeBytes: Long? = null,
    val mimeType: String? = null, val baseRevision: Long? = null,
    val reviewEventId: String? = null, val learningItemId: String? = null,
    val learnerId: String? = null, val payload: String? = null
) {
    fun toDomain(): OutboundSyncChange = OutboundSyncChange(
        SyncAccountId(accountId), SyncEventId(eventId), IdempotencyKey(idempotencyKey),
        SyncDeviceId(sourceDeviceId), SyncEntityId(entityId), payloadVersion,
        when (kind) {
            "CONTENT_FIELD" -> ContentFieldDelta(
                ContentField.valueOf(requireNotNull(field)), DeltaOperation.valueOf(requireNotNull(operation)),
                value, customFieldId, baseRevision?.let(::SyncRevision)
            )
            "MEDIA" -> MediaDelta(
                MediaSlot.valueOf(requireNotNull(field)), DeltaOperation.valueOf(requireNotNull(operation)),
                mediaReference, sha256, sizeBytes, mimeType, baseRevision?.let(::SyncRevision)
            )
            "REVIEW_EVENT" -> ReviewEventDelta(
                requireNotNull(reviewEventId), requireNotNull(learningItemId),
                requireNotNull(learnerId), requireNotNull(payload)
            )
            else -> error("Unsupported sync delta kind: $kind")
        }
    )
}

private fun OutboundSyncChange.toRecord(): SyncChangeRecord = when (val payload = delta) {
    is ContentFieldDelta -> SyncChangeRecord(
        accountId.value, eventId.value, idempotencyKey.value, sourceDeviceId.value, entityId.value,
        payloadVersion, payload.namespace.name, "CONTENT_FIELD", payload.operation.name,
        payload.field.name, payload.customFieldId, payload.value, baseRevision = payload.baseRevision?.value
    )
    is MediaDelta -> SyncChangeRecord(
        accountId.value, eventId.value, idempotencyKey.value, sourceDeviceId.value, entityId.value,
        payloadVersion, payload.namespace.name, "MEDIA", payload.operation.name, payload.slot.name,
        mediaReference = payload.mediaReference, sha256 = payload.sha256, sizeBytes = payload.sizeBytes,
        mimeType = payload.mimeType, baseRevision = payload.baseRevision?.value
    )
    is ReviewEventDelta -> SyncChangeRecord(
        accountId.value, eventId.value, idempotencyKey.value, sourceDeviceId.value, entityId.value,
        payloadVersion, payload.namespace.name, "REVIEW_EVENT", reviewEventId = payload.reviewEventId,
        learningItemId = payload.learningItemId, learnerId = payload.learnerId, payload = payload.payload
    )
}
