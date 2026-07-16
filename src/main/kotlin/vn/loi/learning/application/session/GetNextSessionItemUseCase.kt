package vn.loi.learning.application.session

import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.study.GetNextLearningItemQuery
import vn.loi.learning.application.study.GetNextLearningItemUseCase
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionStatus

class GetNextSessionItemUseCase(
    private val sessionRepository: StudySessionRepository,
    private val getNextLearningItemUseCase:
    GetNextLearningItemUseCase
) {

    fun execute(
        sessionId: SessionId,
        now: Moment
    ): NextSessionItem? {
        val session = requireNotNull(
            sessionRepository.findById(sessionId)
        ) {
            "Session $sessionId does not exist."
        }

        require(session.status == SessionStatus.ACTIVE) {
            "Cannot get an item from a finished session."
        }

        if (
            !session.canReviewNewItem &&
            !session.canReviewDueItem
        ) {
            return null
        }

        val excludedItemIds =
            if (session.policy.allowRepeatInSameSession) {
                emptySet()
            } else {
                session.reviewedItemIds
            }

        val excludedContentIds =
            if (session.policy.allowRepeatInSameSession) {
                emptySet()
            } else {
                session.reviewedContentIds
            }

        val nextItem = getNextLearningItemUseCase.execute(
            GetNextLearningItemQuery(
                learnerId = session.learnerId,
                now = now,
                excludedItemIds = excludedItemIds,
                excludedContentIds = excludedContentIds,
                includeNewItems = session.canReviewNewItem,
                includeReviewItems = session.canReviewDueItem
            )
        ) ?: return null

        return NextSessionItem(
            session = session,
            item = nextItem
        )
    }
}