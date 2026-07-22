# Learning Engine 2.0 — AI Architect Context

Short-term repository snapshot only. Standing workflow is defined in
[`../AGENTS.md`](../AGENTS.md).

## Repository

- Repository: `loilephuoc/LearningEngine`
- Branch: `develop`
- Baseline HEAD before Workflow Foundation Refinement:
  `435535a684616159ea3557ffcd2f4f5764a76b19`
- Baseline working tree: clean
- Workflow documentation HEAD: the commit containing this snapshot
- Required post-commit working tree: clean
- Configured upstream: `origin/develop`
- Local upstream tracking ref at baseline:
  `d5f76e00505f87a31781ab40370465296cd32353`
- No fetch or push was performed during this milestone.

## Current State

- Most recently completed product milestone: **Persistence Integrity & Recovery**, through
  Batch88.
- Current documentation milestone: **Workflow Foundation Refinement**.
- Next product milestone: **Desktop Beta release readiness**.
- No Desktop Beta release-readiness product capability has started.

## Recent Commits Before This Milestone

- `435535a` — hand off persistence integrity milestone
- `4667058` — verify large persistence restart correctness
- `c229618` — preserve corrupt snapshots across transaction rollback
- `4069b35` — define stale JSON temporary artifact behavior
- `fa3579f` — preserve JSON snapshots on replacement failure
- `4c9a6b7` — verify non-destructive corrupt persistence reads
- `babb309` — classify corrupt JSON persistence reads

## Latest Verified Test Evidence

- Product HEAD: `4667058223c975c0800ed340af2f876b662bb449`
- Command: `.\gradlew.bat clean test`
- Result: `BUILD SUCCESSFUL`
- Test suites: 345
- Tests: 1,455
- Failures/errors/skipped: 0/0/0
- Workflow Foundation Refinement changes Markdown only; Gradle was not rerun.

## Immediate Product Context

- Candidate first capability: stable application identity, version/build metadata, or
  data-directory behavior, selected after reading actual Desktop build and composition source.
- Release work still lacks centralized logs/diagnostic export, distributables, onboarding,
  clean-machine evidence, and an approved backup/restore policy.
- Sensitive boundaries include JSON reader/writer/codec compatibility, transaction membership,
  persisted schemas/mappers, OPD3 diagnostics, application composition roots, and Desktop
  accessibility/recovery behavior.
