# Product Brain — Conceptual Framework

## 1. Overview

The **Product Brain** is the central pedagogical intelligence of Learning Engine 2.0. It is responsible for making all teaching decisions autonomously, freeing the learner from manual mode configuration.

Product Brain lives entirely in the application layer of the platform-neutral core. It is UI-independent and client-agnostic.

---

## 2. The Core Teaching Loop

Product Brain operates through a continuous, closed-loop feedback system:

```text
       +-------------------------------------------------------+
       |                                                       |
       v                                                       |
Learner Model  -->  Teaching Objective  -->  Teaching Strategy |
       ^                                            |          |
       |                                            v          |
Evidence Log  <--  Learner Response  <--  Learning Scene  <----+
```

1. **Learner Model**: Represents the current state of the learner's knowledge, memory stability, difficulty history, and engagement patterns.
2. **Teaching Objective**: Determines *what* outcome the system is attempting to achieve for the active item or session (e.g., Durable Recall, Rapid Familiarization, Active Synthesis).
3. **Teaching Strategy**: Determines *how* to achieve the objective by selecting pedagogical rules, rotation policies, and eligibility criteria.
4. **Learning Scene**: Constructs the presentation recipe (e.g., Image Prompt, Listening Prompt, Typing Challenge, Answer Reveal) presented to the learner.
5. **Learner Response**: Captures learner interaction, attempt evaluation, and self-reported rating.
6. **Evidence Log**: Feeds response data back into the Learner Model and Scheduler to update memory state and refine future teaching decisions.

---

## 3. Product Brain Architecture Layers

The Product Brain pipeline consists of distinct, single-responsibility components:

- **`LearningObjectivePolicy`**: Evaluates the learner's current state and content capabilities to resolve a `LearningObjective` (e.g., `DURABLE_RECALL`).
- **`LearningStrategyPlanner`**: Inspects the objective and `LearningExperiencePlan` to derive a rotation-independent `LearningStrategyDefinition` (specifying primary strategy rules and optional challenge eligibility).
- **`LearningFlowTemplateFactory`**: Translates `LearningStrategyDefinition` into a reusable, deterministic `LearningFlowTemplate` containing semantic slots (`ROTATED_PRIMARY`, `OPTIONAL_TYPING`, `ANSWER_REVEAL`, `RATING_READY`).
- **`ProductBrainPlanner`**: The sole application-level orchestration boundary coordinating Objective → Strategy → Template → Instantiation.
- **`LearningFlowInstantiationService`**: Combines `LearningFlowTemplate`, `LearningStrategyDefinition`, `LearningExperiencePlan`, and `ExperienceRotationContext` to resolve concrete `ExperienceSelectionResult` values for template slots and delegate runtime flow creation to `LearningFlowPlanner`.

---

## 4. Conceptual Responsibilities

Product Brain is exclusively responsible for:
- Deciding which learning experiences best serve an objective.
- Determining when optional or advanced exercise modes (such as Typing Recall) are eligible.
- Orchestrating the sequence of stages in a Learning Flow.
- Adapting teaching strategy based on learner performance and historical evidence.

Product Brain is **NOT** responsible for:
- Executing UI rendering or managing Compose layout state (owned by Presentation/Client).
- Calculating mathematical memory stability or next review intervals (owned by Scheduler/FSRS).
- Managing raw filesystem I/O or JSON serialization (owned by Infrastructure).

---

## 5. Future Evolution

As Learning Engine matures, Product Brain will evolve from rule-based policies to advanced adaptive models, incorporating:
- Multi-dimensional difficulty adjustment based on response latency and attempt history.
- AI-augmented content generation and contextual hints.
- Dynamic session sizing and cognitive fatigue detection.
- Personalized scene sequencing tailored to individual learning styles.
