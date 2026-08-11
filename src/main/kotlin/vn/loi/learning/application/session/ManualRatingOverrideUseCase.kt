package vn.loi.learning.application.session

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.application.review.ReviewResult
import vn.loi.learning.application.study.ContentLearningStateQueryService
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.RatingSource
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.UndoableSessionReview

data class ManualRatingOverrideCommand(
    val sessionId: SessionId,
    val reviewEventId: ReviewEventId,
    val learningItemId: LearningItemId,
    val expectedCurrentRating: ReviewRating?,
    val selectedRating: ReviewRating,
    val overriddenAt: Moment
)

data class ManualRatingOverrideResult(
    val session: vn.loi.learning.domain.study.session.model.StudySession,
    val reviewResult: ReviewResult
)

enum class ManualRatingOverrideAvailability {
    AVAILABLE,
    NOT_PRACTICE,
    NO_COMMITTED_RATING
}

object ManualRatingOverrideAvailabilityResolver {
    fun resolve(
        policy: SessionPolicy,
        currentRating: ReviewRating?
    ): ManualRatingOverrideAvailability = when {
        policy.evaluationPolicy != SessionEvaluationPolicy.PRACTICE_ONLY ->
            ManualRatingOverrideAvailability.NOT_PRACTICE
        currentRating == null -> ManualRatingOverrideAvailability.NO_COMMITTED_RATING
        else -> ManualRatingOverrideAvailability.AVAILABLE
    }
}

/** Explicit evaluative mutation that does not turn the enclosing practice session evaluative. */
class ManualRatingOverrideUseCase(
    private val sessions: StudySessionRepository,
    private val learningItems: LearningItemRepository,
    private val contentStates: ContentLearningStateQueryService,
    private val review: ReviewLearningItemUseCase,
    private val transactions: TransactionRunner,
    private val queues: StudyQueueService? = null
) {
    fun execute(command: ManualRatingOverrideCommand): ManualRatingOverrideResult =
        transactions.runInTransaction {
            val session = requireNotNull(sessions.findById(command.sessionId))
            require(session.policy.evaluationPolicy == SessionEvaluationPolicy.PRACTICE_ONLY)
            require(session.policy.focusedPracticeKind ==
                vn.loi.learning.domain.study.session.model.FocusedPracticeKind.NONE) {
                "Focused practice cannot mutate canonical learning state."
            }
            require(session.currentLearningItemId == command.learningItemId)
            val item = requireNotNull(learningItems.findById(command.learningItemId))
            require(
                contentStates.resolve(session.learnerId, item.contentId).latestEffectiveRating ==
                    command.expectedCurrentRating
            ) { "Current rating changed before manual override confirmation." }
            val result = review.execute(
                ReviewCommand(
                    command.reviewEventId,
                    session.learnerId,
                    command.learningItemId,
                    command.selectedRating,
                    command.overriddenAt,
                    source = RatingSource.MANUAL_USER_OVERRIDE
                )
            )
            if (session.policy.practiceLoopPolicy ==
                vn.loi.learning.domain.study.session.model.PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP) {
                requireNotNull(queues).updateDifficultPracticeMembership(
                    command.sessionId, command.learningItemId, command.selectedRating
                )
            } else if (session.policy.practiceLoopPolicy ==
                vn.loi.learning.domain.study.session.model.PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED) {
                requireNotNull(queues).updateAdaptivePracticePriority(
                    command.sessionId, command.learningItemId, command.selectedRating
                )
            }
            val updated = session.recordManualOverride(
                UndoableSessionReview(
                    reviewEventId = result.reviewEvent.id,
                    learningItemId = command.learningItemId,
                    contentId = item.contentId,
                    memoryStateBefore = result.reviewEvent.stateBefore,
                    memoryStateExistedBefore = result.memoryStateExistedBefore,
                    reviewedItemIdsBefore = session.reviewedItemIds,
                    reviewedContentIdsBefore = session.reviewedContentIds,
                    lapsedContentIdsBefore = session.lapsedContentIds,
                    newItemsReviewedBefore = session.newItemsReviewed,
                    reviewItemsReviewedBefore = session.reviewItemsReviewed,
                    currentItemPresentedAtBefore = session.currentItemPresentedAt,
                    answerRevealedBefore = session.answerRevealed,
                    advancesSessionProgress = false
                )
            )
            sessions.save(updated)
            ManualRatingOverrideResult(updated, result)
        }
}
