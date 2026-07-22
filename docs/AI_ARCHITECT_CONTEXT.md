# Learning Engine 2.0 — AI Architect Context

Short-term repository and Phase snapshot only. Standing workflow is defined in
[`../AGENTS.md`](../AGENTS.md).

## Repository

- Repository: `loilephuoc/LearningEngine`
- Branch: `develop`
- Baseline HEAD for Desktop 1.0 release-candidate preparation:
  `d8324acd08669816278d7bbb389b9f90c94d5af4`
- Baseline `origin/develop` was at the same commit
- Baseline working tree: clean
- Continuation baseline commit message: `test: verify Desktop learning flow end to end`.

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

- Desktop 1.0 release-candidate preparation: complete after the final repository audit.
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
