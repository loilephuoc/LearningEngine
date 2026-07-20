package vn.loi.learning.domain.study.selection.pipeline

import vn.loi.learning.domain.study.selection.model.NoSelectionReason
import vn.loi.learning.domain.study.selection.model.SessionSelectionRequest
import vn.loi.learning.domain.study.selection.model.SessionSelectionResult
import vn.loi.learning.domain.study.session.model.SessionStatus

/**
 * Điều phối toàn bộ Session Selection Pipeline.
 *
 * Pipeline thực hiện theo thứ tự:
 * 1. Kiểm tra trạng thái session.
 * 2. Kiểm tra quota tổng.
 * 3. Lọc candidate eligible.
 * 4. Lọc theo quota new/review.
 * 5. Lọc item đã lặp trong session.
 * 6. Sắp xếp theo priority.
 * 7. Áp dụng sibling avoidance.
 */
class SelectionPipeline(
    private val eligibilityFilter: EligibilityFilter = EligibilityFilter(),
    private val sessionQuotaFilter: SessionQuotaFilter = SessionQuotaFilter(),
    private val repeatFilter: RepeatFilter = RepeatFilter(),
    private val priorityOrdering: PriorityOrdering = PriorityOrdering(),
    private val siblingAvoidance: SiblingAvoidance = SiblingAvoidance()
) {

    fun execute(
        request: SessionSelectionRequest
    ): SessionSelectionResult {

        val session = request.session

        if (session.status == SessionStatus.FINISHED) {
            return noSelection(NoSelectionReason.SESSION_FINISHED)
        }

        if (!session.canReviewNewItem && !session.canReviewDueItem) {
            return noSelection(NoSelectionReason.SESSION_LIMIT_REACHED)
        }

        val eligibleCandidates =
            eligibilityFilter.filter(
                candidates = request.candidates,
                at = request.at
            )

        if (eligibleCandidates.isEmpty()) {
            return noSelection(
                NoSelectionReason.NO_ELIGIBLE_CANDIDATES
            )
        }

        val candidatesWithinSessionLimits =
            sessionQuotaFilter.filter(
                candidates = eligibleCandidates,
                session = session
            )

        if (candidatesWithinSessionLimits.isEmpty()) {
            return noSelection(
                NoSelectionReason.SESSION_LIMIT_REACHED
            )
        }

        val repeatFilteredCandidates =
            repeatFilter.filter(
                candidates = candidatesWithinSessionLimits,
                session = session
            )

        if (repeatFilteredCandidates.isEmpty()) {
            return noSelection(
                NoSelectionReason.REPEAT_NOT_ALLOWED
            )
        }

        val orderedCandidates =
            priorityOrdering.order(
                candidates = repeatFilteredCandidates,
                priority = request.selectionPolicy.priority
            )

        return siblingAvoidance.select(
            orderedCandidates = orderedCandidates,
            previousContentId = request.previousContentId,
            policy = request.selectionPolicy
        )
    }

    private fun noSelection(
        reason: NoSelectionReason
    ): SessionSelectionResult =
        SessionSelectionResult.NoSelection(reason)
}