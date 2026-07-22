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

**Status: In progress through Batch76**

Delivered:

- structured package-import failure categories and stable diagnostic codes;
- preserved validation issue codes and actionable recovery guidance;
- non-fail-fast candidate reporting without changing successful-package commits.
- bounded OPD3 JSON entry reads with strict UTF-8 validation.

Planned capability areas:

- malformed, partial, duplicate, or incompatible OPD3 data;
- actionable diagnostics that identify the affected package, record, and recovery action;
- corrupt or interrupted persistence recovery;
- compatibility and version validation;
- deterministic behavior with large real packages;
- regression fixtures based on representative real data;
- protection against UI blocking or excessive allocation where source evidence supports it.

Each batch must close one concrete robustness gap.

## Milestone 6 — Desktop Beta release readiness

**Status: Planned**

Planned capability areas:

- distributable Desktop packaging;
- application version and build metadata;
- stable data-directory behavior;
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

## Batch policy

Every batch must deliver one coherent, testable increment toward the nearest milestone.
Normally target 8–15 affected files, but choose the smallest complete vertical slice.
Do not reserve placeholder batch numbers and do not mark work complete until the supplied
source passes the required verification.
