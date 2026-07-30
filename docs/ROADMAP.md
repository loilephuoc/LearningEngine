# Roadmap

The repository source and tests are authoritative. This roadmap plans work in durable Phases;
each Phase is delivered through separately verified capability commits and may span multiple
Codex sessions. Standing execution rules live only in [`../AGENTS.md`](../AGENTS.md).

## Phase 1 — Learning Engine Foundation

**Status: Completed**

Outcome: establish the reusable learning engine and its first complete Desktop learning flow.

Delivered boundaries include domain learning/review behavior, FSRS scheduling, study queues,
JSON persistence and transactions, analytics, content packages, real OPD3 import, lesson-scoped
study, persisted restart, grading, and completion.

## Phase 2 — Desktop Learning Experience

**Status: Completed**

Outcome: make the core Desktop workflows discoverable, keyboard-operable, accessible, and
recoverable.

Delivered boundaries include Study focus and keyboard operation, semantic screen
presentations, recoverable load states, Content Library navigation, shared search/refinement,
multi-term matching, and Unicode-safe matching/highlighting.

## Phase 3 — Data Integrity and Package Robustness

**Status: Completed**

Outcome: reject malformed external data before mutation and preserve persisted evidence across
failures and restart.

Delivered boundaries include stable package diagnostics, bounded strict OPD3 reads, archive
structure/size validation, required-entry and JSON context, non-fail-fast directory import,
classified JSON corruption, crash-safer replacement, inert stale temporary files, exact-byte
transaction rollback, and representative large-state restart verification.

## Phase 4 — Desktop Product Foundation

**Status: Completed**

Outcome: establish stable runtime and UX contracts suitable for preparing a Desktop Beta.

Delivered boundaries include application/build identity, platform runtime directories, typed
configuration, retained logging, lifecycle and diagnostics, shell navigation, safe window
restart, Light/Dark/System theme, English/Vietnamese shell localization, focus traversal,
startup presentation, and About diagnostics.

## Phase 5 — Desktop Beta Readiness

**Status: Implementation complete — external verification pending**

Outcome: produce an installable, supportable, recoverable Desktop Beta candidate and verify it
on a clean Windows environment without silently migrating existing data.

Planned capability sequence:

1. Desktop distributable packaging contract and deterministic local artifacts (delivered).
2. Diagnostic export with privacy-preserving support data (delivered).
3. Backup/restore or an explicitly approved equivalent recovery path (delivered).
4. First-run onboarding and representative sample content (delivered).
5. Windows path, permission, Unicode, install/update, and clean-machine smoke verification
   (local automation delivered; clean-machine execution pending).
6. Beta release checklist, known limitations, and release-candidate evidence (checklist and
   local unsigned-candidate evidence delivered; Product Owner gates pending).

### Phase Definition of Done

- A versioned Desktop artifact installs or runs through the approved distribution model on a
  clean supported Windows environment.
- User data/config/log/temp ownership remains explicit; existing data is neither moved nor
  migrated implicitly.
- Support diagnostics can be exported without secrets or persisted learning content.
- An approved recovery workflow protects user data, with failure and restart evidence.
- A new user can reach the primary learning flow from first run.
- Clean-machine smoke evidence covers paths, permissions, Unicode, lifecycle, and the primary
  import-to-study flow.
- Release checklist, known limitations, tests, docs, capability commits, and handoff are
  complete and consistent.

## Phase 6 — Learning Experience

**Status: Implementation complete — P6-01 through P6-10 complete; manual evidence pending**

### Problem statement

The engine can already import structured OPD3 content, plan and persist lesson-scoped queues,
recover an active session after restart, reveal an answer, record one of four ratings, update
FSRS state atomically, and show progress/completion in Desktop Study. The learner experience is
is evolving from a narrow screen-state flow: P6-02 made session presentation/recovery durable,
P6-03 established the Review Workspace state machine, and P6-04 established learner-facing
content independently of Compose. Remaining capabilities turn these foundations into a coherent
daily workspace without moving learning rules into UI or inventing hypothetical abstractions.

### Learner outcomes

- A learner can understand where they are in a session, what action is available, and what
  progress their action produced.
- Interruption, restart, pause, completion, and recoverable failure do not silently lose or
  duplicate a review.
- Prompt, answer, feedback, and supported rich content remain readable, keyboard-operable, and
  accessible.
- The experience supports the current retrieval-practice use case while preserving domain
  seams for additional evidence-backed learning modes.
- Daily use feels focused, predictable, responsive, and motivating rather than like repetitive
  card administration.

### Scope and capability order

1. **P6-01 — Define Phase 6: Learning Experience (complete)**: repository-owned problem,
   sequence, constraints, evidence, decisions, and exit criteria.
2. **P6-02 — Study Session lifecycle and recovery contract (complete)**: reconcile the existing
   `ACTIVE`/`FINISHED` domain model, persisted queue, `ActiveStudySessionRecovery`, and Desktop
   transient state; define valid lifecycle transitions and pause/resume semantics before UI
   expansion.
3. **P6-03 — Review Workspace state and action boundary (complete)**: replace ambiguous boolean
   combinations with a deterministic presentation/action model around prompt, reveal, rating,
   loading, failure, and completion, wired to existing application use cases.
4. **P6-04 — Learning Content Model (complete)**: establish ordered Question, Answer, and
   Example blocks for plain text, Markdown, image, and audio without renderer or lifecycle state.
5. **P6-05 — Rich Content Renderer (complete)**: render the structured text and local media forms
   represented by the P6-04 contract with explicit missing/unsupported fallbacks.
6. **P6-06 — Session progress, completion, and learning feedback (complete)**: make queue position,
   reviewed/new/due counts, completion, and scheduler feedback useful and consistent across
   session scopes.
7. **P6-07 — Pause, resume, one-step undo, and safe interruption (complete)**: deliver only transitions supported
   by explicit persistence and transaction semantics; undo must define its atomic boundary and
   must never partially reverse a review.
8. **P6-08 — Interaction, accessibility, and recoverable errors (complete)**: consolidate keyboard-first
   actions, focus transitions, semantic announcements, localization, and error recovery across
   the completed workspace.
9. **P6-09 — End-to-end learning-flow verification (complete)**: verifies the persisted OPD3
   package-to-Desktop flow through installed-package discovery, global session planning,
   reveal/rating, restart, progress, completion, final-review undo, re-rating, and completion
   recovery. Focused suites remain authoritative for lesson isolation, transaction rollback,
   pending-review replay, rich rendering, keyboard, focus, and accessibility.
10. **P6-10 — Topic Selection & Exact Resume (complete)**: enables independent multi-topic learning
    in Desktop Study, exact checkpoint resume across topic switches and application restarts,
    protecting active in-memory sessions while maintaining single source of truth across
    `StudySession`, `MemoryState`, `ReviewHistory`, and `StudyQueue` without duplicate progress models.

### Architectural constraints

- Domain/application contracts remain independent of Compose and concrete JSON storage.
- Extend the existing `StudySession`, queue, review transaction, and recovery boundaries before
  adding parallel lifecycle state.
- Persisted schema/API changes require compatibility or migration plus rollback/restart tests in
  the same capability.
- A review remains one atomic operation across review event, memory state, session, and queue.
- UI state may project domain/application state but must not become its authoritative source.
- Generalize beyond flashcard presentation only where a current content or learning-mode use
  case proves the seam; avoid speculative framework work.
- Phase 5 distribution/recovery contracts and its external verification gate remain intact.

### Out of scope

- Android, iOS, Web, cloud synchronization, generative AI, marketplace, and social features.
- Scheduler replacement or learning-science changes without separate evidence and acceptance
  criteria.
- Cloud/scheduled backup, cross-device merge, or release-signing work owned by Phase 5.

### Acceptance and exit criteria

- Every lifecycle state and transition has one authoritative owner and deterministic tests.
- Pause/resume/restart cannot duplicate, skip, or partially persist a review.
- Any delivered undo operation is atomic, bounded, restart-safe, and clearly disclosed.
- Review Workspace renders every currently supported content form selected for Phase scope with
  explicit fallback and error behavior.
- Global and lesson-scoped sessions expose consistent progress, completion, feedback, keyboard,
  focus, localization, and accessibility behavior.
- Failure paths preserve the last valid state and offer an actionable safe recovery path.
- Representative persisted end-to-end flows pass through real composition boundaries.
- Owned architecture, test matrix, capability map, changelog, handoff, and continuation state
  match committed behavior; required builds/tests are green.
- The Phase 5 external verification debt remains visible until independently closed.

### Open product decisions

- Pause is a user-facing interpretation of an active resumable session, not a persisted domain
  status.
- Undo is bounded to exactly the latest rating. P6-07 must reverse its derived scheduler,
  review-event, session, and queue effects atomically; multi-level undo is out of scope.
- P6-05 must render P6-04 plain text, Markdown, local image, and local audio blocks with fallback;
  safe HTML remains excluded until a real sanitized import use case exists.
- Learning feedback remains concise and neutral; gamification is excluded from Desktop 1.0.

## Milestone PB-00 — Repository Constitution & Product DNA

**Status: Completed**

Outcome: establish the repository knowledge system, Product Philosophy, Repository Constitution, System Overview, Product Brain conceptual framework, Cross-Platform Strategy, AI Design Rules, and Architectural Decision Records (ADRs) to permanently document Learning Engine 2.0 as an adaptive Teaching Engine.

Delivered boundaries include:
- `docs/PRODUCT_PHILOSOPHY.md`: Teaching Engine vs Flashcard app, North Star, and product values.
- `docs/PRODUCT_BRAIN.md`: Conceptual teaching loop (Learner Model → Objective → Strategy → Scene → Response → Evidence).
- `docs/LEARNING_PRINCIPLES.md`: 7 immutable pedagogical principles for guided learning.
- `docs/CROSS_PLATFORM_STRATEGY.md`: Shared Kotlin core architecture across Desktop, Android, iOS, and Web.
- `docs/SYSTEM_OVERVIEW.md`: High-level architecture map of all core subsystems.
- `docs/REPOSITORY_CONSTITUTION.md`: Non-negotiable architectural laws governing decision ownership.
- `docs/AI_DESIGN_RULES.md`: Mandatory MUST and MUST NOT guidelines for future AI agents.
- `docs/adr/ADR-0001` through `ADR-0004`: Architectural decision records defining key system boundaries.
- Mandatory 10-step reading order in `docs/PROJECT_HANDOFF.md`.

---

## Milestone PB-01 — Product Brain Specification

**Status: Completed**

Outcome: establish the official specification and architectural blueprint for the AI Teacher (`ProductBrain`). Defines the 17 core pedagogical concepts, complete 10-step Teaching Loop, subsystem responsibility matrix, Product Brain principles, and multi-year evolutionary roadmap in `docs/PRODUCT_BRAIN_SPECIFICATION.md`.

---

## Milestone PB-01.5 — Knowledge Model Specification

**Status: Completed**

Outcome: establish the canonical, subject-independent Knowledge Model specification in `docs/KNOWLEDGE_MODEL.md`. Defines the 15 core knowledge concepts (World, Topic, Module, Lesson, Concept, Knowledge Unit, Learning Asset, Learning Relationship, Difficulty Metadata, Prerequisite, Learning Dependency, Semantic Tag, Objective Mapping, Content Metadata, Evidence Mapping), universal domain mappings (Vocabulary, Stories, Medical Physics, Language, Technical), subsystem interaction boundaries, and Mermaid relationship diagrams.

---

## Milestone PB-01.8 — Learning Experience Architecture

**Status: Completed**

Outcome: establish the end-to-end session journey architecture in `docs/LEARNING_EXPERIENCE_ARCHITECTURE.md`. Defines the 7 session phases (Warm-Up, Teaching, Practice, Challenge, Review, Reflection, Summary), 5-subsystem orchestration rules (Product Brain, Knowledge Model, Strategy, Scheduler, Scenes), session runtime contracts (State, Context, Adaptive Transitions, Motivation, Termination), and complete session walkthroughs for Vocabulary, Interactive Story, Medical Physics, Language Learning, and General Knowledge.

---

## Milestone PB-02A — Learning Scene Framework

**Status: Completed**

Outcome: establish the canonical Learning Scene Framework specification in `docs/LEARNING_SCENE_FRAMEWORK.md`. Defines the 12 framework concepts, 9 architectural scene categories, scene lifecycle state machine (`Created` → `Prepared` → `Running` → `Paused` → `Resumed` → `Completed` / `Cancelled` → `Disposed`), input/output contracts, invariant prohibitions, subsystem authority matrix, cross-platform rendering strategy, and Mermaid architectural diagrams.

---

## Milestone PB-02B — Canonical Learning Scene Library

**Status: Completed**

Outcome: establish the complete Canonical Learning Scene Library specification in `docs/LEARNING_SCENE_LIBRARY.md`. Defines the 19-point uniform scene specification contract, taxonomy systems (memory types, cognitive load, duration, difficulty), 10 architectural categories (Teaching, Practice, Assessment, Story, Speaking, Medical, Programming, Mathematics, Reflection, Challenge), Product Brain selection and transition rules, and Mermaid architectural diagrams.

---

## Milestone PB-03 — Instructional Decision Engine

**Status: Completed**

Outcome: establish the canonical Instructional Decision Engine specification in `docs/INSTRUCTIONAL_DECISION_ENGINE.md`. Defines the 11 decision input streams, 10 decision outputs, multi-variate decision rules matrix across 10 cognitive scenarios, strict 5-tier priority hierarchy for conflict resolution, real-time closed-loop adaptive teaching engine, structured auditable Decision Trace logging, and Mermaid architectural diagrams.

---

## Milestone Desktop Alpha-01 — Session Bootstrap

**Status: Completed**

Outcome: implement Product Brain session bootstrap capability. When the learner selects a topic/content and triggers session start, Product Brain evaluates learner context, resolves `LearningSessionContext`, formulates `TeachingGoal`, initializes `SessionTimeline`, creates `InitialDecisionSnapshot`, projects `SessionOverview`, and enables `StartLearning`.

---

## Milestone Desktop Alpha-02 — Scene Execution

**Status: Completed**

Outcome: implement Product Brain single-scene execution capability. When the learner presses Start Learning, Product Brain selects the first Learning Scene (`TypingRecallScene`), renders the scene, collects learner input, evaluates `SceneResult` (with exact match, normalized match, and Levenshtein edit distance), converts `SceneResult` into `LearningEvidence`, and returns `LearningEvidence` to Product Brain.

---

## Milestone Desktop Alpha-03 — Adaptive Decision

**Status: Completed**

Outcome: implement Product Brain adaptive teaching capability. After receiving every `LearningEvidence`, `InstructionalDecisionEngine` evaluates performance and latency, generates an `AdaptiveDecision` (`INCREASE_DIFFICULTY`, `DECREASE_DIFFICULTY`, `REPEAT_SIMILAR_SCENE`, `MAINTAIN_PACE`), produces a `DecisionTrace` with triggered rules, updates the `SessionTimeline`, and notifies Desktop UI of the adaptive decision.

---

## Milestone Desktop Alpha Architecture Review

**Status: Completed**

Outcome: conduct architectural review of Desktop Alpha-01, Alpha-02, and Alpha-03 implementations in `docs/DESKTOP_ALPHA_ARCHITECTURE_REVIEW.md`. Confirms a coherent platform-neutral closed adaptive teaching loop, zero UI instructional logic, 100% test pass rate (1,643 tests), stable contracts across session bootstrap, scene execution, and adaptive decisioning, and issues GO recommendations for Alpha-03.5 and Alpha-04.

---

## Milestone Desktop Alpha-03.5 — Decision Explainability

**Status: Completed**

Outcome: implement Product Brain decision explainability capability. Product Brain generates a learner-facing `DecisionExplanation` for every adaptive decision (`INCREASE_DIFFICULTY`, `DECREASE_DIFFICULTY`, `REPEAT_SIMILAR_SCENE`, `MAINTAIN_PACE`), articulating evidence observation, decision summary, pedagogical reason, and next step without leaking internal rule IDs or technical enums. Desktop preserves the explanation through `StudyFacade` and `StudyViewModel`, renders it in `StudyScreen`, and lets the learner hide and show the same explanation without losing state.

---

## Milestone Desktop Alpha-04 — Session Completion

**Status: Completed**

Outcome: Product Brain aggregates the bootstrapped session, scene result, learning evidence, adaptive decision, decision trace, decision explanation, timeline, and final difficulty into a platform-neutral completion plan. The established review workflow remains the scheduler and transaction owner. A learner-facing completion snapshot is persisted with the finished `StudySession`, recovered after restart, and projected through Desktop with the learning outcome, reflection, reinforcement, next step, and scheduling guidance. Starting a new study workflow clears the prior completion presentation.

---












## Phase 7 — Desktop Beta Validation and v1

Repository-driven stabilization now includes the real-data responsiveness boundary: asynchronous
long operations, observable import phases, bulk library/study queries, virtualized lesson rows,
debounced search, and lazy bounded thumbnails. Final acceptance still requires Product Owner
testing with the original 179 MB package and interactive resize/scroll/media observation.

The repository-driven release-blocker remediation now includes reliable import of the Product
Owner's builder format: a same-basename legacy JSON document plus an `OPD3`-magic binary PKG.
Automated evidence covers routing, validation, persistence, media, library discovery, and
session startup. Phase 7 still requires manual verification with the original large package;
that external evidence is not inferred from the synthetic fixture.

Repository stabilization also rejects completed sessions from an earlier installation lifecycle
when deterministic package IDs are reused. Orphan reimport transactionally reconciles exact
package learning state, while valid current-installation completion/Undo and unrelated package
sessions remain preserved. Automated store-backed evidence is complete; interactive confirmation
with the Product Owner's current persisted data remains manual Phase 7 evidence.

**Status: Automated stabilization complete — manual and external validation pending**

Outcome: validate the Beta and completed learning experience with representative real workloads
and establish the stable Desktop v1 boundary. Measure startup, import, search, queue planning,
and Study responsiveness; prioritize crashes, data loss, incompatible upgrades, and blocked
workflows; refine behavior using observed evidence rather than speculative polish.

Desktop 1.0 is reached only after P6-09 and repository-driven release-candidate defect fixing
(both complete), plus the Phase 5 external clean-machine/install/upgrade/signing evidence and
representative manual or real-user verification.

The final repository-driven release audit hardened malformed backup-manifest validation and
completed all locally automatable compile, test, packaging, and documentation checks. Remaining
Phase 7 work requires Product Owner/manual, clean-machine, installer, upgrade, uninstall,
signing, or real-user evidence.

The Windows native launcher blocker discovered after that audit is resolved and guarded by an
accessibility-enabled bundled-runtime smoke task. Native app-image startup is automated; actual
installer lifecycle, signing, clean-machine, and real-user approval remain external gates.

## Library and Topic Persistence Beta — Package Platform v1

**Status: Package Platform v1 complete (Media Packaging, OPD3 Export, Package Inspector, Verification)**

Outcome: make installed topics portable and locally manageable without mixing content packages
with learner progress or silently breaking resume state.

Capability sequence:

1. **Beta-L01 — Topic Identity and Resume State (complete):** persist a durable `TopicId` with
   installed packages, migrate legacy records deterministically, bind sessions to optional topic
   identity, and restore checkpoints by `(LearnerId, TopicId)`. Existing item-level memory,
   review history and scheduler state remain authoritative and are not duplicated.
2. **Beta-L02A — Legacy Pair Discovery & Validation (complete):** scan one folder through a JVM
   file-reader port while Application owns same-name pairing, one-JSON/one-PKG validation,
   deterministic ordering, and structured diagnostics.
3. **Beta-L02B — Legacy Pair Conversion (complete):** convert one validated legacy topic pair
   into one deterministic, platform-neutral canonical topic package model (`CanonicalTopicPackage`).
4. **Package Platform v1 (complete):**
   - **Media Packaging:** catalog media, deduplicate assets, compute SHA-256 checksums, build media manifest.
   - **OPD3 Export:** byte-for-byte deterministic export of `.opd3` package archives (metadata, contents, learning items, media manifest, SHA-256 manifest).
   - **Package Inspector:** inspection API exposing schema version, topic identity, content/item/media counts, asset sizes, checksums, and diagnostics.
   - **Verification:** package integrity, manifest hash verification, schema v1.0 validation, and missing asset detection.
5. **Beta-L04 — Conflict-aware Import & Duplicate Sanitization (complete):** preserve compatible learner progress across re-import and package update, reject duplicate import gracefully without emitting false completed stage.
6. **Beta-L05 — Delete/Archive & Library Integrity Recovery (complete):** non-destructive archive, active package lifecycle consistency, and full 10-boundary atomic uninstall reconciliation with transaction rollback.
7. **Beta-L06 — Workspace & Rich Lesson Exploration (complete):** rich lesson exploration workspace supporting `EXPLORE` mode (item-by-item content browsing) → `PREPARE` mode (study session setup) → `STUDY` mode (active study execution).
8. **Topic Selection & Exact Resume (complete):** active topic is authority when Study is idle, topic switching without session/progress loss, exact checkpoint resume.
9. **PLE-020 — Content Studio Desktop UX Polish & Layout Remediation (complete):** native Drag & Drop media, 4-row desktop hierarchy (Question/Answer, 50/50 compact IPA/POS, 50/50 paired Example/Translation, StudioHeroImage), adaptive field collapsing, full-resolution Skia hero renderer with zoom/fullscreen, symmetrical 56dp card height, and responsive BoxWithConstraints breakpoint.
10. **PLE-021 — Modern Learning Workspace (In Progress):**
    - **PLE-021A — Modern Learning Workspace Shell (complete):** modular composable workspace hierarchy (`SessionHeader`, `LearningWorkspaceSurface`, `SecondaryWorkspace`, `ActionDock`, `StatusStrip`), active study context header topic resolution (`PLE-021A.1`).
    - **PLE-021B — Adaptive Vocabulary Discovery and Focused Answer Experience (complete):** focused answer surface (`PLE-021B.1`), discovery mode for brand-new vocabulary (`PLE-021B.2`), design system visual polish and accessibility (`PLE-021B.3`).
11. **PLE-026 — Adaptive Study Presentation (complete):**
    - **Status**: Completed (Desktop Manual UAT: PASS).
    - **Scope Delivered**: Adaptive Question Presentation, Preference Guided Presentation, Manual Presentation, Full Answer disclosure, standardized audio behavior, image presentation, scheduler feedback, rating dock, English/Vietnamese semantic highlighting with exact word boundaries and canonical infinitive target normalization (`"to sign"` -> `"sign"`), Content-level Study Badge (`contentPresentationStage`), and clear separation between `learningStage` (LearningItem level) and `contentPresentationStage` (Content level).
12. **PLE-027 — Study Experience Visual Polish (Awaiting Product Owner UAT):**
    - **Status**: Technical UAT Complete (Awaiting Product Owner Final Desktop UAT).
    - **PLE-027A — Responsive Study Visual Layout Contract (complete)**: pure Kotlin `StudyVisualLayoutResolver` and immutable `StudyVisualLayout` contract, deterministic viewport classification (`COMPACT`, `STANDARD`, `WIDE`), max content width bounding (800dp), short viewport height image scaling, metadata arrangement (`INLINE`/`STACKED`), and rating buttons arrangement (`HORIZONTAL`/`GRID_2X2`).
    - **PLE-027B — Answer Surface Visual Hierarchy & Responsive Content Polish (complete)**: visual hierarchy, typography, image viewport, meaning card, example layout, scheduler feedback polish, rating dock, responsive desktop.
    - **PLE-027C — Representative Desktop UAT & Closure (technical UAT complete)**: Desktop technical UAT validation across 15 content variants and 3 viewport classes, zero regressions verified.
    - **Giữ nguyên**: Scheduler, FSRS, Queue Planning, Persistence, Learning semantics.
13. **PLE-028 — Visual Theme System (In Progress):**
    - **PLE-028A / PLE-028A.1 — Visual Theme System Foundation & Design Language Completion (complete):** approved 21-chapter Design Constitution.
    - **PLE-028B — Design System Core Token Architecture & Theme Engine Foundation (complete):** immutable semantic token groups, single `LETheme` component façade, internal deterministic resolution, compatibility-only Material adapter, and durable authority/token regression coverage. No screen migration or visual change.
    - **PLE-028C — Base Controls and Surface Migration (complete):** LETheme-only semantic surface/button primitives, deterministic interaction-state and density projection, controlled migration of shared load-state and search-scope cards, with legacy screen controls explicitly retained for later migration.
    - **PLE-028D — Study Screen Visual Theme Migration and Contrast Remediation (implementation complete; Manual UAT pending):** neutral themed canvas, semantic answer/Meaning/Example/Scheduler/rating surfaces, readable Pause/Undo, tokenized four-action rating dock, and preserved responsive/audio/scheduler/keyboard authorities.
    - **PLE-028D.1 — Study Visual UAT Remediation (implementation complete; visual re-UAT pending):** stronger semantic POS badge, POS beside Vietnamese meaning, subdued Dark ready status, stable Answer hover contrast, and resolver-owned vertical image budget/dock reservation.
    - **PLE-028D.2 — Study Visual Re-UAT Final Remediation (implementation complete; final visual re-UAT pending):** balanced Meaning/POS composition in Question and Answer, readable POS and rating typography, and stable semantic Example hover contrast in Light/Dark while preserving all behavior and responsive/image contracts.
    - **PLE-028E — Dynamic Part-of-Speech Semantic Color Registry (implementation complete; visual UAT pending):** one application canonicalization/registry authority, installed-content startup reconciliation, post-import registration, fixed known semantic identities, deterministic future-POS allocation, and shared Light/Dark Study badge resolution.
    - **PLE-029 — Robust Learning-Key Highlighting and Configurable Study Audio Shortcuts (implementation complete; Manual UAT pending):** original-index normalized highlighting and configurable vocabulary/example loop plus Vietnamese one-shot audio commands.
    - **PLE-030 — Realtime Study Header Statistics (FINAL PASS):** exact-scope statistics, corrected session/review semantics, measured adaptive Full Answer layout, and bounded semantic Study Chrome passed Product Owner Manual UAT.
    - **PLE-030.1 — Study Header Session Progress Semantics Remediation (FINAL PASS):** Total learned/latest-bucket invariant, New completed/configured target, Review remaining/configured target, effective workload separation, and audited session/Continue/Review Mode boundaries.
    - **PLE-030.2 — Study Statistics Header Visual Refresh (FINAL PASS):** approved compact eight-metric dashboard, semantic typography/icons/colors, split fraction emphasis, muted zero states, localized accessibility, and existing-responsive-authority 8-column/4+4 layouts; business semantics unchanged.
    - **PLE-030.3 — Session Classification and Review Cue Remediation (FINAL PASS):** persisted immutable NEW/REVIEW admission origin, exact-once counters and Undo/restart recovery, previous-rating underline for REVIEW only, redundant ready-cue removal, and resolver-owned vertical fit.
    - **PLE-030.4 — Content-Level Learning Progress and Review Context Remediation (FINAL PASS):** ContentId learner-facing progress identity across admission/quota/counters/Total/buckets/previous rating, while LearningItemId remains scheduler and execution identity.
    - **PLE-030.5 — Front Context, Display Reflow, and Session Goals (FINAL PASS):** read-only question rating context, display-environment cache invalidation, and durable configured/effective goals.
    - **PLE-030.6 — Compact Height and New-Content Introduction (FINAL PASS):** height modes and image budgets, fixed-center-scroll layout, de-cluttered Study header, persisted ContentId Introduction, and one-shot Vietnamese meaning audio.
    - **PLE-030.7 — Introduction and Compact Chrome Remediation (FINAL PASS):** atomic one-step visual Introduction-to-Answer, mandatory-input preservation, compact top/rating chrome, and both-example compact budget.
    - **PLE-030.8 — Answer Dock Restoration (FINAL PASS):** direct reveal now enters normal Rating Ready, renders the existing actionable answer dock, and restores it after restart.
    - **PLE-030.9 — Study Goal Synchronization and Review Memory Indicator (FINAL PASS):** fresh persisted goals for each new session and Content-owned read-only REVIEW memory across every pre-answer experience.
    - **PLE-031 — Live Audio Shortcut Toolbar (FINAL PASS):** shared chord formatting, live persisted Change/Reset projection, four availability-aware audio actions, and minimum-width overflow passed Product Owner UAT.
    - **PLE-031.1 — Semantic Audio Toolbar remediation (FINAL PASS):** command-owned stable loop/audio icons, distinct vocabulary/example identities, and accessible `VI`/`VI+` locale badges.
    - **PLE-031.2 — Live Shortcut Cues for Semantic Audio Actions (FINAL PASS):** semantic icon plus live formatted chord, structured tooltip/accessibility, fixed-height Standard/Compact/Minimum composition, and icon/name/chord overflow.
    - **PLE-032-B1 — Application Continuation Boundary (complete):** application-owned
      validation and orchestration for one manual general-Study continuation, explicit
      Accepted/NoWork/Rejected outcomes, deterministic predecessor-derived next Session
      identity, retained predecessor/Undo evidence, and thin Desktop delegation.
    - **PLE-033-B1 — Desktop UX Polish (complete):** durable remembered Custom Review target,
      Answer-side Space through the existing Good action, token-rounded adaptive Answer image,
      and aligned purple configured targets without changing Study/Planner/Scheduler semantics.
    - **Next:** PLE-032-B2 — durable Continuous Review intent and restart continuation. Full
      PLE-032 Continuous Review Mode remains incomplete.

Package Platform v1 implements media packaging, OPD3 export, package inspection, verification, conflict-aware import, rich lesson exploration, complete library integrity recovery, Content Studio UX polish, modern learning workspace (PLE-021A/B), and Adaptive Study Presentation (PLE-026) without modifying learner state.

## Phase 8 — Desktop Product Evolution

**Status: Defined — begins only after Desktop v1 external gates**

Outcome: make Desktop measurably better than the Android product reference while retaining the
verified learning engine. Evidence, gaps, ownership decisions, and independently deliverable
capabilities are in [`ANDROID_PRODUCT_BEHAVIOR.md`](ANDROID_PRODUCT_BEHAVIOR.md),
[`DESKTOP_GAP_ANALYSIS.md`](DESKTOP_GAP_ANALYSIS.md),
[`DESKTOP_PRODUCT_ARCHITECTURE.md`](DESKTOP_PRODUCT_ARCHITECTURE.md), and
[`DESKTOP_PRODUCT_ROADMAP.md`](DESKTOP_PRODUCT_ROADMAP.md).

Platform-independent learner behavior is authoritative under [`spec/`](spec/). The revised
sequence is LX-01 Hierarchical Scope → LX-02 Session Entry/Setup → LX-03 Focused Workspace →
LX-04/05 Semantic and Deterministic Media → LX-06 Multi-Lesson → LX-07 Listening → LX-08 Typed
Recall → LX-09 Summary/Goals → LX-10 Curation → LX-11 Ethical Auto Flow. This replaces the
former subsystem-first DP order because a stable entry/setup journey must precede optional
mode automation.

Recommended order starts with read-only hierarchical learning scope (LX-01), followed by
Session Entry/Setup and Focused Workspace before approved multi-lesson and listening slices.
Android scheduler steps, queue
heuristics, mutable sentence persistence, lock-screen/device-admin behavior, and Activity-owned
business logic are explicitly excluded.

### Phase 8 Definition of Done

- accepted Desktop product outcomes reach the real composition boundary;
- scheduler/session/queue/persistence authority remains in Domain/Application;
- media/input callbacks cannot create learning evidence or duplicate progress;
- legacy packages, profiles, sessions, and generic media behavior remain compatible;
- each capability has automated acceptance and representative manual UAT evidence;
- product decisions and residual debt remain explicit.
- all implemented clients conform to the same action permissions, rating semantics,
  interruption recovery, and media safety behavior defined in `docs/spec/`.

## Phase 9 — Additional Platforms

**Status: Deferred until Desktop v1**

Outcome: introduce Android, iOS, and Web consumers only after shared engine contracts and
Desktop v1 behavior are stable. Do not add premature cross-platform abstractions solely to
prepare for this Phase.

## Delivery references

Capability/build/Git rules are in [`../AGENTS.md`](../AGENTS.md). Historical milestone and
completed-Phase records are in [`MILESTONE_HISTORY.md`](MILESTONE_HISTORY.md); detailed verified
increments are in [`CHANGELOG.md`](CHANGELOG.md).
PLE-030.5 implements front-side review context, density-aware reflow, and durable
configured/effective Content workloads. Automated verification is complete; manual UAT remains
pending.
