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

**Status: Current**

Outcome: produce an installable, supportable, recoverable Desktop Beta candidate and verify it
on a clean Windows environment without silently migrating existing data.

Planned capability sequence:

1. Desktop distributable packaging contract and deterministic local artifacts (delivered).
2. Diagnostic export with privacy-preserving support data (delivered).
3. Backup/restore or an explicitly approved equivalent recovery path.
4. First-run onboarding and representative sample content.
5. Windows path, permission, Unicode, install/update, and clean-machine smoke verification.
6. Beta release checklist, known limitations, and release-candidate evidence.

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

## Phase 6 — Desktop Beta Validation and v1

**Status: Planned**

Outcome: validate the Beta with representative real learning workloads and establish the
stable Desktop v1 boundary.

Measure startup, import, search, queue planning, and Study responsiveness; prioritize crashes,
data loss, incompatible upgrades, and blocked workflows; refine behavior using observed
evidence rather than speculative polish.

## Phase 7 — Additional Platforms

**Status: Deferred until Desktop v1**

Outcome: introduce Android, iOS, and Web consumers only after shared engine contracts and
Desktop v1 behavior are stable. Do not add premature cross-platform abstractions solely to
prepare for this Phase.

## Delivery references

Capability/build/Git rules are in [`../AGENTS.md`](../AGENTS.md). Historical milestone and
completed-Phase records are in [`MILESTONE_HISTORY.md`](MILESTONE_HISTORY.md); detailed verified
increments are in [`CHANGELOG.md`](CHANGELOG.md).
