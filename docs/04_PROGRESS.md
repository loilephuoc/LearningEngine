# Verified Project Progress

## Repository state at audit

- Branch: `develop`
- Commit: `79af828` — revert of the incomplete documentation simplification
- Remote tracking: `origin/develop`
- Stable branch: `main` at `fa6b498` — Learning Engine 2.0 milestone
- Working tree was reported clean.

## Latest verified build

Date: 2026-07-20

```text
BUILD SUCCESSFUL in 14s
13 actionable tasks: 3 executed, 10 up-to-date
```

This build occurred after verifying that the active in-memory repositories remained intact following cleanup of untracked legacy paths.

## Verified historical milestones

- `bad917d`: initial project setup.
- `0737abb`: end-to-end review capability.
- `0064010`: next due/new learning-item selection.
- `a9e3c57`: sibling learning-item prevention in a session.
- `fa6b498`: Learning Engine 2.0 milestone.

## Source-confirmed implemented areas

### Core learning

- Review learning item.
- Start, select next item, review item, and finish a study session.
- New/review limits and no-repeat session policy.
- Memory states and review events.
- Simple and FSRS schedulers.
- Selection pipeline and priority/policy structures.

### Content platform

- Content models, learning items, libraries and package catalogs.
- Content search models, ranking service and persisted search repository.
- Legacy JSON import.
- OPD3 and package scanning/import paths.
- Media archive reading, extraction, mapping, storage and verification.

### Content packaging

- Package descriptor and metadata models.
- Directory/ZIP/bundle readers and writers.
- SHA-256 package ID generation.
- Package validation, including metadata, dependencies, integrity, duplicate content, installed conflicts, references and warnings.
- Package install, registration, import, export, upgrade and uninstall use cases/services.
- Package import workflow and scanner-routing integration tests.

### Persistence and queries

- In-memory repositories for content, items, memory, reviews, sessions, libraries, packages and catalogs.
- JSON stores, records, mappers and store-backed repositories.
- In-memory and JSON-file transaction runners.
- Dashboard, learning-dashboard, progress, review-history and study-statistics query services.

### Desktop

- Compose Desktop shell and navigation.
- Dashboard, Study, Content Library, Review History, Statistics and Settings screens.
- Feature facades/view models/UI states for the primary screens.

## Test baseline

The standard root test tree contains 217 Kotlin test files covering domain, application, infrastructure and JVM adapters. No Kotlin tests were found under the desktop test source tree at audit time.

## Known technical debt

- A tracked duplicate/nonstandard `src/src` source and test tree exists. Current root Gradle configuration uses standard `src/main` and `src/test`; the duplicate tree requires a separate reconciliation milestone.
- Existing documentation artifacts and old source ZIPs remain in `docs/` until this baseline is applied and intentionally committed.
- Desktop test coverage is absent in the inspected source tree.

## Current objective

Apply this documentation baseline at repository root, review the diff, run `./gradlew build`, and commit it on `develop`. After that, resume the next performance-focused Learning Engine increment from source, not from old ZIP documentation.
