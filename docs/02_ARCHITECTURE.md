# Architecture Baseline

## Build structure

```text
LearningEngine
├── root JVM application/library
│   ├── src/main/kotlin
│   └── src/test/kotlin
└── desktop
    └── Compose Desktop application depending on project(":")
```

The root project uses the standard Gradle source sets. The repository also contains tracked `src/src/main` and `src/src/test` trees. They are not standard root source-set paths under the present Gradle configuration and should be treated as legacy/duplicate material until deliberately reconciled. Do not edit both trees automatically.

## Layer map

### Domain — `vn.loi.learning.domain`

Framework-independent learning concepts and algorithms:

- `content`: content, libraries, package metadata, search models and ranking.
- `study.learning`: learning-item models.
- `study.memory`: learner memory state, review events, algorithms, science and evolution structures.
- `study.scheduling`: scheduler contracts, simple scheduler, FSRS scheduler, decisions and transitions.
- `study.selection`: candidate models, pipeline, policies, priorities and `SessionSelector`.
- `study.session`: session models and policies.
- `study.analytics`: analytics models and services.
- `study.fsrs`: FSRS model, calibration profile contracts, and evolution structures.

### Application — `vn.loi.learning.application`

Use cases, orchestration, query services, ports, commands, results, and package workflows:

- `LearningEngine` is the primary study/review/session facade.
- `port` defines repository and transaction boundaries.
- `review`, `study`, and `session` implement core learning workflows.
- `contentpackaging` contains package scan/import/export/install/register/upgrade/uninstall orchestration and validators.
- `contentlibrary`, `contentmedia`, `dashboard`, `learningdashboard`, `progress`, `reviewhistory`, and `analytics` provide query and projection services.
- `importing` supports legacy JSON import orchestration.

### Infrastructure — `vn.loi.learning.infrastructure`

Concrete composition and adapters:

- Top-level factories compose in-memory and persisted applications.
- `persistence.memory`: in-memory repositories.
- `persistence.store`, `record`, `mapper`, `json`, and `repository`: JSON-backed persistence pipeline.
- `transaction`: in-memory and file transaction implementations.
- `contentpackaging`: JVM ZIP/directory/OPD3 readers, writers, scanners and importers.
- `contentmedia`: media extraction, path mapping, storage and verification.
- `importer.legacy`: legacy importer implementation.

### Adapter — `vn.loi.learning.adapter.jvm`

JVM-facing entry points and presentation adapters, including console output, package import commands, JSON file import, and the legacy package import main.

### Desktop — `vn.loi.learning.desktop`

Compose Desktop presentation layer:

- App shell, navigation, theme and shared components.
- Feature facades, view models, UI state and screens for dashboard, study, content library, review history and statistics.
- Settings screen and lesson browser components.

## Dependency direction

Intended direction inferred from source composition:

```text
Desktop / JVM adapters
        ↓
Application services and ports
        ↓
Domain models and algorithms

Infrastructure implements application ports and composes the graph.
```

Domain code must not depend on application, infrastructure, desktop, Compose, filesystem, or JSON details. Application code should depend on domain and ports, not concrete stores. Infrastructure may depend inward to implement ports.

## Persistence

Two supported persistence modes are visible:

- In-memory repositories for demos/tests and transient operation.
- JSON-file stores wrapped by store-backed repositories for persisted operation.

`JsonFileTransactionRunner` coordinates multiple persistence files for package workflows. Persisted factories are the composition root for JSON-backed repositories.

## Core composition roots

- `LearningApplicationFactory`
- `LearningEngineFactory`
- `LearningDashboardQueryServiceFactory`
- `PersistedLearningEngineFactory`
- `PersistedLearningPlatformFactory`
- `PersistedLegacyPackageImportFactory`
- `ContentPackageImportFactory`

Always inspect the current factory implementation before changing constructor graphs.

## Entry points

- `vn.loi.learning.MainKt`: root CLI/demo and package-import routing.
- `vn.loi.learning.adapter.jvm.LegacyPackageImportMainKt`: registered Gradle `legacyPackageImport` task.
- `vn.loi.learning.desktop.DesktopMainKt`: Compose Desktop application.

## Verified scale at baseline audit

Active standard source trees contain:

- Root production Kotlin files: 378
- Root test Kotlin files: 217
- Desktop production Kotlin files: 41

Tracked nonstandard duplicate trees contain an additional 273 production and 131 test Kotlin files. Their presence is an architecture/maintenance risk, not evidence that Gradle compiles them.

## Architecture risks and follow-up

1. Reconcile or remove the tracked `src/src` duplicate tree after a deliberate file-by-file comparison.
2. Add desktop tests as UI behavior stabilizes; no Kotlin tests were found in `desktop/src/test` at audit time.
3. Keep package workflow services narrow; `application/contentpackaging` is the largest application area and requires disciplined incremental changes.
4. Preserve transaction boundaries across package registration, content import and persistence updates.
