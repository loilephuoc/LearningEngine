package vn.loi.learning.application.sync

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.sync.protocol.*

sealed interface ContentDeltaApplyResult {
    data class Applied(val content: Content, val operation: DeltaOperation) : ContentDeltaApplyResult
    data object Duplicate : ContentDeltaApplyResult
    data class Conflict(val diagnostic: SyncConflictDiagnostic) : ContentDeltaApplyResult
}

@Suppress("DEPRECATION")
class ContentFieldSyncService(
    private val contents: ContentRepository,
    private val localState: LocalSyncStateRepository,
    private val coordinator: LocalSyncCoordinator
) {
    fun applyLocal(change: OutboundSyncChange): Content =
        coordinator.mutateAndEnqueue(change) {
            applyField(requireContentDelta(change), change.entityId).also(contents::save)
        }

    fun applyRemote(remote: RemoteSyncChange): ContentDeltaApplyResult {
        var result: ContentDeltaApplyResult? = null
        val applied = coordinator.applyOnce(remote) { change ->
            val delta = requireContentDelta(change)
            val pendingSameField = localState.pendingOutbox(change.accountId).firstOrNull { pending ->
                pending.entityId == change.entityId &&
                    (pending.delta as? ContentFieldDelta)?.field?.canonicalPath == delta.field.canonicalPath
            }
            if (pendingSameField != null) {
                result = ContentDeltaApplyResult.Conflict(
                    SyncConflictDiagnostic(
                        eventId = change.eventId,
                        namespace = SyncNamespace.CONTENT,
                        entityId = change.entityId,
                        field = delta.field.canonicalPath,
                        localRevision = (pendingSameField.delta as ContentFieldDelta).baseRevision,
                        remoteRevision = remote.revision,
                        outcome = SyncConflictOutcome.PRESERVED_LOCAL,
                        code = "SYNC_CONTENT_FIELD_LOCAL_PENDING"
                    )
                )
            } else {
                val updated = applyField(delta, change.entityId)
                contents.save(updated)
                result = ContentDeltaApplyResult.Applied(updated, delta.operation)
            }
        }
        return if (applied) requireNotNull(result) else ContentDeltaApplyResult.Duplicate
    }

    private fun requireContentDelta(change: OutboundSyncChange): ContentFieldDelta {
        val delta = change.delta as? ContentFieldDelta
            ?: throw IllegalArgumentException("Content sync requires a ContentFieldDelta.")
        require(delta.field in SUPPORTED_FIELDS) {
            "Unsupported Content Editor sync field: ${delta.field.canonicalPath}"
        }
        require(delta.operation != DeltaOperation.APPEND) { "Content Editor fields do not support APPEND." }
        return delta
    }

    private fun applyField(delta: ContentFieldDelta, entityId: SyncEntityId): Content {
        val contentId = ContentId(entityId.value)
        val existing = requireNotNull(contents.findById(contentId)) { "Content ${contentId.value} does not exist." }
        val value = when (delta.operation) {
            DeltaOperation.SET -> requireNotNull(delta.value)
            DeltaOperation.REMOVE -> null
            DeltaOperation.APPEND -> error("APPEND was rejected before Content mutation.")
        }
        val text = when (delta.field) {
            ContentField.QUESTION -> {
                require(!value.isNullOrBlank()) { "Question cannot be empty or removed." }
                existing.text.copy(primaryText = value.trim())
            }
            ContentField.ANSWER -> existing.text.copy(translatedText = value?.trim()?.takeIf(String::isNotBlank))
            ContentField.EXAMPLE -> existing.text.copy(exampleText = value?.trim()?.takeIf(String::isNotBlank))
            ContentField.TRANSLATION,
            ContentField.EXAMPLE_TRANSLATION -> existing.text.copy(exampleTranslation = value?.trim()?.takeIf(String::isNotBlank))
            else -> error("Unsupported Content Editor sync field: ${delta.field.canonicalPath}")
        }
        return existing.copy(text = text)
    }

    private companion object {
        val SUPPORTED_FIELDS = setOf(
            ContentField.QUESTION, ContentField.ANSWER, ContentField.EXAMPLE,
            ContentField.TRANSLATION, ContentField.EXAMPLE_TRANSLATION
        )
    }
}
