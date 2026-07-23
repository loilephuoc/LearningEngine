# Learning Engine 2.0 — Domain Context Map

## Purpose
The **Domain Context Map** identifies the Bounded Contexts within Learning Engine 2.0, defines their strategic relationships, and establishes allowed dependency directions and integration patterns.

## Responsibilities
- Define the boundaries of all primary domain contexts.
- Document relationship patterns (Upstream/Downstream, Customer/Supplier, Anti-Corruption Layer, Shared Kernel).
- Enforce clean integration rules between bounded contexts.

## Out of Scope
- Detailed method signatures inside individual classes.
- UI layout hierarchy.

## Dependencies
- Strategic DDD Context Mapping principles.
- [`01_SYSTEM_VISION.md`](01_SYSTEM_VISION.md)

---

## Bounded Context Map Diagram

```text
+-----------------------------------------------------------------------------------+
|                               BOUNDED CONTEXT MAP                                 |
|                                                                                   |
|  +---------------------------+             +----------------------------------+  |
|  | Package Platform Context  |============>|    Library Platform Context      |  |
|  | (OPD3 Export/Inspection)  |  (Upstream) | (Catalog, Collections, Version)  |  |
|  +---------------------------+             +----------------+-----------------+  |
|                                                             |                    |
|                                                             | (Upstream Supplier)|
|                                                             v                    |
|  +---------------------------+             +----------------------------------+  |
|  | Analytics & History       |<------------|    Workspace Platform Context    |  |
|  | Context (Review Logs)     | (Downstream)| (Active Study Sources & Binding) |  |
|  +--------------^------------+             +----------------+-----------------+  |
|                 |                                           |                    |
|                 | (Events: ReviewCompleted)                 | (Upstream Supplier)|
|                 |                                           v                    |
|  +--------------+------------+             +----------------------------------+  |
|  | FSRS Scheduler Domain     |<------------|    Learning Session Context      |  |
|  | (Algorithm & Memory State)| (Downstream)| (Review Queue & Presentation)    |  |
|  +---------------------------+             +----------------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## Bounded Context Descriptions

### 1. Package Platform Context
- **Boundary**: Raw `.opd3` package byte streams, package building, media asset collection, streaming inspection, path security validation, SHA-256 manifest calculation, and byte-for-byte deterministic ZIP export.
- **Upstream / Downstream**: Upstream to Library Platform.
- **Integration Pattern**: Produces immutable `CanonicalTopicPackage` models and valid `.opd3` file archives consumed by downstream importers.

### 2. Library Platform Context
- **Boundary**: Persistence registry of installed packages, topic cataloging, collection organization, tag filtering, version tracking, and package deletion/archival.
- **Upstream / Downstream**: Downstream to Package Platform (Customer/Supplier); Upstream to Workspace Platform.
- **Integration Pattern**: Exposes catalog query APIs (`findInstalledPackages()`, `getTopicDetails()`) to Workspace Platform. Uses Anti-Corruption Layer (ACL) when importing raw legacy package formats.

### 3. Workspace Platform Context
- **Boundary**: Configuration of active learner study environments, binding specific Library topics/collections, setting daily card limits, and isolating study source selection.
- **Upstream / Downstream**: Downstream to Library Platform; Upstream Supplier to Learning Session Context.
- **Integration Pattern**: Provides `getWorkspaceStudyItems(workspaceId)` port to Learning Session Context.

### 4. Learning Session Context
- **Boundary**: Interactive study execution, active review queue management, sibling item spacing, card presentation rhythm, prompt/reveal state machine, and rating capture.
- **Upstream / Downstream**: Downstream to Workspace Platform (Customer) and FSRS Scheduler; Upstream event publisher to Analytics & History Context.
- **Integration Pattern**: Consumes Workspace items, invokes FSRS Scheduler for memory state calculations, and emits `ReviewCompleted` domain events.

### 5. FSRS Scheduler Domain
- **Boundary**: Pure mathematical memory calculations (Free Spaced Repetition Scheduler algorithm), computing stability, difficulty, retrievability, due interval, and state transitions.
- **Upstream / Downstream**: Downstream core domain service invoked by Learning Session Context.
- **Integration Pattern**: Pure domain function inputs/outputs (`calculateNextState(previousState, rating)`). Zero database or IO coupling.

### 6. Analytics & Review History Context
- **Boundary**: Append-only log storage of completed reviews, historical performance queries, retention rate analytics, streak calculation, and study metrics.
- **Upstream / Downstream**: Downstream subscriber to `ReviewCompleted` events emitted by Learning Session Context.
- **Integration Pattern**: Asynchronous domain event listener updates persistent review log stores and recalculates analytics caches.

---

## Integration & Dependency Rules

1. **Strict Upstream/Downstream Flow**: Downstream contexts may depend on Upstream APIs/Ports, but Upstream contexts MUST NOT import or depend on Downstream code.
2. **Anti-Corruption Layer (ACL)**: When integrating external package formats (e.g., OPD3 legacy pairs, Anki packages), an ACL must map raw external DTOs into pure domain models before entering the core system.
3. **Event-Driven Decoupling**: Learning Sessions communicate review outcomes to Analytics via immutable Domain Events (`ReviewCompleted`), eliminating direct database calls between Session and Analytics contexts.

---

## Future Evolution
Future Bounded Contexts will integrate into this map following strict context boundaries:
- **Sync Context**: Downstream subscriber to domain change events, using ACL to interface with cloud storage APIs.
- **Marketplace Context**: Upstream package provider interfacing directly with Package Platform export/import ports.

---

## Architecture Notes
- Cross-context calls MUST go through explicit Application Ports or Domain Events.
- Shared domain types (e.g., `TopicId`, `ContentId`, `LearningItemId`) reside in shared domain value object packages (`vn.loi.learning.domain.*`).
