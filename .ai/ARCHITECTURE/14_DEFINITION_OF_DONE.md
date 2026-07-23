# Learning Engine 2.0 — Definition of Done (DoD)

## Purpose
This document establishes the explicit **Definition of Done (DoD)** criteria for every development activity in Learning Engine 2.0. A task, blueprint, capability, or implementation is NOT complete until all criteria for its category are 100% satisfied and verified.

---

## 1. Definition of Done: Architecture Blueprint
A Subsystem Blueprint (e.g., `PackagePlatform.md`, `WorkspacePlatform.md`) is DONE when:
- [ ] Purpose, Responsibilities, Out of Scope, Dependencies, Architecture Notes are fully documented without placeholders.
- [ ] Subsystem architecture diagram and component interaction flow are clearly drawn.
- [ ] Data models, Aggregate roots, Value Objects, and JSON schemas are explicitly defined.
- [ ] Security boundaries and resource safety limits are specified.
- [ ] Reviewed and linked within `.ai/BLUEPRINTS/README.md`.

---

## 2. Definition of Done: Capability Increment
A Roadmap Capability (e.g., Media Packaging, OPD3 Export, Package Inspector) is DONE when:
- [ ] Implementation reaches through real consumer and composition boundaries.
- [ ] Focused unit and integration tests are added for success, boundary, malformed input, failure, restart, and compatibility.
- [ ] 100% of automated tests pass via `.\gradlew.bat clean test` resulting in `BUILD SUCCESSFUL`.
- [ ] `git diff --check` passes with zero formatting errors.
- [ ] Code is committed in one intentional atomic commit following conventional commit format (`feat: ...` or `fix: ...`).
- [ ] Architectural documentation (`ARCHITECTURE.md`, `CHANGELOG.md`, `PROJECT_HANDOFF.md`) updated.

---

## 3. Definition of Done: Implementation Code
Every production Kotlin code file is DONE when:
- [ ] Complies strictly with Clean Architecture layer dependency rules (no infrastructure imports in domain/application).
- [ ] Domain models are immutable and enforce invariants in initialization blocks.
- [ ] Explicit constructor injection is used (no hidden singletons or global state).
- [ ] Public API signatures remain backward-compatible or include explicit migration tests.
- [ ] No secrets, hardcoded local absolute paths, or unhandled exceptions.

---

## 4. Definition of Done: Architecture Review
An Independent Architecture Review is DONE when:
- [ ] Critical evaluation performed across all 10 review areas (Domain Model, Dependency Direction, Single Source of Truth, Layering, Vocabulary, Events, Aggregates, Scalability, Risks, Documentation Quality).
- [ ] Findings and technical debt items logged in `12_TECHNICAL_DEBT_REGISTER.md`.
- [ ] Formal `ARCHITECTURE_REVIEW_REPORT.md` generated with clear Approval Decision.
- [ ] Atomic docs commit created.

---

## 5. Definition of Done: Production Ready Release
A release baseline is PRODUCTION READY when:
- [ ] Full clean build `.\gradlew.bat clean test` succeeds with zero test failures.
- [ ] Total exact test count verified from XML test results.
- [ ] Architecture Lock Version 1.0 active and approved in `ARCHITECTURE_LOCK.md`.
- [ ] Git status clean on `develop` branch.
