# Learning Engine 2.0 — AI Architect Context

Short-term repository snapshot only. Standing workflow is defined in
[`../AGENTS.md`](../AGENTS.md).

## Repository

- Repository: `loilephuoc/LearningEngine`
- Branch: `develop`
- Verified product HEAD: `82b527839a3d71adcabcb5514114d9ca63f5cd12`
- Product worktree after capability commit: clean
- Configured upstream: `origin/develop`
- No fetch or push was performed during Milestone 7.

## Current State

- Most recently completed milestone: **Milestone 7 — Desktop UX Foundation**.
- Next milestone: **Milestone 8 — Desktop packaging and Beta release readiness**.
- Installer/distributable packaging, diagnostic export, backup/restore, and onboarding were
  intentionally outside Milestone 7.

## Milestone 7 Capability Commits

- `82b5278` — complete Desktop startup and About experience
- `05e8527` — standardize Desktop focus traversal
- `52a60f6` — add Desktop localization foundation
- `10e8b2b` — connect Desktop theme settings
- `93007ed` — persist safe Desktop window placement
- `92afd97` — standardize Desktop shell navigation

## Verified Desktop UX Boundary

- Central shell routes and Home/Learn/Review/Library/Settings vocabulary, retaining Statistics.
- Safe, non-destructive window size/position/maximized restart state.
- Typed persisted Light/Dark/System theme and English/Vietnamese locale configuration.
- Localized shell and Settings catalog with stable locale-independent route identities.
- Shell-wide navigation shortcuts and explicit navigation/content focus traversal.
- Localized startup-to-shell presentation and About dialog using redacted runtime diagnostics.

## Latest Test Evidence

- Product HEAD: `82b527839a3d71adcabcb5514114d9ca63f5cd12`
- Command: `.\gradlew.bat clean test`
- Result: `BUILD SUCCESSFUL`
- Test suites: 358
- Tests: 1,490
- Failures/errors/skipped: 0/0/0

## Immediate Product Context

- Compose distributable packaging is not configured.
- Localization currently covers the shell and Settings foundation, not every feature-screen
  string.
- Diagnostic information is visible and redacted, but no export/copy workflow exists.
- Backup/restore needs an explicit retention and recovery-source product policy before design.
- Clean-machine Windows permission, Unicode-path, signing, and install/update evidence remain.
- Sensitive boundaries remain data-directory identity, legacy data selection, non-destructive
  config/window-state handling, lifecycle ordering, and persisted composition paths.
