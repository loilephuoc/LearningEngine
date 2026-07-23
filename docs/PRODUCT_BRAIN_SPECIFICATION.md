# Product Brain Specification — The AI Teacher Blueprint

## 1. Overview & Vision

The **Product Brain** is the pedagogical engine of Learning Engine 2.0. It acts as an autonomous AI Teacher, transforming raw knowledge content and scheduling data into adaptive, guided learning experiences.

The learner interacts with the application through a single primary action: **"Start Learning"**. The Product Brain handles all subsequent instructional decisions:
- *Who* is learning (Learner Profile & Model).
- *What* to teach right now (Teaching Goal & Content Selection).
- *How* to teach it (Teaching Strategy & Scene Parameters).
- *When* to challenge vs. encourage (Difficulty Adaptation, Fatigue & Motivation).
- *How* to evaluate response evidence to update long-term mastery (Learning Evidence & Reflection).

This document serves as the official architectural blueprint for all future Product Brain implementations across Desktop, Mobile, and Web platforms.

---

## 2. Core Pedagogical Concepts

### 2.1. Learner Profile
The persistent identity and baseline preferences of the learner (e.g., target language, native language, study availability, preferred exercise modalities).

### 2.2. Learning Goal
The high-level objective declared by the learner or curriculum (e.g., "Achieve B2 conversational fluency in Spanish" or "Master 1,000 core vocabulary items").

### 2.3. Teaching Goal
The immediate, tactical pedagogical objective formulated by Product Brain for a specific item or session phase (e.g., `DURABLE_RECALL`, `RAPID_FAMILIARIZATION`, `ACTIVE_SYNTHESIS`, or `CONFIDENCE_RECOVERY`).

### 2.4. Knowledge Model
The platform-neutral representation of what the learner knows, including memory stability (FSRS), difficulty ratings, error history, and retention statistics.

### 2.5. Content Semantics
The structured attributes of learning material—including semantic blocks (text, image, audio, example sentences), topic tags, prerequisite relationships, and presentation capabilities.

### 2.6. Session Context
The transient state of the active study session, including current queue position, session elapsed time, consecutive correct/incorrect responses, and recent scene rotation history.

### 2.7. Teaching Strategy
The high-level ruleset selected by Product Brain to achieve a Teaching Goal (e.g., including optional typed challenges, enforcing listening-first recall, or prioritizing image prompts).

### 2.8. Learning Scene
The multi-stage presentation recipe rendered to the learner (e.g., Prompt Recall → Answer Reveal → Manual Rating, or Image Prompt → Typed Challenge → Answer Reveal → Rating Ready).

### 2.9. Difficulty Adaptation
The dynamic adjustment of exercise complexity (e.g., switching from passive recognition to active typing recall) based on recent performance, response latency, and memory stability.

### 2.10. Motivation
The psychological state of the learner. Product Brain protects motivation by preventing streak breaks, interspersing encouraging items during difficult reviews, and celebrating mastery milestones.

### 2.11. Fatigue
The accumulation of cognitive exhaustion during a study session. Product Brain monitors session duration, latency spikes, and error clusters to reduce item difficulty or recommend session completion.

### 2.12. Confidence
The learner's self-efficacy and certainty in their knowledge. Product Brain builds confidence through gradual scaffolding before introducing high-challenge exercises.

### 2.13. Mastery
The long-term state of durable acquisition. An item achieves mastery when memory stability exceeds target retention thresholds across extended intervals and diverse Learning Scenes.

### 2.14. Learning Evidence
The observable data captured during a Learning Scene interaction, including accuracy, attempt latency, self-reported rating, and selected experience choices.

### 2.15. Teaching Outcome
The qualitative evaluation of a completed Learning Scene (e.g., `SUCCESSFUL_RECALL`, `HESITANT_RECALL`, or `RECALL_FAILURE`).

### 2.16. Session Reflection
The post-session synthesis where Product Brain analyzes evidence, evaluates overall retention performance, and updates the Learner Model.

### 2.17. Long-Term Learner Model
The durable, historical record of the learner's cognitive growth, decay rates, learning velocity, and long-term retention trajectories across all content packages.

---

## 3. The Complete Teaching Loop

Product Brain operates through a continuous 10-phase closed-loop cycle during every learning interaction:

```text
  (1) Learner Profile & History
               │
               ▼
       (2) Diagnosis (Fatigue, Motivation, Memory State)
               │
               ▼
       (3) Formulate Teaching Goal (e.g., DURABLE_RECALL)
               │
               ▼
       (4) Select Teaching Strategy (Rules & Eligibility)
               │
               ▼
       (5) Construct Reusable Flow Template (Semantic Slots)
               │
               ▼
       (6) Instantiation & Scene Parameter Selection
               │
               ▼
       (7) Render Learning Experience (UI Execution)
               │
               ▼
       (8) Evidence Collection (Response, Latency, Rating)
               │
               ▼
       (9) Reflection & FSRS Memory Update
               │
               ▼
      (10) Learner Model Update & Next Item/Session Planning
```

1. **Learner**: The learner initiates study via **"Start Learning"**.
2. **Diagnosis**: Product Brain inspects current Session Context, cognitive fatigue indicators, recent performance, and FSRS memory stability.
3. **Teaching Goal**: Resolves the target objective for the next item (e.g., `DURABLE_RECALL`).
4. **Teaching Strategy**: Formulates strategy parameters (e.g., `includeOptionalTyping = true`).
5. **Template Construction**: `LearningFlowTemplateFactory` generates a reusable `LearningFlowTemplate` containing semantic slots.
6. **Instantiation & Parameters**: `LearningFlowInstantiationService` resolves concrete experience selections (e.g., RoundRobin primary, UserChoice typing) based on `ExperienceRotationContext`.
7. **Learning Experience**: The presentation client (Desktop, Mobile, Web) renders the multi-stage `LearningFlowDefinition`.
8. **Evidence Collection**: Interaction evidence (response accuracy, time elapsed, self-rating) is captured.
9. **Reflection**: Application use cases run atomic transactions to update FSRS memory state and record review events.
10. **Learner Model Update**: Long-term retention statistics and session fatigue metrics are updated, planning the next item transition or session wrap-up.

---

## 4. Subsystem Responsibility Matrix

| Responsibility Area | Product Brain | Learning Flow Engine | Scheduler (FSRS) | Presentation UI (Desktop/Mobile) | Shared Core |
|---|---|---|---|---|---|
| **Formulate Teaching Goal** | **OWNER** | No | No | No | Defines Interface |
| **Select Teaching Strategy** | **OWNER** | No | No | No | Defines Interface |
| **Create Flow Template** | **OWNER** | No | No | No | Defines Interface |
| **Instantiate Flow Definition** | Delegates | **OWNER** | No | No | Executes |
| **Manage Stage Transitions** | No | **OWNER** | No | Interacts | Executes |
| **Compute Next Review Interval**| No | No | **OWNER** | No | Executes Math |
| **Render Screens & Layouts** | No | No | No | **OWNER** | No |
| **Handle Keyboard & Audio** | No | No | No | **OWNER** | Platform Audio |
| **Atomic Persistence** | Invokes | No | Invokes | No | **OWNER** (Infrastructure) |

---

## 5. Product Brain Principles

1. **Product Brain Never Teaches Randomly**: Every scene selection, rotation, and difficulty adjustment must have a clear pedagogical rationale.
2. **Minimize Learner Decision Burden**: The learner should never be forced to manually configure review modes, deck filters, or card intervals.
3. **Continuous Adaptation**: Teaching strategies adapt dynamically to real-time performance, response latency, and fatigue signals.
4. **Balance Challenge and Confidence**: Introduce high-challenge exercises (like Typing Recall) only when memory stability and learner confidence support them.
5. **Optimize Long-Term Memory**: Prioritize durable long-term retention over short-term session completion speed.
6. **Protect Learner Motivation**: Manage difficulty progression to maintain momentum and prevent frustration or burnout.

---

## 6. Multi-Year Evolutionary Roadmap

Product Brain is designed to evolve gracefully through future milestones without altering core architectural contracts:

- **Phase 1 (Current Baseline)**: Rule-based objective policy (`DURABLE_RECALL`), strategy planning, semantic template slots, and session-aware experience rotation.
- **Phase 2 (Adaptive Difficulty)**: Latency-aware challenge scaling and dynamic typing eligibility.
- **Phase 3 (Fatigue & Motivation Management)**: Cognitive fatigue detection via latency spikes and automated session pacing adjustments.
- **Phase 4 (AI-Augmented Content & Scaffolding)**: Generative contextual hints, semantic feedback, and personalized exercise generation.
- **Phase 5 (Cross-Platform Personalization)**: Unified cross-device Learner Models tracking lifelong retention trajectories across Desktop, Mobile, and Web.
