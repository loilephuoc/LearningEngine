# Architecture

## Module boundary

The Gradle build contains two modules:

1. Root project
   - Domain model and domain services
   - Application use cases and ports
   - Infrastructure and persistence adapters
   - JVM adapters and command-line entry points
   - Automated tests

2. `desktop`
   - Compose Desktop application
   - Depends on the root project
   - Contains desktop presentation and UI components

## Architectural direction

The codebase follows a layered dependency direction:

```text
Desktop / JVM adapters
        |
        v
Application use cases and ports
        |
        v
Domain model and domain services

Infrastructure implements application ports.
```

Domain and application code should not depend on Compose Desktop or concrete JSON storage details.

## Current capability areas visible in the Batch 16 baseline

- Learning and review workflows
- Study sessions and study queues
- Queue planning, strategy selection, balancing, diversity, diagnostics, and metrics
- Scheduling and scheduler validation/diagnostics
- JSON-backed persistence and in-memory repositories
- Content packages and package installation/query workflows
- Content-library collections and package attachment
- Dashboard and learning analytics
- Legacy JSON and package import
- Compose Desktop presentation

## Change rule

New increments must use existing packages, constructors, interfaces, and wiring found in the current source. A batch must not invent an integration contract without first adding and testing that contract as part of the same complete capability.
