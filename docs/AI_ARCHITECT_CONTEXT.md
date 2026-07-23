# Learning Engine 2.0 — AI Architect Context

Short-term repository and Phase snapshot only. Standing workflow is defined in
[`../AGENTS.md`](../AGENTS.md).

## Repository

- Repository: `loilephuoc/LearningEngine`
- Branch: `develop`
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

- Platform-Independent Learning Product Specification gate:
  `gradlew.bat clean test :desktop:compileKotlin --no-daemon` passed 1,582 tests with 0
  failures/errors/skipped; Desktop Kotlin compilation passed. Only Markdown changed.

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
