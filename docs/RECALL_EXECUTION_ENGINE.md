# Cross-Platform Recall Execution Engine

LQ-006F defines the pure Shared Core boundary `RecallPlan + RecallSubmission -> RecallResult`.
Platforms render plans, collect typed submissions, invoke this engine, and render results. They do
not normalize answers, grade text or choices, resolve terminal outcomes, classify evidence, or own
duplicate-attempt behavior.

## Validation and evaluator registry

The engine revalidates contract version and the complete LQ-006A plan, then checks plan/session/
learner/Content identity, mode/submission kind, timestamps, evaluation context, attempt snapshot,
and assistance state. An immutable registry dispatches one evaluator for every recall mode and
reports duplicate or missing handlers without reflection or platform DI.

Typing, Listening, Image Recall, Reverse Translation, Example Completion, and Dictation consume
only `TypedText` and the plan's answer contract. Multiple Choice resolves the submitted option ID
against the validated plan and never trusts display text, index, or platform correctness. Playback
acknowledgement is incomplete and cannot become a correct recall.

## Normalization and outcomes

Typing-compatible contracts reuse `TypingAnswerEvaluator` normalization. The broader answer
contract evaluator applies explicit Unicode NFC, case, whitespace, and punctuation policies and
checks only canonical plus declared accepted alternatives. It never removes accents, translates,
spell-corrects, or performs fuzzy grading. Results distinguish exact, normalized, incorrect, and
not-applicable correctness.

Reveal, skip, and timeout become typed terminal outcomes. Reveal adds the typed reveal assistance;
unsupported assistance or contradictory NONE/reveal combinations are rejected. Latency derives
only from plan/submission timestamps and cannot be negative.

## Evidence, context, and idempotency

Policy centralizes eligibility. Evaluative correct text is capped by the plan's eligibility;
recognition and assisted correctness are weak by default. Reveal, non-correct terminal outcomes,
and every practice-only result are ineligible. This is classification only—no Evidence, rating,
Scheduler, trajectory, queue, or session command runs.

The caller supplies an immutable attempt snapshot. Known attempt IDs return typed duplicate results
without mutation. Same plan, submission, policy, context, and snapshot produce the same result.
`RecallResultWireCodec` preserves outcome, correctness, assistance, eligibility, evaluator/policy
versions, platform telemetry, and deterministic attempt identity.

The implementation depends on no Desktop, Compose, Android SDK, filesystem, media player, browser,
AI/network, or mutable global state. It is portable to Desktop JVM, Android, iOS adapters, and Web
service boundaries. This phase stops before learning execution integration.
