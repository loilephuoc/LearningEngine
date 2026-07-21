# Learning Engine 2.0

Learning Engine is a Kotlin/JVM learning platform with a reusable engine, JSON persistence, content-package import/export, FSRS-oriented scheduling, analytics, and a Compose Desktop client.

## Modules

- Root project: domain, application, JVM adapters, infrastructure, CLI entry points, and tests.
- `desktop`: Compose Desktop UI depending on the root project.

The current architecture, module inventory, roadmap, and development rules live in [`docs/`](docs/):

- [`docs/PROJECT.md`](docs/PROJECT.md)
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- [`docs/MODULES.md`](docs/MODULES.md)
- [`docs/ROADMAP.md`](docs/ROADMAP.md)
- [`docs/WORKFLOW.md`](docs/WORKFLOW.md)
- [`docs/CHANGELOG.md`](docs/CHANGELOG.md)

## Requirements

- JDK 21
- Gradle Wrapper

## Build and test

From the repository root:

```powershell
.\gradlew clean test
```

Full project build:

```powershell
.\gradlew build
```

## Run

Root CLI:

```powershell
.\gradlew run --args="<legacy-json-path>"
```

Legacy package import task:

```powershell
.\gradlew legacyPackageImport --args="<arguments>"
```

Desktop application:

```powershell
.\gradlew :desktop:run
```

## Repository workflow

- `main`: stable milestone branch.
- `develop`: active integration branch and the baseline for new batches.
- Git is the only source of truth.
- Apply ZIP files are transport artifacts, not repository content.
- Store and extract apply packages outside the repository, under:

```text
C:\Users\M72Q\LearningEngine_Batches\
```

Run an extracted batch script from the repository root:

```powershell
& "C:\Users\M72Q\LearningEngine_Batches\BatchXX_APPLY\apply_batch.ps1"
```
