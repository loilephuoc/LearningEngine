# Learning Engine 2.0 — AI Architect Context

Short-term repository and Phase snapshot only. Standing workflow is defined in
[`../AGENTS.md`](../AGENTS.md).

## Repository

- Repository: `loilephuoc/LearningEngine`
- Branch: `develop`
- Baseline HEAD for P6-01: `438366774beae097bc9486472c04e514edd8674f`
- Baseline upstream: `origin/develop` at the same commit
- Baseline working tree: clean
- No push, dependency, build, test, or production-source change belongs to P6-01.

## Phase State

- Phase 5 — Desktop Beta Readiness: implementation/local automation complete; Product Owner
  clean-machine install/launch/flow/recovery/uninstall/reinstall/upgrade/signing evidence remains
  pending in [`BETA_RELEASE_CHECKLIST.md`](BETA_RELEASE_CHECKLIST.md).
- Phase 6 — Learning Experience: formally defined by P6-01; production implementation has not
  started.
- Phase 6 definition and exit criteria:
  [`ROADMAP.md`](ROADMAP.md#phase-6--learning-experience).

## Verified Starting Boundary

- Domain `StudySession` has `ACTIVE` and `FINISHED` states, immutable lesson scope, reviewed
  item/content sets, new/due counters, limits, and finish invariants.
- Application services own start, next-item selection, atomic review, finish, queue progress,
  and persisted active-session reconciliation.
- `ActiveStudySessionRecovery` distinguishes no session, resumable session, missing queue, and
  already-completed queue.
- Desktop `StudyFacade` owns transient current item/reveal timing/title/feedback state and maps
  it into boolean-rich `StudyUiState`; `StudyScreen` owns current keyboard/focus/accessibility
  presentation.
- Persisted import-to-lesson-study, restart/resume, grading, completion, queue isolation, and
  recovery coverage already exists and must remain green.

## Current Capability

- P6-01 — Define Phase 6: Learning Experience.
- State after this commit: complete, documentation only.
- Next capability: **P6-02 — Study Session lifecycle and recovery contract**.
- P6-02 must inspect existing domain/application/persistence/Desktop call sites before deciding
  whether pause is a new persisted state or a user-facing interpretation of resumable `ACTIVE`.
- Do not begin Review Workspace or undo implementation until lifecycle authority and transaction
  semantics are explicit.

## Decision Boundaries and Risks

- Persisted session status changes require migration/compatibility and restart evidence.
- Review remains atomic across review event, memory state, session, and queue.
- Undo scope and reversal of derived scheduler state remain an open product decision for P6-06.
- Rich rendering must be limited to content/media forms proven by current contracts and real
  fixtures.
- Avoid encoding flashcard-specific screen states into general domain concepts, but do not add
  abstractions without a current use case.
- Phase 5 external verification debt must remain visible and must not be reported as complete.

## Latest Verified Test Evidence

- Product HEAD: `5edc27723a1e6fe2c53aaffeb23c98e020adeedf`
- Full local gate: 363 suites / 1,504 tests, 0 failures/errors/skipped.
- P6-01 changes Markdown only; no new product test claim is introduced.
