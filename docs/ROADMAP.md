# Roadmap

The source and tests are authoritative. This roadmap describes product milestones, not a
line-by-line history of every batch. Detailed completed increments belong in `CHANGELOG.md`.

## Milestone 1 — Learning engine and persistence foundations

**Status: Completed foundation**

Delivered foundations include:

- learning and review domain behavior;
- memory state and scheduling;
- queue planning and study-session selection;
- JSON persistence, repositories, stores, and transactions;
- review history, progress, dashboard, statistics, and analytics;
- content-package import, registration, querying, and content-library workflows.

## Milestone 2 — Desktop end-to-end learning flow

**Status: Completed functional boundary**

Verified flow:

```text
Import real OPD3 package
→ browse and select a lesson
→ start a lesson-scoped session
→ persist and recreate the application
→ resume the same lesson queue
→ reveal and grade
→ persist scheduler and completion state
```

This milestone also includes recoverable persisted-session reconciliation and isolation
between lessons.

## Milestone 3 — Desktop UX, keyboard, and accessibility hardening

**Status: Substantially completed**

Delivered work includes:

- state-aware Study keyboard operation;
- stable focus across Study transitions;
- accessible prompt, answer, progress, rating guidance, and scheduler feedback;
- recoverable loading and failure states;
- semantic navigation, screen summaries, metrics, charts, dialogs, cards, and actions;
- Content Library keyboard navigation;
- shell-wide destination and refresh shortcuts;
- stale-data preservation during refresh failures.

Remaining work in this area should be driven by Beta testing, not by isolated speculative
polish.

## Milestone 4 — Search and discovery

**Status: Completed through Batch75**

Delivered work includes:

- shared normalized search contracts;
- Lesson Browser and Review History query, filter, and sort projections;
- one-action reset and individual active-refinement removal;
- actionable empty-result recovery;
- Ctrl+F focus and progressive Escape recovery;
- accessible result status and option groups;
- visible match highlighting;
- searchable-field and active-scope disclosure;
- contextual placeholders and short-query guidance;
- multi-term AND matching with per-term highlighting;
- canonical Unicode and compatibility-width matching with original-text highlight ranges.

Future search changes should address measured correctness, Unicode, or large-data issues.

## Milestone 5 — Real-data robustness

**Status: Completed robustness tracks through Batch88**

Delivered:

- structured package-import failure categories and stable diagnostic codes;
- preserved validation issue codes and actionable recovery guidance;
- non-fail-fast candidate reporting without changing successful-package commits;
- bounded OPD3 JSON entry reads with strict UTF-8 validation;
- pre-read OPD3 archive structure validation for unsafe, duplicate, ambiguous, or excessive
  entries across descriptor and bundle-content paths;
- a metadata-only 512 MiB default budget for total declared OPD3 uncompressed size, enforced
  before required JSON reads;
- one first-class missing required-entry exception contract with preserved legacy messages for
  descriptor, modern bundle, and legacy content paths;
- structured required-entry context for malformed OPD3 JSON with preserved parser messages and
  stable malformed-package diagnostics;
- contextual rejection of invalid required JSON value shapes, including optional metadata
  fields, without narrowing backward-compatible metadata omission.
- classified corrupt JSON persistence reads that distinguish missing initialization from blank,
  malformed, truncated, and invalid-shape snapshots without silently resetting state.
- non-destructive, restart-stable corruption diagnosis that leaves persisted bytes and
  directory artifacts unchanged.
- crash-safer JSON replacement that preserves the previous snapshot on move failures and uses
  non-atomic fallback only when atomic replacement is explicitly unsupported.
- an explicit interrupted-write contract that ignores and preserves stale temporary artifacts
  instead of ambiguously promoting or deleting them.
- corruption-safe transaction rollback that restores exact pre-state bytes and preserves the
  same diagnosis after store recreation.
- deterministic 5,000-record persistence round-trip and restart correctness through the real
  JSON store boundary.

Deferred evidence-driven follow-up areas:

- deterministic behavior with large real packages;
- regression fixtures based on representative real data;
- protection against UI blocking or excessive allocation where source evidence supports it.

Package Import & OPD3 Robustness and Persistence Integrity & Recovery are complete at their
current verified boundaries. Further large-package or UI-allocation work requires
representative evidence and is not a blocker for beginning Desktop Beta release readiness.

## Milestone 6 — Desktop Beta release readiness

**Status: In progress — identity, metadata, and runtime directories delivered**

Planned capability areas:

- distributable Desktop packaging;
- application version, build channel, revision, and build-number metadata (delivered);
- platform-aware data/config/cache/log/temp directory contract with legacy data preservation
  (delivered);
- logs and diagnostic export;
- backup and restore or an equivalent safe recovery path;
- first-run onboarding and representative sample content;
- clean-machine smoke testing;
- Windows path, permission, and Unicode verification;
- release checklist and known-limitations documentation.

## Milestone 7 — Desktop Beta validation and Desktop v1

**Status: Planned**

- run sustained testing with real learning packages;
- prioritize crashes, data loss, incompatible upgrades, and blocked workflows;
- measure startup, import, search, queue planning, and Study responsiveness;
- refine workflows based on observed user behavior;
- establish a stable Desktop v1 release boundary.

## Milestone 8 — Android, iOS, and Web

**Status: Deferred until Desktop Beta**

Platform expansion begins only after the shared engine contracts and Desktop Beta behavior
are stable. Do not introduce premature cross-platform abstractions solely to prepare for
this milestone.

## Delivery references

Standing capability, build, test, documentation, and Git rules are defined only in
[`../AGENTS.md`](../AGENTS.md). Official completion records are in
[`MILESTONE_HISTORY.md`](MILESTONE_HISTORY.md); detailed capability history is in
[`CHANGELOG.md`](CHANGELOG.md).
