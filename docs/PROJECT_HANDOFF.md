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
- Current source baseline includes verified Batch36 at commit `9e9fe28`.
- Current increment package: `Batch37` — accessible Desktop Study action descriptions.
- Next increment after Batch37 passes: `Batch38`.
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
├── payload/
├── apply_batch.ps1
├── manifest.json
└── README.txt
```

The package is applied from the external batch folder with:

```powershell
.\run_batch.ps1 BatchXX
```

`run_batch.ps1` remains outside the repository and launches the archive. It must display clear ASCII progress/error messages to avoid Windows console encoding problems.

`apply_batch.ps1` must verify the repository and payload, back up affected files, apply the increment, run `clean test`, and automatically roll back on failure.

A batch becomes the realtime baseline only after `clean test` passes and the user reports `BUILD SUCCESSFUL`.

## Source-only snapshot rule

`LearningEngine_SOURCE_ONLY.zip` is the only file needed to continue in a new conversation. It must include this `docs/` directory and exclude generated, temporary, backup, and historical archive artifacts.

## Documentation contract

Only these continuation documents are canonical:

- `docs/PROJECT_HANDOFF.md`
- `docs/CHANGELOG.md`
- `docs/ROADMAP.md`
- `docs/ARCHITECTURE.md`

## Immediate continuation instruction

Batch31 verifies a real four-file OPD3 bundle through import, Content Library browsing, lesson selection, and navigation into lesson-scoped Study. Batch32 extends that same Desktop presentation path through persisted process restart before grading, resumed lesson isolation, answer reveal, grading, and persisted session completion. Batch33 adds a state-aware keyboard workflow for the complete Study interaction: Enter/Space starts or reveals, and 1–4 grades Again through Easy, with visible hints and recoverable-error protection. Batch34 replaces the ambiguous idle placeholder with an explicit, actionable Study start state that shares the verified keyboard contract. Batch35 turns persisted-data failures into a dedicated recoverable error presentation and allows Enter or Space to retry loading without restarting the application. Batch36 adds polite screen-reader announcements for each major Study workflow state and semantic lesson-progress descriptions. Batch37 adds explicit screen-reader descriptions and exact shortcut guidance to every Study action control. Batch38 explains the scheduling meaning of Again, Hard, Good, and Easy at the exact point where the learner chooses a grade, with matching screen-reader and 1–4 shortcut context. Batch39 adds screen-reader confirmation that the latest rating was persisted, including its scheduling result. Batch40 exposes the complete visible scheduler feedback card through one ordered semantic description. Batch41 labels the active learning content as a prompt and the revealed translation as an answer without weakening the reveal boundary. Batch42 keeps the Study keyboard surface focused after start, reveal, grade, retry, item advance, and completion. Batch43 makes the completed-session result readable as one ordered semantic summary. After Batch43 passes, continue Desktop Beta UX hardening with the next highest-value workflow-state or release-readiness increment supported by the real source.

# Source of Truth

The current repository source and `docs/` are authoritative. After each successful batch, the applied files become the realtime baseline.

# AI Startup Rules

Khi người dùng nói 'Tiếp tục dự án.', hãy đọc docs rồi toàn bộ source, tiếp tục capability tiếp theo, không yêu cầu giải thích lại workflow nếu tài liệu đã đầy đủ.


## Latest verified increment target

Batch44 hardens scheduler feedback rendering by replacing the corrupted stage-transition text with one tested UTF-8 formatter shared by visible and accessibility output. After verification, continue the Desktop Beta robustness audit from the Batch44 baseline.


## Batch45 handoff

Batch45 closes the first-run Content Library empty-state gap with a direct import action and one semantic description. After verification, continue the Desktop Beta robustness audit from the Batch45 baseline.


## Batch46 handoff

Batch46 makes Content Library repository/load failures recoverable through a dedicated retry card without changing import-diagnostic behavior. After verification, continue the Desktop Beta robustness audit from the Batch46 baseline.


## Batch47 handoff

Batch47 makes Review History understandable as complete semantic units without changing review data, ordering, or metrics. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch47 baseline.


## Batch48 handoff

Batch48 makes the Statistics screen readable in visual order and removes ambiguous spoken placeholders while preserving the existing UI and statistics model. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch48 baseline.


## Batch49 handoff

Batch49 makes the read-only Settings configuration understandable as ordered semantic label/value units without changing configuration behavior or visual layout. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch49 baseline.


## Batch50 handoff

Batch50 makes desktop navigation state explicit to assistive technology while preserving the existing destination model and click behavior. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch50 baseline.


## Batch51 handoff

Batch51 completes semantic grouping for the persistent shell header and status bar without changing shell layout or runtime state. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch51 baseline.


## Batch52 handoff

Batch52 makes the Dashboard overview hierarchy and metric cards understandable as ordered semantic units without changing analytics calculations or layout. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch52 baseline.


## Batch53 handoff

Batch53 makes Dashboard visualization containers, empty states, and the retention gauge understandable without relying on visual chart rendering. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch53 baseline.


## Batch54 handoff

Batch54 makes individual Dashboard chart values and heatmap days understandable without interpreting bar length, color, or cell intensity. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch54 baseline.


## Batch55 handoff

Batch55 makes Lesson Browser navigation and lesson details understandable as ordered semantic units without changing content mapping or study routing. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch55 baseline.


## Batch56 handoff

Batch56 makes Content Library page structure and import outcomes understandable without relying on visual grouping or color. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch56 baseline.


## Batch57 handoff

Batch57 makes every Content Library collection/package dialog understandable without relying on visual grouping, checkmarks, or warning color. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch57 baseline.


## Batch58 handoff

Batch58 makes Content Library cards and package properties understandable without visually scanning multiple columns, while keeping every card action independently accessible. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch58 baseline.


## Batch59 handoff

Batch59 makes every Content Library action explicit to assistive technology, including the affected library, collection, or package and whether confirmation follows. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch59 baseline.


## Batch60 handoff

Batch60 gives Content Library a documented keyboard surface for refresh, package import, and hierarchical back navigation without interfering with active dialogs. After verification, continue the Desktop Beta accessibility and robustness audit from the Batch60 baseline.


## Batch61 handoff

Batch61 accelerates Desktop Beta hardening by completing the shell-wide keyboard navigation epic in one increment: direct destination keys, cyclic traversal, current-screen refresh, stable focus, and synchronized sidebar hints. Continue with similarly broad UX epics rather than one-control batches.


## Batch62 handoff

Batch62 begins the accelerated epic workflow. It changes the shared UX contract across Dashboard, Statistics, Review History, ContentHost, and LearningShell in one increment, with reusable state presentation and cross-screen tests. Continue using broad, coherent epics that touch all affected screens rather than isolated one-control batches.


## Batch63 search and discovery epic
Batch63 adds shared search normalization and result announcements, Review History query/rating/sort controls, and Lesson Browser query/translation/sort controls with deterministic pure projections and regression tests.

## Batch64 search refinement reset epic

Batch64 adds a shared active-refinement contract and a single accessible Reset view action to Review History and Lesson Browser. The action restores query, filter, and sort defaults together. Continue broad Desktop Beta release-readiness epics from the verified Batch64 baseline.


## Batch65 actionable search recovery epic

Batch65 turns zero-result search states into accessible recovery surfaces. Review History and Lesson Browser now offer only the actions that can restore results while genuinely empty datasets remain honest and action-free. Continue broad Desktop Beta release-readiness epics from the verified Batch65 baseline.
