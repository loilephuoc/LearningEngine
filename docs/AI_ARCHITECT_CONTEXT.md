# Learning Engine 2.0 — AI Architect Context

Short-term repository and Phase snapshot only. Standing workflow is defined in
[`../AGENTS.md`](../AGENTS.md).

## Repository

- Repository: `loilephuoc/LearningEngine`
- Branch: `develop`
- Baseline HEAD for P6-03: `0da5834f1eaae7fcbbb864977eeeb0fe68989f5c`
- Baseline `origin/develop` was at the same commit
- Baseline working tree: clean
- Baseline working tree was clean; P6-03 is one Desktop implementation/test/docs commit and is
  not pushed.

## Phase State

- Phase 5 — Desktop Beta Readiness: implementation/local automation complete; Product Owner
  clean-machine install/launch/flow/recovery/uninstall/reinstall/upgrade/signing evidence remains
  pending in [`BETA_RELEASE_CHECKLIST.md`](BETA_RELEASE_CHECKLIST.md).
- Phase 6 — Learning Experience: active; P6-02 lifecycle/recovery and P6-03 Review Workspace
  foundations are complete.
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

- P6-03 — Review Workspace State & Action Boundary: complete.
- Next capability: **P6-04 — Rich learning-content rendering**.
- Pause is resume of `ACTIVE`, not a domain state. Undo is exactly one latest rating; its reversal
  belongs to P6-06 and has not been implemented by P6-02.

## Decision Boundaries and Risks

- Session schema-v1 checkpoint fields are optional/defaulted; preserve legacy JSON readability.
- Review stages one durable intent, then remains atomic across event, memory, session checkpoint,
  and queue. Recovery reuses the original event ID.
- Desktop workspace state is projection only. Keep action permission in `ReviewWorkspaceState`,
  and do not move session, scheduler, or persistence authority into Desktop.
- Rich rendering must be limited to content/media forms proven by current contracts and real
  fixtures.
- Avoid encoding flashcard-specific screen states into general domain concepts, but do not add
  abstractions without a current use case.
- Phase 5 external verification debt must remain visible and must not be reported as complete.

## Latest Verified Test Evidence

- P6-03 baseline HEAD: `0da5834f1eaae7fcbbb864977eeeb0fe68989f5c`.
- Full local gate: `gradlew.bat clean test --no-daemon`, BUILD SUCCESSFUL; 370 suites / 1,536
  tests, 0 failures/errors/skipped.
