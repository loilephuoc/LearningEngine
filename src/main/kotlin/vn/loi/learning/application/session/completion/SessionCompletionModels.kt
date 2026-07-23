package vn.loi.learning.application.session.completion

import vn.loi.learning.application.decision.AdaptiveDecision
import vn.loi.learning.application.decision.DecisionExplanation
import vn.loi.learning.application.decision.DecisionTrace
import vn.loi.learning.application.scene.LearningEvidence
import vn.loi.learning.application.scene.SceneResult
import vn.loi.learning.application.session.bootstrap.LearningSessionContext
import vn.loi.learning.application.session.bootstrap.SessionTimeline
import vn.loi.learning.application.session.bootstrap.TeachingGoal
import vn.loi.learning.domain.study.memory.model.ReviewRating

data class SessionCompletionInput(
    val context: LearningSessionContext,
    val goal: TeachingGoal,
    val sceneResult: SceneResult,
    val evidence: LearningEvidence,
    val decision: AdaptiveDecision,
    val decisionTrace: DecisionTrace,
    val decisionExplanation: DecisionExplanation,
    val timeline: SessionTimeline,
    val finalDifficultyLevel: Int
)

data class SessionReflection(
    val encouragement: String,
    val reinforcement: String
)

data class LearnerSessionSummary(
    val whatWasLearned: String,
    val overallOutcome: String,
    val whatHappensNext: String
)

data class SessionLearningOutcome(
    val demonstratedLearning: String,
    val needsReinforcement: Boolean
)

data class SessionSchedulingOutcome(
    val rating: ReviewRating,
    val scheduledIntervalMillis: Long,
    val nextReviewAtEpochMillis: Long,
    val guidance: String
)

data class SessionCompletionPlan(
    val input: SessionCompletionInput,
    val reflection: SessionReflection,
    val summary: LearnerSessionSummary,
    val learningOutcome: SessionLearningOutcome,
    val recommendedRating: ReviewRating
)

data class SessionCompletionResult(
    val sessionId: String,
    val completedAtEpochMillis: Long,
    val input: SessionCompletionInput,
    val reflection: SessionReflection,
    val summary: LearnerSessionSummary,
    val learningOutcome: SessionLearningOutcome,
    val schedulingOutcome: SessionSchedulingOutcome
)
