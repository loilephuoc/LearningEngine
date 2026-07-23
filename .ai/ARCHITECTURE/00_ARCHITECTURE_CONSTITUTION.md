# Learning Engine 2.0 — Architecture Constitution

## Purpose
The **Architecture Constitution** establishes the non-negotiable supreme laws governing all system design, technical decisions, implementation strategies, and code modifications within the Learning Engine 2.0 codebase.

Every developer, architect, and AI agent operating on this repository must adhere strictly to these constitutional principles without exception.

## Baseline & Categorization Rule
To prevent ambiguity between executing code and future roadmap goals, constitutional laws are categorized into:
- **CURRENT ARCHITECTURE**: Enforced and active in the codebase HEAD (Phase 1–2).
- **TARGET ARCHITECTURE**: Enforced for future roadmap capabilities (Phase 3+).

## Responsibilities
- Define the foundational platform-first philosophy of Learning Engine 2.0.
- Enforce strict layer boundaries and clean dependency directions.
- Protect domain model purity and application transaction integrity.
- Guarantee byte-level determinism, backward compatibility, and data safety.
- Mandate test coverage standards and stopping conditions for development iterations.

## Out of Scope
- Specific UI component styling or framework-specific layout code.
- Ephemeral sprint notes, temporary build scripts, or transient debug tools.

## Dependencies
- Clean Git repository HEAD at `develop` branch.
- Standing working agreements documented in [`AGENTS.md`](../../AGENTS.md).
- Architecture Lock Baseline in [`ARCHITECTURE_LOCK.md`](ARCHITECTURE_LOCK.md).

---

## Part I: CURRENT ARCHITECTURE (Enforced in Active Codebase)

### Law 1: Platform-First System Architecture
Learning Engine is designed fundamentally as a **Core Platform**, not an application wrapper. All core business rules, study algorithms, package management pipelines, scheduling state machines, and statistics calculations belong exclusively inside the pure Kotlin/JVM core platform engine (`vn.loi.learning.*`). Presentation clients (Desktop Compose, Android, iOS, Web) are thin adapters that consume the platform via application use cases and ports.

### Law 2: Single Source of Truth
The clean, Git-tracked repository source code, build configuration, and tests at `HEAD` represent the absolute single source of truth. Architectural decisions must be grounded in empirical evidence from the repository. Speculative abstractions or invented APIs are strictly prohibited.

### Law 3: Pure Domain First
The Domain Layer (`vn.loi.learning.domain.*`) is the absolute nucleus of the system. Domain models and domain services:
- Must have ZERO dependencies on UI frameworks (Compose), filesystem IO, or specific serialization formats (JSON/Jackson/Kotlinx).
- Must enforce invariants early via factory constructors or initialization rules.
- Must represent domain facts using immutable value objects and entity aggregates.

### Law 4: Architecture Before Features
No feature capability may be implemented without a verified architectural boundary, explicit domain/application port contracts, and clear persistence schemas. Code quality, architectural integrity, and data safety take precedence over feature velocity.

### Law 5: Absolute Isolation of Business Logic from UI
Presentation layers (Compose Desktop, Android views, Web components) must contain ZERO domain logic, review scheduling algorithms, or package parsing code. UI components are strictly limited to rendering state objects (`StateFlow`/State models) and dispatching user intent commands to application use cases.

### Law 6: Immutable OPD3 Package Standard
The OPD3 Content Package format (`.opd3`) is an immutable, byte-for-byte deterministic product contract. Packages are self-contained archives featuring SHA-256 integrity manifests, canonical JSON structures, path safety validation, and resource safety limits (`PackageSafetyLimits`). Once created, package contents cannot be modified in-place; changes produce new package versions.

### Law 7: Strict Backward Compatibility & Schema Evolution
Persisted user records, FSRS review histories, package schemas, and public API signatures are immutable product contracts. Schema changes must provide explicit migration paths, backward-compatible parsers, or strict rejection policies. Discarding or silently rewriting user data is illegal.

### Law 8: Clean Dependency Direction
Dependencies must flow strictly inward toward the Domain Layer:
```text
Presentation (Desktop / Mobile / Web)
        ↓
Application (Use Cases, Ports, Orchestration)
        ↓
Domain (Entities, Aggregates, Value Objects, Domain Services)

Infrastructure implements Application Ports.
```

### Law 9: Stable & Explicit Public APIs
Application entry points, use case signatures, and infrastructure ports must remain stable and explicit. Modifying a public API signature requires updating all call sites and maintaining compatibility tests.

### Law 10: Long-Term Maintainability & Test Evidence
Every capability increment must be fully verified by deterministic unit and integration tests. A feature is incomplete without passing automated tests (`.\gradlew.bat clean test`), valid Git state (`git diff --check`), and accurate architectural documentation updates.

---

## Part II: TARGET ARCHITECTURE (Roadmap Vision Phase 3+)

### Law 11: Workspace as the Sole Study Source (Target: Phase 3-4)
In future phases, the **Workspace** will become the sole study source aggregate root for active study sessions. Learning sessions will not query raw packages or global libraries directly; they will bind exclusively to a configured Workspace Aggregate containing explicit study sources, item selections, and review schedules. *(Note: Current Phase 1-2 codebase operates directly on Topic and ContentCollection study queues).*

### Law 12: Event-Driven Domain Inter-Communication (Target: Phase 3+)
Material changes in domain state (e.g., `ReviewCompleted`, `PackageInstalled`, `ItemMastered`) will emit immutable Domain Events via an in-memory `DomainEventPublisher`. Inter-module communication and asynchronous workflows will rely on event dispatching to preserve loose coupling. *(Note: Current Phase 1-2 codebase relies on synchronous callback ports).*

### Law 13: Decentralized Multi-Device Synchronization (Target: Phase 5+)
Multi-device sync and cloud backup will operate via append-only event logs and vector clocks, preserving local-first execution without requiring continuous cloud connectivity.

---

## Future Evolution
Changes to this Constitution require an explicit Architecture Decision Record (ADR) and formal approval following the process in [`13_ARCHITECTURE_GOVERNANCE.md`](13_ARCHITECTURE_GOVERNANCE.md).

---

## Architecture Notes
- Architectural violations identified during code reviews or automated linting must be treated as critical build failures.
