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

**Status: Active — P6-07 complete; P6-08 next**

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
8. **P6-08 — Interaction, accessibility, and recoverable errors**: consolidate keyboard-first
   actions, focus transitions, semantic announcements, localization, and error recovery across
   the completed workspace.
9. **P6-09 — End-to-end learning-flow verification**: verify representative global and
   lesson-scoped flows through import, start/resume, rich presentation, review, interruption,
   completion, persistence restart, keyboard, and accessibility boundaries.

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

## Phase 7 — Desktop Beta Validation and v1

**Status: Planned**

Outcome: validate the Beta and completed learning experience with representative real workloads
and establish the stable Desktop v1 boundary. Measure startup, import, search, queue planning,
and Study responsiveness; prioritize crashes, data loss, incompatible upgrades, and blocked
workflows; refine behavior using observed evidence rather than speculative polish.

Desktop 1.0 is reached only after P6-08 through P6-09, release-candidate defect fixing, the
Phase 5 external clean-machine/install/upgrade/signing evidence, and representative manual or
real-user verification are complete.

## Phase 8 — Additional Platforms

**Status: Deferred until Desktop v1**

Outcome: introduce Android, iOS, and Web consumers only after shared engine contracts and
Desktop v1 behavior are stable. Do not add premature cross-platform abstractions solely to
prepare for this Phase.

## Delivery references

Capability/build/Git rules are in [`../AGENTS.md`](../AGENTS.md). Historical milestone and
completed-Phase records are in [`MILESTONE_HISTORY.md`](MILESTONE_HISTORY.md); detailed verified
increments are in [`CHANGELOG.md`](CHANGELOG.md).
