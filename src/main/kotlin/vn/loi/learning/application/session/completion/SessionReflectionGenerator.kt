package vn.loi.learning.application.session.completion

import vn.loi.learning.application.scene.EvidencePerformance
import vn.loi.learning.application.scene.LearningEvidence

class SessionReflectionGenerator {
    fun generate(evidence: LearningEvidence): SessionReflection =
        when (evidence.performance) {
            EvidencePerformance.CORRECT ->
                SessionReflection(
                    encouragement = "You recalled the answer successfully and completed this learning step.",
                    reinforcement = "Keep revisiting the idea on schedule so the recall remains durable."
                )

            EvidencePerformance.PARTIAL ->
                SessionReflection(
                    encouragement = "You were close, and the partial recall is useful evidence of progress.",
                    reinforcement = "Reinforce the exact answer during the next review to make recall more precise."
                )

            EvidencePerformance.INCORRECT ->
                SessionReflection(
                    encouragement = "This attempt identified a clear opportunity to strengthen your memory.",
                    reinforcement = "Review the answer again soon and focus on the part that was missed."
                )
        }
}
