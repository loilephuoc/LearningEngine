# Test Matrix

This matrix maps common changes to focused verification neighborhoods. Exact source and tests
remain authoritative. Build and testing policy lives only in [`../AGENTS.md`](../AGENTS.md).

## Study, queue, and review flow

Run or inspect tests covering:

- queue lifecycle and planning;
- lesson-scoped selection and sibling avoidance;
- reveal and grading transitions;
- scheduler integration;
- atomic persisted review transactions;
- restart, resume, lesson isolation, and completion;
- Desktop Study state, keyboard routing, focus, and presentation contracts.

Phase 6 lifecycle work must additionally cover every valid/invalid state transition, persisted
schema compatibility, process restart at transition boundaries, missing/completed queue
reconciliation, duplicate-review prevention, and transaction rollback. Undo requires exact
forward/reverse state assertions across review event, memory state, session, and queue.

P6-02 covers current-item/reveal/pending-intent lifecycle invariants, schema-v1 checkpoint
round-trip and legacy defaults, interruption before transaction mutation, single replay with a
stable event ID, existing restart recovery, and the full root/Desktop regression suite.

P6-03 adds pure workspace transition and allowed-action coverage, forbidden rating/reveal
ordering, keyboard routing from explicit state, and persisted Desktop restart projection of a
revealed answer.

P6-04 covers deterministic Question/Answer/Example block ordering, plain/Markdown formats,
newline preservation, optional sections, unsafe and missing asset fallback, package DTO and
persistence compatibility, and Desktop restart projection. Content-model changes must also run
package import, JSON persistence, and Desktop Study regression coverage.

P6-05 covers Question-only versus revealed Question/Answer/Example visibility, ordered blocks,
safe Markdown structures and inline styles, inert HTML, local image/audio resolution, localized
missing/unsupported fallbacks, and failure-safe audio state. Renderer changes must preserve the
workspace action, keyboard, restart projection, package import, and persistence suites.

P6-06 covers known/unknown and empty totals, processed/reviewed/skipped distinctions, start and
post-review progress, transaction failure and pending-review recovery, persisted restart,
queue-based completion, scheduler-result feedback, and visible/screen-reader summaries. Progress
changes must retain atomic session/queue/review tests, completed-queue restart projection, plus
P6-05 renderer and keyboard coverage.

P6-07 minimum evidence must cover pause-as-active-resume, exactly-one latest undo, absence of
multi-level undo, event/memory/session/queue/progress reversal, final-review completion reopen,
no-prior-memory restoration, failed undo rollback, retry idempotency, restart behavior, legacy
record compatibility, and existing P6-02/P6-06 interruption/completion regressions.
Focused coverage exercises first-memory deletion, event/session/queue/progress restoration,
idempotent retry, final-session reopening, persisted restart, optional-record compatibility,
and the established pending-review recovery/workspace regressions.

## P6-08 Desktop workspace evidence

Automated coverage owns state/action permission, reveal/rating/undo/retry/pause keyboard routing,
busy/repeat/text-input suppression, focus-phase identity, localized action contracts, safe error
copy, media fallbacks, renderer semantics, restart/resume, final-review undo, progress rollback,
and second-undo blocking.

Product Owner manual evidence remains pending for P6-09:

1. Start by mouse, then complete a separate session using only keyboard.
2. Reveal, close, restart, and confirm the revealed answer returns.
3. Rate, Undo, rate again; then Undo the final rating from completion and try a second Undo.
4. Spam rating keys and try every shortcut in an invalid state.
5. Verify Tab and Shift+Tab order, visible focus, Escape pause, and resume.
6. Exercise narrow-window scrolling and long Markdown wrapping.
7. Verify missing image and unavailable audio fallbacks with keyboard and assistive output.
8. Exercise a review/undo transaction failure fixture and confirm progress/checkpoint remain.
9. Confirm completion and progress after Undo.
10. Confirm no raw exception or stack trace appears in the workspace.

This checklist is not recorded as passed until the UI is exercised in the target Desktop
environment; automated tests are supporting evidence, not a substitute for that observation.

## Scheduling and memory state

Run or inspect tests covering:

- scheduler decisions for each rating;
- decision validation;
- interval and next-review calculations;
- stage transitions and metrics;
- review-to-persistence integration.

Any change to time semantics requires deterministic clock-based tests.

## OPD3 and package import

Run or inspect tests covering:

- scanner routing;
- manifest and metadata identity validation;
- bundle/content import;
- package registration and installed-package queries;
- partial and failed import diagnostics;
- persisted import-to-review restart;
- representative real-package fixtures.

Malformed-input work must include both rejection behavior and actionable diagnostic text.

## Persistence and recovery

Run or inspect tests covering:

- record mapping round trips;
- JSON store behavior;
- repository behavior;
- transaction atomicity;
- incompatible or corrupt records;
- restart reconciliation;
- Desktop composition wiring using persisted stores.

Persisted schema coverage includes compatibility or migration behavior.

## Search and discovery

Minimum focused tests include:

- `SearchQueryTermsTest`;
- `SearchTextTest`;
- `SearchMatchPresentationTest`;
- `SearchQueryGuidancePresentationTest`;
- `LessonBrowserProjectionTest`;
- `ReviewHistoryProjectionTest`;
- keyboard, refinement, result-status, and empty-state contracts affected by the change.

Test matching and highlighting against the same normalization rules. Include multi-field
rows when terms may be distributed across searchable fields.

## Content Library and Lesson Browser

Run or inspect tests covering:

- library/collection/package presentation;
- attachment, detachment, rename, and delete flows;
- dialog state and confirmation boundaries;
- import outcomes and retry;
- lesson selection and Study navigation;
- keyboard navigation;
- Lesson Browser search projection.

## Dashboard, statistics, and review history

Run or inspect tests covering:

- application query/calculation correctness;
- loading, ready, failed, retry, and stale-data behavior;
- active-screen refresh routing;
- semantic summaries and empty states;
- chart/value presentation where affected.

## Desktop shell and navigation

Run or inspect tests covering:

- destination state and selected semantics;
- F-key and cyclic navigation;
- active-screen refresh;
- focus retention;
- ContentHost and LearningShell callback wiring.

## Performance and large-data work

Representative performance coverage includes:

- a deterministic representative fixture size;
- a correctness assertion at that size;
- a measured operation boundary;
- an explicit regression threshold only when the build environment is stable enough;
- otherwise, allocation/algorithmic assertions that do not create flaky wall-clock tests.

Timing-only checks are not correctness evidence.

## Release-readiness work

Verify as applicable:

- clean build from a clean checkout;
- distributable creation;
- launch on a clean Windows user profile or equivalent isolated directory;
- data-directory creation and permissions;
- Unicode paths and filenames;
- logging and diagnostic export;
- first-run and existing-data startup;
- upgrade/restart behavior;
- smoke flow from import to persisted completion.

Desktop runtime foundation coverage includes identity stability, generated metadata loading,
platform-specific path resolution, corrupt configuration preservation, log retention,
lifecycle ordering, restart behavior, and support-diagnostic redaction.
