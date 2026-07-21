# Modules and capability map

## Root project

### Domain

Owns learning concepts and rules, including content, learning items, memory state, reviews, sessions, scheduling, selection, and content-library collection models.

### Application

Coordinates use cases and exposes ports. Current application areas include review, study sessions, study-queue planning, content packaging, content-library operations, progress, dashboard queries, analytics, and import workflows.

### Infrastructure

Provides concrete factories, JSON stores, in-memory stores and repositories, record mappers, transaction support, JVM package readers, and persistence-backed wiring.

### JVM adapters

Provides command-line entry points, legacy import adapters, and console-oriented integration.

### Tests

The root test suite contains domain tests, use-case tests, repository/store contract tests, persistence restart tests, scheduling tests, queue-policy tests, and integration tests.

## Desktop module

Provides the Compose Desktop client. Existing UI areas include dashboard presentation and content-library dialogs, along with application startup and supporting desktop UI code.

## Build dependencies

- Kotlin/JVM 2.4.0
- Kotlin serialization plugin 2.4.0
- kotlinx-serialization-json 1.11.0
- JDK 21
- Compose plugin 2.4.0
- Compose Desktop 1.11.1
- Compose Material 3
