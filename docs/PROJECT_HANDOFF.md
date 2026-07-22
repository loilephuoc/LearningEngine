# Learning Engine 2.0 — Strategic Project Handoff

This document is the concise durable handoff for product and architecture continuity. Standing
AI workflow rules live only in [`../AGENTS.md`](../AGENTS.md).

## Product Vision

Build an adaptive learning platform—not merely a flashcard application. Flashcard-based spaced
repetition is the first capability, grounded in retrieval practice and scheduling science, while
the architecture must support richer evidence-backed learning forms over time. The first
release target remains Desktop; mobile and Web remain deferred until shared engine contracts
and Desktop behavior are stable.

**North Star:** Every design decision must measurably improve the learner's ability to learn,
remember, and stay motivated.

Product decisions prioritize learner outcomes and learning science, keep domain behavior
independent from UI, adapt rather than remain static, reveal complexity progressively, use
evidence, let AI augment rather than replace people, preserve long-term maintainability and
safe migration/rollback, and treat delight as part of product quality.

## Product Phase

The functional import-to-persisted-study flow and its robustness/runtime/UX foundations are
complete at their verified boundaries. **Phase 5 — Desktop Beta Readiness** is implementation
complete but still awaits external clean-machine verification. **Phase 6 — Learning
Experience** is active; its lifecycle/recovery foundation is complete.

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

Phases 1–4 are complete. Phase 5 retains its external verification gate. Phase 6 turns the
verified Desktop learning foundations into a complete daily Learning Experience; Phase 7 owns
Beta validation/Desktop v1, and additional platforms remain deferred to Phase 8. See
[`ROADMAP.md`](ROADMAP.md).

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

Phase 5 remains open only for Product Owner clean-machine/install/upgrade/signing evidence.
Phase 6 is active so learning-experience implementation can proceed without erasing
that independent release gate.

## Current Capability

Windows MSI/EXE packaging, an unpacked application image, and privacy-preserving diagnostic
export are delivered locally without publishing, signing, installation, or data migration.
Manual durable-state backup and whole-snapshot restore are delivered with manifest/checksum
validation, pre-restore safety backup, rollback, active-session exclusion, and restart after
success. Empty profiles receive localized first-run onboarding with an optional starter lesson
imported through the production OPD3 boundary; existing profiles are not prompted. The next
work package is reproducible Windows distribution and clean-machine verification evidence.
The repository includes a reproducible Windows harness and Beta checklist. Local clean
test/package, artifact hash/signature reporting, and Unicode writable-path probes pass. The
remaining Phase gate requires Product Owner execution on a disposable clean Windows machine,
including install/launch/primary flow/recovery/uninstall/reinstall and approved prior-MSI
upgrade evidence; local execution cannot honestly substitute for that environment. P6-01 has
defined Learning Experience scope and constraints. The next implementation capability is
**P6-04 — Rich learning-content rendering**. P6-02 established durable lifecycle checkpoints;
P6-03 established the deterministic Desktop Review Workspace projection and action boundary.
Restart restores Question or Answer Revealed, while application recovery resolves any pending
review intent before projection.

## Phase Definition of Done

Phase 5 retains the external checklist in [`BETA_RELEASE_CHECKLIST.md`](BETA_RELEASE_CHECKLIST.md).
Phase 6 outcomes, sequence, open decisions, and exit criteria are owned by
[`ROADMAP.md`](ROADMAP.md#phase-6--learning-experience). Every capability must also satisfy
[`../AGENTS.md`](../AGENTS.md).

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
