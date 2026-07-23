# Learning Engine 2.0 — Architecture Governance

## Purpose
This document defines the **Architecture Governance Framework** for Learning Engine 2.0. It specifies authority rules, the Architecture Decision Record (ADR) approval workflow, constitution modification policies, mandatory review triggers, and the formal definition of an **Architecture Lock**.

---

## 1. Architecture Authority Rules

1. **Chief Architecture Governance Engineer / Architecture Board**:
   - Holds ultimate authority over architectural boundaries, constitutional laws, ADR approvals, and Architecture Lock baselines.
   - Any architectural modification requires explicit Board review and approval.
2. **Feature Development Teams & AI Agents**:
   - Must implement code strictly within established architectural boundaries and ports.
   - May not alter layer dependency rules, introduce global state, or bypass application ports without an approved ADR.

---

## 2. Architecture Decision Record (ADR) Workflow

An ADR is required for any proposal that:
- Introduces a new database, storage format, or major third-party framework.
- Modifies existing application port signatures or aggregate root boundaries.
- Alters cross-context communication patterns or domain event contracts.

### ADR Lifecycle
```text
[ Proposed ] ──> [ Under Architecture Review ] ──> [ Approved / Rejected ] ──> [ Superseded ]
```

1. **Proposal**: Author creates `ADR-XXXX-<title>.md` in `.ai/DECISIONS/` using the standard ADR template (Context, Decision, Consequences, Alternatives Considered, Status).
2. **Review**: Reviewed by Chief Architect against `00_ARCHITECTURE_CONSTITUTION.md`.
3. **Approval**: Upon approval, status transitions to `APPROVED`. The Architecture Status matrix (`11_ARCHITECTURE_STATUS.md`) is updated accordingly.

---

## 3. Constitution Modification Policy

- `00_ARCHITECTURE_CONSTITUTION.md` is the supreme law of the repository.
- Modifying Constitutional Laws requires:
  1. An approved ADR demonstrating mandatory technical justification.
  2. Formal Architecture Board review.
  3. Re-evaluation of all affected domain blueprints.

---

## 4. Mandatory Architecture Review Triggers

An Independent Architecture Review is mandatory whenever:
- A major roadmap Phase (e.g., Phase 2, Phase 3) completes its blueprint foundation.
- A new Bounded Context or platform target (Android, iOS, Web) is added to the codebase.
- A high-severity technical debt item (`TD-01`) is targeted for resolution.

---

## 5. Definition of Architecture Lock

An **Architecture Lock** is a formal governance milestone declaring that the architecture specification for a target version (e.g., Version 1.0) is complete, internally consistent, verified against codebase HEAD, and frozen for implementation.

### Implications of Architecture Lock:
- No architectural structural changes are permitted during feature implementation sprints.
- Implementation teams must build strictly within locked boundaries and ports.
- Unplanned structural changes require unlocking the architecture via a formal ADR review cycle.
