# Learning Engine 2.0 — Project Handoff

This file is the canonical continuation context for both humans and AI assistants. Read it before planning or changing the project.

## New-conversation bootstrap

When this repository is supplied in a new conversation and the user says `Continue the project.`:

1. Read this file, `CHANGELOG.md`, `ROADMAP.md`, and `ARCHITECTURE.md`.
2. Inspect the actual source and tests before choosing or implementing work.
3. Continue from the next batch without asking the user to repeat established rules.
4. Ask only when a decision is genuinely product-defining, destructive, security-sensitive, or architecturally irreversible.

Do not treat old chat descriptions, historical ZIPs, or abandoned batch payloads as newer than the source currently supplied.

## Canonical baseline

- Repository: `loilephuoc/LearningEngine`
- Canonical branch: `develop`
- Current source baseline includes verified Batch21.
- Current increment package: `Batch22` — contextual incompatible persisted-record diagnostics.
- Next increment after Batch22 passes: `Batch23`.
- The supplied source plus `docs/` is the source of truth for continuation.
- Historical ZIPs and old batch payloads must not override the current source baseline.

The HEAD value above must be updated after every successfully committed batch. If the actual repository HEAD differs, the actual clean `develop` HEAD wins and this document must be corrected in the same batch.

## Product direction

Primary objective: deliver a usable **Desktop Beta** before expanding to Android, iOS, or Web.

The immediate product milestone is a real end-to-end learning flow:

```text
Launch Desktop
→ import/attach a real learning package
→ browse content and choose a lesson
→ start a study session
→ reveal/answer and grade items
→ persist review/progress state
→ reopen and continue correctly
```

Performance and UX improvements follow functional correctness. Mobile and web work must not distract from the Desktop Beta milestone.

## Current codebase snapshot

- Kotlin/JVM with JDK 21
- Gradle multi-project build
- Root project: domain, application, infrastructure, JVM adapters, CLI, and tests
- `desktop` module: Compose Desktop UI depending on the root project
- Approximately 454 root production Kotlin files, 280 root test files, and 71 desktop production Kotlin files in this supplied snapshot
- Existing capability areas include content packages, content library, study/review flows, scheduling, queue planning, persistence, analytics/dashboard, review history, statistics, settings, and desktop study UI

Counts are orientation only; source inspection is authoritative.

## Development rules

1. Analyze before coding. Read the relevant production code, tests, factories, persistence adapters, and desktop wiring.
2. One batch equals one coherent capability/increment. Do not deliver isolated class patches that leave an incomplete workflow.
3. Every change for an increment is delivered in a ZIP, regardless of individual file length.
4. Preserve existing public contracts unless the same batch includes migration, compatibility, and tests.
5. Do not invent an integration contract without implementing and testing the full wiring in the same increment.
6. Do not make speculative architecture rewrites while a smaller compatible increment can reach the product milestone.
7. Do not ask for routine confirmations. Select the highest-value next capability from the source and roadmap, implement it, test it, package it, and report the result.
8. Before releasing a batch, update this handoff, the changelog, and the roadmap so source and continuation context stay synchronized.

## Batch delivery contract

Each increment is delivered as:

```text
BatchXX_APPLY.zip
├── payload/                 # files at repository-relative paths
├── apply_batch.ps1
├── manifest.json
└── README.txt
```

The package is applied from the external batch folder with:

```powershell
.\run_batch.ps1 BatchXX
```

`run_batch.ps1` remains outside the repository and launches the archive. It must display clear ASCII progress/error messages to avoid Windows console encoding problems.

`apply_batch.ps1` must:

1. Verify the LearningEngine repository root and required source structure.
2. Verify all payload paths and SHA-256 hashes from `manifest.json`.
3. Create backups only for files that will be replaced or deleted.
4. Apply the complete payload atomically enough to support rollback.
5. Run `clean test` using the repository Gradle wrapper.
6. Restore the pre-batch state automatically when verification or build/tests fail.
7. Return a non-zero exit code on failure and print the exact reason.
8. Record enough result information for diagnosis without adding generated artifacts to source-only snapshots.

A batch is not considered complete merely because files copied successfully. It becomes the realtime baseline when `clean test` passes and the user reports `BUILD SUCCESSFUL`.

## Source-only snapshot rule

`LearningEngine_SOURCE_ONLY.zip` is the only file needed to continue in a new conversation. It must include this `docs/` directory and exclude generated or historical artifacts, including:

- `.git/`, `.github/`, `.gradle/`, `.idea/`, `.kotlin/`
- `build/`, `out/`, `bin/`, `target/`
- caches, logs, reports, test-results, temporary files
- `.batch-backups/`, `.batch-results/`
- nested archive snapshots and obsolete handoff packages

The ZIP should contain one root directory named `LearningEngine/`.

## Documentation contract

Only these continuation documents are canonical:

- `docs/PROJECT_HANDOFF.md` — current state, rules, and exact continuation context
- `docs/CHANGELOG.md` — completed batch history and meaningful technical changes
- `docs/ROADMAP.md` — Done / In progress / Planned priorities
- `docs/ARCHITECTURE.md` — stable architecture and dependency boundaries

Avoid duplicating the same status across extra files. Update documentation as part of every completed capability batch.

## Immediate continuation instruction

Batch22 wraps domain reconstruction failures for persisted libraries, collections, contents, learning items, packages, and catalogs with the entity type and record ID while preserving the original cause. After Batch22 passes, inspect Desktop error-state wiring so persistence diagnostics are actionable to the user without terminating the complete learning flow.

# Source of Truth

The current repository source and `docs/` are authoritative. After each successful batch, the applied files become the realtime baseline.

# AI Startup Rules
Khi người dùng nói 'Tiếp tục dự án.', hãy đọc docs rồi toàn bộ source, tiếp tục capability tiếp theo, không yêu cầu giải thích lại workflow nếu tài liệu đã đầy đủ.
