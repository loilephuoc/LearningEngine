package vn.loi.learning.infrastructure.persistence.json

import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.application.sync.LocalSyncStateRepository
import vn.loi.learning.application.sync.SyncQuarantineRecord
import vn.loi.learning.application.sync.PendingMediaApply
import vn.loi.learning.application.sync.MediaGcCandidate
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

    override fun recordQuarantine(record: SyncQuarantineRecord) = update { current ->
        val persisted = record.toRecord()
        val existing = current.quarantines.firstOrNull {
            it.accountId == persisted.accountId && it.eventId == persisted.eventId
        }
        require(existing == null || existing == persisted) {
            "Quarantined sync event ID cannot identify different failures."
        }
        if (existing == null) current.copy(quarantines = current.quarantines + persisted) else current
    }

    override fun quarantines(accountId: SyncAccountId): List<SyncQuarantineRecord> =
        load().quarantines.filter { it.accountId == accountId.value }.map(QuarantineRecord::toDomain)

    override fun recordPendingMedia(record: PendingMediaApply) = update { current ->
        val persisted = record.toRecord()
        val existing = current.pendingMedia.firstOrNull {
            it.accountId == persisted.accountId && it.eventId == persisted.eventId
        }
        require(existing == null || existing == persisted) { "Pending media event cannot change identity." }
        if (existing == null) current.copy(pendingMedia = current.pendingMedia + persisted) else current
    }

    override fun removePendingMedia(accountId: SyncAccountId, eventId: SyncEventId) = update { current ->
        current.copy(pendingMedia = current.pendingMedia.filterNot {
            it.accountId == accountId.value && it.eventId == eventId.value
        })
    }

    override fun pendingMedia(accountId: SyncAccountId): List<PendingMediaApply> =
        load().pendingMedia.filter { it.accountId == accountId.value }.map(PendingMediaRecord::toDomain)

    override fun recordMediaGcCandidate(candidate: MediaGcCandidate) = update { current ->
        val record = MediaGcRecord(candidate.reference, candidate.sha256)
        val existing = current.mediaGcCandidates.firstOrNull { it.reference == record.reference }
        require(existing == null || existing == record) { "Media GC reference cannot change identity." }
        if (existing == null) current.copy(mediaGcCandidates = current.mediaGcCandidates + record) else current
    }

    override fun removeMediaGcCandidate(reference: String) = update { current ->
        current.copy(mediaGcCandidates = current.mediaGcCandidates.filterNot { it.reference == reference })
    }

    override fun mediaGcCandidates(): List<MediaGcCandidate> =
        load().mediaGcCandidates.map { MediaGcCandidate(it.reference, it.sha256) }

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
    val cursors: Map<String, Long> = emptyMap(),
    val quarantines: List<QuarantineRecord> = emptyList(),
    val pendingMedia: List<PendingMediaRecord> = emptyList(),
    val mediaGcCandidates: List<MediaGcRecord> = emptyList()
) {
    fun validated(): LocalSyncStateRecord {
        require(schemaVersion == 1) { "Unsupported local sync state schema version: $schemaVersion" }
        require(cursors.values.all { it >= 0L }) { "Local sync cursors must not be negative." }
        require(outbox.map { it.accountId to it.eventId }.distinct().size == outbox.size) {
            "Duplicate local sync outbox event identity."
        }
        require(inbox.distinct().size == inbox.size) { "Duplicate local sync inbox event identity." }
        require(quarantines.map { it.accountId to it.eventId }.distinct().size == quarantines.size) {
            "Duplicate quarantined sync event identity."
        }
        require(pendingMedia.map { it.accountId to it.eventId }.distinct().size == pendingMedia.size) {
            "Duplicate pending media event identity."
        }
        require(mediaGcCandidates.map { it.reference }.distinct().size == mediaGcCandidates.size) {
            "Duplicate media GC reference."
        }
        outbox.forEach { it.toDomain() }
        return this
    }
}

@Serializable
private data class InboxRecord(val accountId: String, val eventId: String)

@Serializable
private data class QuarantineRecord(
    val accountId: String,
    val eventId: String,
    val reviewEventId: String,
    val learningItemId: String,
    val remoteRevision: Long,
    val payloadVersion: Int,
    val code: String,
    val reason: String,
    val contentId: String? = null,
    val mediaSha256: String? = null
) {
    fun toDomain() = SyncQuarantineRecord(
        SyncAccountId(accountId), SyncEventId(eventId), reviewEventId, learningItemId,
        remoteRevision, payloadVersion, code, reason, contentId, mediaSha256
    )
}

private fun SyncQuarantineRecord.toRecord() = QuarantineRecord(
    accountId.value, eventId.value, reviewEventId, learningItemId,
    remoteRevision, payloadVersion, code, reason, contentId, mediaSha256
)

@Serializable
private data class PendingMediaRecord(
    val accountId: String, val eventId: String, val contentId: String, val slot: String,
    val sha256: String, val sizeBytes: Long, val mimeType: String,
    val remoteRevision: Long, val payloadVersion: Int
) {
    fun toDomain() = PendingMediaApply(
        SyncAccountId(accountId), SyncEventId(eventId), contentId, slot, sha256,
        sizeBytes, mimeType, remoteRevision, payloadVersion
    )
}

private fun PendingMediaApply.toRecord() = PendingMediaRecord(
    accountId.value, eventId.value, contentId, slot, sha256, sizeBytes,
    mimeType, remoteRevision, payloadVersion
)

@Serializable
private data class MediaGcRecord(val reference: String, val sha256: String)

@Serializable
private data class SyncChangeRecord(
    val accountId: String, val eventId: String, val idempotencyKey: String,
    val sourceDeviceId: String, val entityId: String, val payloadVersion: Int,
    val namespace: String, val kind: String, val operation: String? = null,
    val field: String? = null, val customFieldId: String? = null, val value: String? = null,
    val mediaReference: String? = null, val sha256: String? = null, val sizeBytes: Long? = null,
    val mimeType: String? = null, val baseRevision: Long? = null,
    val previousSha256: String? = null,
    val reviewEventId: String? = null, val learningItemId: String? = null,
    val learnerId: String? = null, val payload: String? = null,
    val contentId: String? = null, val rating: String? = null,
    val reviewedAtEpochMillis: Long? = null, val responseTimeMillis: Long? = null,
    val ratingSource: String? = null, val predecessorReviewEventId: String? = null,
    val stateBefore: ReviewMemoryStateProofRecord? = null,
    val expectedStateAfter: ReviewMemoryStateProofRecord? = null
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
                mediaReference, sha256, sizeBytes, mimeType, baseRevision?.let(::SyncRevision), previousSha256
            )
            "REVIEW_EVENT" -> ReviewEventDelta(
                requireNotNull(reviewEventId), requireNotNull(learningItemId),
                requireNotNull(learnerId), payload, contentId, rating, reviewedAtEpochMillis,
                responseTimeMillis, ratingSource, predecessorReviewEventId,
                stateBefore?.toDomain(), expectedStateAfter?.toDomain()
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
        mimeType = payload.mimeType, baseRevision = payload.baseRevision?.value,
        previousSha256 = payload.previousSha256
    )
    is ReviewEventDelta -> SyncChangeRecord(
        accountId.value, eventId.value, idempotencyKey.value, sourceDeviceId.value, entityId.value,
        payloadVersion, payload.namespace.name, "REVIEW_EVENT", reviewEventId = payload.reviewEventId,
        learningItemId = payload.learningItemId, learnerId = payload.learnerId, payload = payload.payload,
        contentId = payload.contentId, rating = payload.rating,
        reviewedAtEpochMillis = payload.reviewedAtEpochMillis,
        responseTimeMillis = payload.responseTimeMillis, ratingSource = payload.ratingSource,
        predecessorReviewEventId = payload.predecessorReviewEventId,
        stateBefore = payload.stateBefore?.toRecord(),
        expectedStateAfter = payload.expectedStateAfter?.toRecord()
    )
}

@Serializable
private data class ReviewMemoryStateProofRecord(
    val stage: String,
    val difficulty: Double,
    val stabilityDays: Double,
    val dueAtEpochMillis: Long,
    val lastReviewedAtEpochMillis: Long?,
    val reviewCount: Int,
    val lapseCount: Int
) {
    fun toDomain() = ReviewMemoryStateProof(
        stage, difficulty, stabilityDays, dueAtEpochMillis,
        lastReviewedAtEpochMillis, reviewCount, lapseCount
    )
}

private fun ReviewMemoryStateProof.toRecord() = ReviewMemoryStateProofRecord(
    stage, difficulty, stabilityDays, dueAtEpochMillis,
    lastReviewedAtEpochMillis, reviewCount, lapseCount
)
