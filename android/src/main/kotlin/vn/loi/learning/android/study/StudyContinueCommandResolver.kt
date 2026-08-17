package vn.loi.learning.android.study

import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.FocusedPracticeKind

/**
 * Pure domain/presentation authority resolving the canonical continue command / swipe-up action
 * for both physical UI swipe gestures and controller CONTINUE_CURRENT_MODE actions.
 */
object StudyContinueCommandResolver {

    /**
     * Resolves the canonical AndroidStudyEvent to dispatch when a Continue / Swipe-up action occurs.
     * Returns null if Continue is not applicable for the given state.
     */
    fun resolveContinueEvent(state: AndroidStudyState): AndroidStudyEvent? {
        return when (state) {
            is AndroidStudyState.Introduction -> {
                if (state.historyPreview) {
                    if (state.navigation.canNext) AndroidStudyEvent.NextVisited else null
                } else if (usesFocusedSkimUx(state.focusedPracticeKind)) {
                    when (state.focusedPracticeKind) {
                        FocusedPracticeKind.QUICK_REVIEW -> AndroidStudyEvent.QuickReviewUnratedAdvance
                        FocusedPracticeKind.DIFFICULT -> AndroidStudyEvent.DifficultPracticeAdvance
                        else -> if (state.revealedStage) AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD) else null
                    }
                } else {
                    // Canonical Learn New: progress with GOOD rating when revealed
                    if (state.revealedStage) {
                        AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD)
                    } else {
                        null
                    }
                }
            }
            is AndroidStudyState.Runtime -> {
                if (state.navigation.canNext) {
                    AndroidStudyEvent.NextVisited
                } else if (state.completed) {
                    AndroidStudyEvent.Next
                } else {
                    null
                }
            }
            else -> null
        }
    }
}
