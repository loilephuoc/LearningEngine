# Learning Engine 2.0

Learning Engine is a Kotlin/JVM learning platform with a reusable engine, JSON persistence, content-package import/export, FSRS-oriented scheduling, analytics, and a Compose Desktop client.

## Modules

- Root project: domain, application, JVM adapters, infrastructure, CLI entry points, and tests.
- `desktop`: Compose Desktop UI depending on the root project.

## Requirements

- JDK 21
- Gradle Wrapper

## Build

```powershell
.\gradlew build
```

Latest verified result on 2026-07-20:

```text
BUILD SUCCESSFUL in 14s
13 actionable tasks: 3 executed, 10 up-to-date
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
- `develop`: active integration branch.
- Git is the source of truth; source/docs checkpoint ZIP files are obsolete.

Read `.ai/HANDOFF.md` and `.ai/CURRENT_SESSION.md` before beginning a new AI-assisted development session.
