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
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.session.model.SessionCompletionSnapshot
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.application.packageprogress.StudyHeaderStatistics
import vn.loi.learning.domain.study.confidence.model.MemoryConfidenceProjection

sealed interface StudyHeaderStatisticsState {
    data object Loading : StudyHeaderStatisticsState
    data class Available(val value: StudyHeaderStatistics) : StudyHeaderStatisticsState
    data class Unavailable(
        val lastKnownGood: StudyHeaderStatistics? = null
    ) : StudyHeaderStatisticsState
}

data class CurrentStudyItemReviewContext(
    val origin: SessionItemOrigin,
    val previousRating: ReviewRating?,
    val previousReviewAtMillis: Long? = null,
    val reviewedEarlierInCurrentSession: Boolean = false,
    val memoryContextReliable: Boolean = false,
    val itemPresentedAtEpochMillis: Long? = null,
    val easyConfidenceProjection: MemoryConfidenceProjection? = null
) {
    init {
        require(origin == SessionItemOrigin.REVIEW || previousRating == null) {
            "A New session item cannot have a previous-rating indicator."
        }
    }
}

enum class ContentIntroductionState {
    REQUIRED,
    COMPLETED,
    NOT_APPLICABLE
}

data class StudyUiState(
    val hasActiveSession: Boolean = false,
    val learnEntryChooserVisible: Boolean = false,
    val sessionStarted: Boolean = false,
    val topicId: String? = null,
    val activeInstalledPackageId: InstalledPackageId? = null,
    val activeContentId: ContentId? = null,
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
    val currentItemReviewContext: CurrentStudyItemReviewContext? = null,
    val typingRatingMode: TypingRatingMode = TypingRatingMode.STANDARD,
    val pendingTypingSuccessRequest: TypingRecallSuccessRequest? = null,
    val forcedTypingRevealRequest: TypingRecallRevealRequest? = null,
    val contentIntroductionState: ContentIntroductionState = ContentIntroductionState.NOT_APPLICABLE,
    val experienceRotationContext: ExperienceRotationContext? = null,
    val learningFlowDefinition: LearningFlowDefinition? = null,
    val learningFlowState: LearningFlowState? = null,
    val learningFlowProgress: LearningFlowProgress? = null,
    val learningFlowCurrentStage: LearningFlowStage? = null,
    val learningFlowSelection: ExperienceSelectionResult? = null,
    val learningExperiencePlan: LearningExperiencePlan? = null,
    val sessionCompleted: Boolean = false,
    val continuousReviewEnabled: Boolean = false,
    val ratingActionFeedback: RatingActionFeedback? = null,
    val loadError: String? = null,
    val failureKind: StudyFailureKind? = null,
    val schedulerFeedback:
    StudySchedulerFeedback? = null,
    val message: String = "Press Start Study",
    val learningContent: LearningContent? = null,
    val domainContent: vn.loi.learning.domain.content.model.Content? = null,
    val learningStage: vn.loi.learning.domain.study.memory.model.LearningStage? = null,
    val contentPresentationStage: vn.loi.learning.domain.study.memory.model.LearningStage? = null,
    val learningStageDiagnostics: LearningStageDiagnostics? = null,
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
    val headerStatistics: StudyHeaderStatisticsState = StudyHeaderStatisticsState.Loading,
    val learnEntryReviewAvailability:
        vn.loi.learning.application.session.LearnEntryReviewAvailability? = null,
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
                        "${progress.completedItemCount} of $total technical experiences completed"
                    } else {
                        "Technical experience ${progress.currentPosition} of $total · ${progress.completedItemCount} completed"
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
