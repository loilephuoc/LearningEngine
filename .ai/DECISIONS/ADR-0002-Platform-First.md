# ADR-0002: Platform-First Architecture

## Context
Learning Engine 2.0 must support multiple client platforms (Desktop Compose today; Android, iOS, and Web WASM in subsequent phases). If the initial desktop implementation tightly couples learning session state, card scheduling algorithms (FSRS), package reading, or review history logging to Compose Desktop runtime APIs, porting to mobile and web will require rewriting core engine capabilities.

## Decision
We adopt a **Platform-First Architecture**:
1. All domain models (`Content`, `Topic`, `LearningItem`, `Workspace`, `FSRSState`), domain services (`FsrsScheduler`), package pipelines (`Opd3PackageExporter`, `Opd3PackageInspector`, `Opd3PackageVerifier`), and application use cases reside inside a pure Kotlin/JVM engine module (`vn.loi.learning.*`).
2. The core platform engine depends exclusively on the standard Kotlin library and platform-neutral utility packages.
3. Presentation clients (Compose Desktop in `desktop`, Android Activity/Views, Web WASM) interact with the platform engine strictly through Application Use Cases and Ports (`vn.loi.learning.application.*`).
4. Presentation clients own zero business or review scheduling logic.

## Consequences

### Positive
- 100% of teaching algorithms, package tools, and review logic can be reused without modification on Android, iOS, and Web.
- Core engine can be tested in isolation using fast unit tests without launching a GUI window.
- System capabilities can be exposed via CLI tools or background workers seamlessly.

### Negative
- Requires maintaining explicit application DTOs and mapper functions between UI view states and internal domain models.
- Slightly higher initial setup overhead for simple presentation features.

## Alternatives Considered
- **Desktop-First Monolith**: Implementing review scheduling and package management directly within Compose Desktop ViewModels. Rejected because it would require duplicating business logic across Android, iOS, and Web.
- **Microservices / Backend Server**: Hosting engine logic on a remote server. Rejected because Learning Engine 2.0 must operate 100% offline-first on local user devices without network dependency.

## Status
**ACCEPTED** (Enforced across all repository modules).
