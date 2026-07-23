# ADR-0003: UI Layers Must Render Scenes and Never Make Teaching Decisions

- **Status**: Accepted
- **Date**: 2026-07-23
- **Deciders**: Architecture Team, Product Architect

## Context

Presentation layers (such as Compose Desktop, Android Views/Compose, or Web clients) are responsible for rendering visual controls, managing layout focus, handling keyboard shortcuts, and playing local media. In improperly decoupled applications, UI code frequently takes on business logic—such as determining card difficulty, choosing the next learning mode, or managing review queues.

## Decision

We decide that **UI layers (Desktop, Android, iOS, Web) are strictly presentation clients and MUST NOT make teaching, strategy, or scheduling decisions.**

- Presentation coordinators (e.g., `DesktopLearningFlowCoordinator`) must depend ONLY on `ProductBrainPlanner` and `LearningFlowController`.
- UI coordinators must not directly coordinate `LearningObjectivePolicy`, `LearningStrategyPlanner`, `LearningFlowTemplateFactory`, `LearningFlowInstantiationService`, or `LearningFlowPlanner`.
- UI layers render the active `LearningFlowStage` / `LearningScene` and dispatch user interactions back to the shared application controllers.

## Consequences

### Positive
- Client UI code remains thin, declarative, and focused entirely on presentation quality.
- Prevents pedagogical logic duplication across different platform clients.
- Enables complete automated unit testing of teaching flows without UI frameworks.

### Negative / Trade-offs
- UI coordinators must map state through application boundaries rather than executing inline logic.
