# Learning Engine 2.0 — Agent Guide

This file is the standing operating guide for Codex agents working in this repository.
Follow it together with the actual source, tests, and canonical documents. When guidance
conflicts with a clean repository HEAD, the source and tests at that HEAD are authoritative;
correct stale documentation in the same capability increment.

## Product vision

Learning Engine 2.0 is a dependable, reusable learning platform intended to provide a richer,
more purposeful learning experience than repetitive Anki-style drilling. It remains grounded
in retrieval practice and scheduling science while improving content structure, lesson scope,
feedback, discovery, recovery, and learner control. The product follows a Desktop-first
strategy. The immediate objective is a usable Desktop Beta that can complete this real-data
workflow safely:

```text
Launch Desktop
→ import and attach a real OPD3 package
→ browse content and select a lesson
→ start a lesson-scoped study session
→ reveal and grade learning items
→ persist progress, queue, and scheduling state
→ recreate the application
→ resume and complete the same lesson correctly
```

Desktop is the active release target. Android, iOS, and Web are deferred until the shared
engine contracts and Desktop behavior are stable. Do not introduce speculative abstractions
solely for future platforms.

Product work is prioritized in this order:

1. prevent data loss and corruption;
2. preserve correct learning, review, and scheduling behavior;
3. keep the end-to-end Desktop learning flow unblocked;
4. handle malformed, incompatible, and partial real-world data safely;
5. close Desktop Beta release blockers;
6. address measured performance and allocation problems;
7. improve usability and accessibility based on evidence;
8. add optional polish.

## Repository orientation

The Gradle build has two modules:

- Root project: domain model and services, application use cases and ports, infrastructure,
  persistence adapters, JVM adapters and CLI entry points, and their tests.
- `desktop`: Compose Desktop presentation state, screens, components, accessibility models,
  and Desktop composition wiring. It depends on the root project.

Preserve the dependency direction:

```text
Desktop / JVM adapters
        ↓
Application use cases and ports
        ↓
Domain model and domain services

Infrastructure implements application ports.
```

Domain and application code must not depend on Compose Desktop, filesystem APIs, or concrete
JSON persistence. A feature is complete only when every required layer is connected; do not
add disconnected placeholders or abstractions that have no real consumer.

## Required context before work

At the start of a capability, verify the branch, HEAD, and worktree. Preserve any pre-existing
user changes and never assume an unrelated dirty file belongs to the current task.

Read context in this order:

1. `docs/PROJECT_HANDOFF.md`;
2. `docs/CAPABILITY_MAP.md`;
3. `docs/ROADMAP.md`;
4. `docs/TEST_MATRIX.md`;
5. `docs/BATCH_PLANNING.md`;
6. the complete production and test neighborhood for the selected capability;
7. relevant composition roots and persistence adapters;
8. `docs/ARCHITECTURE.md` for durable boundaries or compatibility decisions;
9. `docs/CHANGELOG.md` for historical detail.

Do not read or modify the whole repository by default. Follow imports and call sites until the
complete affected boundary is understood, then stop expanding scope.

## Capability and batch planning

One batch equals one coherent, testable capability. File count is not a target; use the
smallest complete vertical slice. Before editing, determine:

```text
Capability
User-visible, correctness, or safety outcome
Nearest milestone
Expected baseline
Affected boundaries and composition roots
Required production files and tests
Compatibility and data-safety risks
Explicitly out-of-scope work
Proposed commit message
```

Do not split work by individual class when that leaves an incomplete workflow. Split only at
a natural compatibility boundary, and keep every intermediate commit buildable and useful.

## Coding conventions

- Use Kotlin conventions already established in the surrounding package.
- Prefer immutable domain objects and value objects for identifiers and validated values.
- Put business rules in domain or application boundaries, not Compose screens or storage
  adapters.
- Keep application ports independent of JVM and persistence implementation details.
- Use constructor injection and explicit composition roots; avoid hidden global state.
- Prefer deterministic ordering, stable identifiers, and deterministic diagnostics.
- Make invalid states fail at the earliest appropriate boundary, before persistence begins.
- Keep transaction ownership at the application workflow/use-case boundary. Operations called
  inside a transaction must not silently create their own transaction.
- Preserve repository batching and atomic writes where a workflow changes related entities.
- Reuse an existing shared contract before creating a parallel representation of the same
  concept.
- Keep public APIs stable unless the capability cannot be completed safely without changing
  them. When an API must change, update all consumers and compatibility tests together.
- Keep pure presentation logic separate from Compose where it needs deterministic unit tests.
- Do not introduce platform abstractions, frameworks, migrations, or configuration systems
  without a current product consumer.
- Comments should explain boundaries, invariants, or non-obvious tradeoffs, not restate code.
- Preserve source encoding as UTF-8 and avoid introducing mojibake into visible or semantic
  text.

## Testing rules

Every behavior change requires focused tests at the narrowest useful boundary plus regression
coverage for affected integration contracts.

Tests must cover, as applicable:

- normal success;
- boundary values and deterministic ordering;
- malformed or incompatible input;
- failure before transaction/persistence;
- atomicity and rollback behavior;
- restart and persisted-data compatibility;
- non-fail-fast batch continuation;
- stable diagnostic codes, messages, and recovery guidance;
- Desktop state, keyboard, focus, and accessibility contracts;
- real composition wiring rather than only isolated mocks.

Never rely on timing alone for performance correctness. Prefer deterministic representative
fixtures and algorithmic or allocation assertions. Add a wall-clock threshold only when the
environment is stable enough to avoid flaky tests.

The mandatory final verification for every capability is:

```powershell
.\gradlew.bat clean test
```

If compilation or tests fail, read the failure, fix the cause, and rerun `clean test`. Repeat
until `BUILD SUCCESSFUL`. Do not promote, commit, or describe a capability as complete while
any required test is failing. After verification, inspect the generated test reports when an
exact test count is needed, run `git diff --check`, and confirm `git status --short` contains
only the intended capability.

## Documentation rules

Documentation must describe actual verified behavior, not planned implementation presented as
complete. Every completed capability must review and update `docs/ARCHITECTURE.md`,
`docs/CHANGELOG.md`, `docs/PROJECT_HANDOFF.md`, and `docs/ROADMAP.md` so they remain consistent;
an architecture file may receive only a concise confirmation when no durable boundary changes.
Update other documents when their responsibilities apply:

- `docs/PROJECT_HANDOFF.md`: concise current baseline, latest completed increment, current or
  immediate next capability, and operational continuation context. Do not accumulate detailed
  history here.
- `docs/ROADMAP.md`: milestone-level status, delivered boundaries, and remaining capability
  areas. Do not turn it into a per-file changelog.
- `docs/CHANGELOG.md`: detailed record of each verified capability and its compatibility or
  safety outcome.
- `docs/ARCHITECTURE.md`: durable dependency, transaction, persistence, import, validation, or
  presentation boundaries only.
- `docs/CAPABILITY_MAP.md`: update when a source neighborhood or direct dependency boundary is
  discovered or changed.
- `docs/TEST_MATRIX.md`: update when the minimum verification contract changes.
- `docs/BATCH_PLANNING.md`: update only when the standing delivery process changes.

Source and tests remain authoritative. If documentation disagrees with the clean HEAD, correct
the stale document as part of the next appropriate batch.

## Backward compatibility and data safety

Persisted data, package formats, public APIs, diagnostic codes, and user-visible recovery
contracts are product contracts.

- Preserve existing JSON records and OPD3/legacy package compatibility unless an explicit
  migration or rejection policy is included in the same batch.
- A persisted schema change requires mapping, compatibility or migration behavior, rollback
  considerations, and restart tests in the same capability.
- Never silently discard, rewrite, or partially persist incompatible user data.
- Validate external input before opening the candidate's repository transaction whenever
  possible.
- Keep all related writes inside the established transaction boundary.
- A failed package candidate must not persist partial libraries, contents, learning items,
  packages, catalogs, queues, sessions, reviews, or media references.
- Preserve successful candidates in non-fail-fast directory imports and continue reporting
  later candidates independently.
- Preserve stable diagnostic category/code behavior and legacy message fields. Add structured
  metadata rather than requiring UI code to parse human-readable messages.
- Maintain deterministic package identity, dependency validation, ordering, and restart
  behavior.
- When compatibility cannot be preserved, stop and request a product decision before changing
  the contract.

## Git workflow and commits

The canonical development branch is `develop`. Never work directly on `main`.

Before editing:

- verify the expected branch and HEAD;
- verify the worktree state;
- identify and preserve unrelated user changes.

After implementation:

1. run the full `clean test` gate until successful;
2. run `git diff --check`;
3. inspect `git diff`, `git diff --stat`, and `git status --short`;
4. update the handoff and other required docs;
5. stage only files belonging to the capability;
6. create one intentional commit for the coherent capability;
7. verify the resulting commit and worktree before starting another capability.

Commit messages should be concise, imperative, and capability-oriented, for example:

```text
feat: validate OPD3 archive structure before entry reads
fix: preserve persisted queue during restart recovery
test: cover package import transaction rollback
docs: clarify Desktop Beta verification boundary
```

Do not mix unrelated cleanup into a capability commit. Do not amend, squash, reset, force-push,
or rewrite user history unless explicitly requested. Never push unless the user explicitly
authorizes pushing. A successful local commit is not permission to push.

## When an agent may stop

An implementation task may stop only when one of these conditions is true:

- the requested capability or milestone is genuinely complete;
- all required source, tests, wiring, and documentation are finished;
- the mandatory full test gate reports `BUILD SUCCESSFUL`;
- Git state contains only the intended delivered changes or commits;
- the requested final report has been provided;
- progress is blocked by a decision or authority that cannot be inferred safely.

Do not stop merely because one test failed, the change is large, context is long, or the next
step requires investigation. Diagnose, iterate, and continue. When asked to complete multiple
capabilities or a milestone, commit each verified capability separately and continue without
waiting unless a genuine decision boundary is reached.

Passing tests alone is never sufficient grounds to declare completion. The capability must
also be fully implemented through its real composition boundary, preserve compatibility and
data integrity, include focused regression coverage, have accurate documentation, and leave
Git in the intended state.

## When an agent must ask the user

Ask for direction before proceeding when:

- two plausible product behaviors have materially different user-visible or data-safety
  outcomes and the repository does not establish a preference;
- completing the task requires deleting, rewriting, migrating, or irreversibly changing user
  data without an existing approved policy;
- a backward-incompatible package, persistence, public API, or diagnostic change is required;
- unrelated user changes directly conflict with the required files and cannot be preserved;
- the requested operation requires external credentials, release authority, publishing,
  pushing, or coordination not already authorized;
- the expected baseline or branch differs in a way that makes the requested patch unsafe;
- milestone completion criteria are genuinely undefined and choosing them would materially
  change product scope rather than merely select the next evidence-backed robustness gap.

Do not ask for routine implementation choices that can be resolved from source, tests,
architecture, or established conventions. State reasonable low-risk assumptions and proceed.

## Technical priorities by milestone

For the current real-data robustness work, prefer:

1. package and archive integrity before parsing;
2. bounded reads, entry counts, total sizes, and allocation safety;
3. strict encoding and deterministic JSON/schema rejection;
4. manifest, metadata, dependency, and engine-version compatibility;
5. actionable per-candidate diagnostics with no partial persistence;
6. corrupt or interrupted persistence recovery;
7. deterministic representative large-package behavior;
8. real-data regression fixtures.

For Desktop Beta release readiness after robustness is complete, prioritize distributable
packaging, version/build metadata, stable data-directory behavior, logs and diagnostic export,
backup/recovery, first-run content, clean-machine smoke testing, Windows permission/Unicode
paths, and a documented release checklist.

Accessibility, keyboard operation, deterministic restart, lesson isolation, transaction
atomicity, and scheduling correctness remain cross-cutting non-regression requirements for all
future work.

## Reporting language

All progress updates, handoff summaries, implementation reports, test results, Git reports,
known limitations, and questions addressed to the user must be written in Vietnamese. Source
code, identifiers, commit messages, and repository documentation should continue using the
language and conventions established in their surrounding files.
