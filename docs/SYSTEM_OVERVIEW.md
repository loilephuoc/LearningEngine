# System Overview — Learning Engine 2.0

## 1. High-Level System Architecture

Learning Engine 2.0 is structured into clear, decoupled subsystem layers within a Kotlin/JVM multi-module project.

```text
                                 +------------------------------+
                                 |  Desktop Presentation Client |
                                 |  (Compose Desktop UI, State) |
                                 +------------------------------+
                                                |
                                                v
+---------------------------------------------------------------------------------------------------+
| APPLICATION LAYER                                                                                 |
|                                                                                                   |
|  +---------------------------------------------------------------------------------------------+  |
|  | PRODUCT BRAIN (Orchestration Boundary)                                                      |  |
|  |  ProductBrainPlanner                                                                        |  |
|  |  +-- LearningObjectivePolicy  --> LearningObjective                                         |  |
|  |  +-- LearningStrategyPlanner  --> LearningStrategyDefinition                                |  |
|  |  +-- LearningFlowTemplateFactory --> LearningFlowTemplate (Semantic Slots)                  |  |
|  |  +-- LearningFlowInstantiationService --> Resolves selections & calls LearningFlowPlanner  |  |
|  +---------------------------------------------------------------------------------------------+  |
|                                                |                                                  |
|                                                v                                                  |
|  +---------------------------------------------------------------------------------------------+  |
|  | LEARNING FLOW ENGINE                                                                        |  |
|  |  LearningFlowPlanner          --> Instantiates LearningFlowDefinition                       |  |
|  |  LearningFlowController       --> Manages stage transitions & reveal/rating readiness       |  |
|  +---------------------------------------------------------------------------------------------+  |
|                                                |                                                  |
|                                                v                                                  |
|  +---------------------------------------------------------------------------------------------+  |
|  | SCHEDULER & MEMORY ENGINE                                                                   |  |
|  |  FSRS Calculator              --> Computes memory stability, difficulty & next review interval |  |
|  |  Review Use Cases             --> Executes atomic review transactions                        |  |
|  +---------------------------------------------------------------------------------------------+  |
+---------------------------------------------------------------------------------------------------+
                                                |
                                                v
+---------------------------------------------------------------------------------------------------+
| DOMAIN & INFRASTRUCTURE LAYERS                                                                    |
|  Canonical Knowledge Model (World -> Topic -> Module -> Lesson -> Concept -> KnowledgeUnit -> Asset)|
|  Domain Models (StudySession, LearningContent, ReviewEvent)                                       |
|  Persistence (JsonMemoryStateStore, JsonStudySessionStore, Transactions)                          |
|  Packaging & Media (OPD3 Package Scanner/Importer, ContentMediaStorage)                           |
+---------------------------------------------------------------------------------------------------+
```

For complete specification of the subject-independent knowledge structure, see [`KNOWLEDGE_MODEL.md`](KNOWLEDGE_MODEL.md). For end-to-end session journey architecture, see [`LEARNING_EXPERIENCE_ARCHITECTURE.md`](LEARNING_EXPERIENCE_ARCHITECTURE.md). For the canonical interaction specification, see [`LEARNING_SCENE_FRAMEWORK.md`](LEARNING_SCENE_FRAMEWORK.md). For the catalog of pedagogical scene capabilities, see [`LEARNING_SCENE_LIBRARY.md`](LEARNING_SCENE_LIBRARY.md).





---

## 2. Key Subsystem Boundaries

1. **Product Brain Layer**:
   - **`LearningObjectivePolicy`**: Selects outcome objective (`DURABLE_RECALL`).
   - **`LearningStrategyPlanner`**: Derives strategy behavior without rotation dependency.
   - **`LearningFlowTemplateFactory`**: Translates strategy behavior into reusable, deterministic template slots without `LearningExperiencePlan` dependency.
   - **`ProductBrainPlanner`**: Application-level orchestration boundary.
   - **`LearningFlowInstantiationService`**: Resolves concrete runtime experience selections for template slots and delegates flow construction to `LearningFlowPlanner`.

2. **Learning Flow Engine**:
   - **`LearningFlowPlanner`**: Narrowly instantiates `LearningFlowDefinition` from template + resolved selections + `ExperienceRotationContext`.
   - **`LearningFlowController`**: Pure, immutable state-machine executing stage transitions (Experience → AnswerReveal → RatingReady).

3. **Scheduler & Memory Engine**:
   - Evaluates memory stability and difficulty using FSRS algorithms.
   - Computes due states and next review intervals.
   - Does NOT make teaching, presentation, or scene selection decisions.

4. **Domain & Infrastructure**:
   - Manages aggregate roots (`StudySession`, `Content`, `ReviewEvent`).
   - Handles JSON file persistence, atomic transaction snapshots, OPD3 package parsing, and local media extraction.

5. **Desktop Presentation Client**:
   - `DesktopLearningFlowCoordinator` depends strictly on `ProductBrainPlanner` and `LearningFlowController`.
   - Owns transient UI state, Compose rendering, focus, media playback, and keyboard routing.

---

## 3. Communication Rules

- **UI → Product Brain**: UI requests flow instantiation via `ProductBrainPlanner.planFlow(plan, rotation)`.
- **Product Brain → Flow Engine**: Product Brain resolves selections and calls `LearningFlowInstantiationService` / `LearningFlowPlanner`.
- **Flow Engine → Controller**: UI interacts with `LearningFlowController` to advance stages.
- **Controller → Scheduler**: Rating submission delegates to application review use cases to trigger FSRS scheduling and atomic persistence.
