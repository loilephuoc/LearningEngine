# Cross-Platform Recall Contract

The Recall contract is the Shared Core authority for describing a recall interaction independently
of any client UI. Its governing rule is: **Port UI, reuse recall intelligence.** Desktop is the
reference renderer; Android, iOS, and Web inherit the same plan, submission, validation, result,
wire, and determinism semantics.

## Authority flow

```text
Content + learner state + recall policy + typed seed/clock
                          ↓
                      RecallPlan
                          ↓
                   Platform renderer
                          ↓
                   RecallSubmission
                          ↓
              RecallContractValidator / evaluator
                          ↓
                     RecallResult
```

LQ-006A defines the contracts and validators only. It does not select a mode adaptively, generate
multiple-choice distractors, evaluate typing, write `ReviewEvent`, schedule, mutate memory, advance a
queue, or execute evidence promotion.

## Plan and content capabilities

`RecallPlan` is immutable and identified by version, plan, learner, Content, optional LearningItem,
and session. Content remains the memory authority even when sibling LearningItems execute the plan.
The plan carries a mode, direction, sealed prompt, answer contract, available assistance, evidence
class placeholder, deterministic seed, generated time, provenance, platform requirements, and a
Content capability projection.

`RecallContentCapabilityResolver` derives capabilities exclusively from existing `Content.text` and
`Content.media`: source text, translation, pronunciation, POS, image, word audio, example source,
example translation, and example audio. Media is represented by portable typed resource identifiers,
never `Path`, `File`, streams, or platform objects. Content gains no hint, choice, or distractor data.

## Modes, prompts, submissions, and results

Supported modes have stable wire identifiers: typing, multiple choice, listening, image recall,
reverse translation, example completion, and dictation. Directions are also typed. Each prompt is a
sealed mode-specific value; no untyped map is part of the public API.

The answer contract preserves the canonical answer, explicit alternatives, normalization-policy
reference, case/punctuation/whitespace rules, expected language, and answer kind. It does not replace
or duplicate the existing typing evaluator.

Platforms return a sealed `RecallSubmission` with a shared context containing plan/attempt/learner/
Content/session identity, mode, timestamp, immutable assistance state, retry count, and platform
provenance. Platform kind is diagnostic provenance only and never decides correctness.

`RecallResult` is a passive normalized output. Outcomes are not mapped to Again/Hard/Good/Easy.
Evidence eligibility is categorical (`STRONG`, `STANDARD`, `WEAK`, `INELIGIBLE`) with no numeric
weights. Reveal, practice, and manual provenance are ineligible by the explicit placeholder policy.

## Validation, determinism, and portability

`RecallContractValidator` is pure. It checks schema support, prompt/mode consistency, required media
and examples, multiple-choice correctness cardinality, submission kind/mode compatibility, identity,
and duplicate attempt IDs supplied by the workflow boundary. It neither stores duplicate state nor
performs side effects.

`RecallDeterministicSeed` and `RecallClock` make seed and time explicit inputs. The wire codec orders
sets deterministically and serializes stable identifiers, never enum ordinals or process hashes.
`RecallContractVersion` allows old clients to return a typed unsupported-version result rather than
silently misread a plan. The flat DTO boundary is JSON-ready and round-trips plan semantics.

## Platform responsibility

Clients render only the typed platform requirements (text input, choice selection, audio playback,
image rendering, or example rendering), collect a typed submission, and display the shared result.
They must not select modes, construct answer authority, determine correctness, infer evidence value,
or parse raw Content metadata. No Desktop wiring or mode UI is delivered by LQ-006A.

## Valid contract example

A typing plan uses `RecallPrompt.Typing`, `RecallMode.TYPING`, a source-to-target direction, a text
answer contract, `requiresTextInput = true`, explicit seed and clock values, and capabilities derived
from the same `ContentId`. `RecallContractValidator.validatePlan` returns `Valid`; encoding then
decoding through `RecallPlanWireCodec` preserves the complete plan.
LQ-006B specializes the Content side of this contract through the pure, ContentId-owned capability
projection documented in [`CONTENT_RECALL_CAPABILITIES.md`](CONTENT_RECALL_CAPABILITIES.md). It
answers data eligibility only; mode selection remains outside the resolver.
