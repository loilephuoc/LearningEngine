package vn.loi.learning.application.decision

import vn.loi.learning.application.scene.EvidencePerformance
import vn.loi.learning.application.scene.LearningEvidence
import java.util.UUID

/**
 * Pedagogical decision-making engine of Product Brain.
 */
class InstructionalDecisionEngine {

    fun evaluate(
        evidence: LearningEvidence,
        currentDifficulty: Int = 1
    ): Pair<AdaptiveDecision, DecisionTrace> {
        val decisionId = "dec-" + UUID.randomUUID().toString().take(8)
        val traceId = "trc-" + UUID.randomUUID().toString().take(8)

        val (action, newDifficulty, rules, rationale) = when {
            evidence.performance == EvidencePerformance.CORRECT && evidence.attemptLatencyMs < 2000L -> {
                val nextDiff = currentDifficulty + 1
                Tuple4(
                    AdaptiveAction.INCREASE_DIFFICULTY,
                    nextDiff,
                    listOf("FAST_CORRECT_MASTERY_BOOST"),
                    "Learner answered correctly in ${evidence.attemptLatencyMs}ms (<2000ms). Increasing difficulty to level $nextDiff."
                )
            }
            evidence.performance == EvidencePerformance.INCORRECT -> {
                val nextDiff = maxOf(1, currentDifficulty - 1)
                Tuple4(
                    AdaptiveAction.DECREASE_DIFFICULTY,
                    nextDiff,
                    listOf("INCORRECT_SCAFFOLDING_SUPPORT"),
                    "Learner answer was incorrect. Decreasing difficulty to level $nextDiff for scaffolding."
                )
            }
            evidence.performance == EvidencePerformance.PARTIAL -> {
                Tuple4(
                    AdaptiveAction.REPEAT_SIMILAR_SCENE,
                    currentDifficulty,
                    listOf("PARTIAL_CONSOLIDATION_RETRY"),
                    "Learner answer was partially correct. Repeating similar scene for consolidation."
                )
            }
            else -> {
                Tuple4(
                    AdaptiveAction.MAINTAIN_PACE,
                    currentDifficulty,
                    listOf("NOMINAL_PROGRESSION"),
                    "Learner answered correctly at nominal pace. Maintaining current difficulty level $currentDifficulty."
                )
            }
        }

        val decision = AdaptiveDecision(
            decisionId = decisionId,
            action = action,
            targetSceneType = "TYPING_RECALL",
            newDifficultyLevel = newDifficulty,
            rationale = rationale
        )

        val trace = DecisionTrace(
            traceId = traceId,
            timestamp = System.currentTimeMillis(),
            evidenceId = evidence.evidenceId,
            rulesTriggered = rules,
            decision = decision,
            explanation = rationale
        )

        return Pair(decision, trace)
    }

    private data class Tuple4<A, B, C, D>(
        val val1: A,
        val val2: B,
        val val3: C,
        val val4: D
    )
}
