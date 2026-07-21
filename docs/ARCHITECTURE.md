# Architecture

## Build modules

The Gradle build has two modules:

1. Root project
   - Domain model and domain services
   - Application use cases and ports
   - Infrastructure and persistence adapters
   - JVM adapters and command-line entry points
   - Automated tests

2. `desktop`
   - Compose Desktop application
   - Depends on the root project
   - Owns presentation state, screens, components, and desktop wiring

## Dependency direction

```text
Desktop / JVM adapters
        ↓
Application use cases and ports
        ↓
Domain model and domain services

Infrastructure implements application ports.
```

Domain and application code must not depend on Compose Desktop or concrete JSON storage details.

## Main capability boundaries

The current source contains these broad areas:

- Content and learning-item domain
- Content packaging, package validation, import, registration, and media
- Content-library collections and package attachment
- Study sessions, item selection, sibling avoidance, and queue planning
- Review transitions, memory state, FSRS/scheduling, and due-state calculation
- Progress and review-history queries
- JSON-backed and in-memory persistence, mappers, stores, repositories, and transactions
- Dashboard, statistics, analytics, and scheduling diagnostics
- JVM/legacy import entry points
- Compose Desktop shell, dashboard, content library, study, review history, statistics, and settings

## Integration rule

A feature is complete only when all required layers are connected:

```text
Domain behavior (when needed)
→ application command/query/use case
→ infrastructure implementation and persistence
→ desktop/JVM adapter wiring
→ automated tests
→ user-visible flow
```

Do not add disconnected abstractions or placeholders solely to name a future capability.

## Persistence compatibility

Existing persisted data and package formats are product contracts. Changes must preserve compatibility or include explicit migration, validation, rollback considerations, and tests in the same batch.

## Platform strategy

The engine remains reusable and UI-independent. Compose Desktop is the active client and the first release target. Android, iOS, and Web are later consumers; their future needs must not force premature shared abstractions before Desktop Beta works end-to-end.
## Desktop Study accessibility presentation

Desktop Study derives screen-reader status and progress text through the pure `StudyAccessibilityPresentation` model. Compose semantics consume that model, keeping accessibility wording testable without UI instrumentation and aligned with the same `StudyUiState` that drives visible controls and keyboard shortcuts.
