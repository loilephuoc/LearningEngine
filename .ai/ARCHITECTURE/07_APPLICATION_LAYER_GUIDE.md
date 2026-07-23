# Learning Engine 2.0 — Application Layer Guide

## Purpose
This document provides implementation guidance for the **Application Layer** (`vn.loi.learning.application.*`) of Learning Engine 2.0.

## Responsibilities
- Orchestrate domain entities, domain services, and repository ports to fulfill application use cases.
- Enforce transaction boundaries and idempotency.
- Separate Command workflows (state mutations) from Query workflows (read-only reporting).
- Define Application Ports for infrastructure adapters.

## Out of Scope
- Direct SQL/file persistence code (see [`08_INFRASTRUCTURE_LAYER_GUIDE.md`](08_INFRASTRUCTURE_LAYER_GUIDE.md)).
- UI rendering or Composable state hoisters (see [`09_PRESENTATION_LAYER_GUIDE.md`](09_PRESENTATION_LAYER_GUIDE.md)).

## Dependencies
- [`00_ARCHITECTURE_CONSTITUTION.md`](00_ARCHITECTURE_CONSTITUTION.md)
- [`06_DOMAIN_LAYER_GUIDE.md`](06_DOMAIN_LAYER_GUIDE.md)

---

## Application Layer Architecture

The Application Layer is organized around bounded context use cases and application ports:

```text
vn.loi.learning.application/
├── contentpackaging/      # Package Exporter, Inspector, Verifier, PathValidator
├── contentlibrary/        # Library Management Use Cases, Collection Ports
├── workspace/             # Workspace Orchestration Use Cases
├── study/                 # Learning Session Orchestration & Review Handling
└── statistics/            # Performance Queries & Heatmap Use Cases
```

---

## Key Application Concepts & Rules

### 1. Use Cases (Application Services)
- Single-purpose application workflows orchestrating domain models and ports.
- Explicit inputs (Commands/Queries) and outputs (Results/DTOs).
- Owns transaction management and exception mapping.
- Example:
  ```kotlin
  class ProcessReviewUseCase(
      private val sessionRepository: LearningSessionRepository,
      private val fsrsScheduler: FsrsScheduler,
      private val eventPublisher: DomainEventPublisher
  ) {
      fun execute(command: SubmitReviewCommand): ProcessReviewResult {
          val session = sessionRepository.findActiveSession(command.sessionId)
              ?: return ProcessReviewResult.SessionNotFound

          val updatedSession = session.processReview(command.itemId, command.rating, fsrsScheduler)
          sessionRepository.save(updatedSession)
          eventPublisher.publish(ReviewCompletedEvent(...))

          return ProcessReviewResult.Success(updatedSession.currentCard)
      }
  }
  ```

### 2. Commands vs Queries (CQRS Principles)
- **Commands**: Intent to mutate state (e.g., `InstallPackageCommand`, `CreateWorkspaceCommand`). Returns success/failure status or generated ID.
- **Queries**: Intent to fetch data without mutating state (e.g., `GetLibraryCatalogQuery`, `GetStudyStatisticsQuery`). Returns read-only DTOs.

### 3. Application Ports (Interfaces)
- Defined by the Application Layer to express dependencies on external capabilities (e.g., persistence, package files, clock, integrity hashing).
- Implemented by Infrastructure Layer adapters.
- Example: `PackageIntegrityHasher`, `MediaByteReader`, `WorkspaceRepository`.

### 4. Deterministic Error Handling & Diagnostics
- Application use cases must return explicit result objects (`sealed interface Result`) or structured diagnostics containing severity codes (`FATAL`, `WARNING`), rather than throwing untyped raw exceptions to presentation clients.

---

## Anti-Patterns to Avoid
1. **Leaking Domain Objects to Remote APIs**: Map internal domain entities into clean DTOs when communicating across remote boundaries.
2. **Bypassing Application Layer**: Presentation views calling infrastructure adapters directly without an application use case.
3. **Implicit Transactions**: Performing partial writes across multiple repositories without transaction ownership.

---

## Future Evolution
Application use cases will remain unchanged as presentation clients evolve (Desktop to Mobile/Web), serving as the universal platform API.

---

## Architecture Notes
- All application use cases MUST be covered by integration tests in `src/test/kotlin/`.
