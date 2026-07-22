# Learning Engine 2.0 — AI Architect Context

Short-term repository and Phase snapshot only. Standing workflow is defined in
[`../AGENTS.md`](../AGENTS.md).

## Repository

- Repository: `loilephuoc/LearningEngine`
- Branch: `develop`
- Verified product HEAD: `5edc27723a1e6fe2c53aaffeb23c98e020adeedf`
- Product working tree after capability commit: clean
- Configured upstream: `origin/develop`
- No push, publish, install, signing, or history rewrite was performed.

## Current Phase

- Phase: **Phase 5 — Desktop Beta Readiness**
- Status: implementation and local automation complete; external clean-machine gates pending.
- Outcome and Definition of Done: [`ROADMAP.md`](ROADMAP.md#phase-5--desktop-beta-readiness)

## Completed Phase 5 Capabilities

- `a20d89e` — deterministic Windows app-image, MSI, and EXE distributions.
- `7e2eea5` — privacy-safe runtime diagnostic export.
- `f345687` — manual durable-state backup, validated whole-snapshot restore, safety backup, and rollback.
- `dbef50d` — restart-safe first-run onboarding and production-OPD3 starter content.
- `5edc277` — reproducible Windows Beta verification harness and release checklist.

## Latest Test and Local Candidate Evidence

- Command: `scripts/verify-windows-beta.ps1` with Temurin JDK 21 and WiX.
- Result: `BUILD SUCCESSFUL`; clean test, MSI, EXE, hashing, signature reporting, and Unicode writable-path probe passed.
- Test suites/tests: 363 / 1,504; failures/errors/skipped: 0/0/0.
- MSI: `LearningEngine-1.0.0.msi`, 64,749,994 bytes, SHA-256 `ef9228a5ac56f7b40502ade64fb8f14e40d38c59579adb33eb82a9c3b423f8e9`.
- EXE: `LearningEngine-1.0.0.exe`, 65,433,600 bytes, SHA-256 `dccbfd333f1a8a48e882c864b039ff158e4c6bc460de4d2862c83dc0a8a4812b`.
- Authenticode: both artifacts correctly reported `NotSigned`.
- Install/uninstall/upgrade: not run on the development machine.

## Remaining Phase Gate

Product Owner must run [`BETA_RELEASE_CHECKLIST.md`](BETA_RELEASE_CHECKLIST.md) on a disposable
clean supported Windows machine and retain the generated evidence. Required observations are:

- MSI install and installed-app launch under a Unicode username;
- onboarding, representative import-to-study flow, diagnostics, backup, and restore;
- uninstall preserves user data and reinstall reopens it without migration;
- upgrade from an approved previous MSI whose commit/hash is recorded;
- signing status accepted or signed artifacts supplied through authorized secrets.

These claims cannot be produced honestly from the current non-clean development machine. The
Phase remains incomplete until the evidence is supplied and the release checklist is closed.

## Known Limitations

- Local artifacts are unsigned.
- Cloud/scheduled backup, automatic retention, and cross-device merge are unsupported.
- Localization does not cover every feature screen.
- Downgrade compatibility is unverified and must not be claimed.
