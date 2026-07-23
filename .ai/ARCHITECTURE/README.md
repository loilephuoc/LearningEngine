# Learning Engine 2.0 — Architecture Documentation Index

## Purpose
This directory contains the foundational **Architecture Blueprint & Layer Guides** for Learning Engine 2.0. It defines the constitutional laws, ubiquitous language, strategic context map, dependency rules, layer-by-layer design guidelines, status matrix, technical debt register, governance frameworks, and the official **Architecture Lock Version 1.0**.

---

## Architectural Index

1. **[`ARCHITECTURE_LOCK.md`](ARCHITECTURE_LOCK.md)**
   - Official Version 1.0 LOCKED Architecture Specification & Change Governance Policy.

2. **[`00_ARCHITECTURE_CONSTITUTION.md`](00_ARCHITECTURE_CONSTITUTION.md)**
   - Supreme constitutional rules, laws, and principles of Learning Engine 2.0 (categorized into CURRENT and TARGET architecture).

3. **[`01_SYSTEM_VISION.md`](01_SYSTEM_VISION.md)**
   - High-level system vision, core shared Kotlin/JVM engine strategy, cross-platform architecture, and subsystem relationships.

4. **[`02_UBIQUITOUS_LANGUAGE.md`](02_UBIQUITOUS_LANGUAGE.md)**
   - Official domain vocabulary dictionary defining Package, Installed Package, Library, Collection, Workspace, Learning Item, Topic, Content, Study Source, Learning Session, Review, Scheduler, Review History, Statistics, Version, Repository, Aggregate, Value Object, Entity, and Vocabulary Alignment table.

5. **[`03_CONTEXT_MAP.md`](03_CONTEXT_MAP.md)**
   - Strategic Domain-Driven Design (DDD) Context Map defining bounded contexts, upstream/downstream relationships, and integration patterns.

6. **[`04_ARCHITECTURE_PRINCIPLES.md`](04_ARCHITECTURE_PRINCIPLES.md)**
   - Engineering rationales behind platform-first design, byte-for-byte determinism, immutability, explicit constructor injection, streaming resource validation, and forbidden anti-patterns.

7. **[`05_DEPENDENCY_RULES.md`](05_DEPENDENCY_RULES.md)**
   - Strict clean dependency rules, package import boundaries, and concrete code examples of valid vs forbidden dependencies.

8. **[`06_DOMAIN_LAYER_GUIDE.md`](06_DOMAIN_LAYER_GUIDE.md)**
   - Developer implementation guide for Domain Entities, Aggregates, Value Objects, Domain Events, and Repository interface ports (`vn.loi.learning.domain.*`).

9. **[`07_APPLICATION_LAYER_GUIDE.md`](07_APPLICATION_LAYER_GUIDE.md)**
   - Guide for application use case orchestration, CQRS command/query separation, application ports, and diagnostic error handling (`vn.loi.learning.application.*`).

10. **[`08_INFRASTRUCTURE_LAYER_GUIDE.md`](08_INFRASTRUCTURE_LAYER_GUIDE.md)**
    - Guide for persistence adapters, OPD3 deterministic ZIP export, streaming inspection, security validation, and schema evolution (`vn.loi.learning.infrastructure.*`).

11. **[`09_PRESENTATION_LAYER_GUIDE.md`](09_PRESENTATION_LAYER_GUIDE.md)**
    - Presentation client guide for Compose Desktop and future mobile/web clients, enforcing Unidirectional Data Flow (UDF) and Zero Business Logic in UI (`vn.loi.learning.desktop.*`).

12. **[`10_IMPLEMENTATION_RULES.md`](10_IMPLEMENTATION_RULES.md)**
    - Coding conventions, package structures, error handling rules, test matrices, assertion standards, and git commit workflows.

13. **[`11_ARCHITECTURE_STATUS.md`](11_ARCHITECTURE_STATUS.md)**
    - Implementation status matrix across all subsystems (`Implemented`, `Partially Implemented`, `Blueprint`, `Planned`, `Deferred`).

14. **[`12_TECHNICAL_DEBT_REGISTER.md`](12_TECHNICAL_DEBT_REGISTER.md)**
    - Technical Debt Log tracking layer violations (`TD-01`), vocabulary alignment (`TD-02`), event bus migration (`TD-03`), and workspace migration (`TD-04`).

15. **[`13_ARCHITECTURE_GOVERNANCE.md`](13_ARCHITECTURE_GOVERNANCE.md)**
    - Governance framework, ADR approval workflow, constitution amendment policy, and review triggers.

16. **[`14_DEFINITION_OF_DONE.md`](14_DEFINITION_OF_DONE.md)**
    - Explicit Definition of Done (DoD) criteria for Blueprints, Capabilities, Implementation, Reviews, and Production Releases.

17. **[`15_ARCHITECTURE_REVIEW_CHECKLIST.md`](15_ARCHITECTURE_REVIEW_CHECKLIST.md)**
    - Architectural review checklist covering DDD, Dependencies, Ownership, Events, Repositories, Naming, Scalability, Testing, Backward Compatibility, Performance, and Security.

---

## Architectural Decisions, Blueprints & Reviews

- **Architecture Decision Records (ADRs)**: Located in [`../DECISIONS/`](../DECISIONS/)
- **Subsystem Platform Blueprints**: Located in [`../BLUEPRINTS/`](../BLUEPRINTS/)
- **Independent Architecture Review Report**: Located in [`../ARCHITECTURE_REVIEW_REPORT.md`](../ARCHITECTURE_REVIEW_REPORT.md)
