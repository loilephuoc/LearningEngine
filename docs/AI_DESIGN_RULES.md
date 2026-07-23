# AI Design Rules — Learning Engine 2.0

## 1. Purpose

This document establishes strict, mandatory rules for any AI agent (including Antigravity, Codex, or subagents) contributing code, architecture, or documentation to Learning Engine 2.0.

---

## 2. Mandatory Workflow

### MUST
1. **Read Mandatory Documents**: Before proposing architecture or modifying source code, read the mandatory documents in the exact order specified in [`PROJECT_HANDOFF.md`](PROJECT_HANDOFF.md).
2. **Protect Product Philosophy**: Ensure all changes align with [`PRODUCT_PHILOSOPHY.md`](PRODUCT_PHILOSOPHY.md) and [`REPOSITORY_CONSTITUTION.md`](REPOSITORY_CONSTITUTION.md).
3. **Preserve Product Brain Ownership**: Keep pedagogical decision-making in `ProductBrainPlanner`, `LearningObjectivePolicy`, and `LearningStrategyPlanner`.
4. **Maintain Reusable Templates**: Ensure `LearningFlowTemplate` contains semantic slots only and never embeds `ExperienceSelectionResult`, `LearningItemId`, `SessionId`, or `ordinal`.
5. **Enforce Single UI Orchestration Boundary**: Ensure UI coordinators (such as `DesktopLearningFlowCoordinator`) depend ONLY on `ProductBrainPlanner` and `LearningFlowController`.
6. **Prioritize Learner Outcomes**: Evaluate every feature by its contribution to retention, engagement, and cognitive efficiency over raw feature count.
7. **Write Comprehensive Architecture Tests**: Every architectural boundary change must be accompanied by reflection-based architecture tests enforcing contract rules.

---

## 3. Strict Prohibitions

### MUST NOT
1. **MUST NOT Clone Anki / Traditional Flashcard Apps**: Do not add manual card configuration options, complex deck settings, or flashcard-specific UI clutter that shifts teaching decisions onto the learner.
2. **MUST NOT Put Pedagogy in UI**: Never place objective selection, strategy planning, experience rotation, or scheduling logic inside Compose components, ViewModels, or UI coordinators.
3. **MUST NOT Duplicate Product Brain**: Never build parallel orchestration boundaries or allow UI components to manually coordinate objective policies, strategy planners, and template factories.
4. **MUST NOT Mix Scheduling with Teaching**: Keep FSRS scheduler calculations strictly separate from Product Brain teaching strategies and scene generation.
5. **MUST NOT Contaminate Templates**: Never pass `LearningExperiencePlan` into `LearningFlowTemplateFactory.create()`, and never store `ExperienceSelectionResult` inside `LearningFlowTemplate` or `LearningStrategyDefinition`.
6. **MUST NOT Implement Unjustified Features**: Do not add features simply because another learning app has them. Every addition requires a clear pedagogical rationale.
7. **MUST NOT Break Backward Compatibility**: Never silently discard, rewrite, or partially persist user data or schema formats.
