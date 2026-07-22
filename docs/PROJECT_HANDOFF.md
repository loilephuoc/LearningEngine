# Learning Engine 2.0 — Strategic Project Handoff

This document is the durable strategic handoff for Chief Architects and future AI sessions.
Keep it concise. Update it only when product strategy, architecture, roadmap, or milestone
state changes. Operational session state belongs in `AI_ARCHITECT_CONTEXT.md`.

## Product vision

Build Learning Engine 2.0 into a dependable learning platform that combines retrieval practice
and scheduling science with richer content structure, lesson scope, feedback, discovery,
recovery, and learner control. The product should be more purposeful than repetitive
Anki-style drilling while remaining deterministic, inspectable, and safe with user data.

The first release target is a usable Desktop Beta. Mobile and Web clients are deferred until
the shared engine contracts and Desktop behavior are stable.

## Product principles

1. Data safety and correct learning behavior outrank feature volume.
2. Real user workflows must work end to end, including restart and recovery.
3. External packages and persisted records are untrusted inputs and must fail safely.
4. Scheduling, queue selection, diagnostics, and ordering must be deterministic.
5. Accessibility and keyboard operation are product behavior, not optional polish.
6. Add abstractions only for a current consumer; do not design prematurely for future clients.
7. A capability is complete only when implementation, wiring, tests, and documentation agree.

## Current product phase

The product is in **Real-data robustness**, preparing for **Desktop Beta release readiness**.
The Package Import & OPD3 Robustness track is complete. The next proposed track is
**Persistence Integrity & Recovery**; no capability from that track is implemented by this
documentation commit.

## Architecture overview

Learning Engine uses Kotlin/JVM 21, Gradle, kotlinx.serialization, and a layered DDD-style
architecture:

```text
Compose Desktop / JVM adapters
              ↓
Application use cases and ports
              ↓
Domain models and services

Infrastructure implements application ports.
```

Composition roots construct in-memory or JSON-backed repositories explicitly. Domain and
application code remain independent of Compose, filesystem APIs, and concrete JSON stores.

## Module boundaries

- Root module: domain, application workflows and ports, infrastructure, JSON persistence,
  OPD3/legacy import adapters, JVM CLI entry points, and automated tests.
- `desktop`: Compose Desktop shell, navigation, presentation state, accessibility models,
  screens, and Desktop wiring. It depends on the root module.

Do not move Desktop concerns into the engine or concrete persistence details into domain and
application layers.

## Main domain model

- Content: `Content`, structured text, metadata, media, content types, libraries, and library
  collections.
- Packaging: `ContentPackage`, `PackageDescriptor`, dependencies, package catalogs, import,
  validation, registration, upgrade, uninstall, and integrity boundaries.
- Learning: `LearningItem`, learning modes, lesson-scoped selection, study queues, policies,
  and `StudySession` lifecycle.
- Memory and review: `MemoryState`, `ReviewEvent`, ratings, learning stages, forgetting curves,
  and interval solving.
- Scheduling: FSRS state/configuration/parameters, scheduler decisions, validation,
  diagnostics, and metrics.
- Read models: progress, review history, dashboard, statistics, analytics, and search.

## Important architectural decisions

- Persisted JSON supports legacy top-level arrays and schema-versioned envelope format v1.
- New JSON writes use UTF-8 temporary files, force file contents, prefer atomic replacement,
  and synchronize the parent directory where supported.
- Multi-file application transactions snapshot managed files and restore them on an in-process
  failure; the original error is preserved and rollback failures are suppressed onto it.
- Study session, queue, memory, and review writes share the persisted transaction boundary.
- Package candidates are parsed and validated before their repository transaction. Detailed
  directory import is non-fail-fast and preserves successful candidates.
- Modern OPD3 validates archive structure and resource limits before required JSON reads, then
  enforces strict UTF-8, schema/identity/integrity/content/dependency validation, and stable
  diagnostics.
- Pure Desktop presentation models own testable keyboard, accessibility, and error wording
  where behavior must remain independent of Compose instrumentation.

## Compatibility principles

- Persisted JSON, legacy arrays, OPD3/legacy package formats, public application contracts,
  diagnostic codes, and established user-facing messages are product contracts.
- Preserve compatibility by default. A breaking change requires an explicit migration or
  rejection policy, rollback analysis, focused tests, and a product decision in the same
  capability.
- Do not silently reinterpret unsupported schema versions or malformed records.
- Do not change a public API when an internal or defaulted extension can complete the work.

## Data integrity principles

- Never overwrite known-corrupt input or partially persist a failed workflow.
- Validate before mutation and keep related writes within the established transaction boundary.
- Treat missing, blank, malformed, unsupported-version, and incompatible-record states as
  distinct when their recovery implications differ.
- Preserve the original failure and attach context rather than parsing human-readable messages.
- Recovery work must be non-destructive by default and backed by restart and failure-injection
  tests.
- A passing test suite alone does not establish data safety; verify composition, transaction,
  compatibility, and recovery boundaries explicitly.

## Desktop-first direction

The verified product path is import → browse/select lesson → start lesson-scoped study → reveal
and grade → persist → recreate the application → resume and complete. Desktop packaging,
diagnostics, onboarding, clean-machine verification, and sustained Beta testing follow the
current robustness work. Android, iOS, and Web remain deferred.

## Milestone state

Completed or verified tracks:

- learning engine, scheduling, and persistence foundations;
- functional Desktop import-to-persisted-study flow;
- substantial Desktop keyboard and accessibility hardening;
- search and discovery through Unicode-robust multi-term matching;
- Package Import & OPD3 Robustness through Batch82.

Next proposed milestone track:

- **Persistence Integrity & Recovery** within Real-data robustness.
- Start by defining safe handling for corrupt or interrupted JSON persistence without deleting
  or overwriting user data.

See `ROADMAP.md` for milestone-level status and `CHANGELOG.md` for verified batch history.

## Technical debt

- Missing persistence files initialize an empty dataset, while corrupt existing files fail with
  classified context and remain byte-for-byte unchanged across repeated store recreation.
- JSON transaction rollback protects in-process failures but is not a crash-recovery journal.
- Transaction rollback restores exact pre-state bytes, including corrupt snapshots, and a
  restarted store diagnoses that restored state consistently.
- Filesystems explicitly reporting unsupported atomic move use a replacement fallback with
  weaker crash safety; unrelated atomic-move I/O failures do not trigger that fallback.
- No durable backup, quarantine, restore, or corrupt-file recovery policy exists yet.
- Stale JSON `.tmp` files are deliberately inert and preserved; there is no automatic promotion
  or cleanup policy.
- Schema support is v1 plus legacy arrays; there is no general migration framework.
- Diagnostic export, centralized logs, and user-facing recovery tooling are not release-ready.
- Representative large-data performance evidence remains limited; do not add timing thresholds
  without stable measurements.

## Known limitations

- A corrupt persistence file is diagnosed but not automatically repaired or quarantined.
- Corrupt persistence is diagnosed but has no automatic recovery, quarantine, or user-facing
  repair workflow.
- Multi-file snapshots are held in memory during a transaction.
- Crash consistency depends partly on filesystem atomic-move support.
- Desktop distributables, backup/restore, clean-profile smoke tests, and release diagnostics are
  still planned.
- Legacy media archives use a separate reader boundary from modern OPD3 packages.

## Definition of Done

A capability is done only when:

- its product, safety, or architectural outcome is implemented through the real composition
  boundary;
- focused unit tests and appropriate integration/restart/failure tests cover the contract;
- backward compatibility and data-integrity implications are resolved;
- `.\gradlew.bat clean test` reports `BUILD SUCCESSFUL` for code changes;
- architecture, changelog, handoff, and roadmap documentation match verified behavior;
- `git diff --check` is clean and Git contains only the intended capability;
- the capability has one intentional commit and has not been pushed without authorization.

## Review checklist

- Does the change follow the actual source and dependency direction?
- Does it preserve public, persistence, package, and diagnostic contracts?
- Can any failure cause partial writes, silent reset, or loss of the original data?
- Are transaction and restart boundaries tested, not merely mocked?
- Are malformed, unsupported, empty, and missing states handled deliberately?
- Are diagnostics contextual, stable, and actionable without message parsing?
- Are deterministic ordering, keyboard, accessibility, and lesson isolation preserved?
- Do docs describe only committed and tested behavior?
- Is the worktree clean and is pushing explicitly authorized?

## Rules for Codex

- Follow `../AGENTS.md` before doing work.
- Verify branch, HEAD, upstream, and worktree before each capability.
- Read the relevant source, tests, composition roots, and canonical docs before designing.
- Never invent APIs, constructors, packages, classes, wiring, or abstractions.
- Deliver one complete capability and one commit at a time.
- Never delete, skip, weaken, or disable tests to make a build pass.
- Iterate until the required build succeeds; do not declare completion from tests alone.
- Ask only for an irreducible product decision, destructive migration/data risk, external secret
  or service, or a material architectural trade-off.
- Do not push unless explicitly requested. Report to the user in Vietnamese.

## Handoff update policy

- Update this file only for strategic, architectural, roadmap, or milestone changes.
- Update `AI_ARCHITECT_CONTEXT.md` at the end of every milestone or when a Codex session must
  stop mid-work.
- Record only Git- or test-verified state. Do not store speculation as fact.
- Do not use chat history as durable project memory.

## Source of Truth order

When information conflicts, use this order:

1. Clean Git-tracked repository state at the current HEAD: source, build files, and tests.
2. `../AGENTS.md` for standing operating rules.
3. `ARCHITECTURE.md` and `ROADMAP.md` for durable architecture and milestone intent.
4. This strategic handoff.
5. `AI_ARCHITECT_CONTEXT.md` for the latest operational snapshot.
6. `CHANGELOG.md` and Git history for verified historical detail.
7. Chat messages and external notes; these are never authoritative project memory.
