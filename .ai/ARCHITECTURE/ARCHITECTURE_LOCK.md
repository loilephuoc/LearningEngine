# Learning Engine 2.0 — Architecture Lock Specification

## Architecture Baseline Metadata

| Field | Value |
|---|---|
| **Architecture Version** | **1.0** |
| **Status** | **LOCKED** |
| **Approval Date** | **July 23, 2026** |
| **Governance Body** | **Chief Architecture Governance Board** |
| **Repository Baseline** | `LearningEngine` (Branch `develop`, Commit `5684b20`) |

---

## 1. Architecture Scope

This **Architecture Lock** formally freezes the architectural baseline for Learning Engine 2.0 Version 1.0, encompassing:
1. **Constitution**: The 13 laws of [`00_ARCHITECTURE_CONSTITUTION.md`](00_ARCHITECTURE_CONSTITUTION.md) (categorized into CURRENT and TARGET architecture).
2. **Layering & Dependencies**: Pure Domain Layer isolation, Application Ports, and Clean Dependency rules (`05_DEPENDENCY_RULES.md`).
3. **OPD3 Package Standard**: Byte-for-byte deterministic export, streaming inspection, security path validation, and resource safety limits.
4. **Platform Core Strategy**: 100% UI-independent Kotlin/JVM engine core serving Desktop, Android, iOS, and Web presentation clients.
5. **Subsystem Status & Blueprints**: Status Registry (`11_ARCHITECTURE_STATUS.md`) and platform blueprints (`.ai/BLUEPRINTS/`).

---

## 2. Change Policy & Governance Rules

Now that Version 1.0 is **LOCKED**:
- **No Unplanned Structural Changes**: Feature development teams and AI agents MUST implement capabilities strictly within locked layer boundaries and application ports.
- **Production Code Isolation**: Architectural lock modifications must NOT touch production Kotlin code directly during documentation lock milestones.
- **Technical Debt Tracking**: All identified architecture debt must be logged in [`12_TECHNICAL_DEBT_REGISTER.md`](12_TECHNICAL_DEBT_REGISTER.md) and resolved according to prioritized target milestones.

---

## 3. How Future Architecture Changes Occur

If a future feature or platform expansion requires modifying an architectural boundary, port contract, or constitutional law:

```text
[ Feature / Expansion Requirement ]
               │
               ▼
[ Create Architecture Proposal (ADR) in .ai/DECISIONS/ ]
               │
               ▼
[ Formal Architecture Board Review against 15_CHECKLIST ]
               │
               ▼
[ Approved ADR ] ──> [ Update Subsystem Blueprint ] ──> [ Unlock & Bump Version to 1.1 ]
```

1. **Submit ADR**: Create an ADR in `.ai/DECISIONS/` detailing Context, Proposed Decision, Consequences, and Alternatives Considered.
2. **Board Review**: Execute review using [`15_ARCHITECTURE_REVIEW_CHECKLIST.md`](15_ARCHITECTURE_REVIEW_CHECKLIST.md).
3. **Blueprint & Lock Update**: Upon ADR approval, update the corresponding blueprint in `.ai/BLUEPRINTS/`, increment the Architecture Lock version (e.g., Version 1.1), and log the baseline transition.

---

## 4. Relationship with ADRs and Blueprints

- **ADRs (`.ai/DECISIONS/`)**: Define individual architectural decisions, trade-offs, and historical choices.
- **Blueprints (`.ai/BLUEPRINTS/`)**: Specify exact technical component structures, data models, and schemas for each subsystem.
- **Architecture Lock (`ARCHITECTURE_LOCK.md`)**: The binding contract that binds all ADRs, Blueprints, and Constitutional Laws into an immutable, versioned baseline.
