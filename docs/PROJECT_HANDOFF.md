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
Experience** is implementation complete through P6-09; representative manual verification is
still pending.

Desktop 1.0 continuation is now bounded only by Phase 6 manual verification, Phase 7
manual/real-user validation, and the still-open external evidence from Phase 5. The
repository—not chat history—is sufficient to resume this work.

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
Phase 6 implementation is complete. Phase 7 is at the manual/external validation gate without
erasing the independent Phase 5 distribution evidence gate.

## Current Capability

**Desktop 1.0 release-candidate preparation** is complete. The final repository audit found and
fixed one recovery-integrity defect: a negative backup manifest file count could be interpreted
as an empty snapshot. Validation now rejects any negative or archive-mismatched declared count
before safety-backup creation or mutation.

Automated release-path evidence crosses persisted OPD3 import and real Desktop composition, and
the local test/compile/package gates are complete. Product Owner manual, real-user, clean-machine,
installer, upgrade/uninstall, and signing evidence remains pending; none is represented as passed.

## Desktop 1.0 Continuation

Completed Phase 6 capabilities:

- P6-01 — Phase 6 Definition;
- P6-02 — Learning Session Lifecycle & Recovery Contract;
- P6-03 — Review Workspace State & Action Boundary;
- P6-04 — Learning Content Model;
- P6-05 — Rich Content Renderer;
- P6-06 — Progress, Completion & Learning Feedback.
- P6-07 — Pause, Resume, One-Step Undo & Safe Interruption.
- P6-08 — Desktop Accessibility, Keyboard Navigation, Error Recovery & Release Polish.
- P6-09 — Desktop End-to-End Verification, Defect Remediation & Release Evidence.

Remaining before Desktop 1.0:

- Phase 7 manual/real-user validation, external release evidence, and Desktop 1.0 approval;
- Product Owner clean-machine install/launch/upgrade/uninstall/reinstall and signing evidence
  retained from Phase 5.

## Stable Desktop 1.0 Boundaries

Unless a concrete defect or accepted use case proves otherwise, Desktop 1.0 treats the Learning
Session lifecycle, Review Workspace state/actions, Learning Content, rich-content renderer, and
session progress/completion contracts as stable. A change must identify the defect/use case,
assess compatibility, add focused and regression coverage, and update the owning architecture
documentation.

Authoritative ownership remains:

- Domain: `StudySession` lifecycle, current item, reveal state, and pending review intent.
- Application: orchestration, atomic review transaction, scheduler interaction, queue/session
  progress projection, and recovery.
- Desktop: workspace projection, rendering, temporary feedback, focus, and presentation state.

Desktop must not own scheduling, durable lifecycle, persistence transactions, or durable
progress counts.

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
- Desktop retains compatibility boolean/string projections while consumers migrate to explicit
  workspace, content, and progress contracts.
- Core learning actions and accessibility labels are localized; legacy explanatory/metric copy
  still needs broader product-copy localization after Desktop 1.0.
- Java Sound codec availability varies; guaranteed MP3 playback is not a Desktop 1.0 promise.
- Compose does not yet have a stable UI-test harness for every visual behavior.
- Legacy sessions without a persisted queue have an unknown progress denominator.
- Individual queue-skip reasons are not persisted.
- Desktop 1.0 still requires representative real-user/manual verification.

## Repository Self-Onboarding

Start with [`../AGENTS.md`](../AGENTS.md) for the authoritative working agreement, then read
[`ARCHITECTURE.md`](ARCHITECTURE.md), [`ROADMAP.md`](ROADMAP.md), this strategic handoff, and
[`AI_ARCHITECT_CONTEXT.md`](AI_ARCHITECT_CONTEXT.md). Use [`CAPABILITY_MAP.md`](CAPABILITY_MAP.md)
to locate source, [`TEST_MATRIX.md`](TEST_MATRIX.md) to select verification, and
[`CHANGELOG.md`](CHANGELOG.md) plus Git history for committed capability evidence. Chat is not
durable project memory.

## Definition of Done

A Phase is done only when its roadmap Definition of Done is fully evidenced through real
boundaries and every included capability satisfies [`../AGENTS.md`](../AGENTS.md). A passing
build or one completed capability does not complete a Phase.

## Source of Truth Order

Use the order defined in [`../AGENTS.md`](../AGENTS.md): clean repository source/tests first,
then the standing working agreement, architecture/roadmap, this handoff, current AI context,
and finally historical records. Chat is never durable project memory.
