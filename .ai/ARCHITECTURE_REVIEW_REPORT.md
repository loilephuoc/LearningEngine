# Learning Engine 2.0 — Architecture Blueprint Formal Review Report

**Reviewer**: Independent Principal Software Architect  
**Target Baseline**: `.ai/` Architecture Blueprint Foundation (Commit `0cc1e23`)  
**Repository Baseline**: `LearningEngine` (Branch `develop`)  
**Date**: July 23, 2026  

---

## 1. Executive Summary

This formal architecture review evaluates the **Architecture Blueprint Foundation** documented in `.ai/` against the actual Kotlin codebase implementation of `LearningEngine 2.0`. 

The blueprint establishes an ambitious, platform-first, Domain-Driven Design (DDD) foundation aimed at supporting cross-platform clients (Desktop, Android, iOS, Web). However, an independent audit comparing documented architectural laws against real codebase evidence reveals notable gaps:
1. **Dependency Layer Violations**: Concrete infrastructure classes are directly imported by application layer orchestrators.
2. **Constitutional vs Code Divergence**: Documented constitutional laws (e.g., `Workspace` as the sole study source, `DomainEventPublisher`) describe future Phase 3–4 targets rather than reflecting the current production HEAD.
3. **Domain Naming Drift**: Minor vocabulary friction exists between codebase terms (`ContentCollection`, `Opd3PackageExporter`) and blueprint terms (`Collection`, `OPD3 Package`).

Overall, the architecture blueprint provides a solid strategic direction, but requires explicit alignment between documented laws and real code boundaries.

---

## 2. Strengths of the Architecture Blueprint

1. **Clear Platform-First Vision**: The separation between a pure Kotlin/JVM core platform engine and presentation clients ensures long-term multi-platform portability (Desktop, Android, iOS, Web).
2. **Deterministic OPD3 Package Standard**: Byte-for-byte reproducibility (`DeterministicZipWriter`), fixed timestamps (`2020-01-01T00:00:00Z`), sorted payload entries, and SHA-256 manifest integrity provide cryptographic verification.
3. **Comprehensive Resource Safety**: Explicit streaming inspection (`Opd3PackageInspector`), canonical path security (`Opd3PathValidator`), and safety limits (`PackageSafetyLimits`) protect against Zip Slip, path traversal, DoS, and Zip Bomb attacks.
4. **Structured Documentation Organization**: The `.ai/` hierarchy (`ARCHITECTURE/`, `DECISIONS/`, `BLUEPRINTS/`) is well-organized, detailed, and free of placeholders.

---

## 3. Weaknesses & Contradictions Identified

### W-01: Premature Constitutional Mandate (`Workspace` Aggregate)
- **Documented Law**: Law 7 of `00_ARCHITECTURE_CONSTITUTION.md` states: *"Workspace is the sole study source. Learning sessions do not query raw packages or global libraries directly; they bind exclusively to a configured Workspace Aggregate."*
- **Codebase Reality**: In the current codebase (`vn.loi.learning.domain.study` & `application.session`), `Workspace` as an aggregate root **does not exist**. Active study sessions operate directly over `Topic` / `ContentCollection` study queues (`StudyQueueFactory`, `PersistedLearningPlatform`).
- **Impact**: Claiming `Workspace` as an existing constitutional law creates a contradiction between documented architecture and executing production code.

### W-02: Event-Driven Architecture vs Synchronous Callbacks
- **Documented Law**: Law 8 states: *"Material changes in domain state must produce immutable Domain Events."* `07_APPLICATION_LAYER_GUIDE.md` specifies `DomainEventPublisher`.
- **Codebase Reality**: Session processing and review history recording rely on synchronous callback interfaces (`ReviewHistoryStore`, `LearningProgressService`) wired inside composition roots.
- **Impact**: Async subscribers and multi-platform sync adapters cannot currently subscribe to a central event bus.

---

## 4. Architecture Smells & Layering Violations

### AS-01: Application-to-Infrastructure Direct Coupling (Concrete Imports)
- **Violation**: The Application Layer must depend on Domain interfaces or Application Ports, never concrete Infrastructure classes.
- **Code Evidence**:
  - `vn.loi.learning.application.contentpackaging.Opd3PackageExporter.kt` directly imports `vn.loi.learning.infrastructure.contentpackaging.DeterministicZipWriter`.
  - `vn.loi.learning.application.contentpackaging.LegacyPairCanonicalConverter.kt` directly imports `vn.loi.learning.infrastructure.contentpackaging.JvmLegacyPkgMediaScanner`.
  - `vn.loi.learning.application.importing.LegacyJsonImportService.kt` directly imports infrastructure persistence implementations.
- **Remediation**: Extract `DeterministicZipWriterPort` and `LegacyPkgMediaScannerPort` into `vn.loi.learning.application.port`, moving concrete implementations entirely behind ports.

---

## 5. Ubiquitous Language Audit

| Code Identifier | Blueprint Term | Status | Recommendation |
|---|---|---|---|
| `ContentCollection` | `Collection` | Synonyms | Standardize on `Collection` in domain docs, mapping to `ContentCollection` entity in code. |
| `Opd3PackageExporter` | `OPD3 Exporter` | Alignment Good | Maintain `Opd3` class prefix in Kotlin, `OPD3` in documentation. |
| `LegacyTopicSourceMetadata` | `SourceMetadata` | Alignment Good | Preserve `LegacyTopicSourceMetadata` for legacy OPD3 import context. |
| `StudyQueueFactory` | `SessionQueuePlanner` | Divergence | Rename application planner service to `SessionQueuePlanner` during Phase 3 session refactoring. |

---

## 6. Future Scalability & Multi-Device Assessment

1. **Cloud Sync Readiness**:
   - *Status*: **MODERATE RISK**.
   - *Gap*: Current storage relies on single-file JSON persistence (`PersistedLearningPlatform`). Syncing across devices requires append-only event logs or vector clocks to prevent overwrite collisions.
2. **AI Tutor Integration**:
   - *Status*: **READY**.
   - *Design*: The pure application use case layer (`ProcessReviewUseCase`) provides clean extension ports for AI hint generators without modifying domain core logic.
3. **Marketplace Distribution**:
   - *Status*: **EXCELLENT**.
   - *Design*: OPD3 package format's SHA-256 manifest and byte-level determinism provide cryptographic validation for marketplace package distribution.

---

## 7. Technical Debt Inventory

1. **TD-01 (High)**: Direct infrastructure class dependencies in application use cases (`Opd3PackageExporter` -> `DeterministicZipWriter`).
2. **TD-02 (Medium)**: Absence of a unified `DomainEventPublisher` bus for review completion notifications.
3. **TD-03 (Low)**: Minor naming drift between `ContentCollection` (code) and `Collection` (blueprint).

---

## 8. Ranked Risk Matrix

| Risk ID | Severity | Category | Description | Impact |
|---|---|---|---|---|
| **R-01** | **HIGH** | Dependency Smell | Application classes directly import Infrastructure adapters. | Violates Clean Architecture dependency direction; hinders swapping Zip/Media infrastructure implementations. |
| **R-02** | **MEDIUM** | Documentation Gap | Constitutional Law 7 mandates `Workspace` as sole study source, but code HEAD uses `Topic` queues. | Causes confusion for new developers / AI agents reading architecture docs vs code. |
| **R-03** | **MEDIUM** | Architecture Smell | Review history relies on synchronous callback wiring instead of Domain Events. | Tight coupling between session execution and analytics logging; limits async multi-device sync. |
| **R-04** | **LOW** | Ubiquitous Language | `ContentCollection` in code vs `Collection` in blueprint. | Minor vocabulary fragmentation. |

---

## 9. Recommended Improvements & Action Plan

1. **Refactor Infrastructure Dependency Imports (Phase 2.1)**:
   - Define `DeterministicZipWriterPort` and `LegacyPkgMediaScannerPort` in `vn.loi.learning.application.port`.
   - Update `Opd3PackageExporter` and `LegacyPairCanonicalConverter` to accept port interfaces via constructor injection.
2. **Clarify Roadmap Phasing in Constitution (Phase 2.1)**:
   - Annotate Constitutional Law 7 (`Workspace`) and Law 8 (`Domain Events`) with explicit target roadmap phase labels (e.g., *"Targeting Phase 3 Workspace Capability"*), clarifying that current Phase 1–2 HEAD uses `Topic` / `ContentCollection` study sources.
3. **Introduce Event Bus (`DomainEventPublisher`) (Phase 3)**:
   - Implement an in-memory `DomainEventPublisher` port in `application.common` to decouple `LearningSession` from `Analytics` and `ReviewHistory`.

---

## 10. Formal Architecture Approval Decision

### **CONDITIONALLY APPROVED WITH REMEDIATIONS**

**Justification**:  
The Architecture Blueprint Foundation (`.ai/`) provides an outstanding strategic roadmap, clean DDD context boundaries, and robust safety mechanisms. It is **APPROVED** as the guiding architectural framework for Learning Engine 2.0, subject to addressing the **High-severity dependency import refactoring (R-01)** and **Phasing clarification in documentation (R-02)** during the upcoming capability iterations.
