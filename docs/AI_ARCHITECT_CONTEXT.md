# Learning Engine 2.0 — AI Architect Context

Short-term repository snapshot only. Standing workflow is defined in
[`../AGENTS.md`](../AGENTS.md).

## Repository

- Repository: `loilephuoc/LearningEngine`
- Branch: `develop`
- Verified product HEAD: `1096f8e0931e0bd27831b42a5e0f03134914ec9e`
- Product worktree after capability commit: clean
- Configured upstream: `origin/develop`
- Local upstream tracking ref at milestone start:
  `611ac0eaa0773d7761ec4b723881a8e4ac1da02b`
- No fetch or push was performed.

## Current State

- Most recently completed milestone: **Milestone 6 — Desktop Runtime Foundation**.
- Next milestone: **Milestone 7 — Desktop packaging and Beta release readiness**.
- No installer, distributable, automatic migration, diagnostic export, or backup/restore
  capability was implemented in Milestone 6.

## Milestone 6 Capability Commits

- `1096f8e` — expose Desktop runtime support diagnostics
- `9936698` — compose Desktop runtime lifecycle
- `fbfbe3f` — add Desktop runtime file logging
- `d160a9a` — add non-destructive Desktop runtime configuration
- `ca6a13d` — define Desktop runtime directories
- `d5f7338` — establish Desktop application identity metadata

## Verified Runtime Boundary

- Stable application ID/name/directory identity and generated version/channel/revision/build
  metadata.
- Separate Windows/macOS/Linux data, config, cache, logs, and temp paths.
- Existing `~/.learning-engine/data` remains selected in place when present; no migration.
- Missing config uses typed defaults; corrupt existing config remains unchanged and fails with
  contextual diagnostics.
- UTF-8 session logging, typed levels/events, exact-namespace retention, and idempotent close.
- Deterministic startup/shutdown ownership and original-error preservation on startup failure.
- Redacted runtime diagnostics flow through composition into Settings/About.
- Two-session restart coverage preserves config bytes and complete bounded logs.

## Latest Test Evidence

- Product HEAD: `1096f8e0931e0bd27831b42a5e0f03134914ec9e`
- Command: `.\gradlew.bat clean test`
- Result: `BUILD SUCCESSFUL`
- Test suites: 352
- Tests: 1,475
- Failures/errors/skipped: 0/0/0

## Immediate Product Context

- Packaging/distributable support is not yet configured in the Compose build.
- Diagnostic information is visible and redacted, but export/copy workflow is not implemented.
- Backup/restore still requires an explicit retention and recovery-source product policy.
- Clean-machine Windows permission/Unicode-path evidence and onboarding remain outstanding.
- Sensitive boundaries remain runtime directory identity, legacy data selection, config
  preservation, log namespace/retention, lifecycle ordering, and persisted composition paths.
