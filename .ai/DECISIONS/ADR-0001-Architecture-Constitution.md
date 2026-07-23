# ADR-0001: Architecture Constitution

## Context
As Learning Engine 2.0 evolved from a desktop-centric proof-of-concept into a multi-platform teaching engine, technical decision-making risks becoming fragmented without a clear, binding architectural authority. Without explicit constitutional laws, business logic tends to leak into presentation composables, persistence formats fragment across platforms, and non-deterministic package exports corrupt integrity verification.

## Decision
We establish `.ai/ARCHITECTURE/00_ARCHITECTURE_CONSTITUTION.md` as the supreme non-negotiable architectural constitution of Learning Engine 2.0. The constitution mandates:
1. **Platform-First Architecture**: Core engine logic resides in pure Kotlin/JVM core ports and domain models.
2. **Single Source of Truth**: Repository HEAD source, build configurations, and tests are the sole evidence-backed truth.
3. **Pure Domain First**: Zero framework or UI dependencies in the domain layer.
4. **Architecture Before Features**: Architectural boundaries and ports must precede feature implementations.
5. **No Business Logic in UI**: Presentation components render UI states and dispatch commands only.
6. **Immutable OPD3 Standard**: OPD3 packages are byte-for-byte deterministic, self-contained product contracts.
7. **Workspace Study Source**: Active study sessions draw items exclusively from configured Workspaces.
8. **Domain Events**: Significant state changes emit immutable domain events.
9. **Backward Compatibility**: Data persistence and schemas are product contracts requiring explicit migration paths.
10. **Clean Dependency Direction**: Dependencies point strictly inward towards the domain layer.

## Consequences

### Positive
- Guarantees complete platform independence for the core teaching engine.
- Enables 100% reusable domain logic across Desktop, Android, iOS, and Web.
- Guarantees byte-level reproducibility and cryptographic verifiability for OPD3 content packages.
- Protects user data integrity across software updates.

### Negative
- Requires strict discipline and explicit application port interfaces before implementing UI screens.
- Increases initial file count due to strict layering and DTO mapping.

## Alternatives Considered
- **UI-Centric Architecture (Direct ViewModel Persistence)**: Rejected because it tightly couples business rules to Compose/Desktop runtime objects, preventing reuse on Android/iOS/Web.
- **In-Place Package Mutation**: Rejected because modifying package archives in-place compromises SHA-256 checksum verification and risks partial file corruption during system crashes.

## Status
**ACCEPTED** (Enforced across all repository modules).
