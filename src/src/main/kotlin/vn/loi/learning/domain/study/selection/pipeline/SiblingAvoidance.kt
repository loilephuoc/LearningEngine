package vn.loi.learning.domain.study.selection.pipeline

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.selection.model.NoSelectionReason
import vn.loi.learning.domain.study.selection.model.SelectionCandidate
import vn.loi.learning.domain.study.selection.model.SessionSelectionResult
import vn.loi.learning.domain.study.selection.policy.SessionSelectionPolicy

/**
 * Chọn candidate đầu tiên phù hợp với quy tắc tránh sibling liên tiếp.
 *
 * Hai LearningItem được xem là sibling khi chúng có cùng ContentId.
 */
class SiblingAvoidance {

    fun select(
        orderedCandidates: List<SelectionCandidate>,
        previousContentId: ContentId?,
        policy: SessionSelectionPolicy
    ): SessionSelectionResult {

        require(orderedCandidates.isNotEmpty()) {
            "SiblingAvoidance requires at least one ordered candidate."
        }

        if (
            !policy.avoidConsecutiveSiblings ||
            previousContentId == null
        ) {
            return selected(
                candidate = orderedCandidates.first(),
                usedSiblingFallback = false
            )
        }

        val nonSiblingCandidate =
            orderedCandidates.firstOrNull { candidate ->
                candidate.contentId != previousContentId
            }

        if (nonSiblingCandidate != null) {
            return selected(
                candidate = nonSiblingCandidate,
                usedSiblingFallback = false
            )
        }

        if (policy.allowSiblingFallback) {
            return selected(
                candidate = orderedCandidates.first(),
                usedSiblingFallback = true
            )
        }

        return SessionSelectionResult.NoSelection(
            reason = NoSelectionReason.SIBLING_BLOCKED
        )
    }

    private fun selected(
        candidate: SelectionCandidate,
        usedSiblingFallback: Boolean
    ): SessionSelectionResult =
        SessionSelectionResult.Selected(
            candidate = candidate,
            usedSiblingFallback = usedSiblingFallback
        )
}