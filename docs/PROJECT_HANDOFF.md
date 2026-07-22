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
robustness, and persistence-integrity tracks are complete at their verified boundaries. The
product is entering **Desktop Beta release readiness**.

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

Milestone 6, **Desktop Beta release readiness**, is next. Begin with stable application
identity, version/build metadata, and data-directory behavior, followed by diagnostics,
packaging, onboarding, clean-machine verification, and the release checklist. See
[`ROADMAP.md`](ROADMAP.md) for scope and status.

## Completed Milestones

- Learning engine and persistence foundations.
- Desktop end-to-end learning flow.
- Desktop UX, keyboard, and accessibility hardening at the Beta-test boundary.
- Search and discovery through Unicode-robust multi-term matching.
- Package Import & OPD3 Robustness.
- Persistence Integrity & Recovery.
- Workflow Foundation Refinement (this documentation increment).

Official completion records belong in [`MILESTONE_HISTORY.md`](MILESTONE_HISTORY.md); detailed
capability history belongs in [`CHANGELOG.md`](CHANGELOG.md).

## Current Milestone

Desktop Runtime Foundation is active. Stable application identity, generated version/build
metadata, and platform-aware data/config/cache/log/temp resolution are delivered. Existing
legacy data remains in place. Typed runtime configuration rejects corrupt files without
rewriting them. Typed per-session file logging and bounded retention are delivered. Desktop
startup/shutdown now owns deterministic directory/config/log/application composition and
cleanup. Runtime diagnostics and About/support presentation are the remaining boundary;
installer and distributable packaging remain out of scope for this milestone.

## Technical Debt

- No crash journal exists for multi-file JSON transactions.
- Non-atomic replacement fallback has weaker crash guarantees on unsupported filesystems.
- No approved backup, quarantine, restore, or recovery-manifest policy exists.
- Stale JSON temporary artifacts are intentionally inert and may accumulate after crashes.
- Persistence supports legacy arrays and envelope v1 but no general migration framework.
- Multi-file transaction snapshots allocate complete managed files in memory.
- Release-ready logging, diagnostic export, distributables, and clean-machine smoke evidence
  remain incomplete.
- Large real-package and UI-allocation evidence remains measurement-driven follow-up work.

## Definition of Done

A milestone is done when every agreed capability is implemented through its real boundaries,
compatibility and data integrity are resolved, required tests and builds pass, owned documents
match committed behavior, completion is recorded in milestone history, and Git is in the
requested state. The authoritative operational checklist is in [`../AGENTS.md`](../AGENTS.md).

## Source of Truth Order

Use the order defined in [`../AGENTS.md`](../AGENTS.md): clean repository source/tests first,
then the standing working agreement, architecture/roadmap, this handoff, current AI context,
and finally historical records. Chat is never durable project memory.
