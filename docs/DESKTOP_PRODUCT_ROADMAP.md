# Learning Product Capability Roadmap

This roadmap implements the platform-independent specifications under [`spec/`](spec/) with
Desktop as the primary target. It begins only after current Desktop 1.0 external/manual gates.
Android remains evidence; Learning Engine remains algorithm and durable-state authority.

## Delivered experience foundation

Learning Flow Engine Foundation now composes each item presentation as a bounded deterministic
stage sequence. Desktop v1 uses rotated primary → optional eligible Typing → authoritative
reveal → manual rating-ready. This is transient orchestration, not adaptive scheduling:
no flow history, stage persistence, auto-rating, queue mutation, AI, or branching graph exists.
Its Product Brain is now explicit: durable-recall objective → standard rotated-recall strategy →
immutable flow template. Flow execution only instantiates and advances that template, allowing
future approved objectives/templates without changing Desktop execution semantics.

Adaptive Learning Scenes Foundation now uses platform-independent eligibility options plus a
selection engine/strategy/result boundary. Default mode rotates deterministically through
eligible passive Image, Listening, and Prompt experiences using stable session queue position;
ordinal zero preserves first-item compatibility. Typing remains an explicit transient per-item
choice backed by semantic exact evaluation and unchanged manual rating. Rotation adds no
persisted field or adaptive behavior. Fuzzy matching, personalization, AI, metrics, automatic
rating, and typing-history/preference persistence remain excluded.

## Ordering decision

The former sequence began with hierarchy, then jumped to multi-lesson and Listening before a
complete session-entry/setup contract. The revised order delivers the smallest coherent learner
journey first: discover one lesson, understand/setup a session, then enrich media and exercise
modes. Multi-lesson follows stable single-scope setup. Timed Listening follows semantic audio
and a deterministic media coordinator. This reduces schema, recovery, and UX risk.

## LX-01 — Hierarchical Learning Scope (M / Medium)

- **Problem:** large packages are presented as flat lesson lists.
- **Goal:** navigate Library → Package → Topic → Section → Lesson with graceful flat fallback.
- **Owner:** application read projection + platform presentation.
- **Dependencies:** current content/package metadata and query ports.
- **Acceptance:** deterministic identity/order; missing levels collapse; Unicode search retains
  path context; no package/persistence rewrite; real Desktop composition and lazy large-list
  behavior.
- **Manual UAT:** navigate representative flat and hierarchical packages at multiple window
  sizes and return without losing location.
- **Automated tests:** hierarchy projection, duplicates, missing metadata, Unicode search,
  fallback, performance-shape and Desktop wiring.
- **Commit scope:** read-only hierarchy projection + Desktop browser + docs/tests.
- **Risk:** Medium—source metadata may not encode every desired level.

## LX-02 — Session Entry & Setup Contract (L / High)

- **Problem:** Start Study hides scope, limits, eligibility, and active-session conflict.
- **Goal:** Resume-first entry and concise immutable setup for one lesson or Daily Study.
- **Owner:** application session request/planning boundary + platform setup projection.
- **Dependencies:** LX-01; existing `SessionPolicy`, recovery, and session creation.
- **Acceptance:** active session is never replaced silently; scope/budget/preset summary is
  deterministic; validation fails before mutation; Standard preset preserves current behavior.
- **Manual UAT:** start, cancel, fail, retry, resume, and switch intended scope.
- **Automated tests:** conflict, idempotency, empty/invalid scope, limits, recovery, restart,
  transaction failure, legacy entry points and composition.
- **Commit scope:** one setup request/outcome and Desktop entry flow; no new study mode.
- **Risk:** High—touches the session creation boundary.

## LX-03 — Focused Workspace Shell (M / Medium)

- **Problem:** current Study is functional but lacks the final cross-platform hierarchy and
  distraction policy.
- **Goal:** implement the spec’s session bar, content stage, contextual media/actions, compact
  status, focus policy and distraction-free presentation over existing workspace states.
- **Owner:** platform presentation/input only.
- **Dependencies:** LX-02; current `ReviewWorkspaceState`.
- **Acceptance:** no business lifecycle moves into Desktop; wide/standard/compact reading order;
  stable keyboard/focus/accessibility; detailed statistics stay outside active card.
- **Manual UAT:** resize, zoom, keyboard-only, screen reader, reduced motion, distraction mode.
- **Automated tests:** state/action mapping, focus keys, shortcuts, accessibility strings,
  responsive layout decisions and recovery projection.
- **Commit scope:** presentation and tests; no scheduler/persistence change.
- **Risk:** Medium—visual/manual evidence remains necessary.

## LX-04 — Semantic Learning Media (L / High / Product decision)

- **Problem:** generic audio cannot identify prompt, answer, example, translation, or voice.
- **Goal:** backward-compatible semantic audio/image roles with deterministic authored order and
  safe legacy fallback.
- **Owner:** content/application projection + package mapping + platform renderer.
- **Dependencies:** approved role vocabulary; current safe content/media boundaries.
- **Acceptance:** legacy Generic assets unchanged; roles survive import/export/persistence
  projections; unknown roles degrade safely; phase visibility follows `MEDIA_SPEC.md`.
- **Manual UAT:** bilingual package with prompt/answer/example and alternate voice media.
- **Automated tests:** legacy records/packages, round-trip, missing/unsafe/unknown roles,
  accessibility, renderer and real import composition.
- **Commit scope:** optional role contract + mappings + renderer; no timed playback.
- **Risk:** High—package compatibility and content semantics.

## LX-05 — Deterministic Media Session (L / High)

- **Problem:** view-local playback cannot reliably coordinate loop, delay, cancellation, or
  stale callbacks.
- **Goal:** injected one-player media session for manual play/replay/stop and bounded loop.
- **Owner:** platform media coordinator + playback adapter.
- **Dependencies:** LX-04.
- **Acceptance:** item/session identity guards callbacks; transitions/pause/completion stop;
  failure is per asset; no callback reveals, rates, advances queue, or changes progress.
- **Manual UAT:** supported/unsupported audio, device interruption, rapid item transitions.
- **Automated tests:** fake player/clock, race/stale callback, loop/delay, cancellation, failure,
  resource release and workspace composition.
- **Commit scope:** media coordinator, injected adapter, manual controls and tests.
- **Risk:** High—timing and platform codec behavior.

## LX-06 — Multi-Lesson Session Plan (L / High / Product decision)

- **Problem:** learners cannot combine a deliberate set of lessons.
- **Goal:** review and submit a deterministic multi-lesson scope without UI-built queues.
- **Owner:** application scope request/planning + platform selection.
- **Dependencies:** LX-01 and LX-02; approved ordering and selection-limit policy.
- **Acceptance:** identity-based de-duplication; missing scope rejects before mutation; engine
  owns ordering; resume/undo/completion preserve exact scope; single/global APIs remain.
- **Manual UAT:** select, deselect, reorder if approved, start, restart, complete.
- **Automated tests:** duplicates, ordering, missing content, sibling isolation, limits,
  rollback, restart and legacy calls.
- **Commit scope:** multi-scope request/validation + setup selection + tests.
- **Risk:** High—scope persistence and user expectation.

## LX-07 — Listening Preset (L / High / Product decision)

- **Problem:** listening practice needs repeated manual playback setup.
- **Goal:** opt-in Prompt-focused listening over the same authoritative Learning Session.
- **Owner:** platform preference/projection + media session; application only for accepted scope
  differences.
- **Dependencies:** LX-04 and LX-05; approved reveal/autoplay policy.
- **Acceptance:** plan is explained before start; autoplay never surprises on resume; Stop is
  immediate; missing audio falls back; ratings remain manual; restart resumes card, not timer.
- **Manual UAT:** extended start/stop/pause/recovery with mixed media availability.
- **Automated tests:** playback plan, fake clock, interruption, stale callback, fallback,
  reveal policy, restart and no scheduler/queue drift.
- **Commit scope:** one Listening preset and sequence; no generic automation framework.
- **Risk:** High—automation can harm focus or correctness if boundaries leak.

## LX-08 — Optional Typed Recall (XL / High / Product decision)

**Foundation delivered:** one deterministic normalized-exact expected answer from semantic
answer text, explicit per-item Desktop activation, transient input/feedback, existing reveal,
and manual rating. Alternative answers, tolerance/fuzzy linguistics, persisted preference/history,
and adaptive activation remain outside the delivered foundation.

- **Problem:** reveal-based study does not practice answer production.
- **Goal:** optional typed-answer exercise, transparent comparison, then normal reveal/rating.
- **Owner:** shared Application evaluation contract + platform input/feedback.
- **Dependencies:** approved normalization/tolerance, alternatives, locale and activation policy.
- **Acceptance:** partial input is transient; evaluator deterministic; Show answer is escape;
  failure permits reveal; no forced typing from rating history without separate approval.
- **Manual UAT:** representative English/Vietnamese answers, diacritics, alternatives, mistakes,
  hints, keyboard and assistive technology.
- **Automated tests:** Unicode/case/punctuation/blank/alternatives, hint/reveal, evaluator failure,
  restart non-persistence and no partial review.
- **Commit scope:** one evaluator contract and Standard typed exercise; no NLP/TTS.
- **Risk:** High—language correctness and learner trust.

## LX-09 — Completion, Summary & Daily Intention (L / Medium / Product decision)

- **Problem:** correct progress lacks a calm closure and optional learner-chosen daily intent.
- **Goal:** authoritative completion summary and non-punitive goal projection.
- **Owner:** application history/progress queries + typed preference + platform summary.
- **Dependencies:** stable session setup; approved goal semantics.
- **Acceptance:** counts never come from UI; final Undo reopens session; timezone/day boundary is
  deterministic; absent/corrupt goal preserves normal study; no streak threat.
- **Manual UAT:** finish, undo, re-finish, cross-day and localized tone review.
- **Automated tests:** counts, new/review split, duration/outlook availability, final undo,
  restart, timezone, corrupt preference and accessibility.
- **Commit scope:** summary projection + optional informational goal; no notifications/rewards.
- **Risk:** Medium.

## LX-10 — Curation: Pinned, Recents & Favorites (L / High / Product decision)

- **Problem:** learners cannot quickly return to important scopes/content.
- **Goal:** stable pinned/recent navigation, then favorites after durable relation policy.
- **Owner:** application learner-organization model/query + platform.
- **Dependencies:** LX-01; deletion/orphan/privacy decisions.
- **Acceptance:** never mutate imported content; missing targets remain explicit; Recents derive
  from authoritative activity; delete/update semantics are tested.
- **Manual UAT:** pin/reorder, update/delete package, revisit recent/favorite content.
- **Automated tests:** identity/title changes, orphan, atomicity, privacy bounds, restart,
  compatibility and package lifecycle integration.
- **Commit scope:** split into Pinned/Recents and Favorites commits if data boundaries differ.
- **Risk:** High—new durable learner data.

## LX-11 — Ethical Auto Learning Flow (XL / High / Product decision)

- **Problem:** hands-light practice can improve flow but hidden automation can fabricate
  progress or remove agency.
- **Goal:** visible, configurable, interruptible media/presentation sequence with manual rating.
- **Owner:** platform orchestration over media/workspace contracts.
- **Dependencies:** LX-05 and LX-07; approved reveal, loop, delay, and stop policies.
- **Acceptance:** no auto-rating; current step and next action visible; stop/pause immediate;
  timers are transient; every automated action has manual parity.
- **Manual UAT:** long-running flow, interruption, media failure, accessibility and fatigue.
- **Automated tests:** fake-clock sequences, cancellation races, stale identity, completion,
  recovery and zero scheduler/progress mutation from media.
- **Commit scope:** one approved sequence; no background service/global hooks.
- **Risk:** High.

## Mandatory blockers

- Desktop 1.0 manual/clean-machine/install/upgrade/signing/real-data evidence remains a release
  gate and is not satisfied by this specification.
- LX-04 requires a backward-compatible role vocabulary decision.
- LX-06 requires multi-lesson ordering/limit decisions.
- LX-07/LX-11 require autoplay/reveal decisions.
- LX-08 requires typed-answer correctness decisions.
- LX-09/LX-10 require new preference/data lifecycle decisions.

### Blocker register

| Category | Blocker | Mandatory for | Resolution evidence |
|---|---|---|---|
| Product | multi-lesson order/limit is undefined | LX-06 | Product Owner decision recorded with acceptance examples |
| Product | Listening autoplay/reveal policy is undefined | LX-07/LX-11 | explicit default, consent, stop and resume behavior |
| Product | favorite/pinned/goal lifecycle is undefined | LX-09/LX-10 | data ownership, deletion/orphan and compatibility decision |
| UX | current flat browser cannot express path/context | LX-02 and later scoped modes | LX-01 hierarchy UAT on flat and hierarchical packages |
| UX | no approved setup conflict flow for an already-active session | LX-02 | Resume/end/back behavior tested and manually reviewed |
| Learning | typed matching and alternative-answer semantics are undefined | LX-08 | learning-science/product review plus deterministic examples |
| Learning | auto flow could reveal too early or imply recall evidence | LX-07/LX-11 | manual rating invariant and approved reveal policy |
| Technical | generic media lacks semantic roles | LX-05/LX-07 | compatible role representation through import/projection |
| Technical | view-local player lacks identity/timer cancellation | LX-07/LX-11 | LX-05 fake-player/clock race coverage |
| Architecture | clients must not create queues or scheduling evidence | every LX capability | application command/query ownership and regression tests |
| Architecture | new hierarchy must not rewrite imported packages | LX-01 | read-only projection and legacy flat fallback tests |
| Release | real 179 MB package and interactive responsiveness UAT pending | Desktop 1.0 and later media claims | Product Owner real-data evidence |
| Release | clean-machine install/upgrade/uninstall/signing pending | Desktop 1.0 distribution | completed external release checklist |

Product, learning, architecture, and release blockers marked above are mandatory only for the
capability/release named in the third column. They do not block earlier read-only work whose
acceptance boundary is independent.

## Optional backlog

Configurable rating gestures, transient scratchpad, in-study quick search, future TTS, expanded
display preferences, and delight animation follow demonstrated use. They do not block LX-01
through LX-03 or manual semantic media.

## Next capability

After the external Desktop 1.0 gates, start **LX-01 — Hierarchical Learning Scope**. It is
read-only, improves the known large-package experience, establishes vocabulary required by all
later session setup, and preserves current scheduler/persistence behavior.
