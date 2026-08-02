package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.evidence.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.persistence.record.*

object LearningTrajectoryRecordMapper {
    fun toRecord(learnerId: LearnerId, trajectory: LearningTrajectory) = LearningTrajectoryRecord(
        learnerId = learnerId.value,
        contentId = trajectory.contentId.value,
        chains = trajectory.chains.map { chain ->
            EvidenceChainRecord(chain.stage.name, chain.anchor.reason.name,
                chain.sequence.map { entry -> when (entry) {
                    is EvidenceSequenceEntry.Recall -> entry.evidence.toRecord()
                    is EvidenceSequenceEntry.NonEvidence -> entry.event.toRecord()
                } }, chain.closedBy?.name)
        }
    )

    fun toDomain(record: LearningTrajectoryRecord): Pair<LearnerId, LearningTrajectory> {
        require(record.schemaVersion == LearningTrajectoryRecord.CURRENT_SCHEMA_VERSION)
        val contentId = ContentId(record.contentId)
        val chains = record.chains.map { chain ->
            val entries = chain.entries.map { it.toDomain(contentId) }
            val anchorEvidence = (entries.first() as EvidenceSequenceEntry.Recall).evidence
            val anchor = ChainAnchor(anchorEvidence.currentRating, anchorEvidence.timestamp, anchorEvidence,
                ChainAnchorReason.valueOf(chain.anchorReason))
            EvidenceChain.reconstitute(contentId, PromotionStage.valueOf(chain.stage), anchor, entries,
                chain.closedBy?.let(ChainResetReason::valueOf))
        }
        return LearnerId(record.learnerId) to LearningTrajectory.reconstitute(contentId, chains)
    }

    private fun RecallEvidence.toRecord() = EvidenceEntryRecord(
        "RECALL", reviewEventId.value, timestamp.epochMillis, currentRating.name, result.name,
        sessionId.value, sessionPolicy.name, provenance.name, wasRevealUsed,
        schedulerDue.epochMillis, typingLatency?.millis, origin.name, commitStatus.name
    )

    private fun NonEvidenceEvent.toRecord() = EvidenceEntryRecord(
        "NON_EVIDENCE", timestampMillis = timestamp.epochMillis, rating = rating.name,
        sessionId = sessionId.value, provenance = provenance.name
    )

    private fun EvidenceEntryRecord.toDomain(contentId: ContentId): EvidenceSequenceEntry =
        if (kind == "RECALL") EvidenceSequenceEntry.Recall(RecallEvidence(
            ReviewEventId(requireNotNull(reviewEventId)), contentId, Moment(timestampMillis),
            ReviewRating.valueOf(rating), RecallResult.valueOf(requireNotNull(result)), SessionId(sessionId),
            SessionEvaluationPolicy.valueOf(requireNotNull(sessionPolicy)), RatingSource.valueOf(provenance),
            revealUsed, Moment(requireNotNull(schedulerDueMillis)), typingLatencyMillis?.let(::TimeSpan),
            RecallEvidenceOrigin.valueOf(requireNotNull(origin)), RecallEvidenceCommitStatus.valueOf(requireNotNull(commitStatus))
        )) else EvidenceSequenceEntry.NonEvidence(NonEvidenceEvent(
            contentId, Moment(timestampMillis), SessionId(sessionId), ReviewRating.valueOf(rating), RatingSource.valueOf(provenance)
        ))
}
