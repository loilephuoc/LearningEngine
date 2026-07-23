# Learning Engine 2.0 — Domain Layer Guide

## Purpose
This document serves as the authoritative implementation guide for developer work within the **Domain Layer** (`vn.loi.learning.domain.*`) of Learning Engine 2.0.

## Responsibilities
- Define the rules for creating Domain Entities, Aggregates, Value Objects, Domain Events, and Repository interfaces.
- Enforce invariant validation and immutability standards.
- Maintain absolute independence from framework dependencies, persistence infrastructure, and UI libraries.

## Out of Scope
- Database ORM mappings or JSON DTO schemas (see [`08_INFRASTRUCTURE_LAYER_GUIDE.md`](08_INFRASTRUCTURE_LAYER_GUIDE.md)).
- Use case orchestration (see [`07_APPLICATION_LAYER_GUIDE.md`](07_APPLICATION_LAYER_GUIDE.md)).

## Dependencies
- Strategic Domain-Driven Design (DDD) principles.
- [`00_ARCHITECTURE_CONSTITUTION.md`](00_ARCHITECTURE_CONSTITUTION.md)

---

## Domain Layer Structural Blueprint

The Domain Layer is structured into cohesive domain packages:
```text
vn.loi.learning.domain/
├── content/
│   ├── model/           # Content Entity, ContentId, ContentType, ContentText
│   └── topic/
│       └── model/       # Topic Aggregate, TopicId, TopicMetadata
├── study/
│   ├── learning/
│   │   └── model/       # LearningItem, LearningItemId, LearningMode
│   ├── session/
│   │   └── model/       # LearningSession Aggregate, SessionQueue, SessionState
│   └── scheduler/
│       └── model/       # FSRS MemoryState, Rating, Interval, FsrsScheduler
├── library/
│   └── model/           # Library Aggregate, Collection, InstalledPackage
├── workspace/
│   └── model/           # Workspace Aggregate, StudySource, SelectionSettings
└── common/
    └── event/           # DomainEvent interface, Core Event Contracts
```

---

## Domain Building Block Rules

### 1. Value Objects
- Must be immutable (`data class` with `val` properties).
- Must encapsulate domain validation in the `init` block.
- Must have structural equality.
- Example:
  ```kotlin
  data class ContentId(val value: String) {
      init {
          require(value.isNotBlank()) { "ContentId value must not be blank." }
      }
  }
  ```

### 2. Entities & Aggregates
- Every Aggregate Root must have a unique Value Object identity (e.g., `TopicId`, `WorkspaceId`).
- Mutations on Aggregates must happen through explicit domain methods that return a new updated state copy or emit Domain Events.
- Aggregates enforce invariants across internal entities.
- Example:
  ```kotlin
  data class Workspace(
      val id: WorkspaceId,
      val name: String,
      val studySources: List<StudySource>,
      val settings: WorkspaceSettings
  ) {
      init {
          require(name.isNotBlank()) { "Workspace name must not be blank." }
      }

      fun addSource(source: StudySource): Workspace {
          require(studySources.none { it.id == source.id }) { "Duplicate study source in workspace." }
          return copy(studySources = studySources + source)
      }
  }
  ```

### 3. Domain Services
- Stateless domain logic that does not naturally belong inside a single Entity or Value Object.
- Pure functions without side effects or IO operations.
- Example: `FsrsScheduler` calculating memory stability transitions.

### 4. Domain Events
- Represent immutable facts that occurred in the domain.
- Named in the past tense (e.g., `ReviewCompleted`, `PackageInstalled`).
- Example:
  ```kotlin
  data class ReviewCompletedEvent(
      val itemId: LearningItemId,
      val rating: Rating,
      val elapsedMs: Long,
      val timestamp: Long
  ) : DomainEvent
  ```

### 5. Repository Interfaces (Ports)
- Interface abstractions defined in the domain/application boundary for storing and retrieving Aggregates.
- Uses domain types only; no DTOs or database cursors.
- Example:
  ```kotlin
  interface WorkspaceRepository {
      fun findById(id: WorkspaceId): Workspace?
      fun save(workspace: Workspace)
  }
  ```

---

## Anti-Patterns to Avoid
1. **Public Mutable Var Properties**: Never use `var` in domain entities; use copy-on-write immutable methods.
2. **Nullable ID Types**: Entity identities must never be null or empty.
3. **Leaking Infrastructure**: Never import `kotlinx.serialization`, `File`, `Context`, or database classes into domain packages.

---

## Future Evolution
The Domain Layer will expand as new capabilities (Sync, Marketplace) are added, maintaining 100% purity and framework isolation.

---

## Architecture Notes
- Every domain rule MUST be covered by focused unit tests in `src/test/kotlin/`.
