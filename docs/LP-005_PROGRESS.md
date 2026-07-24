# LP-005 Progress — Knowledge Graph Foundation

**Phase**: LP-005 — Knowledge Graph Foundation
**Branch**: `develop`
**Baseline HEAD**: `c3945fa3d94df26aa646ae3e857251d9235a80dd`

---

## Scope

Introduce an immutable, learner-state-free knowledge graph that describes how
installed learning content is structured. Provide domain models, traversal
analysis, JSON persistence, application wiring, and an installed-library
projection.

---

## Checkpoint Summary

| # | Checkpoint | Status | Commit |
|---|---|---|---|
| 1 | Domain Models (nodes, edges, kinds, relationships, validation) | ✅ Complete | `9976529` |
| 2 | Deterministic Query API (findNode, edges, containsNode, nodesBy, edgesFrom/To) | ✅ Complete | `9976529` |
| 3 | Graph Traversal and Analysis (BFS/DFS, shortest path, topo order, transitive successors) | ✅ Complete | `9976529` |
| 4 | JSON Persistence (records, mapper, store, atomic write, envelope, restart-safe) | ✅ Complete | `9976529` |
| 5 | Application Wiring (use cases, query service, factory, context) | ✅ Complete | `c18f373` |
| 6 | Installed Library Projection (active packages → PACKAGE nodes) | ✅ Complete | `d39725a` |
| 7 | Final Documentation and Gates (progress update, clean test, diff patch) | ✅ Complete | `docs commit` |

---

## Commits

1. `9976529` — `feat(lp-005): introduce knowledge graph core`
   - Domain models (Checkpoints 1–3), persistence (Checkpoint 4), 75 tests.
2. `c18f373` — `feat(lp-005): persist and wire knowledge graph foundation`
   - Application use cases, query service, factory wiring, progress docs.
3. `d39725a` — `feat(lp-005): project installed library as knowledge graph nodes`
   - InstalledLibraryKnowledgeGraphProjection (Checkpoint 6), 8 tests.

---

## Test Evidence

| Test Suite | Tests | Failures |
|---|---|---|
| `KnowledgeGraphDomainTest` | 37 | 0 |
| `KnowledgeGraphAnalyzerTest` | 26 | 0 |
| `KnowledgeGraphRecordMapperTest` | 7 | 0 |
| `JsonKnowledgeGraphStoreTest` | 5 | 0 |
| `InstalledLibraryKnowledgeGraphProjectionTest` | 8 | 0 |
| **Total LP-005** | **83** | **0** |

Full main-module test: `.\gradlew.bat :test` → BUILD SUCCESSFUL (existing baseline passes).
Full clean test: `.\gradlew.bat clean test -x :desktop:test` → BUILD SUCCESSFUL.

---

## Delivered Contracts

### Domain Layer (`vn.loi.learning.domain.knowledge`)

| File | Responsibility |
|---|---|
| `KnowledgeNodeId` | `@JvmInline` value class; blank guard; identity |
| `KnowledgeNodeKind` | 5 kinds: PACKAGE, TOPIC, LESSON, RESOURCE, COLLECTION |
| `KnowledgeNode` | Immutable; equality on (id, kind), not displayName |
| `KnowledgeRelationshipType` | 7 typed directed relationships |
| `KnowledgeEdge` | Directed typed edge: sourceId → targetId |
| `KnowledgeGraphValidationIssue` | 5 sealed subtypes (duplicate, self-edge, missing source/target) |
| `KnowledgeGraphValidationResult` | Valid / Invalid typed result |
| `KnowledgeGraph` | Immutable aggregate; internal constructor; deterministic ordering |
| `KnowledgeGraphFactory` | Validates before build; typed result, no exceptions |
| `KnowledgeGraphRepository` | Domain port interface |
| `KnowledgeGraphAnalyzer` | BFS/DFS, shortest path, topological order (Kahn's), transitive successors |

### Infrastructure Layer (`vn.loi.learning.infrastructure.persistence`)

| File | Responsibility |
|---|---|
| `KnowledgeNodeRecord` | Persistence DTO |
| `KnowledgeEdgeRecord` | Persistence DTO |
| `KnowledgeGraphRecord` | Persistence DTO (top-level) |
| `KnowledgeGraphRecordMapper` | Domain ↔ record round-trip; defensive on unknown enum names |
| `KnowledgeGraphStore` | Store port interface |
| `InMemoryKnowledgeGraphStore` | In-memory store (test/stub) |
| `JsonKnowledgeGraphStore` | Envelope-versioned, atomic write, missing-file safe |
| `StoreBackedKnowledgeGraphRepository` | Bridges repository port to JSON store |

### Application Layer (`vn.loi.learning.application.knowledge`)

| File | Responsibility |
|---|---|
| `GetKnowledgeGraphUseCase` | Load graph from repository |
| `SaveKnowledgeGraphUseCase` | Persist graph through repository |
| `KnowledgeGraphQueryService` | Read-only query facade (nodes, edges, traversal) |
| `InstalledLibraryKnowledgeGraphProjection` | ACTIVE packages → PACKAGE KnowledgeNodes (flat) |

### Wiring (`LearningApplicationContext`, `LearningApplicationFactory`)

- `LearningApplicationContext` has optional fields: `knowledgeGraphQuery`,
  `saveKnowledgeGraph`, `getKnowledgeGraph`, `installedLibraryGraphProjection`.
- `LearningApplicationFactory.createContext` wires the full graph stack
  (JsonKnowledgeGraphStore → StoreBackedKnowledgeGraphRepository →
  GetKnowledgeGraphUseCase / SaveKnowledgeGraphUseCase / KnowledgeGraphQueryService)
  and always provides `InstalledLibraryKnowledgeGraphProjection`.

---

## Architecture Invariants

- `KnowledgeGraph` describes knowledge structure only; no learner state.
- Domain does not depend on infrastructure, JSON, filesystem, Desktop, or Android.
- Identity is not tied to display names.
- All graph traversal results are deterministic (sorted by canonical IDs).
- Persistence is envelope schema-versioned; no legacy array format for this store.
- Recovery from missing file returns an empty graph (safe default).
- `InstalledLibraryKnowledgeGraphProjection` is a read-only snapshot;
  it does not mutate any domain state.

---

## Deferred / Out of Scope

- Edge types from OPD3 content structure (TOPIC CONTAINS LESSON, etc.) — requires
  a content-aware projection service with a separate product decision.
- Topic hierarchy inference from `ContentPackageRepository` — no structured
  topic-to-lesson relationship available without OPD3 content parsing.
- Cross-graph merge or multi-graph federation.
- Desktop UI integration for knowledge graph visualization.

---

## Recovery Note

Session was interrupted during Checkpoint 5 (task-208 from previous run).
WIP was recovered intact from the working tree without reset, restore, or stash loss.
All LP-005 work committed cleanly after recovery as 3 coherent capability commits.
No patch files were committed. Nothing was pushed.
