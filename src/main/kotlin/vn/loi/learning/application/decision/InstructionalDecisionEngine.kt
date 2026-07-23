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

    fun generateExplanation(
        decision: AdaptiveDecision,
        evidence: LearningEvidence
    ): DecisionExplanation {
        val expId = "exp-" + UUID.randomUUID().toString().take(8)

        val (obs, summary, reason, next) = when (decision.action) {
            AdaptiveAction.INCREASE_DIFFICULTY -> Tuple4(
                "You answered correctly in ${evidence.attemptLatencyMs}ms (under 2 seconds).",
                "Product Brain is increasing the challenge level to level ${decision.newDifficultyLevel}.",
                "Fast, accurate recall demonstrates high fluency and strong memory stability.",
                "Presenting higher difficulty prompts to build deeper mastery."
            )
            AdaptiveAction.DECREASE_DIFFICULTY -> Tuple4(
                "The response was incorrect.",
                "Product Brain is easing the challenge level to level ${decision.newDifficultyLevel}.",
                "Lowering difficulty provides supportive scaffolding to rebuild recall confidence.",
                "Presenting a clearer prompt with foundational support."
            )
            AdaptiveAction.REPEAT_SIMILAR_SCENE -> Tuple4(
                "The response was almost correct with a slight typo or partial match.",
                "Product Brain is scheduling a similar practice scene.",
                "Near-correct attempts benefit from immediate consolidation retry.",
                "Presenting a similar recall exercise to solidify accuracy."
            )
            AdaptiveAction.MAINTAIN_PACE -> Tuple4(
                "You answered correctly at a steady pace.",
                "Product Brain is maintaining the current challenge level at level ${decision.newDifficultyLevel}.",
                "Steady progress confirms an appropriate learning pace without overloading.",
                "Continuing practice at the current difficulty level."
            )
        }

        return DecisionExplanation(
            explanationId = expId,
            decisionId = decision.decisionId,
            observation = obs,
            decisionSummary = summary,
            pedagogicalReason = reason,
            nextStep = next
        )
    }

    private data class Tuple4<A, B, C, D>(
        val val1: A,
        val val2: B,
        val val3: C,
        val val4: D
    )
}
