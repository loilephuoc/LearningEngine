# Capability Map

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
- Desktop projection/rendering: `DesktopLearningSceneProjector` → active `TypingScene` →
  `StudyScreen` input, submit, feedback, reveal, then unchanged manual rating.
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
