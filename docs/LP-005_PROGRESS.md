# LP-005 Progress — Knowledge Graph Foundation

## Status

| Checkpoint | Description | Status |
|-----------|-------------|--------|
| 1 | Immutable Graph Domain | ✅ COMMITTED |
| 2 | Deterministic Query API | ✅ COMMITTED (co-located with CK1) |
| 3 | Graph Traversal and Analysis | ✅ COMMITTED (co-located with CK1) |
| 4 | Graph Persistence | ✅ COMMITTED |
| 5 | Application Wiring | ✅ COMMITTED |
| 6 | Installed Library Projection | 🔲 NOT STARTED |
| 7 | Final Documentation and Gates | 🔲 NOT STARTED |

## Deliverables by Checkpoint

### Checkpoint 1–3 (Domain: `feat: introduce immutable knowledge graph domain`)

**Domain models:**
- `domain/knowledge/model/KnowledgeNodeId` — @JvmInline value class, blank guard
- `domain/knowledge/model/KnowledgeNodeKind` — 5 kinds: PACKAGE, TOPIC, LESSON, RESOURCE, COLLECTION
- `domain/knowledge/model/KnowledgeNode` — immutable; equality on (id, kind), NOT displayName
- `domain/knowledge/model/KnowledgeRelationshipType` — 7 typed relationships
- `domain/knowledge/model/KnowledgeEdge` — directed typed edge
- `domain/knowledge/model/KnowledgeGraphValidationIssue` — 5 sealed subtypes
- `domain/knowledge/model/KnowledgeGraphValidationResult` — Valid/Invalid sealed result
- `domain/knowledge/model/KnowledgeGraph` — immutable, deterministic ordering, internal constructor
- `domain/knowledge/model/KnowledgeGraphFactory` — validates and builds; no exceptions for business errors

**Service:**
- `domain/knowledge/service/KnowledgeGraphAnalyzer` — cycle-safe traversal, shortest path, topo order, transitive successors

**Domain repository interface:**
- `domain/knowledge/repository/KnowledgeGraphRepository`

**Tests:**
- `domain/knowledge/KnowledgeGraphDomainTest` — 30+ tests for domain + query API
- `domain/knowledge/KnowledgeGraphAnalyzerTest` — 25+ tests for traversal + analysis

### Checkpoint 4 (Persistence)

- `infrastructure/persistence/record/KnowledgeNodeRecord`
- `infrastructure/persistence/record/KnowledgeEdgeRecord`
- `infrastructure/persistence/record/KnowledgeGraphRecord`
- `infrastructure/persistence/mapper/KnowledgeGraphRecordMapper`
- `infrastructure/persistence/store/KnowledgeGraphStore` (interface)
- `infrastructure/persistence/store/InMemoryKnowledgeGraphStore`
- `infrastructure/persistence/json/JsonKnowledgeGraphStore`
- `infrastructure/persistence/repository/StoreBackedKnowledgeGraphRepository`

**Tests:**
- `infrastructure/persistence/mapper/KnowledgeGraphRecordMapperTest`
- `infrastructure/persistence/json/JsonKnowledgeGraphStoreTest`

### Checkpoint 5 (Application Wiring)

- `application/knowledge/SaveKnowledgeGraphUseCase`
- `application/knowledge/GetKnowledgeGraphUseCase`
- `application/knowledge/KnowledgeGraphQueryService`
- `LearningApplicationContext` — added `knowledgeGraphQuery`, `saveKnowledgeGraph`, `getKnowledgeGraph` (null-defaulted for backward compatibility)
- `LearningApplicationFactory` — wired `StoreBackedKnowledgeGraphRepository` in `createPersisted`; optional param in `createContext`

## Safety Invariants Verified

- KnowledgeGraph has no learner state
- Domain does not depend on infrastructure, JSON, filesystem, Desktop, Android
- Identity does not depend on display name
- All returned collections are immutable (sorted deterministic lists)
- All queries are cycle-safe
- Factory validates before build; no silent silent data mutation
- Unknown kinds/relationship types are defensively skipped during load
