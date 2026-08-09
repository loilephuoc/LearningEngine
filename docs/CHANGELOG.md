# ANDROID-STUDY-3.0G — Explicit Study Mode Launcher

- Home no longer starts the default Adaptive session from Start Learning or Review Due. It opens
  Study, where Learn new, Adaptive study, and Typing practice are separate explicit actions.
- Learn new receives only remaining NEW quota and unseen eligible content. Adaptive and Typing
  receive only remaining REVIEW quota; Adaptive requires due review and Typing requires introduced
  learned content. Disabled launcher actions remain visible with their unavailable reason.
- Existing exact-session resume, package selection, daily accounting, Introduction, Recall,
  scheduling, persistence, and Practice entry contracts remain authoritative.

# ANDROID-STUDY-3.0D10F — Cold-start Navigation Intent

- Cold bootstrap now projects canonical Home even when a compatible ACTIVE session exists; Home
  exposes exact Continue without automatically opening Study or mutating the session.
- Root Study navigation is driven only by explicit Start/Resume/OpenSession intent. Home, Study hub,
  Review/Typing, Library scoped start and package actions retain their explicit entry paths.
- Completion/Failed inside Study, Back/Home, D10C idle refresh and all planning/persistence behavior
  remain unchanged.

# ANDROID-STUDY-3.0D10E — Start Planning Performance and Adaptive Fallback

- Queue diversity retains the same first-compatible-candidate ordering while removing repeated
  array-front shifting; seeded NEW ordering computes each SHA-256/hex key once before sorting.
- Low-evidence rich content now prefers supported image, audio, or example scaffolds before Typing.
  Text-only fallback and explicit `StudyMode.TYPING` remain Typing.
- A deterministic 990-item regression verifies complete identity preservation without truncation.
  Physical timing improvement remains pending device UAT.

# ANDROID-STUDY-3.0D10D — Study Start Recovery

- Generic Start now reconciles persisted active-session state before creation. A compatible ACTIVE
  session for the canonical package is loaded by exact ID instead of attempting duplicate creation.
- Unexpected Study event exceptions now publish `AndroidStudyState.Failed` and emit startup-trace
  evidence instead of leaving an apparently unchanged Home projection.
- Daily budget, idle-only refresh, live runtime, Introduction, scheduler and history semantics are
  unchanged.

# ANDROID-STUDY-3.0D10C — Package Selection Recovery

- Library package cards and package detail now distinguish installed usability (`Available`) from
  canonical Study selection (`Current learning package`) and expose `Use for Study` recovery.
- Selection delegates to `LibraryCommandService.setActivePackage`, reloads canonical projections,
  and creates no session. Existing Study Package and exact Continue Learning paths remain intact.
- Entering Home, Study, or Review refreshes the long-lived Study Home projection only while it is
  idle; Introduction, Typing, Recall and other live runtime states are preserved unchanged.
- Selecting a package preserves learner-global daily history: after 20 NEW, raising the target to
  50 still leaves exactly 30 NEW and does not reset review history or re-import content.

# ANDROID-STUDY-3.0D10B — Canonical Daily Study Budget

- Added learner-global, local-calendar-day NEW/REVIEW accounting from canonical review events.
  First review per content counts NEW exactly once; later reviews count REVIEW; reveal is absent from
  history and Undo removes its event, so retries and package switching cannot reset or double-count.
- Durable Android Study preferences default to NEW 20 / REVIEW 100 per day. Session creation passes
  only remaining daily quotas into per-session `SessionPolicy`; raising 20 to 50 after 20 completions
  immediately admits at most 30 additional NEW.
- Home, Study landing, HUD and completion expose daily progress/reasons. Adaptive was audited and
  remains unchanged because existing deterministic history penalties already prevent mode monotony.

# ANDROID-STUDY-3.0D10A — Canonical Active Package and Session Continuation

- Home and Study landing actions now consume the canonical action projected from
  `Library.activePackageId`; installed/ACTIVE packages and memory statistics no longer independently
  expose Start Study or a current-package title when no package is selected.
- Package detail and Library package/lesson/selection Study actions set and confirm the canonical
  package before reconciling a mismatched scoped session and creating the new Adaptive session.
- Exact compatible session resume remains non-duplicating, session completion preserves package
  selection, and first/subsequent import selection policy remains unchanged.

# ANDROID-STUDY-3.0D9 — Legacy-density Study Canvas Refinement

- Introduction now owns the useful viewport below the HUD. Its internal lazy stage centers the
  coupled clue/hero front and bottom-aligns revealed content next to the fixed rating dock, while
  overflowing content remains scrollable on short viewports or with larger text.
- Hero media uses 90% of useful stage width while retaining D8 intrinsic aspect-aware Fit bounds.
  Front clue audio uses tint without a pill or geometry change; the revealed English answer alone
  keeps the stronger semantic halo.
- Preserved D7/D8 reveal/rating gates, session authority, audio ownership, fullscreen interaction,
  explicit Typing, REVIEW modes, scheduler, and persistence behavior.

# ANDROID-STUDY-3.0D8 — Reveal-first NEW Interaction and Aspect-aware Hero

- Unrevealed Introduction now composes no rating dock and disables swipe Good; reveal remains a
  learning action that records no review and does not advance the queue. Four-way rating and swipe
  Good become available only in the revealed phase.
- Removed fixed-square/min-height/SpaceEvenly Introduction geometry. The existing decoded bitmap's
  intrinsic aspect ratio now determines centered Fit bounds: landscape uses full useful width with
  natural height, while square/portrait media use viewport-bounded height without crop or distortion.
- Preserved D7 NEW/restart/Undo authority and ViewModel serialization, all audio/image interactions,
  explicit Typing, five REVIEW modes, scheduler, and persistence behavior.

# ANDROID-STUDY-3.0D7 — Adaptive NEW Resume and Typing Completion Recovery

- Unrated NEW queue items now remain in Introduction based on NEW origin plus absence from canonical
  reviewed content, even when reveal persistence has recorded them as introduced. Restart and Undo
  restore the same revealed Introduction until rating advances the item exactly once.
- Android Study ViewModel serializes initial loads and Study events through one mutex. A correct
  AnswerChanged auto-submit followed immediately by IME Submit now publishes the committed completed
  state; duplicate Submit remains idempotent.
- AdaptiveRecallStrategy, explicit Typing intent, D1–D6 presentation, scheduler, persistence, and
  package/session lifecycle authorities are unchanged.

# ANDROID-STUDY-3.0D6 — Immersive Image-led Study Canvas

- Quieted Study chrome to one-line package identity, mode/item progress, and a slim progress bar;
  retained the canonical projection-only HUD as a compact secondary strip.
- Reordered NEW reveal into image, English answer, POS/pronunciation, Vietnamese meaning, and one
  coherent bilingual example block. Front/reveal remain states of the same canvas; reveal preserves
  roughly 86% of the responsive hero footprint and short viewports scroll instead of collapsing it.
- Preserved four-way rating, swipe Good, single-controller audio ownership, image tap/fullscreen
  semantics, reduced motion, explicit Typing, and all five canonical REVIEW renderers.

# ANDROID-STUDY-3.0D5 — Active Package Scope and Study Package Navigation Fix

- First-package import now assigns canonical `Library.activePackageId` through the existing Library
  command authority; later imports preserve an existing active selection.
- Android Study resolves only the canonical active Library package and no longer chooses an
  arbitrary ACTIVE installed package. Package Study selects that package, starts its scoped session,
  and navigates with the resulting ID; Continue Learning opens its exact existing ID without a new
  session.
- Preserved Adaptive/Typing intent, Introduction gating, completion/history, scheduler, and D4
  import/session reconciliation behavior.

# ANDROID-STUDY-3.0D4 — Canonical Import → Library → Study Lifecycle Reconciliation

- Extracted the InstalledPackage/Library-entry completion previously embedded in Desktop import
  presentation into one shared Application use case used by Android and Desktop. Android reports
  import success only after this canonical lifecycle step, so Library queries immediately see it.
- Added shared active-session scope reconciliation across installed-package existence/state, queue,
  and package content ownership. Compatible sessions and identical imports remain intact; stale
  package-bound queues are canonically left and cannot be resumed by Home, generic load, or exact
  process restoration.
- Preserved Adaptive default, explicit session-scoped Typing, Introduction gating, D3 completion,
  learning history, and all Study presentation behavior.

# ANDROID-STUDY-3.0D3 — Complete Adaptive Recall Context and Session Lifecycle

- Moved Study invocation mode and bounded actual recall-mode history into persisted canonical
  `StudySession` state. Recall commits append mode/direction/outcome history transactionally, and
  Android Adaptive planning now supplies that history alongside engine-derived trajectory context.
- Android finishes an ACTIVE session through `LearningEngine.finishSession` only when its canonical
  queue is exhausted. Finished sessions project Completion without being rediscovered as active or
  replaying the finish transition.
- Explicit Typing remains attached to its originating session; every ordinary new Study invocation
  defaults independently to Adaptive, eliminating SavedState-global mode leakage.

# ANDROID-STUDY-3.0D2 — Canonical Study Mode Routing + Image-First New Learning

- Added shared Adaptive/explicit-Typing study intent. Adaptive delegates to production recall
  strategy; explicit Typing produces a Typing-only production plan.
- Android exposes deliberate Typing practice while unseen NEW still enters the existing image-first
  Introduction path before recall planning. Canonical queue completion, rating, HUD, and 3.0D1 image
  behavior remain unchanged.

# ANDROID-UAT-001 — Android Runtime JVM File API Compatibility

# ANDROID-UI-3.0D1 — Legacy-Proven Study Canvas UAT Correction

- Replaced NEW's fixed 220dp image and 32sp Vietnamese vocabulary style with a pure responsive
  240–340dp front/60%-reveal image policy and length-adaptive 22/25/28sp clue presentation.
- Removed persistent reveal instruction, centered the clue/image canvas with bounded vertical
  rhythm, and animated answer/image/example transformation within the same learning stage.
- Replaced full-width tinted Introduction audio rows with wrap-content semantic targets: active
  English Word/Example loops receive a restrained 1.00–1.045 breathing halo, while reduced motion
  uses static emphasis and Vietnamese one-shots receive tint only.
- Reworked the canonical HUD into a transparent two-row inline status strip without adding state,
  counters or queries; all 3.0D rating, gesture, image, fullscreen and playback ownership remains.

# ANDROID-UI-3.0D — Focus-first New Content Learning Loop

- Exposed the existing four-way `RateIntroduction` path before and after reveal, preserving the
  facade's introduction completion, standard review transaction, duplicate guard and queue advance.
- Added a pure scroll/axis/threshold/child-consumption gesture contract that maps one eligible
  upward swipe to canonical Good while leaving visible ratings available for long content.
- Added reveal-transition English word autoplay, presentation-local Word/Example loop cycling,
  direct Vietnamese one-shot ownership, and compact/expanded revealed-image state using the one
  existing audio controller and image decode.
- Added `dueCount` to the immutable Android HUD projection directly from
  `StudyHeaderStatistics`; Compose owns no statistics or optimistic counters.
- Preserved all five RecallPlan REVIEW modes and added focused canonical-rating, duplicate,
  gesture, playback, image, HUD and composition regression coverage.

# ANDROID-UI-3.0C — Immersive Study Runtime Recomposition

- Replaced the split REVIEW main/reveal card composition with one `LearningEngineLearningStage` so
  prompt, response, feedback and label-free answer content transform in one mobile surface.
- Made the context header subordinate, added canonical rating distribution to the compact HUD, and
  gave NEW/Image Recall hero media 220dp prominence with 110dp supporting reveal treatment.
- Preserved discovery-first NEW, four canonical audio roles, fullscreen session continuity,
  four-way `RateIntroduction`, all five RecallPlan modes and TextFieldValue IME ownership.
- Added wrapping 56dp choice tiles, polite result live-region semantics and focused composition/HUD/
  media-role/accessibility regressions; Android qualification passes 184 tests.

# ANDROID-UI-3.0B — Home and Study Landing Product Composition

- Replaced Home's generic stacked-card layout with a compact continuation hero, three real dashboard
  stat tiles, conditional due-review action and inline memory progress; removed duplicate giant
  Library/Review navigation actions.
- Replaced Study landing's technical availability card with active/no-session hero states, canonical
  context title, memory progress, compact stats and conditional Review-tab entry.
- Added reusable hero, stat and action components on the 3.0A theme and a pure shared landing
  projection with no repository, statistics or scheduler duplication.
- Added focused active/no-session/due/progress/navigation/fake-data/responsive semantics contracts.

# ANDROID-UI-3.0A — Mockup 03/09 Design System Foundation

- Replaced the mixed blue/purple generic Material treatment with intentional warm green/teal Light
  and premium layered navy/teal Dark semantic schemes.
- Centralized compact typography, spacing, shape, elevation and icon scales plus shared primary,
  compact, settings-row and screen-shell components.
- Rebuilt five-destination bottom navigation with a compact custom tonal selected state and explicit
  accessibility selection semantics, preserving routes and navigation behavior.
- Migrated Home, Library root, Study/Review landing, Settings and Study runtime foundation; removed
  legacy headings, dominant giant card/button stacks, duplicate Study progress, oversized HUD and
  the overlapping Introduction reveal transition.
- Added focused palette, token, component, shell, navigation, responsive-title and color-authority
  contracts.

# ANDROID-STUDY-2.0C — Immersive Mobile Study Composition

- Replaced stacked technical Study cards with a mobile stage, compact HUD/progress, lightweight
  token-driven surfaces, and a persistent thumb-reachable Introduction rating dock.
- Made Vietnamese meaning and hero image lead NEW discovery; reveal now animates in-place to the
  English word, pronunciation, meaning, examples, smaller image, and canonical four ratings.
- Unified all five REVIEW modes with the same label-free answer language, mobile MCQ tiles, existing
  IME behavior, canonical audio roles, fullscreen image flow, and reduced-motion handling.
- Added focused composition, theme, semantics, touch-target, HUD-authority, and mode regressions.

# ANDROID-STUDY-2.0B — Canonical Realtime Session HUD

- Added immutable Android HUD projection over Shared `StudyHeaderStatistics`, including canonical
  NEW/REVIEW completion and effective/configured targets, total learned, and latest rating counts.
- Refreshed it at load/resume, Introduction and REVIEW commits, queue advancement, and Undo without
  Compose-side counters or recomposition queries.
- Added a compact Material 3 HUD and focused persisted-state tests for target semantics, total,
  Introduction Good/Again, REVIEW updates, Undo, resume, and rating mapping.

## ANDROID-UAT-004 — Device Runtime Path and Main-Thread Audit

## ANDROID-UI-003 - Redesign Android Library Experience

- Replaced Library Home full-content counting/search with a bounded canonical navigation-tree
  projection for installed packages, collections and active-package identity.
- Added cancellable 250 ms Unicode package/collection search, duplicate-query and generation
  guards, typed All/Collections/Packages filters, collection opening and stable package identity.
- Added Material 3 package/collection cards, semantic state labels, lazy stable keys, accessible
  search clearing, import action and actionable empty/failure/import states.
- Kept package browser, editor, scoped Study, export/verify/uninstall and import authorities intact;
  successful import refreshes Library exactly once.

## ANDROID-UI-002 - Redesign Android Home Experience

- Replaced the management-style Home stack with one mobile-native hero action plus conditional
  due Review, today summary, learning progress, quick navigation, and actionable empty content.
- Projected exact active-session ID, Dashboard due/activity/memory values, installed packages and
  existing Learn-entry availability into one immutable Android Home model.
- Enforced primary action priority: resume, due Review, normal Study, then Library/import; Compose
  remains state-hoisted and does not query repositories or construct plans/sessions.
- Added accessibility, semantic progress/status, large-font wrapping, typed retry behavior and
  focused authority/state/navigation/theme regressions.

## ANDROID-UI-001 - Establish Material 3 Design System

- Added explicit calm green/teal light and charcoal/blue-gray plus teal dark Material 3 schemes,
  semantic learning/status colors, typography, spacing, shape, elevation, and motion contracts.
- Added accessible foundation scaffold, app bar, buttons, cards, headers, loading/empty/error,
  progress, status, and feedback components plus light/dark design-catalog previews.
- Added persisted typed Follow system/Light/Dark selection owned by the Android Application;
  appearance changes recompose without resetting graph, navigation, or session identity.
- Migrated the root shell and appearance selector to the foundation without changing destinations,
  application services, learning semantics, or Desktop behavior.
- Verified 635 XML suites / 3,456 tests and debug/release APK plus release AAB assembly.

### ANDROID-UAT-006 - Stabilize Startup Shell and Root Navigation State

- Added a synchronized Application graph owner and an explicit Bootstrapping/Ready/typed Failure
  root state; graph exceptions now render a safe retryable error instead of infinite loading.
- Kept Navigation Compose as the root destination authority, added typed route resolution and
  deterministic invalid-route fallback, and removed blank Home/Review feature branches.
- Isolated Library and Study loading/failure presentation inside the root shell and guarded both
  ViewModels against stale asynchronous result publication.
- Added bounded START/END/failure startup diagnostics plus root-state and destination transitions.
- Allowed RC qualification to validate an explicitly supplied local baseline HEAD while preserving
  the default `HEAD == origin/develop` guard.

### ANDROID-UAT-005 - Eliminate Active-Session N+1 Persistence Loading

- Added canonical `ContentRepository.findByIds` semantics: first-occurrence input order, missing-ID
  omission, duplicate suppression, and one store load in the store-backed implementation.
- Routed Android and Desktop Recall plan scope construction through the bulk operation, preserving
  the complete session scope and fallback only for a truly empty canonical result.
- Added a 990-content call-count/runtime regression and fixed the Windows diagnostic script's
  reserved `$PID` collision with null-safe `$appProcessId` handling.
- Device install-over-data verification completed the existing active-session initial load in
  3,969 ms and 4,059 ms with no sustained GC; no data was cleared, uninstalled, or re-imported.

### ANDROID-UAT-004 details

- Resolves legacy `media/Package/path` and canonical `Package/path` deterministically against one
  `<data>/media` root without scanning or duplication.
- Renders a startup shell before exactly-once persisted graph initialization, moves Study engine
  work to an injected serialized worker, consumes scoped handoff once, and adds safe device runtime
  diagnostics under ignored `build/` evidence.

## ANDROID-UAT-003 — Device Media and Study Handoff

- Aligns Android media resolution with the persisted importer at `data/media`, preserving existing
  imported files without copying or fallback roots.
- Replaces generic Resume for scoped Study with exact typed session loading, post-publication
  navigation and recoverable Retry/Back failure UI; adds canonical package Export, Verify and
  confirmed Uninstall actions.

## ANDROID-010 — Native Android Learning Experience

- Replaces the technical Home launcher with conditional canonical learning actions and moves data
  tools to Settings.
- Adds five root destinations, mobile package hero/cards, human-readable titles, canonical content
  counts, semantic media indicators, bounded thumbnails, detail audio replay, and guarded scoped
  Study launch without adding Android business authority.

- Replaced the shared JSON reader's Java 11 `Files.readString` call with a charset-aware,
  stream-closing portable reader used identically by Android and Desktop; schemas and error behavior
  are unchanged. The existing durable writer required no change.
- Added a production-source forbidden API guard plus Unicode/large-input/charset/closure tests and
  an Android persisted content/package restart test through the production graph.
- Device verification installed the debug APK with `adb install -r` over existing data, launched
  MainActivity, retained a live process, and found no fatal exception, `NoSuchMethodError`, or
  `Files.readString` in bounded logcat. No app data was cleared.
- Verification: Root 392 / 2,054, Desktop 230 / 1,326, Android 9 / 46; total 631 suites / 3,426
  tests with zero failures, errors, or skipped tests; Android RC qualification PASS.

# ANDROID-009 — Production Polish (Pre-UAT)

- Enabled edge-to-edge presentation with safe/IME insets and subtle destination/runtime fade
  transitions that do not alter navigation or Study state authority.
- Added adaptive portrait/landscape rhythm, bounded elevated prompt/detail cards, stable lazy-list
  states, Material loading/empty/error presentation, selection fade, IME Done, image crossfade and
  explicit audio preparation/playback states.
- Retained Material tokens/ripples, 48dp minimum targets, bounded off-main image decode, stable list
  keys, focus-once behavior and all existing event/navigation semantics.
- Verification: Root 391 / 2,051, Desktop 230 / 1,326, Android 7 / 44; total 628 suites / 3,421
  tests with zero failures, errors, or skipped tests; Android RC qualification PASS.

# ANDROID-008 — Automated System Acceptance & Defect Remediation

- Added a reusable persisted Android acceptance fixture and deterministic lifecycle/storage suites
  for SavedState restoration, stale navigation, cancelled search, provider/write/checksum failures,
  cleanup, callback idempotency and durable-state preservation.
- Fixed recreation dropping selected content and global search query, and changed stale saved package
  restoration to clear invalid identities and return to the safe Library root.
- Injected the Library ViewModel worker dispatcher with the existing I/O default for deterministic
  cancellation/lifecycle verification. Shared learning, queue, Practice, Recall, Scheduler/FSRS,
  Evidence, package and backup semantics are unchanged.
- Verification: focused 2 suites / 9 tests; Root 391 / 2,051, Desktop 230 / 1,326, Android 7 / 43;
  total 628 suites / 3,420 tests with zero failures, errors, or skipped tests; Android RC PASS.

# ANDROID-007 — Android Release Candidate Qualification

- Added an explicit non-debuggable Android release build with R8 optimization/minification and
  resource shrinking, using only narrow entry-point keep rules and library consumer rules.
- Added a fail-fast qualification entry point covering Git baseline, clean tests, debug/release APK,
  release AAB, manifest/permission audit, artifact integrity, hashes, ABI/native inventory and
  startup/service composition. Generated artifacts and evidence remain ignored.
- Release APK/AAB are intentionally unsigned; no signing secret or keystore was added. Physical
  launch, device UAT, production signing and Play validation remain pending.
- Verification: Root 391 suites / 2,051 tests, Desktop 230 / 1,326, Android 5 / 34; total
  626 suites / 3,411 tests with zero failures, errors, or skipped tests; `BUILD SUCCESSFUL`.

# ANDROID-006 — Canonical Library Operations & Scoped Study

- Composed canonical editor, export, OPD3 verifier, upgrade and uninstall authorities in
  `LearningApplicationContext`; Android package I/O remains a SAF stream/staging adapter.
- Added `ScopedStudySessionService` for package, lesson, selected-content and collection scopes.
  It validates canonical membership and delegates to unchanged production session/queue authority.
- Added canonical Lesson Browser summaries over existing package content projections and Android
  lesson/package/selected Study entry. Editor save now refreshes the selected authoritative detail.
- Added focused Application tests proving scope membership and absence of platform queue planning.
- Verification: Root 391 suites / 2,051 tests, Desktop 230 / 1,326, Android 4 / 31; total
  625 suites / 3,408 tests with zero failures, errors, or skipped tests; debug APK assembled.

# ANDROID-005B — Android-Native Library Experience

- Added debounced/cancellable global cross-package search, touch-first stable item selection and
  mobile item detail without persistence or media-byte access.
- Composed canonical `ContentBrowserEditService` and added a section editor whose validation errors
  preserve draft input and whose success reloads authoritative projections.
- Kept unavailable scoped Study, verify and upgrade flows explicit rather than adding Android
  business fallbacks. Existing Library commands, SAF import and offline persistence remain unchanged.
- Verification: Root 390 suites / 2,047 tests, Desktop 230 / 1,326, Android 4 / 31; total
  624 suites / 3,404 tests with zero failures, errors, or skipped tests; debug APK assembled.

# ANDROID-005 — Android Library, Browser & Package Workspace

- Added Android Library navigation, lifecycle ViewModel, authoritative collection/package root,
  and lazy Package Content Browser with stable identities.
- Reused Application search/media/sort policy and typed Library command boundary; Android stores
  only lightweight package/query state and performs queries off-main.
- Integrated Library entry with the existing Home/Study navigation without changing session or
  learning authority. Added focused criteria/identity tests.
- Verification: Root 390 suites / 2,047 tests, Desktop 230 / 1,326, Android 4 / 29; total
  624 suites / 3,402 tests with zero failures, errors, or skipped tests; debug APK assembled.

# ANDROID-004 — Responsive UI, Accessibility & Performance Hardening

- Added Compact/Medium/Expanded layout policy, bounded tablet content/media, safe drawing and IME
  insets, flexible multiline answers, and item-keyed focus/bring-into-view behavior.
- Added localized EN/VI semantics for MCQ position/selection, audio, images, progress, answers, and
  Example blanks without exposing canonical answers before reveal.
- Made document-operation Back cancel lifecycle work and disabled OS Auto Backup in favor of the
  explicit validated `.lebak` flow. No Shared/Application learning semantics changed.
- Added deterministic responsive and accessibility policy tests. Device font-scale, TalkBack,
  predictive-back, and startup performance remain manual/ANDROID-005 evidence.
- Verification: Root 390 suites / 2,047 tests, Desktop 230 / 1,326, Android 3 / 24; total
  623 suites / 3,397 tests with zero failures, errors, or skipped tests; debug APK assembled.

# ANDROID-003 — Android Content, Media, Import, Backup & Lifecycle Hardening

- Added SAF import, create-backup, and restore entry points with app-private staging cleanup, typed
  failures, and one-shot lifecycle identities.
- Reused Application package import and added a shared JVM `.lebak` recovery adapter with checksum
  validation, safety backup, and rollback preservation.
- Made audio preparation asynchronous and release deterministic; bounded image decoding now runs on
  the I/O dispatcher without exposing Android types to Shared.
- Added Android tests for identity, interruption, unavailable input, cleanup/stream closure,
  failed-restore preservation, and backup/restore round trip.
- Verification: Root 390 suites / 2,047 tests, Desktop 230 / 1,326, Android 2 / 15; total
  622 suites / 3,388 tests with zero failures, errors, or skipped tests; debug APK assembled.

# ANDROID-002 — Complete Android Study Experience

- Added engine-backed Home/session entry for Review, latest-session Practice, Again/Hard Practice,
  learned-item Review, Resume, Completion, Back, and Undo.
- Added Android MCQ, Listening, Image Recall, and Example Completion renderers beside unchanged
  Typing, with plan-owned identity/order/span, responsive layout, semantics, and media failures.
- Routed Practice adaptive reinforcement, dynamic membership, manual SRS override, and completion
  through existing Application authorities without changing Shared or Desktop behavior.

# ANDROID-001 — Android Foundation and Study Runtime Bootstrap

- Added an Android application module with Compose Material 3, Navigation, lifecycle-aware
  StateFlow ViewModel, saved session identity, and explicit persisted composition.
- Added the first Android Typing renderer over production Shared planning, execution, and learning
  bridge, including prefix feedback, exact auto-submit, reveal/comparison, retry, one-shot gating,
  continuation, and completion.
- Reused existing persistence, content-media, and package-import authorities without changing
  Desktop or Shared learning semantics.

# REL-001 — Desktop Release Candidate Qualification

- Added release-only launcher verification for the ProGuard `main-release` image, including
  bundled runtime/accessibility/resource integrity and isolated startup verification.
- Added release-runtime MP3 reader/converter discovery without device-dependent playback.
- Added a Windows qualification script for clean tests, release EXE/MSI packaging, XML counts,
  artifact hashing/signature inspection, portable manifesting, and explicit pending gates.

# LQ-007X — Desktop Recall Runtime Conformance

## BUG-003 — Practice Attempt and Queue Mutation Consistency

- Bound Typing eligibility to the issued plan/presented-item snapshot rather than mutable rating
  and confidence state recomputed at submission.
- Kept dynamic Practice progress valid as Good/Easy membership removal changes a current round.
- Added end-to-end Desktop tests spanning latest-session adaptive Practice and difficult Practice
  manual Good/Easy, Undo, Typing execution, queue transition, and next-item publication.

## UAT-DESK-002 — Adaptive Practice Reinforcement

- Routed latest-session Practice to adaptive session-local feedback reinforcement with graduated,
  bounded gaps and deterministic persisted recovery.
- Routed difficult Again/Hard Practice to dynamic SRS-backed membership; explicit manual Good/Easy
  removes future membership, Again/Hard retains it, and Undo restores membership atomically.
- Preserved fixed Practice, Coverage Review spacing, Practice isolation, and Desktop's adapter-only
  boundary.

## UAT-DESK-001 — Typing Direction Authority Regression

- Restored strategy-selected Typing to `TARGET_TO_SOURCE` and explicitly retained Reverse
  Translation as `SOURCE_TO_TARGET`.
- Added Desktop projection validation so target cue, English/source canonical answer, and plan
  direction cannot diverge silently.
- Added focused strategy, production-plan, projection, realtime Typing, reveal, and transition
  regression coverage for the reported UAT path.

- Added one Desktop interaction contract for prompt, focus, input, one-shot submission, result,
  transition, Practice, completion, keyboard, accessibility, and valid runtime-specific exceptions.
- Keyed MCQ, Listening, Image, and Example Completion initial focus by plan identity so equal-content
  next items receive focus once without recomposition loops.
- Disabled every Listening submission path when audio is unavailable and announced the state through
  a polite live region. Existing Typing, MCQ, Listening replay, Image, and Example UX remain intact.
- Verified focused 16 suites / 154 tests and full clean 613 suites / 3,337 tests with zero failures,
  errors, or skipped tests.

# LQ-007F — Desktop Example Completion Recall Runtime

- Routed production-resolved Example Completion plans to a contextual Desktop scene that splits
  only the Shared masked prompt at its typed target span; Desktop performs no target search,
  replacement, masking, canonical-answer lookup, or evaluator work.
- Added accessible, localized blank/input presentation with long-line wrapping and explicit
  unavailable state for invalid/out-of-range or answer-bearing spans.
- Added one-shot raw `TypedText` submission through the existing execution and learning bridge,
  preserving Practice isolation, evidence policy, duplicate safety, and queue/session authority.
- Verified focused 9 suites / 98 tests and full clean 612 suites / 3,329 tests with zero failures,
  errors, or skipped tests.

# LQ-007E — Desktop Image Recall Runtime

- Routed production-resolved Image Recall plans to an image-first Desktop scene without exposing
  canonical answer, translation, pronunciation, part of speech, or supporting scenes before input.
- Added localized loading, unavailable, and decode-failure states; only successfully decoded media
  enables raw text submission. Existing aspect-fit authority handles landscape, portrait, square,
  and large images without cropping or overlapping input/footer space.
- Added one-shot `TypedText` submission through the Shared execution engine and learning bridge.
  Desktop adds no mode selection, normalization, correctness, rating, learning, or queue authority.
- Verified focused 8 suites / 71 tests and full clean 611 suites / 3,318 tests with zero failures,
  errors, or skipped tests.

# LQ-007D — Desktop Listening Recall Runtime

- Added plan-mode routing and an audio-first Listening scene that preserves opaque resource
  identity and excludes canonical/supporting answer content before submission.
- Reused Desktop audio playback/error/replay authority with accessible controls and Ctrl+R;
  recomposition binds without autoplay and cannot create parallel looping playback.
- Added one-shot raw `TypedText` submission through `RecallExecutionEngine` and
  `RecallLearningExecutionBridge`; no Desktop normalization, correctness, rating, or transaction
  authority was introduced. Practice and evaluative behavior retain existing bridge semantics.
- Verified focused 6 suites / 46 tests and full clean 610 suites / 3,307 tests with zero failures,
  errors, or skipped tests.

# LQ-007C.1 — Production Recall Mode Activation

- Replaced Desktop's fabricated Typing decision with Shared Application production resolution
  through Content capability, adaptive strategy, deterministic MCQ generation, and RecallPlanFactory.
- Added ordered typed fallback with requested/resolved mode and factory-failure provenance; no
  eligible candidate returns no plan and performs no queue or learning mutation.
- Reused the same resolved plan for one Desktop attempt across recomposition. No mode selector,
  Desktop randomizer, Scheduler/FSRS/Evidence policy, transaction, or queue behavior was added.
- Focused verification passes 5 suites / 32 tests; full clean verification passes 609 suites /
  3,299 tests (Root 384 / 2,012; Desktop 225 / 1,287), with zero failures, errors, or skips.

# LQ-006G — Recall Result to Learning Execution Integration

- Added typed integration request/result/policy/classification contracts and a Shared Application
  bridge into existing session review, practice completion, and manual override use cases.
- Added evidence-quality gating, mode-aware rating intents, practice isolation, deterministic
  attempt-derived ReviewEvent identity, duplicate protection, and typed stale/invalid outcomes.
- Reused existing transaction, Evidence Promotion, Scheduler/FSRS, pending recovery, queue, and Undo
  authorities; completed typing and recall execution converge at the same review command boundary.
- Verified focused 4 suites / 38 tests and full clean 605 suites / 3,273 tests (root 382 / 1,999;
  Desktop 223 / 1,274), with zero failures/errors/skips.
- Preserved Desktop UI, live typing, recall contracts, algorithms, and all platform boundaries.
  Integrated Desktop UAT is not applicable.

# LQ-006F — Cross-Platform Recall Session Execution Engine

- Added typed execution request/result/policy/provenance/violation contracts, immutable evaluator
  registry, attempt snapshot, and final result validator.
- Added all-mode text/choice evaluation, existing typing normalization reuse, accepted alternatives,
  exact/normalized correctness, terminal outcomes, latency, assistance, and eligibility mapping.
- Added backward-compatible RecallResult provenance fields and stable result wire round-trip.
- Verified focused 4 suites / 73 tests and full clean 604 suites / 3,260 tests (root 381 / 1,986;
  Desktop 223 / 1,274), with zero failures/errors/skips.
- Preserved all UI, persistence, Scheduler/FSRS, Evidence, Practice-loop, queue, and learning-state
  boundaries. Integrated Desktop UAT is not applicable.

# LQ-006E — Deterministic Multiple Choice Distractor Generator

- Added typed scope/candidate/policy/result/rejection/fallback contracts and a bounded deterministic
  generator with stable option identity and seed-based ordering.
- Added ContentId/scope/lifecycle/normalization/ambiguity filtering, POS/topic/length ranking,
  explicit fallback tiers, reduced-count policy, and typed insufficient-inventory behavior.
- Integrated the generator through the LQ-006D provider without stored or synthetic distractors.
- Verified focused 2 suites / 37 tests and full clean 603 suites / 3,241 tests (root 380 / 1,967;
  Desktop 223 / 1,274), with zero failures/errors/skips.
- Preserved all UI, execution, persistence, Scheduler/FSRS, Evidence, Practice, and platform
  boundaries. Integrated Desktop UAT is not applicable.

# LQ-006D — Cross-Platform Recall Plan Factory

- Added typed request/result/policy/provider contracts and deterministic plan identity.
- Added mode-specific prompt construction, answer/assistance/requirement projection, decision
  revalidation, safe example masking, leakage checks, registry validation, and final LQ-006A plan
  validation.
- Multiple Choice requires an injected option provider and never invents distractors.
- Verified focused 3 suites / 50 tests and full clean 602 suites / 3,223 tests (root 379 / 1,949;
  Desktop 223 / 1,274), with zero failures/errors/skips.
- Preserved all platform, execution, persistence, Scheduler/FSRS, Evidence, Practice-loop, and UI
  boundaries. Integrated Desktop UAT is not applicable.

# LQ-006C — Adaptive Recall Strategy Selection

- Added typed request, policy, history, evidence context, strength, reason, decision, and result
  contracts with stable wire IDs.
- Added deterministic capability-filtered mode/direction ranking, safe fallback, diversity
  exemptions, unique fallback chain, and typed rejection reasons.
- Verified focused 2 suites / 33 tests and full clean 601 suites / 3,204 tests (root 378 / 1,930;
  Desktop 223 / 1,274), with zero failures/errors/skips.
- Preserved LQ-006A/B and all execution, persistence, Scheduler/FSRS, Evidence, Practice, and UI
  boundaries. Integrated Desktop UAT is not applicable.

# LQ-005E — Adaptive Learning Strategy & Recommendation Engine

- Added immutable typed recommendation actions, categories, priorities, engine confidence, and
  explanation reasons, preserving learner + Content identity from the difficulty profile.
- Added validated `AdaptiveLearningPolicy` and a pure deterministic `AdaptiveLearningStrategy`
  that reads only `LearningDifficultyProfile`.
- Covered required mappings for very difficult, mastered, recovering, unstable, and stable
  profiles, plus promotion readiness, confidence projection, critical regression, risk/lapse/fast
  promotion explanations, determinism, and input immutability.
- Added no execution, persistence, UI, Scheduler, FSRS, Queue, review, trajectory, evidence, or
  difficulty-profile changes.
- Verified focused 1 suite / 11 tests and full clean 591 suites / 3,094 tests with zero failures,
  errors, or skips. Integrated UAT remains pending.

# LQ-005D — Learning Difficulty Intelligence Profile

- Added a pure learner-and-Content `LearningDifficultyProfile` derived from the authoritative
  `LearningTrajectory`, with distinct lifetime, current stability, promotion analytics, risk,
  engine confidence, trend, and difficulty-level contracts.
- Added bounded typed scores and validated `LearningDifficultyPolicy` weights/thresholds; time is
  supplied only by `EvidenceClock`.
- Added deterministic coverage for lifetime retention, current scores, risk, generic promotion
  analytics, confidence growth, all trend categories, difficult Again histories, mastered stable
  histories, rating-independent instability, sibling projection identity, and fake-clock aging.
- No Desktop, Scheduler, FSRS, transaction, persistence, or Evidence Engine behavior changed.
- Verified focused 1 suite / 10 tests and full clean 590 suites / 3,083 tests with zero failures,
  errors, or skips. Integrated UAT remains pending.

# LQ-005B — Evidence Promotion Execution & Durable Learning Trajectory

- Integrated evidence construction, trajectory advancement, promotion evaluation, final-rating
  resolution, and `PromotionDecision` exposure ahead of Scheduler execution.
- Added learner-and-Content JSON trajectory persistence, durable pending recovery, and atomic undo.
- Preserved practice isolation and uncapped manual behavior; reveal and duplicates add no evidence.
- Verified focused 3 suites / 11 tests and full clean 589 suites / 3,073 tests, all green. Integrated
  Desktop UAT remains pending.

# UX-007 — Image Space Maximization and Single-Pass Question Transition

- Added content-derived `TypingFieldLayoutMetrics` separating protected inner line-box height from
  outer label/input/action/inset height; removed fixed 96–116dp outer-card authority.
- Allocated complete remaining body height to every fitted image aspect class, removing the legacy
  layout cap, landscape height fractions, and duplicate dock/inventory subtraction.
- Replaced immediate next-item publication plus child arrival reset with a typed result/exit/enter
  pipeline. ViewModel holds one pending committed destination, publishes it once at entry, and
  keeps one in-progress race guard across the lifecycle.
- Destination image, typing/reveal state, timer, focus, bring-into-view, audio, and animation keys
  cannot observe the next item before the atomic swap. Practice advancement reuses the pipeline;
  direct evaluative rating and all shared queue/transaction authorities are unchanged.
- Integrated Desktop image-size and no-flash UAT remains pending.
- Verification: focused capability/regression suites pass; full clean build 584 suites / 3,038
  tests (root 365 / 1,792; Desktop 219 / 1,246), failures/errors/skipped 0 / 0 / 0.

# UX-006 — Direct Evaluative Rating Dock and Dynamic Study Space Allocation

- Made evaluative Again/Hard/Good/Easy dock controls actionable on concealed fronts and revealed
  answers, including Review All, Normal Study, and resumed sessions through shared availability.
- Front intent resolves reveal authority and commits exactly one normal review with `MANUAL_USER`;
  keyboard 1–4 follows the same path while focused typing retains its existing input contract.
- Removed the separate evaluative manual-rating header action, dialog, callbacks, and local state.
  Practice dock feedback and confirmed `MANUAL_USER_OVERRIDE` remain unchanged.
- Replaced worst-case typing and aspect-percentage reserves with true outer typing minima and real
  remaining-height image allocation. Inner line-box safety, Decision Dock, `ContentScale.Fit`, and
  the headerless six-item inventory are preserved. Integrated Desktop UAT remains pending.
- Verification: focused 14 XML suites / 71 tests; full clean build 584 suites / 3,036 tests (root
  365 / 1,792; Desktop 219 / 1,244), failures/errors/skipped 0 / 0 / 0.

# UX-005 — Evaluative Manual Rating and Vertical Space Budget

- Added shared Manual Evaluation availability and persisted `MANUAL_USER` provenance through the
  existing atomic evaluative review, Scheduler/FSRS, advancement, inventory, and Undo boundaries.
- Added a visible `Đánh giá thủ công` action for active Normal Study, resumed evaluative sessions,
  and Review All. Explicit opening reveals before confirmation; Practice Override remains separate.
- Added typed vertical allocation that reserves translation/POS, timer, complete typing field,
  Decision Dock, inventory/examples, and safe spacing before aspect-aware image allocation.
  Portrait/extreme portrait yield height/width first; images remain `ContentScale.Fit`.
- Removed Rating Inventory visual header, duplicate total summary, expand/collapse symbols, and
  local expansion state. Six semantic items render directly as one wide row or compact 2×3 grid.
  Integrated Desktop UAT remains pending.
- Verification: focused 9 XML suites / 32 tests; full clean build 589 suites / 3,056 tests (root
  370 / 1,815; Desktop 219 / 1,241), failures/errors/skipped 0 / 0 / 0.

# UX-004 — Typing Semantics, Practice Controls, and Text Layout Finalization

- Added shared normalized `VALID_PREFIX` evaluation; proper prefixes are live-only and no longer
  classified as incorrect or completed attempts. Real mismatch and extra characters remain typed
  `INCORRECT` states.
- Reworked the typing input as `CenteredTypingField` with one minimum inner line-box authority for
  typed text, placeholder, cursor, selection, label, and trailing action; removed the visual
  `Luyện gõ` progress subtitle while retaining field accessibility context.
- Added explicit `LUYỆN TẬP` identity and a visible labeled `Đổi đánh giá` action for every active
  Practice item. Shared availability controls enabled state/reason; unchanged dialog selection
  cannot be confirmed.
- Added six typed inventory semantic kinds/color roles and maps them to the existing rating/design
  tokens, with one-row wide and 2×3 compact presentation. Integrated Desktop UAT remains pending.
- Verification: focused 15 XML suites / 97 tests; full clean build 586 suites / 3,049 tests (root
  369 / 1,813; Desktop 217 / 1,236), failures/errors/skipped 0 / 0 / 0.

# UX-003 — Typing Feedback, Manual Override Discoverability, and Realtime Rating Inventory

- Mapped shared realtime typing evaluation to a stable neutral, semantic red-X, or semantic
  green-check action icon with accessible descriptions; reveal/click/Enter behavior is unchanged.
- Added inner vertical line-box padding while retaining font leading, protecting typed text,
  placeholder, selection, cursor, and descenders across responsive viewport classes.
- Made confirmed Manual Rating Override discoverable throughout Practice, gated by shared typed
  availability, and relabeled four ordinary actions as explicitly practice-local recall feedback.
- Projected the shared Rating Inventory into idle, active Study, active Practice, and completion
  UI state with expandable compact presentation and no Desktop counting logic. Integrated Desktop
  UAT remains pending.
- Verification: focused 7 XML suites / 30 tests; full clean build 586 suites / 3,046 tests (root
  369 / 1,811; Desktop 217 / 1,235), failures/errors/skipped 0 / 0 / 0.

# REV-002 — Review All Current Again / Hard Items

- Removed the normal Study `reviewItemLimit` from difficult-review availability and start
  contracts. Again/Hard focused review now selects every eligible deduplicated item in the current
  learner/package/topic/content scope.
- Availability exposes one exact count, which is also the queue size, configured/effective review
  workload, progress denominator, and focused session `reviewItemLimit`. The focused action no
  longer calls `take(reviewItemLimit)` or consumes a Study preset.
- Preserved latest-effective-rating membership, Again-before-Hard/due/oldest/ID ordering,
  fresh-session and rollback safety, latest-session New review, Review All, and the global normal
  Study planner/`SessionPolicyLimiter` behavior.
- Verification: focused 6 XML suites / 70 tests; full clean build 575 suites / 3,006 tests (root
  361 / 1,777; Desktop 214 / 1,229), failures/errors/skipped 0 / 0 / 0. Integrated Desktop UAT
  remains pending.

# REV-001 — Focused Review Entry Modes

- Replaced the chooser's broad latest-session replay with an application-owned latest-completed
  NEW-only review. The predecessor queue's `SessionItemOrigin.NEW`, committed completion/review
  membership, current scope, enabled status, content deduplication, and stable queue order are the
  selection authority.
- Added Again/Hard focused review from each scoped content's latest chronological committed
  `ReviewEvent`. Ordering is Again before Hard, then due/overdue, oldest latest review, and stable
  item ID; the current review limit caps session membership.
- Both focused modes create ordinary review-origin sessions with zero New limit, fresh identities,
  active-session rejection, and queue-failure session rollback. Review All retains its existing
  learned-item policy and ordering; Scheduler/FSRS, ratings, memory, persistence schema, Undo,
  Continuous Review, Typing, and Study V3 presentation authority are unchanged.
- Desktop presents Continue, latest-session New, Again/Hard, Review All, and Library actions in
  that order with typed availability, localized descriptions, and explicit callbacks.
- Verification: focused 7 XML suites / 74 tests; full clean build 575 suites / 3,005 tests (root
  361 / 1,776; Desktop 214 / 1,229), failures/errors/skipped 0 / 0 / 0. Integrated Desktop UAT
  remains pending.

# V3-004 — Center Typing Content and Replace Success Text with POS

- Replaced the legacy filled Typing `TextField` layout with an equivalent themed surface and
  `BasicTextField`, centering editable text, caret, placeholder, and trailing Reveal action within
  the adaptive field while retaining the existing label, font authority, focus, keyboard, live
  evaluation, and BringIntoView behavior.
- Replaced the Typing-success copy with the current item's existing semantic POS badge. The overlay
  now announces canonical answer, canonical POS, rating transition, and rating explanation without
  announcing the removed success copy; unknown and long POS labels retain registry fallback and
  responsive badge behavior.
- Preserved popup timing/dismissal, automatic/manual rating, LQ-002, Scheduler/FSRS, queue/session,
  persistence, Undo, Continuous Review, audio, completion, and every learning authority.
- Verification: focused 8 XML suites / 71 tests; full clean build 575 suites / 3,000 tests (root
  361 / 1,771; Desktop 214 / 1,229), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop
  UAT remains pending.

# V3-003 — Adaptive Answer Space Utilization

- Added one typed Study space-allocation resolver over width, height, Answer/disclosure state,
  example requirements, bottom controls, and image aspect class. It owns image min/max height,
  vertical spacing, Typing minimum height, full Example allocation, and last-resort bounded scroll.
- Collapsed Answer assigns surplus height to the Fit image; expanded Answer compresses spacing and
  image before scroll while retaining complete Example content. Continuation and Decision Dock
  remain outside the bounded content area.
- Added explicit viewport-aware Typing field minimum height so line height, padding, cursor, and
  trailing action no longer clip text on wide, standard, compact, or short layouts.
- Preserved the V3-002 centered one-row translation, P0-001 concealment, Examples commands,
  Scheduler/FSRS, rating/Typing behavior, keyboard/focus/accessibility, and all data authorities.
- Verification: focused 11 suites / 99 tests; full clean build 574 suites / 2,997 tests (root
  361 / 1,771; Desktop 213 / 1,226), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop
  UAT remains pending.

# V3-002 — Finalize Approved Answer Golden Layout

- Made the Answer illustration the dominant visual element with a 98%-of-stage width budget and
  viewport-aware height that remains large while Examples are collapsed and contracts when the
  existing accordion expands. Intrinsic aspect handling and `ContentScale.Fit` remain authoritative.
- Replaced the labelled Meaning block with one centered, single-line audio-and-translation row;
  removed the heading and its reserved spacing while preserving row audio interaction.
- Kept examples disclosure/keyboard behavior, scheduler metadata, Decision Dock, rating/Typing,
  P0-001 concealment, focus, scrolling, accessibility, and every business/data authority unchanged.
- Verification: focused 8 suites / 74 tests; full clean build 573 suites / 2,995 tests (root
  361 / 1,771; Desktop 212 / 1,224), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop
  UAT remains pending.

# P0-001 — Restore True Front-Side Recall Isolation

- Restored Typing recall front projection to `LearningSceneRenderer` with the existing typed
  presentation filters; `DiscoveryFrontSurface` is again reserved for explicit new-content
  introduction rather than unrevealed Typing recall.
- Removed the regression introduced by `ff865de`, where unrevealed Typing routed through a
  surface that composes canonical English identity, IPA/POS, and Vietnamese meaning. Focus and
  BringIntoView remain UX behavior only and no longer conceal composed answer content.
- Preserved Typing evaluation/automatic rating including LQ-002, manual reveal/rating,
  Scheduler/FSRS, queue/session/persistence, keyboard, focus, Undo, UX-001, and UX-002 authority.
- Verification: focused 4 suites / 49 tests; full clean build 569 suites / 2,982 tests (root
  361 / 1,771; Desktop 208 / 1,211), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop
  UAT remains pending.


# V3-001 — Implement Approved Study Visual System

- Replaced the active Study presentation tree with a signature composition governed by one typed
  width-and-height policy (`Expanded`, `Standard`, `Compact`, `Compressed`). Compression reduces
  spacing, image budget, scheduler density, example count, then optional metadata; only compressed
  Answer may use bounded content scrolling while the Decision Dock remains fixed.
- Rebuilt active chrome as a compact session header; flattened the lexical hero onto the clean
  stage; made Discovery meaning learning content; replaced the default outlined recall field with
  a filled control and circular trailing action; removed `FullAnswerFitLayout` from Answer runtime;
  and rendered EN/VI examples as one reading passage rather than nested field surfaces.
- Added one intrinsic-aspect image presentation owner for wide landscape, standard landscape,
  square, portrait, and extreme media. It budgets stable frame width/height without changing the
  image source, stretching/cropping, or the default `ContentScale.Fit` authority.
- Unified rating actions inside shared Decision Dock geometry while preserving four independent
  semantic hit targets, 1–4, Space=Good, focus order, rating feedback, manual/automatic rating, and
  LQ-002. Image source/sizing and `ContentScale.Fit`, UX-001, UX-002, motion, audio, completion, and
  every business/data authority remain unchanged.
- Verification: 12 focused suites / 112 tests; full clean build 572 suites / 2,992 tests (root
  361 / 1,771; Desktop 211 / 1,221), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop
  UAT remains pending.

# EPIC-001R — Reimplement Approved Study Workspace

- Re-composed the two approved runtime scenes around an explicit reading order. Discovery now
  presents lexical hero and pronunciation metadata first, followed by contextual image, meaning,
  Typing, its integrated reveal action, and the established Decision Area. Answer now confirms the
  result before the lexical hero, then reads image, Meaning, Examples, scheduler explanation, and
  Decision Area as one continuous learning workspace.
- Removed presentation-only card treatment from Meaning and Examples and replaced the separate
  Typing reveal button with an action inside the input boundary. Image sizing and
  `ContentScale.Fit`, responsive policies, semantic rating actions, keyboard/focus/accessibility,
  and existing motion authority remain intact.
- Preserved Scheduler/FSRS, LQ-002, StudyFacade, rating/review transaction, queue/session,
  persistence, Typing evaluation, audio, Undo, Continuous Review, UX-001, and UX-002 authorities.
- Verification: 8 focused suites / 95 tests; full clean build 569 suites / 2,979 tests (root
  361 / 1,771; Desktop 208 / 1,208), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop
  UAT remains pending.

# EPIC-001 — Premium Study Experience

- Established **Learning Engine Focused Immersion** as the Desktop Study design direction through
  pure Canvas, hero, content-rhythm, Decision Area, and motion presentation contracts.
- Re-composed Study into a bounded, elevated learning stage with clearly separated workspace,
  focal hero, knowledge/reading, explanation, and action depth in Light/Dark and all responsive
  classes. Discovery now reads as a visual prompt; Understanding centers word identity and grouped
  learning content; Rating presents as one intentional semantic decision group.
- Added item-identity-keyed arrival motion using existing `LEMotionTokens`, without replay on
  reveal/recomposition/resize or transaction delay. Existing reveal, rating confirmation,
  continuity, completion, UX-001, and UX-002 motion/feedback contracts remain authoritative.
- Preserved Scheduler/FSRS, LQ-002, rating policy, review transaction, queue/session/persistence,
  Typing, audio, image sizing/`ContentScale.Fit`, keyboard, focus, accessibility, Undo, and
  Continuous Review behavior.
- Verification: 13 focused suites / 143 tests; full clean build 569 suites / 2,975 tests (root
  361 / 1,771; Desktop 208 / 1,204), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop
  UAT remains pending.

# UX-002 — Quiet Scheduler Feedback

- Added typed `ACTIVE_ANSWER`, `COMPLETION`, and `CONTINUITY` scheduler-feedback contexts with a
  pure deterministic presentation resolver. Active-answer feedback is an explanatory inline layer:
  bounded on standard/wide layouts, compact-wrap capable, secondary in content tone, and lower in
  font weight while retaining the established typography scale.
- Preserved structured committed-rating identity through existing Light/Dark semantic tokens and
  retained interval wording, details expansion, accessibility summary, metric content, ordering,
  and null behavior. Completion and continuity keep their consequence emphasis and existing layout.
- Scheduler data, calculations, learning authority, UX-001 sequencing, rating/Typing, callbacks,
  focus, keyboard, and accessibility ownership are unchanged. Focused verification passed 7 suites /
  95 tests; full `clean test` passed 568 suites / 2,966 tests (root 361 / 1,771; Desktop 207 /
  1,195), with 0 failures, errors, or skipped. Integrated Desktop UAT remains pending.

# UX-001 — Clear Previous Consequence Before Next Item

- Added a pure continuity presentation projection that keeps the next item visually hidden while
  the committed source consequence is visible, then disposes the Next Item overlay without an exit
  composition before destination arrival begins. The previous consequence can no longer overlap
  the newly arriving item during fade-out or recomposition.
- Preserved Completion's existing consequence, background emphasis, and fade-out behavior. Motion
  durations, focus, keyboard, accessibility, audio, Scheduler/FSRS, rating, Typing, queue, session,
  transaction, and persistence authorities are unchanged.
- Focused verification passed 4 suites / 45 tests. Full `clean test` passed 567 suites / 2,960
  tests (root 361 / 1,771; Desktop 206 / 1,189), with 0 failures, errors, or skipped.

# LQ-002 — Immediate Post-Lapse Automatic Rating Guard

- Capped automatic Typing candidates above Hard when an authoritative review-origin context has
  previous rating Again, current stage Relearning, and prior review in the active session. Reveal
  and existing Hard evidence retain their established outcomes; normal Review, New, Learning, and
  manual rating paths are unchanged.
- Kept `TypingAutoRatingPolicy` as candidate authority and left `StudyFacade` re-validation,
  `MemoryConfidenceRatingGate`, Scheduler/FSRS, queue, session, transaction, analytics, Continuous
  Review, and persistence contracts unchanged.
- Focused verification passed 3 suites / 34 tests. Full `clean test` passed 567 suites / 2,956
  tests (root 361 / 1,771; Desktop 206 / 1,185), with 0 failures, errors, or skipped.

# AURORA-005 — Learning Session Continuity

- Added one transient, Desktop-owned, token-safe sequence after each successful committed rating:
  action confirmation, scheduler consequence, then next-item or completion arrival. Review and
  persistence still finish before presentation begins; failure, Undo, restart, and stale callbacks
  cannot create or replay a success sequence.
- Bound the sequence to immutable source/destination item identities and the existing unique rating
  token. Structured committed rating now travels with scheduler feedback, so automatic Typing uses
  the final authoritative rating without parsing display text.
- During continuity, the outgoing scheduler consequence renders separately from destination content.
  Next-item feedback is cleared after arrival; completion retains its existing consequence contract.
  Focus, scroll, autoplay, Typing reset, action guards, completion actions, Undo, replay, Continue,
  and Continuous Review authorities are unchanged.
- Focused verification passed 8 suites / 57 tests; broad continuity regression passed 42 suites /
  305 tests. Full `clean test` passed 563 suites / 2,909 tests (root 361 / 1,771; Desktop 202 /
  1,138), with 0 failures, errors, or skipped. Integrated Desktop UAT remains pending.

# AURORA-004 — Study Micro Interaction Polish

- Added a deterministic, token-timed Question-to-Answer reveal progression: Answer settles first,
  Meaning follows more lightly, and Scheduler becomes perceptible last. The transition uses opacity
  and a token-sized graphics-layer translation only, so layout, spacing, hierarchy, and focus order do not move.
- Rating Dock elevation now distinguishes busy/resting from ready state through existing theme tokens.
  Study rating buttons gain subtle press scale plus hover/focus elevation; polished hover/press does
  not replace their semantic colors, and the existing semantic rating-confirmation contract is unchanged.
- Added pure timeline and source-contract coverage. Focused verification passed 8 suites / 72 tests;
  Study/Typing/Rating/Scheduler/Completion/Focus/Keyboard/Audio regression passed 35 suites / 270 tests.
  Full `clean test` passed 562 suites / 2,899 tests (root 361 / 1,771; Desktop 201 / 1,128),
  with 0 failures, errors, or skipped. Integrated Desktop UAT remains pending.

# AURORA-003 — Study Visual Focus

- Added one deterministic Study presentation hierarchy for Question, Answer, Meaning, Example,
  Scheduler, header/metadata, Rating Dock, and rating actions. The hierarchy changes only
  semantic surface, border, elevation, and content-tone emphasis through existing LETheme tokens.
- Question/Answer remain the primary reading anchor; Meaning is secondary-primary; Rating remains
  the action anchor. Example, Scheduler, header, and metadata recede without changing layout order,
  typography scale, spacing, image authority, keyboard, accessibility, motion, or learning behavior.
- Scheduler feedback no longer presents its committed rating as a selected rating badge; the exact
  rating text and interval/technical feedback remain visible. Rating actions retain their semantic
  red/orange/green/blue identity.
- Focused verification passed 6 suites / 47 tests, plus the post-session regression suite with the
  new focus contract. Full `clean test` passed 561 suites / 2,894 tests (root 361 / 1,771;
  Desktop 200 / 1,123), with 0 failures, errors, or skipped. Integrated Desktop UAT remains pending.

# AURORA-002 — Study Experience Polish, Phase 1

- Rebalanced Study whitespace and vertical rhythm without changing the established
  Question → Answer → Meaning → Example → Scheduler → Rating order.
- Removed redundant nested content padding from the Meaning, Example, compact Scheduler, and
  Rating Dock semantic surfaces; retained their existing LETheme roles and component bounds.
- Increased responsive full-answer section gaps for compact/minimum density and added a clearer
  top separation for the fixed action dock. Image sizing, typography, motion, semantics, focus,
  keyboard, counters, rating/review behavior, and Scheduler/FSRS are unchanged.
- Focused verification passed 4 suites / 63 tests. Full `clean test` passed 560 suites / 2,881
  tests with 0 failures, errors, or skipped. Integrated Desktop UAT remains pending.

# PLE-039-H — Semantic Rating Action Feedback

- Added transient two-phase feedback shared by mouse, keyboard, Space = Good, forced Again, and
  automatic Typing using the final rating accepted by the existing review path.
- Only the selected semantic rating scales/highlights and receives a confirmation check. LETheme
  motion/color/border/elevation tokens preserve dock geometry and the duplicate-action guard.
- Rating policy, Memory Confidence, Scheduler/FSRS, review/session/queue persistence, Undo, and
  Continuous Review are unchanged. Integrated Desktop UAT remains pending.
- Focused XML evidence is 6 suites / 47 tests; full `clean test` passed 560 suites / 2,878 tests
  (root 361 / 1,771; Desktop 199 / 1,107), with 0 failures, errors, or skipped.
- Review remediation localizes the confirmation semantic through `StudyWorkspaceStrings`
  (`Confirmed` / `Đã xác nhận`) and adds a single screen-owned token-safe release callback.
  Matching tokens clear transient feedback only; stale/null callbacks are no-ops.
- Remediation focused verification passed 3 suites / 25 tests. Full `clean test` passed 560 suites /
  2,881 tests (root 361 / 1,771; Desktop 199 / 1,110), with 0 failures, errors, or skipped.

# PLE-032-B2.1 — Durable Continuous Review Restart Foundation

- Added an opt-in, learner/package/topic-scoped Continuous Review intent with a dedicated
  schema-v1 JSON envelope. Missing legacy files mean disabled; malformed/unsupported data is
  rejected without rewrite.
- Restart now reconciles an active session first, then selects the latest successfully completed
  general-Study predecessor by finished time and SessionId before delegating to the unchanged
  deterministic B1 continuation use case. No-work and inconsistency remain explicit outcomes.
- Wired enable/disable/query/recovery through `LearningEngine`, persisted/in-memory composition,
  Desktop startup, guarded ViewModel actions, and a completion-only localized accessible switch
  for eligible general Study. Scheduler/FSRS, review, Undo, queue reinsertion, and ordering are
  unchanged; integrated Desktop UAT remains pending.
- Final remediation verification passed 559 XML suites / 2,873 tests (root 361 / 1,771;
  Desktop 198 / 1,102) with zero failures, errors, or skipped tests.
- Review remediation adds durable completion provenance, safely decodes legacy absence as
  `UNKNOWN`, excludes reconciliation/replacement closures, validates exact requested scope before
  mutation, and projects/enables/disables the completion switch only for its displayed scope.
  Ordinary completion without a Product Brain snapshot remains eligible and restart-deterministic.

# PLE-039 Consolidated Product Baseline

Stable implementation baseline: `a1cb4600d5433c7a4e786168ba96fb6ecae45429`.

- Delivered a deterministic, ReviewEvent-derived Memory Confidence model and an Easy-only gate;
  confidence is not persisted and cannot promote ratings or create Hard/Again.
- Remediated Typing rating semantics with independent speed bands, bounded 45% Easy timing,
  normalized error severity, logical mistake episodes, and one preview/final resolver.
- Compacted the Typing surface and rating-state-only dock, preserved frame-synchronized input
  visibility, and reset revealed answers once to the typed/canonical comparison top.
- Added deterministic SessionId/LearningItemId SHA-256 ordering for NEW slots before policy
  limiting. REVIEW priority, Again/Hard reinsertion, Scheduler/FSRS, review transactions, Undo,
  and persistence schemas are unchanged.
- Full automated evidence: 555 XML suites / 2,857 tests (root 359 / 1,759; Desktop 196 / 1,098),
  with 0 failures, errors, or skipped. Final integrated Desktop UAT remains pending.

# PLE-039-G — Reset Answer Surface Scroll on Reveal

- Hoisted the Study main-body scroll state so Typing front input visibility and answer-back
  positioning share one presentation-local authority with distinct transition intent.
- Typing answer activation now waits for the revealed layout frame and immediately resets the
  main body to its top, exposing the typed/canonical comparison before lower answer sections.
- The reset is one-shot and item/phase scoped; recomposition, audio/timer/rating updates, manual
  back-side scrolling, and resize do not retrigger it. Exact-success overlay, rating, scheduler,
  queue, session, persistence, and NEW ordering behavior are unchanged.

# PLE-039-F — Compact Rating Dock and Seeded New-Item Randomization

- Reduced the Typing rating dock to its four semantic status segments and removed the redundant
  automatic-rating explanation card and journey note, including their unused localization.
- Typing layout now reserves only the single four-status row while retaining the existing target
  height, fixed-dock boundary, keyboard semantics, colors, icons, and active underline.
- Added stable SessionId-seeded NEW candidate ordering after strategy placement and before
  diversity, balance, and policy limiting. SHA-256-derived UTF-8 seeds avoid runtime hash/global
  randomness; REVIEW candidates are untouched and no persisted contract changed.

# PLE-039-E — Typing Input Visibility Guarantee

- Suppressed the redundant primary Typing instruction and the duplicate visible `Typing Recall`
  label immediately above the answer field; non-Typing primary instructions and supporting
  Typing meaning behavior remain unchanged.
- Attached a Compose `BringIntoViewRequester` to the complete answer field. Item bind,
  re-enablement, height-mode changes, and explicit refocus request visibility after a frame;
  timer ticks, keystrokes, and projected-rating updates do not trigger scrolling.
- The field retains autofocus, typography, live diff, caret, Reveal behavior, and the bounded
  height-mode sizing from PLE-039-D. Accessibility now exposes `Typing Recall. Your answer.`
  without requiring either removed visible label.
- The existing center scroll viewport and external rating dock remain authoritative. Rating,
  learning, scheduling, and persistence semantics are unchanged.

# PLE-039-D — Compact Typing Study Surface

- Removed the persistent speed/explanation row and four-item threshold legend from the visible
  Typing Study surface while retaining the timer and final projected-rating presentation.
- Returned the reclaimed vertical space to the answer field through bounded height-mode-aware
  minimum sizing: +32dp in comfortable, +24dp in compact-height, and +8dp in minimum-height.
- Preserved the existing center scroll region and fixed rating dock boundary, so short viewports
  scroll naturally without timer-driven auto-scroll or dock overlap.
- Timer accessibility still describes elapsed time, speed band, final rating, and the policy
  explanation. Rating policy, confidence gate, thresholds, input behavior, and persistence are
  unchanged.

# PLE-039-C — Typing Rating Semantics Remediation

- Separated pure active-typing speed bands from final automatic ratings, so the timer retains
  Easy blue while confidence or spaced-memory evidence may cap the projected result at Good.
- Recalibrated Easy active typing to bounded 45% of expected duration (2.5–8.0 seconds) while
  retaining the existing expected-duration formula, recall threshold, and Hard timing.
- Added expected-prefix edit-distance evidence and immutable mistake episodes. Normalized
  severity by canonical answer length and repeated independent episodes now own typing-quality
  Hard decisions; raw keystroke mismatch/correction counters are diagnostic only.
- Minor corrected typos lock Easy but remain Good without genuine Hard evidence. Immediate
  Relearning/previous-Again clean exact attempts also remain Good rather than becoming false
  Hard. The confidence gate remains Easy-only.
- Timer, projected rating, legend, success overlay, and accessibility now distinguish speed
  evidence from the final rating and provide one concise policy-owned explanation.

# PLE-039-B — Memory Confidence Gate

- Added a fail-safe confidence gate after the unchanged Typing candidate policy and existing
  spaced-memory guards. The gate can only block candidate Easy to Good; Again, Hard, and Good
  pass through unchanged, and it never promotes a rating.
- Facade queries exact learner/item history once through `MemoryConfidenceQueryService`, applies
  pending Easy evidence, and caches the projection for the current experience. Preview and final
  consume that same immutable projection through one automatic-rating resolver.
- Reliable projected High/Very High permits Easy. Medium or below, missing/unavailable
  projection, unreliable history, or query failure resolves to Good. Scheduler, FSRS,
  ReviewEvent, Undo, Queue, Session transaction, timing, and persistence semantics are unchanged.

# PLE-039-A — Derived Memory Confidence Domain

- Added immutable 0–100 confidence score, deterministic tier/spacing/reason models, and a pure
  projector over canonical ReviewEvent history with optional caller-timed pending evidence.
- The conservative heuristic rewards independently spaced Good/Easy evidence, limits immediate
  repetition, and reacts to Again/Hard without claiming a calibrated probability. Mixed
  learner/item history is rejected; broken state continuity is explicitly unreliable.
- Added an application query service over the exact learner/item ReviewEvent repository port.
  ReviewEvent remains the sole durable authority: no confidence repository, record, schema,
  cache, Desktop integration, rating change, Scheduler/FSRS change, or UI change was introduced.
- Historical typing mismatch, correction, active/pre-typing duration, mode, Reveal provenance,
  and same-session evidence remain unavailable because ReviewEvent does not persist them.

# PLE-038-D — Spaced-Memory Guard and Rating Transition Feedback

- Easy now requires durable spaced-memory evidence: a previous Good/Easy event, at least twelve
  hours since that event, a trusted history snapshot, no Relearning context, and no earlier
  committed review of the same Content in the current Session. Fast clean attempts without that
  evidence are capped at Good; Hard evidence and Reveal retain precedence.
- Facade reconstruction validates previous rating, review timestamp, same-session status, and
  item-presentation time against `ReviewEventRepository` and `StudySession`. Preview, legend,
  and final rating share the same policy and cannot rely on UI-authored memory context.
- The exact-success overlay now presents a text-and-color previous-to-final automatic-rating
  transition with accessibility semantics. No Scheduler, FSRS, Queue, Session, ReviewEvent, or
  persistence-schema semantics changed.

# PLE-038-C — Smart Typing Timer Start and Rating Calibration

- Split transient Typing timing into item-presented, pre-typing recall, active typing, and total
  attempt elapsed semantics. The visible timer stays at `00:00` Ready until first committed
  material input, then measures only active typing.
- Recalibrated expected duration to `4000 + 450 × code points`, clamped to 6–30 seconds. Hard
  uses slow active typing, extreme pre-typing recall, or existing mismatch/correction evidence;
  Easy retains eligibility safeguards and adds a conservative pre-typing limit.
- Added an explicit Ready preview without fabricated exact metrics. Preview and final rating use
  the same policy; Reveal remains Again. Metrics remain transient and Scheduler, FSRS, Queue,
  Session, and persistence are unchanged.

# PLE-038-B — Dynamic Auto-Rating Timer Presentation

- Replaced the small `labelLarge` Typing timer and emoji with a dedicated vector timer
  presentation using responsive 48/40/30sp bold monospaced values and 44/38/30dp icons for
  Wide/Standard/Compact viewports. Wide aligns the timer to the content edge; smaller viewports
  center it without horizontal scrolling.
- Added a pure `TypingAutoRatingPreviewResolver`. It projects an immutable “completed now”
  metrics snapshot from the current attempt and delegates to the unchanged
  `TypingAutoRatingPolicy`; ticks do not mutate attempts, counters, focus, audio, or rating state.
  Stopped attempts use their authoritative `stoppedAtMillis` and retain their final preview.
- Named the existing policy percentages/counts so policy and presentation share one source.
  Item-specific Easy maximum and Hard minimum elapsed values derive from the same normalized
  expected duration. Easy availability also reflects origin, stage, previous Again, recall
  latency, mismatch, and correction evidence.
- Added one localized dynamic legend below the timer. Again means Reveal Answer rather than an
  elapsed threshold; Hard, Good, and Easy show item-derived bands or Easy ineligibility.
  Semantic colors remain Again red, Hard orange, Good green, and Easy blue.
- Kept the PLE-038-A four-column learning-memory panel distinct from the attempt projection.
  Its active status now says `Previous`, with `Next` and `Available` for the other short states;
  no timing threshold returned to that panel.
- Added a compact localized information card explaining automatic timing/rating and that manual
  rating selection is unnecessary. The journey note remains below it, and merged semantics avoid
  duplicate screen-reader statements.
- Timer lifecycle, final auto-rating decisions, Forced Again, keyboard remapping, success
  overlay/audio, comparison, autofocus, meaning autoplay, non-Typing presentation, Scheduler,
  FSRS, Queue, Session, and persistence are unchanged.
- Focused Desktop verification passed 14 suites / 128 tests. Full
  `clean test --no-daemon --console=plain` passed 548 XML suites / 2,810 tests (root 354 suites /
  1,739 tests; Desktop 194 suites / 1,071 tests), with 0 failures, errors, or skipped tests.
  The +2 Desktop suites / +15 tests cover preview policy fidelity, purity, clamps, responsive
  timer/legend composition, semantic colors, accessibility, and the protected regressions.

# PLE-038-A — Simplified Typing Rating Status Panel

- Replaced the Typing pre-answer memory footer with a presentation-only four-column status
  panel. Again, Hard, Good, and Easy retain distinct icons and colors, while the latest effective
  rating is the sole strongly emphasized, underlined `Previous` segment.
- The rating immediately after the previous status is labeled `Next`; remaining segments are
  labeled `Available`. Inactive icons and titles retain their semantic rating colors at reduced
  emphasis instead of becoming fully gray.
- Removed all timing detail from the lower panel contract. It contains no seconds, threshold
  ranges, or timing explanation; the existing timer remains the sole timing presentation.
  Segment height is reduced by 4dp and the localized learning-journey note remains centered.
- The new projection is restricted to Typing. Image, Listening, Prompt, full-answer rating
  actions, timer behavior, auto-rating, keyboard dispatch, Scheduler, FSRS, Queue, Session, and
  persistence are unchanged.
- Focused Desktop verification passed 3 selected suites / 7 tests. Full
  `clean test --no-daemon --console=plain` passed 546 XML suites / 2,795 tests (root 354 suites /
  1,739 tests; Desktop 192 suites / 1,056 tests), with 0 failures, errors, or skipped tests.

# PLE-038 — Typing Attempt Measurement and Automatic Rating

- Added an item-scoped monotonic Typing attempt tracker. It measures first-input recall latency,
  typing duration, total elapsed time, committed material input changes, positional mismatch
  events, and deterministic correction evidence without counting selection-only edits,
  composition-in-progress, recomposition, or presentation timer ticks.
- Typing Question now displays a compact `⏱ mm:ss` timer with queryable accessibility text and
  no per-second live-region announcements. Exact completion, Reveal, Pause, Undo, item change,
  navigation/disposal, session completion, and recoverable failure stop or discard the transient
  attempt before success dwell, answer audio, rating, or next-item time can be counted.
- Added a pure deterministic auto-rating policy. Expected time is
  `2,500ms + 260ms × canonical code points`, clamped to 3,500–18,000ms. Reveal always resolves to
  Again; exact attempts resolve to Hard at 160% expected total time, 75% expected recall latency,
  three mismatch events, or three corrections; conservative fast/clean Review or Mastered
  attempts may resolve to Easy; remaining exact attempts resolve to Good.
- `StudyFacade.completeCorrectTypingRecall` no longer hard-codes Good. It validates current
  session/item/generation evidence and item-origin/stage/previous-rating context, recomputes the
  policy decision, reveals the answer, then sends only the final Hard/Good/Easy rating through
  the existing atomic `reviewInternal` transaction.
- Manual Typing Reveal snapshots metrics and enters item-scoped `FORCED_AGAIN`. The answer and
  C3 comparison remain visible, while the four-button Rating Dock is replaced by one localized
  Continue — Review Again action. Enter, NumPad Enter, Space, 1, 2, 3, 4, and legacy rating
  callbacks all converge on the specialized idempotent Again boundary.
- Scheduler, FSRS, Queue, Session, canonical answer, live diff, comparison, autoplay, success
  overlay/audio, and input typography are unchanged. Raw attempt metrics, timer ticks, and
  pending tokens are not persisted; the existing `ReviewEvent` remains durable authority for
  the final rating. Historical Typing speed analytics remain future work.
- Focused Desktop verification passed 12 XML suites / 117 tests. Full
  `clean test --no-daemon --console=plain` passed 545 XML suites / 2,793 tests (root 354 suites /
  1,739 tests; Desktop 191 suites / 1,054 tests), with 0 failures, errors, or skipped tests.
  The +2 Desktop suites / +20 Desktop tests exactly cover PLE-038 measurement, policy,
  presentation/keyboard guards, invalid-decision rejection, retry/idempotency, and real-session
  Hard/Easy/Forced-Again Next and final-item persistence.

# PLE-037-B — Centered Typing Input Typography

- Centered the raw editable English text and `Your answer…` placeholder through the existing
  `OutlinedTextField` text style. The entire natural text block reflows from center without a
  character grid, per-glyph animation, inserted whitespace, or artificial letter spacing.
- Extended `TypingPresentationResolver` with explicit responsive alignment, line-height, and
  zero-letter-spacing policy. Typed text is now 48/43/38sp SemiBold and placeholder text
  40/36/32sp at 0.70 alpha for Wide/Standard/Compact, while the floating label and existing
  116/106/96dp field heights remain unchanged.
- Short input uses the existing Material single-line layout to center its natural text block
  vertically. Explicit newlines or input longer than 24 Unicode code points retain multiline
  2–5 line wrapping without an overlay, offset, character grid, or input mutation.
- The same `TextFieldValue`, identity `OffsetMapping`, autofocus, selection, composition, paste,
  IME Done, Enter Reveal, positional live diff, success, meaning-autoplay, and Manual Reveal
  boundaries remain unchanged.
- Focused Desktop verification passed 6 XML suites / 67 tests. Full
  `clean test --no-daemon --console=plain` passed 543 XML suites / 2,773 tests (root 354 suites /
  1,739 tests; Desktop 189 suites / 1,034 tests), with 0 failures, errors, or skipped tests.

# PLE-037-A — Typing Front-Side Focus Polish

- Replaced the role-specific Typing audio guard with an explicit scene-level manual-audio
  interaction policy. Typing Question suppresses every manual audio control and image audio
  affordance, including legacy `OTHER` blocks, while retaining all media paths for autoplay,
  success, and revealed Answer surfaces. Non-Typing scenes retain existing controls.
- Typing presentation recommendation now requires an available Vietnamese meaning and its
  one-shot audio regardless of Adaptive, Preference Guided, or Manual controls. The existing
  `StudyAutoplayCoordinator` selects meaning audio at Question bind and deduplicates by item and
  phase; input/caret recomposition, success, and Manual Reveal do not replay it.
- Removed the Meaning supporting heading on Typing Question only. Vietnamese meaning and POS now
  use a centered wrapping group with responsive 32/28/24sp meaning and 15/14/13sp POS typography.
- Enlarged the responsive Typing input to 116/106/96dp minimum height with 30/27/24sp typed text,
  a resolved 28/25/23sp `Your answer…` placeholder, and readable label sizing. Existing editable
  control, focus border, caret, selection, paste, IME, live diff, Enter, and Reveal behavior remain.
- Success overlay, English answer audio, atomic GOOD, Next/Completion, Manual Reveal comparison
  and audio controls, flow strategy, scheduler, FSRS, queue, Session, and persistence are unchanged.
- Focused Desktop verification passed 14 XML suites / 149 tests. Full
  `clean test --no-daemon --console=plain` passed 543 XML suites / 2,772 tests (root 354 suites /
  1,739 tests; Desktop 189 suites / 1,033 tests), with 0 failures, errors, or skipped tests.

# PLE-037 — Typing-First Review and Success Focus

- Added an application-owned primary-experience strategy mode. REVIEW, RELEARNING, and MASTERED
  items with an eligible canonical Typing prompt now instantiate Typing Recall directly as their
  sole pre-reveal experience; the template remains Typing → Answer Reveal → Rating Ready.
- NEW content retains its existing Introduction/rotated-primary behavior, and items without
  Typing retain image, listening, or prompt rotation. Desktop does not auto-click, mutate the
  flow index, or bypass `LearningFlowController`.
- Typing input now requests focus by item/prompt identity and enabled lifecycle, uses a large
  responsive multiline surface with preserved caret/selection/IME/live-diff behavior, and places
  a centered filled Reveal Answer button immediately below it.
- Typing Question presentation suppresses only manual primary-answer audio interaction; the
  canonical audio path remains available to the existing success effect and returns on Manual
  Reveal. Other scene audio interactions are unchanged.
- Exact input presents an input-blocking root overlay with a scrim, success icon, responsive
  canonical English headline, live-region announcement, and lightweight fade/scale animation.
  One rendered frame precedes the existing answer-audio wait and dwell; the established
  rating-ready → GOOD → Next/Completion boundary remains the only completion authority.
- Manual Reveal retains the C3 integrated comparison and Rating Dock. Positional live diff,
  scheduler, FSRS, queue, Session, persistence, review ratings, and frozen capability-design
  artifacts are unchanged.
- Focused application strategy/template verification passed 24 tests; focused Desktop
  flow/input/audio/overlay/comparison verification passed 59 tests. Full
  `clean test --no-daemon --console=plain` passed 543 XML suites / 2,767 tests (root 354 suites /
  1,739 tests; Desktop 189 suites / 1,028 tests), with 0 failures, errors, or skipped tests.

# PLE-036-C3 — Integrated Typing Comparison Header

- Replaced the standalone Manual Reveal comparison block with an optional typed-answer section
  inside the existing canonical Vocabulary identity header. The existing Word renderer remains
  the sole canonical-answer line, followed immediately by the unchanged audio/IPA/POS row.
- Typed and canonical lines now share one responsive answer-header font-size and line-height
  resolution through `StudyTypographyPresentationResolver`; both remain naturally wrapping text
  with no character grid, artificial spacing, or horizontal scrolling.
- Levenshtein replacement spans retain danger/success underline emphasis, inserted text adds
  strike-through, and deletion highlights only the missing canonical segment. Correct typed
  prefixes remain neutral; grouped accessibility operations announce replacement, insertion,
  and missing suffixes without relying on color.
- Integrated rendering requires exact identity between the comparison's correct answer and the
  canonical Word. A mismatch suppresses the comparison without replacing or modifying the
  canonical answer.
- Manual Reveal still keeps the Rating Dock; empty/exact input omits comparison. Positional live
  feedback, atomic rating-ready auto-GOOD, scheduler, FSRS, queue, Session, persistence, Review
  All, and frozen capability-design artifacts are unchanged.
- Focused comparison/header/typography/regression selection passed 63 tests. Full
  `clean test --no-daemon --console=plain` passed 542 XML suites / 2,753 tests (root 353 suites /
  1,733 tests; Desktop 189 suites / 1,020 tests), with 0 failures, errors, or skipped tests.

# PLE-036-C2 — Positional Live Feedback and Rating-Ready Auto-GOOD

- Manual Desktop UAT found two independent defects. Live styling reused Levenshtein operations,
  so `socs` aligned as a missing `k` plus a matching final `s` and painted no typed character.
  Automatic success also called ordinary GOOD while the authoritative workspace was still
  `Question`, which the state machine correctly rejected.
- Live Typing now has a dedicated Unicode code-point positional resolver over the evaluator's
  existing normalized values. Typed characters are neutral only at matching expected positions;
  mismatches and overrun characters are danger spans, while untyped expected suffixes remain
  neutral. Unsafe raw/normalized mappings still fail safely to unchanged text.
- Reveal comparison remains independently backed by the existing Levenshtein evaluator, retaining
  replacement, insertion, and deletion explanation semantics.
- A dedicated Typing success request carries Session/item rotation identity and input revision.
  One ViewModel operation invokes a Facade boundary that validates identity, performs the
  authoritative reveal, synchronizes flow to rating-ready, then dispatches existing GOOD.
  `Question` still cannot Rate directly.
- Compose clears its transient success lock before invoking completion. Duplicate/stale callbacks
  cannot rate a newer item; failure after reveal remains retryable without duplicating a review.
  Manual Reveal/rating, Scheduler, FSRS, queue, and persistence semantics are unchanged.
- Full `clean test --no-daemon --console=plain` passed 542 XML suites / 2,747 tests (root
  354 / 1,734; Desktop 188 / 1,013), with no failures, errors, or skipped tests.

# PLE-036-C1 — Canonical Typing Answer Authority

- Manual Desktop UAT established that Typing Recall was evaluating against every answer text
  block joined by newlines. POS/pronunciation and Vietnamese meaning therefore became part of
  the expected answer, preventing correct prefixes, exact success, audio, GOOD, and continuation.
- `TypingRecallPromptExtractor` now selects only the first non-blank
  `LearningTextRole.PRIMARY_ENGLISH` block in deterministic question-then-answer order. It never
  joins metadata, meaning, examples, instructions, or neutral text, and unsupported content has
  no Typing prompt.
- The existing evaluator, composition-safe 450 ms debounce, audio completion/failure boundary,
  GOOD dispatch, and Session-owned continuation remain unchanged. With the canonical prompt,
  `sock` is visually neutral, `socks` is exact, and `soaks` highlights only its replacement.
- The front side no longer renders realtime incorrect prose. Check Answer strings and UI remain
  removed; Reveal is always available. Manual comparison continues to use only latest raw input
  and the canonical prompt before Word, with natural wrapping and non-color semantics.
- Full `clean test --no-daemon --console=plain` passed 542 XML suites / 2,739 tests (root
  354 / 1,734; Desktop 188 / 1,005), with no failures, errors, or skipped tests.

# PLE-036-B2 — Final Typing Recall UX

- Typing Recall no longer exposes a Check Answer action. Reveal is always available, and Enter
  or IME Done invokes Reveal only while the current committed input is not already exact.
- The existing realtime evaluator remains the sole comparison authority: correct prefixes stay
  neutral, while only entered replacement/insertion spans receive mismatch styling. Exact
  committed input retains the cancellable 450 ms Correct → answer audio → GOOD sequence without
  an extra key, button, or rating action.
- Manual Reveal presents an incorrect draft above Word as two centered, naturally wrapping text
  lines under the small `You typed` caption. Only operation-specific characters are emphasized;
  the original strings retain normal typography without per-character layout or synthetic
  placeholders. Exact input omits the redundant comparison.
- Manual Reveal retains the ordinary Rating Dock. Automatic success continues directly through
  GOOD and never exposes the rating choice for that item.
- Scheduler, FSRS, Planner, queue, Session policy, persistence, and other learning experiences
  are unchanged.
- Full `clean test --no-daemon --console=plain` passed 542 XML suites / 2,733 tests (root
  354 / 1,729; Desktop 188 / 1,004), with no failures, errors, or skipped tests.

# PLE-036-B1 — Realtime Typing Mastery Remediation

- Manual Desktop UAT found that PLE-036 still treated live Typing as submit-driven: exact input
  did not start success, operation-level mismatch styling was not used, and post-Reveal scene
  projection could suppress the saved comparison.
- Typing presentation state now separates raw `TextFieldValue`, live evaluation, explicit
  incorrect feedback, pending/success facts, and item-scoped Reveal evaluation. Every material
  input/composition change reuses `TypingAnswerEvaluator`; exact committed input starts a
  cancellable 450 ms debounce without requiring Check.
- Live styling now maps only evaluator REPLACEMENT/INSERTION operations back to typed code-point
  spans. Correct/incomplete prefixes and exact answers remain neutral; deletion never paints a
  character that has not been typed, and normalization-length mismatches fail safely to unchanged
  editable text.
- Manual Reveal cancels pending success, evaluates the latest raw input, and stores a distinct
  comparison authority. Full Answer reads it independently of the projected post-Reveal scene
  type and renders separate user/expected operation lines before Meaning and Examples.
- Added 11 regression tests for the exact `socks` UAT sequence, localized `soaks` replacement,
  realtime evaluation, selection/composition safety, debounce cancellation, explicit-feedback
  cleanup, manual Reveal precedence, and post-Reveal comparison wiring.
- Full `clean test --no-daemon --console=plain` passed 542 XML suites / 2,728 tests (root
  354 / 1,729; Desktop 188 / 999), with no failures, errors, or skipped tests. Scheduler, FSRS,
  Planner, Queue, Session, persistence, and other Study modes are unchanged.

# PLE-036 — Typing Mastery Completion

- Editable Typing now uses the existing evaluator/diff authority through an identity-mapped
  visual transformation: a genuinely mismatching suffix is styled directly in the text field,
  while an incomplete correct prefix remains neutral and the real field retains caret, selection,
  clipboard, IME, focus, and accessibility ownership.
- Incorrect Check preserves the draft and remains in correction mode with localized nearby
  feedback. Correct Check freezes duplicate submissions, shows localized success, plays answer
  audio once through the existing audio controller, awaits completion/failure, then dispatches
  GOOD through the existing review boundary after a non-blocking dwell.
- The success sequence is keyed to current item/lifecycle state. Item changes, Undo, navigation,
  disposal, and superseding audio cancel it; a late callback cannot rate a newer item. Audio
  absence/failure falls back without blocking the review.
- Explicit Typing Reveal evaluates the current input snapshot even without a prior Check. Non-empty
  answers show original user text, original expected text, and match/replacement/insertion/deletion
  differences before Meaning and Examples on Wide, Medium, and Narrow Full Answer surfaces.
- Scheduler, FSRS, queue, Session, planner, persistence, Review All, and learning semantics are
  unchanged. Full `clean test --no-daemon` passed 542 XML suites / 2,717 tests (root 1,729;
  Desktop 988), with no failures, errors, or skipped tests.

# PLE-035-B3 — Examples Keyboard Disclosure

- Narrow Full Answer Examples now bind an item-scoped Desktop presentation controller: plain
  `E`/`Shift+E` toggles, while `Esc` collapses an expanded disclosure and is a consumed no-op
  when already collapsed.
- The existing Typing focus authority protects editable input before disclosure dispatch.
  Focused Enter/Space is consumed by the disclosure before the unchanged Study rating resolver,
  preventing a simultaneous Good action.
- Localized Material tooltip guidance exposes the E/Esc shortcut on the existing clickable,
  focusable Button-role header. State is keyed by current learning-item identity, so Next, Undo,
  and restart default Narrow Examples to collapsed; Medium/Wide remain expanded.
- No Scheduler, FSRS, queue, Session, persistence, Review All, rating, audio, or navigation
  contract changed.
- Full `clean test`: 546 suites, 2,733 tests (root 1,752; Desktop 981), all passed.

# PLE-035-B2 — Responsive Full Answer Surface

- Full Answer now derives `WIDE`, `MEDIUM`, or `NARROW` presentation from measured available
  content width. Wide gives Translation/Examples a 38/62 row; Medium stacks both full-width;
  Narrow stacks Translation and defaults Examples to an accessible collapsible section.
- The shared hierarchy remains Word → audio/IPA/POS → Image → Translation → Examples → fixed
  Rating Dock. POS is rendered only in the pronunciation header; the compact Translation card
  contains only its audio affordance and wrapping meaning.
- The image consumes its measured responsive height, is centered within its width policy, and
  retains `ContentScale.Fit`. The wider 1040dp answer cap and compact supporting region return
  materially more display area to landscape, portrait, square, and small intrinsic images.
- Typing reveal comparison is composed inside the shared responsive supporting region and never
  collapses with Examples. Scheduler, FSRS, queue, Session, persistence, Undo, ratings, and
  keyboard actions are unchanged.
- Full `clean test`: 546 suites, 2,727 tests (root 1,752; Desktop 975), all passed.

# PLE-035-B1 — Typing Live Diff & Reveal Comparison

- Typing evaluation now produces a pure Unicode code-point alignment with match, replacement,
  insertion, and deletion operations while retaining both normalized comparison values and exact
  original display strings.
- Live Typing feedback marks only the entered correct prefix and erroneous remainder; it may mark
  a missing-character boundary but never exposes the untyped expected suffix.
- An incorrect Check keeps the draft editable and does not reveal. Explicit Reveal continues
  through the existing authoritative answer transition, then adds a localized user/correct-answer
  comparison without replacing the Full Answer or rating pipeline.
- Typing presentation state remains keyed to current Content identity, so stage transition retains
  the comparison while Next/Undo identity changes reset it. Scheduler, queue, Review All, and
  persistence schemas are unchanged.

# PLE-034-B3 — Review All Scope & Settings Preset Fix

- Review All no longer accepts or applies the ordinary `reviewItemsPerSession` limit. Application
  snapshots every eligible unique learned `ContentId` in scope and uses that complete set for
  availability, queue membership, immutable target, retry coverage, Undo, and restart.
- The Learn chooser now describes the full learned scope instead of a capped `N in total`
  projection. Ordinary General Study retains its existing Session policy limits.
- New and Review preset/custom controls now render from their effective numeric setting. Review
  preset and custom edits synchronize the compatibility custom property, persistence writes the
  effective value, and restart cannot restore a conflicting custom display.
- Full `clean test`: 538 suites, 2,687 tests (root 1,724; Desktop 963), all passed.

# PLE-034-B2 — Learn Hub Access & Unique Review Coverage

- Learn/F2 now opens the learning-action chooser even when an active Study Session exists.
  Continue explicitly resumes that Session; Replay and Review All remain independently available.
- Added one application leave boundary for switching practice sources. It closes the prior
  Session, releases its queue/session-local Undo checkpoint, and preserves every committed review
  before the replacement source creates its own Session and queue.
- Review All snapshots unique learned `ContentId` coverage. `StudySession.reviewedContentIds`
  remains numerator/Undo authority; persisted queue attempts may repeat without changing the
  immutable effective coverage target.
- Reused `allowRepeatInSameSession` and the existing persisted queue for deterministic
  session-local priority: Again returns after one intervening attempt, Hard after up to three,
  Good/Easy remain behind unseen work, and reaching full unique coverage discards pending retries.
- Scheduler/FSRS formulas, ordinary review transactions, statistics rating counts, and
  `docs/capability-design/` remain unchanged.
- Full `clean test`: 538 suites, 2,686 tests (root 1,723; Desktop 963), all passed.

# PLE-034 — Learning Hub MVP

- Unified idle Learn and Session Completion around the same four semantic actions: Continue,
  replay latest completed Session, review all learned Content, and return to Library.
- Completion retains its summary and renders all four actions directly in balanced two-column
  rows; Review All no longer requires leaving completion or rediscovering the idle chooser.
- One Desktop dispatcher delegates to the existing Continue, scoped replay, review-all, and
  navigation paths. Application availability remains authoritative; no queue, Scheduler,
  Planner, persistence, or completion-lifecycle behavior moved into Compose.
- Full `clean test`: 537 suites, 2,679 tests (root 1,718; Desktop 961), all passed.

# PLE-033-B3 — Learn Entry & Review Progress

- Replaced the Good rating label's accidental single-line wrapping with an explicit centered
  `[3] Good` plus supporting `Space` line; accessibility and the existing Good action path remain
  unchanged.
- Corrected Review session progress from remaining work to committed Review Content completion
  over the immutable configured target. Undo and restart re-project the persisted session count.
- Added an application-derived Learn chooser for Continue, latest completed-session replay,
  learned-items review, and Library navigation. Desktop does not scan persistence.
- Added scoped latest-completed selection and ordinary review-only learned-item Session creation.
  Durable reviewed MemoryState/ReviewEvent evidence, enabled content, deterministic due/oldest
  ordering, Content deduplication, and the configured Review limit define the queue.
- Full `clean test`: 537 suites, 2,676 tests (root 1,718; Desktop 958), all passed.

# PLE-033-B2 — Post-Session Experience

- Added `ReplayCompletedStudySessionUseCase`: the finished predecessor and persisted queue supply
  exact committed-review membership and queue order for a new ordinary review Session.
- A deterministic purpose-specific Session identity reuses sequential/restart-visible acceptance.
  Existing review, Scheduler, completion, and Undo boundaries remain authoritative.
- Session Completed now presents Học tiếp, Ôn lại phiên vừa học, and Back to Library with the
  existing action guard and explicit NoItems/Rejected feedback.
- Good exposes both `3` and `Space`; New and Review targets use existing semantic green and blue.
- Full `clean test`: 540 suites, 2,689 tests (root 1,736; Desktop 953), all passed.

# PLE-033-B1 — Desktop UX Polish

- Fixed the Desktop Custom Review target at its real configuration boundary: valid edits now
  persist immediately, a remembered Custom value survives preset selection and restart, and the
  active value continues through `DesktopRuntimeConfiguration.toSessionPolicy()` to ordinary
  Session planning without changing Planner logic.
- Space now resolves to the existing Good action after Answer reveal while retaining the
  existing front-side reveal, busy/repeat/input-focus guards, rating path, Undo, and shortcuts.
- The Study Answer bitmap is clipped with the existing large-radius design token while retaining
  `ContentScale.Fit` and adaptive sizing.
- New/Review fractions use aligned current/target typography, readable separator spacing, and
  the existing purple metric token for configured targets.
- `.\gradlew.bat clean test` completed `BUILD SUCCESSFUL`: 538 suites and 2,676 tests
  (root 1,728; Desktop 948), with 0 failures, errors, or skipped tests, calculated from XML.

# PLE-032-B1 — Application Continuation Boundary

- Added `ContinueGeneralStudyUseCase` as the first application-owned Continuous Review boundary.
  It validates a canonical finished general predecessor, learner, and package/topic scope before
  coordinating the existing ordinary Session start and queue-planning boundaries.
- Added explicit Accepted, NoWork, and Rejected results. Planner remains authoritative for work
  availability; ordinary Study remains authoritative for Session/queue acceptance and history.
- Derived one deterministic next `SessionId` from the preceding `SessionId`. Sequential and
  restart-visible repeated requests reuse the same accepted Session instead of creating another.
  Current repositories do not provide an atomic concurrent-create guarantee; B1 adds no schema,
  FlowId, or speculative persistence.
- Refactored Desktop manual Continue into a thin adapter. The completed predecessor and retained
  Undo queue evidence are no longer purged before continuation.
- Added application and Desktop coverage for accepted continuation, no-work, invalid/active
  predecessor, learner/scope rejection, repeated invocation, ordinary ownership retention, and
  compatible next-Cycle presentation.
- `.\gradlew.bat clean test` completed `BUILD SUCCESSFUL`: root 1,705 tests and Desktop 942
  tests, total 2,647 passed with 0 failures, errors, or skipped tests, calculated from XML.
- Full Continuous Review Mode remains incomplete. Durable intent and restart continuation are
  deferred to PLE-032-B2.

## PLE-030.4 — Content-Level Learning Progress and Review Context Remediation

- Established `ContentId` as learner-facing progress identity while preserving `LearningItemId`
  for scheduling, review transactions, modes and experience execution.
- Added one application content-learning projection over batch sibling lookup and authoritative
  ReviewEvent order. Any completed sibling makes an unseen technical item REVIEW and supplies
  the previous Content rating without mutating its NEW scheduler state.
- Persisted item-to-content identity in queue schema v3 with backward-compatible v1/v2 reads.
  Policy limits, session counters, Review remaining, Total and rating buckets now count unique
  Content identities.
- Desktop uses the immutable content projection outside Compose. Exact sibling UAT automation
  proves Good underline, Hard rerating, unchanged New/Total, one bucket move and Undo restoration.
- Preserved existing same-session anti-repetition: sibling experiences are not forced into one
  session; Continue Learning may select the unseen sibling as REVIEW.
- Focused content projection/planner/quota/counter/Undo/persistence/restart/header/Desktop
  sibling regressions passed. `.\gradlew.bat clean test --no-daemon` completed
  `BUILD SUCCESSFUL`: root `:test` 349 XML suites / 1,695 tests and Desktop `:desktop:test`
  172 suites / 888 tests; total 521 suites / 2,583 passed, 0 failed, 0 errors, 0 skipped.
  Product Owner Manual UAT later closed this remediation as part of PLE-030 FINAL PASS;
  Continuous Review Mode remains the separate PLE-032 scope.

## PLE-030.3 — Session Classification and Review Cue Remediation

- Preserved the planner's immutable NEW/REVIEW admission classification through policy limiting,
  queue planning, runtime snapshots and schema-v2 queue persistence. Schema-v1 records remain
  readable through the established inference fallback.
- Made the persisted queue origin authoritative for review transactions, exactly-once session
  counters, Undo/restart recovery and header remaining-workload partitioning. Mutable memory or
  review history no longer reclassifies new queues.
- Added an immutable Desktop review context resolved outside Compose. REVIEW items underline
  exactly one rating matching the latest persisted event; NEW and missing-history items show no
  underline. Labels, shortcuts, callbacks, enabled rules, colors and 64dp dimensions are
  unchanged.
- Removed redundant REVIEW/Answer Ready cues from the revealed rating state. Reused the sole
  `StudyVisualLayoutResolver` to reserve the statistics header and rating dock before bounding
  answer media, and removed duplicate inner padding from the answer surface.
- Added planner/counter/Undo, persistence/restart, header-origin, rating-context, accessibility
  and vertical-budget regression coverage. Audit evidence is in
  `docs/reports/PLE-030_3_SESSION_CLASSIFICATION_AUDIT.md`.
- `.\gradlew.bat clean test` completed `BUILD SUCCESSFUL`: root `:test` 348 XML suites / 1,687
  tests and Desktop `:desktop:test` 172 suites / 887 tests; total 520 suites / 2,574 passed,
  0 failed, 0 errors, 0 skipped. Manual visual and interaction UAT remains pending.

## PLE-028D.2 — Study Visual Re-UAT Final Remediation

- Closed the four remaining Study visual re-UAT findings without changing learning behavior:
  Meaning and POS now share one wrapping, vertically centered presentation group; the POS badge
  uses a dedicated readable semantic typography role; rating actions use a dedicated 15sp role;
  and English/Vietnamese Example hover retains its semantic container with tokenized border,
  content, highlight, and icon contrast in Light and Dark themes.
- Passed the already-normalized answer POS from the Study composition root into the Question
  Meaning renderer. Question and revealed Answer now use the same `StudyMeaningPosGroup`;
  no normalization, scene, domain, scheduler, persistence, or audio authority was duplicated.
- Preserved the exact Again/Hard/Good/Easy order, labels, shortcuts, callbacks, enabled rule,
  64dp action height, audio click/loop behavior, responsive/image constraints, and
  `ContentScale.Fit`.
- Added focused regression coverage for typography hierarchy, shared Meaning/POS composition,
  Question POS propagation, Light/Dark Example rest/hover/focus/active-loop semantics, rating
  contracts, and dependency boundaries. Focused selection passed 126 tests.
- `.\gradlew.bat clean test --no-daemon` completed `BUILD SUCCESSFUL`: root `:test` 345 XML
  suites / 1,663 tests and Desktop `:desktop:test` 169 suites / 862 tests; total 514 suites /
  2,525 passed, 0 failed, 0 errors, 0 skipped. Manual final visual re-UAT remains pending.

## PLE-028D.1 — Study Visual UAT Remediation

- Remediated four Product Owner `PASS WITH REMEDIATION` findings without changing Study
  behavior: low-contrast POS badges, missing POS beside Vietnamese meaning, harsh Dark ready
  status/Answer hover contrast, and image-driven vertical overflow at wide/full-screen sizes.
- Root causes were presentation-local: POS reused the legacy muted `NotEvaluated` badge;
  `MeaningCard` did not receive the already-normalized disclosure POS; flow status used the
  legacy primary accent; audio hover changed the identity container without coordinating
  content; and Wide layout expanded images to 680x380dp without a dock/chrome vertical budget.
- Added deterministic Study POS, ready-status, and Answer interaction styles. POS now uses
  semantic accent container/content plus medium border in both themes and appears immediately
  after meaning through a wrapping `FlowRow`; blank/absent POS collapses cleanly.
- Answer hover/press now retains `surfacePrimary` while focus/active-loop borders remain
  semantic, preserving the existing clickable English-loop behavior. Ready status now uses
  subdued `textSecondary`.
- Kept `StudyVisualLayoutResolver` as the sole responsive authority. Standard/Wide images are
  capped at 620x240dp, Compact remains width-adaptive up to 560x260dp, vertical budget reserves
  88dp for the horizontal dock or 144dp for the 2x2 dock, and `ContentScale.Fit` remains intact.
  Scrolling remains the fallback for long content, enlarged typography, or low viewports.
- Focused remediation/layout/theme/hierarchy/audio tests passed. Full
  `clean test --no-daemon` passed 513 XML suites / 2,516 tests (root 345 / 1,663; Desktop
  168 / 853), with 0 failed, 0 errors, and 0 skipped. Manual visual re-UAT remains pending.

## PLE-028D — Study Screen Visual Theme Migration and Contrast Remediation

- Migrated the active Study canvas, answer hierarchy, Meaning/Example/Scheduler surfaces,
  rating dock, and header Pause/Undo actions to `LETheme`, `LESurface`, and `LEButton`.
- Added semantic `ANSWER`, `MEANING`, `EXAMPLE`, `SCHEDULER`, and `RATING_DOCK` surface variants
  plus `QUIET` and four ordered rating button variants. Existing PLE-028C variants remain
  source-compatible.
- Removed direct Material color-scheme authority from the migrated Study composition and bound
  word, IPA, meaning, definition, metadata, shortcut, focus, and rating presentation to semantic
  theme roles. The Study canvas now uses the neutral theme window background.
- Preserved the PLE-027 `StudyVisualLayoutResolver`, image `ContentScale.Fit` constraints,
  rating callbacks/order/enabled rules, keyboard routing, scheduler interval projection, and
  `LearningContentAudioController` ownership/lifecycle.
- Added deterministic light/dark mapping, interaction-state, callback/order, responsive,
  image/audio ownership, and static dependency guards. Focused verification passed; full
  `clean test --no-daemon` passed 512 XML suites / 2,506 tests (root 345 / 1,663; Desktop
  167 / 843), with 0 failed, 0 errors, and 0 skipped.
- Automated implementation is complete. Manual Light/Dark/standard/narrow/minimum-viewport UAT
  remains pending; this does not declare PLE-028 complete.

## PLE-028C — Base Controls and Surface Migration

- **Status**: COMPLETE.
- **Base Boundary**: Added LETheme-only `LESurface` and `LEButton` primitives under
  `ui.designsystem.components.base`, backed by pure semantic state resolvers. Public APIs expose
  variants rather than raw colors, padding, radius, border, elevation, or duration.
- **Variants**: Surfaces support `PRIMARY`, `SECONDARY`, and `ERROR`; buttons support `PRIMARY`,
  `SECONDARY`, and `DESTRUCTIVE`. Only evidence-backed variants were added.
- **State Contract**: Resting, hover, pressed, focused, disabled, and loading-disable behavior;
  token 2dp focus without layout padding; Comfort, Compact, and Touch minimum targets.
- **Controlled Migration**: `DesktopLoadStateCard` (Dashboard, Statistics, Review History) and
  `SearchScopeCard` (Lesson Browser, Review History) now use `LESurface`; retry uses `LEButton`.
  Labels, callback identity, enabled authority, live regions, and semantics are preserved.
- **Compatibility Plan**: Existing Studio/Study controls remain explicit legacy compatibility
  consumers. Study answer/rating, broad Library/Dashboard, Settings, dialogs, browser rows,
  media/audio, and keyboard routing were not migrated.
- **No Visual UAT Claim**: Semantic tokens improve focus/disabled consistency only; this is not
  a Study redesign or Manual UAT pass.
- **Verification**: Focused selection passed 60 tests. `.\gradlew.bat clean test --no-daemon`
  completed `BUILD SUCCESSFUL`: root `:test` 345 suites / 1,663 tests; `:desktop:test` 166
  suites / 833 tests; total **511 XML suites / 2,496 passed, 0 failed, 0 errors, 0 skipped**.
  `git diff --check` passed.

## PLE-028B — Design System Core Token Architecture & Theme Engine Foundation

- **Status**: COMPLETE.
- **Architecture**: `LETheme` is the only component-facing token façade. Internal
  CompositionLocals and `ResolvedLETheme` carry one deterministic resolution from the Desktop
  preference boundary through semantic colors, typography, spacing, shapes, motion, elevation,
  icons, density, borders, and the compatibility Material adapter.
- **Completion Audit**: All token groups are immutable and contain no mutable state, viewport
  logic, animation execution, callbacks, screen/domain/scheduler/persistence dependencies, or
  direct `MaterialTheme` reads. The unused pre-token `Color.kt` constants were removed after
  repository-wide reference analysis. PLE-028A `surfaceSecondary` is the elevated-surface role;
  selection states intentionally compose the existing accent/state tokens rather than add a
  duplicate color alias.
- **Compatibility**: `LearningTheme` retains its public signature and delegates exactly once to
  `LearningEngineTheme`. Existing Material light/dark palettes, `LearningTypography`, and
  `LearningShapes` remain unchanged. No Study, Settings, Library, or Dashboard screen migrated,
  and no visual UAT is claimed.
- **Durable Gate**: Theme tests cover deterministic light/dark/system resolution, every token
  group/default, semantic color and typography completeness, exact scales, density and icons,
  Material palette regression, source dependency guards, single authority, and bridge wiring.
- **Test Discovery Audit**: The full Gradle `test` lifecycle executes root `:test` and
  `:desktop:test`. The previously written 2,504 figure had no matching XML evidence and was
  corrected during PLE-028B.1. Git evidence shows no test deletion between PLE-028A and
  PLE-028B.1; the verified PLE-028B baseline was 510 XML suites / 2,484 tests, before the
  completion tests added here. The completion gate retained all 510 suites and added four
  discovered theme cases: `:test` ran 345 suites / 1,663 tests and `:desktop:test` ran 165
  suites / 825 tests.
- **Verification**: Focused theme gate passed 19 tests from 2 XML suites.
  `.\gradlew.bat clean test --no-daemon` completed `BUILD SUCCESSFUL` with **510 XML suites /
  2,488 tests passed, 0 failed, 0 errors, 0 skipped**. `git diff --check` passed.

## PLE-028B.1 — Theme Engine Integration Remediation

- **Status**: COMPLETE; PLE-028B remains in progress.
- **Authority Remediation**: Removed the duplicate `resolveDarkTheme` and `LearningTheme`
  implementations. `LearningEngineTheme` is the single theme entry point,
  `ThemeResolver.kt` exclusively owns preference resolution, token selection, CompositionLocal
  provisioning, and Material adaptation, while the established `LearningTheme` API remains a
  logic-free compatibility adapter for the Desktop composition root.
- **Pure Kotlin Theme Engine Architecture**: Built the complete Kotlin design system architecture under `vn.loi.learning.desktop.ui.theme` implementing the 21-chapter Design Constitution established in `PLE-028A`:
  - `LEColors` & `LEColorTokens`: Immutable semantic color tokens (`windowBackground`, `surfacePrimary`, `surfaceSecondary`, `surfaceMeaning`, `surfaceExample`, `surfaceScheduler`, `surfaceToolbar`, `borderSubtle`, `borderMedium`, `borderFocus`, `textPrimary`, `textSecondary`, `textMuted`, `textDisabled`, `accentPrimary`, `accentHover`, `accentSoft`, `danger`, `warning`, `success`, `info`, `stageNew`, `stageLearning`, `stageReview`, `stageMastered`) with pre-defined `LightLEColors` (pure white canvas) and `DarkLEColors` (WCAG AAA contrast remediation for IPA, meanings, examples and progress).
  - `LETypography` & `LETypographyTokens`: Typographic role tokens (`displayWord`, `headlinePane`, `sectionTitle`, `meaningPrimary`, `bodyDefinition`, `exampleEnglish`, `exampleVietnamese`, `metadataIpa`, `metadataPos`, `schedulerRatingLabel`, `schedulerIntervalHint`, `shortcutBadge`, `fieldLabel`, `fieldValue`, `fieldValueEmphasized`, `secondaryMetadata`, `caption`, `statusText`) dynamically bound via `createLETypography(colors)`.
  - `LESpacingTokens`: Geometric 4dp/8dp grid tokens (`space0` to `space9`) with semantic aliases.
  - `LEShapesTokens`: Corner radius scale tokens (`radiusNone`, `radiusXS`, `radiusS`, `radiusM`, `radiusL`, `radiusXL`, `radius2XL`, `radiusPill`, `radiusCircle`).
  - `LEMotionTokens`: Desktop micro-interaction duration scale (0ms to 400ms) and standard cubic bezier easing curves (`easingStandard`, `easingDecelerate`, `easingAccelerate`).
  - `LEElevationTokens` & `LEBorderTokens`: 5-level Z-index depth layering tokens (`elevation0` to `elevation4`) and dynamic border stroke factory.
  - `LEIconsTokens`: Standard vector icon tokens covering primary, secondary, metadata, status, learning, and scheduler actions.
  - `LEDensityTokens`: Interactive density tokens for `COMFORT` (1.0x), `COMPACT` (0.75x), and `TOUCH` (1.25x) modes.
  - `LETheme`: Single entry point accessing all design system tokens via CompositionLocals (`LETheme.colors`, `LETheme.typography`, `LETheme.spacing`, `LETheme.shapes`, `LETheme.motion`, `LETheme.elevation`, `LETheme.icons`, `LETheme.density`, `LETheme.borders`).
  - `ThemeResolver`: Internal deterministic resolver and `LearningEngineTheme` adapter bridging
    semantic LE tokens with the compatibility Material boundary.
- **Zero Production Visual Regressions**: The Material compatibility adapter preserves the exact
  pre-remediation light/dark palettes, typography, and shapes used by existing UI screens;
  no screen or component was migrated.
- **Comprehensive Unit Test Coverage**: `LEThemeEngineTest` verifies theme resolution,
  light/dark contrast values, dynamic typography color binding, spacing, shapes, motion,
  elevation, density, borders, Material mapping, and the unchanged production Material palette.
- **Verification**: `.\gradlew.bat clean test` — BUILD SUCCESSFUL (**2,484 passed, 0 failed,
  0 errors, 0 skipped**, calculated from 510 XML suites).

## PLE-027C — Representative Desktop UAT & Closure

- **Status**: TECHNICAL UAT COMPLETE (Awaiting Product Owner Final Desktop UAT).
- **Representative Viewport Matrix**:
  - **Compact (480x720, 599x800)**: Verified responsive word identity (36sp), stacked metadata arrangement, 2x2 grid rating dock (`Again` & `Hard` row 1, `Good` & `Easy` row 2), vertical scroll access, zero horizontal overflow.
  - **Standard (1024x768, 1280x800)**: Verified balanced visual hierarchy, inline metadata arrangement, horizontal 4-button rating dock, max content width bounded at 680dp.
  - **Wide (1600x900, Fullscreen)**: Verified centered layout bounded at 800dp max readable line length, image max bounds constrained to 680x380dp without over-stretching.
- **Content Variants Verified (15 Variants)**:
  1. Full content (Word, IPA, POS, Image, Meaning, Definition, Example, Audio)
  2. No image (Collapses cleanly without blank spacer)
  3. No IPA (Clean IPA collapse without slash artifacts)
  4. No POS (Clean badge collapse without status artifact)
  5. No IPA & POS (Clean early return for metadata row)
  6. Long word (Wraps safely without clipping)
  7. Long IPA (Wraps cleanly without overlap)
  8. Long meaning (Multiline wrap with high-contrast text)
  9. Long definition (Separate row underneath meaning card)
  10. Long English example (Soft wrap with red bold target highlight)
  11. Long Vietnamese translation (Normal font weight underneath English row)
  12. Multiple examples (Preserves original deterministic order)
  13. Missing example translation (English row renders cleanly)
  14. Missing audio roles (Hides audio icon without blank spacer)
  15. Expanded scheduler feedback (Compact summary + expandable details)
- **Interaction & Regression Matrix**: Verified Space reveal, primary word loop, `[R]` replay, meaning audio one-shot, English example loop, Vietnamese example one-shot, `1-4` rating shortcuts, `Ctrl+Z` Undo, `Esc` Pause, zero answer leakage, and theme contrast.
- **Verification**: `.\gradlew.bat clean test --no-daemon` — 2,494 passed, 0 failed, 0 errors, 0 skipped; `git diff --check` clean.

## PLE-027B — Answer Surface Visual Hierarchy & Responsive Content Polish

- **Status**: COMPLETE (Product Owner Manual Desktop UAT: Pending).
- **Visual Hierarchy Polish**: Enhanced Study Answer Surface visual hierarchy to match semantic prominence: Word Identity (primary visual anchor) > Meaning Card & Image Viewport > Example Card > Pronunciation Metadata Group > Compact Scheduler Feedback & Secondary Controls.
- **Pronunciation & Metadata Grouping**: Unified audio control, italic IPA, and part of speech status badge into a clean metadata group supporting `INLINE` and `STACKED` arrangements. Collapses cleanly without leftover spacers or dividers when missing optional fields.
- **Meaning & Example Cards**: Formatted Vietnamese meaning as bold primary explanation with optional English definition on a separate row underneath. Formatted bilingual examples with distinct English/Vietnamese typography, target highlighting (`SpanStyle(color = LEColors.danger, fontWeight = FontWeight.Bold)`), and clean collapse for missing translations/audio.
- **Preserved Semantics**: All FSRS math, scheduler rules, review transactions, Undo, Full Answer disclosure, audio truth mode, and `contentPresentationStage` badge behavior remain strictly untouched.
- **Verification**: `.\gradlew.bat clean test --no-daemon` — 2,494 passed, 0 failed, 0 errors, 0 skipped; `git diff --check` clean.

## PLE-027A — Responsive Study Visual Layout Contract

- **Status**: COMPLETE.
- **Pure Responsive Visual Layout Resolver**: Established `StudyVisualLayoutResolver` and immutable `StudyVisualLayout` contract in pure Kotlin without Compose imports or side effects. Deterministically classifies viewports into `COMPACT` (< 600dp), `STANDARD` (600 - 1023dp), and `WIDE` (>= 1024dp).
- **Single Responsive Rating Authority Remediation**: Removed `maxWidth < 480.dp` and inner `BoxWithConstraints` from `ActionDock`/`StudyScreen`, centralizing `RatingArrangement` (`HORIZONTAL` vs `GRID_2X2`) exclusively inside `StudyVisualLayoutResolver` at `RATING_GRID_MAX_WIDTH_DP = 479`. `ActionDock` receives immutable `ratingArrangement` directly from resolved layout.
- **Responsive Layout Authority**: Bounded maximum content width at wide viewports (800dp) with centered alignment. Calculated responsive identity typography size and line height. Scaled max image bounds conservatively for short viewport heights (< 600dp).
- **Metadata & Rating Arrangement**: Provided `MetadataArrangement` (`INLINE` vs `STACKED`) for IPA/POS metadata, and `RatingArrangement` (`HORIZONTAL` vs `GRID_2X2`) for rating buttons to eliminate horizontal overflow and preserve 100% reachability on narrow windows.
- **Preserved Semantics**: All scheduler math, FSRS, review transactions, content disclosure (`FullAnswerPresentation`), audio loop/truth mode, example target highlighting, and `contentPresentationStage` badge behavior remain untouched.
- **Verification**: `.\gradlew.bat clean test --no-daemon` — 2,469 passed, 0 failed, 0 errors, 0 skipped; `git diff --check` clean.

## PLE-026 COMPLETE — Adaptive Study Presentation

- **Status**: COMPLETE (Desktop Manual UAT: PASS).
- **Adaptive Study Presentation**: Delivered Adaptive, Preference Guided, and Manual presentation policies with persisted user preferences and quick controls.
- **Full Answer Disclosure & Audio Standard**: Guaranteed complete Answer disclosure on reveal regardless of Question preference switches. Standardized primary English audio once on reveal with explicit truth-mode autoplay and replay interactions.
- **Semantic Target Highlighting & Infinitive Normalization**: Added exact word-boundary semantic target highlighting in examples for English and Vietnamese with red bold emphasis (`LEColors.danger` + `FontWeight.Bold`), punctuation tolerance, multi-word matching, common inflections, and canonical infinitive verb target normalization (`"to + verb"` -> `"verb"`, e.g. `"to sign"` -> `"sign"`).
- **Content-Level Study Stage Badge**: Projected the learner-facing stage badge on `StudyScreen` from Content-level learning history via `ContentStageQueryService` (`contentPresentationStage`), resolving stage across multiple `LearningItem` modes for a single `Content`.
- **Explicit Architecture Separation**: `learningStage` (authoritative `LearningItem` stage) remains dedicated to scheduler/FSRS, review history, queue planning, and diagnostics, while `contentPresentationStage` is dedicated to the learner-facing Study Badge.
- **Verification**: `.\gradlew.bat clean test --no-daemon` — 2,447 passed, 0 failed, 0 errors; `git diff --check` clean.

## PLE-026-R5 — Adaptive Question Integrity, Full Answer Audio Truth Mode & Stage Diagnostics

- Replaced the global Adaptive show-all baseline with a fail-closed Question recommendation
  derived from the current `LearningExperiencePlan` and selected experience. Primary English
  text and audio now have separate permissions, so Listening keeps its typed audio without
  exposing identity, while Image exposes no answer text/audio.
- Removed answer/meaning appends from primary Question block sanitization. Vietnamese meaning
  remains available only as semantic optional Question support for Manual/Guided resolution;
  Adaptive filtering removes it from rendering, accessibility, and autoplay availability when
  the experience rejects it. Examples remain Answer-only.
- Added `FullAnswerAudioPresentation` over the complete current-item presentation. Reveal uses
  primary English once independently of Question switches; recovery, recomposition, resize,
  Apply, stale items, and missing-primary cases remain silent without cross-role fallback.
  Existing primary/example loop, Vietnamese once, and replay-primary interactions are preserved.
- Made `MemoryState.stage` authoritative for `NextLearningItem.isNew` and the Desktop badge.
  Production mapping now preserves explicit persisted-state existence, and typed diagnostics
  expose stable item/content identity, stage, review count, last review, and queue-selection
  reason without learner content.
- Preserved Learning Strategy/Product Brain decisions, scheduler/FSRS math, review semantics,
  queue policy, persistence schema, runtime preferences, Quick Controls, shortcuts, typography,
  package progress, and example-highlight matching.

## PLE-026-R4 — Full Answer Disclosure & Semantic Example Highlighting

- Separated revealed Answer disclosure from Question visibility: `EffectiveStudyPresentation`
  continues to filter Question blocks, while `FullAnswerPresentation` exposes every available
  word, IPA, part of speech, image, meaning, definition, and example field.
- Removed Question visibility switches from `FocusedAnswerSurface` and its example rows, so
  Manual EN-off/VI-on, Manual EN-on/VI-off, Adaptive, and Preference Guided all reveal the same
  complete answer.
- Added exact presentation-only example annotation. English target matching is case-insensitive;
  English and Vietnamese matching support multiple exact word-boundary and multi-word matches,
  with no fuzzy fallback or canonical-content mutation.
- Preserved Study presentation policy, semantic roles, scheduler/FSRS, Product Brain, queue,
  persistence, shortcuts, typography, package progress, Quick Controls, and all autoplay/manual
  audio/loop/replay behavior.
- Added focused full-answer and target-matching tests plus existing Question, zero-leakage,
  audio, Adaptive, Guided, and Manual regression coverage.
- Verification: `.\gradlew.bat clean test --no-daemon` — 2,398 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean.

## PLE-026-R3 — Semantic Text Roles for Unified Question Presentation

- Added required immutable semantic roles to application learning text blocks and Desktop
  presented text blocks: primary English, Vietnamese meaning, English/Vietnamese examples,
  instruction, and neutral text. No nullable or fallback role was introduced.
- Assigned roles from canonical content slots in `LearningContentProjector` and mapped them
  explicitly at the Desktop presenter boundary.
- Made Study use the complete available semantic presentation for scene projection while
  preserving workspace-controlled Question/Answer rendering.
- Removed language inference from `LearningSceneRenderer`; text visibility now maps directly
  from `PresentedTextRole` to `EffectiveStudyPresentation`.
- Fixed `LISTENING_RECALL` to consume sanitized semantic Question blocks, including available
  Vietnamese meaning/audio, rather than bypassing projection with raw Question blocks.
- Added role projection, Manual EN/VI inverse visibility, instruction/neutral accessibility,
  Listening Question, Image no-duplication, Answer consistency, and existing audio/Adaptive
  regression coverage.
- Verification: `.\gradlew.bat clean test --no-daemon` — 2,390 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean.

## PLE-026-R2 — Presentation Policy vs Workspace Phase Separation

- Removed reveal/workspace phase from `StudyPresentationAvailability` and
  `StudyPresentationPolicy`; effective language visibility and autoplay eligibility now depend
  only on mode, preferences, recommendation, content availability, and audio availability.
- Made Question and Answer rendering own phase-specific layer selection. Question scene blocks
  filter semantic English/Vietnamese audio and supporting text through the same effective
  presentation that governs the focused answer after reveal.
- Moved transition-specific playback into `StudyAutoplayCoordinator`: Adaptive retains its
  established silent Question/English-on-Reveal baseline, while Manual can autoplay an available
  visible Vietnamese meaning on the Question transition.
- Added phase-independent policy, Question/Answer scene filtering, Manual Vietnamese Question
  autoplay, Adaptive regression, and duplicate-transition coverage.
- Verification: `.\gradlew.bat clean test --no-daemon` — 2,386 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean.

## PLE-026-R1 — Study Presentation Consistency & Quick Controls

- Fixed the revealed vocabulary answer to consume `showPrimaryEnglish` from the same effective
  presentation policy as its meaning and example layers. Manual/Preference Guided English-off
  now removes the word, IPA, POS, and their interactive audio target instead of returning to the
  raw answer model after reveal.
- Added a keyboard-operable Study-header presentation menu for mode, English/Vietnamese
  visibility, autoplay preferences, and navigation to full Settings.
- Added one transient item-scoped staging state above the Study/Settings navigation branches.
  Quick Controls and Settings still persist through the same runtime-configuration callback,
  while the current item keeps its presentation and active loop until item advance.
- Header status reports the persisted next-item mode and EN/VI choices and marks pending changes.
- Added policy and staging coverage for answer visibility, shared persistence reconciliation,
  pending/current separation, item advance, and header status.
- Verification: `.\gradlew.bat clean test --no-daemon` — 2,380 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean.

## PLE-026 — Adaptive Study Presentation Preferences

- Added backward-compatible Desktop preferences for Adaptive, Preference Guided, and Manual
  presentation control plus English/Vietnamese visibility and autoplay choices.
- Added a Settings draft/preview/Apply flow; Adaptive explains Product Brain authority and
  disables subordinate switches, while preview never plays audio.
- Added one pure presentation policy that intersects learner preferences, available content,
  and the existing adaptive recommendation. Primary English remains visible as a safety rule;
  unrevealed, unavailable, or hidden support remains silent.
- Routed effective visibility through the focused answer surface and autoplay through a
  transition coordinator. Applying preferences does not replay the current item, and a loop is
  stopped when its content becomes hidden.
- Added legacy/round-trip/invalid persistence, Settings mapping, all-mode policy, unavailable
  content, duplicate transition, item transition, and hidden-loop coverage.
- Verification: `.\gradlew.bat clean test --no-daemon` — 2,376 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean.

## PLE-025B — Study Shortcut Manager

- Added Compose-independent Desktop key chords, Study commands, bindings, immutable registry,
  deterministic serialization, default restoration, and duplicate conflict detection.
- Persisted the complete Study registry in typed Desktop runtime configuration. Missing legacy
  properties and malformed/incomplete/duplicate values fall back atomically to the default
  Space, 1–4, R, Ctrl+Z, and Escape mapping.
- Routed physical keys through one Compose adapter into the injected registry before existing
  workspace-state permission checks; learning-flow, scheduler, audio, and review semantics are
  unchanged.
- Added a Settings table with Change/Reset/Restore Defaults, key-capture preview, Save/Cancel,
  and conflict Cancel/Swap/Replace paths.
- Made the fixed Study status strip render current registry bindings instead of hardcoded keys.
- Added focused serialization, legacy/round-trip/fallback, defaults, conflict, swap, replace,
  restoration, keyboard-routing, status-presentation, and existing-flow regression coverage.
- Verification: `.\gradlew.bat clean test --no-daemon` — 2,365 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean.

## PLE-025A — Study Typography Preferences

- Added validated English-example (20sp default, 16–30) and Vietnamese-example (16sp default,
  14–26) typography preferences to the backward-compatible Desktop runtime configuration.
- Added a Settings section with bounded steppers, a bilingual live preview, and explicit Apply;
  the persisted configuration reaches an open Study screen without restarting the application.
- Replaced fixed example sizes with one presentation resolver: configured sizes are the minimum
  at fullscreen and narrow widths, text wraps, line heights resolve near 1.30×/1.35×, and
  English/Vietnamese retain SemiBold/Normal weight.
- Added runtime legacy/round-trip/range coverage plus preview, mapping, fullscreen, narrow-width,
  wrapping, and invalid-input presentation coverage.
- Verification: `.\gradlew.bat clean test --no-daemon` — 2,361 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean.

## PLE-024-R1 — Example Surface & Authoritative Audio Wiring

- Preserved the five authoritative `ContentMedia` slots through learner-content projection by
  adding typed primary-word, meaning-translation, example-primary, and example-translation
  audio roles; Desktop no longer routes playback from localized labels or guessed ordinals.
- Resolved each focused-answer audio path from its typed role and conservatively paired legacy
  two-line examples only when the separate translated-example audio slot proves bilingual
  semantics. No persistence, OPD3 schema, reset, or reimport is required.
- Rebuilt meaning and example presentation as distinct interactive surfaces with leading audio
  affordances, separate EN/VI rows, and a visible border/tint only on the active English loop.
- Normalized common part-of-speech aliases (`N`, `n.`, `V`, `adj`, and related forms) while
  preserving unknown valid values in uppercase.
- Loop-delay setting changes now reschedule an already pending repeat; item/scene changes,
  completion, pause, and Study disposal continue to stop controller-owned playback.
- Real-package UAT verified that `Vocabulary_In_Use_Elementary` contains all four audio slots,
  and confirmed meaning/EN/VI affordances plus active English-example loop styling.

## PLE-024 — Approved Study Answer Surface

- Reworked the revealed vocabulary answer into the approved identity → pronunciation/audio/POS
  → image → meaning → examples → scheduler-feedback hierarchy while retaining the existing
  scene projection and `LearningContentAudioController`.
- Normalized combined legacy pronunciation text for presentation only, including extraction of
  part of speech and canonical IPA slashes without rewriting OPD3 or persisted content.
- Enlarged adaptive imagery and answer typography, added semantic meaning/feedback surfaces,
  and replaced thin review controls with a fixed, equal-width Again/Hard/Good/Easy rating dock.
- Scheduler intervals now use human-readable Vietnamese minutes, hours, days, and weeks; the
  underlying scheduler outcome and review transaction are unchanged.
- Manual UAT with `Vocabulary_In_Use_Elementary` confirmed the full revealed answer and fixed
  rating dock fit in the standard desktop viewport, with GOOD feedback rendered as `2 ngày`.

## PLE-023-R1 — Visual Parity & Package Latest Ratings

- Added package-scoped latest-rating distribution from the durable append-only
  `ReviewEventRepository`: each current package item contributes only its newest authoritative
  review, while unrated, removed, and other-package items are excluded.
- Library background refresh loads review history once per learner and projects Again, Hard,
  Good, and Easy chips without N+1 item queries or UI-thread repository access.
- Raised the approved visual hierarchy with a 52dp package icon, 21sp title, balanced 88dp
  six-metric row, 23sp values, lavender 10dp progress surface, semantic rating pills, and a
  modern primary/secondary/danger action toolbar.
- Existing ReviewEvent persistence already contains rating, chronology, learner, and item
  ownership, so no schema migration or legacy reset is required.

## PLE-021E-R1 — Continue General Study after Completion

- General-study completion now exposes **Học tiếp** without requiring a lesson `contentId`;
  lesson completion retains its existing Back to Lesson and content-scoped Continue Learning
  actions.
- Continuing removes the completed general session and queue from restore/Undo eligibility,
  then creates a new UUID-backed session from the canonical ACTIVE package/topic and the current
  durable memory state without navigating through Library.
- A new session with no eligible queue candidates is removed immediately and returns a clear
  idle/no-items state, preventing zero-item completion loops.
- The learning-card badge now maps directly from `StudyUiState.learningStage` for NEW, LEARNING,
  REVIEW, RELEARNING, MASTERED, and SUSPENDED instead of inferring NEW/REVIEW from message text.
- Added projection, stage-label, general continuation, durable-memory, next-NEW-item,
  scheduler-transition, completion-restore, no-candidate, and lesson-regression coverage.
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 2m 2s; XML-verified
  **2,331 passed, 0 failed, 0 errors, 0 skipped**. `git diff --check` clean.

## PLE-021C-R2 — Bulk Learning-Item Removal

- Added `LearningItemRepository.deleteAllById` and efficient in-memory/store-backed
  implementations while preserving the existing single-item API.
- `PackageUninstallOperation` now deletes the exact ownership-plan learning-item IDs with one
  bulk repository call instead of issuing one full JSON load/filter/save cycle per item.
- Store-backed bulk deletion returns immediately for an empty set, loads once, filters once,
  saves at most once, and skips the save when none of the requested IDs exist.
- Added repository-contract and persistence-call-count coverage plus a 990-content/4,950-item
  uninstall regression proving one bulk mutation, zero repeated single-item deletes, and
  preservation of unrelated learning items. Existing shared-content and persisted-restart
  uninstall coverage remains authoritative for ownership isolation.
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 2m 48s; XML-verified
  **2,325 passed, 0 failed, 0 errors, 0 skipped**. `git diff --check` clean.

## UAT Remediation — Async Topic Removal and Legacy Completion Ownership

- Topic removal now enters an immediate Desktop `Loading` state and executes both the uninstall
  mutation and the post-uninstall persistence reload through `DesktopTaskRunner`. The guard
  suppresses repeated clicks; the success callback only resets Library navigation, publishes
  the already-reloaded state and removal message, and invokes the existing content-data
  callback. Failure preserves the current presentation, reports the sanitized operation error,
  and always returns to `Idle`.
- `LibraryScreen` closes the confirmation dialog before dispatch and no longer performs the two
  eager `LibraryViewModel`/`ContentLibraryViewModel` refresh calls that previously overlapped the
  uninstall transaction on the Compose UI thread.
- Completed-session lookup now requires `FINISHED`, Undo evidence, `installedPackageId`, and
  `topicId` in both in-memory and store-backed repositories. `StudyFacade` independently scans
  and purges legacy Undo completions missing either ownership field, including their queues,
  before applying the existing ACTIVE package, canonical ID, topic, installation timestamp, and
  queue-item ownership rules.
- Controlled-task-runner regression coverage verifies deferred uninstall execution, busy/double
  click protection, post-completion callback timing, success, and failure recovery. Repository
  contract and Desktop recovery coverage verify both missing ownership variants, valid completion
  restoration, and safe legacy session/queue purge.
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 2m 9s; XML-verified
  **2,320 passed, 0 failed, 0 skipped**. `git diff --check` clean.

## PLE-021B-R10 — Study Lifecycle Reconciliation after Package Reinstall

- `StudyFacade` now restores a package-owned completed session only when the canonical
  `InstalledPackage` is `ACTIVE`, package/topic provenance matches, the session began at or after
  the current `installedAt`, and every queued/completed/current learning item belongs to the
  current package.
- Removed the `ContentPackage` existence fallback from completed-session lifecycle authority.
  Invalid same-package completions have their `StudyQueue` and `StudySession` deleted before
  Study returns Idle; unrelated-package completions are ignored without deletion.
- Added transactional orphan-reimport reconciliation before content registration. For the exact
  repaired package learning-item graph it deletes stale `MemoryState`, `ReviewEvent`,
  `StudyQueue`, and `StudySession` records, including finished sessions retaining one-step Undo,
  while preserving unrelated package state.
- Store-backed production-composition coverage reproduces the deterministic InstalledPackageId
  reinstall defect after four NEW reviews and proves fresh NEW/Discovery startup, no stale
  completion/Undo, no duplicate registration, and unrelated-session preservation. Focused
  coverage preserves legitimate current-installation completion restoration and active/resumable
  recovery.
- **Implementation:** `1cf2157` (`fix: reject stale study completion after package reinstall`).
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 1m 59s; XML-verified
  **2,316 passed, 0 failed, 0 skipped**. Desktop composition smoke startup succeeded with current
  persisted data; native-window interaction was not observable from the execution environment.

## PLE-021B-R9 — Authoritative Import Ownership and Complete Uninstall Graph

- `InstalledContentConflictValidator` now treats an available `InstalledPackageRepository` as
  authoritative even when it is empty or contains only `REMOVED` records. Only `ACTIVE` and
  `ARCHIVED` installed packages contribute live content ownership; the legacy
  `ContentPackageRepository` fallback remains available only when the canonical repository is
  absent.
- Orphan `ContentPackage`, `ContentLibrary`, `Content`, and `LearningItem` records no longer
  produce false installed-ID or fingerprint conflicts. Store-backed OPD3 reimport replaces the
  deterministic orphan records without creating duplicate package or library records.
- `PackageUninstallOperation` now resolves an immutable removal plan before mutation, rejects an
  installed package whose `ContentPackage`/owned `ContentLibrary` graph cannot be resolved, and
  deletes only the exact owned package, library, content, learning-item, session, queue, progress,
  review, catalog, and library-registration identifiers. Shared libraries/content and unrelated
  package state remain preserved.
- Store-backed regression coverage verifies import, study/rating, uninstall, restart, reimport,
  fresh `NEW` memory, duplicate rejection for a live package, orphan repair, unrelated-package
  isolation, and repeated destructive removal with a complete ownership graph.
- **Implementation:** `35321c3` (`fix: repair orphan content ownership across uninstall and
  reimport`).
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 2m 5s; XML-verified
  **2,314 passed, 0 failed, 0 skipped**. `git diff --check` clean.

## PLE-021B-R6 — Destructive Topic Removal and Large-Surface Audio Interaction

- **PART A — Destructive Topic Removal (`b7ecbd5`):**
  - Updated `PackageUninstallOperation` and `UninstallContentPackageUseCase` to permanently delete `MemoryState` records, `ReviewEvent` history, active/resumable `StudySession` records, and owned content/package records upon package removal.
  - Added `deleteByLearningItemIds(learningItemIds)` to `MemoryStateRepository` and `ReviewEventRepository` (and their in-memory / store-backed implementations).
  - Updated `StudySessionRepository.deleteForTopic(learnerId, topicId)` to support topic session cleanup across all learners.
  - Updated Remove Topic confirmation dialog in `LibraryScreen.kt` with exact required Vietnamese copy (`Xóa chủ đề và toàn bộ tiến độ?`, `Chủ đề, nội dung đã cài đặt, lịch sử học và lịch ôn của chủ đề này sẽ bị xóa vễn viễn...`, `Hủy`, `Xóa chủ đề`).
  - Verified package removal lifecycle and fresh `NEW` re-import Discovery state with `DestructiveTopicRemovalIntegrationTest.kt`.
- **PART B — Large-Surface Audio Interaction (`0286c10`):**
  - Updated `FocusedAnswerSurface.kt` to make the full Vocabulary Identity surface (English word, IPA, POS) clickable to play primary answer audio when present, with hover/focus state feedback and accessible description (`Phát âm tiếng Anh: <word>`).
  - Made full `MeaningCard` clickable when Vietnamese meaning audio is present, with accessible description (`Phát nghĩa tiếng Việt: <meaning>`). Not clickable when audio is absent.
  - Split `ExampleCard` into `EnglishExampleAudioRow` and `VietnameseExampleAudioRow`, making each row independently clickable when audio is present with content-specific descriptions (`Phát ví dụ tiếng Anh: <text>`, `Phát bản dịch tiếng Việt: <text>`).
  - Preserved single `LearningContentAudioController` authority and keyboard shortcut `R`.
  - Verified 14 large-surface audio requirements with `LargeSurfaceAudioInteractionTest.kt`.
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 2m 54s (**654 passed, 0 failed**). `:desktop:run` verified. `git diff --check` clean.

## PLE-021B-R3 — Zero Answer Leakage and Topic Progress Reset


- **PART A — Zero Answer Leakage:**
  - Updated `DesktopLearningSceneProjector` (`LearningScene.kt`) to filter out `primaryText` (English headword target answer) from prompt blocks during `IMAGE_RECALL`, `PROMPT_RECALL`, and `TYPING_RECALL` before answer reveal/submit.
  - Updated `LearningExperiencePolicy` so `TYPING_RECALL` requires `canBuildSafePrompt` (presence of image or Vietnamese meaning).
  - Updated `resolveStudyContentAccessibility` (`StudyContentAccessibility.kt`) to conceal target answers in accessibility semantics before reveal.
  - Verified 10 zero-leakage requirements with `ZeroAnswerLeakageTest.kt`.
- **PART B — Explicit Topic Learning Progress Reset:**
  - Implemented `ResetTopicLearningProgressUseCase` resetting `MemoryState` to `NEW` and deleting active/stored `StudySession`s for a selected topic/package without deleting installed assets or affecting other topics.
  - Added `deleteForTopic` to `StudySessionRepository` and its implementations (`InMemoryStudySessionRepository` and `StoreBackedStudySessionRepository`).
  - Added `ResetPackageProgressConfirm` state in `LibraryDialogState` and implemented confirmation dialog in `LibraryDialogs.kt` matching exact specification ("Đặt lại tiến độ học?", "Hủy", "Đặt lại").
  - Added "Đặt lại tiến độ" action button in `PackageListSection.kt` and wired handlers in `LibraryViewModel.kt`.
  - Verified 10 reset requirements with `TopicProgressResetTest.kt`.
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 2m 54s (**642 passed, 0 failed**). `:desktop:run` verified.

## PLE-021B — Adaptive Vocabulary Discovery and Focused Answer Experience

- **PLE-021B.1 — Focused Vocabulary Answer Surface:**
  - Designed `FocusedAnswerSurface` displaying revealed English vocabulary word prominently (`headlineLarge` / `displaySmall`), inline compact audio button + IPA + POS badge row, prominent centered prompt image with adaptive max height (240dp), dedicated `MeaningCard` (Vietnamese primary, optional definition secondary), dedicated `ExampleCard` (English sentence primary, Vietnamese secondary, compact replay button), and `CompactSchedulerFeedback` (collapsed summary by default with `Chi tiết` toggle).
  - Created `FocusedVocabularyAnswerModel` and `FocusedVocabularyAnswerResolver` taking `StudyUiState` and `LearningScene` to deterministically resolve answer attributes.
- **PLE-021B.2 — Discovery Mode for New Vocabulary:**
  - Configured `LearningExperiencePolicy` and `DesktopLearningFlowCoordinator` so genuinely new items (`LearningStage.NEW`) enter Discovery Mode (`DiscoveryFrontSurface`) showing prompt image (if present), Vietnamese meaning cue, and explicit `Xem đáp án` action (Space / Enter).
  - Excluded `TYPING_RECALL` from initial experience options for `LearningStage.NEW` items while keeping typing eligible for `LEARNING`, `REVIEW`, `RELEARNING`, and `MASTERED` items.
  - Ensured English answer, IPA, POS, examples, typing field, and rating dock remain hidden before reveal.
- **PLE-021B.3 — Learning Workspace Visual Polish:**
  - Applied LE Design System tokens (`LEColors`, `LETypography`, `LESpacing`, `LERadius`, `LEElevation`, `LEBorder`) and explicit accessibility semantics across all new composable surfaces.
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 1m 32s (**626 passed, 0 failed**).

## PLE-021A.1 — Active Study Context Header

- **Authoritative Topic Display Title Resolution:** Updated `StudyFacade.kt` to resolve and present the exact Topic display name / package name for active and restored study sessions instead of displaying generic fallback `"All learning items"` when a session belongs to a specific Topic.
- **Title Priority Hierarchy:**
  1. Session Topic display name / Package name (via `InstalledPackageRepository`, `ContentPackageRepository`, `InstalledPackageQueryService`, `TopicQueryService`).
  2. Session Topic name.
  3. Existing collection/session lesson context (`selectedMetadata.lesson` / `selectedContent.displayName`).
  4. Generic fallback (`"All learning items"`).
- **Exact Session Topic Preservation:** Saved sessions preserve their original Topic context during restoration (`restoreResumableSession` / `restoreCompletedSession`), even when the learner subsequently switches the active package in Library.
- **Automated Integration Coverage:** Added `ActiveStudyContextHeaderTest.kt` with 4 focused integration tests covering Topic session title resolution, active Topic switching title updates, saved session Topic preservation during resume, and safe fallback for general study.
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 1m 46s (**624 passed, 0 failed**).

## PLE-021A — Modern Learning Workspace Shell

- **Modular Workspace Hierarchy:** Re-architected `StudyScreen.kt` presentation layer into five cohesive, focused composable layers:
  - `SessionHeader`: Displays session title, progress label, status badges (`Lesson Study` / `Active Session` / `Idle`), progress bar, and active action controls (`Undo [Z]`, `Pause [Esc]`).
  - `LearningWorkspaceSurface`: Main active learning container rendering prompt text, `LearningSceneRenderer`, `TypingRecallInput`, and evaluation feedback with Design System tokens (`LEColors`, `LETypography`, `LESpacing`, `LERadius`, `LEElevation`, `LEBorder`).
  - `SecondaryWorkspace`: Houses secondary cards and information panels (`StudyLoadErrorCard`, `SessionCompletionCard`, `StudyIdleCard`, `SchedulerFeedbackCard`, `DecisionExplanationCard`, metrics).
  - `ActionDock`: Pinned fixed-position action bar at the bottom displaying primary flow actions (Rating buttons `[1-4]`, `Reveal Answer [Space]`, `Next Stage [Space]`, `Start Learning [Space]`).
  - `StatusStrip`: Fixed bottom status bar providing clear shortcut hints (`[1-4] Rate`, `[Space] Reveal/Next`, `[Z] Undo`, `[Esc] Pause`, `[R] Replay`) and live session status badge.
- **Strict Boundary Preservation:** Zero changes to `LearningFlowPlanner`, `ProductBrainPlanner`, `StudyQueue`, `Scheduler`, `FSRS`, `StudyFacade`, `Domain`, `Application`, `Persistence`, keyboard shortcuts, focus transition, typing evaluator, audio player, or undo/resume logic.
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 2m 08s. All existing unit & integration tests passed (**614 passed, 0 failed**).

## PLE-020 — Content Studio Desktop UX Polish & Layout Remediation

- **Native Drag & Drop Media:** Implemented native Drag & Drop for Image Card and Question/Answer/Example/Translation Audio slots from Explorer and Desktop with type validation and friendly rejection without mutating draft.
- **Final 4-Row Desktop Layout:**
  - **ROW 1:** Question (50%) | Answer (50%)
  - **ROW 2:** IPA (50%) | POS (50%) — Compact metadata row with symmetrical ~56dp card height and aligned baselines using `CompactMetadataFieldCard` (`BasicTextField`).
  - **ROW 3:** Example (English) (50%) | Translation (Vietnamese) (50%) — Paired side-by-side equal-width columns with bounded height (max 3 lines) and local text scrolling.
  - **ROW 4:** Hero Image — Positioned immediately after Example/Translation row, fitting entire editor within 1080p viewport without whole-pane scrolling.
- **Adaptive Field Expansion & Dirty Safety:** Empty optional fields collapse into compact `+ Add IPA`, `+ Add Example`, and `+ Add Translation` actions. Revealing an empty field does not mark draft dirty (`isDirty == false`) until text is changed. Single present field expands to 100% width automatically.
- **Full-Resolution Hero Image Renderer (`StudioHeroImage`):** Bypassed 72dp list-thumbnail constraints. Decodes full-resolution Skia `ImageBitmap` asynchronously with aspect ratio preservation (`ContentScale.Fit`), interactive zoom (50%–250%), Fit Width, Fit Height, and Fullscreen preview dialog.
- **Responsive Window Adaptation:** Wrapped editor in `BoxWithConstraints` — wide desktop (`maxWidth >= 600.dp`) uses 2-column paired layout; narrow window (`maxWidth < 600.dp`) stacks fields vertically without text overlap or dirty-state mutations.
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 2m 15s. Total unit/integration tests: **614 passed, 0 failed**. Commits: `d930950`, `48ceda6`, `8c13557`, `0105315`, `67b948a`.

## PLE-019 — Desktop UX Modernization & Design System

- **Desktop Design Token Layer:** Created `desktop/ui/designsystem/` containing `LEColors` (curated neutral & purple palette), `LETypography` (desktop IDE font hierarchy), `LESpacing`, `LERadius`, `LEElevation`, `LEBorder`, and `LEIcons`.
- **Reusable Desktop Components:** Created reusable composable components (`LEPrimaryButton`, `LESecondaryButton`, `LEDangerButton`, `LEIconButton`, `LECard`, `LEInspectorCard`, `LEStatusBadge`, `LEFieldCard`, `LESearchField`, `LEFilterChip`, `LEWaveform`, `LEDragDropTarget`).
- **Toolbar & Header Modernization:** Restyled Content Studio top toolbar (`+ New Item` primary entry point, `Save`, `Discard`, `Delete`, `Keyboard Shortcuts`, `?`, `⚙`) and bottom breadcrumb navigation bar (`Back to Library`).
- **Content Explorer Modernization:** Replaced plain search with icon-assisted `LESearchField`, filter chips (`Only image`, `Only audio`, `Missing media`), rounded item rows with subtle hover & purple selection state, and vector status icons (`LEIcons.Image`, `LEIcons.Audio`).
- **Content Editor Modernization:** Restyled form with `LEFieldCard` containers, POS dropdown selector, and Image Hero banner with zoom and fullscreen controls.
- **Media Manager & Quality/AI Review Panel:** Replaced duplicate audio controls with Media Manager cards (`Replace`, `Preview`, `Remove`, drag-and-drop targets, compact waveforms). Restyled Quality panel with status badges and expandable **AI Suggestions (3)** accordion.
- **Verification:** `.\gradlew.bat test` — BUILD SUCCESSFUL in 1m 12s. Desktop XML-verified: **566 tests passed, 0 failures, 0 errors, 0 skipped**.

## PLE-018C — Production Audio Platform

- **`AudioPlayer` Engine Architecture:** Defined `AudioPlayer` interface (`load`, `play`, `pause`, `stop`, `release`, `positionMs`, `durationMs`, `state`) in `desktop/ui/studio/`. Decoupled `PlaybackCoordinator` so it depends strictly on `AudioPlayer` interface rather than concrete classes.
- **`DesktopAudioPlayer` MP3 & Sound Engine:** Implemented `DesktopAudioPlayer` using JavaSound SPI (`com.googlecode.soundlibs:mp3spi`). Decodes MP3 files (`superfreetts-*.mp3`), WAV, AIFF, and AU into PCM audio streams played via `SourceDataLine` off the Compose UI thread.
- **Production Package Audio Playback:** Verified real audible playback across Question, Answer, Example, and Translation audio for package `Vocabulary_In_Use_Elementary`.
- **Resource Management & Safety:** Immediate line drain/stop/close on stop/finish. Zero thread leaks or unclosed file handles. Graceful error handling (`Unavailable` for missing files, `Cannot play audio` for decoder/device errors). UI established in PLE-018B preserved 100%.
- **Verification:** `.\gradlew.bat test` — BUILD SUCCESSFUL in 1m 18s. Desktop module XML-verified: **563 tests passed, 0 failures, 0 errors, 0 skipped**.

## PLE-018B — Content Studio UI Completion (Media + UX + Audio)

- **Centralized Audio `PlaybackCoordinator`:** Implemented a single `PlaybackCoordinator` in `desktop/ui/studio/` using `javax.sound.sampled` and `ContentMediaStorage` path resolution. Manages audio playback state (`Play`, `Stop`, `Loading`, `Unavailable`, `Error ("Cannot play audio")`). Guarantees single active audio stream (playing a new track stops previous track) and displays "Cannot play audio" on playback or resolution error.
- **Shared Audio Architecture:** `PlaybackCoordinator` is shared across both `ContentEditorPane` and `MediaInspectorPane` (no duplicate playback engines). Audio buttons in both Editor and Inspector cards dynamically reflect live playback states.
- **Explorer Tooltips & Polish:** Added `TooltipArea` on Explorer rows showing item details on hover, status badges (`Img`, `Aud`), responsive layout split (Explorer 22%, Editor 56%, Inspector 22%), top toolbar header with dirty indicator badge, and bottom breadcrumb bar (`Package › Lesson › Content Item`).
- **Ordered Editor Fields & Separated Example/Translation:** Fields ordered strictly: Question -> Answer -> IPA/POS row -> Example (English) -> Translation (Vietnamese) -> Image. Example and Translation remain strictly separated. Full keyboard navigation with `Ctrl+S` (Save) and `ESC` (Discard).
- **Image Viewer & Fullscreen Preview:** Large image container preserving aspect ratio (`ContentScale.Fit`), interactive zoom controls (`-`, `+`, `Reset`, `Fit W`, `Fit H`), and Fullscreen preview dialog.
- **Media Inspector & Waveform Preview:** Audio cards with custom waveform visualizer canvas, image inspector metadata, and truthful Quality Checks ("Not Evaluated" for unverified checks like Duplicate Check or Audio Spectrum Quality).
- **Verification:** `.\gradlew.bat test` — BUILD SUCCESSFUL in 54s. Desktop module XML-verified: **560 tests passed, 0 failures, 0 errors, 0 skipped**.

## PLE-018A — Content Studio Layout Rework

- **Three-Pane Content Studio:** Replaced the 2-pane `PackageContentBrowserCard` (height 540dp fixed) with `ContentStudioScreen` — a full-height three-pane desktop workspace in package `desktop/ui/studio/`:
  - **Content Explorer** (left, 25%): `LazyColumn` + `VerticalScrollbar`, stable `key` per row, search field, lesson/media/sort dropdowns, item count footer, auto-scroll to selection on `LaunchedEffect`.
  - **Content Editor** (center, 50%): sticky toolbar (Save / Discard / Delete active; Duplicate / AI Assistant / History disabled placeholders); scrollable form with Question, Answer, IPA+POS row, **Example (English)** and **Translation (Vietnamese)** as separate fields; large image viewer (`heightIn(min=200.dp, max=350.dp)`) with disabled zoom controls; per-field Play/Stop audio buttons.
  - **Media & Details Inspector** (right, 25%): image thumbnail + reference; four audio rows (Question / Answer / Example / Translation) with Play/Stop; truthful Quality Checks panel (Image, Question Audio, Answer Audio, IPA, Example, Translation — no fabricated results; "Not evaluated" for checks without backing data).
- **Full-Height Takeover:** `LibraryScreenContent` (library dashboard) is suppressed when Content Studio is open; `ContentStudioScreen` receives `Modifier.weight(1f).fillMaxHeight()`.
- **Per-Field Audio Refs:** Added `questionAudioRef`, `answerAudioRef`, `exampleAudioRef`, `translationAudioRef` to `PackageContentBrowserItem` (default null, backward compatible). Populated from `content.media.primaryAudio / translatedAudio / exampleAudio / exampleTranslatedAudio` in `PackageContentBrowserQueryService`.
- **PLE-017A Behavior Fully Preserved:** All existing callbacks wired to `ContentStudioScreen`: edit, save, discard, safe-delete, dirty-guard dialogs, pending actions, search, filter, sort, double-click, Back to Library.
- **Files changed:** `PackageContentBrowserItem.kt`, `PackageContentBrowserQueryService.kt`, `LibraryScreen.kt`, `ContentStudioScreen.kt`, `ContentExplorerPane.kt`, `ContentEditorPane.kt`, `MediaInspectorPane.kt`.
- **Commit:** `1a2a212 feat: redesign Learning Browser into Content Studio`
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 1m 59s. Desktop module XML-verified: **557 tests passed, 0 failures, 0 errors, 0 skipped**.

## PLE-016 — Learning Browser 1.0 & Production Navigation Remediation


- **Production Navigation Route Remediation:** Fixed composition root defect in `LearningShell.kt` where `packageBrowserFacade` was omitted during `ContentLibraryViewModel` instantiation, causing fallback to null query service and rendering of legacy `LessonBrowserCard`. Wired `PackageContentBrowserFacade(queryService = applicationContext.packageBrowserQuery)` directly into production runtime.
- **Production UI State Ownership & Back Navigation:** Configured `LibraryScreen.kt` and `ContentLibraryViewModel.kt` so clicking "Browse Lessons" strictly opens `packageBrowserUiState` and renders `PackageContentBrowserCard`. Clicking "Back to Library" closes `packageBrowserUiState`, restoring the Library Overview while preserving tab and package list order.
- **Adaptive Responsive Layout & Export OPD3 Action Wrapping Fix:** Updated `PackageListSection.kt` using `@OptIn(ExperimentalLayoutApi::class) FlowRow` and `softWrap = false` on action button text so `"Export OPD3"` never wraps character-by-character vertically on narrow screens (~700px). Added `BoxWithConstraints` adaptive layout in `PackageContentBrowserCard.kt` to stack Toolbar, Data Table, and Preview Panel cleanly on narrow windows.
- **Behavioral Integration Coverage:** Added `PackageContentBrowserNavigationIntegrationTest.kt` verifying production navigation route, search "vegetarian" (matching question, answer, IPA, POS), row selection updating preview panel (with image & audio controls), Back to Library state restoration, and narrow window layout adaptation.
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 2m 36s. Total XML-verified tests: **2,164 passed, 0 failed**.

## PLE-014 — Orphan Content Ownership Reconciliation during Package Reimport

- **Canonical Authority Re-alignment (`InstalledPackageRepository`):** Corrected `InstalledContentConflictValidator.kt` to start conflict validation strictly from `InstalledPackageRepository` (filtering `ACTIVE` and `ARCHIVED` packages), resolving canonical `PackageId`s, matching `ContentPackage`s, and live `ContentLibrary` content IDs.
- **Orphan Content Exemption:** Guaranteed that legacy orphaned content (present in `ContentPackage`/`ContentLibrary`/`Content`/`LearningItem` but absent from `InstalledPackageRepository` and `Library`) is recognized as orphan state and does NOT trigger false `CONTENT_ID_ALREADY_INSTALLED` conflicts upon package re-import.
- **State-Aware UI Diagnostics:** Updated `ContentLibraryViewModel.sanitizeFailureMessage()` and `ContentLibraryFacade.getPackageLifecycleState()` to present state-aware diagnostic messages (`State: ACTIVE` / `State: ARCHIVED`), avoiding generic or false assertions when lifecycle state is absent.
- **Automated Integration Coverage:** Added `reconcile orphan content ownership during package reimport and reject active package duplicates` test to `GeneralStudyActivePackageAuthorityIntegrationTest.kt` verifying both legacy orphan re-import success and active package duplicate rejection.
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 1m 36s. Total XML-verified tests: **2,155 passed, 0 failed**.

## PLE-014 — Removed Package Reimport State Reconciliation

- **Infrastructure & Platform Factory Dependency Wiring:** Resolved composition root defect in `LearningApplicationFactory.kt` and `PersistedLearningPlatformFactory.kt` where `installedPackageRepository`, `contentPackageRepository`, and `contentLibraryRepository` were omitted when instantiating `PackageImportService`.
- **Live Owner Conflict Validation:** Updated `InstalledContentConflictValidator.kt` to validate content conflicts against active or archived packages in `InstalledPackageRepository` and live `ContentPackageRepository`/`ContentLibraryRepository`. Orphaned content from previously uninstalled packages does not trigger false `CONTENT_ID_ALREADY_INSTALLED` conflicts during re-import.
- **Learner Progress Preservation:** Guaranteed that `MemoryState` and `ReviewHistory` records are preserved by item identity and reconnected seamlessly when a previously removed package is re-imported.
- **Automated Regression Coverage:** Added `AC-06 - Complete removed package reimport lifecycle with progress reconnection` to `GeneralStudyActivePackageAuthorityIntegrationTest.kt` verifying real application/composition boundary import -> study -> uninstall -> re-import -> progress reconnection flow.
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 2m 30s. Total XML-verified tests: **2,154 passed, 0 failed**.

## PLE-014 — Study Authority & Hidden Package Lifecycle Remediation

- **Same-Runtime Active Package Authority Refresh:** Resolved in-memory caching defect in `StudyFacade.kt` so that when a study session is paused/idle and active package is changed in Library, `load()` and `startStudy()` immediately invalidate stale cached package/session projection and bind to the new canonical active package without requiring application restart.
- **Transactional Package Uninstall & Orphan Clean-up:** Enforced complete transactional cleanup of all `ContentPackage`, `ContentLibrary`, `Content`, and `LearningItem` records in `PackageUninstallOperation.kt` across candidate package ID keys, eliminating orphaned content leaks during package uninstallation.
- **Active/Archived Installed Package Conflict Validation:** Updated `InstalledContentConflictValidator.kt` to scope conflict validation against active or archived packages in `InstalledPackageRepository`, ensuring orphaned content from previously removed packages does not trigger false `CONTENT_ID_ALREADY_INSTALLED` conflicts upon package re-import.
- **Automated Integration Coverage:** Extended `GeneralStudyActivePackageAuthorityIntegrationTest.kt` with tests for same-runtime paused session active package switching, symmetric active session protection, and complete package uninstall + re-import lifecycle (AC-06).
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 2m 39s. Total XML-verified tests: **2,153 passed, 0 failed**.

## PLE-014 — General Study Active Package Authority Remediation

- **Active Package Authority when Idle:** Enforced that when Study is idle, canonical Library `activePackageId` strictly governs General Study. Stale packages, uninstalled packages, and packages no longer in Library are rejected when starting or recovering idle sessions.
- **Resumable Active Session Protection:** Protected active/paused sessions from being overwritten when changing Library active package while Study is active. Active session provenance and topic state remain authoritative.
- **Finished Session Isolation:** Corrected session recovery so that older finished sessions from a previous active package are not resurrected as active completion cards when a newer active session exists or when Study is idle for a different package.
- **Non-Due Package Study Fallback:** Added explicit fallback in `StudyQueuePlanningService` and `GetNextSessionItemUseCase` allowing package and lesson study to present review items even when zero items are currently due.
- **Automated Integration Coverage:** Created `GeneralStudyActivePackageAuthorityIntegrationTest.kt` with comprehensive tests covering idle package authority switching, stale historical package rejection, active session protection, and app restart recovery.
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 1m 24s. Total XML-verified tests: **2,130 passed, 0 failed**.

## Gradle Default Memory Configuration Stabilization

- **Repository-Level JVM Memory Configuration:** Created `gradle.properties` with `org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8`, stabilizing standard `.\gradlew.bat clean test` execution without requiring manual `-D"org.gradle.jvmargs=-Xmx4g"` CLI arguments.
- **Verification:** Verified by executing `.\gradlew.bat --stop` followed by `.\gradlew.bat clean test` across two consecutive clean runs. Both runs succeeded with `BUILD SUCCESSFUL` (Run 1: 2m 38s, Run 2: 2m 25s). Exact XML-verified test result: **2,129 passed, 0 failed**. Commit `39a9b2a`.

## PLE-013 — Topic Selection & Exact Resume

- **Topic Selection & Exact Resume Execution:** Enabled independent multi-topic learning in Desktop Study. Learners can study Topic A to position X, switch to Topic B, and return to Topic A to resume at position X.
- **Single Source of Progress Truth:** Maintained `StudySession`, `MemoryState`, `ReviewHistory`/`ReviewEvent`, and `StudyQueue` as the sole authoritative progress records. No duplicate `TopicResumeState` or `TopicProgressState` models created.
- **Active Session Protection vs Idle Topic Authority:** Upgraded `StudyFacade.kt` so that when Study is idle, selecting a package/topic in Library governs which session is restored via `LearningEngine.recoverTopicSession`. When a session is active in memory, that `StudySession` remains authoritative and protected from silent overwrites.
- **Application Restart Recovery:** Restored topic checkpoint and exact review workspace state (`AnswerRevealed`, `PromptPresented`) after desktop application restart.
- **Automated Integration Coverage:** Created `TopicSelectionExactResumeIntegrationTest.kt` with 6 comprehensive test functions covering all 10+ acceptance cases (Topic A → B → A isolation & exact resume, application restart recovery, idle topic authority vs active session protection, new topic session pipeline vs completed topic lifecycle, `MemoryState` & `ReviewHistory` immutability during switching/resuming, and failure-safety during switching).
- **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL. Exact XML-verified tests: **2,129 passed, 0 failed**. Commit `6c2eaff`.

## Library Integrity Recovery — Final Ownership Remediation

- **Canonical ContentLibrary Ownership:** Derived `ContentLibrary` ownership strictly from canonical `ContentPackage.libraryIds`, eliminating invalid cross-type mapping from `InstalledPackage.libraryId` (`LibraryId`). Added Test 8 verifying that a canonical `LibraryId` matching a `ContentLibraryId` raw string does not cause invalid deletion or preservation.
- **Target Package Resolution Simplification:** Simplified target package resolution in `PackageUninstallOperation` to direct `command.packageId`.
- **Complete Transaction Rollback Assertions:** Extended failure-injection integration test (`LibraryIntegrityLifecycleIntegrationTest.kt` Test 7) proving `JsonFileTransactionRunner` rolls back all 10 domain boundaries (`ContentPackage`, `PackageCatalog`, `InstalledPackage`, canonical `Library`, `ContentLibrary`, `Content`, `LearningItem`, `Collection`, `MemoryState`, `ReviewHistory`) on failure without partial mutation.
- **Verification:** `.\gradlew.bat --no-daemon clean test -D"org.gradle.jvmargs=-Xmx4g"` — BUILD SUCCESSFUL. Exact XML-verified tests: **2,123 passed, 0 failed**. Commit `1119d08`.

## PLE-012 — Rich Lesson Exploration Workspace

- **Multi-stage Lesson Workspace Flow:** Delivered interactive multi-stage lesson workspace (`EXPLORE` mode for item-by-item content browsing with previous/next navigation → `PREPARE` mode for study setup → `STUDY` mode for active lesson execution).
- **Workspace Projection & Facade Integration:** Added `WorkspaceProjectionAssembler`, `LearningWorkspaceUiState` mode transitions, explore item navigation in `LearningWorkspaceCard`, `LessonBrowserFacade.getExploreItemsForLesson`, and comprehensive test suite `LearningWorkspaceExploreModeTest`.
- **Verification:** `.\gradlew.bat --no-daemon clean test -D"org.gradle.jvmargs=-Xmx4g"` — BUILD SUCCESSFUL. Commit `e5e0379`.

## PLE-010 — Library Navigation Recovery

- **Canonical Library Root Recovery:** Added `ContentLibraryViewModel.resetLibraryNavigationState()` and `StudyFacade.dismissCompletionPresentation()` (exposed via `StudyViewModel`). Unified `LearningShell.kt` to invoke the same canonical recovery flow for sidebar "Thư viện" clicks, F5 refresh, and "Back to Library" callbacks.
- **Completion Presentation Separation & Invariants:** Separated completion UI presentation from persisted completion evidence. Dismissing completion presentation clears `sessionCompleted` without deleting or mutating session records, review history, analytics, or scheduler state. Starting a new session resets dismissal state so new completions render normally. Active/paused sessions remain bound to their original package.
- **Automated Integration Coverage:** Created `LibraryNavigationRecoveryIntegrationTest.kt` covering T1-T10 contracts (Lesson Browser recovery, Workspace recovery, completed A to active B projection, active/paused A binding, unified sidebar/F5 reset behavior, domain evidence non-mutation, completion B rendering after dismissal, active session preservation, Idle B projection, and app restart recovery without schema changes).
- **Verification:** `.\gradlew.bat --no-daemon clean test -D"org.gradle.jvmargs=-Xmx4g"` — BUILD SUCCESSFUL in 1m 57s. Total XML-verified tests: **2102 passed, 0 failed**.

## PLE-009R2 — Real UI Package Authority & Navigation Remediation

- **Real UI Study Package Authority (Defect A):** Remediated `StudyFacade.kt` (`restoreLatestUndoableCompletion` and `createIdleUiState`) to reject completed/undoable sessions from other packages when idle. Selecting Package A in Library strictly projects Package A in Study without leaking completed session data from Package B.
- **Trapped Navigation & Focus Recovery (Defect B):** Wired `LearningShell.kt` to reset `contentLibraryViewModel` overlays (`learningWorkspaceUiState` & `lessonBrowserUiState`) when navigating to `CONTENT_LIBRARY` (via sidebar click or F5), restoring root Library package list. Equipped `LearningWorkspaceCard.kt` and `LessonBrowserCard.kt` with `FocusRequester` + `LaunchedEffect` for deterministic keyboard `Key.Escape` navigation.
- **Scope Clarification & Non-Defects:** Confirmed duplicate import atomicity passed UAT without defect. Content Editor and multi-lesson editing remain out-of-scope/future capabilities.
- **Automated Integration Coverage:** Created `RealUiPackageAuthorityNavigationIntegrationTest.kt` verifying shell active package authority switching (Test A), active/paused session binding (Test B), and state-machine navigation/F5 recovery (Test C).
- **Verification:** `.\gradlew.bat --no-daemon test` — BUILD SUCCESSFUL. Commit `PLE-009R2: fix real UI package authority and navigation`.

## PLE-009 — Desktop Package Selection & Navigation Stabilization

- **Active Package Synchronization & Session Safety:** Wired `LibraryViewModel.onLibraryDataChanged` callback in `LearningShell.kt` to trigger state refresh across all shell ViewModels (`dashboardViewModel`, `statisticsViewModel`, `reviewHistoryViewModel`, `studyViewModel`, `contentLibraryViewModel`). Updated `StudyFacade.kt` to resolve canonical active package dynamically when idle while preserving active/paused sessions unchanged without silent rebinding.
- **Browse Lessons Batch Query Optimization:** Added `findByContentIds` set-matching to `LearningItemRepository.kt`, `InMemoryLearningItemRepository.kt`, `StoreBackedLearningItemRepository.kt`, and `LearningEngine.kt`. Updated `PackageLearningProgressQueryService.kt` to batch-query all package learning items in 1 single operation instead of ~400 repeated per-content queries.
- **Import Error Sanitization & Stage Invariants:** Enforced `PackageImportProgressStage.COMPLETED` is never reported when import candidate failures occur in `PackageImportService.kt`. Sanitized duplicate/validation error messages in `ContentLibraryViewModel.kt` into concise, bounded UI summaries while retaining full details in logs. Ensured failed imports leave active package, study, selection, and navigation state untouched.
- **Deterministic Navigation & Visual Loading:** Added `Key.Escape` event handlers to `LessonBrowserCard.kt` (returns to Library package list when search/filters are clear) and `LearningWorkspaceCard.kt` (returns to Lesson Browser). Rendered visible loading banner in `LibraryScreen.kt` when `ContentLibraryOperation.Loading`.
- **Automated Integration & Performance Coverage:** Added `ActivePackageStudySyncIntegrationTest.kt` (verifies active package sync & session safety), `ImportErrorSanitizationTest.kt` (verifies bounded error text, COMPLETED stage invariant, and state non-mutation), and `PackageLearningProgressQueryPerformanceTest.kt` (verifies batch repository query performance on 400 lessons without per-content loops).
- **Verification:** `.\gradlew.bat --no-daemon clean test` — BUILD SUCCESSFUL in 1m 58s. Total XML-verified tests: **2089 passed, 0 failed**.`

## PLE-003-R2 — Persist InstalledPackage Provenance in StudySession

- **Domain Session Provenance (Part A / AC-R2-01, AC-R2-05):** Added `val installedPackageId: InstalledPackageId? = null` directly to `StudySession` aggregate and `StudySession.start(...)`. Provenance is immutable and preserved across all lifecycle transitions (`recordReview`, `reveal`, `undo`, `finish`).
- **Start Session Command Propagation (Part B & C / AC-R2-02 - AC-R2-04):** Propagated `installedPackageId` through `StartStudySessionCommand.kt` and `StartStudySessionUseCase.kt`. `StudyFacade.kt` passes `activeInstalledPackageId` when package validation succeeds, while legacy/general study sessions maintain `installedPackageId == null`.
- **Persistence & Backward Compatibility (Part E / AC-R2-06 - AC-R2-08):** Extended `StudySessionRecord.kt` with `val installedPackageId: String? = null` and updated `StudySessionRecordMapper.kt`. Legacy JSON session records without `installedPackageId` decode safely to `null` without schema migration.
- **Recovery & UI Projection (Part F, G & H / AC-R2-09 - AC-R2-13):** Restored active session recovery sets `activeInstalledPackageId = session.installedPackageId`. `StudyUiState.kt` exposes `activeInstalledPackageId: InstalledPackageId?`. Clearing active study state resets transient context without leaking package provenance across sessions.
- **Automated Integration Coverage:** Updated test suite `PreservePackageContextStudyEntryIntegrationTest.kt` covering domain start provenance (T1), transition immutability (T2), command propagation (T3), mapper round-trip (T4), backward-compatible record load (T5), package-aware desktop session (T6), reveal context retention (T7), review advancement context retention (T8), finish provenance retention & projection clearing (T9), restart recovery (T10), package A to B isolation (T11), and package A to legacy isolation (T12).
- **Verification:** `.\gradlew.bat clean test` — 1,984 tests passed across all modules (385 in desktop module), 0 failures. Commit `fix: persist package provenance in study sessions`.

## PLE-003-R1 — Preserve Package Context Through Study Entry

- **Removed Fake ID Fallback (Part B / AC-R1-01, AC-R1-02):** Removed `InstalledPackageId(uiState.libraryId)` fallback in `LessonBrowserCard.kt`. If `installedPackageId == null`, Start Lesson button is disabled with explicit feedback `"Package context is unavailable."` without fake ID generation, callback invocation, or crashes.
- **Package-Aware Study Request & Navigation (Part C / AC-R1-03, AC-R1-04):** Created `StartPackageLessonStudyRequest.kt` typed request class (`installedPackageId: InstalledPackageId, contentId: ContentId`). `LessonStudyNavigationCoordinator.kt` preserves and forwards `installedPackageId` and `contentId` down to `StudyViewModel.startLessonStudy(request)`.
- **Application Ownership Validation (Part C & E / AC-R1-05 - AC-R1-08, AC-R1-10):** `StudyFacade.kt` validates: (1) Package exists, (2) Package is `ACTIVE` (not `ARCHIVED` or `REMOVED`), (3) Package belongs to default library, (4) Selected lesson content belongs strictly to target package, (5) Lesson contains enabled learning items. Any validation failure sets recoverable `uiState.loadError` without creating an active session or navigating to `STUDY`.
- **Automated Integration Coverage:** Created integration test suite `PreservePackageContextStudyEntryIntegrationTest.kt` in `:desktop` covering coordinator context preservation (T1), missing package context guard (T2), valid package & lesson session creation (T3), cross-package mismatch rejection (T4), archived package rejection (T5), removed package rejection (T6), missing lesson rejection (T7), zero learning items rejection (T8), and package-scoped ID resolution isolation (T9).
- **Verification:** `.\gradlew.bat clean test` — 1,980 tests passed across all modules (381 in desktop module), 0 failures. Commit `fix: preserve package context through lesson study entry`.

## PLE-003 — Lesson Browser Product Completion

- **Package Context Header (Part B / AC-03-01):** Rendered package name, total lesson & learning item counts, `InstalledPackageId` caption, and "Back to Library" action button in `LessonBrowserCard.kt`. Preserved `installedPackageId` in `LessonBrowserUiState.kt`.
- **Hierarchical Presentation (Part C / AC-03-03, AC-03-04):** Implemented `groupLessonsHierarchically(lessons)` rendering Group headers and Section subheaders. Applied consistent fallback labels (`"General"` for blank group, `"Other Lessons"` for blank section) without displaying `"null"` or blank titles.
- **Search & Filter (Part D / AC-03-05 - AC-03-07):** Supported local case-insensitive search across title, primary text, translated text, group, and section. Provided clear search reset. Differentiated Package Empty state (`lessons.isEmpty()`) from Search Empty state (`visibleLessons.isEmpty()`).
- **Lesson Selection (Part E / AC-03-08, AC-03-09):** Single-select lesson on row click with primary container highlight and "Selected" badge. Selection remains preserved in state when query filter hides selected lesson.
- **Start Lesson Flow (Part F & G / AC-03-10 - AC-03-13):** Created `PackageLessonSelection.kt` typed context class. Rendered bottom action bar with Start Lesson button enabled only when a valid lesson in current view is selected AND has `learningItemCount > 0`. Disabled Start Lesson for 0-item lessons with explicit feedback ("No learning items available for this lesson").
- **Navigation & Isolation (Part H & I / AC-03-14 - AC-03-16):** Back button returns to Library overview without mutating package state. Loading a package resets search query, filter, sort, and selection, eliminating stale state leakage between browse sessions.
- **Automated Test Coverage:** Added unit & integration test suite `LessonBrowserProductCompletionTest.kt` in `:desktop` covering package context (T1), hierarchy & fallback labels (T2), search title matching (T3), search translated text matching (T4), search isolation (T5), search empty state (T6), single selection (T7), start disabled without selection (T8), typed start selection (T9), zero-item start guard (T10), back navigation (T11), and stale state reset isolation (T12).
- **Verification:** `.\gradlew.bat clean test` — 1,971 tests passed across all modules (372 in desktop module), 0 failures. Commit `feat: complete lesson browser experience`.

## PLE-002-R1 — Correct Package-Scoped Browsing and Active-Package Lifecycle

- **Browse Lessons Package-Scoped Contract (Issue A / AC-R1-01 - AC-R1-04):**
  - Removed ambiguous `openLibrary` fallback to first library in `ContentLibraryViewModel.kt`.
  - Added typed `browsePackageLessons(installedPackageId: InstalledPackageId, packageName: String)` in `ContentLibraryViewModel.kt` and `LessonBrowserFacade.loadForPackage`.
  - Added `queryForLibraries(libraryIds: Collection<ContentLibraryId>)` to `LibraryContentQueryService.kt`.
  - Resolved `InstalledPackageId` -> `InstalledPackage.packageId` -> `ContentPackage` -> `ContentLibraryId` -> `LibraryContentQueryService.queryForLibraries`.
  - Guaranteed package isolation: Topic A Browse Lessons displays ONLY Topic A lessons; Topic B Browse Lessons displays ONLY Topic B lessons.
  - Non-existent package IDs return clear `loadError` ("Package with id '...' not found.") without fallback or silent failures.
- **Active Package Lifecycle Consistency (Issue B / AC-R1-05 - AC-R1-09):**
  - **Archive Policy:** `LibraryCommandService.archivePackage` atomically sets `library.activePackageId = null` and saves both `updatedLibrary` and `updatedPackage` inside the same transaction when archiving the current active package. Archiving a non-active package leaves `activePackageId` intact.
  - **Restore Policy:** `LibraryCommandService.restorePackage` restores state to `ACTIVE` but does NOT automatically set `activePackageId` (remains `null` or unchanged).
  - **Sanitizing Invalid Legacy References:** `LibraryQueryService.getNavigationTree` sanitizes `activePackageId = library.activePackageId?.takeIf { id -> activePackages.any { it.id == id } }`. Legacy persisted state referencing missing or non-ACTIVE packages evaluates to `null` safely without app crash or displaying "Current Active" on archived packages.
- **Verification:** `.\gradlew.bat clean test` — 1,959 tests passed (360 in desktop module), 0 failures. Commit `fix: correct package scoped browsing and active package lifecycle`.

## PLE-001C-R1 — Restore File-Scoped Import and Installed Topic Removal

- Implemented `JvmFileScopedPackageScanner` in `vn.loi.learning.infrastructure.contentpackaging` supporting single file selection (`.opd3`, `.pkg`, `.json`), resolving same-basename companion pairs (`<base-name>.json` and `<base-name>.pkg`), and throwing `MissingOpd3JsonPairException` without partial persistence when a companion is missing.
- Updated `ContentPackageImportFactory.createScanner` to route regular files to `JvmFileScopedPackageScanner` and directories to `JvmDirectoryPackageScanner`.
- Updated `PackageDirectoryChooser.kt` to `choosePackageFile` using `JFileChooser` set to `FILES_ONLY` with extension filter `*.opd3, *.pkg, *.json`.
- Extended `PackageUninstallOperation` to accept optional `installedPackageRepository`, `libraryRepository`, and `collectionRepository` domain parameters, performing full atomic reconciliation of `InstalledPackage`, `LibraryEntry`, and `Collection` package assignments upon uninstallation.
- Wired `UninstallContentPackageUseCase` into `LearningApplicationContext` and `LearningApplicationFactory`.
- Updated `ContentLibraryFacade.kt` and `ContentLibraryViewModel.kt` to expose `uninstallPackage(packageId, packageName)`, clearing `lessonBrowserUiState = null` if the removed package was currently open in the lesson browser.
- Added `Remove Topic` action button to `PackageListSection.kt` and `LibraryOverviewSection.kt` for installed package cards, connected to Compose Material3 confirmation `AlertDialog` in `LibraryScreen.kt` detailing package name and data impact.
- Expanded `CanonicalDesktopImportIntegrationTest.kt` with 17 comprehensive automated unit and integration tests covering file-scoped chooser configuration, OPD3 import, JSON/PKG pair resolution, single-file isolation, missing companion failure, conflict behavior, confirmation presentation, full persistence reconciliation, failure state preservation, package A/B isolation, collection assignment reconciliation, lesson browser clearing, restart persistence, re-import after removal, architecture dependency guards, and negative export UI check.
- Verification: `.\gradlew.bat clean test` — 1,949 tests passed across all modules (354 in desktop), 0 failures. Commit `fix: complete desktop import and topic removal flow`.

## PLE-001C — Restore Canonical Desktop Import Entry and Product Flow

- Integrated canonical `LibraryScreen` (`desktop/ui/library`) with `ContentLibraryViewModel` import pipeline (`desktop/ui/contentlibrary`), eliminating the unreachable legacy screen switch in `ContentHost.kt`.
- Added directory chooser component `PackageDirectoryChooser.kt` using `javax.swing.JFileChooser` set to `DIRECTORIES_ONLY`.
- Updated `LibraryHeader.kt` and `LibraryEmptyView.kt` to expose a prominent `Import Package` action button.
- Added `Browse Lessons` action button to `PackageListSection.kt` and `LibraryOverviewSection.kt` for installed package cards.
- Integrated `LessonBrowserCard` into `LibraryScreen.kt` for browsing, searching, selecting lessons, and launching study sessions via `onStartLessonStudy`.
- Updated `ContentLibraryFacade.kt` to sync newly imported packages with `ConflictAwarePackageImporter` for the canonical `defaultLibraryId`, reconciling the canonical `Library` navigation tree and content projections.
- Updated `LearningShell.kt` to trigger `libraryViewModel.refresh()` whenever `contentLibraryViewModel` emits `onContentDataChanged`.
- Added test suite `CanonicalDesktopImportIntegrationTest.kt` covering production composition, empty state import, delegation to import pipeline, busy guard against duplicate import, success reconciliation, failure preservation & retry, end-to-end lesson study launch, dependency direction source guard, and negative verification for zero Export OPD3 UI.
- Verification: `.\gradlew.bat clean test` — 1,941 tests passed across all modules (346 in desktop), 0 failures. Commit `fix: restore canonical desktop import flow`.

## LP-005 — Knowledge Graph Foundation

- Introduced immutable, learner-state-free `KnowledgeGraph` domain aggregate with deterministic ordering (commits `9976529`, `c18f373`, `d39725a`).
- Domain layer (`vn.loi.learning.domain.knowledge`): `KnowledgeNodeId` (@JvmInline, blank guard), `KnowledgeNodeKind` (5 kinds), `KnowledgeNode` (equality on id+kind only, not displayName), `KnowledgeRelationshipType` (7 typed directed relationships), `KnowledgeEdge`, `KnowledgeGraphValidationIssue` (5 sealed subtypes), `KnowledgeGraphValidationResult`, `KnowledgeGraph` (immutable, internal constructor), `KnowledgeGraphFactory` (validates before build, typed result), `KnowledgeGraphRepository` (domain port), `KnowledgeGraphAnalyzer` (cycle-safe BFS/DFS, shortest path, Kahn topological sort, transitive successors — all results deterministic).
- Infrastructure persistence: `KnowledgeNodeRecord`, `KnowledgeEdgeRecord`, `KnowledgeGraphRecord` (serializable DTOs); `KnowledgeGraphRecordMapper` (domain↔record round-trip, defensive on unknown enum names); `KnowledgeGraphStore` interface; `InMemoryKnowledgeGraphStore`; `JsonKnowledgeGraphStore` (envelope schema-versioned, atomic write via `JsonFileWriter`, missing-file-safe with empty-graph default, no legacy array format); `StoreBackedKnowledgeGraphRepository`.
- Application layer: `GetKnowledgeGraphUseCase`, `SaveKnowledgeGraphUseCase`, `KnowledgeGraphQueryService`, `InstalledLibraryKnowledgeGraphProjection` (ACTIVE installed packages → PACKAGE-kind nodes, canonical `packageId.value` identity, flat read-only snapshot).
- Wiring: `LearningApplicationContext` extended with optional `knowledgeGraphQuery`, `saveKnowledgeGraph`, `getKnowledgeGraph`, `installedLibraryGraphProjection`; `LearningApplicationFactory.createContext` wires full stack (JsonKnowledgeGraphStore → StoreBackedKnowledgeGraphRepository → use cases) and always provides `InstalledLibraryKnowledgeGraphProjection`.
- Test evidence: 83 LP-005 tests, 0 failures (`KnowledgeGraphDomainTest` 37, `KnowledgeGraphAnalyzerTest` 26, `KnowledgeGraphRecordMapperTest` 7, `JsonKnowledgeGraphStoreTest` 5, `InstalledLibraryKnowledgeGraphProjectionTest` 8). Full main-module `.\gradlew.bat clean test -x :desktop:test` BUILD SUCCESSFUL.
- Architecture invariants: graph describes knowledge structure only (no learner state); domain has no dependency on infrastructure, JSON, filesystem, Desktop, or Android; identity not tied to display names; all traversal results deterministic.

## LP-004R — Canonical Import Identity and Typed Conflict Semantics


- Removed `matchByName` from `PackageImportInspector`; `PackageId` and `TopicId` are the sole canonical identity authorities (commit `fb36ac4`).
- Added `AMBIGUOUS_EXISTING_IDENTITY` detection: when `PackageId` and `TopicId` each resolve to different existing records, inspection returns a typed `CONFLICT` with no repository mutation.
- Conservative identical evidence contract: `IDENTICAL_PACKAGE` verdict requires both the installed record and the candidate to supply a matching `contentChecksum`; absent or mismatched checksums produce `INSUFFICIENT_IDENTITY_EVIDENCE` conflict, preventing false identical conclusions.
- Replaced free-form `List<String>` conflict reasons with the typed `PackageImportConflictReason` sealed enum (`TOPIC_ID_MISMATCH`, `AMBIGUOUS_EXISTING_IDENTITY`, `OLDER_VERSION`, `INSUFFICIENT_IDENTITY_EVIDENCE`); consumers switch on enum values without string parsing.
- Removed `InstalledPackage.reconstitute` fabrication fallback from `ConflictAwarePackageImporter.executeImport`; the `IDENTICAL_PACKAGE` branch now returns the real aggregate from the repository, or a `TechnicalFailure` on repository inconsistency.
- Added nullable `contentChecksum: String?` field to `InstalledPackage` (backward-compatible default `null`); the field is persisted during `NEW_PACKAGE` and `SAFE_REPLACEMENT` imports for future fingerprint comparison.
- `ConflictAwarePackageImporter.executeImport` now accepts an explicit `contentChecksum` parameter and propagates it to the persisted aggregate.
- Extended `ConflictAwarePackageImporterTest` with 13 focused test cases covering identity invariant violations, ambiguous identity, checksum presence/mismatch, conservative conflict, fabrication prevention, real-aggregate verification, rollback safety, and typed reason switching.
- BUILD SUCCESSFUL: `gradlew.bat clean test` — 1,820 tests, 0 failures, 0 errors.

## LP-004 — Conflict-Aware Package Import

- Built the application/domain conflict-aware import decision boundary (`PackageImportInspector`, `PackageImportDecision`, `PackageImportOutcome`, `ConflictAwarePackageImporter`).
- Read-only inspection before mutation classifying candidates into `NEW_PACKAGE`, `IDENTICAL_PACKAGE`, `SAFE_REPLACEMENT`, or `CONFLICT`.
- Re-used authoritative core identities (`PackageId`, `TopicId`) without reliance on filenames, display labels, or filesystem paths.
- Deterministic identical package comparison resulting in no-op without creating duplicate records or altering learner progress.
- Safe replacement preserves `TopicId` identity, learner progress/history, and collection assignments.
- Structured conflict return without mutating repositories on conflict; atomic transaction rollback on failure.
- Wired into `LearningApplicationContext` and `LearningApplicationFactory`. Verified with comprehensive test suite in `ConflictAwarePackageImporterTest`.



## LP-003R.1 — Deterministic Library Failure Mapping


- Refactored `LibraryFailureMessage` and failure classification to produce 100% deterministic user-facing UI messages (`SERVICE_UNAVAILABLE_MESSAGE`, `LIBRARY_NOT_FOUND_MESSAGE`, `UNEXPECTED_FAILURE_MESSAGE`) without taking any part of `Throwable.message`, cause message, stack trace, or class names.
- Introduced typed exceptions `LibraryServiceUnavailableException` and `LibraryNotFoundException` in `vn.loi.learning.desktop.ui.library` for precise, type-safe failure classification without text string searching.
- Added comprehensive test coverage in `LibraryViewModelTest` asserting that misconfiguration, non-existent library ID, and unexpected exceptions with sensitive paths/SQL/URLs/secrets produce exact deterministic error messages, and different unexpected exceptions produce identical UI messages.

## LP-003R — Library Runtime Identity & Failure Semantics


- Hardened Desktop Library production contracts by resolving runtime `LibraryId` at the application/composition root (`LearningApplicationContext` & `LearningApplicationFactory`).
- Completely eliminated hard-coded string literals `"default-library"` from Desktop Presentation layer (`LibraryFacade`, `LibraryViewModel`).
- Hardened failure semantics: `LibraryUiState.Empty` is rendered strictly for valid empty libraries; missing services, unmapped identities, or query exceptions strictly produce `LibraryUiState.Error`.
- Created `LibraryFailureMessage` to sanitize technical exceptions, preventing raw `Throwable.message`, filesystem paths, class names, stack traces, or secrets from reaching UI state.
- Added comprehensive unit tests in `LibraryViewModelTest` covering runtime Library ID propagation, sanitized failure handling, secret/path masking, empty library state mapping, section switching, and zero repository access from presentation layer.

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

# Stale Study restore without an ACTIVE package

- Made an `ACTIVE` `InstalledPackage` the only canonical authority for General Study; archived,
  removed, orphan `ContentPackage`, legacy content, and all-items fallbacks no longer qualify.
- Reordered `StudyFacade.load()` so missing ACTIVE scope clears General Study runtime and purges
  active General Study sessions and queues before cache, current-item, or recovery inspection.
- Rejects recovered General Study sessions without package provenance, with a non-ACTIVE or
  non-canonical package, a mismatched topic, or content/queue items outside package ownership.
- Preserved explicitly lesson-scoped/manual flows while making normal General Study sessions
  persist package scope through `installedPackageId` rather than an all-content fallback.
- Corrected uninstall to delete every resolved matching `ContentPackage.id` while preserving
  unrelated package state.
- Added store-backed restart, same-process removal, orphan content, null-package session/queue,
  reimport-as-NEW, unrelated progress, and resolved uninstall identity coverage.
- Verification: `.\gradlew.bat clean test` — 2,307 tests passed, 0 failed.
## PLE-023 — Modern Compact Package Card

- Reorganized each Library package card into a compact header, one six-metric row, started
  progress, latest-rating availability, and a single-row action bar.
- Replaced internal progress terminology with Vietnamese learner-facing labels while retaining
  the canonical projection semantics: learning is started minus mastered, learned is the full
  started count, and unseen remains distinct from persisted NEW state.
- Added vi-VN count and percentage formatting so 4,950 renders as `4.950` and 23/4,950 renders
  as `0,5%` rather than the rounded integer `0%`.
- Rating history remains explicitly unavailable because no trustworthy package-scoped rating
  query exists; no global or fabricated counts are shown.
- Manual Desktop UAT with the installed 990-lesson/4,950-item package confirmed the six metrics
  remain on one row at the current desktop width, actions remain one row, and the card remains
  compact without exposing Topic or Package IDs.

## PLE-022B — Reliable Interactive Study Audio Loops

- Audio playback now publishes path-specific completion, allowing the controller to schedule
  each repeat only after the current clip finishes.
- The controller owns and cancels pending replay work, uses a generation guard against stale
  callbacks, stops the current loop when switching or single-playing, and clears failed loops.
- English vocabulary and English example surfaces retain toggle-loop behavior; Vietnamese
  meaning and example icons now use single-play semantics.
- The persisted loop gap keeps the existing property, defaults missing values to 0.35 seconds,
  offers common presets plus validated custom input, and is wired from current runtime
  configuration into `StudyScreen`; the next repeat observes setting changes.

## PLE-022A — Package Learning Progress & Session Limits

- Library package cards replace internal Topic/Package IDs with application-projected learning
  progress, including distinct unseen, NEW-state, started, due, mastered, and suspended metrics.
- Library refresh loads navigation and package progress on `DesktopTaskRunner`; the application
  batch projection reads learner memory once and isolates package-specific failures as
  `progress unavailable`.
- Runtime configuration now persists backward-compatible new/review session maxima (20/100
  defaults), validates safe ranges and rejects 0/0. Desktop composition maps current settings
  to a domain `SessionPolicy` only when a new general or lesson session starts.
- Active/resumed sessions continue using their persisted policy snapshot; newly continued
  general sessions use the then-current settings.
## PLE-024-R2 — Discoverable Audio Interactions

- Added a shared Desktop audio-interaction presentation for hover, press, keyboard focus,
  disabled, and active-loop states; audio surfaces now use visible tint/border feedback instead
  of suppressing indications.
- Revealed vocabulary text, speaker, and image use the same primary-audio loop state. Image
  overlays are decorative within one owning clickable surface, so a pointer or keyboard action
  dispatches exactly once.
- Image-recall projection retains the authoritative `PRIMARY_WORD` audio beside the prompt
  image. Front images play it once, while revealed images toggle it as a pronunciation loop;
  translated meaning/example audio is never selected as the primary.
- Primary replay prefers the typed role and falls back to the first block only for all-legacy
  `OTHER` scenes, preserving old content without guessing across typed audio roles.
- Meaning and Vietnamese-example audio remain single-play; English examples remain loop
  targets. Existing item/scene/pause/completion/disposal cancellation remains controller-owned.
- **Verification:** `.\gradlew.bat clean test --no-daemon` — BUILD SUCCESSFUL; XML-verified
  **2,355 passed, 0 failed, 0 errors, 0 skipped**. `git diff --check` clean.
## PLE-028E — Dynamic Part-of-Speech Semantic Color Registry

- Consolidated POS extraction and canonicalization into one locale-independent application
  authority covering case-insensitive `partOfSpeech`/`pos` custom fields, `pos:` tags, and POS
  embedded in pronunciation. Common aliases share canonical identities; unknown nonblank values
  remain distinct and `WORD` is no longer inferred from missing POS.
- Added an application-owned semantic registry and idempotent repository reconciler. Startup
  reconciles installed content once; successful package imports register their parsed content
  after the established transaction without mutating imported records or package payloads.
- Added fixed semantic identities for the approved known catalog and deterministic SHA-256
  identities for future values. A bounded 1,048,576-slot visual space probes active collisions;
  semantic keys include canonical identity and do not use random allocation or
  `String.hashCode()`. No new persistence/schema boundary was required.
- Added Light/Dark POS palette tokens and a design-system adapter outside the theme boundary.
  Every Study POS badge uses the same registry/style resolver, retains canonical text and border,
  and contains no raw color or Material color authority.
- Inventory evidence is recorded in `docs/reports/PLE-028E_POS_INVENTORY.md`: runtime installed
  content supplied 14 canonical values; repository fixtures/Studio add `ANIMAL` and explicit
  `WORD`. `ANIMAL`, `NOUN PHRASE`, and `PROPER NOUN` remain dynamic/custom identities.
- Focused application/import/browser/theme/Study selection passed 105 tests.
  `.\gradlew.bat clean test --no-daemon` completed `BUILD SUCCESSFUL`: root `:test` 346 XML
  suites / 1,674 tests and Desktop `:desktop:test` 170 suites / 867 tests; total 516 suites /
  2,541 passed, 0 failed, 0 errors, 0 skipped. Manual visual UAT remains pending.

## PLE-029 — Robust Learning-Key Highlighting and Configurable Study Audio Shortcuts

- Replaced literal regex and suffix guessing with one English/Vietnamese matcher using NFC,
  case folding, apostrophe/hyphen normalization, flexible whitespace, lexical boundaries,
  longest non-overlapping matches and exact original-text index mapping.
- Added vocabulary loop (`L`), English example loop (`Shift+L`), Vietnamese meaning (`V`) and
  Vietnamese example (`Shift+V`) commands. Replay remains one-shot `R`.
- Routed commands through the established registry, Study router and audio controller. Legacy
  settings preserve existing mappings and gain only missing actions; conflicts do not silently
  overwrite.
- Aggregate evidence is in `docs/reports/PLE-029_HIGHLIGHT_AUDIT.md`. Manual UAT remains pending.
- Focused matcher/shortcut/persistence/audio selection passed 42 tests.
  `.\gradlew.bat clean test --no-daemon` completed `BUILD SUCCESSFUL`: root `:test` 346 XML
  suites / 1,674 tests and Desktop `:desktop:test` 170 suites / 873 tests; total 516 suites /
  2,547 passed, 0 failed, 0 errors, 0 skipped.

## PLE-030 — Realtime Study Header Statistics

- Added one application projection for exact package, lesson or multi-content session scope.
  Total counts unique enabled items; New/Review partitions on completed persisted review events;
  rating buckets use only each item's latest effective event.
- Due reuses `MemoryState.isDue()` with an injected clock and returns the nearest future due
  instant. Desktop schedules one refresh at that transition instead of polling.
- StudyFacade/ViewModel refresh from repository truth after successful review, Undo and content
  changes. Failed mutations do not publish speculative counts; query failure retains
  last-known-good data when available.
- Added a compact localized two-row header with LETheme semantic rating colors, stable
  loading/unavailable height and one complete accessibility description. Product Owner Manual
  UAT later closed PLE-030 as FINAL PASS.
- Focused projection/presentation/Study regression selection passed 69 tests.
  `.\gradlew.bat clean test --no-daemon` completed `BUILD SUCCESSFUL`: root `:test` 347 XML
  suites / 1,680 tests and Desktop `:desktop:test` 171 suites / 876 tests; total 518 suites /
  2,556 passed, 0 failed, 0 errors, 0 skipped.

## PLE-030.1 — Study Header Session Progress Semantics Remediation

- Corrected Total from raw enabled inventory to unique learned items with a latest effective
  rating; Total now equals Again + Hard + Good + Easy.
- Split session progress from package learned-state. New renders committed unique New completions
  over the frozen configured target; Review renders exact planned remaining Review identities
  over the frozen configured target. Effective workloads remain separately modeled.
- Reused StudySession policy/counters, exact persisted queue remaining IDs, review events and
  domain Due authority. Review is not inferred from Due and no UI-local arithmetic was added.
- Audited queue planning, Undo, completion and Continue Learning. Dedicated continuous Review
  Mode is not currently implemented and is documented as separate future scope.
- Focused projection/presentation/Study queue/session regression selection passed.
  `.\gradlew.bat clean test --no-daemon` completed `BUILD SUCCESSFUL`: root `:test` 347 XML
  suites / 1,682 tests and Desktop `:desktop:test` 171 suites / 876 tests; total 518 suites /
  2,558 passed, 0 failed, 0 errors, 0 skipped.
- Product Owner Manual UAT later closed this remediation as part of PLE-030 FINAL PASS.

## PLE-030.2 — Study Statistics Header Visual Refresh

- Replaced the two small caption rows with one unified, non-interactive eight-segment dashboard
  ordered Total, New, Review, Due, Again, Hard, Good and Easy.
- Added semantic metric label/value/subtitle typography (13sp/28sp/11sp), eight icon roles and a
  compact statistics surface variant. Existing Light/Dark purple, green, blue, orange and red
  metric families remain the only color authority.
- Split New/Review numerator emphasis from the muted denominator. Zero progress and zero
  Due/rating values remain visible but muted; Total remains a clear neutral-state metric.
- Reused `StudyViewportClass`: Standard/Wide displays one row of eight; Compact displays 4+4 and
  hides subtitles without horizontal scrolling or a second viewport resolver.
- Added localized subtitles and complete accessibility sentences so fractions are announced by
  meaning rather than as slash-delimited text. Loading and last-known-good behavior is unchanged.
- No statistics, session, queue, scheduler, review, Undo, realtime refresh or persistence
  semantics changed. Product Owner Manual UAT later closed this remediation as part of
  PLE-030 FINAL PASS.
- Focused semantics/presentation/theme/responsive/accessibility and active-header selection
  passed 9 XML suites / 99 tests. `.\gradlew.bat clean test --no-daemon` completed
  `BUILD SUCCESSFUL`: root `:test` 347 suites / 1,682 passed and Desktop `:desktop:test` 171
  suites / 883 passed; total 518 suites / 2,565 passed, 0 failed, 0 errors, 0 skipped.
## PLE-030.5 — Front context, display reflow, and session goals

- Added a read-only rating context dock before reveal and preserved answer-side rating actions.
- Added density/font-scale invalidation through the existing layout resolver.
- Persisted configured/effective Content workloads separately from technical queue entries in
  schema v4 and clarified technical progress wording.
## PLE-030.6 — New-content Introduction and compact-height Study layout

- Added persisted ContentId-level Introduction exposure before the existing NEW learning flow,
  with fitted image, Vietnamese meaning/POS, replay control, and one-shot Vietnamese autoplay.
- Added comfortable, compact-height, and minimum-height resolver modes, smaller adaptive image
  budgets, fixed chrome reserves, and the existing stable center-scroll fallback.
- Removed dashboard subtitles and learner-facing technical experience progress; compacted
  single-stage metadata without changing accessibility progress or diagnostic models.
- Preserved scheduler/FSRS, rating/counter, queue, Content identity, audio-loop, and legacy JSON
  behavior. Automated verification is complete; Product Owner Manual UAT later closed this
  remediation as part of PLE-030 FINAL PASS.
## PLE-030.7 — Introduction flow and compact Study chrome remediation

- Changed single visual NEW flows from Introduction → planned front → Answer to one atomic
  Introduction → persisted Full Answer transition; mandatory Typing/multi-experience flows and
  all later recall planning remain unchanged.
- Compacted Undo/Pause, front context segments, answer rating buttons, dock padding/reserves,
  footer gap, and compact answer estimates through resolver/shared density tokens.
- Added compact-viewport evidence for both example rows, fitted images, stable single scroll,
  restart-safe reveal, rating semantics, and accessibility minimum targets.
- Automated verification is complete; Product Owner Manual UAT later closed this remediation
  as part of PLE-030 FINAL PASS.
## PLE-030.8 — Answer rating dock restoration

- Corrected same-item Introduction direct reveal synchronization from stale Experience state
  to the normal Rating Ready phase.
- Centralized existing dock render conditions in `StudyActionDockMode`; answer content now uses
  the same actionable Again/Hard/Good/Easy dock as normal reveal, including busy-disabled state.
- Preserved shortcuts 1–4, callbacks, compact/minimum reserves, restart behavior, counters,
  review transaction, scheduler/FSRS, and PLE-030.7 one-Next behavior.
- Automated verification is complete; Product Owner Manual UAT later closed this remediation
  as part of PLE-030 FINAL PASS.
## PLE-030.9 — Study goal synchronization and review memory

- Remediated Settings → Study navigation so an active session with stale New/Review limits is
  finished and replaced on Study entry using a fresh persisted policy. Matching limits retain
  the active session, and unrelated settings do not participate in invalidation. The replacement
  owns fresh counters while preserving the prior session's immutable policy and history.
- Made Full Answer height-adaptive from the live content viewport and display environment.
  Shorter supported viewports compress gaps, card padding, then image height while reserving the
  first bilingual example and fixed Rating Dock; pre-answer image-first sizing remains unchanged.
- Replaced estimated Full Answer fit claims with two-pass measured Compose geometry using the
  actual weighted body height. The first bilingual example is measured before the image receives
  the remainder; continuation content and exceptional content scroll.
- Corrected Full Answer information priority so Scheduler Feedback is measured and placed as
  continuation after the required region. Its height and separating gap now return to the image
  while word, pronunciation, meaning, first bilingual example, and fixed Rating Dock remain fit.
- Added a true compact-inline Statistics composition selected from actual usable dashboard
  width. It retains all eight metrics and denominators in 2×4 order while reducing the measured
  common fixture from 140dp to 92dp, returning 48dp to Learning Content without changing the
  measured Full Answer or pre-answer presentation contracts.
- Replaced dynamic Content-grouping Review counts with persisted queue effective workload minus
  session Review completions, with synchronous header refresh after Review and Undo.

- Routed new-session policy creation through a fresh persisted runtime-configuration read,
  removing the remembered composition closure that retained startup goals.
- Kept queue limits and header configured targets downstream of the immutable session policy;
  running-session counters and history remain unchanged when Settings change.
- Replaced pre-answer rating-like cards with a non-interactive REVIEW-only memory footer based
  on Content identity. NEW shows no rating memory; Prompt, Image, Listening, and Typing REVIEW
  experiences share the same semantics; Full Answer retains the real action dock.
- Corrected the final compact-width Full Answer regression where the outer viewport class
  forced speaker, IPA, and POS into a tall stack despite sufficient Identity-card width.
  Identity now selects standard inline, compact inline, or ultra-narrow stacked composition
  from its own measured usable width. Compact inline preserves word typography while tightening
  only metadata tokens; the acceptance fixture reduces Identity from 160dp to 84dp and returns
  76dp to the adaptive fitted image.
- Focused regression verification passed 97 tests; full `clean test --no-daemon` passed 2,627
  tests (root 1,699; Desktop 928), with no failures, errors, or skipped tests.
- Replaced width-squeezed Study Chrome text wrapping with an actual-usable-width semantic
  presentation. Compact/minimum modes use square Material Undo/Pause actions with tooltips,
  bounded shortcut strips, priority-ordered chord tokens, and a non-wrapping icon-only session
  indicator while retaining full merged accessibility and the existing keyboard registry.
  The compact acceptance fixture falls from 308dp to 176dp and returns 132dp to Learning
  Content without changing Rating Dock or Full Answer fit behavior.
- Focused adaptive-chrome and protected-boundary verification passed 42 tests; full
  `clean test --no-daemon` passed 2,632 tests (root 1,699; Desktop 933), with no failures,
  errors, or skipped tests.
- Corrected the bottom Study surface from a row of visible keyboard tokens into the approved
  icon-only Quick Action Toolbar. Rating Ready now renders four semantic-color numbered
  circles, outlined Replay, Material Undo, separators, and a success session indicator in the
  existing bounded row. Existing callbacks and keyboard bindings are unchanged; Ctrl+Z and
  rating names remain available only through tooltip/accessibility text.
- Focused toolbar/chrome/keyboard/protected-boundary verification passed 40 tests; full
  `clean test --no-daemon` passed 2,633 tests (root 1,699; Desktop 934), with no failures,
  errors, or skipped tests.

## PLE-031 — Live Audio Shortcut Toolbar

- Added one shared chord formatter used by registry display, Settings, Study toolbar,
  tooltips, and accessibility; compact Shift chords render as `⇧` without losing complex
  modifiers.
- Projected all four configured audio commands into the live Quick Action Toolbar and wired
  them to the existing audio keyboard execution boundary. Availability follows current
  vocabulary/example/Vietnamese audio paths.
- Standard and compact retain the full fixed single-row toolbar. Minimum width keeps priority
  actions and moves every secondary action into an accessible, actionable overflow menu.
- Settings Change/Reset recomposes toolbar and dispatcher from the same persisted runtime
  registry without restarting or invalidating the active Study Session.
- Focused shortcut/runtime/toolbar/audio/session verification passed 49 tests; full
  `clean test --no-daemon` passed 2,637 tests (root 1,699; Desktop 938), with no failures,
  errors, or skipped tests.

## PLE-031.1 — Semantic Audio Toolbar remediation

- Removed visible shortcut chords from the four main audio action buttons. A command-based
  semantic resolver now owns stable icons independently of Change/Reset.
- Distinguished vocabulary repeat from example repeat, and replaced translation-like
  Vietnamese visuals with distinct audio/example vectors plus accessible `VI`/`VI+` badges.
- Live chords remain in tooltip, content description, Settings, dispatcher, and overflow;
  disabled actions preserve the same icon and fixed toolbar geometry.
- Focused verification passed 33 tests. Full `clean test --no-daemon` passed 2,639 tests
  (root 1,699; Desktop 940), with no failures, errors, or skipped tests. Product Owner Manual
  UAT later closed PLE-031.1 as FINAL PASS.

## PLE-031.2 — Live Shortcut Cues for Semantic Audio Actions

- Kept command-owned semantic icons and added formatter-owned live chord micro-labels beside
  each main audio action; no action-name text is rendered in the toolbar.
- Standard uses full chord formatting, Compact/Minimum use compact formatting, and the existing
  fixed strip heights, single row, adaptive priority, callbacks, and overflow boundary remain.
- Tooltips now use action name plus a dedicated `Shortcut:` line; accessibility includes the
  current full chord, and disabled actions retain icon/chord plus an unavailable reason.
- Minimum overflow now presents semantic icon, action name, and current chord. Focused
  verification passed 35 tests. Full `clean test --no-daemon --console=plain` passed 2,641
  tests (root 1,699; Desktop 942), with no failures, errors, or skipped tests.

- Final UAT remediation makes REVIEW memory eligibility independent of the reveal-action flag,
  so Prompt, Image, Listening, and Typing pre-answer states always retain the read-only status;
  REVIEW never uses the NEW-only Introduction surface.
- Replaced the pre-answer 620×340 fallback and Standard/Wide 620×150/200 image caps with one
  display-resolved 90%-content-width and vertical-budget contract consumed by every image
  surface. Aspect ratio remains `Fit`; crop and scheduler/rating behavior are unchanged.
- Remediation verification: focused memory/image/Introduction/flow/chrome selection passed 82
  tests; full `clean test --no-daemon` passed 2,607 tests (root 1,698; Desktop 909), with no
  failures, errors, or skipped tests. Product Owner Manual UAT later closed this remediation as
  part of PLE-030 FINAL PASS.

## PLE-031C — Study Experience Closure

- Product Owner Manual UAT closed PLE-030, PLE-031, PLE-031.1, and PLE-031.2 as FINAL PASS.
- Verified closure baseline is clean `develop` at
  `49f0472d3e5c8e1dd004a2c9a1c9e6298f3e1e12`, identical to `origin/develop`.
- The next capability is PLE-032 Continuous Review Mode; it is not yet implemented.
- Phase 7 and Desktop v1 remain open for clean-machine, installer/update/uninstall, signing,
  real large-package/manual, and external Beta evidence.
- This closure is documentation-only and reuses the committed PLE-031.2 build evidence:
  2,641 tests (root 1,699; Desktop 942), with no failures, errors, or skipped tests.
# AURORA-006 — Session Completion Reinforcement

- Reframed the existing Study completion surface into a clear acknowledgement, factual outcome,
  scheduler consequence, and next-action hierarchy using existing `LETheme` surfaces and controls.
- Added a pure Desktop presentation resolver with typed action identity and deterministic priority.
  At most one available continuation action is primary; Library, Back to Lesson, Undo, and
  Continuous Review retain their existing callbacks, availability, and learning authority.
- Moved final scheduler consequence, Continuous Review, and Undo into the completion experience
  without changing review, session, queue, Scheduler/FSRS, persistence, keyboard, or restart
  behavior. No score, streak, XP, mastery, celebration, or gamification was added.
- Focused verification passed 8 suites / 52 tests; broad completion/continuity/keyboard/focus/
  scheduler/Undo/restart regression passed 21 suites / 130 tests. Full `clean test` passed 564
  suites / 2,919 tests (root 361 / 1,771; Desktop 203 / 1,148), with 0 failures, errors, or
  skipped. Integrated Desktop UAT remains pending.
# AURORA-007 — Learning Entry Clarity

- Reorganized the existing Learn-entry chooser into current context, readiness, one primary Study
  action, alternative study modes, and a separate Library-management exit. The composition uses
  existing `LETheme` surfaces and remains one responsive focal column.
- Added typed, deterministic presentation priority over immutable `StudyUiState` projections.
  Resume/Continue is primary only when enabled; Replay and Review All remain alternatives; Library
  remains navigation. Busy and no-scope states cannot create a false enabled primary.
- Readiness uses only projected active-session progress, New/Review workload, and learned-item
  availability. English/Vietnamese labels and shortcut semantics are localized; technical IDs,
  gamification, invented metrics, repository scans, and display-text callback routing are absent.
- Study planning, queue, session lifecycle, Replay, Review All, Scheduler/FSRS, Continuous Review,
  persistence, search, and Library administration are unchanged. Focused verification passed 4
  suites / 44 tests; broad regression passed 47 suites / 254 tests. Full `clean test` passed 565
  suites / 2,934 tests (root 361 / 1,771; Desktop 204 / 1,163), with 0 failures, errors, or skipped.
  Integrated Desktop UAT remains pending.
# AURORA-008 — Dashboard Clarity & Information Hierarchy

- Added a pure Dashboard presentation resolver that orders today's status and existing Learn action
  context before key metrics, recent activity, and historical analytics. `dueToday`, `dueNow`,
  `overdue`, Total, New, and Active Memories remain unchanged `DashboardUiState` projections.
- Added a dominant “Ready today” semantic surface, moved Due out of the equal-weight overview grid,
  and reduced metric/chart card weight through `LETheme` surfaces, typography, spacing, border, and
  elevation tokens. Activity and heatmap remain secondary; scheduling, memory, retention, and
  forecast remain later historical sections.
- Dashboard query services, Facade computation/formatting, heatmap/rating distribution data,
  callbacks, shell Learn navigation, keyboard/focus, Scheduler/FSRS, persistence, counters, and
  analytics authority are unchanged. No metric or Dashboard-local action was invented.
- Focused Desktop verification passed 4 suites / 24 tests; application + Desktop Dashboard
  regression passed 30 suites / 180 tests. Full `clean test` passed 566 suites / 2,943 tests
  (root 361 / 1,771; Desktop 205 / 1,172), with 0 failures, errors, or skipped. Integrated Desktop
  UAT remains pending.

# AURORA-009 — Premium Study Surfaces

- Added a pure shared Study surface resolver with typed discovery and understanding stages, semantic
  roles, layers, border prominence, resting elevation, and deterministic hierarchy.
- Discovery now presents the existing image/question anchor as hero content and Vietnamese meaning
  as primary support. Understanding presents the existing word, image, meaning, examples, scheduler,
  and rating regions with distinct semantic weight while flattening the redundant outer content-card
  outline/elevation and reducing nested borders.
- Existing image bounds and `ContentScale.Fit`, content order, typography scale, reveal/continuity
  motion, keyboard/focus/semantics, audio, Typing, rating feedback, scheduler, session, and learning
  authorities are unchanged. No new copy, feature, workflow, or raw color/elevation authority was added.
- Focused verification passed 4 suites / 68 tests. Full `clean test` passed 567 suites / 2,949 tests
  (root 361 / 1,771; Desktop 206 / 1,178), with 0 failures, errors, or skipped. Integrated Desktop
  UAT remains pending.

# AURORA-010 — Unified Study Stage

- Added `UnifiedStudyStageResolver`, a pure deterministic composition contract over the existing
  AURORA-009 semantic roles. Discovery keeps Hero → Meaning → Action; Understanding keeps Hero →
  Meaning → Examples → Scheduler explanation → Rating decision, with image support retained in its
  existing ordered position.
- Flattened wrapper-only surfaces around discovery Meaning, answer Meaning/Examples, and compact
  Scheduler feedback. Removed the extra per-example outer card while retaining audio-row interaction
  surfaces, hero image/word clipping and focus seams, whitespace grouping, and the Rating Dock boundary.
- Layout order, responsive rules, image sizing/`ContentScale.Fit`, typography scale, motion timing,
  keyboard/focus/accessibility, audio, Typing, rating feedback, Scheduler/FSRS, session, queue,
  persistence, Undo, and Continuous Review are unchanged.
- Focused verification passed 5 suites / 75 tests. Full `clean test` passed 567 suites / 2,953 tests
  (root 361 / 1,771; Desktop 206 / 1,182), with 0 failures, errors, or skipped. Integrated Desktop
  UAT remains pending.
# SESSION-001 — Explicit Resume or Start New Study Session

- Learn entry now separates resuming the persisted active queue from starting a new session with current limits.
- Changing session limits no longer replaces an active session on Study entry; explicit start-new confirmation is required.
- Active queue progress and next-session configuration are projected as separate facts.
# LQ-003 — Exact Session Word Limits and Durable Same-Session Lapse Guard

- Normal Study planning now persists one deterministic representative LearningItem per selected Content, so New/Review limits are exact word-level limits.
- A committed Again records durable session-local Content lapse ancestry; later automatic Typing successes in that session remain capped at Hard even after intermediate Hard ratings.
- Undo restores the prior lapse ancestry and persisted session recovery retains it. Manual ratings, Scheduler/FSRS, focused review membership, and experience models are unchanged.
# LQ-004B — Cross-Platform Practice Loops, Manual Override, and Rating Inventory

- Converted Latest-New and Again/Hard focused entry to persisted `PRACTICE_ONLY` sessions. The
  first freezes predecessor committed NEW-origin Content; the second freezes the complete current
  latest-rating Again/Hard set without the normal Study limit.
- Added deterministic Fisher–Yates infinite rounds over fixed membership with persisted
  membership/order/position/seed/round, round-local typed progress, no queue growth, exact restart,
  practice-local result semantics, and explicit leave.
- Practice answers now bypass review staging, ReviewEvent, MemoryState, Scheduler/FSRS, rating,
  reinsertion, and evaluative completion. Desktop renders explicit practice copy and local result
  actions without rating-transition/scheduler feedback.
- Added separately confirmed Manual Rating Override using the existing atomic evaluation boundary,
  typed `MANUAL_USER_OVERRIDE` provenance, unchanged practice membership/policy, and Undo that
  restores prior event/memory/due state without rewinding practice navigation.
- Added realtime shared-Core `RatingInventory` projection over latest committed rating per eligible
  Content and a compact Desktop chooser projection. Practice-local results do not change counts.
- Added [`PRACTICE_SESSION_CONTRACT.md`](PRACTICE_SESSION_CONTRACT.md); Integrated Desktop UAT
  remains pending.
- Verification: focused 8 XML suites / 39 tests; full clean build 578 suites / 3,019 tests (root
  363 / 1,787; Desktop 215 / 1,232), failures/errors/skipped 0 / 0 / 0.

# LQ-004A — Cross-Platform Practice Foundation (Phase 1)

- Added persisted `SessionEvaluationPolicy` to shared `SessionPolicy`, distinguishing
  `EVALUATIVE` from `PRACTICE_ONLY` without strings, booleans, or Desktop state as authority.
- Added a shared Application guard before review intent staging and transaction entry. A practice
  session therefore cannot create ReviewEvent, mutate MemoryState/rating/due/stability/difficulty,
  or invoke Scheduler/FSRS through the established review workflow.
- Preserved legacy JSON/session compatibility by defaulting absent policy data to `EVALUATIVE`.
  The guarded shared review command/transaction remains the extension point for a later Manual
  Rating Override capability without implementing it in this phase.
- Practice loop, shuffle, evidence, promotion, UI/layout/navigation, queue behavior, and Desktop
  behavior remain unchanged and out of scope. Integrated Desktop UAT remains pending.
- Verification: focused 3 XML suites / 18 tests; full clean build 575 suites / 3,013 tests (root
  361 / 1,783; Desktop 214 / 1,230), failures/errors/skipped 0 / 0 / 0.
# UX-008 — Final Media Sizing and Typing Visibility Correction

- Replaced the compressible typing-card internals with a measured Compose layout whose actual outer,
  label, inner line-box, and trailing-action bounds preserve the content-derived minimum at compact
  and wide widths. Typing fronts use bounded body scrolling only when their content overflows.
- Added typed frame/rendered media metrics. Landscape uses available width when height permits;
  square and portrait remain aspect-derived; the existing 1.35x source-quality cap shrinks both
  bitmap and frame instead of leaving an oversized empty frame. `ContentScale.Fit` remains intact.
- Preserved the UX-007 result/exit/enter transition, direct evaluative rating, Practice, inventory,
  queue, Scheduler/FSRS, ReviewEvent, MemoryState, Undo, and `VALID_PREFIX` authorities.
- Integrated Desktop UAT remains pending.
- Verification: focused 8 XML suites / 56 tests; full clean build 585 suites / 3,041 tests
  (root 365 / 1,792; Desktop 220 / 1,249), failures/errors/skipped 0 / 0 / 0.
# LQ-005A.1 — Evidence Chain & Learning Trajectory Foundation

- Added immutable Content-owned `EvidenceChain`, validated `ChainAnchor`, chronological mixed
  evidence/non-evidence sequence, typed promotion stages, and typed reset/anchor reasons.
- Added `LearningTrajectory` as an ordered chain history with one open current chain. Promotion,
  new Again/lapse, and explicit manual/recovery/Undo boundaries close the prior chain and start a
  fresh chain without reusing old recalls.
- Added `ContentId` to recall evidence; sibling LearningItems therefore converge on one trajectory.
  Manual ratings are recorded only as non-evidence events and do not reset or create a chain.
- Changed `PromotionCandidate` to read one current chain instead of loose evidence and anchor inputs.
  No persistence, migration, execution, Desktop, Scheduler/FSRS, queue, Practice, ReviewEvent,
  MemoryState, transition, or Undo wiring was added. Integrated UAT remains pending.
- Verification: focused 2 XML suites / 23 tests; full clean build 587 suites / 3,064 tests
  (root 367 / 1,815; Desktop 220 / 1,249), failures/errors/skipped 0 / 0 / 0.

# LQ-005A — Long-Term Evidence Promotion Authority

- Added shared-Domain recall evidence, promotion candidate/decision/reason, evidence-window policy,
  and injected clock contracts without any Desktop or persistence dependency.
- Implemented default Again→Hard (24h/1 recall), Hard→Good (72h/2 independent recalls/no Again),
  and Good→Easy (14d/3 recalls/no lapse) eligibility rules.
- Excluded Practice, Reveal, manual override/direct manual rating, replay, undone, and duplicate
  evidence with typed explanations, remaining duration, and missing-recall counts.
- Added no execution wiring: ratings, Scheduler/FSRS, ReviewEvent, MemoryState, queue, inventory,
  Practice, Undo, and Desktop remain unchanged. Integrated UAT remains pending.
- Verification: focused 1 XML suite / 12 tests; full clean build 586 suites / 3,053 tests
  (root 366 / 1,804; Desktop 220 / 1,249), failures/errors/skipped 0 / 0 / 0.
# LQ-005C — Cross-Platform Learning Insight & Learner Transparency

- Added deterministic typed `LearningInsight` primary/secondary projections in Shared Application.
- Added a read-only learner-and-Content query through the existing trajectory, difficulty, and
  recommendation authorities, with optional typed promotion-decision transparency.
- Preserved remaining promotion time, missing recalls, typed reasons, practice exclusion, and
  manual-rating provenance without changing authority semantics.
- Wired a localized, accessible, compact answer-side and completion card in Desktop.
- Added 18 focused root tests and 8 focused Desktop presentation/wiring tests; integrated Desktop
  UAT remains pending.

# UX-009 — Show IPA Before POS in Correct-Answer Feedback

- The typing-success popup now reads the current Content's normalized IPA from the existing focused
  vocabulary presentation and renders it immediately before the existing POS badge.
- Missing or blank IPA/POS values collapse naturally without separators or reserved spacing.
- Compact presentation bounds IPA while preserving the POS badge and the rating/explanation rows.
- EN/VI accessibility labels preserve answer → IPA → POS → rating transition → explanation order.

# UX-011 — Typing Success Popup Rapid Sequential Reveal

- Refined the final hierarchy to icon, canonical answer, prominent semantic-accent translation,
  same-row IPA/POS, rating transition, and explanation.
- Added one typed, availability-aware reveal timeline: full content is visible by 265 ms, then held
  to a target 865 ms lifecycle without stacking a second dwell after answer audio.
- Final layout is composed from the first frame; graphics-layer opacity, scale, and movement of at
  most 3 dp avoid layout jumps while the root live-region exposes all semantics immediately.
- Missing translation or lexical metadata removes and compresses its stage deterministically.
  Integrated Desktop UAT remains pending.

# LQ-006A — Cross-Platform Recall Mode Contract

- Added stable typed recall modes, directions, outcomes, assistance, provenance, capabilities, and
  evidence-eligibility categories.
- Added immutable plan, sealed prompt/submission, answer, platform requirement, and passive result
  contracts using existing learner, Content, LearningItem, session, and time identities.
- Added Content-derived capability projection, pure plan/submission validation, duplicate-attempt
  recognition, reveal/practice evidence exclusion, typed seed/clock, schema versioning, and a
  deterministic JSON-ready wire codec with typed unsupported-version handling.
- Added 2 focused suites / 26 portability and contract tests; full clean verification passes 598
  suites / 3,163 tests. No Desktop wiring or recall execution is included.
# LQ-006B — Cross-Platform Content Capability Resolver

- Added a pure ContentId-owned projection covering all LQ-006A recall modes with supported
  directions or deterministic typed unavailable reasons.
- Added immutable capability sets, typed media/lexical/context/assistance facts, valid opaque-media
  checks, and safe unique token-boundary example-completion spans.
- Added stable versioned projection serialization and cross-platform dependency-boundary coverage.
- Adaptive mode choice, distractor generation, client UI, learner state, evidence, scheduling, and
  persistence remain out of scope.
- Focused verification passes 4 suites / 53 tests; full clean verification passes 600 suites /
  3,190 tests with zero failures, errors, or skipped tests.
# LQ-007A — Desktop Recall Pipeline Integration

- Added `LearningEngine` composition APIs for recall-plan creation, recall execution, and learning
  execution through the existing transactional use cases.
- Desktop Typing now renders its canonical answer from `RecallPlan`, submits typed/reveal attempts
  as `RecallSubmission`, consumes `RecallResult`, and refreshes from the bridge commit result.
- Preserved popup, Practice, Undo, Scheduler/FSRS, evidence, queue, rating, Learning Insight, and
  UX-011 behavior. Other recall modes are unchanged.
# UX-012 — Practice Review Experience Refinement

- Increased the Discovery Front Card Vietnamese translation by 19% and tightened its image gap.
- Removed Practice answer-ready/typed-answer status space, compacted expanded Example content, and
  reduced Practice feedback controls to 30 dp while preserving colors and shortcuts `[1]`–`[4]`.
- Renamed the Practice SRS and exit actions without changing Practice, rating, queue, Recall,
  Scheduler/FSRS, evidence, Learning Insight, or non-Practice popup behavior.
# LQ-007B — Graduated In-Session Reinforcement Spacing

- Replaced fixed Coverage Review Again/Hard insertion offsets with an injected typed policy and
  persisted immutable per-item reinforcement state.
- Added graduated Again gaps 2/8/20/40 and Hard gaps 4/12/30, rating-specific maximums, and
  near-end defer instead of compressed repetition.
- Added schema-v6 persistence, legacy empty-state compatibility, deterministic restart, and Undo
  restoration of state plus completion-discarded queue tails. Practice, Scheduler/FSRS, Evidence,
  Recall, and Desktop UI remain unchanged.
# LQ-007C — Desktop Multiple Choice Runtime

- Added mode-based Desktop recall routing with a dedicated Multiple Choice scene and explicit
  unsupported-mode presentation boundary; Typing behavior is unchanged.
- Added responsive plan-owned option rendering, keys 1-4, click input, deterministic focus and
  accessibility descriptions. One shared submission gate blocks double-click and click/key races.
- Desktop submits only stable choice identity to `RecallExecutionEngine`, then routes the typed
  result through `RecallLearningExecutionBridge` and the existing evaluative/practice lifecycle.
  No Desktop correctness, rating, Scheduler/FSRS, Evidence, transaction, or queue policy was added.
