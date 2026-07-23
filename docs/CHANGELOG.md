## LP-003 — Desktop Library Experience

- Delivered the first production-quality Desktop Library experience backed by LP-002 application query layer (`LibraryQueryService`).
- Created presentation models and controller in `vn.loi.learning.desktop.ui.library`: `LibraryUiState` (`Loading`, `Content`, `Empty`, `Error`), `LibrarySection` (`OVERVIEW`, `INSTALLED`, `ACTIVE`, `ARCHIVED`, `COLLECTIONS`, `DELETED`), `LibraryFacade`, and `LibraryViewModel`.
- Implemented modular Compose Desktop components: `LibraryScreen`, `LibraryHeader`, `LibrarySectionTabs`, `PackageListSection`, `CollectionListSection`, `LibraryOverviewSection`, `LibraryEmptyView`, `LibraryErrorView`, `LibraryLoadingView`.
- Displays Library identity, total/active/archived installed package summaries, collection nodes with assigned active package chips, deleted collection summaries, and library-level aggregate statistics.
- Wired `LibraryQueryService` into `LearningApplicationContext` and `LearningApplicationFactory`.
- Integrated `LibraryScreen` directly into the main desktop application shell and navigation framework (`NavigationDestination.CONTENT_LIBRARY`).
- Added state unit tests in `LibraryViewModelTest` verifying state mapping, section switching, statistics propagation, empty state, and repository-decoupled presentation logic.

## LP-002 — Library Query & Navigation Foundation


- Implemented the complete read-side navigation layer for Library without aggregate mutation.
- Exposed immutable DTOs and query models (`InstalledPackageSummary`, `CollectionSummary`, `CollectionNode`, `LibraryStatistics`, `LibraryNavigationTree`).
- Added `LibraryQueryService` in `vn.loi.learning.application.library.query` orchestrating read queries for installed packages, active packages, archived packages, active collections, deleted collections, statistics, and full navigation hierarchy.
- Created `toSummary()` projection extension functions in `LibraryQueryProjections.kt`.
- Extended `InstalledPackageRepository` and `CollectionRepository` with read-by-library default query methods.
- Added comprehensive unit tests in `LibraryQueryProjectionsTest` and `LibraryQueryServiceTest` verifying immutability, zero mutation, exact statistics calculations, and deterministic ordering.

## Package Platform v1.1 — Production Hardening

- Upgraded `Opd3PackageInspector` to perform incremental streaming reading with 8KB bounded buffers and running byte counters, aborting immediately upon exceeding single-entry or total package size limits.
- Refactored `Opd3PackageVerifier` to use a single unified validation pipeline (`verify(inspectionResult)`) across ByteArray and Path overloads with zero duplicated validation logic.
- Replaced synthetic fake resource limit tests with real streaming limit enforcement tests over `Opd3PackageInspector`.
- Adopted Option A architecture rejecting duplicate keys in `manifest.json` `files` map.
- Added strict format validation requiring `metadata.json` format == "OPD3".
- Made mandatory `TopicId` validation strict in package metadata.
- Made `media-manifest.json` a strictly required entry in OPD3 package archives.
- Centralized all path traversal and layout validation into single canonical `Opd3PathValidator`.
- Strengthened adversarial test suite covering all 20+ production hardening test cases.

## Package Platform v1

- Implemented Capability A Media Packaging (`CanonicalMediaBundle`, `CanonicalMediaManifest`, `PackageMediaAssetCollector`): asset collection, deduplication, SHA-256 checksum calculation, unresolved asset diagnostics, deterministic ordering.
- Implemented Capability B OPD3 Export (`Opd3PackageExporter`, `DeterministicZipWriter`): 100% byte-for-byte deterministic `.opd3` ZIP archive generation containing `metadata.json` (schema v1.0), `contents.json`, `learning-items.json`, `media-manifest.json`, `media/*`, and `manifest.json`.
- Implemented Capability C Package Inspector (`Opd3PackageInspector`, `PackageInspectionResult`): inspection API exposing package version, schema version, topic ID, topic name, content count, learning item count, media count, asset sizes, checksums, and diagnostics without requiring Desktop UI.
- Implemented Capability D Verification (`Opd3PackageVerifier`, `PackageVerificationReport`): package integrity verification, SHA-256 manifest checksum verification, schema v1.0 validation, missing asset detection.
- Added comprehensive unit and integration test suites: `MediaPackagingTest`, `Opd3DeterministicExporterTest`, `Opd3PackageInspectorTest`, `Opd3PackageVerifierTest`, `PackagePlatformRoundTripTest`.
- Documented remaining roadmap capabilities: Conflict-aware Import, Workspace, Collections, Archive/Delete.

## Beta-L02B — Legacy Pair Canonical Conversion

- Added platform-neutral `CanonicalTopicPackage`, `LegacyTopicSourceMetadata`, `CanonicalMediaReference`, `CanonicalConversionDiagnostic`, and `LegacyPairCanonicalConversionResult` models.
- Implemented `LegacyPairCanonicalConverter` in `vn.loi.learning.application.contentpackaging` to convert `ValidatedLegacyTopicPair` into one deterministic canonical topic package model.
- Preserved durable `TopicId` explicitly from Beta-L02A discovery without deriving fresh random identities.
- Derived stable `ContentId` and `LearningItemId` values deterministically while preserving content-to-item relationships and supported structural data.
- Added structured conversion diagnostics for malformed JSON, invalid required fields, duplicate content IDs, duplicate learning-item IDs, unresolved content-to-item relationships, and unresolved media references with explicit `FATAL` vs `WARNING` severity.
- Represented media references with `PRESENT` vs `MISSING` status by analyzing PKG entries (OPD3-binary or ZIP) via `JvmLegacyPkgMediaScanner` without extracting bytes or writing OPD3 archives.
- Excluded learner-specific SRS/scheduler/mastery state from canonical package models.
- Added focused unit tests covering all 15 prompt requirements plus an end-to-end synthetic pair conversion integration test.

## Beta-L02A — Legacy Pair Discovery & Validation

- Added platform-neutral Application models and a discovery service for legacy topic folders.
- Defined one validated pair as exactly one readable, supported `<logical-name>.json` and one
  same-name `<logical-name>.pkg`, matched case-insensitively.
- Added structured diagnostics for missing JSON, missing PKG, duplicate JSON, duplicate PKG,
  base-name mismatch, unreadable files, and unsupported file/package formats.
- Added deterministic ordering for input files, validated pairs, diagnostic codes, and diagnostic
  source paths.
- Added a JVM folder reader that enumerates direct regular files and recognizes existing ZIP or
  OPD3 PKG signatures without parsing JSON, converting content, extracting media, persisting data,
  or writing OPD3.
- Added focused unit coverage for every pairing/cardinality rule plus unreadable/unsupported
  diagnostics, and a real folder-to-validated-pair integration test.

## Beta-L01 — Topic Identity and Resume State

- Added durable `TopicId` ownership to installed `ContentPackage` records. Existing package JSON
  without the optional field derives one deterministic identity from logical package name and
  format; subsequent writes persist it explicitly.
- Preserved `TopicId` across compatible package-version replacement and kept it independent of
  package release ID, filesystem path, display ordering, and mutable display metadata.
- Added optional topic ownership to `StudySession` and its schema-compatible record mapper.
  Learner-topic checkpoints reuse the existing session and queue stores; memory, review history,
  difficulty, mastery, and scheduling remain authoritative per learner and learning item.
- Added topic-specific active-session recovery through the application boundary. Desktop topic
  switching clears transient projection, resumes the selected learner-topic checkpoint when
  present, or creates a new scoped session that reuses existing scheduler progress.
- Added deterministic compatibility identity for unpackaged legacy/local content without adding
  a parallel learner database.
- Added domain, mapper, store, compatible-upgrade, installed OPD3 restart, and Desktop
  A → B → A switching/restart integration coverage.

## Desktop Alpha-04 — Session Completion

- Added platform-neutral Product Brain completion models and orchestration for reflection, learner summary, learning outcome, scheduler rating intent, scheduling outcome, and final completion result.
- Aggregated the real Alpha-01 through Alpha-03.5 session context, scene result, evidence, adaptive decision, trace, explanation, timeline, and difficulty state before completion.
- Reused `ReviewSessionItemUseCase` and the configured scheduler for the committed review; no scheduling rule or scheduler implementation moved into Product Brain or Desktop.
- Added an optional learner-facing `SessionCompletionSnapshot` to the existing `StudySession` schema-v1 record and mapper, preserving legacy-record defaults while enabling restart recovery through the existing session repository.
- Projected completion through `StudyFacade`, `StudyViewModel`, `StudyUiState`, `LearningShell`, `ContentHost`, and `StudyScreen`; a new workflow clears stale completion presentation.
- Added reflection, summary, orchestration, scheduler, persistence, end-to-end application, and Desktop integration coverage.

## Desktop Alpha-03.5R — Decision Explainability UI Completion

- Completed the Desktop consumer boundary for learner-facing decision explanations.
- Preserved the current `DecisionExplanation` through `StudyFacade` and `StudyViewModel` show, hide, and toggle transitions.
- Rendered the observation, decision summary, pedagogical reason, and next step in `StudyScreen`, with controls to hide and show the same explanation without losing state.
- Replaced the state-copy test with a Desktop integration test covering `StudyFacade` through `StudyViewModel` to `StudyUiState`.

## Desktop Alpha-03.5 — Decision Explainability

- Implemented Product Brain decision explainability capability for learner-facing adaptive teaching explanations.
- Created `DecisionExplanation` model (`explanationId`, `decisionId`, `observation`, `decisionSummary`, `pedagogicalReason`, `nextStep`) in `vn.loi.learning.application.decision`.
- Added `InstructionalDecisionEngine.generateExplanation(...)` covering all 4 adaptive decisions (`INCREASE_DIFFICULTY`, `DECREASE_DIFFICULTY`, `REPEAT_SIMILAR_SCENE`, `MAINTAIN_PACE`) without exposing technical rule IDs or enums.
- Projected `DecisionExplanation` in Desktop UI state (`StudyUiState`, `StudyFacade`, `StudyViewModel`) and added show/hide visibility toggle handlers (`toggleDecisionExplanationVisibility()`, `showDecisionExplanation()`, `hideDecisionExplanation()`).
- Added unit tests (`InstructionalDecisionEngineExplanationTest`) and Desktop integration tests (`DesktopDecisionExplainabilityTest`).
- Updated `docs/ROADMAP.md`, `docs/CHANGELOG.md`, `docs/AI_ARCHITECT_CONTEXT.md`, and `docs/PROJECT_HANDOFF.md`.

## Desktop Alpha Architecture Review


- Completed architectural audit evaluating Desktop Alpha-01, Alpha-02, and Alpha-03 implementations across 12 core areas in `docs/DESKTOP_ALPHA_ARCHITECTURE_REVIEW.md`.
- Confirmed a coherent, platform-neutral closed adaptive teaching loop where Product Brain owns session bootstrap, scene execution, evidence processing, adaptive decisions, and timeline updates.
- Verified that Compose Desktop UI remains 100% presentation-only with zero instructional or grading logic.
- Confirmed stability of contracts across `vn.loi.learning.application.session.bootstrap`, `vn.loi.learning.application.scene`, and `vn.loi.learning.application.decision`.
- Established Prioritized Refactoring Backlog for post-Alpha milestones and issued GO recommendations for Alpha-03.5 and Alpha-04.
- Updated `docs/ROADMAP.md`, `docs/CHANGELOG.md`, `docs/AI_ARCHITECT_CONTEXT.md`, and `docs/PROJECT_HANDOFF.md`.

## Desktop Alpha-03 — Adaptive Decision


- Implemented Product Brain adaptive decision-making capability.
- Created platform-neutral models in `vn.loi.learning.application.decision`: `AdaptiveAction`, `AdaptiveDecision`, `DecisionTrace`, `AdaptiveOutcome`, and `InstructionalDecisionEngine`.
- Implemented initial rule set: `CORRECT` + fast (<2000ms) $\rightarrow$ `INCREASE_DIFFICULTY`, `INCORRECT` $\rightarrow$ `DECREASE_DIFFICULTY`, `PARTIAL` $\rightarrow$ `REPEAT_SIMILAR_SCENE`, `CORRECT` + nominal pace $\rightarrow$ `MAINTAIN_PACE`.
- Added `SessionTimeline.updateWithDecision(action)` to dynamically adjust estimated phase times and item counts.
- Extended `ProductBrainPlanner` with `evaluateAndAdapt(...)` and integrated adaptive decision fields (`lastAdaptiveDecision`, `lastDecisionTrace`, `currentDifficultyLevel`) into Desktop UI projection (`StudyUiState`, `StudyFacade`, `StudyViewModel`).
- Added unit tests (`InstructionalDecisionEngineTest`) and Desktop integration tests (`DesktopAdaptiveDecisionTest`).
- Updated `docs/ROADMAP.md`, `docs/CHANGELOG.md`, `docs/AI_ARCHITECT_CONTEXT.md`, and `docs/PROJECT_HANDOFF.md`.

## Desktop Alpha-02 — Scene Execution


- Implemented Product Brain single-scene execution capability for `TypingRecallScene`.
- Created platform-neutral contracts in `vn.loi.learning.application.scene`: `LearningSceneInput`, `SceneResult`, `LearningEvidence`, `EvidenceReceipt`, `LearningScene`, and `TypingRecallScene`.
- Extended `ProductBrainPlanner` with `selectFirstScene(...)` and `processEvidence(...)`.
- Integrated scene execution fields (`activeScene`, `lastSceneResult`, `lastLearningEvidence`) into Desktop UI state projection (`StudyUiState`, `StudyFacade`, `StudyViewModel`).
- Added unit tests (`TypingRecallSceneTest`) and Desktop integration tests (`DesktopSceneExecutionTest`).
- Updated `docs/ROADMAP.md`, `docs/CHANGELOG.md`, `docs/AI_ARCHITECT_CONTEXT.md`, and `docs/PROJECT_HANDOFF.md`.

## Desktop Alpha-01 — Session Bootstrap


- Implemented Product Brain session bootstrap capability allowing Product Brain to evaluate learner context and topic selections to initialize study sessions.
- Created core platform-neutral models in `vn.loi.learning.application.session.bootstrap`: `LearningSessionContext`, `TeachingGoal`, `SessionTimeline`, `InitialDecisionSnapshot`, and `SessionOverview`.
- Implemented `ProductBrainSessionBootstrap` application service and integrated `bootstrapSession(...)` into `ProductBrainPlanner`.
- Integrated `SessionOverview` projection into Desktop UI (`StudyUiState`, `StudyFacade`, `StudyViewModel`).
- Added unit tests (`ProductBrainSessionBootstrapTest`) and Desktop integration tests (`DesktopSessionBootstrapTest`).
- Updated `docs/ROADMAP.md`, `docs/CHANGELOG.md`, `docs/AI_ARCHITECT_CONTEXT.md`, and `docs/PROJECT_HANDOFF.md`.

## Architecture Audit v1.0


- Created `docs/ARCHITECTURE_AUDIT_V1.md` evaluating the codebase (`vn.loi.learning.*` and `vn.loi.learning.desktop.*`) against all established architectural specifications.
- Evaluated 15 core architectural areas: Domain Layer, Application Layer, Product Brain, Learning Flow, Scheduler, Knowledge Model, Learning Scenes, Desktop Presentation, Persistence, Import Pipeline, Cross-Platform Readiness, Dependency Directions, Layer Boundaries, Separation of Concerns, and Technical Debt.
- Delivered an empirical assessment of **Desktop Alpha Readiness: READY**, supported by 1,639 passing tests and strict inward dependency flow.
- Established a Prioritized Refactoring Backlog across High, Medium, and Low priorities.
- Updated `README.md`, `docs/PROJECT_HANDOFF.md`, and `docs/AI_ARCHITECT_CONTEXT.md`.

## Milestone PB-03 — Instructional Decision Engine


- Created `docs/INSTRUCTIONAL_DECISION_ENGINE.md` defining the reasoning architecture of Product Brain for making all pedagogical decisions.
- Formulated the 11 Decision Input streams (Learner Model, Knowledge Model, Session Context, Evidence, Scheduler State, Motivation, Fatigue, Confidence, Mastery, Available Time, Objectives) and 10 Decision Output types (Goal, Strategy, Scene, Difficulty, Plan, Feedback, Transition, Reflection, Review, Completion).
- Established the Decision Rules Matrix across 10 cognitive scenarios (High Fatigue, Low Confidence, High Mastery, Low Retention, Limited Time, Repeated Mistakes, Fast Improvement, Long Inactivity, High Motivation, Mixed Mastery).
- Specified the 5-Tier Priority Hierarchy for deterministic conflict resolution when multiple rules trigger simultaneously.
- Designed the closed-loop Adaptive Teaching Engine for real-time micro and macro lesson adjustments.
- Defined the auditable Decision Trace logging format for full explainability of pedagogical choices.
- Included Mermaid diagrams for Decision Pipeline, Decision Flow, and Subsystem Sequence.
- Updated `README.md`, `docs/PROJECT_HANDOFF.md`, `docs/SYSTEM_OVERVIEW.md`, `docs/ROADMAP.md`, and `docs/AI_ARCHITECT_CONTEXT.md`.

## Milestone PB-02B — Canonical Learning Scene Library


- Created `docs/LEARNING_SCENE_LIBRARY.md` defining the complete canonical library of reusable educational interaction capabilities available to Product Brain.
- Established a 19-point uniform scene specification contract covering Purpose, Learning Objectives, Typical Inputs, Interaction Pattern, Expected Evidence, Strengths, Weaknesses, Best Used When, Avoid When, Compatible Strategies, Next Scenes, Cognitive Load, Estimated Duration, Memory Types, Difficulty Range, Adaptation Opportunities, and Accessibility.
- Formulated canonical taxonomy systems for Memory Types (11 types), Cognitive Load (Low/Med/High), Duration (Very Short to Long), and Difficulty Range (Beginner to Adaptive).
- Authored canonical scene specifications across 10 architectural categories: Teaching (Concept Intro, Guided Explanation, Worked Example, Interactive Demo), Practice (Typing, Oral, Image, Audio, Free Recall, Matching, Classification, Sequencing, Cloze), Assessment (Multiple Choice, Short Answer, Essay, Confidence Rating, Explain Back, Teach Back), Story (Reading, Listening, Prediction, Continuation, Dialogue, Narrative Reconstruction), Speaking (Pronunciation, Shadowing, Conversation, Role Playing), Medical (Clinical Case, Diagnosis, Treatment Planning, Imaging Interpretation, Anatomy Labeling), Programming (Code Completion, Debugging, Algorithm Tracing, Refactoring, Architecture Review), Mathematics (Equation Solving, Proof Construction, Graph Interpretation, Visualization), Reflection (Reflection, Self Assessment, Learning Journal, Goal Review), and Challenge (Mixed Review, Mission, Speed Round, Boss Challenge, Capstone).
- Included Mermaid diagrams for Scene Taxonomy, Scene Selection Flow, and Subsystem Interaction.
- Updated `README.md`, `docs/PROJECT_HANDOFF.md`, `docs/SYSTEM_OVERVIEW.md`, `docs/ROADMAP.md`, and `docs/AI_ARCHITECT_CONTEXT.md`.

## Milestone PB-02A — Learning Scene Framework


- Created `docs/LEARNING_SCENE_FRAMEWORK.md` defining the canonical interaction framework and contract for all Learning Scenes.
- Defined 12 core framework concepts: What is a Learning Scene, Responsibilities, Non-responsibilities, Scene Lifecycle, Input Contract, Output Contract, Scene Context, Scene State, Scene Events, Scene Result, Scene Completion, and Scene Cancellation.
- Established 9 architectural scene categories: Teaching Scene, Practice Scene, Assessment Scene, Review Scene, Reflection Scene, Challenge Scene, Motivation Scene, Recovery Scene, and Transition Scene.
- Specified the Scene Lifecycle State Machine (`Created` → `Prepared` → `Running` → `Paused` → `Resumed` → `Completed` / `Cancelled` → `Disposed`) with Mermaid diagram.
- Defined explicit input/output data contracts and invariant prohibitions (Scenes never decide strategy, scheduling, persistence, or profile mutations).
- Defined subsystem authority matrix across Product Brain, Learning Flow Engine, Learning Scene, and Presentation Layer (Compose Desktop, Android, iOS, Web).
- Updated `README.md`, `docs/PROJECT_HANDOFF.md`, `docs/SYSTEM_OVERVIEW.md`, `docs/ROADMAP.md`, and `docs/AI_ARCHITECT_CONTEXT.md`.

## Milestone PB-01.8 — Learning Experience Architecture


- Created `docs/LEARNING_EXPERIENCE_ARCHITECTURE.md` defining the architecture of a complete study session from "Start Learning" to "Session Complete".
- Defined 7 session phases: Warm-up, Teaching Phase, Practice Phase, Challenge Phase, Review Phase, Reflection Phase, and Session Summary & Completion.
- Detailed 5-subsystem orchestration rules across Product Brain, Knowledge Model, Learning Strategy, Scheduler (FSRS), and Learning Scenes.
- Defined session runtime contracts: Session State, Session Context, Experience Flow, Adaptive Transitions, User Motivation Management, and Session Termination / Interruption Recovery.
- Provided complete session journey walkthroughs for Vocabulary, Interactive Story, Medical Physics, Language Learning, and General Knowledge domains.
- Updated `README.md`, `docs/PROJECT_HANDOFF.md`, `docs/SYSTEM_OVERVIEW.md`, `docs/ROADMAP.md`, and `docs/AI_ARCHITECT_CONTEXT.md`.

## Milestone PB-01.5 — Knowledge Model Specification


- Created `docs/KNOWLEDGE_MODEL.md` defining the canonical, subject-independent Knowledge Model specification.
- Defined 15 core knowledge concepts: Knowledge World, Topic, Module, Lesson, Concept, Knowledge Unit, Learning Asset, Learning Relationship, Difficulty Metadata, Prerequisite, Learning Dependency, Semantic Tag, Objective Mapping, Content Metadata, and Evidence Mapping.
- Provided universal domain mappings for Vocabulary, Interactive Stories, Medical Physics, Language Courses, and Technical Courses.
- Detailed subsystem interaction boundaries showing how Product Brain reads the model, how Learning Scene consumes assets, and how Scheduler remains strictly independent.
- Included Mermaid diagrams for Structural Model Hierarchy and Runtime System Data Flow.
- Updated `README.md`, `docs/PROJECT_HANDOFF.md`, `docs/SYSTEM_OVERVIEW.md`, `docs/ROADMAP.md`, `docs/AI_ARCHITECT_CONTEXT.md`, and cross-referenced `docs/PRODUCT_BRAIN_SPECIFICATION.md`.

## Milestone PB-01 — Product Brain Specification


- Created `docs/PRODUCT_BRAIN_SPECIFICATION.md` defining the official architectural specification and blueprint for the AI Teacher (`ProductBrain`).
- Defined the 17 core pedagogical concepts: Learner Profile, Learning Goal, Teaching Goal, Knowledge Model, Content Semantics, Session Context, Teaching Strategy, Learning Scene, Difficulty Adaptation, Motivation, Fatigue, Confidence, Mastery, Learning Evidence, Teaching Outcome, Session Reflection, and Long-Term Learner Model.
- Designed the complete 10-step Teaching Loop (Diagnosis → Goal → Strategy → Template → Instantiation → Experience → Evidence → Reflection → Model Update → Planning).
- Established explicit subsystem responsibility matrix across Product Brain, Learning Flow Engine, Scheduler (FSRS), Presentation UI, and Shared Core.
- Established 6 core Product Brain principles and multi-year evolutionary roadmap.
- Updated `README.md`, `docs/PROJECT_HANDOFF.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, and `docs/AI_ARCHITECT_CONTEXT.md`.

## PB-00 — Repository Constitution & Product DNA


- Established the repository knowledge system, Product Philosophy, Repository Constitution, System Overview, Product Brain conceptual framework, Cross-Platform Strategy, AI Design Rules, and Architectural Decision Records (ADRs).
- Added `docs/PRODUCT_PHILOSOPHY.md` defining Learning Engine 2.0 as an adaptive Teaching Engine (not an Anki clone).
- Added `docs/PRODUCT_BRAIN.md` defining the conceptual teaching loop (Learner Model → Objective → Strategy → Scene → Response → Evidence).
- Added `docs/LEARNING_PRINCIPLES.md` defining 7 immutable pedagogical principles for guided learning.
- Added `docs/CROSS_PLATFORM_STRATEGY.md` defining the shared Kotlin core strategy across Desktop, Android, iOS, and Web.
- Added `docs/SYSTEM_OVERVIEW.md` providing a high-level architecture map of all core subsystems.
- Added `docs/REPOSITORY_CONSTITUTION.md` establishing non-negotiable architectural laws governing decision ownership.
- Added `docs/AI_DESIGN_RULES.md` establishing mandatory MUST and MUST NOT guidelines for future AI working sessions.
- Added `docs/adr/ADR-0001` through `ADR-0004` defining key architectural decisions.
- Updated `README.md`, `docs/PROJECT_HANDOFF.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, and `docs/AI_ARCHITECT_CONTEXT.md`.
- Established mandatory 10-step onboarding reading order in `docs/PROJECT_HANDOFF.md`.

## Learning Objectives, Strategies, and Flow Templates Foundation


- Refactored `LearningFlowTemplateStage` to contain semantic template slots (`ROTATED_PRIMARY`, `OPTIONAL_TYPING`, `ANSWER_REVEAL`, `RATING_READY`) rather than concrete `ExperienceSelectionResult` objects.
- `LearningFlowTemplate` is now fully immutable, deterministic, and reusable across items, sessions, and rotation context.
- `LearningStrategyDefinition` describes strategy behavior semantically (`includeOptionalTyping`) without holding template slots or concrete selections.
- `LearningFlowTemplateFactory` translates `LearningStrategyDefinition` into reusable template slots with zero `LearningExperiencePlan` dependency.
- `LearningFlowInstantiationService` resolves runtime experience selections for template slots and delegates to `LearningFlowPlanner` to produce `LearningFlowDefinition`.
- `ProductBrainPlanner` acts as an orchestration-only boundary coordinating Objective → Strategy → Template → Instantiation.
- Simplified `DesktopLearningFlowCoordinator` to depend strictly on `ProductBrainPlanner` and `LearningFlowController`.
- Added comprehensive architecture tests for template immutability, rotation independence, resolver non-mutation, and strict component dependency boundaries.


## Learning Flow Engine Foundation and Desktop Multi-stage Vertical Slice

- Added platform-neutral immutable flow definition, experience/reveal/rating-ready stages,
  deterministic planner, pure controller, semantic transitions, and actionable-stage progress.
- V1 plans the session-rotated automatic primary experience, then eligible Typing, then the
  authoritative answer reveal and manual-rating boundary. Typing is never the automatic first
  stage.
- Desktop `StudyViewModel` owns one transient coordinator keyed by real session/item identity.
  Continue completes Image/Listening/Prompt; completed Typing advances once; the controller
  requests reveal and `StudyFacade` remains its sole authority.
- Removed the conflicting Default/Typing chooser from active flow presentation. Added localized
  English/Vietnamese progress, stage labels, Continue, reveal-pending, and rating-ready copy.
- Flow state is not persisted. Pause/resume in the same runtime retains it; restart reconstructs
  stage one, or safe rating-ready when the authoritative session already records reveal.
- Scheduler, FSRS, rating meanings, review transaction, queue, undo domain behavior,
  persistence/import/package/media contracts, and Typing evaluation are unchanged.

## Session-aware Experience Rotation Foundation

- Activated deterministic Default-mode rotation over passive Image, Listening, and Prompt
  options while retaining full policy eligibility and explicit Typing `USER_CHOICE`.
- Added an immutable session/item rotation context derived from stable zero-based queue position.
  Reveal, retry, recomposition, pause/resume, and Typing-to-Default retain the current context;
  undo follows the rewound queue position and active-session restart reconstructs it.
- Added shared profile/selection/context tests and Desktop integration coverage. No rotation
  field, schema, scheduler, queue rule, rating, import, package, or media behavior changed.

## Typing Recall Submission Hardening

- Empty or whitespace-only Typing submissions now retain `EMPTY` feedback without revealing the
  answer, rating, or advancing the session; editing clears that evaluation and permits retry.
- Shared evaluation exposes completed-attempt semantics. Desktop submission returns a semantic
  outcome, reveals only for non-empty correct/incorrect attempts, rejects submissions while an
  action is in progress, and prevents a completed attempt from revealing twice.
- Eligibility, option order, extraction, normalization, scheduler/review, persistence, and
  package behavior are unchanged.

## Typing Recall Vertical Slice Foundation

- Added `TYPING_RECALL` as a real shared experience kind when semantic answer content contains at
  least one non-blank text block. Canonical Image, Listening, Prompt, Typing order preserves the
  existing ordinal-zero experience.
- Added deterministic expected-answer extraction plus conservative locale-stable evaluation:
  trim, whitespace collapse, and case-insensitive exact matching without punctuation,
  diacritic, symbol, or word-order removal.
- Activated Desktop `TypingScene` projection, an explicit per-item Default/Typing chooser,
  transient identity-keyed input/evaluation, localized accessible feedback, Enter/visible
  submission, and reveal through the existing lifecycle. Ratings remain manual.
- Added shared and Desktop coverage for eligibility, extraction, evaluation, selection,
  projection, state reset, feedback semantics, and focused-shortcut suppression. Scheduler,
  FSRS, queue, evidence, persistence, JSON, PKG, OPD3, import, and media behavior are unchanged.

## Experience Selection Framework Foundation

- Split shared eligibility from final selection: policy now returns canonical ordered,
  non-empty experience options and no longer owns a primary kind.
- Added semantic selection request, result, reason, strategy, and engine contracts plus a
  stateless floor-mod round-robin implementation for arbitrary positive, large, or negative
  ordinals.
- Kept production Desktop in explicit ordinal-zero compatibility mode and changed scene
  projection to consume the authoritative selection result without re-running eligibility,
  ordinal normalization, or selection.
- Added policy/options, round-robin, engine invariant, dependency boundary, Desktop mapping,
  missing-media, compatibility, audio, keyboard, and full regression evidence. No persisted or
  user-visible rotation was activated.

## Shared Learning Experience Policy Foundation

- Moved Image > Audio > Prompt experience selection from Desktop into the root Application
  `learningexperience` package over semantic `LearningContent`.
- Added platform-neutral experience kind, capabilities, reveal context, supporting roles, plan,
  and deterministic policy without Compose, Desktop, filesystem, Path, playback, or mutation.
- Replaced Desktop selection with `DesktopLearningSceneProjector`, which trusts the shared plan
  and combines it with resolved/localized presentation blocks, including missing-media fallback.
- Added shared policy, dependency-boundary, Desktop projector, audio lifecycle, keyboard, and
  full regression evidence without changing scheduler/session/review/persistence/package behavior.

## Adaptive Learning Scenes Foundation

- Added a transient Desktop `LearningScene` contract with Prompt, Listening, Image, Meaning,
  Example, and inert Typing scene types plus explicit context and capability values.
- Added a deterministic rule-based experience generator: prompt image takes scene priority,
  then prompt audio, then plain prompt; revealed meaning and examples become supporting scenes.
- Replaced direct section rendering with scene rendering, including scene-specific instruction,
  media-first Image/Listening composition, localized English/Vietnamese copy, and scene-bound
  audio replay/cancellation.
- Preserved every scheduler, queue, rating, evidence, persistence, import, JSON, and PKG contract.

## Desktop Learning Experience Alpha

- Replaced the dashboard-like ACTIVE Learn composition with a centered, bounded focus workspace
  where prompt, semantic audio, media, reveal, answer/examples, and ratings own the visual order.
- Suppressed branding, sidebar, technical status, and dashboard metrics only during an ACTIVE
  Learn destination; compact progress, Undo, Pause, keyboard behavior, and engine ownership remain.
- Made reveal visually prominent, made all four rating decisions equivalent, enlarged semantic
  content/media, and added deterministic focus-shell presentation coverage plus an exact Product
  Owner audio/workspace UAT checklist.

## Desktop semantic audio reliability

- Added real MP3 decoding to the Desktop Java Sound runtime and streamed decoded PCM through a
  single cancellable output instead of relying on the JRE's unsupported bare MP3 path.
- Centralized semantic audio playback outside Compose buttons with observable start/play/failure
  state, role-specific controls, primary-audio replay on `R`, safe cancellation on item/session
  transitions, and stale-callback protection.
- Added real-decoder tests for MP3 files under spaced Unicode paths plus output-boundary,
  cancellation, failure, semantic-role, and shortcut coverage. Physical speaker output remains
  an explicit Product Owner UAT gate.

## Platform-Independent Learning Product Specification

- Defined the full learner journey from entry/resume through scope, setup, card phases,
  completion, summary, and interruption recovery.
- Added platform-independent Study Workspace, product behavior, interaction, semantic media,
  and learner-facing topic-model specifications under `docs/spec/`.
- Reordered post-1.0 work from subsystem-first DP items into outcome-based LX-01 through LX-11,
  placing session entry/setup and focused workspace before media automation and exercise modes.
- Classified mandatory product/architecture/release blockers separately from optional gestures,
  notes, TTS, search overlays, preferences, and delight work.
- Preserved Learning Engine authority and all current Desktop 1.0 external evidence gates; no
  production behavior changed.

## Android Product Reverse Engineering & Desktop Product Architecture

- Catalogued the complete supplied Android Activity/XML/video reference into stable behavior
  IDs with source, UI, video, Desktop, ownership, roadmap, and decision traceability.
- Separated Android learning algorithm behavior from product interaction and retained Learning
  Engine as scheduler, queue, session, persistence, recovery, and undo authority.
- Added the Desktop gap analysis, better-than-Android vision, subsystem architecture with eight
  decisions, independently deliverable product roadmap, and technical-debt register.
- Synchronized strategic handoff, short-term context, roadmap, architecture, capability map,
  and test matrix without changing production behavior.

## Real-data Desktop responsiveness remediation

- Moved import, library/query refresh, study preparation, review persistence, dashboard,
  statistics, and history refresh work off the Compose event thread with immediate typed busy
  states and duplicate-action guards.
- Connected honest package-import phases to Desktop presentation; deterministic counts are shown
  when available and progress is capped below 100% until the transaction has committed.
- Removed content-library and study-planner N+1 persistence scans through bulk snapshots. On the
  2,425-content/12,125-item production-boundary harness, library query improved from 26,981 ms
  to 95 ms and study preparation from 141,876 ms to 315 ms in the final clean run.
- Virtualized lesson rows with stable keys, memoized projection, debounced search, and lazy
  bounded thumbnails with strict visible-row loading, 96 px decode bounds, and a 64-entry LRU.

## Real JSON + OPD3 PKG pair import remediation

- Replaced extension-only `.pkg` routing with a four-byte signature boundary: `OPD3` selects
  the binary-pair importer and supported ZIP signatures retain existing archive behavior.
- Connected the existing legacy JSON importer and OPD3 media reader to Desktop composition,
  with deterministic case-insensitive same-basename JSON discovery and explicit missing or
  ambiguous pair failures.
- Hardened binary index validation for entry count, strict UTF-8 names, media types, offsets,
  lengths, overlap, bounds, and CRC; malformed candidates remain pre-persistence.
- Added persisted evidence for Unicode/spaced paths, installed-library discovery, media
  extraction, learning-item availability, and session startup while retaining existing formats.

## Windows native launcher accessibility runtime fix

- Reproduced jpackage's `Failed to launch JVM` with the real native executable and captured the
  underlying `ClassNotFoundException` for `com.sun.java.accessibility.AccessBridge`.
- Added `jdk.accessibility` to the Compose Desktop runtime image; the full external Temurin JDK
  worked previously because it already contained that module, while the minimized jlink image
  did not.
- Added an isolated native-launcher startup mode and `verifyWindowsLauncher` Gradle task that
  exercises an accessibility-enabled user profile, bundled runtime, exit code, timeout, and
  captured diagnostics without mutating real user data.
- Wired the launcher smoke gate into the existing Windows Beta verification script. No Compose,
  Kotlin, Material, lifecycle, saved-state, or other dependency version was changed.

## Desktop 1.0 release-candidate preparation

- Audited repository release boundaries after P6-09 and retained the frozen Domain/Application/
  Infrastructure/Desktop ownership without adding product scope.
- Fixed malformed recovery manifests whose negative declared file count could previously be
  interpreted as an empty inventory and reach whole-snapshot restore mutation.
- Required declared backup file counts to be non-negative and equal the archive payload count
  before inventory allocation, safety backup creation, or durable-data mutation; added focused
  failure-before-mutation regression coverage.
- Revalidated the complete test, Desktop compile, and non-interactive packaging gates. Manual,
  clean-machine, installer, upgrade/uninstall, signing, and real-user evidence remains pending.

## P6-09 — Desktop End-to-End Verification, Defect Remediation & Release Evidence

- Added a deterministic persisted integration path from OPD3 import through installed-package
  discovery, Desktop global study, reveal/rating, progress, restart, completion, final-review
  undo, re-rating, and recovered completion.
- Confirmed the frozen Phase 6 architecture is sufficient: no production contract, schema,
  public API, scheduler, or persistence implementation change was required by the defect hunt.
- Consolidated automated evidence and the 40-step Product Owner manual checklist in
  `TEST_MATRIX.md`; manual UI and Phase 5 clean-machine/install/upgrade/signing evidence remain
  explicitly pending.

## P6-08 — Desktop Accessibility, Keyboard Navigation, Error Recovery & Release Polish

- Centralized state-aware keyboard routing for reveal, ratings, retry, undo, and pause/leave,
  including busy/repeat/text-input suppression and a shared ViewModel action guard.
- Added deterministic focus transitions, localized English/Vietnamese action labels, accessible
  Undo/Pause/media/Markdown semantics, and non-color fallback presentation.
- Classified Desktop recovery failures without exposing raw exception text and preserved the
  last confirmed workspace state on review/undo errors.
- Verified P6-07 completion reopening, progress rollback, second-undo blocking, and restart
  projection through Desktop integration coverage; manual visual/assistive evidence remains P6-09.

## P6-07 — Pause, Resume, One-Step Undo & Safe Interruption

- Kept pause outside the domain lifecycle and continued restart recovery of the same `ACTIVE`
  session without reapplying a staged review.
- Added a durable, backward-compatible latest-review checkpoint and Application-owned atomic
  undo across review history, scheduler memory, queue, session counters/current item, progress,
  and final-session reopening.
- Added Desktop projection and action wiring for undo without moving scheduling or persistence
  rules into Compose.
- Added focused first-review, idempotency, completion-reopen, persistence-restart, mapper, and
  lifecycle regression coverage.

## Repository Self-Onboarding & Desktop 1.0 Continuation Handoff

- Consolidated the verified P6-01 through P6-06 continuation point across the strategic handoff,
  operational context, roadmap, architecture, capability map, and test matrix.
- Marked the session, workspace, content, rich-renderer, and progress/completion boundaries as
  stable for Desktop 1.0 unless a concrete defect or accepted use case requires tested change.
- Recorded fixed Domain/Application/Desktop ownership, closed product decisions, known debt,
  remaining P6-07 through P6-09 work, Phase 7 release validation, and external Phase 5 evidence.
- Kept workflow policy solely in `AGENTS.md` and used existing documents instead of creating a
  duplicate onboarding or Desktop-status file.

## P6-06 — Progress, Completion & Learning Feedback

- Added renderer-neutral `LearningSessionProgress`, projected from the durable `StudySession`
  review counts and immutable persisted queue position rather than Desktop counters.
- Distinguished processed, reviewed, skipped, remaining, total, and current-position semantics.
  The current queued runtime has a stable known total; the legacy no-queue path explicitly uses
  unknown-total semantics instead of fabricating a percentage.
- Returned authoritative progress with next-item and successful-review results. Pending or failed
  reviews do not advance queue progress, while recovery resumes one intent atomically.
- Corrected completion reporting when queue eligibility skips planned sibling/stale items: all
  planned items may be processed while only committed reviews are reported as reviewed.
- Preserved final queue progress through the completed-queue recovery result so a restart after
  the last atomic review but before normal finalization projects the Completed workspace.
- Generalized the existing lesson progress card to every active queued session and strengthened
  completion/screen-reader summaries. Existing scheduler-result feedback remains concise,
  ephemeral, and derived from the committed review result without Desktop rescheduling.
- Added no gamification, rewards, long-term analytics, or new persistence fields.

## P6-05 — Rich Content Renderer

- Added a Desktop presentation adapter for the ordered P6-04 Question, Answer, and Example
  blocks. Workspace state alone controls reveal visibility; the renderer owns no learning action.
- Added an allowlist Markdown renderer for paragraphs, line breaks, headings, lists, emphasis,
  inline code, and fenced code. HTML, links, remote resources, and executable content remain inert.
- Resolved images and audio only through the existing local `ContentMediaStorage` boundary, with
  localized deterministic fallbacks for missing, corrupt, and unsupported assets.
- Added aspect-ratio-preserving image presentation and explicit play/stop audio controls. Audio
  never autoplays and stops when the item/state changes or the renderer leaves composition.
- Preserved legacy plain-string Study views and introduced no package, persistence, scheduler,
  lifecycle, or public domain contract changes.

## P6-04 — Learning Content Model

- Kept `Content` as canonical domain truth and added a renderer-neutral Application projection:
  ordered Question, Answer, and optional Example sections containing Text, Markdown, Image,
  Audio, or Unavailable Asset blocks.
- Added explicit plain-text/Markdown source formats without renderer styling or Compose types.
  Legacy package and persistence records default to plain text.
- Standardized local relative asset references. Unsafe/remote references and locally unresolved
  assets project to deterministic fallback blocks; Core never fetches or executes content.
- Preserved media, text formats, title, tags, and source through `ContentRecord`, fixing the
  learner-content loss that would otherwise occur after persisted restart.
- Changed Desktop Study to receive `LearningContent` from `NextLearningItem` rather than invent
  a renderer-side content structure; the existing plain-string fields remain compatibility views.

## P6-03 — Review Workspace State & Action Boundary

- Introduced deterministic Desktop states for Idle, Preparing, Question, Answer Revealed,
  Feedback, Transitioning, Completed, and Recoverable Failure.
- Question permits reveal only; Answer Revealed permits ratings in `AGAIN`, `HARD`, `GOOD`,
  `EASY` order. Invalid and repeated actions fail before application mutation.
- Routed keyboard decisions through the workspace contract while retaining existing
  `StudyUiState` booleans as compatibility projections.
- Added restart, double-reveal, pre-reveal rating, transition-order, and recovery tests.

## P6-02 — Learning Session Lifecycle & Recovery Contract

- Made `StudySession` authoritative for the durable current item, presentation time, reveal
  state, and one pending review intent while retaining only `ACTIVE` and `FINISHED` statuses.
- A review now persists one stable intent before atomically updating the review event, memory
  state, session, and queue. Startup replays an interruption with the original event ID.
- Desktop persists reveal through `LearningEngine` and restores reveal/timing without owning
  business lifecycle state. Pause remains resume of `ACTIVE`, not a domain state.
- Extended schema-v1 session JSON with optional/defaulted checkpoint fields and added lifecycle,
  interruption/replay, compatibility, and mapping coverage.

## P6-01 — Define Phase 6 Learning Experience

- Defined Learning Experience as the next product Phase without closing Phase 5's outstanding
  external verification gate.
- Established learner outcomes, source-grounded problem statement, ordered capability sequence,
  architectural constraints, out-of-scope work, acceptance/exit criteria, and open decisions.
- Identified P6-02 as Study Session lifecycle and recovery contract based on the existing
  `ACTIVE`/`FINISHED`, persisted queue, restart recovery, and transient Desktop state boundary.
- Reframed the product vision around an adaptive learning platform and a measurable learner
  North Star rather than a flashcard-only application.

## Phase 5 — Reproducible Windows Beta verification harness

- Added a PowerShell harness that runs clean tests plus MSI/EXE packaging with a full JDK 21.
- Added artifact name, byte-size, SHA-256, Authenticode status, OS/JDK, and UTC evidence output.
- Added a real Unicode-path UTF-8 read/write probe under the evidence directory.
- Added opt-in disposable-machine MSI install/uninstall and previous-MSI upgrade automation;
  these operations never run by default.
- Added a release checklist separating repository/local evidence from Product Owner
  clean-machine, primary-flow, uninstall-data, reinstall, signing, and upgrade evidence.
- Local non-install verification passed; artifacts were correctly reported as unsigned.

## Phase 5 — First-run onboarding and starter content

- Added restart-stable first-run detection that prompts only an empty profile; existing durable
  data skips onboarding without writing a marker.
- Added localized choices to continue with an empty library or install a two-item retrieval and
  spacing starter lesson.
- Installed starter content through the production OPD3 scanner, validation, transaction, and
  persisted package-registration boundary rather than direct repository writes.
- Kept the sample optional and deleted its temporary import archive after each attempt.
- Added first-run/restart/existing-user tests and a persisted OPD3 import/restart integration
  test proving the sample is browsable through Content Library.

## Phase 5 — Manual durable-state backup and restore

- Added manual ZIP snapshots covering Desktop data and configuration while excluding logs,
  temporary files, diagnostic exports, and prior backups.
- Added a versioned manifest with UTC timestamp and sorted file inventory, sizes, and SHA-256
  checksums; existing backup targets are never overwritten or retained automatically.
- Restore validates archive names, inventory, compatibility, sizes, and checksums before any
  mutation, then replaces the complete snapshot without merge.
- Added an automatic pre-restore safety backup and exact-byte rollback on replacement failure.
- Blocked restore while a persisted Study session is active; persistence operations are
  synchronous on the same Desktop UI boundary and cannot overlap the restore callback.
- Added localized manual backup controls and an explicit destructive restore confirmation that
  closes the application after success.
- Added deterministic backup, exclusion, validation, checksum, active-session, replacement,
  safety-backup, non-overwrite, and injected rollback-failure coverage.

## Phase 5 — Privacy-preserving diagnostic export

- Added deterministic UTF-8 support exports containing only the existing redacted runtime
  diagnostic snapshot, with no timestamps or persisted learning content.
- Added same-directory temporary writes and atomic placement where supported.
- Refused missing parent directories and existing targets, preventing implicit overwrite or
  directory creation outside the established runtime contract.
- Added a native save-file flow and localized export status inside the About dialog.
- Added focused content/privacy and non-destructive existing-target regression coverage.

## Phase 5 — Windows distributable packaging foundation

- Added Compose Desktop native distribution configuration for Windows MSI and EXE artifacts.
- Derived a three-part package version deterministically from the established root project
  version while retaining the existing runtime build version contract.
- Added generated distribution metadata and a typed loader validating package identity,
  version shape, and supported Windows formats.
- Verified the unpacked application image plus `LearningEngine-1.0.0.msi` and
  `LearningEngine-1.0.0.exe` with a full JDK 21 `jpackage` toolchain.
- Added focused metadata success, missing-field, invalid-version, and unsupported-format tests.

## Workflow evolution — Phase-based continuous delivery

- Made Phase the primary planning unit and capability the independently verified commit unit.
- Required automatic continuation from one completed capability to the next until the current
  Phase Definition of Done or an established stop condition is reached.
- Reframed the roadmap into seven durable Phases while preserving verified milestone history.
- Added explicit current Phase, current capability, continuation evidence, and Phase completion
  context to the durable and short-term handoff documents.

## Milestone 7.6 — About dialog and startup transition

- Added a localized startup presentation with a deterministic one-way transition into the
  fully composed main shell.
- Replaced the implicit Settings-only About presentation with an explicit accessible dialog.
- Kept About data sourced exclusively from the immutable, redacted Milestone 6 runtime
  diagnostic snapshot.
- Added pure startup-transition and About-presentation coverage.
- Completed Milestone 7 — Desktop UX Foundation without installer or distributable packaging.

## Milestone 7.5 — Shell keyboard and focus traversal

- Added deterministic navigation/content focus regions with Ctrl+F6 forward and
  Ctrl+Shift+F6 backward traversal.
- Kept unmodified F6 as the Settings destination shortcut and retained native Tab/Shift+Tab
  traversal inside each Compose focus group.
- Extended the global accessibility hint from the same shortcut contract.
- Added focused action-resolution and cyclic focus-state coverage.

## Milestone 7.4 — English/Vietnamese localization foundation

- Added typed English and Vietnamese locale preferences to schema-v1 Desktop configuration,
  with English as the compatible default for existing files.
- Added one deterministic localization catalog for shell navigation, Settings headings, theme
  choices, and language choices.
- Connected Settings language selection to immediate UI updates and persisted restart behavior.
- Kept stable route IDs and the existing English destination labels as compatibility contracts.
- Added catalog-completeness and configuration restart coverage.

## Milestone 7.3 — Configurable Desktop theme

- Added Light, Dark, and System theme preferences to the typed Desktop runtime configuration.
- Kept schema-v1 files from Milestone 6 compatible by treating an absent theme as System.
- Added atomic configuration replacement for explicit Settings changes; invalid files remain
  non-destructive startup failures and are never overwritten implicitly.
- Connected Settings theme controls to live Compose theming and persisted restart behavior.
- Added deterministic preference-resolution, configuration restart, and invalid-value tests.

## Milestone 7.2 — Safe Desktop window placement restart

- Added a separate schema-v1 window-state contract for size, optional absolute position, and
  maximized state.
- Added safe bounds for restored/captured size and coordinates plus centered defaults.
- Missing state is writable; corrupt/unsupported/unsafe state falls back in memory while its
  original file is preserved and auto-save is disabled for that session.
- Writes use a same-directory temporary file and atomic replacement when supported.
- Wired Compose window state to restore at startup and capture once on close, with restart and
  corrupt-file regression coverage.

## Milestone 7.1 — Centralized Desktop shell navigation vocabulary

- Standardized the central destination registry on Home, Learn, Statistics, Review, Library,
  and Settings without removing the verified Statistics capability.
- Added stable route identifiers independent of visible labels.
- Kept sidebar text, selected-state accessibility, function-key navigation, cyclic traversal,
  and shortcut guidance derived from the same ordered registry.

## Milestone 6.6 — Redacted runtime diagnostics and About presentation

- Added an immutable runtime diagnostic snapshot covering identity, version/build metadata,
  OS/JVM details, selected directories, current log, and legacy-data mode.
- Added deterministic support-summary ordering and user-home path redaction.
- Passed the snapshot through Desktop composition into a visible, accessible About and Support
  section without reading global runtime state from UI code.
- Added diagnostic redaction/order tests and a two-session restart regression proving config
  bytes survive while bounded session logs remain complete.
- Completed Milestone 6 — Desktop Runtime Foundation without installer, distributable, or data
  migration behavior.

## Milestone 6.5 — Desktop startup and shutdown lifecycle

- Added one runtime session that owns resolved directories, build metadata, typed configuration,
  the persisted application context, and the session logger.
- Startup creates only declared runtime directories, then loads configuration, opens logging,
  and composes persistence in deterministic order.
- Startup composition failures preserve the original exception, log only its type, close the
  logger, and attach cleanup failures as suppressed context.
- Desktop entry wiring now starts before Compose and closes once in `finally`; repeated close is
  safe and emits one shutdown event.
- Added lifecycle ordering, directory creation, persistence-path, failure, privacy, and
  idempotent-shutdown tests.

## Milestone 6.4 — Desktop file logging and retention

- Added a dependency-free UTF-8 per-session file logger with typed levels and validated event
  codes.
- Flushes every accepted event, normalizes multiline messages, and rejects logging after close.
- Enforces configured retention by deleting only oldest regular files matching the exact
  Learning Engine log namespace.
- Preserves unrelated files and does not follow symbolic links during retention selection.
- Added deterministic clock/session tests for filenames, filtering, Unicode, normalization,
  idempotent close, and retention.

## Milestone 6.3 — Non-destructive typed Desktop configuration

- Added schema-v1 typed runtime configuration for log level and retained log-file count.
- Missing configuration resolves to in-memory defaults without creating a file.
- Existing blank, incomplete, unsupported-schema, invalid-enum, and out-of-range configuration
  fails with file/property context.
- Diagnostic messages omit property values, and repeated loads preserve corrupt bytes and
  directory contents exactly.

## Milestone 6.2 — Platform-aware Desktop runtime directories

- Added a typed contract for distinct user data, configuration, cache, log, and temporary
  directories.
- Added deterministic Windows, macOS, and Linux/XDG resolution with filesystem-valid fallbacks.
- Wired Desktop persistence to the resolved data directory.
- Preserved an existing `~/.learning-engine/data` directory in place when present; no data is
  copied, moved, renamed, or migrated.
- Kept path resolution side-effect free; creation remains owned by the startup lifecycle.

## Milestone 6.1 — Desktop application identity and build metadata

- Added one stable Desktop application identity for application ID, display name, and
  filesystem-safe directory name.
- Replaced the duplicated window-title literal with the shared identity contract.
- Added generated classpath build metadata for application version, build channel, revision,
  and build number.
- Made Gradle properties override channel/revision/build number while retaining deterministic
  local defaults and no generated timestamp.
- Added validation, classpath-loading, missing-property, identity, and display-version tests.

## Workflow Foundation Refinement

- Made root `AGENTS.md` the sole authority for AI workflow, testing/build policy, documentation
  ownership, Git safety, product decisions, stop conditions, and reporting.
- Reduced `PROJECT_HANDOFF.md` to durable product, architecture, roadmap, debt, completion, and
  source-of-truth context.
- Reduced `AI_ARCHITECT_CONTEXT.md` to the current repository, milestone, test, and risk
  snapshot without standing policy or continuation instructions.
- Added `MILESTONE_HISTORY.md` as the concise official milestone ledger.
- Removed duplicate batch workflow from the roadmap and replaced cross-document rule copies
  with links to the owning document.

## Batch88 — Representative large persistence restart correctness

- Added a deterministic 5,000-record memory-state snapshot fixture through the real JSON
  codec, durable writer, reader, and recreated store boundary.
- Verified exact record count, ordering, nullable values, representative middle boundaries,
  and complete object equality after restart-equivalent recreation.
- Avoided environment-sensitive wall-clock thresholds; the regression checks correctness and
  allocation-representative behavior rather than claiming a benchmark.
- Completed the Persistence Integrity & Recovery milestone at its approved non-destructive
  boundary.

## Batch87 — Corruption-safe transaction rollback and restart

- Verified that transaction snapshots remain opaque bytes even when the pre-transaction JSON
  target is corrupt.
- Verified a failed transaction restores the exact corrupt pre-state rather than replacing it
  with a newly serialized empty or valid snapshot.
- Verified the original operation failure remains primary and a newly created store reports the
  same contextual corruption after rollback.
- Preserved the existing in-process transaction contract without introducing a crash journal,
  migration, or automatic repair policy.

## Batch86 — Explicit interrupted-write artifact contract

- Defined stale JSON temporary files as inert artifacts rather than implicit recovery sources.
- Confirmed reads use only the canonical target and never promote a neighboring `.tmp` file.
- Confirmed later writes use an independent unique candidate and clean only their own temporary
  file, leaving pre-existing artifacts byte-for-byte unchanged.
- Avoided ambiguous automatic cleanup, quarantine, restore, or migration behavior until an
  approved recovery policy and trusted recovery source exist.

## Batch85 — Crash-safer JSON snapshot replacement

- Kept temporary files on the target filesystem and durable file-channel flushing before
  replacement.
- Restricted non-atomic replacement fallback to the explicit
  `AtomicMoveNotSupportedException` signal instead of retrying every atomic-move I/O failure.
- Preserved the previous valid target and propagated the original move failure when atomic
  replacement fails unexpectedly.
- Preserved the previous target when the explicit fallback also fails, propagated the fallback
  failure, retained the atomic failure as suppressed context, and cleaned the candidate temp.
- Added deterministic fault-injection coverage for atomic failure, fallback success, fallback
  failure, previous-snapshot preservation, and temporary-file cleanup.

## Batch84 — Non-destructive corrupt persistence reads

- Verified that corrupt persisted bytes remain unchanged after repeated failed reads through
  newly created JSON store instances.
- Verified that failed reads do not update the target timestamp or create recovery artifacts.
- Confirmed stable failure kind and record/file context across restart-equivalent store
  recreation without leaking persisted values in messages.
- Kept recovery deliberately observational: no automatic rewrite, delete, quarantine, or
  reset behavior was introduced.

## Batch83 — Classified corrupt JSON persistence reads

- Kept a missing persistence file as the only implicit empty-store initialization state.
- Rejected existing blank or whitespace-only files instead of silently treating possible
  truncation as an empty dataset.
- Added stable `BLANK`, `MALFORMED`, `TRUNCATED`, and `INVALID_SHAPE` failure kinds while
  retaining the established exception message and original serializer cause.
- Added record-type and file-path context without exposing persisted content in diagnostics.
- Applied the shared read boundary to every JSON store.

## Batch82 — Contextual required OPD3 JSON value shapes

- Routed invalid optional metadata value shapes through `InvalidPackageJsonException`.
- Preserved `metadata.json` entry context and the original JSON accessor message.
- Verified wrong manifest, metadata, contents, and learning-item root-field shapes through one
  focused regression matrix.
- Preserved omitted optional metadata compatibility and all established identity-validation
  messages.
- Completed the Package Import & OPD3 Robustness track after archive, text, required-entry,
  JSON, validation, diagnostic, transaction, restart, and Desktop flow hardening.
- Selected corrupt/interrupted persisted-data recovery as the next Milestone 5 capability area.

## Batch81 — Contextual malformed required OPD3 JSON

- Added `InvalidPackageJsonException` with structured required-entry context.
- Applied the boundary to descriptor manifest decoding and all four bundle JSON inputs.
- Preserved the original parser message exactly as the exception and Batch76 failure message.
- Kept manifest compatibility and metadata identity validation outside the parse wrapper so
  established validation messages remain unchanged.
- Preserved `MALFORMED_PACKAGE`, `PACKAGE_MALFORMED`, failure-before-persistence, and detailed
  directory continuation behavior.
- Added focused coverage for each required entry, descriptor decoding, diagnostic mapping, and
  parser-message compatibility.

## Batch80 — Shared missing required-entry contract

- Added `MissingRequiredPackageEntryException` with structured entry-name context.
- Routed modern bundle missing-file failures through the package-import exception hierarchy.
- Made existing manifest and legacy-content exceptions specialized subtypes of the shared
  contract without changing their messages.
- Preserved the modern bundle `IllegalArgumentException` type relationship and exact legacy
  `Missing package file: <name>` message.
- Preserved Batch76 malformed-package classification, diagnostic code, non-fail-fast behavior,
  and failure-before-persistence boundary.
- Added focused exception, descriptor, bundle, routing, and message compatibility coverage.

## Batch79 — Bounded total OPD3 uncompressed size

- Extended the shared pre-read archive validator with a configurable total declared
  uncompressed-size budget and a 512 MiB default.
- Accumulated sizes from ZIP metadata without opening or reading entry payloads.
- Rejected unknown negative declared sizes and used remaining-budget checks to avoid overflow.
- Added a package-import exception for archives exceeding the total budget while preserving
  `MALFORMED_PACKAGE`, `PACKAGE_MALFORMED`, and the legacy failure message field.
- Added exact-limit, cumulative-over-limit, single-entry-over-limit, configuration, and
  diagnostic regression coverage.
- Kept Batch77's actual streamed-byte and strict UTF-8 enforcement unchanged.

## Batch78 — OPD3 archive structure integrity validation

- Established Batch77 at `a618893` as the verified baseline for archive-structure hardening.
- Added one shared pre-read structure validator to both modern OPD3 descriptor and
  bundle-content paths.
- Rejected unsafe path forms, exact duplicates, Unicode-normalized collisions, and
  case-ambiguous required JSON entries before reading or deserializing package text.
- Added a configurable maximum archive-entry count with a default of 4096 and metadata-only
  enforcement during archive enumeration.
- Kept structure failures on the package-import exception path, preserving Batch76's
  `MALFORMED_PACKAGE` and `PACKAGE_MALFORMED` diagnostics and non-fail-fast directory import.
- Added focused boundary, read-order, persistence-safety, and batch-continuation regression
  tests while retaining Batch77 size-limit and strict UTF-8 coverage.
- Identified total declared uncompressed archive-size enforcement as the preferred bounded
  capability for Batch79.

## Batch77 — Bounded and strict OPD3 text entry reading

- Added a configurable 32 MiB default limit for each OPD3 text entry.
- Added declared-size and streamed-byte enforcement to prevent unbounded archive reads.
- Added strict UTF-8 decoding so malformed package text is rejected deterministically.
- Treated directory entries as missing text files instead of reading them as empty content.
- Added package-import exceptions for oversized entries and invalid text encoding.
- Added focused tests for exact-limit reads, oversized entries, malformed UTF-8, missing
  entries, and directory entries.

## Batch76 — Actionable package import diagnostics

- Added stable package-import failure categories and diagnostic codes.
- Preserved validation issue codes without requiring UI text parsing.
- Added recovery guidance for invalid data, duplicate identity, file access, malformed
  packages, and unexpected failures.
- Preserved the exact legacy failure-message contract while adding structured diagnostic
  and recovery fields.
- Updated detailed directory import to create diagnostics through one shared classifier.
- Added focused tests for classification, fallback behavior, validation details, and the
  non-fail-fast service boundary.

## Batch75 — Unicode-robust Desktop search

- Added one shared Unicode canonicalization boundary for Desktop search.
- Matched canonically equivalent composed and decomposed diacritics.
- Matched compatibility forms such as full-width Latin characters.
- Preserved highlight ranges against the original visible text even when normalization
  changes UTF-16 length.
- Applied the same normalization to query parsing, duplicate-term removal, matching, and
  highlighting.
- Added focused regression coverage for canonical equivalence, compatibility width, and
  decomposed-grapheme highlighting.

## Batch74 — Scalable continuation context

- Rebased the canonical continuation state on verified Batch73 commit `8b8baaa`.
- Replaced the accumulated historical handoff with one concise current-state contract.
- Reorganized the roadmap around product milestones instead of batch-by-batch narration.
- Added a capability map for selective, dependency-aware source loading.
- Added a test matrix that connects capability changes to focused regression coverage.
- Added an explicit batch-planning policy for coherent 8–15-file vertical slices.
- Documented how capability context, source inspection, and full `clean test` work together.
- No production behavior or persisted-data contract is changed by this increment.

## Batch67 - Accessible search option groups

- Replaces duplicated filter and sort chip rows with one shared search-option group component.
- Announces each group heading, selected option, option count, and activation intent to assistive technology.
- Adds deterministic presentation contracts for Lesson Browser and Review History filter/sort controls.
- Adds regression tests for shared validation and screen-specific selected-option mapping.

## Batch65 — Actionable search empty-state recovery

- Added one shared empty-result presentation contract for Desktop search surfaces.
- Added accessible Clear search and Reset view actions only when each action can recover results.
- Distinguished genuinely empty data from query/filter-produced empty results.
- Wired the shared recovery card into Review History and Lesson Browser.
- Added shared and screen-specific regression tests for recovery availability and wording.

## Batch63 — Desktop search and discovery epic

- Added reusable search field, normalization, summaries, filters, and deterministic projections.
- Added Review History search, rating filters, sorting, no-result recovery, and view-model actions.
- Added Lesson Browser search, translation filters, sorting, no-result recovery, and view-model actions.
- Added broad projection, presentation, normalization, and state contract tests.

## Batch62 — Desktop UX recovery-state epic

- Added one shared loading, ready, and failed state contract for Desktop data screens.
- Added a reusable loading/error card with polite loading announcements and assertive failure announcements.
- Added direct retry actions for Dashboard, Statistics, and Review History.
- Preserved the last successful data when a refresh fails instead of replacing it with placeholders.
- Normalized unexpected exception messages into stable user-facing failure details.
- Routed screen-specific recovery callbacks through ContentHost and LearningShell.
- Added cross-screen tests for presentation wording, default loading state, fallback messages, retry availability, and stale-data preservation.
- Updated architecture, roadmap, changelog, and handoff for the new epic-sized batch policy.

## Batch61 — Shell-wide keyboard navigation epic

- Added direct F1–F6 navigation for all six Desktop destinations.
- Added Ctrl+PageUp and Ctrl+PageDown cyclic screen traversal.
- Added Ctrl+Shift+R refresh for the active data-backed screen.
- Centralized destination refresh behavior in the shell instead of refreshing unrelated screens.
- Added stable shell focus and one documented global keyboard surface.
- Exposed destination shortcuts in both visible sidebar labels and screen-reader descriptions.
- Added pure shortcut-routing tests plus cyclic NavigationState coverage.

## Batch60 — Content Library keyboard navigation

- Added Ctrl+R refresh and Ctrl+I package-import shortcuts.
- Added hierarchical Escape navigation that clears lesson detail before closing the lesson browser.
- Suspended screen-level shortcuts while any Content Library dialog is visible.
- Added a visible and screen-reader-readable shortcut hint to the Content Library header.
- Added focused tests for modifier requirements, Escape precedence, dialog isolation, and the documented shortcut contract.

## Batch59 — Content Library action descriptions

- Added contextual screen-reader descriptions for refresh, import, and retry actions.
- Added target-aware descriptions for opening libraries and creating collections.
- Added target-aware descriptions for attaching, renaming, deleting, and detaching.
- Explicitly announced confirmation boundaries for destructive collection and package actions.
- Added stable fallback wording for blank library, collection, and package names.
- Added focused presentation tests for global, contextual, destructive, and fallback action speech.

## Batch58 — Content Library card semantics

- Added ordered semantic summaries for library cards with normalized content, learning-item, and collection counts.
- Added collection summaries that distinguish empty and populated package attachment states.
- Added attached-package summaries with optional version and format metadata.
- Added installed-package summaries with version, format, and normalized library count.
- Grouped every package property into one label-and-value semantic unit.
- Added stable fallback wording for blank names and metadata.
- Added focused presentation tests for counts, attachment states, optional metadata, and property fallbacks.

## Batch57 — Content Library dialog semantics

- Added ordered purpose-and-context announcements to create, rename, delete, attach, and detach dialogs.
- Exposed every dialog title as a semantic heading.
- Exposed attach-package choices with explicit selected state and spoken package identity.
- Added destructive-scope wording for collection deletion and package detachment.
- Added normalized count grammar and stable fallbacks for blank library, collection, and package names.
- Added focused tests for all dialog summaries and package-option selection states.

## Batch56 — Content Library screen semantics

- Exposed the Content Library page title and normalized counts as one semantic heading.
- Added polite import-status and assertive import-error announcements.
- Added stable fallback wording for blank import messages.
- Exposed Libraries and Installed Packages labels as semantic section headings.
- Added tests for count normalization, pluralization, live-message wording, and section labels.

## Batch55 — Lesson Browser semantics

- Exposed the Lesson Browser library header as one semantic heading with normalized item count.
- Grouped lesson title, hierarchy, type, and learning-item count into one ordered card announcement.
- Exposed the selected lesson heading together with study availability.
- Grouped every lesson property into one label-and-value semantic unit.
- Added stable fallback wording for blank names, titles, types, labels, and values.
- Added focused presentation tests for pluralization, hierarchy, availability, and fallback behavior.

## Batch54 — Dashboard chart-data semantics

- Grouped forecast rows into label, review count, and unit announcements.
- Grouped scheduling-pressure rows into label, card count, and unit announcements.
- Grouped memory-stage legend entries into count-and-percentage announcements.
- Added full-date review activity descriptions to individual heatmap cells.
- Added stable fallbacks for blank chart labels and units.
- Added tests for chart values, percentages, dates, pluralization, zero activity, and future dates.

## Batch53 — Dashboard visualization semantics

- Added semantic identity and heading treatment to visualization cards.
- Added an explicit no-data announcement when a visualization has no data.
- Grouped empty chart title and description into one ordered semantic unit.
- Added a percentage announcement for the retention gauge with clamped values.
- Added stable fallbacks for blank visualization, empty-state, and retention labels.
- Added focused presentation tests for all new accessibility behavior.

## Batch52 — Dashboard summary semantics

- Exposed the Dashboard page header as one semantic heading.
- Exposed every Dashboard section header as one title-and-description heading.
- Grouped each metric title, value, and supporting text into one ordered semantic unit.
- Added stable fallback wording for blank metric values and details.
- Added tests for metric ordering, fallback wording, section headings, and page heading.

## Batch51 — Shell chrome semantics

- Exposed the persistent application header as one semantic heading.
- Grouped product name and edition into one concise header announcement.
- Grouped engine and dashboard status into one ordered status announcement.
- Added stable visible and spoken fallbacks for blank status values.
- Added tests for header, normal status, and blank-status presentation.

## Batch50 — Sidebar navigation semantics

- Added explicit tab semantics to every desktop sidebar destination.
- Added selected-state semantics for the active destination.
- Added concise destination descriptions while preserving visible labels and navigation behavior.
- Added tests for active, inactive, and label-preservation presentation.

## Batch49 — Settings semantic configuration summaries

- Added one merged semantic description for every Settings property row.
- Added ordered section summaries matching the visible configuration order.
- Added a stable unavailable fallback for blank configuration values.
- Added tests for property semantics, fallback wording, and section ordering.

## Batch48 — Statistics semantic summaries

- Added one merged semantic description for each statistic card.
- Added one ordered screen-level summary matching the visible metric order.
- Announces `--` and blank metric values as **Unavailable** without changing the visual placeholder.
- Added tests for normal, placeholder, blank, and full-summary presentation.

## Batch47 — Review History semantic reading order

- Added correct singular and plural grammar for the Review History count.
- Added one merged semantic description for the empty state.
- Added one ordered semantic summary for every review event card.
- Added tests for count grammar, empty-state guidance, and review metric order.

## Batch46 — Recoverable Content Library load errors

- Added a dedicated presentation boundary for Content Library load failures.
- Preserves the underlying failure detail while adding concrete local-data recovery guidance.
- Added a direct **Retry** action wired to Content Library refresh.
- Announces the complete load error and recovery path as an assertive semantic region.
- Added tests for real messages, blank-message fallback, and semantic wording.

## Batch45 — Actionable Content Library empty state

- Added a dedicated empty-state presentation model for the Content Library.
- Added a direct **Import First Package** action inside the empty card.
- Added a merged semantic description explaining the empty state and recovery action.
- Added tests for first-import guidance and screen-reader wording.

## Batch44 — Scheduler stage-transition presentation hardening

- Replaced the corrupted scheduler transition separator with a tested UTF-8 presentation boundary.
- Converts enum-style stage names into readable labels before showing scheduler feedback.
- Uses the same corrected transition text for visible and screen-reader feedback.
- Added coverage for normal, multi-word, whitespace, and blank stage names.

## Batch43 — Accessible session completion summary

- Added a pure accessibility presentation for completed Study sessions.
- Reads the session title, total reviewed items, new/review split, and optional lesson progress as one ordered result.
- Includes the Enter-key next action in the completion summary.
- Added coverage for general, singular-item, and lesson-completion summaries.

## Batch42 — Study focus transition hardening

- Added a pure Study focus-transition key and phase model.
- Reacquires Study keyboard focus after start, reveal, grade, item advance, retry, and completion transitions.
- Normalizes recoverable error identity so repeated error-state changes remain deterministic.
- Added focused coverage for idle, question, revealed-answer, next-item, error, and completed transitions.

## Batch41 — Explicit Study prompt and answer semantics

- Added a pure presentation boundary for prompt and answer accessibility labels.
- Labels active content as a Study prompt before reveal.
- Exposes the translation as a Study answer only when review actions are available.
- Added deterministic fallbacks for blank imported content and focused unit coverage.

## Batch40 — Complete scheduler feedback card semantics

- Added a unified semantic description to the visible Scheduler Feedback card.
- Exposed every displayed scheduler metric in a predictable reading order.
- Reused the Batch39 accessibility presentation boundary so visible and announced values cannot drift.
- Added focused coverage for rating, interval, next-review, transition, and counter descriptions.

## Batch39 — Accessible scheduler feedback confirmation

- Added a pure accessibility presentation for persisted scheduler feedback.
- Announces the saved rating, stage transition, next interval, next review time, review count, and lapse count.
- Integrates the confirmation into the next-question and completed-session Study announcements without changing scheduler behavior.
- Added focused unit and presentation-integration coverage.

## Batch38 — Contextual Study rating guidance

- Added concise explanations for what Again, Hard, Good, and Easy mean for recall and the next scheduling interval.
- Displayed the guidance only when an answer is revealed and rating actions are available.
- Added a combined screen-reader description that preserves the verified 1–4 keyboard order.
- Added focused unit coverage for rating order, scheduling meaning, and shortcut descriptions.

## Batch37 — Accessible Study action descriptions

- Added a centralized accessibility presentation for retry, start, reveal, and all four review-rating controls.
- Added explicit screen-reader descriptions that state each action and its exact keyboard shortcut.
- Reused the presentation in Desktop Study buttons so visible labels and semantic descriptions cannot drift.
- Added unit coverage for primary, recovery, reveal, and rating action descriptions.

## Batch36 — Accessible Desktop Study state announcements

- Added a pure accessibility presentation model for idle, question, revealed-answer, completed, active, and recoverable-error Study states.
- Added polite screen-reader status announcements that include the currently available keyboard action.
- Added semantic lesson-progress descriptions with current item and completed-item context.
- Added focused unit coverage for all major Study accessibility states and error priority.
## Batch35 — Recoverable Desktop Study error state

- Added a pure presentation model for persisted Study load failures with explicit recovery guidance.
- Added Enter/Space retry handling while an error is shown, while preserving protection from review shortcuts.
- Updated the Study error card with a clear title, actionable guidance, and visible keyboard hint.
- Added unit coverage for both error presentation and state-aware retry shortcuts.

## Batch34 — Actionable Desktop Study idle state

- Replaced the ambiguous `--` idle learning-item placeholder with a dedicated ready-to-study card.
- Added clear guidance describing what a general study session will do.
- Kept the primary action aligned with the verified Enter/Space keyboard workflow.
- Added focused state-resolution coverage so active, completed, and recoverable-error states cannot display the idle presentation.

## Batch33 — Desktop Study keyboard workflow

- Added state-aware Study shortcuts: Enter/Space starts or reveals, while 1–4 grades Again, Hard, Good, and Easy.
- Automatically focuses the Study surface so the keyboard flow works immediately after navigation.
- Added visible shortcut hints to every affected Study action.
- Disabled shortcut dispatch while recoverable load errors are shown and added focused resolver coverage.

## Batch32 — OPD3 Desktop graded-study restart completion

- Recreated the persisted Desktop application context before grading and verified the same lesson-scoped queue resumes at the same first item.
- Extended the resumed OPD3 Content Library path through answer reveal and grading via `StudyViewModel`.
- Verified persisted completion clears the active session while sibling-lesson content never leaks into the study flow.
- Closed the functional Desktop Beta path from real OPD3 import through persisted session completion.

## Batch31 — Real OPD3 browse-to-study presentation flow

- Added a real four-file OPD3 package fixture using `manifest.json`, `metadata.json`, `contents.json`, and `learning-items.json`.
- Created a content library for imported OPD3 bundle content using manifest identity.
- Verified Content Library browsing, lesson selection, lesson-scoped session creation, and navigation into Study through Desktop presentation components.

## Batch30 — Content Library lesson-study navigation boundary

- Added a Desktop presentation coordinator for the Content Library lesson start action.
- A successful lesson start now navigates to Study only after an active session exists.
- A failed lesson start remains in Content Library and preserves the recoverable Study error state.
- Added integration coverage using a persisted application context and imported multi-lesson data.

## Batch29 — Desktop lesson-scoped persisted restart coverage

- Added Desktop-module integration coverage that imports multiple lessons and starts study through `StudyFacade.startLessonStudy`.
- Fixed Desktop progress totals to use the actual planned study queue rather than all enabled lesson learning items.
- Aligned completed-session reviewed count and position with the completed queue total.
- Verified the persisted Desktop application context resumes and completes the planner-selected lesson queue without leaking an item from a sibling lesson.

## Batch28 — Persisted Desktop application study queue wiring

- Replaced the in-memory study queue used by `LearningApplicationFactory.createPersisted` with the existing JSON-backed study queue repository.
- Added `study-queues.json` to the Desktop application context transaction boundary so session, queue, memory-state, and review writes remain restart-consistent.
- Added integration coverage proving a study session created through the Desktop application factory is resumable after recreating the application context.

## Batch27 — Persisted OPD3 restart integration coverage

- Added an end-to-end integration test that imports a representative two-item OPD3 package through the composed persisted platform.
- Verified imported learning items can start a real study session and persist a review through the production transaction boundary.
- Recreated the complete platform from disk and verified the remaining queue item resumes correctly after restart.

## Batch26 — OPD3 manifest and metadata consistency validation

- Added package-level compatibility validation that decodes required `metadata.json` during bundle import.
- Rejected OPD3 bundles whose supplied metadata name, version, or format disagrees with `manifest.json`, preventing descriptor/content identity drift.
- Preserved legacy metadata files with omitted optional identity fields and case-insensitive OPD3 format compatibility.

## Batch25 — Atomic persisted study queue transaction coverage

- Added `study-queues.json` to the persisted platform transaction boundary used by review/session operations.
- Added integration coverage proving the composed persisted platform writes session, queue, memory-state, and review-event state together during a real review flow.
- Closed a restart-consistency gap where queue advancement was previously outside the JSON transaction snapshot set.

## Batch24 — Recoverable study/review data diagnostics

- Prevented Desktop study initialization, refresh, session start, reveal, and review persistence failures from terminating the UI flow.
- Preserved the last good study state while exposing contextual persisted-record diagnostics.
- Added an explicit Retry action for repairing persisted data and reloading the study path.
- Added focused tests for incompatible and generic study persistence failures.

## Batch23 — Recoverable Desktop persisted-data diagnostics

- Prevented Content Library startup and refresh failures from terminating the Desktop flow when persisted content records are incompatible.
- Added actionable Desktop messages that identify the persisted entity type and record ID while preserving the root cause detail.
- Kept the last successfully loaded Content Library state visible when a later refresh fails.
- Added retry-through-Refresh behavior for Content Library and lesson browsing loads.
- Added Desktop tests for contextual and generic persisted-data failure messages.
- Updated handoff and roadmap continuation context.

## Batch22 — Contextual incompatible persisted-record diagnostics

- Added `InvalidPersistedRecordException` with persisted entity type, record ID, and preserved root cause.
- Applied contextual mapping to content libraries, collections, contents, learning items, content packages, and package catalogs.
- Added tests for invalid enum values and prevention of nested duplicate wrapping.
- Updated handoff and roadmap continuation context.

## Batch21 — Actionable Package Import Diagnostics

- Added non-fail-fast directory import results that preserve successful package imports while reporting each incompatible or malformed candidate independently.
- Added source-specific failure diagnostics to the Desktop Content Library instead of collapsing the whole directory import into one generic exception.
- Desktop now distinguishes an empty directory, a fully failed import, and a partially successful import with skipped packages.
- Added application coverage proving that detailed directory import continues after candidate failures.

## Batch20 — persisted session recovery reconciliation

- Added an application recovery use case that reconciles the latest active session with its persisted study queue.
- Resumable sessions now return queue progress through one explicit recovery result.
- Active sessions with a missing queue are safely finalized instead of remaining permanently orphaned.
- Sessions whose queue completed before restart are finalized and their stale queue is removed.
- Desktop Study now displays actionable recovery messages and allows a clean new session after incomplete persistence is detected.
- Added recovery coverage for no-session, resumable, missing-queue, completed-queue, and clock-skew cases.

## Batch19 — active study session recovery

- Added learner-scoped lookup for the latest active study session across in-memory and store-backed repositories.
- Exposed active-session recovery through `LearningEngine`.
- Restored the persisted queue, lesson scope, title, counts, and current item when the Desktop study screen is reopened after restart.
- Added repository contract coverage for active-session selection and learner isolation.

This changelog records verified repository increments. Historical descriptions are concise because source and commits remain authoritative.

## Batch18 — repository workflow guardrails

- Normalized the Git-first repository workflow and guardrails.
- Established the clean `develop` baseline represented by commit `748b328`.
- Confirmed the batch package/apply direction used for subsequent increments.

## Batch17 — living documentation baseline

- Restored project documentation under `docs/`.
- Documented the two-module structure and layered dependency direction.
- Established `develop` as the canonical development branch.
- Documented verified apply, build, backup, and rollback expectations.

## Batch16 milestone

- Included study-queue planning, policy, persistence, diagnostics, and metrics foundations.
- Included content-library collection workflows and Desktop dialogs.
- Included dashboard visualization foundations.
- Continued migration toward Git as the source of truth.

## Discarded Batch19 package

A previously generated Batch19 archive was not accepted as a verified functional increment and did not change the repository. It must not be reused or treated as completed work. The next genuine increment remains `Batch19`, rebuilt from the current clean source baseline.

## Documentation consolidation

Durable project memory is repository-owned: `AGENTS.md` governs workflow; strategic handoff,
operational context, roadmap, architecture, capability history, and milestone history each have
one documented responsibility. Stale external handoff ZIPs and repository-generated batch
artifacts are not part of the baseline.

## Batch64 - Search refinement reset epic

- Added one shared refinement-state and presentation contract for search query, filter, and sort changes.
- Added an accessible Reset view control to Review History and Lesson Browser.
- Reset restores the complete default view in one action instead of requiring three separate controls.
- Added regression tests for default, partial, and fully refined states.

## Batch66 — Keyboard-first search recovery

- Added one shared keyboard contract for search surfaces.
- `Ctrl+F` focuses search in Review History and Lesson Browser.
- `Escape` progressively clears the query first and then resets filter and sort refinements.
- Added visible and screen-reader shortcut guidance plus pure regression tests.

## Batch68 — Accessible result status
Searchable desktop collections now expose a polite live result status that distinguishes complete collections, filtered subsets, empty matches, and truly empty sources.

## Batch69

- Added independently removable search query, filter, and sort refinements.
- Preserved the existing one-action full reset and keyboard recovery contract.
- Added deterministic shared and screen-level regression coverage for refinement action ordering and availability.

## Batch70 - Search match highlighting
- Added reusable, case-insensitive search match presentation with deterministic non-overlapping ranges.
- Highlighted matching query text across lesson browser rows and review history cards.
- Preserved complete screen-reader text while announcing the number of visible matches.

## Batch71
- Added reusable search-scope disclosure for desktop search surfaces.
- Lesson Browser now states searchable lesson fields and current query/filter/sort context.
- Review History now states searchable metrics and current query/filter/sort context.
- Added pure presentation and feature adapter tests.

## Batch72 - Contextual search query guidance

- Adds shared contextual placeholders and searchable examples to Desktop search fields.
- Guides one-character queries toward more specific matches.
- Announces normalized active queries without changing search projection behavior.
- Adds shared and screen-specific regression tests.


## Batch73 - Multi-term desktop search

- Adds shared whitespace-normalized query parsing with case-insensitive duplicate removal.
- Makes Lesson Browser and Review History require every query word while allowing any word order.
- Highlights every matching query term and safely merges overlapping highlight ranges.
- Explains multi-word matching behavior in visible and screen-reader guidance.
- Adds parser, matcher, highlighting, guidance, and screen projection regression tests.
