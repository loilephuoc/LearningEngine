# Desktop Alpha Architecture Review — Closed Teaching Loop Assessment

## 1. Executive Summary & Review Scope

This document presents an architectural audit of the implementation introduced across **Desktop Alpha-01 — Session Bootstrap**, **Desktop Alpha-02 — Scene Execution**, and **Desktop Alpha-03 — Adaptive Decision** (commits `9977fed`, `6f9b935`, `357faee`).

The evaluation measures code conformance against the architectural foundations established in:
- [`Repository Constitution`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/AGENTS.md)
- [`docs/PROJECT_HANDOFF.md`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/docs/PROJECT_HANDOFF.md)
- [`docs/ARCHITECTURE_AUDIT_V1.md`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/docs/ARCHITECTURE_AUDIT_V1.md)
- [`docs/LEARNING_SCENE_FRAMEWORK.md`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/docs/LEARNING_SCENE_FRAMEWORK.md)
- [`docs/INSTRUCTIONAL_DECISION_ENGINE.md`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/docs/INSTRUCTIONAL_DECISION_ENGINE.md)

### Key Finding
Desktop Alpha-01 through Alpha-03 successfully establish a **platform-neutral, closed adaptive teaching loop**. Product Brain owns all pedagogical decisions (bootstrap, scene selection, evidence evaluation, adaptive decisioning, timeline updates), while presentation clients (`desktop`) remain 100% presentation-only without introducing any instructional logic into the UI. All 1,643 automated tests pass cleanly.

---

## 2. Architectural Audit of the 12 Core Areas

### 2.1. ProductBrainPlanner Responsibility and API Growth
- **Current State**: [`ProductBrainPlanner`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/src/main/kotlin/vn/loi/learning/application/learningstrategy/ProductBrainPlanner.kt) acts as the single orchestration boundary for Product Brain. In Alpha-01 to Alpha-03, methods `bootstrapSession`, `selectFirstScene`, `processEvidence`, and `evaluateAndAdapt` were added directly to `ProductBrainPlanner`.
- **Assessment**: For Alpha iterations, aggregating session bootstrap, scene selection, evidence processing, and adaptive evaluation into `ProductBrainPlanner` provides a clean, cohesive application facade. However, as Product Brain expands, `ProductBrainPlanner` will benefit from delegating sub-workflows to specialized sub-planners or domain services to maintain Single Responsibility.

### 2.2. Separation Between SceneResult and LearningEvidence
- **Current State**: [`SceneResult`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/src/main/kotlin/vn/loi/learning/application/scene/SceneResult.kt) encapsulates interaction-level evaluation output (`isExactMatch`, `isNormalizedMatch`, `attemptLatencyMs`, `editDistance`), whereas [`LearningEvidence`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/src/main/kotlin/vn/loi/learning/application/scene/LearningEvidence.kt) encapsulates the normalized domain evidence payload (`evidenceId`, `performance`, `userAttempt`, `attemptLatencyMs`).
- **Assessment**: **Excellent architectural separation.** `SceneResult` is specific to transient scene execution, while `LearningEvidence` is the platform-neutral payload returned to Product Brain. This prevents scene execution details from distorting core memory models or scheduler queues.

### 2.3. Ownership of Evidence Translation
- **Current State**: [`TypingRecallScene.toEvidence(...)`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/src/main/kotlin/vn/loi/learning/application/scene/TypingRecallScene.kt) maps `isNormalizedMatch` $\rightarrow$ `CORRECT`, `editDistance <= 2` $\rightarrow$ `PARTIAL`, else `INCORRECT`.
- **Assessment**: Encapsulating `toEvidence` inside the `LearningScene` implementation keeps scene translation self-contained. However, hardcoding `editDistance <= 2` as `PARTIAL` embeds a pedagogical threshold inside the scene class rather than in Product Brain policy. For Alpha, this is an acceptable simplification, but post-Alpha grading parameters should be injected or configured via Product Brain policy.

### 2.4. InstructionalDecisionEngine Boundaries
- **Current State**: [`InstructionalDecisionEngine`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/src/main/kotlin/vn/loi/learning/application/decision/InstructionalDecisionEngine.kt) evaluates `LearningEvidence` and `currentDifficulty` to return `Pair<AdaptiveDecision, DecisionTrace>`.
- **Assessment**: **Strictly platform-neutral and decoupled.** The decision engine depends only on core application models (`LearningEvidence`, `AdaptiveDecision`, `DecisionTrace`) and contains no filesystem, UI, or framework dependencies.

### 2.5. DecisionTrace Completeness and Explainability Readiness
- **Current State**: [`DecisionTrace`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/src/main/kotlin/vn/loi/learning/application/decision/DecisionTrace.kt) records `traceId`, `timestamp`, `evidenceId`, `rulesTriggered`, `decision`, and human-readable `explanation`.
- **Assessment**: Strong foundation for decision auditing. In Alpha-03, `rulesTriggered` captures rule keys (`FAST_CORRECT_MASTERY_BOOST`, `INCORRECT_SCAFFOLDING_SUPPORT`, `PARTIAL_CONSOLIDATION_RETRY`, `NOMINAL_PROGRESSION`). Future post-Alpha expansion should include a structured `inputSnapshot` map (fatigue, motivation, retrievability) to support complete offline replay.

### 2.6. SessionTimeline Mutation and Authority
- **Current State**: [`SessionTimeline`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/src/main/kotlin/vn/loi/learning/application/session/bootstrap/SessionTimeline.kt) is an immutable data class with `updateWithDecision(action: AdaptiveAction): SessionTimeline`.
- **Assessment**: Product Brain owns timeline updates via `ProductBrainPlanner.evaluateAndAdapt(...)`. The UI receives the updated `SessionTimeline` in `StudyUiState` and renders it passively.

### 2.7. Difficulty Representation and Lifecycle
- **Current State**: Represented as `currentDifficultyLevel: Int` (default 1) in [`AdaptiveDecision`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/src/main/kotlin/vn/loi/learning/application/decision/AdaptiveDecision.kt) and `StudyUiState`.
- **Assessment**: Integer levels (1, 2, 3...) provide a clean scalar representation for Alpha. As defined in `INSTRUCTIONAL_DECISION_ENGINE.md`, future post-Alpha milestones will expand difficulty into multi-dimensional metadata (prompt complexity, distractor plausibility, hint availability, time limits).

### 2.8. Desktop StudyFacade, StudyViewModel, and StudyUiState Responsibilities
- **Current State**: [`StudyFacade`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyFacade.kt) coordinates session presentation workflows; [`StudyViewModel`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyViewModel.kt) manages transient UI state updates; [`StudyUiState`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyUiState.kt) projects state to Compose UI.
- **Assessment**: **Clean presentation layer architecture.** The Desktop UI acts purely as a renderer and input collector, delegating all teaching decisions to `ProductBrainPlanner`.

### 2.9. UI Independence from Instructional Logic
- **Current State**: Audited `StudyFacade.kt`, `StudyViewModel.kt`, and `StudyUiState.kt`. Zero decision rules, zero grading logic, and zero hardcoded state transitions exist in the UI layer.
- **Assessment**: **100% compliant** with Repository Constitution and Architecture Audit rules.

### 2.10. Readiness for Multiple Scene Types
- **Current State**: Interface [`LearningScene`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/src/main/kotlin/vn/loi/learning/application/scene/LearningScene.kt) is generic and platform-neutral. [`TypingRecallScene`](file:///c:/Users/M72Q/IdeaProjects/LearningEngine/src/main/kotlin/vn/loi/learning/application/scene/TypingRecallScene.kt) implements `LearningScene`. `AdaptiveDecision.targetSceneType` supports scene type identification.
- **Assessment**: **Ready.** Additional scenes (`FlashcardScene`, `MultipleChoiceScene`, `StoryScene`) can be implemented by creating new `LearningScene` classes and extending Product Brain scene selection.

### 2.11. Readiness for Session Completion
- **Current State**: `SessionPhase.SUMMARY` and `SessionTimeline` exist. Alpha-03 updates timeline and evaluates evidence.
- **Assessment**: Alpha-03 intentionally bounds execution to active scene evaluation and adaptive decisions without auto-advancing to session wrap-up or FSRS updates. Full multi-phase progression and session completion are scheduled for Alpha-04.

### 2.12. Test Coverage Across Alpha-01 to Alpha-03 Flow
- **Current State**: Covered by focused unit and integration test suites:
  - `ProductBrainSessionBootstrapTest.kt`
  - `DesktopSessionBootstrapTest.kt`
  - `TypingRecallSceneTest.kt`
  - `DesktopSceneExecutionTest.kt`
  - `InstructionalDecisionEngineTest.kt`
  - `DesktopAdaptiveDecisionTest.kt`
- **Assessment**: **Comprehensive.** Full repository test suite passes with 1,643 passing tests.

---

## 3. Detailed Findings Matrix

| ID | Component / Area | Evidence | Assessment | Risk | Recommendation | Priority | Must Fix Before Alpha-03.5 |
|---|---|---|---|---|---|---|---|
| **F-01** | `ProductBrainPlanner` API Surface | `ProductBrainPlanner.kt` holds `bootstrapSession`, `selectFirstScene`, `processEvidence`, `evaluateAndAdapt`. | Aggregates multiple application sub-workflows into a single facade class. | Medium (Long-term maintainability as rules expand). | Extract sub-planner delegates (e.g. `SessionBootstrapPlanner`, `SceneExecutionPlanner`) behind `ProductBrainPlanner` in post-Alpha refactoring. | **Medium** | No |
| **F-02** | Evidence Translation Ownership | `TypingRecallScene.toEvidence` hardcodes `editDistance <= 2` $\rightarrow$ `PARTIAL`. | Scene embeds pedagogical grading threshold instead of delegating to Product Brain policy. | Low (Acceptable Alpha simplification). | Parameterize grading thresholds via policy configuration in post-Alpha iterations. | **Low** | No |
| **F-03** | `DecisionTrace` Input Snapshot | `DecisionTrace.kt` records `rulesTriggered` and `explanation`. | Lacks structured snapshot of raw input signals (fatigue, motivation, retrievability). | Low (Sufficient for current 4-rule set). | Add `inputSnapshot: Map<String, Any>` to `DecisionTrace` in post-Alpha audit logging. | **Low** | No |
| **F-04** | Scalar Difficulty Representation | `currentDifficultyLevel: Int` used in `AdaptiveDecision` and `StudyUiState`. | Scalar integer is simpler than multi-dimensional `DifficultyMetadata`. | Low (Acceptable Alpha simplification). | Expand `DifficultyMetadata` into multi-dimensional domain type in post-Alpha phases. | **Low** | No |
| **F-05** | Session Phase Progression | `submitSceneAttempt` updates timeline but does not auto-advance session phase state to `SUMMARY`. | Active session does not yet auto-complete. | Low (Intended scope boundary for Alpha-03). | Deliver multi-phase progression and session completion in **Desktop Alpha-04**. | **Medium** | No |

---

## 4. System Strengths

1. **Strict Inward Dependency Flow**: `desktop` $\rightarrow$ `application` $\rightarrow$ `domain`. Zero platform leakage into core Kotlin modules.
2. **Platform-Neutral Closed Teaching Loop**: Product Brain owns session bootstrap, scene creation, evidence processing, adaptive decisioning, and timeline updates in pure Kotlin code.
3. **Clean Scene Abstraction**: `LearningScene` interface (`prepare`, `evaluate`, `toEvidence`) cleanly separates scene execution from core Product Brain decision making.
4. **Auditable Decision Tracing**: `DecisionTrace` logs rule keys and pedagogical rationale for every adaptive decision.
5. **Zero UI Instructional Logic**: Desktop Compose UI remains strictly presentation-only.
6. **High Test Quality**: 1,643 tests passing with zero regressions across core and presentation layers.

---

## 5. API Stability Assessment

- **`vn.loi.learning.application.session.bootstrap`**: **STABLE.** `LearningSessionContext`, `TeachingGoal`, `SessionTimeline`, `InitialDecisionSnapshot`, `SessionOverview` provide durable contracts for session initialization.
- **`vn.loi.learning.application.scene`**: **STABLE.** `LearningSceneInput`, `SceneResult`, `LearningEvidence`, `EvidenceReceipt`, `LearningScene` contracts support scalable scene additions.
- **`vn.loi.learning.application.decision`**: **STABLE.** `AdaptiveAction`, `AdaptiveDecision`, `DecisionTrace`, `AdaptiveOutcome`, `InstructionalDecisionEngine` establish a clean adaptive decision pipeline.

---

## 6. Prioritized Refactoring Backlog

1. **[Medium] `ProductBrainPlanner` Sub-service Modularization**: Group session bootstrap, scene planning, and decision evaluation into sub-planners behind `ProductBrainPlanner`.
2. **[Low] Externalized Scene Grading Policy**: Extract hardcoded scene grading thresholds (`editDistance <= 2`) into Product Brain policy configuration.
3. **[Low] Decision Trace Input Signal Snapshot**: Add `inputSnapshot` map to `DecisionTrace` for complete state reproducibility.
4. **[Low] Multi-Dimensional Difficulty Model**: Expand `currentDifficultyLevel: Int` into multi-dimensional `DifficultyMetadata`.

---

## 7. Milestone Recommendations

### Recommendation for Milestone Alpha-03.5 (Polish & Consolidation)
- **Verdict**: **GO**
- **Rationale**: The architecture across Alpha-01, Alpha-02, and Alpha-03 forms a cohesive, clean, and extensible closed loop. No critical or high architectural defects block continuation. Alpha-03.5 can proceed with UI polish, visual overview enhancements, and scene interaction refinements.

### Recommendation for Milestone Alpha-04 (Multi-Phase Session Loop & Completion)
- **Verdict**: **GO**
- **Rationale**: The underlying contracts (`SessionTimeline`, `LearningEvidence`, `AdaptiveDecision`, `InstructionalDecisionEngine`) are fully ready to support multi-phase transitions (Warm-up $\rightarrow$ Teaching $\rightarrow$ Practice $\rightarrow$ Review $\rightarrow$ Reflection $\rightarrow$ Summary) and session completion in Desktop Alpha-04.
