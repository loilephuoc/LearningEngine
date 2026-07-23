# Learning Engine 2.0 — Architecture Documentation Index

## Purpose
This directory contains the foundational **Architecture Blueprint & Layer Guides** for Learning Engine 2.0. It defines the constitutional laws, ubiquitous language, strategic context map, dependency rules, and layer-by-layer design guidelines for the codebase.

---

## Architectural Index

1. **[`00_ARCHITECTURE_CONSTITUTION.md`](00_ARCHITECTURE_CONSTITUTION.md)**
   - Supreme constitutional rules, laws, and non-negotiable architectural principles of Learning Engine 2.0.

2. **[`01_SYSTEM_VISION.md`](01_SYSTEM_VISION.md)**
   - High-level system vision, core shared Kotlin/JVM engine strategy, cross-platform architecture, and subsystem relationships.

3. **[`02_UBIQUITOUS_LANGUAGE.md`](02_UBIQUITOUS_LANGUAGE.md)**
   - Official domain vocabulary dictionary defining Package, Installed Package, Library, Collection, Workspace, Learning Item, Topic, Content, Study Source, Learning Session, Review, Scheduler, Review History, Statistics, Version, Repository, Aggregate, Value Object, and Entity.

4. **[`03_CONTEXT_MAP.md`](03_CONTEXT_MAP.md)**
   - Strategic Domain-Driven Design (DDD) Context Map defining bounded contexts, upstream/downstream relationships, and integration patterns.

5. **[`04_ARCHITECTURE_PRINCIPLES.md`](04_ARCHITECTURE_PRINCIPLES.md)**
   - Engineering rationales behind platform-first design, byte-for-byte determinism, immutability, explicit constructor injection, streaming resource validation, and forbidden anti-patterns.

6. **[`05_DEPENDENCY_RULES.md`](05_DEPENDENCY_RULES.md)**
   - Strict clean dependency rules, package import boundaries, and concrete code examples of valid vs forbidden dependencies.

7. **[`06_DOMAIN_LAYER_GUIDE.md`](06_DOMAIN_LAYER_GUIDE.md)**
   - Developer implementation guide for Domain Entities, Aggregates, Value Objects, Domain Events, and Repository interface ports (`vn.loi.learning.domain.*`).

8. **[`07_APPLICATION_LAYER_GUIDE.md`](07_APPLICATION_LAYER_GUIDE.md)**
   - Guide for application use case orchestration, CQRS command/query separation, application ports, and diagnostic error handling (`vn.loi.learning.application.*`).

9. **[`08_INFRASTRUCTURE_LAYER_GUIDE.md`](08_INFRASTRUCTURE_LAYER_GUIDE.md)**
   - Guide for persistence adapters, OPD3 deterministic ZIP export, streaming inspection, security validation, and schema evolution (`vn.loi.learning.infrastructure.*`).

10. **[`09_PRESENTATION_LAYER_GUIDE.md`](09_PRESENTATION_LAYER_GUIDE.md)**
    - Presentation client guide for Compose Desktop and future mobile/web clients, enforcing Unidirectional Data Flow (UDF) and Zero Business Logic in UI (`vn.loi.learning.desktop.*`).

11. **[`10_IMPLEMENTATION_RULES.md`](10_IMPLEMENTATION_RULES.md)**
    - Coding conventions, package structures, error handling rules, test matrices, assertion standards, and git commit workflows.

---

## Architectural Decisions & Blueprints

- **Architecture Decision Records (ADRs)**: Located in [`../DECISIONS/`](../DECISIONS/)
- **Subsystem Platform Blueprints**: Located in [`../BLUEPRINTS/`](../BLUEPRINTS/)
