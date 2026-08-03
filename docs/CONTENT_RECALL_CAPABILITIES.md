# Content Recall Capabilities

LQ-006C consumes this projection unchanged: capability detection answers what Content supports;
adaptive strategy answers which supported pair should be used for this attempt.

`ContentRecallCapabilityResolver` is the Shared Application authority for one question only:
**which recall modes are data-capable for this Content?** It does not choose which mode should run.
Adaptive, learner-specific selection belongs to LQ-006C.

## Authority and boundary

The resolver accepts the existing `Content` aggregate and returns an immutable
`RecallCapabilityProjection` identified only by `ContentId`. Sibling LearningItems therefore receive
the same projection. It reads no learner, difficulty, recommendation, evidence, scheduler, rating,
session-progress, or recall-history state. It has no clock, random source, repository write, cache,
network call, platform API, or filesystem probe.

The projection reuses the LQ-006A `RecallContentCapabilities`, `RecallMode`, `RecallDirection`,
`RecallAssistance`, stable wire identifiers, and contract version. Content gains no hint, choice,
distractor, generated prompt, recall-mode, or `supportsX` field.

## Typed projection

`RecallCapabilitySet` is an immutable set authority with `supports(mode)` and stable wire ordering.
Every LQ-006A mode has one `RecallModeEligibility`: an available mode has one or more typed
directions and no failure reasons; an unavailable mode has no directions and one or more typed
`RecallUnavailableReason` values. The projection also exposes typed media, lexical, contextual, and
assistance facts plus an optional safe example-completion span.

Media facts distinguish word image, word audio, and example audio. Lexical facts distinguish source
text, target text, pronunciation, and POS. Context facts distinguish example source, translation,
audio, and a safe completion target. IPA and POS enable presentation/assistance only; they never make
a recall mode eligible by themselves.

## Mode eligibility

- Typing and Multiple Choice require distinct source/canonical and target/prompt text. Multiple
  Choice does not require stored distractors; later generation remains LQ-006D.
- Listening and word Dictation require a valid word-audio reference and canonical source text, and
  expose `AUDIO_TO_TEXT`.
- Image Recall requires a valid image reference and canonical source text, and exposes
  `IMAGE_TO_TEXT`.
- Reverse Translation requires distinct source and target text and exposes both meaningful text
  directions.
- Example Completion requires example source text and exactly one safe token-boundary occurrence of
  the canonical answer, exposing `CONTEXT_TO_TEXT`. The resolver records the span; it never masks text
  with naive replacement.

Blank optional strings are missing. Media references are opaque portable identifiers; a reference
containing control characters is malformed, but the resolver never interprets it as a local path or
checks whether it exists.

## Assistance

Example text enables `EXAMPLE_VIEWED`; example audio enables `EXAMPLE_AUDIO_PLAYED`; pronunciation
enables `PRONUNCIATION_HINT_USED`; canonical text enables `LETTER_HINT_USED` and
`ANSWER_REVEALED`. This is capability only—no progression UI or evidence weight is implemented.

## Determinism and portability

The resolver is a pure function. Mode, direction, reason, fact, and assistance collections are
serialized in stable wire-ID order, never enum ordinal order. `RecallCapabilityProjectionWireCodec`
round-trips the complete versioned projection and returns a typed unsupported-version result.
Contracts contain no Compose, Android, Swing, AWT, `File`, `Path`, stream, bitmap, or painter type,
so Desktop, Android, iOS adapters, Web, and service boundaries can consume the same projection.
