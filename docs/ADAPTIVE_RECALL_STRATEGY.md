# Adaptive Recall Strategy

LQ-006D consumes the decision at the next boundary and revalidates it before constructing a plan.
Only strategy may reselect a stale mode; the plan factory never repeats ranking.

LQ-006C separates capability detection from strategy selection. `RecallCapabilityProjection`
answers which mode/direction pairs content can support; `AdaptiveRecallStrategy` chooses the best
eligible pair for one learner attempt. The boundary ends at `RecallStrategyDecision` and does not
construct a prompt or `RecallPlan`.

## Typed inputs and decision

`RecallStrategyRequest` carries learner and Content identity, the LQ-006B capability projection,
optional authoritative `LearningDifficultyProfile` and `LearningRecommendation`, a read-only
promotion/evidence projection, bounded recent mode history, validated policy, deterministic seed,
and generated time. Missing intelligence lowers confidence and activates the safe fallback;
mismatched identities produce `InvalidRequest`.

The immutable decision contains the selected supported pair, typed confidence and reasons, ordered
eligible fallbacks, typed rejected candidates, policy/decision versions, seed, identity, and time.
Stable wire IDs, never enum ordinals, are the portable vocabulary.

## Ranking policy

Policy owns all weights and thresholds. Typing, Dictation, and Listening are strong recall;
Reverse Translation and Image Recall are standard recall; Example Completion is assisted context;
Multiple Choice is recognition. Difficult, unstable, evidence-building, promotion, and recovery
contexts favor strong recall. Stable/mastered and practice-only contexts permit efficient or
scaffolded diversity. Evaluative policy can disable recognition selection.

Repeated modes receive a diversity penalty unless strong-recall requirements apply. Candidates are
filtered by the Content projection before ranking. Directions come only from
`supportedDirectionsFor(mode)`. Fallbacks contain each eligible mode once in deterministic order.

Typing specifically prefers `TARGET_TO_SOURCE`: the target-language meaning is the cue and the
learner supplies the source-language text. Reverse Translation retains the distinct
`SOURCE_TO_TARGET` authority. Other modes retain their existing direction policy.

## Safety and portability

Low confidence or missing intelligence uses the deterministic preference Typing, Reverse
Translation, media-backed recall, Example Completion, then Multiple Choice. No eligible mode
returns typed `NoEligibleMode`.

The pure Shared Core implementation performs no persistence, scheduling, evidence/promotion
execution, rating, queue/session mutation, filesystem access, or UI work. Desktop, Android, iOS,
and Web do not own selection policy. This phase stops before RecallPlan generation.
