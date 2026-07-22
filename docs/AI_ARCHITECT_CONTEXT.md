# Learning Engine 2.0 — AI Architect Context

Short-term repository and Phase snapshot only. Standing workflow is defined in
[`../AGENTS.md`](../AGENTS.md).

## Repository

- Repository: `loilephuoc/LearningEngine`
- Branch: `develop`
- Baseline HEAD: `dd27cbe45602aa72b0ee4e507c92ecdd16e6be56`
- Baseline working tree: clean
- Configured upstream: `origin/develop`
- No fetch or push was performed during this workflow-maintenance increment.

## Current Phase

- Phase: **Phase 5 — Desktop Beta Readiness**
- Status: current; no Phase 5 product capability has started.
- Outcome: an installable, supportable, recoverable Desktop Beta candidate verified on a clean
  Windows environment without implicit data migration.
- Definition of Done: [`ROADMAP.md`](ROADMAP.md#phase-definition-of-done)

## Current Capability

- Next capability: Desktop distributable packaging contract and deterministic local artifacts.
- State: unstarted.
- Required discovery: current Compose/Gradle packaging support, artifact identity/version
  wiring, Windows runtime assumptions, and testable boundaries.
- Explicit exclusions until separately authorized: publishing, signing secrets, release upload,
  installer execution on external machines, and automatic data migration.

## Latest Completed Capability

- `82b5278` — completed Desktop startup and About experience.
- `dd27cbe` — completed the Milestone 7 handoff; this is the clean product baseline before the
  workflow evolution increment.

## Latest Test Evidence

- Verified product HEAD: `82b527839a3d71adcabcb5514114d9ca63f5cd12`
- Command: `.\gradlew.bat clean test`
- Result: `BUILD SUCCESSFUL`
- Test suites/tests: 358 / 1,490
- Failures/errors/skipped: 0/0/0
- The later `dd27cbe` commit changed only Markdown handoff files.

## Current Risks and Constraints

- Compose distributable packaging is not configured.
- Diagnostic export and approved backup/restore do not exist.
- Backup/restore requires a product decision about retention and authoritative recovery source.
- Clean-machine Windows permission, Unicode-path, signing, install/update, and primary-flow
  smoke evidence remain absent.
- Localization covers the shell and Settings foundation, not every feature-screen string.
- Sensitive boundaries remain application identity/version, runtime paths, legacy data
  selection, non-destructive config/window-state handling, lifecycle ordering, and persisted
  composition paths.
