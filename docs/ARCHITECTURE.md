## Constitution & Strategic Foundation

Android owns one visual system in `LearningEngineTheme`, `LearningEngineDesignTokens`, and
`LearningEngineComponents`. Material color semantics map Mockup 03's warm green/teal Light identity
and Mockup 09's layered navy/teal Dark identity; shared typography, density, shapes, icons, cards,
buttons, screen shell, settings rows, and navigation treatment are presentation-only. Top-level
screens consume these contracts and retain their existing facade/ViewModel/Application authorities.

Home and Study landing share a pure `AndroidLearningLandingPresentation` derived only from the
existing immutable `AndroidStudyState.Home`. It projects no new statistics: active-session presence,
context title, due/review activity, accuracy and memory progress remain Dashboard/session/package
authority. Shared hero/stat/action components render this projection; navigation uses existing routes.

Android Study Compose owns one responsive presentation stage: compact canonical HUD/progress at
the top, scroll-safe content in the center, and state-appropriate actions at the reachable bottom.
Introduction front/reveal motion, fullscreen image state, and audio emphasis are local presentation
concerns; RecallPlan selection, audio/media references, rating commands, queue mutation, Undo, and
statistics remain facade/Application/Core-owned.

Android idle Study scope is derived from canonical `Library.activePackageId`, validated against its
registered ACTIVE `InstalledPackage`; installed-package repository order is not a selection policy.
Shared import completion assigns the first usable package only when that Library selection is empty.
Package Study uses `LibraryCommandService` before scoped session creation, while Continue Learning
routes the exact existing session ID without invoking the session-start path.

The active runtime has one `LearningEngineLearningStage` composition. REVIEW prompt, response and
label-free feedback transform inside that surface rather than appending a report card; Introduction
uses the same stage grammar with hero media before reveal and supporting media afterward. The
two-tier compact HUD renders only immutable `AndroidStudySessionHud` values, and shared choice/rating
controls dispatch existing events without owning submission or scheduling state.

Study-mode intent is a shared recall contract. `StudyMode.ADAPTIVE` is the default and leaves mode
selection to `AdaptiveRecallStrategy`; `StudyMode.TYPING` is an explicit user request and permits
only a Typing plan. Android captures intent but never ranks modes. Canonical session queue and
`introducedContentIds` state gate unseen NEW into Introduction before recall planning.

Invocation mode and bounded actual recall-mode history are durable `StudySession` facts, not Android
preferences. Recall learning commits append mode, direction, outcome, timestamp, and assistance use
through the established session transaction; production Adaptive planning consumes this history and
engine-derived trajectory profile/recommendation. When canonical queue selection returns no item,
Android invokes `LearningEngine.finishSession`; only a FINISHED session may back Completion, and
reloading it is an idempotent projection rather than another lifecycle mutation.

For a queue entry whose canonical origin is NEW, `reviewedContentIds`—not introduction/reveal
persistence—determines whether Android remains in Introduction. `answerRevealed` restores the stage
within that lifecycle. Android Study ViewModel serializes load and event processing so each event
reads and publishes the result of its predecessor; facade duplicate guards retain exactly-once review
commit authority.

Package import has one cross-client completion boundary: raw content/package persistence is followed
by canonical conflict-aware `InstalledPackage` creation/replacement and `LibraryEntry` registration
before success is published. Active package-bound Study sessions are then reusable only while their
installed package is ACTIVE and their persisted queue/session content identities remain owned by that
package. Incompatible sessions use the existing leave-session lifecycle; compatible and legacy
unscoped sessions retain established continuation behavior.

NEW Introduction interaction is an Android presentation state machine over the existing
`RevealIntroduction` and `RateIntroduction` events. Word/Example playback focus, revealed-image
expansion, gesture qualification and per-item swipe suppression are ephemeral Compose state.
Swipe-up dispatches canonical Good only when the shared body has no vertical scroll range and no
child consumed the gesture; visible four-way ratings remain the accessibility and long-content path.
Direct audio fields retain their existing role mapping and one `AndroidAudioController` replaces the
active source. No gesture, playback-focus or image-expansion fact enters session or memory state.

ANDROID-UI-3.0D1 keeps this interaction boundary and replaces only NEW canvas projection. A pure
width policy maps available stage width to a 240–340dp front image and a 60% reveal image. The clue
uses length-bounded 22/25/28sp presentation, while Word/Example loop ownership drives a semantic
wrap-content halo and reduced-motion-safe static highlight. The shared image component may fill an
explicit bounded canvas with `ContentScale.Fit`; it neither resolves nor decodes a second asset.

ANDROID-STUDY-3.0D6 supersedes only that presentation geometry: width plus viewport height bound a
240–420dp hero and reveal retains approximately 86% of its footprint. The same Introduction stage
orders reveal as image, English answer, POS/pronunciation, Vietnamese meaning, then one bilingual
example surface. Chrome/HUD remain projection-only and secondary; Compose still owns no learning,
rating, scheduling, persistence, session, or audio-controller authority.

ANDROID-STUDY-3.0D8 supersedes the fixed reveal ratio and square-like canvas sizing. The shared image
component derives one centered width/height from its already-decoded bitmap aspect ratio and
presentation-provided viewport bounds, retaining `ContentScale.Fit` and one decode/load state.
Introduction front does not compose or semantically expose rating and its gesture resolver disables
swipe rating; revealed state alone exposes those existing event paths. D7 lifecycle authority stays
in the facade/session boundary.

ANDROID-STUDY-3.0D9 changes only Android presentation composition. Introduction receives the
remaining constrained viewport below projected HUD chrome and owns an internal lazy stage: compact
front content is centered, revealed content is bottom-aligned beside the fixed rating dock, and
overflow scrolls. Clue audio emphasis preserves geometry and remains neutral; only the revealed
answer uses the stronger semantic halo. D7/D8 lifecycle, event, and media-loading authorities remain
unchanged.

ANDROID-STUDY-3.0D10F separates restored Study state from root navigation intent. Cold bootstrap
projects Home and its exact-session Resume action; `MainActivity` never infers navigation from a
Runtime/Completion/Failed state transition. Only explicit Start, Resume, or OpenSession events and
their existing Library/package callbacks enter Study. Session persistence and lifecycle are not
mutated to control destination visibility.

ANDROID-STUDY-3.0D10A enforces `Library.activePackageId` as the Android current Study package.
`PackageState.ACTIVE` remains installed-package usability only. Landing projections may retain
canonical global memory statistics, but scoped Start/Review actions and current-package titles
require the Library selection. Package-scoped entry commits and confirms that selection before
finishing a mismatched scoped active session through the existing engine boundary or creating a new
session; legacy unscoped recovery remains compatible.

ANDROID-STUDY-3.0D10B introduces `DailyStudyBudgetQueryService` as canonical application projection.
Limits are learner-global and reset by local calendar date; eligible NEW and due REVIEW remain
filtered to canonical package scope. The first review event per content counts NEW, later events
count REVIEW, and Undo removes the corresponding contribution. `SessionPolicy` remains a persisted
per-session execution cap populated from remaining daily budget. Android SharedPreferences persist
limits only and own no counters, history, scheduling, or eligibility.

ANDROID-STUDY-3.0D10C adds recovery presentation without adding authority. Android Library and
package detail delegate current-learning selection to `LibraryCommandService.setActivePackage` and
reload canonical projections; selection alone never creates or finishes a Study session. Installed
`PackageState.ACTIVE` is presented as availability, while `Library.activePackageId` alone marks the
current learning package. Learner-global daily progress and review history are unaffected.
Root navigation requests a canonical Home re-projection only when the long-lived Android Study
ViewModel is already idle at Home; it cannot replace a live runtime state or create a session.

ANDROID-STUDY-3.0D10D hardens the Android Start execution boundary. Before creating a session, the
facade reconciles persisted active-session scope and loads an exact compatible session for the
canonical package. Unexpected ViewModel event exceptions become a traced `Failed` state. This adds
no session, package, budget, history, scheduler, FSRS, or runtime-state authority.

ANDROID-STUDY-3.0D10E preserves queue-selection results while changing planner cost: diversity uses
linked removal instead of shifting array fronts, and seeded NEW ordering materializes one canonical
SHA-256/hex key per item. Adaptive low-evidence ranking prefers supported image/audio/example
scaffolds; text-only and explicit Typing retain their contracts. Scheduler, FSRS and queue size are
unchanged.

Android Study session statistics follow the existing cross-platform query boundary. The facade
resolves package scope from canonical installed-package identity and package content authority
(falling back to the session's included Content IDs only when no package identity exists), builds
`StudySessionProgressSource` from persisted session/queue state, and invokes
`StudyHeaderStatisticsQueryService` after loads and successful mutations. Compose receives only an
immutable `AndroidStudySessionHud`; it owns no repositories, counters, or statistics queries.
The HUD projection includes `dueCount` directly from `StudyHeaderStatistics`; reveal, rating, next,
Undo and reload continue to refresh the entire projection through the same query boundary.

LQ-006G adds a Shared Application adapter from RecallResult to the existing session review,
practice completion, and manual override use cases. It creates no parallel transaction: existing
ports retain atomic persistence, Scheduler/Evidence, queue, recovery, and Undo ownership.

LQ-006F adds a pure Shared Application execution engine after RecallPlan rendering. Immutable typed
submissions enter a mode-evaluator registry and produce validated RecallResult values; no learning
state, scheduling, persistence, queue, or platform boundary is invoked.

LQ-006E keeps distractor generation in Shared Application behind the LQ-006D provider contract.
Candidate scope is injected, ContentId owns deduplication, and platforms only render validated
options. No repository scan, persistence, network, or learning-state dependency exists.

LQ-006D adds a pure Shared Application plan factory after adaptive strategy. Typed mode handlers
construct prompts, answer contracts, assistance, requirements, and deterministic identity, then
pass every plan through the LQ-006A validator. Platforms remain renderers only.

LQ-006C places deterministic recall strategy selection in Shared Application over shared typed
Domain contracts. Platforms cannot rank modes. The pure selector has no execution, persistence,
Scheduler, Evidence, filesystem, or UI dependency.

### Evidence-based rating promotion authority

LQ-005E adds a pure recommendation layer after the derived difficulty profile. An
`AdaptiveLearningStrategy` reads exactly one `LearningDifficultyProfile` and returns an immutable
`LearningRecommendation` identified by learner + Content. Actions, categories, priorities,
confidence, and explanation reasons are typed; risk/readiness/lapse/promotion thresholds live in a
validated `AdaptiveLearningPolicy`. Recommendations describe engine advice only and have no
command, persistence, scheduling, queue, review, transition, or UI semantics.

LQ-005D adds a pure derived intelligence layer above `LearningTrajectory`. The
`LearningDifficultyProfileCalculator` accepts the learner identity and Content-owned trajectory,
uses only an injected `EvidenceClock` plus a validated `LearningDifficultyPolicy`, and returns a
non-authoritative `LearningDifficultyProfile`. The aggregate separates lifetime statistics,
current stability, generic per-stage promotion analytics, risk, engine confidence, trend, and
difficulty level. Scores are bounded typed values; thresholds and weights live in policy rather
than UI, Scheduler, or execution code. No profile is persisted as source of truth.

LQ-005B integrates that authority into the evaluative review transaction. A typed automatic recall
payload is staged durably, converted to `RecallEvidence`, appended to the learner-and-Content
trajectory, and evaluated before the committed rating reaches Scheduler. `PromotionDecision` is
returned with the review result. Manual, reveal, replay, and practice paths create no evidence and
receive no evidence cap. Trajectory JSON joins the review transaction and durable undo checkpoint.

LQ-005A.1 makes the evidence input a Content-owned learning trajectory rather than an unbounded
list. A `LearningTrajectory` retains an ordered history of immutable `EvidenceChain` instances;
each chain has one `PromotionStage`, one validated `ChainAnchor`, a chronological evidence/non-
evidence sequence, and an optional typed close reason. Promotion, a new Again/lapse, or an explicit
manual/recovery/Undo reset closes the current chain and starts a fresh chain from its new anchor.
Old recall evidence remains historical but cannot enter a later promotion decision.

Manual ratings are typed `NonEvidenceEvent` entries: they neither create recall evidence nor reset
the chain. Chain and trajectory identity is `ContentId`, deliberately not `LearningItemId`, so
sibling learning items share one memory-development trajectory. `PromotionCandidate` now accepts
only the current `EvidenceChain`; LQ-005B supplies persistence, reconstruction, and execution wiring.

LQ-005A adds a pure shared-Domain `EvidencePromotionAuthority`; it is the sole authority for
promotion eligibility from Again to Hard, Hard to Good, and Good to Easy. A typed
`PromotionCandidate` names the exact anchor `ReviewEventId`, and immutable `RecallEvidence`
references existing time, rating, session-policy, provenance, due-time, and optional latency
authorities. Replay, undone, duplicate, Practice, Reveal, manual override, and direct manual-rating
evidence are classified and excluded before policy evaluation.

`PromotionPolicy` defaults to 24 hours plus one independent correct recall, 72 hours plus two, and
14 days plus three respectively; Hard-to-Good forbids an intervening Again and Good-to-Easy forbids
a lapse. Time enters only through injected `EvidenceClock`. Decisions expose typed reasons,
remaining time, and missing recall count. LQ-005A deliberately does not promote a rating or write
`ReviewEvent`, `MemoryState`, Scheduler/FSRS, persistence, queue, Undo, Practice, or platform state.

### Cross-platform practice session foundation

`SessionPolicy.evaluationPolicy` is the shared Domain authority that distinguishes evaluative
sessions from `PRACTICE_ONLY` sessions. Application transaction boundaries must consult this typed
policy before staging or committing review, rating, Scheduler/FSRS, or MemoryState mutations;
Desktop and future Android/Web/iOS clients may only render state and dispatch intents. The policy
is persisted with `StudySession` and restored on restart, while legacy records default to
`EVALUATIVE`.

Manual Rating Override is a future shared-Core input to the existing guarded evaluation command
and transaction boundary. LQ-004A does not implement that input or any mutation/UI for it; this
placement prevents a later platform-specific authority or persistence refactor. Practice loop,
shuffle, evidence, promotion, and Desktop UI behavior are outside this foundation.

LQ-004B completes the behavior defined by [`PRACTICE_SESSION_CONTRACT.md`](PRACTICE_SESSION_CONTRACT.md).
UX-003 keeps typing evaluation, override availability, and inventory counting in shared Core;
Desktop only maps typed state to semantic visuals, presents confirmation, and dispatches intents.
UX-004 extends shared typing evaluation with `VALID_PREFIX`: a normalized non-empty proper prefix
is live guidance, not a completed attempt, reveal, review, scheduling input, or practice result.
Desktop `CenteredTypingField` owns the field/decorator/placeholder/cursor/action constraint and
maps typed Practice and inventory semantics to existing design-system tokens.
UX-006 supersedes UX-005's separate evaluative entry point: shared evaluative availability enables
the Decision Dock on both front and answer, and `StudyFacade` resolves front reveal before the
existing atomic review transaction with `MANUAL_USER`. Compose owns no ReviewEvent, Scheduler, or
queue logic. Practice continues to branch before review staging and uses `MANUAL_USER_OVERRIDE`
only for its confirmed override. Dynamic study allocation distinguishes the descender-safe inner
line box from the compact outer typing minimum and assigns real remaining height to fitted images.

UX-007 keeps Application as the one queue-advance authority and adds a Desktop-only pending
destination publication boundary described in
[`QUESTION_TRANSITION_PIPELINE_CONTRACT.md`](QUESTION_TRANSITION_PIPELINE_CONTRACT.md). A typed
result/exit/enter lifecycle prevents next-item identity, image, timer, focus, and child animations
from observing the destination before the single atomic presentation swap. Image allocation now
uses content-derived typing metrics and removes duplicate chrome reserves and aspect height caps.

UX-008 makes those Desktop layout contracts measurable. `TypingFieldMeasuredLayout` preserves the
resolved outer minimum under actual Compose measurement and keeps label, protected inner line box,
and trailing action inside that bound; an overflowing typing front receives bounded body scrolling
instead of compressing the field. `MediaPresentationMetrics` distinguishes frame and rendered
bitmap dimensions, derives both from source aspect, usable width, vertical budget, and the existing
1.35x anti-upscale limit, and never lets media consume the typing/Dock reservation. Frame and bitmap
therefore grow or shrink together while `ContentScale.Fit` remains the no-crop/no-stretch authority.
The UX-007 result/exit/enter transition boundary is unchanged.

UX-005 originally defined evaluative manual rating in
[`EVALUATIVE_MANUAL_RATING_CONTRACT.md`](EVALUATIVE_MANUAL_RATING_CONTRACT.md) and centralized
image/typing allocation in
[`ADAPTIVE_STUDY_VERTICAL_SPACE_CONTRACT.md`](ADAPTIVE_STUDY_VERTICAL_SPACE_CONTRACT.md).
Persisted queue state owns fixed membership, current permutation/position, seed, and round. Shared
Application use cases own practice advancement, explicit leave, manual override, Undo interaction,
and content-level rating inventory. Desktop renders these projections and confirmation surfaces;
it contains no shuffle, membership, rating-counting, or scheduler policy.

Learning Engine 2.0 is an adaptive **Teaching Engine**. For high-level system overview, product philosophy, cross-platform strategy, and non-negotiable architectural laws, refer to:

### Desktop design-system component boundary

Reusable presentation primitives live under
`desktop/ui/designsystem/components/base`. They accept semantic variants, consume `LETheme`
only, and may use pure presentation resolvers for deterministic state tests. They must not own
screen/domain behavior, navigation, persistence, audio, viewport policy, or keyboard routing.
Pre-PLE-028C Studio/Study controls remain an explicit compatibility layer until migrated by a
bounded capability.

PLE-028D migrates the active Study composition boundary to semantic base surfaces/buttons and
`LETheme`. Study-only visual mapping stays in `ui/study`; it may map immutable action/surface
roles but cannot own scheduler/FSRS, persistence, navigation, audio lifecycle, keyboard routing,
or viewport detection. `StudyVisualLayoutResolver` remains the single responsive authority.
Legacy Design System controls remain a compatibility layer for screens not migrated by PLE-028D.

Study session completion remains a Desktop projection over existing session facts and action
availability. `SessionCompletionPresentationResolver` may assign typed visual roles and action
priority only; it must not derive learning outcomes, enable unavailable work, invoke callbacks, or
own review/session/queue/Scheduler/FSRS/persistence behavior. The completion card may co-locate the
already-projected final scheduler consequence, Undo recovery action, and Continuous Review toggle
as one experience while their established application and interaction authorities remain unchanged.

Learn entry is a Desktop presentation over the current `StudyUiState` and application-projected
focused/learned-item availability. Its pure resolver may group the current display title, projected
readiness, and typed action identities, and may assign at most one enabled Resume/Continue action as
primary. It cannot plan a queue, create a session, promote Replay/Review All by business inference,
scan repositories, or treat localized text as identity. Library retains package/content management,
import, search/refinement, archive, and destructive-action ownership.

REV-001 keeps focused review membership in application session use cases. Latest-session NEW review
uses the latest matching finished session plus predecessor queue origin and committed membership;
Again/Hard review uses the latest chronological committed `ReviewEvent` per scoped content. Both
create ordinary review-origin queues without mutating scheduler, rating, or memory evidence. Desktop
facade/ViewModel/shell wiring may invoke these capabilities but cannot reproduce their filters.

Dashboard information hierarchy is Desktop presentation over immutable `DashboardUiState`. A pure
resolver may group and visually prioritize today's projected due values, existing Learn-action
context, key metrics, recent activity, and historical analytics. It cannot calculate statistics,
query events, invent metrics or actions, or own navigation. Application learning-dashboard queries,
Dashboard Facade formatting, Review History heatmap input, and shell navigation remain authoritative.

- **System Overview**: [`SYSTEM_OVERVIEW.md`](SYSTEM_OVERVIEW.md)
- **Product Philosophy**: [`PRODUCT_PHILOSOPHY.md`](PRODUCT_PHILOSOPHY.md)
- **Repository Constitution**: [`REPOSITORY_CONSTITUTION.md`](REPOSITORY_CONSTITUTION.md)
- **Product Brain**: [`PRODUCT_BRAIN.md`](PRODUCT_BRAIN.md)
- **Product Brain Specification**: [`PRODUCT_BRAIN_SPECIFICATION.md`](PRODUCT_BRAIN_SPECIFICATION.md)
- **Cross-Platform Strategy**: [`CROSS_PLATFORM_STRATEGY.md`](CROSS_PLATFORM_STRATEGY.md)

- **AI Design Rules**: [`AI_DESIGN_RULES.md`](AI_DESIGN_RULES.md)
- **Architecture Decision Records (ADRs)**: [`adr/`](adr/)

---

## Build modules

### Current Study decision flows

PLE-039-H keeps rating-action feedback transient in Desktop. Mouse, keyboard/Space, forced Again,
and automatic Typing converge on one final `ReviewRating` identity in `StudyViewModel`; activation
precedes guarded dispatch and confirmation follows only transaction success. A monotonic token
distinguishes consecutive equal ratings without persistence or scheduler/review authority changes.
Confirmation semantics come from `StudyWorkspaceStrings`. One screen-level completion effect
consumes the displayed token; `StudyViewModel` clears only an exact current-token match, so stale
callbacks cannot erase newer feedback and release performs no facade or persistence operation.

Continuous Review intent is a separate durable application value scoped by learner, installed
package, and nullable topic; it is not a `StudySession` status or Desktop preference. Restart
first invokes established active-session recovery. Only when no resumable/just-closed session
exists does it validate the intent against the caller's exact package/nullable-topic scope, then
select the latest ordinary-success general-Study predecessor by
`(finishedAt, SessionId)`, and delegate to `ContinueGeneralStudyUseCase`. The deterministic B1
SessionId remains duplicate authority. Missing intent means disabled; corrupt/incompatible intent
fails explicitly and is never repaired silently. A NoWork predecessor marker prevents repeated
empty continuation attempts across restart while preserving the enabled intent for later work.
`StudySession.completionProvenance` is the durable completion authority: normal queue exhaustion
persists `ORDINARY_SUCCESS`, restart closure persists `RECOVERY_RECONCILIATION`, replacement/leave
persists `REPLACED_OR_LEFT`, and legacy records without the additive field decode as `UNKNOWN`.
Only `ORDINARY_SUCCESS` is eligible; Product Brain completion presentation is not used as a proxy.

```text
Canonical prompt → Typing attempt evidence → timing/quality policy → candidate rating
→ spaced-memory eligibility → Memory Confidence Easy gate → final ReviewRating
→ review transaction → ReviewEvent → FSRS/Scheduler
```

`TypingAutoRatingPolicy` owns the candidate; Desktop does not infer the rating. The confidence
gate can only retain candidate Easy or cap it to Good. Scheduler/FSRS remains the sole interval,
due, stability, and difficulty authority.

```text
ReviewEvent history → canonical ordering and continuity validation
→ derived confidence projection → optional pending-evidence projection → Easy-only gate
```

Memory Confidence is a deterministic derived heuristic, not a persisted aggregate, calibrated
probability, or FSRS replacement. Reprojection after Undo/restart follows durable review evidence.

```text
Typing front → focus input → await layout → BringIntoView
Reveal/back → await answer layout → scrollTo(0) → typed diff/canonical identity first
```

Both scroll intents share one Desktop presentation-local state and are item/phase scoped.

```text
Candidate selection → strategy placement → SessionId-seeded NEW ordering
→ diversity/balance → SessionPolicyLimiter → durable StudyQueue
```

The SHA-256 orderer deterministically changes only the NEW subsequence. REVIEW priority and
Again/Hard reinsertion are not randomized.


The Gradle build has two modules:

1. Root project
   - Domain model and domain services
   - Application use cases and ports
   - Infrastructure and persistence adapters
   - JVM adapters and command-line entry points
   - Automated tests

2. `desktop`
   - Compose Desktop application
   - Depends on the root project
   - Owns presentation state, screens, components, and desktop wiring

## Dependency direction

```text
Desktop / JVM adapters
        ↓
Application use cases and ports
        ↓
Domain model and domain services

Infrastructure implements application ports.
```

Domain and application code must not depend on Compose Desktop or concrete JSON storage details.

## Main capability boundaries

- Content and learning-item domain
- Package Platform v1 (Media Packaging, OPD3 Export, Package Inspector, Verification)
- Content packaging, package validation, import, registration, and media
- Content-library collections and package attachment
- Study sessions, item selection, sibling avoidance, and queue planning
- Review transitions, memory state, FSRS/scheduling, and due-state calculation
- Progress and review-history queries
- JSON-backed and in-memory persistence, mappers, stores, repositories, and transactions
- Dashboard, statistics, analytics, and scheduling diagnostics
- JVM/legacy import entry points
- Compose Desktop shell, dashboard, content library, study, review history, statistics, and settings

Package latest-rating presentation is derived at the application boundary. The package-scoped
query resolves current enabled learning-item ownership, reads the learner's append-only
`ReviewEventRepository` history once, and selects the newest event per item before grouping
Again/Hard/Good/Easy counts. Desktop receives only the resulting distribution through its
existing background Library refresh; Compose does not read repositories or aggregate events.

### Package Platform v1.1 Architecture & Production Hardening

Package Platform v1.1 defines the platform-neutral pipeline for content packages:
1. **Canonical Topic Package (`CanonicalTopicPackage`)**: platform-neutral model holding contents, learning items, metadata, and media references.
2. **Media Packaging (`PackageMediaAssetCollector`, `CanonicalMediaBundle`)**: collects present media assets, deduplicates byte payloads, calculates SHA-256 checksums, and builds deterministic media manifests without modifying learner state.
3. **OPD3 Exporter (`Opd3PackageExporter`, `DeterministicZipWriter`)**: byte-for-byte deterministic `.opd3` ZIP package generator with schema v1.0 metadata, contents, learning items, media manifest, and file checksum manifest.
4. **Streaming Package Inspector (`Opd3PackageInspector`, `PackageInspectionResult`)**: incremental streaming inspection API using bounded buffers and running byte counters, enforcing `PackageSafetyLimits` without allocating unbounded memory.
5. **Package Verifier (`Opd3PackageVerifier`, `PackageVerificationReport`)**: unified single-pipeline package verifier enforcing package integrity, SHA-256 manifest validation, strict schema v1.0 & OPD3 format validation, mandatory `TopicId` check, strict `media-manifest.json` presence, and missing asset detection.
6. **Canonical Path Safety (`Opd3PathValidator`)**: single authoritative validator rejecting `..`, `.`, empty segments, double separators `//`, directory-only media paths, and unallowed archive layout entries.

## Integration rule

A feature is complete only when all required layers are connected:

```text
Domain behavior (when needed)
→ application command/query/use case
→ infrastructure implementation and persistence
→ desktop/JVM adapter wiring
→ automated tests
→ user-visible flow
```

Do not add disconnected abstractions or placeholders solely to name a future capability.

## Persistence compatibility

Existing persisted data and package formats are product contracts. Changes must preserve compatibility or include explicit migration, validation, rollback considerations, and tests in the same batch.

## Platform strategy

The engine remains reusable and UI-independent. Compose Desktop is the active client and the first release target. Android, iOS, and Web are later consumers; their future needs must not force premature shared abstractions before Desktop Beta works end-to-end.

## Android product-reference boundary

The supplied Android `LockScreenActivity` is product evidence, not reusable architecture or a
learning authority. Preserve or adapt its strongest learner-facing ideas—rapid prompt/reveal/
rate rhythm, lesson control, bilingual media, listening practice, shortcuts, and visible
context—through the existing layered system. Do not port its Activity-owned scheduling arrays,
queue heuristics, mutable sentence-file writes, counters, lock-screen/device-admin controls, or
platform key handling.

Post-1.0 Desktop product work follows
[`DESKTOP_PRODUCT_ARCHITECTURE.md`](DESKTOP_PRODUCT_ARCHITECTURE.md): immutable session setup
feeds existing application planning; Review Workspace remains a projection; content
presentation, media session, input routing, and progress are separate subsystems. Media and
timer callbacks are never review evidence. Presentation presets default from typed Desktop
configuration and become domain policy only when an accepted learning invariant requires it.

The platform-independent product contract lives under [`spec/`](spec/). Those documents define
learner-visible state, actions, media roles, and hierarchy; they do not create shared UI
frameworks or alter dependency direction. Clients may render differently but must preserve the
same action permission, atomic rating, resume, interruption, and fallback behavior.

## Learning Experience development boundary

Study typography is a Desktop presentation preference, not a learning or scheduling rule.
`StudyTypographyPreferences` is persisted by the typed `DesktopRuntimeConfiguration`; missing
properties retain schema-v1 compatibility by loading the established defaults. Settings owns a
draft and persists it only on Apply. The live runtime configuration then flows through the
Desktop composition root to one pure `StudyTypographyPresentationResolver`, which maps base
font sizes, line heights, and wrapping before Compose renders example text. Fullscreen and
narrow viewports never reduce either configured base size; narrow content wraps instead of
introducing viewport-specific font shrink logic in individual composables.

Adaptive Study presentation is likewise a Desktop projection concern. Persisted
`StudyPresentationPreferences` selects Adaptive, Preference Guided, or Manual control and
bilingual visibility/autoplay constraints. A single pure `StudyPresentationPolicy` combines
those preferences with available semantic content/media and a Question recommendation derived
from the current `LearningExperiencePlan` plus selected experience. Adaptive preserves that
experience recommendation authority;
Preference Guided intersects it with learner choices; Manual uses learner choices directly.
Primary English text and primary English audio are separate Question permissions, allowing
Listening Recall audio without exposing identity. Hidden, unrevealed, or unavailable support
cannot autoplay. Compose owns only draft/Apply state and transition-keyed playback;
preference changes never become learning evidence or trigger scheduler, queue, or review writes.

PLE-026-R1 makes the effective presentation authoritative across the complete focused-answer
hierarchy, including English identity, IPA, POS, meaning, examples, semantics, and autoplay.
Runtime configuration remains the only persisted preference. `ContentHost` owns a transient
`StudyPresentationStagingState` above both Study and Settings: persisted changes become the
next-item preference while the current item retains its effective snapshot and active audio
loop. Item identity change atomically promotes the persisted preference. The Study-header quick
control and full Settings therefore share persistence without duplicating configuration or
moving presentation decisions into the learning engine.

Presentation resolution is phase-independent. `StudyPresentationAvailability` contains only
semantic content/audio availability, and `StudyPresentationPolicy` has no reveal, Question,
Answer, review, or workspace-state input. Question scene rendering and the focused Answer
renderer independently select their valid layers from the same `EffectiveStudyPresentation`.
Likewise, the effective model declares autoplay eligibility while
`StudyAutoplayCoordinator` owns whether a Question or Reveal transition may consume it. This
keeps workspace sequencing outside preference policy without moving it into Product Brain or
the scheduler.

Learning text semantics are explicit before Desktop rendering. `LearningContentProjector`
assigns required `LearningTextRole` values from canonical primary, translation, example, and
neutral slots; `LearningContentPresenter` maps them one-to-one into `PresentedTextRole`.
`LearningSceneRenderer` never infers language from scene type, reveal state, block position, or
text content. Workspace phase selects valid Question/Answer blocks, while semantic role maps
each selected block to `EffectiveStudyPresentation`. `LISTENING_RECALL` and `IMAGE_RECALL`
therefore consume the same sanitized semantic projection without duplicating meaning content.

Study shortcuts are also Desktop interaction configuration rather than learning-domain policy.
`DesktopKeyChord`, `StudyShortcutCommand`, immutable `ShortcutRegistry`, and stable string
serialization contain no Compose types. `DesktopRuntimeConfiguration` persists the complete
duplicate-free registry and falls back atomically to defaults when an optional legacy value is
missing or invalid. Physical Compose key events cross one terminal adapter into a
`DesktopKeyChord`; the registry selects a command and the existing workspace-state router
decides whether that command is currently permitted. Settings receives and replaces the
registry through the existing runtime-configuration callback. Study status presentation reads
the same injected registry, so its shortcut hints cannot drift from routing. No singleton or
mutable global shortcut state is introduced.

Phase 6 evolves the verified learning flow through existing ownership seams. `StudySession` and
application session/queue/review use cases own durable lifecycle and atomic learning behavior;
Desktop `StudyFacade`, `StudyUiState`, and `StudyScreen` project that behavior into current-item,
reveal, feedback, keyboard, focus, and accessibility presentation. New lifecycle state belongs
in domain/application only when it has durable learning semantics. Compose-only interaction
state must not be persisted merely to simplify UI rendering.

P6-09 freezes these seams with a composition-level verification boundary rather than a new
runtime abstraction. A deterministic OPD3 fixture enters through `packageImporter`, is observed
through `InstalledPackageQueryService`, and is learned through `StudyFacade` backed by persisted
repositories. Restart, final-review undo, re-rating, recovered completion, and review-event
cardinality are asserted there; specialized failure, renderer, keyboard, and accessibility
behavior remains in the narrower owning suites.

The current retrieval-practice workspace is the first supported learning experience, not the
definition of the entire product. Generalization requires a present content or learning-mode
use case and tests; hypothetical mobile, AI, or non-card experiences do not justify a new
abstraction. Phase sequence and open product decisions are owned by
[`ROADMAP.md`](ROADMAP.md#phase-6--learning-experience).

### Desktop 1.0 architecture freeze

The Learning Session lifecycle, Review Workspace state/action model, Learning Content contract,
rich-content renderer boundary, and session progress/completion projection are stable Desktop
1.0 contracts. They may change only for a concrete defect or accepted use case with an explicit
compatibility assessment, focused/regression tests, and corresponding architecture updates.

Ownership is fixed across these boundaries: Domain owns durable `StudySession` lifecycle,
current item, reveal state, and pending review intent; Application owns orchestration, atomic
review, scheduler interaction, progress projection, and recovery; Desktop owns workspace and
rendering projections plus temporary focus/feedback state. Desktop never owns the scheduler,
durable session lifecycle, transaction boundary, or durable progress count.

The approved Study answer surface remains a Desktop projection over the existing learning scene
and audio-controller boundaries. Legacy combined IPA/part-of-speech text is normalized only in
`FocusedVocabularyAnswerResolver`; image, meaning, example, loop-state, and feedback components
consume projected paths and labels. The fixed rating dock invokes the existing review callbacks,
and human-readable interval labels format the committed scheduler result without recalculation.
Question visibility is resolved through `EffectiveStudyPresentation`; reveal crosses into the
separate `FullAnswerPresentation`, which discloses every available identity, pronunciation,
part-of-speech, image, meaning, definition, and example field without consulting Question
visibility switches. Exact semantic target matching annotates rendered English and Vietnamese
examples only; it never mutates canonical content or persistence. English matching is
case-insensitive, all matching requires Unicode-aware word boundaries, and an uncertain match
produces unannotated text rather than a fuzzy guess.

Full Answer media follows the same truth-mode isolation. `LearningContentPresenter.presentAvailable`
is the authoritative current-item media projection; `FocusedVocabularyAnswerResolver` combines
that complete presentation with the current scene without allowing Question sanitization to
discard answer paths. `FullAnswerAudioPresentation` deduplicates typed primary, meaning, and
example roles. A live Question-to-Answer transition plays primary English once; an initially
recovered Answer, recomposition, resize, preference Apply, stale item, or missing primary path
does not autoplay or fall back across roles. Revealed interaction bindings remain primary word/
image and English-example loop, Vietnamese meaning/example once, and primary replay once.

The authoritative learning-stage value is `MemoryState.stage`. Selection may construct an
effective `MemoryState.new(...)` without persisting it, so persisted-record existence is an
explicit property and never the definition of NEW. `NextLearningItem.isNew`, the Desktop badge,
and typed stage diagnostics all use the effective stage; diagnostics carry stable item/content
identity, review count, last-review time, persistence existence, and queue-selection boundary
without logging learner content.

Audio routing retains the authoritative semantic slot from `ContentMedia` through
`LearningContentBlock.Audio` and `PresentedLearningBlock.Audio`. Primary-word,
meaning-translation, example-primary, and example-translation are typed roles independent of
localized accessibility labels. Desktop uses those roles only to select the correct existing
`LearningContentAudioController` interaction; role projection adds no persistence schema.
Interactive audio presentation has one action owner per surface and derives hover, press,
keyboard-focus, disabled, and active-loop visuals from ephemeral Compose interaction/controller
state. Front image prompts retain typed primary-word audio and invoke single-play; revealed
word/image and English-example surfaces may toggle the controller's single loop. Vietnamese
meaning/example surfaces remain single-play, and no interaction state is persisted.

### One-step session undo and interruption

Pause remains the absence of Desktop interaction, not a `StudySession` status; resume recovers
the same persisted `ACTIVE` session, current item, reveal state, pending intent, and queue.
Each committed session review replaces the previous undo checkpoint with immutable before-state:
the review-event identity, scheduler memory before-state/existence, session counters and sets,
presentation/reveal state, plus the queue item identity. Application-owned undo verifies that
the event and current memory still match, then removes the latest event, restores or deletes
memory, rewinds the queue, and restores/reopens the session inside the established transaction.
Clearing the checkpoint makes retry idempotent and prevents multi-level undo. A completed queue
is retained only while its final review is undoable, including across restart; legacy sessions
without the optional checkpoint remain readable and have no undo action.

Completed-session recovery is also installation-lifecycle scoped. For package-owned sessions,
the current `ACTIVE` `InstalledPackage` is canonical: package/topic provenance must match, the
session must have started at or after the package's current `installedAt`, and all persisted queue
items must resolve to content owned by that package. `ContentPackage` existence is not lifecycle
authority. A same-package completion that fails these checks is stale and its session/queue are
purged; a completion owned by another package is left untouched. Orphan package reimport clears
only learning state and sessions proven by the candidate package/topic/exact learning-item graph,
inside the existing import transaction.

### Desktop learning-workspace interaction contract

Keyboard routing, focus identity, action availability, accessibility copy, and recoverable-error
presentation are Desktop projections. Mouse and keyboard converge on `StudyViewModel`; its
single-action guard prevents re-entry while the state machine rejects stale actions. Focus is
reset deterministically by workspace phase/item identity and is never persisted.

| Action | Allowed presentation state | Keyboard | Focus/error behavior |
|---|---|---|---|
| Start/Retry | Idle or Completed / RecoverableFailure | Enter or Space | Workspace focus; safe retry preserves last confirmed state |
| Show answer | Question only | Enter or Space | Moves to revealed-answer action group |
| Rate | AnswerRevealed only | 1–4 | Disabled while dispatching; next Question or Completed receives focus |
| Undo | Any visible checkpoint, including Completed | Ctrl+Z | Reopened Question/Answer receives focus; failed undo preserves checkpoint |
| Pause/leave | Active workspace | Escape | Navigates away without changing durable `ACTIVE` lifecycle |
| Audio | Focused media button | Enter or Space (Compose control) | Local fallback remains visible and does not close the session |

Shortcuts are suppressed for busy, repeated, or text-input-owned input. Rich-content headings,
images, unavailable media, action controls, progress, completion, and errors expose semantic
labels without relying on color alone. User-facing failures are classified at the Desktop
boundary and never include raw exception messages or stack traces.

### Learning Session lifecycle checkpoint

PLE-032-B1 places manual general-Study continuation behind
`ContinueGeneralStudyUseCase`/`LearningEngine.continueGeneralStudy`. The application boundary
validates the authoritative finished predecessor, learner, and package/topic scope; invokes the
existing ordinary Session start and queue-planning authorities; and returns Accepted, NoWork,
or Rejected. Each accepted Cycle remains an ordinary `StudySession`.

The next Session identity is deterministic from the preceding `SessionId`. Existing persisted
Session/queue repositories therefore prevent sequential or restart-visible repeated requests
from silently accepting a second Cycle without adding a FlowId or Continuous Review schema.
This is not an atomic concurrent-create guarantee: repository-level conditional creation and
durable continuous intent remain later PLE-032 scope. Desktop is a thin adapter for the manual
Continue action and no longer deletes the finished predecessor before requesting continuation.

`StudySession` is authoritative for the durable learning lifecycle. An active session may hold
the current learning-item ID, its presentation time, reveal state, and at most one
`PendingSessionReview`. Pause introduces no domain status: leaving and reopening an `ACTIVE`
session is resume. Window, focus, scroll, animation, expanded-panel, and Compose state remain
Desktop-only.

`GetNextSessionItemUseCase` checkpoints the queue-selected item before returning it. Review
first stages a stable event ID, rating, time, and response duration. The existing transaction
then writes the event and memory state, records the session review, clears its checkpoint, and
advances the queue. If interrupted before commit, `LearningEngine.recoverActiveSession` replays
that exact intent. Session record checkpoint fields remain optional/defaulted in schema v1, so
legacy data means “no presented item” and needs no destructive migration.

### Product Brain session completion boundary

`ProductBrainSessionCompletion` owns the platform-neutral completion plan assembled from the
bootstrapped context and goal, scene result, learning evidence, adaptive decision and trace,
learner explanation, timeline, and final difficulty. It generates learner reflection, summary,
learning outcome, and the existing review rating intent. It does not call a scheduler or
repository.

`ReviewSessionItemUseCase` remains the only scheduler and committed-review owner. Its
`ReviewResult` is projected into a learner-facing scheduling outcome before
`FinishStudySessionUseCase` persists the finished session. `StudySession` may retain an optional
`SessionCompletionSnapshot` containing learner-facing completion and scheduling guidance.
The corresponding schema-v1 record field is optional, so legacy sessions decode with no
snapshot and require no migration. The snapshot is separate from `DecisionTrace` and other
technical diagnostics.

Desktop requests completion and renders the persisted snapshot. It does not derive reflection,
outcome, reinforcement, rating, or scheduling guidance. Starting another study workflow removes
the prior snapshot from presentation state; it does not rewrite the historical finished session.

### Review Workspace projection boundary

Desktop projects the Learning Session contract through `ReviewWorkspaceState`: `Idle`,
`Preparing`, `Question`, `AnswerRevealed`, `Feedback`, `Transitioning`, `Completed`, and
`RecoverableFailure`. It owns action availability and deterministic presentation transitions,
not lifecycle, scheduling, or persistence. Question accepts only Show Answer; Answer Revealed
accepts the four domain ratings in enum order; recoverable failure accepts Retry. Transitional
states accept no user action.

The synchronous facade may pass through Preparing, Feedback, and Transitioning within one call
and return the next observable state. Restart projects the persisted current item and reveal
state; application recovery resolves a pending review intent before Desktop projection.
Window, focus, animation, and scroll are absent. Existing boolean fields on `StudyUiState`
remain compatibility projections, while action dispatch and keyboard routing use the explicit
workspace state.

### Learning Content Model boundary

`Content` remains the canonical domain aggregate. `ContentText` records source text plus the
minimal `PLAIN_TEXT` or `MARKDOWN` format; `ContentMedia` remains reference-only. Package JSON
and `ContentRecord` are transport/persistence DTOs, not learner-facing models. Legacy DTOs omit
format and rich metadata fields and therefore decode to plain text with absent media.

`LearningContentProjector` maps `Content` into ordered Question, Answer, and optional Example
sections. Blocks are renderer-neutral Text, Image, Audio, or Unavailable Asset values. Question
orders primary text, image, and primary audio; Answer orders pronunciation, translated text,
and translated audio; Example preserves example/example-translation and audio order. Missing
optional examples produce no section; missing answer text produces a semantic
unavailable-answer block for adapter localization.

Asset blocks contain normalized local relative references only—never bytes, Compose objects,
remote fetch behavior, or executable content. Unsafe/remote references and references reported
missing by an adapter become `UnavailableAsset`. Safe HTML is intentionally absent because no
current import/domain use case establishes a sanitization contract. `NextLearningItem` exposes
the projection to Desktop; Review Workspace still controls reveal/actions and content never
controls session lifecycle or scheduling.

### Desktop rich-content renderer boundary

Desktop `LearningContentPresenter` converts the Application projection into visible sections
according to `ReviewWorkspaceState`: Question is always first; Answer and optional Example exist
only after reveal. It resolves asset references through `ContentMediaStorage`, never package or
persistence DTOs. Missing assets become localized presentation fallbacks.

The Markdown adapter is an allowlist for paragraphs, line breaks, headings, lists, emphasis,
inline code, and fenced code. HTML and remote/executable content are not interpreted. Compose
loads verified local images with fit scaling. Semantic audio controls delegate to one
screen-scoped Desktop controller; the controller owns only playback state, cancellation, and
primary-audio replay. The Java Sound adapter decodes supported native formats and MP3 through a
registered SPI into PCM, streams PCM to one `SourceDataLine` off the Compose event thread, and
exposes Starting, Playing, Failed, and Idle states. Starting another clip, changing content,
pausing, completing, or disposing the screen cancels and releases the active output. Generation
tokens make callbacks from superseded clips inert. Playback has no callback into learning
actions, scheduling, evidence, or persistence.

An ACTIVE Desktop study session selects a focused shell projection: application branding,
navigation sidebar, technical status bar, and dashboard counters are removed from the attention
path while the centered, bounded, scrollable learning surface remains. Compact progress, Undo,
and Pause stay available above the content. Question, Answer, and Example hierarchy and all
actions remain projections of `ReviewWorkspaceState`; shell focus mode owns no lifecycle,
scheduler, progress, evidence, or persistence decisions.

### Adaptive Learning Scene boundary

Desktop no longer sends `LearningContentPresentation` sections directly to Compose rendering.
Application `LearningExperiencePolicy` reads semantic `LearningContent` before filesystem
resolution and returns a platform-neutral `LearningExperiencePlan`. The policy owns semantic
capabilities, reveal-dependent supporting roles, and a non-empty ordered
`LearningExperienceOptions`: Image when eligible, Listening when eligible, then Prompt as the
mandatory safe fallback. It does not select the final primary kind.

`ExperienceSelectionEngine` delegates an `ExperienceSelectionRequest` to an injected
`ExperienceSelectionStrategy` and constructs the authoritative `ExperienceSelectionResult`.
The result retains selected kind, ordered available kinds, selected index, and semantic reason;
the engine rejects strategy indices outside availability. `RoundRobinExperienceStrategy` is
the first stateless implementation and uses floor-mod of a supplied `Long` ordinal. It reads no
time, random source, global counter, scheduler, history, persistence, or platform state.
`ExperienceSelectionProfile.AUTOMATIC` projects full policy eligibility to the passive
Image/Listening/Prompt sequence while preserving order and Prompt fallback. Full options remain
available for explicit choices. `ExperienceRotationContext` derives a zero-based ordinal from
`LearningSessionProgress.currentPosition`, tied to the real session and learning-item identities.
The first item preserves ordinal-zero behavior; later queue positions rotate deterministically.
Reveal, retry, pause/resume, and Typing interaction do not change it. Undo follows the rewound
queue position. No rotation field or schema is persisted; active-session restart reconstructs
the context from existing session/queue state, and a new session starts at zero.

Desktop `LearningContentPresenter` independently resolves media into Path-backed presentation
blocks and localized fallbacks. `DesktopLearningSceneProjector` trusts
`ExperienceSelectionResult.selectedKind`; it does not inspect resolved image/audio blocks,
normalize ordinals, or repeat eligibility/round-robin rules. It maps the selection result plus
plan and presentation into `ImageScene`, `ListeningScene`, or `PromptScene`, with revealed
`MeaningScene` and `ExampleScene` support. Thus a declared but locally missing image remains an
Image experience with a safe unavailable-media presentation rather than a crash or policy
change.

Typing Recall is the first active-production scene. `TypingRecallPromptExtractor` derives one
expected answer solely from ordered, non-blank semantic answer text blocks, joining multiple
blocks with a line break. `TypingAnswerEvaluator` performs locale-stable normalized exact
matching: outer whitespace is trimmed, consecutive whitespace/line breaks collapse, and case is
folded with `Locale.ROOT`; punctuation, diacritics, symbols, and word order remain significant.
Policy appends `TYPING_RECALL` after the mandatory Prompt fallback only when extraction succeeds.
Automatic selection excludes it in v1; it remains available through explicit `USER_CHOICE`.

Desktop owns the explicit per-item Default/Typing chooser, input/focus/submit state, accessible
localized feedback, and reset by durable learning-item identity. The chooser still obtains an
authoritative `ExperienceSelectionResult` through `ExperienceSelectionEngine`.
`DesktopLearningSceneProjector` maps that result to `TypingScene` without re-evaluating
eligibility. At this foundation's delivery point, submission called the existing reveal action
and manual rating remained the sole review outcome. PLE-038/039 supersedes that historical
Typing outcome: exact success now produces an automatic rating through the decision pipeline
above, while Reveal remains Again and Scheduler/FSRS still owns scheduling.

Shared kind, capabilities, context, options, plan, policy, selection request/result/reason,
strategy, and engine contain no Compose, Desktop, localized string, playback, `Path`, or
filesystem dependency. Desktop scenes retain
`PresentedLearningBlock`, Path resolution, localized instructions, supporting-scene structure,
renderer ordering, playback, focus, and layout. Typing input, correctness, selected mode, and
history are deliberately transient. This foundation is not fuzzy/linguistic matching,
alternative-answer semantics, persisted rotation history, automatic
rating, adaptive selection/difficulty, personalization, Story Mode, AI, metrics, or a Kotlin
Multiplatform migration.

### Learning-session progress and feedback boundary

### Platform-neutral Learning Flow boundary

A Learning Session owns durable item/reveal/review lifecycle; a Learning Flow is transient
presentation orchestration for that one session item. The Product Brain pipeline is structured into
pure platform-neutral layers: `LearningObjectivePolicy` selects the intended learning outcome;
`LearningStrategyPlanner` derives strategy behavior without rotation dependency;
`LearningFlowTemplateFactory` translates strategy behavior into a reusable, deterministic, and immutable
`LearningFlowTemplate` containing semantic template slots (`ROTATED_PRIMARY`, `OPTIONAL_TYPING`, `ANSWER_REVEAL`, `RATING_READY`);
`ProductBrainPlanner` acts as an orchestration-only boundary; `LearningFlowInstantiationService` resolves
runtime experience selections for template slots and delegates to `LearningFlowPlanner` to produce the concrete
`LearningFlowDefinition`; and `LearningFlowController` executes stage transitions.

`LearningFlowPlanner` is deliberately not Product Brain. It accepts a `LearningFlowTemplate`, resolved selections,
and `ExperienceRotationContext` to instantiate typed runtime stages/identity without accepting product policies or selection engines.
Desktop `DesktopLearningFlowCoordinator` depends strictly on `ProductBrainPlanner` and `LearningFlowController`.


`LearningFlowController` is a pure immutable transition engine. Experience completion advances
in order; the final experience emits `AnswerRevealRequested`; only successful existing reveal
reconciliation reaches rating-ready. It performs no I/O, playback, scheduling, review, queue, or
persistence operation. Progress counts user-actionable experience stages; reveal and
rating-ready are semantic system stages.

Desktop `StudyViewModel` owns the transient coordinator so recomposition, focus, resize, audio
replay, and navigation pause do not reset it. A changed session/item context creates a new
definition, including after undo. App restart has no exact stage persistence: an unrevealed item
restarts at stage one, while an authoritatively revealed item reconstructs rating-ready.
`StudyFacade` remains reveal/review validation authority. The foundation originally exposed only
manual review commits; PLE-038/039 later added exact-Typing automatic rating through the same
application review transaction without moving scheduler authority into the controller or UI.

`LearningSessionProgress` is an Application read projection. `StudySession.totalReviews` is the
durable count of committed reviews; immutable `StudyQueueProgress.currentIndex` is the count of
planned entries already processed and may be larger when eligibility changes cause queue skips.
Their difference is reported explicitly rather than relabeling skipped entries as reviews.

Queued sessions always have a stable known total because the planned item list does not replan.
The legacy no-queue compatibility path exposes an unknown denominator and no percentage. Empty
queues are known and completed. Completion comes from the queue and is finalized by the session
use case; null UI content, animation, or local indices are never completion authority.

Successful review results carry the post-transaction progress projection and the scheduler result
already calculated by Application. Desktop formats that result as ephemeral feedback but never
reruns scheduling or persists presentation state. A staged/failed review leaves session review
counts and queue position unchanged; pending recovery advances both exactly once in the existing
transaction boundary.
If restart occurs after the final atomic review but before normal session finalization, recovery
captures the completed queue projection before deleting the queue and Desktop deterministically
restores the Completed workspace.

### Durable topic identity and learner-topic resume boundary

`TopicId` is a durable content-domain identity distinct from the version-sensitive `PackageId`.
An installed `ContentPackage` persists its topic ID. Legacy package records without that optional
field derive a deterministic ID from logical package name and format; record mapping never uses
filesystem path, display order, or a new random value. Compatible package replacement carries the
existing topic ID forward. A future portable OPD3 export must preserve this explicit identity;
Beta-L01 does not change the package format or implement export.

`StudySession.topicId` links a checkpoint to a topic without moving learner data into package
state. The effective resume key is `(LearnerId, TopicId)`. The existing session and queue stores
remain checkpoint authority, including current item, reveal/pending state and committed progress.
`MemoryState` and `ReviewEvent` remain authoritative per `(LearnerId, LearningItemId)` for
difficulty, stability, mastery/review counts, due time and review history. Topic state does not
duplicate those values.

Application topic resolution maps selected content through installed library/package ownership.
For pre-package local content, a deterministic content-ID compatibility identity is used.
Topic-specific recovery selects only the requested learner-topic session. Desktop clears its
transient projection when switching, delegates recovery/start to Application, and contains no
scheduler, progress, persistence, or resume rule.

## Desktop shell navigation boundary

`NavigationDestination` is the ordered registry for stable route IDs and shell labels. The
primary product vocabulary is Home, Learn, Review, Library, and Settings; Statistics remains a
separate verified destination. Sidebar accessibility, cyclic traversal, and F1–F6 shortcuts
consume the same registry/order rather than maintaining parallel navigation labels.

`DesktopWindowPlacementSession` owns the separate schema-v1 `window-state.properties`
contract. It restores bounded size, optional absolute position, and maximized state. Missing
state uses centered defaults and may be saved; invalid state also uses defaults but preserves
the original file and disables automatic save for that process.

Valid captures are clamped to safe bounds and replaced through a same-directory temporary file,
preferring atomic move. Window state is UI configuration only and is never mixed with learning
data or migrated automatically.

## Desktop runtime identity boundary

`DesktopApplicationIdentity` is the single Desktop contract for application ID, display name,
and filesystem-safe directory name. `DesktopBuildMetadata` represents application version,
build channel, revision, and build number as validated values loaded from a generated classpath
resource.

Gradle generates that resource from the root project version and optional
`learningEngineBuildChannel`, `learningEngineBuildRevision`, and `learningEngineBuildNumber`
properties. Local defaults are deterministic and no build timestamp is synthesized, so clean
builds remain reproducible at this boundary.

The Desktop Compose application also owns the Windows native-distribution boundary. Gradle
produces an unpacked application image plus MSI and EXE packages named `LearningEngine`; the
package version is the root version's numeric core normalized to three components. The same
build task writes package name, version, and formats into the generated metadata resource,
which `DesktopDistributionMetadataLoader` validates in runtime/tests. Packaging performs no
publishing, signing, installation, user-data migration, or external release action.

`DesktopRuntimeDirectoryResolver` maps that stable identity into separate data, config, cache,
logs, and temp paths. Windows uses `LOCALAPPDATA` with a user-home fallback; macOS uses the
appropriate `Library` locations; Linux honors XDG data/config/cache/state variables with
standard user-home fallbacks. Temp remains under `java.io.tmpdir`.

Resolution performs no writes. If the established `~/.learning-engine/data` directory already
exists, only the data path continues to reference it; the resolver never moves or copies that
data. New runtime directories are created later by the startup lifecycle.

`DesktopRuntimeConfigurationLoader` owns the schema-v1 `runtime.properties` read boundary.
It returns typed defaults only when the file is absent. Once a file exists, blank content,
missing required keys, unsupported schema, invalid enums, and invalid retention fail with
structured file/property context; diagnostics never include property values. The optional
schema-v1 theme key defaults to System so existing Milestone 6 files remain compatible.

`DesktopRuntimeConfigurationStore` writes only explicit typed Settings changes through a
same-directory temporary file and atomic replacement where supported. The runtime session
updates its in-memory configuration only after that write succeeds. Compose resolves Light,
Dark, or the current system appearance from the typed preference; UI code does not edit
properties directly. Loading never normalizes or overwrites an invalid file.

The same typed configuration stores an English or Vietnamese locale, defaulting an absent
schema-v1 key to English for compatibility. `DesktopLocalization` is the deterministic catalog
boundary for shell and Settings vocabulary. Navigation route IDs and enum ordering remain
locale-independent; Compose obtains labels from the selected catalog and updates only after a
successful configuration write. Feature-screen text can migrate into this boundary
incrementally without coupling domain/application code to locale concerns.

The shell owns two explicit focus regions: navigation and current content. Ctrl+F6 and
Ctrl+Shift+F6 cycle those regions through a pure `ShellFocusRegion` contract; Tab and Shift+Tab
remain native Compose traversal within each focus group. Existing unmodified function-key
navigation and screen-local shortcuts keep their established precedence.

`DesktopStartupState` is a presentation-only one-way boundary from Starting to Ready. Runtime
directory/configuration/logging/persistence composition still completes before Compose starts;
the localized startup surface standardizes only the first rendered UI transition and cannot
mask lifecycle failures. The About dialog consumes the immutable `DesktopRuntimeDiagnostics`
snapshot already passed through composition, so it does not re-read system state or expose
unredacted user-home paths.

`FileDesktopRuntimeLogger` writes one UTF-8 file per runtime session under the resolved logs
directory. Level and event code are typed/validated, accepted records are flushed immediately,
and multiline text is normalized to one record. The API does not implicitly serialize
exceptions or persisted values.

Retention runs when a session logger opens and deletes only oldest regular files matching the
exact Learning Engine runtime-log filename contract. Unrelated files and symbolic links are
excluded. Retention count comes from the validated runtime configuration.

`DesktopRuntimeLifecycle` owns startup and shutdown ordering. It creates the five resolved
runtime directories, loads build/config contracts, opens the logger, and composes the persisted
application against the selected data path before Compose starts. Its session owns the logger;
Desktop `main` closes it in `finally` after the application loop exits.

Composition failure retains the original throwable, logs only its exception type, closes the
logger, and suppresses any logging/cleanup failures onto the original. Session close is
idempotent and emits one shutdown event.

`DesktopRuntimeDiagnostics` is an immutable snapshot assembled during startup and passed through
Desktop composition into Settings/About. It includes application/build identity, OS/JVM data,
selected data/config/log paths, current log, and legacy-data mode. Paths beneath `user.home`
are rendered with `<user-home>` in support text and UI, preventing usernames from leaking into
copied diagnostics. UI code consumes the snapshot and does not query system properties.

`DesktopDiagnosticExporter` serializes only that already-redacted immutable snapshot into a
deterministic schema-1 UTF-8 text document. It does not read logs, configuration, persistence,
or learning content. The exporter requires an existing destination directory, refuses an
existing target, and places a same-directory temporary file atomically where supported. The
native file chooser remains a Desktop adapter concern wired at `DesktopMain`.

`DesktopRecoveryManager` owns manual schema-1 backup archives across the resolved data and
configuration directories. Logs, `.tmp` artifacts, diagnostic exports, and the configuration
`backups` subtree are outside the inventory. Each sorted regular-file entry has an exact size
and SHA-256 checksum in the manifest; unsafe paths, duplicates, unknown roots, incompatible
formats, inventory drift, and checksum failures are rejected before mutation.
Manifest file counts must be non-negative and exactly match the archive payload count before
inventory objects are allocated. This prevents malformed negative or oversized declared counts
from being interpreted as an empty snapshot or exhausting validation before mutation.

Restore is whole-snapshot replacement, never merge. It is refused while Desktop reports an
active persisted Study session; all other persistence commands and restore execute
synchronously on the single Desktop event boundary. A validated safety snapshot is created
under configuration backups before replacement. Current bytes are also retained for immediate
rollback if any replacement write fails. Successful restore closes the process so no
pre-restore repository instance remains live against replaced files.

`DesktopOnboardingSession` requires onboarding only when both its schema-1 completion marker
and regular persisted data are absent. Existing profiles therefore do not receive a synthetic
first-run state. The optional starter lesson is assembled in a temporary OPD3 archive and
installed through the production import, validation, registration, and transaction path;
onboarding never writes learning repositories directly.

`scripts/verify-windows-beta.ps1` is the reproducible release-evidence boundary. Its default
mode runs clean tests and native packaging, hashes artifacts, reports signature state, and
performs an actual UTF-8 read/write probe in a Unicode path. MSI installation, uninstallation,
and previous-version upgrade are explicit opt-in operations intended only for a disposable
clean Windows verification machine. Generated evidence remains a build artifact, not a tracked
claim of release approval.

The Windows runtime image explicitly includes `jdk.accessibility`. A user profile may request
the standard Java Access Bridge through `.accessibility.properties`; `java.desktop` alone does
not contain its implementation, so omitting this JDK module makes the jpackage launcher fail
before Compose can open. `verifyWindowsLauncher` builds the app image, creates an isolated
accessibility-enabled profile, launches the native executable against its bundled runtime, and
requires the startup probe to exit successfully without touching real user data. The normal
launcher path is unchanged unless the internal verification property is explicitly enabled.

## Desktop Study accessibility presentation

Desktop Study derives screen-reader status and progress text through the pure `StudyAccessibilityPresentation` model. Compose semantics consume that model, keeping accessibility wording testable without UI instrumentation and aligned with the same `StudyUiState` that drives visible controls and keyboard shortcuts.


## Desktop Study action accessibility

`StudyActionAccessibility` is the single presentation boundary for Study control labels, shortcut hints, and screen-reader descriptions. `StudyScreen` applies those descriptions directly to retry, start, reveal, and rating controls while keyboard action resolution remains in `StudyKeyboardShortcut`.


## Desktop Study rating guidance

`StudyRatingGuidance` centralizes the learner-facing meaning of Again, Hard, Good, and Easy. `StudyScreen` renders the same ordered guidance beside the rating controls and exposes a combined semantic description, keeping scheduling intent, keyboard shortcuts, and screen-reader wording aligned through one pure presentation model.


## Accessible scheduler feedback

`StudySchedulerFeedbackAccessibility` converts the latest persisted scheduler decision into a concise, screen-reader-safe announcement. `StudyAccessibilityPresentation` includes that result when the next question becomes ready or the session completes, so keyboard-only and screen-reader users receive confirmation that the previous rating was saved together with its next interval and review time.


## Scheduler feedback card semantics

The visible `SchedulerFeedbackCard` uses the same `StudySchedulerFeedbackAccessibility` boundary as live announcements. Its merged semantic description exposes the rating, stage transition, interval, next-review time, difficulty and stability transitions, review count, and lapse count as one coherent screen-reader unit.


## Study content semantics

`StudyContentAccessibility` labels the active learning text as a Study prompt and exposes the translation only after answer reveal as a Study answer. This prevents unlabeled prompt/answer text from becoming ambiguous to screen-reader users while preserving the existing reveal boundary.


## Study focus transition boundary

`StudyFocusTransitionKey` converts the current Study state into a stable, testable focus identity. `StudyScreen` reacquires its keyboard focus whenever the phase, reviewed count, current item, or recoverable error changes. This keeps Enter, Space, and 1–4 shortcuts active after start, reveal, grade, retry, item advance, and completion transitions without coupling Compose focus behavior to scheduler internals.


## Session completion summary semantics

`StudySessionSummaryAccessibility` converts the completed Study state into one ordered semantic description containing the session title, total reviewed items, new/review split, optional lesson progress, and the Enter-key next action. The visible completion card merges descendants so screen readers hear a concise result instead of disconnected labels and numbers.


## Scheduler stage-transition presentation

`formatStudyStageTransition` is the presentation boundary for scheduler stage changes shown by Desktop Study. It converts enum-style stage identifiers into readable labels and inserts the Unicode transition arrow from UTF-8 source, preventing corrupted mojibake from leaking into visible and screen-reader scheduler feedback.


## Content Library empty-state presentation

`ContentLibraryEmptyPresentation` owns the first-run empty-state copy and semantic description. The empty card now includes a direct import action, so new users do not need to discover the same action in the header before they can create their first library.


## Recoverable Content Library load errors

`ContentLibraryLoadErrorPresentation` normalizes repository/load failures into a stable title, preserved technical detail, recovery guidance, retry label, and one assertive semantic announcement. `ContentLibraryScreen` keeps import diagnostics separate while routing `loadError` through a dedicated retry card backed by `refresh`.


## Review History accessibility presentation

`ReviewHistoryAccessibility` owns count grammar, empty-state semantics, and the ordered semantic summary for each review event. The visual card keeps its existing layout while screen readers receive one predictable sequence: rating, review time, response time, stability, and difficulty.


## Statistics accessibility presentation

`StatisticsAccessibility` normalizes statistic values and owns both per-card semantics and the ordered screen summary. Placeholder values remain visually unchanged while assistive technology receives the explicit word “Unavailable” instead of punctuation with no meaning.


## Settings accessibility presentation

`SettingsAccessibility` groups every configuration label and value into one semantic unit and builds section summaries in the same order as the visible rows. Blank values receive a stable unavailable fallback rather than becoming silent or ambiguous.


## Sidebar navigation accessibility

`SidebarAccessibility` derives the visible label, selected state, and concise spoken description for every desktop navigation destination. Sidebar entries expose tab semantics and explicitly announce the active destination without changing navigation behavior.


## Shell chrome accessibility presentation

`ShellChromeAccessibility` owns the semantic presentation of the persistent desktop header and status bar. The header is exposed as one heading, while the status bar announces engine and dashboard state in visual order with stable fallbacks for missing values.


## Dashboard summary accessibility

`DashboardAccessibility` centralizes spoken presentation for the dashboard page heading, section headings, and metric cards. Each metric is exposed as one ordered title/value/supporting-text unit, and blank metric content receives stable unavailable fallbacks.


## Dashboard visualization accessibility

`DashboardVisualizationAccessibility` centralizes chart identity, no-data presentation, and retention-gauge announcements. Visualization titles are exposed as headings, empty chart messages are grouped into one ordered semantic unit, and retention values are clamped and announced as percentages.


## Dashboard chart-data accessibility

`DashboardChartDataAccessibility` owns semantic descriptions for individual chart values and heatmap days. Forecast and scheduling rows announce labels, values, and units; memory stages announce count and percentage; heatmap cells announce a full date and review activity state.


## Lesson Browser accessibility presentation

`LessonBrowserAccessibility` centralizes semantic presentation for the library browser header, lesson summary cards, selected lesson heading, and lesson properties. Counts are normalized and pluralized, blank values receive stable fallbacks, and study availability is announced without relying on disabled-button state alone.


## Content Library screen accessibility

`ContentLibraryAccessibility` centralizes the page-header count summary, import status announcements, and section-heading wording. Counts are clamped and pluralized, import outcomes use live-region announcements, and blank messages receive stable fallbacks.


## Content Library dialog accessibility

`ContentLibraryDialogAccessibility` centralizes purpose, target context, destructive scope, package-option selection, count grammar, and blank-value fallbacks for every Content Library dialog. Dialog titles are semantic headings and attach-package choices expose selected state without relying on the visible checkmark.


## Content Library card accessibility

`ContentLibraryCardAccessibility` centralizes spoken summaries for libraries, collections, attached packages, installed packages, and label/value properties. Card headings announce identity and high-value counts or metadata while action buttons remain independently discoverable.


## Content Library action accessibility

`ContentLibraryActionAccessibility` owns contextual descriptions for global, library, collection, and package actions. Visible button text remains concise while assistive technology receives the target, result, and confirmation boundary for destructive operations.


## Content Library keyboard navigation

`ContentLibraryKeyboardShortcut` defines a pure keyboard contract for screen-level actions. Ctrl+R refreshes, Ctrl+I opens package import, and Escape unwinds lesson detail before closing the browser. Shortcuts are suspended while any Content Library dialog is visible so dialog input and dismissal remain authoritative.


## Shell-wide keyboard navigation

`ShellKeyboardShortcut` centralizes global function-key navigation, cyclic screen traversal, and destination refresh. `LearningShell` owns the single global keyboard boundary and delegates screen-specific shortcuts to child surfaces. Sidebar labels expose the same F1–F6 contract visually and semantically.


## Shared Desktop load/recovery state

`DesktopLoadState` is the cross-screen loading and failure boundary for Dashboard, Statistics, and Review History. Each view model keeps the last successful data while a refresh is in progress or fails. `DesktopLoadStateCard` provides one consistent polite loading announcement, assertive failure announcement, and retry action. The shell supplies refresh callbacks through `ContentHost`, keeping recovery owned by the corresponding view model.


## Batch63 search and discovery epic
Batch63 adds shared search normalization and result announcements, Review History query/rating/sort controls, and Lesson Browser query/translation/sort controls with deterministic pure projections and regression tests.

## Shared search refinement presentation

Desktop search surfaces derive active query, filter, and sort state through `SearchRefinementState`. `SearchRefinementPresentation` owns visible and assistive wording, while each feature maps its own default filter and sort values. Reset remains an explicit UI action that restores all three defaults together.


## Shared search empty-state recovery

Desktop search surfaces use `SearchEmptyStatePresentation` to distinguish missing source data from refinements that hide existing data. `SearchEmptyStateCard` renders Clear search and Reset view only when those operations can recover visible results, keeping visual and assistive-technology behavior consistent across Review History and Lesson Browser.

## Shared search keyboard boundary
Search keyboard intent is resolved by a pure presentation-layer contract in `desktop.ui.search`. Feature screens translate Compose key events into that contract and execute only feature-owned callbacks, keeping focus and recovery behavior consistent without coupling search state models to Compose APIs.

## Search option-group presentation

Desktop search screens use `SearchOptionGroupPresentation` and `SearchOptionGroup` for filter and sort controls. Screen-specific adapters map domain enums to stable labels, while the shared presentation validates that exactly one known option is selected and provides group-level and option-level accessibility descriptions. This keeps Lesson Browser and Review History behavior synchronized without coupling their domain filters or sorts.

## Batch68 — Accessible result status
Searchable desktop collections now expose a polite live result status that distinguishes complete collections, filtered subsets, empty matches, and truly empty sources.

## Batch69 removable search refinements

`SearchRefinementPresentation` now owns the ordered, domain-neutral actions for clearing the query, restoring the default filter, and restoring the default sort independently. `SearchRefinementBar` renders that contract while Review History and Lesson Browser map each action to their existing state callbacks. The full reset remains available as a separate atomic recovery action.

### Search match presentation
`SearchMatchPresentation` keeps query matching deterministic and UI-independent. `HighlightedSearchText` is the shared Compose renderer used by search result surfaces.

### Search scope disclosure
Desktop search surfaces use `SearchScopePresentation` and `SearchScopeCard` to disclose which fields participate in text matching and to summarize the active query, filter, and sort state. Feature adapters own their searchable-field lists so the shared UI stays domain-neutral.

## Batch72 contextual search query guidance
Desktop search fields now consume a shared pure presentation contract for placeholders, searchable examples, short-query guidance, and screen-reader wording.


## Batch73 multi-term search semantics
Desktop search now parses normalized, case-insensitive query terms through one shared boundary. Projection matching applies AND semantics across all terms in any order, while result highlighting independently marks every term and merges overlapping ranges.

## Unicode-safe Desktop search boundary

Desktop search canonicalizes query terms and searchable text with Unicode NFKC before
case-insensitive matching. Matching operates on the canonical representation, while a
per-character mapping preserves ranges in the original visible UTF-16 text for highlighting.

Canonicalization is implemented in the shared Desktop search package so Lesson Browser and
Review History cannot drift. It preserves diacritics semantically: canonically equivalent
composed and decomposed forms match, but accent removal is not performed.

## Package import diagnostic boundary

## Desktop long-operation and large-data boundary

Compose callbacks may only publish presentation intent. Import, persisted queries, queue/session
preparation, CRC/media reads, and thumbnail decode run through `DesktopTaskRunner` on a worker
dispatcher; results return on the Compose dispatcher. Import, Library, and Study loading states
are presentation-only and never alter the domain session lifecycle.

Package progress uses application stages and measured counts. A determinate value is capped at
95% until `COMPLETED` is emitted after the transaction, avoiding fake completion. Cancellation
is intentionally absent because current JSON transactions and media extraction have no safe
cooperative cancellation contract.

Large-library queries bulk-load each repository once and index by ID instead of repeating JSON
store scans. Study planning similarly bulk-loads content and learner memory when the repository
supports `MemoryStateQuery`, retaining point-query fallback compatibility. Lesson rendering is
virtualized with stable IDs. Thumbnail I/O/decode occurs only for composed rows, uses image-reader
subsampling, a 96 px bound, deterministic fallback, and a 64-entry LRU cache.

### Content-based JVM package routing

Filesystem discovery treats `.pkg` as an ambiguous container extension. The JVM routing
boundary reads only the first four bytes and then reopens the source at the selected parser:
`OPD3` identifies a binary media package paired with legacy JSON; supported ZIP signatures
identify existing archives. Unknown signatures fail as a typed format error and never reach
`ZipFile`.

An OPD3 binary source pairs only with a regular sibling JSON file whose complete basename
matches case-insensitively. This models Windows deterministically on every platform; zero
matches is missing and multiple case variants are ambiguous. Application installation,
validation, and transaction ownership remain unchanged.

Binary version 1 supports media types 1 (audio) and 2 (image), a 100,000-entry ceiling, strict
UTF-8 names, table/payload bounds and non-overlap validation, and CRC32 payload verification.

Non-fail-fast directory import reports failures as structured application data rather than
forcing Desktop presentation to infer error types from exception text.

Each failed candidate carries:

- the original source;
- a stable diagnostic code and failure category;
- optional package-validation issue codes;
- a user-facing message;
- an explicit recovery action.

The existing `message` field preserves the original exception message for backward compatibility; structured diagnostic and recovery fields carry the new metadata.
Successful candidates remain committed independently, while validation happens before the
candidate's repository transaction.

## OPD3 text-entry safety boundary

OPD3 JSON files are read through `JvmOpd3EntryReader` before deserialization. The reader:

- rejects directory entries where a required text file is expected;
- enforces a configurable uncompressed byte limit;
- checks both the ZIP-declared size and the bytes actually streamed;
- decodes with a strict UTF-8 decoder rather than silently replacing malformed input.

The default limit is 32 MiB per text entry. Limit and encoding failures are application-level
package import exceptions, so directory imports surface them through the structured Batch76
diagnostic path without persisting the failed candidate.

## OPD3 archive-structure safety boundary

Before either the descriptor path or bundle-content path reads a required JSON entry,
`Opd3ArchiveStructureValidator` enumerates the archive metadata once for that opened archive.
It does not read entry payloads. The validator:

- enforces a configurable maximum of 4096 entries by default while enumerating;
- accumulates declared uncompressed sizes against a configurable 512 MiB default budget,
  rejects unknown negative sizes, and checks remaining capacity without arithmetic overflow;
- rejects exact duplicate entry names;
- rejects absolute paths, backslashes, parent traversal, dot or empty path segments, leading
  or trailing separators, and surrounding path whitespace;
- applies Unicode NFKC normalization and rejects distinct names with the same logical form;
- treats case variants of `manifest.json`, `metadata.json`, `contents.json`, and
  `learning-items.json` as ambiguous rather than choosing one implicitly.

Structure and entry-count failures inherit `PackageImportException`. They therefore preserve
Batch76's `MALFORMED_PACKAGE` / `PACKAGE_MALFORMED` diagnostic behavior, occur before the
candidate transaction, and do not prevent later candidates in `importAllDetailed()`.

The total declared-size budget complements rather than replaces Batch77's 32 MiB per-text-entry
streamed limit. Structure validation remains payload-free; the entry reader still verifies the
actual bytes delivered for every required JSON entry.

## Missing package entry contract

`MissingRequiredPackageEntryException` is the shared application-level contract for a required
package entry that cannot be read. It retains the missing entry name as structured context.
`MissingPackageManifestException` and `MissingPackageContentException` remain specialized
subtypes with their original messages, while modern bundle reads preserve the established
`Missing package file: <name>` message.

`PackageImportException` is an `IllegalArgumentException`, preserving the historical bundle
catch contract while allowing every missing-entry failure to follow Batch76's package-import
classification. Missing entries are detected before the candidate transaction.

## Required OPD3 JSON decoding contract

Required JSON syntax and serializer failures are wrapped in `InvalidPackageJsonException` at
the decode boundary. The exception records `manifest.json`, `metadata.json`, `contents.json`,
or `learning-items.json` while using the original parser message unchanged. This gives logging
and future diagnostic export stable entry context without changing Batch76's user-facing
`PackageImportFailure.message` contract.

Only parsing and serialization failures are wrapped. Manifest compatibility, metadata identity,
count, integrity, and domain validation continue to use their established messages and types.

Optional metadata fields are read through the same JSON context boundary. Omitted `name`,
`version`, or `format` fields remain valid for backward compatibility; present object or array
values are rejected with `metadata.json` context while retaining the original JSON accessor
message. Typed serializers provide equivalent shape rejection for manifest, contents, and
learning-item documents.

## JSON persistence read-integrity boundary

All JSON stores read through `JsonFileReader`. A missing target file represents first-use
initialization and returns the store's empty value. Once a target file exists, blank content is
treated as corruption rather than empty state. Decode failures expose a stable structured kind:
`BLANK`, `MALFORMED`, `TRUNCATED`, or `INVALID_SHAPE`.

Persisted text is opened as a byte stream and decoded by a charset-aware buffered reader shared by
Desktop and Android. Do not use Java convenience methods such as `Files.readString` or
`Files.writeString` in Android-packaged production source: they can compile against the configured
JDK/compile SDK while remaining absent from the device ART runtime at min SDK 26.

`InvalidJsonPersistenceException` preserves its established message and the original
serialization cause while adding record-type and file-path context. Classification never
includes persisted content in its diagnostic message. Reads remain observational: this
boundary does not rewrite, delete, quarantine, or recover the source file.

The non-destructive contract is restart-stable: recreating a store and reading the same corrupt
target yields the same structured diagnosis while preserving the target bytes, modification
time, and directory contents. Automatic quarantine or restoration remains outside this shared
read boundary because no approved recovery source exists.

## JSON snapshot replacement boundary

`JsonFileWriter` serializes before entering replacement, creates a unique temporary file in the
target directory, writes and forces the complete UTF-8 candidate, then requests an atomic
replace. A non-atomic replace is attempted only when the filesystem explicitly reports that
atomic move is unsupported. Other atomic-move I/O failures propagate without touching the
previous target.

The fallback is intentionally described as non-atomic: it improves filesystem compatibility
but cannot provide the same crash guarantee. If it fails, the fallback error remains primary
and the unsupported-atomic error is retained as suppressed context. In-process exits always
clean the operation's temporary candidate; process-crash artifacts have a separate contract.

Interrupted-process temporary artifacts are inert. JSON readers address only the canonical
store path; they never inspect or promote sibling `.tmp` files. A later writer creates its own
unique candidate and cleans only that candidate, so it cannot destroy forensic evidence or
mistake an incomplete stale file for valid state. Cleanup, quarantine, and restoration require
a future explicit recovery policy rather than filename inference.

`JsonFileTransactionRunner` snapshots managed targets as opaque bytes before invoking the
operation. Rollback therefore restores the exact prior representation, including a corrupt
snapshot, and never silently normalizes it to an empty or newly encoded store. The original
operation failure remains primary; rollback failures are suppressed. Recreated stores then
apply the same read-integrity diagnosis to the restored bytes.

Representative large-state verification crosses the same production store boundary rather
than a test-only codec. The deterministic fixture validates complete ordered round-trip and
store recreation without a timing threshold; performance claims require separate measured
evidence.

## Learning-key highlighting and Study audio shortcuts

```text
original example + supplied candidates
    → LearningKeyMatcher / NormalizedTextIndex
    → longest non-overlapping original-text ranges
    → semantic AnnotatedString style
```

Matching uses NFC, locale-independent case folding, canonical apostrophe/hyphen variants and
flexible whitespace. Every normalized unit retains its original UTF-16 start/end. Lexical
boundaries prevent inner-word false positives; semantic inflection is not guessed.

```text
persisted ShortcutRegistry → Study keyboard router
    → StudyShortcutAudioPaths → LearningContentAudioController
```

Legacy mappings retain existing commands and fill only missing actions. Vocabulary/example
actions call the existing loop authority; Vietnamese actions call one-shot playback; replay
remains a distinct one-shot primary-audio action.

## Realtime Study header statistics

```text
exact package/lesson/session content scope
    → StudyHeaderStatisticsQueryService
    → enabled LearningItems + persisted ReviewEvents + MemoryState.isDue(clock)
    → immutable StudyHeaderStatistics
    → StudyFacade / StudyViewModel
    → compact Study header
```

The header combines two explicit projections. Session progress uses the immutable session policy,
committed New/Review counters and exact remaining queue identities; configured targets remain
visible while effective workloads may be smaller. Package learned-state uses only the latest
persisted review event per eligible item, so Total equals Again + Hard + Good + Easy and excludes
unseen inventory. Due counts learned, non-suspended states via domain `isDue`, independently of
the session Review cap. The nearest future due instant drives a one-shot refresh. Successful
review, Undo, content and import boundaries re-query source state; failed mutations retain prior
data.

The Desktop presentation maps that immutable state into eight typed metric models before
Compose. `StudyStatisticsDashboard` consumes only LETheme typography, icon, color, spacing,
surface and border roles. It reuses `StudyViewportClass`: Standard/Wide renders one balanced
eight-segment row, while Compact renders two four-segment rows and omits subtitles. Fraction
parts and accessibility sentences are presentation concerns; statistics arithmetic remains in
the application projection.

Session admission classification is an immutable queue fact. The planner assigns every admitted
identity `NEW` or `REVIEW` before policy limiting, and the persisted queue carries that origin
through restart, review transaction, counters, Undo, next-item projection and header statistics.
New schema queues never reconstruct origin from mutable memory or event history. Schema-v1 queues
remain readable and use the prior state/history inference only as a compatibility fallback.

For a newly planned session, NEW candidate input order is deterministic per `SessionId` using a
stable SHA-256 seed over its UTF-8 value. This application-only ordering runs after strategy
placement and before diversity, balance, and `SessionPolicyLimiter`; it changes neither the
REVIEW subsequence nor any persisted queue, recovery, scheduler, or memory contract.

The Desktop resolves the latest persisted rating outside Compose and publishes an immutable
current-item review context. Only a `REVIEW` item with history may underline exactly one matching
Again/Hard/Good/Easy label; `NEW` and missing-history states show no indicator. Underline is
supplementary to the existing label, shortcut and accessibility description.

Learner-facing progress identity is `ContentId`; execution and scheduling identity remains
`LearningItemId`.

```text
LearningItem candidate
    → ContentLearningStateQueryService(learnerId, ContentId)
    → content-level NEW/REVIEW origin
    → persisted queue { LearningItemId, ContentId, origin }
    → content-level quota/counters/header/previous rating
    → Compose
```

`ContentLearningStateQueryService` batches sibling lookup and the learner's authoritative event
stream, then selects the last effective event in repository order for each Content. It does not
mutate or merge sibling memory states. Queue execution, review transactions, scheduler state,
learning mode and event persistence continue to use the exact LearningItem.

New and Review policy limits count unique Content identities. `StudySession.reviewedItemIds`
retains technical occurrence history while `reviewedContentIds` prevents learner-facing counter
duplication. Header Total and rating buckets aggregate the latest event once per Content. Queue
schema v3 persists item-to-content identity; v1/v2 remain readable and application queries repair
their learner-facing projection without resetting the session.
## Part-of-Speech Semantic Registry

POS classification is an application presentation-support boundary, not a property of review,
scheduler, package schema, or persisted learning state.

```text
ContentRepository / successful imported content
        ↓
PartOfSpeechExtractor + PartOfSpeechNormalizer
        ↓
PartOfSpeechSemanticRegistry
        ↓
Desktop design-system POS adapter
        ↓
LETheme POS palette tokens
        ↓
StudyPosBadge
```

`LearningApplicationFactory` performs one idempotent startup reconciliation and shares the same
registry with import services and Desktop. `PackageImportService` registers parsed content only
after its established transaction succeeds. Neither operation modifies imported `Content`,
package payloads, review/scheduler records, or learning progress.

Known canonical categories have fixed semantic keys. Unknown categories preserve normalized text
and derive stable keys from locale-independent UTF-8 SHA-256, with bounded visual-slot probing
for active collisions. The visual palette is finite; canonical badge text and border remain
mandatory accessibility channels. Theme code owns colors only and does not depend on application
or domain packages.
## Study presentation and workload authorities

Study visual emphasis is a Desktop-only projection owned by `StudyVisualFocusResolver`. It maps
stable presentation roles to existing LETheme surface, border, elevation, and content-tone tokens.
Question/Answer are the primary reading anchor, Meaning is secondary-primary, Rating is the action
anchor, and Example/Scheduler/header/metadata are supporting layers. This contract does not own
layout order, typography scale, spacing, image sizing, keyboard/focus/accessibility, motion, rating,
Scheduler/FSRS, review, session, queue, domain, or persistence behavior. Rating actions retain their
semantic identity; Scheduler feedback remains explanatory and must not mimic a selected action.

Study reveal micro-interaction is a separate Desktop-only presentation projection owned by
`StudyMicroInteractionResolver`. It derives bounded Answer, Meaning, and Scheduler opacity plus a
non-layout translation fraction from transient animation progress. `LEMotionTokens` remains timing
and easing authority. Rating-action interaction motion is opt-in at `LEButton`; semantic colors,
focus semantics, action dispatch, and the rating-confirmation lifecycle remain independently owned.

Post-review session continuity is a transient Desktop presentation projection. After the application
transaction returns, `StudyViewModel` combines the existing unique rating token, immutable source and
destination item identities, structured committed rating, and scheduler feedback into one non-persisted
sequence. Compose advances confirmation, consequence, and destination-arrival phases with LETheme motion
tokens through token-safe callbacks. This projection never delays or replaces review persistence,
`ReviewWorkspaceState`, `StudySession`, queue selection, Scheduler/FSRS, Undo, restart, focus, scroll,
audio, Typing, completion, replay, Continue, or Continuous Review authority.

`StudyVisualLayoutResolver` resolves immutable viewport width/height, density, and font scale.
Session plans separate configured Content targets, effective unique-Content workloads, and
ordered technical LearningItem experiences.
## PLE-030.6 height and Introduction boundaries

Study height adaptation follows `StudyDisplayEnvironment → StudyVisualLayoutResolver →
StudyHeightMode/vertical budget`. The resolver is the only viewport authority. Header/dashboard
and dock/footer are fixed or reserved; the center pane is the only vertical scroll fallback.

First-content exposure follows persisted `StudySession.introducedContentIds` keyed by
`ContentId` → Desktop Introduction projection → one-shot meaning audio through the existing
controller → existing experience/reveal/rating flow. Exposure persistence is distinct from a
completed review and cannot increment learner progress or scheduler state.
## PLE-030.7 Introduction-to-Answer transition

For a first Content exposure whose flow has one non-input presentation experience, Next owns
one application operation: persist the ContentId and `answerRevealed` in the same `StudySession`
save, then project Full Answer/Rating Ready. Primary Typing and additional experience stages
remain planner-owned and cannot be bypassed. UI code neither simulates multiple actions nor
stores reveal authority locally.

Compact Study chrome remains downstream of
`StudyDisplayEnvironment → StudyVisualLayoutResolver → StudyVisualLayout`. Shared button
density supplies pointer minimums and stable focus borders; fixed header/dock/footer reserves
bound the single scrollable center pane.
## PLE-030.9 session-goal and review-memory authorities

Every new Desktop session obtains its `SessionPolicy` from a fresh read of the persisted
`DesktopRuntimeConfiguration`; the resulting immutable policy is the single source for queue
admission, configured targets, and header statistics for that session. An active session keeps
its original policy when Settings change. On each Study entry, the active session's explicit
`(newItemLimit, reviewItemLimit)` fingerprint is compared with a fresh persisted policy. A
matching fingerprint resumes the session. A mismatch finishes the stale session and creates a
replacement in the same package/topic/lesson scope; the replacement starts with zero counters
and its own immutable policy. Runtime settings outside those two limits do not participate in
the fingerprint: theme, typography, audio delay, shortcut, and presentation configuration
cannot reset an active session.

Pre-answer learner-facing classification remains Content-based:
`SessionItemOrigin` plus `LearningEngine.getContentLearningState(ContentId)` produce
`CurrentStudyItemReviewContext`. NEW renders no rating memory. REVIEW renders one non-interactive
footer memory status for every planned experience regardless of reveal-action availability,
while Rating Ready replaces that footer with the existing interactive action dock. Scheduler
execution identity remains `LearningItemId`; MemoryState, ReviewEvent, and learning-experience
execution also remain LearningItem-owned. Content owns NEW/REVIEW presentation, Total, previous
rating, and learner-facing counters.

Pre-answer image bounds are resolved from the display environment and remain image-first at 90%
of resolved content width. Full Answer reuses the width authority but receives its height from
the measured fit boundary below. `ContentScale.Fit` preserves aspect ratio without crop, and the
fixed dock remains outside the single scrollable center pane.

Full Answer keeps height-aware presentation density for gaps and padding, distinct from
pre-answer image-first sizing. Resolver inputs are recomposed from `BoxWithConstraints`, never
monitor identity or named resolutions; measured content geometry below is the fit authority.

The final Full Answer fit authority is measured rather than estimated. `StudyScreen` exposes
the actual weighted body height after fixed shell surfaces; `FullAnswerFitLayout` subcomposes
and measures identity, meaning, and the first bilingual example before measuring the image with
the exact remainder. Scheduler Feedback, details, and additional examples form scroll
continuation and never reduce the required-fit image budget. The required-fit region is Word,
IPA/pronunciation/POS, Image, Meaning/Translation, the first English example, the first
Vietnamese example, and Rating Dock. Estimated fixed budgets are not a fit authority.
The general visual resolver no longer claims that measured content fits.

The Study main-body scroll state is presentation-local and shared across Question and Answer
composition. Typing Question gives input visibility to its frame-synchronized
`BringIntoViewRequester`; activation of the actual Typing Answer side instead waits for its
layout frame and immediately resets that same scroll state to the top. The reset identity is the
current item plus answer-side phase, so unrelated recomposition, audio/timer updates, resize, and
manual Answer scrolling do not create a second reset or mutate a later item's viewport.

Session header progress uses persisted queue workload plus immutable session counters.
`StudySessionProgressSource` carries effective New/Review workloads from `StudyQueueProgress`;
Review current progress is the committed `reviewItemsReviewed` count. Remaining queue identities
remain diagnostic data, not per-action counter authority.

Study Statistics density is resolved from the dashboard's actual usable width, not the outer
Study viewport class. Standard density renders all eight metrics in one row. Compact-inline
density renders two ordered rows of four with reduced surface/row/metric padding, smaller icons,
and a dedicated readable value token while preserving denominators and merged accessibility.
The Compose-measured header remains authoritative for the weighted Learning Content body;
`statisticsDashboardReservedHeightDp` is only a legacy pre-answer estimate aligned to compact
density and is not a Full Answer fit input.

Full Answer Identity has a separate width authority from the outer viewport classification.
`VocabularyIdentitySurface` measures its own usable card width and resolves
`StudyIdentityPresentation`: standard inline at 480dp or wider, compact inline from 240dp, and
stacked only below 240dp. Compact inline reduces metadata gap, card padding, speaker/icon size,
IPA size, and POS padding without reducing the word typography. The resulting measured Identity
height automatically returns vertical budget to `FullAnswerFitLayout`; pre-answer presentation
and the measured fit algorithm remain unchanged.

Study Chrome has its own semantic width authority. Each top-action and status-strip surface
resolves `StudyChromePresentation` from its actual `BoxWithConstraints` usable width rather
than the window class. Standard width retains text actions and labeled shortcuts. Compact and
minimum widths use fixed-square Undo/Pause icon actions, bounded 32dp/28dp shortcut strips,
short semantic chord tokens, and an icon-only session state. Shortcut projection remains a
list of command/chord/accessibility/priority values; Compose never parses a long display
string. The fixed StatusStrip remains after the Rating Dock and outside the scroll body.

Responsive Study composition reduces density before allowing wrap: Statistics, Identity, and
Study Chrome compact before the layout sacrifices Learning Content.

The fixed bottom surface is a Quick Action Toolbar, not a keyboard-hint strip. At Rating Ready
it maps semantic shortcut commands directly to four equal colored rating circles, an outlined
Replay circle, a Material Undo action, and a success session indicator. Audio action commands
retain stable semantic icons while their current configurable chords appear as secondary live
cues. The chrome authority continues to own fixed height, gap, padding, and minimum-width item
priority.

PLE-031 extends the toolbar without introducing a second shortcut authority. Settings persists
`DesktopRuntimeConfiguration.studyShortcuts`; the same live configuration object reaches
`StudyScreen`, where `StudyShortcutStatusPresentation` and `resolveStudyKeyboardAction` consume
the identical `ShortcutRegistry` snapshot. `ShortcutChordFormatter` is shared by Settings and
Study, retaining complex modifiers while compacting Shift to `⇧`. Four audio commands project
stable icon/chord actions whose enabled state comes from `StudyShortcutAudioPaths`. Minimum
width keeps rating actions, an available audio action, Undo, and Session while the remaining
commands stay actionable and fully described in a non-wrapping overflow menu. Shortcut changes
do not participate in `StudySessionGoalFingerprint`.

PLE-031.1 separates toolbar action identity from shortcut metadata.
`resolveStudyToolbarActionIcon(StudyShortcutCommand)` is the sole semantic icon authority:
vocabulary loop, example loop, Vietnamese meaning, and Vietnamese example have stable distinct
vector identities; Vietnamese actions add `VI`/`VI+` composition badges.

PLE-031.2 composes registry-owned chord metadata beside, but never instead of, that semantic
identity. `StudyShortcutStatusItem.chordText` supplies full tooltip/accessibility text and
`compactLabel` supplies Compact/Minimum visual cues; both originate from
`ShortcutChordFormatter`. Standard, Compact, and Minimum stay single-row within the existing
36/32/28dp strip heights. Change/Reset can update chord cues immediately without altering icons
or participating in session, queue, counter, scheduler, or policy identity.

## AURORA-009 Study surface presentation boundary

`StudySurfacePresentationResolver` is the pure Desktop authority for semantic surface role, layer,
border prominence, resting elevation, and relative hierarchy across discovery and understanding.
Compose surfaces consume that projection; `StudyVisualLayout`, image sizing/`ContentScale.Fit`,
typography, motion, interaction/accessibility, audio, rating, Scheduler/FSRS, session, application,
domain, and persistence authorities remain unchanged.

`UnifiedStudyStageResolver` adds the AURORA-010 composition contract over those roles. It owns only
ordered grouping, integrated content membership, the separate action role, and the flat shared-stage
surface. Wrapper-only content surfaces may be flattened; image/word interaction seams and Rating Dock
remain explicit. No layout, responsive, learning, interaction, or persistence authority moves.

## EPIC-001 Focused Immersion presentation boundary

`StudyCanvasPresentationResolver`, `StudyHeroPresentationResolver`,
`StudyContentRhythmPresentationResolver`, and `StudyDecisionAreaPresentationResolver` form the
pure, deterministic Desktop presentation foundation for Learning Engine Focused Immersion. They
separate workspace, stage, hero, understanding, and decision depth without becoming a second
content, interaction, or learning model. Compose retains the existing responsive and image-sizing
authorities while consuming semantic roles, tone, shape, elevation, and bounded stage width.

`StudyImmersionMotionResolver` projects transform values only. Compose owns an item-identity-keyed
arrival animation through `LEMotionTokens`; reveal, continuity, rating confirmation, UX-001 source
consequence sequencing, and completion keep their existing owners. Scheduler/FSRS, LQ-002, rating,
review, queue, session, persistence, Typing evaluation, audio, image source/fit, keyboard, focus, and
accessibility boundaries do not move.

### EPIC-001R approved scene composition

`ApprovedStudyWorkspacePresentationResolver` records the approved Discovery and Answer region
orders without becoming content or workflow authority. `DiscoveryFrontSurface` owns lexical hero,
contextual image, and Meaning composition; `FocusedAnswerSurface` owns confirmation, lexical hero,
image, continuous Meaning/Examples, and scheduler composition; `StudyScreen` retains Typing and the
Decision Area. Existing callbacks and state continue through their established owners. The
resolver introduces no parallel learning model and does not alter Scheduler/FSRS, LQ-002,
StudyFacade, review transaction, queue/session, persistence, media, focus, keyboard, or
accessibility boundaries.

## PLE-033-B2 Completed-Session Replay Boundary

`ReplayCompletedStudySessionUseCase` uses the finished predecessor's persisted
`StudySession.reviewedItemIds` for committed membership and its retained `StudyQueueSnapshot` for
ordering. It creates a new ordinary Session and queue with a deterministic replay-purpose
identity. Existing review, Scheduler, completion, and Undo boundaries remain authoritative; the
workflow does not invoke Planner, scan history by time, or add replay persistence/schema.

## PLE-033-B3 Learn-entry review boundary

Learn-entry availability is an Application projection over learner/package/topic scope,
finished Study Sessions, retained queues, enabled LearningItems, reviewed MemoryState, and
committed ReviewEvent evidence. Desktop renders availability and delegates selected actions; it
does not scan repositories.

Learned-content review creates an ordinary review-only StudySession and queue. Current reviewed
MemoryState is the primary learned authority, committed ReviewEvent history is the durable
fallback, and Content identity prevents learner-facing duplicate progress. Due items precede
non-due items, then oldest review time and stable LearningItem identity order candidates.
The existing Review Session policy caps admission; ordinary review transaction, Scheduler,
statistics, completion, and Undo remain authoritative.
# Exact session workload and lapse ancestry

Normal Study session policy limits count Content identities. Planning preserves the established candidate ordering and selects the first deterministic LearningItem representative for each Content; siblings remain domain inventory for later sessions. Initial queue workload equals the number of representatives. Rating-driven reinsertion is a later queue occurrence and may increase the effective denominator only after commit.

Same-session lapse ancestry belongs to `StudySession`, not UI state or permanent memory. A committed Again adds the Content identity, Undo restores its before-state, and persistence supports restart. This fact constrains automatic Typing rating only; Scheduler/FSRS and manual rating authority are unchanged.
LQ-005C adds a read-only Shared Application transparency boundary after the existing evidence,
difficulty, recommendation, and promotion authorities. `GetLearningInsightUseCase` reads the
Content-owned trajectory and `LearningInsightProjector` emits a deterministic typed projection;
Desktop only localizes and renders it. The durable contract is documented in
[`LEARNING_INSIGHT.md`](LEARNING_INSIGHT.md).
LQ-006A establishes the platform-neutral recall boundary under Shared Domain/Application:
Content capabilities and typed seed/clock inputs produce an immutable `RecallPlan`; clients render
and return a sealed `RecallSubmission`; shared validation/evaluation produces a passive
`RecallResult`. Stable wire IDs, schema versioning, deterministic DTO ordering, and portable media
identifiers are defined in [`RECALL_CONTRACT.md`](RECALL_CONTRACT.md). Desktop remains a reference
client and owns no recall intelligence.
LQ-006B adds a pure Content-to-recall-capability projection after the LQ-006A contracts. The resolver
reuses existing Content text/media and recall enums, produces deterministic per-mode directions and
typed unavailable reasons, and never reads learner/session/evidence/scheduler state. Its portable
boundary is documented in [`CONTENT_RECALL_CAPABILITIES.md`](CONTENT_RECALL_CAPABILITIES.md).
# LQ-007A Desktop Recall Boundary

Desktop Typing consumes a Shared Core `RecallPlan`, submits a `RecallSubmission` through
`LearningEngine.executeRecall`, and sends the resulting `RecallResult` through
`LearningEngine.executeRecallLearning`. The engine composition root owns the plan factory,
execution engine, learning bridge, and existing transaction use cases. Desktop retains only
presentation and interaction concerns; see `DESKTOP_RECALL_PIPELINE.md`.
# LQ-007B Coverage Reinforcement Authority

Unique Coverage Review stores immutable, session-local reinforcement history in
`StudyQueueSnapshot`. A typed `CoverageReinforcementPolicy` owns graduated gaps, rating-specific
limits, and near-end defer decisions; queue mutation only applies its decision. Schema-v6 queue
persistence and the typed Undo checkpoint preserve deterministic recovery without wall-clock,
Scheduler, Evidence, Practice, or platform dependencies. See `COVERAGE_REINFORCEMENT_SPACING.md`.
# Android peer-client boundary

The `:android` application is a peer of `:desktop`; neither client depends on the other. Android
owns Compose, navigation, lifecycle, UI state restoration, and Android platform paths. Both clients
call the same root Application/Domain engine. Production recall planning and all correctness,
rating, transaction, Scheduler/FSRS, Evidence, Practice, and Queue authority remain shared.
