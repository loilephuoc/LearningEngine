package vn.loi.learning.android.study

import android.util.Log
import vn.loi.learning.android.BuildConfig
import vn.loi.learning.application.session.ContinuousSkimPriorityPolicy
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy
import vn.loi.learning.domain.study.session.model.StudySession

internal object AndroidContinuousSkimTrace {
    private const val TAG = "ContinuousSkimTrace"

    fun selection(session: StudySession, queue: StudyQueueSnapshot, status: String?) {
        if (!BuildConfig.DEBUG || status == null) return
        val item = queue.currentLearningItemId ?: return
        val contentId = queue.itemContentIds[item]?.value ?: return
        val state = queue.practiceReinforcementStates[item]
        val phase = if (queue.practiceLoopPolicy == PracticeLoopPolicy.NONE) "COVERAGE" else "CONTINUOUS"
        val reason = if (phase == "COVERAGE" && contentId in session.reviewedContentIds.map { it.value }) {
            "REINFORCEMENT"
        } else if (phase == "COVERAGE") "UNIQUE_COVERAGE" else "PRACTICE_ROUND"
        val gap = state?.lastExposureSequence?.let { queue.practiceExposureSequence - it }
        runCatching {
            Log.d(
                TAG,
                "SKIM select phase=$phase round=${queue.practiceRound} selectedContentId=$contentId " +
                    "latestRating=${state?.latestFeedback ?: "NONE"} " +
                    "priority=${ContinuousSkimPriorityPolicy.tier(state?.latestFeedback)} " +
                    "cardsSinceLastExposure=${gap ?: -1} currentSessionExposureCount=${state?.exposureCount ?: 0} " +
                    "coverageUniqueNew=${session.newItemsReviewed} coverageUniqueReview=${session.reviewItemsReviewed} " +
                    "reinforcementReason=$reason"
            )
        }
    }
}
