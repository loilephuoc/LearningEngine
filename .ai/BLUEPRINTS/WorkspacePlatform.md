# Workspace Platform Blueprint

## Purpose
The **Workspace Platform** subsystem manages active learner study environments (**Workspaces**), allowing learners to select, configure, and isolate specific study sources (topics or collections) for daily learning sessions.

## Responsibilities
- Create, update, and manage Workspace Aggregates.
- Bind installed Library topics and collections as active `StudySource` records.
- Configure daily study limits (e.g., maximum new cards per day, maximum review cards per day).
- Serve as the **Sole Study Source Provider** for active Learning Sessions.

## Out of Scope
- Raw package extraction or cataloging (handled by Library Platform).
- FSRS rating calculations or interactive card presentation (handled by Learning Session Platform).

## Dependencies
- Library Platform (`LibraryRepository`, `InstalledPackage`).
- Domain model (`WorkspaceId`, `Workspace`, `StudySource`).

---

## Subsystem Architecture & Components

```text
+-----------------------------------------------------------------------------------+
|                                WORKSPACE PLATFORM                                 |
|                                                                                   |
|  +------------------------------+             +--------------------------------+  |
|  |    CreateWorkspaceUseCase    |============>|    ConfigureStudySourceUseCase |  |
|  |    (Workspace Creation)      |             |    (Topic / Collection Binding)|  |
|  +--------------+---------------+             +---------------+----------------+  |
|                 |                                             |                   |
|                 v                                             v                   |
|  +------------------------------+             +--------------------------------+  |
|  |     WorkspaceAggregate       |============>|     WorkspaceRepository        |  |
|  |     (Domain Entity Root)     |             |     (Persistence Adapter)      |  |
|  +------------------------------+             +--------------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## Data Models & Schema Contracts

### Workspace Aggregate Root
- `id`: `WorkspaceId` (unique identifier).
- `name`: User-assigned workspace title (e.g., "Japanese Vocabulary Focus").
- `description`: Optional workspace description.
- `studySources`: List of bound `StudySource` items.
- `settings`: `WorkspaceSettings` (newCardsPerDay, maxReviewsPerDay, siblingAvoidanceEnabled).
- `createdAt`: Timestamp of creation.

### StudySource Value Object
- `id`: `StudySourceId`.
- `sourceType`: `TOPIC` or `COLLECTION`.
- `targetId`: Associated `TopicId` or `CollectionId`.
- `enabled`: Boolean toggle status.

---

## Invariants & Validation Rules
- Workspace name must not be blank.
- Daily card limits must be positive integers (`newCardsPerDay > 0`).
- Duplicate study source bindings within the same Workspace are rejected.

---

## Future Evolution
- Support for multi-workspace scheduling presets (e.g., Exam Mode vs Maintenance Mode).
- Support for workspace export/import across learner devices.

---

## Architecture Notes
- Learning sessions MUST bind to a Workspace Aggregate rather than querying raw library tables directly.
