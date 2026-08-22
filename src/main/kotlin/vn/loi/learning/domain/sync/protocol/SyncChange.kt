package vn.loi.learning.domain.sync.protocol

enum class SyncNamespace { CONTENT, MEDIA, LEARNING }

enum class ContentField {
    QUESTION, ANSWER, TRANSLATION, PRONUNCIATION, EXAMPLE, EXAMPLE_TRANSLATION,
    TITLE, TAGS, CUSTOM_FIELD
}

enum class MediaSlot {
    QUESTION_AUDIO, ANSWER_AUDIO, EXAMPLE_AUDIO, TRANSLATION_AUDIO, IMAGE
}

enum class DeltaOperation { SET, REMOVE, APPEND }

sealed interface SyncDelta {
    val namespace: SyncNamespace
}

data class ContentFieldDelta(
    val field: ContentField,
    val operation: DeltaOperation,
    val value: String? = null,
    val customFieldId: String? = null,
    val baseRevision: SyncRevision? = null
) : SyncDelta {
    override val namespace: SyncNamespace = SyncNamespace.CONTENT

    init {
        require(operation == DeltaOperation.REMOVE || value != null) {
            "A content SET/APPEND delta requires a value."
        }
        require((field == ContentField.CUSTOM_FIELD) == (customFieldId != null)) {
            "Custom field ID must be present only for CUSTOM_FIELD deltas."
        }
    }
}

data class MediaDelta(
    val slot: MediaSlot,
    val operation: DeltaOperation,
    val mediaReference: String?,
    val sha256: String?,
    val sizeBytes: Long?,
    val mimeType: String? = null,
    val baseRevision: SyncRevision? = null
) : SyncDelta {
    override val namespace: SyncNamespace = SyncNamespace.MEDIA

    init {
        require(operation != DeltaOperation.APPEND) { "Media deltas do not support APPEND." }
        if (operation == DeltaOperation.SET) {
            require(!mediaReference.isNullOrBlank()) { "A media SET delta requires a reference." }
            require(sha256?.matches(Regex("[0-9a-f]{64}")) == true) {
                "A media SET delta requires a lowercase SHA-256 checksum."
            }
            require(sizeBytes != null && sizeBytes >= 0L) { "A media SET delta requires a valid size." }
        } else {
            require(mediaReference == null && sha256 == null && sizeBytes == null) {
                "A media REMOVE delta must not carry object metadata."
            }
        }
    }
}

data class ReviewEventDelta(
    val reviewEventId: String,
    val learningItemId: String,
    val learnerId: String,
    val payload: String
) : SyncDelta {
    override val namespace: SyncNamespace = SyncNamespace.LEARNING

    init {
        require(reviewEventId.isNotBlank()) { "Review event ID must not be blank." }
        require(learningItemId.isNotBlank()) { "Learning item ID must not be blank." }
        require(learnerId.isNotBlank()) { "Learner ID must not be blank." }
        require(payload.isNotBlank()) { "Review event payload must not be blank." }
    }
}

data class OutboundSyncChange(
    val accountId: SyncAccountId,
    val eventId: SyncEventId,
    val idempotencyKey: IdempotencyKey,
    val sourceDeviceId: SyncDeviceId,
    val entityId: SyncEntityId,
    val payloadVersion: Int = 1,
    val delta: SyncDelta
) {
    init { require(payloadVersion > 0) { "Payload version must be positive." } }
}

data class RemoteSyncChange(
    val revision: SyncRevision,
    val change: OutboundSyncChange
)

data class SyncConflictDiagnostic(
    val eventId: SyncEventId,
    val namespace: SyncNamespace,
    val entityId: SyncEntityId,
    val field: String?,
    val localRevision: SyncRevision?,
    val remoteRevision: SyncRevision,
    val outcome: SyncConflictOutcome,
    val code: String
) {
    init { require(code.isNotBlank()) { "Conflict diagnostic code must not be blank." } }
}

enum class SyncConflictOutcome { APPLIED, PRESERVED_LOCAL, QUARANTINED }
