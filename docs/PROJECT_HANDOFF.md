# Learning Engine 2.0 — Strategic Project Handoff

This document is the concise durable handoff for product and architecture continuity. Standing
AI workflow rules live only in [`../AGENTS.md`](../AGENTS.md).

## Product Vision

Build an adaptive learning platform—not merely a flashcard application. Flashcard-based spaced
repetition is the first capability, grounded in retrieval practice and scheduling science, while
the architecture must support richer evidence-backed learning forms over time. The first
release target remains Desktop; mobile and Web remain deferred until shared engine contracts
and Desktop behavior are stable.

**North Star:** Every design decision must measurably improve the learner's ability to learn,
remember, and stay motivated.

Product decisions prioritize learner outcomes and learning science, keep domain behavior
independent from UI, adapt rather than remain static, reveal complexity progressively, use
evidence, let AI augment rather than replace people, preserve long-term maintainability and
safe migration/rollback, and treat delight as part of product quality.

## Product Phase

The functional import-to-persisted-study flow and its robustness/runtime/UX foundations are
complete at their verified boundaries. **Phase 5 — Desktop Beta Readiness** is implementation
complete but still awaits external clean-machine verification. **Phase 6 — Learning
Experience** is implementation complete through P6-10; representative manual verification is
still pending.

Desktop 1.0 continuation is now bounded only by Phase 6 manual verification, Phase 7
manual/real-user validation, and the still-open external evidence from Phase 5. The
repository—not chat history—is sufficient to resume this work.

Android product behavior is now catalogued as a reference for post-1.0 Desktop evolution. It
does not supersede Learning Engine scheduling, queue, session, persistence, recovery, or undo.
The accepted direction is **Desktop Better Than Android**, documented in
[`PRODUCT_VISION.md`](PRODUCT_VISION.md).

The platform-independent learner contract is now defined under [`spec/`](spec/). It describes
the complete Learning Session, Study Workspace, media, topic hierarchy, interaction semantics,
and product behavior without assigning scheduler or persistence ownership to any client.

The first Desktop Learning Experience Alpha applies that contract to the existing active-session
screen without changing engine behavior: active study suppresses distracting shell chrome,
centers and enlarges semantic learning content, retains compact progress/Undo/Pause, and provides
cancellable role-labelled MP3 playback with keyboard replay. Automated evidence reaches the real
decoded-PCM output boundary; physical audio and final visual acceptance remain Product Owner UAT
in [`DESKTOP_LEARNING_EXPERIENCE_ALPHA_UAT.md`](DESKTOP_LEARNING_EXPERIENCE_ALPHA_UAT.md).

Session-aware Experience Rotation activates the selection framework without changing review
authority. Root Application policy retains full Image/Listening/Prompt/Typing eligibility; the
automatic profile retains passive Image/Listening/Prompt options and the selection engine uses
zero-based stable queue position. Desktop offers explicit Typing and restores the same automatic
selection when returning to Default. Reveal, retry, pause/resume, and rendering retain context;
undo follows the rewound position. No rotation field or typing state is persisted. Scheduler,
import, persistence, and package contracts remain unchanged.

Desktop Question presentation derives visibility and audio permission from the current
experience selection rather than a global Adaptive show-all baseline. Reveal switches to
complete-content and complete-media truth mode: Question preferences cannot hide Answer fields
or audio interactions, and only a live Reveal transition may autoplay primary English once.
Learning-stage presentation reads the effective `MemoryState.stage`; absence of a persisted
record is tracked separately for diagnostics and is not a second definition of NEW.

Learning Flow Engine Foundation builds on that rotation with an immutable platform-neutral
planner/controller and a real Desktop multi-stage slice. Each item receives rotated primary,
optional eligible Typing, authoritative reveal, then manual rating-ready. Desktop ViewModel owns
transient state; `StudyFacade` still owns reveal/review. Same-runtime pause preserves the stage;
restart reconstructs rather than persists it.

Learning Objectives, Strategies, and Flow Templates Foundation separates Product Brain from
Flow execution. Objective policy chooses durable recall; strategy derives strategy behavior
(`includeOptionalTyping`); `LearningFlowTemplateFactory` translates strategy into reusable template slots
(`ROTATED_PRIMARY`, `OPTIONAL_TYPING`, `ANSWER_REVEAL`, `RATING_READY`) without `LearningExperiencePlan` dependency.
`ProductBrainPlanner` is the single application orchestration boundary; `LearningFlowInstantiationService` resolves
runtime selections for slots and delegates to `LearningFlowPlanner`. Desktop depends only on `ProductBrainPlanner`
Desktop Alpha-01 Session Bootstrap implements Product Brain session bootstrap capability.
When the user selects a Topic and triggers session start, Product Brain evaluates learner context, resolves `LearningSessionContext`,
formulates `TeachingGoal`, initializes `SessionTimeline`, generates `InitialDecisionSnapshot`, projects `SessionOverview`,
and enables `StartLearning` in Desktop UI.
Desktop Alpha-02 Scene Execution implements Product Brain single-scene execution capability.
When the learner presses Start Learning, Product Brain selects the first scene (`TypingRecallScene`), renders the scene,
collects learner input, evaluates `SceneResult`, converts `SceneResult` into `LearningEvidence`, and returns `LearningEvidence` to Product Brain.
Desktop Alpha-03 Adaptive Decision implements Product Brain adaptive teaching capability.
After receiving every `LearningEvidence`, `InstructionalDecisionEngine` generates an `AdaptiveDecision` (`INCREASE_DIFFICULTY`,
`DECREASE_DIFFICULTY`, `REPEAT_SIMILAR_SCENE`, `MAINTAIN_PACE`), produces a `DecisionTrace`, updates the `SessionTimeline`,
and notifies Desktop UI of the adaptive decision.
Desktop Alpha Architecture Review evaluates Desktop Alpha-01 through Alpha-03 implementations in `docs/DESKTOP_ALPHA_ARCHITECTURE_REVIEW.md`.
Confirms a coherent platform-neutral closed adaptive teaching loop, zero UI instructional logic, 100% test pass rate (1,643 tests), stable application contracts, and issues GO recommendations for Alpha-03.5 and Alpha-04.
Desktop Alpha-03.5 Decision Explainability implements Product Brain learner-facing decision explanation capability.
Product Brain generates a `DecisionExplanation` (observation, decision summary, pedagogical reason, next step) for every adaptive decision without leaking internal rule IDs. Desktop preserves the explanation through the Facade and ViewModel, renders it in the Study screen, and lets the learner hide and show the same explanation without losing state.
Desktop Alpha-04 Session Completion closes the first Product Brain session loop. Product Brain owns completion planning and learner-facing reflection/summary generation; the existing review transaction remains the only scheduler and review-persistence boundary. Finished sessions may persist an optional learner-facing completion snapshot through the existing session repository, allowing Desktop to recover the outcome after restart and present learning, reinforcement, next-step, and scheduling guidance without instructional rules in Compose.

Beta-L01 establishes durable installed-topic identity and learner-topic resume. `TopicId` is
persisted with `ContentPackage`, remains distinct from version-sensitive `PackageId`, and is
carried across compatible replacement. Existing package records derive a deterministic legacy
identity from logical package name and format; no filesystem path, display order, or per-import
random value is used. `StudySession` stores an optional topic reference, so the existing
session/queue persistence provides checkpoints keyed effectively by `(LearnerId, TopicId)`.
Item-level `MemoryState`, review history and scheduler state remain authoritative and are not
copied into topic records. Desktop switching delegates topic resolution and recovery to
Application and clears only transient presentation state.







## Architecture Overview

Learning Engine uses Kotlin/JVM 21, Gradle, kotlinx.serialization, and two modules:

- Root: domain, application ports/workflows, infrastructure, persistence/import adapters, JVM
  entry points, and tests.
- `desktop`: Compose Desktop presentation, accessibility, navigation, and composition wiring;
  it depends on the root module.

Dependency direction is Desktop/JVM adapters → application ports/use cases → domain. Concrete
infrastructure implements application ports. Durable technical decisions live in
[`ARCHITECTURE.md`](ARCHITECTURE.md).

## Domain Overview

- Content, structured text, media, libraries, collections, packages, catalogs, dependencies,
  validation, import, upgrade, and uninstall.
- Learning items, lesson-scoped selection, study queues/policies, and study-session lifecycle.
- Memory state, review events, ratings, learning stages, forgetting behavior, and FSRS
  scheduling.
- Progress, review history, dashboard, statistics, analytics, search, and Desktop presentation.

## Current Roadmap

Phases 1–4 are complete. Phase 5 retains its external verification gate. Phase 6 implementation
is complete; Phase 7 owns Beta validation/Desktop v1. After those gates, the evidence-backed
Desktop product sequence starts with hierarchical learning scope, then session entry/setup and
the focused workspace before semantic media, multi-lesson, Listening, and Typed Recall.
Additional platforms remain deferred. See [`ROADMAP.md`](ROADMAP.md) and
[`DESKTOP_PRODUCT_ROADMAP.md`](DESKTOP_PRODUCT_ROADMAP.md).

## Completed Milestones

- Learning engine and persistence foundations.
- Desktop end-to-end learning flow.
- Desktop UX, keyboard, and accessibility hardening at the Beta-test boundary.
- Search and discovery through Unicode-robust multi-term matching.
- Package Import & OPD3 Robustness.
- Persistence Integrity & Recovery.
- Workflow Foundation Refinement (this documentation increment).
- Desktop Runtime Foundation.
- Desktop UX Foundation.

Official completion records belong in [`MILESTONE_HISTORY.md`](MILESTONE_HISTORY.md); detailed
capability history belongs in [`CHANGELOG.md`](CHANGELOG.md).

## Current Phase

Phase 5 remains open only for Product Owner clean-machine/install/upgrade/signing evidence.
Phase 6 implementation is complete. Phase 7 is at the manual/external validation gate without
erasing the independent Phase 5 distribution evidence gate.

## Current Capability

- **PLE-020 — Content Studio Desktop UX Polish & Layout Remediation** complete (commits `d930950`, `48ceda6`, `8c13557`, `0105315`, `67b948a`):
  - Native Drag & Drop media support for Image Card and Audio slots.
  - Final 4-row Desktop Layout: Question/Answer (Row 1), 50/50 compact IPA/POS row (Row 2), 50/50 paired Example/Translation row (Row 3), StudioHeroImage (Row 4).
  - Adaptive field collapsing (`+ Add IPA`, `+ Add Example`, `+ Add Translation`) with strict dirty safety (`isDirty == false` on reveal).
  - Full-resolution Skia `StudioHeroImage` with zoom (50%-250%), Fit Width, Fit Height, and Fullscreen preview.
  - Symmetrical ~56dp card height for IPA and POS using `CompactMetadataFieldCard` (`BasicTextField`).
  - Responsive `BoxWithConstraints` layout (wide desktop 2-column, narrow window vertical stack).
  - Automated verification: `.\gradlew.bat clean test` — BUILD SUCCESSFUL (614 passed, 0 failed).

- **Next Capability: PLE-021 — Modern Learning Workspace**:
  - **Objective:** Expose and polish existing Learning Flow, session, typing, audio, reveal, rating, resume, and FSRS capabilities through a modern, keyboard-first Desktop learning experience suitable for daily use.
  - **Baseline Authority:** Existing Learning Flow architecture (`StudySession`, `StudyQueue`, FSRS, reveal/rating, `ProductBrainPlanner`, typing recall, audio) is the baseline authority and MUST NOT be duplicated, bypassed, or redesigned.
  - **Target:** First usable learning experience in 3–5 days; polished and stable completion in 5–7 days.
  - **Scope:** Desktop presentation, interaction, integration, and UAT capability — not a new learning algorithm initiative.

Learning Objectives + Learning Strategies + Flow Templates Foundation is the current capability.
The Architecture Gate removed experience-policy and sequence decisions from
`LearningFlowPlanner`; Product Brain now ends at an immutable template and Flow begins at
template instantiation.

Learning Flow Engine Foundation + Desktop Multi-stage Learning Vertical Slice is the preceding
implemented capability. Shared flow definition/planner/controller/progress are platform-neutral;
Desktop uses one identity-keyed coordinator and localized stage UI. The former Default/Typing
chooser was removed because the flow definition is now authoritative; Typing remains represented
as an eligible planned `USER_CHOICE` stage.

Session-aware Experience Rotation Foundation is the preceding implemented capability. Shared
Application derives an immutable session/item ordinal from queue progress, projects passive
automatic options, and keeps Typing explicit. First-item behavior remains ordinal zero;
same-item presentation is stable; undo/restart reconstruct from authoritative session/queue
state without a new persisted field.
Its verified local gate passed 1,619 tests plus Desktop compilation and Temurin 21 app-image
creation.

Typing Recall Vertical Slice Foundation is the preceding implemented capability. Shared Application
owns typing eligibility, semantic expected-answer extraction, conservative normalization, and
evaluation result semantics. Desktop owns the explicit chooser, transient input/focus, submit,
localized accessible feedback, and reveal integration. Scheduler/review continues to own the
unchanged manual rating and scheduling outcome. There is no fuzzy matching, synonyms, AI,
automatic rating, persisted typing history/preference, or adaptive selection.

The preceding Platform-Independent Learning Product Specification capability defines the ideal journey
from Start/Resume through scope, setup, thinking, optional help/media, reveal, rating,
completion, summary, and interruption recovery. It supersedes subsystem-first roadmap ordering
with outcome-based `LX-01` through `LX-11`, while retaining current Desktop 1.0 external gates.

The Android Product Reverse Engineering & Desktop Product Architecture documentation
capability establishes a behavior matrix, Desktop gap analysis, subsystem architecture,
product vision, technical-debt register, and post-1.0 roadmap. The Android Activity is a UX and
product reference; its SRS arrays, queue heuristics, mutable file persistence, lock-screen
controls, and God-object structure are not candidates for porting.

The real-data Desktop responsiveness blocker is remediated: long workflows publish immediate
busy/loading phases and execute off the UI thread; library/study N+1 JSON scans are eliminated;
lesson rows are lazy; search is debounced; and imported images have bounded lazy thumbnails.
Synthetic production-boundary evidence covers 2,425 contents and 12,125 learning items. The
Product Owner's 179 MB package remains the required manual acceptance input.

The real-user Desktop blocker for builder-produced `<topic>.json` + `<topic>.pkg` pairs is
resolved at package composition. `.pkg` is no longer assumed to be ZIP: signature routing uses
the existing OPD3 binary reader and legacy JSON importer, persists a queryable Content Library,
extracts local media, and exposes learning items to the normal session engine. Existing
ZIP/bundle and standalone `.opd3` paths remain supported.

Product Owner verification with the original 179 MB package remains external evidence; the
repository uses a tiny deterministic fixture matching the exact builder wire format.

**Desktop 1.0 release-candidate preparation** is complete. The final repository audit found and
fixed one recovery-integrity defect: a negative backup manifest file count could be interpreted
as an empty snapshot. Validation now rejects any negative or archive-mismatched declared count
before safety-backup creation or mutation.

The subsequent Windows launcher blocker is fixed: accessibility-enabled Windows profiles now
start through the generated jpackage executable because the bundled runtime explicitly includes
`jdk.accessibility`. A native app-image smoke task guards this boundary using isolated storage.

Automated release-path evidence crosses persisted OPD3 import and real Desktop composition, and
the local test/compile/package gates are complete. Product Owner manual, real-user, clean-machine,
installer installation, upgrade/uninstall, signing, and clean-machine evidence remains pending;
none is represented as passed.

## Desktop 1.0 Continuation

Completed Phase 6 / Package Learning Experience capabilities:

- P6-01 — Phase 6 Definition;
- P6-02 — Learning Session Lifecycle & Recovery Contract;
- P6-03 — Review Workspace State & Action Boundary;
- P6-04 — Learning Content Model;
- P6-05 — Rich Content Renderer;
- P6-06 — Progress, Completion & Learning Feedback.
- P6-07 — Pause, Resume, One-Step Undo & Safe Interruption.
- P6-08 — Desktop Accessibility, Keyboard Navigation, Error Recovery & Release Polish.
- P6-09 — Desktop End-to-End Verification, Defect Remediation & Release Evidence.
- P6-10 / PLE-013 — Topic Selection & Exact Resume (Active topic authority when Study is idle, topic switching without session/progress loss, exact checkpoint resume, MemoryState/ReviewEvent/scheduler preservation).
- Gradle Default Memory Configuration Stabilization (`gradle.properties` `org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8`).
- PLE-010 — Library Navigation Recovery.
- PLE-012 — Rich Lesson Exploration Workspace.
- Library Integrity Recovery — Final Ownership Remediation.

Desktop 1.0 continuation point:
- Phase 6 implementation complete through P6-10.
- Phase 7 capability **PLE-026: Adaptive Study Presentation** is COMPLETE (Desktop Manual UAT: PASS).
- PLE-030 adaptive Study statistics/layout/chrome remediation is FINAL PASS. PLE-031 live audio
  shortcut toolbar, PLE-031.1 semantic icons, and PLE-031.2 live chord cues are FINAL PASS by
  Product Owner Manual UAT.
- **PLE-032-B1 — Application Continuation Boundary** is implemented: manual general-Study
  continuation now delegates to an application use case that preserves Planner/ordinary Study
  authority and accepts at most one persisted next Session for sequential/restart-visible
  repeated requests. Full Continuous Review Mode remains incomplete.
- **PLE-033-B1 — Desktop UX Polish** preserves and applies Custom Review targets across preset
  use/restart, routes Answer-side Space through the existing Good action, clips the adaptive
  Answer bitmap with the design-system radius, and distinguishes configured New/Review targets
  with aligned purple metric styling. Planner, Scheduler, Session, Undo, and PLE-032 semantics
  are unchanged.
- **PLE-033-B2 — Post-Session Experience** replays the exact committed items from one completed
  Session through an application boundary, preserving retained queue order and ordinary rating,
  Scheduler, completion, and Undo ownership. Desktop exposes the guarded completion action,
  Good advertises `3`/`Space`, and New/Review targets use semantic green/blue tokens.
- **PLE-033-B3 — Learn Entry & Review Progress** changes Review fractions to committed
  completion, presents Good and Space as an intentional two-line control, and gives idle Learn a
  four-choice application-derived entry. Latest replay is learner/package/topic scoped.
  Review-all uses durable reviewed MemoryState or committed ReviewEvent evidence, enabled
  Content-scoped identities, deterministic due/oldest ordering, and the configured Review cap to
  create an ordinary review-only Session.
- **PLE-034 — Learning Hub MVP** makes the same four application-backed learning actions visible
  on both idle Learn and Session Completion. Completion summary remains intact; a shared Desktop
  semantic model/dispatcher exposes Continue, latest scoped replay, review-all, and Library
  navigation without duplicating queue selection or session business logic.
- **PLE-034-B2 — Learn Hub Access & Unique Review Coverage** makes Learn/F2 an explicit chooser
  even with an active Session. Continue resumes it; choosing Replay or Review All leaves the old
  practice source through Application before creating one replacement queue. Review All measures
  committed unique `ContentId` coverage while deterministic Again/Hard retries remain
  attempt-based, fair to unseen Content, Undo-safe, and bounded by completion at full coverage.
- **PLE-034-B3 — Review All Scope & Settings Preset Fix** separates Review All from ordinary
  Session limits: its Application-owned snapshot includes every eligible unique learned Content
  in scope. Settings preset/custom presentation and persistence now share the same effective
  numeric New/Review values; General Study continues to use those ordinary limits.
- **PLE-035-B1 — Typing Live Diff & Reveal Comparison** adds Unicode code-point Typing
  differences, non-revealing live error feedback, editable incorrect checks, and a localized
  comparison after the existing authoritative Reveal transition. Full Answer and ordinary manual
  rating remain unchanged.
- The next Study capability is **PLE-032-B2 — durable Continuous Review intent and restart
  continuation**. B1 intentionally does not add Continuous Review persistence or automatic
  continuation.
- A new chat session does not need earlier debug history. Reading `AGENTS.md`,
  `PROJECT_HANDOFF.md`, `AI_ARCHITECT_CONTEXT.md`, `ROADMAP.md`, `CHANGELOG.md`, and the clean
  codebase is sufficient to continue with PLE-032-B2.

Remaining before Desktop 1.0:

- Phase 7 manual/real-user validation beyond the passed Study capabilities, external release
  evidence, and Desktop 1.0 approval;
- Product Owner clean-machine install/launch/upgrade/uninstall/reinstall and signing evidence retained from Phase 5.

## Stable Desktop 1.0 Boundaries

Unless a concrete defect or accepted use case proves otherwise, Desktop 1.0 treats the Learning
Session lifecycle, Review Workspace state/actions, Learning Content, rich-content renderer, and
session progress/completion contracts as stable. A change must identify the defect/use case,
assess compatibility, add focused and regression coverage, and update the owning architecture
documentation.

Authoritative ownership remains:

- Domain: `StudySession` lifecycle, current item, reveal state, and pending review intent.
- Application: orchestration, atomic review transaction, scheduler interaction, queue/session
  progress projection, and recovery.
- Desktop: workspace projection, rendering, temporary feedback, focus, and presentation state.

Desktop must not own scheduling, durable lifecycle, persistence transactions, or durable
progress counts.

## Phase Definition of Done

Phase 5 retains the external checklist in [`BETA_RELEASE_CHECKLIST.md`](BETA_RELEASE_CHECKLIST.md).
Phase 6 outcomes, sequence, open decisions, and exit criteria are owned by
[`ROADMAP.md`](ROADMAP.md#phase-6--learning-experience). Every capability must also satisfy
[`../AGENTS.md`](../AGENTS.md).

Phase 8 product behavior and acceptance boundaries are owned by [`spec/`](spec/) and the
capability sequence in [`DESKTOP_PRODUCT_ROADMAP.md`](DESKTOP_PRODUCT_ROADMAP.md).

## Technical Debt

- No crash journal exists for multi-file JSON transactions.
- Non-atomic replacement fallback has weaker crash guarantees on unsupported filesystems.
- Recovery is manual and local only; cloud, scheduling, cross-device merge, and automatic
  retention remain intentionally unsupported.
- Stale JSON temporary artifacts are intentionally inert and may accumulate after crashes.
- Persistence supports legacy arrays and envelope v1 but no general migration framework.
- Multi-file transaction snapshots allocate complete managed files in memory.
- Clean-machine smoke evidence remains incomplete; distributables are locally buildable but not
  yet signed or clean-machine verified.
- Large real-package and UI-allocation evidence remains measurement-driven follow-up work.
- Desktop retains compatibility boolean/string projections while consumers migrate to explicit
  workspace, content, and progress contracts.
- Core learning actions and accessibility labels are localized; legacy explanatory/metric copy
  still needs broader product-copy localization after Desktop 1.0.
- Java Sound codec availability varies; guaranteed MP3 playback is not a Desktop 1.0 promise.
- Compose does not yet have a stable UI-test harness for every visual behavior.
- Legacy sessions without a persisted queue have an unknown progress denominator.
- Individual queue-skip reasons are not persisted.
- Desktop 1.0 still requires representative real-user/manual verification.
- Generic audio references and view-local Java Sound playback cannot yet express bilingual or
  voice roles and deterministic listening sequences.
- Multi-lesson selection, optional typed recall, favorites, and daily goals require the Product
  Owner decisions recorded in [`DESKTOP_GAP_ANALYSIS.md`](DESKTOP_GAP_ANALYSIS.md).
- Detailed evidence-backed product debt is tracked in
  [`DESKTOP_TECH_DEBT.md`](DESKTOP_TECH_DEBT.md).

## Mandatory Repository Onboarding Order for AI Agents & Contributors

Every future AI assistant or developer MUST read the repository knowledge system in this exact order before proposing architecture or modifying source code:

1. [`README.md`](../README.md)
2. [`docs/PRODUCT_PHILOSOPHY.md`](PRODUCT_PHILOSOPHY.md)
3. [`docs/REPOSITORY_CONSTITUTION.md`](REPOSITORY_CONSTITUTION.md)
4. [`docs/PRODUCT_BRAIN.md`](PRODUCT_BRAIN.md)
5. [`docs/PRODUCT_BRAIN_SPECIFICATION.md`](PRODUCT_BRAIN_SPECIFICATION.md)
6. [`docs/KNOWLEDGE_MODEL.md`](KNOWLEDGE_MODEL.md)
7. [`docs/LEARNING_EXPERIENCE_ARCHITECTURE.md`](LEARNING_EXPERIENCE_ARCHITECTURE.md)
8. [`docs/LEARNING_SCENE_FRAMEWORK.md`](LEARNING_SCENE_FRAMEWORK.md)
9. [`docs/LEARNING_SCENE_LIBRARY.md`](LEARNING_SCENE_LIBRARY.md)
10. [`docs/INSTRUCTIONAL_DECISION_ENGINE.md`](INSTRUCTIONAL_DECISION_ENGINE.md)
11. [`docs/ARCHITECTURE_AUDIT_V1.md`](ARCHITECTURE_AUDIT_V1.md)
12. [`docs/LEARNING_PRINCIPLES.md`](LEARNING_PRINCIPLES.md)
13. [`docs/SYSTEM_OVERVIEW.md`](SYSTEM_OVERVIEW.md)
14. [`docs/ARCHITECTURE.md`](ARCHITECTURE.md)
15. [`docs/AI_DESIGN_RULES.md`](AI_DESIGN_RULES.md)
16. [`docs/PROJECT_HANDOFF.md`](PROJECT_HANDOFF.md)
17. [`docs/AI_ARCHITECT_CONTEXT.md`](AI_ARCHITECT_CONTEXT.md)








Standing AI working agreements and delivery policies are governed by [`../AGENTS.md`](../AGENTS.md). Chat history is never durable project memory.


## Definition of Done

A Phase is done only when its roadmap Definition of Done is fully evidenced through real
boundaries and every included capability satisfies [`../AGENTS.md`](../AGENTS.md). A passing
build or one completed capability does not complete a Phase.

## Source of Truth Order

Use the order defined in [`../AGENTS.md`](../AGENTS.md): clean repository source/tests first,
then the standing working agreement, architecture/roadmap, this handoff, current AI context,
and finally historical records. Chat is never durable project memory.
