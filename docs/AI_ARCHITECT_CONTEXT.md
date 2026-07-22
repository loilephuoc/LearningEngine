# Learning Engine 2.0 — AI Architect Context

This is the short-term operational handoff. Update it at milestone completion or when a Codex
session must stop mid-work. Do not record unverified or speculative state.

## Repository state

- Repository: `loilephuoc/LearningEngine`
- Local path: repository root containing this file
- Branch: `develop`
- Verified product baseline HEAD: `5ae1d3b9bf3b6092dde308dad81e2d83660baa56`
- Current repository HEAD: the commit containing this context; resolve with
  `git rev-parse HEAD` after checkout.
- Working tree at product-baseline capture: clean
- Required final state for this handoff: clean after the documentation commit
- Configured upstream: `origin/develop`
- Local `origin/develop` tracking ref at capture:
  `5ae1d3b9bf3b6092dde308dad81e2d83660baa56`
- Upstream evidence scope: local tracking ref only; no network fetch was performed for this
  documentation task.

## Recently completed milestone

**Package Import & OPD3 Robustness** completed through Batch82.

Relevant commits, newest first:

- `5ae1d3b` — contextualize OPD3 JSON value shapes
- `b324d0d` — contextualize malformed OPD3 JSON
- `5b9a6e5` — unify missing OPD3 entry failures
- `66b5dbd` — bound total OPD3 uncompressed size
- `f37152a` — validate OPD3 archive structure before entry reads
- `a618893` — harden OPD3 text entry reading
- `74aa3f4` — add actionable package import diagnostics

## Verified capabilities

- Stable per-candidate package diagnostics and non-fail-fast directory continuation.
- No persistence for a package candidate rejected before its transaction.
- Unsafe, duplicate, ambiguous, or excessive archive entries rejected before JSON reads.
- Configurable archive entry count and total declared uncompressed-size budgets.
- Bounded streamed required-entry reads with strict UTF-8 decoding.
- Shared missing-entry and invalid-JSON contracts with entry context and preserved messages.
- Manifest/metadata identity, schema, integrity, dependency, duplicate, and reference validation.
- Persisted OPD3 import-to-study restart and Desktop browse-to-grade integration coverage.

## Latest test evidence

- Command: `.\gradlew.bat clean test`
- Result: `BUILD SUCCESSFUL`
- Test suites: 345
- Tests: 1,446
- Failures: 0
- Errors: 0
- Skipped: 0
- Evidence applies to product baseline `5ae1d3b`; this handoff changes documentation only.

## New decisions

- Durable memory is split into strategic `PROJECT_HANDOFF.md` and operational
  `AI_ARCHITECT_CONTEXT.md`.
- Repository source and tests at a clean HEAD outrank all handoff text.
- Chat is not a durable state store.
- The next proposed track is Persistence Integrity & Recovery; no product capability in that
  track has started.

## Current priorities

1. Define corrupt/truncated persistence states without silently converting user data to empty.
2. Establish a non-destructive quarantine/backup/recovery boundary.
3. Preserve legacy-array and schema-v1 compatibility.
4. Add restart and failure-injection coverage through real persisted composition roots.
5. Keep Desktop recovery messages actionable while preserving the last known good UI state.

## Known risks

- Blank persistence files currently return empty collections and may hide truncation.
- In-process transaction snapshots do not provide crash recovery.
- Non-atomic move fallback weakens crash consistency on unsupported filesystems.
- A recovery feature could destroy evidence or user data if it overwrites corrupt files.
- Multi-file recovery can create cross-file inconsistency unless transaction membership and
  recovery ordering are explicit.

## Technical debt to track

- No backup, quarantine, restore, or recovery manifest exists.
- No general persistence migration framework beyond legacy arrays and schema envelope v1.
- No centralized diagnostic export or release-ready logging workflow.
- Multi-file transaction snapshots allocate complete file contents in memory.
- Large-data performance evidence is not yet representative enough for stable thresholds.

## Boundaries not to refactor lightly

- `JsonFileReader`, `JsonFileWriter`, and `JsonPersistenceCodec` compatibility semantics.
- `JsonFileTransactionRunner` snapshot membership and original-error preservation.
- `PersistedLearningPlatformFactory` and `LearningApplicationFactory` composition roots.
- Store-backed repository mappers and persisted record schemas.
- Study session/queue/memory/review atomic transaction boundary.
- Package diagnostic codes/messages and OPD3 validation boundaries completed in Batch76–82.
- Desktop stale-data and recoverable-error presentation contracts.

## Proposed next milestone

**Persistence Integrity & Recovery**, starting with one bounded capability: distinguish and
diagnose corrupt or suspiciously blank persisted JSON without overwriting it, then define the
recovery policy only after source evidence and failure tests establish safe behavior.

This is a proposal, not an implemented capability.

## Open decisions and stop conditions

Stop and ask before:

- automatically replacing, deleting, or migrating corrupt user data;
- choosing retention, backup count, quarantine naming, or restore UX without repository policy;
- changing legacy-array or schema-v1 compatibility;
- introducing a journal/database or materially replacing JSON persistence;
- requiring external secrets, services, release authority, or push access.

No unresolved product decision blocks read-only analysis of the first persistence-integrity
capability.

## Continuation prompt

```text
Continue Learning Engine 2.0 from the clean develop HEAD. Read AGENTS.md,
docs/PROJECT_HANDOFF.md, docs/AI_ARCHITECT_CONTEXT.md, and the real JSON persistence source and
tests. Design the smallest non-destructive Persistence Integrity & Recovery capability. Do not
overwrite or migrate corrupt data without an explicit product decision. Add focused and restart
tests, update canonical docs, run .\gradlew.bat clean test until BUILD SUCCESSFUL, commit one
capability, and do not push.
```
