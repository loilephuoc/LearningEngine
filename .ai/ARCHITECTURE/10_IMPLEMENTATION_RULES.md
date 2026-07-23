# Learning Engine 2.0 — Implementation Rules

## Purpose
This document specifies the mandatory **Implementation Rules**, coding conventions, folder organization, DDD patterns, and testing expectations for all code written within Learning Engine 2.0.

## Responsibilities
- Establish strict coding standards and package naming conventions.
- Define git commit workflows and pull request quality gates.
- Specify verification standards and test coverage rules.

## Out of Scope
- Ephemeral IDE keymap settings or local developer environment tooling.

## Dependencies
- [`AGENTS.md`](../../AGENTS.md)
- [`00_ARCHITECTURE_CONSTITUTION.md`](00_ARCHITECTURE_CONSTITUTION.md)

---

## Code & Naming Conventions

### 1. Package Naming
- Package names must be lowercase without underscores: `vn.loi.learning.domain.content.model`.
- Subsystem feature folders must follow bounded context names.

### 2. Class & File Naming
- Aggregates and Entities: Noun phrases (e.g., `CanonicalTopicPackage`, `Workspace`, `LearningItem`).
- Value Objects: Descriptive domain terms (e.g., `TopicId`, `ContentText`, `FSRSState`).
- Use Cases: Verb-noun action phrases (e.g., `ExportPackageUseCase`, `ProcessReviewUseCase`).
- Repositories & Ports: Interface named after aggregate (e.g., `WorkspaceRepository`), implementation prefixed by technology (e.g., `JsonWorkspaceRepository`).

### 3. Error Handling Conventions
- Domain/Application methods return explicit result objects (`sealed interface Result`) or diagnostic lists (`List<Diagnostic>`).
- Never throw untyped runtime exceptions across layer boundaries.
- Exceptions during external file IO must be caught and converted into structured application diagnostics with explicit severity (`FATAL`, `WARNING`).

---

## Folder & DDD Conventions

```text
src/
├── main/kotlin/vn/loi/learning/
│   ├── domain/               # Core Entities, Aggregates, Value Objects, Domain Events
│   ├── application/          # Use Cases, Ports, Orchestration, Diagnostics
│   └── infrastructure/       # Persistence Adapters, Exporters, Readers, Serializers
desktop/
└── src/main/kotlin/vn/loi/learning/desktop/
    ├── Main.kt               # Desktop Composition Root
    └── ui/                   # Screen ViewModels, Composables, Design System
```

---

## Testing & Quality Expectations

### 1. Verification Command
For any source, configuration, resource, or test change, execution of:
```powershell
.\gradlew.bat clean test
```
must result in `BUILD SUCCESSFUL`.

### 2. Test Matrix Coverage
- **Unit Tests**: Every domain entity, value object, and application use case must have focused unit test coverage.
- **Integration Tests**: File processors, package exporters, inspectors, and repository adapters must be covered by integration tests.
- **Adversarial & Safety Tests**: Security boundaries (path traversal, ZIP bombs, malformed checksums, missing fields) must be covered by explicit adversarial tests (`Opd3AdversarialTest`).

### 3. Git & Commit Workflow
- One capability increment per commit.
- Atomic commit messages in imperative style: `feat: ...`, `fix: ...`, `docs: ...`, `test: ...`.
- Run `git diff --check` before committing to ensure clean diff formatting.

---

## Future Evolution
These rules will be enforced via CI/CD linting rules and automated test execution.

---

## Architecture Notes
- All code must strictly conform to these implementation rules.
