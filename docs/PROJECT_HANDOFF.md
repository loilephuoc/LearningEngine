# Learning Engine 2.0 — Strategic Project Handoff

This document is the concise durable handoff for product and architecture continuity. Standing
AI workflow rules live only in [`../AGENTS.md`](../AGENTS.md).

## Product Vision

Build a dependable learning platform grounded in retrieval practice and scheduling science,
with richer content structure, lesson scope, feedback, discovery, recovery, and learner
control than repetitive Anki-style drilling. The first release target is Desktop Beta; mobile
and Web remain deferred until shared engine contracts and Desktop behavior are stable.

## Product Phase

The functional import-to-persisted-study flow, Desktop accessibility/search hardening, package
robustness, persistence integrity, Desktop Runtime Foundation, and Desktop UX Foundation are
complete at their verified boundaries. The product is in **Phase 5 — Desktop Beta Readiness**.

## Architecture Overview

Learning Engine uses Kotlin/JVM 21, Gradle, kotlinx.serialization, and two modules:

- Root: domain, application ports/workflows, infrastructure, persistence/import adapters, JVM
  entry points, and tests.
- `desktop`: Compose Desktop presentation, accessibility, navigation, and composition wiring;
  it depends on the root module.

Dependency direction is Desktop/JVM adapters → application ports/use cases → domain. Concrete
infrastructure implements application ports. Durable technical decisions live in
[`ARCHITECTURE.md`](ARCHITECTURE.md).

## Domain Overview

- Content, structured text, media, libraries, collections, packages, catalogs, dependencies,
  validation, import, upgrade, and uninstall.
- Learning items, lesson-scoped selection, study queues/policies, and study-session lifecycle.
- Memory state, review events, ratings, learning stages, forgetting behavior, and FSRS
  scheduling.
- Progress, review history, dashboard, statistics, analytics, search, and Desktop presentation.

## Current Roadmap

Phases 1–4 are complete. Phase 5 prepares an installable, supportable, recoverable Desktop Beta;
Phase 6 validates that Beta and establishes Desktop v1; additional platforms remain deferred
to Phase 7. See [`ROADMAP.md`](ROADMAP.md).

## Completed Milestones

- Learning engine and persistence foundations.
- Desktop end-to-end learning flow.
- Desktop UX, keyboard, and accessibility hardening at the Beta-test boundary.
- Search and discovery through Unicode-robust multi-term matching.
- Package Import & OPD3 Robustness.
- Persistence Integrity & Recovery.
- Workflow Foundation Refinement (this documentation increment).
- Desktop Runtime Foundation.
- Desktop UX Foundation.

Official completion records belong in [`MILESTONE_HISTORY.md`](MILESTONE_HISTORY.md); detailed
capability history belongs in [`CHANGELOG.md`](CHANGELOG.md).

## Current Phase

**Phase 5 — Desktop Beta Readiness** is current. It ends only when a clean Windows environment
can exercise an approved versioned distribution, privacy-safe diagnostic export, approved user
data recovery, first-run entry into the learning flow, and the documented Beta release checks.

## Current Capability

Windows MSI/EXE packaging, an unpacked application image, and privacy-preserving diagnostic
export are delivered locally without publishing, signing, installation, or data migration.
Manual durable-state backup and whole-snapshot restore are delivered with manifest/checksum
validation, pre-restore safety backup, rollback, active-session exclusion, and restart after
success. Empty profiles receive localized first-run onboarding with an optional starter lesson
imported through the production OPD3 boundary; existing profiles are not prompted. The next
work package is reproducible Windows distribution and clean-machine verification evidence.
The repository now includes a reproducible Windows harness and Beta checklist. Local clean
test/package, artifact hash/signature reporting, and Unicode writable-path probes pass. The
remaining Phase gate requires Product Owner execution on a disposable clean Windows machine,
including install/launch/primary flow/recovery/uninstall/reinstall and approved prior-MSI
upgrade evidence; local execution cannot honestly substitute for that environment.

## Phase Definition of Done

The authoritative Phase 5 checklist is in [`ROADMAP.md`](ROADMAP.md). In addition to those
product outcomes, completion requires every capability to satisfy the implementation, test,
documentation, compatibility, Git, and evidence rules in [`../AGENTS.md`](../AGENTS.md).

## Technical Debt

- No crash journal exists for multi-file JSON transactions.
- Non-atomic replacement fallback has weaker crash guarantees on unsupported filesystems.
- Recovery is manual and local only; cloud, scheduling, cross-device merge, and automatic
  retention remain intentionally unsupported.
- Stale JSON temporary artifacts are intentionally inert and may accumulate after crashes.
- Persistence supports legacy arrays and envelope v1 but no general migration framework.
- Multi-file transaction snapshots allocate complete managed files in memory.
- Clean-machine smoke evidence remains incomplete; distributables are locally buildable but not
  yet signed or clean-machine verified.
- Large real-package and UI-allocation evidence remains measurement-driven follow-up work.

## Definition of Done

A Phase is done only when its roadmap Definition of Done is fully evidenced through real
boundaries and every included capability satisfies [`../AGENTS.md`](../AGENTS.md). A passing
build or one completed capability does not complete a Phase.

## Source of Truth Order

Use the order defined in [`../AGENTS.md`](../AGENTS.md): clean repository source/tests first,
then the standing working agreement, architecture/roadmap, this handoff, current AI context,
and finally historical records. Chat is never durable project memory.
