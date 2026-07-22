# Capability Map

Use this map to select the smallest source neighborhood that can safely implement a
capability. It is an orientation document; actual source and tests remain authoritative.

## Dependency direction

```text
Desktop and JVM adapters
        ↓
Application use cases and ports
        ↓
Domain model and services

Infrastructure implements application ports.
```

Do not make domain or application code depend on Compose Desktop or concrete JSON storage.

## Study and review

**Responsibility**

- lesson-scoped study sessions;
- queue lifecycle and next-item selection;
- prompt reveal and rating;
- review transitions and persisted scheduler decisions;
- session completion and restart recovery.

**Read when changing**

- study/session domain and application use cases;
- queue planner and repository contracts;
- scheduler and review integration;
- persistence adapters for session, queue, and review state;
- Desktop Study state, presentation, keyboard, accessibility, and composition wiring;
- restart and lesson-isolation integration tests.

**Direct dependencies**

Content identity, persistence, scheduler, Desktop composition root.

**High-risk contracts**

Atomic review persistence, lesson isolation, deterministic resume, rating order, and
existing persisted-data compatibility. The domain session owns `ACTIVE`/`FINISHED` plus its
durable current-item checkpoint. Desktop `ReviewWorkspaceState` is a projection and must not
become a second lifecycle, scheduler, or persistence owner.

**Learning content seam**

Canonical content lives under `domain/content/model`; package and persistence records are DTOs.
`application/learningcontent` owns the ordered learner-facing block projection consumed by
Desktop Study. Rich renderers must consume that projection rather than parse OPD3 fields or
invent content semantics in Compose.

## Scheduling and memory state

**Responsibility**

- scheduler decisions;
- intervals and next-review timestamps;
- memory-stage transitions;
- validation and metrics.

**Read when changing**

Scheduler interfaces and implementations, decision validators, review integration,
scheduling tests, and persistence mappings that store scheduler output.

**High-risk contracts**

Time semantics, deterministic decisions, compatibility with stored review state.

## Content package and OPD3 import

**Responsibility**

- package scanning and validation;
- manifest and metadata identity;
- import, registration, media, and installed-package queries;
- diagnostics for partial, invalid, and incompatible input.

**Read when changing**

Package models, scanners, validators, importers, repositories, JVM adapters, Desktop
import wiring, and real-package integration fixtures.

**Direct dependencies**

Content identity, persistence, Content Library.

**High-risk contracts**

Format compatibility, duplicate identity behavior, partial writes, actionable diagnostics.

## Content Library and Lesson Browser

**Responsibility**

- libraries and collections;
- package attachment and detachment;
- lesson hierarchy and selection;
- navigation from content into lesson-scoped Study.

**Desktop search neighborhood**

- `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/contentlibrary/`
- shared search presentation under
  `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/search/`
- corresponding tests under `desktop/src/test/kotlin/...`

**Direct dependencies**

Installed packages, lesson/content queries, Desktop navigation, Study start boundary.

## Search and discovery

**Responsibility**

- normalized query terms;
- AND matching;
- filtering and sorting;
- result status, guidance, refinements, recovery, keyboard behavior, and highlighting.

**Known production files from the verified Batch73 boundary**

- `SearchQueryTerms.kt`
- `SearchText.kt`
- `SearchMatchPresentation.kt`
- `SearchQueryGuidancePresentation.kt`
- `LessonBrowserProjection.kt`
- `ReviewHistoryProjection.kt`

**Read when changing**

The complete shared search package, both screen projections and state holders, their
Compose surfaces, and all corresponding tests.

**High-risk contracts**

Normalization consistency between matching and highlighting, multi-field row matching,
stable sort order, and genuinely-empty versus no-match recovery.

## Review History

**Responsibility**

- review-history query and presentation;
- result filtering, sorting, semantics, and recovery;
- review event summaries.

**Read when changing**

Review-history application queries, Desktop state/projection/presentation, shared search
contracts, loading-state contracts, and tests.

## Dashboard, statistics, and analytics

**Responsibility**

- metrics and analytics queries;
- dashboard projections and chart presentation;
- loading, failure, refresh, stale-data, and accessibility contracts.

**Read when changing**

Analytics/application queries, calculators, Desktop view models and charts, shell refresh
routing, shared data-state presentation, and tests.

## Persistence and recovery

**Responsibility**

- JSON stores and mappings;
- repositories and transactions;
- atomic review/session updates;
- restart reconciliation;
- incompatible and corrupt-data reporting.

**Read when changing**

Record models, mappers, stores, repository implementations, factories/composition roots,
restart integration tests, and every consumer of a changed persisted contract.

**High-risk contracts**

Backward compatibility, atomicity, partial-write recovery, and data-loss prevention.

**Phase 6 session lifecycle seam**

`StudySession` owns its durable current-item/reveal/pending-review checkpoint. Application
session use cases own presentation, staging, atomic application, and startup replay. Desktop
must cross `LearningEngine` for reveal/recovery and must not create a parallel lifecycle owner.

## Desktop shell and navigation

**Responsibility**

- screen composition and navigation state;
- global shortcuts and active-screen refresh;
- persistent header/status presentation;
- focus and screen-level wiring.

**Read when changing**

Learning shell, content host, navigation state, shortcut routing, screen composition roots,
and cross-screen tests.

## Desktop runtime foundation

**Responsibility**

- application identity and generated build metadata;
- platform-aware runtime directories;
- typed Desktop configuration;
- logging, lifecycle, and runtime diagnostics.

**Read when changing**

`desktop/build.gradle.kts`, `desktop.runtime`, `DesktopMain`, Desktop composition, Settings/About
presentation, and runtime-focused unit/integration/restart tests.

**High-risk contracts**

Stable application/directory identity, no implicit data migration, corrupt-config preservation,
log privacy/retention, deterministic startup/shutdown, and support diagnostics.

## Settings and configuration

**Responsibility**

- visible configuration and presentation;
- configuration persistence or runtime wiring when present.

Do not turn the read-only presentation into a mutable contract without implementing
validation, persistence, restart behavior, and tests in the same increment.

## Map maintenance

Standing capability workflow lives in [`../AGENTS.md`](../AGENTS.md). Update this map when
source inspection establishes a new production neighborhood, direct dependency, composition
root, or high-risk contract.
