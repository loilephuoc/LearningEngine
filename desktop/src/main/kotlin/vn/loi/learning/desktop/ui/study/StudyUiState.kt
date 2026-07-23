package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.learningcontent.LearningContent
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.learningexperience.ExperienceSelectionResult
import vn.loi.learning.application.learningexperience.LearningExperiencePlan
import vn.loi.learning.application.learningflow.LearningFlowDefinition
import vn.loi.learning.application.learningflow.LearningFlowProgress
import vn.loi.learning.application.learningflow.LearningFlowStage
import vn.loi.learning.application.learningflow.LearningFlowState
import vn.loi.learning.application.session.LearningSessionProgress

import vn.loi.learning.application.decision.AdaptiveDecision
import vn.loi.learning.application.decision.DecisionExplanation
import vn.loi.learning.application.decision.DecisionTrace
import vn.loi.learning.application.scene.LearningEvidence
import vn.loi.learning.application.scene.LearningScene
import vn.loi.learning.application.scene.SceneResult
import vn.loi.learning.application.session.bootstrap.SessionOverview
import vn.loi.learning.domain.study.session.model.SessionCompletionSnapshot

data class StudyUiState(
    val hasActiveSession: Boolean = false,
    val sessionStarted: Boolean = false,
    val studyTitle: String = "All learning items",
    val isLessonStudy: Boolean = false,
    val contentText: String = "--",
    val translationText: String = "--",
    val canRevealAnswer: Boolean = false,
    val canReview: Boolean = false,
    val canUndo: Boolean = false,
    val actionInProgress: Boolean = false,
    val reviewedCount: Int = 0,
    val newItemsReviewed: Int = 0,
    val reviewItemsReviewed: Int = 0,
    val totalItems: Int = 0,
    val currentItemPosition: Int = 0,
    val currentLearningItemId: String? = null,
    val experienceRotationContext: ExperienceRotationContext? = null,
    val learningFlowDefinition: LearningFlowDefinition? = null,
    val learningFlowState: LearningFlowState? = null,
    val learningFlowProgress: LearningFlowProgress? = null,
    val learningFlowCurrentStage: LearningFlowStage? = null,
    val learningFlowSelection: ExperienceSelectionResult? = null,
    val learningExperiencePlan: LearningExperiencePlan? = null,
    val sessionCompleted: Boolean = false,
    val loadError: String? = null,
    val failureKind: StudyFailureKind? = null,
    val schedulerFeedback:
    StudySchedulerFeedback? = null,
    val message: String = "Press Start Study",
    val learningContent: LearningContent? = null,
    val sessionProgress: LearningSessionProgress? = null,
    val sessionOverview: SessionOverview? = null,
    val isSessionOverviewVisible: Boolean = false,
    val activeScene: LearningScene? = null,
    val lastSceneResult: SceneResult? = null,
    val lastLearningEvidence: LearningEvidence? = null,
    val lastAdaptiveDecision: AdaptiveDecision? = null,
    val lastDecisionTrace: DecisionTrace? = null,
    val lastDecisionExplanation: DecisionExplanation? = null,
    val isDecisionExplanationVisible: Boolean = false,
    val sessionCompletion: SessionCompletionSnapshot? = null,
    val currentDifficultyLevel: Int = 1,
    val workspaceState: ReviewWorkspaceState =




        ReviewWorkspaceState.projectLegacy(
            hasActiveSession = hasActiveSession,
            canRevealAnswer = canRevealAnswer,
            canReview = canReview,
            sessionCompleted = sessionCompleted,
            hasLoadError = loadError != null
        )
) {

    val hasKnownTotal: Boolean
        get() =
            sessionProgress?.totalIsKnown ?: (totalItems > 0)

    val progress: Float
        get() {
            sessionProgress?.fractionComplete?.let { return it.toFloat() }
            if (!hasKnownTotal) {
                return 0f
            }

            if (sessionCompleted) {
                return 1f
            }

            return (
                    reviewedCount.toFloat() /
                            totalItems.toFloat()
                    ).coerceIn(
                    minimumValue = 0f,
                    maximumValue = 1f
                )
        }

    val progressLabel: String
        get() {
            sessionProgress?.let { progress ->
                val total = progress.totalItemCount
                if (total != null) {
                    return if (progress.isCompleted) {
                        "${progress.completedItemCount} of $total completed"
                    } else {
                        "Item ${progress.currentPosition} of $total · ${progress.completedItemCount} completed"
                    }
                }
                return "${progress.reviewedItemCount} reviewed · total unknown"
            }
            if (!hasKnownTotal) {
                return reviewedCount.toString()
            }

            if (sessionCompleted) {
                return "$totalItems of $totalItems"
            }

            return "$currentItemPosition of $totalItems"
        }
}

enum class StudyFailureKind {
    PREPARATION,
    SESSION_RECOVERY,
    REVIEW_TRANSACTION,
    UNDO,
    CONTENT
}
