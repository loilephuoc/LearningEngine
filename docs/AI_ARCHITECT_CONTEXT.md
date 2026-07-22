# Learning Engine 2.0 — AI Architect Context

This is the short-term operational handoff. Update it at milestone completion or when a Codex
session must stop mid-work. Do not record unverified or speculative state.

## Repository state

- Repository: `loilephuoc/LearningEngine`
- Branch: `develop`
- Verified product HEAD: `4667058223c975c0800ed340af2f876b662bb449`
- Working tree before this context update: clean
- Required state after the documentation commit: clean
- Configured upstream: `origin/develop`
- Local `origin/develop` tracking ref: `d5f76e00505f87a31781ab40370465296cd32353`
- Upstream evidence is the local tracking ref; no network fetch or push was performed.

## Recently completed milestone

**Persistence Integrity & Recovery** completed through Batch88.

Relevant commits, newest first:

- `4667058` — verify large persistence restart correctness
- `c229618` — preserve corrupt snapshots across transaction rollback
- `4069b35` — define stale JSON temporary artifact behavior
- `fa3579f` — preserve JSON snapshots on replacement failure
- `4c9a6b7` — verify non-destructive corrupt persistence reads
- `babb309` — classify corrupt JSON persistence reads

## Verified capabilities

- Missing JSON targets initialize empty stores; existing blank targets fail as corruption.
- Stable `BLANK`, `MALFORMED`, `TRUNCATED`, and `INVALID_SHAPE` diagnostics carry record and
  file context without persisted values in messages.
- Corrupt bytes remain unchanged across repeated reads and recreated stores.
- Snapshot writes use a same-directory durable candidate and prefer atomic replacement.
- Non-atomic fallback occurs only when atomic move is explicitly unsupported; unexpected move
  failures preserve the previous target and original error.
- Stale `.tmp` files remain inert and are neither promoted nor deleted automatically.
- Failed in-process transactions restore exact pre-state bytes and retain the original
  operation failure; recreated stores diagnose restored corruption consistently.
- A deterministic 5,000-record snapshot round-trips in order through the real JSON store and
  store-recreation boundary.

## Latest test evidence

- Command: `.\gradlew.bat clean test`
- Product HEAD: `4667058223c975c0800ed340af2f876b662bb449`
- Result: `BUILD SUCCESSFUL`
- Test suites: 345
- Tests: 1,455
- Failures: 0
- Errors: 0
- Skipped: 0

## Decisions established

- Missing and blank persistence are distinct; only a missing target implies first-use state.
- Read failures are observational and never rewrite, delete, quarantine, or repair the target.
- Stale temp filenames are insufficient evidence for promotion or cleanup.
- Atomic replacement fallback is a compatibility path with explicitly weaker crash safety.
- Transaction rollback preserves opaque bytes, including corruption, rather than normalizing
  data during failure recovery.
- No wall-clock performance claim is made from the deterministic large-state regression.

## Current priorities

1. Begin **Desktop Beta release readiness**.
2. Establish stable application identity, version/build metadata, and data-directory behavior.
3. Add release-ready logs and diagnostic export that can surface structured persistence and
   package failures safely.
4. Define backup/restore only with an explicit retention and recovery-source policy.
5. Build distributables and verify clean-machine Windows paths, permissions, and Unicode.

## Known risks and technical debt

- There is no crash journal for multi-file transactions.
- Filesystems without atomic move support retain a weaker non-atomic replacement window.
- No backup, quarantine, restore, or recovery manifest exists.
- Stale temp artifacts require manual/diagnostic handling and may accumulate after crashes.
- Schema support remains legacy arrays plus envelope v1; there is no general migration system.
- Multi-file transaction snapshots allocate complete managed files in memory.
- Centralized logs, diagnostic export, distributables, and clean-machine smoke evidence remain
  release work.

## Boundaries not to refactor lightly

- `JsonFileReader`, `InvalidJsonPersistenceException`, `JsonFileWriter`, and
  `JsonPersistenceCodec` compatibility semantics.
- `JsonFileTransactionRunner` managed-path membership and original-error preservation.
- `PersistedLearningPlatformFactory` and `LearningApplicationFactory` composition roots.
- Persisted record schemas, legacy-array decoding, and store-backed mappers.
- Study session/queue/memory/review transaction membership.
- Package diagnostics and OPD3 safety boundaries completed through Batch82.
- Desktop stale-data, recoverable-error, keyboard, and accessibility contracts.

## Open decisions and stop conditions

Stop and ask before automatically deleting, migrating, quarantining, backing up, or restoring
user data; choosing retention or restore UX; changing schema-v1 or legacy compatibility;
introducing a database/journal; or requiring release credentials, services, publishing, or
push access.

No unresolved decision blocks read-only analysis and the first bounded Desktop Beta release
readiness capability.

## Continuation prompt

```text
Continue Learning Engine 2.0 from the clean develop HEAD. Read AGENTS.md,
docs/PROJECT_HANDOFF.md, docs/AI_ARCHITECT_CONTEXT.md, ROADMAP, ARCHITECTURE, and the actual
Desktop composition/build source. Design the smallest Desktop Beta release-readiness
capability, starting with stable application identity, version/build metadata, or data-directory
behavior as source evidence indicates. Add focused tests and docs, run .\gradlew.bat clean test
until BUILD SUCCESSFUL, commit one capability, and do not push.
```
