# Learning Engine 2.0 — AI Architect Context

Short-term repository and Phase snapshot only. Standing workflow is defined in
[`../AGENTS.md`](../AGENTS.md).

## Repository

- Repository: `loilephuoc/LearningEngine`
- Branch: `develop`
- Verified product HEAD: `7e2eea54c2d47696d77c806d52d697af95f92944`
- Product working tree after capability commit: clean
- Configured upstream: `origin/develop`
- No push, publish, install, signing, or history rewrite was performed.

## Current Phase

- Phase: **Phase 5 — Desktop Beta Readiness**
- Status: in progress; capabilities 1–2 of the planned sequence are delivered.
- Outcome and Definition of Done: [`ROADMAP.md`](ROADMAP.md#phase-5--desktop-beta-readiness)

## Completed Phase 5 Capabilities

- `a20d89e` — established deterministic Windows app-image, MSI, and EXE distributions.
- `7e2eea5` — added privacy-safe runtime diagnostic export through the About dialog.

## Current Capability and Blocker

- Next capability: user-data backup/restore or an approved equivalent recovery path.
- State: not started; no partial implementation exists.
- Blocking product decision: define authoritative backup scope, manual versus automatic backup,
  retention, restore replacement versus merge semantics, validation before replacement, and
  rollback behavior after restore failure.
- Existing stale `.tmp` files are explicitly non-authoritative and cannot be promoted into a
  recovery source.

## Latest Test and Packaging Evidence

- Command: `.\gradlew.bat clean test` with full Temurin JDK 21 toolchain.
- Result: `BUILD SUCCESSFUL`.
- Test suites/tests: 360 / 1,495.
- Failures/errors/skipped: 0/0/0.
- Packaging verification: `:desktop:createDistributable`, `:desktop:packageMsi`, and
  `:desktop:packageExe` succeeded using JDK 21 `jpackage` and WiX.
- Verified artifact names: `LearningEngine-1.0.0.msi` and `LearningEngine-1.0.0.exe`.

## Remaining Phase 5 Work

1. Resolve and implement the recovery policy above.
2. First-run onboarding and representative sample content.
3. Windows path, permission, Unicode, install/update, and clean-machine smoke verification.
4. Beta release checklist, known limitations, and release-candidate evidence.

## Current Risks and Constraints

- Local packages are unsigned and have not been installed on a clean machine.
- No approved recovery source or retention policy exists.
- Onboarding and representative sample-content policy are not defined.
- Localization covers shell/Settings, not every feature-screen string.
- Sensitive boundaries remain application identity/version, runtime paths, legacy data
  selection, persistence replacement/transactions, and lifecycle ordering.
