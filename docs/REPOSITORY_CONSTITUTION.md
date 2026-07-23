# Repository Constitution — Learning Engine 2.0

## Pre-amble

This Constitution contains the non-negotiable architectural laws of Learning Engine 2.0. All software engineers, AI assistants, and contributors MUST strictly comply with these laws. No pull request, commit, or architectural modification may violate these articles.

---

## Article I: Fundamental Nature

1. **Teaching Engine Supremacy**: Learning Engine 2.0 is an adaptive Teaching Engine, NOT a flashcard application or Anki clone.
2. **Pedagogical Autonomy**: The engine is responsible for deciding what, how, and when to teach. The learner experience must prioritize guided progression over manual administration.
3. **No Feature Parity Fallacy**: Features from other applications (such as Anki, Quizlet, or traditional SRS software) shall not be implemented simply because they exist elsewhere. Every feature must be justified by its measurable contribution to learning outcomes.

---

## Article II: Ownership & Separation of Concerns

1. **Product Brain Ownership**: Product Brain alone owns teaching decisions, strategy selection, objective resolution, and scene sequencing.
2. **Scheduler Ownership**: The Scheduler owns memory calculation, interval computation, and due-state determination ONLY. It shall not make presentation, scene, or teaching strategy decisions.
3. **UI Ownership Boundary**: Presentation clients (Desktop, Mobile, Web) own rendering, layout, focus, media playback, and user input routing. **UI layers shall never make pedagogical or teaching decisions.**
4. **Flow Planner Scope**: `LearningFlowPlanner` is an instantiator only. It converts resolved recipes into runtime flow definitions and shall not absorb Product Brain decision-making.

---

## Article III: Template Reusability & Immutability

1. **Template Independence**: `LearningFlowTemplate` must describe a reusable presentation recipe containing semantic slots only (`ROTATED_PRIMARY`, `OPTIONAL_TYPING`, `ANSWER_REVEAL`, `RATING_READY`).
2. **No Selection Contamination**: `LearningFlowTemplate` and `LearningFlowTemplateStage` must NEVER contain runtime selection results (`ExperienceSelectionResult`), item IDs (`LearningItemId`), session IDs (`SessionId`), rotation ordinals, or platform UI state.
3. **Factory Isolation**: `LearningFlowTemplateFactory` translates `LearningStrategyDefinition` into reusable template slots with zero dependency on `LearningExperiencePlan`.

---

## Article IV: Cross-Platform Integrity

1. **Single Engine Core**: Product Brain, Learning Flow, Scheduler, Domain Models, and Persistence contracts live exclusively in the platform-neutral shared core.
2. **Reference Client**: Compose Desktop is the 1.0 reference client. Future clients (Android, iOS, Web) shall consume the shared core without duplicating teaching or scheduling logic.
3. **No Platform Infiltration**: Shared core contracts shall not be polluted with platform-specific UI or runtime dependencies.

---

## Article V: Data Safety & Backward Compatibility

1. **Non-Destructive Persistence**: Persisted user data, review events, and schema formats are immutable product contracts. Incompatible schema rewrites or silent data drops are strictly forbidden.
2. **Atomic Writes**: Persisted mutations must occur within established transaction boundaries. A failed transaction must restore exact pre-failure state without leaving partial updates.

---

## Article VI: AI Agent Governance

1. **Sole Authority of AGENTS.md**: [`AGENTS.md`](../AGENTS.md) is the sole authority for AI workflow, delivery policy, safety rules, and stop conditions.
2. **Repository as Source of Truth**: Chat history is transient. Verified repository code, tests, and documentation constitute the sole durable memory of the project.
