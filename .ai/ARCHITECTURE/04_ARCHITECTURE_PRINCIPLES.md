# Learning Engine 2.0 — Architecture Principles

## Purpose
This document details the core **Architectural Philosophy** and foundational design decisions behind Learning Engine 2.0, providing explicit rationale for why specific architectural patterns, constraints, and trade-offs were selected.

## Responsibilities
- Explain the engineering rationales behind key architectural decisions.
- Document trade-off matrices between speed, maintainability, determinism, and complexity.
- Establish guidance on anti-patterns to prevent architectural degradation.

## Out of Scope
- Code syntax conventions (see [`10_IMPLEMENTATION_RULES.md`](10_IMPLEMENTATION_RULES.md)).
- Specific UI layout frameworks.

## Dependencies
- [`00_ARCHITECTURE_CONSTITUTION.md`](00_ARCHITECTURE_CONSTITUTION.md)

---

## Architectural Philosophy & Rationales

### Principle 1: Platform-First over Monolithic Application
- **Rationale**: Building a GUI application directly bound to UI frameworks (e.g., embedding business rules inside Compose ViewModels or Android Activities) leads to code duplication, test fragility, and high platform porting costs.
- **Decision**: All business logic, algorithms, package management, and scheduling rules are built inside a pure, UI-independent Kotlin platform core. Presentation layers are thin clients.
- **Trade-off**: Requires writing explicit port interfaces and application use cases upfront, slightly increasing initial file count, but yielding long-term multi-platform portability and unit testability.

### Principle 2: Byte-for-Byte Determinism
- **Rationale**: In learning software, package exports and scheduling calculations must produce identical results across different OS environments (Windows, macOS, Linux, Android). Non-deterministic file ordering, variable timestamps, or random identifiers corrupt package checksum verification and break sync.
- **Decision**: Package export (`Opd3PackageExporter`) enforces fixed entry timestamps (`2020-01-01T00:00:00Z`), deterministic map key sorting (`ContentId`, `LearningItemId`), pretty-printed UTF-8 JSON formatting, and SHA-256 manifest hashing.
- **Trade-off**: Requires strict sorting and fixed metadata rules during serialization, but guarantees byte-level reproducibility.

### Principle 3: Immutability & Early Invariant Enforcement
- **Rationale**: Mutable domain state and deferred validation lead to subtle runtime crashes, corrupted data states, and multi-threading race conditions.
- **Decision**: Domain entities and value objects are immutable (`data class` with `val` properties). Validation rules (`require()`) are evaluated inside `init` blocks during object instantiation, preventing invalid objects from ever existing.
- **Trade-off**: Requires constructing new instances upon state mutation, but eliminates invalid state bugs.

### Principle 4: Explicit Constructor Injection over Global State
- **Rationale**: Hidden global state (`object` singletons, service locators, static context singletons) makes code difficult to test in parallel, obscures dependencies, and creates hidden coupling.
- **Decision**: All application use cases, repositories, and services use explicit constructor injection. Dependency graphs are wired explicitly at composition roots (e.g., Desktop `Main` wiring).
- **Trade-off**: Requires passing dependencies explicitly through constructors, but guarantees clean testability and modular isolation.

### Principle 5: Streaming Validation over Unbounded Memory Allocation
- **Rationale**: Loading entire ZIP files or large media packages into memory before validating size limits creates Severe Denial of Service (DoS) and Zip Bomb vulnerability risks.
- **Decision**: Inspectors and parsers (`Opd3PackageInspector`) use incremental streaming reading via 8KB bounded buffers and running byte counters (`PackageSafetyLimits`), aborting immediately upon exceeding configured size limits.
- **Trade-off**: Requires streaming stream-copy loops rather than simple `readBytes()`, but protects system resources.

---

## Architectural Trade-Off Matrix

| Design Choice | Benefit | Accepted Cost |
|---|---|---|
| **Pure Domain Layer** | 100% UI & DB framework independent, ultra-fast unit testing | Cannot use UI helpers or DB annotations directly inside domain classes |
| **OPD3 Immutable Package** | Cryptographically verifiable integrity, zero partial state corruption | Cannot edit package files in-place; must re-export new version |
| **Workspace Isolation** | Learner can tailor review sources without distorting original library content | Requires explicit Workspace-to-Topic binding mapping layer |
| **Event-Driven Analytics** | Review logging does not block UI or session execution | Event dispatching requires background listener handlers |

---

## Anti-Patterns Strictly Forbidden

1. **Anemic Domain Model**: Creating pure DTO domain classes with zero behavior, putting all domain logic inside UI view models or database scripts.
2. **Framework Leakage**: Importing Compose, Android SDK, or Jackson annotations into domain or application layer packages.
3. **Silent Exception Swallowing**: Catching errors and returning dummy null/empty values without diagnostic logging or proper error reporting.
4. **Ad-Hoc UI Persistence**: Writing persistent JSON files directly from Compose screen handlers without passing through application use cases.

---

## Future Evolution
These principles will govern all upcoming platform developments (Android client, iOS client, Web WASM client, Cloud Sync) to maintain structural elegance and system robustness.

---

## Architecture Notes
- Architectural decisions documented here take precedence over temporary convenience shortcuts.
