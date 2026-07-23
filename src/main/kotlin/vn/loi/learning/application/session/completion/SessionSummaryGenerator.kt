package vn.loi.learning.application.session.completion

import vn.loi.learning.application.scene.EvidencePerformance

class SessionSummaryGenerator {
    fun generate(input: SessionCompletionInput): LearnerSessionSummary {
        val overallOutcome =
            when (input.evidence.performance) {
                EvidencePerformance.CORRECT ->
                    "You demonstrated successful recall in this session."

                EvidencePerformance.PARTIAL ->
                    "You demonstrated developing recall with a small accuracy gap."

                EvidencePerformance.INCORRECT ->
                    "This session identified material that needs another supported review."
            }

        return LearnerSessionSummary(
            whatWasLearned = "You practiced ${input.goal.description}",
            overallOutcome = overallOutcome,
            whatHappensNext = input.decisionExplanation.nextStep
        )
    }
}
