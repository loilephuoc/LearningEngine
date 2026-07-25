# Learning Engine 2.0 — AI Architect Context

Short-term repository and Phase snapshot only. Standing workflow is defined in
[`../AGENTS.md`](../AGENTS.md).

## Repository

- Repository: `loilephuoc/LearningEngine`
- Branch: `develop`
- HEAD before Foundation Freeze audit: `89e2d29`
- HEAD after Foundation Freeze audit: `4eec095`
- Baseline HEAD before platform-independent product specification:
  `37c10b881d31379d6c8ea49090f04bfa1fe3f41c`
- Baseline `origin/develop`: `e119e0588f48f59ba8b8e83873501846979dce12`
  (local branch was two commits ahead).
- `reference/android/` is tracked product evidence at this baseline.
- Baseline commit message: `docs: define Desktop product architecture from Android behavior`.

## Phase State

- Phase 5 — Desktop Beta Readiness: implementation/local automation complete; Product Owner
  clean-machine install/launch/flow/recovery/uninstall/reinstall/upgrade/signing evidence remains
  pending in [`BETA_RELEASE_CHECKLIST.md`](BETA_RELEASE_CHECKLIST.md).
- Phase 6 — Learning Experience: implementation complete; lifecycle, Review Workspace, Learning
  Content Model foundations, and automated end-to-end evidence through P6-09 are complete;
  manual evidence is pending.
- Phase 7 — Desktop Beta Validation and v1: all repository-driven stabilization is complete;
  manual, clean-machine, distribution, signing, and real-user evidence remains pending.
- Phase 6 definition and exit criteria:
  [`ROADMAP.md`](ROADMAP.md#phase-6--learning-experience).

## Verified Starting Boundary

- Domain `StudySession` has `ACTIVE` and `FINISHED` states, immutable lesson scope, reviewed
  item/content sets, new/due counters, limits, and finish invariants.
- Application services own start, next-item selection, atomic review, finish, queue progress,
  and persisted active-session reconciliation.
- `ActiveStudySessionRecovery` distinguishes no session, resumable session, missing queue, and
  already-completed queue.
- `StudySession` owns durable current item, presentation time, reveal state, and at most one
  pending review intent; Desktop only caches a rendering projection.
- Persisted import-to-lesson-study, restart/resume, grading, completion, queue isolation, and
  recovery coverage already exists and must remain green.

## Current Capability

- **PLE-010 — Library Navigation Recovery** complete on `develop`:
  - **Canonical Library Root Recovery:** Established `ContentLibraryViewModel.resetLibraryNavigationState()` and `StudyFacade.dismissCompletionPresentation()` (exposed via `StudyViewModel`). Unified `LearningShell.kt` to trigger the same canonical navigation flow when clicking sidebar "Thư viện", pressing F5, or triggering "Back to Library" callbacks.
  - **Completion Presentation Dismissal:** Separated completion UI presentation from domain completion evidence. Dismissing completion UI clears `sessionCompleted` presentation without deleting or mutating persisted session records, review history events, or scheduler state. New sessions reset dismissal so subsequent completions render normally.
  - **Active/Paused & Restart Invariants:** Verified active/paused sessions remain bound to their original package when returning to Library. App restart on a fresh `StudyFacade` preserves existing session recovery contracts without introducing new persisted schema fields.
  - **Verification:** `.\gradlew.bat --no-daemon clean test -D"org.gradle.jvmargs=-Xmx4g"` — BUILD SUCCESSFUL in 1m 57s. Total XML-verified tests: **2102 passed, 0 failed**.

- **PLE-009R2 — Real UI Package Authority & Navigation Remediation** complete on `develop`:
  - **Real UI Study Package Authority (Defect A):** Updated `StudyFacade.kt` (`restoreLatestUndoableCompletion` & `createIdleUiState`) so that when idle (no active/paused session), canonical Library `activePackageId` strictly governs Study. Completed undoable sessions from other packages are rejected when idle and cannot override the active package.
  - **Trapped Navigation Remediation (Defect B):** Wired `LearningShell.kt` so that navigating to `CONTENT_LIBRARY` (via sidebar click or F5) explicitly resets `learningWorkspaceUiState` and `lessonBrowserUiState`, restoring the root Library view. Added `FocusRequester` + `LaunchedEffect` in `LearningWorkspaceCard.kt` and `LessonBrowserCard.kt` ensuring keyboard `Key.Escape` works deterministically.
  - **Scope & Non-Defects:** Duplicate import atomicity verified intact (duplicate import rejected, no false Completed stage). Content Editor & single-lesson preview remain expected/out-of-scope capabilities.
  - **Verification:** `.\gradlew.bat --no-daemon test` — BUILD SUCCESSFUL. Focused & full test suite 100% passed.

- **PLE-009 — Desktop Package Selection & Navigation Stabilization** complete on `develop`:
  - **Active Package Synchronization & Authority:** Enforced state authority: `ACTIVE` or `PAUSED` study sessions remain authoritative and bound to their original package. When idle, canonical Library `activePackageId` is authoritative. Idle `StudyFacade` clears stale cached package identity and projects the newly active Library package without leaking previous package content. Preserved active/paused session safety and explicit non-mutating multi-package lesson launching.
  - **Browse Lessons Batch Query Optimization:** Added `findByContentIds` set-matching to `LearningItemRepository`, `InMemoryLearningItemRepository`, `StoreBackedLearningItemRepository`, and `LearningEngine`. `PackageLearningProgressQueryService` batch-queries all package learning items in 1 batch repository query instead of ~400 repeated per-content queries (verified `findByContentIds` call count = 1, `findByContentId` call count = 0).
  - **Duplicate-Import Atomicity & Error Sanitization:** Verified duplicate import failures preserve all authoritative state (catalog, active package, selected package/lesson, navigation, Study state). Enforced `PackageImportProgressStage.COMPLETED` is never emitted on failure, and error messages are sanitized into bounded, concise summaries.
  - **Acceptance & Navigation Verification:** Delivered acceptance coverage proving single-load Browse Lessons execution, loading/busy guards, single catalog addition on import, and full `Library` -> `Lesson Browser` -> `Workspace` -> `Back` navigation preserving package context.
  - **Verification:** `.\gradlew.bat --no-daemon clean test` — BUILD SUCCESSFUL in 1m 58s. Total XML-verified tests: **2089 passed, 0 failed**.

- **PLE-008 — Session Completion & Reflection Foundation** complete:
  - **Baseline:** Built on PLE-007 baseline commit `eea032b562fcfae08352cee49a6cab4b877dca83`.
  - **Session Completion Presentation Projection:** Created `SessionCompletionUiState`, `SessionCompletionStatus`, `SessionCompletionProjectionPolicy`, and `SessionCompletionCard` in `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/`.
  - **Authoritative Status & Reflection Mapping:** Maps domain completion state to `SessionCompletionStatus` (`COMPLETED`, `PAUSED`, `STOPPED`, `ABANDONED`, `INTERRUPTED`). Generates pure reflection message based on authoritative review metrics and lesson progress without artificial scores, AI text, gamification, or FSRS mutations.
  - **Navigation & Recovery:** Renders `SessionCompletionCard` when session ends. Provides clear navigation controls: (1) **Back to Lesson** (navigates to Content Library for package/lesson selection with zero session creation), (2) **Back to Library** (clears package browser and completion state), (3) **Continue Learning** (navigates to Content Library and opens PLE-007 Learning Workspace without auto-starting a session).
  - **No Lifecycle / Scheduler / Persistence Alteration:** Zero changes to scheduler, queue planning, session lifecycle, package ownership, progress calculation, FSRS algorithm, or persistence schema.
  - **Verification:** `.\gradlew.bat --no-daemon clean test` — BUILD SUCCESSFUL in 1m 13s, 15 actionable tasks executed, 100% tests passed.

- **PLE-007 — Learning Workspace Foundation** complete:
  - **Baseline:** Built on PLE-006 baseline commit `983e435c17d933b14ab023926da347634e7127a1`.
  - **Learning Workspace Presentation Projection:** Created `LearningWorkspaceUiState`, `SessionPreviewStage`, `SessionPreviewFactory`, `LearningWorkspaceProjectionPolicy`, and `LearningWorkspaceCard` in `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/contentlibrary/`.
  - **State Ownership & UX Flow:** Owned by `ContentLibraryViewModel.learningWorkspaceUiState` as transient presentation state. Clicking CTA in Lesson Browser opens Learning Workspace without creating a `StudySession`. Clicking "Back" returns to Lesson Browser with zero session creation. Clicking "Start Learning" reuses the exact existing `StartPackageLessonStudyRequest` boundary with single-invocation busy guard protection (`isStartingSession`).
  - **Session Preview:** Based on authoritative `LearningFlowTemplateStage` types (`Recall Prompt` -> `Reveal Answer` -> `Rate Recall`), without instantiating runtime scenes or consuming queue items.
  - **No Lifecycle / Scheduler / Persistence Alteration:** Zero changes to scheduler, queue planning, session lifecycle, package ownership, progress calculation, FSRS algorithm, or persistence schema.
  - **Verification:** `.\gradlew.bat --no-daemon clean test` — BUILD SUCCESSFUL in 1m 15s, 15 actionable tasks executed, 100% tests passed.

- **PLE-006 — Recommended Next Lesson** complete:
  - **Baseline:** Built on PLE-005 baseline commit `383e0f42ef7e65952583bb930641c7d22c039130`.
  - **Deterministic Recommendation Policy:** Created `PackageLearningRecommendation`, `RecommendationReasonType`, and `PackageLearningRecommendationPolicy` in `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/contentlibrary/`.
  - **Priority Order & Tie-Breaker:** Strict priority: (1) `DUE_NOW`, (2) `CONTINUE_IN_PROGRESS`, (3) `START_NEW`, (4) `REVIEW_COMPLETED`, (5) `NONE`. Canonical package lesson order is the sole tie-breaker. Lessons with 0 items are skipped.
  - **Desktop UI Integration:** Projected recommendation into `LessonBrowserUiState` and rendered compact Recommendation Card in `LessonBrowserCard`. Clicking "Select Lesson" selects the exact recommended `ContentId` (resetting search refinements if hidden) without starting a study session.
  - **No Lifecycle / Scheduler Alteration:** Zero changes to scheduler, queue planning, session lifecycle, package ownership, progress calculation, persistence schema, or learning algorithm.
  - **Verification:** `.\gradlew.bat --no-daemon clean test` — BUILD SUCCESSFUL in 1m 15s, 15 actionable tasks executed, 100% tests passed.

- **PLE-005 — Progress-Aware Lesson Study Entry** complete:
  - **Deterministic Presentation Policy:** Created `LessonStudyAction` and `LessonStudyActionPolicy` in `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/contentlibrary/`.
  - **Action Guidance Rules:**
    - `UNAVAILABLE` (`total == 0`): CTA disabled, label `"No Learning Items"`.
    - `START` (`total > 0 && started == 0 && mastered == 0`): CTA enabled, label `"Start Lesson"`.
    - `CONTINUE` (`started > 0 && mastered < total`): CTA enabled, label `"Continue Lesson"`.
    - `REVIEW` (`total > 0 && mastered == total`): CTA enabled, label `"Review Lesson"`.
  - **Due Indicator:** Exposes `dueText` (e.g. `"<n> item(s) due now"`) in selected lesson summary without altering action policy or CTA action/label.
  - **No Lifecycle / Scheduler Alteration:** Zero changes to scheduler, queue planning, session lifecycle, package ownership, progress calculation, persistence schema, or learning algorithm.
  - **Verification:** `.\gradlew.bat --no-daemon clean test` — BUILD SUCCESSFUL in 1m 13s, 15 actionable tasks executed, 100% tests passed.

- **PLE-004 — Package Learning Progress** complete:
  - **Application Service & Metrics:** Implemented `PackageLearningProgressQueryService` calculating package and lesson progress metrics (total, unseen, NEW stage, started, mastered, due, suspended, completion percentage, started percentage).
  - **Package-Scoped Content Lookup:** Created `InstalledPackageContentQueryService` for single-query content resolution.
  - **Desktop UI Integration:** Projected progress into `LessonBrowserItem`, `LessonBrowserUiState`, `LessonBrowserCard`, and `ContentLibraryViewModel`.
  - **Verification:** `.\gradlew.bat --no-daemon clean test` — BUILD SUCCESSFUL. Commit `f6602ff` on `develop`.

- **PLE-003-R2 — Persist InstalledPackage Provenance in StudySession** complete:
  - **Domain Session Provenance (Part A / AC-R2-01, AC-R2-05):** Added `val installedPackageId: InstalledPackageId? = null` directly to `StudySession` aggregate and `StudySession.start(...)`. Provenance is immutable and preserved across all lifecycle transitions (`recordReview`, `reveal`, `undo`, `finish`).
  - **Start Session Command Propagation (Part B & C / AC-R2-02 - AC-R2-04):** Propagated `installedPackageId` through `StartStudySessionCommand` and `StartStudySessionUseCase`. `StudyFacade` passes `activeInstalledPackageId` when package validation succeeds, while legacy/general study sessions maintain `installedPackageId == null`.
  - **Persistence & Backward Compatibility (Part E / AC-R2-06 - AC-R2-08):** Extended `StudySessionRecord` with `val installedPackageId: String? = null` and updated `StudySessionRecordMapper`. Legacy JSON session records without `installedPackageId` decode safely to `null` without schema migration.
  - **Recovery & UI Projection (Part F, G & H / AC-R2-09 - AC-R2-13):** Restored active session recovery sets `activeInstalledPackageId = session.installedPackageId`. `StudyUiState` exposes `activeInstalledPackageId: InstalledPackageId?`. Clearing active study state resets transient context without leaking package provenance across sessions.
  - **Verification:** `.\gradlew.bat clean test` — 1,984 tests passed across all modules (385 in desktop module), 0 failures. Commit: `fix: persist package provenance in study sessions`.

- **PLE-003-R1 — Preserve Package Context Through Study Entry** complete:
  - **Removed Fake ID Fallback (Part B / AC-R1-01, AC-R1-02):** Removed `InstalledPackageId(uiState.libraryId)` fallback in `LessonBrowserCard`. If `installedPackageId == null`, Start Lesson button is disabled with explicit feedback `"Package context is unavailable."` without fake ID generation, callback invocation, or crashes.
  - **Package-Aware Study Request & Navigation (Part C / AC-R1-03, AC-R1-04):** Created `StartPackageLessonStudyRequest(installedPackageId: InstalledPackageId, contentId: ContentId)`. `LessonStudyNavigationCoordinator` preserves and forwards `installedPackageId` and `contentId` down to `StudyViewModel.startLessonStudy(request)`.
  - **Application Ownership Validation (Part C & E / AC-R1-05 - AC-R1-08, AC-R1-10):** `StudyFacade` validates: (1) Package exists, (2) Package is `ACTIVE` (not `ARCHIVED` or `REMOVED`), (3) Package belongs to default library, (4) Selected lesson content belongs strictly to target package, (5) Lesson contains enabled learning items. Any validation failure sets recoverable `uiState.loadError` without creating an active session or navigating to `STUDY`.
  - **Verification:** `.\gradlew.bat clean test` — 1,980 tests passed across all modules (381 in desktop module), 0 failures. Commit: `fix: preserve package context through lesson study entry`.

- **PLE-003 — Lesson Browser Product Completion** complete:
  - **Package Context Header (Part B / AC-03-01):** Rendered package name, total lesson & learning item counts, `InstalledPackageId` caption, and "Back to Library" action button in `LessonBrowserCard`. Preserved `installedPackageId` in `LessonBrowserUiState`.
  - **Hierarchical Presentation (Part C / AC-03-03, AC-03-04):** Implemented `groupLessonsHierarchically(lessons)` rendering Group headers and Section subheaders. Applied consistent fallback labels (`"General"` for blank group, `"Other Lessons"` for blank section) without displaying `"null"` or blank titles.
  - **Search & Filter (Part D / AC-03-05 - AC-03-07):** Supported local case-insensitive search across title, primary text, translated text, group, and section. Provided clear search reset. Differentiated Package Empty state (`lessons.isEmpty()`) from Search Empty state (`visibleLessons.isEmpty()`).
  - **Lesson Selection (Part E / AC-03-08, AC-03-09):** Single-select lesson on row click with primary container highlight and "Selected" badge. Selection remains preserved in state when query filter hides selected lesson.
  - **Start Lesson Flow (Part F & G / AC-03-10 - AC-03-13):** Created `PackageLessonSelection` typed context. Rendered bottom action bar with Start Lesson button enabled only when a valid lesson in current view is selected AND has `learningItemCount > 0`. Disabled Start Lesson for 0-item lessons with explicit feedback ("No learning items available for this lesson").
  - **Navigation & Isolation (Part H & I / AC-03-14 - AC-03-16):** Back button returns to Library overview without mutating package state. Loading a package resets search query, filter, sort, and selection, eliminating stale state leakage between browse sessions.
  - **Verification:** `.\gradlew.bat clean test` — 1,971 tests passed (372 in desktop module), 0 failures. Commit: `feat: complete lesson browser experience`.

- **PLE-002-R1 — Correct Package-Scoped Browsing and Active-Package Lifecycle** complete:
  - **Browse Lessons Package-Scoped Contract (Issue A / AC-R1-01 - AC-R1-04):**
    - Removed ambiguous `openLibrary` fallback to first library in `ContentLibraryViewModel`.
    - Implemented typed `browsePackageLessons(installedPackageId: InstalledPackageId, packageName: String)` in `ContentLibraryViewModel` and `LessonBrowserFacade.loadForPackage`.
    - Resolved `InstalledPackageId` -> `InstalledPackage.packageId` -> `ContentPackage` -> `ContentLibraryId` -> `LibraryContentQueryService.queryForLibraries(contentLibraryIds)`.
    - Guaranteed package isolation: Topic A Browse Lessons displays ONLY Topic A lessons; Topic B Browse Lessons displays ONLY Topic B lessons.
    - Non-existent package IDs return clear `loadError` ("Package with id '...' not found.") without fallback or silent failures.
  - **Active Package Lifecycle Consistency (Issue B / AC-R1-05 - AC-R1-09):**
    - **Archive Policy:** `LibraryCommandService.archivePackage` atomically sets `library.activePackageId = null` and saves both `updatedLibrary` and `updatedPackage` inside the same transaction when archiving the current active package. Archiving a non-active package leaves `activePackageId` intact.
    - **Restore Policy:** `LibraryCommandService.restorePackage` restores state to `ACTIVE` but does NOT automatically set `activePackageId` (remains `null` or unchanged).
    - **Sanitizing Invalid Legacy References:** `LibraryQueryService.getNavigationTree` sanitizes `activePackageId = library.activePackageId?.takeIf { id -> activePackages.any { it.id == id } }`. Legacy persisted state referencing missing or non-ACTIVE packages evaluates to `null` safely without app crash or displaying "Current Active" on archived packages.
  - **Verification:** `.\gradlew.bat clean test` — 1,959 tests passed (360 in desktop module), 0 failures. Commit: `fix: correct package scoped browsing and active package lifecycle`.

- **PLE-002 — Complete Library User Experience** complete:
  - **Browse Lessons (Part B / AC-01, AC-02):** Resolved `ContentLibraryViewModel.openLibrary(libraryId)` fallback to available library when invoked from `PackageCard`, connecting `Package` -> `Browse Lessons` -> `LessonBrowserCard` -> `onStartLessonStudy`.
  - **Archive (Part C / AC-03):** Wired `onArchivePackage` and `onRestorePackage` in `LibraryOverviewSection` down to `PackageListSection` so Archive and Restore buttons in Overview section show `ArchivePackageConfirm` / `RestorePackageConfirm` dialogs, executing `LibraryCommandService.archivePackage`/`restorePackage` without silent failures.
  - **Active Package (Part D / AC-04, AC-05):** Added `activePackageId: InstalledPackageId?` to domain aggregate `Library`, application DTO `LibraryNavigationTree`, UI state `LibraryUiState.Content`, and persistence record `CanonicalLibraryRecord`. Added `LibraryCommandService.setActivePackage` command with full transaction and restart persistence in `canonical-libraries.json`. Rendered "Current Active" badge and "Set Active" button on `PackageCard`.
  - **Package Ordering (Part E / AC-06, AC-07, AC-08):** Implemented `movePackageUp` and `movePackageDown` on domain aggregate `Library` and `LibraryCommandService`. Preserved entry order in `LibraryQueryService` for installed/active package lists. Added "Move Up" and "Move Down" action buttons to `PackageCard` with boundary enablement. Persisted entry order to `canonical-libraries.json` across app restart.
  - **UX Audit & Polish (Part A, F / AC-09, AC-10, AC-11):** Audited all `PackageCard` actions. Ensured zero unresponding silent clicks. Verified no regressions in Import, Remove Topic, or Restart Persistence.
  - **Verification:** `.\gradlew.bat clean test` — 1,954 tests passed (357 in desktop module), 0 failures.

- **PLE-001C-R1 — Restore File-Scoped Import and Installed Topic Removal** complete:
  - Implemented `JvmFileScopedPackageScanner` supporting single file selection (`.opd3`, `.pkg`, `.json`), resolving same-basename companion pairs (`<base-name>.json` and `<base-name>.pkg`), and throwing `MissingOpd3JsonPairException` without partial persistence when a companion is missing.
  - Updated `ContentPackageImportFactory.createScanner` to route single files to `JvmFileScopedPackageScanner` and directories to `JvmDirectoryPackageScanner`.
  - Updated `PackageDirectoryChooser.kt` to `choosePackageFile` with `FILES_ONLY` and extension filter `*.opd3, *.pkg, *.json`.
  - Implemented explicit installed topic removal in `PackageUninstallOperation` with full domain reconciliation across `installedPackageRepository`, `libraryRepository`, and `collectionRepository`.
  - Wired `UninstallContentPackageUseCase` in `LearningApplicationContext` and `LearningApplicationFactory`.
  - Updated `ContentLibraryFacade` and `ContentLibraryViewModel` with `uninstallPackage(packageId, packageName)` which removes the installed package, clears `lessonBrowserUiState = null` if the removed package was open, and reloads library state.
  - Rendered `Remove Topic` action button on installed package cards with Compose Material3 confirmation `AlertDialog` detailing package name and data impact.
  - Expanded `CanonicalDesktopImportIntegrationTest.kt` with 17 comprehensive automated unit and integration tests covering file-scoped chooser, OPD3 import, JSON/PKG pair resolution, single-file isolation, missing companion failure, conflict behavior, confirmation presentation, full persistence reconciliation, failure state preservation, package A/B isolation, collection assignment reconciliation, lesson browser clearing, restart persistence, re-import after removal, architecture dependency guards, and negative export UI check.
  - Verification: `.\gradlew.bat clean test` — 1,949 tests passed (354 in desktop), 0 failures. Commit: `fix: complete desktop import and topic removal flow`.

- **PLE-001C — Restore Canonical Desktop Import Entry and Product Flow** complete:
  - Integrated canonical `LibraryScreen` (`desktop/ui/library`) with `ContentLibraryViewModel` import pipeline (`desktop/ui/contentlibrary`), restoring full user-facing import entry without fallback switches.
  - Added `PackageDirectoryChooser.kt` using `javax.swing.JFileChooser` (`DIRECTORIES_ONLY`).
  - Added prominent `Import Package` action buttons in `LibraryHeader` and `LibraryEmptyView`.
  - Added `Browse Lessons` action button to `PackageListSection` cards, exposing `LessonBrowserCard` for imported package browsing, search, selection, and starting lesson study (`onStartLessonStudy`).
  - Rendered non-blocking import progress card (`ContentLibraryOperation.Importing`) and import success/error banners with actionable retry/clear controls.
  - Synced imported packages in `ContentLibraryFacade` to `ConflictAwarePackageImporter` for `defaultLibraryId`, reconciling canonical library navigation tree and content projection.
  - Verified zero imports from `infrastructure` or `adapter` in `desktop/ui/library` and zero Export OPD3 UI/actions.
  - Verification: `.\gradlew.bat clean test` — 1,941 tests passed (346 in desktop), 0 failures. Commit: `fix: restore canonical desktop import flow`.
  - Corrected historical architecture documentation in `AI_ARCHITECT_CONTEXT.md` to eliminate stale mentions of `LearningApplicationContext` in `LibraryFacade`.
  - Verification: `./gradlew clean test` and `./gradlew :desktop:test` BUILD SUCCESSFUL.

- **PLE-001B-R1 — Remove Desktop-to-Infrastructure Dependency and Repair Architecture Evidence** complete:
  - Refactored `LibraryFacade` in `vn.loi.learning.desktop.ui.library` to accept explicit Application ports/services (`LibraryQueryService?` and `LibraryCommandService?`) via constructor dependency injection.
  - Eliminated `LearningApplicationContext` and all direct `infrastructure` dependencies from `LibraryFacade` and the canonical Desktop Library consumer layer (`desktop -> application -> domain`).
  - Updated production composition root in `LearningShell.kt` to extract Application query and command services and inject them explicitly into `LibraryFacade`.
  - Strengthened architecture dependency guard in `LibraryViewModelTest`: scans all `.kt` files under `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/library` and fails on any `import vn.loi.learning.infrastructure` or `import vn.loi.learning.adapter`, and asserts zero forbidden Infrastructure/Factory mentions in `LibraryFacade.kt`.
  - Verified production composition wiring test proving `LibraryFacade` operates cleanly with explicit Application dependencies.
  - Verification: `./gradlew clean test` and `./gradlew :desktop:test` BUILD SUCCESSFUL.

- **PLE-001B — Desktop Canonical Library Command Experience** implemented:
  - Extended `LibraryFacade` to expose all 7 canonical Library commands through `LibraryCommandService` via explicitly injected Application services (`LibraryQueryService` and `LibraryCommandService`).
  - Implemented `LibraryViewModel` command controller managing state transitions, concurrency guard (`isBusy`), post-mutation refresh, and selection reconciliation.
  - Created `LibraryDialogState` and `LibraryDialogHost` supporting 7 user interactions: Create Collection, Rename Collection, Soft-delete Collection, Assign Package, Remove Assignment, Archive Package, Restore Package.
  - Added deterministic typed error mapping in `LibraryFailureMessage.forCommandResult(...)` switching on `LibraryCommandResult` sealed hierarchy without exception message parsing or leaking internal paths.
  - Added 12 comprehensive automated unit and integration tests in `LibraryViewModelTest` covering all success cases, typed failures, persistence rollback/failures, duplicate click prevention, persisted application context restart, and layer dependency guards.
  - Zero imports of `infrastructure` persistence classes in `desktop` library UI, zero `desktop` or `infrastructure` imports in `application`.
  - Verification: `./gradlew clean test` and `./gradlew :desktop:test` BUILD SUCCESSFUL.

- **PLE-001A-R2 — Persist Canonical Library State and Prove Command Transactions** complete:
  - Implemented persistent store-backed repositories for canonical Library platform:
    - `StoreBackedCanonicalLibraryRepository` & `JsonCanonicalLibraryStore` (`canonical-libraries.json`).
    - `StoreBackedCanonicalCollectionRepository` & `JsonCanonicalCollectionStore` (`canonical-library-collections.json`).
  - Wired into `LearningApplicationFactory.createPersisted(...)` and managed by `JsonFileTransactionRunner`.
  - Implemented safe default library bootstrap policy (loads existing without overwrite, creates/reconciles once).
  - Replaced `catch (e: Throwable)` with JVM-safe `catch (e: Exception)` in `LibraryCommandService`.
  - Comprehensive integration tests in `LibraryCommandIntegrationTest`: Test A (active collection round-trip), Test B (soft-delete round-trip in `DELETED` state), Test C (library registration round-trip), Test D (command-level multi-file rollback without partial state), Test E (default library bootstrap preservation).
  - Application layer contains zero imports of `infrastructure`, `adapter`, or `desktop`.
  - Verification: `./gradlew clean test` and `./gradlew :desktop:test` BUILD SUCCESSFUL.

- **FFR-001 — Foundation Freeze Remediation** complete:
  - Audited dependency flow between `desktop`, `application`, `domain`, and `infrastructure`.
  - Confirmed LD-008 is a real architectural violation (3 Application files importing Infrastructure directly, 1 Application file importing Adapter directly).
  - Remediated Application layer dependency direction:
    - Extracted application ports: `LegacyJsonImporter`, `DeterministicZipWriter`, `LegacyImportResult`, `LegacyImportException`, `DeterministicZipEntry`, `Sha256PackageIdGenerator` in `vn.loi.learning.application`.
    - Infrastructure implements application ports (`JvmDeterministicZipWriter`, `LegacyJsonImporter` in infrastructure with backward-compatible typealiases).
    - Refactored `LegacyPairCanonicalConverter`, `Opd3PackageExporter`, `LegacyJsonImportService` in `application` layer to eliminate ALL `infrastructure` and `adapter` imports.
  - Zero `infrastructure` or `adapter` imports remain in `application`.
  - Build: SUCCESSFUL. Tests: all passed.

- **Foundation Freeze Audit** complete (commit `4eec095`):
  - Audited toàn bộ Domain, Application, Infrastructure layers (611 source files).
  - Xóa 5 dead code files (150 lines): toàn bộ `domain/study/fsrs/evolution/` package (3 files không có caller), `InMemoryContentLibraryStore`, `InMemoryKnowledgeGraphStore`.
  - Fix indentation regression trong `LearningApplicationFactory` (3 vị trí bị mất indent).
  - Chuẩn hóa imports trong `LearningApplicationContext` (2 FQN → top-level import).
  - Fix 11 redundant explicit casts trong `KnowledgeGraphAnalyzerTest` và `KnowledgeGraphDomainTest` (→ smart casts).
  - Documented 10 Large Debt items (LD-001 đến LD-010) cho Chief Architect review:
    - **LD-008 (Resolved in FFR-001):** Application layer direct Infrastructure imports eliminated.
    - LD-001: `application/dashboard/` dead package (predecessor của `learningdashboard`)
    - LD-002: Content.library vs domain.library naming overlap
    - LD-003: `ContentSearchRepository` + `StoreBackedContentSearchRepository` không được wire
    - LD-004: File name constant duplication giữa 3 factory files
    - LD-005: `LearningApplicationContext` optional fields luôn được khởi tạo
    - LD-006: Domain repos inline trong `createContext()`
    - LD-007: Inline FQN trong `LearningApplicationFactory`
    - LD-009: `ContentMediaStorage` port expose `java.nio.file.Path`
    - LD-010: `application/dashboard/` dead/historical package
  - Build: SUCCESSFUL (59s). Tests: 1,587 passed, 0 failed, 0 warnings.

- LP-005 — Knowledge Graph Foundation complete (commits `9976529`, `c18f373`, `d39725a`):
  - Immutable `KnowledgeGraph` aggregate (no learner state, deterministic, internal constructor).
  - `KnowledgeNodeId`, `KnowledgeNodeKind` (5 kinds), `KnowledgeNode` (equality on id+kind), `KnowledgeRelationshipType` (7 types), `KnowledgeEdge`, `KnowledgeGraphValidationIssue` (5 sealed subtypes), `KnowledgeGraphFactory` (typed result), `KnowledgeGraphAnalyzer` (cycle-safe BFS/DFS, shortest path, topological order, transitive successors — all deterministic).
  - Persistence: `KnowledgeNodeRecord`, `KnowledgeEdgeRecord`, `KnowledgeGraphRecord`, `KnowledgeGraphRecordMapper`, `JsonKnowledgeGraphStore` (envelope schema, atomic write, missing-file-safe), `StoreBackedKnowledgeGraphRepository`.
  - Application: `GetKnowledgeGraphUseCase`, `SaveKnowledgeGraphUseCase`, `KnowledgeGraphQueryService`, `InstalledLibraryKnowledgeGraphProjection` (ACTIVE packages → PACKAGE nodes, canonical packageId identity, flat read-only).
  - `LearningApplicationContext` and `LearningApplicationFactory` wired; `installedLibraryGraphProjection` always created in `createContext`.
  - 83 LP-005 tests, 0 failures. Full clean main-module gate (`.\gradlew.bat clean test -x :desktop:test`) BUILD SUCCESSFUL.
- LP-004R — Canonical Import Identity and Typed Conflict Semantics complete (commit `fb36ac4`):
  - Removed `matchByName` from identity resolution; only `PackageId` and `TopicId` are canonical authorities.
  - Added `AMBIGUOUS_EXISTING_IDENTITY` detection: when `PackageId` and `TopicId` each point to a different existing record, the result is a typed `CONFLICT` with no mutation.
  - Conservative identical evidence: `IDENTICAL_PACKAGE` verdict requires both sides to supply a matching canonical `contentChecksum`; absent checksum returns `INSUFFICIENT_IDENTITY_EVIDENCE` conflict.
  - Replaced free-form `List<String>` conflict reasons with typed `PackageImportConflictReason` enum (`TOPIC_ID_MISMATCH`, `AMBIGUOUS_EXISTING_IDENTITY`, `OLDER_VERSION`, `INSUFFICIENT_IDENTITY_EVIDENCE`). Consumer can switch without string parsing.
  - Removed `InstalledPackage.reconstitute` fabrication fallback from `IDENTICAL_PACKAGE` branch; outcome now returns the real aggregate from repository or a `TechnicalFailure` on inconsistency.
  - Added nullable `contentChecksum` field to `InstalledPackage` (backward-compatible default `null`); persisted on `NEW_PACKAGE` and `SAFE_REPLACEMENT` for future fingerprint comparison.
  - Comprehensive test coverage: identity invariant violations, ambiguous identity, checksum comparison, conservative conflict, fabrication prevention, rollback safety, typed reason switching.
- LP-004 — Conflict-Aware Package Import complete (commit `4c0a070`).
- LP-003R.1 — Deterministic Library Failure Mapping complete.
- LP-003R — Library Runtime Identity & Failure Semantics complete.
- LP-003 — Desktop Library Experience complete.
- LP-002 — Library Query & Navigation Foundation complete.
- LP-001 — Library Domain complete.





- Comprehensive test coverage in `MediaPackagingTest`, `Opd3DeterministicExporterTest`, `Opd3PackageInspectorTest`, `Opd3PackageVerifierTest`, and `PackagePlatformRoundTripTest`.
- Remaining roadmap capabilities:
  1. Conflict-aware Import
  2. Workspace
  3. Collections
  4. Archive/Delete

- Beta-L02A — Legacy Pair Discovery & Validation provides the platform-neutral
  `LegacyTopicPairDiscoveryService` and structured `LegacyTopicDiscoveryResult`.
- A valid pair contains exactly one readable/supported JSON and one readable/supported PKG with
  the same case-insensitive logical base name. Results and diagnostic source paths are sorted
  deterministically.
- Diagnostics explicitly represent missing JSON, missing PKG, duplicate JSON, duplicate PKG,
  base-name mismatch, unreadable file, and unsupported format.
- `JvmLegacyTopicFolderReader` is the filesystem adapter and recognizes existing ZIP/OPD3 PKG
  signatures. The capability performs no JSON conversion, media extraction, persistence,
  package serialization, or OPD3 writing.

- Beta-L01 — Topic Identity and Resume State adds a persisted `TopicId` to installed package
  records and an optional topic reference to study-session records.
- Legacy package records without `topicId` derive the same ID from logical package name and
  format on every restart and persist it on their next write. Compatible package replacement
  preserves the current topic ID.
- Learner-topic checkpoint ownership reuses `StudySession` and `StudyQueue`; topic-specific
  recovery queries by `(LearnerId, TopicId)`. `MemoryState`, `ReviewEvent`, scheduler difficulty,
  stability, mastery/review counts and due state remain item-scoped authority.
- Desktop Library/Learn selection resolves topic identity through Application, clears transient
  projection on switch, and resumes the selected topic without leaking the previous topic UI.
- Focused evidence covers identity derivation, package/session mapper round-trip, legacy JSON
  compatibility, compatible replacement, installed OPD3 restart, and Desktop A → B → A switch
  plus restart.
- Next capability: Beta-L02 — Legacy Pair Conversion. Export, conflict-aware update,
  delete/archive, ordering and collection migration remain out of scope.

- Desktop Alpha-04 — Session Completion completes the first Product Brain session loop from bootstrap through persisted completion and Desktop presentation.
- `application/session/completion` owns reflection, learner summary, learning outcome, scheduler rating intent, scheduling projection, and completion result models. `ReviewSessionItemUseCase` remains the scheduler and review transaction owner.
- `StudySession.completionSnapshot` is an optional learner-facing persisted value with schema-v1 defaults for legacy compatibility. Desktop recovery projects the same snapshot after restart.
- `StudyFacade` orchestrates the real Product Brain inputs, established review workflow, session finish, and Desktop projection; `StudyScreen` only renders the core-produced outcome. New study workflows clear stale completion presentation.

- Desktop Alpha-03.5 — Decision Explainability implements Product Brain decision explainability capability.
- Creates `DecisionExplanation` model in `vn.loi.learning.application.decision` articulating observation, decision summary, pedagogical reason, and next step across all 4 adaptive decisions without leaking technical rule IDs.
- Desktop Alpha-03.5R completes the consumer boundary: `StudyFacade` and `StudyViewModel` preserve the current explanation across visibility changes, and `StudyScreen` renders learner-facing content with hide/show controls.
- `DesktopDecisionExplainabilityTest` covers the real `StudyFacade` → `StudyViewModel` → `StudyUiState` path and verifies explanation identity and content survive hide, show, and toggle transitions.

- Desktop Alpha Architecture Review evaluates Desktop Alpha-01, Alpha-02, and Alpha-03 implementations across 12 core architectural areas in `docs/DESKTOP_ALPHA_ARCHITECTURE_REVIEW.md`.
- Confirms a coherent platform-neutral closed adaptive teaching loop, zero UI instructional logic, 100% test pass rate (1,643 tests), stable application contracts, and issues GO recommendations for Alpha-03.5 and Alpha-04. Zero Kotlin source code, UI, or build logic was changed.


- Desktop Alpha-03 — Adaptive Decision implements Product Brain adaptive teaching capability.
- Creates platform-neutral models `AdaptiveAction`, `AdaptiveDecision`, `DecisionTrace`, `AdaptiveOutcome`, and `InstructionalDecisionEngine` in `vn.loi.learning.application.decision`.
- Integrates initial rule set evaluation (`CORRECT`+fast, `INCORRECT`, `PARTIAL`, `MAINTAIN_PACE`), timeline updates, and Desktop UI state projection (`lastAdaptiveDecision`, `lastDecisionTrace`, `currentDifficultyLevel`).


- Desktop Alpha-02 — Scene Execution implements Product Brain single-scene execution capability for `TypingRecallScene`.
- Creates platform-neutral scene contracts (`LearningSceneInput`, `SceneResult`, `LearningEvidence`, `EvidenceReceipt`, `LearningScene`, `TypingRecallScene`) in `vn.loi.learning.application.scene`.
- Integrates `selectFirstScene(...)` and `processEvidence(...)` into `ProductBrainPlanner` and projects scene state in Desktop UI (`StudyUiState`, `StudyFacade`, `StudyViewModel`).


- Desktop Alpha-01 — Session Bootstrap implements Product Brain session bootstrap capability.
- Creates platform-neutral models `LearningSessionContext`, `TeachingGoal`, `SessionTimeline`, `InitialDecisionSnapshot`, and `SessionOverview` in `vn.loi.learning.application.session.bootstrap`.
- Integrates `ProductBrainSessionBootstrap` into `ProductBrainPlanner` and projects `SessionOverview` in Desktop UI (`StudyUiState`, `StudyFacade`, `StudyViewModel`).


- Architecture Audit v1.0 evaluates codebase conformance across 15 core architectural areas in `docs/ARCHITECTURE_AUDIT_V1.md`.
- Concludes **Desktop Alpha Readiness: READY**, supported by 1,639 passing tests, strict inward dependency flow, and zero architecture violations. Established Prioritized Refactoring Backlog. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-03 — Instructional Decision Engine defines the reasoning architecture of Product Brain for making all pedagogical
  decisions in `docs/INSTRUCTIONAL_DECISION_ENGINE.md`.
- Details 11 decision inputs, 10 outputs, Decision Rules matrix across 10 cognitive scenarios, 5-tier priority hierarchy for conflict resolution,
  real-time closed-loop adaptive teaching engine, auditable Decision Trace logging, and Mermaid diagrams. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-02B — Canonical Learning Scene Library defines the complete canonical library of reusable educational interaction capabilities
  available to Product Brain in `docs/LEARNING_SCENE_LIBRARY.md`.
- Details 19-point uniform specification contract, taxonomy systems (11 memory types, cognitive load, duration, difficulty), 10 scene categories (Teaching, Practice, Assessment, Story, Speaking, Medical, Programming, Mathematics, Reflection, Challenge), selection rules, and Mermaid diagrams. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-02A — Learning Scene Framework defines the canonical interaction framework and contract for all Learning Scenes
  in `docs/LEARNING_SCENE_FRAMEWORK.md`.
- Details 12 framework concepts, 9 scene categories, lifecycle state machine (`Created` → `Prepared` → `Running` → `Paused` → `Resumed` → `Completed` / `Cancelled` → `Disposed`), input/output contracts, prohibitions, authority matrix, and Mermaid diagrams. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-01.8 — Learning Experience Architecture defines the end-to-end session journey architecture
  in `docs/LEARNING_EXPERIENCE_ARCHITECTURE.md`.
- Details 7 session phases (Warm-up → Teaching → Practice → Challenge → Review → Reflection → Summary), subsystem orchestration,
  session runtime contracts, motivation safeguarding, and domain walkthroughs. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-01.5 — Knowledge Model Specification defines the canonical, subject-independent Knowledge Model
  specification in `docs/KNOWLEDGE_MODEL.md`.
- Details 15 core knowledge concepts, universal domain mappings (Vocabulary, Stories, Medical Physics, Language, Technical),
  subsystem interaction boundaries, and Mermaid relationship diagrams. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-01 — Product Brain Specification defines the official architectural blueprint and specification
  for the AI Teacher (`ProductBrain`) in `docs/PRODUCT_BRAIN_SPECIFICATION.md`.
- Details the 17 core pedagogical concepts, complete 10-step Teaching Loop, subsystem responsibility matrix, Product Brain
  principles, and multi-year evolutionary roadmap. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-00 — Repository Constitution & Product DNA establishes the repository knowledge system,
  Product Philosophy, Repository Constitution, System Overview, Product Brain conceptual framework,
  Cross-Platform Strategy, AI Design Rules, and Architectural Decision Records (ADRs).
- Documents created/updated: `docs/PRODUCT_PHILOSOPHY.md`, `docs/PRODUCT_BRAIN.md`, `docs/PRODUCT_BRAIN_SPECIFICATION.md`,
  `docs/LEARNING_PRINCIPLES.md`, `docs/CROSS_PLATFORM_STRATEGY.md`, `docs/SYSTEM_OVERVIEW.md`, `docs/REPOSITORY_CONSTITUTION.md`,
  `docs/AI_DESIGN_RULES.md`, `docs/adr/ADR-0001` through `ADR-0004`, `README.md`, `docs/PROJECT_HANDOFF.md`, `docs/ARCHITECTURE.md`,
  `docs/ROADMAP.md`, `docs/CHANGELOG.md`.
- Enforces mandatory 11-step onboarding reading order in `docs/PROJECT_HANDOFF.md`. Zero Kotlin source code, UI, or build logic was changed.


- Learning Objectives + Learning Strategies + Flow Templates Foundation separates Product Brain
  from Flow execution into platform-neutral layers. Objective policy selects `DURABLE_RECALL`; strategy
  planner derives strategy behavior (`includeOptionalTyping`) without rotation dependency;
  `LearningFlowTemplateFactory` translates strategy into semantic template slots (`ROTATED_PRIMARY`,
  `OPTIONAL_TYPING`, `ANSWER_REVEAL`, `RATING_READY`) without `LearningExperiencePlan` dependency;
  `ProductBrainPlanner` acts as an orchestration-only boundary; `LearningFlowInstantiationService` resolves
  runtime selections; and `LearningFlowPlanner` converts template + selections + rotation context into
  concrete `LearningFlowDefinition`.
- Architecture Gate: Reusable templates contain no `ExperienceSelectionResult` and are independent of
  item/session/rotation context. `DesktopLearningFlowCoordinator` depends strictly on `ProductBrainPlanner`
  and `LearningFlowController`. `LearningFlowPlanner` accepts no product policies or selection engines.
- Scheduler, reveal, rating, persistence, import/package, and playback semantics are unchanged.



- Learning Flow Engine Foundation + Desktop Multi-stage Vertical Slice adds immutable shared
  definition/stage/state/progress, deterministic planner, and pure controller.
- Production v1 flow is rotated primary → eligible Typing → authoritative reveal → manual
  rating-ready; without Typing it is primary → reveal → rating-ready.
- Desktop `StudyViewModel` owns transient flow state keyed by session/item. Recomposition,
  focus/resize/audio replay and same-runtime pause do not alter it; item/session change resets it.
- App restart does not persist exact stage. Unrevealed current items reconstruct stage one;
  already-revealed items reconstruct safe rating-ready. Undo rebuilds for the restored item.
- The Default/Typing chooser is removed from active flow UI; Typing eligibility/evaluation stays
  shared and unchanged. No scheduler, FSRS, queue, review, schema, import, package, or playback
  authority moved into flow.
- Full local gate: `gradlew.bat clean test :desktop:compileKotlin --no-daemon`, BUILD SUCCESSFUL;
  1,626 tests, zero failures/errors/skips. Temurin 21.0.11
  `:desktop:createDistributable` passed.

- Session-aware Experience Rotation Foundation activates Default-mode round robin over passive
  Image/Listening/Prompt options. `ExperienceRotationContext` binds session ID, learning-item ID,
  and zero-based `LearningSessionProgress.currentPosition`; first item is ordinal zero.
- Reveal, retry, recomposition, pause/resume, and Typing interaction retain the context. Undo
  reconstructs from the rewound queue position; active-session restart reconstructs from
  existing persisted session/queue state. No rotation field or schema was added.
- Full policy eligibility still includes Typing. Automatic projection excludes it; explicit
  Typing remains `USER_CHOICE`, and returning to Default restores the rotated result.
- Scheduler, FSRS, queue semantics, review/rating, persistence, import, JSON, PKG, OPD3, media
  resolution, and playback remain unchanged.
- Full local gate: `gradlew.bat clean test :desktop:compileKotlin --no-daemon`, BUILD SUCCESSFUL;
  1,619 tests, zero failures/errors/skips. Temurin 21 `:desktop:createDistributable` passed.

- Typing Recall Vertical Slice Foundation adds shared `TYPING_RECALL`, semantic expected-answer
  extraction, and conservative locale-stable exact evaluation. Policy orders it after
  Image/Listening/Prompt, preserving ordinal-zero behavior.
- Desktop offers an explicit per-item Default/Typing chooser, routes both paths through
  `ExperienceSelectionEngine`, and owns transient identity-keyed input, focus, submit, localized
  feedback, and reset behavior.
- Desktop owns media resolution and fallback, `DesktopLearningSceneProjector`, Path-backed
  scenes, localized rendering, playback, keyboard/focus, accessibility, and layout. Typing
  submission invokes existing reveal; rating remains manual.
- Scheduler, queue, review, evidence, persistence, import, JSON, and PKG code were not changed.
- Full local gate: `gradlew.bat clean test :desktop:compileKotlin --no-daemon`, BUILD SUCCESSFUL;
  1,609 tests, zero failures/errors/skips. Temurin 21 `:desktop:createDistributable` also passed.

- Desktop Learning Experience Alpha is implemented over the existing Phase 6 contracts. ACTIVE
  Learn now uses a focused shell and content-first workspace; semantic MP3 playback uses commit
  `888f9bf` with observable state, cancellation, role labels, and `R` replay.
- Automated audio evidence ends at decoded PCM writes to the real Desktop output boundary.
  Product Owner physical-speaker and visual/responsive evidence remains pending in
  [`DESKTOP_LEARNING_EXPERIENCE_ALPHA_UAT.md`](DESKTOP_LEARNING_EXPERIENCE_ALPHA_UAT.md).
- No scheduler, queue, rating, evidence, progress, undo, persistence, recovery, or package
  authority moved into Desktop.

- Platform-Independent Learning Product Specification is the preceding documentation capability.
  Six specifications under `docs/spec/` define learner journey, workspace, behavior,
  interactions, media, and topic hierarchy for every future client.
- The outcome-based product roadmap is `LX-01` through `LX-11`. It places Session Entry/Setup
  and Focused Workspace before semantic/timed media, multi-lesson, Listening, Typed Recall,
  goals/curation, and ethical auto flow.
- Mandatory Product Owner gates remain role vocabulary, multi-lesson order/limits,
  autoplay/reveal, typed-answer evaluation, and new durable learner-data lifecycle.
- Next implementation after external Desktop 1.0 gates remains LX-01 Hierarchical Learning
  Scope. No Kotlin/Compose behavior changed in this capability.

- Android Product Reverse Engineering & Desktop Product Architecture completed at baseline
  `37c10b8`; it added the behavior catalog, gap analysis, product architecture, initial
  post-1.0 roadmap, vision, and technical-debt register.
- Android is a product/UX reference only. Learning Engine remains authoritative for scheduler,
  FSRS, queue, session lifecycle, rating, persistence, recovery, undo, and correctness.
- The 19,222-line Activity, both complete XML layouts, and the full 195.93-second video timeline
  were inventoried. Missing collaborator source means their internals remain unknown.
- Recommended first post-1.0 capability: LX-01 hierarchical learning scope. Multi-lesson,
  listening, typing, favorites, and goals retain explicit Product Owner gates.

- Real-data Desktop performance remediation is complete in baseline commit `075b098`.
  Production-scale
  synthetic evidence measured import 1,940 ms, library query 95 ms, and study preparation
  315 ms for 2,425 contents/12,125 learning items in the final clean run.
- Immediate UI feedback is architecture-tested; real visual first-row/resize smoothness and the
  original 179 MB media package still require Product Owner manual verification.
- Final local gate: `gradlew.bat clean test --no-daemon` passed 1,559 tests with zero
  failures/errors/skips; Desktop compile, app-image creation, and native Windows launcher smoke
  also passed with Temurin 21.0.11.

- The Desktop release blocker for real JSON + OPD3 PKG pairs is resolved in commit `e119e05`:
  signature routing, sibling pairing, binary validation, persisted media wiring, installed
  library visibility, and session-start evidence are covered.
- The 179 MB Product Owner artifact is intentionally not tracked or claimed as locally tested;
  manual re-test with that source remains required.

- Desktop 1.0 release-candidate preparation: complete after the final repository audit.
- Windows native launcher remediation: complete; `jdk.accessibility` is included and the
  generated executable passes the isolated bundled-runtime startup probe.
- No further autonomous product capability is authorized before Desktop 1.0. Continue only with
  Product Owner/manual or external release evidence.
- Pause remains resume of `ACTIVE`; one-step undo is Application-owned, persisted, atomic, and
  able to reopen final-review completion after restart.

## Desktop 1.0 Continuation

- Complete: P6-01 through P6-09 implementation and automated verification.
- Remaining Learning Experience evidence: Product Owner execution of the P6-09 manual matrix.
- Then: Phase 7 release candidate, defect fixing, external/manual evidence, and Desktop 1.0.
- Stable for Desktop 1.0 absent a concrete defect: session lifecycle, workspace actions,
  Learning Content, rich renderer, and progress/completion projection.
- Phase 5 clean-machine install/upgrade/uninstall/reinstall and signing evidence remains open.

## Decision Boundaries and Risks

- Preserve/adapt Android's rapid learning rhythm, lesson choice, bilingual media, and input
  flexibility; redesign automation/gestures; retire Android SRS, file mutation,
  lock-screen/device-admin, and Activity-owned business lifecycle.
- New study presets initially configure Desktop presentation/media over the same authoritative
  session. They must not create a Desktop scheduler or second queue.
- Media orchestration must be injected and deterministic; playback callbacks never rate or
  mutate durable progress.
- Arbitrary previous-card mutation must not bypass the established one-step undo contract.

- Session schema-v1 checkpoint fields are optional/defaulted; preserve legacy JSON readability.
- Review stages one durable intent, then remains atomic across event, memory, session checkpoint,
  and queue. Recovery reuses the original event ID.
- Desktop workspace state is projection only. Keep action permission in `ReviewWorkspaceState`,
  and do not move session, scheduler, or persistence authority into Desktop.
- `Content` remains canonical; `LearningContent` is the renderer-neutral Application projection.
  Desktop presentation resolves only local media through `ContentMediaStorage`; its Markdown
  allowlist never interprets HTML or remote/executable content.
- Java Sound is the current dependency-free audio adapter. Unsupported codecs fail safely and
  remain visible as unavailable; broader codec support needs an evidence-backed product choice.
- Queue totals are stable and known for the composed runtime. Progress distinguishes processed,
  reviewed, and skipped entries; the legacy no-queue path explicitly reports an unknown total.
- Completion is queue/session-owned. Scheduler feedback is ephemeral Desktop formatting of the
  committed Application result and never performs a second scheduler calculation.
- Avoid encoding flashcard-specific screen states into general domain concepts, but do not add
  abstractions without a current use case.
- Phase 5 external verification debt must remain visible and must not be reported as complete.
- Pause is continuation of `ACTIVE`, not a new status. Undo is one latest committed rating,
  atomic, never multi-level, and must not drift event, memory, queue, session, progress, or
  completion state.
- Safe HTML and remote media remain excluded. Markdown is allowlisted; media is local-only;
  Java Sound with safe fallback is accepted while guaranteed MP3 support remains deferred.
- Compose-only window, focus, scroll, and animation state is not durable. Desktop never
  recalculates scheduler outcomes.

## P6-07 Decisions

- Existing `ReviewEvent.stateBefore` is the authoritative scheduler before-state.
- An optional session checkpoint records whether memory existed plus the session/queue identity
  required for exactly one reversal; old records decode with no undo available.
- The completed queue remains persisted while the final review is undoable, allowing a
  `FINISHED` session to reopen after undo, including after restart.
- Application owns validation and the atomic transaction; Desktop only requests and projects.

## Latest Verified Test Evidence

- Foundation Freeze Audit gate (commit `4eec095`):
  `gradlew.bat clean test -x :desktop:test --no-daemon` BUILD SUCCESSFUL (59s);
  1,587 tests, 0 failures, 0 skipped, 0 compiler warnings.

- LP-005 baseline: `gradlew.bat clean test -x :desktop:test` BUILD SUCCESSFUL; 1,587 tests (includes LP-005's 83 new tests), 0 failures.

- JSON + OPD3 PKG remediation: `gradlew.bat clean test --no-daemon` passed 1,551 tests
  with 0 failures/errors/skipped. Desktop compile, app-image creation, and native Windows
  launcher verification passed using Temurin 21.0.11. The original 179 MB Product Owner
  package remains a manual re-test input and is not tracked.

- The native launcher failure was reproduced as missing
  `com.sun.java.accessibility.AccessBridge` under an accessibility-enabled user profile. The
  rebuilt Temurin 21 runtime includes `jdk.accessibility`; `:desktop:verifyWindowsLauncher`
  launches the generated executable with isolated profile/storage and exits successfully.
  `gradlew.bat clean test --no-daemon` passed 1,546 tests with 0 failures/errors/skipped;
  Desktop compile, app-image, MSI, and EXE packaging tasks passed. The artifacts are unsigned
  and were not installed.

- The final release audit rejects negative or payload-mismatched recovery manifest counts before
  safety-backup creation or mutation. `gradlew.bat clean test --no-daemon` passed 1,545 tests
  with 0 failures/errors/skipped; `:desktop:compileKotlin` and the non-interactive
  `:desktop:packageUberJarForCurrentOS` task also passed. `:desktop:createDistributable` passed
  with the available full Temurin JDK 21; native installer/signing evidence remains external.

- P6-09 adds a persisted OPD3-to-Desktop integration path covering package registration,
  global queue creation, reveal/rating, restart, progress, completion, final undo, re-rating,
  completion recovery, and duplicate-review prevention. `gradlew.bat clean test --no-daemon`
  passed 1,544 tests with 0 failures/errors/skipped; `:desktop:compileKotlin` and the
  non-interactive `:desktop:packageUberJarForCurrentOS` smoke task also passed.

- P6-08 full local gate: `gradlew.bat clean test --no-daemon`, BUILD SUCCESSFUL; 1,543 tests,
  0 failures/errors/skipped. Focused keyboard, focus, localization, safe-error, renderer, and
  Desktop completion-undo integration tests also passed.

- P6-06 baseline HEAD: `3e675420fca0fc304d8459132f6755329c48ddfb`.
- Full local gate: `gradlew.bat clean test --no-daemon`, BUILD SUCCESSFUL; 372 suites / 1,535
  tests, 0 failures/errors/skipped. Focused progress, transaction, restart, completion, and
  accessibility tests also passed.
