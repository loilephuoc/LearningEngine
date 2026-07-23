# LP-001: Library Domain — Architecture Constraints

## Mandatory Architecture Constraints

1. **Architecture Lock 1.0 Compliance**: LP-001 MUST comply strictly with Architecture Lock Version 1.0 (`ARCHITECTURE_LOCK.md`) and `00_ARCHITECTURE_CONSTITUTION.md`.
2. **Pure Domain Isolation**: Code implemented under LP-001 must reside in `vn.loi.learning.domain.library.*` and have ZERO imports from:
   - `vn.loi.learning.infrastructure.*`
   - `vn.loi.learning.desktop.*`
   - `kotlinx.serialization.*`
   - `java.io.File` / filesystem APIs
3. **Explicit Constructor Injection**: All aggregates, use cases, and repositories must use explicit constructor parameters. Global singletons or static locators are forbidden.
4. **Immutability Standard**: All domain models, value objects, and events must be immutable (`data class` with `val` properties). State updates must return new instances.
5. **No Technical Debt Introduction**: Implementation must not introduce concrete infrastructure dependencies inside application orchestrators (`TD-01`).
