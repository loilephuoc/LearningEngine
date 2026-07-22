# Learning Media Product Specification

## Purpose

Media supports comprehension and retrieval without becoming a second learning engine. This
specification defines platform-independent media roles and behavior; platform adapters decide
how supported local assets are decoded and played.

## Semantic audio roles

| Role | Meaning | Earliest phase | Default autoplay |
|---|---|---|---|
| Prompt | stimulus to retrieve from | Thinking | Off; Listening preset may opt in |
| Answer | canonical answer pronunciation/content | Reveal | Off |
| Example | contextual example | Reveal | Off |
| Translation | translated prompt or answer | Reveal unless authored as prompt | Off |
| Explanation | longer explanatory narration | Reveal | Off |
| Alternate voice | equivalent rendition of another role | same as parent role | Off |

Unlabelled legacy audio remains `Generic`; it is manually playable after its containing content
block becomes visible. Unknown roles degrade to Generic rather than failing import.

## Asset association

An audio item has stable content association, semantic role, optional language tag, optional
voice label, deterministic authored order, and a safe local reference. Roles describe purpose,
not filenames. A content block may expose multiple assets for alternate voices or examples.

## Playback rules

- One active playback identity per workspace.
- Starting another asset stops the previous asset.
- Item transition, Pause/Exit, completion, asset invalidation, and workspace disposal stop audio.
- Replay restarts the selected asset from its beginning.
- Loop repeats only the explicitly selected asset or approved playback sequence.
- Playback completion never reveals, rates, schedules, or advances a durable queue by itself.
- Stale callbacks from an old item/session are ignored.

## Autoplay policy

Autoplay is Off by default. It may be enabled per study preset or learner preference only after
the product shows what will play. Surprise audio on application launch, resume, error recovery,
or switching scope is forbidden. System mute, accessibility, and reduced-distraction
preferences take precedence.

Listening preset may autoplay Prompt audio. Answer/example autoplay requires an explicit
sequence approved in session setup and cannot auto-rate. Autoplay stops after user interaction
that conflicts with the sequence.

## Multi-audio and sequences

A playback plan is an ordered list of role/asset steps with optional bounded delay. It is
presentation state tied to current content identity. The learner can see the current step,
skip, replay, stop, and disable the plan. Loops have an explicit count or continue-until-stopped
label; no infinite loop is hidden.

## Interruption

On platform audio interruption, device loss, sleep, session pause, or competing playback:

- stop or pause according to platform convention;
- never infer learning progress;
- expose a resumable manual action when safe;
- after application restart, remain stopped rather than restoring playback position;
- preserve the authoritative current card/reveal state independently.

## Failure behavior

Missing, unsafe, unsupported, corrupt, or unreachable audio produces a localized unavailable
message for that asset. Text/image study continues. A player failure releases resources and
allows retry or another asset. Diagnostics may include role and safe asset identifier, never
private content or raw external paths.

## Image behavior

Images have Prompt, Answer, Example, or Decorative purpose. Meaningful images require authored
description or a localized fallback. They load lazily with bounded decode, preserve aspect
ratio, support enlargement, and fail locally without collapsing text. Remote and executable
content remain excluded until a separate security contract exists.

## Future TTS compatibility

TTS is a potential media provider, not a scheduler feature. Generated speech must declare role,
language, voice, source text identity, and whether it is cached. The UI distinguishes recorded
and synthesized audio when relevant. No TTS network call, secret, consent, or caching policy is
assumed here.

## Accessibility

All controls expose role, language/voice when useful, playing/stopped state, and action. Audio
never replaces visible text by default. Captions/transcripts use authored content; the product
does not claim automatically generated text is canonical without review.

## Compatibility

Existing generic local audio remains valid and manually playable. Adding roles must use
optional, defaulted package/content fields or a compatible projection. Persisted session and
review records do not store player state.

## Product decisions still open

- canonical role vocabulary in package format;
- whether answer autoplay is permitted in Listening preset;
- supported loop limits and default delay;
- guaranteed codec set per release;
- offline/online TTS provider, consent, privacy, and cache policy.
