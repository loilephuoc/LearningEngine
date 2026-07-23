# Learning Engine 2.0 — Technical Debt Register

## Purpose
This document logs all identified architectural technical debt, layer boundary violations, and pending refactoring items within Learning Engine 2.0. Each entry specifies location, impact, priority, target resolution milestone, and status.

---

## Technical Debt Log

### TD-01: Application → Infrastructure Direct Dependency Imports
- **ID**: `TD-01`
- **Description**: Application layer classes directly import concrete infrastructure adapters instead of depending on application ports.
  - `Opd3PackageExporter.kt` directly imports `infrastructure.contentpackaging.DeterministicZipWriter`.
  - `LegacyPairCanonicalConverter.kt` directly imports `infrastructure.contentpackaging.JvmLegacyPkgMediaScanner`.
- **Location**: `vn.loi.learning.application.contentpackaging`
- **Impact**: Violates Clean Architecture dependency law. Hardcodes concrete infrastructure adapters inside application orchestrators.
- **Priority**: **HIGH**
- **Target Milestone**: Phase 2.1 Refactoring
- **Owner**: Core Architecture Team
- **Status**: **ACCEPTED**

---

### TD-02: Terminology Drift (`ContentCollection` vs `Collection`)
- **ID**: `TD-02`
- **Description**: Code uses `ContentCollection` entity in `domain.content.library`, while architecture blueprints use `Collection`.
- **Location**: `vn.loi.learning.domain.content.library.ContentCollection` & `.ai/BLUEPRINTS/LibraryPlatform.md`
- **Impact**: Minor Ubiquitous Language friction between codebase and documentation.
- **Priority**: **LOW**
- **Target Milestone**: Phase 3 (Library Platform Expansion)
- **Owner**: Architecture Governance Team
- **Status**: **ACCEPTED**

---

### TD-03: Synchronous Callback Coupling (Future Event Bus Migration)
- **ID**: `TD-03`
- **Description**: Learning session execution communicates review completion to analytics via synchronous callback interfaces (`ReviewHistoryStore`) instead of an in-memory `DomainEventPublisher`.
- **Location**: `vn.loi.learning.application.session` & `application.reviewhistory`
- **Impact**: Tight coupling between session state machine and analytics persistence. Prevents asynchronous background processing and multi-device sync event distribution.
- **Priority**: **MEDIUM**
- **Target Milestone**: Phase 4 (Learning Session Refactoring)
- **Owner**: Core Engine Team
- **Status**: **ACCEPTED**

---

### TD-04: Study Source Migration to `Workspace` Aggregate
- **ID**: `TD-04`
- **Description**: Active review sessions operate directly over `Topic` and `ContentCollection` queues (`StudyQueueFactory`) rather than binding to a `Workspace` aggregate root.
- **Location**: `vn.loi.learning.domain.study.session` & `application.study`
- **Impact**: Limits workspace isolation capabilities and custom per-workspace card limit settings.
- **Priority**: **MEDIUM**
- **Target Milestone**: Phase 4 (Workspace Capability)
- **Owner**: Core Engine Team
- **Status**: **ACCEPTED**

---

## Governance & Resolution
Technical debt items must be reviewed before starting any capability phase. Resolving a technical debt item requires an explicit commit referencing the Debt ID (e.g., `fix(arch): resolve TD-01 port refactoring`).
