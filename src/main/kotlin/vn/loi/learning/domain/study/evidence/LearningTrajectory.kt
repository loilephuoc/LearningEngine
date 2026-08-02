package vn.loi.learning.domain.study.evidence

import vn.loi.learning.domain.content.model.ContentId

@ConsistentCopyVisibility
data class LearningTrajectory private constructor(
    val contentId: ContentId,
    val chains: List<EvidenceChain>
) {
    init {
        require(chains.isNotEmpty()) { "Learning trajectory must contain at least one chain." }
        require(chains.all { it.contentId == contentId }) {
            "Every trajectory chain must belong to the same Content."
        }
        require(chains.dropLast(1).all { it.closedBy != null }) {
            "Every historical trajectory chain must be closed."
        }
        require(chains.last().closedBy == null) {
            "The current trajectory chain must be open."
        }
    }

    fun currentChain(): EvidenceChain = chains.last()

    fun advance(evidence: RecallEvidence): LearningTrajectory =
        replaceCurrent(currentChain().advance(evidence))

    fun recordNonEvidence(event: NonEvidenceEvent): LearningTrajectory =
        replaceCurrent(currentChain().recordNonEvidence(event))

    fun reset(newAnchor: ChainAnchor, reason: ChainResetReason): LearningTrajectory {
        val reset = currentChain().reset(newAnchor, reason)
        return LearningTrajectory(
            contentId = contentId,
            chains = chains.dropLast(1) + reset.completedChain + reset.newChain
        )
    }

    private fun replaceCurrent(chain: EvidenceChain): LearningTrajectory =
        LearningTrajectory(contentId, chains.dropLast(1) + chain)

    companion object {
        fun reconstitute(contentId: ContentId, chains: List<EvidenceChain>): LearningTrajectory =
            LearningTrajectory(contentId, chains)

        fun start(chain: EvidenceChain): LearningTrajectory =
            LearningTrajectory(chain.contentId, listOf(chain))
    }
}
