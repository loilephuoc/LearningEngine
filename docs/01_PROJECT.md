# Project Definition

## Product vision

Provide a durable learning platform that can import structured learning content, schedule reviews, run study sessions, persist learner state, and expose progress and analytics through reusable application services and a desktop UI.

## Implemented capability areas

Source inspection confirms the following substantial capability areas:

- Content and learning-item domain models.
- Review execution and review-event recording.
- Due/new item selection.
- Study-session start, next-item, review-item, and finish flows.
- Session sibling/repetition constraints.
- Simple and FSRS scheduling models.
- FSRS parameter profiles, calibration models, and evolution-related domain structures.
- Content libraries and package catalogs.
- Package scanning, validation, install, register, upgrade, uninstall, import, and export workflows.
- Legacy JSON and OPD3 package/media import support.
- SHA-256 package identity and package integrity validation.
- In-memory repositories.
- JSON file stores and store-backed repositories.
- Transaction runners for in-memory and JSON-file persistence.
- Dashboard, learning dashboard, progress, review-history, and statistics queries.
- Content search/ranking domain and store-backed search repository.
- Compose Desktop screens for dashboard, study, content library, review history, statistics, settings, and navigation shell.

## Technology baseline

- Kotlin JVM 2.4.0
- Kotlin serialization 1.11.0
- JDK toolchain 21
- Gradle multi-project build
- Compose Desktop 1.11.1 and Material 3
- Kotlin test with JUnit Platform

## Roadmap discipline

Future work must be recorded as planned until source and tests exist. A feature is verified complete only after a successful build/test result is supplied by the user and the corresponding source is committed.
