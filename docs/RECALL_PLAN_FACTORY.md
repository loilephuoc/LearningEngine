# Cross-Platform Recall Plan Factory

LQ-006G changes no plan construction semantics; only validated execution results may reach the
existing learning transaction through the integration bridge.

LQ-006F consumes the completed plan at the next pure boundary. Every plan is revalidated before a
submission is evaluated; execution never changes plan selection or construction.

LQ-006E supplies the real deterministic Multiple Choice provider documented in
[`MULTIPLE_CHOICE_GENERATOR.md`](MULTIPLE_CHOICE_GENERATOR.md). Provider business failures remain
typed and the factory continues to reject invalid option sets.

LQ-006D is the boundary from an LQ-006C `RecallStrategyDecision` to a complete LQ-006A
`RecallPlan`. Shared Core plans; platforms render. The factory revalidates learner/Content identity,
current capability availability, supported direction, and contract version without rerunning or
duplicating strategy scoring.

## Request, identity, and deterministic generation

`RecallPlanRequest` carries the decision, authoritative Content snapshot and capability projection,
learner/item/session identity, typed attempt nonce, policy, version, seed, generated time, and
evaluative/practice context. Plan identity is derived deterministically from those identity facts,
mode, direction, seed, and contract version. A different attempt nonce produces a distinct ID.

The final plan always passes `RecallContractValidator`. Unsupported versions, stale decisions,
identity/direction mismatches, missing handlers/providers, invalid provider output, leakage, and
final validation failures are typed results rather than silent fallback selection.

## Prompt and answer construction

An immutable registry dispatches one factory per mode and reports duplicate or missing handlers.
Typing and Reverse Translation select prompt/answer sides from the chosen direction. Listening,
Image Recall, and Dictation retain typed resource IDs without loading media. Example Completion
uses the exact safe LQ-006B span and replaces only that occurrence with a visible blank.

Every factory reuses `RecallAnswerContract` with policy-owned normalization, case, punctuation,
whitespace, and language contracts. Accepted alternatives remain empty because Content has no
separate accepted-answer authority. Assistance is the intersection of current Content capability
and the mode-specific safe set. Platform requirements contain interaction capabilities only—no
layout, styling, permission, or platform objects.

## Multiple Choice and leakage

Multiple Choice depends on an injected `MultipleChoiceOptionProvider`. Without it the factory
returns `ExternalGenerationRequired`; invalid choice identity/count/canonical correctness returns a
typed generation failure. The factory never invents distractors or scans storage. Provider choice
ordering is preserved and therefore forms part of deterministic provider output.

Front text and example context pass leakage validation. Media prompts contain only typed resource
references; the factory adds no answer-bearing accessibility or media metadata.

## Evaluation context and portability

Evaluative plans use evaluative provenance and the existing standard placeholder eligibility.
Practice-only plans use practice provenance and are always evidence-ineligible. Neither path
executes grading, evidence, promotion, scheduling, persistence, queue, or session mutation.

The contracts use Shared Core values only and are portable to Desktop JVM, Android, iOS adapters,
and Web/service boundaries. This phase stops before rendering, submission evaluation, and recall
result execution.
