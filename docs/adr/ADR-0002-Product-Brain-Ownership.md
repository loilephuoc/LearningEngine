# ADR-0002: Product Brain Ownership and Application Layer Orchestration

- **Status**: Accepted
- **Date**: 2026-07-23
- **Deciders**: Architecture Team, Product Architect

## Context

Prior implementations risked blurring the lines between product decision-making (pedagogy), scheduling calculations (FSRS), flow execution, and UI presentation. If teaching decisions are scattered across UI components or embedded directly in the scheduler, the system becomes rigid, hard to test, and impossible to reuse across platforms.

## Decision

We decide to **centralize all pedagogical decision-making within the Product Brain on the application layer.**

- `LearningObjectivePolicy` resolves outcomes (`DURABLE_RECALL`).
- `LearningStrategyPlanner` derives strategy behavior semantically (`includeOptionalTyping`) without rotation dependency.
- `LearningFlowTemplateFactory` translates strategy into reusable template slots with zero `LearningExperiencePlan` dependency.
- `ProductBrainPlanner` acts as the single application-level orchestration boundary.
- `LearningFlowInstantiationService` resolves concrete runtime experience selections and delegates flow instantiation to `LearningFlowPlanner`.

## Consequences

### Positive
- Enforces a clean, testable separation of concerns.
- Keeps `LearningFlowPlanner` focused narrowly on recipe instantiation without absorbing Product Brain rules.
- Guarantees reusable, deterministic templates free of runtime selection contamination.

### Negative / Trade-offs
- Requires strict adherence to layer boundaries and reflection-based architecture tests to prevent leakage.
