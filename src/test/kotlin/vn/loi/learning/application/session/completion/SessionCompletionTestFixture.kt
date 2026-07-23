package vn.loi.learning.application.session.completion

import vn.loi.learning.application.decision.AdaptiveAction
import vn.loi.learning.application.decision.AdaptiveDecision
import vn.loi.learning.application.decision.DecisionExplanation
import vn.loi.learning.application.decision.DecisionTrace
import vn.loi.learning.application.scene.EvidencePerformance
import vn.loi.learning.application.scene.LearningEvidence
import vn.loi.learning.application.scene.SceneResult
import vn.loi.learning.application.session.bootstrap.LearningSessionContext
import vn.loi.learning.application.session.bootstrap.SessionPhase
import vn.loi.learning.application.session.bootstrap.SessionPhaseInfo
import vn.loi.learning.application.session.bootstrap.SessionTimeline
import vn.loi.learning.application.session.bootstrap.TeachingGoal

internal fun completionInput(
    performance: EvidencePerformance = EvidencePerformance.CORRECT,
    action: AdaptiveAction = AdaptiveAction.INCREASE_DIFFICULTY
): SessionCompletionInput {
    val decision =
        AdaptiveDecision(
            decisionId = "decision",
            action = action,
            targetSceneType = "TYPING_RECALL",
            newDifficultyLevel = 2,
            rationale = "Learner-facing rationale"
        )
    val evidence =
        LearningEvidence(
            evidenceId = "evidence",
            sceneId = "scene",
            learningItemId = "item",
            learnerId = "learner",
            performance = performance,
            userAttempt = "answer",
            attemptLatencyMs = 1_100L,
            timestamp = 1_000L
        )
    return SessionCompletionInput(
        context = LearningSessionContext("learner", "Medical Physics"),
        goal = TeachingGoal("DURABLE_RECALL", 1, "Medical Physics", "durable recall for Medical Physics."),
        sceneResult = SceneResult("scene", "answer", true, true, 1_100L, 0),
        evidence = evidence,
        decision = decision,
        decisionTrace =
            DecisionTrace(
                traceId = "trace",
                timestamp = 1_000L,
                evidenceId = evidence.evidenceId,
                rulesTriggered = listOf("INTERNAL_RULE"),
                decision = decision,
                explanation = "Technical trace"
            ),
        decisionExplanation =
            DecisionExplanation(
                explanationId = "explanation",
                decisionId = decision.decisionId,
                observation = "You answered correctly.",
                decisionSummary = "The challenge increased.",
                pedagogicalReason = "Recall was fluent.",
                nextStep = "Practice again at the scheduled time."
            ),
        timeline =
            SessionTimeline(
                phases = listOf(SessionPhaseInfo(SessionPhase.SUMMARY, "Summary", 1, 1)),
                totalEstimatedMinutes = 1
            ),
        finalDifficultyLevel = 2
    )
}
