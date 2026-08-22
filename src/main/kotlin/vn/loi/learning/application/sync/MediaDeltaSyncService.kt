package vn.loi.learning.application.sync

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.IncrementalMediaBlobStore
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.sync.protocol.*

sealed interface MediaDeltaApplyResult {
    data class Applied(val content: Content) : MediaDeltaApplyResult
    data object Duplicate : MediaDeltaApplyResult
    data class PendingBlob(val sha256: String) : MediaDeltaApplyResult
    data class Quarantined(val diagnostic: SyncConflictDiagnostic, val reason: String) : MediaDeltaApplyResult
}

data class MediaGcResult(val deleted: Int, val retained: Int)

class MediaDeltaSyncService(
    private val contents: ContentRepository,
    private val blobs: IncrementalMediaBlobStore,
    private val state: LocalSyncStateRepository,
    private val coordinator: LocalSyncCoordinator
) {
    fun provideBlob(delta: MediaDelta, bytes: ByteArray) {
        require(delta.operation == DeltaOperation.SET) { "Only SET media deltas carry blobs." }
        val sha = requireNotNull(delta.sha256)
        require(bytes.size.toLong() == delta.sizeBytes) { "Media blob size mismatch." }
        require(blobs.sha256(bytes) == sha) { "Media blob checksum mismatch." }
        validateSignature(requireNotNull(delta.mimeType), bytes)
        blobs.stage(sha, bytes)
    }

    fun applyLocal(change: OutboundSyncChange, bytes: ByteArray? = null): Content {
        val delta = requireMedia(change)
        bytes?.let { provideBlob(delta, it) }
        val reference = if (delta.operation == DeltaOperation.SET) materialize(delta) else null
        val updated = coordinator.mutateAndEnqueue(change) {
            val existing = requireNotNull(contents.findById(ContentId(change.entityId.value))) {
                "Content ${change.entityId.value} does not exist."
            }
            validateBase(existing, delta)
            val oldReference = reference(existing, delta.slot)
            val next = withReference(existing, delta.slot, reference)
            if (next != existing) contents.save(next)
            recordGcCandidate(oldReference, reference)
            next
        }
        delta.sha256?.let(blobs::discardStaged)
        collectGarbage()
        return updated
    }

    fun applyRemote(remote: RemoteSyncChange): MediaDeltaApplyResult {
        val change = remote.change
        if (state.hasApplied(change.accountId, change.eventId)) return MediaDeltaApplyResult.Duplicate
        val delta = change.delta as? MediaDelta
            ?: throw IllegalArgumentException("Media sync requires a MediaDelta.")

        if (change.payloadVersion != SUPPORTED_VERSION) {
            return applyQuarantine(remote, delta, "SYNC_MEDIA_UNSUPPORTED_PAYLOAD", "Media payload version is unsupported.")
        }
        val structuralFailure = validateStructure(change, delta)
        if (structuralFailure != null) {
            return applyQuarantine(remote, delta, structuralFailure.first, structuralFailure.second)
        }
        if (contents.findById(ContentId(change.entityId.value)) == null) {
            return applyQuarantine(
                remote, delta, "SYNC_MEDIA_CONTENT_NOT_FOUND", "Content does not exist locally."
            )
        }
        if (delta.operation == DeltaOperation.SET &&
            !blobs.hasStaged(requireNotNull(delta.sha256), requireNotNull(delta.sizeBytes))
        ) {
            state.recordPendingMedia(
                PendingMediaApply(
                    change.accountId, change.eventId, change.entityId.value, delta.slot.name,
                    requireNotNull(delta.sha256), requireNotNull(delta.sizeBytes), requireNotNull(delta.mimeType),
                    remote.revision.value, change.payloadVersion
                )
            )
            return MediaDeltaApplyResult.PendingBlob(requireNotNull(delta.sha256))
        }

        var result: MediaDeltaApplyResult? = null
        val handled = coordinator.applyOnce(remote) { accepted ->
            val acceptedDelta = requireMedia(accepted)
            val content = contents.findById(ContentId(accepted.entityId.value))
            if (content == null) {
                result = quarantine(remote, acceptedDelta, "SYNC_MEDIA_CONTENT_NOT_FOUND", "Content does not exist locally.")
                return@applyOnce
            }
            val pendingSameSlot = state.pendingOutbox(accepted.accountId).any {
                it.entityId == accepted.entityId && (it.delta as? MediaDelta)?.slot == acceptedDelta.slot
            }
            if (pendingSameSlot) {
                result = quarantine(remote, acceptedDelta, "SYNC_MEDIA_LOCAL_PENDING", "A local change is pending for the same media slot.")
                return@applyOnce
            }
            val oldReference = reference(content, acceptedDelta.slot)
            val oldDigest = oldReference?.let(blobs::digestReference)
            if (acceptedDelta.operation == DeltaOperation.SET && oldDigest == acceptedDelta.sha256) {
                state.removePendingMedia(accepted.accountId, accepted.eventId)
                result = MediaDeltaApplyResult.Applied(content)
                return@applyOnce
            }
            val baseFailure = runCatching { validateBase(content, acceptedDelta) }.exceptionOrNull()
            if (baseFailure != null) {
                result = quarantine(remote, acceptedDelta, "SYNC_MEDIA_BASE_MISMATCH", "Media slot does not match the causal base identity.")
                return@applyOnce
            }
            val newReference = if (acceptedDelta.operation == DeltaOperation.SET) {
                try {
                    materialize(acceptedDelta)
                } catch (_: Exception) {
                    result = quarantine(
                        remote, acceptedDelta, "SYNC_MEDIA_MATERIALIZE_FAILED",
                        "Validated media blob could not be materialized."
                    )
                    return@applyOnce
                }
            } else null
            val updated = withReference(content, acceptedDelta.slot, newReference)
            contents.save(updated)
            state.removePendingMedia(accepted.accountId, accepted.eventId)
            recordGcCandidate(oldReference, newReference)
            result = MediaDeltaApplyResult.Applied(updated)
        }
        if (!handled) return MediaDeltaApplyResult.Duplicate
        if (result is MediaDeltaApplyResult.Applied) {
            delta.sha256?.let(blobs::discardStaged)
            collectGarbage()
        }
        return requireNotNull(result)
    }

    fun collectGarbage(): MediaGcResult {
        val referenced = contents.findAll().flatMap { content -> allReferences(content) }.toHashSet()
        var deleted = 0
        var retained = 0
        state.mediaGcCandidates().forEach { candidate ->
            if (candidate.reference in referenced || !blobs.isManaged(candidate.reference) ||
                blobs.digestReference(candidate.reference) != candidate.sha256
            ) {
                retained++
            } else if (runCatching { blobs.deleteManaged(candidate.reference) }.getOrDefault(false)) {
                state.removeMediaGcCandidate(candidate.reference)
                deleted++
            } else retained++
        }
        return MediaGcResult(deleted, retained)
    }

    private fun applyQuarantine(
        remote: RemoteSyncChange,
        delta: MediaDelta,
        code: String,
        reason: String
    ): MediaDeltaApplyResult {
        var result: MediaDeltaApplyResult? = null
        val handled = coordinator.applyOnce(remote) {
            result = quarantine(remote, delta, code, reason)
        }
        return if (handled) requireNotNull(result) else MediaDeltaApplyResult.Duplicate
    }

    private fun quarantine(
        remote: RemoteSyncChange,
        delta: MediaDelta,
        code: String,
        reason: String
    ): MediaDeltaApplyResult.Quarantined {
        state.recordQuarantine(
            SyncQuarantineRecord(
                remote.change.accountId, remote.change.eventId, "", "",
                remote.revision.value, remote.change.payloadVersion, code, reason,
                contentId = remote.change.entityId.value, mediaSha256 = delta.sha256
            )
        )
        state.removePendingMedia(remote.change.accountId, remote.change.eventId)
        return MediaDeltaApplyResult.Quarantined(
            SyncConflictDiagnostic(
                remote.change.eventId, SyncNamespace.MEDIA, remote.change.entityId,
                delta.slot.name, delta.baseRevision, remote.revision,
                SyncConflictOutcome.QUARANTINED, code
            ), reason
        )
    }

    private fun validateStructure(change: OutboundSyncChange, delta: MediaDelta): Pair<String, String>? {
        if (delta.operation == DeltaOperation.REMOVE && delta.mimeType != null) {
            return "SYNC_MEDIA_UNSUPPORTED_PAYLOAD" to "A media removal cannot carry MIME metadata."
        }
        if (delta.operation == DeltaOperation.SET) {
            val mime = requireNotNull(delta.mimeType)
            val extension = MIME_EXTENSIONS[mime]
                ?: return "SYNC_MEDIA_TYPE_UNSUPPORTED" to "Media MIME type is unsupported."
            if (!slotAccepts(delta.slot, mime)) {
                return "SYNC_MEDIA_TYPE_UNSUPPORTED" to "Media MIME type is invalid for the target slot."
            }
            val expected = "sync/${delta.sha256}.$extension"
            if (delta.mediaReference != expected || !safeReference(requireNotNull(delta.mediaReference))) {
                return "SYNC_MEDIA_REFERENCE_UNSAFE" to "Media reference is not the canonical content-addressed reference."
            }
            if (requireNotNull(delta.sizeBytes) <= 0L) {
                return "SYNC_MEDIA_SIZE_MISMATCH" to "Media byte size must be positive."
            }
        }
        if (change.entityId.value.isBlank()) {
            return "SYNC_MEDIA_CONTENT_NOT_FOUND" to "Content identity is invalid."
        }
        return null
    }

    private fun validateBase(content: Content, delta: MediaDelta) {
        val existing = reference(content, delta.slot)
        if (existing == null) require(delta.previousSha256 == null) { "Media base identity mismatch." }
        else require(blobs.digestReference(existing) == delta.previousSha256) { "Media base identity mismatch." }
    }

    private fun materialize(delta: MediaDelta): String = blobs.materialize(
        requireNotNull(delta.sha256), requireNotNull(delta.sizeBytes), requireNotNull(delta.mimeType)
    )

    private fun recordGcCandidate(oldReference: String?, newReference: String?) {
        if (oldReference == null || oldReference == newReference || !blobs.isManaged(oldReference)) return
        blobs.digestReference(oldReference)?.let { state.recordMediaGcCandidate(MediaGcCandidate(oldReference, it)) }
    }

    private fun reference(content: Content, slot: MediaSlot): String? = when (slot) {
        MediaSlot.QUESTION_AUDIO -> content.media.primaryAudio
        MediaSlot.ANSWER_AUDIO -> content.media.translatedAudio
        MediaSlot.EXAMPLE_AUDIO -> content.media.exampleAudio
        MediaSlot.TRANSLATION_AUDIO -> content.media.exampleTranslatedAudio
        MediaSlot.IMAGE -> content.media.image
    }

    private fun withReference(content: Content, slot: MediaSlot, value: String?): Content = content.copy(
        media = when (slot) {
            MediaSlot.QUESTION_AUDIO -> content.media.copy(primaryAudio = value)
            MediaSlot.ANSWER_AUDIO -> content.media.copy(translatedAudio = value)
            MediaSlot.EXAMPLE_AUDIO -> content.media.copy(exampleAudio = value)
            MediaSlot.TRANSLATION_AUDIO -> content.media.copy(exampleTranslatedAudio = value)
            MediaSlot.IMAGE -> content.media.copy(image = value)
        }
    )

    private fun allReferences(content: Content): List<String> = listOfNotNull(
        content.media.primaryAudio, content.media.translatedAudio, content.media.exampleAudio,
        content.media.exampleTranslatedAudio, content.media.image
    )

    private fun requireMedia(change: OutboundSyncChange): MediaDelta =
        change.delta as? MediaDelta ?: throw IllegalArgumentException("Media sync requires a MediaDelta.")

    private fun slotAccepts(slot: MediaSlot, mime: String): Boolean =
        if (slot == MediaSlot.IMAGE) mime.startsWith("image/") else mime.startsWith("audio/")

    private fun safeReference(value: String): Boolean =
        value.startsWith("sync/") && !value.contains('\\') && !value.contains("..") && !value.startsWith('/')

    private fun validateSignature(mime: String, bytes: ByteArray) {
        val valid = when (mime) {
            "audio/mpeg" -> bytes.startsWith(byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte())) ||
                (bytes.size >= 2 && bytes[0] == 0xff.toByte() && (bytes[1].toInt() and 0xe0) == 0xe0)
            "audio/wav" -> bytes.startsWith("RIFF".encodeToByteArray()) && bytes.size >= 12 &&
                bytes.copyOfRange(8, 12).contentEquals("WAVE".encodeToByteArray())
            "image/jpeg" -> bytes.startsWith(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte()))
            "image/png" -> bytes.startsWith(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a))
            "image/webp" -> bytes.startsWith("RIFF".encodeToByteArray()) && bytes.size >= 12 &&
                bytes.copyOfRange(8, 12).contentEquals("WEBP".encodeToByteArray())
            else -> false
        }
        require(valid) { "Media bytes do not match the declared MIME type." }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private companion object {
        const val SUPPORTED_VERSION = 1
        val MIME_EXTENSIONS = mapOf(
            "audio/mpeg" to "mp3", "audio/wav" to "wav",
            "image/jpeg" to "jpg", "image/png" to "png", "image/webp" to "webp"
        )
    }
}
