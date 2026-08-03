# Capability Map

LQ-006C adds portable contracts under `domain/study/recall` and a pure selector under
`application/recall`. It has no adapter, infrastructure, persistence, or Desktop composition root.

Use this map to select the smallest source neighborhood that can safely implement a
capability. It is an orientation document; actual source and tests remain authoritative.

## Dependency direction

```text
Desktop and JVM adapters
        ↓
Application use cases and ports
        ↓
Domain model and services

Infrastructure implements application ports.
```

Do not make domain or application code depend on Compose Desktop or concrete JSON storage.

## Current Study capability status

LQ-005A.1 extends `domain/study/evidence` with Content-owned trajectories, anchored immutable
chains, typed reset boundaries, and non-evidence manual events. The promotion authority consumes
only the current chain. No Application, Infrastructure, persistence, or Desktop composition root
is added.

LQ-005A adds a new shared-Domain neighborhood under `domain/study/evidence`: immutable recall
evidence, exact promotion anchors, configurable windows, injected clock, and the pure promotion
authority. It reuses memory/session value contracts but has no dependency on Application,
Infrastructure, Scheduler/FSRS, or Desktop and has no execution/persistence composition root yet.

UX-007 adds `TypingFieldLayoutMetrics` beside Desktop typing presentation, removes duplicate caps in
adaptive image allocation, and evolves `StudySessionContinuityPresentation` plus `StudyViewModel`
into one pending-destination publication pipeline. Shared Application review/practice use cases
remain the only queue mutation boundary.

UX-006 reuses shared evaluative availability/provenance through `application/session`, enables the
Desktop rating dock and keyboard on concealed fronts, removes the obsolete header/dialog wiring,
and makes `StudyVerticalSpaceAllocation` consume true fixed/typing/dock minima before image space.

UX-005 adds shared Manual Evaluation availability/provenance through `application/session` and the
existing review transaction. Desktop adds typed vertical allocation beside adaptive image
presentation and removes inventory header/collapse state; it owns no scheduling or rating count.

UX-004 extends `application/learningexperience` with normalized `VALID_PREFIX` authority. Desktop
`CenteredTypingField`, Practice identity presentation, and Rating Inventory semantic presentation
consume typed shared state without evaluating answers, inferring policy, or counting ratings.

UX-003 adds a thin Desktop presentation layer for shared typing evaluation, manual-override
availability, and Rating Inventory. `StudyFacade` refreshes shared inventory at active and
completion state boundaries; Compose maps it without owning counts or rating semantics.

LQ-004B adds shared practice boundaries under `application/session`: fixed queue membership,
deterministic shuffled round state, practice completion/progress/results, manual rating override,
and rating inventory. `StudyQueueRecord` persists practice navigation; `StudySessionRecord`
persists loop policy; `ReviewEventRecord` persists typed rating provenance. Desktop Study files
only dispatch these use cases and render their typed state. Full contract:
[`PRACTICE_SESSION_CONTRACT.md`](PRACTICE_SESSION_CONTRACT.md).

| Capability | Implementation | Automated evidence | Manual status |
|---|---|---|---|
| LQ-005A.1 — Evidence Chain & Learning Trajectory Foundation | Implemented | 2 focused suites / 23 tests; 587 suites / 3,064 full tests pass | Integrated UAT pending |
| LQ-005A — Long-Term Evidence Promotion Authority | Implemented | 1 focused suite / 12 tests; 586 suites / 3,053 full tests pass | Integrated UAT pending |
| UX-007 — Image maximization and single-pass transition | Implemented | Focused suites pass; 584 suites / 3,038 full tests pass | Integrated Desktop UAT pending |
| UX-005 — Evaluative manual rating and vertical budget | Implemented | 9 focused suites / 32 tests; 589 suites / 3,056 full tests pass | Integrated Desktop UAT pending |
| UX-006 — Direct evaluative dock and dynamic study space | Implemented | 14 focused suites / 71 tests; 584 suites / 3,036 full tests pass | Integrated Desktop UAT pending |
| UX-004 — Typing semantics and Practice controls | Implemented | 15 focused suites / 97 tests; 586 suites / 3,049 full tests pass | Integrated Desktop UAT pending |
| UX-003 — Typing feedback, override access, rating visibility | Implemented | 7 focused suites / 30 tests; 586 suites / 3,046 full tests pass | Integrated Desktop UAT pending |
| REV-002 — Review All Current Again / Hard Items | Implemented | 6 focused suites / 70 tests; 575 suites / 3,006 full tests pass | Integrated Desktop UAT pending |
| REV-001 — Focused Review Entry Modes | Implemented | 7 focused suites / 74 tests; 575 suites / 3,005 full tests pass | Integrated Desktop UAT pending |
| V3-004 — Center Typing and POS feedback | Implemented | 8 focused suites / 71 tests; 575 suites / 3,000 full tests pass | Integrated Desktop UAT pending |
| V3-003 — Adaptive Answer Space Utilization | Implemented | 11 focused suites / 99 tests; 574 suites / 2,997 full tests pass | Integrated Desktop UAT pending |
| V3-002 — Approved Answer Golden Layout | Implemented | 8 focused suites / 74 tests; 573 suites / 2,995 full tests pass | Integrated Desktop UAT pending |
| P0-001 — Front-side recall isolation | Implemented | 569 suites / 2,982 tests pass | Integrated Desktop UAT pending |
| PLE-036 through PLE-038-D | Implemented | Verified at capability commits | Earlier interactive checks exist; final integrated pass pending |
| PLE-039-A — Derived Memory Confidence | Implemented | Verified (`7185889`) | Integrated Desktop pass pending |
| PLE-039-B — Easy-only confidence gate | Implemented | Verified (`5a252ec`) | Integrated Desktop pass pending |
| PLE-039-C — Rating semantics remediation | Implemented | Verified (`034fbb3`) | Integrated Desktop pass pending |
| PLE-039-D — Compact Typing surface | Implemented | Verified (`6c96f63`) | Integrated Desktop pass pending |
| PLE-039-E — Input visibility | Implemented | Verified (`7f0fa44`) | Low-height/caret UAT pending |
| PLE-039-F — Compact dock and seeded NEW order | Implemented | Verified (`5eaeda5`) | Integrated queue/visual UAT pending |
| PLE-039-G — Reveal scroll reset | Implemented | Verified (`a1cb460`) | UAT-01 through UAT-07 pending |
| PLE-032-B2.1 — Durable restart foundation | Implemented | 559 suites / 2,873 tests pass after remediation | Desktop integrated UAT pending |
| PLE-032-B2.2 — Learner-facing opt-in control | Implemented | Desktop wiring/composition verified | Product Owner UAT pending |
| PLE-032-B2 remediation — provenance and scope safety | Implemented | Behavioral restart/persistence/Desktop scope tests | Product Owner UAT pending |
| PLE-039-H — Semantic rating action feedback | Implemented | 560 suites / 2,878 tests pass | Integrated Desktop UAT pending |

P0-001 is owned by the Desktop Study presentation branch in `StudyScreen.kt`. Unrevealed Typing
recall is consumed by `LearningSceneRenderer.kt` and its `EffectiveStudyPresentation` role filters;
`DiscoveryFrontSurface.kt` remains the explicit new-content introduction consumer. Domain,
application, Scheduler, rating, queue, and persistence boundaries are unchanged.
| AURORA-002 — Study Experience Polish, Phase 1 | Implemented | 4 focused suites / 63 tests; 560 suites / 2,881 full tests pass | Integrated Desktop UAT pending |
| AURORA-003 — Study Visual Focus | Implemented | 6 focused suites / 47 tests plus post-session regression; 561 suites / 2,894 full tests pass | Integrated Desktop UAT pending |
| AURORA-004 — Study Micro Interaction Polish | Implemented | 8 focused suites / 72 tests; 35 regression suites / 270 tests; 562 suites / 2,899 full tests pass | Integrated Desktop UAT pending |
| AURORA-005 — Learning Session Continuity | Implemented | 8 focused suites / 57 tests; 42 regression suites / 305 tests; 563 suites / 2,909 full tests pass | Integrated Desktop UAT pending |
| AURORA-006 — Session Completion Reinforcement | Implemented | 8 focused suites / 52 tests; 21 regression suites / 130 tests; 564 suites / 2,919 full tests pass | Integrated Desktop UAT pending |
| AURORA-007 — Learning Entry Clarity | Implemented | 4 focused suites / 44 tests; 47 regression suites / 254 tests; 565 suites / 2,934 full tests pass | Integrated Desktop UAT pending |
| AURORA-008 — Dashboard Clarity & Information Hierarchy | Implemented | 4 focused suites / 24 tests; 30 regression suites / 180 tests; 566 suites / 2,943 full tests pass | Integrated Desktop UAT pending |
| AURORA-009 — Premium Study Surfaces | Implemented | 4 focused suites / 68 tests; 567 suites / 2,949 full tests pass | Integrated Desktop UAT pending |
| AURORA-010 — Unified Study Stage | Implemented | 5 focused suites / 75 tests; 567 suites / 2,953 full tests pass | Integrated Desktop UAT pending |
| UX-002 — Quiet Scheduler Feedback | Implemented | 7 focused suites / 95 tests; 568 suites / 2,966 full tests pass | Integrated Desktop UAT pending |
| EPIC-001 — Premium Study Experience | Implemented | 13 focused suites / 143 tests; 569 suites / 2,975 full tests pass | Integrated Desktop UAT pending |
| EPIC-001R — Approved Study Workspace remediation | Implemented | 8 focused suites / 95 tests; 569 suites / 2,979 full tests pass | Integrated Desktop UAT pending |
| V3-001 — Approved Study Visual System | Implemented | 12 focused suites / 112 tests; 572 suites / 2,992 full tests pass | Integrated Desktop UAT pending |

PLE-032-B2.1 spans `application/continuousreview`, the intent repository port and JSON/in-memory
adapters, `LearningEngine`/`LearningApplicationFactory`, plus Desktop `StudyFacade` startup and
guarded `StudyViewModel` actions. B2.2 adds the localized accessible completion-only switch while
keeping scope/predecessor/session decisions outside Compose.
The remediation adds the domain/persistence `SessionCompletionProvenance` contract and an exact-
scope recovery request/result boundary; Desktop projects the durable intent against the completed
session package/topic rather than learner-global state.

PLE-039-H is confined to Desktop `StudyViewModel` transient state, `StudyScreen` rating controls,
and the existing base `LEButton` visual seam; domain/application/persistence authority is unchanged.

AURORA-003 is confined to Desktop presentation: `StudyVisualFocusResolver` owns relative emphasis,
while `StudyScreen`, focused answer content, scheduler feedback, statistics header, and `LESurface`
consume existing theme tokens. It changes no layout, interaction, accessibility, or learning authority.

AURORA-004 is confined to Desktop presentation: `StudyMicroInteractionResolver` owns the pure reveal
timeline; `StudyScreen` and `FocusedAnswerSurface` consume it; `LEButton` exposes opt-in subtle motion
for Study rating actions. Existing motion/theme tokens remain timing and visual authority.

AURORA-005 adds `StudySessionContinuityPresentation` plus transient `StudyUiState` projection,
token-safe `StudyViewModel` phase advancement, and `StudyScreen` rendering through the existing
ContentHost/LearningShell composition boundary. `StudyFacade` only supplies structured committed
rating evidence alongside its existing scheduler projection; transaction and lifecycle authority do
not move to Desktop presentation.

AURORA-006 is confined to Desktop completion presentation. `SessionCompletionPresentationResolver`
maps existing immutable completion facts and action availability to typed visual roles and priority;
`SessionCompletionCard` consumes that model through existing `LETheme`, action callbacks, scheduler
feedback, Undo, and Continuous Review contracts. No application/domain/persistence authority moves.

AURORA-007 is confined to `StudyIdlePresentation`, the existing `StudyIdleCard` consumer, localized
`LearningEntryStrings`, and display-title projection through `StudyFacade`. Typed action priority
uses existing immutable availability and callback identities; Content Library/Lesson Browser retain
selection, search, import, editing, archive, and administration ownership.

AURORA-008 is confined to the Desktop Dashboard presentation resolver, screen ordering, and existing
metric/visualization card consumers. `DashboardFacade`, application learning-dashboard queries,
Review History heatmap input, statistics calculations, and shell navigation remain authoritative.

AURORA-009 is confined to Desktop Study surface presentation. `StudySurfacePresentationResolver`
owns typed roles and layers shared by `DiscoveryFrontSurface`, `FocusedAnswerSurface`, and their
existing content-stage consumer. Image/layout/typography/motion/accessibility/audio/rating authority
and every learning/application/persistence boundary remain unchanged.

AURORA-010 extends that Desktop-only presentation boundary with `UnifiedStudyStageResolver` and
flattens wrapper-only surfaces in discovery Meaning, answer Meaning/Examples, and Scheduler feedback.
Hero image/word interaction seams and Rating Dock remain explicit boundaries. Layout, responsive,
typography, motion, accessibility, audio, rating, scheduler, and learning authorities do not move.

UX-002 is confined to Desktop scheduler-feedback presentation. `SchedulerFeedbackPresentationResolver`
owns typed active-answer, completion, and continuity roles; `CompactSchedulerFeedback` and its four
existing call sites consume them. Structured scheduler values, details/accessibility content,
completion consequence, UX-001 sequencing, and application/domain/persistence authorities do not move.

EPIC-001 is confined to Desktop Study presentation and the shared `LESurface` shape seam.
`StudyFocusedImmersionPresentation` owns pure Canvas/hero/content/decision/motion projections;
`StudyScreen`, `DiscoveryFrontSurface`, `FocusedAnswerSurface`, and `LearningSceneRenderer` consume
them through existing composition and interaction owners. Scheduler/FSRS, LQ-002, rating, review,
queue/session/persistence, Typing, audio, image sizing, keyboard/focus/accessibility, UX-001, and
UX-002 authorities do not move.

EPIC-001R stays inside the same Desktop Study composition boundary. The approved scene-order
projection is consumed by `StudyScreen`, `DiscoveryFrontSurface`, and `FocusedAnswerSurface`;
existing Typing, media, responsive, rating, scheduler, continuity, application, domain, and
persistence owners remain authoritative.

V3-001 replaces that presentation composition inside Desktop Study. `SignatureStudyPresentation`
owns centralized width+height adaptation; `AdaptiveStudyImagePresentationResolver` owns intrinsic
aspect classification and stable frame budgets; `StudyScreen`, `DiscoveryFrontSurface`, and
`FocusedAnswerSurface` consume it; the base `LEButton` exposes an opt-in shape seam for grouped
rating segments. Scheduler/FSRS, StudyFacade, LQ-002, Typing evaluation, audio, review/session,
application/domain, and persistence owners do not move.

V3-002 remains inside `FocusedAnswerSurface`: the Answer presentation derives image height from
viewport and existing disclosure state, while `MeaningCard` owns the centered single-line
audio/translation row. No application, domain, scheduler, rating, or persistence boundary changes.

V3-003 supersedes the V3-002 height-only owner with `AdaptiveStudySpacePresentationResolver`.
`FocusedAnswerSurface` consumes its image/spacing/example allocation and `StudyScreen` consumes
its Typing minimum-height and bounded-scroll decisions. Scheduler, rating, application/domain,
session, and persistence boundaries do not move.

V3-004 remains inside `StudyScreen`: `TypingRecallInput` owns centered editable-field composition,
while `TypingSuccessFocusOverlay` consumes the existing focused-answer POS projection,
`PartOfSpeechSemanticRegistry`, and shared `StudyPosBadge`. Typing evaluation, popup orchestration,
rating, Scheduler, application/domain, session, and persistence boundaries do not move.

The stable implementation baseline is `a1cb4600d5433c7a4e786168ba96fb6ecae45429`;
“automated verified” is not equivalent to Product Owner acceptance.

### Desktop themed base controls

- Production: `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/designsystem/components/base/`
- Focused tests:
  `desktop/src/test/kotlin/vn/loi/learning/desktop/ui/designsystem/components/base/`
- Current controlled consumers: `ui/state/DesktopLoadStateCard.kt` and
  `ui/search/SearchScopeCard.kt`.
- Boundary: semantic presentation only; no domain, repository, persistence, scheduler, audio,
  navigation, breakpoint, or theme-preference authority.

### Study semantic presentation

- Production composition:
  `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyScreen.kt`,
  `FocusedAnswerSurface.kt`, `CompactSchedulerFeedback.kt`, and
  `StudyVisualThemePresentation.kt`.
- Focused tests: `StudyVisualThemeMigrationTest`, `StudyVisualLayoutResolverTest`,
  `FocusedAnswerSurfaceVisualHierarchyTest`, and Study audio/keyboard regressions.
- Boundary: immutable presentation mapping and semantic Design System consumption only.
  `StudyVisualLayoutResolver` remains responsive authority; ViewModel/Facade, scheduler,
  persistence, keyboard routing, and `LearningContentAudioController` retain behavior ownership.

## Study and review

**Responsibility**

- lesson-scoped study sessions;
- queue lifecycle and next-item selection;
- prompt reveal and rating;
- review transitions and persisted scheduler decisions;
- session completion and restart recovery.

**Read when changing**

- study/session domain and application use cases;
- queue planner and repository contracts;
- scheduler and review integration;
- persistence adapters for session, queue, and review state;
- Desktop Study state, presentation, keyboard, accessibility, and composition wiring;
- restart and lesson-isolation integration tests.

**Direct dependencies**

Content identity, persistence, scheduler, Desktop composition root.

**High-risk contracts**

Atomic review persistence, lesson isolation, deterministic resume, rating order, and
existing persisted-data compatibility. The domain session owns `ACTIVE`/`FINISHED` plus its
durable current-item checkpoint. Desktop `ReviewWorkspaceState` is a projection and must not
become a second lifecycle, scheduler, or persistence owner.

**Learning content seam**

Canonical content lives under `domain/content/model`; package and persistence records are DTOs.
`application/learningcontent` owns the ordered learner-facing block projection consumed by
Desktop Study. Rich renderers must consume that projection rather than parse OPD3 fields or
invent content semantics in Compose.

**Learning progress seam**

`application/session/LearningSessionProgress` combines durable session review counts with the
persisted queue read model. Desktop may format this projection but must not count local actions,
infer completion from missing content, or recalculate scheduler outcomes.

**P6-07 undo/interruption neighborhood**

Read `StudySession`/`PendingSessionReview`, review-event and memory-state models/mappers/stores,
`ReviewSessionItemUseCase`, queue/session repositories and transaction composition,
`ActiveStudySessionRecovery`, `LearningSessionProgress`, Desktop Study workspace/facade, and
completion/restart tests together. One-step undo must reverse the latest committed review as
one Application-owned transaction; Desktop may request and present it but cannot implement the
reversal locally.

Delivered through `UndoableSessionReview`, `UndoLatestSessionReviewUseCase`, memory/event
reversal ports, queue rewind, session persistence mapping, `LearningEngine`, and the Desktop
Study facade/view-model/screen projection.

**Desktop Alpha-04 completion neighborhood**

Read `application/session/completion`, Product Brain planning, scene/evidence/decision contracts,
the review transaction and scheduler result, `StudySession` plus its record mapper/store,
Desktop Study facade/state/view-model/screen, and restart tests together. Product Brain owns
reflection, summary, rating intent, and completion orchestration; the existing review use case
owns scheduler invocation and committed memory/review state. The optional completion snapshot
travels through the established session repository rather than a parallel persistence path.

**P6-08 Desktop interaction neighborhood**

`StudyKeyboardShortcut`, `StudyFocusTransition`, `StudyActionAccessibility`,
`StudyWorkspaceStrings`, `StudyFailureMessage`/load-error presentation, `StudyViewModel`,
`StudyScreen`, rich-content semantics, `ContentHost`, and `LearningShell` jointly own keyboard,
focus, localized action copy, guarded dispatch, accessible fallback, and pause navigation.

## Scheduling and memory state

**Responsibility**

- scheduler decisions;
- intervals and next-review timestamps;
- memory-stage transitions;
- validation and metrics.

**Read when changing**

Scheduler interfaces and implementations, decision validators, review integration,
scheduling tests, and persistence mappings that store scheduler output.

**High-risk contracts**

Time semantics, deterministic decisions, compatibility with stored review state.

## Content package and OPD3 import

**Desktop performance seam**

`DesktopTaskRunner`/`CoroutineDesktopTaskRunner` own worker-to-Compose dispatch. Content Library
state and `PackageImportProgressListener` provide observable import/load phases. Bulk query
optimization lives in `ContentLibraryQueryService`, `LibraryContentQueryService`, and
`StudyQueuePlanner`; lazy rendering/search/thumbnail policy stays in Desktop presentation.

**JSON + binary OPD3 PKG routing seam**

`JvmDirectoryPackageScanner` discovers the candidate; `JvmPackageFormatDetector` reads its
signature; `ContentBasedPackageDescriptorReader` and `PackageContentImporterCompat` share that
format decision; `JvmOpd3PairResolver` finds the deterministic sibling JSON;
`LegacyOpd3PackageImporter` reuses `LegacyJsonImporter`, `LegacyOpd3MediaArchiveReader`, media
storage/mapping, and the normal `PackageImportService` transaction and registration boundary.
Desktop composition supplies `<data>/media` through `LearningApplicationFactory`.

**Responsibility**

- package scanning and validation;
- manifest and metadata identity;
- import, registration, media, and installed-package queries;
- diagnostics for partial, invalid, and incompatible input.

**Read when changing**

Package models, scanners, validators, importers, repositories, JVM adapters, Desktop
import wiring, and real-package integration fixtures.

**Direct dependencies**

Content identity, persistence, Content Library.

**High-risk contracts**

Format compatibility, duplicate identity behavior, partial writes, actionable diagnostics.

## Content Library and Lesson Browser

**Responsibility**

- libraries and collections;
- package attachment and detachment;
- lesson hierarchy and selection;
- navigation from content into lesson-scoped Study.

**Desktop search neighborhood**

- `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/contentlibrary/`
- shared search presentation under
  `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/search/`
- corresponding tests under `desktop/src/test/kotlin/...`

**Direct dependencies**

Installed packages, lesson/content queries, Desktop navigation, Study start boundary.

## Search and discovery

**Responsibility**

- normalized query terms;
- AND matching;
- filtering and sorting;
- result status, guidance, refinements, recovery, keyboard behavior, and highlighting.

**Known production files from the verified Batch73 boundary**

- `SearchQueryTerms.kt`
- `SearchText.kt`
- `SearchMatchPresentation.kt`
- `SearchQueryGuidancePresentation.kt`
- `LessonBrowserProjection.kt`
- `ReviewHistoryProjection.kt`

**Read when changing**

The complete shared search package, both screen projections and state holders, their
Compose surfaces, and all corresponding tests.

**High-risk contracts**

Normalization consistency between matching and highlighting, multi-field row matching,
stable sort order, and genuinely-empty versus no-match recovery.

## Review History

**Responsibility**

- review-history query and presentation;
- result filtering, sorting, semantics, and recovery;
- review event summaries.

**Read when changing**

Review-history application queries, Desktop state/projection/presentation, shared search
contracts, loading-state contracts, and tests.

## Dashboard, statistics, and analytics

**Responsibility**

- metrics and analytics queries;
- dashboard projections and chart presentation;
- loading, failure, refresh, stale-data, and accessibility contracts.

**Read when changing**

Analytics/application queries, calculators, Desktop view models and charts, shell refresh
routing, shared data-state presentation, and tests.

## Persistence and recovery

**Responsibility**

- JSON stores and mappings;
- repositories and transactions;
- atomic review/session updates;
- restart reconciliation;
- incompatible and corrupt-data reporting.

**Read when changing**

Record models, mappers, stores, repository implementations, factories/composition roots,
restart integration tests, and every consumer of a changed persisted contract.

**High-risk contracts**

Backward compatibility, atomicity, partial-write recovery, and data-loss prevention.

Backup recovery additionally requires manifest counts to match the already-enumerated archive
payload before allocating inventory or creating a safety backup. Malformed count validation is
owned by `DesktopRecoveryManager` and must remain failure-before-mutation covered.

**Phase 6 session lifecycle seam**

`StudySession` owns its durable current-item/reveal/pending-review checkpoint. Application
session use cases own presentation, staging, atomic application, and startup replay. Desktop
must cross `LearningEngine` for reveal/recovery and must not create a parallel lifecycle owner.

**P6-09 release-verification seam**

`DesktopPackageLearningFlowIntegrationTest` is the representative cross-module release path:
OPD3 scan/import and registration → installed-package query → persisted
`LearningApplicationContext` → `StudyFacade` queue/reveal/review/progress/completion → restart
and one-step undo. Keep malformed-package, transaction-failure, renderer, keyboard, focus, and
accessibility assertions in their focused owning suites.

## Desktop shell and navigation

**Responsibility**

- screen composition and navigation state;
- global shortcuts and active-screen refresh;
- persistent header/status presentation;
- focus and screen-level wiring.

**Read when changing**

Learning shell, content host, navigation state, shortcut routing, screen composition roots,
and cross-screen tests.

## Desktop runtime foundation

**Responsibility**

- application identity and generated build metadata;
- platform-aware runtime directories;
- typed Desktop configuration;
- logging, lifecycle, and runtime diagnostics.

**Read when changing**

`desktop/build.gradle.kts`, `desktop.runtime`, `DesktopMain`, Desktop composition, Settings/About
presentation, and runtime-focused unit/integration/restart tests.

**High-risk contracts**

Stable application/directory identity, no implicit data migration, corrupt-config preservation,
log privacy/retention, deterministic startup/shutdown, and support diagnostics.

**Windows launcher boundary**

`desktop/build.gradle.kts` owns jlink modules and the `verifyWindowsLauncher` app-image smoke
task. `DesktopMain` owns the minimal opt-in startup probe. Preserve `jdk.accessibility`, isolated
profile/storage, bundled-runtime execution, timeout, output capture, and nonzero-exit failure.

## Settings and configuration

**Responsibility**

- visible configuration and presentation;
- configuration persistence or runtime wiring when present.

Do not turn the read-only presentation into a mutable contract without implementing
validation, persistence, restart behavior, and tests in the same increment.

## Map maintenance

### Desktop product evolution references

- Android behavior evidence and stable IDs:
  [`ANDROID_PRODUCT_BEHAVIOR.md`](ANDROID_PRODUCT_BEHAVIOR.md).
- Current Desktop gaps: [`DESKTOP_GAP_ANALYSIS.md`](DESKTOP_GAP_ANALYSIS.md).
- Target ownership: [`DESKTOP_PRODUCT_ARCHITECTURE.md`](DESKTOP_PRODUCT_ARCHITECTURE.md).
- Post-1.0 capability sequence:
  [`DESKTOP_PRODUCT_ROADMAP.md`](DESKTOP_PRODUCT_ROADMAP.md).
- Platform-independent product specifications: [`spec/`](spec/), split into session,
  workspace, behavior, interaction, media, and topic-model contracts.

LX-01 initially touches application content/package query projections and
`desktop/ui/contentlibrary`; it must not touch scheduler, session transactions, or persistence
schemas. Later listening work touches learning-content projection, a Desktop media coordinator,
the playback adapter, typed preferences, and composition wiring.

Standing capability workflow lives in [`../AGENTS.md`](../AGENTS.md). Update this map when
source inspection establishes a new production neighborhood, direct dependency, composition
root, or high-risk contract.

## Learning Flow Engine Foundation

- Product Brain: `application/learningobjective` → `application/learningstrategy` →
  `application/flowtemplate`.
- Objective: conservative `DURABLE_RECALL`; Strategy: rotated passive primary plus eligible
  explicit Typing; Template: ordered experiences → reveal → rating-ready.
- Shared model/controller/planner: `application/learningflow`.
- Planner input boundary: immutable `LearningFlowTemplate` + `ExperienceRotationContext`;
  it does not accept `LearningExperiencePlan`.
- Strategy selection: existing `ExperienceSelectionEngine`; round robin selects primary and
  `UserChoiceExperienceStrategy` selects eligible planned Typing.
- Desktop transient owner: `StudyViewModel` → `DesktopLearningFlowCoordinator` → flow fields in
  `StudyUiState` → `StudyScreen`/`DesktopLearningSceneProjector`.
- Reveal synchronization: semantic request → existing `StudyFacade.revealAnswer` → authoritative
  revealed state → rating-ready.
- Explicitly outside: scheduler, FSRS, review commit, queue mutation, persistence, import,
  packages, media resolution/playback, adaptive/AI planning.

## Experience Selection Framework Foundation

- Shared policy neighborhood: `application/learningexperience`; stable semantic input:
  `application/learningcontent/LearningContent`.
- Eligibility boundary: `LearningExperiencePolicy` → ordered `LearningExperienceOptions` inside
  `LearningExperiencePlan`.
- Selection boundary: `ExperienceSelectionRequest` → `ExperienceSelectionEngine` → injected
  `ExperienceSelectionStrategy` → authoritative `ExperienceSelectionResult`.
- Automatic projection: `ExperienceSelectionProfile.AUTOMATIC` retains canonical passive
  Image/Listening/Prompt options and Prompt fallback; `USER_SELECTABLE` retains full eligibility.
- Rotation source: `NextSessionItem` → `ExperienceRotationContext.from` uses the real session ID,
  learning-item ID, and zero-based `LearningSessionProgress.currentPosition`; the legacy
  no-queue path falls back to committed reviews. No rotation field is persisted.
- Desktop boundary: `LearningContentPresenter` resolves media, then
  `DesktopLearningSceneProjector` combines plan, selection result, and
  `LearningContentPresentation` before `LearningSceneRenderer`.
- Consumer wiring: Desktop `StudyScreen`; playback lifecycle remains in
  `LearningContentAudioController`.
- Dependency guard: root policy/selection API may not expose Desktop, Compose, Path, filesystem,
  localized string, playback, scheduler, or persistence types.
- Explicitly unaffected: scheduler, queue, review/evidence, persistence, import, JSON, and PKG
  contracts.

## Typing Recall Vertical Slice Foundation

- Shared semantic boundary: `application/learningexperience/TypingRecall.kt` owns ordered
  expected-answer extraction and deterministic normalized exact evaluation.
- Eligibility boundary: `LearningExperiencePolicy` appends `TYPING_RECALL` after Prompt only
  when a `TypingRecallPrompt` exists.
- Desktop selection/state boundary: `DesktopTypingRecall.kt` owns the explicit chooser,
  identity-keyed transient input/evaluation, and engine-backed user-choice selection.
- Historical foundation projection/rendering: `DesktopLearningSceneProjector` → active
  `TypingScene` → `StudyScreen` input, submit, feedback, reveal, then manual rating. PLE-038/039
  supersedes the exact-success outcome through the existing review transaction.
- Stable identity comes from `StudyFacade` projecting the real `LearningItemId` and immutable
  rotation context into `StudyUiState`; display text is never used as identity.
- Explicitly unaffected: scheduler/FSRS, queue, review evidence, undo, persistence, import,
  package/JSON schemas, media resolution, and playback.

## Adaptive Study Presentation Preferences

- Desktop preference/persistence: `desktop/runtime/StudyPresentationPreferences.kt` and
  `DesktopRuntimeConfiguration.kt`.
- Settings draft, silent preview, and Apply mapping:
  `desktop/ui/settings/StudyPresentationSettings.kt` and `SettingsScreen.kt`.
- Effective policy and playback transition ownership:
  `desktop/ui/study/StudyPresentationPolicy.kt`, `StudyAutoplayCoordinator.kt`, and
  `StudyScreen.kt`.
- Consumer surface: `FocusedAnswerSurface.kt`; primary English remains mandatory while
  bilingual meaning/examples are availability- and preference-gated.
- Explicitly unaffected: Product Brain planning, scheduler/FSRS, queue, review evidence,
  learning persistence, import, package schemas, typography, and shortcut routing.
- PLE-026-R1 staging and quick-control boundary:
  `desktop/ui/component/ContentHost.kt`,
  `desktop/ui/study/StudyPresentationStagingState.kt`, and `StudyScreen.kt`. The current item
  consumes `active`; Settings and quick controls persist `next` through the same runtime callback.
- PLE-026-R2 phase separation: `StudyPresentationPolicy.kt` owns only effective eligibility;
  `LearningSceneRenderer.kt` owns Question/scene layer filtering; `FocusedAnswerSurface.kt` owns
  Answer layers; `StudyAutoplayCoordinator.kt` owns Question/Reveal transition consumption.
- PLE-026-R3 semantic text chain:
  `application/learningcontent/LearningContent.kt` and `LearningContentProjector.kt` assign
  canonical roles; Desktop `LearningContentPresentation.kt`, `LearningScene.kt`, and
  `LearningSceneRenderer.kt` map, project, and filter those roles without language inference.
- PLE-026-R4 answer disclosure and example annotation:
  `desktop/ui/study/FullAnswerPresentation.kt` derives every available Answer field independently
  of Question preferences; `FocusedAnswerSurface.kt` consumes that contract; and
  `ExampleTargetHighlighting.kt` produces exact, non-mutating annotated ranges for semantic
  English/Vietnamese example targets.
- PLE-026-R5 Question/audio/stage integrity:
  `QuestionPresentationRecommendation.kt`, `StudyPresentationPolicy.kt`, `LearningScene.kt`, and
  `LearningSceneRenderer.kt` derive and enforce experience-specific Question permissions;
  `LearningContentPresentation.kt`, `FocusedVocabularyAnswerModel.kt`,
  `FullAnswerPresentation.kt`, `StudyAutoplayCoordinator.kt`, and `StudyScreen.kt` retain complete
  current-item Answer media and truth-mode transitions; `SelectionCandidateFactory.kt`,
  `GetNextLearningItemUseCase.kt`, `NextLearningItem.kt`, `LearningStageDiagnostics.kt`,
  `StudyFacade.kt`, and `StudyUiState.kt` preserve stage authority and explicit persistence
  evidence.
## PLE-028E — Dynamic Part-of-Speech Semantic Color Registry

- `application/partofspeech`: canonical POS normalization, extraction, aggregate inventory,
  semantic identities, deterministic registry, and repository reconciliation.
- `application/contentpackaging/PackageImportService`: optional post-transaction registration of
  canonical POS from successfully imported content; imported content is not rewritten.
- `infrastructure/LearningApplicationFactory` and `LearningApplicationContext`: one startup
  registry/reconciliation composition boundary shared with package import and Desktop.
- `desktop/ui/theme`: theme-only POS palette tokens; no application/domain dependency.
- `desktop/ui/designsystem/pos`: application identity to LETheme style adapter and registry
  CompositionLocal.
- `desktop/ui/study`: the existing metadata and shared Meaning/POS badge consume the single
  adapter. Scheduler, audio, keyboard, responsive and image boundaries are untouched.

## PLE-029 — Robust highlighting and configurable Study audio shortcuts

- `desktop/ui/study/ExampleTargetHighlighting`: sole normalization, original-index mapping,
  boundary and longest-match authority for English and Vietnamese examples.
- `desktop/shortcut/StudyShortcutRegistry`: four version-safe audio commands, defaults, conflict
  validation and legacy preference completion.
- `desktop/ui/study/StudyKeyboardShortcut` and `StudyScreen`: centralized persisted-chord routing
  into existing audio-controller operations; absent paths are deterministic no-ops.
- `desktop/ui/settings/SettingsScreen`: existing Change, Reset and Restore Defaults table projects
  every command and rejects conflicts without silent overwrite.

## PLE-030 — Realtime Study Header Statistics

- `application/packageprogress/StudyHeaderStatisticsQueryService`: immutable scoped aggregate
  over enabled items, effective review events and domain due state with injected clock.
- `LearningApplicationFactory` / `LearningApplicationContext`: one shared projection authority.
- `desktop/ui/study/StudyFacade` / `StudyViewModel`: exact scope resolution, post-success refresh,
  last-known-good failure behavior and nearest-due refresh.
- `StudyHeaderStatisticsPresentation` / `StudyScreen`: localized two-row compact projection using
  LETheme semantic colors and merged accessibility description.

## PLE-030.1 — Study Header Session Progress Semantics Remediation

- `StudyHeaderStatisticsQueryService`: separates `StudySessionProgressStatistics` from
  `StudyPackageLearningStatistics`; combines them only in the immutable header model.
- Session progress consumes frozen policy targets, committed session counters and exact queue
  remaining IDs. Package learned-state consumes latest effective review events and due states.
- `StudyHeaderStatisticsPresentation`: renders New completed/configured and Review
  remaining/configured fractions without exposing raw package inventory.
- `docs/reports/PLE-030_1_SESSION_SEMANTICS_AUDIT.md`: planner, Undo, completion, Continue Learning
  and incomplete Review Mode evidence.

## PLE-039-F — Compact Rating Dock and Seeded New-Item Randomization

- `StudyScreen` / `StudyWorkspaceStrings` / `DesktopStrings`: Typing dock owns only four status
  segments; redundant automatic-rating guidance and journey note are removed.
- `StudyVisualLayoutResolver`: Typing scenes reserve one status-row dock without changing legacy
  rating geometry, accessible target height, or the external dock boundary.
- `SessionSeededNewItemOrderer` / `StudyQueuePlanner` / `StudyQueuePlanningService`: stable
  SessionId-seeded NEW ordering follows strategy placement and precedes diversity/balance and policy limiting;
  REVIEW order, scheduler, FSRS, queue persistence, and restart authority are unchanged.

## PLE-039-G — Reset Answer Surface Scroll on Reveal

- `StudyScreen`: owns the shared presentation-local main-body `ScrollState`; Typing front keeps
  input `BringIntoViewRequester` authority and actual answer-side activation resets to offset zero
  after one layout frame.
- `StudyAnswerScrollTransition`: derives a nullable item-scoped answer identity from the stable
  current item and `canReview` phase authority, cancelling stale effects and excluding timer,
  audio, rating, confidence, disclosure, and resize state.
- `FocusedAnswerSurface`: retains its existing semantic/content order—typed diff and canonical
  identity before image, meaning, examples, scheduler feedback, and continuation—with no nested
  scroll authority or learning behavior change.

## PLE-030.2 — Study Statistics Header Visual Refresh

- `StudyHeaderStatisticsPresentation`: typed ordered metrics, separately styled fractions,
  zero/active emphasis, localized subtitles and meaningful accessibility sentences.
- `StudyStatisticsDashboard`: unified non-interactive eight-segment LETheme surface; reuses
  `StudyViewportClass` for Standard/Wide 8-column and Compact 4+4 layouts.
- `LETypographyTokens`, `LEIconsTokens`, and `LESurfaceVariant.STATISTICS`: reusable metric
  hierarchy, semantic icon identities and compact dashboard container.
- Statistics query, Facade/ViewModel state, scheduler, queue, review and persistence boundaries
  are unchanged.

## PLE-030.3 — Session Classification and Review Cue Remediation

- `SessionPolicyLimiter` / `StudyQueuePlanningService`: retain each admitted plan entry until
  immutable `SessionItemOrigin` is assigned.
- `StudyQueuePlan` / `StudyQueueSnapshot` / `StudyQueueRecord`: carry origin through policy,
  runtime queue and schema-v2 persistence; schema v1 remains readable.
- `ReviewSessionItemUseCase` / `StudySession`: admission origin owns exactly-once New/Review
  counters and Undo validation.
- `StudyHeaderStatisticsQueryService`: persisted origin owns remaining New/Review partition;
  history inference is legacy fallback only.
- `StudyFacade` / `CurrentStudyItemReviewContext` / `StudyScreen`: repository lookup stays
  outside Compose; REVIEW may show one semantic previous-rating underline.
- `StudyVisualLayoutResolver`: remains the sole viewport authority and reserves dashboard/header
  plus dock space before answer-image budgeting.
- `docs/reports/PLE-030_3_SESSION_CLASSIFICATION_AUDIT.md`: root cause, authority flow,
  compatibility and out-of-scope boundaries.

## PLE-030.4 — Content-Level Learning Progress and Review Context

- `ContentLearningStateQueryService`: sole application authority for learned state, sibling IDs,
  latest effective event/rating and authoritative order per learner + Content.
- `StudyQueuePlanner` / `StudyQueuePlanEntry` / `SessionPolicyLimiter`: content-aware candidate
  classification and unique-Content quota counting without changing scheduler memory.
- `StudyQueuePlan` / `StudyQueueSnapshot` / schema-v3 `StudyQueueRecord`: persist LearningItem
  execution identity together with Content progress identity and origin.
- `StudySession` / `ReviewSessionItemUseCase`: content-first New/Review counters with exact
  LearningItem transaction and Undo metadata retained.
- `StudyHeaderStatisticsQueryService`: unique Content Total, latest rating bucket and remaining
  workload projection.
- `StudyFacade` / `CurrentStudyItemReviewContext`: immutable content-level previous rating;
  Compose remains repository-free.
- `docs/reports/PLE-030_4_CONTENT_IDENTITY_AUDIT.md`: UAT reproduction, corrected authority,
  persistence, performance and compatibility evidence.
## PLE-030.5

- Rating context: `RatingDockPresentation.kt`, `StudyScreen.kt`.
- Responsive authority: `StudyVisualLayout.kt`.
- Workload/restart: queue planning, snapshot, schema-v4 record and mapper.
## PLE-030.6 — Compact Height and Content Introduction

- Domain/persistence: `StudySession.introducedContentIds`, additive legacy-compatible
  `StudySessionRecord` mapping, and `LearningEngine.completeContentIntroduction`.
- Desktop flow: `ContentIntroductionState`, `StudyFacade`, `StudyViewModel`,
  `DiscoveryFrontSurface`, and the existing `LearningContentAudioController`.
- Layout/header: `StudyVisualLayoutResolver`, `StudyScreen`, statistics presentation/dashboard,
  fixed chrome reserves, and the single center scroll container.
- Focused tests: `StudyVisualLayoutResolverTest`, `StudyHeaderStatisticsPresentationTest`,
  `ContentIntroductionPresentationTest`, `StudySessionRecordMapperTest`, and existing audio tests.
## PLE-030.7 — One-step Introduction and Compact Chrome

- Atomic state: `StudySession.completeIntroductionAndReveal`,
  `LearningEngine.completeContentIntroduction`, `StudyFacade`, and `StudyViewModel`.
- Flow policy: `shouldRevealAnswerAfterIntroduction` skips only a single non-Typing front;
  planner-owned mandatory input and later recall flows remain intact.
- Chrome/layout: `StudyVisualLayoutResolver`, `StudyScreen`, and shared `LEButton` compact
  padding/minimum-target behavior.
- Focused tests: `ContentIntroductionPresentationTest`, `StudySessionRecordMapperTest`,
  `StudyVisualLayoutResolverTest`, `StudyVisualUatRemediationTest`, and
  `StudyCompactChromeTest`.
## PLE-030.8 — Answer Dock Restoration

- Flow authority: `DesktopLearningFlowCoordinator` reconciles persisted authoritative reveal
  with normal `LearningFlowController.initializeRevealed` Rating Ready state.
- Dock projection: `StudyActionDockMode` and `StudyScreen.ActionDock` use one shared answer
  action path; no Introduction-specific rating dock exists.
- Focused coverage: `DesktopLearningFlowCoordinatorTest`, `StudyKeyboardShortcutTest`,
  `StudyCompactChromeTest`, and existing review/restart suites.
## PLE-030.9 — Session Goals and Review Memory

- Study entry authority:
  `LearningShell.refreshDestination(STUDY)` → `StudyViewModel.enterStudy()` →
  `StudyFacade.enterStudy()`.
- Goal invalidation identity:
  `StudySessionGoalFingerprint(newItemLimit, reviewItemLimit)` compares active immutable policy
  with the latest persisted policy; unrelated runtime configuration is excluded.
- Stale-session transition:
  finish the old session, preserve package/topic/lesson scope and history, clear transient
  presentation state, then create a zero-counter replacement whose policy drives planner,
  queue, limits, and header.
- Full Answer height authority:
  weighted `StudyScreen.BoxWithConstraints` body → `FullAnswerFitLayout` pass-one required-block measurement →
  exact remaining image constraint → fixed-dock-safe placement or scroll continuation.
  `StudyVisualLayoutResolver` retains presentation density/width policy, and
  `LearningSceneRenderer` retains the existing pre-answer image bounds.
- Review counter:
  persisted `StudyQueueProgress.effective*Workload` + `StudySession.*ItemsReviewed` →
  `StudySessionProgressSource` → `StudyHeaderStatisticsQueryService`; remaining identities are
  diagnostics only.
- Statistics density:
  actual `StudyHeaderStatisticsRow.BoxWithConstraints` width →
  `StudyStatisticsLayoutPresentation` standard/compact-inline tokens →
  `StudyStatisticsDashboard` surface/row/metric composition. Actual Compose header measurement
  then determines the weighted body height consumed by `FullAnswerFitLayout`.

- Full Answer Identity density:
  `VocabularyIdentitySurface.BoxWithConstraints` actual card width →
  `StudyIdentityPresentation` standard-inline/compact-inline/stacked tokens →
  `InlinePronunciationRow`. The measured Identity height then participates normally in
  `FullAnswerFitLayout`; the pre-answer renderer does not consume this authority.

- Adaptive Study Chrome:
  actual `ActiveSessionChrome` / `StatusStrip` usable width →
  `StudyChromePresentation` standard/compact/minimum tokens →
  text or fixed-square icon top actions plus semantic `StudyShortcutStatusItem` rendering.
  `ShortcutRegistry` remains command authority and the fixed strip remains below Rating Dock.
- Quick Action Toolbar rendering:
  `StudyShortcutStatusItem.command` → semantic rating/Replay/Undo action component →
  existing rating, audio replay, and Undo callbacks. Chords remain registry-owned metadata for
  tooltip/accessibility only; session status is a separate success indicator.

## PLE-031 — Live Audio Shortcut Toolbar

- Live authority:
  `SettingsScreen.StudyShortcutSetting` → persisted
  `DesktopRuntimeConfiguration.studyShortcuts` → `ContentHost` →
  one `StudyScreen.shortcutRegistry` snapshot → toolbar projection and keyboard dispatcher.
- Shared formatting:
  `ShortcutChordFormatter` serves `DesktopKeyChord.displayName`, Settings, toolbar chord labels,
  tooltips, and accessibility.
- Execution/availability:
  `FocusedVocabularyAnswerModel.presentationAvailability` → `StudyShortcutAudioPaths` →
  enabled toolbar actions → existing `performStudyAudioKeyboardAction`.
- Adaptive composition:
  `StudyChromePresentation.maximumShortcutItems` → required visible actions plus
  `StudyAudioOverflow`; fixed strip height and Rating/Replay/Undo/Session groups are preserved.
- Semantic audio identity remediation:
  `StudyShortcutCommand` → `resolveStudyToolbarActionIcon` →
  stable `StudyToolbarSemanticIcon` plus optional `VI`/`VI+` badge →
  `StudyAudioQuickAction`.
- Live audio cue remediation:
  `DesktopRuntimeConfiguration.studyShortcuts` → `resolveStudyShortcutStatus` →
  formatter-owned `chordText`/`compactLabel` → `resolveStudyAudioToolbarCue` →
  same-row main cue, structured tooltip/accessibility, and icon/name/chord overflow. Registry
  chords bypass the semantic icon resolver.

- Goal composition: `DesktopRuntimeSession.loadStudySessionPolicy` →
  `LearningApp`/`LearningShell` → `StudyFacade` → immutable `StudySession.policy`.
- Header/queue authority: `StudyFacade.resolveSessionProgressSource` and the queue consume the
  same persisted session policy.
- Review-memory projection: Content-level `CurrentStudyItemReviewContext`,
  `RatingDockPresentation`, `StudyActionDockMode`, and `StudyScreen.ActionDock`.
- Final remediation: semantic pre-answer REVIEW eligibility no longer depends on
  `canRevealAnswer`; `StudyVisualLayoutResolver`, `LearningSceneRenderer`, and
  `VocabularyImageBlock` share adaptive image bounds.
- Focused coverage: `DesktopRuntimeLifecycleTest`, `GeneralStudyContinuationIntegrationTest`,
  `CurrentStudyItemReviewContextTest`, `RatingDockPresentationTest`, and
  `StudyReviewMemoryDockTest`.
# LQ-003 source boundary

- `application/study/SessionPolicyLimiter`: Content-unique representative selection and exact New/Review quotas.
- `domain/study/session/StudySession` plus persistence mapper/record: durable, undoable same-session lapse ancestry.
- Desktop Study typing context/resolver: authoritative lapse projection, validation, preview, popup, and commit decision consistency.
LQ-005C adds `application/learninginsight` as a read-only composition boundary over the existing
trajectory repository, difficulty calculator, adaptive strategy, and optional promotion decision.
Desktop `ui/study` maps typed presentation tokens to localized text and renders them without policy.
LQ-006A adds `domain/study/recall` for portable immutable contracts and `application/recall` for
Content capability projection, pure contract validation, evidence placeholder policy, and the stable
wire DTO codec. The boundary depends on existing Content/identity/time values and has no Desktop,
Compose, filesystem, persistence, Scheduler, or Evidence execution dependency.
LQ-006B extends `domain/study/recall` with the immutable capability set/projection and adds the pure
`application/recall/ContentRecallCapabilityResolver` plus its portable wire codec. It reuses LQ-006A
types and existing Content; no Desktop, filesystem, learner, Evidence, Scheduler, or persistence
boundary is introduced.
