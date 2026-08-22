package vn.loi.learning.application.sync

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.application.review.ReviewResult
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.sync.protocol.*

sealed interface ReviewDeltaApplyResult {
    data class Applied(val reviewEvent: ReviewEvent) : ReviewDeltaApplyResult
    data object Duplicate : ReviewDeltaApplyResult
    data class Quarantined(val diagnostic: SyncConflictDiagnostic, val reason: String) : ReviewDeltaApplyResult
}

/** Applies immutable review facts through the same scheduler/use-case used by LearningEngine.review. */
class ReviewDeltaSyncService(
    private val learningItems: LearningItemRepository,
    private val contents: ContentRepository,
    private val memories: MemoryStateRepository,
    private val reviewEvents: ReviewEventRepository,
    private val review: ReviewLearningItemUseCase,
    private val state: LocalSyncStateRepository,
    private val coordinator: LocalSyncCoordinator
) {
    fun reviewLocalAndEnqueue(
        accountId: SyncAccountId,
        syncEventId: SyncEventId,
        idempotencyKey: IdempotencyKey,
        sourceDeviceId: SyncDeviceId,
        command: ReviewCommand
    ): ReviewResult = coordinator.mutateAndEnqueue {
        val item = requireNotNull(learningItems.findById(command.learningItemId)) {
            "LearningItem ${command.learningItemId.value} does not exist."
        }
        require(item.isEnabled) { "LearningItem ${command.learningItemId.value} is disabled." }
        requireNotNull(contents.findById(item.contentId)) { "Content ${item.contentId.value} does not exist." }
        val predecessor = reviewEvents.findAll(command.learnerId, command.learningItemId).lastOrNull()?.id?.value
        val result = review.execute(command)
        val delta = ReviewEventDelta(
            result.reviewEvent.id.value, command.learningItemId.value, command.learnerId.value,
            contentId = item.contentId.value,
            rating = command.rating.name,
            reviewedAtEpochMillis = command.reviewedAt.epochMillis,
            responseTimeMillis = command.responseTime?.millis,
            ratingSource = command.source.name,
            predecessorReviewEventId = predecessor,
            stateBefore = result.reviewEvent.stateBefore.toProof(),
            expectedStateAfter = result.reviewEvent.stateAfter.toProof()
        )
        result to OutboundSyncChange(
            accountId, syncEventId, idempotencyKey, sourceDeviceId,
            SyncEntityId(command.learningItemId.value), SUPPORTED_PAYLOAD_VERSION, delta
        )
    }

    fun applyRemote(remote: RemoteSyncChange): ReviewDeltaApplyResult {
        var result: ReviewDeltaApplyResult? = null
        val newlyHandled = coordinator.applyOnce(remote) { change ->
            val delta = change.delta as? ReviewEventDelta
                ?: throw IllegalArgumentException("Learning sync requires a ReviewEventDelta.")
            val duplicate = reviewEvents.findAll().firstOrNull { it.id.value == delta.reviewEventId }
            if (duplicate != null) {
                result = ReviewDeltaApplyResult.Duplicate
                return@applyOnce
            }

            val failure = validate(remote, delta)
            if (failure != null) {
                result = quarantine(remote, delta, failure.first, failure.second)
                return@applyOnce
            }

            val itemId = LearningItemId(delta.learningItemId)
            val learnerId = LearnerId(delta.learnerId)
            val reviewedAt = Moment(requireNotNull(delta.reviewedAtEpochMillis))
            val command = ReviewCommand(
                ReviewEventId(delta.reviewEventId), learnerId, itemId,
                ReviewRating.valueOf(requireNotNull(delta.rating)), reviewedAt,
                delta.responseTimeMillis?.let(::TimeSpan),
                RatingSource.valueOf(requireNotNull(delta.ratingSource))
            )
            try {
                val applied = review.execute(command) { event ->
                    if (event.stateBefore.toProof() != delta.stateBefore ||
                        event.stateAfter.toProof() != delta.expectedStateAfter
                    ) throw ReplayMismatch()
                }
                result = ReviewDeltaApplyResult.Applied(applied.reviewEvent)
            } catch (_: ReplayMismatch) {
                result = quarantine(
                    remote, delta, "SYNC_REVIEW_REPLAY_MISMATCH",
                    "Canonical scheduler replay did not match the supplied validation proof."
                )
            }
        }
        return if (newlyHandled) requireNotNull(result) else ReviewDeltaApplyResult.Duplicate
    }

    private fun validate(remote: RemoteSyncChange, delta: ReviewEventDelta): Pair<String, String>? {
        if (remote.change.payloadVersion != SUPPORTED_PAYLOAD_VERSION || !delta.hasReplayContract) {
            return "SYNC_REVIEW_UNSUPPORTED_PAYLOAD" to
                "Review payload version or replay contract is unsupported."
        }
        val itemId = runCatching { LearningItemId(delta.learningItemId) }.getOrNull()
            ?: return "SYNC_REVIEW_ITEM_NOT_FOUND" to "LearningItem identity is invalid."
        val item = learningItems.findById(itemId)
            ?: return "SYNC_REVIEW_ITEM_NOT_FOUND" to "LearningItem does not exist locally."
        if (!item.isEnabled) return "SYNC_REVIEW_ITEM_DISABLED" to "LearningItem is disabled."
        if (remote.change.entityId.value != delta.learningItemId || item.contentId.value != delta.contentId ||
            contents.findById(ContentId(requireNotNull(delta.contentId))) == null
        ) return "SYNC_REVIEW_ITEM_NOT_FOUND" to "LearningItem and Content relationship is invalid."

        val learner = runCatching { LearnerId(delta.learnerId) }.getOrNull()
            ?: return "SYNC_REVIEW_REPLAY_MISMATCH" to "Learner identity is invalid."
        val current = memories.find(learner, itemId)
        if (current?.stage == LearningStage.SUSPENDED) {
            return "SYNC_REVIEW_ITEM_DISABLED" to "Learner memory state is suspended."
        }
        if (runCatching { ReviewRating.valueOf(requireNotNull(delta.rating)) }.isFailure ||
            runCatching { RatingSource.valueOf(requireNotNull(delta.ratingSource)) }.isFailure ||
            runCatching { Moment(requireNotNull(delta.reviewedAtEpochMillis)) }.isFailure ||
            runCatching { delta.responseTimeMillis?.let(::TimeSpan) }.isFailure
        ) return "SYNC_REVIEW_UNSUPPORTED_PAYLOAD" to "Review rating, source, or time is invalid."

        val history = reviewEvents.findAll(learner, itemId)
        val latest = history.lastOrNull()
        val predecessor = delta.predecessorReviewEventId
        if (latest == null && predecessor != null) {
            return "SYNC_REVIEW_TIMELINE_GAP" to "Predecessor ReviewEvent is missing."
        }
        if (latest != null && predecessor != latest.id.value) {
            return if (predecessor == null || history.any { it.id.value == predecessor }) {
                "SYNC_REVIEW_CONCURRENT_BRANCH" to "ReviewEvent does not extend the current timeline head."
            } else {
                "SYNC_REVIEW_TIMELINE_GAP" to "Predecessor ReviewEvent is missing."
            }
        }
        val expectedBefore = current ?: MemoryState.new(
            learner, itemId, Moment(requireNotNull(delta.reviewedAtEpochMillis))
        )
        if (expectedBefore.toProof() != delta.stateBefore) {
            return "SYNC_REVIEW_REPLAY_MISMATCH" to "Local MemoryState does not match the review base proof."
        }
        return null
    }

    private fun quarantine(
        remote: RemoteSyncChange,
        delta: ReviewEventDelta,
        code: String,
        reason: String
    ): ReviewDeltaApplyResult.Quarantined {
        state.recordQuarantine(
            SyncQuarantineRecord(
                remote.change.accountId, remote.change.eventId, delta.reviewEventId,
                delta.learningItemId, remote.revision.value, remote.change.payloadVersion, code, reason
            )
        )
        return ReviewDeltaApplyResult.Quarantined(
            SyncConflictDiagnostic(
                remote.change.eventId, SyncNamespace.LEARNING, remote.change.entityId,
                "review.timeline", null, remote.revision, SyncConflictOutcome.QUARANTINED, code
            ),
            reason
        )
    }

    private class ReplayMismatch : RuntimeException()

    private companion object { const val SUPPORTED_PAYLOAD_VERSION = 1 }
}

fun MemoryState.toProof(): ReviewMemoryStateProof = ReviewMemoryStateProof(
    stage.name, difficulty, stabilityDays, dueAt.epochMillis,
    lastReviewedAt?.epochMillis, reviewCount, lapseCount
)
