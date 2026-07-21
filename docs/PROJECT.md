# Learning Engine 2.0

## Purpose

Learning Engine is a Kotlin/JVM learning platform with a reusable engine, JSON persistence, content-package import and export, scheduling, analytics, and a Compose Desktop client.

## Current repository baseline

- Repository: `loilephuoc/LearningEngine`
- Integration branch: `develop`
- Batch 17 base commit: `e8ecc51ac31b8a6706a25fe1d8295210a453f4bd`
- Java toolchain: JDK 21
- Root module: engine, domain, application, infrastructure, adapters, CLI, and tests
- Desktop module: Compose Desktop UI depending on the root project

## Build and run

```powershell
.\gradlew clean test
.\gradlew build
.\gradlew run --args="<legacy-json-path>"
.\gradlew legacyPackageImport --args="<arguments>"
.\gradlew :desktop:run
```

## Source-of-truth policy

Git on `develop` is the only development baseline. Historical source and documentation checkpoint ZIP files are not development baselines. Batch archives are delivery packages only.
