# AI Baseline

## Identity

- Project: Learning Engine 2.0
- Language: Kotlin/JVM 2.4.0
- Runtime toolchain: JDK 21
- Build: Gradle Wrapper
- UI: Compose Desktop
- Stable branch: `main`
- Development branch: `develop`

## Architecture summary

- Domain: learning/content models and algorithms.
- Application: use cases, query services and ports.
- Infrastructure: repositories, JSON stores, transactions, package/media adapters and composition factories.
- JVM adapter: CLI/file adapters.
- Desktop: Compose UI depending on the root project.

## Core rules

- Git source/tests are authoritative.
- Inspect declarations, call sites, tests and factories before editing.
- Never invent APIs or completed work.
- One narrow increment at a time.
- Return complete files with IntelliJ-opening PowerShell commands.
- Wait for explicit `BUILD SUCCESSFUL`.
- Update progress/handoff after verification.
- Never run destructive cleanup without a reviewed dry-run.

## Repository caveat

The tracked `src/src` tree is nonstandard under current Gradle configuration and appears to duplicate older source/test material. Treat `src/main` and `src/test` as the active root trees unless Gradle configuration changes.
