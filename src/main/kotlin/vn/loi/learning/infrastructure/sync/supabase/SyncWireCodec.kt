package vn.loi.learning.infrastructure.sync.supabase

import kotlinx.serialization.json.*
import vn.loi.learning.domain.sync.protocol.*

class SyncWireCodec(private val json: Json = Json { ignoreUnknownKeys = false }) {
    fun encode(change: OutboundSyncChange): JsonObject = buildJsonObject {
        put("event_id", change.eventId.value)
        put("idempotency_key", change.idempotencyKey.value)
        put("origin_device_id", change.sourceDeviceId.value)
        put("namespace", change.delta.namespace.name)
        put("kind", kind(change.delta))
        put("entity_id", change.entityId.value)
        put("payload_version", change.payloadVersion)
        put("payload", encodeDelta(change.delta))
    }

    fun decode(revision: Long, value: JsonObject, accountId: SyncAccountId): RemoteSyncChange {
        val version = value.int("payload_version")
        if (version != 1) throw SupabaseTransportException("SYNC_SUPABASE_PAYLOAD_VERSION_UNSUPPORTED", false)
        val kind = value.string("kind")
        val payload = value.obj("payload")
        val change = OutboundSyncChange(
            accountId, SyncEventId(value.string("event_id")), IdempotencyKey(value.string("idempotency_key")),
            SyncDeviceId(value.string("origin_device_id")), SyncEntityId(value.string("entity_id")), version,
            decodeDelta(kind, payload)
        )
        return RemoteSyncChange(SyncRevision(revision), change)
    }

    fun parseObject(text: String): JsonObject = try { json.parseToJsonElement(text).jsonObject } catch (e: Exception) {
        throw SupabaseTransportException("SYNC_SUPABASE_RESPONSE_MALFORMED", false, cause = e)
    }

    fun parseArray(text: String): JsonArray = try { json.parseToJsonElement(text).jsonArray } catch (e: Exception) {
        throw SupabaseTransportException("SYNC_SUPABASE_RESPONSE_MALFORMED", false, cause = e)
    }

    private fun encodeDelta(delta: SyncDelta): JsonObject = buildJsonObject {
        when (delta) {
            is ContentFieldDelta -> {
                put("field", delta.field.name); put("operation", delta.operation.name)
                delta.value?.let { put("value", it) }; delta.customFieldId?.let { put("custom_field_id", it) }
                delta.baseRevision?.let { put("base_revision", it.value) }
            }
            is MediaDelta -> {
                put("slot", delta.slot.name); put("operation", delta.operation.name)
                delta.mediaReference?.let { put("media_reference", it) }; delta.sha256?.let { put("sha256", it) }
                delta.sizeBytes?.let { put("size_bytes", it) }; delta.mimeType?.let { put("mime_type", it) }
                delta.baseRevision?.let { put("base_revision", it.value) }; delta.previousSha256?.let { put("previous_sha256", it) }
            }
            is ReviewEventDelta -> {
                put("review_event_id", delta.reviewEventId); put("learning_item_id", delta.learningItemId); put("learner_id", delta.learnerId)
                delta.payload?.let { put("legacy_payload", it) }; delta.contentId?.let { put("content_id", it) }
                delta.rating?.let { put("rating", it) }; delta.reviewedAtEpochMillis?.let { put("reviewed_at_epoch_millis", it) }
                delta.responseTimeMillis?.let { put("response_time_millis", it) }; delta.ratingSource?.let { put("rating_source", it) }
                delta.predecessorReviewEventId?.let { put("predecessor_review_event_id", it) }
                delta.stateBefore?.let { put("state_before", proof(it)) }; delta.expectedStateAfter?.let { put("expected_state_after", proof(it)) }
            }
        }
    }

    private fun decodeDelta(kind: String, p: JsonObject): SyncDelta = when (kind) {
        "CONTENT_FIELD" -> ContentFieldDelta(ContentField.valueOf(p.string("field")), DeltaOperation.valueOf(p.string("operation")), p.optionalString("value"), p.optionalString("custom_field_id"), p.optionalLong("base_revision")?.let(::SyncRevision))
        "MEDIA" -> MediaDelta(MediaSlot.valueOf(p.string("slot")), DeltaOperation.valueOf(p.string("operation")), p.optionalString("media_reference"), p.optionalString("sha256"), p.optionalLong("size_bytes"), p.optionalString("mime_type"), p.optionalLong("base_revision")?.let(::SyncRevision), p.optionalString("previous_sha256"))
        "REVIEW_EVENT" -> ReviewEventDelta(p.string("review_event_id"), p.string("learning_item_id"), p.string("learner_id"), p.optionalString("legacy_payload"), p.optionalString("content_id"), p.optionalString("rating"), p.optionalLong("reviewed_at_epoch_millis"), p.optionalLong("response_time_millis"), p.optionalString("rating_source"), p.optionalString("predecessor_review_event_id"), p.optionalObj("state_before")?.let(::decodeProof), p.optionalObj("expected_state_after")?.let(::decodeProof))
        else -> throw SupabaseTransportException("SYNC_SUPABASE_DELTA_KIND_UNSUPPORTED", false)
    }

    private fun proof(p: ReviewMemoryStateProof) = buildJsonObject {
        put("stage", p.stage); put("difficulty", p.difficulty); put("stability_days", p.stabilityDays); put("due_at_epoch_millis", p.dueAtEpochMillis)
        p.lastReviewedAtEpochMillis?.let { put("last_reviewed_at_epoch_millis", it) }; put("review_count", p.reviewCount); put("lapse_count", p.lapseCount)
    }
    private fun decodeProof(p: JsonObject) = ReviewMemoryStateProof(p.string("stage"), p.double("difficulty"), p.double("stability_days"), p.long("due_at_epoch_millis"), p.optionalLong("last_reviewed_at_epoch_millis"), p.int("review_count"), p.int("lapse_count"))
    private fun kind(delta: SyncDelta) = when (delta) { is ContentFieldDelta -> "CONTENT_FIELD"; is MediaDelta -> "MEDIA"; is ReviewEventDelta -> "REVIEW_EVENT" }
}

private fun JsonObject.element(name: String) = this[name] ?: throw SupabaseTransportException("SYNC_SUPABASE_RESPONSE_MISSING_FIELD", false)
private fun JsonObject.string(name: String) = runCatching { element(name).jsonPrimitive.content }.getOrElse { throw SupabaseTransportException("SYNC_SUPABASE_RESPONSE_MALFORMED", false, cause = it) }
private fun JsonObject.long(name: String) = runCatching { element(name).jsonPrimitive.long }.getOrElse { throw SupabaseTransportException("SYNC_SUPABASE_RESPONSE_MALFORMED", false, cause = it) }
private fun JsonObject.int(name: String) = runCatching { element(name).jsonPrimitive.int }.getOrElse { throw SupabaseTransportException("SYNC_SUPABASE_RESPONSE_MALFORMED", false, cause = it) }
private fun JsonObject.double(name: String) = runCatching { element(name).jsonPrimitive.double }.getOrElse { throw SupabaseTransportException("SYNC_SUPABASE_RESPONSE_MALFORMED", false, cause = it) }
private fun JsonObject.obj(name: String) = runCatching { element(name).jsonObject }.getOrElse { throw SupabaseTransportException("SYNC_SUPABASE_RESPONSE_MALFORMED", false, cause = it) }
private fun JsonObject.optionalString(name: String) = this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
private fun JsonObject.optionalLong(name: String) = this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.long
private fun JsonObject.optionalObj(name: String) = this[name]?.takeUnless { it is JsonNull }?.jsonObject
